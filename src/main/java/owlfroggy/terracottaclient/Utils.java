package owlfroggy.terracottaclient;

import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.AttributeModifierSlot;
import net.minecraft.component.type.AttributeModifiersComponent;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtOps;
import net.minecraft.network.packet.c2s.play.CreativeInventoryActionC2SPacket;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.Vec3i;

import java.util.List;

public class Utils {
    /**
     * @param slot Slot index according to the PlayerInventory class' mappings
     * @param item The item to set in this slot
     * @param silent If true, send the server packet only and do not update the client's inv
     */
    public static void setItemInSlot(int slot, ItemStack item, boolean silent) {
        if (!silent) TCClient.MCI.player.getInventory().setStack(slot,item);
        // whoever at mojang decided that slot indexes are different in the creative inventory
        // packet needs to be SMONGULATED!!!!!! you just wasted HALF AN HOUR of my time!!!!
        if (slot == 40) slot = 45; // offhand slot
        if (slot >= 0 && slot <= 8) slot += 36; //hotbar
        TCClient.MCI.getNetworkHandler().sendPacket(new CreativeInventoryActionC2SPacket(slot, item));
    }
    public static void setItemInSlot(int slot, ItemStack item) {
        setItemInSlot(slot,item,false);
    }

    public static Vec3d toVec3d(Vec3i vec) {
        return new Vec3d(
            (double) vec.getX(),
            (double) vec.getY(),
            (double) vec.getZ()
        );
    }

    public static ItemStack applyReachToItem(ItemStack item, String attributeId) {
        AttributeModifiersComponent.Entry attribute = new AttributeModifiersComponent.Entry(
            EntityAttributes.BLOCK_INTERACTION_RANGE,
            new EntityAttributeModifier(Identifier.of(TCClient.MOD_ID, attributeId),64.0d, EntityAttributeModifier.Operation.ADD_VALUE),
            // VV for testing placement failure
//            new EntityAttributeModifier(Identifier.of(TCClient.MOD_ID, attributeId),64.0d * ((Math.random() < 0.5) ? -1 : 1), EntityAttributeModifier.Operation.ADD_VALUE),
            AttributeModifierSlot.OFFHAND,
            AttributeModifiersComponent.Display.getDefault()
        );

        AttributeModifiersComponent root = new AttributeModifiersComponent(List.of(attribute));

        item.set(DataComponentTypes.ATTRIBUTE_MODIFIERS, root);
        return item;
    }

    public static String itemToSnbt(ItemStack item) {
        return ItemStack.CODEC.encodeStart(
            TCClient.MCI.player.getRegistryManager().getOps(NbtOps.INSTANCE),
            item
        ).getOrThrow().toString();
    }
}
