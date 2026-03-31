package net.minecraft.world.entity.ai.behavior;

import java.util.Collections;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Util;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.behavior.declarative.BehaviorBuilder;
import net.minecraft.world.entity.ai.behavior.declarative.MemoryAccessor;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.WalkTarget;

public class InsideBrownianWalk {
    public static BehaviorControl<PathfinderMob> create(float speedModifier) {
        return BehaviorBuilder.create(
            p_258399_ -> p_258399_.group(p_258399_.absent(MemoryModuleType.WALK_TARGET))
                .apply(
                    p_258399_,
                    p_258397_ -> (p_466566_, p_466567_, p_466568_) -> {
                        if (p_466566_.canSeeSky(p_466567_.blockPosition())) {
                            return false;
                        } else {
                            BlockPos blockpos = p_466567_.blockPosition();
                            List<BlockPos> list = BlockPos.betweenClosedStream(blockpos.offset(-1, -1, -1), blockpos.offset(1, 1, 1))
                                .map(BlockPos::immutable)
                                .collect(Util.toMutableList());
                            Collections.shuffle(list);
                            list.stream()
                                .filter(p_466570_ -> !p_466566_.canSeeSky(p_466570_))
                                .filter(p_23237_ -> p_466566_.loadedAndEntityCanStandOn(p_23237_, p_466567_))
                                .filter(p_23227_ -> p_466566_.noCollision(p_466567_))
                                .findFirst()
                                .ifPresent(p_258402_ -> p_258397_.set(new WalkTarget(p_258402_, speedModifier, 0)));
                            return true;
                        }
                    }
                )
        );
    }
}
