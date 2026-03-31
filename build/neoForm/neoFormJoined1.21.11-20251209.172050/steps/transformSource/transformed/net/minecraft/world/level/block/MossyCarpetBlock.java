package net.minecraft.world.level.block;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import com.mojang.serialization.MapCodec;
import java.util.Map;
import java.util.Map.Entry;
import java.util.function.BooleanSupplier;
import java.util.function.Function;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.WallSide;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jspecify.annotations.Nullable;

public class MossyCarpetBlock extends Block implements BonemealableBlock {
    public static final MapCodec<MossyCarpetBlock> CODEC = simpleCodec(MossyCarpetBlock::new);
    public static final BooleanProperty BASE = BlockStateProperties.BOTTOM;
    public static final EnumProperty<WallSide> NORTH = BlockStateProperties.NORTH_WALL;
    public static final EnumProperty<WallSide> EAST = BlockStateProperties.EAST_WALL;
    public static final EnumProperty<WallSide> SOUTH = BlockStateProperties.SOUTH_WALL;
    public static final EnumProperty<WallSide> WEST = BlockStateProperties.WEST_WALL;
    public static final Map<Direction, EnumProperty<WallSide>> PROPERTY_BY_DIRECTION = ImmutableMap.copyOf(
        Maps.newEnumMap(Map.of(Direction.NORTH, NORTH, Direction.EAST, EAST, Direction.SOUTH, SOUTH, Direction.WEST, WEST))
    );
    private final Function<BlockState, VoxelShape> shapes;

    @Override
    public MapCodec<MossyCarpetBlock> codec() {
        return CODEC;
    }

    public MossyCarpetBlock(BlockBehaviour.Properties p_380381_) {
        super(p_380381_);
        this.registerDefaultState(
            this.stateDefinition
                .any()
                .setValue(BASE, true)
                .setValue(NORTH, WallSide.NONE)
                .setValue(EAST, WallSide.NONE)
                .setValue(SOUTH, WallSide.NONE)
                .setValue(WEST, WallSide.NONE)
        );
        this.shapes = this.makeShapes();
    }

    public Function<BlockState, VoxelShape> makeShapes() {
        Map<Direction, VoxelShape> map = Shapes.rotateHorizontal(Block.boxZ(16.0, 0.0, 10.0, 0.0, 1.0));
        Map<Direction, VoxelShape> map1 = Shapes.rotateAll(Block.boxZ(16.0, 0.0, 1.0));
        return this.getShapeForEachState(p_393367_ -> {
            VoxelShape voxelshape = p_393367_.getValue(BASE) ? map1.get(Direction.DOWN) : Shapes.empty();

            for (Entry<Direction, EnumProperty<WallSide>> entry : PROPERTY_BY_DIRECTION.entrySet()) {
                switch ((WallSide)p_393367_.getValue(entry.getValue())) {
                    case NONE:
                    default:
                        break;
                    case LOW:
                        voxelshape = Shapes.or(voxelshape, map.get(entry.getKey()));
                        break;
                    case TALL:
                        voxelshape = Shapes.or(voxelshape, map1.get(entry.getKey()));
                }
            }

            return voxelshape.isEmpty() ? Shapes.block() : voxelshape;
        });
    }

    @Override
    protected VoxelShape getShape(BlockState p_380262_, BlockGetter p_379532_, BlockPos p_379586_, CollisionContext p_380281_) {
        return this.shapes.apply(p_380262_);
    }

    @Override
    protected VoxelShape getCollisionShape(BlockState p_380336_, BlockGetter p_380068_, BlockPos p_379717_, CollisionContext p_379651_) {
        return p_380336_.getValue(BASE) ? this.shapes.apply(this.defaultBlockState()) : Shapes.empty();
    }

    @Override
    protected boolean propagatesSkylightDown(BlockState p_379750_) {
        return true;
    }

    @Override
    protected boolean canSurvive(BlockState p_379574_, LevelReader p_379768_, BlockPos p_380354_) {
        BlockState blockstate = p_379768_.getBlockState(p_380354_.below());
        return p_379574_.getValue(BASE) ? !blockstate.isAir() : blockstate.is(this) && blockstate.getValue(BASE);
    }

