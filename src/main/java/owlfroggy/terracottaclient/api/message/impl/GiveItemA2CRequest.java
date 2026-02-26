package owlfroggy.terracottaclient.api.message.impl;

import com.google.gson.JsonObject;
import owlfroggy.terracottaclient.api.Permission;
import owlfroggy.terracottaclient.api.RequestMethod;
import owlfroggy.terracottaclient.api.message.Request;
import owlfroggy.terracottaclient.itemlibrary.ItemLibraryManager;

public class GiveItemA2CRequest extends Request {
    private String snbt;
    private int itemDataVersion;


    public String getSnbt() { return snbt; }
    public int getItemDataVersion() { return itemDataVersion; }

    public GiveItemA2CRequest(String snbt, int itemDataVersion) {
        super(RequestMethod.GIVE_ITEM, Permission.GIVE_ITEMS);
        this.snbt = snbt;
        this.itemDataVersion = itemDataVersion;
    }

    public static GiveItemA2CRequest parse(JsonObject message, JsonObject data) {
        return new GiveItemA2CRequest(
            data.get("snbt").getAsString(),
            data.get("data_version").getAsInt()
        );
    }
}
