package net.minecraft.world.level.block;

import com.google.common.collect.Maps;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.mojang.serialization.codecs.RecordCodecBuilder.Instance;
import java.util.Map;
import java.util.function.BiConsumer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.util.Util;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.WoodType;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraft.world.level.redstone.Orientation;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.BooleanOp;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jspecify.annotations.Nullable;

public class FenceGateBlock extends HorizontalDirectionalBlock {
    public static final MapCodec<FenceGateBlock> CODEC = RecordCodecBuilder.mapCodec(
        p_308823_ -> p_308823_.group(WoodType.CODEC.optionalFieldOf("wood_type").forGetter(p_304842_ -> java.util.Optional.ofNullable(p_304842_.type)), propertiesCodec(),
                        net.minecraft.sounds.SoundEvent.DIRECT_CODEC.optionalFieldOf("open_sound").forGetter(fence -> java.util.Optional.of(fence.openSound).filter(s -> fence.type == null || s != fence.type.fenceGateOpen())),
                        net.minecraft.sounds.SoundEvent.DIRECT_CODEC.optionalFieldOf("close_sound").forGetter(fence -> java.util.Optional.of(fence.closeSound).filter(s -> fence.type == null || s != fence.type.fenceGateClose())))
                .apply(p_308823_, FenceGateBlock::new)
    );
    public static final BooleanProperty OPEN = BlockStateProperties.OPEN;
    public static final BooleanProperty POWERED = BlockStateProperties.POWERED;
    public static final BooleanProperty IN_WALL = BlockStateProperties.IN_WALL;
    private static final Map<Direction.Axis, VoxelShape> SHAPES = Shapes.rotateHorizontalAxis(Block.cube(16.0, 16.0, 4.0));
    private static final Map<Direction.Axis, VoxelShape> SHAPES_WALL = Maps.newEnumMap(
        Util.mapValues(SHAPES, p_393358_ -> Shapes.join(p_393358_, Block.column(16.0, 13.0, 16.0), BooleanOp.ONLY_FIRST))
    );
    private static final Map<Direction.Axis, VoxelShape> SHAPE_COLLISION = Shapes.rotateHorizontalAxis(Block.column(16.0, 4.0, 0.0, 24.0));
    private static final Map<Direction.Axis, VoxelShape> SHAPE_SUPPORT = Shapes.rotateHorizontalAxis(Block.column(16.0, 4.0, 5.0, 24.0));
    private static final Map<Direction.Axis, VoxelShape> SHAPE_OCCLUSION = Shapes.rotateHorizontalAxis(
        Shapes.or(Block.box(0.0, 5.0, 7.0, 2.0, 16.0, 9.0), Block.box(14.0, 5.0, 7.0, 16.0, 16.0, 9.0))
    );
    private static final Map<Direction.Axis, VoxelShape> SHAPE_OCCLUSION_WALL = Maps.newEnumMap(
        Util.mapValues(SHAPE_OCCLUSION, p_393359_ -> p_393359_.move(0.0, -0.1875, 0.0).optimize())
    );
    public final net.minecraft.sounds.SoundEvent openSound, closeSound;
    @org.jspecify.annotations.Nullable
    private final WoodType type;

    @Override
    public MapCodec<FenceGateBlock> codec() {
        return CODEC;
    }

    public FenceGateBlock(WoodType type, BlockBehaviour.Properties properties) {
        this(java.util.Optional.of(type), properties.sound(type.soundType()), java.util.Optional.of(type.fenceGateOpen()), java.util.Optional.of(type.fenceGateClose()));
    }

    public FenceGateBlock(BlockBehaviour.Properties p_273352_, net.minecraft.sounds.SoundEvent openSound, net.minecraft.sounds.SoundEvent closeSound) {
        this(java.util.Optional.empty(), p_273352_, java.util.Optional.of(openSound), java.util.Optional.of(closeSound));
    }

    public FenceGateBlock(java.util.Optional<WoodType> p_273340_, BlockBehaviour.Properties p_273352_, java.util.Optional<net.minecraft.sounds.SoundEvent> openSound, java.util.Optional<net.minecraft.sounds.SoundEvent> closeSound) {
        super(p_273352_);
        com.google.common.base.Preconditions.checkArgument(p_273340_.isPresent() || (openSound.isPresent() && closeSound.isPresent()), "Fence gates must have sounds set");
        this.type = p_273340_.orElse(null);
        this.openSound = openSound.orElseGet(() -> type.fenceGateOpen()); // Type may be null, so we cannot do a method ref
        this.closeSound = closeSound.orElseGet(() -> type.fenceGateClose());
        this.registerDefaultState(this.stateDefinition.any().setValue(OPEN, false).setValue(POWERED, false).setValue(IN_WALL, false));
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        Direction.Axis direction$axis = state.getValue(FACING).getAxis();
        return (state.getValue(IN_WALL) ? SHAPES_WALL : SHAPES).get(direction$axis);
    }

    @Override
    protected BlockState updateShape(
        BlockState p_53382_,
        LevelReader p_374250_,
        ScheduledTickAccess p_374455_,
        BlockPos p_53386_,
        Direction p_53383_,
        BlockPos p_53387_,
        BlockState p_53384_,
        RandomSource p_374522_
    ) {
        Direction.Axis direction$axis = p_53383_.getAxis();
        if (p_53382_.getValue(FACING).getClockWise().getAxis() != direction$axis) {
            return super.updateShape(p_53382_, p_374250_, p_374455_, p_53386_, p_53383_, p_53387_, p_53384_, p_374522_);
        } else {
            boolean flag = this.isWall(p_53384_) || this.isWall(p_374250_.getBlockState(p_53386_.relative(p_53383_.getOpposite())));
            return p_53382_.setValue(IN_WALL, flag);
        }
    }

