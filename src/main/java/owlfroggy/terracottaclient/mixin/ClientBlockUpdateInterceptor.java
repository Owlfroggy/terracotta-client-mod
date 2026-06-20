package owlfroggy.terracottaclient.mixin;

import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import owlfroggy.terracottaclient.TCClient;

@Mixin(ClientLevel.class)
public class ClientBlockUpdateInterceptor {
    @Inject(method = "setBlock", at = @At("RETURN"))
    public void init(BlockPos pos, BlockState state, int flags, int maxUpdateDepth, CallbackInfoReturnable<Boolean> cir) {
        TCClient.fireClientBlockUpdateReceivers(pos,state);
    }
}
