package owlfroggy.terracottaclient.api.message.impl;

import com.google.gson.JsonObject;
import owlfroggy.terracottaclient.api.message.Response;

public class RenderItemC2AResponse extends Response {
    private String image;

    public RenderItemC2AResponse(String image) {
        super();
        this.image = image;
    }

    @Override
    protected void buildOn(JsonObject out, JsonObject data) {
        super.buildOn(out, data);
        data.addProperty("image", image);
    }
}
