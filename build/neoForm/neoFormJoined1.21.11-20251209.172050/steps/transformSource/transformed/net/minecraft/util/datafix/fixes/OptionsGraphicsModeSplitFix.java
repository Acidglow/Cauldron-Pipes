package net.minecraft.util.datafix.fixes;

import com.mojang.datafixers.DSL;
import com.mojang.datafixers.DataFix;
import com.mojang.datafixers.DataFixUtils;
import com.mojang.datafixers.TypeRewriteRule;
import com.mojang.datafixers.Typed;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.serialization.Dynamic;

public class OptionsGraphicsModeSplitFix extends DataFix {
    private final String newFieldName;
    private final String valueIfFast;
    private final String valueIfFancy;
    private final String valueIfFabulous;

    public OptionsGraphicsModeSplitFix(Schema outputSchema, String newFieldName, String valueIfFast, String valueIfFancy, String valueIfFabulous) {
        super(outputSchema, true);
        this.newFieldName = newFieldName;
        this.valueIfFast = valueIfFast;
        this.valueIfFancy = valueIfFancy;
        this.valueIfFabulous = valueIfFabulous;
    }

    @Override
    public TypeRewriteRule makeRule() {
        return this.fixTypeEverywhereTyped(
            "graphicsMode split to " + this.newFieldName,
            this.getInputSchema().getType(References.OPTIONS),
            p_454935_ -> p_454935_.update(
                DSL.remainderFinder(),
                p_454718_ -> DataFixUtils.orElseGet(
                    p_454718_.get("graphicsMode")
                        .asString()
                        .map(p_454671_ -> p_454718_.set(this.newFieldName, p_454718_.createString(this.getValue(p_454671_))))
                        .result(),
                    () -> p_454718_.set(this.newFieldName, p_454718_.createString(this.valueIfFancy))
                )
            )
        );
    }

    private String getValue(String value) {
        return switch (value) {
            case "2" -> this.valueIfFabulous;
            case "0" -> this.valueIfFast;
            default -> this.valueIfFancy;
        };
    }
}
