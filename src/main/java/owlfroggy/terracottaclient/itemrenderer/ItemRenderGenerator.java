//package owlfroggy.terracottaclient.itemrenderer;
//
//import com.mojang.blaze3d.buffers.GpuBuffer;
//import com.mojang.blaze3d.systems.CommandEncoder;
//import com.mojang.blaze3d.systems.RenderSystem;
//import com.mojang.blaze3d.textures.GpuTexture;
//import net.minecraft.client.Minecraft;
//import com.mojang.blaze3d.pipeline.RenderTarget;
//import com.mojang.blaze3d.pipeline.TextureTarget;
//import net.minecraft.client.gui.GuiGraphics;
//import net.minecraft.client.gui.render.state.GuiRenderState;
//import net.minecraft.client.renderer.OutlineBufferSource;
//import net.minecraft.client.renderer.MultiBufferSource;
//import net.minecraft.client.renderer.SubmitNodeStorage;
//import net.minecraft.client.renderer.feature.FeatureRenderDispatcher;
//import net.minecraft.client.renderer.fog.FogRenderer;
//import com.mojang.blaze3d.platform.NativeImage;
//import net.minecraft.world.item.ItemStack;
//import net.minecraft.util.Util;
//import owlfroggy.terracottaclient.TCClient;
//import owlfroggy.terracottaclient.mixin.GuiRendererAccessor;
//
//import javax.imageio.ImageIO;
//import java.awt.image.BufferedImage;
//import java.io.ByteArrayOutputStream;
//import java.io.File;
//import java.util.ArrayList;
//import java.util.Base64;
//import java.util.function.Consumer;
//
//public class ItemRenderGenerator {
//    private static void framebufferToImageWithAlpha(RenderTarget framebuffer, Consumer<NativeImage> callback) {
//        // write framebuffer to native image
//
//        int i = framebuffer.width;
//        int j = framebuffer.height;
//        GpuTexture gpuTexture = framebuffer.getColorTexture();
//        GpuBuffer gpuBuffer = RenderSystem.getDevice().createBuffer(() -> "Screenshot buffer", 9, (long)i * j * gpuTexture.getFormat().pixelSize());
//        CommandEncoder commandEncoder = RenderSystem.getDevice().createCommandEncoder();
//        RenderSystem.getDevice().createCommandEncoder().copyTextureToBuffer(gpuTexture, gpuBuffer, 0L, () -> {
//            try (GpuBuffer.MappedView mappedView = commandEncoder.mapBuffer(gpuBuffer, true, false)) {
//                int l = j;
//                int m = i;
//
//                NativeImage image = new NativeImage(i,j,false);
//
//                for (int n = 0; n < l; n++) {
//                    for (int o = 0; o < m; o++) {
//                        int p = mappedView.data().getInt((o + n * i) * gpuTexture.getFormat().pixelSize());
////                        image.setColor(o, j - n - 1, p | 0xFF000000);
//                        image.setPixelABGR(o, j - n - 1, p);
//                    }
//                }
//
//                callback.accept(image);
//            } catch (Exception e) {
//                TCClient.LOGGER.error("dimgus",e);
//            }
//
//            gpuBuffer.close();
//        }, 0);
//    }
//
//    private static RenderTarget renderToFramebuffer(ItemStack itemStack, int renderScale) {
//        Minecraft client = TCClient.MCI;
//        GuiRenderState guiState = new GuiRenderState();
//
//        SubmitNodeStorage queue = new SubmitNodeStorage();
//        int wsf = client.getWindow().getGuiScale();
//        int wfx = client.getWindow().getWidth();
//        int wfy = client.getWindow().getHeight();
//        client.getWindow().setGuiScale(renderScale);
//        client.getWindow().setHeight(16 * renderScale);
//        client.getWindow().setWidth(16 * renderScale);
//        RenderTarget framebuffer = new TextureTarget(
//            "itemRender",
//            16 * renderScale,
//            16 * renderScale,
//            true
//        );
//
//        MultiBufferSource.BufferSource vertexConsumerProvider = Minecraft.getInstance().renderBuffers().bufferSource();
//        RetargetableRenderer renderer = new RetargetableRenderer(
//            framebuffer,
//            guiState,
//            vertexConsumerProvider,
//            queue,
//            new FeatureRenderDispatcher(
//                queue,
//                TCClient.MCI.getBlockRenderer(),
//                vertexConsumerProvider,
//                TCClient.MCI.getAtlasManager(),
//                new OutlineBufferSource(),
//                vertexConsumerProvider,
//                TCClient.MCI.font
//            ),
//            new ArrayList<>()
//        );
//
//        FogRenderer fogRenderer = new FogRenderer();
//
//        int mx = (int)client.mouseHandler.getScaledXPos(client.getWindow());
//        int my = (int)client.mouseHandler.getScaledYPos(client.getWindow());
//        GuiGraphics drawContext = new GuiGraphics(client, guiState, mx, my);
//        drawContext.renderFakeItem(itemStack,0,0);
//
//        ((GuiRendererAccessor)renderer).invokePrepare();
//        ((GuiRendererAccessor)renderer).invokeRenderPreapredDraws(fogRenderer.getBuffer(FogRenderer.FogMode.NONE));
//
//        vertexConsumerProvider.endBatch();
//        client.getWindow().setGuiScale(wsf);
//        client.getWindow().setWidth(wfx);
//        client.getWindow().setHeight(wfy);
//        TCClient.MCI.resizeDisplay();
//
//        return framebuffer;
//    }
//
//    /**
//     * @param callback Will return error if the process failed
//     */
//    public static void renderToDataURI(ItemStack item, int renderScale, Consumer<String> callback) {
//        framebufferToImageWithAlpha(renderToFramebuffer(item, renderScale), image -> {
//            try {
//                int w = image.getWidth();
//                int h = image.getHeight();
//
//                BufferedImage bi = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
//
//                for (int y = 0; y < h; y++) {
//                    for (int x = 0; x < w; x++) {
//                        bi.setRGB(x, y, image.getPixel(x, y));
//                    }
//                }
//
//                ByteArrayOutputStream out = new ByteArrayOutputStream();
//                ImageIO.write(bi, "PNG", out);
//
//                String b64 = Base64.getEncoder().encodeToString(out.toByteArray());
//                callback.accept("data:image/png;base64," + b64);
//            } catch (Exception e) {
//                callback.accept("error");
//            }
//        });
//    }
//
//    public static void renderToFile(String filePath, ItemStack item, int renderScale) {
//        framebufferToImageWithAlpha(renderToFramebuffer(item,renderScale), (image) -> {
//            File savePath = new File(filePath);
//            Util.ioPool()
//            .execute(() -> {
//                try {
//                    try {
//                        image.writeToFile(savePath);
//                    } catch (Throwable var7) {
//                        if (image != null) {
//                            try {
//                                image.close();
//                            } catch (Throwable var6) {
//                                var7.addSuppressed(var6);
//                            }
//                        }
//
//                        throw var7;
//                    }
//
//                    if (image != null) {
//                        image.close();
//                    }
//                } catch (Exception e) {
//                    TCClient.LOGGER.error("yongus",e);
//                }
//            });
//        });
//    }
//}
