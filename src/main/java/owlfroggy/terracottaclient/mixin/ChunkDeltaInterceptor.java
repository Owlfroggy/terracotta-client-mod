package owlfroggy.terracottaclient.mixin;

import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.protocol.game.ClientboundSectionBlocksUpdatePacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import owlfroggy.terracottaclient.TCClient;

@Mixin(ClientPacketListener.class)
public class ChunkDeltaInterceptor {
    @Inject(method = "handleChunkBlocksUpdate", at = @At("RETURN"))
    public void init(ClientboundSectionBlocksUpdatePacket packet, CallbackInfo info) {
        TCClient.fireChunkDeltaReceivers(packet);
    }
}
