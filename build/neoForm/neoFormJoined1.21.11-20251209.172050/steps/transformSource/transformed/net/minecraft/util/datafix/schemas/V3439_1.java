package net.minecraft.util.datafix.schemas;

import com.mojang.datafixers.schemas.Schema;
import com.mojang.datafixers.types.templates.TypeTemplate;
import java.util.Map;
import java.util.function.Supplier;

public class V3439_1 extends NamespacedSchema {
    public V3439_1(int p_415778_, Schema p_416748_) {
        super(p_415778_, p_416748_);
    }

    @Override
    public Map<String, Supplier<TypeTemplate>> registerBlockEntities(Schema schema) {
        Map<String, Supplier<TypeTemplate>> map = super.registerBlockEntities(schema);
        this.register(map, "minecraft:hanging_sign", () -> V3439.sign(schema));
        return map;
    }
}
