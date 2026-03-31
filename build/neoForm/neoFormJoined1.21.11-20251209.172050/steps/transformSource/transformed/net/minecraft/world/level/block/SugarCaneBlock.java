package net.minecraft.world.level.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

public class SugarCaneBlock extends Block {
    public static final MapCodec<SugarCaneBlock> CODEC = simpleCodec(SugarCaneBlock::new);
    public static final IntegerProperty AGE = BlockStateProperties.AGE_15;
    private static final VoxelShape SHAPE = Block.column(12.0, 0.0, 16.0);

    @Override
    public MapCodec<SugarCaneBlock> codec() {
        return CODEC;
    }

    public SugarCaneBlock(BlockBehaviour.Properties p_57168_) {
        super(p_57168_);
        this.registerDefaultState(this.stateDefinition.any().setValue(AGE, 0));
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    protected void tick(BlockState p_222543_, ServerLevel p_222544_, BlockPos p_222545_, RandomSource p_222546_) {
        if (!p_222543_.canSurvive(p_222544_, p_222545_)) {
            p_222544_.destroyBlock(p_222545_, true);
        }
    }

    @Override
    protected void randomTick(BlockState p_222548_, ServerLevel p_222549_, BlockPos p_222550_, RandomSource p_222551_) {
        if (p_222549_.isEmptyBlock(p_222550_.above())) {
            int i = 1;

            while (p_222549_.getBlockState(p_222550_.below(i)).is(this)) {
                i++;
            }

            if (i < 3) {
                int j = p_222548_.getValue(AGE);
                if (net.neoforged.neoforge.common.CommonHooks.canCropGrow(p_222549_, p_222550_, p_222548_, true)) {
                if (j == 15) {
                    p_222549_.setBlockAndUpdate(p_222550_.above(), this.defaultBlockState());
                    net.neoforged.neoforge.common.CommonHooks.fireCropGrowPost(p_222549_, p_222550_.above(), this.defaultBlockState());
                    p_222549_.setBlock(p_222550_, p_222548_.setValue(AGE, 0), 260);
                } else {
                    p_222549_.setBlock(p_222550_, p_222548_.setValue(AGE, j + 1), 260);
                }
                }
            }
        }
    }

    @Override
    protected BlockState updateShape(
        BlockState p_57179_,
        LevelReader p_374168_,
        ScheduledTickAccess p_374217_,
        BlockPos p_57183_,
        Direction p_57180_,
        BlockPos p_57184_,
        BlockState p_57181_,
        RandomSource p_374240_
    ) {
        if (!p_57179_.canSurvive(p_374168_, p_57183_)) {
            p_374217_.scheduleTick(p_57183_, this, 1);
        }

        return super.updateShape(p_57179_, p_374168_, p_374217_, p_57183_, p_57180_, p_57184_, p_57181_, p_374240_);
    }

    @Override
    protected boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        BlockState blockstate = level.getBlockState(pos.below());
        if (blockstate.is(this)) {
            return true;
        } else {
            var soilDecision = blockstate.canSustainPlant(level, pos.below(), Direction.UP, state);
            if (!soilDecision.isDefault()) return soilDecision.isTrue();
            if (blockstate.is(BlockTags.DIRT) || blockstate.is(BlockTags.SAND)) {
                BlockPos blockpos = pos.below();

                for (Direction direction : Direction.Plane.HORIZONTAL) {
                    BlockState blockstate1 = level.getBlockState(blockpos.relative(direction));
                    FluidState fluidstate = level.getFluidState(blockpos.relative(direction));
                    if (state.canBeHydrated(level, pos, fluidstate, blockpos.relative(direction)) || blockstate1.is(Blocks.FROSTED_ICE)) {
                        return true;
                    }
                }
            }

            return false;
        }
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(AGE);
    }
}
