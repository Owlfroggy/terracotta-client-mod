package owlfroggy.terracottaclient;

import net.minecraft.component.DataComponentTypes;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.text.ClickEvent;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.Vec3d;
import owlfroggy.terracottaclient.gameinterface.ChatMessageReceiver;
import owlfroggy.terracottaclient.gameinterface.InvChangeReceiver;
import owlfroggy.terracottaclient.gameinterface.TeleportReceiver;

import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

//    public Vec3d PlotOrigin = new Vec3d(5100,0,4675);
public class DFState extends Manager implements ChatMessageReceiver, InvChangeReceiver, TeleportReceiver {
    public enum Mode {
        SPAWN,
        DEV,
        PLAY,
        BUILD,
    }
    public enum PlotType {
        UNKNOWN,
        BASIC,
        LARGE,
        MASSIVE,
        MEGA
    }
    private enum PlotScanState {

    }

    private static final Pattern MODE_REGEX = Pattern.compile("You are currently (?:at )?(\\w+)");
    private static final Pattern PLOT_REGEX = Pattern.compile("^→ (.+) \\[(\\d+)");
    private static final double TP_MAGIC_Y_VALUE = 52.15763;
    private static final Text OUT_OF_BOUNDS_TEXT = (
        Text.empty()
        .append(Text.literal("Error: ").formatted(Formatting.RED))
        .append(Text.literal("That location is out of bounds!").formatted(Formatting.GRAY))
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

    private Vec3d plotOrigin;
    public Vec3d getPlotOrigin() {return plotOrigin;}

    private Mode mode = Mode.SPAWN;
    public Mode getMode() {return mode;}

    private String plotName = "Spawn";
    public String getPlotName() {return plotName;}

    private int plotId = -1;
    public int getPlotId() {return plotId;}

    private PlotType plotType = PlotType.UNKNOWN;
    public PlotType getPlotType() {return plotType;};

    private CompletableFuture<Optional<Vec3d>> ptpFuture;

    public void queueModeRefresh() {
        if (modeRefreshQueued) return;
        modeRefreshQueued = true;
        TCClient.COMMAND_MANAGER.queueCommand("locate");
    }

    /**
     * Updates plot bounds, size, and code contents
     */
    public void scanPlot() {
        if (plotScanActive) {return;}
        plotScanActive = true;
        CompletableFuture.runAsync(() -> {
            PlotType currentSizeGuess = PlotType.BASIC;
            Optional<Vec3d> teleportResult;

            // get plot origin
            TCClient.COMMAND_MANAGER.queueCommand(String.format("ptp 0.0 %s 0.0",TP_MAGIC_Y_VALUE));
            ptpFuture = new CompletableFuture<>();
            try {
                Optional<Vec3d> result = ptpFuture.get(5, TimeUnit.SECONDS);
                if (result.isEmpty()) { throw new RuntimeException("Failed to get plot origin"); }
                plotOrigin = result.get().multiply(1,0,1);
            } catch (Exception e) {
                TCClient.LOGGER.warn("Plot scan failed during origin fetch due to not receiving a teleport response ({})",e.toString());
                plotScanActive = false;
                return;
            }

            // get plot size
            sizeGuessLoop: while (getMode() == Mode.DEV) {
                String command;
                switch (currentSizeGuess) {
                    case PlotType.BASIC -> command = String.format("ptp -1 %s 51",TP_MAGIC_Y_VALUE);
                    case PlotType.LARGE -> command = String.format("ptp -1 %s 101",TP_MAGIC_Y_VALUE);
                    case PlotType.MASSIVE -> command = String.format("ptp -301 %s 1",TP_MAGIC_Y_VALUE);
                    default -> {
                        break sizeGuessLoop;
                    }
                }

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

            plotType = currentSizeGuess;
            plotScanActive = false;
            TCClient.MCI.player.sendMessage(Text.literal("Detected plot size:" + currentSizeGuess), false);
            TCClient.MCI.player.sendMessage(Text.literal("Detected plot origin:" + plotOrigin), false);
        });
    }

    public Vec3d toPlotSpace(Vec3d worldSpacePos) {
        return worldSpacePos.subtract(getPlotOrigin());
    }

    public Vec3d toWorldSpace(Vec3d plotSpacePos) {
        return plotSpacePos.add(getPlotOrigin());
    }

    public boolean isMessageLocateResult(Text message) {
        String[] messageStrLines = message.getString().split("\n");
        ClickEvent clickEvent = message.getStyle().getClickEvent();
        return (
            clickEvent instanceof ClickEvent.RunCommand(String command) &&
            (command.startsWith("/server") || command.startsWith("/join")) &&
            messageStrLines.length >= 2 &&
            messageStrLines[1].startsWith("You are currently")
        );
    }

    public boolean isMessageOutOfBoundsError(Text message) {
        return message.equals(OUT_OF_BOUNDS_TEXT);
    }

    public void onJoin() {
        TCClient.LOGGER.info("askjhdfg");
        queueModeRefresh();
    }

    public void onTeleported(Vec3d newPos, Vec3d oldPos) {
        if (ptpFuture != null && !ptpFuture.isDone() && newPos.y == TP_MAGIC_Y_VALUE) {
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
        TCClient.LOGGER.info(message.toString());

        // /locate parsing checks the click event since that cannot be faked by plots
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

            if (mode == Mode.SPAWN) {
                plotId = -1;
                plotName = "Spawn";
            } else {
                Matcher plotMatcher = PLOT_REGEX.matcher(messageStrLines[3]);
                if (!plotMatcher.find()) break locateParser;

                plotName = plotMatcher.group(1);
                plotId = Integer.parseInt(plotMatcher.group(2));
            }
        }
    }
}
