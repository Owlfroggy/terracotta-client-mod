package owlfroggy.terracottaclient.mixin;

import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.protocol.game.ServerboundContainerClosePacket;
import net.minecraft.network.protocol.game.ClientboundOpenScreenPacket;
import net.minecraft.world.inventory.MenuType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import owlfroggy.terracottaclient.TCClient;

@Mixin(ClientPacketListener.class)
public class ScreenOpenInterceptor {
	@Inject(method = "handleOpenScreen", at = @At("HEAD"), cancellable = true)
	private void init(ClientboundOpenScreenPacket packet, CallbackInfo info) {
        // the code editor sometimes accidentally opens the event selection menu
        // this prevents that from showing up
        if (
            TCClient.CODE_EDIT_MANAGER.isEditingCode()
            && packet.getType().equals(MenuType.GENERIC_9x5)
            && (
                packet.getTitle().getString().equals("Player Event Categories")
                || packet.getTitle().getString().equals("Entity Events")
            )
        ) {
            TCClient.MCI.getConnection().send(new ServerboundContainerClosePacket(packet.getContainerId()));
            info.cancel();
        }
    }
}