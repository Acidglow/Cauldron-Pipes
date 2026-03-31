package net.minecraft.world.entity.ai.behavior;

import java.util.Map;
import net.minecraft.core.component.DataComponents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.MemoryStatus;
import net.minecraft.world.entity.ai.util.LandRandomPos;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

public class SpearRetreat extends Behavior<PathfinderMob> {
    public static final int MIN_COOLDOWN_DISTANCE = 9;
    public static final int MAX_COOLDOWN_DISTANCE = 11;
    public static final int MAX_FLEEING_TIME = 100;
    double speedModifierWhenRepositioning;

    public SpearRetreat(double speedModifierWhenRepositioning) {
        super(Map.of(MemoryModuleType.SPEAR_STATUS, MemoryStatus.VALUE_PRESENT), 100);
        this.speedModifierWhenRepositioning = speedModifierWhenRepositioning;
    }

    private @Nullable LivingEntity getTarget(PathfinderMob mob) {
        return mob.getBrain().getMemory(MemoryModuleType.ATTACK_TARGET).orElse(null);
    }

    private boolean ableToAttack(PathfinderMob mob) {
        return this.getTarget(mob) != null && mob.getMainHandItem().has(DataComponents.KINETIC_WEAPON);
    }

    protected boolean checkExtraStartConditions(ServerLevel p_480383_, PathfinderMob p_480274_) {
        if (this.ableToAttack(p_480274_) && !p_480274_.isUsingItem()) {
            if (p_480274_.getBrain().getMemory(MemoryModuleType.SPEAR_STATUS).orElse(SpearAttack.SpearStatus.APPROACH) != SpearAttack.SpearStatus.RETREAT) {
                return false;
            } else {
                LivingEntity livingentity = this.getTarget(p_480274_);
                double d0 = p_480274_.distanceToSqr(livingentity.getX(), livingentity.getY(), livingentity.getZ());
                int i = p_480274_.isPassenger() ? 2 : 0;
                double d1 = Math.sqrt(d0);
                Vec3 vec3 = LandRandomPos.getPosAway(p_480274_, Math.max(0.0, 9 + i - d1), Math.max(1.0, 11 + i - d1), 7, livingentity.position());
                if (vec3 == null) {
                    return false;
                } else {
                    p_480274_.getBrain().setMemory(MemoryModuleType.SPEAR_FLEEING_POSITION, vec3);
                    return true;
                }
            }
        } else {
            return false;
        }
    }

    protected void start(ServerLevel p_479412_, PathfinderMob p_480901_, long p_478082_) {
        p_480901_.setAggressive(true);
        p_480901_.getBrain().setMemory(MemoryModuleType.SPEAR_FLEEING_TIME, 0);
        super.start(p_479412_, p_480901_, p_478082_);
    }

    protected boolean canStillUse(ServerLevel p_478402_, PathfinderMob p_480228_, long p_479597_) {
        return p_480228_.getBrain().getMemory(MemoryModuleType.SPEAR_FLEEING_TIME).orElse(100) < 100
            && p_480228_.getBrain().getMemory(MemoryModuleType.SPEAR_FLEEING_POSITION).isPresent()
            && !p_480228_.getNavigation().isDone()
            && this.ableToAttack(p_480228_);
    }

    protected void tick(ServerLevel p_480204_, PathfinderMob p_478642_, long p_478332_) {
        LivingEntity livingentity = this.getTarget(p_478642_);
        float f = p_478642_.getRootVehicle() instanceof Mob mob ? mob.chargeSpeedModifier() : 1.0F;
        p_478642_.getBrain().setMemory(MemoryModuleType.LOOK_TARGET, new EntityTracker(livingentity, true));
        p_478642_.getBrain().setMemory(MemoryModuleType.SPEAR_FLEEING_TIME, p_478642_.getBrain().getMemory(MemoryModuleType.SPEAR_FLEEING_TIME).orElse(0) + 1);
        p_478642_.getBrain()
            .getMemory(MemoryModuleType.SPEAR_FLEEING_POSITION)
            .ifPresent(p_477997_ -> p_478642_.getNavigation().moveTo(p_477997_.x, p_477997_.y, p_477997_.z, f * this.speedModifierWhenRepositioning));
    }

    protected void stop(ServerLevel p_477955_, PathfinderMob p_479446_, long p_478023_) {
        p_479446_.getNavigation().stop();
        p_479446_.setAggressive(false);
        p_479446_.stopUsingItem();
        p_479446_.getBrain().eraseMemory(MemoryModuleType.SPEAR_FLEEING_TIME);
        p_479446_.getBrain().eraseMemory(MemoryModuleType.SPEAR_FLEEING_POSITION);
        p_479446_.getBrain().eraseMemory(MemoryModuleType.SPEAR_STATUS);
    }
}
