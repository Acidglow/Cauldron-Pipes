package com.mojang.blaze3d.opengl;

import com.mojang.blaze3d.textures.GpuTexture;
import com.mojang.blaze3d.textures.GpuTextureView;
import it.unimi.dsi.fastutil.ints.Int2IntArrayMap;
import it.unimi.dsi.fastutil.ints.Int2IntMap;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.jspecify.annotations.Nullable;

@OnlyIn(Dist.CLIENT)
public class GlTextureView extends GpuTextureView {
    private static final int EMPTY = -1;
    private boolean closed;
    private int firstFboId = -1;
    private int firstFboDepthId = -1;
    private @Nullable Int2IntMap fboCache;

    protected GlTextureView(GlTexture texture, int baseMipLevel, int mipLevels) {
        super(texture, baseMipLevel, mipLevels);
        texture.addViews();
    }

    @Override
    public boolean isClosed() {
        return this.closed;
    }

    @Override
    public void close() {
        if (!this.closed) {
            this.closed = true;
            this.texture().removeViews();
            if (this.firstFboId != -1) {
                GlStateManager._glDeleteFramebuffers(this.firstFboId);
            }

            if (this.fboCache != null) {
                for (int i : this.fboCache.values()) {
                    GlStateManager._glDeleteFramebuffers(i);
                }
            }
        }
    }

    public int getFbo(DirectStateAccess directStateAccess, @Nullable GpuTexture texture) {
        int i = texture == null ? 0 : ((GlTexture)texture).id;
        if (this.firstFboDepthId == i) {
            return this.firstFboId;
        } else if (this.firstFboId == -1) {
            this.firstFboId = this.createFbo(directStateAccess, i);
            this.firstFboDepthId = i;
            return this.firstFboId;
        } else {
            if (this.fboCache == null) {
                this.fboCache = new Int2IntArrayMap();
            }

            return this.fboCache.computeIfAbsent(i, p_470463_ -> this.createFbo(directStateAccess, p_470463_));
        }
    }

    private int createFbo(DirectStateAccess directStateAccess, int id) {
        int i = directStateAccess.createFrameBufferObject();
        directStateAccess.bindFrameBufferTextures(i, this.texture().id, id, this.baseMipLevel(), 0);
        return i;
    }

    public GlTexture texture() {
        return (GlTexture)super.texture();
    }
}
