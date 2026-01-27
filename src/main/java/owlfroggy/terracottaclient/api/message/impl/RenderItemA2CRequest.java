package owlfroggy.terracottaclient.api.message.impl;

import com.google.gson.JsonObject;
import owlfroggy.terracottaclient.api.RequestMethod;
import owlfroggy.terracottaclient.api.message.Request;
import owlfroggy.terracottaclient.itemlibrary.ItemLibraryManager;

public class RenderItemA2CRequest extends Request {
    private String snbt;


    public String getSnbt() { return snbt; }

    public RenderItemA2CRequest(String snbt) {
        super(RequestMethod.RENDER_ITEM);
        this.snbt = snbt;
    }

    public static RenderItemA2CRequest parse(JsonObject message, JsonObject data) {
        return new RenderItemA2CRequest(
            data.get("snbt").getAsString()
        );
    }
}
