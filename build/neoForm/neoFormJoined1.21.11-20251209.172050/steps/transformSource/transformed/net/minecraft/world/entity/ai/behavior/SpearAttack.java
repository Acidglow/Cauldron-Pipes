package net.minecraft.world.entity.ai.behavior;

import java.util.Map;
import java.util.Optional;
import net.minecraft.core.component.DataComponents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.MemoryStatus;
import net.minecraft.world.entity.ai.util.LandRandomPos;
import net.minecraft.world.item.component.KineticWeapon;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

public class SpearAttack extends Behavior<PathfinderMob> {
    public static final int MIN_REPOSITION_DISTANCE = 6;
    public static final int MAX_REPOSITION_DISTANCE = 7;
    double speedModifierWhenCharging;
    double speedModifierWhenRepositioning;
    float approachDistanceSq;
    float targetInRangeRadiusSq;

    public SpearAttack(double speedModifierWhenCharging, double speedModifierWhenRepositioning, float approachDistance, float targetInRangeRadius) {
        super(Map.of(MemoryModuleType.SPEAR_STATUS, MemoryStatus.VALUE_PRESENT));
        this.speedModifierWhenCharging = speedModifierWhenCharging;
        this.speedModifierWhenRepositioning = speedModifierWhenRepositioning;
        this.approachDistanceSq = approachDistance * approachDistance;
        this.targetInRangeRadiusSq = targetInRangeRadius * targetInRangeRadius;
    }

    private @Nullable LivingEntity getTarget(PathfinderMob mob) {
        return mob.getBrain().getMemory(MemoryModuleType.ATTACK_TARGET).orElse(null);
    }

    private boolean ableToAttack(PathfinderMob mob) {
        return this.getTarget(mob) != null && mob.getMainHandItem().has(DataComponents.KINETIC_WEAPON);
    }

    private int getKineticWeaponUseDuration(PathfinderMob mob) {
        return Optional.ofNullable(mob.getMainHandItem().get(DataComponents.KINETIC_WEAPON)).map(KineticWeapon::computeDamageUseDuration).orElse(0);
    }

    protected boolean checkExtraStartConditions(ServerLevel p_477943_, PathfinderMob p_478350_) {
        return p_478350_.getBrain().getMemory(MemoryModuleType.SPEAR_STATUS).orElse(SpearAttack.SpearStatus.APPROACH) == SpearAttack.SpearStatus.CHARGING
            && this.ableToAttack(p_478350_)
            && !p_478350_.isUsingItem();
    }

    protected void start(ServerLevel p_481845_, PathfinderMob p_481252_, long p_479951_) {
        p_481252_.setAggressive(true);
        p_481252_.getBrain().setMemory(MemoryModuleType.SPEAR_ENGAGE_TIME, this.getKineticWeaponUseDuration(p_481252_));
        p_481252_.getBrain().eraseMemory(MemoryModuleType.SPEAR_CHARGE_POSITION);
        p_481252_.startUsingItem(InteractionHand.MAIN_HAND);
        super.start(p_481845_, p_481252_, p_479951_);
    }

    protected boolean canStillUse(ServerLevel p_478074_, PathfinderMob p_480194_, long p_480770_) {
        return p_480194_.getBrain().getMemory(MemoryModuleType.SPEAR_ENGAGE_TIME).orElse(0) > 0 && this.ableToAttack(p_480194_);
    }

    protected void tick(ServerLevel p_482125_, PathfinderMob p_480503_, long p_481370_) {
        LivingEntity livingentity = this.getTarget(p_480503_);
        double d0 = p_480503_.distanceToSqr(livingentity.getX(), livingentity.getY(), livingentity.getZ());
        Entity entity = p_480503_.getRootVehicle();
        float f = 1.0F;
        if (entity instanceof Mob mob) {
            f = mob.chargeSpeedModifier();
        }

        int i = p_480503_.isPassenger() ? 2 : 0;
        p_480503_.getBrain().setMemory(MemoryModuleType.LOOK_TARGET, new EntityTracker(livingentity, true));
        p_480503_.getBrain().setMemory(MemoryModuleType.SPEAR_ENGAGE_TIME, p_480503_.getBrain().getMemory(MemoryModuleType.SPEAR_ENGAGE_TIME).orElse(0) - 1);
        Vec3 vec3 = p_480503_.getBrain().getMemory(MemoryModuleType.SPEAR_CHARGE_POSITION).orElse(null);
        if (vec3 != null) {
            p_480503_.getNavigation().moveTo(vec3.x, vec3.y, vec3.z, f * this.speedModifierWhenRepositioning);
            if (p_480503_.getNavigation().isDone()) {
                p_480503_.getBrain().eraseMemory(MemoryModuleType.SPEAR_CHARGE_POSITION);
            }
        } else {
            p_480503_.getNavigation().moveTo(livingentity, f * this.speedModifierWhenCharging);
            if (d0 < this.targetInRangeRadiusSq || p_480503_.getNavigation().isDone()) {
                double d1 = Math.sqrt(d0);
                Vec3 vec31 = LandRandomPos.getPosAway(p_480503_, 6 + i - d1, 7 + i - d1, 7, livingentity.position());
                p_480503_.getBrain().setMemory(MemoryModuleType.SPEAR_CHARGE_POSITION, vec31);
            }
        }
    }

    protected void stop(ServerLevel p_479368_, PathfinderMob p_480087_, long p_482117_) {
        p_480087_.getNavigation().stop();
        p_480087_.stopUsingItem();
        p_480087_.getBrain().eraseMemory(MemoryModuleType.SPEAR_CHARGE_POSITION);
        p_480087_.getBrain().eraseMemory(MemoryModuleType.SPEAR_ENGAGE_TIME);
        p_480087_.getBrain().setMemory(MemoryModuleType.SPEAR_STATUS, SpearAttack.SpearStatus.RETREAT);
    }

    @Override
    protected boolean timedOut(long p_480750_) {
        return false;
    }

    public static enum SpearStatus {
        APPROACH,
        CHARGING,
        RETREAT;
    }
}
