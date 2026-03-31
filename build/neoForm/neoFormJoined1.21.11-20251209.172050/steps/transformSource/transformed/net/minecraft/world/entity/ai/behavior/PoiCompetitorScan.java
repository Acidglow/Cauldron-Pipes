package net.minecraft.world.entity.ai.behavior;

import java.util.List;
import java.util.Optional;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.Holder;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.behavior.declarative.BehaviorBuilder;
import net.minecraft.world.entity.ai.behavior.declarative.MemoryAccessor;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.village.poi.PoiType;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.entity.npc.villager.VillagerProfession;

public class PoiCompetitorScan {
    public static BehaviorControl<Villager> create() {
        return BehaviorBuilder.create(
            p_258576_ -> p_258576_.group(p_258576_.present(MemoryModuleType.JOB_SITE), p_258576_.present(MemoryModuleType.NEAREST_LIVING_ENTITIES))
                .apply(
                    p_258576_,
                    (p_258590_, p_258591_) -> (p_258580_, p_481947_, p_258582_) -> {
                        GlobalPos globalpos = p_258576_.get(p_258590_);
                        p_258580_.getPoiManager()
                            .getType(globalpos.pos())
                            .ifPresent(
                                p_258588_ -> p_258576_.<List<LivingEntity>>get(p_258591_)
                                    .stream()
                                    .filter(p_477819_ -> p_477819_ instanceof Villager && p_477819_ != p_481947_)
                                    .map(p_477817_ -> (Villager)p_477817_)
                                    .filter(LivingEntity::isAlive)
                                    .filter(p_477822_ -> competesForSameJobsite(globalpos, p_258588_, p_477822_))
                                    .reduce(p_481947_, PoiCompetitorScan::selectWinner)
                            );
                        return true;
                    }
                )
        );
    }

    private static Villager selectWinner(Villager p_villager1, Villager villager2) {
        Villager villager;
        Villager villager1;
        if (p_villager1.getVillagerXp() > villager2.getVillagerXp()) {
            villager = p_villager1;
            villager1 = villager2;
        } else {
            villager = villager2;
            villager1 = p_villager1;
        }

        villager1.getBrain().eraseMemory(MemoryModuleType.JOB_SITE);
        return villager;
    }

    private static boolean competesForSameJobsite(GlobalPos pos, Holder<PoiType> poiType, Villager villager) {
        Optional<GlobalPos> optional = villager.getBrain().getMemory(MemoryModuleType.JOB_SITE);
        return optional.isPresent() && pos.equals(optional.get()) && hasMatchingProfession(poiType, villager.getVillagerData().profession());
    }

    private static boolean hasMatchingProfession(Holder<PoiType> poiType, Holder<VillagerProfession> profession) {
        return profession.value().heldJobSite().test(poiType);
    }
}
