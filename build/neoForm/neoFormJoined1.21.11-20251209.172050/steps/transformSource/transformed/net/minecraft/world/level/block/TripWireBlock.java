package net.minecraft.world.level.block;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.mojang.serialization.codecs.RecordCodecBuilder.Instance;
import java.util.List;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.InsideBlockEffectApplier;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Items;
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
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

public class TripWireBlock extends Block {
    public static final MapCodec<TripWireBlock> CODEC = RecordCodecBuilder.mapCodec(
        p_432688_ -> p_432688_.group(BuiltInRegistries.BLOCK.byNameCodec().fieldOf("hook").forGetter(p_304664_ -> p_304664_.hook), propertiesCodec())
            .apply(p_432688_, TripWireBlock::new)
    );
    public static final BooleanProperty POWERED = BlockStateProperties.POWERED;
    public static final BooleanProperty ATTACHED = BlockStateProperties.ATTACHED;
    public static final BooleanProperty DISARMED = BlockStateProperties.DISARMED;
    public static final BooleanProperty NORTH = PipeBlock.NORTH;
    public static final BooleanProperty EAST = PipeBlock.EAST;
    public static final BooleanProperty SOUTH = PipeBlock.SOUTH;
    public static final BooleanProperty WEST = PipeBlock.WEST;
    private static final Map<Direction, BooleanProperty> PROPERTY_BY_DIRECTION = CrossCollisionBlock.PROPERTY_BY_DIRECTION;
    private static final VoxelShape SHAPE_ATTACHED = Block.column(16.0, 1.0, 2.5);
    private static final VoxelShape SHAPE_NOT_ATTACHED = Block.column(16.0, 0.0, 8.0);
    private static final int RECHECK_PERIOD = 10;
    private final Block hook;

    @Override
    public MapCodec<TripWireBlock> codec() {
        return CODEC;
    }

    public TripWireBlock(Block hook, BlockBehaviour.Properties properties) {
        super(properties);
        this.registerDefaultState(
            this.stateDefinition
                .any()
                .setValue(POWERED, false)
                .setValue(ATTACHED, false)
                .setValue(DISARMED, false)
                .setValue(NORTH, false)
                .setValue(EAST, false)
                .setValue(SOUTH, false)
                .setValue(WEST, false)
        );
        this.hook = hook;
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return state.getValue(ATTACHED) ? SHAPE_ATTACHED : SHAPE_NOT_ATTACHED;
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        BlockGetter blockgetter = context.getLevel();
        BlockPos blockpos = context.getClickedPos();
        return this.defaultBlockState()
            .setValue(NORTH, this.shouldConnectTo(blockgetter.getBlockState(blockpos.north()), Direction.NORTH))
            .setValue(EAST, this.shouldConnectTo(blockgetter.getBlockState(blockpos.east()), Direction.EAST))
            .setValue(SOUTH, this.shouldConnectTo(blockgetter.getBlockState(blockpos.south()), Direction.SOUTH))
            .setValue(WEST, this.shouldConnectTo(blockgetter.getBlockState(blockpos.west()), Direction.WEST));
    }

    @Override
    protected BlockState updateShape(
        BlockState p_57645_,
        LevelReader p_374437_,
        ScheduledTickAccess p_374214_,
        BlockPos p_57649_,
        Direction p_57646_,
        BlockPos p_57650_,
        BlockState p_57647_,
        RandomSource p_374065_
    ) {
        return p_57646_.getAxis().isHorizontal()
            ? p_57645_.setValue(PROPERTY_BY_DIRECTION.get(p_57646_), this.shouldConnectTo(p_57647_, p_57646_))
            : super.updateShape(p_57645_, p_374437_, p_374214_, p_57649_, p_57646_, p_57650_, p_57647_, p_374065_);
    }

