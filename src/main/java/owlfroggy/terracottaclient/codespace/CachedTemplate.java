package owlfroggy.terracottaclient.codespace;

import net.minecraft.util.math.Vec3i;

public class CachedTemplate {
    public TemplateIdentifier id;
    public Vec3i plotSpacePos;

    public CachedTemplate(TemplateType type, String name, Vec3i plotSpacePos) {
        this.id = new TemplateIdentifier(type, name);
        this.plotSpacePos = plotSpacePos;
    }

    @Override
    public String toString() {
        return "CachedTemplate{" +
        "type=" + id.type() +
        ", name='" + id.name() + '\'' +
        ", plotSpacePos=" + plotSpacePos +
        '}';
    }
}
