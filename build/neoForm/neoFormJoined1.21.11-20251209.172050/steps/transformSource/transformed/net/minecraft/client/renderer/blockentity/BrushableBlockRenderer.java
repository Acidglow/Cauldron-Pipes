package net.minecraft.client.renderer.blockentity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.state.BrushableBlockRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.client.renderer.state.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.level.block.entity.BrushableBlockEntity;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.jspecify.annotations.Nullable;

@OnlyIn(Dist.CLIENT)
public class BrushableBlockRenderer implements BlockEntityRenderer<BrushableBlockEntity, BrushableBlockRenderState> {
    private final ItemModelResolver itemModelResolver;

    public BrushableBlockRenderer(BlockEntityRendererProvider.Context context) {
        this.itemModelResolver = context.itemModelResolver();
    }

    public BrushableBlockRenderState createRenderState() {
        return new BrushableBlockRenderState();
    }

    public void extractRenderState(
        BrushableBlockEntity p_446197_,
        BrushableBlockRenderState p_445907_,
        float p_446852_,
        Vec3 p_447180_,
        ModelFeatureRenderer.@Nullable CrumblingOverlay p_445754_
    ) {
        BlockEntityRenderer.super.extractRenderState(p_446197_, p_445907_, p_446852_, p_447180_, p_445754_);
        p_445907_.hitDirection = p_446197_.getHitDirection();
        p_445907_.dustProgress = p_446197_.getBlockState().getValue(BlockStateProperties.DUSTED);
        if (p_446197_.getLevel() != null && p_446197_.getHitDirection() != null) {
            p_445907_.lightCoords = LevelRenderer.getLightColor(
                LevelRenderer.BrightnessGetter.DEFAULT,
                p_446197_.getLevel(),
                p_446197_.getBlockState(),
                p_446197_.getBlockPos().relative(p_446197_.getHitDirection())
            );
        }

        this.itemModelResolver.updateForTopItem(p_445907_.itemState, p_446197_.getItem(), ItemDisplayContext.FIXED, p_446197_.getLevel(), null, 0);
    }

    public void submit(BrushableBlockRenderState p_445580_, PoseStack p_439885_, SubmitNodeCollector p_439013_, CameraRenderState p_450972_) {
        if (p_445580_.dustProgress > 0 && p_445580_.hitDirection != null && !p_445580_.itemState.isEmpty()) {
            p_439885_.pushPose();
            p_439885_.translate(0.0F, 0.5F, 0.0F);
            float[] afloat = this.translations(p_445580_.hitDirection, p_445580_.dustProgress);
            p_439885_.translate(afloat[0], afloat[1], afloat[2]);
            p_439885_.mulPose(Axis.YP.rotationDegrees(75.0F));
            boolean flag = p_445580_.hitDirection == Direction.EAST || p_445580_.hitDirection == Direction.WEST;
            p_439885_.mulPose(Axis.YP.rotationDegrees((flag ? 90 : 0) + 11));
            p_439885_.scale(0.5F, 0.5F, 0.5F);
            p_445580_.itemState.submit(p_439885_, p_439013_, p_445580_.lightCoords, OverlayTexture.NO_OVERLAY, 0);
            p_439885_.popPose();
        }
    }

    private float[] translations(Direction direction, int dustedLevel) {
        float[] afloat = new float[]{0.5F, 0.0F, 0.5F};
        float f = dustedLevel / 10.0F * 0.75F;
        switch (direction) {
            case EAST:
                afloat[0] = 0.73F + f;
                break;
            case WEST:
                afloat[0] = 0.25F - f;
                break;
            case UP:
                afloat[1] = 0.25F + f;
                break;
            case DOWN:
                afloat[1] = -0.23F - f;
                break;
            case NORTH:
                afloat[2] = 0.25F - f;
                break;
            case SOUTH:
                afloat[2] = 0.73F + f;
        }

        return afloat;
    }

    @Override
    public net.minecraft.world.phys.AABB getRenderBoundingBox(BrushableBlockEntity blockEntity) {
        net.minecraft.core.BlockPos pos = blockEntity.getBlockPos();
        return new net.minecraft.world.phys.AABB(pos.getX() - .25, pos.getY() - .25, pos.getZ() - .25, pos.getX() + 1.25, pos.getY() + 1.25, pos.getZ() + 1.25);
    }
}
