package net.minecraft.client.renderer.special;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.mojang.serialization.codecs.RecordCodecBuilder.Instance;
import java.util.Optional;
import java.util.function.Consumer;
import net.minecraft.client.model.object.skull.SkullModelBase;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.SkullBlockRenderer;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.level.block.SkullBlock;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.joml.Vector3fc;
import org.jspecify.annotations.Nullable;

@OnlyIn(Dist.CLIENT)
public class SkullSpecialRenderer implements NoDataSpecialModelRenderer {
    private final SkullModelBase model;
    private final float animation;
    private final RenderType renderType;

    public SkullSpecialRenderer(SkullModelBase model, float animation, RenderType renderType) {
        this.model = model;
        this.animation = animation;
        this.renderType = renderType;
    }

    @Override
    public void submit(
        ItemDisplayContext p_439965_, PoseStack p_440519_, SubmitNodeCollector p_439018_, int p_440585_, int p_439887_, boolean p_440182_, int p_451695_
    ) {
        SkullBlockRenderer.submitSkull(null, 180.0F, this.animation, p_440519_, p_439018_, p_440585_, this.model, this.renderType, p_451695_, null);
    }

    @Override
    public void getExtents(Consumer<Vector3fc> p_470705_) {
        PoseStack posestack = new PoseStack();
        posestack.translate(0.5F, 0.0F, 0.5F);
        posestack.scale(-1.0F, -1.0F, 1.0F);
        SkullModelBase.State skullmodelbase$state = new SkullModelBase.State();
        skullmodelbase$state.animationPos = this.animation;
        skullmodelbase$state.yRot = 180.0F;
        this.model.setupAnim(skullmodelbase$state);
        this.model.root().getExtentsForGui(posestack, p_470705_);
    }

    @OnlyIn(Dist.CLIENT)
    public record Unbaked(SkullBlock.Type kind, Optional<Identifier> textureOverride, float animation) implements SpecialModelRenderer.Unbaked {
        public static final MapCodec<SkullSpecialRenderer.Unbaked> MAP_CODEC = RecordCodecBuilder.mapCodec(
            p_465709_ -> p_465709_.group(
                    SkullBlock.Type.CODEC.fieldOf("kind").forGetter(SkullSpecialRenderer.Unbaked::kind),
                    Identifier.CODEC.optionalFieldOf("texture").forGetter(SkullSpecialRenderer.Unbaked::textureOverride),
                    Codec.FLOAT.optionalFieldOf("animation", 0.0F).forGetter(SkullSpecialRenderer.Unbaked::animation)
                )
                .apply(p_465709_, SkullSpecialRenderer.Unbaked::new)
        );

        public Unbaked(SkullBlock.Type p_387200_) {
            this(p_387200_, Optional.empty(), 0.0F);
        }

        @Override
        public MapCodec<SkullSpecialRenderer.Unbaked> type() {
            return MAP_CODEC;
        }

        @Override
        public @Nullable SpecialModelRenderer<?> bake(SpecialModelRenderer.BakingContext p_436037_) {
            SkullModelBase skullmodelbase = SkullBlockRenderer.createModel(p_436037_.entityModelSet(), this.kind);
            Identifier identifier = this.textureOverride
                .<Identifier>map(p_465708_ -> p_465708_.withPath(p_389344_ -> "textures/entity/" + p_389344_ + ".png"))
                .orElse(null);
            if (skullmodelbase == null) {
                return null;
            } else {
                RenderType rendertype = SkullBlockRenderer.getSkullRenderType(this.kind, identifier);
                return new SkullSpecialRenderer(skullmodelbase, this.animation, rendertype);
            }
        }
    }
}
