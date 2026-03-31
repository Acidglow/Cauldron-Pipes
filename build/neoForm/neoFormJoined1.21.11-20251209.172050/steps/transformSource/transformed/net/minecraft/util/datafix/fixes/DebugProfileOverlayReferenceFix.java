package net.minecraft.util.datafix.fixes;

import com.mojang.datafixers.DSL;
import com.mojang.datafixers.DataFix;
import com.mojang.datafixers.TypeRewriteRule;
import com.mojang.datafixers.Typed;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Dynamic;

public class DebugProfileOverlayReferenceFix extends DataFix {
    public DebugProfileOverlayReferenceFix(Schema outputSchema) {
        super(outputSchema, false);
    }

    @Override
    protected TypeRewriteRule makeRule() {
        return this.fixTypeEverywhereTyped(
            "DebugProfileOverlayReferenceFix",
            this.getInputSchema().getType(References.DEBUG_PROFILE),
            p_455756_ -> p_455756_.update(
                DSL.remainderFinder(),
                p_456029_ -> p_456029_.update(
                    "custom",
                    p_455561_ -> p_455561_.updateMapValues(
                        p_455854_ -> p_455854_.mapSecond(p_455931_ -> p_455931_.asString("").equals("inF3") ? p_455931_.createString("inOverlay") : p_455931_)
                    )
                )
            )
        );
    }
}
