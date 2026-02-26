package owlfroggy.terracottaclient.api.message.impl;

import com.google.gson.JsonObject;
import owlfroggy.terracottaclient.DFState;
import owlfroggy.terracottaclient.api.NotificationMethod;
import owlfroggy.terracottaclient.api.Permission;
import owlfroggy.terracottaclient.api.message.Notification;

public class PlotChangedC2ANotification extends Notification {
    private int plotId;
    private String plotName;

    public PlotChangedC2ANotification(int plotId, String plotName) {
        super(NotificationMethod.PLOT_CHANGED, Permission.GET_PLOT_INFO);
        this.plotId = plotId;
        this.plotName = plotName;
    }

    @Override
    protected void buildOn(JsonObject out, JsonObject data) {
        super.buildOn(out, data);
        data.addProperty("plot_id",plotId);
        data.addProperty("plot_name",plotName);
    }
}
