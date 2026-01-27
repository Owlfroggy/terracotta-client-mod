package owlfroggy.terracottaclient.api;

import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.server.command.CommandManager;
import net.minecraft.text.Text;
import net.minecraft.util.Colors;
import org.java_websocket.WebSocket;
import org.java_websocket.handshake.ClientHandshake;
import owlfroggy.terracottaclient.DFState;
import owlfroggy.terracottaclient.TCClient;
import owlfroggy.terracottaclient.Utils;
import owlfroggy.terracottaclient.api.message.ErrorResponse;
import owlfroggy.terracottaclient.api.message.Notification;
import owlfroggy.terracottaclient.api.message.Request;
import owlfroggy.terracottaclient.api.message.Response;
import owlfroggy.terracottaclient.api.message.impl.*;
import owlfroggy.terracottaclient.itemlibrary.ItemLibraryManager;
import owlfroggy.terracottaclient.itemlibrary.NoSpaceException;
import owlfroggy.terracottaclient.itemrenderer.ItemRenderGenerator;

import java.util.HashMap;
import java.util.HashSet;

public class APIConnectionHandler {
    private final char[] TOKEN_CHARACTERS = {'A','B','C','D','E','F','G','H','I','J','K','L','M','N','O','P','Q','R','S','T','U','V','W','X','Y','Z','a','b','c','d','e','f','g','h','i','j','k','l','m','n','o','p','q','r','s','t','u','v','w','x','y','z','0','1','2','3','4','5','6','7','8','9'};
    private int TOKEN_LENGTH = 32;

    private WebSocket connection;
    private ClientHandshake handshake;
    private Thread homeThread;

    private final int id;

    // auth-related
    private String appName = "<uninitialized app>";
    private APIToken token = null;
    private Request authenticationRequest = null;
    private HashSet<Permission> permissions;

    APIConnectionHandler(WebSocket connection, ClientHandshake handshake, int id) {
        this.id = id;
        this.connection = connection;
        this.handshake = handshake;
        homeThread = Thread.currentThread();
    }

    private String generateTokenString() {
        StringBuilder t = new StringBuilder();
        for (int i = 0; i < TOKEN_LENGTH; i++) {
            t.append(TOKEN_CHARACTERS[(int) (Math.random() * TOKEN_CHARACTERS.length)]);
        }

        return t.toString();
    }

    private void sendInitialState() {
        sendNotification(new ModeChangedC2ANotification(TCClient.DF_STATE.getMode()));
        sendNotification(new PlotChangedC2ANotification(TCClient.DF_STATE.getPlotId(),TCClient.DF_STATE.getPlotName()));
    }

    protected void respond(Request request, Response response) {
        response.setId(request.getId());
        String json = response.serialize();
        connection.send(json);
    }

    public int getId() {
        return id;
    }

    public boolean isAuthenticated() {
        return token != null;
    }

    public boolean isPendingAuthentication() {
        return authenticationRequest != null;
    }

    public void allowAuthentication() {
        if (authenticationRequest instanceof RequestTokenA2CRequest r) {
            String tokenString = generateTokenString();
            permissions = r.getPermissions();
            token = TCClient.API_SERVER.registerNewToken(tokenString, r.getAppName(), permissions);
            respond(r,new RequestTokenC2AResponse(tokenString));
            authenticationRequest = null;
            TCClient.safeMessage(Text.literal("authed "+appName).withColor(Colors.GREEN));
            sendInitialState();
        }
    }

    public void denyAuthentication() {
        respond(authenticationRequest,new ErrorResponse(
            APIErrorCode.AUTHENTICATION_DENIED,
            "Authentication was denied from within Minecraft."
        ));
        // TODO: send a different error message for invalid token
        authenticationRequest = null;
    }

