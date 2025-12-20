package owlfroggy.terracottaclient.gameinterface;

import net.minecraft.block.BlockState;
import net.minecraft.util.math.BlockPos;

public interface ClientBlockUpdateReceiver {
    public void onClientBlockUpdate(BlockPos pos, BlockState state);
}
