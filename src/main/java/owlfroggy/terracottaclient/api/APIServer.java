package owlfroggy.terracottaclient.api;

import com.google.gson.*;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.datafixers.util.Function8;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.text.Text;
import org.java_websocket.WebSocket;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.server.WebSocketServer;
import owlfroggy.terracottaclient.TCClient;
import owlfroggy.terracottaclient.api.message.Message;
import owlfroggy.terracottaclient.api.message.Request;
import owlfroggy.terracottaclient.api.message.Response;
import owlfroggy.terracottaclient.api.message.impl.InitiateCodeEditA2CRequest;
import owlfroggy.terracottaclient.api.message.impl.ProvideTokenA2CRequest;
import owlfroggy.terracottaclient.api.message.impl.ProvideTokenC2AResponse;
import owlfroggy.terracottaclient.api.message.impl.RequestTokenA2CRequest;
import owlfroggy.terracottaclient.codespacemanager.TemplateIdentifier;
import owlfroggy.terracottaclient.codespacemanager.TemplateType;

import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.net.InetSocketAddress;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.HashMap;
import java.util.HashSet;
import java.util.function.BiFunction;
import java.util.function.Function;

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

                // request types must be mapped here or else they won't parse
                BiFunction<JsonObject, JsonObject, Request> parser = (switch (method) {
                    case "request_token" -> RequestTokenA2CRequest::parse;
                    case "provide_token" -> ProvideTokenC2AResponse::parse;
                    case "initiate_code_edit" -> InitiateCodeEditA2CRequest::parse;
                    default -> throw new RuntimeException("invalid_request_method");
                });
                message = parser.apply(json, data);
            }
            default -> throw new RuntimeException("invalid_message_type");
        }

        int id = json.get("id").getAsInt();
        message.setId(id);

        //TODO: handle incorrectly formatted messages
        return message;
    }

    /** this is the command handler for /tcallow and /tcdeny */
    public static int decideAppAuthentication(CommandContext<FabricClientCommandSource> commandContext, boolean allow) {
        if (TCClient.API_SERVER == null)
            commandContext.getSource().sendError(Text.literal("Terracotta API has not started yet."));

        int appId = IntegerArgumentType.getInteger(commandContext, "app_id");
        if (!TCClient.API_SERVER.connectedAppsById.containsKey(appId)) {
            commandContext.getSource().sendError(Text.literal("No connected app has id '%s'".formatted(appId)));
            return 0;
        }
        APIConnectionHandler app = TCClient.API_SERVER.connectedAppsById.get(appId);

        if (!app.isPendingAuthentication()) {
            if (app.isAuthenticated()) {
                if (allow) {
                    commandContext.getSource().sendError(Text.literal("App has already been authenticated."));
                    return 0;
                } else {
                    //TODO: Make this actually work
                    commandContext.getSource().sendError(Text.literal("App has already been authenticated. If you want to disconnect the app, click [here]."));
                    return 0;
                }
            } else {
                commandContext.getSource().sendError(Text.literal("App has already been denied authentication."));
                return 0;
            }
        }

        if (allow) {
            app.allowAuthentication();
            commandContext.getSource().sendFeedback(Text.literal("Allowed %s".formatted(appId)));
        } else {
            app.denyAuthentication();
            commandContext.getSource().sendFeedback(Text.literal("Denied %s".formatted(appId)));
        }

        return 1;
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
