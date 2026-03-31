package net.minecraft.client.renderer.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.state.BoatRenderState;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.state.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.vehicle.boat.AbstractBoat;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.joml.Quaternionf;

@OnlyIn(Dist.CLIENT)
public abstract class AbstractBoatRenderer extends EntityRenderer<AbstractBoat, BoatRenderState> {
    public AbstractBoatRenderer(EntityRendererProvider.Context p_376164_) {
        super(p_376164_);
        this.shadowRadius = 0.8F;
    }

    public void submit(BoatRenderState p_432989_, PoseStack p_433482_, SubmitNodeCollector p_435618_, CameraRenderState p_451066_) {
        p_433482_.pushPose();
        p_433482_.translate(0.0F, 0.375F, 0.0F);
        p_433482_.mulPose(Axis.YP.rotationDegrees(180.0F - p_432989_.yRot));
        float f = p_432989_.hurtTime;
        if (f > 0.0F) {
            p_433482_.mulPose(Axis.XP.rotationDegrees(Mth.sin(f) * f * p_432989_.damageTime / 10.0F * p_432989_.hurtDir));
        }

        if (!p_432989_.isUnderWater && !Mth.equal(p_432989_.bubbleAngle, 0.0F)) {
            p_433482_.mulPose(new Quaternionf().setAngleAxis(p_432989_.bubbleAngle * (float) (Math.PI / 180.0), 1.0F, 0.0F, 1.0F));
        }

        p_433482_.scale(-1.0F, -1.0F, 1.0F);
        p_433482_.mulPose(Axis.YP.rotationDegrees(90.0F));
        p_435618_.submitModel(
            this.model(), p_432989_, p_433482_, this.renderType(), p_432989_.lightCoords, OverlayTexture.NO_OVERLAY, p_432989_.outlineColor, null
        );
        this.submitTypeAdditions(p_432989_, p_433482_, p_435618_, p_432989_.lightCoords);
        p_433482_.popPose();
        super.submit(p_432989_, p_433482_, p_435618_, p_451066_);
    }

    protected void submitTypeAdditions(BoatRenderState renderState, PoseStack poseStack, SubmitNodeCollector nodeCollector, int lightCoords) {
    }

    protected abstract EntityModel<BoatRenderState> model();

    protected abstract RenderType renderType();

    public BoatRenderState createRenderState() {
        return new BoatRenderState();
    }

    public void extractRenderState(AbstractBoat p_479134_, BoatRenderState p_376855_, float p_376877_) {
        super.extractRenderState(p_479134_, p_376855_, p_376877_);
        p_376855_.yRot = p_479134_.getYRot(p_376877_);
        p_376855_.hurtTime = p_479134_.getHurtTime() - p_376877_;
        p_376855_.hurtDir = p_479134_.getHurtDir();
        p_376855_.damageTime = Math.max(p_479134_.getDamage() - p_376877_, 0.0F);
        p_376855_.bubbleAngle = p_479134_.getBubbleAngle(p_376877_);
        p_376855_.isUnderWater = p_479134_.isUnderWater();
        p_376855_.rowingTimeLeft = p_479134_.getRowingTime(0, p_376877_);
        p_376855_.rowingTimeRight = p_479134_.getRowingTime(1, p_376877_);
    }
}
