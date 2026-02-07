package owlfroggy.terracottaclient.mixin;

import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientCommandSource;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.network.packet.s2c.play.CommandTreeS2CPacket;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import owlfroggy.terracottaclient.TCClient;

@Mixin(ClientPlayNetworkHandler.class)
public class CommandTreeInterceptor {
    @Inject(
        method = "onCommandTree",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/network/NetworkThreadUtils;forceMainThread(Lnet/minecraft/network/packet/Packet;Lnet/minecraft/network/listener/PacketListener;Lnet/minecraft/network/PacketApplyBatcher;)V",
            shift = At.Shift.AFTER
        )
    )
    private void onCommandTreeReceived(CommandTreeS2CPacket packet, CallbackInfo ci) {
        CommandDispatcher<ClientCommandSource> dispatcher = TCClient.MCI.getNetworkHandler().getCommandDispatcher();

        boolean hasLagslayerCommand =
            dispatcher.getRoot()
            .getChildren()
            .stream()
            .anyMatch(node -> node.getName().equals("lagslayer"));

        TCClient.setIsOnDiamondFire(hasLagslayerCommand);
    }
}