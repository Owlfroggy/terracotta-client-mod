package owlfroggy.terracottaclient.api;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.java_websocket.WebSocket;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.server.WebSocketServer;
import owlfroggy.terracottaclient.TCClient;
import owlfroggy.terracottaclient.api.message.Message;
import owlfroggy.terracottaclient.api.message.Request;
import owlfroggy.terracottaclient.api.message.impl.ProvideTokenA2CRequest;
import owlfroggy.terracottaclient.api.message.impl.RequestTokenA2CRequest;

import java.net.InetSocketAddress;
import java.util.HashMap;
import java.util.HashSet;

public class APIServer extends WebSocketServer {
    public APIServer(InetSocketAddress address) {
        super(address);
    }

    private final HashMap<WebSocket, APIConnectionHandler> connectedAppsBySocket = new HashMap<>();
    private final HashMap<java.lang.Integer, APIConnectionHandler> connectedAppsById = new HashMap<>();
    private int latestAppId = (int)(Math.random()*9999999);
    private int latestMessageId = 0;

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
