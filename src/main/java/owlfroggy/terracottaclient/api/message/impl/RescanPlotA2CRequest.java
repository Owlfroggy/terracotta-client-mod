package owlfroggy.terracottaclient.api.message.impl;

import com.google.gson.JsonObject;
import owlfroggy.terracottaclient.DFState;
import owlfroggy.terracottaclient.api.RequestMethod;
import owlfroggy.terracottaclient.api.message.Request;

public class RescanPlotA2CRequest extends Request {
    public RescanPlotA2CRequest() {
        super(RequestMethod.RESCAN_PLOT);
    }

    public static RescanPlotA2CRequest parse(JsonObject message, JsonObject data) {
        return new RescanPlotA2CRequest();
    }
}
