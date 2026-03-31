package net.minecraft.client.gui.font;

import com.mojang.blaze3d.font.GlyphInfo;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.textures.GpuTextureView;
import com.mojang.blaze3d.vertex.VertexConsumer;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GlyphSource;
import net.minecraft.client.gui.font.glyphs.BakedGlyph;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.Identifier;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.joml.Matrix4f;

@OnlyIn(Dist.CLIENT)
public class AtlasGlyphProvider {
    static final GlyphInfo GLYPH_INFO = GlyphInfo.simple(8.0F);
    final TextureAtlas atlas;
    final GlyphRenderTypes renderTypes;
    private final GlyphSource missingWrapper;
    private final Map<Identifier, GlyphSource> wrapperCache = new HashMap<>();
    private final Function<Identifier, GlyphSource> spriteResolver;

    public AtlasGlyphProvider(TextureAtlas atlas) {
        this.atlas = atlas;
        this.renderTypes = GlyphRenderTypes.createForColorTexture(atlas.location());
        TextureAtlasSprite textureatlassprite = atlas.missingSprite();
        this.missingWrapper = this.createSprite(textureatlassprite);
        this.spriteResolver = p_465468_ -> {
            TextureAtlasSprite textureatlassprite1 = atlas.getSprite(p_465468_);
            return textureatlassprite1 == textureatlassprite ? this.missingWrapper : this.createSprite(textureatlassprite1);
        };
    }

    public GlyphSource sourceForSprite(Identifier path) {
        return this.wrapperCache.computeIfAbsent(path, this.spriteResolver);
    }

    private GlyphSource createSprite(final TextureAtlasSprite sprite) {
        return new SingleSpriteSource(
            new BakedGlyph() {
                @Override
                public GlyphInfo info() {
                    return AtlasGlyphProvider.GLYPH_INFO;
                }

                @Override
                public TextRenderable.Styled createGlyph(
                    float p_439717_, float p_439641_, int p_439964_, int p_439008_, Style p_439131_, float p_438969_, float p_439857_
                ) {
                    return new AtlasGlyphProvider.Instance(
                        AtlasGlyphProvider.this.renderTypes,
                        AtlasGlyphProvider.this.atlas.getTextureView(),
                        sprite,
                        p_439717_,
                        p_439641_,
                        p_439964_,
                        p_439008_,
                        p_439857_,
                        p_439131_
                    );
                }
            }
        );
    }

    @OnlyIn(Dist.CLIENT)
    record Instance(
        GlyphRenderTypes renderTypes,
        GpuTextureView textureView,
        TextureAtlasSprite sprite,
        float x,
        float y,
        int color,
        int shadowColor,
        float shadowOffset,
        Style style
    ) implements PlainTextRenderable {
        @Override
        public void renderSprite(Matrix4f pose, VertexConsumer consumer, int packedLight, float x, float y, float z, int color) {
            float f = x + this.left();
            float f1 = x + this.right();
            float f2 = y + this.top();
            float f3 = y + this.bottom();
            consumer.addVertex(pose, f, f2, z).setUv(this.sprite.getU0(), this.sprite.getV0()).setColor(color).setLight(packedLight);
            consumer.addVertex(pose, f, f3, z).setUv(this.sprite.getU0(), this.sprite.getV1()).setColor(color).setLight(packedLight);
            consumer.addVertex(pose, f1, f3, z).setUv(this.sprite.getU1(), this.sprite.getV1()).setColor(color).setLight(packedLight);
            consumer.addVertex(pose, f1, f2, z).setUv(this.sprite.getU1(), this.sprite.getV0()).setColor(color).setLight(packedLight);
        }

        /**
         * Neo: returns the {@link RenderType} to use for the given {@link Font.DisplayMode} and blur setting
         */
        public RenderType renderType(Font.DisplayMode p_181388_, boolean blur) {
            return this.renderTypes.select(p_181388_, blur);
        }

        /** @deprecated Neo: Use {@link #renderType(Font.DisplayMode, boolean)} instead */
        @Deprecated
        @Override
        public RenderType renderType(Font.DisplayMode p_440474_) {
            return this.renderTypes.select(p_440474_);
        }

        @Override
        public RenderPipeline guiPipeline() {
            return this.renderTypes.guiPipeline();
        }
    }
}
