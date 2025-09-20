package owlfroggy.terracottaclient.mixin;

import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.network.packet.s2c.play.ChunkDeltaUpdateS2CPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import owlfroggy.terracottaclient.TCClient;

@Mixin(ClientPlayNetworkHandler.class)
public class ChunkDeltaInterceptor {
    @Inject(method = "onChunkDeltaUpdate", at = @At("RETURN"))
    public void init(ChunkDeltaUpdateS2CPacket packet, CallbackInfo info) {
        TCClient.fireChunkDeltaReceivers(packet);
    }
}
