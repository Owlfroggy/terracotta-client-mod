package owlfroggy.terracottaclient.itemrenderer;

import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.systems.CommandEncoder;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.GpuTexture;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.Framebuffer;
import net.minecraft.client.gl.SimpleFramebuffer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.render.state.GuiRenderState;
import net.minecraft.client.render.OutlineVertexConsumerProvider;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.command.OrderedRenderCommandQueueImpl;
import net.minecraft.client.render.command.RenderDispatcher;
import net.minecraft.client.render.fog.FogRenderer;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.util.ScreenshotRecorder;
import net.minecraft.item.ItemStack;
import net.minecraft.text.ClickEvent;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Util;
import net.minecraft.util.math.ColorHelper;
import org.apache.commons.lang3.function.Consumers;
import owlfroggy.terracottaclient.TCClient;
import owlfroggy.terracottaclient.itemlibrary.HijackedRenderer;

import java.io.File;
import java.util.ArrayList;
import java.util.function.Consumer;

public class ItemRenderGenerator {
    private static void saveImage(Framebuffer framebuffer, String filePath) {
        framebufferToImageWithAlpha(framebuffer, (image) -> {
            File savePath = new File(filePath);
            Util.getIoWorkerExecutor()
            .execute(() -> {
                try {
                    try {
                        image.writeTo(savePath);
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

    private static void framebufferToImageWithAlpha(Framebuffer framebuffer, Consumer<NativeImage> callback) {
        // write framebuffer to native image

        int i = framebuffer.textureWidth;
        int j = framebuffer.textureHeight;
        GpuTexture gpuTexture = framebuffer.getColorAttachment();
        GpuBuffer gpuBuffer = RenderSystem.getDevice().createBuffer(() -> "Screenshot buffer", 9, (long)i * j * gpuTexture.getFormat().pixelSize());
        CommandEncoder commandEncoder = RenderSystem.getDevice().createCommandEncoder();
        RenderSystem.getDevice().createCommandEncoder().copyTextureToBuffer(gpuTexture, gpuBuffer, 0L, () -> {
            try (GpuBuffer.MappedView mappedView = commandEncoder.mapBuffer(gpuBuffer, true, false)) {
                int l = j;
                int m = i;

                NativeImage image = new NativeImage(i,j,false);

                for (int n = 0; n < l; n++) {
                    for (int o = 0; o < m; o++) {
                        int p = mappedView.data().getInt((o + n * i) * gpuTexture.getFormat().pixelSize());
//                        image.setColor(o, j - n - 1, p | 0xFF000000);
                        image.setColor(o, j - n - 1, p);
                    }
                }

                callback.accept(image);
            } catch (Exception e) {
                TCClient.LOGGER.error("dimgus",e);
            }

            gpuBuffer.close();
        }, 0);
    }

    public static void saveItemRender(ItemStack itemStack, String filePath, int renderScale) {
        MinecraftClient client = TCClient.MCI;
        GuiRenderState guiState = new GuiRenderState();

        OrderedRenderCommandQueueImpl queue = new OrderedRenderCommandQueueImpl();
        int wsf = client.getWindow().getScaleFactor();
        int wfx = client.getWindow().getFramebufferWidth();
        int wfy = client.getWindow().getFramebufferHeight();
        client.getWindow().setScaleFactor(renderScale);
        client.getWindow().setFramebufferHeight(16 * renderScale);
        client.getWindow().setFramebufferWidth(16 * renderScale);
        Framebuffer framebuffer = new SimpleFramebuffer(
            "itemRender",
            16 * renderScale,
            16 * renderScale,
            true
        );

        VertexConsumerProvider.Immediate vertexConsumerProvider = MinecraftClient.getInstance().getBufferBuilders().getEntityVertexConsumers();
        HijackedRenderer renderer = new HijackedRenderer(
            framebuffer,
            guiState,
            vertexConsumerProvider,
            queue,
            new RenderDispatcher(
                queue,
                TCClient.MCI.getBlockRenderManager(),
                vertexConsumerProvider,
                TCClient.MCI.getAtlasManager(),
                new OutlineVertexConsumerProvider(),
                vertexConsumerProvider,
                TCClient.MCI.textRenderer
            ),
            new ArrayList<>()
        );

        FogRenderer fogRenderer = new FogRenderer();

        int mx = (int)client.mouse.getScaledX(client.getWindow());
        int my = (int)client.mouse.getScaledY(client.getWindow());
        DrawContext drawContext = new DrawContext(client, guiState, mx, my);
        drawContext.drawItemWithoutEntity(itemStack,0,0);

        renderer.prepare();
        renderer.renderPreparedDraws(fogRenderer.getFogBuffer(FogRenderer.FogType.NONE));

        vertexConsumerProvider.draw();
        client.getWindow().setScaleFactor(wsf);
        client.getWindow().setFramebufferWidth(wfx);
        client.getWindow().setFramebufferHeight(wfy);
        TCClient.MCI.onResolutionChanged();

        saveImage(framebuffer, filePath);
//        ScreenshotRecorder.saveScreenshot(new File(filePath),framebuffer, Consumers.nop());
    }
}
