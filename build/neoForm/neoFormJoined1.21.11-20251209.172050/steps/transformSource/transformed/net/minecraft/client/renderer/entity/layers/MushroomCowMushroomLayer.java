package net.minecraft.client.renderer.entity.layers;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.model.animal.cow.CowModel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.block.model.BlockStateModel;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.state.MushroomCowRenderState;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class MushroomCowMushroomLayer extends RenderLayer<MushroomCowRenderState, CowModel> {
    private final BlockRenderDispatcher blockRenderer;

    public MushroomCowMushroomLayer(RenderLayerParent<MushroomCowRenderState, CowModel> renderer, BlockRenderDispatcher blockRenderer) {
        super(renderer);
        this.blockRenderer = blockRenderer;
    }

    public void submit(PoseStack p_117256_, SubmitNodeCollector p_432964_, int p_117258_, MushroomCowRenderState p_361786_, float p_117260_, float p_117261_) {
        if (!p_361786_.isBaby) {
            boolean flag = p_361786_.appearsGlowing() && p_361786_.isInvisible;
            if (!p_361786_.isInvisible || flag) {
                BlockState blockstate = p_361786_.variant.getBlockState();
                int i = LivingEntityRenderer.getOverlayCoords(p_361786_, 0.0F);
                BlockStateModel blockstatemodel = this.blockRenderer.getBlockModel(blockstate);
                p_117256_.pushPose();
                p_117256_.translate(0.2F, -0.35F, 0.5F);
                p_117256_.mulPose(Axis.YP.rotationDegrees(-48.0F));
                p_117256_.scale(-1.0F, -1.0F, 1.0F);
                p_117256_.translate(-0.5F, -0.5F, -0.5F);
                this.submitMushroomBlock(p_117256_, p_432964_, p_117258_, flag, p_361786_.outlineColor, blockstate, i, blockstatemodel);
                p_117256_.popPose();
                p_117256_.pushPose();
                p_117256_.translate(0.2F, -0.35F, 0.5F);
                p_117256_.mulPose(Axis.YP.rotationDegrees(42.0F));
                p_117256_.translate(0.1F, 0.0F, -0.6F);
                p_117256_.mulPose(Axis.YP.rotationDegrees(-48.0F));
                p_117256_.scale(-1.0F, -1.0F, 1.0F);
                p_117256_.translate(-0.5F, -0.5F, -0.5F);
                this.submitMushroomBlock(p_117256_, p_432964_, p_117258_, flag, p_361786_.outlineColor, blockstate, i, blockstatemodel);
                p_117256_.popPose();
                p_117256_.pushPose();
                this.getParentModel().getHead().translateAndRotate(p_117256_);
                p_117256_.translate(0.0F, -0.7F, -0.2F);
                p_117256_.mulPose(Axis.YP.rotationDegrees(-78.0F));
                p_117256_.scale(-1.0F, -1.0F, 1.0F);
                p_117256_.translate(-0.5F, -0.5F, -0.5F);
                this.submitMushroomBlock(p_117256_, p_432964_, p_117258_, flag, p_361786_.outlineColor, blockstate, i, blockstatemodel);
                p_117256_.popPose();
            }
        }
    }

    private void submitMushroomBlock(
        PoseStack poseStack,
        SubmitNodeCollector nodeCollector,
        int packedLight,
        boolean renderOutline,
        int outlineColor,
        BlockState blockState,
        int packedOverlay,
        BlockStateModel model
    ) {
        if (renderOutline) {
            nodeCollector.submitBlockModel(
                poseStack, RenderTypes.outline(TextureAtlas.LOCATION_BLOCKS), model, 0.0F, 0.0F, 0.0F, packedLight, packedOverlay, outlineColor
            );
        } else {
            nodeCollector.submitBlock(poseStack, blockState, packedLight, packedOverlay, outlineColor);
        }
    }
}
