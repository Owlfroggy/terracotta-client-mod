package owlfroggy.terracottaclient.api.message.impl;

import com.google.gson.JsonObject;
import net.minecraft.item.ItemStack;
import owlfroggy.terracottaclient.Utils;
import owlfroggy.terracottaclient.api.NotificationMethod;
import owlfroggy.terracottaclient.api.message.Notification;
import owlfroggy.terracottaclient.itemlibrary.ItemLibraryManager;

public class ItemImageChangedC2ANotification extends Notification {
    private ItemLibraryManager.ItemId itemId;
    private String image;

    public ItemImageChangedC2ANotification(ItemLibraryManager.ItemId itemId, String image) {
        super(NotificationMethod.ITEM_IMAGE_CHANGED);
        this.itemId = itemId;
        this.image = image;
    }

    @Override
    protected void buildOn(JsonObject out, JsonObject data) {
        super.buildOn(out, data);
        data.addProperty("workspace_path",itemId.workspace());
        data.addProperty("library_id",itemId.library());
        data.addProperty("item_id",itemId.item());
        data.addProperty("image",image);
    }
}
