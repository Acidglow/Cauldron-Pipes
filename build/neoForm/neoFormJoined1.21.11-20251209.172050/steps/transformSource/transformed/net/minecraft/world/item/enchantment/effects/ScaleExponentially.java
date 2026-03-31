package net.minecraft.world.item.enchantment.effects;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.mojang.serialization.codecs.RecordCodecBuilder.Instance;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.enchantment.LevelBasedValue;

public record ScaleExponentially(LevelBasedValue base, LevelBasedValue exponent) implements EnchantmentValueEffect {
    public static final MapCodec<ScaleExponentially> CODEC = RecordCodecBuilder.mapCodec(
        p_455127_ -> p_455127_.group(
                LevelBasedValue.CODEC.fieldOf("base").forGetter(ScaleExponentially::base),
                LevelBasedValue.CODEC.fieldOf("exponent").forGetter(ScaleExponentially::exponent)
            )
            .apply(p_455127_, ScaleExponentially::new)
    );

    @Override
    public float process(int p_455580_, RandomSource p_455771_, float p_454723_) {
        return (float)(p_454723_ * Math.pow(this.base.calculate(p_455580_), this.exponent.calculate(p_455580_)));
    }

    @Override
    public MapCodec<ScaleExponentially> codec() {
        return CODEC;
    }
}
