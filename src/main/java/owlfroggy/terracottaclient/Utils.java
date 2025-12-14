package owlfroggy.terracottaclient;

import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.Vec3i;

public class Utils {
    static Vec3d toVec3d(Vec3i vec) {
        return new Vec3d(
            (double) vec.getX(),
            (double) vec.getY(),
            (double) vec.getZ()
        );
    }
}
