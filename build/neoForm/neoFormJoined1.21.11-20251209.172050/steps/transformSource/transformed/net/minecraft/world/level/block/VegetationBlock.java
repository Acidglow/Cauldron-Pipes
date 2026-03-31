package net.minecraft.world.level.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.pathfinder.PathComputationType;

public abstract class VegetationBlock extends Block {
    public VegetationBlock(BlockBehaviour.Properties p_401368_) {
        super(p_401368_);
    }

    @Override
    protected abstract MapCodec<? extends VegetationBlock> codec();

    protected boolean mayPlaceOn(BlockState state, BlockGetter level, BlockPos pos) {
        return state.is(BlockTags.DIRT) || state.getBlock() instanceof net.minecraft.world.level.block.FarmBlock;
    }

    @Override
    protected BlockState updateShape(
        BlockState p_401118_,
        LevelReader p_401198_,
        ScheduledTickAccess p_401107_,
        BlockPos p_401142_,
        Direction p_401236_,
        BlockPos p_401082_,
        BlockState p_401336_,
        RandomSource p_401169_
    ) {
        return !p_401118_.canSurvive(p_401198_, p_401142_)
            ? Blocks.AIR.defaultBlockState()
            : super.updateShape(p_401118_, p_401198_, p_401107_, p_401142_, p_401236_, p_401082_, p_401336_, p_401169_);
    }

    @Override
    protected boolean canSurvive(BlockState p_401395_, LevelReader p_401031_, BlockPos p_401248_) {
        BlockPos blockpos = p_401248_.below();
        BlockState belowBlockState = p_401031_.getBlockState(blockpos);
        var soilDecision = belowBlockState.canSustainPlant(p_401031_, blockpos, Direction.UP, p_401395_);
        if (!soilDecision.isDefault()) return soilDecision.isTrue();
        return this.mayPlaceOn(belowBlockState, p_401031_, blockpos);
    }

    @Override
    protected boolean propagatesSkylightDown(BlockState p_401261_) {
        return p_401261_.getFluidState().isEmpty();
    }

    @Override
    protected boolean isPathfindable(BlockState p_401351_, PathComputationType p_401371_) {
        return p_401371_ == PathComputationType.AIR && !this.hasCollision ? true : super.isPathfindable(p_401351_, p_401371_);
    }
}
