package net.minecraft.util;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.mojang.serialization.codecs.RecordCodecBuilder.Instance;

public record Keyframe<T>(int ticks, T value) {
    public static <T> Codec<Keyframe<T>> codec(Codec<T> valueCodec) {
        return RecordCodecBuilder.create(
            p_467463_ -> p_467463_.group(
                    ExtraCodecs.NON_NEGATIVE_INT.fieldOf("ticks").forGetter(Keyframe::ticks), valueCodec.fieldOf("value").forGetter(Keyframe::value)
                )
                .apply(p_467463_, Keyframe::new)
        );
    }
}
