package owlfroggy.terracottaclient.codespacemanager;

import java.util.HashMap;

public class CodespaceRow {
    /**
     * Key is Z axis plot-space position of the template
     */
    public final HashMap<java.lang.Integer, CachedTemplate> templates = new HashMap<>();
    public final int xPos;
    private final CodespaceFloor parentFloor;

    public CodespaceRow(int xPos, CodespaceFloor parentFloor) {
        this.xPos = xPos;
        this.parentFloor = parentFloor;
    }

    public void addTemplate(CachedTemplate template) {
        templates.put(template.plotSpacePos.getZ(), template);
    }

    public void removeTemplate(CachedTemplate template) {
        templates.remove(template.plotSpacePos.getZ(),template);
    }

    public CachedTemplate[] getTemplates() {
        return templates.values().toArray(new CachedTemplate[0]);
    }

    public String toString() {
        return "row["+templates.toString()+"]";
    }
}
