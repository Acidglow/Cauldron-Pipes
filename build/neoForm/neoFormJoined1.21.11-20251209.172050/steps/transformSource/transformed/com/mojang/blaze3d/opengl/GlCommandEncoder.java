package com.mojang.blaze3d.opengl;

import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.buffers.GpuFence;
import com.mojang.blaze3d.pipeline.BlendFunction;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.platform.DepthTestFunction;
import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.shaders.UniformType;
import com.mojang.blaze3d.systems.CommandEncoder;
import com.mojang.blaze3d.systems.GpuQuery;
import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.GpuTexture;
import com.mojang.blaze3d.textures.GpuTextureView;
import com.mojang.blaze3d.textures.TextureFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.logging.LogUtils;
import java.nio.ByteBuffer;
import java.util.Collection;
import java.util.Collections;
import java.util.OptionalDouble;
import java.util.OptionalInt;
import java.util.Map.Entry;
import java.util.function.BiConsumer;
import java.util.function.Supplier;
import net.minecraft.util.ARGB;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.jspecify.annotations.Nullable;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL11C;
import org.lwjgl.opengl.GL31;
import org.lwjgl.opengl.GL32;
import org.lwjgl.opengl.GL32C;
import org.lwjgl.opengl.GL33C;
import org.slf4j.Logger;

@OnlyIn(Dist.CLIENT)
public class GlCommandEncoder implements CommandEncoder {
    private static final Logger LOGGER = LogUtils.getLogger();
    private final GlDevice device;
    private final int readFbo;
    private final int drawFbo;
    private @Nullable RenderPipeline lastPipeline;
    private boolean inRenderPass;
    private @Nullable GlProgram lastProgram;
    private @Nullable GlTimerQuery activeTimerQuery;

    protected GlCommandEncoder(GlDevice device) {
        this.device = device;
        this.readFbo = device.directStateAccess().createFrameBufferObject();
        this.drawFbo = device.directStateAccess().createFrameBufferObject();
    }

    @Override
    public RenderPass createRenderPass(Supplier<String> p_419658_, GpuTextureView p_423471_, OptionalInt p_410192_) {
        return this.createRenderPass(p_419658_, p_423471_, p_410192_, null, OptionalDouble.empty());
    }

    @Override
    public RenderPass createRenderPass(
        Supplier<String> p_419957_, GpuTextureView p_423627_, OptionalInt p_410460_, @Nullable GpuTextureView p_423565_, OptionalDouble p_423486_
    ) {
        if (this.inRenderPass) {
            throw new IllegalStateException("Close the existing render pass before creating a new one!");
        } else {
            if (p_423486_.isPresent() && p_423565_ == null) {
                LOGGER.warn("Depth clear value was provided but no depth texture is being used");
            }

            if (p_423627_.isClosed()) {
                throw new IllegalStateException("Color texture is closed");
            } else if ((p_423627_.texture().usage() & 8) == 0) {
                throw new IllegalStateException("Color texture must have USAGE_RENDER_ATTACHMENT");
            } else if (p_423627_.texture().getDepthOrLayers() > 1) {
                throw new UnsupportedOperationException("Textures with multiple depths or layers are not yet supported as an attachment");
            } else {
                if (p_423565_ != null) {
                    if (p_423565_.isClosed()) {
                        throw new IllegalStateException("Depth texture is closed");
                    }

                    if ((p_423565_.texture().usage() & 8) == 0) {
                        throw new IllegalStateException("Depth texture must have USAGE_RENDER_ATTACHMENT");
                    }

                    if (p_423565_.texture().getDepthOrLayers() > 1) {
                        throw new UnsupportedOperationException("Textures with multiple depths or layers are not yet supported as an attachment");
                    }
                }

                this.inRenderPass = true;
                this.device.debugLabels().pushDebugGroup(p_419957_);
                int i = ((GlTextureView)p_423627_).getFbo(this.device.directStateAccess(), p_423565_ == null ? null : p_423565_.texture());
                GlStateManager._glBindFramebuffer(36160, i);
                int j = 0;
                if (p_410460_.isPresent()) {
                    int k = p_410460_.getAsInt();
                    GL11.glClearColor(ARGB.redFloat(k), ARGB.greenFloat(k), ARGB.blueFloat(k), ARGB.alphaFloat(k));
                    j |= 16384;
                }

                if (p_423565_ != null && p_423486_.isPresent()) {
                    GL11.glClearDepth(p_423486_.getAsDouble());
                    j |= 256;
                }

                if (j != 0) {
                    GlStateManager._disableScissorTest();
                    GlStateManager._depthMask(true);
                    GlStateManager._colorMask(true, true, true, true);
                    GlStateManager._clear(j);
                }

                GlStateManager._viewport(0, 0, p_423627_.getWidth(0), p_423627_.getHeight(0));
                this.lastPipeline = null;
                return new GlRenderPass(this, p_423565_ != null);
            }
        }
    }

    @Override
    public void clearColorTexture(GpuTexture p_410228_, int p_410646_) {
        if (this.inRenderPass) {
            throw new IllegalStateException("Close the existing render pass before creating a new one!");
        } else {
            this.verifyColorTexture(p_410228_);
            this.device.directStateAccess().bindFrameBufferTextures(this.drawFbo, ((GlTexture)p_410228_).id, 0, 0, 36160);
            GL11.glClearColor(ARGB.redFloat(p_410646_), ARGB.greenFloat(p_410646_), ARGB.blueFloat(p_410646_), ARGB.alphaFloat(p_410646_));
            GlStateManager._disableScissorTest();
            GlStateManager._colorMask(true, true, true, true);
            GlStateManager._clear(16384);
            GlStateManager._glFramebufferTexture2D(36160, 36064, 3553, 0, 0);
            GlStateManager._glBindFramebuffer(36160, 0);
        }
    }

    @Override
    public void clearColorAndDepthTextures(GpuTexture p_410863_, int p_410603_, GpuTexture p_409616_, double p_410193_) {
        if (this.inRenderPass) {
            throw new IllegalStateException("Close the existing render pass before creating a new one!");
        } else {
            this.verifyColorTexture(p_410863_);
            this.verifyDepthTexture(p_409616_);
            int i = ((GlTexture)p_410863_).getFbo(this.device.directStateAccess(), p_409616_);
            GlStateManager._glBindFramebuffer(36160, i);
            GlStateManager._disableScissorTest();
            GL11.glClearDepth(p_410193_);
            GL11.glClearColor(ARGB.redFloat(p_410603_), ARGB.greenFloat(p_410603_), ARGB.blueFloat(p_410603_), ARGB.alphaFloat(p_410603_));
            GlStateManager._depthMask(true);
            GlStateManager._colorMask(true, true, true, true);
            GlStateManager._clear(16640);
            GlStateManager._glBindFramebuffer(36160, 0);
        }
    }

