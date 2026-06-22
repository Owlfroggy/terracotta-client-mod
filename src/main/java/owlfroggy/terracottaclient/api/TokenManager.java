package owlfroggy.terracottaclient.api;

import com.google.gson.*;
import owlfroggy.terracottaclient.TCClient;

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

                            JsonArray permissionsJsonArray = tokenObject.get("permissions").getAsJsonArray();
                            HashSet<Permission> permissions = new HashSet<>();
                            for (JsonElement permissionElement : permissionsJsonArray) {
                                Permission p = Permission.valueOf(permissionElement.getAsString());
                                permissions.add(p);
                            }

                            tokens.put(tokenString, new APIToken(
                                tokenString,appName,permissions,createdOnTimestamp
                            ));
                        }
                    } catch (Exception ignored) {} // if a token is invalid just throw it away
                } // end of loading for loop
            } else {
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

    private static void writeTokensToFile() {
        requireLoaded();
        JsonArray root = new JsonArray();
        for (APIToken token : getAllTokens()) {
            JsonObject o = new JsonObject();
            o.addProperty("token",token.getToken());
            o.addProperty("app_name",token.getAppName());
            o.addProperty("created_on_timestamp",token.getCreatedOnTimestamp());
            o.add("permissions",
                new Gson().toJsonTree(token.getPermissions().stream().map(Enum::name).toArray()));
            root.add(o);
        }
        String serialized = root.toString();
        try {
            Files.writeString(TOKEN_FILE_PATH,serialized);
        } catch (IOException e) {
            TCClient.LOGGER.error("Could not write token file: {}\n{}",e.getMessage(),e.getStackTrace());
        }
    }

    /**
     * @return returns null if the token is invalid, returns an APIToken object otherwise
     */
    public static APIToken getToken(String tokenString) {
        requireLoaded();
        if (tokens.containsKey(tokenString)) return tokens.get(tokenString);
        return null;
    }
    public static APIToken registerNewToken(String tokenString, String appName, Set<Permission> permissions) {
        requireLoaded();
        APIToken token = new APIToken(tokenString,appName,permissions,Instant.now().getEpochSecond());
        tokens.put(tokenString,token);
        writeTokensToFile();
        return token;
    }

    public static Collection<APIToken> getAllTokens() {
        requireLoaded();
        return tokens.values();
    }
}
