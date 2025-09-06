package owlfroggy.terracottaclient;

import net.fabricmc.api.ClientModInitializer;

import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;
import net.minecraft.util.math.Vec3d;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import owlfroggy.terracottaclient.gameinterface.*;

import java.util.ArrayList;

public class TCClient implements ClientModInitializer {
    public static final String MOD_ID = "terracotta-client";

    // This logger is used to write text to the console and the log file.
    // It is considered best practice to use your mod id as the logger's name.
    // That way, it's clear which mod wrote info, warnings, and errors.
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);
    public static final MinecraftClient MCI = MinecraftClient.getInstance();
    public static ChatCommandManager COMMAND_MANAGER;
    public static DFState DF_STATE;
    public static MovementManager MOVEMENT_MANAGER;

    private static final ArrayList<ChatMessageReceiver> chatMessageReceivers = new ArrayList<>();
    private static final ArrayList<ModeChangeReceiver> modeChangeReceivers = new ArrayList<>();
    private static final ArrayList<TeleportReceiver> teleportReceivers = new ArrayList<>();
    private static final ArrayList<InvChangeReceiver> invChangeReceivers = new ArrayList<>();
    private static final ArrayList<TickEndReceiver> tickEndReceivers = new ArrayList<>();

    @Override
    public void onInitializeClient() {
        COMMAND_MANAGER = setupManager(new ChatCommandManager());
        DF_STATE = setupManager(new DFState());
        MOVEMENT_MANAGER = setupManager(new MovementManager());

        // This code runs as soon as Minecraft is in a mod-load-ready state.
        // However, some things (like resources) may still be uninitialized.
        // Proceed with mild caution.
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> {
            dispatcher.register(ClientCommandManager.literal("terracotta_test").executes(context -> {
                context.getSource().sendFeedback(Text.literal("you edid ait!"));
                MOVEMENT_MANAGER.setMovementDestination(new Vec3d(-6, 255, 0.5));
                return 1;
            }));
        });

        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> {
            dispatcher.register(ClientCommandManager.literal("rescanplot").executes(context -> {
                context.getSource().sendFeedback(Text.literal("Rescanning plot..."));
                DF_STATE.scanPlot();
                return 1;
            }));
        });

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            for (TickEndReceiver receiver : tickEndReceivers) {
                receiver.onTickEnd(client);
            }
        });
    }

    public static void fireModeChangeReceivers(DFState.Mode newMode) {
        TCClient.MCI.player.sendMessage(Text.literal("mode change detected :D (" + newMode.toString() + ")"), false);
        for (ModeChangeReceiver receiver : modeChangeReceivers) {
            receiver.onModeChanged(newMode);
        }
    }

    public static void fireTeleportReceivers(Vec3d newPos, Vec3d oldPos) {
        for (TeleportReceiver receiver : teleportReceivers) {
            receiver.onTeleported(newPos, oldPos);
        }
    }

    public static void fireInvChangeReceivers(int slot, ItemStack newItem) {
        for (InvChangeReceiver receiver : invChangeReceivers) {
            receiver.onSlotChanged(slot, newItem);
        }
    }

    public static void fireChatMessageReceivers(Text message) {
        for (ChatMessageReceiver receiver : chatMessageReceivers) {
            receiver.onChatMessage(message);
        }
    }

    private <T extends Manager> T setupManager(T manager) {
        if (manager instanceof ChatMessageReceiver chatMessageReceiver)
            chatMessageReceivers.add(chatMessageReceiver);
        if (manager instanceof ModeChangeReceiver modeChangeReceiver)
            modeChangeReceivers.add(modeChangeReceiver);
        if (manager instanceof TeleportReceiver teleportReceiver)
            teleportReceivers.add(teleportReceiver);
        if (manager instanceof InvChangeReceiver invChangeReceiver)
            invChangeReceivers.add(invChangeReceiver);
        if (manager instanceof TickEndReceiver tickEndReceiver)
            tickEndReceivers.add(tickEndReceiver);

        return manager;
    }
}