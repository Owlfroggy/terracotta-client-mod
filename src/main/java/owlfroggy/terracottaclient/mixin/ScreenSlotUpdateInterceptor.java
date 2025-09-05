package owlfroggy.terracottaclient.mixin;

import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.network.packet.s2c.play.ScreenHandlerSlotUpdateS2CPacket;
import net.minecraft.network.packet.s2c.play.SetPlayerInventoryS2CPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import owlfroggy.terracottaclient.TCClient;

@Mixin(ClientPlayNetworkHandler.class)
public class ScreenSlotUpdateInterceptor {
    @Inject(method = "onScreenHandlerSlotUpdate", at = @At("HEAD"))
    private void init(ScreenHandlerSlotUpdateS2CPacket packet, CallbackInfo info) {
        TCClient.fireInvChangeReceivers(packet.getSlot(), packet.getStack());
    }
}