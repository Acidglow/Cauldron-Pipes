package net.minecraft.client.renderer.entity.layers;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.model.monster.enderman.EndermanModel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.state.EndermanRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class CarriedBlockLayer extends RenderLayer<EndermanRenderState, EndermanModel<EndermanRenderState>> {
    public CarriedBlockLayer(RenderLayerParent<EndermanRenderState, EndermanModel<EndermanRenderState>> p_234814_) {
        super(p_234814_);
    }

    public void submit(PoseStack p_433727_, SubmitNodeCollector p_435736_, int p_433871_, EndermanRenderState p_434429_, float p_434717_, float p_435092_) {
        BlockState blockstate = p_434429_.carriedBlock;
        if (blockstate != null) {
            p_433727_.pushPose();
            p_433727_.translate(0.0F, 0.6875F, -0.75F);
            p_433727_.mulPose(Axis.XP.rotationDegrees(20.0F));
            p_433727_.mulPose(Axis.YP.rotationDegrees(45.0F));
            p_433727_.translate(0.25F, 0.1875F, 0.25F);
            float f = 0.5F;
            p_433727_.scale(-0.5F, -0.5F, 0.5F);
            p_433727_.mulPose(Axis.YP.rotationDegrees(90.0F));
            p_435736_.submitBlock(p_433727_, blockstate, p_433871_, OverlayTexture.NO_OVERLAY, p_434429_.outlineColor);
            p_433727_.popPose();
        }
    }
}
