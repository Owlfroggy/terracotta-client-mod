package owlfroggy.terracottaclient;

import net.fabricmc.api.ClientModInitializer;

import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientChunkEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.item.ItemStack;
import net.minecraft.network.packet.s2c.play.BlockEntityUpdateS2CPacket;
import net.minecraft.network.packet.s2c.play.ChunkDeltaUpdateS2CPacket;
import net.minecraft.text.Text;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.ChunkRegion;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.WorldChunk;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import owlfroggy.terracottaclient.codespacemanager.TemplateType;
import owlfroggy.terracottaclient.gameinterface.*;

import java.util.ArrayList;
import java.util.HashMap;

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
    public static CodespaceManager CODESPACE_MANAGER;

    private static final ArrayList<ChatMessageReceiver> chatMessageReceivers = new ArrayList<>();
    private static final ArrayList<ModeChangeReceiver> modeChangeReceivers = new ArrayList<>();
    private static final ArrayList<TeleportReceiver> teleportReceivers = new ArrayList<>();
    private static final ArrayList<InvChangeReceiver> invChangeReceivers = new ArrayList<>();
    private static final ArrayList<TickEndReceiver> tickEndReceivers = new ArrayList<>();
    private static final ArrayList<ChunkReceiver> chunkReceivers = new ArrayList<>();

    public static final HashMap<ChunkPos, WorldChunk> loadedChunks = new HashMap<>();

    @Override
    public void onInitializeClient() {
        COMMAND_MANAGER = setupManager(new ChatCommandManager());
        DF_STATE = setupManager(new DFState());
        MOVEMENT_MANAGER = setupManager(new MovementManager());
        CODESPACE_MANAGER = setupManager(new CodespaceManager());

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
                DF_STATE.scanPlot();
                return 1;
            }));
        });

        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> {
            dispatcher.register(ClientCommandManager.literal("dumptemplatecache").executes(context -> {
                context.getSource().sendFeedback(Text.literal("BY LOCATION -----------"));
                context.getSource().sendFeedback(Text.literal(CODESPACE_MANAGER.templatesByLocation.toString()));
                context.getSource().sendFeedback(Text.literal("BY NAME-----------"));
                context.getSource().sendFeedback(Text.literal(CODESPACE_MANAGER.templatesByName.toString()));
                context.getSource().sendFeedback(Text.literal("BY FLOOR-----------"));
                context.getSource().sendFeedback(Text.literal(CODESPACE_MANAGER.floors.toString()));

                int numFunctions = 0;
                for (String name : CODESPACE_MANAGER.templatesByName.get(TemplateType.FUNCTION).keySet()) {
                    numFunctions += CODESPACE_MANAGER.templatesByName.get(TemplateType.FUNCTION).get(name).size();
                }

                int numEvents = 0;
                for (String name : CODESPACE_MANAGER.templatesByName.get(TemplateType.PLAYER_EVENT).keySet()) {
                    numEvents += CODESPACE_MANAGER.templatesByName.get(TemplateType.PLAYER_EVENT).get(name).size();
                }
                for (String name : CODESPACE_MANAGER.templatesByName.get(TemplateType.ENTITY_EVENT).keySet()) {
                    numEvents += CODESPACE_MANAGER.templatesByName.get(TemplateType.ENTITY_EVENT).get(name).size();
                }

                int numProcesses = 0;
                for (String name : CODESPACE_MANAGER.templatesByName.get(TemplateType.PROCESS).keySet()) {
                    numProcesses += CODESPACE_MANAGER.templatesByName.get(TemplateType.PROCESS).get(name).size();
                }

                context.getSource().sendFeedback(Text.literal("\n#EVENTS = " + numEvents));
                context.getSource().sendFeedback(Text.literal("#FUNCTIONS = " + numFunctions));
                context.getSource().sendFeedback(Text.literal("#PROCESSES = " + numProcesses));
                return 1;
            }));
        });

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            for (TickEndReceiver receiver : tickEndReceivers) {
                receiver.onTickEnd(client);
            }
        });
        ClientChunkEvents.CHUNK_LOAD.register((clientWorld, worldChunk) -> {
            loadedChunks.put(worldChunk.getPos(),worldChunk);
        });
        ClientChunkEvents.CHUNK_UNLOAD.register((clientWorld, worldChunk) -> {
            loadedChunks.remove(worldChunk.getPos());
        });
        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> {
            loadedChunks.clear();
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

    public static void fireChunkLoadReceivers(ChunkPos chunkPos) {
        for (ChunkReceiver receiver : chunkReceivers) {
            receiver.onChunkLoad(chunkPos);
        }
    };

    public static void fireChunkDeltaReceivers(ChunkDeltaUpdateS2CPacket packet) {
        for (ChunkReceiver receiver : chunkReceivers) {
            receiver.onChunkDelta(packet);
        }
    }

    public static void fireBlockEntityUpdateReceivers(BlockEntityUpdateS2CPacket packet) {
        for (ChunkReceiver receiver : chunkReceivers) {
            receiver.onBlockEntityUpdate(packet);
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
        if (manager instanceof ChunkReceiver chunkReceiver)
            chunkReceivers.add(chunkReceiver);

        return manager;
    }
}