    @Override
    public void clearColorAndDepthTextures(
        GpuTexture p_416345_, int p_416520_, GpuTexture p_416382_, double p_415785_, int p_415996_, int p_416125_, int p_415580_, int p_415906_
    ) {
        if (this.inRenderPass) {
            throw new IllegalStateException("Close the existing render pass before creating a new one!");
        } else {
            this.verifyColorTexture(p_416345_);
            this.verifyDepthTexture(p_416382_);
            this.verifyRegion(p_416345_, p_415996_, p_416125_, p_415580_, p_415906_);
            int i = ((GlTexture)p_416345_).getFbo(this.device.directStateAccess(), p_416382_);
            GlStateManager._glBindFramebuffer(36160, i);
            GlStateManager._scissorBox(p_415996_, p_416125_, p_415580_, p_415906_);
            GlStateManager._enableScissorTest();
            GL11.glClearDepth(p_415785_);
            GL11.glClearColor(ARGB.redFloat(p_416520_), ARGB.greenFloat(p_416520_), ARGB.blueFloat(p_416520_), ARGB.alphaFloat(p_416520_));
            GlStateManager._depthMask(true);
            GlStateManager._colorMask(true, true, true, true);
            GlStateManager._clear(16640);
            GlStateManager._glBindFramebuffer(36160, 0);
        }
    }

    private void verifyRegion(GpuTexture texture, int regionX, int regionY, int regionWidth, int regionHeight) {
        if (regionX < 0 || regionX >= texture.getWidth(0)) {
            throw new IllegalArgumentException("regionX should not be outside of the texture");
        } else if (regionY < 0 || regionY >= texture.getHeight(0)) {
            throw new IllegalArgumentException("regionY should not be outside of the texture");
        } else if (regionWidth <= 0) {
            throw new IllegalArgumentException("regionWidth should be greater than 0");
        } else if (regionX + regionWidth > texture.getWidth(0)) {
            throw new IllegalArgumentException("regionWidth + regionX should be less than the texture width");
        } else if (regionHeight <= 0) {
            throw new IllegalArgumentException("regionHeight should be greater than 0");
        } else if (regionY + regionHeight > texture.getHeight(0)) {
            throw new IllegalArgumentException("regionWidth + regionX should be less than the texture height");
        }
    }

    @Override
    public void clearDepthTexture(GpuTexture p_410548_, double p_410067_) {
        if (this.inRenderPass) {
            throw new IllegalStateException("Close the existing render pass before creating a new one!");
        } else {
            this.verifyDepthTexture(p_410548_);
            boolean hasStencil = p_410548_.getFormat().hasStencilAspect();
            this.device.directStateAccess().bindFrameBufferTextures(this.drawFbo, 0, ((GlTexture)p_410548_).id, 0, 36160, hasStencil);
            GL11.glDrawBuffer(0);
            GL11.glClearDepth(p_410067_);
            GlStateManager._depthMask(true);
            GlStateManager._disableScissorTest();
            GlStateManager._clear(256);
            GL11.glDrawBuffer(36064);
            GlStateManager._glFramebufferTexture2D(36160, 36096, 3553, 0, 0);
            GlStateManager._glBindFramebuffer(36160, 0);
        }
    }

    @Override
    public void clearStencilTexture(GpuTexture texture, int value) {
        if (this.inRenderPass) {
            throw new IllegalStateException("Close the existing render pass before creating a new one!");
        } else if (!texture.getFormat().hasStencilAspect()) {
            throw new IllegalStateException("Trying to clear stencil in a texture that has no stencil component!");
        } else {
            this.device.directStateAccess().bindFrameBufferTextures(this.drawFbo, 0, ((GlTexture)texture).id, 0, GlConst.GL_FRAMEBUFFER, true);
            GL11.glDrawBuffer(GlConst.GL_NONE);
            GL11.glClearStencil(value);
            GlStateManager._depthMask(true);
            GlStateManager._clear(GL11.GL_STENCIL_BUFFER_BIT);
            GL11.glDrawBuffer(GlConst.GL_COLOR_ATTACHMENT0);
            GlStateManager._glBindFramebuffer(GlConst.GL_FRAMEBUFFER, 0);
        }
    }

    private void verifyColorTexture(GpuTexture texture) {
        if (!texture.getFormat().hasColorAspect()) {
            throw new IllegalStateException("Trying to clear a non-color texture as color");
        } else if (texture.isClosed()) {
            throw new IllegalStateException("Color texture is closed");
        } else if ((texture.usage() & 8) == 0) {
            throw new IllegalStateException("Color texture must have USAGE_RENDER_ATTACHMENT");
        } else if (texture.getDepthOrLayers() > 1) {
            throw new UnsupportedOperationException("Clearing a texture with multiple layers or depths is not yet supported");
        }
    }

    private void verifyDepthTexture(GpuTexture texture) {
        if (!texture.getFormat().hasDepthAspect()) {
            throw new IllegalStateException("Trying to clear a non-depth texture as depth");
        } else if (texture.isClosed()) {
            throw new IllegalStateException("Depth texture is closed");
        } else if ((texture.usage() & 8) == 0) {
            throw new IllegalStateException("Depth texture must have USAGE_RENDER_ATTACHMENT");
        } else if (texture.getDepthOrLayers() > 1) {
            throw new UnsupportedOperationException("Clearing a texture with multiple layers or depths is not yet supported");
        }
    }

    @Override
    public void writeToBuffer(GpuBufferSlice p_418368_, ByteBuffer p_410689_) {
        if (this.inRenderPass) {
            throw new IllegalStateException("Close the existing render pass before performing additional commands");
        } else {
            GlBuffer glbuffer = (GlBuffer)p_418368_.buffer();
            if (glbuffer.closed) {
                throw new IllegalStateException("Buffer already closed");
            } else if ((glbuffer.usage() & 8) == 0) {
                throw new IllegalStateException("Buffer needs USAGE_COPY_DST to be a destination for a copy");
            } else {
                int i = p_410689_.remaining();
                if (i > p_418368_.length()) {
                    throw new IllegalArgumentException(
                        "Cannot write more data than the slice allows (attempting to write " + i + " bytes into a slice of length " + p_418368_.length() + ")"
                    );
                } else if (p_418368_.length() + p_418368_.offset() > glbuffer.size()) {
                    throw new IllegalArgumentException(
                        "Cannot write more data than this buffer can hold (attempting to write "
                            + i
                            + " bytes at offset "
                            + p_418368_.offset()
                            + " to "
                            + glbuffer.size()
                            + " size buffer)"
                    );
                } else {
                    this.device.directStateAccess().bufferSubData(glbuffer.handle, p_418368_.offset(), p_410689_, glbuffer.usage());
                }
            }
        }
    }

