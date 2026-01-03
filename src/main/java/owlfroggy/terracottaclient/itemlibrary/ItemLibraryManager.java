package owlfroggy.terracottaclient.itemlibrary;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.serialization.DataResult;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.NbtComponent;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.StringNbtReader;
import net.minecraft.network.packet.c2s.play.CreativeInventoryActionC2SPacket;
import net.minecraft.screen.slot.Slot;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import owlfroggy.terracottaclient.Manager;
import owlfroggy.terracottaclient.TCClient;
import owlfroggy.terracottaclient.Utils;

import java.util.HashMap;
import java.util.Optional;

public class ItemLibraryManager extends Manager {
    public record ItemId(String workspace, String library, String item) {}
    public record LibraryItemData(int appId, ItemId itemId) {}

    private static final String CUSTOM_DATA_KEY = Identifier.of(TCClient.MOD_ID,"library_data").toString();

    /** The value is the app id of the app that's editing this item*/
    public HashMap<ItemId, java.lang.Integer> itemIdsBeingEdited = new HashMap<>();

    @Nullable
    public LibraryItemData getLibraryData(ItemStack item) {
        if (item.isEmpty()) return null;

        NbtComponent customDataComp = item.getOrDefault(DataComponentTypes.CUSTOM_DATA, null);
        if (customDataComp == null) return null;
        NbtCompound nbt = customDataComp.copyNbt();

        Optional<NbtCompound> customDataO = nbt.getCompound(CUSTOM_DATA_KEY);
        if (customDataO.isEmpty()) return null;

        NbtCompound customData = customDataO.get();
        int appId = customData.getInt("app_id",-1);
        String workspace = customData.getString("workspace",null);
        String libraryId = customData.getString("library_id",null);
        String itemId = customData.getString("item_id",null);
        if (
            appId == -1
            || workspace == null
            || libraryId == null
            || itemId == null
        ) return null;

        return new LibraryItemData(appId, new ItemId(workspace,libraryId,itemId));
    }

    public void applySlotDecoration(DrawContext context, Slot slot, int mouseX, int mouseY, CallbackInfo info) {
        ItemStack item = slot.getStack();
        LibraryItemData libraryData = getLibraryData(item);

        if (libraryData == null) return;

        context.drawTexturedQuad(
            Identifier.of(TCClient.MOD_ID,"ui/library_slot.png"),
            slot.x-2, slot.y-2,
            slot.x + 18, slot.y + 18,
            0, 1,
            0, 1
        );
    }

    public void applyHotbarSlotDecoration(DrawContext context, int x, int y, ItemStack item, CallbackInfo info) {
        LibraryItemData libraryData = getLibraryData(item);

        if (libraryData == null) return;

        context.drawTexturedQuad(
            Identifier.of(TCClient.MOD_ID,"ui/library_hotbar_slot.png"),
            x-2, y-2,
            x + 18, y + 18,
            0, 1,
            0, 1
        );
    }

    public void applyHotbarSelectionDecoration(DrawContext context, RenderTickCounter tickCounter, CallbackInfo info) {
        int selectedSlot = TCClient.MCI.player.getInventory().getSelectedSlot();
        ItemStack item = TCClient.MCI.player.getInventory().getStack(selectedSlot);
        ItemLibraryManager.LibraryItemData libraryData = TCClient.ITEM_LIBRARY_MANAGER.getLibraryData(item);

        if (libraryData == null) return;

        int x1 = context.getScaledWindowWidth()/2 - 91 - 1 + selectedSlot * 20;
        int y1 = context.getScaledWindowHeight() - 22 - 1;
        context.drawTexturedQuad(
            Identifier.of(TCClient.MOD_ID,"ui/library_hotbar_selection.png"),
            x1, y1,
            x1 + 24, y1 + 23,
            0, 1,
            0, 1
        );
    }

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
        customData.put(CUSTOM_DATA_KEY,terracottaDict);
        item.set(DataComponentTypes.CUSTOM_DATA, NbtComponent.of(customData));

        Utils.setItemInSlot(slot, item);
    }
}
