package owlfroggy.terracottaclient.mixin;

import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientSuggestionProvider;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.protocol.game.ClientboundCommandsPacket;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import owlfroggy.terracottaclient.TCClient;

@Mixin(ClientPacketListener.class)
public class CommandTreeInterceptor {
    @Inject(
        method = "handleCommands",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/network/protocol/PacketUtils;ensureRunningOnSameThread(Lnet/minecraft/network/protocol/Packet;Lnet/minecraft/network/PacketListener;Lnet/minecraft/network/PacketProcessor;)V",
            shift = At.Shift.AFTER
        )
    )
    private void onCommandTreeReceived(ClientboundCommandsPacket packet, CallbackInfo ci) {
        CommandDispatcher<ClientSuggestionProvider> dispatcher = TCClient.MCI.getConnection().getCommands();

        boolean hasLagslayerCommand =
            dispatcher.getRoot()
            .getChildren()
            .stream()
            .anyMatch(node -> node.getName().equals("lagslayer"));

        TCClient.setIsOnDiamondFire(hasLagslayerCommand);
    }
}