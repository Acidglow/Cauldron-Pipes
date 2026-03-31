package net.minecraft.util.datafix.fixes;

import com.mojang.datafixers.DSL;
import com.mojang.datafixers.DataFix;
import com.mojang.datafixers.TypeRewriteRule;
import com.mojang.datafixers.Typed;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.serialization.Dynamic;

public class OptionsMusicToastFix extends DataFix {
    public OptionsMusicToastFix(Schema outputSchema, boolean changesType) {
        super(outputSchema, changesType);
    }

    @Override
    public TypeRewriteRule makeRule() {
        return this.fixTypeEverywhereTyped(
            "OptionsMusicToastFix",
            this.getInputSchema().getType(References.OPTIONS),
            p_470734_ -> p_470734_.update(
                DSL.remainderFinder(),
                p_470597_ -> p_470597_.renameAndFixField(
                    "showNowPlayingToast",
                    "musicToast",
                    p_470616_ -> p_470597_.createString(p_470616_.asString("false").equals("false") ? "never" : "pause_and_toast")
                )
            )
        );
    }
}
