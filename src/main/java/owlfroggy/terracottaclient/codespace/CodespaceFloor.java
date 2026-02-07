package owlfroggy.terracottaclient.codespace;

import java.util.HashMap;

public class CodespaceFloor {
    /**
     * Key is X axis plot-space position of the row
     */
    public final HashMap<java.lang.Integer, CodespaceRow> rows = new HashMap<>();
    public final int yLevel;

    public CodespaceFloor(int yLevel) {
        this.yLevel = yLevel;
    }

    public CodespaceRow getRow(int pos) {
        if (rows.containsKey(pos)) {
            return rows.get(pos);
        } else {
            CodespaceRow row = new CodespaceRow(pos, this);
            rows.put(pos,row);
            return row;
        }
    }

    public String toString() {
        return "floor["+rows.toString()+"]";
    }
}
