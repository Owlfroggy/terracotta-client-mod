package owlfroggy.terracottaclient.mixin;

import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.inventory.ClickType;
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
@Mixin(AbstractContainerMenu.class)
public abstract class InvSlotClickHandler {

    @Shadow
    public abstract Slot getSlot(int index);

    @Shadow
    private ItemStack carried;

    @Inject(at = @At("HEAD"), method = "clicked(IILnet/minecraft/world/inventory/ClickType;Lnet/minecraft/world/entity/player/Player;)V", cancellable = true)
    private void init(int slotIndex, int button, ClickType actionType, Player player, CallbackInfo ci) {
        Slot s = (slotIndex < 0) ? null : getSlot(slotIndex);
        ItemStack realCursorStack = carried;
        if ((Object) this instanceof CreativeModeInventoryScreen.ItemPickerMenu realThis) {
            realCursorStack = realThis.getCarried();
        }
        SlotClickCanceler.onSlotClick(s, button, actionType, ci, realCursorStack, s);
    }
}
