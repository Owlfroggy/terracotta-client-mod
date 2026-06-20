package owlfroggy.terracottaclient.mixin;

import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.protocol.game.ClientboundPlayerPositionPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import owlfroggy.terracottaclient.TCClient;

@Mixin(ClientPacketListener.class)
public class TeleportInterceptor {
	@Inject(method = "handleMovePlayer", at = @At("RETURN"))
	private void init(ClientboundPlayerPositionPacket packet, CallbackInfo info) {
        if (TCClient.MCI.player != null) {
            TCClient.fireTeleportReceivers(packet.change().position(),TCClient.MCI.player.position());
        }
	}
}