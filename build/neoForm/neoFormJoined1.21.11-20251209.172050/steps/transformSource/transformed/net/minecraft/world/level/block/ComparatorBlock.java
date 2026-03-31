package net.minecraft.world.level.block;

import com.mojang.serialization.MapCodec;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.decoration.ItemFrame;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.ComparatorBlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.ComparatorMode;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.ticks.TickPriority;
import org.jspecify.annotations.Nullable;

public class ComparatorBlock extends DiodeBlock implements EntityBlock {
    public static final MapCodec<ComparatorBlock> CODEC = simpleCodec(ComparatorBlock::new);
    public static final EnumProperty<ComparatorMode> MODE = BlockStateProperties.MODE_COMPARATOR;

    @Override
    public MapCodec<ComparatorBlock> codec() {
        return CODEC;
    }

    public ComparatorBlock(BlockBehaviour.Properties p_51857_) {
        super(p_51857_);
        this.registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(POWERED, false).setValue(MODE, ComparatorMode.COMPARE));
    }

    @Override
    protected int getDelay(BlockState state) {
        return 2;
    }

    @Override
    public BlockState updateShape(
        BlockState p_301069_,
        LevelReader p_374094_,
        ScheduledTickAccess p_374252_,
        BlockPos p_301025_,
        Direction p_301249_,
        BlockPos p_301045_,
        BlockState p_301318_,
        RandomSource p_374228_
    ) {
        return p_301249_ == Direction.DOWN && !this.canSurviveOn(p_374094_, p_301045_, p_301318_)
            ? Blocks.AIR.defaultBlockState()
            : super.updateShape(p_301069_, p_374094_, p_374252_, p_301025_, p_301249_, p_301045_, p_301318_, p_374228_);
    }

    @Override
    protected int getOutputSignal(BlockGetter level, BlockPos pos, BlockState state) {
        BlockEntity blockentity = level.getBlockEntity(pos);
        return blockentity instanceof ComparatorBlockEntity ? ((ComparatorBlockEntity)blockentity).getOutputSignal() : 0;
    }

    private int calculateOutputSignal(Level level, BlockPos pos, BlockState state) {
        int i = this.getInputSignal(level, pos, state);
        if (i == 0) {
            return 0;
        } else {
            int j = this.getAlternateSignal(level, pos, state);
            if (j > i) {
                return 0;
            } else {
                return state.getValue(MODE) == ComparatorMode.SUBTRACT ? i - j : i;
            }
        }
    }

    @Override
    protected boolean shouldTurnOn(Level level, BlockPos pos, BlockState state) {
        int i = this.getInputSignal(level, pos, state);
        if (i == 0) {
            return false;
        } else {
            int j = this.getAlternateSignal(level, pos, state);
            return i > j ? true : i == j && state.getValue(MODE) == ComparatorMode.COMPARE;
        }
    }

    @Override
    protected int getInputSignal(Level level, BlockPos pos, BlockState state) {
        int i = super.getInputSignal(level, pos, state);
        Direction direction = state.getValue(FACING);
        BlockPos blockpos = pos.relative(direction);
        BlockState blockstate = level.getBlockState(blockpos);
        if (blockstate.hasAnalogOutputSignal()) {
            i = blockstate.getAnalogOutputSignal(level, blockpos, direction.getOpposite());
        } else if (i < 15 && blockstate.isRedstoneConductor(level, blockpos)) {
            blockpos = blockpos.relative(direction);
            blockstate = level.getBlockState(blockpos);
            ItemFrame itemframe = this.getItemFrame(level, direction, blockpos);
            int j = Math.max(
                itemframe == null ? Integer.MIN_VALUE : itemframe.getAnalogOutput(),
                blockstate.hasAnalogOutputSignal() ? blockstate.getAnalogOutputSignal(level, blockpos, direction.getOpposite()) : Integer.MIN_VALUE
            );
            if (j != Integer.MIN_VALUE) {
                i = j;
            }
        }

        return i;
    }

    private @Nullable ItemFrame getItemFrame(Level level, Direction facing, BlockPos pos) {
        List<ItemFrame> list = level.getEntitiesOfClass(
            ItemFrame.class,
            new AABB(pos.getX(), pos.getY(), pos.getZ(), pos.getX() + 1, pos.getY() + 1, pos.getZ() + 1),
            p_460568_ -> p_460568_.getDirection() == facing
        );
        return list.size() == 1 ? list.get(0) : null;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState p_51880_, Level p_51881_, BlockPos p_51882_, Player p_51883_, BlockHitResult p_51885_) {
        if (!p_51883_.getAbilities().mayBuild) {
            return InteractionResult.PASS;
        } else {
            p_51880_ = p_51880_.cycle(MODE);
            float f = p_51880_.getValue(MODE) == ComparatorMode.SUBTRACT ? 0.55F : 0.5F;
            p_51881_.playSound(p_51883_, p_51882_, SoundEvents.COMPARATOR_CLICK, SoundSource.BLOCKS, 0.3F, f);
            p_51881_.setBlock(p_51882_, p_51880_, 2);
            this.refreshOutputState(p_51881_, p_51882_, p_51880_);
            return InteractionResult.SUCCESS;
        }
    }

    @Override
    protected void checkTickOnNeighbor(Level level, BlockPos pos, BlockState state) {
        if (!level.getBlockTicks().willTickThisTick(pos, this)) {
            int i = this.calculateOutputSignal(level, pos, state);
            BlockEntity blockentity = level.getBlockEntity(pos);
            int j = blockentity instanceof ComparatorBlockEntity ? ((ComparatorBlockEntity)blockentity).getOutputSignal() : 0;
            if (i != j || state.getValue(POWERED) != this.shouldTurnOn(level, pos, state)) {
                TickPriority tickpriority = this.shouldPrioritize(level, pos, state) ? TickPriority.HIGH : TickPriority.NORMAL;
                level.scheduleTick(pos, this, 2, tickpriority);
            }
        }
    }

    private void refreshOutputState(Level level, BlockPos pos, BlockState state) {
        int i = this.calculateOutputSignal(level, pos, state);
        BlockEntity blockentity = level.getBlockEntity(pos);
        int j = 0;
        if (blockentity instanceof ComparatorBlockEntity comparatorblockentity) {
            j = comparatorblockentity.getOutputSignal();
            comparatorblockentity.setOutputSignal(i);
        }

        if (j != i || state.getValue(MODE) == ComparatorMode.COMPARE) {
            boolean flag1 = this.shouldTurnOn(level, pos, state);
            boolean flag = state.getValue(POWERED);
            if (flag && !flag1) {
                level.setBlock(pos, state.setValue(POWERED, false), 2);
            } else if (!flag && flag1) {
                level.setBlock(pos, state.setValue(POWERED, true), 2);
            }

            this.updateNeighborsInFront(level, pos, state);
        }
    }

    @Override
    protected void tick(BlockState p_221010_, ServerLevel p_221011_, BlockPos p_221012_, RandomSource p_221013_) {
        this.refreshOutputState(p_221011_, p_221012_, p_221010_);
    }

    @Override
    protected boolean triggerEvent(BlockState state, Level level, BlockPos pos, int id, int param) {
        super.triggerEvent(state, level, pos, id, param);
        BlockEntity blockentity = level.getBlockEntity(pos);
        return blockentity != null && blockentity.triggerEvent(id, param);
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos p_153086_, BlockState p_153087_) {
        return new ComparatorBlockEntity(p_153086_, p_153087_);
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, MODE, POWERED);
    }

    @Override
    public boolean getWeakChanges(BlockState state, net.minecraft.world.level.LevelReader world, BlockPos pos) {
        return state.is(Blocks.COMPARATOR);
    }

    @Override
    public void onNeighborChange(BlockState state, net.minecraft.world.level.LevelReader levelReader, BlockPos pos, BlockPos neighbor) {
        if (pos.getY() == neighbor.getY() && levelReader instanceof Level level && !levelReader.isClientSide()) {
            // TODO porting: check this still works as expected
            state.handleNeighborChanged(level, pos, levelReader.getBlockState(neighbor).getBlock(), null, false);
        }
    }
}
