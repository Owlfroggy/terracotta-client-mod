package owlfroggy.terracottaclient.ui;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.*;
import net.minecraft.client.gui.screens.ConfirmScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipPositioner;
import net.minecraft.client.gui.screens.inventory.tooltip.DefaultTooltipPositioner;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextColor;
import net.minecraft.world.inventory.tooltip.TooltipComponent;
import org.lwjgl.glfw.GLFW;
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
import java.util.*;
import java.util.stream.Collectors;

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

            if (type == boolean.class) {
                this.addEntry(new BoolValueEntry(key, f, this));
            } else {
                this.addEntry(new EnumValueEntry(key, f, this));
            }
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

    public abstract static class ConfigEntry extends TCListEntry<ConfigListWidget.ConfigEntry> {
        public ConfigEntry(TCListWidget<?> listWidget) {
            super(listWidget);
        }
    };

    public static class ValueEntry extends ConfigEntry {
        protected String key;
        protected Field field;

        public ValueEntry(String key, Field field, ConfigListWidget tokenListWidget) {
            super(tokenListWidget);
            this.key = key;
            this.field = field;
        }

        @Override
        public void extractContent(GuiGraphicsExtractor graphics, int mouseX, int mouseY, boolean hovered, float a) {
            super.extractContent(graphics,mouseX,mouseY,hovered,a);

            Component configNameComp = Component.translatable("terracotta-client.config.value."+key+".name");

            if (this.isMouseOverBg(mouseX,mouseY)) {
                // all this just to split by \n and add a title...
                ArrayList<Component> lines = Arrays.stream(Component.translatable("terracotta-client.config.value."+key+".description")
                        .getString()
                        .split("\n"))
                        .map(s -> (Component)Component.literal(s).withColor(MsgHelper.COLOR.LIGHT_GRAY))
                        .collect(Collectors.toCollection(ArrayList::new));
                lines.addFirst(configNameComp);

                graphics.setTooltipForNextFrame(
                    TCClient.MCI.font,
                    lines,
                    Optional.empty(),
                    mouseX,mouseY
                );
            }

            graphics.text(
                TCClient.MCI.font,
                configNameComp,
                this.getContentX(), this.getContentY()+6,
                -1
            );
        }
    }

    public static class BoolValueEntry extends ValueEntry {
        private TCCheckbox checkbox;

        public BoolValueEntry(String key, Field field, ConfigListWidget tokenListWidget) {
            super(key, field, tokenListWidget);
        }

        @Override
        public void init() {
            super.init();
            boolean initValue = true;
            try {
                initValue = field.getBoolean(null);
            } catch (Exception ignored) {}

            checkbox = new TCCheckbox(
                0, 0,
                initValue, true,
                (box) -> {
                    try {
                        field.setBoolean(null, box.isChecked());
                        Config.write();
                    } catch (Exception e) {
                        TCClient.LOGGER.error("Failed to set boolean config value {} {} {}", key, e.getMessage(), e.getStackTrace());
                    }
                }
            );
            children.add(checkbox);
        }

        @Override
        public void extractContent(GuiGraphicsExtractor graphics, int mouseX, int mouseY, boolean hovered, float a) {
            super.extractContent(graphics, mouseX, mouseY, hovered, a);

            checkbox.setPosition(getContentRight() - 20, getContentY());
            checkbox.extractContents(graphics, mouseX, mouseY, a);
        }
    }

    public static class EnumValueEntry extends ValueEntry {
        private Button button;


        public EnumValueEntry(String key, Field field, ConfigListWidget tokenListWidget) {
            super(key, field, tokenListWidget);
        }

        @Override
        public void init() {
            super.init();
            boolean initValue = true;

            button = Button.builder(Component.empty(),
                (Button button) -> {
                    try {
                        Enum<?> existingValue = (Enum<?>)field.get(null);
                        var enumConstants = existingValue.getDeclaringClass().getEnumConstants();
                        boolean isShiftDown = InputConstants.isKeyDown(TCClient.MCI.getWindow(), GLFW.GLFW_KEY_LEFT_SHIFT) || InputConstants.isKeyDown(TCClient.MCI.getWindow(), GLFW.GLFW_KEY_RIGHT_SHIFT);
                        int nextOrdinal = existingValue.ordinal() + (isShiftDown ? -1 : 1);
                        if (nextOrdinal > enumConstants.length-1)
                            nextOrdinal = 0;
                        if (nextOrdinal < 0)
                            nextOrdinal = enumConstants.length-1;
                        field.set(null, enumConstants[nextOrdinal]);
                        Config.write();
                    } catch (Exception e) {
                        TCClient.LOGGER.error("Failed to set enum config value {} {} {}", key, e.getMessage(), e.getStackTrace());
                    }
                }
            ).size(100, 20).build();
            children.add(button);
        }

        @Override
        public void extractContent(GuiGraphicsExtractor graphics, int mouseX, int mouseY, boolean hovered, float a) {

            super.extractContent(graphics, mouseX, mouseY, hovered, a);

            Component msg = Component.literal("error :(").withColor(TextColor.RED);
            try {
                msg = Component.translatable("terracotta-client.config.value.%s.option.%s".formatted(key, ((Enum<?>)field.get(null)).name()));
            } catch (Exception ignored) {}


            button.setPosition(getContentRight() - button.getWidth(), getContentY());
            button.setMessage(msg);
            button.extractRenderState(graphics, mouseX, mouseY, a);
        }
    }
}

