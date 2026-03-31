package com.mojang.blaze3d.opengl;

import com.mojang.blaze3d.textures.GpuTexture;
import com.mojang.blaze3d.textures.TextureFormat;
import it.unimi.dsi.fastutil.ints.Int2IntArrayMap;
import it.unimi.dsi.fastutil.ints.Int2IntMap;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.jspecify.annotations.Nullable;

@OnlyIn(Dist.CLIENT)
public class GlTexture extends GpuTexture {
    private static final int EMPTY = -1;
    protected final int id;
    private int firstFboId = -1;
    private int firstFboDepthId = -1;
    private @Nullable Int2IntMap fboCache;
    protected boolean closed;
    private int views;
    protected final boolean external; // If true, the raw OpenGL texture is not managed by this GpuTexture

    protected GlTexture(
        @GpuTexture.Usage int usage, String label, TextureFormat format, int width, int height, int depthOrLayers, int mipLevels, int id
    ) {
        this(usage, label, format, width, height, depthOrLayers, mipLevels, id, false);
    }

    protected GlTexture(
        @GpuTexture.Usage int usage, String label, TextureFormat format, int width, int height, int depthOrLayers, int mipLevels, int id, boolean external
    ) {
        super(usage, label, format, width, height, depthOrLayers, mipLevels);
        this.id = id;
        this.external = external;
    }

    @Override
    public void close() {
        if (!this.closed && !this.external) {
            this.closed = true;
            if (this.views == 0) {
                this.destroyImmediately();
            }
        }
    }

    private void destroyImmediately() {
        GlStateManager._deleteTexture(this.id);
        if (this.firstFboId != -1) {
            GlStateManager._glDeleteFramebuffers(this.firstFboId);
        }

        if (this.fboCache != null) {
            for (int i : this.fboCache.values()) {
                GlStateManager._glDeleteFramebuffers(i);
            }
        }
    }

    @Override
    public boolean isClosed() {
        return this.closed;
    }

    public int getFbo(DirectStateAccess directStateAccess, @Nullable GpuTexture texture) {
        int i = texture == null ? 0 : ((GlTexture)texture).id;
        var useStencil = texture != null && texture.getFormat().hasStencilAspect();
        if (this.firstFboDepthId == i) {
            return this.firstFboId;
        } else if (this.firstFboId == -1) {
            this.firstFboId = this.createFbo(directStateAccess, i, useStencil);
            this.firstFboDepthId = i;
            return this.firstFboId;
        } else {
            if (this.fboCache == null) {
                this.fboCache = new Int2IntArrayMap();
            }

            return this.fboCache.computeIfAbsent(i, p_470461_ -> this.createFbo(directStateAccess, p_470461_, useStencil));
        }
    }

    /**
 * @deprecated use overload that takes useStencil
 */
    @Deprecated
    private int createFbo(DirectStateAccess directStateAccess, int id) {
        return createFbo(directStateAccess, id, false);
    }

    // Neo: Allow for stencil use
    private int createFbo(DirectStateAccess directStateAccess, int id, boolean useStencil) {
        int i = directStateAccess.createFrameBufferObject();
        directStateAccess.bindFrameBufferTextures(i, this.id, id, 0, 0, useStencil);
        return i;
    }

    public int glId() {
        return this.id;
    }

    public void addViews() {
        this.views++;
    }

    public void removeViews() {
        this.views--;
        if (this.closed && this.views == 0) {
            this.destroyImmediately();
        }
    }
}
