package net.minecraft.world.level.block;

import com.mojang.serialization.MapCodec;
import java.util.Collection;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;

public class SculkVeinBlock extends MultifaceSpreadeableBlock implements SculkBehaviour {
    public static final MapCodec<SculkVeinBlock> CODEC = simpleCodec(SculkVeinBlock::new);
    private final MultifaceSpreader veinSpreader = new MultifaceSpreader(new SculkVeinBlock.SculkVeinSpreaderConfig(MultifaceSpreader.DEFAULT_SPREAD_ORDER));
    private final MultifaceSpreader sameSpaceSpreader = new MultifaceSpreader(
        new SculkVeinBlock.SculkVeinSpreaderConfig(MultifaceSpreader.SpreadType.SAME_POSITION)
    );

    @Override
    public MapCodec<SculkVeinBlock> codec() {
        return CODEC;
    }

    public SculkVeinBlock(BlockBehaviour.Properties p_222353_) {
        super(p_222353_);
    }

    @Override
    public MultifaceSpreader getSpreader() {
        return this.veinSpreader;
    }

    public MultifaceSpreader getSameSpaceSpreader() {
        return this.sameSpaceSpreader;
    }

    public static boolean regrow(LevelAccessor level, BlockPos pos, BlockState state, Collection<Direction> directions) {
        boolean flag = false;
        BlockState blockstate = Blocks.SCULK_VEIN.defaultBlockState();

        for (Direction direction : directions) {
            if (canAttachTo(level, pos, direction)) {
                blockstate = blockstate.setValue(getFaceProperty(direction), true);
                flag = true;
            }
        }

        if (!flag) {
            return false;
        } else {
            if (!state.getFluidState().isEmpty()) {
                blockstate = blockstate.setValue(MultifaceBlock.WATERLOGGED, true);
            }

            level.setBlock(pos, blockstate, 3);
            return true;
        }
    }

    @Override
    public void onDischarged(LevelAccessor p_222359_, BlockState p_222360_, BlockPos p_222361_, RandomSource p_222362_) {
        if (p_222360_.is(this)) {
            for (Direction direction : DIRECTIONS) {
                BooleanProperty booleanproperty = getFaceProperty(direction);
                if (p_222360_.getValue(booleanproperty) && p_222359_.getBlockState(p_222361_.relative(direction)).is(Blocks.SCULK)) {
                    p_222360_ = p_222360_.setValue(booleanproperty, false);
                }
            }

            if (!hasAnyFace(p_222360_)) {
                FluidState fluidstate = p_222359_.getFluidState(p_222361_);
                p_222360_ = (fluidstate.isEmpty() ? Blocks.AIR : Blocks.WATER).defaultBlockState();
            }

            p_222359_.setBlock(p_222361_, p_222360_, 3);
            SculkBehaviour.super.onDischarged(p_222359_, p_222360_, p_222361_, p_222362_);
        }
    }

    @Override
    public int attemptUseCharge(
        SculkSpreader.ChargeCursor p_222369_, LevelAccessor p_222370_, BlockPos p_222371_, RandomSource p_222372_, SculkSpreader p_222373_, boolean p_222374_
    ) {
        if (p_222374_ && this.attemptPlaceSculk(p_222373_, p_222370_, p_222369_.getPos(), p_222372_)) {
            return p_222369_.getCharge() - 1;
        } else {
            return p_222372_.nextInt(p_222373_.chargeDecayRate()) == 0 ? Mth.floor(p_222369_.getCharge() * 0.5F) : p_222369_.getCharge();
        }
    }

    private boolean attemptPlaceSculk(SculkSpreader spreader, LevelAccessor level, BlockPos pos, RandomSource random) {
        BlockState blockstate = level.getBlockState(pos);
        TagKey<Block> tagkey = spreader.replaceableBlocks();

        for (Direction direction : Direction.allShuffled(random)) {
            if (hasFace(blockstate, direction)) {
                BlockPos blockpos = pos.relative(direction);
                BlockState blockstate1 = level.getBlockState(blockpos);
                if (blockstate1.is(tagkey)) {
                    BlockState blockstate2 = Blocks.SCULK.defaultBlockState();
                    level.setBlock(blockpos, blockstate2, 3);
                    Block.pushEntitiesUp(blockstate1, blockstate2, level, blockpos);
                    level.playSound(null, blockpos, SoundEvents.SCULK_BLOCK_SPREAD, SoundSource.BLOCKS, 1.0F, 1.0F);
                    this.veinSpreader.spreadAll(blockstate2, level, blockpos, spreader.isWorldGeneration());
                    Direction direction1 = direction.getOpposite();

                    for (Direction direction2 : DIRECTIONS) {
                        if (direction2 != direction1) {
                            BlockPos blockpos1 = blockpos.relative(direction2);
                            BlockState blockstate3 = level.getBlockState(blockpos1);
                            if (blockstate3.is(this)) {
                                this.onDischarged(level, blockstate3, blockpos1, random);
                            }
                        }
                    }

                    return true;
                }
            }
        }

        return false;
    }

    public static boolean hasSubstrateAccess(LevelAccessor level, BlockState state, BlockPos pos) {
        if (!state.is(Blocks.SCULK_VEIN)) {
            return false;
        } else {
            for (Direction direction : DIRECTIONS) {
                if (hasFace(state, direction) && level.getBlockState(pos.relative(direction)).is(BlockTags.SCULK_REPLACEABLE)) {
                    return true;
                }
            }

            return false;
        }
    }

    class SculkVeinSpreaderConfig extends MultifaceSpreader.DefaultSpreaderConfig {
        private final MultifaceSpreader.SpreadType[] spreadTypes;

        public SculkVeinSpreaderConfig(MultifaceSpreader.SpreadType... spreadTypes) {
            super(SculkVeinBlock.this);
            this.spreadTypes = spreadTypes;
        }

        @Override
        public boolean stateCanBeReplaced(BlockGetter p_222405_, BlockPos p_222406_, BlockPos p_222407_, Direction p_222408_, BlockState p_222409_) {
            BlockState blockstate = p_222405_.getBlockState(p_222407_.relative(p_222408_));
            if (!blockstate.is(Blocks.SCULK) && !blockstate.is(Blocks.SCULK_CATALYST) && !blockstate.is(Blocks.MOVING_PISTON)) {
                if (p_222406_.distManhattan(p_222407_) == 2) {
                    BlockPos blockpos = p_222406_.relative(p_222408_.getOpposite());
                    if (p_222405_.getBlockState(blockpos).isFaceSturdy(p_222405_, blockpos, p_222408_)) {
                        return false;
                    }
                }

                FluidState fluidstate = p_222409_.getFluidState();
                if (!fluidstate.isEmpty() && !fluidstate.is(Fluids.WATER)) {
                    return false;
                } else {
                    return p_222409_.is(BlockTags.FIRE)
                        ? false
                        : p_222409_.canBeReplaced() || super.stateCanBeReplaced(p_222405_, p_222406_, p_222407_, p_222408_, p_222409_);
                }
            } else {
                return false;
            }
        }

        @Override
        public MultifaceSpreader.SpreadType[] getSpreadTypes() {
            return this.spreadTypes;
        }

        @Override
        public boolean isOtherBlockValidAsSource(BlockState p_222411_) {
            return !p_222411_.is(Blocks.SCULK_VEIN);
        }
    }
}
