package net.minecraft.advancements.criterion;

import net.minecraft.core.component.DataComponentGetter;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.component.predicates.DataComponentPredicate;

public interface SingleComponentItemPredicate<T> extends DataComponentPredicate {
    @Override
    default boolean matches(DataComponentGetter p_466919_) {
        T t = p_466919_.get(this.componentType());
        return t != null && this.matches(t);
    }

    DataComponentType<T> componentType();

    boolean matches(T value);
}
