package net.minecraft.world.entity.ai.behavior;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.behavior.declarative.BehaviorBuilder;

public class UpdateActivityFromSchedule {
    public static BehaviorControl<LivingEntity> create() {
        return BehaviorBuilder.create(p_259429_ -> p_259429_.point((p_466588_, p_466589_, p_466590_) -> {
            p_466589_.getBrain().updateActivityFromSchedule(p_466588_.environmentAttributes(), p_466588_.getGameTime(), p_466589_.position());
            return true;
        }));
    }
}
