package owlfroggy.terracottaclient.api.message.impl;

import com.google.gson.JsonObject;
import owlfroggy.terracottaclient.api.message.Response;

public class StartEditingItemC2AResponse extends Response {
    public StartEditingItemC2AResponse() {
        super();
    }

    @Override
    protected void buildOn(JsonObject out, JsonObject data) { super.buildOn(out, data); }
}
