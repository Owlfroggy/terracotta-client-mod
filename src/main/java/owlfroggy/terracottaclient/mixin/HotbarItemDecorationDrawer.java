package owlfroggy.terracottaclient.mixin;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.Hud;
import net.minecraft.client.DeltaTracker;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.entity.HumanoidArm;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import owlfroggy.terracottaclient.TCClient;

@Mixin(Hud.class)
public class HotbarItemDecorationDrawer {
    @Inject(method = "extractItemHotbar", at = @At(value = "RETURN"))
    private void init(GuiGraphicsExtractor context, DeltaTracker tickCounter, CallbackInfo info) {
        if (TCClient.MCI.player == null) return;

        int centerX = context.guiWidth() / 2;

        for (int i = 0; i < 9; i++) {
            int x = centerX - 90 + i * 20 + 2;
            int y = context.guiHeight() - 16 - 3;
            TCClient.ITEM_LIBRARY_MANAGER.applyHotbarSlotDecoration(context, x, y, TCClient.MCI.player.getInventory().getItem(i), info);
        }

        ItemStack hotbarItem = TCClient.MCI.player.getOffhandItem();
        if (!hotbarItem.isEmpty()) {
            int m = context.guiHeight() - 16 - 3;
            if (TCClient.MCI.player.getMainArm().getOpposite() == HumanoidArm.LEFT) {
                TCClient.ITEM_LIBRARY_MANAGER.applyHotbarSlotDecoration(context, centerX - 91 - 26, m, hotbarItem, info);
            } else {
                TCClient.ITEM_LIBRARY_MANAGER.applyHotbarSlotDecoration(context, centerX + 91 + 10, m, hotbarItem, info);
            }
        }

        TCClient.ITEM_LIBRARY_MANAGER.applyHotbarSelectionDecoration(context, tickCounter, info);
    }
}