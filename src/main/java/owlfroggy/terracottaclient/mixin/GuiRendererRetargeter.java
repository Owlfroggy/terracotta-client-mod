package owlfroggy.terracottaclient.mixin;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.Framebuffer;
import net.minecraft.client.gui.render.GuiRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import owlfroggy.terracottaclient.itemrenderer.RetargetableRenderer;

@Mixin(GuiRenderer.class)
public class GuiRendererRetargeter {
    @Redirect(
        method = "renderPreparedDraws(Lcom/mojang/blaze3d/buffers/GpuBufferSlice;)V",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/MinecraftClient;getFramebuffer()Lnet/minecraft/client/gl/Framebuffer;"
        )
    )
    private Framebuffer redirectFramebuffer(MinecraftClient instance) {
        // Instead of returning the client's main framebuffer, return the one from this GuiRenderer instance
        return ((GuiRenderer)(Object)this instanceof RetargetableRenderer r) ? r.getFramebuffer() : instance.getFramebuffer();
    }
}
