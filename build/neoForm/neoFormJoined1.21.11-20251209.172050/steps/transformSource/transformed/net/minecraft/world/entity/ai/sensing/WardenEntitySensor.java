package net.minecraft.world.entity.ai.sensing;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.monster.warden.Warden;

public class WardenEntitySensor extends NearestLivingEntitySensor<Warden> {
    @Override
    public Set<MemoryModuleType<?>> requires() {
        return ImmutableSet.copyOf(Iterables.concat(super.requires(), List.of(MemoryModuleType.NEAREST_ATTACKABLE)));
    }

    protected void doTick(ServerLevel p_217833_, Warden p_217834_) {
        super.doTick(p_217833_, p_217834_);
        getClosest(p_217834_, p_423273_ -> p_423273_.getType() == EntityType.PLAYER)
            .or(() -> getClosest(p_217834_, p_423272_ -> p_423272_.getType() != EntityType.PLAYER))
            .ifPresentOrElse(
                p_217841_ -> p_217834_.getBrain().setMemory(MemoryModuleType.NEAREST_ATTACKABLE, p_217841_),
                () -> p_217834_.getBrain().eraseMemory(MemoryModuleType.NEAREST_ATTACKABLE)
            );
    }

    private static Optional<LivingEntity> getClosest(Warden warden, Predicate<LivingEntity> predicate) {
        return warden.getBrain()
            .getMemory(MemoryModuleType.NEAREST_LIVING_ENTITIES)
            .stream()
            .flatMap(Collection::stream)
            .filter(warden::canTargetEntity)
            .filter(predicate)
            .findFirst();
    }
}
