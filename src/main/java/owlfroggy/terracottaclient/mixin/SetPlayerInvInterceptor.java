package owlfroggy.terracottaclient.mixin;

import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.protocol.game.ClientboundSetPlayerInventoryPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import owlfroggy.terracottaclient.TCClient;

@Mixin(ClientPacketListener.class)
public class SetPlayerInvInterceptor {
    @Inject(method = "handleSetPlayerInventory", at = @At("HEAD"))
    private void init(ClientboundSetPlayerInventoryPacket packet, CallbackInfo info) {
        TCClient.fireInvChangeReceivers(packet.slot(), packet.contents());
    }
}