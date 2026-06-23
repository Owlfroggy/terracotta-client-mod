package owlfroggy.terracottaclient.api;

import com.google.gson.*;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.context.CommandContext;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextColor;
import org.java_websocket.WebSocket;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.server.WebSocketServer;
import owlfroggy.terracottaclient.MsgHelper;
import owlfroggy.terracottaclient.TCClient;
import owlfroggy.terracottaclient.api.message.*;
import owlfroggy.terracottaclient.api.message.impl.*;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.*;
import java.util.function.BiFunction;
import java.util.function.Function;

public class APIServer extends WebSocketServer {
    private boolean serverIsOpen = false;
    protected final HashMap<WebSocket, APIConnectionHandler> connectedAppsBySocket = new HashMap<>();
    protected final HashMap<java.lang.Integer, APIConnectionHandler> connectedAppsById = new HashMap<>();
    protected final HashMap<String, List<APIConnectionHandler>> connectedAppsByToken = new HashMap<>();
    private final Set<Request> pendingRequests = new HashSet();
    private static int latestAppId = (int)(Math.random()*9999999);
    private static int latestMessageId = 0;
    private static int latestNotificationId = 0;


    public boolean isOpen() { return serverIsOpen; }

    public APIServer(InetSocketAddress address) {
        super(address);
    }

    /** If there is no active API server, this function does nothing */
    public static void broadcastNotification(Notification notification) {
        if (TCClient.API_SERVER == null) return;

        notification.setId(TCClient.API_SERVER.getNewNotificationId());
        String serialized = notification.serialize();
        for (APIConnectionHandler handler : TCClient.API_SERVER.connectedAppsBySocket.values()) {
            handler.sendNotification(serialized, notification.getRequiredPermission());
        }
    }

    /** If there is no active API server, this function does nothing */
    public static void sendNotification(int appId, Notification notification) {
        if (TCClient.API_SERVER == null) return;

        APIConnectionHandler handler = TCClient.API_SERVER.connectedAppsById.get(appId);
        handler.sendNotification(notification);
    }

    /**
     * If there is no active API server, this function does nothing.
     * Loops through all pending requests and runs resolver on them.
     * @param resolver Return a response to respond to a request, return null to skip over it
     */
    public static void resolvePendingRequests(Function<Request, Response> resolver) {
        if (TCClient.API_SERVER == null) return;
        for (Request request : TCClient.API_SERVER.pendingRequests.stream().toList()) {
            Response response = resolver.apply(request);
            if (response == null) continue;
            request.getHandler().respond(request,response);
            TCClient.API_SERVER.pendingRequests.remove(request);
        }
    }

    public static boolean hasConnectedAppId(int appId) {
        if (TCClient.API_SERVER == null) return false;
        return TCClient.API_SERVER.connectedAppsById.containsKey(appId);
    }

    /** Returns the connection handlers of all connected apps currently using this token */
    public static List<APIConnectionHandler> getTokenConnections(String tokenString) {
        if (
            TCClient.API_SERVER == null ||
            !TCClient.API_SERVER.connectedAppsByToken.containsKey(tokenString)
        ) return new ArrayList<>();
        return TCClient.API_SERVER.connectedAppsByToken.get(tokenString).stream().toList();
    }
    public static List<APIConnectionHandler> getTokenConnections(APIToken token) {
        return getTokenConnections(token.getToken());
    }

    public int getNewMessageId() {
        latestMessageId++;
        return latestMessageId;
    }

    public int getNewNotificationId() {
        latestNotificationId++;
        return latestNotificationId;
    }

    public void setRequestAsPending(Request request, APIConnectionHandler handler) {
        request.setHandler(handler);
        pendingRequests.add(request);
    }

    public Message parseMessage(String rawMessage) throws MessageParsingException {
        JsonObject json = JsonParser.parseString(rawMessage).getAsJsonObject();

        Message message;
        JsonObject data = json.get("data").getAsJsonObject();
        String type = json.get("type").getAsString();

        int id = json.get("id").getAsInt();
        Request reducedRequest = Request.parse(json, data);
        reducedRequest.setId(id);

        messageParser: switch (type) {
            case "REQUEST" -> {
                String method = json.get("method").getAsString();

                // request types must be mapped here or else they won't parse
                BiFunction<JsonObject, JsonObject, Request> parser = (switch (method) {
                    case "REQUEST_TOKEN" -> RequestTokenA2CRequest::parse;
                    case "PROVIDE_TOKEN" -> ProvideTokenA2CRequest::parse;
                    case "INITIATE_CODE_EDIT" -> InitiateCodeEditA2CRequest::parse;
                    case "CHANGE_MODE" -> ChangeModeA2CRequest::parse;
                    case "START_EDITING_ITEM" -> StartEditingItemA2CRequest::parse;
                    case "STOP_EDITING_ITEM" -> StopEditingItemA2CRequest::parse;
                    case "RENDER_ITEM" -> RenderItemA2CRequest::parse;
                    case "GIVE_ITEM" -> GiveItemA2CRequest::parse;
                    case "GET_INVENTORY" -> GetInventoryA2CRequest::parse;
                    case "RESCAN_PLOT" -> RescanPlotA2CRequest::parse;
                    default -> throw new MessageParsingException("Invalid request method", reducedRequest);
                });
                try {
                    message = parser.apply(json, data);
                } catch (Exception e) {
                    throw new MessageParsingException(e,reducedRequest);
                }
            }
            default -> throw new MessageParsingException("Invalid message type",reducedRequest);
        }

        message.setId(id);

        //TODO: handle incorrectly formatted messages
        return message;
    }

