package owlfroggy.terracottaclient.mixin;


import com.mojang.blaze3d.buffers.GpuBufferSlice;
import net.minecraft.client.gui.render.GuiRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(GuiRenderer.class)
public interface GuiRendererAccessor {
    @Invoker("draw")
    void invokeRenderPreparedDraws();
    @Invoker("prepare")
    void invokePrepare();
}
