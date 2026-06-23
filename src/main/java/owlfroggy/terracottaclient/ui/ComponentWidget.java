package owlfroggy.terracottaclient.ui;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarratableEntry;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;
import org.jspecify.annotations.NonNull;
import owlfroggy.terracottaclient.TCClient;

public class ComponentWidget implements GuiEventListener, NarratableEntry, Renderable {
    private final Component comp;
    private final int x;
    private final int y;

    public ComponentWidget(Component comp, int x, int y) {
        this.comp = comp;
        this.x = x;
        this.y = y;
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
        graphics.text(TCClient.MCI.font, comp, x, y, -1);
    }

    @Override
    public void setFocused(boolean focused) {}

    @Override
    public boolean isFocused() { return false; }

    @Override
    public @NonNull NarrationPriority narrationPriority() {
        return NarrationPriority.HOVERED;
    }

    @Override
    public void updateNarration(NarrationElementOutput output) {}
}
