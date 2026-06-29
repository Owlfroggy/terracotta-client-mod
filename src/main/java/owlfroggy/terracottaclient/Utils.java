package owlfroggy.terracottaclient;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.serialization.DataResult;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.TagParser;
import net.minecraft.network.protocol.game.ServerboundSetCreativeModeSlotPacket;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.resources.Identifier;
import net.minecraft.world.phys.Vec3;
import net.minecraft.core.Vec3i;
import org.jetbrains.annotations.Nullable;
import owlfroggy.terracottaclient.itemlibrary.InvalidNBTException;
import owlfroggy.terracottaclient.itemlibrary.ItemLibraryManager;

import javax.swing.text.html.Option;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;

public class Utils {
    /**
     * @param slot Slot index according to the PlayerInventory class' mappings
     * @param item The item to set in this slot
     * @param silent If true, send the server packet only and do not update the client's inv
     */
    public static void setItemInSlot(int slot, ItemStack item, boolean silent) {
        if (!silent) TCClient.MCI.player.getInventory().setItem(slot,item);
        // whoever at mojang decided that slot indexes are different in the creative inventory
        // packet needs to be SMONGULATED!!!!!! you just wasted HALF AN HOUR of my time!!!!
        if (slot == 40) slot = 45; // offhand slot
        if (slot >= 0 && slot <= 8) slot += 36; //hotbar
        TCClient.MCI.getConnection().send(new ServerboundSetCreativeModeSlotPacket(slot, item));
    }
    public static void setItemInSlot(int slot, ItemStack item) {
        setItemInSlot(slot,item,false);
    }

    public static Vec3 toVec3d(Vec3i vec) {
        return new Vec3(
            (double) vec.getX(),
            (double) vec.getY(),
            (double) vec.getZ()
        );
    }

    public static ItemStack applyReachToItem(ItemStack item, String attributeId) {
        ItemAttributeModifiers.Entry attribute = new ItemAttributeModifiers.Entry(
            Attributes.BLOCK_INTERACTION_RANGE,
            new AttributeModifier(Identifier.fromNamespaceAndPath(TCClient.MOD_ID, attributeId),64.0d, AttributeModifier.Operation.ADD_VALUE),
            // VV for testing placement failure
//            new EntityAttributeModifier(Identifier.of(TCClient.MOD_ID, attributeId),64.0d * ((Math.random() < 0.5) ? -1 : 1), EntityAttributeModifier.Operation.ADD_VALUE),
            EquipmentSlotGroup.OFFHAND,
            ItemAttributeModifiers.Display.attributeModifiers()
        );

        ItemAttributeModifiers root = new ItemAttributeModifiers(List.of(attribute));

        item.set(DataComponents.ATTRIBUTE_MODIFIERS, root);
        return item;
    }

    /**
     * Omits terracotta metadata from the outputted snbt
     */
    public static String itemToSnbt(ItemStack item) {
        ItemStack clone = item.copy();

        CustomData customData = clone.getOrDefault(DataComponents.CUSTOM_DATA, null);
        if (customData != null) {
            CompoundTag nbt = customData.copyTag();
            if (nbt.contains(ItemLibraryManager.CUSTOM_DATA_KEY))
                nbt.remove(ItemLibraryManager.CUSTOM_DATA_KEY);
            clone.set(DataComponents.CUSTOM_DATA, CustomData.of(nbt));
        }

        return ItemStack.CODEC.encodeStart(
            TCClient.MCI.player.registryAccess().createSerializationContext(NbtOps.INSTANCE),
            clone
        ).getOrThrow().toString();
    }

    public static ItemStack snbtToItem(String snbt) {
        // parse nbt
        CompoundTag nbt;
        try {
            nbt = TagParser.parseCompoundFully(snbt);
        } catch (CommandSyntaxException e) {
            throw new InvalidNBTException(e.getMessage());
        }

        // convert nbt to item
        DataResult<ItemStack> result = ItemStack.CODEC.parse(TCClient.MCI.player.registryAccess().createSerializationContext(NbtOps.INSTANCE), nbt);
        try {
            return result.getOrThrow();
        } catch (Exception e) {
            throw new InvalidNBTException(e.getMessage());
        }
    }

    @Nullable
    public static String getItemDFValData(ItemStack item) {
        try {
            return (
                item.get(DataComponents.CUSTOM_DATA)
                    .copyTag()
                    .getCompound("PublicBukkitValues").get()
                    .getString("hypercube:varitem").get()
            );
        } catch (NoSuchElementException | NullPointerException e) {
            return null;
        }
    }
}
