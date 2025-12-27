package owlfroggy.terracottaclient.api;

import com.google.gson.*;
import org.java_websocket.WebSocket;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.server.WebSocketServer;
import owlfroggy.terracottaclient.TCClient;
import owlfroggy.terracottaclient.api.message.Message;
import owlfroggy.terracottaclient.api.message.Request;
import owlfroggy.terracottaclient.api.message.impl.ProvideTokenA2CRequest;
import owlfroggy.terracottaclient.api.message.impl.RequestTokenA2CRequest;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.HashMap;
import java.util.HashSet;

public class APIServer extends WebSocketServer {
    private static final Path TOKEN_FILE_PATH = TCClient.getConfigPath().resolve("tokens.json");

    private final HashMap<WebSocket, APIConnectionHandler> connectedAppsBySocket = new HashMap<>();
    private final HashMap<java.lang.Integer, APIConnectionHandler> connectedAppsById = new HashMap<>();
    private final HashMap<String, APIToken> tokens = new HashMap<>();
    private int latestAppId = (int)(Math.random()*9999999);
    private int latestMessageId = 0;

    public APIServer(InetSocketAddress address) {
        super(address);

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
                                Permission p = Permission.valueOf(permissionElement.getAsString().toUpperCase());
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
                    Files.move(TOKEN_FILE_PATH,TCClient.getConfigPath().resolve("tokens_error_"+Instant.now().getEpochSecond()));
                Files.writeString(TOKEN_FILE_PATH,"[]");
            } catch (Exception e) {
                TCClient.LOGGER.error("Could not reset token file: {}\n{}",e.getMessage(),e.getStackTrace());
            }
        }
    }

    /**
     * @return returns null if the token is invalid, returns an APIToken object otherwise
     */
    public APIToken getTokenObject(String tokenString) {
        if (tokens.containsKey(tokenString)) return tokens.get(tokenString);
        return null;
    }
    public APIToken registerNewToken(String tokenString, String appName, HashSet<Permission> permissions) {
        APIToken token = new APIToken(tokenString,appName,permissions,Instant.now().getEpochSecond());
        tokens.put(tokenString,token);
        writeTokensToFile();
        return token;
    }

    private void writeTokensToFile() {
        JsonArray root = new JsonArray();
        for (APIToken token : tokens.values()) {
            JsonObject o = new JsonObject();
            o.addProperty("token",token.getToken());
            o.addProperty("app_name",token.getAppName());
            o.addProperty("created_on_timestamp",token.getCreatedOnTimestamp());
            o.add("permissions",
                new Gson().toJsonTree(token.getPermissions().stream().map(
                    (Permission p) -> p.name().toLowerCase()
                ).toArray())
            );
            root.add(o);
        }
        String serialized = root.toString();
        try {
            Files.writeString(TOKEN_FILE_PATH,serialized);
        } catch (IOException e) {
            TCClient.LOGGER.error("Could not write token file: {}\n{}",e.getMessage(),e.getStackTrace());
        }
    }

    public int getNewMessageId() {
        latestMessageId++;
        return latestMessageId;
    }

    public Message parseMessage(String rawMessage) {
        JsonObject json = JsonParser.parseString(rawMessage).getAsJsonObject();

        Message message;
        JsonObject data = json.get("data").getAsJsonObject();
        String type = json.get("type").getAsString();

        messageParser: switch (type) {
            case "request" -> {
                String method = json.get("method").getAsString();
                switch (method) {
                    case "request_token" -> {
                        HashSet<Permission> permissions = new HashSet<>();
                        for (JsonElement permission : data.get("permissions").getAsJsonArray()) {
                            Permission p;
                            try { p = Permission.valueOf(permission.getAsString()); }
                            catch (Exception e) { throw new RuntimeException("invalid_permission"); }
                            permissions.add(p);
                        }
                        message = new RequestTokenA2CRequest(data.get("app_name").getAsString(), permissions);
                        break messageParser;
                    }

                    case "provide_token" -> {
                        message = new ProvideTokenA2CRequest(data.get("token").getAsString());
                        break messageParser;
                    }

                    default -> throw new RuntimeException("invalid_request_method");
                }
            }
            default -> throw new RuntimeException("invalid_message_type");
        }

        int id = json.get("id").getAsInt();
        message.setId(id);

        //TODO: handle incorrectly formatted messages
        return message;
    }

    public void allowAppAuthentication(int appId) {
        if (!connectedAppsById.containsKey(appId))
            throw new RuntimeException("No connected app has id '%s'".formatted(appId));
        connectedAppsById.get(appId).allowAuthentication();
    }

    public void denyAppAuthentication(int appId) {
        if (!connectedAppsById.containsKey(appId))
            throw new RuntimeException("No connected app has id '%s'".formatted(appId));
        connectedAppsById.get(appId).denyAuthentication();
    }

    //=- websocket stuff below -=\\

    @Override
    public void onOpen(WebSocket conn, ClientHandshake handshake) {
//        conn.send("Welcome to the server!"); //This method sends a message to the new client
//        broadcast( "new connection: " + handshake.getResourceDescriptor() ); //This method sends a message to all clients connected
//        System.out.println("new connection to " + conn.getRemoteSocketAddress());
        latestAppId++;
        int id = latestAppId;
        APIConnectionHandler handler = new APIConnectionHandler(conn, handshake, id);
        connectedAppsBySocket.put(conn, handler);
        connectedAppsById.put(id, handler);
    }

    @Override
    public void onClose(WebSocket conn, int code, String reason, boolean remote) {
        TCClient.LOGGER.info("closed " + conn.getRemoteSocketAddress() + " with exit code " + code + " additional info: " + reason);
        int appId = connectedAppsBySocket.get(conn).getId();
        connectedAppsById.remove(appId);
        connectedAppsBySocket.remove(conn);
    }

    @Override
    public void onMessage(WebSocket conn, String message) {
        APIConnectionHandler handler = connectedAppsBySocket.get(conn);
        Message parsedMessage = parseMessage(message);


        if (parsedMessage instanceof Request request) {
            handler.onRequest(request);
        }

        TCClient.LOGGER.info("received message from "	+ conn.getRemoteSocketAddress() + ": " + message);
    }

//    @Override
//    public void onMessage( WebSocket conn, ByteBuffer message ) {
//        System.out.println("received ByteBuffer from "	+ conn.getRemoteSocketAddress());
//    }

    @Override
    public void onError(WebSocket conn, Exception ex) {
        TCClient.LOGGER.error("an error occurred on connection " + conn.getRemoteSocketAddress()  + ":" + ex);
    }

    @Override
    public void onStart() {
        TCClient.LOGGER.info("server started successfully");
    }
}
