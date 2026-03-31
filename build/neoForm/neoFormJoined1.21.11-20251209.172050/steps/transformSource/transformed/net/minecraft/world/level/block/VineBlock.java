package net.minecraft.world.level.block;

import com.mojang.serialization.MapCodec;
import java.util.Map;
import java.util.Map.Entry;
import java.util.function.Function;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.util.Util;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.gamerules.GameRules;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jspecify.annotations.Nullable;

public class VineBlock extends Block implements net.neoforged.neoforge.common.IShearable {
    public static final MapCodec<VineBlock> CODEC = simpleCodec(VineBlock::new);
    public static final BooleanProperty UP = PipeBlock.UP;
    public static final BooleanProperty NORTH = PipeBlock.NORTH;
    public static final BooleanProperty EAST = PipeBlock.EAST;
    public static final BooleanProperty SOUTH = PipeBlock.SOUTH;
    public static final BooleanProperty WEST = PipeBlock.WEST;
    public static final Map<Direction, BooleanProperty> PROPERTY_BY_DIRECTION = PipeBlock.PROPERTY_BY_DIRECTION
        .entrySet()
        .stream()
        .filter(p_57886_ -> p_57886_.getKey() != Direction.DOWN)
        .collect(Util.toMap());
    private final Function<BlockState, VoxelShape> shapes;

    @Override
    public MapCodec<VineBlock> codec() {
        return CODEC;
    }

    public VineBlock(BlockBehaviour.Properties p_57847_) {
        super(p_57847_);
        this.registerDefaultState(
            this.stateDefinition.any().setValue(UP, false).setValue(NORTH, false).setValue(EAST, false).setValue(SOUTH, false).setValue(WEST, false)
        );
        this.shapes = this.makeShapes();
    }

    private Function<BlockState, VoxelShape> makeShapes() {
        Map<Direction, VoxelShape> map = Shapes.rotateAll(Block.boxZ(16.0, 0.0, 1.0));
        return this.getShapeForEachState(p_393379_ -> {
            VoxelShape voxelshape = Shapes.empty();

            for (Entry<Direction, BooleanProperty> entry : PROPERTY_BY_DIRECTION.entrySet()) {
                if (p_393379_.getValue(entry.getValue())) {
                    voxelshape = Shapes.or(voxelshape, map.get(entry.getKey()));
                }
            }

            return voxelshape.isEmpty() ? Shapes.block() : voxelshape;
        });
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return this.shapes.apply(state);
    }

    @Override
    protected boolean propagatesSkylightDown(BlockState p_181239_) {
        return true;
    }

