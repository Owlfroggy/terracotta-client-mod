package owlfroggy.terracottaclient.gameinterface;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.network.chat.Component;

import java.util.List;

public interface TooltipRenderer {
    void onItemTooltip(ItemStack item, Item.TooltipContext context, TooltipFlag type, List<Component> lines);
}
