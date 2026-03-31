package net.minecraft.client.renderer.entity.layers;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.animal.parrot.ParrotModel;
import net.minecraft.client.model.geom.EntityModelSet;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.player.PlayerModel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.ParrotRenderer;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.client.renderer.entity.state.ParrotRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.entity.animal.parrot.Parrot;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class ParrotOnShoulderLayer extends RenderLayer<AvatarRenderState, PlayerModel> {
    private final ParrotModel model;

    public ParrotOnShoulderLayer(RenderLayerParent<AvatarRenderState, PlayerModel> renderer, EntityModelSet modelSet) {
        super(renderer);
        this.model = new ParrotModel(modelSet.bakeLayer(ModelLayers.PARROT));
    }

    public void submit(PoseStack p_434235_, SubmitNodeCollector p_434714_, int p_434156_, AvatarRenderState p_446731_, float p_433591_, float p_433688_) {
        Parrot.Variant parrot$variant = p_446731_.parrotOnLeftShoulder;
        if (parrot$variant != null) {
            this.submitOnShoulder(p_434235_, p_434714_, p_434156_, p_446731_, parrot$variant, p_433591_, p_433688_, true);
        }

        Parrot.Variant parrot$variant1 = p_446731_.parrotOnRightShoulder;
        if (parrot$variant1 != null) {
            this.submitOnShoulder(p_434235_, p_434714_, p_434156_, p_446731_, parrot$variant1, p_433591_, p_433688_, false);
        }
    }

    private void submitOnShoulder(
        PoseStack poseStack,
        SubmitNodeCollector nodeCollector,
        int packedLight,
        AvatarRenderState renderState,
        Parrot.Variant variant,
        float yRot,
        float xRot,
        boolean isLeft
    ) {
        poseStack.pushPose();
        poseStack.translate(isLeft ? 0.4F : -0.4F, renderState.isCrouching ? -1.3F : -1.5F, 0.0F);
        ParrotRenderState parrotrenderstate = new ParrotRenderState();
        parrotrenderstate.pose = ParrotModel.Pose.ON_SHOULDER;
        parrotrenderstate.ageInTicks = renderState.ageInTicks;
        parrotrenderstate.walkAnimationPos = renderState.walkAnimationPos;
        parrotrenderstate.walkAnimationSpeed = renderState.walkAnimationSpeed;
        parrotrenderstate.yRot = yRot;
        parrotrenderstate.xRot = xRot;
        nodeCollector.submitModel(
            this.model,
            parrotrenderstate,
            poseStack,
            this.model.renderType(ParrotRenderer.getVariantTexture(variant)),
            packedLight,
            OverlayTexture.NO_OVERLAY,
            renderState.outlineColor,
            null
        );
        poseStack.popPose();
    }
}
