package owlfroggy.terracottaclient.gameinterface;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.text.Text;

import java.util.List;

public interface TooltipRenderer {
    void onItemTooltip(ItemStack item, Item.TooltipContext context, TooltipType type, List<Text> lines);
}
