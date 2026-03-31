package net.minecraft.world.entity.ai.behavior;

import java.util.List;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.Holder;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.behavior.declarative.BehaviorBuilder;
import net.minecraft.world.entity.ai.behavior.declarative.MemoryAccessor;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.village.poi.PoiType;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.entity.npc.villager.VillagerProfession;
import net.minecraft.world.level.pathfinder.Path;

public class YieldJobSite {
    public static BehaviorControl<Villager> create(float speedModifier) {
        return BehaviorBuilder.create(
            p_258916_ -> p_258916_.group(
                    p_258916_.present(MemoryModuleType.POTENTIAL_JOB_SITE),
                    p_258916_.absent(MemoryModuleType.JOB_SITE),
                    p_258916_.present(MemoryModuleType.NEAREST_LIVING_ENTITIES),
                    p_258916_.registered(MemoryModuleType.WALK_TARGET),
                    p_258916_.registered(MemoryModuleType.LOOK_TARGET)
                )
                .apply(
                    p_258916_,
                    (p_258901_, p_258902_, p_258903_, p_258904_, p_258905_) -> (p_477873_, p_477874_, p_477875_) -> {
                        if (p_477874_.isBaby()) {
                            return false;
                        } else if (!p_477874_.getVillagerData().profession().is(VillagerProfession.NONE)) {
                            return false;
                        } else {
                            BlockPos blockpos = p_258916_.<GlobalPos>get(p_258901_).pos();
                            Optional<Holder<PoiType>> optional = p_477873_.getPoiManager().getType(blockpos);
                            if (optional.isEmpty()) {
                                return true;
                            } else {
                                p_258916_.<List<LivingEntity>>get(p_258903_)
                                    .stream()
                                    .filter(p_477863_ -> p_477863_ instanceof Villager && p_477863_ != p_477874_)
                                    .map(p_477861_ -> (Villager)p_477861_)
                                    .filter(LivingEntity::isAlive)
                                    .filter(p_477866_ -> nearbyWantsJobsite(optional.get(), p_477866_, blockpos))
                                    .findFirst()
                                    .ifPresent(p_477860_ -> {
                                        p_258904_.erase();
                                        p_258905_.erase();
                                        p_258901_.erase();
                                        if (p_477860_.getBrain().getMemory(MemoryModuleType.JOB_SITE).isEmpty()) {
                                            BehaviorUtils.setWalkAndLookTargetMemories(p_477860_, blockpos, speedModifier, 1);
                                            p_477860_.getBrain().setMemory(MemoryModuleType.POTENTIAL_JOB_SITE, GlobalPos.of(p_477873_.dimension(), blockpos));
                                            p_477873_.debugSynchronizers().updatePoi(blockpos);
                                        }
                                    });
                                return true;
                            }
                        }
                    }
                )
        );
    }

    private static boolean nearbyWantsJobsite(Holder<PoiType> poiType, Villager villager, BlockPos pos) {
        boolean flag = villager.getBrain().getMemory(MemoryModuleType.POTENTIAL_JOB_SITE).isPresent();
        if (flag) {
            return false;
        } else {
            Optional<GlobalPos> optional = villager.getBrain().getMemory(MemoryModuleType.JOB_SITE);
            Holder<VillagerProfession> holder = villager.getVillagerData().profession();
            if (holder.value().heldJobSite().test(poiType)) {
                return optional.isEmpty() ? canReachPos(villager, pos, poiType.value()) : optional.get().pos().equals(pos);
            } else {
                return false;
            }
        }
    }

    private static boolean canReachPos(PathfinderMob mob, BlockPos pos, PoiType poi) {
        Path path = mob.getNavigation().createPath(pos, poi.validRange());
        return path != null && path.canReach();
    }
}