    @Override
    public GpuBuffer.MappedView mapBuffer(GpuBuffer p_418290_, boolean p_418044_, boolean p_418126_) {
        return this.mapBuffer(p_418290_.slice(), p_418044_, p_418126_);
    }

    @Override
    public GpuBuffer.MappedView mapBuffer(GpuBufferSlice p_418128_, boolean p_418013_, boolean p_418412_) {
        if (this.inRenderPass) {
            throw new IllegalStateException("Close the existing render pass before performing additional commands");
        } else {
            GlBuffer glbuffer = (GlBuffer)p_418128_.buffer();
            if (glbuffer.closed) {
                throw new IllegalStateException("Buffer already closed");
            } else if (!p_418013_ && !p_418412_) {
                throw new IllegalArgumentException("At least read or write must be true");
            } else if (p_418013_ && (glbuffer.usage() & 1) == 0) {
                throw new IllegalStateException("Buffer is not readable");
            } else if (p_418412_ && (glbuffer.usage() & 2) == 0) {
                throw new IllegalStateException("Buffer is not writable");
            } else if (p_418128_.offset() + p_418128_.length() > glbuffer.size()) {
                throw new IllegalArgumentException(
                    "Cannot map more data than this buffer can hold (attempting to map "
                        + p_418128_.length()
                        + " bytes at offset "
                        + p_418128_.offset()
                        + " from "
                        + glbuffer.size()
                        + " size buffer)"
                );
            } else {
                int i = 0;
                if (p_418013_) {
                    i |= 1;
                }

                if (p_418412_) {
                    i |= 34;
                }

                return this.device.getBufferStorage().mapBuffer(this.device.directStateAccess(), glbuffer, p_418128_.offset(), p_418128_.length(), i);
            }
        }
    }

    @Override
    public void copyToBuffer(GpuBufferSlice p_428848_, GpuBufferSlice p_428840_) {
        if (this.inRenderPass) {
            throw new IllegalStateException("Close the existing render pass before performing additional commands");
        } else {
            GlBuffer glbuffer = (GlBuffer)p_428848_.buffer();
            if (glbuffer.closed) {
                throw new IllegalStateException("Source buffer already closed");
            } else if ((glbuffer.usage() & 16) == 0) {
                throw new IllegalStateException("Source buffer needs USAGE_COPY_SRC to be a source for a copy");
            } else {
                GlBuffer glbuffer1 = (GlBuffer)p_428840_.buffer();
                if (glbuffer1.closed) {
                    throw new IllegalStateException("Target buffer already closed");
                } else if ((glbuffer1.usage() & 8) == 0) {
                    throw new IllegalStateException("Target buffer needs USAGE_COPY_DST to be a destination for a copy");
                } else if (p_428848_.length() != p_428840_.length()) {
                    throw new IllegalArgumentException(
                        "Cannot copy from slice of size " + p_428848_.length() + " to slice of size " + p_428840_.length() + ", they must be equal"
                    );
                } else if (p_428848_.offset() + p_428848_.length() > glbuffer.size()) {
                    throw new IllegalArgumentException(
                        "Cannot copy more data than the source buffer holds (attempting to copy "
                            + p_428848_.length()
                            + " bytes at offset "
                            + p_428848_.offset()
                            + " from "
                            + glbuffer.size()
                            + " size buffer)"
                    );
                } else if (p_428840_.offset() + p_428840_.length() > glbuffer1.size()) {
                    throw new IllegalArgumentException(
                        "Cannot copy more data than the target buffer can hold (attempting to copy "
                            + p_428840_.length()
                            + " bytes at offset "
                            + p_428840_.offset()
                            + " to "
                            + glbuffer1.size()
                            + " size buffer)"
                    );
                } else {
                    this.device
                        .directStateAccess()
                        .copyBufferSubData(glbuffer.handle, glbuffer1.handle, p_428848_.offset(), p_428840_.offset(), p_428848_.length());
                }
            }
        }
    }

    @Override
    public void writeToTexture(GpuTexture p_409824_, NativeImage p_410255_) {
        int i = p_409824_.getWidth(0);
        int j = p_409824_.getHeight(0);
        if (p_410255_.getWidth() != i || p_410255_.getHeight() != j) {
            throw new IllegalArgumentException(
                "Cannot replace texture of size " + i + "x" + j + " with image of size " + p_410255_.getWidth() + "x" + p_410255_.getHeight()
            );
        } else if (p_409824_.isClosed()) {
            throw new IllegalStateException("Destination texture is closed");
        } else if ((p_409824_.usage() & 1) == 0) {
            throw new IllegalStateException("Color texture must have USAGE_COPY_DST to be a destination for a write");
        } else {
            this.writeToTexture(p_409824_, p_410255_, 0, 0, 0, 0, i, j, 0, 0);
        }
    }

