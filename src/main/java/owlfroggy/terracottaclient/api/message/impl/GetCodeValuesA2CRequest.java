package owlfroggy.terracottaclient.api.message.impl;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import owlfroggy.terracottaclient.api.Permission;
import owlfroggy.terracottaclient.api.RequestMethod;
import owlfroggy.terracottaclient.api.message.Request;

public class GetCodeValuesA2CRequest extends Request {
    public GetCodeValuesA2CRequest() {
        super(RequestMethod.GET_CODE_VALUES, Permission.GET_INVENTORY);
    }

    public static GetCodeValuesA2CRequest parse(JsonObject message, JsonObject data) {
        return new GetCodeValuesA2CRequest();
    }
}
