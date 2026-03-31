package net.minecraft.util.datafix.fixes;

import com.mojang.datafixers.DSL;
import com.mojang.datafixers.DataFix;
import com.mojang.datafixers.TypeRewriteRule;
import com.mojang.datafixers.Typed;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.serialization.Dynamic;

public class OptionsSetGraphicsPresetToCustomFix extends DataFix {
    public OptionsSetGraphicsPresetToCustomFix(Schema ouputSchema) {
        super(ouputSchema, true);
    }

    @Override
    public TypeRewriteRule makeRule() {
        return this.fixTypeEverywhereTyped(
            "graphicsPreset set to \"custom\"",
            this.getInputSchema().getType(References.OPTIONS),
            p_455781_ -> p_455781_.update(DSL.remainderFinder(), p_455558_ -> p_455558_.set("graphicsPreset", p_455558_.createString("custom")))
        );
    }
}
