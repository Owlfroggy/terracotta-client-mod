package owlfroggy.terracottaclient.api.message;

import com.google.gson.JsonObject;
import owlfroggy.terracottaclient.api.Permission;

public class Message {
    private MessageType type;
    private Permission requiredPermission = null;
    private int id = -1;

    public MessageType getType() { return type; }
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public Message(MessageType type, Permission requiredPermission) {
        this.type = type;
        this.requiredPermission = requiredPermission;
    }

    public Permission getRequiredPermission() {
        return requiredPermission;
    }

    public String serialize() {
        JsonObject out = new JsonObject();
        JsonObject data = new JsonObject();
        out.add("data", data);
        buildOn(out,data);
        return out.toString();
    }

    protected void buildOn(JsonObject out, JsonObject data) {
        out.addProperty("id",id);
        out.addProperty("type",type.name());
    }
}
