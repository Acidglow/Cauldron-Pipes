package net.minecraft.client.renderer.special;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.mojang.serialization.codecs.RecordCodecBuilder.Instance;
import java.util.function.Consumer;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.ShulkerBoxRenderer;
import net.minecraft.client.resources.model.Material;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemDisplayContext;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.joml.Vector3fc;

@OnlyIn(Dist.CLIENT)
public class ShulkerBoxSpecialRenderer implements NoDataSpecialModelRenderer {
    private final ShulkerBoxRenderer shulkerBoxRenderer;
    private final float openness;
    private final Direction orientation;
    private final Material material;

    public ShulkerBoxSpecialRenderer(ShulkerBoxRenderer shulkerBoxRenderer, float openness, Direction orientation, Material material) {
        this.shulkerBoxRenderer = shulkerBoxRenderer;
        this.openness = openness;
        this.orientation = orientation;
        this.material = material;
    }

    @Override
    public void submit(
        ItemDisplayContext p_439715_, PoseStack p_439819_, SubmitNodeCollector p_439007_, int p_439286_, int p_439124_, boolean p_438934_, int p_451704_
    ) {
        this.shulkerBoxRenderer.submit(p_439819_, p_439007_, p_439286_, p_439124_, this.orientation, this.openness, null, this.material, p_451704_);
    }

    @Override
    public void getExtents(Consumer<Vector3fc> p_470710_) {
        this.shulkerBoxRenderer.getExtents(this.orientation, this.openness, p_470710_);
    }

    @OnlyIn(Dist.CLIENT)
    public record Unbaked(Identifier texture, float openness, Direction orientation) implements SpecialModelRenderer.Unbaked {
        public static final MapCodec<ShulkerBoxSpecialRenderer.Unbaked> MAP_CODEC = RecordCodecBuilder.mapCodec(
            p_465707_ -> p_465707_.group(
                    Identifier.CODEC.fieldOf("texture").forGetter(ShulkerBoxSpecialRenderer.Unbaked::texture),
                    Codec.FLOAT.optionalFieldOf("openness", 0.0F).forGetter(ShulkerBoxSpecialRenderer.Unbaked::openness),
                    Direction.CODEC.optionalFieldOf("orientation", Direction.UP).forGetter(ShulkerBoxSpecialRenderer.Unbaked::orientation)
                )
                .apply(p_465707_, ShulkerBoxSpecialRenderer.Unbaked::new)
        );

        public Unbaked() {
            this(Identifier.withDefaultNamespace("shulker"), 0.0F, Direction.UP);
        }

        public Unbaked(DyeColor p_388305_) {
            this(Sheets.colorToShulkerMaterial(p_388305_), 0.0F, Direction.UP);
        }

        @Override
        public MapCodec<ShulkerBoxSpecialRenderer.Unbaked> type() {
            return MAP_CODEC;
        }

        @Override
        public SpecialModelRenderer<?> bake(SpecialModelRenderer.BakingContext p_435748_) {
            return new ShulkerBoxSpecialRenderer(new ShulkerBoxRenderer(p_435748_), this.openness, this.orientation, Sheets.SHULKER_MAPPER.apply(this.texture));
        }
    }
}
