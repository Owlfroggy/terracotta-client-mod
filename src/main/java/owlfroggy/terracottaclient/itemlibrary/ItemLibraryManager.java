package owlfroggy.terracottaclient.itemlibrary;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.serialization.DataResult;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.InventoryScreen;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.client.util.InputUtil;
import net.minecraft.client.util.Window;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.NbtComponent;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.StringNbtReader;
import net.minecraft.screen.slot.Slot;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.glfw.GLFW;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import owlfroggy.terracottaclient.Manager;
import owlfroggy.terracottaclient.TCClient;
import owlfroggy.terracottaclient.Utils;
import owlfroggy.terracottaclient.api.APIServer;
import owlfroggy.terracottaclient.api.message.impl.ItemChangedC2ANotification;
import owlfroggy.terracottaclient.api.message.impl.StopEditingItemC2ANotification;
import owlfroggy.terracottaclient.gameinterface.TickEndReceiver;
import owlfroggy.terracottaclient.gameinterface.TooltipRenderer;

import java.util.*;
import java.util.function.Function;

public class ItemLibraryManager extends Manager
implements
    TooltipRenderer,
    TickEndReceiver
{
    public record ItemId(String workspace, String library, String item) {}
    public class LibraryItemEditData {
        public final int appId;
        public final int editId;
        public final ItemId itemId;
        public String lastSnbt;

        // Standard Constructor
        public LibraryItemEditData(int appId, int editId, ItemId itemId, String lastSnbt) {
            this.appId = appId;
            this.editId = editId;
            this.itemId = itemId;
            this.lastSnbt = lastSnbt;
        }
    }

    public static final String CUSTOM_DATA_KEY = Identifier.of(TCClient.MOD_ID,"library_data").toString();

    /** key: edit id */
    private int lastEditId = 0;
    private HashMap<java.lang.Integer, LibraryItemEditData> activeEdits = new HashMap<>();
    private HashMap<ItemId, LibraryItemEditData> activeEditsByItemId = new HashMap<>();

    @Nullable
    public ItemLibraryManager.LibraryItemEditData getLibraryData(ItemStack item) {
        if (item.isEmpty()) return null;

        NbtComponent customDataComp = item.getOrDefault(DataComponentTypes.CUSTOM_DATA, null);
        if (customDataComp == null) return null;
        NbtCompound nbt = customDataComp.copyNbt();

        Optional<NbtCompound> customDataO = nbt.getCompound(CUSTOM_DATA_KEY);
        if (customDataO.isEmpty()) return null;

        NbtCompound customData = customDataO.get();
        int editId = customData.getInt("edit_id",-1);
        int appId = customData.getInt("app_id",-1);
        if (editId == -1 || appId == -1) return null;

        LibraryItemEditData data = activeEdits.get(editId);
        if (data == null || data.appId != appId) return null;

        return data;
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

        lastEditId += 1+(int)(Math.random()*100);
        int editId = lastEditId;

        // add library item marker data
        NbtCompound terracottaDict = new NbtCompound();
        terracottaDict.putInt("edit_id",editId);
        terracottaDict.putInt("app_id",appId);

        NbtCompound customData = item.getOrDefault(DataComponentTypes.CUSTOM_DATA, NbtComponent.of(new NbtCompound())).copyNbt();
        customData.put(CUSTOM_DATA_KEY,terracottaDict);
        item.set(DataComponentTypes.CUSTOM_DATA, NbtComponent.of(customData));

        Utils.setItemInSlot(slot, item);

        LibraryItemEditData editData = new LibraryItemEditData(appId,editId,itemId,Utils.itemToSnbt(item));
        activeEdits.put(editId, editData);
        activeEditsByItemId.put(itemId, editData);
    }

    public void stopEditingItem(LibraryItemEditData editData) {
        int slot = -2;
        if (TCClient.MCI.player != null) {
            for (int s = -1; s <= 40; s++) {
                ItemStack thisItem;
                if (s == -1)
                    thisItem = TCClient.MCI.player.currentScreenHandler.getCursorStack();
                else
                    thisItem = TCClient.MCI.player.getInventory().getStack(s);

                LibraryItemEditData thisEditData = getLibraryData(thisItem);
                if (thisEditData == null) continue;
                if (thisEditData.editId == editData.editId) {
                    slot = s;
                    break;
                }
            }
        }

        if (APIServer.hasConnectedAppId(editData.appId)) {
            APIServer.sendNotification(editData.appId, new StopEditingItemC2ANotification(editData.itemId));
        }
        activeEdits.remove(editData.editId);
        activeEditsByItemId.remove(editData.itemId);
        if (slot == -1) {
            TCClient.MCI.player.currentScreenHandler.setCursorStack(ItemStack.EMPTY);
        } else if (slot >= 0) {
            TCClient.MCI.player.getInventory().setStack(slot,ItemStack.EMPTY);
        }
    }
    public void stopEditingItem(ItemId itemId) {
        stopEditingItem(activeEditsByItemId.get(itemId));
    }

    @Override
    public void onItemTooltip(ItemStack item, Item.TooltipContext context, TooltipType type, List<Text> lines) {
        LibraryItemEditData libraryData = getLibraryData(item);
        if (libraryData == null) return;

        Window handle = TCClient.MCI.getWindow();
        boolean isShiftDown = InputUtil.isKeyPressed(handle, GLFW.GLFW_KEY_LEFT_SHIFT) || InputUtil.isKeyPressed(handle, GLFW.GLFW_KEY_RIGHT_SHIFT);
        if (isShiftDown) {
            lines.add(Text.literal("(terracotta library item,").formatted(Formatting.DARK_GRAY));
            lines.add(Text.literal("edits are being synced live)").formatted(Formatting.DARK_GRAY));
            lines.add(
                Text.literal("Library ID: ").formatted(Formatting.DARK_GRAY)
                .append(Text.literal(libraryData.itemId.library).formatted(Formatting.GRAY))
            );
            lines.add(
                Text.literal("Item ID: ").formatted(Formatting.DARK_GRAY)
                .append(Text.literal(libraryData.itemId.item).formatted(Formatting.GRAY))
            );
        }
    }

    private final Set<LibraryItemEditData> seenEdits = new HashSet<>();

    @Override
    public void onTickEnd(MinecraftClient client) {
        if (TCClient.MCI.player == null) return;

        seenEdits.clear();

        for (int slot = -1; slot <= 40; slot++) {
            ItemStack item;
            if (slot == -1)
                item = TCClient.MCI.player.currentScreenHandler.getCursorStack();
            else
                item = TCClient.MCI.player.getInventory().getStack(slot);

            LibraryItemEditData editData = getLibraryData(item);
            if (editData == null) continue;

            seenEdits.add(editData);

            String oldSnbt = editData.lastSnbt;
            String newSnbt = Utils.itemToSnbt(item);

            if (!oldSnbt.equals(newSnbt)) {
                APIServer.sendNotification(editData.appId, new ItemChangedC2ANotification(
                    editData.itemId,
                    newSnbt
                ));
                editData.lastSnbt = newSnbt;
            }
        }

        // stop all edits whose items are no longer in the inventory
        for (LibraryItemEditData editData : activeEdits.values().stream().toList()) {
            if (
                !APIServer.hasConnectedAppId(editData.appId)
                || !seenEdits.contains(editData)
            ) {
                stopEditingItem(editData);
            }
        }
    }

    public void applySlotDecoration(DrawContext context, Slot slot, int mouseX, int mouseY, CallbackInfo info) {
        ItemStack item = slot.getStack();
        LibraryItemEditData libraryData = getLibraryData(item);

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
        LibraryItemEditData libraryData = getLibraryData(item);

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
        LibraryItemEditData libraryData = TCClient.ITEM_LIBRARY_MANAGER.getLibraryData(item);

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
}
