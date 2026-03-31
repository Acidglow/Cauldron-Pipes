package net.minecraft.client.renderer.blockentity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import java.util.function.Consumer;
import net.minecraft.client.model.Model;
import net.minecraft.client.model.geom.EntityModelSet;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.object.banner.BannerFlagModel;
import net.minecraft.client.model.object.banner.BannerModel;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.state.BannerRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.special.SpecialModelRenderer;
import net.minecraft.client.renderer.state.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.model.Material;
import net.minecraft.client.resources.model.MaterialSet;
import net.minecraft.client.resources.model.ModelBakery;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Unit;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.block.BannerBlock;
import net.minecraft.world.level.block.WallBannerBlock;
import net.minecraft.world.level.block.entity.BannerBlockEntity;
import net.minecraft.world.level.block.entity.BannerPatternLayers;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.RotationSegment;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.joml.Vector3fc;
import org.jspecify.annotations.Nullable;

@OnlyIn(Dist.CLIENT)
public class BannerRenderer implements BlockEntityRenderer<BannerBlockEntity, BannerRenderState> {
    private static final int MAX_PATTERNS = 16;
    private static final float SIZE = 0.6666667F;
    private final MaterialSet materials;
    private final BannerModel standingModel;
    private final BannerModel wallModel;
    private final BannerFlagModel standingFlagModel;
    private final BannerFlagModel wallFlagModel;

    public BannerRenderer(BlockEntityRendererProvider.Context context) {
        this(context.entityModelSet(), context.materials());
    }

    public BannerRenderer(SpecialModelRenderer.BakingContext context) {
        this(context.entityModelSet(), context.materials());
    }

    public BannerRenderer(EntityModelSet modelSet, MaterialSet materials) {
        this.materials = materials;
        this.standingModel = new BannerModel(modelSet.bakeLayer(ModelLayers.STANDING_BANNER));
        this.wallModel = new BannerModel(modelSet.bakeLayer(ModelLayers.WALL_BANNER));
        this.standingFlagModel = new BannerFlagModel(modelSet.bakeLayer(ModelLayers.STANDING_BANNER_FLAG));
        this.wallFlagModel = new BannerFlagModel(modelSet.bakeLayer(ModelLayers.WALL_BANNER_FLAG));
    }

    public BannerRenderState createRenderState() {
        return new BannerRenderState();
    }

    public void extractRenderState(
        BannerBlockEntity p_446844_, BannerRenderState p_447143_, float p_446794_, Vec3 p_447357_, ModelFeatureRenderer.@Nullable CrumblingOverlay p_446651_
    ) {
        BlockEntityRenderer.super.extractRenderState(p_446844_, p_447143_, p_446794_, p_447357_, p_446651_);
        p_447143_.baseColor = p_446844_.getBaseColor();
        p_447143_.patterns = p_446844_.getPatterns();
        BlockState blockstate = p_446844_.getBlockState();
        if (blockstate.getBlock() instanceof BannerBlock) {
            p_447143_.angle = -RotationSegment.convertToDegrees(blockstate.getValue(BannerBlock.ROTATION));
            p_447143_.standing = true;
        } else {
            p_447143_.angle = -blockstate.getValue(WallBannerBlock.FACING).toYRot();
            p_447143_.standing = false;
        }

        long i = p_446844_.getLevel() != null ? p_446844_.getLevel().getGameTime() : 0L;
        BlockPos blockpos = p_446844_.getBlockPos();
        p_447143_.phase = ((float)Math.floorMod(blockpos.getX() * 7 + blockpos.getY() * 9 + blockpos.getZ() * 13 + i, 100L) + p_446794_) / 100.0F;
    }

    public void submit(BannerRenderState p_445505_, PoseStack p_439672_, SubmitNodeCollector p_439893_, CameraRenderState p_451262_) {
        BannerModel bannermodel;
        BannerFlagModel bannerflagmodel;
        if (p_445505_.standing) {
            bannermodel = this.standingModel;
            bannerflagmodel = this.standingFlagModel;
        } else {
            bannermodel = this.wallModel;
            bannerflagmodel = this.wallFlagModel;
        }

        submitBanner(
            this.materials,
            p_439672_,
            p_439893_,
            p_445505_.lightCoords,
            OverlayTexture.NO_OVERLAY,
            p_445505_.angle,
            bannermodel,
            bannerflagmodel,
            p_445505_.phase,
            p_445505_.baseColor,
            p_445505_.patterns,
            p_445505_.breakProgress,
            0
        );
    }

    public void submitSpecial(
        PoseStack poseStack, SubmitNodeCollector nodeCollector, int packedLight, int packedOverlay, DyeColor baseColor, BannerPatternLayers patterns, int outlineColor
    ) {
        submitBanner(
            this.materials,
            poseStack,
            nodeCollector,
            packedLight,
            packedOverlay,
            0.0F,
            this.standingModel,
            this.standingFlagModel,
            0.0F,
            baseColor,
            patterns,
            null,
            outlineColor
        );
    }

