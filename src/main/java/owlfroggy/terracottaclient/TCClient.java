package owlfroggy.terracottaclient;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import net.fabricmc.api.ClientModInitializer;

import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientChunkEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.item.v1.ItemTooltipCallback;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.block.BlockState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.item.ItemStack;
import net.minecraft.network.packet.s2c.play.BlockEntityUpdateS2CPacket;
import net.minecraft.network.packet.s2c.play.ChunkDeltaUpdateS2CPacket;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.Vec3i;
import net.minecraft.world.chunk.WorldChunk;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import owlfroggy.terracottaclient.api.APIServer;
import owlfroggy.terracottaclient.api.message.impl.ModeChangedC2ANotification;
import owlfroggy.terracottaclient.codespace.TemplateIdentifier;
import owlfroggy.terracottaclient.codespace.TemplateType;
import owlfroggy.terracottaclient.gameinterface.*;
import owlfroggy.terracottaclient.itemlibrary.ItemLibraryManager;
import owlfroggy.terracottaclient.itemrenderer.ItemRenderGenerator;

import java.net.InetSocketAddress;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
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
    public static CodeEditManager CODE_EDIT_MANAGER;
    public static ItemLibraryManager ITEM_LIBRARY_MANAGER;
    public static APIServer API_SERVER;

    private static final ArrayList<ChatMessageReceiver> chatMessageReceivers = new ArrayList<>();
    private static final ArrayList<ModeChangeReceiver> modeChangeReceivers = new ArrayList<>();
    private static final ArrayList<TeleportReceiver> teleportReceivers = new ArrayList<>();
    private static final ArrayList<InvChangeReceiver> invChangeReceivers = new ArrayList<>();
    private static final ArrayList<TickEndReceiver> tickEndReceivers = new ArrayList<>();
    private static final ArrayList<ChunkReceiver> chunkReceivers = new ArrayList<>();
    private static final ArrayList<ClientBlockUpdateReceiver> clientBlockUpdateReceivers = new ArrayList<>();
    private static final ArrayList<PlotChangeReceiver> plotChangeReceivers = new ArrayList<>();
    private static final ArrayList<ClientCommandReceiver> clientCommandReceivers = new ArrayList<>();
    private static final ArrayList<TooltipRenderer> tooltipRenderers = new ArrayList<>();

    public static final HashMap<ChunkPos, WorldChunk> loadedChunks = new HashMap<>();
    private static final List<Text> queuedChatMessages = new ArrayList<>();

    private static int ticksUntilTryAPIServer = 0;

    @Override
    public void onInitializeClient() {
        COMMAND_MANAGER = setupManager(new ChatCommandManager());
        DF_STATE = setupManager(new DFState());
        MOVEMENT_MANAGER = setupManager(new MovementManager());
        CODE_EDIT_MANAGER = setupManager(new CodeEditManager());
        ITEM_LIBRARY_MANAGER = setupManager(new ItemLibraryManager());

        WorldRenderEvents.END_MAIN.register(worldRenderContext -> {
            if (MCI.player == null) return;
            // queued chat messages

            Text[] messagesFrozen = queuedChatMessages.toArray(new Text[0]);
            for (Text msg : messagesFrozen) {
                MCI.player.sendMessage(msg,false);
            }
            queuedChatMessages.clear();
        });

        ClientLifecycleEvents.CLIENT_STOPPING.register(client -> {
           if (API_SERVER != null) {
               try {
                   API_SERVER.stop();
               } catch (Exception ignored) {}
           }
        });

        ItemTooltipCallback.EVENT.register((item, context, type, lines) -> {
            for (TooltipRenderer tooltipRenderer : tooltipRenderers) {
                tooltipRenderer.onItemTooltip(item, context, type, lines);
            }
        });

        // This code runs as soon as Minecraft is in a mod-load-ready state.
        // However, some things (like resources) may still be uninitialized.
        // Proceed with mild caution.

        //tcallow
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> {
            dispatcher.register(ClientCommandManager
                .literal("tcallow")
                .then(
                    ClientCommandManager.argument("app_id", IntegerArgumentType.integer())
                    .executes(context -> TCClient.API_SERVER.decideAppAuthentication(context, true))
                ).executes(context -> {
                    context.getSource().sendError(Text.literal("No app id provided."));
                        return 0;
                })
            );
        });
        //tcdeny
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> {
            dispatcher.register(ClientCommandManager
                .literal("tcdeny")
                .then(
                    ClientCommandManager.argument("app_id", IntegerArgumentType.integer())
                    .executes(context -> TCClient.API_SERVER.decideAppAuthentication(context, false))
                ).executes(context -> {
                    context.getSource().sendError(Text.literal("No app id provided."));
                        return 0;
                })
            );
        });



        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> {
            dispatcher.register(ClientCommandManager.literal("terracotta_test").executes(context -> {
//                ItemRenderGenerator.renderToFile( "/tmp/exported/"+Math.random()+".png", TCClient.MCI.player.getMainHandStack(),8);
//                MCI.getNetworkHandler().sendPacket(new CreativeInventoryActionC2SPacket(1, item));
                ItemRenderGenerator.renderToDataURI(TCClient.MCI.player.getMainHandStack(),4, result -> {
                    TCClient.LOGGER.info(result);
                });
                context.getSource().sendFeedback(Text.literal("you edid ait! " + DF_STATE.hasUndergroundCodespace()));
//                MOVEMENT_MANAGER.setMovementDestination(new Vec3d(-6, 255, 0.5));
                return 1;
            }));
        });

        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> {
            dispatcher.register(ClientCommandManager.literal("tcclientdfstate").executes(context -> {
                context.getSource().sendFeedback(Text.literal(
                "=- Internal DF state -="
                + "\nRank: " + DF_STATE.getRank()
                + "\nMode: " + DF_STATE.getMode()
                + "\n\nPlot Name: " + DF_STATE.getPlotName()
                + "\nPlot Id: " + DF_STATE.getPlotId()
                + "\nPlot Origin: " + DF_STATE.getPlotOrigin()
                + "\nPlot Type: " + DF_STATE.getPlotType()
                + "\nHas Underground Codespace: " + DF_STATE.hasUndergroundCodespace()
                ));
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
                    TCClient.CODE_EDIT_MANAGER.editCode(new String[]{
                        "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",
                        "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",
                        "H4sIAAAAAAACA62TTUvEMBCG/8uAoFBhV1gPQTzpUTx4lKVM02mJmyYlTYW19L87aVbs2nZlYU/NTN+ZeZ98dJBpK3cNiPcOVA4ixpAcvgKK1kgO0ZUsYo2nKqi3fQI5ehwUKlVGeYVafVFaa9yTgz5ZaBj/pyi9smamc1wNmVieeixZF6d1ECIBbyqnDB3nbT00EvBsMNN0alJccDH5n/qeORptPYi7+z5AzYkSHupKYhE8UYGt9hfDk7aqR3AGK+LsQ2GNF7yxAh+vPtFd3yab1c3I7GrqVVpHr9nHWWYd1YT+f5dsYWpScaqRtg5rrQwd2QsTxx1MW007rDejmvUx0gsbVzWf5695h3LHXNxGOZIBkIeHPfb7wcSBZpFWotbpwn2+OPD8wU54R2+osq3xKQ9wlFl0+UlyqW0T7vof9G3/DePHGxvSAwAA",
                        "H4sIAAAAAAACA+1XUW/TMBD+K5WlSg0UjRaJh2hMSEOAhOChQntBk+U6l9aaY0eO021M/e+ck5YmWdt5aQsr7Kn25Xzn+77PZ/eOjKXmVxkJf9wREZGwnJP+4jckca44TpmZoBP6WEgW3jgqLG5VqihIdIuYZc6oWAJofk/p93M6uqAD/GZvU2ebMYOTVOaGSRLGTGbQJzq1Qqvfhvm8TzKpLQlfz/t+qSzc2FUSW0x8kwx8k8RatU4y9K4EsQZLmRQTlUD7hG/mlzgugyONgnKdpFphRPETaIFXsaV1rIuYljw9wHvp1CiAumQOKnotIjulUZ6kVM/AGBFBhv4Z10U9ucrYDCKyje61GRY8LONIoaASZFAUzrjDBb9+ENx+ZtkXuK3Uaxi/AhcjEgY4LkIcQa2gVtokm+HJkJ/2+FRQ2V6IJxo74H1f+S3wHtbx/gTWQX7BZI5eWxDnUmfgCzmgtv9R/h6l5nf7g5QzKVE7Pv19Mw47V79o3Bur9wriTUmp1VpTzJcH51B6iXQ+lrDI8Qi4VJ7cD9ZNmJ32upioVwQMXgwDP7Xss8WvuaO8yypvsUZAjkHAtJX9nzv7Ul+DoVMm4/0zedJgshllLKllk0ogNwvJCBUcdb7qyB388jWA1o9Sa7f/NWUvMe0UK7/lyRhMp3pG3taxb/gdBtg8TY8D2HMQuC3nfSzQ1l7hux3TU9ddQ+ybITvrChXBTe9Vv0B6xV8QlF6F2Y2Cs2LoGn1weuIsjbWrQ4VrS4dWLY3jSqPdk7oJUy3CCGxu1P7u0afVSSXE+LB/6n1075rcprY1qn1ZKq/sD0FdjtX7+lmQO/NjxGT6Pyqyrd4elvPxKfJvUNPq38aBcLyc/wLcpTlIahMAAA==",
                        "H4sIAAAAAAACA41Ry0oEMRD8FWkQFObgevCQs+xJvHiUZejJ9CzBvMgk4rLk3+1Mgruo+zhNV091dXVlD4N28mMG8b4HNYKoGLr2FTAlKxli2DKJOZFMY3O1dMqUtz1ppo0YsTQtGuL2rF3kbtz5gmwyDLxOATWICfVMHTgflbM/jZy7OiUecnfdEumMPyxp6Notq7zhuiryrao3LtnYz9IFGhyGERYb/yXjNe4o9CiL9OWImrFf5m8/MdwVK/dw7nRmnTycvS6Xa2XpSGT1R2TQfcTtkU5BAl6TGSjcrF0wWF6rZsU/1uqLxnMH10LAW0nrhdc3jYOJx6cl35NMfjaOjZgJzzRh0jy8yd96PakplgIAAA==",
                        "H4sIAAAAAAACA61UXUvDMBT9L4HBihW26VNREGT4PsQXkRDTVMvSpCTpVEb/uzdJJ+3W1uzjqcntvefcj5O7Re9c0rVGyesW5SlK/B3FzTdBWSUoXIn6ACfwMaxovOHkLDaqFJhxcEuJIdYoSMHA/IDx8yNeveA5/DM/pbVtiIJLyStFOEoywjWLkSxNLsWfoa5jpLk0KJnVcRgVlVxa4IZFVMURLPNQFm2IMqeyLEJZmEhP5bgJ5dhA8Ikct/UbnD0ewlmV4y8FlBhGgHUuDHI59Ikpz7Af/z9y8k69GWsqXco8FwyN6cTXtIcxa4XMXRmE2irh110rbUXomtk5p7liFHyhGUy0+iVVMVylZuaMMps3s3xyj+a8chedcoNCJgUxn9MJpDYFGUbX7uRkH0UttEW3ecvvUgo2NvrzmnKJ2bcqA7joyp1a3e7UtyeO+1FxUC41C1UHJZzDownZqmP68Ds1uCO9ULudOYByqJhelN1OHEA53Hq9KH7nDWAcbrXjVTK2tarRrUWlMEpa6P15dTSyYqZSAgHNLzeTovRWBwAA",
                        "H4sIAAAAAAACA81VUWvCMBD+KyMgKOuD+lgYDLbhy57c8GWMkKVXDcakpKk6pP99l7RorVo7xbGnJpfLd3fffddsyJfUfJ6S8GNDRETCYk+C8huSOFMct8xM0Ql9LCxKb1x5i7uVKAoS3SJmmTMqtgA0P1L6/kTHEzrAM/udONuSGdwkMjNMkjBmMoWA6MQKrbaGPA9IKrUlYT8P2oXS8S6EXdtfhBi0DRFr5XAvCjLMP3FdACKngnKdKUtXIrIz4hM4Rn4KlhZ8neG/cKrlW4BjBlz7jKVQQJqoVdniEKVfuTLwVTDuisSjh1tlXurmZeSF0zr/o1heGCcgavW8WSPU9BXUtKkpBhJgrveXVCau7sdgL/9WFLSis1Dolou7MVNTuKvQYBifg6s7EgY43kC1g9oNRMnLzSUxvFoS1A3gKqIdPO26qe5VIDOVsiVEjSwXs19D7QgVwbqr48Djil7vNLsjsM+C2wmTGeb/n8e/s2B21vUVecDevV9X2rFXZ8Mf4lA/XOoU/lhAxVt0nX7OMNv+L8lRe0a7p6Ze0R7CGGxmFEHYH5s2486wBwAA",
                        "H4sIAAAAAAACA+1Y207jMBD9lZVXu1qkPjTtFkGkrVZcxCMIEC8IWa5xioVjR47DVfl3xkmAtmmDAw0EKX1pPJ6L5xzPOPEjmghFr2Pknz8ifon8fIx6xb+PgkRSGBI9BSXQMSwstOEpk1irSGImQO2SGGKFkoQMxP8xPt3Fx2fYgzlzH1nZDdEwiESiiUB+QETMekhFhiv5IkjTHoqFMsjvpz23UFQJZR0XUWQS1ojipRfwnHtEOEg4vmJ3/IFh8IqyFSyDJmYG59m8gU6utByb/YMMnJiqbN2CS4aq0l/q6zn5FV68kpccngUvg78zNoOSzURgQ6YzZnbkox1ufhxpRnkM2MJsDjJM7LGAJMIsB4wWSmB9y+PZxQ5GrpEPI6aJyRJ/CToej2sH3MzYL2s0w3sFT45sO+0cV85Ho+9P+u+WU/5K2LAlpe5tdqXePO82j3WVesXO6Uq9jaU+akmpb3WV3jztkzVWesXG6Sq9PZSTtry1d2R/Zlvf+jDttSiv+15R5r9uvxrOA3t8sLOb7dPmP4P7H4a2oiYdq6lfieWbJl8G3vY6vyoriHBtSvVhHK6G8VTz8MRoLqdNAWmvuJzhM3e2Oy54+AV+/8y0iY0FwfbGHIpz+f1bnRYPvjSrn/3i57Z4Teg1y9xwOFpAFzo8s6fL862c0mHDtZDfNa4na+9dlFEljYZ7w3Ia872BmUTLSvCoUHCktRi9uruuMRAv0ieX8qSQyhYAAA==",
                        "H4sIAAAAAAACA+1abW/bNhD+K4aAAjWWD7XTZoOQBQu8thvQdUAQdB+GQqAlxhJCkQJFNUmL/LXmJ/Uv9EjJDmVLMvVmqS+fbFHUHe+5493Dkz5ZS8Lc69iy//9kBZ5lp9fWUfZrW1cJdeES8RVMgjkCh9ls+KdG5FMRdTCBaR4SSA5SFGIY/sNxLhfOxTtnBvfEXSTHPiAOFxFJOCKWfYVIjI8sFomA0c3A/f2RFRMmLPvZ/ZGZqhVnSfS396hH3IoaemametTlRgkJ4jpa5vfv4X8qEZANHBTHOFwSbCn1RfjHWDgpZHtckE4qQsUJqIdv4WbssmzVFDRWYFwoy5ELzgR6jmDr/1vCExqjD9jT5O9iW75W5cGSdaboIVeCC3dfY/Fn4Ip3iCS94eeyMKoDXBp0W0JyUORM+L2vdQdhxLjAnuNj5GEeV9mQW9GCYyTwGxnWfS2tDp40CXclzAYAlGDaegNlqaNEyJYl0gVvMF0Jv9wkjiPw1iH8UB+TLXP+8wPIcnAzWZ6vx041yzhyr7HaOwHHLjwPSVSJzxJtZmqP29xRmvr2sZGQCtfs5kAZKL3mwKyIv3ztnBwcnsL9/yREwn/6BBQ8Daa/zKfDwhOx2GlfXI1A3sVnSRyBVpo4eWVbFyyh3uQf5smATtkIjL4ijEnVBXavAZuoJ98m4RLziY7rSR7YrXl9B95vYwy84xEE3l2HgVcB8g8WeC6TxL8dSXnWiKS4iBDg2SZHri5WXl+G4U6Z6xxtbvbM8Vx75thsrXuS73P9vKXOLzcchDpgohMHtIJPfC+OmOlxaOiInPNqOKIiGTV3RHB1aF5XeIaLI7lesz29j8tSxsO+Ule6Tjigj7FizgaumGtwZMcCV56JazRElHFaK2TqbHwgmyS6zi66JAYObtQnaXWWLDGyGaymXjImJeeEsJuJjK7Jwkd0pYRuyMklB1iKcFjj94rxl8iFY3gpKdnM6Ow427b8CD+IZXIdRw2qX4JyZcuwBJkFYVUlSnqlBD6+7cQl+32b1ibdRtAdfFRG9n9Ym73oPbPKK5VcfQyLbpJb91UrXdPwhcv1EWQUgeWtnpB9RFICKxV2jKgeICNAtIvGvrJLSpqeXjEqbMDWds9OYx957MZWd7NNPz1L5679OP3y8LkpmXQJi2XFOkhntH23Pk8Bn+t+b2N2NYnG8NptJIw83WYyPn6y8tKtCC8HvM4wKstxSn6I9H3f7mXl/hUP8MoSkWBFQwxMZoSxltv+g8QaYXyk+/DFwNgIfCugVlEx0tg5aY1Pq15SnVfqhkcr07xnfNJd+Ni93mm//4XiyTm9m6ToFMGhv3RewGpQQOOK825uGowLwYNlAu0x23r77+VI6u6g/srBdR6B0d746WxKYjekVrd1evbl4QE46yPjPQiN6yTZd/jx0y6nWBcUdWgq1NeYYuyvVY3YxXfzZuMwXSWjIPr2m0o99JPamvbYMJi37yiZkRvjnWlaCXYDsylnP97+dFQuglF4RjpCmvettErK2yO6xw0LzKibHb/2VCUH7gnlvupuz6mMeVTVdxOwEblKYdsG5SRcYJFwaoHYr7l6zWn5LgAA",
                        "H4sIAAAAAAACA+2X32vbMBDH/5Vx0NGAC03q9kF0MLp2e9tGKXtZh1AcpROTJSPJ7Ubm/e07SUnmNG7rOGTF0Kfox919daePJGcGY6mzHxbI1xmICZDYh2T+S2Baqgy7zNygEdo4ns+tsRVGvFehKJdoNmGO+UHFco7Dbym9ekcvv9AhzrlfhR+7ZQY7hSwNk0CmTFqegC6c0Go5UFUJWKkdkMMqaSeVaal94LmKKvMNVIZtVaxjxnVVGbVV4WrSVeOorcYtOnfUSKtv2I7xgE5LQe8MSlLcAloK5SCsoQkmyx2N+/8ET9GomaaLDwEnm+mwdikUh8eAicndizWquazvfqPLXs7c9/09XNo+btDgILQCEINBLdooVIdlvnjodfGz0Irvrig5sz7YdtWopVYr8uBgWE9svUyNC1ockQdWtH4IxpI6dlML5HsEzoR79dnwTFhfyAWOOHHOp6yUXqKhgouyo/edsCvCx22VPxXcMBduk6Xo6enGeierICwtdn04jjbBoQNT6xyEzr8ocQ5mmS6VI8Nxcv6efjy7ImmaHiZiQq6BCXMNSabzeDYsmVVV/xn50xtE0q0RWTy3rRnpDm1PaXjdGxqOt6Yhfkxsx8LLu/HMGJz893ejO5cvd8JOYHjiVm+JQat3ZmMaHsGzpzT87sfVEP+97/hzYSWvNw9nlGnljPavzf2MViJcclcaBRj2L8Tp2nLiEAAA",
                    },new TemplateIdentifier[]{});
//                    TCClient.CODESPACE_MANAGER.editCode(new String[]{},new TemplateIdentifier[]{
//                        new TemplateIdentifier(TemplateType.FUNCTION,"omg"),
//                        new TemplateIdentifier(TemplateType.FUNCTION,"globalPlrDeath"),
//                    });
                } catch (Exception e) {
                    context.getSource().sendFeedback(Text.empty().formatted(Formatting.RED).append("ERROR: "+e.getMessage()));
                }
                return 1;
            }));
        });

        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> {
            dispatcher.register(ClientCommandManager.literal("testbreak").executes(context -> {
//                context.getSource().sendFeedback(Text.literal(
//                    TemplateDataUtils.getIdentifier(TemplateDataUtils.parseTemplateData(TEST_TEMPLATE_DATA)).toString()
//                ));
                try {
                    TCClient.CODE_EDIT_MANAGER.editCode(new String[]{},new TemplateIdentifier[]{
                        new TemplateIdentifier(TemplateType.FUNCTION, "fui_data"),
                        new TemplateIdentifier(TemplateType.FUNCTION, "fui_initialize_player"),
                        new TemplateIdentifier(TemplateType.FUNCTION, "fui_componentize_text"),
                        new TemplateIdentifier(TemplateType.FUNCTION, "fui_mount_scoreboard"),
                        new TemplateIdentifier(TemplateType.FUNCTION, "_fui_write_col_sint"),
                        new TemplateIdentifier(TemplateType.FUNCTION, "fui_count_width"),
                        new TemplateIdentifier(TemplateType.FUNCTION, "_fui_hexize_col"),
                        new TemplateIdentifier(TemplateType.FUNCTION, "fui_assemble"),
                        new TemplateIdentifier(TemplateType.FUNCTION, "_fui_write_col_uint"),
                        new TemplateIdentifier(TemplateType.PLAYER_EVENT, "Join")
                    });
                } catch (Exception e) {
                    context.getSource().sendFeedback(Text.empty().formatted(Formatting.RED).append("ERROR: "+e.getMessage()));
                }
                return 1;
            }));
        });

        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> {
            dispatcher.register(ClientCommandManager.literal("dumptemplatecache").executes(context -> {
                context.getSource().sendFeedback(Text.literal("BY LOCATION -----------"));
                context.getSource().sendFeedback(Text.literal(DF_STATE.templatesByLocation.toString()));
                context.getSource().sendFeedback(Text.literal("BY NAME-----------"));
                context.getSource().sendFeedback(Text.literal(DF_STATE.templatesByName.toString()));
                context.getSource().sendFeedback(Text.literal("BY FLOOR-----------"));
                context.getSource().sendFeedback(Text.literal(DF_STATE.floors.toString()));

                int numFunctions = 0;
                for (String name : DF_STATE.templatesByName.get(TemplateType.FUNCTION).keySet()) {
                    numFunctions += DF_STATE.templatesByName.get(TemplateType.FUNCTION).get(name).size();
                }

                int numEvents = 0;
                for (String name : DF_STATE.templatesByName.get(TemplateType.PLAYER_EVENT).keySet()) {
                    numEvents += DF_STATE.templatesByName.get(TemplateType.PLAYER_EVENT).get(name).size();
                }
                for (String name : DF_STATE.templatesByName.get(TemplateType.ENTITY_EVENT).keySet()) {
                    numEvents += DF_STATE.templatesByName.get(TemplateType.ENTITY_EVENT).get(name).size();
                }
                for (String name : DF_STATE.templatesByName.get(TemplateType.GAME_EVENT).keySet()) {
                    numEvents += DF_STATE.templatesByName.get(TemplateType.GAME_EVENT).get(name).size();
                }

                int numProcesses = 0;
                for (String name : DF_STATE.templatesByName.get(TemplateType.PROCESS).keySet()) {
                    numProcesses += DF_STATE.templatesByName.get(TemplateType.PROCESS).get(name).size();
                }

                context.getSource().sendFeedback(Text.literal("\n#EVENTS = " + numEvents));
                context.getSource().sendFeedback(Text.literal("#FUNCTIONS = " + numFunctions));
                context.getSource().sendFeedback(Text.literal("#PROCESSES = " + numProcesses));
                return 1;
            }));
        });

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            // tick receivers
            for (TickEndReceiver receiver : tickEndReceivers) {
                try {
                    receiver.onTickEnd(client);
                } catch (Exception e) {
                    TCClient.LOGGER.error("Error while ticking " + receiver,e);
                }
            }

            // start API server
            ticksUntilTryAPIServer--;
            if (TCClient.MCI.world != null && TCClient.MCI.player != null) {
                if (API_SERVER == null && ticksUntilTryAPIServer <= 0) {
                    ticksUntilTryAPIServer = 100;
                    try {
                        API_SERVER = new APIServer(new InetSocketAddress("localhost", 39893));
                        Thread apiServerThread = new Thread(() -> {
                            try {
                                API_SERVER.run();
                            } catch (Exception e) {
                                TCClient.LOGGER.error("Failed to start API server",e);
                                API_SERVER = null;
                            }
                        });
                        apiServerThread.setName("TCClient API");
                        apiServerThread.start();
                    } catch (Exception e) {
                        TCClient.LOGGER.error("Failed to start API server",e);
                        API_SERVER = null;
                    }
                }
            } else {
                if (API_SERVER != null && API_SERVER.isOpen()) {
                    try {
                        API_SERVER.stop();
                    } catch (Exception ignored) {}
                }
            }
        });
        ClientChunkEvents.CHUNK_LOAD.register((clientWorld, worldChunk) -> {
//            loadedChunks.put(worldChunk.getPos(),worldChunk);
//            TCClient.fireChunkLoadReceivers(worldChunk.getPos());
        });
        ClientChunkEvents.CHUNK_UNLOAD.register((clientWorld, worldChunk) -> {
            loadedChunks.remove(worldChunk.getPos());
        });
        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> {
            loadedChunks.clear();
        });
    }

    public static boolean isChunkLoaded(ChunkPos chunkPos) {
//        return loadedChunks.containsKey(chunkPos);
        return MCI.world.isPosLoaded(new BlockPos(
            chunkPos.x*16,
            0,
            chunkPos.z*16
        ));
    }
    public static boolean isChunkLoaded(BlockPos blockPos) {
        return isChunkLoaded(new ChunkPos(blockPos));
    }
    public static boolean isChunkLoaded(Vec3i iPos) {
        return isChunkLoaded(new BlockPos(iPos));
    }
    public static boolean isChunkLoaded(Vec3d pos) {
        return isChunkLoaded(new Vec3i((int)pos.x,(int)pos.y,(int)pos.z));
    }

    public static Path getConfigPath() {
        Path p = FabricLoader.getInstance().getConfigDir().resolve("tcclient");
        boolean exists = Files.exists(p);
        if (!Files.isDirectory(p) && exists) throw new RuntimeException("Terracotta config path is not accessible (is not a directory)");
        try {
            if (!exists)
                Files.createDirectories(p);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        return p;
    }

    public static void fireModeChangeReceivers(DFState.Mode newMode) {
        safeMessage(Text.literal("mode change detected :D (" + newMode.toString() + ")"));
        APIServer.broadcastNotification(new ModeChangedC2ANotification(newMode));
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

    public static void fireClientBlockUpdateReceivers(BlockPos pos, BlockState state) {
        for (ClientBlockUpdateReceiver receiver : clientBlockUpdateReceivers) {
            receiver.onClientBlockUpdate(pos, state);
        }
    }

    public static void firePlotChangeReceivers(int plotId, DFState.Mode mode) {
        for (PlotChangeReceiver receiver : plotChangeReceivers) {
            receiver.onPlotChanged(plotId, mode);
        }
    }
    public static void fireClientCommandReceivers(String command) {
        for (ClientCommandReceiver receiver : clientCommandReceivers) {
            receiver.onClientSendCommand(command);
        }
    }

    /**
     * Schedules a chat message to be sent at the end of the frame;
     * ensures that the message is always sent from the render thread.
     * Messages will be sent in the order they are queued.
     * @param text The message to send.
     */
    public static void safeMessage(Text text) {
        queuedChatMessages.add(text);
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
        if (manager instanceof ClientBlockUpdateReceiver clientBlockUpdateReceiver)
            clientBlockUpdateReceivers.add(clientBlockUpdateReceiver);
        if (manager instanceof PlotChangeReceiver plotChangeReceiver)
            plotChangeReceivers.add(plotChangeReceiver);
        if (manager instanceof ClientCommandReceiver clientCommandReceiver)
            clientCommandReceivers.add(clientCommandReceiver);
        if (manager instanceof TooltipRenderer tooltipRenderer)
            tooltipRenderers.add(tooltipRenderer);

        return manager;
    }
}