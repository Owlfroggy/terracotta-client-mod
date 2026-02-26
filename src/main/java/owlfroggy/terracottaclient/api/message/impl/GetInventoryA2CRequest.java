package owlfroggy.terracottaclient.api.message.impl;

import com.google.gson.JsonObject;
import owlfroggy.terracottaclient.DFState;
import owlfroggy.terracottaclient.api.Permission;
import owlfroggy.terracottaclient.api.RequestMethod;
import owlfroggy.terracottaclient.api.message.Request;

public class GetInventoryA2CRequest extends Request {

    public GetInventoryA2CRequest() {
        super(RequestMethod.GET_INVENTORY, Permission.GET_INVENTORY);
    }

    public static GetInventoryA2CRequest parse(JsonObject message, JsonObject data) {
        return new GetInventoryA2CRequest();
    }
}