    @Override
    public void writeToTexture(
        GpuTexture p_410473_,
        NativeImage p_423502_,
        int p_410132_,
        int p_409948_,
        int p_410810_,
        int p_409825_,
        int p_410770_,
        int p_423635_,
        int p_423531_,
        int p_423633_
    ) {
        if (this.inRenderPass) {
            throw new IllegalStateException("Close the existing render pass before performing additional commands");
        } else if (p_410132_ >= 0 && p_410132_ < p_410473_.getMipLevels()) {
            if (p_423531_ + p_410770_ > p_423502_.getWidth() || p_423633_ + p_423635_ > p_423502_.getHeight()) {
                throw new IllegalArgumentException(
                    "Copy source ("
                        + p_423502_.getWidth()
                        + "x"
                        + p_423502_.getHeight()
                        + ") is not large enough to read a rectangle of "
                        + p_410770_
                        + "x"
                        + p_423635_
                        + " from "
                        + p_423531_
                        + "x"
                        + p_423633_
                );
            } else if (p_410810_ + p_410770_ > p_410473_.getWidth(p_410132_) || p_409825_ + p_423635_ > p_410473_.getHeight(p_410132_)) {
                throw new IllegalArgumentException(
                    "Dest texture ("
                        + p_410770_
                        + "x"
                        + p_423635_
                        + ") is not large enough to write a rectangle of "
                        + p_410770_
                        + "x"
                        + p_423635_
                        + " at "
                        + p_410810_
                        + "x"
                        + p_409825_
                        + " (at mip level "
                        + p_410132_
                        + ")"
                );
            } else if (p_410473_.isClosed()) {
                throw new IllegalStateException("Destination texture is closed");
            } else if ((p_410473_.usage() & 1) == 0) {
                throw new IllegalStateException("Color texture must have USAGE_COPY_DST to be a destination for a write");
            } else if (p_409948_ >= p_410473_.getDepthOrLayers()) {
                throw new UnsupportedOperationException("Depth or layer is out of range, must be >= 0 and < " + p_410473_.getDepthOrLayers());
            } else {
                int i;
                if ((p_410473_.usage() & 16) != 0) {
                    i = GlConst.CUBEMAP_TARGETS[p_409948_ % 6];
                    GL11.glBindTexture(34067, ((GlTexture)p_410473_).id);
                } else {
                    i = 3553;
                    GlStateManager._bindTexture(((GlTexture)p_410473_).id);
                }

                GlStateManager._pixelStore(3314, p_423502_.getWidth());
                GlStateManager._pixelStore(3316, p_423531_);
                GlStateManager._pixelStore(3315, p_423633_);
                GlStateManager._pixelStore(3317, p_423502_.format().components());
                GlStateManager._texSubImage2D(
                    i, p_410132_, p_410810_, p_409825_, p_410770_, p_423635_, GlConst.toGl(p_423502_.format()), 5121, p_423502_.getPointer()
                );
            }
        } else {
            throw new IllegalArgumentException("Invalid mipLevel " + p_410132_ + ", must be >= 0 and < " + p_410473_.getMipLevels());
        }
    }

    @Override
    public void writeToTexture(
        GpuTexture p_409608_,
        ByteBuffer p_436678_,
        NativeImage.Format p_423578_,
        int p_410252_,
        int p_410814_,
        int p_410606_,
        int p_410618_,
        int p_410484_,
        int p_410120_
    ) {
        if (this.inRenderPass) {
            throw new IllegalStateException("Close the existing render pass before performing additional commands");
        } else if (p_410252_ >= 0 && p_410252_ < p_409608_.getMipLevels()) {
            if (p_410484_ * p_410120_ * p_423578_.components() > p_436678_.remaining()) {
                throw new IllegalArgumentException(
                    "Copy would overrun the source buffer (remaining length of "
                        + p_436678_.remaining()
                        + ", but copy is "
                        + p_410484_
                        + "x"
                        + p_410120_
                        + " of format "
                        + p_423578_
                        + ")"
                );
            } else if (p_410606_ + p_410484_ > p_409608_.getWidth(p_410252_) || p_410618_ + p_410120_ > p_409608_.getHeight(p_410252_)) {
                throw new IllegalArgumentException(
                    "Dest texture ("
                        + p_409608_.getWidth(p_410252_)
                        + "x"
                        + p_409608_.getHeight(p_410252_)
                        + ") is not large enough to write a rectangle of "
                        + p_410484_
                        + "x"
                        + p_410120_
                        + " at "
                        + p_410606_
                        + "x"
                        + p_410618_
                );
            } else if (p_409608_.isClosed()) {
                throw new IllegalStateException("Destination texture is closed");
            } else if ((p_409608_.usage() & 1) == 0) {
                throw new IllegalStateException("Color texture must have USAGE_COPY_DST to be a destination for a write");
            } else if (p_410814_ >= p_409608_.getDepthOrLayers()) {
                throw new UnsupportedOperationException("Depth or layer is out of range, must be >= 0 and < " + p_409608_.getDepthOrLayers());
            } else {
                int i;
                if ((p_409608_.usage() & 16) != 0) {
                    i = GlConst.CUBEMAP_TARGETS[p_410814_ % 6];
                    GL11.glBindTexture(34067, ((GlTexture)p_409608_).id);
                } else {
                    i = 3553;
                    GlStateManager._bindTexture(((GlTexture)p_409608_).id);
                }

                GlStateManager._pixelStore(3314, p_410484_);
                GlStateManager._pixelStore(3316, 0);
                GlStateManager._pixelStore(3315, 0);
                GlStateManager._pixelStore(3317, p_423578_.components());
                GlStateManager._texSubImage2D(i, p_410252_, p_410606_, p_410618_, p_410484_, p_410120_, GlConst.toGl(p_423578_), 5121, p_436678_);
            }
        } else {
            throw new IllegalArgumentException("Invalid mipLevel, must be >= 0 and < " + p_409608_.getMipLevels());
        }
    }

    @Override
    public void copyTextureToBuffer(GpuTexture p_410088_, GpuBuffer p_409674_, long p_482318_, Runnable p_410567_, int p_410546_) {
        if (this.inRenderPass) {
            throw new IllegalStateException("Close the existing render pass before performing additional commands");
        } else {
            this.copyTextureToBuffer(p_410088_, p_409674_, p_482318_, p_410567_, p_410546_, 0, 0, p_410088_.getWidth(p_410546_), p_410088_.getHeight(p_410546_));
        }
    }

