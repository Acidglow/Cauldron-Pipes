package net.minecraft.world.level.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.redstone.ExperimentalRedstoneUtils;
import net.minecraft.world.level.redstone.Orientation;

public class ObserverBlock extends DirectionalBlock {
    public static final MapCodec<ObserverBlock> CODEC = simpleCodec(ObserverBlock::new);
    public static final BooleanProperty POWERED = BlockStateProperties.POWERED;

    @Override
    public MapCodec<ObserverBlock> codec() {
        return CODEC;
    }

    public ObserverBlock(BlockBehaviour.Properties p_55085_) {
        super(p_55085_);
        this.registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.SOUTH).setValue(POWERED, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, POWERED);
    }

    @Override
    protected BlockState rotate(BlockState state, Rotation rot) {
        return state.setValue(FACING, rot.rotate(state.getValue(FACING)));
    }

    @Override
    protected BlockState mirror(BlockState state, Mirror mirror) {
        return state.rotate(mirror.getRotation(state.getValue(FACING)));
    }

    @Override
    protected void tick(BlockState p_221840_, ServerLevel p_221841_, BlockPos p_221842_, RandomSource p_221843_) {
        if (p_221840_.getValue(POWERED)) {
            p_221841_.setBlock(p_221842_, p_221840_.setValue(POWERED, false), 2);
        } else {
            p_221841_.setBlock(p_221842_, p_221840_.setValue(POWERED, true), 2);
            p_221841_.scheduleTick(p_221842_, this, 2);
        }

        this.updateNeighborsInFront(p_221841_, p_221842_, p_221840_);
    }

    @Override
    protected BlockState updateShape(
        BlockState p_55118_,
        LevelReader p_374557_,
        ScheduledTickAccess p_374458_,
        BlockPos p_55122_,
        Direction p_55119_,
        BlockPos p_55123_,
        BlockState p_55120_,
        RandomSource p_374161_
    ) {
        if (p_55118_.getValue(FACING) == p_55119_ && !p_55118_.getValue(POWERED)) {
            this.startSignal(p_374557_, p_374458_, p_55122_);
        }

        return super.updateShape(p_55118_, p_374557_, p_374458_, p_55122_, p_55119_, p_55123_, p_55120_, p_374161_);
    }

    private void startSignal(LevelReader level, ScheduledTickAccess scheduledTickAccess, BlockPos pos) {
        if (!level.isClientSide() && !scheduledTickAccess.getBlockTicks().hasScheduledTick(pos, this)) {
            scheduledTickAccess.scheduleTick(pos, this, 2);
        }
    }

    protected void updateNeighborsInFront(Level level, BlockPos pos, BlockState state) {
        Direction direction = state.getValue(FACING);
        BlockPos blockpos = pos.relative(direction.getOpposite());
        Orientation orientation = ExperimentalRedstoneUtils.initialOrientation(level, direction.getOpposite(), null);
        level.neighborChanged(blockpos, this, orientation);
        level.updateNeighborsAtExceptFromFacing(blockpos, this, direction, orientation);
    }

    @Override
    protected boolean isSignalSource(BlockState state) {
        return true;
    }

    @Override
    protected int getDirectSignal(BlockState blockState, BlockGetter blockAccess, BlockPos pos, Direction side) {
        return blockState.getSignal(blockAccess, pos, side);
    }

    @Override
    protected int getSignal(BlockState blockState, BlockGetter blockAccess, BlockPos pos, Direction side) {
        return blockState.getValue(POWERED) && blockState.getValue(FACING) == side ? 15 : 0;
    }

    @Override
    protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean isMoving) {
        if (!state.is(oldState.getBlock())) {
            if (!level.isClientSide() && state.getValue(POWERED) && !level.getBlockTicks().hasScheduledTick(pos, this)) {
                BlockState blockstate = state.setValue(POWERED, false);
                level.setBlock(pos, blockstate, 18);
                this.updateNeighborsInFront(level, pos, blockstate);
            }
        }
    }

    @Override
    protected void affectNeighborsAfterRemoval(BlockState p_393998_, ServerLevel p_394664_, BlockPos p_394449_, boolean p_394257_) {
        if (p_393998_.getValue(POWERED) && p_394664_.getBlockTicks().hasScheduledTick(p_394449_, this)) {
            this.updateNeighborsInFront(p_394664_, p_394449_, p_393998_.setValue(POWERED, false));
        }
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return this.defaultBlockState().setValue(FACING, context.getNearestLookingDirection().getOpposite().getOpposite());
    }
}
