package owlfroggy.terracottaclient.itemlibrary;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.serialization.DataResult;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.NbtComponent;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.StringNbtReader;
import net.minecraft.network.packet.c2s.play.CreativeInventoryActionC2SPacket;
import owlfroggy.terracottaclient.Manager;
import owlfroggy.terracottaclient.TCClient;
import owlfroggy.terracottaclient.Utils;

import java.util.HashMap;

public class ItemLibraryManager extends Manager {
    public record ItemId(String workspace, String library, String item) {}

    /** The value is the app id of the app that's editing this item*/
    public HashMap<ItemId, java.lang.Integer> itemIdsBeingEdited = new HashMap<>();

    public void startEditingItem(int appId, ItemId itemId, String itemData, int itemDataVersion) {
        //TODO: handle item version not matching client version
        //TODO: handle inventory being full
        int slot = TCClient.MCI.player.getInventory().getEmptySlot();
        if (slot == -1) {
            throw new NoSpaceException("Not enough inventory space to start editing item");
        }

        // parse nbt
        NbtCompound nbt;
        try {
            nbt = StringNbtReader.readCompound(itemData);
        } catch (CommandSyntaxException e) {
            throw new InvalidNBTException(e.getMessage());
        }

        // convert nbt to item
        DataResult<ItemStack> result = ItemStack.CODEC.parse(TCClient.MCI.world.getRegistryManager().getOps(NbtOps.INSTANCE), nbt);
        ItemStack item;
        try {
            item = result.getOrThrow();
        } catch (Exception e) {
            throw new InvalidNBTException(e.getMessage());
        }

        // add library item marker data
        NbtCompound terracottaDict = new NbtCompound();
        terracottaDict.putInt("app_id",appId);
        terracottaDict.putString("workspace", itemId.workspace);
        terracottaDict.putString("library_id", itemId.library);
        terracottaDict.putString("item_id", itemId.item);

        NbtCompound customData = item.getOrDefault(DataComponentTypes.CUSTOM_DATA, NbtComponent.of(new NbtCompound())).copyNbt();
        customData.put("terracotta_client_item_library_data",terracottaDict);
        item.set(DataComponentTypes.CUSTOM_DATA, NbtComponent.of(customData));

        Utils.setItemInSlot(slot, item);
    }
}
