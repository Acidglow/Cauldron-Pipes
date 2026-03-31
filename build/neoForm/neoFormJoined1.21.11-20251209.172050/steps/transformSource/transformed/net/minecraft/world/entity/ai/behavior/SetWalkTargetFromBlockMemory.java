package net.minecraft.world.entity.ai.behavior;

import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ai.behavior.declarative.BehaviorBuilder;
import net.minecraft.world.entity.ai.behavior.declarative.MemoryAccessor;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.WalkTarget;
import net.minecraft.world.entity.ai.util.DefaultRandomPos;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.phys.Vec3;

public class SetWalkTargetFromBlockMemory {
    public static OneShot<Villager> create(MemoryModuleType<GlobalPos> blockTargetMemory, float speedModifier, int closeEnoughDist, int tooFarDistance, int tooLongUnreachableDuration) {
        return BehaviorBuilder.create(
            p_258717_ -> p_258717_.group(
                    p_258717_.registered(MemoryModuleType.CANT_REACH_WALK_TARGET_SINCE),
                    p_258717_.absent(MemoryModuleType.WALK_TARGET),
                    p_258717_.present(blockTargetMemory)
                )
                .apply(p_258717_, (p_258709_, p_258710_, p_258711_) -> (p_477835_, p_477836_, p_477837_) -> {
                    GlobalPos globalpos = p_258717_.get(p_258711_);
                    Optional<Long> optional = p_258717_.tryGet(p_258709_);
                    if (globalpos.dimension() == p_477835_.dimension() && (!optional.isPresent() || p_477835_.getGameTime() - optional.get() <= tooLongUnreachableDuration)) {
                        if (globalpos.pos().distManhattan(p_477836_.blockPosition()) > tooFarDistance) {
                            Vec3 vec3 = null;
                            int i = 0;
                            int j = 1000;

                            while (vec3 == null || BlockPos.containing(vec3).distManhattan(p_477836_.blockPosition()) > tooFarDistance) {
                                vec3 = DefaultRandomPos.getPosTowards(p_477836_, 15, 7, Vec3.atBottomCenterOf(globalpos.pos()), (float) (Math.PI / 2));
                                if (++i == 1000) {
                                    p_477836_.releasePoi(blockTargetMemory);
                                    p_258711_.erase();
                                    p_258709_.set(p_477837_);
                                    return true;
                                }
                            }

                            p_258710_.set(new WalkTarget(vec3, speedModifier, closeEnoughDist));
                        } else if (globalpos.pos().distManhattan(p_477836_.blockPosition()) > closeEnoughDist) {
                            p_258710_.set(new WalkTarget(globalpos.pos(), speedModifier, closeEnoughDist));
                        }
                    } else {
                        p_477836_.releasePoi(blockTargetMemory);
                        p_258711_.erase();
                        p_258709_.set(p_477837_);
                    }

                    return true;
                })
        );
    }
}
