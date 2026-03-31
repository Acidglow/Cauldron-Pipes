package net.minecraft.world.entity.ai.behavior;

import java.util.function.BiPredicate;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.behavior.declarative.BehaviorBuilder;
import net.minecraft.world.entity.ai.behavior.declarative.MemoryAccessor;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.level.gamerules.GameRules;

public class StartCelebratingIfTargetDead {
    public static BehaviorControl<LivingEntity> create(int duration, BiPredicate<LivingEntity, LivingEntity> canDance) {
        return BehaviorBuilder.create(
            p_259600_ -> p_259600_.group(
                    p_259600_.present(MemoryModuleType.ATTACK_TARGET),
                    p_259600_.registered(MemoryModuleType.ANGRY_AT),
                    p_259600_.absent(MemoryModuleType.CELEBRATE_LOCATION),
                    p_259600_.registered(MemoryModuleType.DANCING)
                )
                .apply(p_259600_, (p_259049_, p_259067_, p_259031_, p_259141_) -> (p_460468_, p_460469_, p_460470_) -> {
                    LivingEntity livingentity = p_259600_.get(p_259049_);
                    if (!livingentity.isDeadOrDying()) {
                        return false;
                    } else {
                        if (canDance.test(p_460469_, livingentity)) {
                            p_259141_.setWithExpiry(true, duration);
                        }

                        p_259031_.setWithExpiry(livingentity.blockPosition(), duration);
                        if (livingentity.getType() != EntityType.PLAYER || p_460468_.getGameRules().get(GameRules.FORGIVE_DEAD_PLAYERS)) {
                            p_259049_.erase();
                            p_259067_.erase();
                        }

                        return true;
                    }
                })
        );
    }
}
