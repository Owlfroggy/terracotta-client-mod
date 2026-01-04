package owlfroggy.terracottaclient.itemlibrary;

import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.ingame.CreativeInventoryScreen;
import net.minecraft.client.gui.screen.ingame.InventoryScreen;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.BundleItem;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.Slot;
import net.minecraft.screen.slot.SlotActionType;
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

    // warning: this function sucks
    public static void onSlotClick(Slot slot, int button, SlotActionType actionType, CallbackInfo ci, ItemStack cursorStack, Slot focusedSlot) {
        if (actionType == SlotActionType.QUICK_CRAFT){
            if (button == 0) {
                int stage = ScreenHandler.unpackQuickCraftStage(button);
                if (stage == 0) {
                    quickCraftSlices.clear();
                    return;
                }
                if (stage == 1) {
                    if (!quickCraftSlices.contains(slot)) quickCraftSlices.add(slot);
                    if (quickCraftSlices.size() == 1 && slot.inventory instanceof PlayerInventory) return;
                }
                if (stage == 2) {
                    if (quickCraftSlices.size() == 1 && quickCraftSlices.toArray(new Slot[0])[0].inventory instanceof PlayerInventory) return;
                }
            }
            cancelIfLibItem(ci, cursorStack);
        }
        else if (actionType == SlotActionType.QUICK_MOVE) {
            if (focusedSlot == null || slot == null) return;
            if (TCClient.MCI.currentScreen instanceof InventoryScreen || TCClient.MCI.currentScreen instanceof CreativeInventoryScreen)
                return;
            cancelIfLibItem(ci, focusedSlot.getStack());
        }
        else if (actionType == SlotActionType.CLONE) {
            if (focusedSlot == null) return;
            cancelIfLibItem(ci, focusedSlot.getStack());
        }
        else if (actionType == SlotActionType.PICKUP || actionType == SlotActionType.PICKUP_ALL) {
            if (focusedSlot == null) {
                cancelIfLibItem(ci, cursorStack);
                return;
            }
            ItemStack focusedStack = focusedSlot.getStack();

            // dont let items be put into bundles
            if (button == 0) {
                if (focusedStack.getItem() instanceof BundleItem)
                    cancelIfLibItem(ci, cursorStack);
                if (cursorStack.getItem() instanceof BundleItem)
                    cancelIfLibItem(ci, focusedStack);
            }

            if (!focusedStack.isEmpty() && !cursorStack.isEmpty()) {
                if (!(focusedSlot.inventory instanceof PlayerInventory)) {
                    cancelIfLibItem(ci, cursorStack);
                }
            }
            else if (focusedStack.isEmpty() && !cursorStack.isEmpty()) {
                if (button == 1 && cursorStack.getCount() > 1) cancelIfLibItem(ci, cursorStack);
                if (!(focusedSlot.inventory instanceof PlayerInventory)) cancelIfLibItem(ci, cursorStack);
            }
            else if (!focusedStack.isEmpty() && cursorStack.isEmpty()) {
                if (button == 1 && focusedStack.getCount() > 1) cancelIfLibItem(ci, focusedStack);
                if (!(focusedSlot.inventory instanceof PlayerInventory)) cancelIfLibItem(ci, focusedStack);
            }
        }
    }
}
