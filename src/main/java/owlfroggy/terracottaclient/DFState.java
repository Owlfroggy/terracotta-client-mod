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

import java.util.Objects;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

//    public Vec3d PlotOrigin = new Vec3d(5100,0,4675);
public class DFState extends Manager implements ChatMessageReceiver, InvChangeReceiver {
    public enum Mode {
        SPAWN,
        DEV,
        PLAY,
        BUILD,
    }
    private static final Pattern MODE_REGEX = Pattern.compile("You are currently (?:at )?(\\w+)");
    private static final Pattern PLOT_REGEX = Pattern.compile("^→ (.+) \\[(\\d+)");

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

    public void queueModeRefresh() {
        if (modeRefreshQueued) return;
        modeRefreshQueued = true;
        TCClient.COMMAND_MANAGER.queueCommand("locate");
    }

    private Vec3d plotOrigin;
    public Vec3d getPlotOrigin() {return plotOrigin;}

    private Mode mode = Mode.SPAWN;
    public Mode getMode() {return mode;}

    private String plotName = "Spawn";
    public String getPlotName() {return plotName;}

    private int plotId = -1;
    public int getPlotId() {return plotId;}

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

    public void onJoin() {
        TCClient.LOGGER.info("askjhdfg");
        queueModeRefresh();
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
