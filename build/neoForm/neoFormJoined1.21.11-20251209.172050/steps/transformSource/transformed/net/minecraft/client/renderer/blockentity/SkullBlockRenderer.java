package net.minecraft.client.renderer.blockentity;

import com.google.common.collect.Maps;
import com.mojang.blaze3d.vertex.PoseStack;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;
import net.minecraft.client.model.geom.EntityModelSet;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.object.skull.DragonHeadModel;
import net.minecraft.client.model.object.skull.PiglinHeadModel;
import net.minecraft.client.model.object.skull.SkullModel;
import net.minecraft.client.model.object.skull.SkullModelBase;
import net.minecraft.client.renderer.PlayerSkinRenderCache;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.state.SkullBlockRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.DefaultPlayerSkin;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Util;
import net.minecraft.world.item.component.ResolvableProfile;
import net.minecraft.world.level.block.AbstractSkullBlock;
import net.minecraft.world.level.block.SkullBlock;
import net.minecraft.world.level.block.WallSkullBlock;
import net.minecraft.world.level.block.entity.SkullBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.RotationSegment;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.jspecify.annotations.Nullable;

@OnlyIn(Dist.CLIENT)
public class SkullBlockRenderer implements BlockEntityRenderer<SkullBlockEntity, SkullBlockRenderState> {
    private final Function<SkullBlock.Type, SkullModelBase> modelByType;
    public static final Map<SkullBlock.Type, Identifier> SKIN_BY_TYPE = Util.make(Maps.newHashMap(), p_465624_ -> {
        p_465624_.put(SkullBlock.Types.SKELETON, Identifier.withDefaultNamespace("textures/entity/skeleton/skeleton.png"));
        p_465624_.put(SkullBlock.Types.WITHER_SKELETON, Identifier.withDefaultNamespace("textures/entity/skeleton/wither_skeleton.png"));
        p_465624_.put(SkullBlock.Types.ZOMBIE, Identifier.withDefaultNamespace("textures/entity/zombie/zombie.png"));
        p_465624_.put(SkullBlock.Types.CREEPER, Identifier.withDefaultNamespace("textures/entity/creeper/creeper.png"));
        p_465624_.put(SkullBlock.Types.DRAGON, Identifier.withDefaultNamespace("textures/entity/enderdragon/dragon.png"));
        p_465624_.put(SkullBlock.Types.PIGLIN, Identifier.withDefaultNamespace("textures/entity/piglin/piglin.png"));
        p_465624_.put(SkullBlock.Types.PLAYER, DefaultPlayerSkin.getDefaultTexture());
    });
    private final PlayerSkinRenderCache playerSkinRenderCache;

    public static @Nullable SkullModelBase createModel(EntityModelSet modelSet, SkullBlock.Type type) {
        if (type instanceof SkullBlock.Types skullblock$types) {
            return (SkullModelBase)(switch (skullblock$types) {
                case SKELETON -> new SkullModel(modelSet.bakeLayer(ModelLayers.SKELETON_SKULL));
                case WITHER_SKELETON -> new SkullModel(modelSet.bakeLayer(ModelLayers.WITHER_SKELETON_SKULL));
                case PLAYER -> new SkullModel(modelSet.bakeLayer(ModelLayers.PLAYER_HEAD));
                case ZOMBIE -> new SkullModel(modelSet.bakeLayer(ModelLayers.ZOMBIE_HEAD));
                case CREEPER -> new SkullModel(modelSet.bakeLayer(ModelLayers.CREEPER_HEAD));
                case DRAGON -> new DragonHeadModel(modelSet.bakeLayer(ModelLayers.DRAGON_SKULL));
                case PIGLIN -> new PiglinHeadModel(modelSet.bakeLayer(ModelLayers.PIGLIN_HEAD));
            });
        } else {
            return net.neoforged.neoforge.client.ClientHooks.getModdedSkullModel(modelSet, type); // Neo: Lookup model for modded skull types
        }
    }

    public SkullBlockRenderer(BlockEntityRendererProvider.Context context) {
        EntityModelSet entitymodelset = context.entityModelSet();
        this.playerSkinRenderCache = context.playerSkinRenderCache();
        this.modelByType = Util.memoize(p_477732_ -> createModel(entitymodelset, p_477732_));
    }

    public SkullBlockRenderState createRenderState() {
        return new SkullBlockRenderState();
    }

