package net.minecraft.world.entity.ai.behavior;

import java.util.Optional;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ai.behavior.declarative.BehaviorBuilder;
import net.minecraft.world.entity.ai.behavior.declarative.MemoryAccessor;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.village.poi.PoiType;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.entity.npc.villager.VillagerProfession;

public class AssignProfessionFromJobSite {
    public static BehaviorControl<Villager> create() {
        return BehaviorBuilder.create(
            p_258312_ -> p_258312_.group(p_258312_.present(MemoryModuleType.POTENTIAL_JOB_SITE), p_258312_.registered(MemoryModuleType.JOB_SITE))
                .apply(
                    p_258312_,
                    (p_258304_, p_258305_) -> (p_477793_, p_477794_, p_477795_) -> {
                        GlobalPos globalpos = p_258312_.get(p_258304_);
                        if (!globalpos.pos().closerToCenterThan(p_477794_.position(), 2.0) && !p_477794_.assignProfessionWhenSpawned()) {
                            return false;
                        } else {
                            p_258304_.erase();
                            p_258305_.set(globalpos);
                            p_477793_.broadcastEntityEvent(p_477794_, (byte)14);
                            if (!p_477794_.getVillagerData().profession().is(VillagerProfession.NONE)) {
                                return true;
                            } else {
                                MinecraftServer minecraftserver = p_477793_.getServer();
                                Optional.ofNullable(minecraftserver.getLevel(globalpos.dimension()))
                                    .flatMap(p_22467_ -> p_22467_.getPoiManager().getType(globalpos.pos()))
                                    .flatMap(
                                        p_396688_ -> BuiltInRegistries.VILLAGER_PROFESSION
                                            .listElements()
                                            .filter(p_477797_ -> p_477797_.value().heldJobSite().test((Holder<PoiType>)p_396688_))
                                            .findFirst()
                                    )
                                    .ifPresent(p_477800_ -> {
                                        p_477794_.setVillagerData(p_477794_.getVillagerData().withProfession(p_477800_));
                                        p_477794_.refreshBrain(p_477793_);
                                    });
                                return true;
                            }
                        }
                    }
                )
        );
    }
}
