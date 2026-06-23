package owlfroggy.terracottaclient.ui;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.ImageButton;
import net.minecraft.client.gui.components.WidgetSprites;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextColor;
import net.minecraft.resources.Identifier;
import owlfroggy.terracottaclient.TCClient;

public class TCCheckbox extends Button {
    public interface OnPress {
        void onPress(final TCCheckbox button);
    }

    private static final WidgetSprites ON_SPRITES = new WidgetSprites(
        Identifier.withDefaultNamespace("widget/checkbox_selected"),
        Identifier.withDefaultNamespace("widget/checkbox_selected_highlighted")
    );
    private static final WidgetSprites OFF_SPRITES = new WidgetSprites(
        Identifier.withDefaultNamespace("widget/checkbox"),
        Identifier.withDefaultNamespace("widget/checkbox_highlighted")
    );

    private boolean checked;
    private boolean showText;

    public TCCheckbox(int x, int y, boolean initialState, boolean showText, TCCheckbox.OnPress onPress) {
        super(
            x, y,
            20, 20,
            Component.empty(),
            (b) -> {
                TCCheckbox box = (TCCheckbox)b;
                box.checked = !box.checked;
                onPress.onPress(box);
            },
            DEFAULT_NARRATION
        );
        this.checked = initialState;
        this.showText = showText;
    }

    public boolean isChecked() { return checked; }

    private static WidgetSprites getSprites(boolean checked) {
        return checked ? ON_SPRITES : OFF_SPRITES;
    }

    @Override
    protected void extractContents(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
        // isHovered is incorrectly cached when this runs so that has to be done out manually here
        Identifier sprite = getSprites(checked).get(this.isActive(), (graphics.containsPointInScissor(mouseX, mouseY) && this.isMouseOver(mouseX,mouseY)) || this.isFocused());
        graphics.blitSprite(RenderPipelines.GUI_TEXTURED, sprite, this.getX(), this.getY(), this.width, this.height);

        if (showText) {
            if (checked) {
                graphics.text(
                    TCClient.MCI.font,
                    Component.translatable("options.on").withColor(TextColor.GREEN),
                    getRight() - 35, getY()+6,
                    -1
                );
            } else {
                graphics.text(
                    TCClient.MCI.font,
                    Component.translatable("options.off").withColor(TextColor.RED),
                    getRight() - 41, getY()+6,
                    -1
                );
            }
        }
    }
}
