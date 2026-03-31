package net.minecraft.util.datafix.fixes;

import com.mojang.datafixers.schemas.Schema;
import com.mojang.serialization.Dynamic;
import java.util.Optional;

public class LodestoneCompassComponentFix extends DataComponentRemainderFix {
    public LodestoneCompassComponentFix(Schema outputSchema) {
        super(outputSchema, "LodestoneCompassComponentFix", "minecraft:lodestone_target", "minecraft:lodestone_tracker");
    }

    @Override
    protected <T> Dynamic<T> fixComponent(Dynamic<T> p_332647_) {
        Optional<Dynamic<T>> optional = p_332647_.get("pos").result();
        Optional<Dynamic<T>> optional1 = p_332647_.get("dimension").result();
        p_332647_ = p_332647_.remove("pos").remove("dimension");
        if (optional.isPresent() && optional1.isPresent()) {
            p_332647_ = p_332647_.set("target", p_332647_.emptyMap().set("pos", optional.get()).set("dimension", optional1.get()));
        }

        return p_332647_;
    }
}
