package owlfroggy.terracottaclient.itemlibrary;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.serialization.DataResult;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.DeltaTracker;
import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.platform.Window;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.TagParser;
import net.minecraft.world.inventory.Slot;
import net.minecraft.network.chat.Component;
import net.minecraft.ChatFormatting;
import net.minecraft.resources.Identifier;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.glfw.GLFW;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import owlfroggy.terracottaclient.Manager;
import owlfroggy.terracottaclient.TCClient;
import owlfroggy.terracottaclient.Utils;
import owlfroggy.terracottaclient.api.APIServer;
import owlfroggy.terracottaclient.api.message.impl.ItemChangedC2ANotification;
import owlfroggy.terracottaclient.api.message.impl.ItemImageChangedC2ANotification;
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

    public static final String CUSTOM_DATA_KEY = Identifier.fromNamespaceAndPath(TCClient.MOD_ID,"library_data").toString();

    /** key: edit id */
    private int lastEditId = 0;
    private HashMap<java.lang.Integer, LibraryItemEditData> activeEdits = new HashMap<>();
    private HashMap<ItemId, LibraryItemEditData> activeEditsByItemId = new HashMap<>();

    @Nullable
    public ItemLibraryManager.LibraryItemEditData getLibraryData(ItemStack item) {
        if (item.isEmpty()) return null;

        CustomData customDataComp = item.getOrDefault(DataComponents.CUSTOM_DATA, null);
        if (customDataComp == null) return null;
        CompoundTag nbt = customDataComp.copyTag();

        Optional<CompoundTag> customDataO = nbt.getCompound(CUSTOM_DATA_KEY);
        if (customDataO.isEmpty()) return null;

        CompoundTag customData = customDataO.get();
        int editId = customData.getIntOr("edit_id",-1);
        int appId = customData.getIntOr("app_id",-1);
        if (editId == -1 || appId == -1) return null;

        LibraryItemEditData data = activeEdits.get(editId);
        if (data == null || data.appId != appId) return null;

        return data;
    }

    public void startEditingItem(int appId, ItemId itemId, String itemData, int itemDataVersion) {
        //TODO: handle item version not matching client version
        int slot = TCClient.MCI.player.getInventory().getFreeSlot();
        if (slot == -1)
            throw new NoSpaceException("Not enough inventory space to start editing item.");

        ItemStack item = Utils.snbtToItem(itemData);

        lastEditId += 1+(int)(Math.random()*100);
        int editId = lastEditId;

        // add library item marker data
        CompoundTag terracottaDict = new CompoundTag();
        terracottaDict.putInt("edit_id",editId);
        terracottaDict.putInt("app_id",appId);

        CompoundTag customData = item.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.of(new CompoundTag())).copyTag();
        customData.put(CUSTOM_DATA_KEY,terracottaDict);
        item.set(DataComponents.CUSTOM_DATA, CustomData.of(customData));

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
                    thisItem = TCClient.MCI.player.containerMenu.getCarried();
                else
                    thisItem = TCClient.MCI.player.getInventory().getItem(s);

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
            TCClient.MCI.player.containerMenu.setCarried(ItemStack.EMPTY);
        } else if (slot >= 0) {
            TCClient.MCI.player.getInventory().setItem(slot, ItemStack.EMPTY);
        }
    }
    public void stopEditingItem(ItemId itemId) {
        stopEditingItem(activeEditsByItemId.get(itemId));
    }

    public void stopEditingAllItems() {
        for (LibraryItemEditData editData : activeEditsByItemId.values().stream().toList()) {
            stopEditingItem(editData);
        }
    }

    @Override
    public void onItemTooltip(ItemStack item, Item.TooltipContext context, TooltipFlag type, List<Component> lines) {
        LibraryItemEditData libraryData = getLibraryData(item);
        if (libraryData == null) return;

        Window handle = TCClient.MCI.getWindow();
        boolean isShiftDown = InputConstants.isKeyDown(handle, GLFW.GLFW_KEY_LEFT_SHIFT) || InputConstants.isKeyDown(handle, GLFW.GLFW_KEY_RIGHT_SHIFT);
        if (isShiftDown) {
            lines.add(Component.literal("(terracotta library item,").withStyle(ChatFormatting.DARK_GRAY));
            lines.add(Component.literal("edits are being synced live)").withStyle(ChatFormatting.DARK_GRAY));
            lines.add(
                Component.literal("Library ID: ").withStyle(ChatFormatting.DARK_GRAY)
                .append(Component.literal(libraryData.itemId.library).withStyle(ChatFormatting.GRAY))
            );
            lines.add(
                Component.literal("Item ID: ").withStyle(ChatFormatting.DARK_GRAY)
                .append(Component.literal(libraryData.itemId.item).withStyle(ChatFormatting.GRAY))
            );
        }
    }

    private final Set<LibraryItemEditData> seenEdits = new HashSet<>();

    @Override
    public void onTickEnd(Minecraft client) {
        if (TCClient.MCI.player == null) return;

        seenEdits.clear();

        for (int slot = -1; slot <= 40; slot++) {
            ItemStack item;
            if (slot == -1)
                item = TCClient.MCI.player.containerMenu.getCarried();
            else
                item = TCClient.MCI.player.getInventory().getItem(slot);

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
//                ItemRenderGenerator.renderToDataURI(item,2,image -> {
//                    APIServer.sendNotification(editData.appId, new ItemImageChangedC2ANotification(
//                        editData.itemId,
//                        image
//                    ));
//                });
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

//    public void applySlotDecoration(GuiGraphics context, Slot slot, int mouseX, int mouseY, CallbackInfo info) {
//        ItemStack item = slot.getItem();
//        LibraryItemEditData libraryData = getLibraryData(item);
//
//        if (libraryData == null) return;
//
//        context.blit(
//        Identifier.fromNamespaceAndPath(TCClient.MOD_ID,"ui/library_slot.png"),
//        slot.x-2, slot.y-2,
//        slot.x + 18, slot.y + 18,
//        0, 1,
//        0, 1
//        );
//    }
//
//    public void applyHotbarSlotDecoration(GuiGraphics context, int x, int y, ItemStack item, CallbackInfo info) {
//        LibraryItemEditData libraryData = getLibraryData(item);
//
//        if (libraryData == null) return;
//
//        context.blit(
//        Identifier.fromNamespaceAndPath(TCClient.MOD_ID,"ui/library_hotbar_slot.png"),
//        x-2, y-2,
//        x + 18, y + 18,
//        0, 1,
//        0, 1
//        );
//    }
//
//    public void applyHotbarSelectionDecoration(GuiGraphics context, DeltaTracker tickCounter, CallbackInfo info) {
//        int selectedSlot = TCClient.MCI.player.getInventory().getSelectedSlot();
//        ItemStack item = TCClient.MCI.player.getInventory().getItem(selectedSlot);
//        LibraryItemEditData libraryData = TCClient.ITEM_LIBRARY_MANAGER.getLibraryData(item);
//
//        if (libraryData == null) return;
//
//        int x1 = context.guiWidth()/2 - 91 - 1 + selectedSlot * 20;
//        int y1 = context.guiHeight() - 22 - 1;
//        context.blit(
//        Identifier.fromNamespaceAndPath(TCClient.MOD_ID,"ui/library_hotbar_selection.png"),
//        x1, y1,
//        x1 + 24, y1 + 23,
//        0, 1,
//        0, 1
//        );
//    }
}
