package owlfroggy.terracottaclient.codespacemanager;

import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.Vec3i;

public class CachedTemplate {
    public TemplateType type;
    public String name;
    public Vec3i plotSpacePos;

    public CachedTemplate(TemplateType type, String name, Vec3i plotSpacePos) {
        this.type = type;
        this.name = name;
        this.plotSpacePos = plotSpacePos;
    }

    @Override
    public String toString() {
        return "CachedTemplate{" +
        "type=" + type +
        ", name='" + name + '\'' +
        ", plotSpacePos=" + plotSpacePos +
        '}';
    }
}
