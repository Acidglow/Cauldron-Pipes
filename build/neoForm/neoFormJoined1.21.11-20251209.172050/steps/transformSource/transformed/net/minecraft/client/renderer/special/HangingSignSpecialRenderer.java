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
import net.minecraft.client.renderer.blockentity.HangingSignRenderer;
import net.minecraft.client.resources.model.Material;
import net.minecraft.client.resources.model.MaterialSet;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.level.block.state.properties.WoodType;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.joml.Vector3fc;

@OnlyIn(Dist.CLIENT)
public class HangingSignSpecialRenderer implements NoDataSpecialModelRenderer {
    private final MaterialSet materials;
    private final Model.Simple model;
    private final Material material;

    public HangingSignSpecialRenderer(MaterialSet materials, Model.Simple model, Material material) {
        this.materials = materials;
        this.model = model;
        this.material = material;
    }

    @Override
    public void submit(
        ItemDisplayContext p_439248_, PoseStack p_439308_, SubmitNodeCollector p_439707_, int p_440428_, int p_440626_, boolean p_439097_, int p_451690_
    ) {
        HangingSignRenderer.submitSpecial(this.materials, p_439308_, p_439707_, p_440428_, p_440626_, this.model, this.material);
    }

    @Override
    public void getExtents(Consumer<Vector3fc> p_470729_) {
        PoseStack posestack = new PoseStack();
        HangingSignRenderer.translateBase(posestack, 0.0F);
        posestack.scale(1.0F, -1.0F, -1.0F);
        this.model.root().getExtentsForGui(posestack, p_470729_);
    }

    @OnlyIn(Dist.CLIENT)
    public record Unbaked(WoodType woodType, Optional<Identifier> texture) implements SpecialModelRenderer.Unbaked {
        public static final MapCodec<HangingSignSpecialRenderer.Unbaked> MAP_CODEC = RecordCodecBuilder.mapCodec(
            p_465706_ -> p_465706_.group(
                    WoodType.CODEC.fieldOf("wood_type").forGetter(HangingSignSpecialRenderer.Unbaked::woodType),
                    Identifier.CODEC.optionalFieldOf("texture").forGetter(HangingSignSpecialRenderer.Unbaked::texture)
                )
                .apply(p_465706_, HangingSignSpecialRenderer.Unbaked::new)
        );

        public Unbaked(WoodType p_389680_) {
            this(p_389680_, Optional.empty());
        }

        @Override
        public MapCodec<HangingSignSpecialRenderer.Unbaked> type() {
            return MAP_CODEC;
        }

        @Override
        public SpecialModelRenderer<?> bake(SpecialModelRenderer.BakingContext p_434530_) {
            Model.Simple model$simple = HangingSignRenderer.createSignModel(
                p_434530_.entityModelSet(), this.woodType, HangingSignRenderer.AttachmentType.CEILING_MIDDLE
            );
            Material material = this.texture.map(Sheets.HANGING_SIGN_MAPPER::apply).orElseGet(() -> Sheets.getHangingSignMaterial(this.woodType));
            return new HangingSignSpecialRenderer(p_434530_.materials(), model$simple, material);
        }
    }
}
