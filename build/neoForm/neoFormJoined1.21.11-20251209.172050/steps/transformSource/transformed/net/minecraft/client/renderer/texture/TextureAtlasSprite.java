package net.minecraft.client.renderer.texture;

import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.buffers.Std140Builder;
import com.mojang.blaze3d.textures.GpuTexture;
import com.mojang.blaze3d.vertex.VertexConsumer;
import java.nio.ByteBuffer;
import net.minecraft.client.renderer.SpriteCoordinateExpander;
import net.minecraft.resources.Identifier;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.joml.Matrix4f;
import org.jspecify.annotations.Nullable;
import org.lwjgl.system.MemoryUtil;

@OnlyIn(Dist.CLIENT)
public class TextureAtlasSprite implements AutoCloseable {
    private final Identifier atlasLocation;
    private final SpriteContents contents;
    private final int x;
    private final int y;
    private final float u0;
    private final float u1;
    private final float v0;
    private final float v1;
    private final int padding;

    protected TextureAtlasSprite(Identifier atlasLocation, SpriteContents contents, int textureWidth, int textureHeight, int x, int y, int padding) {
        this.atlasLocation = atlasLocation;
        this.contents = contents;
        this.padding = padding;
        this.x = x;
        this.y = y;
        this.u0 = (float)(x + padding) / textureWidth;
        this.u1 = (float)(x + padding + contents.width()) / textureWidth;
        this.v0 = (float)(y + padding) / textureHeight;
        this.v1 = (float)(y + padding + contents.height()) / textureHeight;
    }

    public int getX() {
        return this.x;
    }

    public int getY() {
        return this.y;
    }

    public float getU0() {
        return this.u0;
    }

    public float getU1() {
        return this.u1;
    }

    public SpriteContents contents() {
        return this.contents;
    }

    public SpriteContents.@Nullable AnimationState createAnimationState(GpuBufferSlice buffer, int size) {
        return this.contents.createAnimationState(buffer, size);
    }

    public float getU(float u) {
        float f = this.u1 - this.u0;
        return this.u0 + f * u;
    }

    public float getV0() {
        return this.v0;
    }

    public float getV1() {
        return this.v1;
    }

    public float getV(float v) {
        float f = this.v1 - this.v0;
        return this.v0 + f * v;
    }

    public Identifier atlasLocation() {
        return this.atlasLocation;
    }

    @Override
    public String toString() {
        return "TextureAtlasSprite{contents='" + this.contents + "', u0=" + this.u0 + ", u1=" + this.u1 + ", v0=" + this.v0 + ", v1=" + this.v1 + "}";
    }

    public void uploadFirstFrame(GpuTexture texture, int mipLevel) {
        this.contents.uploadFirstFrame(texture, mipLevel);
    }

    public VertexConsumer wrap(VertexConsumer consumer) {
        return new SpriteCoordinateExpander(consumer, this);
    }

    boolean isAnimated() {
        return this.contents.isAnimated();
    }

    public void uploadSpriteUbo(ByteBuffer buffer, int offset, int maxMipLevel, int width, int height, int length) {
        for (int i = 0; i <= maxMipLevel; i++) {
            Std140Builder.intoBuffer(MemoryUtil.memSlice(buffer, offset + i * length, length))
                .putMat4f(new Matrix4f().ortho2D(0.0F, width >> i, 0.0F, height >> i))
                .putMat4f(
                    new Matrix4f()
                        .translate(this.x >> i, this.y >> i, 0.0F)
                        .scale(this.contents.width() + this.padding * 2 >> i, this.contents.height() + this.padding * 2 >> i, 1.0F)
                )
                .putFloat((float)this.padding / this.contents.width())
                .putFloat((float)this.padding / this.contents.height())
                .putInt(i);
        }
    }

    @Override
    public void close() {
        this.contents.close();
    }

    // Neo: Exposed a pixel RGBA getter
    public int getPixelRGBA(int frameIndex, int x, int y) {
         if (this.contents.animatedTexture != null) {
              x += this.contents.animatedTexture.getFrameX(frameIndex) * this.contents.width;
              y += this.contents.animatedTexture.getFrameY(frameIndex) * this.contents.height;
         }

         return this.contents.getOriginalImage().getPixel(x, y);
    }
}
