package net.minecraft.world.entity.ai.behavior;

import java.util.Map;
import net.minecraft.core.component.DataComponents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.MemoryStatus;
import org.jspecify.annotations.Nullable;

public class SpearApproach extends Behavior<PathfinderMob> {
    double speedModifierWhenRepositioning;
    float approachDistanceSq;

    public SpearApproach(double speedModifierWhenRepositioning, float approachDistance) {
        super(Map.of(MemoryModuleType.SPEAR_STATUS, MemoryStatus.VALUE_ABSENT));
        this.speedModifierWhenRepositioning = speedModifierWhenRepositioning;
        this.approachDistanceSq = approachDistance * approachDistance;
    }

    private boolean ableToAttack(PathfinderMob mob) {
        return this.getTarget(mob) != null && mob.getMainHandItem().has(DataComponents.KINETIC_WEAPON);
    }

    protected boolean checkExtraStartConditions(ServerLevel p_479816_, PathfinderMob p_480371_) {
        return this.ableToAttack(p_480371_) && !p_480371_.isUsingItem();
    }

    protected void start(ServerLevel p_480370_, PathfinderMob p_481666_, long p_478777_) {
        p_481666_.setAggressive(true);
        p_481666_.getBrain().setMemory(MemoryModuleType.SPEAR_STATUS, SpearAttack.SpearStatus.APPROACH);
        super.start(p_480370_, p_481666_, p_478777_);
    }

    private @Nullable LivingEntity getTarget(PathfinderMob mob) {
        return mob.getBrain().getMemory(MemoryModuleType.ATTACK_TARGET).orElse(null);
    }

    protected boolean canStillUse(ServerLevel p_479731_, PathfinderMob p_477963_, long p_480663_) {
        return this.ableToAttack(p_477963_) && this.farEnough(p_477963_);
    }

    private boolean farEnough(PathfinderMob mob) {
        LivingEntity livingentity = this.getTarget(mob);
        double d0 = mob.distanceToSqr(livingentity.getX(), livingentity.getY(), livingentity.getZ());
        return d0 > this.approachDistanceSq;
    }

    protected void tick(ServerLevel p_481133_, PathfinderMob p_478677_, long p_478612_) {
        LivingEntity livingentity = this.getTarget(p_478677_);
        Entity entity = p_478677_.getRootVehicle();
        float f = 1.0F;
        if (entity instanceof Mob mob) {
            f = mob.chargeSpeedModifier();
        }

        p_478677_.getBrain().setMemory(MemoryModuleType.LOOK_TARGET, new EntityTracker(livingentity, true));
        p_478677_.getNavigation().moveTo(livingentity, f * this.speedModifierWhenRepositioning);
    }

    protected void stop(ServerLevel p_480695_, PathfinderMob p_480316_, long p_480772_) {
        p_480316_.getNavigation().stop();
        p_480316_.getBrain().setMemory(MemoryModuleType.SPEAR_STATUS, SpearAttack.SpearStatus.CHARGING);
    }

    @Override
    protected boolean timedOut(long p_479534_) {
        return false;
    }
}
