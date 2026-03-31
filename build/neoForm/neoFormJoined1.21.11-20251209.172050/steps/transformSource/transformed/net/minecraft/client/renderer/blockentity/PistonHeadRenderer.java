package net.minecraft.client.renderer.blockentity;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.block.MovingBlockRenderState;
import net.minecraft.client.renderer.blockentity.state.PistonHeadRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.state.CameraRenderState;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.piston.PistonBaseBlock;
import net.minecraft.world.level.block.piston.PistonHeadBlock;
import net.minecraft.world.level.block.piston.PistonMovingBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.PistonType;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.jspecify.annotations.Nullable;

@OnlyIn(Dist.CLIENT)
public class PistonHeadRenderer implements BlockEntityRenderer<PistonMovingBlockEntity, PistonHeadRenderState> {
    public PistonHeadRenderState createRenderState() {
        return new PistonHeadRenderState();
    }

    public void extractRenderState(
        PistonMovingBlockEntity p_445667_,
        PistonHeadRenderState p_445712_,
        float p_446910_,
        Vec3 p_445923_,
        ModelFeatureRenderer.@Nullable CrumblingOverlay p_446317_
    ) {
        BlockEntityRenderer.super.extractRenderState(p_445667_, p_445712_, p_446910_, p_445923_, p_446317_);
        p_445712_.xOffset = p_445667_.getXOff(p_446910_);
        p_445712_.yOffset = p_445667_.getYOff(p_446910_);
        p_445712_.zOffset = p_445667_.getZOff(p_446910_);
        p_445712_.block = null;
        p_445712_.base = null;
        BlockState blockstate = p_445667_.getMovedState();
        Level level = p_445667_.getLevel();
        if (level != null && !blockstate.isAir()) {
            BlockPos blockpos = p_445667_.getBlockPos().relative(p_445667_.getMovementDirection().getOpposite());
            Holder<Biome> holder = level.getBiome(blockpos);
            if (blockstate.is(Blocks.PISTON_HEAD) && p_445667_.getProgress(p_446910_) <= 4.0F) {
                blockstate = blockstate.setValue(PistonHeadBlock.SHORT, p_445667_.getProgress(p_446910_) <= 0.5F);
                p_445712_.block = createMovingBlock(blockpos, blockstate, holder, level);
            } else if (p_445667_.isSourcePiston() && !p_445667_.isExtending()) {
                PistonType pistontype = blockstate.is(Blocks.STICKY_PISTON) ? PistonType.STICKY : PistonType.DEFAULT;
                BlockState blockstate1 = Blocks.PISTON_HEAD
                    .defaultBlockState()
                    .setValue(PistonHeadBlock.TYPE, pistontype)
                    .setValue(PistonHeadBlock.FACING, blockstate.getValue(PistonBaseBlock.FACING));
                blockstate1 = blockstate1.setValue(PistonHeadBlock.SHORT, p_445667_.getProgress(p_446910_) >= 0.5F);
                p_445712_.block = createMovingBlock(blockpos, blockstate1, holder, level);
                BlockPos blockpos1 = blockpos.relative(p_445667_.getMovementDirection());
                blockstate = blockstate.setValue(PistonBaseBlock.EXTENDED, true);
                p_445712_.base = createMovingBlock(blockpos1, blockstate, holder, level);
            } else {
                p_445712_.block = createMovingBlock(blockpos, blockstate, holder, level);
            }
        }
    }

    public void submit(PistonHeadRenderState p_445405_, PoseStack p_439297_, SubmitNodeCollector p_440309_, CameraRenderState p_450916_) {
        if (p_445405_.block != null) {
            p_439297_.pushPose();
            p_439297_.translate(p_445405_.xOffset, p_445405_.yOffset, p_445405_.zOffset);
            p_440309_.submitMovingBlock(p_439297_, p_445405_.block);
            p_439297_.popPose();
            if (p_445405_.base != null) {
                p_440309_.submitMovingBlock(p_439297_, p_445405_.base);
            }
        }
    }

    private static MovingBlockRenderState createMovingBlock(BlockPos pos, BlockState state, Holder<Biome> biome, Level level) {
        MovingBlockRenderState movingblockrenderstate = new MovingBlockRenderState();
        movingblockrenderstate.randomSeedPos = pos;
        movingblockrenderstate.blockPos = pos;
        movingblockrenderstate.blockState = state;
        movingblockrenderstate.biome = biome;
        movingblockrenderstate.level = level;
        return movingblockrenderstate;
    }

    @Override
    public int getViewDistance() {
        return 68;
    }

    @Override
    public net.minecraft.world.phys.AABB getRenderBoundingBox(PistonMovingBlockEntity blockEntity) {
        return net.minecraft.world.phys.AABB.INFINITE;
    }
}
