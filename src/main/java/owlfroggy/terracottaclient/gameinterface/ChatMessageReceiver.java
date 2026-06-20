package owlfroggy.terracottaclient.gameinterface;

import net.minecraft.network.chat.Component;

public interface ChatMessageReceiver {
    public void onChatMessage(Component message);
}
