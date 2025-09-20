package owlfroggy.terracottaclient.mixin;

import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.network.packet.s2c.play.ChunkDataS2CPacket;
import net.minecraft.util.math.ChunkPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import owlfroggy.terracottaclient.TCClient;

@Mixin(ClientPlayNetworkHandler.class)
public class ChunkLoadInterceptor {
    @Inject(method = "onChunkData", at = @At("TAIL"))
    public void init(ChunkDataS2CPacket packet, CallbackInfo info) {
        TCClient.fireChunkLoadReceivers(new ChunkPos(packet.getChunkX(), packet.getChunkZ()));
    }
}
