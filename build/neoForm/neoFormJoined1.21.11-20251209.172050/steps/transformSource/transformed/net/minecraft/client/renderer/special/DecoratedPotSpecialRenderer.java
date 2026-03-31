package net.minecraft.client.renderer.special;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.serialization.MapCodec;
import java.util.Objects;
import java.util.function.Consumer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.DecoratedPotRenderer;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.PotDecorations;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.joml.Vector3fc;
import org.jspecify.annotations.Nullable;

@OnlyIn(Dist.CLIENT)
public class DecoratedPotSpecialRenderer implements SpecialModelRenderer<PotDecorations> {
    private final DecoratedPotRenderer decoratedPotRenderer;

    public DecoratedPotSpecialRenderer(DecoratedPotRenderer decoratedPotRenderer) {
        this.decoratedPotRenderer = decoratedPotRenderer;
    }

    public @Nullable PotDecorations extractArgument(ItemStack p_386678_) {
        return p_386678_.get(DataComponents.POT_DECORATIONS);
    }

    public void submit(
        @Nullable PotDecorations p_451706_,
        ItemDisplayContext p_440702_,
        PoseStack p_438956_,
        SubmitNodeCollector p_439353_,
        int p_439981_,
        int p_439623_,
        boolean p_440093_,
        int p_451684_
    ) {
        this.decoratedPotRenderer.submit(p_438956_, p_439353_, p_439981_, p_439623_, Objects.requireNonNullElse(p_451706_, PotDecorations.EMPTY), p_451684_);
    }

    @Override
    public void getExtents(Consumer<Vector3fc> p_470724_) {
        this.decoratedPotRenderer.getExtents(p_470724_);
    }

    @OnlyIn(Dist.CLIENT)
    public record Unbaked() implements SpecialModelRenderer.Unbaked {
        public static final MapCodec<DecoratedPotSpecialRenderer.Unbaked> MAP_CODEC = MapCodec.unit(new DecoratedPotSpecialRenderer.Unbaked());

        @Override
        public MapCodec<DecoratedPotSpecialRenderer.Unbaked> type() {
            return MAP_CODEC;
        }

        @Override
        public SpecialModelRenderer<?> bake(SpecialModelRenderer.BakingContext p_435515_) {
            return new DecoratedPotSpecialRenderer(new DecoratedPotRenderer(p_435515_));
        }
    }
}
