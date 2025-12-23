package owlfroggy.terracottaclient;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.scoreboard.Team;
import net.minecraft.text.ClickEvent;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.Vec3i;
import owlfroggy.terracottaclient.gameinterface.*;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

//    public Vec3d PlotOrigin = new Vec3d(5100,0,4675);
public class DFState extends Manager
implements
    ChatMessageReceiver,
    InvChangeReceiver,
    TeleportReceiver,
    ClientCommandReceiver,
    TickEndReceiver
{
    public enum Mode {
        SPAWN,
        DEV,
        PLAY,
        BUILD,
    }
    public enum Rank {
        NON,
        NOBLE,
        EMPEROR,
        MYTHIC,
        OVERLORD
    }
    public enum PlotType {
        UNKNOWN,
        BASIC,
        LARGE,
        MASSIVE,
        MEGA
    }
    public enum CodespaceCorner {
        FRONT_LEFT,
        FRONT_RIGHT,
        BACK_LEFT,
        BACK_RIGHT
    }

    public static final HashMap<PlotType, Integer> CODESPACE_Z_SIZES = new HashMap<>(Map.of(
        PlotType.BASIC, 51,
        PlotType.LARGE, 101,
        PlotType.MASSIVE, 301,
        PlotType.MEGA, 300
    ));public static final HashMap<PlotType, Integer> CODESPACE_X_SIZES = new HashMap<>(Map.of(
        PlotType.BASIC, 20,
        PlotType.LARGE, 20,
        PlotType.MASSIVE, 20,
        PlotType.MEGA, 300
    ));
    private static final Pattern MODE_REGEX = Pattern.compile("You are currently (?:at )?(\\w+)");
    private static final Pattern PLOT_REGEX = Pattern.compile("^→ (.+) \\[(\\d+)");
    private static final double TP_MAGIC_Y_VALUE = 52.15763;
    private static final double TP_MAGIC_Y_VALUE_UNDERGROUND = TP_MAGIC_Y_VALUE - 12;
    private static final Text OUT_OF_BOUNDS_TEXT = (
        Text.empty()
        .append(Text.literal("Error: ").formatted(Formatting.RED))
        .append(Text.literal("That location is out of bounds!").formatted(Formatting.GRAY))
    );
    private static final Text FUNC_RENAME_HINT_TEXT = (
        Text.empty()
        .append(Text.literal("Right click the sign while holding a ").formatted(Formatting.YELLOW))
        .append(Text.literal("String").formatted(Formatting.AQUA))
        .append(Text.literal(" to name the Function.\n").formatted(Formatting.YELLOW))
        .append(Text.literal("You can also use String on ").formatted(Formatting.YELLOW))
        .append(Text.literal("Call Function").withColor(0xFF55AA))
        .append(Text.literal(" blocks!").formatted(Formatting.YELLOW))
    );
    private static final Text PREFERENCES_ITEM_NAME = (
        Text.empty().setStyle(Style.EMPTY.withItalic(false))
        .append(Text.literal("◇ ").withColor(0x7F7F2A))
        .append(Text.literal("Preferences").formatted(Formatting.YELLOW))
        .append(Text.literal(" ◇").withColor(0x7F7F2A))
    );
    public static final Text PREFERENCES_ITEM_TOOLTIP = (
        Text.empty().formatted(Formatting.DARK_PURPLE,Formatting.ITALIC)
        .append(
            Text.literal("Edit your preferences here.").setStyle(
                Style.EMPTY
                .withColor(Formatting.GRAY)
                .withItalic(false)
                .withBold(false)
                .withUnderline(false)
                .withStrikethrough(false)
                .withObfuscated(false)
            )
        )
    );

    public boolean modeRefreshQueued = false;
    public boolean plotScanActive = false;

    private Rank rank = null;
    public Rank getRank() { return rank != null ? rank : Rank.NON; }

    private Vec3d plotOrigin;
    public Vec3d getPlotOrigin() {return plotOrigin;}
    public Vec3i getIntPlotOrigin() {return new Vec3i((int)plotOrigin.x, (int)plotOrigin.y, (int)plotOrigin.z);}

    private Mode mode = Mode.SPAWN;
    public Mode getMode() {return mode;}

    private String plotName = "Spawn";
    public String getPlotName() {return plotName;}

    private int plotId = -1;
    public int getPlotId() {return plotId;}

    private PlotType plotType = PlotType.UNKNOWN;
    public PlotType getPlotType() {return plotType;};

    private boolean doesHaveUndergroundCodespace = false;
    public boolean hasUndergroundCodespace() { return doesHaveUndergroundCodespace; }

    private int totalCodespaceChunks = -1;
    public int getTotalCodespaceChunks() {return totalCodespaceChunks;}

    private CompletableFuture<Optional<Vec3d>> ptpFuture;
    private AtomicReference<Vec3d> plotScanTargetPos = new AtomicReference<Vec3d>(null);

    private boolean hideNextWhois = false;
    public boolean shouldHideNextWhois() { return hideNextWhois; }

    private int t = 0;

    public boolean hasRank(Rank r) {
        return getRank().ordinal() >= r.ordinal();
    }

    public Vec3d getPlotCorner(CodespaceCorner corner){
        if (plotOrigin == null)
            throw new RuntimeException("Cannot get plot corner because plot origin is unknown");

        Vec3d coords = plotOrigin;
        if (corner == CodespaceCorner.FRONT_RIGHT || corner == CodespaceCorner.BACK_RIGHT) {
            coords = coords.add(-1,0,CODESPACE_Z_SIZES.get(plotType));
        }
        if (corner == CodespaceCorner.BACK_LEFT || corner == CodespaceCorner.BACK_RIGHT) {
            coords = coords.add(-CODESPACE_X_SIZES.get(plotType),0,0);
        }
        return coords;
    }

    public boolean isWorldPosInCodespace(Vec3d worldPos) {
        if (plotOrigin == null)
            throw new RuntimeException("Cannot get plot corner because plot origin is unknown");

        Vec3d minusCorner = getPlotCorner(CodespaceCorner.BACK_LEFT);
        Vec3d plusCorner = getPlotCorner(CodespaceCorner.FRONT_RIGHT);

        if (worldPos.x < minusCorner.x || worldPos.z < minusCorner.z) return false;
        if (worldPos.x > plusCorner.x || worldPos.z > plusCorner.z) return false;

        return true;
    }
    public boolean isWorldPosInCodespace(BlockPos worldPos) {
        return isWorldPosInCodespace(new Vec3d((double)worldPos.getX(), (double)worldPos.getY(), (double)worldPos.getZ()));
    }

    public BlockPos clampWorldPosToCodespace(BlockPos worldPos) {
        Vec3d minusCorner = getPlotCorner(CodespaceCorner.BACK_LEFT);
        Vec3d plusCorner = getPlotCorner(CodespaceCorner.FRONT_RIGHT);

        return new BlockPos(
            (int)Math.clamp(worldPos.getX(),minusCorner.x,plusCorner.x),
            worldPos.getY(),
            (int)Math.clamp(worldPos.getZ(),minusCorner.z,plusCorner.z)
        );
    }
    public Vec3d clampWorldPosToCodespace(Vec3d worldPos) {
        Vec3d minusCorner = getPlotCorner(CodespaceCorner.BACK_LEFT);
        Vec3d plusCorner = getPlotCorner(CodespaceCorner.FRONT_RIGHT);

        return new Vec3d(
            Math.clamp(worldPos.getX(),minusCorner.x,plusCorner.x),
            worldPos.getY(),
            Math.clamp(worldPos.getZ(),minusCorner.z,plusCorner.z)
        );
    }

    public void queueModeRefresh() {
        if (modeRefreshQueued) return;
        modeRefreshQueued = true;
        TCClient.COMMAND_MANAGER.queueCommand("locate");
    }

    /**
     * Updates plot bounds, size, and code contents
     */
    public void scanPlot() {
        plotScanActive = false;
        if (plotScanActive) {return;}
        plotScanActive = true;

        CompletableFuture.runAsync(() -> {
            try {
                PlotType currentSizeGuess = PlotType.BASIC;
                Optional<Vec3d> teleportResult = Optional.empty();
                Vec3d plotOriginGuess;

                plotScanTargetPos.set(null);
                doesHaveUndergroundCodespace = false;

                // get plot origin
                // tries teleporting to x -1 so that plots without a buildspace (worldplots) still work
                ptpFuture = new CompletableFuture<>();
                TCClient.COMMAND_MANAGER.queueCommand(String.format("ptp -1.0 %s 0.0",TP_MAGIC_Y_VALUE));
                try {
                    Optional<Vec3d> result = ptpFuture.get(5, TimeUnit.SECONDS);
                    if (result.isEmpty()) { throw new RuntimeException("Failed to get plot origin"); }
                    plotOriginGuess = result.get().multiply(1, 0, 1).add(1.0,0.0,0.0);
                } catch (Exception e) {
                    TCClient.LOGGER.warn("Plot scan failed during origin fetch due to not receiving a teleport response ({})",e.toString());
                    plotScanActive = false;
                    return;
                }

                // test for underground codespace
                ptpFuture = new CompletableFuture<>();
                TCClient.COMMAND_MANAGER.queueCommand(String.format("ptp -4 %s 4",TP_MAGIC_Y_VALUE_UNDERGROUND));
                try {
                    Optional<Vec3d> result = ptpFuture.get(5, TimeUnit.SECONDS);
                    if (result.isPresent()) doesHaveUndergroundCodespace = true;
                } catch (Exception e) {
                    TCClient.LOGGER.warn("Plot scan failed during underground codespace check due to not receiving a teleport response ({})",e.toString());
                    plotScanActive = false;
                    return;
                }

                // get plot size
                sizeGuessLoop: while (getMode() == Mode.DEV) {
                    Vec3d plotSpacePos;
                    switch (currentSizeGuess) {
                        case PlotType.BASIC -> plotSpacePos = new Vec3d(-1,TP_MAGIC_Y_VALUE,51);
                        case PlotType.LARGE -> plotSpacePos = new Vec3d(-1,TP_MAGIC_Y_VALUE,101);
                        case PlotType.MASSIVE -> plotSpacePos = new Vec3d(-300,TP_MAGIC_Y_VALUE,1);
                        default -> {
                            break sizeGuessLoop;
                        }
                    }

                    plotScanTargetPos.set(plotSpacePos.add(plotOriginGuess));
                    String command = String.format("ptp %s %s %s", plotSpacePos.x, plotSpacePos.y, plotSpacePos.z);

                    TCClient.COMMAND_MANAGER.queueCommand(command);
                    ptpFuture = new CompletableFuture<>();
                    try {
                        teleportResult = ptpFuture.get(5, TimeUnit.SECONDS);
                    } catch (Exception e) {
                        TCClient.LOGGER.warn("Plot scan failed during size fetch due to not receiving a teleport response ({})", e.toString());
                        plotScanActive = false;
                        return;
                    }

                    // if teleport target was out of bounds, the plot size has been found
                    if (teleportResult.isEmpty()) {
                        break;
                    } else {
                        currentSizeGuess = PlotType.values()[currentSizeGuess.ordinal()+1];
                    }
                }

                plotOrigin = plotOriginGuess;
                plotType = currentSizeGuess;

                Vec3d minusCorner = getPlotCorner(CodespaceCorner.BACK_LEFT);
                Vec3d plusCorner = getPlotCorner(CodespaceCorner.FRONT_RIGHT);

                int minusCornerChunkX = (int)(minusCorner.x/16);
                int minusCornerChunkZ = (int)(minusCorner.z/16);
                int plusCornerChunkX = (int)(plusCorner.x/16);
                int plusCornerChunkZ = (int)(plusCorner.z/16);

                // queue every chunk in the codespace for a rescan
                totalCodespaceChunks = 0;
                for (int cx = minusCornerChunkX; cx <= plusCornerChunkX; cx++){
                    for (int cz = minusCornerChunkZ; cz <= plusCornerChunkZ; cz++) {
                        TCClient.CODESPACE_MANAGER.queueChunkForScan(new ChunkPos(cx,cz));
                        totalCodespaceChunks++;
                    }
                }

                for (ChunkPos chunkPos : TCClient.loadedChunks.keySet().toArray(new ChunkPos[0])) {
                    if (chunkPos == null) continue;
                    TCClient.CODESPACE_MANAGER.scanChunk(chunkPos);
                }

                TCClient.MCI.player.sendMessage(Text.literal("Detected plot size:" + currentSizeGuess), false);
                TCClient.MCI.player.sendMessage(Text.literal("Detected plot origin:" + plotOrigin), false);
                plotScanActive = false;
            } catch (Exception e) {
                TCClient.LOGGER.error("Error while scanning plot",e);
            }
        });
    }

    public Vec3d toPlotSpace(Vec3d worldSpacePos) {
        return worldSpacePos.subtract(getPlotOrigin());
    }
    public Vec3i toPlotSpace(Vec3i worldSpacePos) {
        return worldSpacePos.subtract(getIntPlotOrigin());
    }
    public Vec3i toPlotSpace(BlockPos worldSpacePos) {
        return worldSpacePos.subtract(getIntPlotOrigin());
    }

    public Vec3d toWorldSpace(Vec3d plotSpacePos) {
        return plotSpacePos.add(getPlotOrigin());
    }
    public Vec3i toWorldSpace(Vec3i plotSpacePos) {
        return plotSpacePos.add(getIntPlotOrigin());
    }
    public Vec3i toWorldSpace(BlockPos plotSpacePos) {
        return plotSpacePos.add(getIntPlotOrigin());
    }

    public boolean isMessageLocateResult(Text message) {
        String[] messageStrLines = message.getString().split("\n");
        ClickEvent clickEvent = message.getStyle().getClickEvent();
        // /locate parsing checks the click event since that cannot be faked by plots
        return (
            clickEvent instanceof ClickEvent.RunCommand(String command) &&
            (command.startsWith("/server") || command.startsWith("/join")) &&
            messageStrLines.length >= 2 &&
            messageStrLines[1].startsWith("You are currently")
        );
    }

    public boolean isMessageWhoisResult(Text message) {
        String[] messageStrLines = message.getString().split("\n");
        // /locate parsing checks the click event since that cannot be faked by plots
        return (
            messageStrLines.length >= 4
            && messageStrLines[1].startsWith("Profile of ")
            && messageStrLines[3].startsWith("→ Ranks: ")
            && messageStrLines[4].startsWith("→ Badges: ")
            && messageStrLines[5].startsWith("→ Joined: ")
        );
    }

    public boolean isMessageOutOfBoundsError(Text message) {
        return message.equals(OUT_OF_BOUNDS_TEXT);
    }

    public boolean isMessageFuncRenameHint(Text message) {
        return message.equals(FUNC_RENAME_HINT_TEXT);
    }

    public void onTeleported(Vec3d newPos, Vec3d oldPos) {
        if (
            ptpFuture != null && !ptpFuture.isDone() &&
            (newPos.y == TP_MAGIC_Y_VALUE || newPos.y == TP_MAGIC_Y_VALUE_UNDERGROUND) &&
            (plotScanTargetPos.get() == null || newPos.isInRange(plotScanTargetPos.get(),0.01))
        ) {
            ptpFuture.complete(Optional.of(newPos));
        }
//        TCClient.MCI.player.sendMessage(Text.literal(newPos.toString()),false);
    }

    public void onSlotChanged(int slot, ItemStack newItem) {
        //check for preferences item entering inventory as spawn indicator
        if (
            mode != Mode.SPAWN &&
            slot == 37 &&
            newItem.getItem() == Items.COMPARATOR &&
            newItem.getComponents().contains(DataComponentTypes.CUSTOM_DATA) &&
            Objects.equals(PREFERENCES_ITEM_NAME, newItem.getCustomName()) &&
            Objects.equals(PREFERENCES_ITEM_TOOLTIP, newItem.getTooltip(Item.TooltipContext.DEFAULT, TCClient.MCI.player, TooltipType.BASIC).get(1))
        ) {
            NbtCompound customData = newItem.get(DataComponentTypes.CUSTOM_DATA).copyNbt();
            Optional<NbtCompound> publicBukkitValues = customData.getCompound("PublicBukkitValues");
            if (
                publicBukkitValues.isPresent() &&
                publicBukkitValues.get().getString("hypercube:item_instance").isPresent()
            ) {
                queueModeRefresh();
            }
        }
    }

    public void onChatMessage(Text message) {
        String messageStr = message.getString();
        String[] messageStrLines = messageStr.split("\n");

        if (
            messageStr.equals("» You are now in dev mode.") ||
            messageStr.equals("» You are now in build mode.") ||
            messageStr.startsWith("» Joined game: ")
        ) {
            queueModeRefresh();
        }

        if (ptpFuture != null && !ptpFuture.isDone() && isMessageOutOfBoundsError(message)) {
            ptpFuture.complete(Optional.empty());
        }

        locateParser: if (isMessageLocateResult(message)) {
            Mode oldMode = mode;

            Matcher modeMatcher = MODE_REGEX.matcher(messageStrLines[1]);
            if (!modeMatcher.find()) break locateParser;

            switch (modeMatcher.group(1)) {
                case "coding" -> mode = Mode.DEV;
                case "building" -> mode = Mode.BUILD;
                case "playing" -> mode = Mode.PLAY;
                case "spawn" -> mode = Mode.SPAWN;
            }
            if (oldMode != mode) {
                TCClient.fireModeChangeReceivers(mode);
            }
            modeRefreshQueued = false;

            int oldPlotId = plotId;
            if (mode == Mode.SPAWN) {
                plotId = -1;
                plotName = "Spawn";
                plotOrigin = null;
                plotType = PlotType.UNKNOWN;
                doesHaveUndergroundCodespace = false;
            } else {
                Matcher plotMatcher = PLOT_REGEX.matcher(messageStrLines[3]);
                if (!plotMatcher.find()) break locateParser;

                plotName = plotMatcher.group(1);
                plotId = Integer.parseInt(plotMatcher.group(2));
            }

            if (oldPlotId != plotId) {
                doesHaveUndergroundCodespace = false;
                TCClient.firePlotChangeReceivers(plotId,mode);
            }
        }

        whoisParser: if (isMessageWhoisResult(message)) {
            String playerName = TCClient.MCI.player.getName().getString();
            if (!messageStrLines[1].equals("Profile of "+playerName+ " "))
                break whoisParser;

            hideNextWhois = false;
            String rankLine = messageStrLines[3];
            rank = Rank.NON;
            if (rankLine.contains("Overlord")) rank = Rank.OVERLORD;
            else if (rankLine.contains("Mythic")) rank = Rank.MYTHIC;
            else if (rankLine.contains("Emperor")) rank = Rank.EMPEROR;
            else if (rankLine.contains("Noble")) rank = Rank.NOBLE;

            TCClient.LOGGER.info("Detected rank: {}",rank);
        }
    }

    public void onClientSendCommand(String command) {
        // update client state when adding or removing underground codespace
        if (command.startsWith("plot")) {
            if (command.equalsIgnoreCase("plot codespace underground create")) {
                doesHaveUndergroundCodespace = true;
                return;
            }
            if (command.equalsIgnoreCase("plot codespace underground remove")) {
                doesHaveUndergroundCodespace = false;
                return;
            }
        }
    }

    public void onTickEnd(MinecraftClient client) {
        t++;
        // figure out rank
        if (rank == null && t % 20 == 0 && !hideNextWhois && mode != Mode.PLAY && !TCClient.loadedChunks.isEmpty()) {
            hideNextWhois = true;
            TCClient.COMMAND_MANAGER.queueCommand("whois");
        }
    }
}
