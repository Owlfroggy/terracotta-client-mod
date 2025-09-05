package owlfroggy.terracottaclient.gameinterface;

import net.minecraft.item.ItemStack;

public interface InvChangeReceiver {
    public void onSlotChanged(int slot, ItemStack newItem);
}