    @Override
    public void copyTextureToBuffer(
        GpuTexture p_410781_,
        GpuBuffer p_410413_,
        long p_482330_,
        Runnable p_410081_,
        int p_410080_,
        int p_410819_,
        int p_409841_,
        int p_409880_,
        int p_409853_
    ) {
        if (this.inRenderPass) {
            throw new IllegalStateException("Close the existing render pass before performing additional commands");
        } else if (p_410080_ >= 0 && p_410080_ < p_410781_.getMipLevels()) {
            if (p_410781_.getWidth(p_410080_) * p_410781_.getHeight(p_410080_) * p_410781_.getFormat().pixelSize() + p_482330_ > p_410413_.size()) {
                throw new IllegalArgumentException(
                    "Buffer of size "
                        + p_410413_.size()
                        + " is not large enough to hold "
                        + p_409880_
                        + "x"
                        + p_409853_
                        + " pixels ("
                        + p_410781_.getFormat().pixelSize()
                        + " bytes each) starting from offset "
                        + p_482330_
                );
            } else if ((p_410781_.usage() & 2) == 0) {
                throw new IllegalArgumentException("Texture needs USAGE_COPY_SRC to be a source for a copy");
            } else if ((p_410413_.usage() & 8) == 0) {
                throw new IllegalArgumentException("Buffer needs USAGE_COPY_DST to be a destination for a copy");
            } else if (p_410819_ + p_409880_ > p_410781_.getWidth(p_410080_) || p_409841_ + p_409853_ > p_410781_.getHeight(p_410080_)) {
                throw new IllegalArgumentException(
                    "Copy source texture ("
                        + p_410781_.getWidth(p_410080_)
                        + "x"
                        + p_410781_.getHeight(p_410080_)
                        + ") is not large enough to read a rectangle of "
                        + p_409880_
                        + "x"
                        + p_409853_
                        + " from "
                        + p_410819_
                        + ","
                        + p_409841_
                );
            } else if (p_410781_.isClosed()) {
                throw new IllegalStateException("Source texture is closed");
            } else if (p_410413_.isClosed()) {
                throw new IllegalStateException("Destination buffer is closed");
            } else if (p_410781_.getDepthOrLayers() > 1) {
                throw new UnsupportedOperationException("Textures with multiple depths or layers are not yet supported for copying");
            } else {
                GlStateManager.clearGlErrors();
                this.device.directStateAccess().bindFrameBufferTextures(this.readFbo, ((GlTexture)p_410781_).glId(), 0, p_410080_, 36008);
                GlStateManager._glBindBuffer(35051, ((GlBuffer)p_410413_).handle);
                GlStateManager._pixelStore(3330, p_409880_);
                GlStateManager._readPixels(
                    p_410819_,
                    p_409841_,
                    p_409880_,
                    p_409853_,
                    GlConst.toGlExternalId(p_410781_.getFormat()),
                    GlConst.toGlType(p_410781_.getFormat()),
                    p_482330_
                );
                RenderSystem.queueFencedTask(p_410081_);
                GlStateManager._glFramebufferTexture2D(36008, 36064, 3553, 0, p_410080_);
                GlStateManager._glBindFramebuffer(36008, 0);
                GlStateManager._glBindBuffer(35051, 0);
                int i = GlStateManager._getError();
                if (i != 0) {
                    throw new IllegalStateException("Couldn't perform copyTobuffer for texture " + p_410781_.getLabel() + ": GL error " + i);
                }
            }
        } else {
            throw new IllegalArgumentException("Invalid mipLevel " + p_410080_ + ", must be >= 0 and < " + p_410781_.getMipLevels());
        }
    }

    @Override
    public void copyTextureToTexture(
        GpuTexture p_410700_, GpuTexture p_410735_, int p_410458_, int p_409803_, int p_410236_, int p_410552_, int p_410677_, int p_409870_, int p_409949_
    ) {
        if (this.inRenderPass) {
            throw new IllegalStateException("Close the existing render pass before performing additional commands");
        } else if (p_410458_ >= 0 && p_410458_ < p_410700_.getMipLevels() && p_410458_ < p_410735_.getMipLevels()) {
            if (p_409803_ + p_409870_ > p_410735_.getWidth(p_410458_) || p_410236_ + p_409949_ > p_410735_.getHeight(p_410458_)) {
                throw new IllegalArgumentException(
                    "Dest texture ("
                        + p_410735_.getWidth(p_410458_)
                        + "x"
                        + p_410735_.getHeight(p_410458_)
                        + ") is not large enough to write a rectangle of "
                        + p_409870_
                        + "x"
                        + p_409949_
                        + " at "
                        + p_409803_
                        + "x"
                        + p_410236_
                );
            } else if (p_410552_ + p_409870_ > p_410700_.getWidth(p_410458_) || p_410677_ + p_409949_ > p_410700_.getHeight(p_410458_)) {
                throw new IllegalArgumentException(
                    "Source texture ("
                        + p_410700_.getWidth(p_410458_)
                        + "x"
                        + p_410700_.getHeight(p_410458_)
                        + ") is not large enough to read a rectangle of "
                        + p_409870_
                        + "x"
                        + p_409949_
                        + " at "
                        + p_410552_
                        + "x"
                        + p_410677_
                );
            } else if (p_410700_.isClosed()) {
                throw new IllegalStateException("Source texture is closed");
            } else if (p_410735_.isClosed()) {
                throw new IllegalStateException("Destination texture is closed");
            } else if ((p_410700_.usage() & 2) == 0) {
                throw new IllegalArgumentException("Texture needs USAGE_COPY_SRC to be a source for a copy");
            } else if ((p_410735_.usage() & 1) == 0) {
                throw new IllegalArgumentException("Texture needs USAGE_COPY_DST to be a destination for a copy");
            } else if (p_410700_.getDepthOrLayers() > 1) {
                throw new UnsupportedOperationException("Textures with multiple depths or layers are not yet supported for copying");
            } else if (p_410735_.getDepthOrLayers() > 1) {
                throw new UnsupportedOperationException("Textures with multiple depths or layers are not yet supported for copying");
            } else {
                GlStateManager.clearGlErrors();
                GlStateManager._disableScissorTest();
                boolean flag = p_410700_.getFormat().hasDepthAspect();
                int i = ((GlTexture)p_410700_).glId();
                int j = ((GlTexture)p_410735_).glId();
                var hasStencil = p_410700_.getFormat().hasStencilAspect();
                this.device.directStateAccess().bindFrameBufferTextures(this.readFbo, flag ? 0 : i, flag ? i : 0, 0, 0, hasStencil);
                this.device.directStateAccess().bindFrameBufferTextures(this.drawFbo, flag ? 0 : j, flag ? j : 0, 0, 0, hasStencil);
                var bufferMask = 0;
                if (p_410700_.getFormat().hasColorAspect()) {
                    bufferMask |= GlConst.GL_COLOR_BUFFER_BIT;
                }
                if (p_410700_.getFormat().hasDepthAspect()) {
                    bufferMask |= GlConst.GL_DEPTH_BUFFER_BIT;
                }
                if (p_410700_.getFormat().hasStencilAspect()) {
                    bufferMask |= GL11.GL_STENCIL_BUFFER_BIT;
                }
                this.device
                    .directStateAccess()
                    .blitFrameBuffers(
                        this.readFbo,
                        this.drawFbo,
                        p_410552_,
                        p_410677_,
                        p_409870_,
                        p_409949_,
                        p_409803_,
                        p_410236_,
                        p_409870_,
                        p_409949_,
                        bufferMask,
                        9728
                    );
                int k = GlStateManager._getError();
                if (k != 0) {
                    throw new IllegalStateException(
                        "Couldn't perform copyToTexture for texture " + p_410700_.getLabel() + " to " + p_410735_.getLabel() + ": GL error " + k
                    );
                }
            }
        } else {
            throw new IllegalArgumentException(
                "Invalid mipLevel " + p_410458_ + ", must be >= 0 and < " + p_410700_.getMipLevels() + " and < " + p_410735_.getMipLevels()
            );
        }
    }