    public void extractRenderState(
        SkullBlockEntity p_446674_, SkullBlockRenderState p_446649_, float p_447371_, Vec3 p_445526_, ModelFeatureRenderer.@Nullable CrumblingOverlay p_446270_
    ) {
        BlockEntityRenderer.super.extractRenderState(p_446674_, p_446649_, p_447371_, p_445526_, p_446270_);
        p_446649_.animationProgress = p_446674_.getAnimation(p_447371_);
        BlockState blockstate = p_446674_.getBlockState();
        boolean flag = blockstate.getBlock() instanceof WallSkullBlock;
        p_446649_.direction = flag ? blockstate.getValue(WallSkullBlock.FACING) : null;
        int i = flag ? RotationSegment.convertToSegment(p_446649_.direction.getOpposite()) : blockstate.getValue(SkullBlock.ROTATION);
        p_446649_.rotationDegrees = RotationSegment.convertToDegrees(i);
        p_446649_.skullType = ((AbstractSkullBlock)blockstate.getBlock()).getType();
        p_446649_.renderType = this.resolveSkullRenderType(p_446649_.skullType, p_446674_);
    }

    public void submit(SkullBlockRenderState p_445740_, PoseStack p_440693_, SubmitNodeCollector p_440619_, CameraRenderState p_451265_) {
        SkullModelBase skullmodelbase = this.modelByType.apply(p_445740_.skullType);
        submitSkull(
            p_445740_.direction,
            p_445740_.rotationDegrees,
            p_445740_.animationProgress,
            p_440693_,
            p_440619_,
            p_445740_.lightCoords,
            skullmodelbase,
            p_445740_.renderType,
            0,
            p_445740_.breakProgress
        );
    }

    public static void submitSkull(
        @Nullable Direction direction,
        float rotation,
        float animationProgress,
        PoseStack poseStack,
        SubmitNodeCollector nodeCollector,
        int packedLight,
        SkullModelBase model,
        RenderType renderType,
        int outlineColor,
        ModelFeatureRenderer.@Nullable CrumblingOverlay breakProgress
    ) {
        poseStack.pushPose();
        if (direction == null) {
            poseStack.translate(0.5F, 0.0F, 0.5F);
        } else {
            float f = 0.25F;
            poseStack.translate(0.5F - direction.getStepX() * 0.25F, 0.25F, 0.5F - direction.getStepZ() * 0.25F);
        }

        poseStack.scale(-1.0F, -1.0F, 1.0F);
        SkullModelBase.State skullmodelbase$state = new SkullModelBase.State();
        skullmodelbase$state.animationPos = animationProgress;
        skullmodelbase$state.yRot = rotation;
        nodeCollector.submitModel(model, skullmodelbase$state, poseStack, renderType, packedLight, OverlayTexture.NO_OVERLAY, outlineColor, breakProgress);
        poseStack.popPose();
    }

    private RenderType resolveSkullRenderType(SkullBlock.Type skullType, SkullBlockEntity blockEntity) {
        if (skullType == SkullBlock.Types.PLAYER) {
            ResolvableProfile resolvableprofile = blockEntity.getOwnerProfile();
            if (resolvableprofile != null) {
                return this.playerSkinRenderCache.getOrDefault(resolvableprofile).renderType();
            }
        }

        return getSkullRenderType(skullType, null);
    }

    public static RenderType getSkullRenderType(SkullBlock.Type skullType, @Nullable Identifier texture) {
        return RenderTypes.entityCutoutNoCullZOffset(texture != null ? texture : SKIN_BY_TYPE.get(skullType));
    }

    public static RenderType getPlayerSkinRenderType(Identifier texture) {
        return RenderTypes.entityTranslucent(texture);
    }

    @Override
    public net.minecraft.world.phys.AABB getRenderBoundingBox(SkullBlockEntity blockEntity) {
        SkullBlock.Type type = ((AbstractSkullBlock) blockEntity.getBlockState().getBlock()).getType();
        if (type == SkullBlock.Types.DRAGON) {
            net.minecraft.core.BlockPos pos = blockEntity.getBlockPos();
            return new net.minecraft.world.phys.AABB(pos.getX() - .75, pos.getY() - .35, pos.getZ() - .75, pos.getX() + 1.75, pos.getY() + 1.0, pos.getZ() + 1.75);
        }
        return BlockEntityRenderer.super.getRenderBoundingBox(blockEntity);
    }
}
