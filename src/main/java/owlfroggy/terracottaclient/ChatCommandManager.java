package owlfroggy.terracottaclient;

import net.minecraft.client.Minecraft;
import owlfroggy.terracottaclient.gameinterface.TickEndReceiver;

import java.util.PriorityQueue;

public class ChatCommandManager extends Manager implements TickEndReceiver {
    // commands will not be sent while commandCooldown > LIMIT
    private final static int LIMIT = 100;

    private final PriorityQueue<String> commandQueue = new PriorityQueue<>();
    private int commandCooldown = 0;

    /**
     * @param command DO NOT INCLUDE LEADING SLASH!
     */
    public void queueCommand(String command) {
        commandQueue.add(command);
    }
    public void queueCommandIfInImode(String command, DFState.Mode mode) {
        if (TCClient.DF_STATE.getMode() == mode) {
            queueCommand(command);
        }
    }

    public void onTickEnd(Minecraft client) {
        if (TCClient.MCI.getConnection() == null) return;
        if (commandCooldown > 0) {
            commandCooldown--;
        }
        if (commandCooldown > LIMIT) {
            return;
        }

        String command = commandQueue.poll();
        if (command == null) return;

        TCClient.MCI.getConnection().sendCommand(command);
        TCClient.LOGGER.info("Sending chat command: "+command);
        commandCooldown += 20;
    }
}
