package owlfroggy.terracottaclient.gameinterface;

import net.minecraft.client.Minecraft;

public interface TickEndReceiver {
    public void onTickEnd(Minecraft client);
}
