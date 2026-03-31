package net.minecraft.world.entity.ai.util;

import com.google.common.annotations.VisibleForTesting;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.function.ToDoubleFunction;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

public class RandomPos {
    private static final int RANDOM_POS_ATTEMPTS = 10;

    /**
     * Gets a random position within a certain distance.
     */
    public static BlockPos generateRandomDirection(RandomSource random, int horizontalDistance, int verticalDistance) {
        int i = random.nextInt(2 * horizontalDistance + 1) - horizontalDistance;
        int j = random.nextInt(2 * verticalDistance + 1) - verticalDistance;
        int k = random.nextInt(2 * horizontalDistance + 1) - horizontalDistance;
        return new BlockPos(i, j, k);
    }

    public static @Nullable BlockPos generateRandomDirectionWithinRadians(
        RandomSource random, double minDistance, double maxDistance, int verticalDistance, int y, double x, double z, double radians
    ) {
        double d0 = Mth.atan2(z, x) - (float) (Math.PI / 2);
        double d1 = d0 + (2.0F * random.nextFloat() - 1.0F) * radians;
        double d2 = Mth.lerp(Math.sqrt(random.nextDouble()), minDistance, maxDistance) * Mth.SQRT_OF_TWO;
        double d3 = -d2 * Math.sin(d1);
        double d4 = d2 * Math.cos(d1);
        if (!(Math.abs(d3) > maxDistance) && !(Math.abs(d4) > maxDistance)) {
            int i = random.nextInt(2 * verticalDistance + 1) - verticalDistance + y;
            return BlockPos.containing(d3, i, d4);
        } else {
            return null;
        }
    }

    /**
     * @return the highest above position that is within the provided conditions
     */
    @VisibleForTesting
    public static BlockPos moveUpOutOfSolid(BlockPos pos, int maxY, Predicate<BlockPos> posPredicate) {
        if (!posPredicate.test(pos)) {
            return pos;
        } else {
            BlockPos.MutableBlockPos blockpos$mutableblockpos = pos.mutable().move(Direction.UP);

            while (blockpos$mutableblockpos.getY() <= maxY && posPredicate.test(blockpos$mutableblockpos)) {
                blockpos$mutableblockpos.move(Direction.UP);
            }

            return blockpos$mutableblockpos.immutable();
        }
    }

    /**
     * Finds a position above based on the conditions.
     *
     * After it finds the position once, it will continue to move up until aboveSolidAmount is reached or the position is no longer valid
     */
    @VisibleForTesting
    public static BlockPos moveUpToAboveSolid(BlockPos pos, int aboveSolidAmount, int maxY, Predicate<BlockPos> posPredicate) {
        if (aboveSolidAmount < 0) {
            throw new IllegalArgumentException("aboveSolidAmount was " + aboveSolidAmount + ", expected >= 0");
        } else if (!posPredicate.test(pos)) {
            return pos;
        } else {
            BlockPos.MutableBlockPos blockpos$mutableblockpos = pos.mutable().move(Direction.UP);

            while (blockpos$mutableblockpos.getY() <= maxY && posPredicate.test(blockpos$mutableblockpos)) {
                blockpos$mutableblockpos.move(Direction.UP);
            }

            int i = blockpos$mutableblockpos.getY();

            while (blockpos$mutableblockpos.getY() <= maxY && blockpos$mutableblockpos.getY() - i < aboveSolidAmount) {
                blockpos$mutableblockpos.move(Direction.UP);
                if (posPredicate.test(blockpos$mutableblockpos)) {
                    blockpos$mutableblockpos.move(Direction.DOWN);
                    break;
                }
            }

            return blockpos$mutableblockpos.immutable();
        }
    }

    public static @Nullable Vec3 generateRandomPos(PathfinderMob mob, Supplier<@Nullable BlockPos> posSupplier) {
        return generateRandomPos(posSupplier, mob::getWalkTargetValue);
    }

    /**
     * Tries 10 times to maximize the return value of the position to double function based on the supplied position
     */
    public static @Nullable Vec3 generateRandomPos(Supplier<@Nullable BlockPos> posSupplier, ToDoubleFunction<BlockPos> toDoubleFunction) {
        double d0 = Double.NEGATIVE_INFINITY;
        BlockPos blockpos = null;

        for (int i = 0; i < 10; i++) {
            BlockPos blockpos1 = posSupplier.get();
            if (blockpos1 != null) {
                double d1 = toDoubleFunction.applyAsDouble(blockpos1);
                if (d1 > d0) {
                    d0 = d1;
                    blockpos = blockpos1;
                }
            }
        }

        return blockpos != null ? Vec3.atBottomCenterOf(blockpos) : null;
    }

    public static BlockPos generateRandomPosTowardDirection(PathfinderMob mob, double radius, RandomSource random, BlockPos pos) {
        double d0 = pos.getX();
        double d1 = pos.getZ();
        if (mob.hasHome() && radius > 1.0) {
            BlockPos blockpos = mob.getHomePosition();
            if (mob.getX() > blockpos.getX()) {
                d0 -= random.nextDouble() * radius / 2.0;
            } else {
                d0 += random.nextDouble() * radius / 2.0;
            }

            if (mob.getZ() > blockpos.getZ()) {
                d1 -= random.nextDouble() * radius / 2.0;
            } else {
                d1 += random.nextDouble() * radius / 2.0;
            }
        }

        return BlockPos.containing(d0 + mob.getX(), pos.getY() + mob.getY(), d1 + mob.getZ());
    }
}
