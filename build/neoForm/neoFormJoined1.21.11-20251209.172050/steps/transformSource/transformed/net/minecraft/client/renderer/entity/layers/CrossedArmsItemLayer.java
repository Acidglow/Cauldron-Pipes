package net.minecraft.client.renderer.entity.layers;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.VillagerLikeModel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.state.HoldingEntityRenderState;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class CrossedArmsItemLayer<S extends HoldingEntityRenderState, M extends EntityModel<S> & VillagerLikeModel> extends RenderLayer<S, M> {
    public CrossedArmsItemLayer(RenderLayerParent<S, M> p_234818_) {
        super(p_234818_);
    }

    public void submit(PoseStack p_436000_, SubmitNodeCollector p_434361_, int p_432786_, S p_433547_, float p_435878_, float p_435123_) {
        ItemStackRenderState itemstackrenderstate = p_433547_.heldItem;
        if (!itemstackrenderstate.isEmpty()) {
            p_436000_.pushPose();
            this.applyTranslation(p_433547_, p_436000_);
            itemstackrenderstate.submit(p_436000_, p_434361_, p_432786_, OverlayTexture.NO_OVERLAY, p_433547_.outlineColor);
            p_436000_.popPose();
        }
    }

    protected void applyTranslation(S renderState, PoseStack poseStack) {
        this.getParentModel().translateToArms(renderState, poseStack);
        poseStack.mulPose(Axis.XP.rotation(0.75F));
        poseStack.scale(1.07F, 1.07F, 1.07F);
        poseStack.translate(0.0F, 0.13F, -0.34F);
        poseStack.mulPose(Axis.XP.rotation((float) Math.PI));
    }
}
