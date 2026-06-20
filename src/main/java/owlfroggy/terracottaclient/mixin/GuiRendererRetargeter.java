//package owlfroggy.terracottaclient.mixin;
//
//import net.minecraft.client.Minecraft;
//import com.mojang.blaze3d.pipeline.RenderTarget;
//import net.minecraft.client.gui.render.GuiRenderer;
//import org.spongepowered.asm.mixin.Mixin;
//import org.spongepowered.asm.mixin.Shadow;
//import org.spongepowered.asm.mixin.injection.At;
//import org.spongepowered.asm.mixin.injection.Redirect;
//import owlfroggy.terracottaclient.itemrenderer.RetargetableRenderer;
//
//@Mixin(GuiRenderer.class)
//public class GuiRendererRetargeter {
//    @Redirect(
//        method = "draw(Lcom/mojang/blaze3d/buffers/GpuBufferSlice;)V",
//        at = @At(
//            value = "INVOKE",
//            target = "Lnet/minecraft/client/Minecraft;getMainRenderTarget()Lcom/mojang/blaze3d/pipeline/RenderTarget;"
//        )
//    )
//    private RenderTarget redirectFramebuffer(Minecraft instance) {
//        // Instead of returning the client's main framebuffer, return the one from this GuiRenderer instance
//        return ((GuiRenderer)(Object)this instanceof RetargetableRenderer r) ? r.getFramebuffer() : instance.getMainRenderTarget();
//    }
//}
