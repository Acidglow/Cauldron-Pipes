package net.minecraft.client.renderer.special;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.mojang.serialization.codecs.RecordCodecBuilder.Instance;
import java.util.function.Consumer;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.object.chest.ChestModel;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.resources.model.Material;
import net.minecraft.client.resources.model.MaterialSet;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemDisplayContext;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.joml.Vector3fc;

@OnlyIn(Dist.CLIENT)
public class ChestSpecialRenderer implements NoDataSpecialModelRenderer {
    public static final Identifier GIFT_CHEST_TEXTURE = Identifier.withDefaultNamespace("christmas");
    public static final Identifier NORMAL_CHEST_TEXTURE = Identifier.withDefaultNamespace("normal");
    public static final Identifier TRAPPED_CHEST_TEXTURE = Identifier.withDefaultNamespace("trapped");
    public static final Identifier ENDER_CHEST_TEXTURE = Identifier.withDefaultNamespace("ender");
    public static final Identifier COPPER_CHEST_TEXTURE = Identifier.withDefaultNamespace("copper");
    public static final Identifier EXPOSED_COPPER_CHEST_TEXTURE = Identifier.withDefaultNamespace("copper_exposed");
    public static final Identifier WEATHERED_COPPER_CHEST_TEXTURE = Identifier.withDefaultNamespace("copper_weathered");
    public static final Identifier OXIDIZED_COPPER_CHEST_TEXTURE = Identifier.withDefaultNamespace("copper_oxidized");
    private final MaterialSet materials;
    private final ChestModel model;
    private final Material material;
    private final float openness;

    public ChestSpecialRenderer(MaterialSet materials, ChestModel model, Material material, float openness) {
        this.materials = materials;
        this.model = model;
        this.material = material;
        this.openness = openness;
    }

    @Override
    public void submit(
        ItemDisplayContext p_440682_, PoseStack p_439468_, SubmitNodeCollector p_440053_, int p_440465_, int p_440260_, boolean p_439501_, int p_451677_
    ) {
        p_440053_.submitModel(
            this.model,
            this.openness,
            p_439468_,
            this.material.renderType(RenderTypes::entitySolid),
            p_440465_,
            p_440260_,
            -1,
            this.materials.get(this.material),
            p_451677_,
            null
        );
    }

    @Override
    public void getExtents(Consumer<Vector3fc> p_470834_) {
        PoseStack posestack = new PoseStack();
        this.model.setupAnim(this.openness);
        this.model.root().getExtentsForGui(posestack, p_470834_);
    }

    @OnlyIn(Dist.CLIENT)
    public record Unbaked(Identifier texture, float openness) implements SpecialModelRenderer.Unbaked {
        public static final MapCodec<ChestSpecialRenderer.Unbaked> MAP_CODEC = RecordCodecBuilder.mapCodec(
            p_465704_ -> p_465704_.group(
                    Identifier.CODEC.fieldOf("texture").forGetter(ChestSpecialRenderer.Unbaked::texture),
                    Codec.FLOAT.optionalFieldOf("openness", 0.0F).forGetter(ChestSpecialRenderer.Unbaked::openness)
                )
                .apply(p_465704_, ChestSpecialRenderer.Unbaked::new)
        );

        public Unbaked(Identifier p_468986_) {
            this(p_468986_, 0.0F);
        }

        @Override
        public MapCodec<ChestSpecialRenderer.Unbaked> type() {
            return MAP_CODEC;
        }

        @Override
        public SpecialModelRenderer<?> bake(SpecialModelRenderer.BakingContext p_434841_) {
            ChestModel chestmodel = new ChestModel(p_434841_.entityModelSet().bakeLayer(ModelLayers.CHEST));
            Material material = Sheets.CHEST_MAPPER.apply(this.texture);
            return new ChestSpecialRenderer(p_434841_.materials(), chestmodel, material, this.openness);
        }
    }
}
