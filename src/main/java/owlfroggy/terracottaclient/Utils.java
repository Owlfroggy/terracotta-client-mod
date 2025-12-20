package owlfroggy.terracottaclient;

import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.AttributeModifierSlot;
import net.minecraft.component.type.AttributeModifiersComponent;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.Vec3i;

import java.util.List;

public class Utils {
    static Vec3d toVec3d(Vec3i vec) {
        return new Vec3d(
            (double) vec.getX(),
            (double) vec.getY(),
            (double) vec.getZ()
        );
    }

    static ItemStack applyReachToItem(ItemStack item, String attributeId) {
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
}
