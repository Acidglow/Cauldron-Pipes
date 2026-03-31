package net.minecraft.client.renderer.special;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.serialization.MapCodec;
import java.util.Objects;
import java.util.function.Consumer;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.object.equipment.ShieldModel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BannerRenderer;
import net.minecraft.client.resources.model.Material;
import net.minecraft.client.resources.model.MaterialSet;
import net.minecraft.client.resources.model.ModelBakery;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.component.DataComponents;
import net.minecraft.util.Unit;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BannerPatternLayers;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.joml.Vector3fc;
import org.jspecify.annotations.Nullable;

@OnlyIn(Dist.CLIENT)
public class ShieldSpecialRenderer implements SpecialModelRenderer<DataComponentMap> {
    private final MaterialSet materials;
    private final ShieldModel model;

    public ShieldSpecialRenderer(MaterialSet materials, ShieldModel model) {
        this.materials = materials;
        this.model = model;
    }

    public @Nullable DataComponentMap extractArgument(ItemStack p_387204_) {
        return p_387204_.immutableComponents();
    }

    public void submit(
        @Nullable DataComponentMap p_439858_,
        ItemDisplayContext p_439137_,
        PoseStack p_439913_,
        SubmitNodeCollector p_440473_,
        int p_439119_,
        int p_439273_,
        boolean p_440377_,
        int p_451692_
    ) {
        BannerPatternLayers bannerpatternlayers = p_439858_ != null
            ? p_439858_.getOrDefault(DataComponents.BANNER_PATTERNS, BannerPatternLayers.EMPTY)
            : BannerPatternLayers.EMPTY;
        DyeColor dyecolor = p_439858_ != null ? p_439858_.get(DataComponents.BASE_COLOR) : null;
        boolean flag = !bannerpatternlayers.layers().isEmpty() || dyecolor != null;
        p_439913_.pushPose();
        p_439913_.scale(1.0F, -1.0F, -1.0F);
        Material material = flag ? ModelBakery.SHIELD_BASE : ModelBakery.NO_PATTERN_SHIELD;
        p_440473_.submitModelPart(
            this.model.handle(),
            p_439913_,
            this.model.renderType(material.atlasLocation()),
            p_439119_,
            p_439273_,
            this.materials.get(material),
            false,
            false,
            -1,
            null,
            p_451692_
        );
        if (flag) {
            BannerRenderer.submitPatterns(
                this.materials,
                p_439913_,
                p_440473_,
                p_439119_,
                p_439273_,
                this.model,
                Unit.INSTANCE,
                material,
                false,
                Objects.requireNonNullElse(dyecolor, DyeColor.WHITE),
                bannerpatternlayers,
                p_440377_,
                null,
                p_451692_
            );
        } else {
            p_440473_.submitModelPart(
                this.model.plate(),
                p_439913_,
                this.model.renderType(material.atlasLocation()),
                p_439119_,
                p_439273_,
                this.materials.get(material),
                false,
                p_440377_,
                -1,
                null,
                p_451692_
            );
        }

        p_439913_.popPose();
    }

    @Override
    public void getExtents(Consumer<Vector3fc> p_470572_) {
        PoseStack posestack = new PoseStack();
        posestack.scale(1.0F, -1.0F, -1.0F);
        this.model.root().getExtentsForGui(posestack, p_470572_);
    }

    @OnlyIn(Dist.CLIENT)
    public record Unbaked() implements SpecialModelRenderer.Unbaked {
        public static final ShieldSpecialRenderer.Unbaked INSTANCE = new ShieldSpecialRenderer.Unbaked();
        public static final MapCodec<ShieldSpecialRenderer.Unbaked> MAP_CODEC = MapCodec.unit(INSTANCE);

        @Override
        public MapCodec<ShieldSpecialRenderer.Unbaked> type() {
            return MAP_CODEC;
        }

        @Override
        public SpecialModelRenderer<?> bake(SpecialModelRenderer.BakingContext p_434068_) {
            return new ShieldSpecialRenderer(p_434068_.materials(), new ShieldModel(p_434068_.entityModelSet().bakeLayer(ModelLayers.SHIELD)));
        }
    }
}
