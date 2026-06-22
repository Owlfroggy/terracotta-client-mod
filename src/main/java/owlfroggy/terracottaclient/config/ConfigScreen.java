package owlfroggy.terracottaclient.config;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.*;
import net.minecraft.client.gui.components.toasts.SystemToast;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import owlfroggy.terracottaclient.TCClient;

public class ConfigScreen extends Screen {
    public ConfigScreen() {
        super(Component.literal("Terracotta Config Screen"));
    }

    @Override
    protected void init() {
        ExampleList list = new ExampleList(this.minecraft, 300, this.height - 20 - 30 - 10, 200);
        list.setPosition(this.width/2-list.getWidth()/2, 20);
        list.addEntry("Terracotta [flintlock project]");
        list.addEntry("Terracotta [fireui]");
        list.addEntry("dingus☐");
        list.addEntry("dingus TWO");
        list.addEntry("dingus THREE");
        for (int i = 0; i < 20; i++) list.addEntry(""+i);
        this.addRenderableWidget(list);
        this.addRenderableWidget(
            Button.builder(Component.literal("Done"), button -> {})
                .size(200, 20)
                .pos(this.width/2 - 100, this.height - 30)
                .build()
        );
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        super.extractRenderState(graphics, mouseX, mouseY, delta);
        graphics.centeredText(this.font, "Manage Terracotta Client Apps", this.width/2, 6, 0xFFFFFFFF);
    }
}