    @Override
    protected boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        return this.hasFaces(this.getUpdatedState(state, level, pos));
    }

    private boolean hasFaces(BlockState state) {
        return this.countFaces(state) > 0;
    }

    private int countFaces(BlockState state) {
        int i = 0;

        for (BooleanProperty booleanproperty : PROPERTY_BY_DIRECTION.values()) {
            if (state.getValue(booleanproperty)) {
                i++;
            }
        }

        return i;
    }

    private boolean canSupportAtFace(BlockGetter level, BlockPos pos, Direction direction) {
        if (direction == Direction.DOWN) {
            return false;
        } else {
            BlockPos blockpos = pos.relative(direction);
            if (isAcceptableNeighbour(level, blockpos, direction)) {
                return true;
            } else if (direction.getAxis() == Direction.Axis.Y) {
                return false;
            } else {
                BooleanProperty booleanproperty = PROPERTY_BY_DIRECTION.get(direction);
                BlockState blockstate = level.getBlockState(pos.above());
                return blockstate.is(this) && blockstate.getValue(booleanproperty);
            }
        }
    }

    public static boolean isAcceptableNeighbour(BlockGetter blockReader, BlockPos neighborPos, Direction attachedFace) {
        return MultifaceBlock.canAttachTo(blockReader, attachedFace, neighborPos, blockReader.getBlockState(neighborPos));
    }

    private BlockState getUpdatedState(BlockState state, BlockGetter level, BlockPos pos) {
        BlockPos blockpos = pos.above();
        if (state.getValue(UP)) {
            state = state.setValue(UP, isAcceptableNeighbour(level, blockpos, Direction.DOWN));
        }

        BlockState blockstate = null;

        for (Direction direction : Direction.Plane.HORIZONTAL) {
            BooleanProperty booleanproperty = getPropertyForFace(direction);
            if (state.getValue(booleanproperty)) {
                boolean flag = this.canSupportAtFace(level, pos, direction);
                if (!flag) {
                    if (blockstate == null) {
                        blockstate = level.getBlockState(blockpos);
                    }

                    flag = blockstate.is(this) && blockstate.getValue(booleanproperty);
                }

                state = state.setValue(booleanproperty, flag);
            }
        }

        return state;
    }

    @Override
    protected BlockState updateShape(
        BlockState p_57875_,
        LevelReader p_374150_,
        ScheduledTickAccess p_374192_,
        BlockPos p_57879_,
        Direction p_57876_,
        BlockPos p_57880_,
        BlockState p_57877_,
        RandomSource p_374278_
    ) {
        if (p_57876_ == Direction.DOWN) {
            return super.updateShape(p_57875_, p_374150_, p_374192_, p_57879_, p_57876_, p_57880_, p_57877_, p_374278_);
        } else {
            BlockState blockstate = this.getUpdatedState(p_57875_, p_374150_, p_57879_);
            return !this.hasFaces(blockstate) ? Blocks.AIR.defaultBlockState() : blockstate;
        }
    }

    @Override
    protected void randomTick(BlockState p_222655_, ServerLevel p_222656_, BlockPos p_222657_, RandomSource p_222658_) {
        if (p_222656_.getGameRules().get(GameRules.SPREAD_VINES)) {
            if (p_222656_.random.nextInt(4) == 0 && p_222656_.isAreaLoaded(p_222657_, 4)) { // Forge: check area to prevent loading unloaded chunks
                Direction direction = Direction.getRandom(p_222658_);
                BlockPos blockpos = p_222657_.above();
                if (direction.getAxis().isHorizontal() && !p_222655_.getValue(getPropertyForFace(direction))) {
                    if (this.canSpread(p_222656_, p_222657_)) {
                        BlockPos blockpos4 = p_222657_.relative(direction);
                        BlockState blockstate4 = p_222656_.getBlockState(blockpos4);
                        if (blockstate4.isAir()) {
                            Direction direction3 = direction.getClockWise();
                            Direction direction4 = direction.getCounterClockWise();
                            boolean flag = p_222655_.getValue(getPropertyForFace(direction3));
                            boolean flag1 = p_222655_.getValue(getPropertyForFace(direction4));
                            BlockPos blockpos2 = blockpos4.relative(direction3);
                            BlockPos blockpos3 = blockpos4.relative(direction4);
                            if (flag && isAcceptableNeighbour(p_222656_, blockpos2, direction3)) {
                                p_222656_.setBlock(blockpos4, this.defaultBlockState().setValue(getPropertyForFace(direction3), true), 2);
                            } else if (flag1 && isAcceptableNeighbour(p_222656_, blockpos3, direction4)) {
                                p_222656_.setBlock(blockpos4, this.defaultBlockState().setValue(getPropertyForFace(direction4), true), 2);
                            } else {
                                Direction direction1 = direction.getOpposite();
                                if (flag && p_222656_.isEmptyBlock(blockpos2) && isAcceptableNeighbour(p_222656_, p_222657_.relative(direction3), direction1)) {
                                    p_222656_.setBlock(blockpos2, this.defaultBlockState().setValue(getPropertyForFace(direction1), true), 2);
                                } else if (flag1
                                    && p_222656_.isEmptyBlock(blockpos3)
                                    && isAcceptableNeighbour(p_222656_, p_222657_.relative(direction4), direction1)) {
                                    p_222656_.setBlock(blockpos3, this.defaultBlockState().setValue(getPropertyForFace(direction1), true), 2);
                                } else if (p_222658_.nextFloat() < 0.05 && isAcceptableNeighbour(p_222656_, blockpos4.above(), Direction.UP)) {
                                    p_222656_.setBlock(blockpos4, this.defaultBlockState().setValue(UP, true), 2);
                                }
                            }
                        } else if (isAcceptableNeighbour(p_222656_, blockpos4, direction)) {
                            p_222656_.setBlock(p_222657_, p_222655_.setValue(getPropertyForFace(direction), true), 2);
                        }
                    }
                } else {
                    if (direction == Direction.UP && p_222657_.getY() < p_222656_.getMaxY()) {
                        if (this.canSupportAtFace(p_222656_, p_222657_, direction)) {
                            p_222656_.setBlock(p_222657_, p_222655_.setValue(UP, true), 2);
                            return;
                        }

                        if (p_222656_.isEmptyBlock(blockpos)) {
                            if (!this.canSpread(p_222656_, p_222657_)) {
                                return;
                            }

                            BlockState blockstate3 = p_222655_;

                            for (Direction direction2 : Direction.Plane.HORIZONTAL) {
                                if (p_222658_.nextBoolean() || !isAcceptableNeighbour(p_222656_, blockpos.relative(direction2), direction2)) {
                                    blockstate3 = blockstate3.setValue(getPropertyForFace(direction2), false);
                                }
                            }

                            if (this.hasHorizontalConnection(blockstate3)) {
                                p_222656_.setBlock(blockpos, blockstate3, 2);
                            }

                            return;
                        }
                    }

                    if (p_222657_.getY() > p_222656_.getMinY()) {
                        BlockPos blockpos1 = p_222657_.below();
                        BlockState blockstate = p_222656_.getBlockState(blockpos1);
                        if (blockstate.isAir() || blockstate.is(this)) {
                            BlockState blockstate1 = blockstate.isAir() ? this.defaultBlockState() : blockstate;
                            BlockState blockstate2 = this.copyRandomFaces(p_222655_, blockstate1, p_222658_);
                            if (blockstate1 != blockstate2 && this.hasHorizontalConnection(blockstate2)) {
                                p_222656_.setBlock(blockpos1, blockstate2, 2);
                            }
                        }
                    }
                }
            }
        }
    }

    private BlockState copyRandomFaces(BlockState sourceState, BlockState spreadState, RandomSource random) {
        for (Direction direction : Direction.Plane.HORIZONTAL) {
            if (random.nextBoolean()) {
                BooleanProperty booleanproperty = getPropertyForFace(direction);
                if (sourceState.getValue(booleanproperty)) {
                    spreadState = spreadState.setValue(booleanproperty, true);
                }
            }
        }

        return spreadState;
    }

    private boolean hasHorizontalConnection(BlockState state) {
        return state.getValue(NORTH) || state.getValue(EAST) || state.getValue(SOUTH) || state.getValue(WEST);
    }

    private boolean canSpread(BlockGetter blockReader, BlockPos pos) {
        int i = 4;
        Iterable<BlockPos> iterable = BlockPos.betweenClosed(
            pos.getX() - 4, pos.getY() - 1, pos.getZ() - 4, pos.getX() + 4, pos.getY() + 1, pos.getZ() + 4
        );
        int j = 5;

        for (BlockPos blockpos : iterable) {
            if (blockReader.getBlockState(blockpos).is(this)) {
                if (--j <= 0) {
                    return false;
                }
            }
        }

        return true;
    }

    @Override
    protected boolean canBeReplaced(BlockState state, BlockPlaceContext useContext) {
        BlockState blockstate = useContext.getLevel().getBlockState(useContext.getClickedPos());
        return blockstate.is(this) ? this.countFaces(blockstate) < PROPERTY_BY_DIRECTION.size() : super.canBeReplaced(state, useContext);
    }

    @Override
    public @Nullable BlockState getStateForPlacement(BlockPlaceContext context) {
        BlockState blockstate = context.getLevel().getBlockState(context.getClickedPos());
        boolean flag = blockstate.is(this);
        BlockState blockstate1 = flag ? blockstate : this.defaultBlockState();

        for (Direction direction : context.getNearestLookingDirections()) {
            if (direction != Direction.DOWN) {
                BooleanProperty booleanproperty = getPropertyForFace(direction);
                boolean flag1 = flag && blockstate.getValue(booleanproperty);
                if (!flag1 && this.canSupportAtFace(context.getLevel(), context.getClickedPos(), direction)) {
                    return blockstate1.setValue(booleanproperty, true);
                }
            }
        }

        return flag ? blockstate1 : null;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(UP, NORTH, EAST, SOUTH, WEST);
    }

    @Override
    protected BlockState rotate(BlockState state, Rotation rotate) {
        switch (rotate) {
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

    public static BooleanProperty getPropertyForFace(Direction face) {
        return PROPERTY_BY_DIRECTION.get(face);
    }
}
