package owlfroggy.terracottaclient.mixin;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.gui.screen.ingame.InventoryScreen;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.item.ItemStack;
import net.minecraft.network.packet.s2c.play.GameMessageS2CPacket;
import net.minecraft.screen.slot.Slot;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import owlfroggy.terracottaclient.TCClient;

@Mixin(HandledScreen.class)
public class ItemDecorationDrawer {
    @Inject(method = "drawSlot", at = @At("HEAD"), cancellable = true)
    private void init(DrawContext context, Slot slot, int mouseX, int mouseY, CallbackInfo info) {
        TCClient.ITEM_LIBRARY_MANAGER.applySlotDecoration(context, slot, mouseX, mouseY, info);
    }
}