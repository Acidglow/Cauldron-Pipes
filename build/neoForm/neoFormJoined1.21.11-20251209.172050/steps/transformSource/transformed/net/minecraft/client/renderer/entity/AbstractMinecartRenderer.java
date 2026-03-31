package net.minecraft.client.renderer.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import java.util.Objects;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.object.cart.MinecartModel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.state.MinecartRenderState;
import net.minecraft.client.renderer.state.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.vehicle.minecart.AbstractMinecart;
import net.minecraft.world.entity.vehicle.minecart.NewMinecartBehavior;
import net.minecraft.world.entity.vehicle.minecart.OldMinecartBehavior;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public abstract class AbstractMinecartRenderer<T extends AbstractMinecart, S extends MinecartRenderState> extends EntityRenderer<T, S> {
    private static final Identifier MINECART_LOCATION = Identifier.withDefaultNamespace("textures/entity/minecart.png");
    private static final float DISPLAY_BLOCK_SCALE = 0.75F;
    protected final MinecartModel model;

    public AbstractMinecartRenderer(EntityRendererProvider.Context context, ModelLayerLocation modelLayer) {
        super(context);
        this.shadowRadius = 0.7F;
        this.model = new MinecartModel(context.bakeLayer(modelLayer));
    }

    public void submit(S p_451220_, PoseStack p_434007_, SubmitNodeCollector p_435160_, CameraRenderState p_451310_) {
        super.submit(p_451220_, p_434007_, p_435160_, p_451310_);
        p_434007_.pushPose();
        long i = p_451220_.offsetSeed;
        float f = (((float)(i >> 16 & 7L) + 0.5F) / 8.0F - 0.5F) * 0.004F;
        float f1 = (((float)(i >> 20 & 7L) + 0.5F) / 8.0F - 0.5F) * 0.004F;
        float f2 = (((float)(i >> 24 & 7L) + 0.5F) / 8.0F - 0.5F) * 0.004F;
        p_434007_.translate(f, f1, f2);
        if (p_451220_.isNewRender) {
            newRender(p_451220_, p_434007_);
        } else {
            oldRender(p_451220_, p_434007_);
        }

        float f3 = p_451220_.hurtTime;
        if (f3 > 0.0F) {
            p_434007_.mulPose(Axis.XP.rotationDegrees(Mth.sin(f3) * f3 * p_451220_.damageTime / 10.0F * p_451220_.hurtDir));
        }

        BlockState blockstate = p_451220_.displayBlockState;
        if (blockstate.getRenderShape() != RenderShape.INVISIBLE) {
            p_434007_.pushPose();
            p_434007_.scale(0.75F, 0.75F, 0.75F);
            p_434007_.translate(-0.5F, (p_451220_.displayOffset - 8) / 16.0F, 0.5F);
            p_434007_.mulPose(Axis.YP.rotationDegrees(90.0F));
            this.submitMinecartContents(p_451220_, blockstate, p_434007_, p_435160_, p_451220_.lightCoords);
            p_434007_.popPose();
        }

        p_434007_.scale(-1.0F, -1.0F, 1.0F);
        p_435160_.submitModel(
            this.model,
            p_451220_,
            p_434007_,
            this.model.renderType(MINECART_LOCATION),
            p_451220_.lightCoords,
            OverlayTexture.NO_OVERLAY,
            p_451220_.outlineColor,
            null
        );
        p_434007_.popPose();
    }

    private static <S extends MinecartRenderState> void newRender(S renderState, PoseStack poseStack) {
        poseStack.mulPose(Axis.YP.rotationDegrees(renderState.yRot));
        poseStack.mulPose(Axis.ZP.rotationDegrees(-renderState.xRot));
        poseStack.translate(0.0F, 0.375F, 0.0F);
    }

    private static <S extends MinecartRenderState> void oldRender(S renderState, PoseStack poseStack) {
        double d0 = renderState.x;
        double d1 = renderState.y;
        double d2 = renderState.z;
        float f = renderState.xRot;
        float f1 = renderState.yRot;
        if (renderState.posOnRail != null && renderState.frontPos != null && renderState.backPos != null) {
            Vec3 vec3 = renderState.frontPos;
            Vec3 vec31 = renderState.backPos;
            poseStack.translate(renderState.posOnRail.x - d0, (vec3.y + vec31.y) / 2.0 - d1, renderState.posOnRail.z - d2);
            Vec3 vec32 = vec31.add(-vec3.x, -vec3.y, -vec3.z);
            if (vec32.length() != 0.0) {
                vec32 = vec32.normalize();
                f1 = (float)(Math.atan2(vec32.z, vec32.x) * 180.0 / Math.PI);
                f = (float)(Math.atan(vec32.y) * 73.0);
            }
        }

        poseStack.translate(0.0F, 0.375F, 0.0F);
        poseStack.mulPose(Axis.YP.rotationDegrees(180.0F - f1));
        poseStack.mulPose(Axis.ZP.rotationDegrees(-f));
    }

    public void extractRenderState(T p_478173_, S p_361455_, float p_363949_) {
        super.extractRenderState(p_478173_, p_361455_, p_363949_);
        if (p_478173_.getBehavior() instanceof NewMinecartBehavior newminecartbehavior) {
            newExtractState(p_478173_, newminecartbehavior, p_361455_, p_363949_);
            p_361455_.isNewRender = true;
        } else if (p_478173_.getBehavior() instanceof OldMinecartBehavior oldminecartbehavior) {
            oldExtractState(p_478173_, oldminecartbehavior, p_361455_, p_363949_);
            p_361455_.isNewRender = false;
        }

        long i = p_478173_.getId() * 493286711L;
        p_361455_.offsetSeed = i * i * 4392167121L + i * 98761L;
        p_361455_.hurtTime = p_478173_.getHurtTime() - p_363949_;
        p_361455_.hurtDir = p_478173_.getHurtDir();
        p_361455_.damageTime = Math.max(p_478173_.getDamage() - p_363949_, 0.0F);
        p_361455_.displayOffset = p_478173_.getDisplayOffset();
        p_361455_.displayBlockState = p_478173_.getDisplayBlockState();
    }

    private static <T extends AbstractMinecart, S extends MinecartRenderState> void newExtractState(
        T minecart, NewMinecartBehavior behavior, S reusedState, float partialTick
    ) {
        if (behavior.cartHasPosRotLerp()) {
            reusedState.renderPos = behavior.getCartLerpPosition(partialTick);
            reusedState.xRot = behavior.getCartLerpXRot(partialTick);
            reusedState.yRot = behavior.getCartLerpYRot(partialTick);
        } else {
            reusedState.renderPos = null;
            reusedState.xRot = minecart.getXRot();
            reusedState.yRot = minecart.getYRot();
        }
    }

    private static <T extends AbstractMinecart, S extends MinecartRenderState> void oldExtractState(
        T minecart, OldMinecartBehavior behavior, S reusedState, float partialTick
    ) {
        float f = 0.3F;
        reusedState.xRot = minecart.getXRot(partialTick);
        reusedState.yRot = minecart.getYRot(partialTick);
        double d0 = reusedState.x;
        double d1 = reusedState.y;
        double d2 = reusedState.z;
        Vec3 vec3 = behavior.getPos(d0, d1, d2);
        if (vec3 != null) {
            reusedState.posOnRail = vec3;
            Vec3 vec31 = behavior.getPosOffs(d0, d1, d2, 0.3F);
            Vec3 vec32 = behavior.getPosOffs(d0, d1, d2, -0.3F);
            reusedState.frontPos = Objects.requireNonNullElse(vec31, vec3);
            reusedState.backPos = Objects.requireNonNullElse(vec32, vec3);
        } else {
            reusedState.posOnRail = null;
            reusedState.frontPos = null;
            reusedState.backPos = null;
        }
    }

    protected void submitMinecartContents(S renderState, BlockState blockState, PoseStack poseStack, SubmitNodeCollector nodeCollector, int packedLight) {
        nodeCollector.submitBlock(poseStack, blockState, packedLight, OverlayTexture.NO_OVERLAY, renderState.outlineColor);
    }

    protected AABB getBoundingBoxForCulling(T p_481228_) {
        AABB aabb = super.getBoundingBoxForCulling(p_481228_);
        return !p_481228_.getDisplayBlockState().isAir() ? aabb.expandTowards(0.0, p_481228_.getDisplayOffset() * 0.75F / 16.0F, 0.0) : aabb;
    }

    public Vec3 getRenderOffset(S p_363702_) {
        Vec3 vec3 = super.getRenderOffset(p_363702_);
        return p_363702_.isNewRender && p_363702_.renderPos != null
            ? vec3.add(p_363702_.renderPos.x - p_363702_.x, p_363702_.renderPos.y - p_363702_.y, p_363702_.renderPos.z - p_363702_.z)
            : vec3;
    }
}
