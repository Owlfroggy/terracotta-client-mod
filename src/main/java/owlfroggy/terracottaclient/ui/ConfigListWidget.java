package owlfroggy.terracottaclient.ui;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.*;
import net.minecraft.client.gui.screens.ConfirmScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextColor;
import net.minecraft.world.inventory.tooltip.TooltipComponent;
import owlfroggy.terracottaclient.MsgHelper;
import owlfroggy.terracottaclient.TCClient;
import owlfroggy.terracottaclient.api.APIConnectionHandler;
import owlfroggy.terracottaclient.api.APIServer;
import owlfroggy.terracottaclient.api.APIToken;
import owlfroggy.terracottaclient.api.TokenManager;
import owlfroggy.terracottaclient.config.Config;
import owlfroggy.terracottaclient.config.ConfigValue;

import java.lang.reflect.Field;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

public class ConfigListWidget extends TCListWidget<ConfigListWidget.ConfigEntry> {
    public ConfigListWidget(Minecraft minecraft, int width, int height, int y) {
        super(minecraft, width, height, y, 24);
    }

    public void populate() {
        Field[] configFields = (Arrays.stream(Config.class.getFields())
            .filter(field -> field.getAnnotation(ConfigValue.class) != null)
            .sorted((a, b) -> a.getAnnotation(ConfigValue.class).order() - b.getAnnotation(ConfigValue.class).order())
            .toArray(Field[]::new)
        );

        for (Field f : configFields) {
            Class<?> type = f.getType();
            String key = f.getAnnotation(ConfigValue.class).key();

            this.addEntry(new ConfigEntry(key,this));

//            this.addRenderableWidget(new ComponentWidget(
//                Component.translatable("terracotta-client.config.value."+key),
//                this.width/2-145, y
//            ));
//            if (type == boolean.class) {
//                this.addRenderableWidget(
//                    Checkbox.builder(Component.translatable("balls"),TCClient.MCI.font)
//                        .pos(this.width/2+145,y)
//                        .build()
//                );
//            }
        }
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

    public static class ConfigEntry extends TCListEntry<ConfigListWidget.ConfigEntry> {
        private String key;

        public ConfigEntry(String key, ConfigListWidget tokenListWidget) {
            super(tokenListWidget);
            this.key = key;
        }

        @Override
        public void extractContent(GuiGraphicsExtractor graphics, int mouseX, int mouseY, boolean hovered, float a) {
            super.extractContent(graphics,mouseX,mouseY,hovered,a);

            if (this.isMouseOver(mouseX,mouseY)) {
                TCClient.LOGGER.info(key);
            }

            graphics.text(
                TCClient.MCI.font,
                Component.translatable("terracotta-client.config.value."+key+".name"),
                this.getContentX(), this.getContentY()+7,
                -1
            );
        }
    }
}

