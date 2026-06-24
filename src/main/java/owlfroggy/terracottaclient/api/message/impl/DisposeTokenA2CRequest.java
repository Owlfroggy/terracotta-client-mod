package owlfroggy.terracottaclient.api.message.impl;

import com.google.gson.JsonObject;
import owlfroggy.terracottaclient.api.Permission;
import owlfroggy.terracottaclient.api.RequestMethod;
import owlfroggy.terracottaclient.api.message.Request;

public class DisposeTokenA2CRequest extends Request {

    public DisposeTokenA2CRequest() {
        super(RequestMethod.DISPOSE_TOKEN);
    }

    public static DisposeTokenA2CRequest parse(JsonObject message, JsonObject data) {
        return new DisposeTokenA2CRequest();
    }
}