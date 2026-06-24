package owlfroggy.terracottaclient.api;

import com.google.gson.*;
import net.minecraft.network.chat.Component;
import owlfroggy.terracottaclient.MsgHelper;
import owlfroggy.terracottaclient.TCClient;
import owlfroggy.terracottaclient.ui.TokenManagementScreen;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

public class TokenManager {
    private static final Path TOKEN_FILE_PATH = TCClient.getConfigPath().resolve("tokens.json");
    private static final HashMap<String, APIToken> tokens = new HashMap<>();
    private static boolean isLoaded = false;

    private static void requireLoaded() throws RuntimeException {
        if (!isLoaded) throw new RuntimeException("Tokens have not yet been loaded from disk.");
    }

    static {
        // load tokens
        boolean resetTokenFile = false;
        try {
            if (Files.exists(TOKEN_FILE_PATH)) {
                String contents = Files.readString(TOKEN_FILE_PATH);
                JsonArray tokenArray = JsonParser.parseString(contents).getAsJsonArray();
                for (JsonElement tokenElement : tokenArray) {
                    try {
                        if (tokenElement instanceof JsonObject tokenObject) {
                            String tokenString = tokenObject.get("token").getAsString();
                            String appName = tokenObject.get("app_name").getAsString();
                            long createdOnTimestamp = tokenObject.get("created_on_timestamp").getAsLong();
                            long expiresOnTimestamp = tokenObject.get("expires_on_timestamp").getAsLong();
                            long lastUsedTimestamp = tokenObject.get("last_used_timestamp").getAsLong();

                            // token expiration
                            if (expiresOnTimestamp < Instant.now().getEpochSecond()) {
                                continue;
                            }

                            JsonArray permissionsJsonArray = tokenObject.get("permissions").getAsJsonArray();
                            HashSet<Permission> permissions = new HashSet<>();
                            for (JsonElement permissionElement : permissionsJsonArray) {
                                Permission p = Permission.valueOf(permissionElement.getAsString());
                                permissions.add(p);
                            }

                            tokens.put(tokenString, new APIToken(
                                tokenString,appName,permissions,createdOnTimestamp,expiresOnTimestamp,lastUsedTimestamp
                            ));
                        }
                    } catch (Exception ignored) {} // if a token is invalid just throw it away
                } // end of loading for loop
            } else {
                TCClient.LOGGER.warn("No token file exists, will reset to create one");
                resetTokenFile = true;
            }
        } catch (Exception e) {
            TCClient.LOGGER.error("Could not read token file: {}\n{}",e.getMessage(),e.getStackTrace());
            resetTokenFile = true;
        }

        if (resetTokenFile) {
            try {
                if (Files.exists(TOKEN_FILE_PATH))
                    Files.move(TOKEN_FILE_PATH,TCClient.getConfigPath().resolve("tokens_error_"+ Instant.now().getEpochSecond()));
                Files.writeString(TOKEN_FILE_PATH,"[]");
            } catch (Exception e) {
                TCClient.LOGGER.error("Could not reset token file: {}\n{}",e.getMessage(),e.getStackTrace());
            }
        }
        isLoaded = true;
    }

    private static boolean isWriting = false;
    public static void writeTokensToFile() {
        requireLoaded();
        if (isWriting) throw new RuntimeException("Attempted to write to token file while a write was already in progress");
        JsonArray root = new JsonArray();
        for (APIToken token : getAllTokens()) {
            JsonObject o = new JsonObject();
            o.addProperty("token",token.getToken());
            o.addProperty("app_name",token.getAppName());
            o.addProperty("created_on_timestamp",token.getCreatedOnTimestamp());
            o.addProperty("expires_on_timestamp",token.getExpiresOnTimestamp());
            o.addProperty("last_used_timestamp",token.getLastUsedTimestamp());
            o.add("permissions",
                new Gson().toJsonTree(token.getPermissions().stream().map(Enum::name).toArray()));
            root.add(o);
        }
        String serialized = root.toString();
        isWriting = true;
        try {
            Files.writeString(TOKEN_FILE_PATH,serialized);
        } catch (IOException e) {
            TCClient.LOGGER.error("Could not write token file: {}\n{}",e.getMessage(),e.getStackTrace());
        }
        isWriting = false;
    }

    /**
     * @return returns null if the token is invalid, returns an APIToken object otherwise
     */
    public static APIToken getToken(String tokenString) {
        requireLoaded();
        if (tokens.containsKey(tokenString)) return tokens.get(tokenString);
        return null;
    }

    /**
     * @param secondsUntilExpiration passing -1 will set the token's expirationTimestamp to -1
     */
    public static APIToken registerNewToken(String tokenString, String appName, Set<Permission> permissions, long secondsUntilExpiration) {
        requireLoaded();
        long now = Instant.now().getEpochSecond();
        APIToken token = new APIToken(
            tokenString,
            appName,
            permissions,
            now,
            secondsUntilExpiration == -1 ? -1 : now + secondsUntilExpiration,
            now
        );
        tokens.put(tokenString,token);
        writeTokensToFile();
        return token;
    }

    public static Collection<APIToken> getAllTokens() {
        requireLoaded();
        return tokens.values();
    }

    /** Also disconnects all apps that were actively using this token */
    public static void removeToken(String tokenString) {
        APIServer.getTokenConnections(tokenString).forEach(APIConnectionHandler::forceDisconnect);

        if (!tokens.containsKey(tokenString)) return;
        APIToken token = tokens.remove(tokenString);

        if (TCClient.MCI.gui.screen() instanceof TokenManagementScreen s) {
            TokenManagementScreen.show(s.parent);
        }

        writeTokensToFile();

        MsgHelper.safeTCMessage(
            Component.translatable(
                "terracotta-client.permissions.appRemoved",
                Component.literal(token.getAppName()).withColor(MsgHelper.COLOR.TC_ORANGE)
            )
        );
    }
    public static void removeToken(APIToken token) {
        removeToken(token.getToken());
    }
}
