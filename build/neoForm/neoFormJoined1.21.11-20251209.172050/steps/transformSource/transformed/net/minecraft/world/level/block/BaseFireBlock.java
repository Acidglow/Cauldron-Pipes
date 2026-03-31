package net.minecraft.world.level.block;

import com.mojang.serialization.MapCodec;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.InsideBlockEffectApplier;
import net.minecraft.world.entity.InsideBlockEffectType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.portal.PortalShape;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

public abstract class BaseFireBlock extends Block {
    private static final int SECONDS_ON_FIRE = 8;
    private static final int MIN_FIRE_TICKS_TO_ADD = 1;
    private static final int MAX_FIRE_TICKS_TO_ADD = 3;
    private final float fireDamage;
    protected static final VoxelShape SHAPE = Block.column(16.0, 0.0, 1.0);

    public BaseFireBlock(BlockBehaviour.Properties properties, float fireDamage) {
        super(properties);
        this.fireDamage = fireDamage;
    }

    @Override
    protected abstract MapCodec<? extends BaseFireBlock> codec();

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return getState(context.getLevel(), context.getClickedPos());
    }

    public static BlockState getState(BlockGetter reader, BlockPos pos) {
        BlockPos blockpos = pos.below();
        BlockState blockstate = reader.getBlockState(blockpos);
        return SoulFireBlock.canSurviveOnBlock(blockstate)
            ? Blocks.SOUL_FIRE.defaultBlockState()
            : ((FireBlock)Blocks.FIRE).getStateForPlacement(reader, pos);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    public void animateTick(BlockState p_220763_, Level p_220764_, BlockPos p_220765_, RandomSource p_220766_) {
        if (p_220766_.nextInt(24) == 0) {
            p_220764_.playLocalSound(
                p_220765_.getX() + 0.5,
                p_220765_.getY() + 0.5,
                p_220765_.getZ() + 0.5,
                SoundEvents.FIRE_AMBIENT,
                SoundSource.BLOCKS,
                1.0F + p_220766_.nextFloat(),
                p_220766_.nextFloat() * 0.7F + 0.3F,
                false
            );
        }

        BlockPos blockpos = p_220765_.below();
        BlockState blockstate = p_220764_.getBlockState(blockpos);
        if (!this.canBurn(blockstate) && !blockstate.isFaceSturdy(p_220764_, blockpos, Direction.UP)) {
            if (this.canBurn(p_220764_.getBlockState(p_220765_.west()))) {
                for (int j = 0; j < 2; j++) {
                    double d3 = p_220765_.getX() + p_220766_.nextDouble() * 0.1F;
                    double d8 = p_220765_.getY() + p_220766_.nextDouble();
                    double d13 = p_220765_.getZ() + p_220766_.nextDouble();
                    p_220764_.addParticle(ParticleTypes.LARGE_SMOKE, d3, d8, d13, 0.0, 0.0, 0.0);
                }
            }

            if (this.canBurn(p_220764_.getBlockState(p_220765_.east()))) {
                for (int k = 0; k < 2; k++) {
                    double d4 = p_220765_.getX() + 1 - p_220766_.nextDouble() * 0.1F;
                    double d9 = p_220765_.getY() + p_220766_.nextDouble();
                    double d14 = p_220765_.getZ() + p_220766_.nextDouble();
                    p_220764_.addParticle(ParticleTypes.LARGE_SMOKE, d4, d9, d14, 0.0, 0.0, 0.0);
                }
            }

            if (this.canBurn(p_220764_.getBlockState(p_220765_.north()))) {
                for (int l = 0; l < 2; l++) {
                    double d5 = p_220765_.getX() + p_220766_.nextDouble();
                    double d10 = p_220765_.getY() + p_220766_.nextDouble();
                    double d15 = p_220765_.getZ() + p_220766_.nextDouble() * 0.1F;
                    p_220764_.addParticle(ParticleTypes.LARGE_SMOKE, d5, d10, d15, 0.0, 0.0, 0.0);
                }
            }

            if (this.canBurn(p_220764_.getBlockState(p_220765_.south()))) {
                for (int i1 = 0; i1 < 2; i1++) {
                    double d6 = p_220765_.getX() + p_220766_.nextDouble();
                    double d11 = p_220765_.getY() + p_220766_.nextDouble();
                    double d16 = p_220765_.getZ() + 1 - p_220766_.nextDouble() * 0.1F;
                    p_220764_.addParticle(ParticleTypes.LARGE_SMOKE, d6, d11, d16, 0.0, 0.0, 0.0);
                }
            }

            if (this.canBurn(p_220764_.getBlockState(p_220765_.above()))) {
                for (int j1 = 0; j1 < 2; j1++) {
                    double d7 = p_220765_.getX() + p_220766_.nextDouble();
                    double d12 = p_220765_.getY() + 1 - p_220766_.nextDouble() * 0.1F;
                    double d17 = p_220765_.getZ() + p_220766_.nextDouble();
                    p_220764_.addParticle(ParticleTypes.LARGE_SMOKE, d7, d12, d17, 0.0, 0.0, 0.0);
                }
            }
        } else {
            for (int i = 0; i < 3; i++) {
                double d0 = p_220765_.getX() + p_220766_.nextDouble();
                double d1 = p_220765_.getY() + p_220766_.nextDouble() * 0.5 + 0.5;
                double d2 = p_220765_.getZ() + p_220766_.nextDouble();
                p_220764_.addParticle(ParticleTypes.LARGE_SMOKE, d0, d1, d2, 0.0, 0.0, 0.0);
            }
        }
    }

    protected abstract boolean canBurn(BlockState state);

    @Override
    protected void entityInside(BlockState p_49260_, Level p_49261_, BlockPos p_49262_, Entity p_49263_, InsideBlockEffectApplier p_404795_, boolean p_451760_) {
        p_404795_.apply(InsideBlockEffectType.CLEAR_FREEZE);
        p_404795_.apply(InsideBlockEffectType.FIRE_IGNITE);
        p_404795_.runAfter(InsideBlockEffectType.FIRE_IGNITE, p_405041_ -> p_405041_.hurt(p_405041_.level().damageSources().inFire(), this.fireDamage));
    }

    public static void fireIgnite(Entity entity) {
        if (!entity.fireImmune()) {
            if (entity.getRemainingFireTicks() < 0) {
                entity.setRemainingFireTicks(entity.getRemainingFireTicks() + 1);
            } else if (entity instanceof ServerPlayer) {
                int i = entity.level().getRandom().nextInt(1, 3);
                entity.setRemainingFireTicks(entity.getRemainingFireTicks() + i);
            }

            if (entity.getRemainingFireTicks() >= 0) {
                entity.igniteForSeconds(8.0F);
            }
        }
    }

    @Override
    protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean isMoving) {
        if (!oldState.is(state.getBlock())) {
            if (inPortalDimension(level)) {
                Optional<PortalShape> optional = PortalShape.findEmptyPortalShape(level, pos, Direction.Axis.X);
                optional = net.neoforged.neoforge.event.EventHooks.onTrySpawnPortal(level, pos, optional);
                if (optional.isPresent()) {
                    optional.get().createPortalBlocks(level);
                    return;
                }
            }

            if (!state.canSurvive(level, pos)) {
                level.removeBlock(pos, false);
            }
        }
    }

    private static boolean inPortalDimension(Level level) {
        return level.dimension() == Level.OVERWORLD || level.dimension() == Level.NETHER;
    }

    @Override
    protected void spawnDestroyParticles(Level p_152139_, Player p_152140_, BlockPos p_152141_, BlockState p_152142_) {
    }

    @Override
    public BlockState playerWillDestroy(Level p_49251_, BlockPos p_49252_, BlockState p_49253_, Player p_49254_) {
        if (!p_49251_.isClientSide()) {
            p_49251_.levelEvent(null, 1009, p_49252_, 0);
        }

        return super.playerWillDestroy(p_49251_, p_49252_, p_49253_, p_49254_);
    }

    public static boolean canBePlacedAt(Level level, BlockPos pos, Direction direction) {
        BlockState blockstate = level.getBlockState(pos);
        return !blockstate.isAir() ? false : getState(level, pos).canSurvive(level, pos) || isPortal(level, pos, direction);
    }

    private static boolean isPortal(Level level, BlockPos pos, Direction p_direction) {
        if (!inPortalDimension(level)) {
            return false;
        } else {
            BlockPos.MutableBlockPos blockpos$mutableblockpos = pos.mutable();
            boolean flag = false;

            for (Direction direction : Direction.values()) {
                if (level.getBlockState(blockpos$mutableblockpos.set(pos).move(direction)).isPortalFrame(level, blockpos$mutableblockpos)) {
                    flag = true;
                    break;
                }
            }

            if (!flag) {
                return false;
            } else {
                Direction.Axis direction$axis = p_direction.getAxis().isHorizontal()
                    ? p_direction.getCounterClockWise().getAxis()
                    : Direction.Plane.HORIZONTAL.getRandomAxis(level.random);
                return PortalShape.findEmptyPortalShape(level, pos, direction$axis).isPresent();
            }
        }
    }
}