    @Override
    public void presentTexture(GpuTextureView p_423537_) {
        if (this.inRenderPass) {
            throw new IllegalStateException("Close the existing render pass before performing additional commands");
        } else if (!p_423537_.texture().getFormat().hasColorAspect()) {
            throw new IllegalStateException("Cannot present a non-color texture!");
        } else if ((p_423537_.texture().usage() & 8) == 0) {
            throw new IllegalStateException("Color texture must have USAGE_RENDER_ATTACHMENT to presented to the screen");
        } else if (p_423537_.texture().getDepthOrLayers() > 1) {
            throw new UnsupportedOperationException("Textures with multiple depths or layers are not yet supported for presentation");
        } else {
            GlStateManager._disableScissorTest();
            GlStateManager._viewport(0, 0, p_423537_.getWidth(0), p_423537_.getHeight(0));
            GlStateManager._depthMask(true);
            GlStateManager._colorMask(true, true, true, true);
            this.device.directStateAccess().bindFrameBufferTextures(this.drawFbo, ((GlTexture)p_423537_.texture()).glId(), 0, 0, 0);
            this.device
                .directStateAccess()
                .blitFrameBuffers(
                    this.drawFbo, 0, 0, 0, p_423537_.getWidth(0), p_423537_.getHeight(0), 0, 0, p_423537_.getWidth(0), p_423537_.getHeight(0), 16384, 9728
                );
        }
    }

    @Override
    public GpuFence createFence() {
        if (this.inRenderPass) {
            throw new IllegalStateException("Close the existing render pass before performing additional commands");
        } else {
            return new GlFence();
        }
    }

    protected <T> void executeDrawMultiple(
        GlRenderPass renderPass,
        Collection<RenderPass.Draw<T>> draws,
        @Nullable GpuBuffer buffer,
        VertexFormat.@Nullable IndexType indexType,
        Collection<String> uniforms,
        T data
    ) {
        if (this.trySetup(renderPass, uniforms)) {
            if (indexType == null) {
                indexType = VertexFormat.IndexType.SHORT;
            }

            for (RenderPass.Draw<T> draw : draws) {
                VertexFormat.IndexType vertexformat$indextype = draw.indexType() == null ? indexType : draw.indexType();
                renderPass.setIndexBuffer(draw.indexBuffer() == null ? buffer : draw.indexBuffer(), vertexformat$indextype);
                renderPass.setVertexBuffer(draw.slot(), draw.vertexBuffer());
                if (GlRenderPass.VALIDATION) {
                    if (renderPass.indexBuffer == null) {
                        throw new IllegalStateException("Missing index buffer");
                    }

                    if (renderPass.indexBuffer.isClosed()) {
                        throw new IllegalStateException("Index buffer has been closed!");
                    }

                    if (renderPass.vertexBuffers[0] == null) {
                        throw new IllegalStateException("Missing vertex buffer at slot 0");
                    }

                    if (renderPass.vertexBuffers[0].isClosed()) {
                        throw new IllegalStateException("Vertex buffer at slot 0 has been closed!");
                    }
                }

                BiConsumer<T, RenderPass.UniformUploader> biconsumer = draw.uniformUploaderConsumer();
                if (biconsumer != null) {
                    biconsumer.accept(data, (p_482263_, p_482264_) -> {
                        if (renderPass.pipeline.program().getUniform(p_482263_) instanceof Uniform.Ubo(int i)) {
                            GL32.glBindBufferRange(35345, i, ((GlBuffer)p_482264_.buffer()).handle, p_482264_.offset(), p_482264_.length());
                        }
                    });
                }

                this.drawFromBuffers(renderPass, 0, draw.firstIndex(), draw.indexCount(), vertexformat$indextype, renderPass.pipeline, 1);
            }
        }
    }

    protected void executeDraw(GlRenderPass renderPass, int firstIndex, int index, int indexCount, VertexFormat.@Nullable IndexType indexType, int primCount) {
        if (this.trySetup(renderPass, Collections.emptyList())) {
            if (GlRenderPass.VALIDATION) {
                if (indexType != null) {
                    if (renderPass.indexBuffer == null) {
                        throw new IllegalStateException("Missing index buffer");
                    }

                    if (renderPass.indexBuffer.isClosed()) {
                        throw new IllegalStateException("Index buffer has been closed!");
                    }

                    if ((renderPass.indexBuffer.usage() & 64) == 0) {
                        throw new IllegalStateException("Index buffer must have GpuBuffer.USAGE_INDEX!");
                    }
                }

                GlRenderPipeline glrenderpipeline = renderPass.pipeline;
                if (renderPass.vertexBuffers[0] == null && glrenderpipeline != null && !glrenderpipeline.info().getVertexFormat().getElements().isEmpty()) {
                    throw new IllegalStateException("Vertex format contains elements but vertex buffer at slot 0 is null");
                }

                if (renderPass.vertexBuffers[0] != null && renderPass.vertexBuffers[0].isClosed()) {
                    throw new IllegalStateException("Vertex buffer at slot 0 has been closed!");
                }

                if (renderPass.vertexBuffers[0] != null && (renderPass.vertexBuffers[0].usage() & 32) == 0) {
                    throw new IllegalStateException("Vertex buffer must have GpuBuffer.USAGE_VERTEX!");
                }
            }

            this.drawFromBuffers(renderPass, firstIndex, index, indexCount, indexType, renderPass.pipeline, primCount);
        }
    }

