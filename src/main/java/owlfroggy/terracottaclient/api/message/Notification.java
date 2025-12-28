package owlfroggy.terracottaclient.api.message;

import com.google.gson.JsonObject;
import owlfroggy.terracottaclient.api.NotificationMethod;

public class Notification extends Message {
    NotificationMethod method;

    public Notification(NotificationMethod method) {
        super(MessageType.NOTIFICATION);
        this.method = method;
    }

    @Override
    protected void buildOn(JsonObject out, JsonObject data) {
        super.buildOn(out, data);
        out.addProperty("method",method.name());
    }
}
