package net.minecraft.util.datafix.fixes;

import com.mojang.datafixers.DataFix;
import com.mojang.datafixers.TypeRewriteRule;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.serialization.Dynamic;

public class WorldBorderWarningTimeFix extends DataFix {
    public WorldBorderWarningTimeFix(Schema outputSchema) {
        super(outputSchema, false);
    }

    @Override
    protected TypeRewriteRule makeRule() {
        return this.writeFixAndRead(
            "WorldBorderWarningTimeFix",
            this.getInputSchema().getType(References.SAVED_DATA_WORLD_BORDER),
            this.getOutputSchema().getType(References.SAVED_DATA_WORLD_BORDER),
            p_460863_ -> p_460863_.update("data", p_460823_ -> p_460823_.update("warning_time", p_461076_ -> p_460823_.createInt(p_461076_.asInt(15) * 20)))
        );
    }
}