    private void drawFromBuffers(
        GlRenderPass renderPass,
        int firstIndex,
        int index,
        int indexCount,
        VertexFormat.@Nullable IndexType indexType,
        GlRenderPipeline pipeline,
        int primCount
    ) {
        this.device.vertexArrayCache().bindVertexArray(pipeline.info().getVertexFormat(), (GlBuffer)renderPass.vertexBuffers[0]);
        if (indexType != null) {
            GlStateManager._glBindBuffer(34963, ((GlBuffer)renderPass.indexBuffer).handle);
            if (primCount > 1) {
                if (firstIndex > 0) {
                    GL32.glDrawElementsInstancedBaseVertex(
                        GlConst.toGl(pipeline.info().getVertexFormatMode()),
                        indexCount,
                        GlConst.toGl(indexType),
                        (long)index * indexType.bytes,
                        primCount,
                        firstIndex
                    );
                } else {
                    GL31.glDrawElementsInstanced(
                        GlConst.toGl(pipeline.info().getVertexFormatMode()), indexCount, GlConst.toGl(indexType), (long)index * indexType.bytes, primCount
                    );
                }
            } else if (firstIndex > 0) {
                GL32.glDrawElementsBaseVertex(
                    GlConst.toGl(pipeline.info().getVertexFormatMode()), indexCount, GlConst.toGl(indexType), (long)index * indexType.bytes, firstIndex
                );
            } else {
                GlStateManager._drawElements(
                    GlConst.toGl(pipeline.info().getVertexFormatMode()), indexCount, GlConst.toGl(indexType), (long)index * indexType.bytes
                );
            }
        } else if (primCount > 1) {
            GL31.glDrawArraysInstanced(GlConst.toGl(pipeline.info().getVertexFormatMode()), firstIndex, indexCount, primCount);
        } else {
            GlStateManager._drawArrays(GlConst.toGl(pipeline.info().getVertexFormatMode()), firstIndex, indexCount);
        }
    }

    private boolean trySetup(GlRenderPass renderPass, Collection<String> uniforms) {
        if (GlRenderPass.VALIDATION) {
            if (renderPass.pipeline == null) {
                throw new IllegalStateException("Can't draw without a render pipeline");
            }

            if (renderPass.pipeline.program() == GlProgram.INVALID_PROGRAM) {
                throw new IllegalStateException("Pipeline contains invalid shader program");
            }

            for (RenderPipeline.UniformDescription renderpipeline$uniformdescription : renderPass.pipeline.info().getUniforms()) {
                GpuBufferSlice gpubufferslice = renderPass.uniforms.get(renderpipeline$uniformdescription.name());
                if (!uniforms.contains(renderpipeline$uniformdescription.name())) {
                    if (gpubufferslice == null) {
                        throw new IllegalStateException(
                            "Missing uniform " + renderpipeline$uniformdescription.name() + " (should be " + renderpipeline$uniformdescription.type() + ")"
                        );
                    }

                    if (renderpipeline$uniformdescription.type() == UniformType.UNIFORM_BUFFER) {
                        if (gpubufferslice.buffer().isClosed()) {
                            throw new IllegalStateException("Uniform buffer " + renderpipeline$uniformdescription.name() + " is already closed");
                        }

                        if ((gpubufferslice.buffer().usage() & 128) == 0) {
                            throw new IllegalStateException("Uniform buffer " + renderpipeline$uniformdescription.name() + " must have GpuBuffer.USAGE_UNIFORM");
                        }
                    }

                    if (renderpipeline$uniformdescription.type() == UniformType.TEXEL_BUFFER) {
                        if (gpubufferslice.offset() != 0L || gpubufferslice.length() != gpubufferslice.buffer().size()) {
                            throw new IllegalStateException("Uniform texel buffers do not support a slice of a buffer, must be entire buffer");
                        }

                        if (renderpipeline$uniformdescription.textureFormat() == null) {
                            throw new IllegalStateException(
                                "Invalid uniform texel buffer " + renderpipeline$uniformdescription.name() + " (missing a texture format)"
                            );
                        }
                    }
                }
            }

            for (Entry<String, Uniform> entry : renderPass.pipeline.program().getUniforms().entrySet()) {
                if (entry.getValue() instanceof Uniform.Sampler) {
                    String s1 = entry.getKey();
                    GlRenderPass.TextureViewAndSampler glrenderpass$textureviewandsampler = renderPass.samplers.get(s1);
                    if (glrenderpass$textureviewandsampler == null) {
                        throw new IllegalStateException("Missing sampler " + s1);
                    }

                    GlTextureView gltextureview = glrenderpass$textureviewandsampler.view();
                    if (gltextureview.isClosed()) {
                        throw new IllegalStateException("Texture view " + s1 + " (" + gltextureview.texture().getLabel() + ") has been closed!");
                    }

                    if ((gltextureview.texture().usage() & 4) == 0) {
                        throw new IllegalStateException("Texture view " + s1 + " (" + gltextureview.texture().getLabel() + ") must have USAGE_TEXTURE_BINDING!");
                    }

                    if (glrenderpass$textureviewandsampler.sampler().isClosed()) {
                        throw new IllegalStateException("Sampler for " + s1 + " (" + gltextureview.texture().getLabel() + ") has been closed!");
                    }
                }
            }

            if (renderPass.pipeline.info().wantsDepthTexture() && !renderPass.hasDepthTexture()) {
                LOGGER.warn("Render pipeline {} wants a depth texture but none was provided - this is probably a bug", renderPass.pipeline.info().getLocation());
            }
        } else if (renderPass.pipeline == null || renderPass.pipeline.program() == GlProgram.INVALID_PROGRAM) {
            return false;
        }

        RenderPipeline renderpipeline = renderPass.pipeline.info();
        GlProgram glprogram = renderPass.pipeline.program();
        this.applyPipelineState(renderpipeline);
        boolean flag1 = this.lastProgram != glprogram;
        if (flag1) {
            GlStateManager._glUseProgram(glprogram.getProgramId());
            this.lastProgram = glprogram;
        }

        for (Entry<String, Uniform> entry1 : glprogram.getUniforms().entrySet()) {
            String s = entry1.getKey();
            boolean flag = renderPass.dirtyUniforms.contains(s);
            switch ((Uniform)entry1.getValue()) {
                case Uniform.Ubo(int j2):
                    int k = j2;
                    if (flag) {
                        GpuBufferSlice gpubufferslice1 = renderPass.uniforms.get(s);
                        GL32.glBindBufferRange(35345, k, ((GlBuffer)gpubufferslice1.buffer()).handle, gpubufferslice1.offset(), gpubufferslice1.length());
                    }
                    break;
                case Uniform.Utb(int l, int i1, TextureFormat textureformat, int i2):
                    int j1 = i2;
                    if (flag1 || flag) {
                        GlStateManager._glUniform1i(l, i1);
                    }

                    GlStateManager._activeTexture(33984 + i1);
                    GL11C.glBindTexture(35882, j1);
                    if (flag) {
                        GpuBufferSlice gpubufferslice2 = renderPass.uniforms.get(s);
                        GL31.glTexBuffer(35882, GlConst.toGlInternalId(textureformat), ((GlBuffer)gpubufferslice2.buffer()).handle);
                    }
                    break;
                case Uniform.Sampler(int $$23, int l1):
                    int k1 = l1;
                    GlRenderPass.TextureViewAndSampler glrenderpass$textureviewandsampler1 = renderPass.samplers.get(s);
                    if (glrenderpass$textureviewandsampler1 == null) {
                        break;
                    }

                    GlTextureView gltextureview1 = glrenderpass$textureviewandsampler1.view();
                    if (flag1 || flag) {
                        GlStateManager._glUniform1i($$23, k1);
                    }

                    GlStateManager._activeTexture(33984 + k1);
                    GlTexture gltexture = gltextureview1.texture();
                    int j;
                    if ((gltexture.usage() & 16) != 0) {
                        j = 34067;
                        GL11.glBindTexture(34067, gltexture.id);
                    } else {
                        j = 3553;
                        GlStateManager._bindTexture(gltexture.id);
                    }

                    GL33C.glBindSampler(k1, glrenderpass$textureviewandsampler1.sampler().getId());
                    GlStateManager._texParameter(j, 33084, gltextureview1.baseMipLevel());
                    GlStateManager._texParameter(j, 33085, gltextureview1.baseMipLevel() + gltextureview1.mipLevels() - 1);
                    break;
                default:
                    throw new MatchException(null, null);
            }
        }

        renderPass.dirtyUniforms.clear();
        if (renderPass.isScissorEnabled()) {
            GlStateManager._enableScissorTest();
            GlStateManager._scissorBox(renderPass.getScissorX(), renderPass.getScissorY(), renderPass.getScissorWidth(), renderPass.getScissorHeight());
        } else {
            GlStateManager._disableScissorTest();
        }

        var stencilTestOpt = renderPass.pipeline.info().getStencilTest();
        if (stencilTestOpt.isPresent()) {
            var stencilTest = stencilTestOpt.get();
            GlStateManager._enableStencilTest();
            var front = stencilTest.front();
            var back = stencilTest.back();
            if (front.equals(back)) {
                GlStateManager._stencilFunc(GlConst.toGl(front.compare()), stencilTest.referenceValue(), stencilTest.readMask());
                GlStateManager._stencilOp(GlConst.toGl(front.fail()), GlConst.toGl(front.depthFail()), GlConst.toGl(front.pass()));
            } else {
                GlStateManager._stencilFuncFront(GlConst.toGl(front.compare()), stencilTest.referenceValue(), stencilTest.readMask());
                GlStateManager._stencilFuncBack(GlConst.toGl(back.compare()), stencilTest.referenceValue(), stencilTest.readMask());
                GlStateManager._stencilOpFront(GlConst.toGl(front.fail()), GlConst.toGl(front.depthFail()), GlConst.toGl(front.pass()));
                GlStateManager._stencilOpBack(GlConst.toGl(back.fail()), GlConst.toGl(back.depthFail()), GlConst.toGl(back.pass()));
            }
            GlStateManager._stencilMask(stencilTest.writeMask());
        } else {
            GlStateManager._disableStencilTest();
        }

        return true;
    }

