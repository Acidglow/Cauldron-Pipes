package net.minecraft.world.level.levelgen.feature;

import com.mojang.serialization.Codec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.configurations.ReplaceSphereConfiguration;
import org.jspecify.annotations.Nullable;

public class ReplaceBlobsFeature extends Feature<ReplaceSphereConfiguration> {
    public ReplaceBlobsFeature(Codec<ReplaceSphereConfiguration> p_66633_) {
        super(p_66633_);
    }

    @Override
    public boolean place(FeaturePlaceContext<ReplaceSphereConfiguration> p_160214_) {
        ReplaceSphereConfiguration replacesphereconfiguration = p_160214_.config();
        WorldGenLevel worldgenlevel = p_160214_.level();
        RandomSource randomsource = p_160214_.random();
        Block block = replacesphereconfiguration.targetState.getBlock();
        BlockPos blockpos = findTarget(
            worldgenlevel, p_160214_.origin().mutable().clamp(Direction.Axis.Y, worldgenlevel.getMinY() + 1, worldgenlevel.getMaxY()), block
        );
        if (blockpos == null) {
            return false;
        } else {
            int i = replacesphereconfiguration.radius().sample(randomsource);
            int j = replacesphereconfiguration.radius().sample(randomsource);
            int k = replacesphereconfiguration.radius().sample(randomsource);
            int l = Math.max(i, Math.max(j, k));
            boolean flag = false;

            for (BlockPos blockpos1 : BlockPos.withinManhattan(blockpos, i, j, k)) {
                if (blockpos1.distManhattan(blockpos) > l) {
                    break;
                }

                BlockState blockstate = worldgenlevel.getBlockState(blockpos1);
                if (blockstate.is(block)) {
                    this.setBlock(worldgenlevel, blockpos1, replacesphereconfiguration.replaceState);
                    flag = true;
                }
            }

            return flag;
        }
    }

    private static @Nullable BlockPos findTarget(LevelAccessor level, BlockPos.MutableBlockPos topPos, Block block) {
        while (topPos.getY() > level.getMinY() + 1) {
            BlockState blockstate = level.getBlockState(topPos);
            if (blockstate.is(block)) {
                return topPos;
            }

            topPos.move(Direction.DOWN);
        }

        return null;
    }
}
