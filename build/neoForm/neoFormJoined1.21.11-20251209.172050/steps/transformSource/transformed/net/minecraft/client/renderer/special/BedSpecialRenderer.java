package net.minecraft.client.renderer.special;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.mojang.serialization.codecs.RecordCodecBuilder.Instance;
import java.util.function.Consumer;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BedRenderer;
import net.minecraft.client.resources.model.Material;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemDisplayContext;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.joml.Vector3fc;

@OnlyIn(Dist.CLIENT)
public class BedSpecialRenderer implements NoDataSpecialModelRenderer {
    private final BedRenderer bedRenderer;
    private final Material material;

    public BedSpecialRenderer(BedRenderer bedRenderer, Material material) {
        this.bedRenderer = bedRenderer;
        this.material = material;
    }

    @Override
    public void submit(
        ItemDisplayContext p_440115_, PoseStack p_439551_, SubmitNodeCollector p_440198_, int p_439951_, int p_439919_, boolean p_440058_, int p_451700_
    ) {
        this.bedRenderer.submitSpecial(p_439551_, p_440198_, p_439951_, p_439919_, this.material, p_451700_);
    }

    @Override
    public void getExtents(Consumer<Vector3fc> p_470701_) {
        this.bedRenderer.getExtents(p_470701_);
    }

    @OnlyIn(Dist.CLIENT)
    public record Unbaked(Identifier texture) implements SpecialModelRenderer.Unbaked {
        public static final MapCodec<BedSpecialRenderer.Unbaked> MAP_CODEC = RecordCodecBuilder.mapCodec(
            p_465703_ -> p_465703_.group(Identifier.CODEC.fieldOf("texture").forGetter(BedSpecialRenderer.Unbaked::texture))
                .apply(p_465703_, BedSpecialRenderer.Unbaked::new)
        );

        public Unbaked(DyeColor p_386855_) {
            this(Sheets.colorToResourceMaterial(p_386855_));
        }

        @Override
        public MapCodec<BedSpecialRenderer.Unbaked> type() {
            return MAP_CODEC;
        }

        @Override
        public SpecialModelRenderer<?> bake(SpecialModelRenderer.BakingContext p_435542_) {
            return new BedSpecialRenderer(new BedRenderer(p_435542_), Sheets.BED_MAPPER.apply(this.texture));
        }
    }
}
