package owlfroggy.terracottaclient.api;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.*;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import org.java_websocket.WebSocket;
import org.java_websocket.handshake.ClientHandshake;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import owlfroggy.terracottaclient.DFState;
import owlfroggy.terracottaclient.MsgHelper;
import owlfroggy.terracottaclient.TCClient;
import owlfroggy.terracottaclient.Utils;
import owlfroggy.terracottaclient.api.message.*;
import owlfroggy.terracottaclient.api.message.impl.*;
import owlfroggy.terracottaclient.config.Config;
import owlfroggy.terracottaclient.config.ConnectionMessageMode;
import owlfroggy.terracottaclient.itemrenderer.ItemRenderGenerator;

import java.util.*;

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
    private Set<Permission> permissions = new HashSet<>();

    APIConnectionHandler(WebSocket connection, ClientHandshake handshake, int id) {
        this.id = id;
        this.connection = connection;
        this.handshake = handshake;
        homeThread = Thread.currentThread();
    }

    public void forceDisconnect() {
        connection.close();
    }

    public void setToken(@NonNull APIToken token) {
        this.token = token;
        List<APIConnectionHandler> connections = TCClient.API_SERVER.connectedAppsByToken.getOrDefault(token.getToken(), null);
        if (connections == null) {
            connections = new ArrayList<>();
            TCClient.API_SERVER.connectedAppsByToken.put(token.getToken(), connections);
        }
        connections.add(this);
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
        sendNotification(new ScanStateChangedC2ANotification(TCClient.DF_STATE.getScanState()));
    }

    protected void respond(Request request, Response response) {
        if (request.hasBeenRespondedTo()) return;
        request.markAsRespondedTo();
        response.setId(request.getId());
        String json = response.serialize();
        connection.send(json);
    }

    /**respond
     * @return Returns true if the permission check passed, false if the required permission is not present
     */
    protected boolean hasRequiredPermission(Permission requiredPermission) {
        if (requiredPermission == null) return true;
        return permissions.contains(requiredPermission);
    }

    public int getId() {
        return id;
    }
    public @Nullable APIToken getToken() { return token; }

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
            setToken(TokenManager.registerNewToken(tokenString, r.getAppName(), permissions));
            respond (r,new RequestTokenC2AResponse(tokenString));
            authenticationRequest = null;
            sendInitialState();

            MsgHelper.safeTCMessage(
                Component.empty()
                    .append(Component.literal("✔ ").withColor(MsgHelper.COLOR.LIGHT_GREEN).withStyle(ChatFormatting.BOLD))
                    .append(Component.translatable("terracotta-client.permissions.allowedConfirmtaion",Component.literal(appName).withColor(MsgHelper.COLOR.TC_ORANGE)))
                    .append(" ")
                    .append(MsgHelper.getIndefiniteAccessWarning())
            );
        }
    }

    public void denyAuthentication() {
        respond(authenticationRequest,new ErrorResponse(
            APIErrorCode.AUTHENTICATION_DENIED,
            "Authentication was denied from within Minecraft."
        ));
        // TODO: send a different error message for invalid token
        authenticationRequest = null;

        MsgHelper.safeTCMessage(
            Component.empty()
                .append(Component.literal("❌ ").withColor(MsgHelper.COLOR.LIGHT_RED).withStyle(ChatFormatting.BOLD))
                .append(Component.translatable("terracotta-client.permissions.deniedConfirmtaion",Component.literal(appName).withColor(MsgHelper.COLOR.TC_ORANGE)))
        );
    }

    public void onRequest(Request request) {
        if (!hasRequiredPermission(request.getRequiredPermission())) {
            respond(request, new ErrorResponse(APIErrorCode.NO_PERMISSION, "Required permission " + request.getRequiredPermission().name() + " is missing."));
            return;
        }

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

            MsgHelper.safeMessage(Component.empty());
            MsgHelper.safeMessage(MsgHelper.textifyPermissions(r.getPermissions()));

            MsgHelper.safeTCMessage(Component.empty()
                .append(
                    Component.translatable(
                        "terracotta-client.permissions.newAppConnected",
                        Component.literal(r.getAppName()).withColor(MsgHelper.COLOR.TC_ORANGE)
                    )
                )
                .append("\n\n   ")
                .append(
                    Component.translatable("terracotta-client.permissions.clickable.accept")
                        .withColor(MsgHelper.COLOR.LIGHT_GREEN)
                        .withStyle(ChatFormatting.BOLD)
                        .withStyle(Style.EMPTY.withClickEvent(
                            new ClickEvent.RunCommand("tcallow "+getId())
                        ))
                )
                .append("    ")
                .append(
                    Component.translatable("terracotta-client.permissions.clickable.deny")
                    .withColor(MsgHelper.COLOR.LIGHT_RED)
                    .withStyle(ChatFormatting.BOLD)
                    .withStyle(Style.EMPTY.withClickEvent(
                        new ClickEvent.RunCommand("tcdeny "+getId())
                    ))
                )
                .append("\n")
            );
        }
        else if (request instanceof ProvideTokenA2CRequest r) {
            APIToken token = TokenManager.getToken(r.getToken());
            if (token == null) {
                respond(r, new ErrorResponse(APIErrorCode.INVALID_TOKEN, "Invalid token."));
            } else {
                if (Config.connectionMessageMode == ConnectionMessageMode.VERBOSE) {
                    MsgHelper.safeMessage(Component.empty());
                    MsgHelper.safeMessage(MsgHelper.textifyPermissions(token.getPermissions()));
                    MsgHelper.safeTCMessage(Component.empty()
                        .append(
                            Component.translatable(
                                "terracotta-client.permissions.knownAppConnected.verbose",
                                Component.literal(token.getAppName()).withColor(MsgHelper.COLOR.TC_ORANGE)
                            )
                        )
                        .append(". ")
                        .append(MsgHelper.getIndefiniteAccessWarning())
                    );
                } else if (Config.connectionMessageMode == ConnectionMessageMode.MINIMAL) {
                    MsgHelper.safeTCMessage(Component.translatable(
                        "terracotta-client.permissions.knownAppConnected.minimal",
                        Component.literal(token.getAppName()).withColor(MsgHelper.COLOR.TC_ORANGE)
                    ));
                }
                token.bumpLastUsedTimestamp();
                TokenManager.writeTokensToFile();
                setToken(token);
                this.permissions = token.getPermissions();
                respond(r, new ProvideTokenC2AResponse());
                sendInitialState();
            }
        }
        else if (request instanceof InitiateCodeEditA2CRequest r) {
            if (TCClient.CODE_EDIT_MANAGER.isEditingCode()) {
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
                TCClient.CODE_EDIT_MANAGER.editCode(r.getPlaceTemplates(), r.getBreakTemplates());
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
            int slot = TCClient.MCI.player.getInventory().getFreeSlot();
            if (slot == -1) {
                respond(r, new ErrorResponse(APIErrorCode.NO_SPACE, "Not inventory enough space to give item."));
                return;
            }

            try {
                TCClient.MCI.player.getInventory().setItem(slot, Utils.snbtToItem(r.getSnbt()));
            } catch (Exception e) {
                respond(r, new ErrorResponse(APIErrorCode.INVALID_ITEM_DATA, "Invalid item data: "+e));
            }
        }
        else if (request instanceof GetInventoryA2CRequest r) {
            Inventory inv = TCClient.MCI.player.getInventory();
            HashMap<Integer, GetInventoryC2AResponse.ItemEntry> itemEntries = new HashMap<>();
            for (int slot = 0; slot < inv.getContainerSize(); slot++) {
                ItemStack item = inv.getItem(slot);
                if (item.isEmpty()) continue;
                if (TCClient.ITEM_LIBRARY_MANAGER.getLibraryData(item) != null) continue;
                itemEntries.put(slot, new GetInventoryC2AResponse.ItemEntry(
                    item.getHoverName().getString(),
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
        else if (request instanceof RescanPlotA2CRequest r) {
            if (TCClient.DF_STATE.getMode() != DFState.Mode.DEV) {
                respond(r, new ErrorResponse(APIErrorCode.NOT_IN_DEV, "Plot can only be scanned in dev mode."));
            } else if (TCClient.DF_STATE.isScanning()) {
                respond(r, new ErrorResponse(APIErrorCode.SCAN_IN_PROGRESS, "Plot is already being scanned."));
            } else {
                TCClient.API_SERVER.setRequestAsPending(r, this);
                TCClient.DF_STATE.scanPlot();
            }
        }
    }


    public void sendNotification(String serializedNotification, Permission requiredPermission) {
        TCClient.LOGGER.warn("{} {} [{}]", requiredPermission, hasRequiredPermission(requiredPermission), String.join(", ",permissions.stream().map(Enum::toString).toList()));
        if (!hasRequiredPermission(requiredPermission)) return;
        TCClient.LOGGER.info("sending notif {}",serializedNotification);
        connection.send(serializedNotification);
    }
    public void sendNotification(Notification notification) {
        if (notification.getId() == -1) notification.setId(TCClient.API_SERVER.getNewNotificationId());
        sendNotification(notification.serialize(), notification.getRequiredPermission());
    }
}
