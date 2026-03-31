package net.minecraft.client.renderer.entity.layers;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.model.animal.golem.SnowGolemModel;
import net.minecraft.client.renderer.ItemBlockRenderTypes;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.block.model.BlockStateModel;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.state.SnowGolemRenderState;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class SnowGolemHeadLayer extends RenderLayer<SnowGolemRenderState, SnowGolemModel> {
    private final BlockRenderDispatcher blockRenderer;

    public SnowGolemHeadLayer(RenderLayerParent<SnowGolemRenderState, SnowGolemModel> renderer, BlockRenderDispatcher blockRenderer) {
        super(renderer);
        this.blockRenderer = blockRenderer;
    }

    public void submit(PoseStack p_434118_, SubmitNodeCollector p_435807_, int p_433104_, SnowGolemRenderState p_434616_, float p_433223_, float p_433380_) {
        if (p_434616_.hasPumpkin) {
            if (!p_434616_.isInvisible || p_434616_.appearsGlowing()) {
                p_434118_.pushPose();
                this.getParentModel().getHead().translateAndRotate(p_434118_);
                float f = 0.625F;
                p_434118_.translate(0.0F, -0.34375F, 0.0F);
                p_434118_.mulPose(Axis.YP.rotationDegrees(180.0F));
                p_434118_.scale(0.625F, -0.625F, -0.625F);
                BlockState blockstate = Blocks.CARVED_PUMPKIN.defaultBlockState();
                BlockStateModel blockstatemodel = this.blockRenderer.getBlockModel(blockstate);
                int i = LivingEntityRenderer.getOverlayCoords(p_434616_, 0.0F);
                p_434118_.translate(-0.5F, -0.5F, -0.5F);
                RenderType rendertype = p_434616_.appearsGlowing() && p_434616_.isInvisible
                    ? RenderTypes.outline(TextureAtlas.LOCATION_BLOCKS)
                    : ItemBlockRenderTypes.getRenderType(blockstate);
                p_435807_.submitBlockModel(p_434118_, rendertype, blockstatemodel, 0.0F, 0.0F, 0.0F, p_433104_, i, p_434616_.outlineColor);
                p_434118_.popPose();
            }
        }
    }
}
