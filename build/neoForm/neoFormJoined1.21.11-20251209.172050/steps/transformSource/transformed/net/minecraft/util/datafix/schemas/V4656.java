package net.minecraft.util.datafix.schemas;

import com.mojang.datafixers.schemas.Schema;
import com.mojang.datafixers.types.templates.TypeTemplate;
import java.util.Map;
import java.util.function.Supplier;

public class V4656 extends NamespacedSchema {
    public V4656(int p_461029_, Schema p_461219_) {
        super(p_461029_, p_461219_);
    }

    @Override
    public Map<String, Supplier<TypeTemplate>> registerEntities(Schema outputSchema) {
        Map<String, Supplier<TypeTemplate>> map = super.registerEntities(outputSchema);
        outputSchema.registerSimple(map, "minecraft:camel_husk");
        outputSchema.registerSimple(map, "minecraft:parched");
        return map;
    }
}
