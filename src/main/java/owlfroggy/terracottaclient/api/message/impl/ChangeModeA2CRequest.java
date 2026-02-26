package owlfroggy.terracottaclient.api.message.impl;

import com.google.gson.JsonObject;
import owlfroggy.terracottaclient.DFState;
import owlfroggy.terracottaclient.api.Permission;
import owlfroggy.terracottaclient.api.RequestMethod;
import owlfroggy.terracottaclient.api.message.Request;

public class ChangeModeA2CRequest extends Request {
    private DFState.Mode newMode;

    public DFState.Mode getMode() { return newMode; }

    public ChangeModeA2CRequest(DFState.Mode newMode) {
        super(RequestMethod.CHANGE_MODE, Permission.CHANGE_MODE);
        this.newMode = newMode;
    }

    public static ChangeModeA2CRequest parse(JsonObject message, JsonObject data) {
        return new ChangeModeA2CRequest(DFState.Mode.valueOf(data.get("new_mode").getAsString()));
    }
}
