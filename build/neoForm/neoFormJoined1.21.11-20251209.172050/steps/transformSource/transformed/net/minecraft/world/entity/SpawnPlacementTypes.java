package net.minecraft.world.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.NaturalSpawner;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.pathfinder.PathComputationType;
import org.jspecify.annotations.Nullable;

public interface SpawnPlacementTypes {
    SpawnPlacementType NO_RESTRICTIONS = (p_321554_, p_321832_, p_321540_) -> true;
    SpawnPlacementType IN_WATER = (p_466537_, p_466538_, p_466539_) -> {
        if (p_466539_ != null && p_466537_.getWorldBorder().isWithinBounds(p_466538_)) {
            BlockPos blockpos = p_466538_.above();
            return p_466537_.getFluidState(p_466538_).is(FluidTags.WATER) && !p_466537_.getBlockState(blockpos).isRedstoneConductor(p_466537_, blockpos);
        } else {
            return false;
        }
    };
    SpawnPlacementType IN_LAVA = (p_466540_, p_466541_, p_466542_) -> p_466542_ != null && p_466540_.getWorldBorder().isWithinBounds(p_466541_)
        ? p_466540_.getFluidState(p_466541_).is(FluidTags.LAVA)
        : false;
    SpawnPlacementType ON_GROUND = new SpawnPlacementType() {
        @Override
        public boolean isSpawnPositionOk(LevelReader p_321666_, BlockPos p_321783_, @Nullable EntityType<?> p_321839_) {
            if (p_321839_ != null && p_321666_.getWorldBorder().isWithinBounds(p_321783_)) {
                BlockPos blockpos = p_321783_.above();
                BlockPos blockpos1 = p_321783_.below();
                BlockState blockstate = p_321666_.getBlockState(blockpos1);
                return !blockstate.isValidSpawn(p_321666_, blockpos1, p_321839_)
                    ? false
                    : this.isValidEmptySpawnBlock(p_321666_, p_321783_, p_321839_) && this.isValidEmptySpawnBlock(p_321666_, blockpos, p_321839_);
            } else {
                return false;
            }
        }

        private boolean isValidEmptySpawnBlock(LevelReader level, BlockPos pos, EntityType<?> entityType) {
            BlockState blockstate = level.getBlockState(pos);
            return NaturalSpawner.isValidEmptySpawnBlock(level, pos, blockstate, blockstate.getFluidState(), entityType);
        }

        @Override
        public BlockPos adjustSpawnPosition(LevelReader p_321527_, BlockPos p_321602_) {
            BlockPos blockpos = p_321602_.below();
            return p_321527_.getBlockState(blockpos).isPathfindable(PathComputationType.LAND) ? blockpos : p_321602_;
        }
    };
}
