package com.mojang.blaze3d.opengl;

import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.systems.ScissorState;
import com.mojang.blaze3d.textures.GpuSampler;
import com.mojang.blaze3d.textures.GpuTextureView;
import com.mojang.blaze3d.vertex.VertexFormat;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Supplier;
import net.minecraft.SharedConstants;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.jspecify.annotations.Nullable;

@OnlyIn(Dist.CLIENT)
public class GlRenderPass implements RenderPass {
    protected static final int MAX_VERTEX_BUFFERS = 1;
    public static final boolean VALIDATION = SharedConstants.IS_RUNNING_IN_IDE && !Boolean.getBoolean("neoforge.disableGlValidation");
    private final GlCommandEncoder encoder;
    private final boolean hasDepthTexture;
    private boolean closed;
    protected @Nullable GlRenderPipeline pipeline;
    protected final @Nullable GpuBuffer[] vertexBuffers = new GpuBuffer[1];
    protected @Nullable GpuBuffer indexBuffer;
    protected VertexFormat.IndexType indexType = VertexFormat.IndexType.INT;
    private final ScissorState scissorState = new ScissorState();
    protected final HashMap<String, GpuBufferSlice> uniforms = new HashMap<>();
    protected final HashMap<String, GlRenderPass.TextureViewAndSampler> samplers = new HashMap<>();
    protected final Set<String> dirtyUniforms = new HashSet<>();
    protected int pushedDebugGroups;

    public GlRenderPass(GlCommandEncoder encoder, boolean hasDepthTexture) {
        this.encoder = encoder;
        this.hasDepthTexture = hasDepthTexture;
    }

    public boolean hasDepthTexture() {
        return this.hasDepthTexture;
    }

    @Override
    public void pushDebugGroup(Supplier<String> p_419777_) {
        if (this.closed) {
            throw new IllegalStateException("Can't use a closed render pass");
        } else {
            this.pushedDebugGroups++;
            this.encoder.getDevice().debugLabels().pushDebugGroup(p_419777_);
        }
    }

    @Override
    public void popDebugGroup() {
        if (this.closed) {
            throw new IllegalStateException("Can't use a closed render pass");
        } else if (this.pushedDebugGroups == 0) {
            throw new IllegalStateException("Can't pop more debug groups than was pushed!");
        } else {
            this.pushedDebugGroups--;
            this.encoder.getDevice().debugLabels().popDebugGroup();
        }
    }

    @Override
    public void setPipeline(RenderPipeline p_409823_) {
        p_409823_ = com.mojang.blaze3d.systems.RenderSystem.applyPipelineModifiers(p_409823_);
        if (this.pipeline == null || this.pipeline.info() != p_409823_) {
            this.dirtyUniforms.addAll(this.uniforms.keySet());
            this.dirtyUniforms.addAll(this.samplers.keySet());
        }

        this.pipeline = this.encoder.getDevice().getOrCompilePipeline(p_409823_);
    }

    @Override
    public void bindTexture(String p_455518_, @Nullable GpuTextureView p_455780_, @Nullable GpuSampler p_455263_) {
        if (p_455263_ == null) {
            this.samplers.remove(p_455518_);
        } else {
            this.samplers.put(p_455518_, new GlRenderPass.TextureViewAndSampler((GlTextureView)p_455780_, (GlSampler)p_455263_));
        }

        this.dirtyUniforms.add(p_455518_);
    }

    @Override
    public void setUniform(String p_410717_, GpuBuffer p_418484_) {
        this.uniforms.put(p_410717_, p_418484_.slice());
        this.dirtyUniforms.add(p_410717_);
    }

    @Override
    public void setUniform(String p_409689_, GpuBufferSlice p_418312_) {
        int i = this.encoder.getDevice().getUniformOffsetAlignment();
        if (p_418312_.offset() % i > 0L) {
            throw new IllegalArgumentException("Uniform buffer offset must be aligned to " + i);
        } else {
            this.uniforms.put(p_409689_, p_418312_);
            this.dirtyUniforms.add(p_409689_);
        }
    }

    @Override
    public void setViewport(int x, int y, int width, int height){
        GlStateManager._viewport(x, y, width, height);
    }

    @Override
    public void enableScissor(int p_409849_, int p_410447_, int p_410110_, int p_410679_) {
        this.scissorState.enable(p_409849_, p_410447_, p_410110_, p_410679_);
    }

    @Override
    public void disableScissor() {
        this.scissorState.disable();
    }

    public boolean isScissorEnabled() {
        return this.scissorState.enabled();
    }

    public int getScissorX() {
        return this.scissorState.x();
    }

    public int getScissorY() {
        return this.scissorState.y();
    }

    public int getScissorWidth() {
        return this.scissorState.width();
    }

    public int getScissorHeight() {
        return this.scissorState.height();
    }

    @Override
    public void setVertexBuffer(int p_410797_, GpuBuffer p_410501_) {
        if (p_410797_ >= 0 && p_410797_ < 1) {
            this.vertexBuffers[p_410797_] = p_410501_;
        } else {
            throw new IllegalArgumentException("Vertex buffer slot is out of range: " + p_410797_);
        }
    }

    @Override
    public void setIndexBuffer(@Nullable GpuBuffer p_410828_, VertexFormat.IndexType p_410040_) {
        this.indexBuffer = p_410828_;
        this.indexType = p_410040_;
    }

    @Override
    public void drawIndexed(int p_410452_, int p_410034_, int p_419595_, int p_419800_) {
        if (this.closed) {
            throw new IllegalStateException("Can't use a closed render pass");
        } else {
            this.encoder.executeDraw(this, p_410452_, p_410034_, p_419595_, this.indexType, p_419800_);
        }
    }

    @Override
    public <T> void drawMultipleIndexed(
        Collection<RenderPass.Draw<T>> p_410499_,
        @Nullable GpuBuffer p_412214_,
        VertexFormat.@Nullable IndexType p_412273_,
        Collection<String> p_418377_,
        T p_428237_
    ) {
        if (this.closed) {
            throw new IllegalStateException("Can't use a closed render pass");
        } else {
            this.encoder.executeDrawMultiple(this, p_410499_, p_412214_, p_412273_, p_418377_, p_428237_);
        }
    }

    @Override
    public void draw(int p_410870_, int p_410463_) {
        if (this.closed) {
            throw new IllegalStateException("Can't use a closed render pass");
        } else {
            this.encoder.executeDraw(this, p_410870_, 0, p_410463_, null, 1);
        }
    }

    @Override
    public void close() {
        if (!this.closed) {
            if (this.pushedDebugGroups > 0) {
                throw new IllegalStateException("Render pass had debug groups left open!");
            }

            this.closed = true;
            this.encoder.finishRenderPass();
        }
    }

    @OnlyIn(Dist.CLIENT)
    protected record TextureViewAndSampler(GlTextureView view, GlSampler sampler) {
    }
}
