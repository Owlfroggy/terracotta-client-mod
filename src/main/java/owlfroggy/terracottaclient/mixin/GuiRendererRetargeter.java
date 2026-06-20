package owlfroggy.terracottaclient.mixin;

import com.mojang.blaze3d.pipeline.RenderTarget;
import net.minecraft.client.gui.render.GuiRenderer;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.state.GameRenderState;
import net.minecraft.client.renderer.state.WindowRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import owlfroggy.terracottaclient.itemrenderer.RetargetableRenderer;

@Mixin(GuiRenderer.class)
public class GuiRendererRetargeter {
    @Redirect(
        method = "draw",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/renderer/GameRenderer;mainRenderTarget()Lcom/mojang/blaze3d/pipeline/RenderTarget;"
        )
    )
    private RenderTarget redirectFramebuffer(GameRenderer instance) {
        // Instead of returning the client's main framebuffer, return the one from this GuiRenderer instance
        return ((GuiRenderer)(Object)this instanceof RetargetableRenderer r) ? r.getRenderTarget() : instance.mainRenderTarget();
    }

    @Redirect(
        method = "draw",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/renderer/GameRenderer;gameRenderState()Lnet/minecraft/client/renderer/state/GameRenderState;"
        )
    )
    private GameRenderState redirectRenderState(GameRenderer instance) {
        // this has to be done so that window size can be overridden
        return ((GuiRenderer)(Object)this instanceof RetargetableRenderer r) ? r.getGameRenderState() : instance.gameRenderState();
    }

    @Redirect(
        method = "getGuiScaleInvalidatingItemAtlasIfChanged",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/renderer/GameRenderer;gameRenderState()Lnet/minecraft/client/renderer/state/GameRenderState;"
        )
    )
    private GameRenderState redirectPIPRenderState(GameRenderer instance) {
        // this has to be done so that window size can be overridden
        return ((GuiRenderer)(Object)this instanceof RetargetableRenderer r) ? r.getGameRenderState() : instance.gameRenderState();
    }
}
