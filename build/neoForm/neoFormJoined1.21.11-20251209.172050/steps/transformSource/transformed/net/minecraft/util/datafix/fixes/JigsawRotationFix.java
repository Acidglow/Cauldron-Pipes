package net.minecraft.util.datafix.fixes;

import com.google.common.collect.ImmutableMap;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.serialization.Dynamic;
import java.util.Map;

public class JigsawRotationFix extends AbstractBlockPropertyFix {
    private static final Map<String, String> RENAMES = ImmutableMap.<String, String>builder()
        .put("down", "down_south")
        .put("up", "up_north")
        .put("north", "north_up")
        .put("south", "south_up")
        .put("west", "west_up")
        .put("east", "east_up")
        .build();

    public JigsawRotationFix(Schema outputSchema) {
        super(outputSchema, "jigsaw_rotation_fix");
    }

    @Override
    protected boolean shouldFix(String p_394312_) {
        return p_394312_.equals("minecraft:jigsaw");
    }

    @Override
    protected <T> Dynamic<T> fixProperties(String p_393714_, Dynamic<T> p_394355_) {
        String s = p_394355_.get("facing").asString("north");
        return p_394355_.remove("facing").set("orientation", p_394355_.createString(RENAMES.getOrDefault(s, s)));
    }
}
