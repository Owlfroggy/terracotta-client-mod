package owlfroggy.terracottaclient.api.message.impl;

import com.google.gson.JsonObject;
import owlfroggy.terracottaclient.DFState;
import owlfroggy.terracottaclient.api.NotificationMethod;
import owlfroggy.terracottaclient.api.Permission;
import owlfroggy.terracottaclient.api.message.Notification;

public class ModeChangedC2ANotification extends Notification {
    private final DFState.Mode newMode;

    public ModeChangedC2ANotification(DFState.Mode newMode) {
        super(NotificationMethod.MODE_CHANGED, Permission.GET_MODE_INFO);
        this.newMode = newMode;
    }

    @Override
    protected void buildOn(JsonObject out, JsonObject data) {
        super.buildOn(out, data);
        data.addProperty("new_mode",newMode.name());
    }
}
