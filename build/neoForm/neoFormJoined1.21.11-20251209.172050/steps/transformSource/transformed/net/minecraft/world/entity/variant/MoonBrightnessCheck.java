package net.minecraft.world.entity.variant;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.mojang.serialization.codecs.RecordCodecBuilder.Instance;
import net.minecraft.advancements.criterion.MinMaxBounds;
import net.minecraft.world.attribute.EnvironmentAttributes;
import net.minecraft.world.level.MoonPhase;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.phys.Vec3;

public record MoonBrightnessCheck(MinMaxBounds.Doubles range) implements SpawnCondition {
    public static final MapCodec<MoonBrightnessCheck> MAP_CODEC = RecordCodecBuilder.mapCodec(
        p_466620_ -> p_466620_.group(MinMaxBounds.Doubles.CODEC.fieldOf("range").forGetter(MoonBrightnessCheck::range))
            .apply(p_466620_, MoonBrightnessCheck::new)
    );

    public boolean test(SpawnContext context) {
        MoonPhase moonphase = context.environmentAttributes().getValue(EnvironmentAttributes.MOON_PHASE, Vec3.atCenterOf(context.pos()));
        float f = DimensionType.MOON_BRIGHTNESS_PER_PHASE[moonphase.index()];
        return this.range.matches(f);
    }

    @Override
    public MapCodec<MoonBrightnessCheck> codec() {
        return MAP_CODEC;
    }
}
