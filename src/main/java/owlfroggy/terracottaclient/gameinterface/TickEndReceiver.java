package owlfroggy.terracottaclient.gameinterface;

import net.minecraft.client.MinecraftClient;

public interface TickEndReceiver {
    public void onTickEnd(MinecraftClient client);
}
