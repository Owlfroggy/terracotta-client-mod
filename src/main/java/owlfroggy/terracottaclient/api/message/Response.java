package owlfroggy.terracottaclient.api.message;

import com.google.gson.JsonObject;

public class Response extends Message {
    int id;
    boolean success = true;

    public Response() {
        super(MessageType.RESPONSE, null);
    }

    @Override
    protected void buildOn(JsonObject out, JsonObject data) {
        super.buildOn(out, data);
        out.addProperty("success",success);
    }
}
