package owlfroggy.terracottaclient.mixin;

import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.network.packet.s2c.play.ChatMessageS2CPacket;
import net.minecraft.network.packet.s2c.play.GameMessageS2CPacket;
import net.minecraft.network.packet.s2c.play.PlayerPositionLookS2CPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import owlfroggy.terracottaclient.TCClient;

@Mixin(ClientPlayNetworkHandler.class)
public class ChatMessageInterceptor {
	@Inject(method = "onGameMessage", at = @At("HEAD"), cancellable = true)
	private void init(GameMessageS2CPacket packet, CallbackInfo info) {
        if (!packet.overlay()) {
            boolean shouldSuppressLocate = TCClient.DF_STATE.modeRefreshQueued;
            boolean shouldSupprsesOOB = TCClient.DF_STATE.plotScanActive;

            TCClient.fireChatMessageReceivers(packet.content());

            if (shouldSuppressLocate && TCClient.DF_STATE.isMessageLocateResult(packet.content()))
                info.cancel();
            if (shouldSupprsesOOB && TCClient.DF_STATE.isMessageOutOfBoundsError(packet.content()))
                info.cancel();
        }
	}
}