    private static void submitBanner(
        MaterialSet materials,
        PoseStack poseStack,
        SubmitNodeCollector nodeCollector,
        int packedLight,
        int packedOverlay,
        float rotation,
        BannerModel standingModel,
        BannerFlagModel standingFlagModel,
        float phase,
        DyeColor baseColor,
        BannerPatternLayers patterns,
        ModelFeatureRenderer.@Nullable CrumblingOverlay breakProgress,
        int outlineColor
    ) {
        poseStack.pushPose();
        poseStack.translate(0.5F, 0.0F, 0.5F);
        poseStack.mulPose(Axis.YP.rotationDegrees(rotation));
        poseStack.scale(0.6666667F, -0.6666667F, -0.6666667F);
        Material material = ModelBakery.BANNER_BASE;
        nodeCollector.submitModel(
            standingModel,
            Unit.INSTANCE,
            poseStack,
            material.renderType(RenderTypes::entitySolid),
            packedLight,
            packedOverlay,
            -1,
            materials.get(material),
            outlineColor,
            breakProgress
        );
        submitPatterns(
            materials, poseStack, nodeCollector, packedLight, packedOverlay, standingFlagModel, phase, material, true, baseColor, patterns, false, breakProgress, outlineColor
        );
        poseStack.popPose();
    }

    public static <S> void submitPatterns(
        MaterialSet materials,
        PoseStack poseStack,
        SubmitNodeCollector nodeCollector,
        int packedLight,
        int packedOverlay,
        Model<S> flag,
        S renderState,
        Material p_material,
        boolean banner,
        DyeColor baseColor,
        BannerPatternLayers patterns,
        boolean withGlint,
        ModelFeatureRenderer.@Nullable CrumblingOverlay crumblingOverlay,
        int outlineColor
    ) {
        nodeCollector.submitModel(
            flag,
            renderState,
            poseStack,
            p_material.renderType(RenderTypes::entitySolid),
            packedLight,
            packedOverlay,
            -1,
            materials.get(p_material),
            outlineColor,
            crumblingOverlay
        );
        if (withGlint) {
            nodeCollector.submitModel(flag, renderState, poseStack, RenderTypes.entityGlint(), packedLight, packedOverlay, -1, materials.get(p_material), 0, crumblingOverlay);
        }

        submitPatternLayer(
            materials,
            poseStack,
            nodeCollector,
            packedLight,
            packedOverlay,
            flag,
            renderState,
            banner ? Sheets.BANNER_BASE : Sheets.SHIELD_BASE,
            baseColor,
            crumblingOverlay
        );

        for (int i = 0; i < 16 && i < patterns.layers().size(); i++) {
            BannerPatternLayers.Layer bannerpatternlayers$layer = patterns.layers().get(i);
            Material material = banner
                ? Sheets.getBannerMaterial(bannerpatternlayers$layer.pattern())
                : Sheets.getShieldMaterial(bannerpatternlayers$layer.pattern());
            submitPatternLayer(materials, poseStack, nodeCollector, packedLight, packedOverlay, flag, renderState, material, bannerpatternlayers$layer.color(), null);
        }
    }

    private static <S> void submitPatternLayer(
        MaterialSet materials,
        PoseStack poseStack,
        SubmitNodeCollector nodeCollector,
        int packedLight,
        int packedOverlay,
        Model<S> flagModel,
        S sway,
        Material material,
        DyeColor color,
        ModelFeatureRenderer.@Nullable CrumblingOverlay crumblingOverlay
    ) {
        int i = color.getTextureDiffuseColor();
        nodeCollector.submitModel(
            flagModel,
            sway,
            poseStack,
            material.renderType(RenderTypes::entityNoOutline),
            packedLight,
            packedOverlay,
            i,
            materials.get(material),
            0,
            crumblingOverlay
        );
    }

    public void getExtents(Consumer<Vector3fc> output) {
        PoseStack posestack = new PoseStack();
        posestack.translate(0.5F, 0.0F, 0.5F);
        posestack.scale(0.6666667F, -0.6666667F, -0.6666667F);
        this.standingModel.root().getExtentsForGui(posestack, output);
        this.standingFlagModel.setupAnim(0.0F);
        this.standingFlagModel.root().getExtentsForGui(posestack, output);
    }

    @Override
    public net.minecraft.world.phys.AABB getRenderBoundingBox(BannerBlockEntity blockEntity) {
        BlockPos pos = blockEntity.getBlockPos();
        boolean standing = blockEntity.getBlockState().getBlock() instanceof BannerBlock;
        return net.minecraft.world.phys.AABB.encapsulatingFullBlocks(pos, standing ? pos.above() : pos.below());
    }
}
