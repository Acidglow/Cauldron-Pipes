package net.minecraft.world.entity.ai.util;

import java.util.function.ToDoubleFunction;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

public class LandRandomPos {
    public static @Nullable Vec3 getPos(PathfinderMob mob, int radius, int verticalRange) {
        return getPos(mob, radius, verticalRange, mob::getWalkTargetValue);
    }

    public static @Nullable Vec3 getPos(PathfinderMob mob, int radius, int yRange, ToDoubleFunction<BlockPos> toDoubleFunction) {
        boolean flag = GoalUtils.mobRestricted(mob, radius);
        return RandomPos.generateRandomPos(() -> {
            BlockPos blockpos = RandomPos.generateRandomDirection(mob.getRandom(), radius, yRange);
            BlockPos blockpos1 = generateRandomPosTowardDirection(mob, radius, flag, blockpos);
            return blockpos1 == null ? null : movePosUpOutOfSolid(mob, blockpos1);
        }, toDoubleFunction);
    }

    public static @Nullable Vec3 getPosTowards(PathfinderMob mob, int radius, int yRange, Vec3 pos) {
        Vec3 vec3 = pos.subtract(mob.getX(), mob.getY(), mob.getZ());
        boolean flag = GoalUtils.mobRestricted(mob, radius);
        return getPosInDirection(mob, 0.0, radius, yRange, vec3, flag);
    }

    public static @Nullable Vec3 getPosAway(PathfinderMob mob, int radius, int yRange, Vec3 pos) {
        return getPosAway(mob, 0.0, radius, yRange, pos);
    }

    public static @Nullable Vec3 getPosAway(PathfinderMob mob, double minDistance, double maxDistance, int yRange, Vec3 pos) {
        Vec3 vec3 = mob.position().subtract(pos);
        if (vec3.length() == 0.0) {
            vec3 = new Vec3(mob.getRandom().nextDouble() - 0.5, 0.0, mob.getRandom().nextDouble() - 0.5);
        }

        boolean flag = GoalUtils.mobRestricted(mob, maxDistance);
        return getPosInDirection(mob, minDistance, maxDistance, yRange, vec3, flag);
    }

    private static @Nullable Vec3 getPosInDirection(
        PathfinderMob mob, double minDistance, double maxDistance, int yRange, Vec3 pos, boolean shortCircuit
    ) {
        return RandomPos.generateRandomPos(
            mob,
            () -> {
                BlockPos blockpos = RandomPos.generateRandomDirectionWithinRadians(
                    mob.getRandom(), minDistance, maxDistance, yRange, 0, pos.x, pos.z, (float) (Math.PI / 2)
                );
                if (blockpos == null) {
                    return null;
                } else {
                    BlockPos blockpos1 = generateRandomPosTowardDirection(mob, maxDistance, shortCircuit, blockpos);
                    return blockpos1 == null ? null : movePosUpOutOfSolid(mob, blockpos1);
                }
            }
        );
    }

    public static @Nullable BlockPos movePosUpOutOfSolid(PathfinderMob mob, BlockPos pos) {
        pos = RandomPos.moveUpOutOfSolid(pos, mob.level().getMaxY(), p_148534_ -> GoalUtils.isSolid(mob, p_148534_));
        return !GoalUtils.isWater(mob, pos) && !GoalUtils.hasMalus(mob, pos) ? pos : null;
    }

    public static @Nullable BlockPos generateRandomPosTowardDirection(PathfinderMob mob, double radius, boolean shortCircuit, BlockPos pos) {
        BlockPos blockpos = RandomPos.generateRandomPosTowardDirection(mob, radius, mob.getRandom(), pos);
        return !GoalUtils.isOutsideLimits(blockpos, mob)
                && !GoalUtils.isRestricted(shortCircuit, mob, blockpos)
                && !GoalUtils.isNotStable(mob.getNavigation(), blockpos)
            ? blockpos
            : null;
    }
}