    /** this is the command handler for /tcallow and /tcdeny */
    public static int decideAppAuthentication(CommandContext<FabricClientCommandSource> commandContext, boolean allow) {
        if (TCClient.API_SERVER == null)
            MsgHelper.safeTCMessage(Component.translatable("terracotta-client.permissions.apiNotStartedYet").withColor(TextColor.RED));

        int appId = IntegerArgumentType.getInteger(commandContext, "app_id");
        if (!TCClient.API_SERVER.connectedAppsById.containsKey(appId)) {
            MsgHelper.safeTCMessage(Component.translatable("terracotta-client.permissions.invalidApp",appId).withColor(TextColor.RED));
            return 0;
        }
        APIConnectionHandler app = TCClient.API_SERVER.connectedAppsById.get(appId);

        if (!app.isPendingAuthentication()) {
            if (app.isAuthenticated()) {
                if (allow) {
                    MsgHelper.safeTCMessage(Component.translatable("terracotta-client.permissions.alreadyConnected").withColor(TextColor.RED));
                    return 0;
                } else {
                    MsgHelper.safeTCMessage(Component.translatable("terracotta-client.permissions.alreadyConnected").withColor(TextColor.RED));
                    MsgHelper.safeMessage(MsgHelper.getManagePermissionsButton());
                    return 0;
                }
            } else {
                MsgHelper.safeTCMessage(Component.translatable("terracotta-client.permissions.alreadyDenied").withColor(TextColor.RED));
                return 0;
            }
        }

        if (allow) {
            app.allowAuthentication();
        } else {
            app.denyAuthentication();
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
        APIConnectionHandler app = connectedAppsBySocket.get(conn);

        APIToken token = app.getToken();
        if (token != null) {
            token.bumpLastUsedTimestamp();
            try {
                TokenManager.writeTokensToFile();
            } catch (Exception e) {
                TCClient.LOGGER.error("error while writing tokens on connection close", e);
            }
            connectedAppsByToken.get(token.getToken()).remove(app);
            if (connectedAppsByToken.get(token.getToken()).isEmpty())
                connectedAppsByToken.remove(token.getToken());
        }



        int appId = app.getId();
        connectedAppsById.remove(appId);
        connectedAppsBySocket.remove(conn);
    }

    @Override
    public void onMessage(WebSocket conn, String message) {
        APIConnectionHandler handler = connectedAppsBySocket.get(conn);
        TCClient.LOGGER.info("received message from "	+ conn.getRemoteSocketAddress() + ": " + message);
        try {
            Message parsedMessage = parseMessage(message);
            if (parsedMessage instanceof Request request) {
                try {
                    handler.onRequest(request);
                } catch (APIException e) {
                    handler.respond(request, new ErrorResponse(e.code, e.getMessage()));
                } catch (Exception e) {
                    handler.respond(request, new ErrorResponse(APIErrorCode.INTERNAL_ERROR, e.getMessage()));
                }
            }
        } catch (Exception exception) {
            if (exception instanceof MessageParsingException e) {
                handler.respond(e.getReducedRequest(),new ErrorResponse(APIErrorCode.MALFORMED_MESSAGE,"Malformed request: "+e.getMessage()));
            } else {
                TCClient.LOGGER.error("Error while handling request",exception);
            }
        }
    }

//    @Override
//    public void onMessage( WebSocket conn, ByteBuffer message ) {
//        System.out.println("received ByteBuffer from "	+ conn.getRemoteSocketAddress());
//    }

    @Override
    public void onError(WebSocket conn, Exception ex) {
        TCClient.LOGGER.error("an error occurred on connection " + conn.getRemoteSocketAddress()  + ":" + ex);
        //stop();
    }

    @Override
    public void onStart() {
        TCClient.LOGGER.info("server started successfully");
        serverIsOpen = true;
    }

    @Override
    public void stop() throws InterruptedException {
        TCClient.ITEM_LIBRARY_MANAGER.stopEditingAllItems();
        super.stop();
        TCClient.API_SERVER = null;
        TCClient.LOGGER.info("server shut down successfully");
    }
}
