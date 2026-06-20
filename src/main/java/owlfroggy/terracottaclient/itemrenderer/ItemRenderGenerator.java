package owlfroggy.terracottaclient.itemrenderer;

import com.mojang.blaze3d.GpuFormat;
import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.systems.CommandEncoder;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.GpuTexture;
import net.minecraft.client.Minecraft;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.pipeline.TextureTarget;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderBuffers;
import net.minecraft.client.renderer.SubmitNodeStorage;
import net.minecraft.client.renderer.feature.FeatureRenderDispatcher;
import net.minecraft.client.renderer.fog.FogRenderer;
import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.renderer.state.GameRenderState;
import net.minecraft.client.renderer.state.gui.GuiRenderState;
import net.minecraft.util.ARGB;
import net.minecraft.world.item.ItemStack;
import net.minecraft.util.Util;
import owlfroggy.terracottaclient.TCClient;
import owlfroggy.terracottaclient.mixin.GuiRendererAccessor;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.util.ArrayList;
import java.util.Base64;
import java.util.function.Consumer;

public class ItemRenderGenerator {
    private static void framebufferToImageWithAlpha(RenderTarget target, Consumer<NativeImage> callback) {
        int width = target.width;
        int height = target.height;
        GpuTexture sourceTexture = target.getColorTexture();
        if (sourceTexture == null) {
            throw new IllegalStateException("Tried to capture screenshot of an incomplete framebuffer");
        } else {
            GpuBuffer buffer = RenderSystem.getDevice().createBuffer(() -> "Screenshot buffer", 9, (long)width * (long)height * (long)sourceTexture.getFormat().blockSize());
            RenderSystem.getDevice().createCommandEncoder().copyTextureToBuffer(sourceTexture, buffer, 0L, () -> {
                try (GpuBufferSlice.MappedView read = buffer.map(true, false)) {
                    NativeImage image = new NativeImage(width, height, false);

                    for(int y = 0; y < height; ++y) {
                        for(int x = 0; x < width; ++x) {
                            int argb = read.data().getInt((x + y * width) * sourceTexture.getFormat().blockSize());
                            image.setPixelABGR(x, height - y - 1, argb);
                        }
                    }

                    callback.accept(image);
                }

                buffer.close();
            }, 0);
        }
    }

    private static RenderTarget renderToFramebuffer(ItemStack itemStack, int renderScale) {
        Minecraft client = TCClient.MCI;

        GameRenderState gameState = new GameRenderState();
        gameState.windowRenderState.height = renderScale*16;
        gameState.windowRenderState.width = renderScale*16;
        gameState.windowRenderState.guiScale = renderScale;

        RenderTarget renderTarget = new TextureTarget(
            "itemRender",
            16 * renderScale,
            16 * renderScale,
            true,
                GpuFormat.RGBA8_UNORM // this HAS to be RGBA8_UNORM or else everything breakss
        );

        int maxSectionBuilders = Runtime.getRuntime().availableProcessors();
        RenderBuffers renderBuffers = new RenderBuffers(maxSectionBuilders);
        RetargetableRenderer renderer = new RetargetableRenderer(
            renderTarget,
            gameState,
            new FeatureRenderDispatcher(renderBuffers, TCClient.MCI.getModelManager(), TCClient.MCI.getAtlasManager(), TCClient.MCI.font, gameState),
            new ArrayList<>()
        );


        int mx = (int)client.mouseHandler.getScaledXPos(client.getWindow());
        int my = (int)client.mouseHandler.getScaledYPos(client.getWindow());

        GuiGraphicsExtractor drawContext = new GuiGraphicsExtractor(client, gameState.guiRenderState, mx, my);
        drawContext.item(itemStack,0,0);

        renderer.render();

        return renderTarget;
    }

    /**
     * @param callback Will return error if the process failed
     */
    public static void renderToDataURI(ItemStack item, int renderScale, Consumer<String> callback) {
        framebufferToImageWithAlpha(renderToFramebuffer(item, renderScale), image -> {
            try {
                int w = image.getWidth();
                int h = image.getHeight();

                BufferedImage bi = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);

                for (int y = 0; y < h; y++) {
                    for (int x = 0; x < w; x++) {
                        bi.setRGB(x, y, image.getPixel(x, y));
                    }
                }

                ByteArrayOutputStream out = new ByteArrayOutputStream();
                ImageIO.write(bi, "PNG", out);

                String b64 = Base64.getEncoder().encodeToString(out.toByteArray());
                callback.accept("data:image/png;base64," + b64);
            } catch (Exception e) {
                callback.accept("error");
            }
        });
    }

    public static void renderToFile(String filePath, ItemStack item, int renderScale) {
        framebufferToImageWithAlpha(renderToFramebuffer(item,renderScale), (image) -> {
            File savePath = new File(filePath);
            Util.ioPool()
            .execute(() -> {
                try {
                    try {
                        image.writeToFile(savePath);
                    } catch (Throwable var7) {
                        if (image != null) {
                            try {
                                image.close();
                            } catch (Throwable var6) {
                                var7.addSuppressed(var6);
                            }
                        }

                        throw var7;
                    }

                    if (image != null) {
                        image.close();
                    }
                } catch (Exception e) {
                    TCClient.LOGGER.error("yongus",e);
                }
            });
        });
    }
}
