package net.minecraft.world.entity.ai.behavior;

import com.google.common.collect.ImmutableMap;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.Holder;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.MemoryStatus;
import net.minecraft.world.entity.ai.village.poi.PoiManager;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.entity.schedule.Activity;

public class GoToPotentialJobSite extends Behavior<Villager> {
    private static final int TICKS_UNTIL_TIMEOUT = 1200;
    final float speedModifier;

    public GoToPotentialJobSite(float speedModifier) {
        super(ImmutableMap.of(MemoryModuleType.POTENTIAL_JOB_SITE, MemoryStatus.VALUE_PRESENT), 1200);
        this.speedModifier = speedModifier;
    }

    protected boolean checkExtraStartConditions(ServerLevel p_23103_, Villager p_481143_) {
        return p_481143_.getBrain()
            .getActiveNonCoreActivity()
            .map(p_23115_ -> p_23115_ == Activity.IDLE || p_23115_ == Activity.WORK || p_23115_ == Activity.PLAY)
            .orElse(true);
    }

    protected boolean canStillUse(ServerLevel p_23106_, Villager p_481139_, long p_23108_) {
        return p_481139_.getBrain().hasMemoryValue(MemoryModuleType.POTENTIAL_JOB_SITE);
    }

    protected void tick(ServerLevel p_23121_, Villager p_479358_, long p_23123_) {
        BehaviorUtils.setWalkAndLookTargetMemories(
            p_479358_, p_479358_.getBrain().getMemory(MemoryModuleType.POTENTIAL_JOB_SITE).get().pos(), this.speedModifier, 1
        );
    }

    protected void stop(ServerLevel p_23129_, Villager p_481103_, long p_23131_) {
        Optional<GlobalPos> optional = p_481103_.getBrain().getMemory(MemoryModuleType.POTENTIAL_JOB_SITE);
        optional.ifPresent(p_448939_ -> {
            BlockPos blockpos = p_448939_.pos();
            ServerLevel serverlevel = p_23129_.getServer().getLevel(p_448939_.dimension());
            if (serverlevel != null) {
                PoiManager poimanager = serverlevel.getPoiManager();
                if (poimanager.exists(blockpos, p_217230_ -> true)) {
                    poimanager.release(blockpos);
                }

                p_23129_.debugSynchronizers().updatePoi(blockpos);
            }
        });
        p_481103_.getBrain().eraseMemory(MemoryModuleType.POTENTIAL_JOB_SITE);
    }
}
