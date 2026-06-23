package owlfroggy.terracottaclient.ui;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.*;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import owlfroggy.terracottaclient.TCClient;
import owlfroggy.terracottaclient.api.APIToken;
import owlfroggy.terracottaclient.api.TokenManager;

import java.util.Collection;

public class TokenManagementScreen extends Screen {
    private Collection<APIToken> tokens;
    public final Screen parent;

    public TokenManagementScreen(Collection<APIToken> tokens, Screen parent) {
        super(Component.literal("Terracotta App Management Screen"));
        this.tokens = tokens;
        this.parent = parent;
    }

    public static void show(Screen parent) {
        TCClient.MCI.execute(() -> {
            TCClient.MCI.gui.setScreen(new TokenManagementScreen(TokenManager.getAllTokens(), parent));
        });
    }

    @Override
    public void onClose() {
        TCClient.MCI.gui.setScreen(parent);
    }

    @Override
    protected void init() {
        TokenListWidget list = new TokenListWidget(this.minecraft, 300, this.height - 20 - 30 - 10, 200);
        list.setPosition(this.width/2-list.getWidth()/2, 20);
        list.populate(tokens);
        this.addRenderableWidget(list);
        this.addRenderableWidget(
            Button.builder(Component.translatable("gui.done"),
                    button -> TCClient.MCI.gui.setScreen(parent)
                )
                .size(200, 20)
                .pos(this.width/2 - 100, this.height - 30)
                .build()
        );
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        super.extractRenderState(graphics, mouseX, mouseY, delta);
        graphics.centeredText(this.font, Component.translatable("terracotta-client.permissions.manageApps"), this.width/2, 6, 0xFFFFFFFF);
    }
}
