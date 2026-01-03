package owlfroggy.terracottaclient.mixin;

import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.hud.InGameHud;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.slot.Slot;
import net.minecraft.util.Arm;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import owlfroggy.terracottaclient.TCClient;
import owlfroggy.terracottaclient.itemlibrary.ItemLibraryManager;

@Mixin(InGameHud.class)
public class HotbarItemDecorationDrawer {
    @Inject(method = "renderHotbar", at = @At(value = "RETURN"))
    private void init(DrawContext context, RenderTickCounter tickCounter, CallbackInfo info) {
        if (TCClient.MCI.player == null) return;

        int centerX = context.getScaledWindowWidth() / 2;

        for (int i = 0; i < 9; i++) {
            int x = centerX - 90 + i * 20 + 2;
            int y = context.getScaledWindowHeight() - 16 - 3;
            TCClient.ITEM_LIBRARY_MANAGER.applyHotbarSlotDecoration(context, x, y, TCClient.MCI.player.getInventory().getStack(i), info);
        }

        ItemStack hotbarItem = TCClient.MCI.player.getOffHandStack();
        if (!hotbarItem.isEmpty()) {
            int m = context.getScaledWindowHeight() - 16 - 3;
            if (TCClient.MCI.player.getMainArm().getOpposite() == Arm.LEFT) {
                TCClient.ITEM_LIBRARY_MANAGER.applyHotbarSlotDecoration(context, centerX - 91 - 26, m, hotbarItem, info);
            } else {
                TCClient.ITEM_LIBRARY_MANAGER.applyHotbarSlotDecoration(context, centerX + 91 + 10, m, hotbarItem, info);
            }
        }

        TCClient.ITEM_LIBRARY_MANAGER.applyHotbarSelectionDecoration(context, tickCounter, info);
    }
}