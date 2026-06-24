package owlfroggy.terracottaclient;

import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderEvents;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.*;
import net.minecraft.resources.Identifier;
import owlfroggy.terracottaclient.api.APIToken;
import owlfroggy.terracottaclient.api.Permission;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class MsgHelper {
    public static class COLOR {
        public static int TC_ORANGE = 0xf9a45b;
        public static int TC_BLUE = 0x79c4f5;
        public static int LIGHT_GREEN = 0x7FFF7F;
        public static int LIGHT_RED = 0xFF5555;
        public static int LIGHT_GOLD = 0xFFD47F;
        public static int LIGHT_GRAY = 0xAAAAAA;
    }

    private static final Component ICON_COMP = Component.literal(" T  ")
        .withStyle(style -> style
            .withFont(new FontDescription.Resource(Identifier.parse("terracotta-client:icons")))
            .withShadowColor(0x000000_00)
        );

    private static final List<Component> queuedChatMessages = new ArrayList<>();

    public static void sendMessage(Component comp) {
        if (TCClient.MCI.player != null)
            TCClient.MCI.player.sendSystemMessage(comp);
    }

    /** Appends the terracotta logo to the start of `comp` before sending it **/
    public static void sendTCMessage(Component comp) {
        sendMessage(
            Component.empty()
                .append(ICON_COMP)
                .append(comp)
        );
    }

    /**
     * Schedules a chat message to be sent at the end of the frame;
     * ensures that the message is always sent from the render thread.
     * Messages will be sent in the order they are queued.
     * @param comp The message to send.
     */
    public static void safeMessage(Component comp) {
        queuedChatMessages.add(comp);
    }
    /** Like safeMessage, but it also appends the terracotta logo to the start of `comp` **/
    public static void safeTCMessage(Component comp) {
        safeMessage(
            Component.empty()
                .append(ICON_COMP)
                .append(comp)
        );
    }

    public static void initSafeMessenger() {
        LevelRenderEvents.END_MAIN.register(worldRenderContext -> {
            if (TCClient.MCI.player == null) return;

            Component[] messagesFrozen = MsgHelper.queuedChatMessages.toArray(new Component[0]);
            for (Component msg : messagesFrozen) {
                sendMessage(msg);
            }
            MsgHelper.queuedChatMessages.clear();
        });
    }


    public static MutableComponent getManagePermissionsButton() {
        return Component.empty()
            .append(
                Component.translatable("terracotta-client.permissions.clickable.managePerms")
                    .withColor(COLOR.LIGHT_GOLD)
                    .withStyle(ChatFormatting.BOLD)
                    .withStyle(Style.EMPTY.withClickEvent(new ClickEvent.RunCommand("tcapps")))
            );
    }

    /* MESSAGE BUILDER STUFF */
    public static String prettifySeconds(long seconds) {
        if (seconds < 60) return seconds+"s";
        if (seconds < 60*60) return (seconds/60)+"m";
        if (seconds < 60*60*24) return (seconds/(60*60))+"hr";
        return (seconds/(60*60*24))+"d";
    }

    public static String prettifySecondsLong(long seconds) {
        if (seconds < 60) return seconds+" seconds";
        if (seconds < 60*60) return (seconds/60)+" minutes";
        if (seconds < 60*60*24) return (seconds/(60*60))+" hours";
        return (seconds/(60*60*24))+" days";
    }

    public static MutableComponent getIndefiniteAccessWarning(APIToken token) {
        return Component.empty()
            .append(Component.translatable(
                "terracotta-client.permissions.indefiniteAccessWarning",
                Component.literal(MsgHelper.prettifySecondsLong(token.getExpiresOnTimestamp() - Instant.now().getEpochSecond()))
                    .withColor(TextColor.YELLOW)
            ))
            .append("\n")
            .append(getManagePermissionsButton());
    }


    public static List<Component> textifyPermissionsSeparateComps(Set<Permission> permissions) {
        List<Component> comps = new ArrayList<>();
        for (Permission p : Permission.values()) {
            if (!permissions.contains(p)) continue;
            comps.add(
                Component.translatable("terracotta-client.permissions.ability."+p.name()).withColor(COLOR.TC_BLUE)
            );
        }
        return comps;
    }

    public static MutableComponent textifyPermissions(Set<Permission> permissions) {
        MutableComponent msg = Component.empty();
        boolean addNewlines = false;
        for (Permission p : Permission.values()) {
            if (!permissions.contains(p)) continue;
            if (addNewlines)
                msg.append("\n");
            addNewlines = true;
            msg.append(Component.literal(" • ").withColor(TextColor.GRAY));
            msg.append(Component.translatable("terracotta-client.permissions.ability."+p.name()).withColor(COLOR.TC_BLUE));
        }
        return msg;
    }

    /* MESSAGE MATCHING STUFF */




    static final Component OUT_OF_BOUNDS_TEXT = (
        Component.empty()
            .append(Component.literal("Error: ").withStyle(ChatFormatting.RED))
            .append(Component.literal("That location is out of bounds!").withStyle(ChatFormatting.GRAY))
    );
    static final Component FUNC_RENAME_HINT_TEXT = (
        Component.empty()
            .append(Component.literal("Right click the sign while holding a ").withStyle(ChatFormatting.YELLOW))
            .append(Component.literal("String").withStyle(ChatFormatting.AQUA))
            .append(Component.literal(" to name the Function.\n").withStyle(ChatFormatting.YELLOW))
            .append(Component.literal("You can also use String on ").withStyle(ChatFormatting.YELLOW))
            .append(Component.literal("Call Function").withColor(0xFF55AA))
            .append(Component.literal(" blocks!").withStyle(ChatFormatting.YELLOW))
    );
    public static boolean isMessageLocateResult(Component message) {
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

    public static boolean isMessageWhoisResult(Component message) {
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

    public static boolean isMessageOutOfBoundsError(Component message) {
        return message.equals(MsgHelper.OUT_OF_BOUNDS_TEXT);
    }

    public static boolean isMessageCodeEditSpam(Component message) {
        String stringMessage = message.getString();
        return (
            message.equals(MsgHelper.FUNC_RENAME_HINT_TEXT)
                || stringMessage.startsWith("» ") && stringMessage.endsWith(" was broken.")
                || stringMessage.equals("Note: You can view your past 5 created templates with /templatehistory!")
                || stringMessage.equals("Error: Invalid template placement.")
        );
    }

    public static boolean isMessageFlightSpeed(Component message) {
        String stringMessage = message.getString();
        return stringMessage.startsWith("» Set fly speed to: ") && stringMessage.endsWith("% of default speed.");
    }
}
