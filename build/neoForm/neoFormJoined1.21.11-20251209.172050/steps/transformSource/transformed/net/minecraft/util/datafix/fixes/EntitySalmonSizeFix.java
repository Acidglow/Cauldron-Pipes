package net.minecraft.util.datafix.fixes;

import com.mojang.datafixers.DSL;
import com.mojang.datafixers.Typed;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.serialization.Dynamic;

public class EntitySalmonSizeFix extends NamedEntityFix {
    public EntitySalmonSizeFix(Schema outputSchema) {
        super(outputSchema, false, "EntitySalmonSizeFix", References.ENTITY, "minecraft:salmon");
    }

    @Override
    protected Typed<?> fix(Typed<?> p_381775_) {
        return p_381775_.update(DSL.remainderFinder(), p_381776_ -> {
            String s = p_381776_.get("type").asString("medium");
            return s.equals("large") ? p_381776_ : p_381776_.set("type", p_381776_.createString("medium"));
        });
    }
}