    public void onRequest(Request request) {
        if (request instanceof RequestTokenA2CRequest r) {
            if (appName.equals("<uninitialized app>")) appName = r.getAppName();
            if (token != null) {
                respond(r,new ErrorResponse(
                    APIErrorCode.TOKEN_ALREADY_PROVIDED,
                    "Applications cannot use more than one token."
                ));
                return;
            }
            if (authenticationRequest != null) {
                respond(r,new ErrorResponse(
                    APIErrorCode.ALREADY_AUTHENTICATING,
                    "An authentication attempt is already in progress."
                ));
                return;
            }

            authenticationRequest = r;

            TCClient.safeMessage(Text.literal(
                r.getAppName() + "is tryin  to connect w/ permissions: " + r.getPermissions().toString()
                + "     & appid = " + getId()
            ));
        }
        else if (request instanceof ProvideTokenA2CRequest r) {
            APIToken token = TCClient.API_SERVER.getTokenObject(r.getToken());
            if (token == null) {
                respond(r, new ErrorResponse(APIErrorCode.INVALID_TOKEN, "Invalid token."));
            } else {
                TCClient.safeMessage(Text.literal(
                    "An app '%s' just connected to terracotta with the following permissions: %s".formatted(token.getAppName(),token.getPermissions())
                ));
                respond(r, new ProvideTokenC2AResponse());
                sendInitialState();
            }
        }
        else if (request instanceof InitiateCodeEditA2CRequest r) {
            if (TCClient.CODESPACE_MANAGER.isEditingCode()) {
                respond(r, new ErrorResponse(
                    APIErrorCode.EDIT_IN_PROGRESS,
                    "A code edit operation is already in progress."
                ));
                return;
            }
            if (TCClient.DF_STATE.getMode() != DFState.Mode.DEV) {
                respond(r, new ErrorResponse(
                    APIErrorCode.NOT_IN_DEV,
                    "Code cannot be edited when not in dev mode."
                ));
                return;
            }

            try {
                TCClient.CODESPACE_MANAGER.editCode(r.getPlaceTemplates(), r.getBreakTemplates());
                TCClient.API_SERVER.setRequestAsPending(r, this);
            } catch (Exception e) {
                respond(r, new ErrorResponse(
                    APIErrorCode.EDIT_FAILED,
                    e.getMessage()
                ));
                return;
            }
        }
        else if (request instanceof ChangeModeA2CRequest r) {
            if (TCClient.DF_STATE.getMode() == DFState.Mode.SPAWN) {
                respond(r, new ErrorResponse(
                    APIErrorCode.AT_SPAWN,
                    "Mode cannot be changed from spawn."
                ));
                return;
            }
            if (r.getMode() == DFState.Mode.SPAWN) {
                respond(r, new ErrorResponse(
                    APIErrorCode.INVALID_MODE,
                    "THe CHANGE_MODE request cannot change to spawn."
                ));
                return;
            }
            TCClient.API_SERVER.setRequestAsPending(r,this);
            TCClient.COMMAND_MANAGER.queueCommand("mode "+r.getMode());
        }
        else if (request instanceof StartEditingItemA2CRequest r) {
            TCClient.ITEM_LIBRARY_MANAGER.startEditingItem(getId(),r.getItemId(),r.getSnbt(),r.getItemDataVersion());
            respond(r, new StartEditingItemC2AResponse());
        }
        else if (request instanceof StopEditingItemA2CRequest r) {
            TCClient.ITEM_LIBRARY_MANAGER.stopEditingItem(r.getItemId());
        }
        else if (request instanceof GiveItemA2CRequest r) {
            int slot = TCClient.MCI.player.getInventory().getEmptySlot();
            if (slot == -1) {
                respond(r, new ErrorResponse(APIErrorCode.NO_SPACE, "Not inventory enough space to give item."));
                return;
            }

            try {
                TCClient.MCI.player.getInventory().setStack(slot, Utils.snbtToItem(r.getSnbt()));
            } catch (Exception e) {
                respond(r, new ErrorResponse(APIErrorCode.INVALID_ITEM_DATA, "Invalid item data: "+e));
            }
        }
        else if (request instanceof GetInventoryA2CRequest r) {
            PlayerInventory inv = TCClient.MCI.player.getInventory();
            HashMap<Integer, GetInventoryC2AResponse.ItemEntry> itemEntries = new HashMap<>();
            for (int slot = 0; slot < inv.size(); slot++) {
                ItemStack item = inv.getStack(slot);
                if (item.isEmpty()) continue;
                if (TCClient.ITEM_LIBRARY_MANAGER.getLibraryData(item) != null) continue;
                itemEntries.put(slot, new GetInventoryC2AResponse.ItemEntry(
                    item.getName().getString(),
                    Utils.itemToSnbt(item)
                ));
            }
            respond(r, new GetInventoryC2AResponse(itemEntries));
        }
        else if (request instanceof RenderItemA2CRequest r) {
            ItemStack item = Utils.snbtToItem(r.getSnbt());
            try {
                TCClient.MCI.execute(() -> {
                    ItemRenderGenerator.renderToDataURI(item,2,image -> {
                        if (image.equals("error")) {
                            respond(r, new ErrorResponse(APIErrorCode.GENERIC_ERROR, "Item could not be rendered"));
                        } else {
                            respond(r, new RenderItemC2AResponse(image));
                        }
                    });
                });
            } catch (Exception e) {
                respond(r, new ErrorResponse(APIErrorCode.GENERIC_ERROR, "Item could not be rendered: " + e));
            }
        }
    }


    public void sendNotification(String serializedNotification) {
        TCClient.LOGGER.info("sending notif {}",serializedNotification);
        connection.send(serializedNotification);
    }
    public void sendNotification(Notification notification) {
        if (notification.getId() == -1) notification.setId(TCClient.API_SERVER.getNewNotificationId());
        sendNotification(notification.serialize());
    }
}
