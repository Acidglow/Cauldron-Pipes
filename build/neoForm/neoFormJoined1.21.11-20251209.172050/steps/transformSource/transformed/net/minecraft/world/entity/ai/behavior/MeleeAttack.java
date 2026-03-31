package net.minecraft.world.entity.ai.behavior;

import java.util.function.Predicate;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.behavior.declarative.BehaviorBuilder;
import net.minecraft.world.entity.ai.behavior.declarative.MemoryAccessor;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.NearestVisibleLivingEntities;

public class MeleeAttack {
    public static <T extends Mob> OneShot<T> create(int attackCooldown) {
        return create(p_379104_ -> true, attackCooldown);
    }

    public static <T extends Mob> OneShot<T> create(Predicate<T> canAttack, int attackCooldown) {
        return BehaviorBuilder.create(
            p_379103_ -> p_379103_.group(
                    p_379103_.registered(MemoryModuleType.LOOK_TARGET),
                    p_379103_.present(MemoryModuleType.ATTACK_TARGET),
                    p_379103_.absent(MemoryModuleType.ATTACK_COOLING_DOWN),
                    p_379103_.present(MemoryModuleType.NEAREST_VISIBLE_LIVING_ENTITIES)
                )
                .apply(
                    p_379103_,
                    (p_379118_, p_379119_, p_379120_, p_379121_) -> (p_477814_, p_477815_, p_477816_) -> {
                        LivingEntity livingentity = p_379103_.get(p_379119_);
                        if (canAttack.test(p_477815_)
                            && !isHoldingUsableNonMeleeWeapon(p_477815_)
                            && p_477815_.isWithinMeleeAttackRange(livingentity)
                            && p_379103_.<NearestVisibleLivingEntities>get(p_379121_).contains(livingentity)) {
                            p_379118_.set(new EntityTracker(livingentity, true));
                            p_477815_.swing(InteractionHand.MAIN_HAND);
                            p_477815_.doHurtTarget(p_477814_, livingentity);
                            p_379120_.setWithExpiry(true, attackCooldown);
                            return true;
                        } else {
                            return false;
                        }
                    }
                )
        );
    }

    private static boolean isHoldingUsableNonMeleeWeapon(Mob mob) {
        return mob.isHolding(mob::canUseNonMeleeWeapon);
    }
}
