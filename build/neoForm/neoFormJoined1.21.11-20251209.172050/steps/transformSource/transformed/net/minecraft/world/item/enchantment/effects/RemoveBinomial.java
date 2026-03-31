package net.minecraft.world.item.enchantment.effects;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.mojang.serialization.codecs.RecordCodecBuilder.Instance;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.enchantment.LevelBasedValue;

public record RemoveBinomial(LevelBasedValue chance) implements EnchantmentValueEffect {
    public static final MapCodec<RemoveBinomial> CODEC = RecordCodecBuilder.mapCodec(
        p_344917_ -> p_344917_.group(LevelBasedValue.CODEC.fieldOf("chance").forGetter(RemoveBinomial::chance)).apply(p_344917_, RemoveBinomial::new)
    );

    @Override
    public float process(int p_345642_, RandomSource p_345903_, float p_345714_) {
        float f = this.chance.calculate(p_345642_);
        int i = 0;
        if (!(p_345714_ <= 128.0F) && !(p_345714_ * f < 20.0F) && !(p_345714_ * (1.0F - f) < 20.0F)) {
            double d1 = Math.floor(p_345714_ * f);
            double d0 = Math.sqrt(p_345714_ * f * (1.0F - f));
            i = (int)Math.round(d1 + p_345903_.nextGaussian() * d0);
            i = Math.clamp((long)i, 0, (int)p_345714_);
        } else {
            for (int j = 0; j < p_345714_; j++) {
                if (p_345903_.nextFloat() < f) {
                    i++;
                }
            }
        }

        return p_345714_ - i;
    }

    @Override
    public MapCodec<RemoveBinomial> codec() {
        return CODEC;
    }
}