    private void applyPipelineState(RenderPipeline pipeline) {
        if (this.lastPipeline != pipeline) {
            this.lastPipeline = pipeline;
            if (pipeline.getDepthTestFunction() != DepthTestFunction.NO_DEPTH_TEST) {
                GlStateManager._enableDepthTest();
                GlStateManager._depthFunc(GlConst.toGl(pipeline.getDepthTestFunction()));
            } else {
                GlStateManager._disableDepthTest();
            }

            if (pipeline.isCull()) {
                GlStateManager._enableCull();
            } else {
                GlStateManager._disableCull();
            }

            if (pipeline.getBlendFunction().isPresent()) {
                GlStateManager._enableBlend();
                BlendFunction blendfunction = pipeline.getBlendFunction().get();
                GlStateManager._blendFuncSeparate(
                    GlConst.toGl(blendfunction.sourceColor()),
                    GlConst.toGl(blendfunction.destColor()),
                    GlConst.toGl(blendfunction.sourceAlpha()),
                    GlConst.toGl(blendfunction.destAlpha())
                );
            } else {
                GlStateManager._disableBlend();
            }

            GlStateManager._polygonMode(1032, GlConst.toGl(pipeline.getPolygonMode()));
            GlStateManager._depthMask(pipeline.isWriteDepth());
            GlStateManager._colorMask(pipeline.isWriteColor(), pipeline.isWriteColor(), pipeline.isWriteColor(), pipeline.isWriteAlpha());
            if (pipeline.getDepthBiasConstant() == 0.0F && pipeline.getDepthBiasScaleFactor() == 0.0F) {
                GlStateManager._disablePolygonOffset();
            } else {
                GlStateManager._polygonOffset(pipeline.getDepthBiasScaleFactor(), pipeline.getDepthBiasConstant());
                GlStateManager._enablePolygonOffset();
            }

            switch (pipeline.getColorLogic()) {
                case NONE:
                    GlStateManager._disableColorLogicOp();
                    break;
                case OR_REVERSE:
                    GlStateManager._enableColorLogicOp();
                    GlStateManager._logicOp(5387);
            }
        }
    }

    public void finishRenderPass() {
        this.inRenderPass = false;
        GlStateManager._glBindFramebuffer(36160, 0);
        this.device.debugLabels().popDebugGroup();
    }

    protected GlDevice getDevice() {
        return this.device;
    }

    @Override
    public GpuQuery timerQueryBegin() {
        RenderSystem.assertOnRenderThread();
        if (this.activeTimerQuery != null) {
            throw new IllegalStateException("A GL_TIME_ELAPSED query is already active");
        } else {
            int i = GL32C.glGenQueries();
            GL32C.glBeginQuery(35007, i);
            this.activeTimerQuery = new GlTimerQuery(i);
            return this.activeTimerQuery;
        }
    }

    @Override
    public void timerQueryEnd(GpuQuery p_457672_) {
        RenderSystem.assertOnRenderThread();
        if (p_457672_ != this.activeTimerQuery) {
            throw new IllegalStateException("Mismatched or duplicate GpuQuery when ending timerQuery");
        } else {
            GL32C.glEndQuery(35007);
            this.activeTimerQuery = null;
        }
    }
}
