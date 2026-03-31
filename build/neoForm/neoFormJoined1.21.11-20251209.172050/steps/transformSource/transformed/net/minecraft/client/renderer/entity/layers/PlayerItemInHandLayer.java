package net.minecraft.client.renderer.entity.layers;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.ArmedModel;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.HeadedModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class PlayerItemInHandLayer<S extends AvatarRenderState, M extends EntityModel<S> & ArmedModel & HeadedModel> extends ItemInHandLayer<S, M> {
    private static final float X_ROT_MIN = (float) (-Math.PI / 6);
    private static final float X_ROT_MAX = (float) (Math.PI / 2);

    public PlayerItemInHandLayer(RenderLayerParent<S, M> p_234866_) {
        super(p_234866_);
    }

    protected void submitArmWithItem(
        S p_454979_,
        ItemStackRenderState p_434802_,
        ItemStack p_455869_,
        HumanoidArm p_434886_,
        PoseStack p_435466_,
        SubmitNodeCollector p_433358_,
        int p_436055_
    ) {
        if (!p_434802_.isEmpty()) {
            InteractionHand interactionhand = p_434886_ == p_454979_.mainArm ? InteractionHand.MAIN_HAND : InteractionHand.OFF_HAND;
            if (p_454979_.isUsingItem && p_454979_.useItemHand == interactionhand && p_454979_.attackTime < 1.0E-5F && !p_454979_.heldOnHead.isEmpty()) {
                this.renderItemHeldToEye(p_454979_, p_434886_, p_435466_, p_433358_, p_436055_);
            } else {
                super.submitArmWithItem(p_454979_, p_434802_, p_455869_, p_434886_, p_435466_, p_433358_, p_436055_);
            }
        }
    }

    private void renderItemHeldToEye(S renderState, HumanoidArm arm, PoseStack poseStack, SubmitNodeCollector nodeCollector, int packedLight) {
        poseStack.pushPose();
        this.getParentModel().root().translateAndRotate(poseStack);
        ModelPart modelpart = this.getParentModel().getHead();
        float f = modelpart.xRot;
        modelpart.xRot = Mth.clamp(modelpart.xRot, (float) (-Math.PI / 6), (float) (Math.PI / 2));
        modelpart.translateAndRotate(poseStack);
        modelpart.xRot = f;
        CustomHeadLayer.translateToHead(poseStack, CustomHeadLayer.Transforms.DEFAULT);
        boolean flag = arm == HumanoidArm.LEFT;
        poseStack.translate((flag ? -2.5F : 2.5F) / 16.0F, -0.0625F, 0.0F);
        renderState.heldOnHead.submit(poseStack, nodeCollector, packedLight, OverlayTexture.NO_OVERLAY, renderState.outlineColor);
        poseStack.popPose();
    }
}
