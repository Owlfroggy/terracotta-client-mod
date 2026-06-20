package owlfroggy.terracottaclient.mixin;

import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.Options;
import net.minecraft.client.KeyMapping;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.inventory.ClickType;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import owlfroggy.terracottaclient.TCClient;
import owlfroggy.terracottaclient.itemlibrary.ItemLibraryManager;
import owlfroggy.terracottaclient.itemlibrary.SlotClickCanceler;

// prevents library items from being put into chests
@Mixin(AbstractContainerScreen.class)
public class SlotClickHandler {
    @Shadow
    @Final
    protected AbstractContainerMenu menu;

    @Shadow
    @Nullable
    protected Slot hoveredSlot;

    @Inject(at = @At("HEAD"), method = "slotClicked(Lnet/minecraft/world/inventory/Slot;IILnet/minecraft/world/inventory/ClickType;)V", cancellable = true)
    private void onSlotClick(Slot slot, int slotId, int button, ClickType actionType, CallbackInfo ci) {
        SlotClickCanceler.onSlotClick(slot, button, actionType, ci, menu.getCarried(), hoveredSlot);
    }

    @Inject(at = @At("HEAD"), method = "checkHotbarKeyPressed", cancellable = true)
    protected void onKeyPressed(KeyEvent keyInput, CallbackInfoReturnable<Boolean> cir) {
        if (TCClient.MCI.player == null) return;
        if (!(this.menu.getCarried().isEmpty() && this.hoveredSlot != null)) return;
        if (this.hoveredSlot.container instanceof Inventory) return;

        Options opt = TCClient.MCI.options;
        ItemStack relevantItem = ItemStack.EMPTY;
        // swap hands
        if (opt.keySwapOffhand.matches(keyInput)) {
            relevantItem = TCClient.MCI.player.getOffhandItem();
        }

        // hotbar
        int slot = -1;
        for (KeyMapping key : opt.keyHotbarSlots) {
            slot++;
            if (key.matches(keyInput)) {
                relevantItem = TCClient.MCI.player.getInventory().getItem(slot);
            }
        }

        // drop
        if (opt.keyDrop.matches(keyInput)) {
            relevantItem = TCClient.MCI.player.getInventory().getItem(this.hoveredSlot.index);
        }

        if (relevantItem.isEmpty() || TCClient.ITEM_LIBRARY_MANAGER.getLibraryData(relevantItem) == null) return;
        cir.cancel();
    }
}