package owlfroggy.terracottaclient.ui;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.ContainerObjectSelectionList;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarratableEntry;

import java.util.ArrayList;
import java.util.List;

public class TCListEntry<E extends ContainerObjectSelectionList.Entry<E>> extends ContainerObjectSelectionList.Entry<E> {
    protected TCListWidget<?> listWidget;
    protected List<GuiEventListener> children = new ArrayList<>();

    public TCListEntry(TCListWidget<?> listWidget) {
        this.listWidget = listWidget;
    }

    // tweak vanilla minecraft's god awful default margins
    @Override public int getContentY() { return super.getContentY()-2; }
    @Override public int getContentBottom() {return super.getContentBottom()-2; }

    @Override public int getContentX() { return super.getContentX()-(listWidget.scrollable() ? 12 : 8); }
    @Override public int getContentWidth() { return super.getContentWidth()+(listWidget.scrollable() ? 19 : 17); }

    @Override
    public void extractContent(GuiGraphicsExtractor graphics, int mouseX, int mouseY, boolean hovered, float a) {
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

    public void init() {}

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

