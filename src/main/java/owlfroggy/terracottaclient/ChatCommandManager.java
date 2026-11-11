package owlfroggy.terracottaclient;

import net.minecraft.client.Minecraft;
import org.jetbrains.annotations.NotNull;
import owlfroggy.terracottaclient.gameinterface.TickEndReceiver;

import java.util.PriorityQueue;

public class ChatCommandManager extends Manager implements TickEndReceiver {
    // commands will not be sent while commandCooldown > LIMIT
    private final static int LIMIT = 100;

    private record QueuedCommand(String command, DFState.Mode requiredMode) implements Comparable<QueuedCommand> {
        @Override
        public int compareTo(@NotNull ChatCommandManager.QueuedCommand o) {
            return command.compareTo(o.command);
        }
    }
    private final PriorityQueue<QueuedCommand> highPriorityQueue = new PriorityQueue<>();
    private final PriorityQueue<QueuedCommand> lowPriorityQueue = new PriorityQueue<>();
    private int rateLimitMeasure = 0;
    private int lowPriorityCooldown = 0;

    /**
     * @param command DO NOT INCLUDE LEADING SLASH!
     * @param lowPriority set to true for commands that can wait, like plot scanning teleports.
     *                    for more important commands (like /locate or /whois), leave this as false
     *
     *                    commands with lowPriority set will wait until there are no high priority commands
     *                    queued and will also have a cooldown applied
     * @param mode sets the mode required for the command to run (dev, build, play).
     *             set to null to ignore
     */
    public void queueCommand(String command, boolean lowPriority, DFState.Mode mode) {
        if (lowPriority) {
            lowPriorityQueue.add(new QueuedCommand(command, mode));
        } else {
            highPriorityQueue.add(new QueuedCommand(command, mode));
        }
    }
    public void queueCommand(String command) {queueCommand(command, false, null);}
    public void queueCommand(String command, boolean lowPriority) {queueCommand(command, lowPriority, null);}
    public void queueCommandIfInMode(String command, DFState.Mode mode) {
        queueCommandIfInMode(command, mode, false);
    }
    public void queueCommandIfInMode(String command, DFState.Mode mode, boolean lowPriority) {
        if (TCClient.DF_STATE.getMode() == mode) {
            queueCommand(command, lowPriority, mode);
        }
    }

    public boolean isCommandQueued(String commandToCheck) {
        return highPriorityQueue.contains(commandToCheck) || lowPriorityQueue.contains(commandToCheck);
    }

    public void onTickEnd(Minecraft client) {
        if (TCClient.MCI.getConnection() == null) return;
        if (rateLimitMeasure > 0) rateLimitMeasure--;
        if (lowPriorityCooldown > 0) lowPriorityCooldown--;

        if (rateLimitMeasure > LIMIT) return;

        QueuedCommand command = null;
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

        if (command == null || command.command == null) return;

        if (command.requiredMode != null && TCClient.DF_STATE.getMode() != command.requiredMode) return;
        TCClient.MCI.getConnection().sendCommand(command.command);
        TCClient.LOGGER.info("Sending {}chat command: {} in mode {}", priorityLogMessageString, command, TCClient.DF_STATE.getMode());
        rateLimitMeasure += 20;
    }


}
