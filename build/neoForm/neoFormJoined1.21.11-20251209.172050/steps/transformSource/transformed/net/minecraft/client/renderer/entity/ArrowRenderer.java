package net.minecraft.client.renderer.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.object.projectile.ArrowModel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.state.ArrowRenderState;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.projectile.arrow.AbstractArrow;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public abstract class ArrowRenderer<T extends AbstractArrow, S extends ArrowRenderState> extends EntityRenderer<T, S> {
    private final ArrowModel model;

    public ArrowRenderer(EntityRendererProvider.Context p_173917_) {
        super(p_173917_);
        this.model = new ArrowModel(p_173917_.bakeLayer(ModelLayers.ARROW));
    }

    public void submit(S p_433387_, PoseStack p_435722_, SubmitNodeCollector p_432834_, CameraRenderState p_451529_) {
        p_435722_.pushPose();
        p_435722_.mulPose(Axis.YP.rotationDegrees(p_433387_.yRot - 90.0F));
        p_435722_.mulPose(Axis.ZP.rotationDegrees(p_433387_.xRot));
        p_432834_.submitModel(
            this.model,
            p_433387_,
            p_435722_,
            RenderTypes.entityCutout(this.getTextureLocation(p_433387_)),
            p_433387_.lightCoords,
            OverlayTexture.NO_OVERLAY,
            p_433387_.outlineColor,
            null
        );
        p_435722_.popPose();
        super.submit(p_433387_, p_435722_, p_432834_, p_451529_);
    }

    protected abstract Identifier getTextureLocation(S renderState);

    public void extractRenderState(T p_478605_, S p_364204_, float p_360538_) {
        super.extractRenderState(p_478605_, p_364204_, p_360538_);
        p_364204_.xRot = p_478605_.getXRot(p_360538_);
        p_364204_.yRot = p_478605_.getYRot(p_360538_);
        p_364204_.shake = p_478605_.shakeTime - p_360538_;
    }
}
