package owlfroggy.terracottaclient.gameinterface;

import net.minecraft.util.math.Vec3d;

public interface TeleportReceiver {
    public void onTeleported(Vec3d newPos, Vec3d oldPos);
}
