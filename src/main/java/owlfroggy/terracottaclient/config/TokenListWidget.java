package owlfroggy.terracottaclient.config;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.*;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarratableEntry;
import net.minecraft.client.gui.screens.ConfirmScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextColor;
import owlfroggy.terracottaclient.MsgHelper;
import owlfroggy.terracottaclient.TCClient;
import owlfroggy.terracottaclient.api.APIConnectionHandler;
import owlfroggy.terracottaclient.api.APIServer;
import owlfroggy.terracottaclient.api.APIToken;
import owlfroggy.terracottaclient.api.TokenManager;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class TokenListWidget extends ContainerObjectSelectionList<TokenListWidget.TokenEntry> {
    public List<TokenEntry> entries = new ArrayList<>();

    public TokenListWidget(Minecraft minecraft, int width, int height, int y) {
        super(minecraft, width, height, y, 24);
    }

    private void addEntry(APIToken token) {
        TokenEntry entry = new TokenEntry(token, this);
        super.addEntry(entry);
        entries.add(entry);
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
        return width-(scrollable() ? 28 : 20);
    }

    @Override
    protected void extractListItems(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
        // if this isn't done here, switching between the scrollable and non-scrollable layout doesnt always happen when it should
        this.refreshScrollAmount();
        super.extractListItems(graphics, mouseX, mouseY, a);
    }

    @Override
    protected void extractListBackground(GuiGraphicsExtractor graphics) {
        super.extractListBackground(graphics);
        if (entries.isEmpty()) {
            graphics.centeredText(
                TCClient.MCI.font,
                Component.translatable("terracotta-client.permissions.noApps"),
                getX() + getWidth()/2,getY() + getHeight()/2-5,
                0xFFaaaaaa
            );
            graphics.centeredText(
                TCClient.MCI.font,
                Component.translatable("terracotta-client.permissions.noApps.hint"),
                getX() + getWidth()/2,getY() + getHeight()/2+5,
                0xFFaaaaaa
            );
        }
    }

    public static class TokenEntry extends ContainerObjectSelectionList.Entry<TokenEntry> {
        private final APIToken token;
        private final TokenListWidget tokenListWidget;

        private Button removeButton = null;
        private Button infoButton = null;
        private Button disconnectButton = null;

        private List<GuiEventListener> children = new ArrayList<>();

        public TokenEntry(APIToken token, TokenListWidget tokenListWidget) {
            this.token = token;
            this.tokenListWidget = tokenListWidget;
        }

        // tweak vanilla minecraft's god awful default margins
        @Override public int getContentY() { return super.getContentY()-2; }
        @Override public int getContentBottom() {return super.getContentBottom()-2; }

        @Override public int getContentX() { return super.getContentX()-(tokenListWidget.scrollable() ? 12 : 8); }
        @Override public int getContentWidth() { return super.getContentWidth()+(tokenListWidget.scrollable() ? 19 : 17); }

        private Button makeButton(String spriteName, String tooltipKey, Button.OnPress onPress) {
            Button button = new ImageButton(
                this.getContentX(), this.getContentY(),
                20, 20,
                new WidgetSprites(
                    TCClient.ident("button/"+spriteName),
                    TCClient.ident("button/"+spriteName+"_highlighted")
                ),
                onPress,
                Component.translatable(tooltipKey)
            );
            button.setTooltip(Tooltip.create(Component.translatable(tooltipKey)));
            children.add(button);
            return button;
        }

        public void init() {
            // declared in this order since that controls tab navigation apparently
            disconnectButton = makeButton("disconnect","terracotta-client.permissions.button.disconnect",
                button -> APIServer.getTokenConnections(token).forEach(APIConnectionHandler::forceDisconnect)
            );
            infoButton = makeButton("info","terracotta-client.permissions.button.info",
                button -> TCClient.MCI.gui.setScreen(new TokenInformationScreen(token, TCClient.MCI.gui.screen()))
            );
            removeButton = makeButton("trash","terracotta-client.permissions.button.remove",
                button -> {
                    Screen old = TCClient.MCI.gui.screen();
                    final Screen parent;
                    if (old instanceof TokenManagementScreen o) parent = o.parent;
                    else { parent = null; }
                    TCClient.MCI.gui.setScreen(new ConfirmScreen(
                        confirmed -> {
                            if (confirmed) TokenManager.removeToken(token);
                            TokenManagementScreen.show(parent);
                        },
                        Component.translatable(
                            "terracotta-client.permissions.removeConfirmation.title",
                            Component.literal(token.getAppName()).withColor(MsgHelper.COLOR.TC_ORANGE)
                        ),
                        Component.translatable(
                            "terracotta-client.permissions.removeConfirmation.description",
                            Component.literal(token.getAppName()).withColor(MsgHelper.COLOR.TC_ORANGE)
                        ),
                        Component.translatable("terracotta-client.permissions.removeConfirmation.button.remove").withColor(TextColor.RED),
                        Component.translatable("terracotta-client.permissions.removeConfirmation.button.cancel")
                    ));
                }
            );
        }

        public String prettifySeconds(long seconds) {
            if (seconds < 60) return seconds+"s";
            if (seconds < 60*60) return (seconds/60)+"m";
            if (seconds < 60*60*24) return (seconds/(60*60))+"hr";
            return (seconds/(60*60*24))+"d";
        }

        @Override
        public void extractContent(GuiGraphicsExtractor graphics, int mouseX, int mouseY, boolean hovered, float a) {
            int connectionCount = APIServer.getTokenConnections(token).size();

            removeButton.setPosition(this.getContentX() + this.getContentWidth() - removeButton.getWidth(), this.getContentY());
            removeButton.extractRenderState(graphics, mouseX, mouseY, a);

            infoButton.setPosition(this.getContentX() + this.getContentWidth() - infoButton.getWidth() - 22, this.getContentY());
            infoButton.extractRenderState(graphics, mouseX, mouseY, a);

            disconnectButton.setPosition(this.getContentX() + this.getContentWidth() - disconnectButton.getWidth() - 44, this.getContentY());
            disconnectButton.visible = connectionCount > 0; // TODO: make this actually work
            disconnectButton.setTooltip(Tooltip.create(Component.translatable(
                connectionCount == 1 ? "terracotta-client.permissions.button.disconnect" : "terracotta-client.permissions.button.disconnect.multi"
            )));
            disconnectButton.extractRenderState(graphics, mouseX, mouseY, a);

            graphics.text(TCClient.MCI.font,token.getAppName(), this.getContentX(), this.getContentY()+1, -1);

            Component secondLineText;
            if (connectionCount > 0) {
                secondLineText = (
                    connectionCount == 1
                        ? Component.translatable("terracotta-client.permissions.connection.active")
                        : Component.translatable("terracotta-client.permissions.connection.active.multi", connectionCount)
                ).withColor(TextColor.GREEN);
            } else {
                long now = Instant.now().getEpochSecond();
                long lastUsedSecondsAgo = now - token.getLastUsedTimestamp();
                long timeUntilExpire = token.getExpiresOnTimestamp() - now;
                secondLineText = Component.translatable(
                    "terracotta-client.permissions.connection.inactive",
                    prettifySeconds(lastUsedSecondsAgo),
                    timeUntilExpire < 0
                        ? Component.translatable("terracotta-client.permissions.connection.inactive.onClose")
                        : Component.translatable("terracotta-client.permissions.connection.inactive.inTime",prettifySeconds(timeUntilExpire))
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
