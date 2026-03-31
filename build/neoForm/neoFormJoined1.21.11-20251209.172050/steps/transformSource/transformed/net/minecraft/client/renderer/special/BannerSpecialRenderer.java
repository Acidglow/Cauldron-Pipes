package net.minecraft.client.renderer.special;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.mojang.serialization.codecs.RecordCodecBuilder.Instance;
import java.util.Objects;
import java.util.function.Consumer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BannerRenderer;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BannerPatternLayers;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.joml.Vector3fc;
import org.jspecify.annotations.Nullable;

@OnlyIn(Dist.CLIENT)
public class BannerSpecialRenderer implements SpecialModelRenderer<BannerPatternLayers> {
    private final BannerRenderer bannerRenderer;
    private final DyeColor baseColor;

    public BannerSpecialRenderer(DyeColor baseColor, BannerRenderer bannerRenderer) {
        this.bannerRenderer = bannerRenderer;
        this.baseColor = baseColor;
    }

    public @Nullable BannerPatternLayers extractArgument(ItemStack p_387879_) {
        return p_387879_.get(DataComponents.BANNER_PATTERNS);
    }

    public void submit(
        @Nullable BannerPatternLayers p_451667_,
        ItemDisplayContext p_440222_,
        PoseStack p_439452_,
        SubmitNodeCollector p_440039_,
        int p_439962_,
        int p_439234_,
        boolean p_440507_,
        int p_451701_
    ) {
        this.bannerRenderer
            .submitSpecial(
                p_439452_, p_440039_, p_439962_, p_439234_, this.baseColor, Objects.requireNonNullElse(p_451667_, BannerPatternLayers.EMPTY), p_451701_
            );
    }

    @Override
    public void getExtents(Consumer<Vector3fc> p_470668_) {
        this.bannerRenderer.getExtents(p_470668_);
    }

    @OnlyIn(Dist.CLIENT)
    public record Unbaked(DyeColor baseColor) implements SpecialModelRenderer.Unbaked {
        public static final MapCodec<BannerSpecialRenderer.Unbaked> MAP_CODEC = RecordCodecBuilder.mapCodec(
            p_386477_ -> p_386477_.group(DyeColor.CODEC.fieldOf("color").forGetter(BannerSpecialRenderer.Unbaked::baseColor))
                .apply(p_386477_, BannerSpecialRenderer.Unbaked::new)
        );

        @Override
        public MapCodec<BannerSpecialRenderer.Unbaked> type() {
            return MAP_CODEC;
        }

        @Override
        public SpecialModelRenderer<?> bake(SpecialModelRenderer.BakingContext p_434507_) {
            return new BannerSpecialRenderer(this.baseColor, new BannerRenderer(p_434507_));
        }
    }
}
