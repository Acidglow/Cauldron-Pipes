package net.minecraft.world.level.material;

import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.attribute.EnvironmentAttributes;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.InsideBlockEffectApplier;
import net.minecraft.world.entity.InsideBlockEffectType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.BaseFireBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.gamerules.GameRules;
import org.jspecify.annotations.Nullable;

public abstract class LavaFluid extends FlowingFluid {
    public static final float MIN_LEVEL_CUTOFF = 0.44444445F;

    @Override
    public Fluid getFlowing() {
        return Fluids.FLOWING_LAVA;
    }

    @Override
    public Fluid getSource() {
        return Fluids.LAVA;
    }

    @Override
    public Item getBucket() {
        return Items.LAVA_BUCKET;
    }

    @Override
    public void animateTick(Level p_230567_, BlockPos p_230568_, FluidState p_230569_, RandomSource p_230570_) {
        BlockPos blockpos = p_230568_.above();
        if (p_230567_.getBlockState(blockpos).isAir() && !p_230567_.getBlockState(blockpos).isSolidRender()) {
            if (p_230570_.nextInt(100) == 0) {
                double d0 = p_230568_.getX() + p_230570_.nextDouble();
                double d1 = p_230568_.getY() + 1.0;
                double d2 = p_230568_.getZ() + p_230570_.nextDouble();
                p_230567_.addParticle(ParticleTypes.LAVA, d0, d1, d2, 0.0, 0.0, 0.0);
                p_230567_.playLocalSound(
                    d0, d1, d2, SoundEvents.LAVA_POP, SoundSource.AMBIENT, 0.2F + p_230570_.nextFloat() * 0.2F, 0.9F + p_230570_.nextFloat() * 0.15F, false
                );
            }

            if (p_230570_.nextInt(200) == 0) {
                p_230567_.playLocalSound(
                    p_230568_.getX(),
                    p_230568_.getY(),
                    p_230568_.getZ(),
                    SoundEvents.LAVA_AMBIENT,
                    SoundSource.AMBIENT,
                    0.2F + p_230570_.nextFloat() * 0.2F,
                    0.9F + p_230570_.nextFloat() * 0.15F,
                    false
                );
            }
        }
    }

    @Override
    public void randomTick(ServerLevel p_376493_, BlockPos p_230573_, FluidState p_230574_, RandomSource p_230575_) {
        if (p_376493_.canSpreadFireAround(p_230573_)) {
            int i = p_230575_.nextInt(3);
            if (i > 0) {
                BlockPos blockpos = p_230573_;

                for (int j = 0; j < i; j++) {
                    blockpos = blockpos.offset(p_230575_.nextInt(3) - 1, 1, p_230575_.nextInt(3) - 1);
                    if (!p_376493_.isLoaded(blockpos)) {
                        return;
                    }

                    BlockState blockstate = p_376493_.getBlockState(blockpos);
                    if (blockstate.isAir()) {
                        if (this.hasFlammableNeighbours(p_376493_, blockpos)) {
                            p_376493_.setBlockAndUpdate(blockpos, net.neoforged.neoforge.event.EventHooks.fireFluidPlaceBlockEvent(p_376493_, blockpos, p_230573_, BaseFireBlock.getState(p_376493_, blockpos)));
                            return;
                        }
                    } else if (blockstate.blocksMotion()) {
                        return;
                    }
                }
            } else {
                for (int k = 0; k < 3; k++) {
                    BlockPos blockpos1 = p_230573_.offset(p_230575_.nextInt(3) - 1, 0, p_230575_.nextInt(3) - 1);
                    if (!p_376493_.isLoaded(blockpos1)) {
                        return;
                    }

                    if (p_376493_.isEmptyBlock(blockpos1.above()) && this.isFlammable(p_376493_, blockpos1, Direction.UP)) {
                        p_376493_.setBlockAndUpdate(blockpos1.above(), net.neoforged.neoforge.event.EventHooks.fireFluidPlaceBlockEvent(p_376493_, blockpos1.above(), p_230573_, BaseFireBlock.getState(p_376493_, blockpos1)));
                    }
                }
            }
        }
    }

    @Override
    protected void entityInside(Level p_405274_, BlockPos p_404936_, Entity p_405835_, InsideBlockEffectApplier p_404807_) {
        p_404807_.apply(InsideBlockEffectType.CLEAR_FREEZE);
        p_404807_.apply(InsideBlockEffectType.LAVA_IGNITE);
        p_404807_.runAfter(InsideBlockEffectType.LAVA_IGNITE, Entity::lavaHurt);
    }

    private boolean hasFlammableNeighbours(LevelReader level, BlockPos pos) {
        for (Direction direction : Direction.values()) {
            if (this.isFlammable(level, pos.relative(direction), direction.getOpposite())) {
                return true;
            }
        }

        return false;
    }

