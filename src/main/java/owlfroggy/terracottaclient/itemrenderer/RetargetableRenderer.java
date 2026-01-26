package owlfroggy.terracottaclient.itemrenderer;

import net.minecraft.client.gl.Framebuffer;
import net.minecraft.client.gui.render.GuiRenderer;
import net.minecraft.client.gui.render.SpecialGuiElementRenderer;
import net.minecraft.client.gui.render.state.GuiRenderState;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.command.OrderedRenderCommandQueue;
import net.minecraft.client.render.command.RenderDispatcher;

import java.util.List;

public class RetargetableRenderer extends GuiRenderer {
    private Framebuffer framebuffer;

    public Framebuffer getFramebuffer() { return framebuffer; }

    public RetargetableRenderer(Framebuffer framebuffer, GuiRenderState state, VertexConsumerProvider.Immediate vertexConsumers, OrderedRenderCommandQueue queue, RenderDispatcher dispatcher, List<SpecialGuiElementRenderer<?>> specialElementRenderers) {
        super(state, vertexConsumers, queue, dispatcher, specialElementRenderers);
        this.framebuffer = framebuffer;
    }
}
