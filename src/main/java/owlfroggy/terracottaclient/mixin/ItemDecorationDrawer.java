//package owlfroggy.terracottaclient.mixin;
//
//import net.minecraft.client.gui.GuiGraphics;
//import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
//import net.minecraft.client.gui.screens.inventory.InventoryScreen;
//import net.minecraft.client.multiplayer.ClientPacketListener;
//import net.minecraft.world.item.ItemStack;
//import net.minecraft.network.protocol.game.ClientboundSystemChatPacket;
//import net.minecraft.world.inventory.Slot;
//import org.spongepowered.asm.mixin.Mixin;
//import org.spongepowered.asm.mixin.injection.At;
//import org.spongepowered.asm.mixin.injection.Inject;
//import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
//import owlfroggy.terracottaclient.TCClient;
//
//@Mixin(AbstractContainerScreen.class)
//public class ItemDecorationDrawer {
//    @Inject(method = "renderSlot", at = @At("HEAD"), cancellable = true)
//    private void init(GuiGraphics context, Slot slot, int mouseX, int mouseY, CallbackInfo info) {
//        TCClient.ITEM_LIBRARY_MANAGER.applySlotDecoration(context, slot, mouseX, mouseY, info);
//    }
//}