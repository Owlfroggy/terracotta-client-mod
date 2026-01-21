package owlfroggy.terracottaclient.api.message.impl;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import org.w3c.dom.Text;
import owlfroggy.terracottaclient.api.message.Response;

import java.sql.Array;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class GetInventoryC2AResponse extends Response {
    public record ItemEntry(String itemName, String itemSnbt) {}

    public HashMap<Integer,ItemEntry> items;

    public GetInventoryC2AResponse(HashMap<Integer, ItemEntry> items) {
        super();
        this.items = items;
    }

    @Override
    protected void buildOn(JsonObject out, JsonObject data) {
        super.buildOn(out, data);
        JsonObject serializedItems = new JsonObject();
        for (int slot : items.keySet()) {
            ItemEntry entry = items.get(slot);
            JsonObject sItem = new JsonObject();
            sItem.addProperty("snbt",entry.itemSnbt);
            sItem.addProperty("name",entry.itemName);
            serializedItems.add(String.valueOf(slot), sItem);
        }
        data.add("items",serializedItems);
    }
}
