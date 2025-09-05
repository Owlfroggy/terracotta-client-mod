package owlfroggy.terracottaclient;

import net.minecraft.client.MinecraftClient;
import owlfroggy.terracottaclient.gameinterface.TickEndReceiver;

import java.util.PriorityQueue;

public class ChatCommandManager extends Manager implements TickEndReceiver {
    private final PriorityQueue<String> commandQueue = new PriorityQueue<>();

    public void queueCommand(String command) {
        commandQueue.add(command);
    }

    public void onTickEnd(MinecraftClient client) {
        if (TCClient.MCI.getNetworkHandler() == null) return;

        String command = commandQueue.poll();
        if (command == null) return;

        TCClient.MCI.getNetworkHandler().sendChatCommand(command);
    }
}
