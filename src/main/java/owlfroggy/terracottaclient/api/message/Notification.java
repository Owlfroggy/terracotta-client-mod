package owlfroggy.terracottaclient.api.message;

import com.google.gson.JsonObject;
import owlfroggy.terracottaclient.api.NotificationMethod;
import owlfroggy.terracottaclient.api.Permission;
import owlfroggy.terracottaclient.api.RequestMethod;

public class Notification extends Message {
    NotificationMethod method;

    public Notification(NotificationMethod method, Permission requiredPermission) {
        super(MessageType.NOTIFICATION, requiredPermission);
        this.method = method;
    }
    public Notification(NotificationMethod method) { this(method, null); }

    @Override
    protected void buildOn(JsonObject out, JsonObject data) {
        super.buildOn(out, data);
        out.addProperty("method",method.name());
    }
}
