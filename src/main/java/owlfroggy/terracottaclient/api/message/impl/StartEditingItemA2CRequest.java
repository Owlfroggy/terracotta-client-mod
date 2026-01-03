package owlfroggy.terracottaclient.api.message.impl;

import com.google.gson.JsonObject;
import owlfroggy.terracottaclient.api.RequestMethod;
import owlfroggy.terracottaclient.api.message.Request;
import owlfroggy.terracottaclient.itemlibrary.ItemLibraryManager;

public class StartEditingItemA2CRequest extends Request {
    private ItemLibraryManager.ItemId itemId;
    private String snbt;
    private int itemDataVersion;


    public ItemLibraryManager.ItemId getItemId() { return itemId; }
    public String getSnbt() { return snbt; }
    public int getItemDataVersion() { return itemDataVersion; }

    public StartEditingItemA2CRequest(ItemLibraryManager.ItemId itemId, String snbt, int itemDataVersion) {
        super(RequestMethod.START_EDITING_ITEM);
        this.itemId = itemId;
        this.snbt = snbt;
        this.itemDataVersion = itemDataVersion;
    }

    public static StartEditingItemA2CRequest parse(JsonObject message, JsonObject data) {
        return new StartEditingItemA2CRequest(
            new ItemLibraryManager.ItemId(
                data.get("workspace_path").getAsString(),
                data.get("library_id").getAsString(),
                data.get("item_id").getAsString()
            ),
            data.get("snbt").getAsString(),
            data.get("data_version").getAsInt()
        );
    }
}
