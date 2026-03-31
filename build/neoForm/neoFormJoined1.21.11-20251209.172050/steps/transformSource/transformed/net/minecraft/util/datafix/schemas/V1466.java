package net.minecraft.util.datafix.schemas;

import com.mojang.datafixers.DSL;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.datafixers.types.templates.TypeTemplate;
import java.util.Map;
import java.util.function.Supplier;
import net.minecraft.util.datafix.fixes.References;

public class V1466 extends NamespacedSchema {
    public V1466(int p_17685_, Schema p_17686_) {
        super(p_17685_, p_17686_);
    }

    @Override
    public void registerTypes(Schema schema, Map<String, Supplier<TypeTemplate>> entityTypes, Map<String, Supplier<TypeTemplate>> blockEntityTypes) {
        super.registerTypes(schema, entityTypes, blockEntityTypes);
        schema.registerType(
            false,
            References.CHUNK,
            () -> DSL.fields(
                "Level",
                DSL.optionalFields(
                    "Entities",
                    DSL.list(References.ENTITY_TREE.in(schema)),
                    "TileEntities",
                    DSL.list(DSL.or(References.BLOCK_ENTITY.in(schema), DSL.remainder())),
                    "TileTicks",
                    DSL.list(DSL.fields("i", References.BLOCK_NAME.in(schema))),
                    "Sections",
                    DSL.list(DSL.optionalFields("Palette", DSL.list(References.BLOCK_STATE.in(schema)))),
                    "Structures",
                    DSL.optionalFields("Starts", DSL.compoundList(References.STRUCTURE_FEATURE.in(schema)))
                )
            )
        );
    }

    @Override
    public Map<String, Supplier<TypeTemplate>> registerBlockEntities(Schema schema) {
        Map<String, Supplier<TypeTemplate>> map = super.registerBlockEntities(schema);
        map.put("DUMMY", DSL::remainder);
        return map;
    }
}
