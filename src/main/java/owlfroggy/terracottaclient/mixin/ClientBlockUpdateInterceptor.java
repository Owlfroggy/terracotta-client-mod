package owlfroggy.terracottaclient.mixin;

import net.minecraft.block.BlockState;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.util.math.BlockPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import owlfroggy.terracottaclient.TCClient;

@Mixin(ClientWorld.class)
public class ClientBlockUpdateInterceptor {
    @Inject(method = "setBlockState", at = @At("RETURN"))
    public void init(BlockPos pos, BlockState state, int flags, int maxUpdateDepth, CallbackInfoReturnable<Boolean> cir) {
        TCClient.fireClientBlockUpdateReceivers(pos,state);
    }
}
