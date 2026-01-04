package owlfroggy.terracottaclient.api.message.impl;

import com.google.gson.JsonObject;
import owlfroggy.terracottaclient.api.NotificationMethod;
import owlfroggy.terracottaclient.api.message.Notification;
import owlfroggy.terracottaclient.itemlibrary.ItemLibraryManager;

public class ItemChangedC2ANotification extends Notification {
    private ItemLibraryManager.ItemId itemId;
    private String snbt;

    public ItemChangedC2ANotification(ItemLibraryManager.ItemId itemId, String snbt) {
        super(NotificationMethod.ITEM_CHANGED);
        this.itemId = itemId;
        this.snbt = snbt;
    }

    @Override
    protected void buildOn(JsonObject out, JsonObject data) {
        super.buildOn(out, data);
        data.addProperty("workspace_path",itemId.workspace());
        data.addProperty("library_id",itemId.library());
        data.addProperty("item_id",itemId.item());
        data.addProperty("snbt",snbt);
    }
}
