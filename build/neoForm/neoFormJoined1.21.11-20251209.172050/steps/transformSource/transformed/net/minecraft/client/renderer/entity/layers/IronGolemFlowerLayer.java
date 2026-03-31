package net.minecraft.client.renderer.entity.layers;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.model.animal.golem.IronGolemModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.state.IronGolemRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class IronGolemFlowerLayer extends RenderLayer<IronGolemRenderState, IronGolemModel> {
    public IronGolemFlowerLayer(RenderLayerParent<IronGolemRenderState, IronGolemModel> p_234842_) {
        super(p_234842_);
    }

    public void submit(PoseStack p_432993_, SubmitNodeCollector p_435087_, int p_433962_, IronGolemRenderState p_434784_, float p_435316_, float p_435013_) {
        if (p_434784_.offerFlowerTick != 0) {
            p_432993_.pushPose();
            ModelPart modelpart = this.getParentModel().getFlowerHoldingArm();
            modelpart.translateAndRotate(p_432993_);
            p_432993_.translate(-1.1875F, 1.0625F, -0.9375F);
            p_432993_.translate(0.5F, 0.5F, 0.5F);
            float f = 0.5F;
            p_432993_.scale(0.5F, 0.5F, 0.5F);
            p_432993_.mulPose(Axis.XP.rotationDegrees(-90.0F));
            p_432993_.translate(-0.5F, -0.5F, -0.5F);
            p_435087_.submitBlock(p_432993_, Blocks.POPPY.defaultBlockState(), p_433962_, OverlayTexture.NO_OVERLAY, p_434784_.outlineColor);
            p_432993_.popPose();
        }
    }
}
