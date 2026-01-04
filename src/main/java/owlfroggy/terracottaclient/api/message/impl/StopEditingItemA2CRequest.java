package owlfroggy.terracottaclient.api.message.impl;

import com.google.gson.JsonObject;
import owlfroggy.terracottaclient.api.RequestMethod;
import owlfroggy.terracottaclient.api.message.Request;
import owlfroggy.terracottaclient.itemlibrary.ItemLibraryManager;

public class StopEditingItemA2CRequest extends Request {
    private ItemLibraryManager.ItemId itemId;
    public ItemLibraryManager.ItemId getItemId() { return itemId; }

    public StopEditingItemA2CRequest(ItemLibraryManager.ItemId itemId) {
        super(RequestMethod.STOP_EDITING_ITEM);
        this.itemId = itemId;
    }

    public static StopEditingItemA2CRequest parse(JsonObject message, JsonObject data) {
        return new StopEditingItemA2CRequest(
            new ItemLibraryManager.ItemId(
                data.get("workspace_path").getAsString(),
                data.get("library_id").getAsString(),
                data.get("item_id").getAsString()
            )
        );
    }
}
