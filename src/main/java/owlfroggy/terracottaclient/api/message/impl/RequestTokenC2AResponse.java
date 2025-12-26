package owlfroggy.terracottaclient.api.message.impl;

import com.google.gson.JsonObject;
import owlfroggy.terracottaclient.api.message.Response;

public class RequestTokenC2AResponse extends Response {
    private String token;

    public RequestTokenC2AResponse(String token) {
        super();
        this.token = token;
    }

    @Override
    protected void buildOn(JsonObject out, JsonObject data) {
        super.buildOn(out, data);
        data.addProperty("token",token);
    }
}
