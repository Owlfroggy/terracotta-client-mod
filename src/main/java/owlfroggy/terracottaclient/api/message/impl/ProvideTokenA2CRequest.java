package owlfroggy.terracottaclient.api.message.impl;

import owlfroggy.terracottaclient.api.RequestMethod;
import owlfroggy.terracottaclient.api.message.Request;

public class ProvideTokenA2CRequest extends Request {
    private String token;

    public String getToken() { return token; }

    public ProvideTokenA2CRequest(String token) {
        super(RequestMethod.PROVIDE_TOKEN);
        this.token = token;
    }
}
