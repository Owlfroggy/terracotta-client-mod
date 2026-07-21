package owlfroggy.terracottaclient;

import net.minecraft.client.Minecraft;
import owlfroggy.terracottaclient.gameinterface.TickEndReceiver;

import java.util.PriorityQueue;

public class ChatCommandManager extends Manager implements TickEndReceiver {
    // commands will not be sent while commandCooldown > LIMIT
    private final static int LIMIT = 100;

    private final PriorityQueue<String> highPriorityQueue = new PriorityQueue<>();
    private final PriorityQueue<String> lowPriorityQueue = new PriorityQueue<>();
    private int rateLimitMeasure = 0;
    private int lowPriorityCooldown = 0;

    /**
     * @param command DO NOT INCLUDE LEADING SLASH!
     * @param lowPriority set to true for commands that can wait, like plot scanning teleports.
     *                    for more important commands (like /locate or /whois), leave this as false
     *
     *                    commands with lowPriority set will wait until there are no high priority commands
     *                    queued and will also have a cooldown applied
     */
    public void queueCommand(String command, boolean lowPriority) {
        if (lowPriority) {
            lowPriorityQueue.add(command);
        } else {
            highPriorityQueue.add(command);
        }
    }
    public void queueCommand(String command) {queueCommand(command, false);}
    public void queueCommandIfInMode(String command, DFState.Mode mode, boolean lowPriority) {
        if (TCClient.DF_STATE.getMode() == mode) {
            queueCommand(command, lowPriority);
        }
    }
    public void queueCommandIfInMode(String command, DFState.Mode mode) {
        queueCommandIfInMode(command, mode, false);
    }

    public boolean isCommandQueued(String commandToCheck) {
        return highPriorityQueue.contains(commandToCheck) || lowPriorityQueue.contains(commandToCheck);
    }

    public void onTickEnd(Minecraft client) {
        if (TCClient.MCI.getConnection() == null) return;
        if (rateLimitMeasure > 0) rateLimitMeasure--;
        if (lowPriorityCooldown > 0) lowPriorityCooldown--;

        if (rateLimitMeasure > LIMIT) return;

        String command = null;
        String priorityLogMessageString = "";
        if (!highPriorityQueue.isEmpty()) {
            command = highPriorityQueue.poll();
        }
        // only let low priority commands through if there are no high priority ones
        else if (lowPriorityCooldown <= 0) {
            command = lowPriorityQueue.poll();
            lowPriorityCooldown = 10;
            priorityLogMessageString = "low priority ";
        }

        if (command == null) return;

        TCClient.MCI.getConnection().sendCommand(command);
        TCClient.LOGGER.info("Sending {}chat command: {}", priorityLogMessageString, command);
        rateLimitMeasure += 20;
    }
}