    @Override
    protected VoxelShape getBlockSupportShape(BlockState p_253862_, BlockGetter p_254569_, BlockPos p_254197_) {
        Direction.Axis direction$axis = p_253862_.getValue(FACING).getAxis();
        return p_253862_.getValue(OPEN) ? Shapes.empty() : SHAPE_SUPPORT.get(direction$axis);
    }

    @Override
    protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        Direction.Axis direction$axis = state.getValue(FACING).getAxis();
        return state.getValue(OPEN) ? Shapes.empty() : SHAPE_COLLISION.get(direction$axis);
    }

    @Override
    protected VoxelShape getOcclusionShape(BlockState p_53401_) {
        Direction.Axis direction$axis = p_53401_.getValue(FACING).getAxis();
        return (p_53401_.getValue(IN_WALL) ? SHAPE_OCCLUSION_WALL : SHAPE_OCCLUSION).get(direction$axis);
    }

    @Override
    protected boolean isPathfindable(BlockState p_53360_, PathComputationType p_53363_) {
        switch (p_53363_) {
            case LAND:
                return p_53360_.getValue(OPEN);
            case WATER:
                return false;
            case AIR:
                return p_53360_.getValue(OPEN);
            default:
                return false;
        }
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        Level level = context.getLevel();
        BlockPos blockpos = context.getClickedPos();
        boolean flag = level.hasNeighborSignal(blockpos);
        Direction direction = context.getHorizontalDirection();
        Direction.Axis direction$axis = direction.getAxis();
        boolean flag1 = direction$axis == Direction.Axis.Z
                && (this.isWall(level.getBlockState(blockpos.west())) || this.isWall(level.getBlockState(blockpos.east())))
            || direction$axis == Direction.Axis.X && (this.isWall(level.getBlockState(blockpos.north())) || this.isWall(level.getBlockState(blockpos.south())));
        return this.defaultBlockState().setValue(FACING, direction).setValue(OPEN, flag).setValue(POWERED, flag).setValue(IN_WALL, flag1);
    }

    private boolean isWall(BlockState state) {
        return state.is(BlockTags.WALLS);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState p_53365_, Level p_53366_, BlockPos p_53367_, Player p_53368_, BlockHitResult p_53370_) {
        if (p_53365_.getValue(OPEN)) {
            p_53365_ = p_53365_.setValue(OPEN, false);
            p_53366_.setBlock(p_53367_, p_53365_, 10);
        } else {
            Direction direction = p_53368_.getDirection();
            if (p_53365_.getValue(FACING) == direction.getOpposite()) {
                p_53365_ = p_53365_.setValue(FACING, direction);
            }

            p_53365_ = p_53365_.setValue(OPEN, true);
            p_53366_.setBlock(p_53367_, p_53365_, 10);
        }

        boolean flag = p_53365_.getValue(OPEN);
        p_53366_.playSound(
            p_53368_,
            p_53367_,
            flag ? openSound : closeSound,
            SoundSource.BLOCKS,
            1.0F,
            p_53366_.getRandom().nextFloat() * 0.1F + 0.9F
        );
        p_53366_.gameEvent(p_53368_, flag ? GameEvent.BLOCK_OPEN : GameEvent.BLOCK_CLOSE, p_53367_);
        return InteractionResult.SUCCESS;
    }

    @Override
    protected void onExplosionHit(
        BlockState p_312699_, ServerLevel p_361116_, BlockPos p_312680_, Explosion p_312186_, BiConsumer<ItemStack, BlockPos> p_312187_
    ) {
        if (p_312186_.canTriggerBlocks() && !p_312699_.getValue(POWERED)) {
            boolean flag = p_312699_.getValue(OPEN);
            p_361116_.setBlockAndUpdate(p_312680_, p_312699_.setValue(OPEN, !flag));
            p_361116_.playSound(
                null,
                p_312680_,
                flag ? closeSound : openSound,
                SoundSource.BLOCKS,
                1.0F,
                p_361116_.getRandom().nextFloat() * 0.1F + 0.9F
            );
            p_361116_.gameEvent(flag ? GameEvent.BLOCK_CLOSE : GameEvent.BLOCK_OPEN, p_312680_, GameEvent.Context.of(p_312699_));
        }

        super.onExplosionHit(p_312699_, p_361116_, p_312680_, p_312186_, p_312187_);
    }

    @Override
    protected void neighborChanged(BlockState p_53372_, Level p_53373_, BlockPos p_53374_, Block p_53375_, @Nullable Orientation p_362989_, boolean p_53377_) {
        if (!p_53373_.isClientSide()) {
            boolean flag = p_53373_.hasNeighborSignal(p_53374_);
            if (p_53372_.getValue(POWERED) != flag) {
                p_53373_.setBlock(p_53374_, p_53372_.setValue(POWERED, flag).setValue(OPEN, flag), 2);
                if (p_53372_.getValue(OPEN) != flag) {
                    p_53373_.playSound(
                        null,
                        p_53374_,
                        flag ? openSound : closeSound,
                        SoundSource.BLOCKS,
                        1.0F,
                        p_53373_.getRandom().nextFloat() * 0.1F + 0.9F
                    );
                    p_53373_.gameEvent(null, flag ? GameEvent.BLOCK_OPEN : GameEvent.BLOCK_CLOSE, p_53374_);
                }
            }
        }
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, OPEN, POWERED, IN_WALL);
    }

    public static boolean connectsToDirection(BlockState state, Direction direction) {
        return state.getValue(FACING).getAxis() == direction.getClockWise().getAxis();
    }
}
