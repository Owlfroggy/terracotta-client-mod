package owlfroggy.terracottaclient.mixin;

import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.network.packet.s2c.play.PlayerPositionLookS2CPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import owlfroggy.terracottaclient.TCClient;

@Mixin(ClientPlayNetworkHandler.class)
public class TeleportInterceptor {
	@Inject(method = "onPlayerPositionLook", at = @At("HEAD"))
	private void init(PlayerPositionLookS2CPacket packet, CallbackInfo info) {
        if (TCClient.MCI.player != null) {
            TCClient.fireTeleportReceivers(packet.change().position(),TCClient.MCI.player.getPos());
        }
	}
}