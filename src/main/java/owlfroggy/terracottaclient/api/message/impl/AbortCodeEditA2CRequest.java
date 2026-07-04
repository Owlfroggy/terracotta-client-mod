package owlfroggy.terracottaclient.api.message.impl;

import com.google.gson.JsonObject;
import owlfroggy.terracottaclient.DFState;
import owlfroggy.terracottaclient.api.Permission;
import owlfroggy.terracottaclient.api.RequestMethod;
import owlfroggy.terracottaclient.api.message.Request;

public class AbortCodeEditA2CRequest extends Request {
    public AbortCodeEditA2CRequest() {
        super(RequestMethod.ABORT_CODE_EDIT, Permission.EDIT_CODE);
    }

    public static AbortCodeEditA2CRequest parse(JsonObject message, JsonObject data) {
        return new AbortCodeEditA2CRequest();
    }
}
