package owlfroggy.terracottaclient;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.scoreboard.Team;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.text.TextColor;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.Vec3i;
import owlfroggy.terracottaclient.gameinterface.TeleportReceiver;
import owlfroggy.terracottaclient.gameinterface.TickEndReceiver;

import java.util.Objects;
import java.util.Optional;

public class MovementManager extends Manager implements TickEndReceiver, TeleportReceiver {
    enum MovementState {
        NOT_MOVING,
        /**
         *Move the player to a y level that's between the code chest and the next layer
         */
        AVOID_CODE,
        ALIGN_XZ,
        ALIGN_Y,
    }

    public static final double MOVEMENT_SPEED = 50; //50;

    private Vec3d destinationPos;
    private Vec3d assumedPlayerPos;
    private MovementState currentMovementState = MovementState.NOT_MOVING;
    private String currentMovementId = null;

    @Override
    public void onTickEnd(MinecraftClient client) {
        if (TCClient.MCI.player == null) return;
        if (TCClient.DF_STATE.getMode() != DFState.Mode.DEV) currentMovementState = MovementState.NOT_MOVING;
        if (currentMovementState == MovementState.NOT_MOVING) return;

        // skip avoid_code if we're already at that y level
        if (currentMovementState == MovementState.AVOID_CODE && assumedPlayerPos.y == assumedPlayerPos.y - (assumedPlayerPos.y%5) + 2.2) {
            currentMovementState = MovementState.ALIGN_XZ;
        }

        Vec3d targetPos;
        switch (currentMovementState) {
            case AVOID_CODE -> targetPos = new Vec3d(assumedPlayerPos.x, assumedPlayerPos.y - (assumedPlayerPos.y%5) + 2.2, assumedPlayerPos.z);
            case ALIGN_XZ -> targetPos = new Vec3d(destinationPos.x, assumedPlayerPos.y, destinationPos.z);
            case ALIGN_Y -> targetPos = new Vec3d(assumedPlayerPos.x, destinationPos.y, assumedPlayerPos.z);
            default -> { return; }
        }

        if (!TCClient.MCI.player.getAbilities().flying) {
            TCClient.MCI.player.getAbilities().flying = true;
            TCClient.MCI.player.sendAbilitiesUpdate();
        }

        double dist = targetPos.distanceTo(assumedPlayerPos);
        Vec3d movementVec = targetPos.subtract(assumedPlayerPos).normalize().multiply(Math.min(dist,MOVEMENT_SPEED));
        assumedPlayerPos = assumedPlayerPos.add(movementVec);
        TCClient.MCI.player.setPosition(assumedPlayerPos);

        // if we're already at the position, don't bother waiting around for extra ticks
        if (assumedPlayerPos.isInRange(destinationPos,0.01)) {
            currentMovementState = MovementState.NOT_MOVING;
            TCClient.MCI.player.setVelocity(0,0,0);
            return;
        }

        if (dist < MOVEMENT_SPEED) {
            currentMovementState = MovementState.values()[(currentMovementState.ordinal() + 1) % 4];
            TCClient.MCI.player.setVelocity(0,0,0);
        }
    }

    public void onTeleported(Vec3d newPos, Vec3d oldPos) {
        if (currentMovementState != MovementState.NOT_MOVING) {
            assumedPlayerPos = newPos;
            currentMovementState = MovementState.AVOID_CODE;
        }
    }

    /**
     * Note: this function assumes that the target is not in a block
     * @param plotSpaceDestination The position to move to
     */
    public void setMovementDestination(Vec3d plotSpaceDestination, String movementId) {
        if (TCClient.MCI.player == null) return;

        currentMovementId = movementId;
        currentMovementState = MovementState.AVOID_CODE;
        destinationPos = TCClient.DF_STATE.toWorldSpace(plotSpaceDestination);
        assumedPlayerPos = TCClient.MCI.player.getPos();
    }
    public void setMovementDestination(Vec3d plotSpaceDestination) {
        setMovementDestination(plotSpaceDestination, null);
    }
    public void setMovementDestination(Vec3i plotSpaceDestination, String movementId) {
        setMovementDestination(
            new Vec3d(plotSpaceDestination.getX(), plotSpaceDestination.getY(), plotSpaceDestination.getZ()),
            movementId
        );
    }
    public void setMovementDestination(Vec3i plotSpaceDestination) {
        setMovementDestination(plotSpaceDestination, null);
    }

    public void cancelMovement(String movementId) {
        if (currentMovementId != null && movementId != currentMovementId) { return; }
        currentMovementState = MovementState.NOT_MOVING;
    }
    public void cancelMovement() {
        cancelMovement(null);
    }

    public boolean isMoving() {
        return currentMovementState != MovementState.NOT_MOVING;
    }
    public String getCurrentMovementId() {
        return currentMovementId;
    }
}
