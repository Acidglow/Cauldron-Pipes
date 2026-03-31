package net.minecraft.client.renderer.entity.layers;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.model.ArmedModel;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.effects.SpearAnimations;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.state.ArmedEntityRenderState;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SwingAnimationType;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class ItemInHandLayer<S extends ArmedEntityRenderState, M extends EntityModel<S> & ArmedModel> extends RenderLayer<S, M> {
    public ItemInHandLayer(RenderLayerParent<S, M> p_234846_) {
        super(p_234846_);
    }

    public void submit(PoseStack p_433803_, SubmitNodeCollector p_434482_, int p_433450_, S p_434546_, float p_433047_, float p_433527_) {
        this.submitArmWithItem(p_434546_, p_434546_.rightHandItemState, p_434546_.rightHandItemStack, HumanoidArm.RIGHT, p_433803_, p_434482_, p_433450_);
        this.submitArmWithItem(p_434546_, p_434546_.leftHandItemState, p_434546_.leftHandItemStack, HumanoidArm.LEFT, p_433803_, p_434482_, p_433450_);
    }

    protected void submitArmWithItem(
        S renderState,
        ItemStackRenderState itemStackRenderState,
        ItemStack item,
        HumanoidArm arm,
        PoseStack poseStack,
        SubmitNodeCollector nodeCollector,
        int packedLight
    ) {
        if (!itemStackRenderState.isEmpty()) {
            poseStack.pushPose();
            this.getParentModel().translateToHand(renderState, arm, poseStack);
            poseStack.mulPose(Axis.XP.rotationDegrees(-90.0F));
            poseStack.mulPose(Axis.YP.rotationDegrees(180.0F));
            boolean flag = arm == HumanoidArm.LEFT;
            poseStack.translate((flag ? -1 : 1) / 16.0F, 0.125F, -0.625F);
            if (renderState.attackTime > 0.0F && renderState.mainArm == arm && renderState.swingAnimationType == SwingAnimationType.STAB) {
                SpearAnimations.thirdPersonAttackItem(renderState, poseStack);
            }

            float f = renderState.ticksUsingItem(arm);
            if (f != 0.0F) {
                (arm == HumanoidArm.RIGHT ? renderState.rightArmPose : renderState.leftArmPose).animateUseItem(renderState, poseStack, f, arm, item);
            }

            itemStackRenderState.submit(poseStack, nodeCollector, packedLight, OverlayTexture.NO_OVERLAY, renderState.outlineColor);
            poseStack.popPose();
        }
    }
}
