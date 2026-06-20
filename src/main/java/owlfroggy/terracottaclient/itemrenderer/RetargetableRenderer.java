package owlfroggy.terracottaclient.itemrenderer;


import com.mojang.blaze3d.pipeline.RenderTarget;
import net.minecraft.client.gui.render.GuiRenderer;
import net.minecraft.client.gui.render.pip.PictureInPictureRenderer;
import net.minecraft.client.renderer.feature.FeatureRenderDispatcher;
import net.minecraft.client.renderer.state.GameRenderState;
import net.minecraft.client.renderer.state.gui.GuiRenderState;

import java.util.List;

public class RetargetableRenderer extends GuiRenderer {
    private RenderTarget renderTarget;
    private GameRenderState gameRenderState;

    public RenderTarget getRenderTarget() { return renderTarget; }
    public GameRenderState getGameRenderState() { return gameRenderState; }

    public RetargetableRenderer(RenderTarget renderTarget, GameRenderState gameRenderState, final FeatureRenderDispatcher featureRenderDispatcher, final List<PictureInPictureRenderer<?>> pictureInPictureRenderers) {
        super(gameRenderState.guiRenderState, featureRenderDispatcher, pictureInPictureRenderers);
        this.renderTarget = renderTarget;
        this.gameRenderState = gameRenderState;
    }
}
