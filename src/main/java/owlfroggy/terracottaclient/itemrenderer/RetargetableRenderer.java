//package owlfroggy.terracottaclient.itemrenderer;
//
//import com.mojang.blaze3d.pipeline.RenderTarget;
//import net.minecraft.client.gui.render.GuiRenderer;
//import net.minecraft.client.gui.render.pip.PictureInPictureRenderer;
//import net.minecraft.client.gui.render.state.GuiRenderState;
//import net.minecraft.client.renderer.MultiBufferSource;
//import net.minecraft.client.renderer.SubmitNodeCollector;
//import net.minecraft.client.renderer.feature.FeatureRenderDispatcher;
//
//import java.util.List;
//
//public class RetargetableRenderer extends GuiRenderer {
//    private RenderTarget framebuffer;
//
//    public RenderTarget getFramebuffer() { return framebuffer; }
//
//    public RetargetableRenderer(RenderTarget framebuffer, GuiRenderState state, MultiBufferSource.BufferSource vertexConsumers, SubmitNodeCollector queue, FeatureRenderDispatcher dispatcher, List<PictureInPictureRenderer<?>> specialElementRenderers) {
//        super(state, vertexConsumers, queue, dispatcher, specialElementRenderers);
//        this.framebuffer = framebuffer;
//    }
//}
