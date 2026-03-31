package net.minecraft.world.entity.ai.navigation;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.pathfinder.Path;
import org.jspecify.annotations.Nullable;

public class WallClimberNavigation extends GroundPathNavigation {
    /**
     * Current path navigation target
     */
    private @Nullable BlockPos pathToPosition;

    public WallClimberNavigation(Mob p_26580_, Level p_26581_) {
        super(p_26580_, p_26581_);
    }

    /**
     * Returns path to given BlockPos
     */
    @Override
    public Path createPath(BlockPos pos, int accuracy) {
        this.pathToPosition = pos;
        return super.createPath(pos, accuracy);
    }

    /**
     * Returns a path to the given entity or null
     */
    @Override
    public Path createPath(Entity entity, int p_26587_) {
        this.pathToPosition = entity.blockPosition();
        return super.createPath(entity, p_26587_);
    }

    /**
     * Try to find and set a path to EntityLiving. Returns {@code true} if successful.
     */
    @Override
    public boolean moveTo(Entity entity, double speed) {
        Path path = this.createPath(entity, 0);
        if (path != null) {
            return this.moveTo(path, speed);
        } else {
            this.pathToPosition = entity.blockPosition();
            this.speedModifier = speed;
            return true;
        }
    }

    @Override
    public void tick() {
        if (!this.isDone()) {
            super.tick();
        } else {
            if (this.pathToPosition != null) {
                // FORGE: Fix MC-94054
                if (!this.pathToPosition.closerToCenterThan(this.mob.position(), Math.max(this.mob.getBbWidth(), 1.0D))
                    && (
                        !(this.mob.getY() > this.pathToPosition.getY())
                            || !BlockPos.containing(this.pathToPosition.getX(), this.mob.getY(), this.pathToPosition.getZ())
                                .closerToCenterThan(this.mob.position(), Math.max(this.mob.getBbWidth(), 1.0D))
                    )) {
                    this.mob
                        .getMoveControl()
                        .setWantedPosition(this.pathToPosition.getX(), this.pathToPosition.getY(), this.pathToPosition.getZ(), this.speedModifier);
                } else {
                    this.pathToPosition = null;
                }
            }
        }
    }
}
