package owlfroggy.terracottaclient.gameinterface;

import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.network.protocol.game.ClientboundSectionBlocksUpdatePacket;
import net.minecraft.world.level.ChunkPos;

public interface ChunkReceiver {
    public void onChunkLoad(ChunkPos chunkPos);
    public void onChunkDelta(ClientboundSectionBlocksUpdatePacket packet);
    public void onBlockEntityUpdate(ClientboundBlockEntityDataPacket packet);
}
