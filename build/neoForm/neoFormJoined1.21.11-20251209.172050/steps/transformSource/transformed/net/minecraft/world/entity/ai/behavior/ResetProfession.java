package net.minecraft.world.entity.ai.behavior;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ai.behavior.declarative.BehaviorBuilder;
import net.minecraft.world.entity.ai.behavior.declarative.MemoryAccessor;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.entity.npc.villager.VillagerData;
import net.minecraft.world.entity.npc.villager.VillagerProfession;

public class ResetProfession {
    public static BehaviorControl<Villager> create() {
        return BehaviorBuilder.create(
            p_259684_ -> p_259684_.group(p_259684_.absent(MemoryModuleType.JOB_SITE)).apply(p_259684_, p_260035_ -> (p_477823_, p_477824_, p_477825_) -> {
                VillagerData villagerdata = p_477824_.getVillagerData();
                boolean flag = !villagerdata.profession().is(VillagerProfession.NONE) && !villagerdata.profession().is(VillagerProfession.NITWIT);
                if (flag && p_477824_.getVillagerXp() == 0 && villagerdata.level() <= 1) {
                    p_477824_.setVillagerData(p_477824_.getVillagerData().withProfession(p_477823_.registryAccess(), VillagerProfession.NONE));
                    p_477824_.refreshBrain(p_477823_);
                    return true;
                } else {
                    return false;
                }
            })
        );
    }
}
