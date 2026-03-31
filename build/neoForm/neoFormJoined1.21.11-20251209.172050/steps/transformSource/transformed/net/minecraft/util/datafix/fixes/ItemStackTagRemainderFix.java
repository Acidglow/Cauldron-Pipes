package net.minecraft.util.datafix.fixes;

import com.mojang.datafixers.DSL;
import com.mojang.datafixers.Typed;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.serialization.Dynamic;
import java.util.function.Predicate;

public abstract class ItemStackTagRemainderFix extends ItemStackTagFix {
    public ItemStackTagRemainderFix(Schema p_394050_, String p_394588_, Predicate<String> p_393643_) {
        super(p_394050_, p_394588_, p_393643_);
    }

    protected abstract <T> Dynamic<T> fixItemStackTag(Dynamic<T> data);

    @Override
    protected final Typed<?> fixItemStackTag(Typed<?> p_393690_) {
        return p_393690_.update(DSL.remainderFinder(), this::fixItemStackTag);
    }
}
