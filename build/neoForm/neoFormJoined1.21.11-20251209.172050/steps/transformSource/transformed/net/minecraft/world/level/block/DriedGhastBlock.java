package net.minecraft.world.level.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.happyghast.HappyGhast;
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
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jspecify.annotations.Nullable;

public class DriedGhastBlock extends HorizontalDirectionalBlock implements SimpleWaterloggedBlock {
    public static final MapCodec<DriedGhastBlock> CODEC = simpleCodec(DriedGhastBlock::new);
    public static final int MAX_HYDRATION_LEVEL = 3;
    public static final IntegerProperty HYDRATION_LEVEL = BlockStateProperties.DRIED_GHAST_HYDRATION_LEVELS;
    public static final BooleanProperty WATERLOGGED = BlockStateProperties.WATERLOGGED;
    public static final int HYDRATION_TICK_DELAY = 5000;
    private static final VoxelShape SHAPE = Block.column(10.0, 10.0, 0.0, 10.0);

    @Override
    public MapCodec<DriedGhastBlock> codec() {
        return CODEC;
    }

    public DriedGhastBlock(BlockBehaviour.Properties p_416326_) {
        super(p_416326_);
        this.registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(HYDRATION_LEVEL, 0).setValue(WATERLOGGED, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> p_415740_) {
        p_415740_.add(FACING, HYDRATION_LEVEL, WATERLOGGED);
    }

    @Override
    protected BlockState updateShape(
        BlockState p_416335_,
        LevelReader p_415693_,
        ScheduledTickAccess p_416196_,
        BlockPos p_415889_,
        Direction p_415854_,
        BlockPos p_415679_,
        BlockState p_415825_,
        RandomSource p_415629_
    ) {
        if (p_416335_.getValue(WATERLOGGED)) {
            p_416196_.scheduleTick(p_415889_, Fluids.WATER, Fluids.WATER.getTickDelay(p_415693_));
        }

        return super.updateShape(p_416335_, p_415693_, p_416196_, p_415889_, p_415854_, p_415679_, p_415825_, p_415629_);
    }

    @Override
    public VoxelShape getShape(BlockState p_416548_, BlockGetter p_416476_, BlockPos p_416401_, CollisionContext p_415860_) {
        return SHAPE;
    }

    public int getHydrationLevel(BlockState state) {
        return state.getValue(HYDRATION_LEVEL);
    }

    private boolean isReadyToSpawn(BlockState state) {
        return this.getHydrationLevel(state) == 3;
    }

    @Override
    protected void tick(BlockState p_416215_, ServerLevel p_416576_, BlockPos p_415822_, RandomSource p_415656_) {
        if (p_416215_.getValue(WATERLOGGED)) {
            this.tickWaterlogged(p_416215_, p_416576_, p_415822_, p_415656_);
        } else {
            int i = this.getHydrationLevel(p_416215_);
            if (i > 0) {
                p_416576_.setBlock(p_415822_, p_416215_.setValue(HYDRATION_LEVEL, i - 1), 2);
                p_416576_.gameEvent(GameEvent.BLOCK_CHANGE, p_415822_, GameEvent.Context.of(p_416215_));
            }
        }
    }

    private void tickWaterlogged(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (!this.isReadyToSpawn(state)) {
            level.playSound(null, pos, SoundEvents.DRIED_GHAST_TRANSITION, SoundSource.BLOCKS, 1.0F, 1.0F);
            level.setBlock(pos, state.setValue(HYDRATION_LEVEL, this.getHydrationLevel(state) + 1), 2);
            level.gameEvent(GameEvent.BLOCK_CHANGE, pos, GameEvent.Context.of(state));
        } else {
            this.spawnGhastling(level, pos, state);
        }
    }

    private void spawnGhastling(ServerLevel level, BlockPos pos, BlockState state) {
        level.removeBlock(pos, false);
        HappyGhast happyghast = EntityType.HAPPY_GHAST.create(level, EntitySpawnReason.BREEDING);
        if (happyghast != null) {
            Vec3 vec3 = pos.getBottomCenter();
            happyghast.setBaby(true);
            float f = Direction.getYRot(state.getValue(FACING));
            happyghast.setYHeadRot(f);
            happyghast.snapTo(vec3.x(), vec3.y(), vec3.z(), f, 0.0F);
            level.addFreshEntity(happyghast);
            level.playSound(null, happyghast, SoundEvents.GHASTLING_SPAWN, SoundSource.BLOCKS, 1.0F, 1.0F);
        }
    }

    @Override
    public void animateTick(BlockState p_416682_, Level p_416357_, BlockPos p_415792_, RandomSource p_416494_) {
        double d0 = p_415792_.getX() + 0.5;
        double d1 = p_415792_.getY() + 0.5;
        double d2 = p_415792_.getZ() + 0.5;
        if (!p_416682_.getValue(WATERLOGGED)) {
            if (p_416494_.nextInt(40) == 0 && p_416357_.getBlockState(p_415792_.below()).is(BlockTags.TRIGGERS_AMBIENT_DRIED_GHAST_BLOCK_SOUNDS)) {
                p_416357_.playLocalSound(d0, d1, d2, SoundEvents.DRIED_GHAST_AMBIENT, SoundSource.BLOCKS, 1.0F, 1.0F, false);
            }

            if (p_416494_.nextInt(6) == 0) {
                p_416357_.addParticle(ParticleTypes.WHITE_SMOKE, d0, d1, d2, 0.0, 0.02, 0.0);
            }
        } else {
            if (p_416494_.nextInt(40) == 0) {
                p_416357_.playLocalSound(d0, d1, d2, SoundEvents.DRIED_GHAST_AMBIENT_WATER, SoundSource.BLOCKS, 1.0F, 1.0F, false);
            }

            if (p_416494_.nextInt(6) == 0) {
                p_416357_.addParticle(
                    ParticleTypes.HAPPY_VILLAGER,
                    d0 + (p_416494_.nextFloat() * 2.0F - 1.0F) / 3.0F,
                    d1 + 0.4,
                    d2 + (p_416494_.nextFloat() * 2.0F - 1.0F) / 3.0F,
                    0.0,
                    p_416494_.nextFloat(),
                    0.0
                );
            }
        }
    }

    @Override
    protected void randomTick(BlockState p_416248_, ServerLevel p_416607_, BlockPos p_415639_, RandomSource p_415633_) {
        if ((p_416248_.getValue(WATERLOGGED) || p_416248_.getValue(HYDRATION_LEVEL) > 0) && !p_416607_.getBlockTicks().hasScheduledTick(p_415639_, this)) {
            p_416607_.scheduleTick(p_415639_, this, 5000);
        }
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext p_416452_) {
        FluidState fluidstate = p_416452_.getLevel().getFluidState(p_416452_.getClickedPos());
        boolean flag = fluidstate.getType() == Fluids.WATER;
        return super.getStateForPlacement(p_416452_).setValue(WATERLOGGED, flag).setValue(FACING, p_416452_.getHorizontalDirection().getOpposite());
    }

    @Override
    protected FluidState getFluidState(BlockState p_416060_) {
        return p_416060_.getValue(WATERLOGGED) ? Fluids.WATER.getSource(false) : super.getFluidState(p_416060_);
    }

    @Override
    public boolean placeLiquid(LevelAccessor p_416379_, BlockPos p_415922_, BlockState p_416350_, FluidState p_415551_) {
        if (!p_416350_.getValue(BlockStateProperties.WATERLOGGED) && p_415551_.getType() == Fluids.WATER) {
            if (!p_416379_.isClientSide()) {
                p_416379_.setBlock(p_415922_, p_416350_.setValue(BlockStateProperties.WATERLOGGED, true), 3);
                p_416379_.scheduleTick(p_415922_, p_415551_.getType(), p_415551_.getType().getTickDelay(p_416379_));
                p_416379_.playSound(null, p_415922_, SoundEvents.DRIED_GHAST_PLACE_IN_WATER, SoundSource.BLOCKS, 1.0F, 1.0F);
            }

            return true;
        } else {
            return false;
        }
    }

    @Override
    public void setPlacedBy(Level p_418520_, BlockPos p_418137_, BlockState p_418409_, @Nullable LivingEntity p_418198_, ItemStack p_418118_) {
        super.setPlacedBy(p_418520_, p_418137_, p_418409_, p_418198_, p_418118_);
        p_418520_.playSound(
            null,
            p_418137_,
            p_418409_.getValue(WATERLOGGED) ? SoundEvents.DRIED_GHAST_PLACE_IN_WATER : SoundEvents.DRIED_GHAST_PLACE,
            SoundSource.BLOCKS,
            1.0F,
            1.0F
        );
    }

    @Override
    public boolean isPathfindable(BlockState p_416514_, PathComputationType p_415674_) {
        return false;
    }
}
