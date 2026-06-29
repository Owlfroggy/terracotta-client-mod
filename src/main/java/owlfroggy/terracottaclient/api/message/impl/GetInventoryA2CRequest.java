package owlfroggy.terracottaclient.api.message.impl;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import owlfroggy.terracottaclient.DFState;
import owlfroggy.terracottaclient.api.Permission;
import owlfroggy.terracottaclient.api.RequestMethod;
import owlfroggy.terracottaclient.api.message.Request;

public class GetInventoryA2CRequest extends Request {
    public boolean excludeDFValues;

    public boolean shouldExcludeDFValues() { return excludeDFValues; }

    public GetInventoryA2CRequest(boolean excludeDFValues) {
        super(RequestMethod.GET_INVENTORY, Permission.GET_INVENTORY);
        this.excludeDFValues = excludeDFValues;
    }

    public static GetInventoryA2CRequest parse(JsonObject message, JsonObject data) {
        boolean excludeDFValues = false;
        JsonElement excludeElm = data.get("exclude_df_values");
        if (excludeElm != null && excludeElm.isJsonPrimitive() && excludeElm.getAsJsonPrimitive().isBoolean()) {
            excludeDFValues = excludeElm.getAsBoolean();
        }
        return new GetInventoryA2CRequest(excludeDFValues);
    }
}
