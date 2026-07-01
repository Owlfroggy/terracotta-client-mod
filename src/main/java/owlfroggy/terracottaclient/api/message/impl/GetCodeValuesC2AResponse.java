package owlfroggy.terracottaclient.api.message.impl;

import com.google.gson.JsonObject;
import owlfroggy.terracottaclient.api.message.Response;

import java.util.HashMap;

public class GetCodeValuesC2AResponse extends Response {
    public HashMap<Integer,String> values;

    public GetCodeValuesC2AResponse(HashMap<Integer, String> items) {
        super();
        this.values = items;
    }

    @Override
    protected void buildOn(JsonObject out, JsonObject data) {
        super.buildOn(out, data);
        JsonObject serializedItems = new JsonObject();
        for (int slot : values.keySet()) {
            String entry = values.get(slot);
            serializedItems.addProperty(String.valueOf(slot), entry);
        }
        data.add("values",serializedItems);
    }
}
