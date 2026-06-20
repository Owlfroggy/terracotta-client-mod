package owlfroggy.terracottaclient.mixin;

import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.protocol.game.ClientboundContainerSetSlotPacket;
import net.minecraft.network.protocol.game.ClientboundSetPlayerInventoryPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import owlfroggy.terracottaclient.TCClient;

@Mixin(ClientPacketListener.class)
public class ScreenSlotUpdateInterceptor {
    @Inject(method = "handleContainerSetSlot", at = @At("HEAD"))
    private void init(ClientboundContainerSetSlotPacket packet, CallbackInfo info) {
        TCClient.fireInvChangeReceivers(packet.getSlot(), packet.getItem());
    }
}