    /**
 * @deprecated Forge: use {@link LavaFluid#isFlammable(LevelReader,BlockPos,
 *             Direction)} instead
 */
    @Deprecated
    private boolean isFlammable(LevelReader level, BlockPos pos) {
        return level.isInsideBuildHeight(pos.getY()) && !level.hasChunkAt(pos) ? false : level.getBlockState(pos).ignitedByLava();
    }

    private boolean isFlammable(LevelReader p_level, BlockPos p_pos, Direction face) {
        if (p_level.isInsideBuildHeight(p_pos.getY()) && !p_level.hasChunkAt(p_pos)) {
            return false;
        }
        BlockState state = p_level.getBlockState(p_pos);
        return state.ignitedByLava(p_level, p_pos, face);
    }

    @Override
    public @Nullable ParticleOptions getDripParticle() {
        return ParticleTypes.DRIPPING_LAVA;
    }

    @Override
    protected void beforeDestroyingBlock(LevelAccessor level, BlockPos pos, BlockState state) {
        this.fizz(level, pos);
    }

    @Override
    public int getSlopeFindDistance(LevelReader level) {
        return isFastLava(level) ? 4 : 2;
    }

    @Override
    public BlockState createLegacyBlock(FluidState state) {
        return Blocks.LAVA.defaultBlockState().setValue(LiquidBlock.LEVEL, getLegacyLevel(state));
    }

    @Override
    public boolean isSame(Fluid fluid) {
        return fluid == Fluids.LAVA || fluid == Fluids.FLOWING_LAVA;
    }

    @Override
    public int getDropOff(LevelReader level) {
        return isFastLava(level) ? 1 : 2;
    }

    @Override
    public boolean canBeReplacedWith(FluidState fluidState, BlockGetter blockReader, BlockPos pos, Fluid fluid, Direction direction) {
        return fluidState.getHeight(blockReader, pos) >= 0.44444445F && fluid.is(FluidTags.WATER);
    }

    @Override
    public int getTickDelay(LevelReader p_76226_) {
        return isFastLava(p_76226_) ? 10 : 30;
    }

    @Override
    public int getSpreadDelay(Level p_76203_, BlockPos p_76204_, FluidState p_76205_, FluidState p_76206_) {
        int i = this.getTickDelay(p_76203_);
        if (!p_76205_.isEmpty()
            && !p_76206_.isEmpty()
            && !p_76205_.getValue(FALLING)
            && !p_76206_.getValue(FALLING)
            && p_76206_.getHeight(p_76203_, p_76204_) > p_76205_.getHeight(p_76203_, p_76204_)
            && p_76203_.getRandom().nextInt(4) != 0) {
            i *= 4;
        }

        return i;
    }

    private void fizz(LevelAccessor level, BlockPos pos) {
        level.levelEvent(1501, pos, 0);
    }

    @Override
    protected boolean canConvertToSource(ServerLevel p_376200_) {
        return p_376200_.getGameRules().get(GameRules.LAVA_SOURCE_CONVERSION);
    }

    @Override
    protected void spreadTo(LevelAccessor level, BlockPos pos, BlockState blockState, Direction direction, FluidState fluidState) {
        if (direction == Direction.DOWN) {
            FluidState fluidstate = level.getFluidState(pos);
            if (this.is(FluidTags.LAVA) && fluidstate.is(FluidTags.WATER)) {
                if (blockState.getBlock() instanceof LiquidBlock) {
                    level.setBlock(pos, net.neoforged.neoforge.event.EventHooks.fireFluidPlaceBlockEvent(level, pos, pos, Blocks.STONE.defaultBlockState()), 3);
                }

                this.fizz(level, pos);
                return;
            }
        }

        super.spreadTo(level, pos, blockState, direction, fluidState);
    }

    @Override
    protected boolean isRandomlyTicking() {
        return true;
    }

    @Override
    protected float getExplosionResistance() {
        return 100.0F;
    }

    @Override
    public Optional<SoundEvent> getPickupSound() {
        return Optional.of(SoundEvents.BUCKET_FILL_LAVA);
    }

    private static boolean isFastLava(LevelReader level) {
        return level.environmentAttributes().getDimensionValue(EnvironmentAttributes.FAST_LAVA);
    }

    public static class Flowing extends LavaFluid {
        @Override
        protected void createFluidStateDefinition(StateDefinition.Builder<Fluid, FluidState> builder) {
            super.createFluidStateDefinition(builder);
            builder.add(LEVEL);
        }

        @Override
        public int getAmount(FluidState state) {
            return state.getValue(LEVEL);
        }

        @Override
        public boolean isSource(FluidState state) {
            return false;
        }
    }

    public static class Source extends LavaFluid {
        @Override
        public int getAmount(FluidState state) {
            return 8;
        }

        @Override
        public boolean isSource(FluidState state) {
            return true;
        }
    }
}
