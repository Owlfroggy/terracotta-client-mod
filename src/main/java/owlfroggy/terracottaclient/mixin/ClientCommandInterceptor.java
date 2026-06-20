package owlfroggy.terracottaclient.mixin;

import net.minecraft.client.multiplayer.ClientPacketListener;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import owlfroggy.terracottaclient.TCClient;

@Mixin(ClientPacketListener.class)
public class ClientCommandInterceptor {
    @Inject(method = "sendCommand", at = @At("RETURN"))
    private void onSendCommand(String command, CallbackInfo ci) {
        TCClient.fireClientCommandReceivers(command);
    }
}