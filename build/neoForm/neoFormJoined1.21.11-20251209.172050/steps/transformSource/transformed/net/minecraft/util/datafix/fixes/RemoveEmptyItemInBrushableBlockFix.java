package net.minecraft.util.datafix.fixes;

import com.mojang.datafixers.schemas.Schema;
import com.mojang.serialization.Dynamic;
import java.util.Optional;
import net.minecraft.util.datafix.schemas.NamespacedSchema;

public class RemoveEmptyItemInBrushableBlockFix extends NamedEntityWriteReadFix {
    public RemoveEmptyItemInBrushableBlockFix(Schema outputSchema) {
        super(outputSchema, false, "RemoveEmptyItemInSuspiciousBlockFix", References.BLOCK_ENTITY, "minecraft:brushable_block");
    }

    @Override
    protected <T> Dynamic<T> fix(Dynamic<T> p_342013_) {
        Optional<Dynamic<T>> optional = p_342013_.get("item").result();
        return optional.isPresent() && isEmptyStack(optional.get()) ? p_342013_.remove("item") : p_342013_;
    }

    private static boolean isEmptyStack(Dynamic<?> tag) {
        String s = NamespacedSchema.ensureNamespaced(tag.get("id").asString("minecraft:air"));
        int i = tag.get("count").asInt(0);
        return s.equals("minecraft:air") || i == 0;
    }
}
