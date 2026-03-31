package net.minecraft.world.entity.ai.sensing;

import com.google.common.collect.ImmutableSet;
import java.util.Optional;
import java.util.Set;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.NearestVisibleLivingEntities;

public abstract class NearestVisibleLivingEntitySensor extends Sensor<LivingEntity> {
    protected abstract boolean isMatchingEntity(ServerLevel level, LivingEntity entity, LivingEntity target);

    protected abstract MemoryModuleType<LivingEntity> getMemory();

    @Override
    public Set<MemoryModuleType<?>> requires() {
        return ImmutableSet.of(this.getMemory());
    }

    @Override
    protected void doTick(ServerLevel p_148288_, LivingEntity p_148289_) {
        p_148289_.getBrain().setMemory(this.getMemory(), this.getNearestEntity(p_148288_, p_148289_));
    }

    private Optional<LivingEntity> getNearestEntity(ServerLevel level, LivingEntity entity) {
        return this.getVisibleEntities(entity)
            .flatMap(p_375751_ -> p_375751_.findClosest(p_375748_ -> this.isMatchingEntity(level, entity, p_375748_)));
    }

    protected Optional<NearestVisibleLivingEntities> getVisibleEntities(LivingEntity entity) {
        return entity.getBrain().getMemory(MemoryModuleType.NEAREST_VISIBLE_LIVING_ENTITIES);
    }
}
