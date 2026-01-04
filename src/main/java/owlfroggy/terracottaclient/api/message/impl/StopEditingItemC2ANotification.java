package owlfroggy.terracottaclient.api.message.impl;

import com.google.gson.JsonObject;
import owlfroggy.terracottaclient.api.NotificationMethod;
import owlfroggy.terracottaclient.api.message.Notification;
import owlfroggy.terracottaclient.itemlibrary.ItemLibraryManager;

public class StopEditingItemC2ANotification extends Notification{
    private ItemLibraryManager.ItemId itemId;

    public StopEditingItemC2ANotification(ItemLibraryManager.ItemId itemId) {
        super(NotificationMethod.STOP_EDITING_ITEM);
        this.itemId = itemId;
    }

    @Override
    protected void buildOn(JsonObject out, JsonObject data) {
        super.buildOn(out, data);
        data.addProperty("workspace_path", itemId.workspace());
        data.addProperty("library_id", itemId.library());
        data.addProperty("item_id", itemId.item());
    }
}
