package owlfroggy.terracottaclient.ui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.ContainerObjectSelectionList;
import org.jspecify.annotations.NonNull;

import java.util.ArrayList;
import java.util.List;

public class TCListWidget<E extends TCListEntry<E>> extends ContainerObjectSelectionList<E> {
    public List<E> entries = new ArrayList<>();

    public TCListWidget(Minecraft minecraft, int width, int height, int y, int itemHeight) {
        super(minecraft, width, height, y, itemHeight);
    }

    protected int addEntry(@NonNull E entry) {
        int ret = super.addEntry(entry);
        entries.add(entry);
        entry.init();
        return ret;
    }

    @Override public boolean scrollable() { return super.scrollable(); }

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
}
