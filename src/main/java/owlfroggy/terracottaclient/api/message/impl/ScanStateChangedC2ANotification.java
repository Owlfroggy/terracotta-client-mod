package owlfroggy.terracottaclient.api.message.impl;

import com.google.gson.JsonObject;
import owlfroggy.terracottaclient.DFState;
import owlfroggy.terracottaclient.api.NotificationMethod;
import owlfroggy.terracottaclient.api.Permission;
import owlfroggy.terracottaclient.api.message.Notification;

public class ScanStateChangedC2ANotification extends Notification {
    private final DFState.ScanState scanState;

    public ScanStateChangedC2ANotification(DFState.ScanState scanState) {
        super(NotificationMethod.SCAN_STATE_CHANGED, Permission.EDIT_CODE);
        this.scanState = scanState;
    }

    @Override
    protected void buildOn(JsonObject out, JsonObject data) {
        super.buildOn(out, data);
        data.addProperty("scan_state",scanState.name());
    }
}
