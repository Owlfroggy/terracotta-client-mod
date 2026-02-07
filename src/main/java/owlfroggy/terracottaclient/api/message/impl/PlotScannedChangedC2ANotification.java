package owlfroggy.terracottaclient.api.message.impl;

import com.google.gson.JsonObject;
import owlfroggy.terracottaclient.DFState;
import owlfroggy.terracottaclient.api.NotificationMethod;
import owlfroggy.terracottaclient.api.message.Notification;

public class PlotScannedChangedC2ANotification extends Notification {
    private final boolean isScanned;

    public PlotScannedChangedC2ANotification(boolean isScanned) {
        super(NotificationMethod.PLOT_SCANNED_CHANGED);
        this.isScanned = isScanned;
    }

    @Override
    protected void buildOn(JsonObject out, JsonObject data) {
        super.buildOn(out, data);
        data.addProperty("is_scanned",isScanned);
    }
}
