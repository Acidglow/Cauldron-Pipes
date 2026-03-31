package net.minecraft.world.entity;

import org.jspecify.annotations.Nullable;

@FunctionalInterface
public interface EntityProcessor {
    EntityProcessor NOP = p_461036_ -> p_461036_;

    @Nullable Entity process(Entity entity);
}
