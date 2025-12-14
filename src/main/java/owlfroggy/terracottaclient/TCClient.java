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
import net.minecraft.util.Formatting;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.ChunkRegion;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.WorldChunk;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import owlfroggy.terracottaclient.codespacemanager.TemplateDataUtils;
import owlfroggy.terracottaclient.codespacemanager.TemplateIdentifier;
import owlfroggy.terracottaclient.codespacemanager.TemplateType;
import owlfroggy.terracottaclient.gameinterface.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.ThreadLocalRandom;

public class TCClient implements ClientModInitializer {
    public static final String MOD_ID = "terracotta-client";
    public static final String TEST_TEMPLATE_DATA = "H4sIAAAAAAAA/6WSy2rDMBBFf0XM2otS2i60C5TSLrxpQjelBFmaKiKyZKQxxAT/e+VHjQgpxWSlx9w7OnfQGSrr5TEC/zyDUcCnMxTzyuG7dTIdRdBJlDSE9axOu/FmcB2Mo6RSgsRyNzjJeAd9X0C0noDfP/bFhbOyexI68/pmNHF4ETZiKgxlDm+RvRql0A0wcpaozonayEvc7MGn/qv/7Q2+1jACXAvaWNFh2M+9/01MpzxwwsCph0TFqo4RhiCkJxKMP2dAdysGsAttlt8dMBhiW+osxnwIW3SqxBiFxj/jZBN5WEGwUYrFJmWKC8cOT8Q+hG2RlRi0cfoGljXf4R11a0VYQDbWaFejI1Z6hTdATF/kqjtVfgAUxuH8IgMAAA==";
    public static final String TEST_TEMPLATE_DATA_2 = "H4sIAAAAAAAA/81YzW4bNxB+FXqBIimgCrLj2KmANlDkBA6atIbtOChqQ+AuqV1WXFIhuYoVQ0BPufbv0PSU9pQX6CXtzce+Rf0EeYQOuZJM2VplJSs/gGDvcmeGM9/8kidByGXU0UH9u5OAkaCevweV4f960M5EBK9YxUAENIamQ2p4ciuWy71UAoINHlHB6snWvdbXd/br6+s3blUimXaloMLo+slhkDJBI4Xbph5l2si0JXBKD4GXHhuFQX4oOanXQmDjUtUPA0XJYVBhBnMW2XUZtjMdYUMdlTaKdahJlMzixC4YkANcOxz3qUJbFJsE2DNBqOKwtWUaHI2oDoPBADbKhKmvVhip++rpDuXUSNHSnYxzoLS0gebSBPXaoHIBh5C3DI49JGTXMCngyz3MNYUP9nM9uK/RNiOECgttNCQhfQCBRRfBP99vbWNwNBjJDmIuQ8x3uHLWBU6XaR7UYEBkWjL8/u1+NMfGU976BBa7DsSZdk/l+6THwLTUY1x1+o8NvisMM4zqphQE2HUWNkaftrFuusDYB7wKTaNWQL81FHjZuonddmkqe7kLVExBm2DPIWO/LgW8Yucf5ECMvH+3B4qj/VwNHw+7Pl6eosKlUJjKWmgMa7d6WL3dkJzoojdzPShBhGLnrkh27YdMaNyjZGZ8iCy9LLFWHBlfeEYoHHUcIIQpUACWYF/rbdN3+wup0lkONB+V0avljF6qFcQWiAcy8vQHuZjP1D6GDbkna4g2iMHDbJuWRwvYlpeX4iRerl61Sb22mLb7P2BxYgQTsc/e4DzvH3ppynex8gulfWURt/o3FZRC8An0BUsRcah+UHSBCKe2MwGitVolSKRiz6SANgimVNduVoIeVY4RCKq1wbnsY8tRBZb+6OHZ6CGVVtsDrFgOmRU9eH+xsDOy+t1irV1TGUPNTJQ4jAAyyYdPGqC1tMM5oWEMFBt0feSMT99lhqzdnL9MXiz7Vu89a8MefTIvnDGIvxqYa2Mw13wwbTvPmLFTF0jvwYxWnH9NLCLKXfda3PETEve6gDQ2Uj2UZN5uP2eETZ16Tl8dn75qu19t/Dt78Qc6fRWd/gPri8RUPjG4QQ89pFrjeMK0LdrGGTcTKVd+NG0QgnQXR1SPJ5R9GIzRAeYZhe1UnBfGc4ipIOdaTAdvZpwX67JL44y7HpYr0uAsFqk1fujORZW4kDiT3HPlzXJ68bDxvN+WXKreVDd83Obw3bdjrzWlVIQJm/k+5glrm29E45jpKXDO8JXHt6zUXbpvCmpBBJlf3AmHG23DGTdWOH231Wqqs2/WFjzP7qvs/DhrK4iRqJkpZVP1gIKGcDDznf8Aw1k2edRdIFc91vmOblfJUziv0bkCYDq8H2DatyGlk/PiduUY3pSyPbOMFPHBDMBxHMPpiSH9lGK1MrOyFIl5jGEUJlWP90Zp3uvMXNPIQLD6s9x6af7txlbzq72VlVVf9csNrYg9xf2Qor7MoPtDW6PwbxMjIWXoidsoLW5LimsGPZVK9avoPmpLaM09J99HZ7O0vIYgoEwoSR+F0iRUUZfHCeXd2yu3fZNvlZZ59vxn9/vV/X46e/5L/vff3//76wdP4uelJe4zfQ2FmUEY6UhhGD19zVbLx/FEFJcPY5Ngo1GHAViyjXSmUaikL6p8KN+hBk51CCQKROFJwIFPd/r2utEXWD6+z168fvPytz/Rm5c/vj578Tes+HLWJ0vOLhZEpm6sKy4+inahDX7QqjnjYuhxwtzB0b8r/HKOy6KhdYXWg768VXDn7d29gsNYC9c+7uZTq864bPpsRv+JpDBK8gVHislt55kp9ijsTLxTCMCAHgk2cUn6GDMzRdfi8cExzIqQiMv8an6pITJ5Pb82jwb5jebR4H/pHO5LnhkAAA==";
    public static final int INSTANCE_ID = ThreadLocalRandom.current().nextInt();

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
    private static final ArrayList<PlotChangeReceiver> plotChangeReceivers = new ArrayList<>();

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
            dispatcher.register(ClientCommandManager.literal("testplace").executes(context -> {
//                context.getSource().sendFeedback(Text.literal(
//                    TemplateDataUtils.getIdentifier(TemplateDataUtils.parseTemplateData(TEST_TEMPLATE_DATA)).toString()
//                ));
                try {
                    TCClient.CODESPACE_MANAGER.editCode(new String[]{
                        TEST_TEMPLATE_DATA,
                        TEST_TEMPLATE_DATA_2
                    },new TemplateIdentifier[]{});
                } catch (Exception e) {
                    context.getSource().sendFeedback(Text.empty().formatted(Formatting.RED).append("ERROR: "+e.getMessage()));
                }
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

    public static void firePlotChangeReceivers(int plotId, DFState.Mode mode) {
        for (PlotChangeReceiver receiver : plotChangeReceivers) {
            receiver.onPlotChanged(plotId, mode);
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
        if (manager instanceof PlotChangeReceiver plotChangeReceiver)
            plotChangeReceivers.add(plotChangeReceiver);

        return manager;
    }
}