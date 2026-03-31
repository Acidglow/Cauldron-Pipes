package net.minecraft.client.renderer.entity.layers;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.level.block.AbstractBannerBlock;
import net.minecraft.world.level.block.AbstractChestBlock;
import net.minecraft.world.level.block.AbstractSkullBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.CopperGolemStatueBlock;
import net.minecraft.world.level.block.FlowerBedBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class BlockDecorationLayer<S extends EntityRenderState, M extends EntityModel<S>> extends RenderLayer<S, M> {
    private final Function<S, Optional<BlockState>> blockState;
    private final Consumer<PoseStack> transform;

    public BlockDecorationLayer(RenderLayerParent<S, M> renderer, Function<S, Optional<BlockState>> blockState, Consumer<PoseStack> transform) {
        super(renderer);
        this.blockState = blockState;
        this.transform = transform;
    }

    @Override
    public void submit(PoseStack p_437287_, SubmitNodeCollector p_437210_, int p_437419_, S p_437376_, float p_437345_, float p_437276_) {
        Optional<BlockState> optional = this.blockState.apply(p_437376_);
        if (!optional.isEmpty()) {
            BlockState blockstate = optional.get();
            Block block = blockstate.getBlock();
            boolean flag = block instanceof CopperGolemStatueBlock;
            p_437287_.pushPose();
            this.transform.accept(p_437287_);
            if (!flag) {
                p_437287_.mulPose(Axis.ZP.rotationDegrees(180.0F));
            }

            if (flag || block instanceof AbstractSkullBlock || block instanceof AbstractBannerBlock || block instanceof AbstractChestBlock) {
                p_437287_.mulPose(Axis.YP.rotationDegrees(180.0F));
            }

            if (block instanceof FlowerBedBlock) {
                p_437287_.translate(-0.25, -1.5, -0.25);
            } else if (!flag) {
                p_437287_.translate(-0.5, -1.5, -0.5);
            } else {
                p_437287_.translate(-0.5, 0.0, -0.5);
            }

            p_437210_.submitBlock(p_437287_, blockstate, p_437419_, OverlayTexture.NO_OVERLAY, p_437376_.outlineColor);
            p_437287_.popPose();
        }
    }
}
