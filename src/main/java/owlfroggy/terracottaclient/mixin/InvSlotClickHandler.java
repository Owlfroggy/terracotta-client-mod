package owlfroggy.terracottaclient.mixin;

import net.minecraft.client.gui.screen.ingame.CreativeInventoryScreen;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.Slot;
import net.minecraft.screen.slot.SlotActionType;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import owlfroggy.terracottaclient.TCClient;
import owlfroggy.terracottaclient.itemlibrary.SlotClickCanceler;

// prevents library items from being split
@Mixin(ScreenHandler.class)
public abstract class InvSlotClickHandler {

    @Shadow
    public abstract Slot getSlot(int index);

    @Shadow
    private ItemStack cursorStack;

    @Inject(at = @At("HEAD"), method = "onSlotClick(IILnet/minecraft/screen/slot/SlotActionType;Lnet/minecraft/entity/player/PlayerEntity;)V", cancellable = true)
    private void init(int slotIndex, int button, SlotActionType actionType, PlayerEntity player, CallbackInfo ci) {
        Slot s = (slotIndex < 0) ? null : getSlot(slotIndex);
        ItemStack realCursorStack = cursorStack;
        if ((Object) this instanceof CreativeInventoryScreen.CreativeScreenHandler realThis) {
            realCursorStack = realThis.getCursorStack();
        }
        SlotClickCanceler.onSlotClick(s, button, actionType, ci, realCursorStack, s);
    }
}
