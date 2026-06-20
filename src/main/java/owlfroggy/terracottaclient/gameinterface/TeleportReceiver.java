package owlfroggy.terracottaclient.gameinterface;

import net.minecraft.world.phys.Vec3;

public interface TeleportReceiver {
    public void onTeleported(Vec3 newPos, Vec3 oldPos);
}
