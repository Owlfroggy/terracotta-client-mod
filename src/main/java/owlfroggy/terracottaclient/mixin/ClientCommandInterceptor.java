package owlfroggy.terracottaclient.mixin;

import net.minecraft.client.network.ClientPlayNetworkHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import owlfroggy.terracottaclient.TCClient;

@Mixin(ClientPlayNetworkHandler.class)
public class ClientCommandInterceptor {
    @Inject(method = "sendChatCommand", at = @At("RETURN"))
    private void onSendCommand(String command, CallbackInfo ci) {
        TCClient.fireClientCommandReceivers(command);
    }
}