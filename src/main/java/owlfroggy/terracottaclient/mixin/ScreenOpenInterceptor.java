package owlfroggy.terracottaclient.mixin;

import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.network.packet.c2s.play.CloseHandledScreenC2SPacket;
import net.minecraft.network.packet.s2c.play.OpenScreenS2CPacket;
import net.minecraft.screen.ScreenHandlerType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import owlfroggy.terracottaclient.TCClient;

@Mixin(ClientPlayNetworkHandler.class)
public class ScreenOpenInterceptor {
	@Inject(method = "onOpenScreen", at = @At("HEAD"), cancellable = true)
	private void init(OpenScreenS2CPacket packet, CallbackInfo info) {
        // the code editor sometimes accidentally opens the event selection menu
        // this prevents that from showing up
        if (
            TCClient.CODE_EDIT_MANAGER.isEditingCode()
            && packet.getScreenHandlerType().equals(ScreenHandlerType.GENERIC_9X5)
            && (
                packet.getName().getString().equals("Player Event Categories")
                || packet.getName().getString().equals("Entity Events")
            )
        ) {
            TCClient.MCI.getNetworkHandler().sendPacket(new CloseHandledScreenC2SPacket(packet.getSyncId()));
            info.cancel();
        }
    }
}