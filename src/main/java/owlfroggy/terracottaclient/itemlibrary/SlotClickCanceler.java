package owlfroggy.terracottaclient.itemlibrary;

import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.BundleItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import owlfroggy.terracottaclient.TCClient;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class SlotClickCanceler {
    private static void cancelIfLibItem(CallbackInfo ci, ItemStack item) {
        if (TCClient.ITEM_LIBRARY_MANAGER.getLibraryData(item) != null)
            ci.cancel();
    }

    private static List<Slot> quickCraftSlices = new ArrayList<>();

//    // warning: this function sucks
//    public static void onSlotClick(Slot slot, int button, ClickType actionType, CallbackInfo ci, ItemStack cursorStack, Slot focusedSlot) {
//        if (actionType == ClickType.QUICK_CRAFT){
//            if (button == 0) {
//                int stage = AbstractContainerMenu.getQuickcraftHeader(button);
//                if (stage == 0) {
//                    quickCraftSlices.clear();
//                    return;
//                }
//                if (stage == 1) {
//                    if (!quickCraftSlices.contains(slot)) quickCraftSlices.add(slot);
//                    if (quickCraftSlices.size() == 1 && slot.container instanceof Inventory) return;
//                }
//                if (stage == 2) {
//                    if (quickCraftSlices.size() == 1 && quickCraftSlices.toArray(new Slot[0])[0].container instanceof Inventory) return;
//                }
//            }
//            cancelIfLibItem(ci, cursorStack);
//        }
//        else if (actionType == ClickType.QUICK_MOVE) {
//            if (focusedSlot == null || slot == null) return;
//            if (TCClient.MCI.screen instanceof InventoryScreen || TCClient.MCI.screen instanceof CreativeModeInventoryScreen)
//                return;
//            cancelIfLibItem(ci, focusedSlot.getItem());
//        }
//        else if (actionType == ClickType.CLONE) {
//            if (focusedSlot == null) return;
//            cancelIfLibItem(ci, focusedSlot.getItem());
//        }
//        else if (actionType == ClickType.PICKUP || actionType == ClickType.PICKUP_ALL) {
//            if (focusedSlot == null) {
//                cancelIfLibItem(ci, cursorStack);
//                return;
//            }
//            ItemStack focusedStack = focusedSlot.getItem();
//
//            // dont let items be put into bundles
//            if (button == 0) {
//                if (focusedStack.getItem() instanceof BundleItem)
//                    cancelIfLibItem(ci, cursorStack);
//                if (cursorStack.getItem() instanceof BundleItem)
//                    cancelIfLibItem(ci, focusedStack);
//            }
//
//            if (!focusedStack.isEmpty() && !cursorStack.isEmpty()) {
//                if (!(focusedSlot.container instanceof Inventory)) {
//                    cancelIfLibItem(ci, cursorStack);
//                }
//            }
//            else if (focusedStack.isEmpty() && !cursorStack.isEmpty()) {
//                if (button == 1 && cursorStack.getCount() > 1) cancelIfLibItem(ci, cursorStack);
//                if (!(focusedSlot.container instanceof Inventory)) cancelIfLibItem(ci, cursorStack);
//            }
//            else if (!focusedStack.isEmpty() && cursorStack.isEmpty()) {
//                if (button == 1 && focusedStack.getCount() > 1) cancelIfLibItem(ci, focusedStack);
//                if (!(focusedSlot.container instanceof Inventory)) cancelIfLibItem(ci, focusedStack);
//            }
//        }
//    }
}
