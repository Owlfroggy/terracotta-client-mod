package owlfroggy.terracottaclient.api.message.impl;

import com.google.gson.JsonObject;
import owlfroggy.terracottaclient.api.RequestMethod;
import owlfroggy.terracottaclient.api.message.Request;

public class ProvideTokenA2CRequest extends Request {
    private String token;

    public String getToken() { return token; }

    public ProvideTokenA2CRequest(String token) {
        super(RequestMethod.PROVIDE_TOKEN);
        this.token = token;
    }

    public static Request parse(JsonObject message, JsonObject data) {
        return new ProvideTokenA2CRequest(data.get("token").getAsString());
    }
}
