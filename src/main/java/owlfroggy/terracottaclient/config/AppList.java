package owlfroggy.terracottaclient.config;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.*;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarratableEntry;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextColor;
import owlfroggy.terracottaclient.TCClient;
import owlfroggy.terracottaclient.api.APIServer;
import owlfroggy.terracottaclient.api.APIToken;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class AppList extends ContainerObjectSelectionList<AppList.AppEntry> {
    public List<AppEntry> entries = new ArrayList<>();

    public AppList(Minecraft minecraft, int width, int height, int y) {
        super(minecraft, width, height, y, 24);
    }

    private void addEntry(APIToken token) {
        AppEntry entry = new AppEntry(token);
        super.addEntry(entry);
        entry.init();
    }

    public void populate(Collection<APIToken> tokens) {
        APIToken[] sorted = tokens
            .stream()
            .sorted((a, b) -> Math.toIntExact(b.getLastUsedTimestamp() - a.getLastUsedTimestamp()))
            .toArray(APIToken[]::new);
        for (APIToken t : sorted) {
            addEntry(t);
        }
    }

    // this has to be done to put the scroll bar in the right place
    @Override public int getRowWidth() {
        return width-28;
    }

    public static class AppEntry extends ContainerObjectSelectionList.Entry<AppEntry> {
        private final APIToken token;

        private Button removeButton = null;
        private Button infoButton = null;
        private Button disconnectButton = null;

        private List<GuiEventListener> children = new ArrayList<>();

        public AppEntry(APIToken token) {
            this.token = token;
        }

        // tweak vanilla minecraft's god awful default margins
        @Override public int getContentY() { return super.getContentY()-2; }
        @Override public int getContentBottom() {return super.getContentBottom()-2; }

        @Override public int getContentX() { return super.getContentX()-12; }
        @Override public int getContentWidth() { return super.getContentWidth()+19; }

        private Button makeButton(String spriteName, String tooltip, Button.OnPress onPress) {
            Button button = new ImageButton(
                this.getContentX(), this.getContentY(),
                20, 20,
                new WidgetSprites(
                    TCClient.ident("button/"+spriteName),
                    TCClient.ident("button/"+spriteName+"_highlighted")
                ),
                button1 -> {},
                Component.literal(tooltip)
            );
            button.setTooltip(Tooltip.create(Component.literal(tooltip)));
            children.add(button);
            return button;
        }

        public void init() {
            removeButton = makeButton("trash","Permanently Remove", button -> {});
            infoButton = makeButton("info","Info", button -> {});
            disconnectButton = makeButton("disconnect","Disconnect", button -> {});
        }

        public String prettifySeconds(long seconds) {
            if (seconds < 60) return seconds+"s";
            if (seconds < 60*60) return (seconds/60)+"m";
            if (seconds < 60*60*24) return (seconds/(60*60))+"hr";
            return (seconds/(60*60*24))+"d";
        }

        @Override
        public void extractContent(GuiGraphicsExtractor graphics, int mouseX, int mouseY, boolean hovered, float a) {
            int connectionCount = APIServer.getTokenConnectionCount(token);

            removeButton.setPosition(this.getContentX() + this.getContentWidth() - removeButton.getWidth(), this.getContentY());
            removeButton.extractRenderState(graphics, mouseX, mouseY, a);

            infoButton.setPosition(this.getContentX() + this.getContentWidth() - infoButton.getWidth() - 22, this.getContentY());
            infoButton.extractRenderState(graphics, mouseX, mouseY, a);

            disconnectButton.setPosition(this.getContentX() + this.getContentWidth() - disconnectButton.getWidth() - 44, this.getContentY());
            disconnectButton.visible = connectionCount > 0; // TODO: make this actually work
            disconnectButton.setTooltip(Tooltip.create(Component.literal(
                connectionCount == 1 ? "Disconnect" : "Disconnect All"
            )));
            disconnectButton.extractRenderState(graphics, mouseX, mouseY, a);

            graphics.text(TCClient.MCI.font,token.getAppName(), this.getContentX(), this.getContentY()+1, -1);

            Component secondLineText;
            if (connectionCount > 0) {
                secondLineText = Component.literal(
                    connectionCount == 1 ? "Actively connected" : connectionCount + " active connections"
                ).withColor(TextColor.GREEN);
            } else {
                long now = Instant.now().getEpochSecond();
                long lastUsedSecondsAgo = now - token.getLastUsedTimestamp();
                long timeUntilExpire = token.getExpiresOnTimestamp() - now;
                secondLineText = Component.literal(
                    "Last used %s ago, expires %s".formatted(
                        prettifySeconds(lastUsedSecondsAgo),
                        timeUntilExpire < 0 ? "on game close" : "in "+prettifySeconds(timeUntilExpire)
                    )
                ).withStyle(ChatFormatting.ITALIC, ChatFormatting.GRAY);
            }
            graphics.text(TCClient.MCI.font,secondLineText, this.getContentX(), this.getContentBottom()-7, -1);

            graphics.fill(
                this.getContentX(), this.getContentBottom()+4,
                this.getContentRight(), this.getContentBottom()+3,
                0x60FFFFFF
            );
            graphics.fill(
                this.getContentX(), this.getContentBottom()+5,
                this.getContentRight(), this.getContentBottom()+4,
                0x60000000
            );
        }

        @Override
        public List<? extends GuiEventListener> children() {
            return children;
        }

        @Override
        public List<? extends NarratableEntry> narratables() {
            // TODO: fill this out
            return List.of();
        }
    }
}
