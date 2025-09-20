package owlfroggy.terracottaclient.gameinterface;

import net.minecraft.network.packet.s2c.play.BlockEntityUpdateS2CPacket;
import net.minecraft.network.packet.s2c.play.ChunkDeltaUpdateS2CPacket;
import net.minecraft.util.math.ChunkPos;

public interface ChunkReceiver {
    public void onChunkLoad(ChunkPos chunkPos);
    public void onChunkDelta(ChunkDeltaUpdateS2CPacket packet);
    public void onBlockEntityUpdate(BlockEntityUpdateS2CPacket packet);
}
