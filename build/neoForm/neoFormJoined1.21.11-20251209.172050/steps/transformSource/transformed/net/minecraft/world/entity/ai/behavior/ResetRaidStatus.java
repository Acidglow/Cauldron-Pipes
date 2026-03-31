package net.minecraft.world.entity.ai.behavior;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.Brain;
import net.minecraft.world.entity.ai.behavior.declarative.BehaviorBuilder;
import net.minecraft.world.entity.raid.Raid;
import net.minecraft.world.entity.schedule.Activity;

public class ResetRaidStatus {
    public static BehaviorControl<LivingEntity> create() {
        return BehaviorBuilder.create(p_259870_ -> p_259870_.point((p_466576_, p_466577_, p_466578_) -> {
            if (p_466576_.random.nextInt(20) != 0) {
                return false;
            } else {
                Brain<?> brain = p_466577_.getBrain();
                Raid raid = p_466576_.getRaidAt(p_466577_.blockPosition());
                if (raid == null || raid.isStopped() || raid.isLoss()) {
                    brain.setDefaultActivity(Activity.IDLE);
                    brain.updateActivityFromSchedule(p_466576_.environmentAttributes(), p_466576_.getGameTime(), p_466577_.position());
                }

                return true;
            }
        }));
    }
}
