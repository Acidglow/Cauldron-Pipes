package net.minecraft.client.renderer.special;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.mojang.serialization.codecs.RecordCodecBuilder.Instance;
import java.util.Optional;
import java.util.function.Consumer;
import net.minecraft.client.model.Model;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.SignRenderer;
import net.minecraft.client.resources.model.Material;
import net.minecraft.client.resources.model.MaterialSet;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.level.block.state.properties.WoodType;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.joml.Vector3fc;

@OnlyIn(Dist.CLIENT)
public class StandingSignSpecialRenderer implements NoDataSpecialModelRenderer {
    private final MaterialSet materials;
    private final Model.Simple model;
    private final Material material;

    public StandingSignSpecialRenderer(MaterialSet materials, Model.Simple model, Material material) {
        this.materials = materials;
        this.model = model;
        this.material = material;
    }

    @Override
    public void submit(
        ItemDisplayContext p_440327_, PoseStack p_438876_, SubmitNodeCollector p_440467_, int p_439536_, int p_439265_, boolean p_439509_, int p_451680_
    ) {
        SignRenderer.submitSpecial(this.materials, p_438876_, p_440467_, p_439536_, p_439265_, this.model, this.material);
    }

    @Override
    public void getExtents(Consumer<Vector3fc> p_470554_) {
        PoseStack posestack = new PoseStack();
        SignRenderer.applyInHandTransforms(posestack);
        this.model.root().getExtentsForGui(posestack, p_470554_);
    }

    @OnlyIn(Dist.CLIENT)
    public record Unbaked(WoodType woodType, Optional<Identifier> texture) implements SpecialModelRenderer.Unbaked {
        public static final MapCodec<StandingSignSpecialRenderer.Unbaked> MAP_CODEC = RecordCodecBuilder.mapCodec(
            p_465710_ -> p_465710_.group(
                    WoodType.CODEC.fieldOf("wood_type").forGetter(StandingSignSpecialRenderer.Unbaked::woodType),
                    Identifier.CODEC.optionalFieldOf("texture").forGetter(StandingSignSpecialRenderer.Unbaked::texture)
                )
                .apply(p_465710_, StandingSignSpecialRenderer.Unbaked::new)
        );

        public Unbaked(WoodType p_389712_) {
            this(p_389712_, Optional.empty());
        }

        @Override
        public MapCodec<StandingSignSpecialRenderer.Unbaked> type() {
            return MAP_CODEC;
        }

        @Override
        public SpecialModelRenderer<?> bake(SpecialModelRenderer.BakingContext p_436063_) {
            Model.Simple model$simple = SignRenderer.createSignModel(p_436063_.entityModelSet(), this.woodType, true);
            Material material = this.texture.map(Sheets.SIGN_MAPPER::apply).orElseGet(() -> Sheets.getSignMaterial(this.woodType));
            return new StandingSignSpecialRenderer(p_436063_.materials(), model$simple, material);
        }
    }
}
