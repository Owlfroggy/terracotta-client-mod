package owlfroggy.terracottaclient.gameinterface;

import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.core.BlockPos;

public interface ClientBlockUpdateReceiver {
    public void onClientBlockUpdate(BlockPos pos, BlockState state);
}
