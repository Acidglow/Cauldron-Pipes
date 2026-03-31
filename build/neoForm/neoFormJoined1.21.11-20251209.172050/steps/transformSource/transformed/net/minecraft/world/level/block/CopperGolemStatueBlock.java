package net.minecraft.world.level.block;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.mojang.serialization.codecs.RecordCodecBuilder.Instance;
import java.util.function.IntFunction;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.FluidTags;
import net.minecraft.tags.ItemTags;
import net.minecraft.util.ByIdMap;
import net.minecraft.util.RandomSource;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.CopperGolemStatueBlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jspecify.annotations.Nullable;

public class CopperGolemStatueBlock extends BaseEntityBlock implements SimpleWaterloggedBlock {
    public static final MapCodec<CopperGolemStatueBlock> CODEC = RecordCodecBuilder.mapCodec(
        p_432934_ -> p_432934_.group(
                WeatheringCopper.WeatherState.CODEC.fieldOf("weathering_state").forGetter(CopperGolemStatueBlock::getWeatheringState), propertiesCodec()
            )
            .apply(p_432934_, CopperGolemStatueBlock::new)
    );
    public static final EnumProperty<Direction> FACING = BlockStateProperties.HORIZONTAL_FACING;
    public static final EnumProperty<CopperGolemStatueBlock.Pose> POSE = BlockStateProperties.COPPER_GOLEM_POSE;
    public static final BooleanProperty WATERLOGGED = BlockStateProperties.WATERLOGGED;
    private static final VoxelShape SHAPE = Block.column(10.0, 0.0, 14.0);
    private final WeatheringCopper.WeatherState weatheringState;

    @Override
    public MapCodec<? extends CopperGolemStatueBlock> codec() {
        return CODEC;
    }

    public CopperGolemStatueBlock(WeatheringCopper.WeatherState weatheringState, BlockBehaviour.Properties properties) {
        super(properties);
        this.weatheringState = weatheringState;
        this.registerDefaultState(
            this.defaultBlockState().setValue(FACING, Direction.NORTH).setValue(POSE, CopperGolemStatueBlock.Pose.STANDING).setValue(WATERLOGGED, false)
        );
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> p_433824_) {
        super.createBlockStateDefinition(p_433824_);
        p_433824_.add(FACING, POSE, WATERLOGGED);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext p_432939_) {
        FluidState fluidstate = p_432939_.getLevel().getFluidState(p_432939_.getClickedPos());
        return this.defaultBlockState()
            .setValue(FACING, p_432939_.getHorizontalDirection().getOpposite())
            .setValue(WATERLOGGED, fluidstate.getType() == Fluids.WATER);
    }

    @Override
    protected BlockState rotate(BlockState p_436043_, Rotation p_434280_) {
        return p_436043_.setValue(FACING, p_434280_.rotate(p_436043_.getValue(FACING)));
    }

    @Override
    protected BlockState mirror(BlockState p_435122_, Mirror p_435553_) {
        return p_435122_.rotate(p_435553_.getRotation(p_435122_.getValue(FACING)));
    }

    @Override
    protected VoxelShape getShape(BlockState p_435171_, BlockGetter p_435686_, BlockPos p_434293_, CollisionContext p_436023_) {
        return SHAPE;
    }

    public WeatheringCopper.WeatherState getWeatheringState() {
        return this.weatheringState;
    }

    @Override
    protected InteractionResult useItemOn(
        ItemStack p_435502_, BlockState p_435541_, Level p_432823_, BlockPos p_433231_, Player p_433625_, InteractionHand p_434602_, BlockHitResult p_432789_
    ) {
        if (p_435502_.is(ItemTags.AXES)) {
            return InteractionResult.PASS;
        } else {
            this.updatePose(p_432823_, p_435541_, p_433231_, p_433625_);
            return InteractionResult.SUCCESS;
        }
    }

    void updatePose(Level level, BlockState state, BlockPos pos, Player player) {
        level.playSound(null, pos, SoundEvents.COPPER_GOLEM_BECOME_STATUE, SoundSource.BLOCKS);
        level.setBlock(pos, state.setValue(POSE, state.getValue(POSE).getNextPose()), 3);
        level.gameEvent(player, GameEvent.BLOCK_CHANGE, pos);
    }

    @Override
    protected boolean isPathfindable(BlockState p_433730_, PathComputationType p_435626_) {
        return p_435626_ == PathComputationType.WATER && p_433730_.getFluidState().is(FluidTags.WATER);
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos p_435983_, BlockState p_435356_) {
        return new CopperGolemStatueBlockEntity(p_435983_, p_435356_);
    }

    @Override
    public boolean shouldChangedStateKeepBlockEntity(BlockState p_432894_) {
        return p_432894_.is(BlockTags.COPPER_GOLEM_STATUES);
    }

    @Override
    protected boolean hasAnalogOutputSignal(BlockState p_434228_) {
        return true;
    }

    @Override
    protected int getAnalogOutputSignal(BlockState p_433184_, Level p_435102_, BlockPos p_433790_, Direction p_433645_) {
        return p_433184_.getValue(POSE).ordinal() + 1;
    }

    @Override
    protected ItemStack getCloneItemStack(LevelReader p_447207_, BlockPos p_446066_, BlockState p_445465_, boolean p_446800_) {
        return p_447207_.getBlockEntity(p_446066_) instanceof CopperGolemStatueBlockEntity coppergolemstatueblockentity
            ? coppergolemstatueblockentity.getItem(this.asItem().getDefaultInstance(), p_445465_.getValue(POSE))
            : super.getCloneItemStack(p_447207_, p_446066_, p_445465_, p_446800_);
    }

    @Override
    protected void affectNeighborsAfterRemoval(BlockState p_442726_, ServerLevel p_443277_, BlockPos p_442605_, boolean p_443611_) {
        p_443277_.updateNeighbourForOutputSignal(p_442605_, p_442726_.getBlock());
    }

    @Override
    protected FluidState getFluidState(BlockState p_434045_) {
        return p_434045_.getValue(WATERLOGGED) ? Fluids.WATER.getSource(false) : super.getFluidState(p_434045_);
    }

    @Override
    protected BlockState updateShape(
        BlockState p_435930_,
        LevelReader p_433586_,
        ScheduledTickAccess p_435299_,
        BlockPos p_434867_,
        Direction p_435129_,
        BlockPos p_433500_,
        BlockState p_434804_,
        RandomSource p_435737_
    ) {
        if (p_435930_.getValue(WATERLOGGED)) {
            p_435299_.scheduleTick(p_434867_, Fluids.WATER, Fluids.WATER.getTickDelay(p_433586_));
        }

        return super.updateShape(p_435930_, p_433586_, p_435299_, p_434867_, p_435129_, p_433500_, p_434804_, p_435737_);
    }

    public static enum Pose implements StringRepresentable {
        STANDING("standing"),
        SITTING("sitting"),
        RUNNING("running"),
        STAR("star");

        public static final IntFunction<CopperGolemStatueBlock.Pose> BY_ID = ByIdMap.continuous(Enum::ordinal, values(), ByIdMap.OutOfBoundsStrategy.ZERO);
        public static final Codec<CopperGolemStatueBlock.Pose> CODEC = StringRepresentable.fromEnum(CopperGolemStatueBlock.Pose::values);
        private final String name;

        private Pose(String name) {
            this.name = name;
        }

        @Override
        public String getSerializedName() {
            return this.name;
        }

        public CopperGolemStatueBlock.Pose getNextPose() {
            return BY_ID.apply(this.ordinal() + 1);
        }
    }
}
