package owlfroggy.terracottaclient.mixin;

import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.network.packet.s2c.play.SetPlayerInventoryS2CPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import owlfroggy.terracottaclient.TCClient;

@Mixin(ClientPlayNetworkHandler.class)
public class SetPlayerInvInterceptor {
    @Inject(method = "onSetPlayerInventory", at = @At("HEAD"))
    private void init(SetPlayerInventoryS2CPacket packet, CallbackInfo info) {
        TCClient.fireInvChangeReceivers(packet.slot(), packet.contents());
    }
}