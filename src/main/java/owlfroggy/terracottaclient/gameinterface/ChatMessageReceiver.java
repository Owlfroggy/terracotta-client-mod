package owlfroggy.terracottaclient.gameinterface;

import net.minecraft.text.Text;

public interface ChatMessageReceiver {
    public void onChatMessage(Text message);
}