    private static boolean hasFaces(BlockState state) {
        if (state.getValue(BASE)) {
            return true;
        } else {
            for (EnumProperty<WallSide> enumproperty : PROPERTY_BY_DIRECTION.values()) {
                if (state.getValue(enumproperty) != WallSide.NONE) {
                    return true;
                }
            }

            return false;
        }
    }

    private static boolean canSupportAtFace(BlockGetter level, BlockPos pos, Direction direction) {
        return direction == Direction.UP ? false : MultifaceBlock.canAttachTo(level, pos, direction);
    }

    private static BlockState getUpdatedState(BlockState state, BlockGetter level, BlockPos pos, boolean tip) {
        BlockState blockstate = null;
        BlockState blockstate1 = null;
        tip |= state.getValue(BASE);

        for (Direction direction : Direction.Plane.HORIZONTAL) {
            EnumProperty<WallSide> enumproperty = getPropertyForFace(direction);
            WallSide wallside = canSupportAtFace(level, pos, direction)
                ? (tip ? WallSide.LOW : state.getValue(enumproperty))
                : WallSide.NONE;
            if (wallside == WallSide.LOW) {
                if (blockstate == null) {
                    blockstate = level.getBlockState(pos.above());
                }

                if (blockstate.is(Blocks.PALE_MOSS_CARPET) && blockstate.getValue(enumproperty) != WallSide.NONE && !blockstate.getValue(BASE)) {
                    wallside = WallSide.TALL;
                }

                if (!state.getValue(BASE)) {
                    if (blockstate1 == null) {
                        blockstate1 = level.getBlockState(pos.below());
                    }

                    if (blockstate1.is(Blocks.PALE_MOSS_CARPET) && blockstate1.getValue(enumproperty) == WallSide.NONE) {
                        wallside = WallSide.NONE;
                    }
                }
            }

            state = state.setValue(enumproperty, wallside);
        }

        return state;
    }

    @Override
    public @Nullable BlockState getStateForPlacement(BlockPlaceContext p_380111_) {
        return getUpdatedState(this.defaultBlockState(), p_380111_.getLevel(), p_380111_.getClickedPos(), true);
    }

    public static void placeAt(LevelAccessor level, BlockPos pos, RandomSource random, @Block.UpdateFlags int flags) {
        BlockState blockstate = Blocks.PALE_MOSS_CARPET.defaultBlockState();
        BlockState blockstate1 = getUpdatedState(blockstate, level, pos, true);
        level.setBlock(pos, blockstate1, flags);
        BlockState blockstate2 = createTopperWithSideChance(level, pos, random::nextBoolean);
        if (!blockstate2.isAir()) {
            level.setBlock(pos.above(), blockstate2, flags);
            BlockState blockstate3 = getUpdatedState(blockstate1, level, pos, true);
            level.setBlock(pos, blockstate3, flags);
        }
    }

    @Override
    public void setPlacedBy(Level p_380310_, BlockPos p_380202_, BlockState p_379659_, @Nullable LivingEntity p_379877_, ItemStack p_380344_) {
        if (!p_380310_.isClientSide()) {
            RandomSource randomsource = p_380310_.getRandom();
            BlockState blockstate = createTopperWithSideChance(p_380310_, p_380202_, randomsource::nextBoolean);
            if (!blockstate.isAir()) {
                p_380310_.setBlock(p_380202_.above(), blockstate, 3);
            }
        }
    }

    private static BlockState createTopperWithSideChance(BlockGetter level, BlockPos pos, BooleanSupplier placeSide) {
        BlockPos blockpos = pos.above();
        BlockState blockstate = level.getBlockState(blockpos);
        boolean flag = blockstate.is(Blocks.PALE_MOSS_CARPET);
        if ((!flag || !blockstate.getValue(BASE)) && (flag || blockstate.canBeReplaced())) {
            BlockState blockstate1 = Blocks.PALE_MOSS_CARPET.defaultBlockState().setValue(BASE, false);
            BlockState blockstate2 = getUpdatedState(blockstate1, level, pos.above(), true);

            for (Direction direction : Direction.Plane.HORIZONTAL) {
                EnumProperty<WallSide> enumproperty = getPropertyForFace(direction);
                if (blockstate2.getValue(enumproperty) != WallSide.NONE && !placeSide.getAsBoolean()) {
                    blockstate2 = blockstate2.setValue(enumproperty, WallSide.NONE);
                }
            }

            return hasFaces(blockstate2) && blockstate2 != blockstate ? blockstate2 : Blocks.AIR.defaultBlockState();
        } else {
            return Blocks.AIR.defaultBlockState();
        }
    }

