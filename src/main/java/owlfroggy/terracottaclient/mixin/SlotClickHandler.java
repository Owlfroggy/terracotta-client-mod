package owlfroggy.terracottaclient.mixin;

import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.input.KeyInput;
import net.minecraft.client.option.GameOptions;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.Slot;
import net.minecraft.screen.slot.SlotActionType;
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
@Mixin(HandledScreen.class)
public class SlotClickHandler {
    @Shadow
    @Final
    protected ScreenHandler handler;

    @Shadow
    @Nullable
    protected Slot focusedSlot;

    @Inject(at = @At("HEAD"), method = "onMouseClick(Lnet/minecraft/screen/slot/Slot;IILnet/minecraft/screen/slot/SlotActionType;)V", cancellable = true)
    private void onSlotClick(Slot slot, int slotId, int button, SlotActionType actionType, CallbackInfo ci) {
        SlotClickCanceler.onSlotClick(slot, button, actionType, ci, handler.getCursorStack(), focusedSlot);
    }

    @Inject(at = @At("HEAD"), method = "handleHotbarKeyPressed", cancellable = true)
    protected void onKeyPressed(KeyInput keyInput, CallbackInfoReturnable<Boolean> cir) {
        if (TCClient.MCI.player == null) return;
        if (!(this.handler.getCursorStack().isEmpty() && this.focusedSlot != null)) return;
        if (this.focusedSlot.inventory instanceof PlayerInventory) return;

        GameOptions opt = TCClient.MCI.options;
        ItemStack relevantItem = ItemStack.EMPTY;
        // swap hands
        if (opt.swapHandsKey.matchesKey(keyInput)) {
            relevantItem = TCClient.MCI.player.getOffHandStack();
        }

        // hotbar
        int slot = -1;
        for (KeyBinding key : opt.hotbarKeys) {
            slot++;
            if (key.matchesKey(keyInput)) {
                relevantItem = TCClient.MCI.player.getInventory().getStack(slot);
            }
        }

        // drop
        if (opt.dropKey.matchesKey(keyInput)) {
            relevantItem = TCClient.MCI.player.getInventory().getStack(this.focusedSlot.id);
        }

        if (relevantItem.isEmpty() || TCClient.ITEM_LIBRARY_MANAGER.getLibraryData(relevantItem) == null) return;
        cir.cancel();
    }
}