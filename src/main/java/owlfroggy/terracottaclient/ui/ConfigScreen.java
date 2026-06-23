package owlfroggy.terracottaclient.ui;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Checkbox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextColor;
import owlfroggy.terracottaclient.MsgHelper;
import owlfroggy.terracottaclient.TCClient;
import owlfroggy.terracottaclient.api.APIToken;
import owlfroggy.terracottaclient.api.Permission;
import owlfroggy.terracottaclient.api.TokenManager;
import owlfroggy.terracottaclient.config.Config;
import owlfroggy.terracottaclient.config.ConfigValue;

import java.lang.reflect.Field;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.Collection;
import java.util.Set;

public class ConfigScreen extends Screen {
    public final Screen parent;

    public ConfigScreen(Screen parent) {
        super(Component.literal("Terracotta App Management Screen"));
        this.parent = parent;
    }

    public static void show(Screen parent) {
        TCClient.MCI.execute(() -> {
            TCClient.MCI.gui.setScreen(new ConfigScreen(parent));
        });
    }

    @Override
    public void onClose() {
        TCClient.MCI.gui.setScreen(parent);
    }

    @Override
    protected void init() {
        ConfigListWidget list = new ConfigListWidget(this.minecraft, 300, this.height - 20 - 30 - 10, 200);
        list.setPosition(this.width/2-list.getWidth()/2, 20);
        list.populate();
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
        graphics.centeredText(this.font, Component.translatable("terracotta-client.config.title"), this.width/2, 6, 0xFFFFFFFF);
    }
}
