package com.mojang.blaze3d.pipeline;

import com.mojang.blaze3d.systems.GpuDevice;
import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.FilterMode;
import com.mojang.blaze3d.textures.GpuTexture;
import com.mojang.blaze3d.textures.GpuTextureView;
import com.mojang.blaze3d.textures.TextureFormat;
import java.util.OptionalInt;
import net.minecraft.client.renderer.RenderPipelines;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.jspecify.annotations.Nullable;

@OnlyIn(Dist.CLIENT)
public abstract class RenderTarget {
    private static int UNNAMED_RENDER_TARGETS = 0;
    public int width;
    public int height;
    protected final String label;
    public final boolean useDepth;
    public final boolean useStencil;
    protected @Nullable GpuTexture colorTexture;
    protected @Nullable GpuTextureView colorTextureView;
    protected @Nullable GpuTexture depthTexture;
    protected @Nullable GpuTextureView depthTextureView;

    public RenderTarget(@Nullable String name, boolean useDepth) {
        this(name, useDepth, false);
    }

    public RenderTarget(@Nullable String name, boolean useDepth, boolean useStencil) {
        if (useStencil && !useDepth) {
            throw new IllegalArgumentException("Stencil can only be enabled if depth is enabled.");
        }
        this.label = name == null ? "FBO " + UNNAMED_RENDER_TARGETS++ : name;
        this.useDepth = useDepth;
        this.useStencil = useStencil;
    }

    public void resize(int width, int height) {
        RenderSystem.assertOnRenderThread();
        this.destroyBuffers();
        this.createBuffers(width, height);
    }

    public void destroyBuffers() {
        RenderSystem.assertOnRenderThread();
        if (this.depthTexture != null) {
            this.depthTexture.close();
            this.depthTexture = null;
        }

        if (this.depthTextureView != null) {
            this.depthTextureView.close();
            this.depthTextureView = null;
        }

        if (this.colorTexture != null) {
            this.colorTexture.close();
            this.colorTexture = null;
        }

        if (this.colorTextureView != null) {
            this.colorTextureView.close();
            this.colorTextureView = null;
        }
    }

    public void copyDepthFrom(RenderTarget otherTarget) {
        RenderSystem.assertOnRenderThread();
        if (this.depthTexture == null) {
            throw new IllegalStateException("Trying to copy depth texture to a RenderTarget without a depth texture");
        } else if (otherTarget.depthTexture == null) {
            throw new IllegalStateException("Trying to copy depth texture from a RenderTarget without a depth texture");
        } else {
            RenderSystem.getDevice()
                .createCommandEncoder()
                .copyTextureToTexture(otherTarget.depthTexture, this.depthTexture, 0, 0, 0, 0, 0, this.width, this.height);
        }
    }

    public void createBuffers(int width, int height) {
        RenderSystem.assertOnRenderThread();
        GpuDevice gpudevice = RenderSystem.getDevice();
        int i = gpudevice.getMaxTextureSize();
        if (width > 0 && width <= i && height > 0 && height <= i) {
            this.width = width;
            this.height = height;
            if (this.useDepth) {
                var format = this.useStencil ? net.neoforged.neoforge.client.ClientHooks.getStencilFormat() : TextureFormat.DEPTH32;
                this.depthTexture = gpudevice.createTexture(() -> this.label + " / Depth", 15, format, width, height, 1, 1);
                this.depthTextureView = gpudevice.createTextureView(this.depthTexture);
            }

            this.colorTexture = gpudevice.createTexture(() -> this.label + " / Color", 15, TextureFormat.RGBA8, width, height, 1, 1);
            this.colorTextureView = gpudevice.createTextureView(this.colorTexture);
        } else {
            throw new IllegalArgumentException("Window " + width + "x" + height + " size out of bounds (max. size: " + i + ")");
        }
    }

    public void blitToScreen() {
        if (this.colorTexture == null) {
            throw new IllegalStateException("Can't blit to screen, color texture doesn't exist yet");
        } else {
            RenderSystem.getDevice().createCommandEncoder().presentTexture(this.colorTextureView);
        }
    }

    public void blitAndBlendToTexture(GpuTextureView textureView) {
        RenderSystem.assertOnRenderThread();

        try (RenderPass renderpass = RenderSystem.getDevice()
                .createCommandEncoder()
                .createRenderPass(() -> "Blit render target", textureView, OptionalInt.empty())) {
            renderpass.setPipeline(RenderPipelines.ENTITY_OUTLINE_BLIT);
            RenderSystem.bindDefaultUniforms(renderpass);
            renderpass.bindTexture("InSampler", this.colorTextureView, RenderSystem.getSamplerCache().getClampToEdge(FilterMode.NEAREST));
            renderpass.draw(0, 3);
        }
    }

    public @Nullable GpuTexture getColorTexture() {
        return this.colorTexture;
    }

    public @Nullable GpuTextureView getColorTextureView() {
        return this.colorTextureView;
    }

    public @Nullable GpuTexture getDepthTexture() {
        return this.depthTexture;
    }

    public @Nullable GpuTextureView getDepthTextureView() {
        return this.depthTextureView;
    }
}