    @Override
    protected BlockState updateShape(
        BlockState p_379698_,
        LevelReader p_379600_,
        ScheduledTickAccess p_380394_,
        BlockPos p_380051_,
        Direction p_380408_,
        BlockPos p_380380_,
        BlockState p_379613_,
        RandomSource p_379309_
    ) {
        if (!p_379698_.canSurvive(p_379600_, p_380051_)) {
            return Blocks.AIR.defaultBlockState();
        } else {
            BlockState blockstate = getUpdatedState(p_379698_, p_379600_, p_380051_, false);
            return !hasFaces(blockstate) ? Blocks.AIR.defaultBlockState() : blockstate;
        }
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> p_379510_) {
        p_379510_.add(BASE, NORTH, EAST, SOUTH, WEST);
    }

    @Override
    protected BlockState rotate(BlockState p_379325_, Rotation p_380164_) {
        return switch (p_380164_) {
            case CLOCKWISE_180 -> (BlockState)p_379325_.setValue(NORTH, p_379325_.getValue(SOUTH))
                .setValue(EAST, p_379325_.getValue(WEST))
                .setValue(SOUTH, p_379325_.getValue(NORTH))
                .setValue(WEST, p_379325_.getValue(EAST));
            case COUNTERCLOCKWISE_90 -> (BlockState)p_379325_.setValue(NORTH, p_379325_.getValue(EAST))
                .setValue(EAST, p_379325_.getValue(SOUTH))
                .setValue(SOUTH, p_379325_.getValue(WEST))
                .setValue(WEST, p_379325_.getValue(NORTH));
            case CLOCKWISE_90 -> (BlockState)p_379325_.setValue(NORTH, p_379325_.getValue(WEST))
                .setValue(EAST, p_379325_.getValue(NORTH))
                .setValue(SOUTH, p_379325_.getValue(EAST))
                .setValue(WEST, p_379325_.getValue(SOUTH));
            default -> p_379325_;
        };
    }

    @Override
    protected BlockState mirror(BlockState p_379462_, Mirror p_380184_) {
        return switch (p_380184_) {
            case LEFT_RIGHT -> (BlockState)p_379462_.setValue(NORTH, p_379462_.getValue(SOUTH)).setValue(SOUTH, p_379462_.getValue(NORTH));
            case FRONT_BACK -> (BlockState)p_379462_.setValue(EAST, p_379462_.getValue(WEST)).setValue(WEST, p_379462_.getValue(EAST));
            default -> super.mirror(p_379462_, p_380184_);
        };
    }

    public static @Nullable EnumProperty<WallSide> getPropertyForFace(Direction direction) {
        return PROPERTY_BY_DIRECTION.get(direction);
    }

    @Override
    public boolean isValidBonemealTarget(LevelReader p_379909_, BlockPos p_379807_, BlockState p_379358_) {
        return p_379358_.getValue(BASE) && !createTopperWithSideChance(p_379909_, p_379807_, () -> true).isAir();
    }

    @Override
    public boolean isBonemealSuccess(Level p_380168_, RandomSource p_380045_, BlockPos p_380299_, BlockState p_379595_) {
        return true;
    }

    @Override
    public void performBonemeal(ServerLevel p_379402_, RandomSource p_379670_, BlockPos p_379387_, BlockState p_379934_) {
        BlockState blockstate = createTopperWithSideChance(p_379402_, p_379387_, () -> true);
        if (!blockstate.isAir()) {
            p_379402_.setBlock(p_379387_.above(), blockstate, 3);
        }
    }
}
