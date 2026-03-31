package net.minecraft.world.timeline;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.MapCodec;
import java.util.Optional;
import java.util.function.LongSupplier;
import net.minecraft.util.KeyframeTrack;
import net.minecraft.util.Util;
import net.minecraft.world.attribute.EnvironmentAttribute;
import net.minecraft.world.attribute.modifier.AttributeModifier;

public record AttributeTrack<Value, Argument>(AttributeModifier<Value, Argument> modifier, KeyframeTrack<Argument> argumentTrack) {
    public static <Value> Codec<AttributeTrack<Value, ?>> createCodec(EnvironmentAttribute<Value> attribute) {
        MapCodec<AttributeModifier<Value, ?>> mapcodec = attribute.type().modifierCodec().optionalFieldOf("modifier", AttributeModifier.override());
        return mapcodec.dispatch(
            AttributeTrack::modifier, Util.memoize(p_468589_ -> createCodecWithModifier(attribute, (AttributeModifier<Value, ?>)p_468589_))
        );
    }

    private static <Value, Argument> MapCodec<AttributeTrack<Value, Argument>> createCodecWithModifier(
        EnvironmentAttribute<Value> attribute, AttributeModifier<Value, Argument> modifier
    ) {
        return KeyframeTrack.mapCodec(modifier.argumentCodec(attribute))
            .xmap(p_467722_ -> new AttributeTrack<>(modifier, (KeyframeTrack<Argument>)p_467722_), AttributeTrack::argumentTrack);
    }

    public AttributeTrackSampler<Value, Argument> bakeSampler(EnvironmentAttribute<Value> attribute, Optional<Integer> periodTicks, LongSupplier dayTimeGetter) {
        return new AttributeTrackSampler<>(periodTicks, this.modifier, this.argumentTrack, this.modifier.argumentKeyframeLerp(attribute), dayTimeGetter);
    }

    public static DataResult<AttributeTrack<?, ?>> validatePeriod(AttributeTrack<?, ?> track, int max) {
        return KeyframeTrack.validatePeriod(track.argumentTrack(), max).map(p_469865_ -> track);
    }
}
