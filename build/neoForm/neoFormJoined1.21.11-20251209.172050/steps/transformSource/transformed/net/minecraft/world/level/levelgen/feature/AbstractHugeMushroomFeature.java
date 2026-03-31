package net.minecraft.world.level.levelgen.feature;

import com.mojang.serialization.Codec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.configurations.HugeMushroomFeatureConfiguration;

public abstract class AbstractHugeMushroomFeature extends Feature<HugeMushroomFeatureConfiguration> {
    public AbstractHugeMushroomFeature(Codec<HugeMushroomFeatureConfiguration> p_65093_) {
        super(p_65093_);
    }

    protected void placeTrunk(
        LevelAccessor level,
        RandomSource random,
        BlockPos pos,
        HugeMushroomFeatureConfiguration config,
        int maxHeight,
        BlockPos.MutableBlockPos mutablePos
    ) {
        for (int i = 0; i < maxHeight; i++) {
            mutablePos.set(pos).move(Direction.UP, i);
            this.placeMushroomBlock(level, mutablePos, config.stemProvider.getState(random, pos));
        }
    }

    protected void placeMushroomBlock(LevelAccessor level, BlockPos.MutableBlockPos mutablePos, BlockState state) {
        BlockState blockstate = level.getBlockState(mutablePos);
        if (blockstate.isAir() || blockstate.is(BlockTags.REPLACEABLE_BY_MUSHROOMS)) {
            this.setBlock(level, mutablePos, state);
        }
    }

    protected int getTreeHeight(RandomSource random) {
        int i = random.nextInt(3) + 4;
        if (random.nextInt(12) == 0) {
            i *= 2;
        }

        return i;
    }

    protected boolean isValidPosition(
        LevelAccessor level, BlockPos pos, int maxHeight, BlockPos.MutableBlockPos mutablePos, HugeMushroomFeatureConfiguration config
    ) {
        int i = pos.getY();
        if (i >= level.getMinY() + 1 && i + maxHeight + 1 <= level.getMaxY()) {
            BlockState blockstate = level.getBlockState(pos.below());
            if (!isDirt(blockstate) && !blockstate.is(BlockTags.MUSHROOM_GROW_BLOCK)) {
                return false;
            } else {
                for (int j = 0; j <= maxHeight; j++) {
                    int k = this.getTreeRadiusForHeight(-1, -1, config.foliageRadius, j);

                    for (int l = -k; l <= k; l++) {
                        for (int i1 = -k; i1 <= k; i1++) {
                            BlockState blockstate1 = level.getBlockState(mutablePos.setWithOffset(pos, l, j, i1));
                            if (!blockstate1.isAir() && !blockstate1.is(BlockTags.LEAVES)) {
                                return false;
                            }
                        }
                    }
                }

                return true;
            }
        } else {
            return false;
        }
    }

    @Override
    public boolean place(FeaturePlaceContext<HugeMushroomFeatureConfiguration> p_159436_) {
        WorldGenLevel worldgenlevel = p_159436_.level();
        BlockPos blockpos = p_159436_.origin();
        RandomSource randomsource = p_159436_.random();
        HugeMushroomFeatureConfiguration hugemushroomfeatureconfiguration = p_159436_.config();
        int i = this.getTreeHeight(randomsource);
        BlockPos.MutableBlockPos blockpos$mutableblockpos = new BlockPos.MutableBlockPos();
        if (!this.isValidPosition(worldgenlevel, blockpos, i, blockpos$mutableblockpos, hugemushroomfeatureconfiguration)) {
            return false;
        } else {
            this.makeCap(worldgenlevel, randomsource, blockpos, i, blockpos$mutableblockpos, hugemushroomfeatureconfiguration);
            this.placeTrunk(worldgenlevel, randomsource, blockpos, hugemushroomfeatureconfiguration, i, blockpos$mutableblockpos);
            return true;
        }
    }

    protected abstract int getTreeRadiusForHeight(int unused, int height, int foliageRadius, int y);

    protected abstract void makeCap(
        LevelAccessor level,
        RandomSource random,
        BlockPos pos,
        int treeHeight,
        BlockPos.MutableBlockPos mutablePos,
        HugeMushroomFeatureConfiguration config
    );
}
