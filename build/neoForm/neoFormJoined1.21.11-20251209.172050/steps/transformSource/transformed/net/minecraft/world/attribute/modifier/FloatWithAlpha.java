package net.minecraft.world.attribute.modifier;

import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.mojang.serialization.codecs.RecordCodecBuilder.Instance;

public record FloatWithAlpha(float value, float alpha) {
    private static final Codec<FloatWithAlpha> FULL_CODEC = RecordCodecBuilder.create(
        p_458124_ -> p_458124_.group(
                Codec.FLOAT.fieldOf("value").forGetter(FloatWithAlpha::value),
                Codec.floatRange(0.0F, 1.0F).optionalFieldOf("alpha", 1.0F).forGetter(FloatWithAlpha::alpha)
            )
            .apply(p_458124_, FloatWithAlpha::new)
    );
    public static final Codec<FloatWithAlpha> CODEC = Codec.either(Codec.FLOAT, FULL_CODEC)
        .xmap(
            p_457817_ -> p_457817_.map(FloatWithAlpha::new, p_457749_ -> (FloatWithAlpha)p_457749_),
            p_458045_ -> p_458045_.alpha() == 1.0F ? Either.left(p_458045_.value()) : Either.right(p_458045_)
        );

    public FloatWithAlpha(float p_457954_) {
        this(p_457954_, 1.0F);
    }
}