    @Override
    protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean isMoving) {
        if (!oldState.is(state.getBlock())) {
            this.updateSource(level, pos, state);
        }
    }

    @Override
    protected void affectNeighborsAfterRemoval(BlockState p_394298_, ServerLevel p_393642_, BlockPos p_393978_, boolean p_393483_) {
        if (!p_393483_) {
            this.updateSource(p_393642_, p_393978_, p_394298_.setValue(POWERED, true));
        }
    }

    @Override
    public BlockState playerWillDestroy(Level p_57615_, BlockPos p_57616_, BlockState p_57617_, Player p_57618_) {
        if (!p_57615_.isClientSide() && !p_57618_.getMainHandItem().isEmpty() && p_57618_.getMainHandItem().canPerformAction(net.neoforged.neoforge.common.ItemAbilities.SHEARS_DISARM)) {
            p_57615_.setBlock(p_57616_, p_57617_.setValue(DISARMED, true), 260);
            p_57615_.gameEvent(p_57618_, GameEvent.SHEAR, p_57616_);
        }

        return super.playerWillDestroy(p_57615_, p_57616_, p_57617_, p_57618_);
    }

    private void updateSource(Level level, BlockPos pos, BlockState state) {
        for (Direction direction : new Direction[]{Direction.SOUTH, Direction.WEST}) {
            for (int i = 1; i < 42; i++) {
                BlockPos blockpos = pos.relative(direction, i);
                BlockState blockstate = level.getBlockState(blockpos);
                if (blockstate.is(this.hook)) {
                    if (blockstate.getValue(TripWireHookBlock.FACING) == direction.getOpposite()) {
                        TripWireHookBlock.calculateState(level, blockpos, blockstate, false, true, i, state);
                    }
                    break;
                }

                if (!blockstate.is(this)) {
                    break;
                }
            }
        }
    }

    @Override
    protected VoxelShape getEntityInsideCollisionShape(BlockState p_371595_, BlockGetter p_400245_, BlockPos p_371231_, Entity p_399588_) {
        return p_371595_.getShape(p_400245_, p_371231_);
    }

    @Override
    protected void entityInside(BlockState p_57625_, Level p_57626_, BlockPos p_57627_, Entity p_57628_, InsideBlockEffectApplier p_405466_, boolean p_451757_) {
        if (!p_57626_.isClientSide()) {
            if (!p_57625_.getValue(POWERED)) {
                this.checkPressed(p_57626_, p_57627_, List.of(p_57628_));
            }
        }
    }

    @Override
    protected void tick(BlockState p_222598_, ServerLevel p_222599_, BlockPos p_222600_, RandomSource p_222601_) {
        if (p_222599_.getBlockState(p_222600_).getValue(POWERED)) {
            this.checkPressed(p_222599_, p_222600_);
        }
    }

    private void checkPressed(Level level, BlockPos pos) {
        BlockState blockstate = level.getBlockState(pos);
        List<? extends Entity> list = level.getEntities(null, blockstate.getShape(level, pos).bounds().move(pos));
        this.checkPressed(level, pos, list);
    }

    private void checkPressed(Level level, BlockPos pos, List<? extends Entity> entities) {
        BlockState blockstate = level.getBlockState(pos);
        boolean flag = blockstate.getValue(POWERED);
        boolean flag1 = false;
        if (!entities.isEmpty()) {
            for (Entity entity : entities) {
                if (!entity.isIgnoringBlockTriggers()) {
                    flag1 = true;
                    break;
                }
            }
        }

        if (flag1 != flag) {
            blockstate = blockstate.setValue(POWERED, flag1);
            level.setBlock(pos, blockstate, 3);
            this.updateSource(level, pos, blockstate);
        }

        if (flag1) {
            level.scheduleTick(new BlockPos(pos), this, 10);
        }
    }

    public boolean shouldConnectTo(BlockState state, Direction direction) {
        return state.is(this.hook) ? state.getValue(TripWireHookBlock.FACING) == direction.getOpposite() : state.is(this);
    }

    @Override
    protected BlockState rotate(BlockState state, Rotation rot) {
        switch (rot) {
            case CLOCKWISE_180:
                return state.setValue(NORTH, state.getValue(SOUTH))
                    .setValue(EAST, state.getValue(WEST))
                    .setValue(SOUTH, state.getValue(NORTH))
                    .setValue(WEST, state.getValue(EAST));
            case COUNTERCLOCKWISE_90:
                return state.setValue(NORTH, state.getValue(EAST))
                    .setValue(EAST, state.getValue(SOUTH))
                    .setValue(SOUTH, state.getValue(WEST))
                    .setValue(WEST, state.getValue(NORTH));
            case CLOCKWISE_90:
                return state.setValue(NORTH, state.getValue(WEST))
                    .setValue(EAST, state.getValue(NORTH))
                    .setValue(SOUTH, state.getValue(EAST))
                    .setValue(WEST, state.getValue(SOUTH));
            default:
                return state;
        }
    }

    @Override
    protected BlockState mirror(BlockState state, Mirror mirror) {
        switch (mirror) {
            case LEFT_RIGHT:
                return state.setValue(NORTH, state.getValue(SOUTH)).setValue(SOUTH, state.getValue(NORTH));
            case FRONT_BACK:
                return state.setValue(EAST, state.getValue(WEST)).setValue(WEST, state.getValue(EAST));
            default:
                return super.mirror(state, mirror);
        }
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(POWERED, ATTACHED, DISARMED, NORTH, EAST, WEST, SOUTH);
    }
}
