package owlfroggy.terracottaclient.api.message;

import com.google.gson.JsonObject;
import net.minidev.json.JSONObject;
import owlfroggy.terracottaclient.api.APIErrorCode;

public class ErrorResponse extends Response {
    private APIErrorCode code;
    private String errorMessage;

    public ErrorResponse(APIErrorCode code, String message) {
        super();
        this.success = false;
        this.code = code;
        this.errorMessage = message;
    }

    @Override
    protected void buildOn(JsonObject out, JsonObject data) {
        super.buildOn(out, data);
        data.addProperty("error_code", code.name());
        data.addProperty("error_message",errorMessage);
    }
}
