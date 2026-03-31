package net.minecraft.world.attribute.modifier;

import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.mojang.serialization.codecs.RecordCodecBuilder.Instance;
import net.minecraft.util.ARGB;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.util.Mth;
import net.minecraft.world.attribute.EnvironmentAttribute;
import net.minecraft.world.attribute.LerpFunction;

public interface ColorModifier<Argument> extends AttributeModifier<Integer, Argument> {
    ColorModifier<Integer> ALPHA_BLEND = new ColorModifier<Integer>() {
        public Integer apply(Integer p_458214_, Integer p_457973_) {
            return ARGB.alphaBlend(p_458214_, p_457973_);
        }

        @Override
        public Codec<Integer> argumentCodec(EnvironmentAttribute<Integer> p_457931_) {
            return ExtraCodecs.STRING_ARGB_COLOR;
        }

        @Override
        public LerpFunction<Integer> argumentKeyframeLerp(EnvironmentAttribute<Integer> p_469537_) {
            return LerpFunction.ofColor();
        }
    };
    ColorModifier<Integer> ADD = (RgbModifier) ARGB::addRgb;
    ColorModifier<Integer> SUBTRACT = (RgbModifier) ARGB::subtractRgb;
    ColorModifier<Integer> MULTIPLY_RGB = (RgbModifier) ARGB::multiply;
    ColorModifier<Integer> MULTIPLY_ARGB = (ArgbModifier) ARGB::multiply;
    ColorModifier<ColorModifier.BlendToGray> BLEND_TO_GRAY = new ColorModifier<ColorModifier.BlendToGray>() {
        public Integer apply(Integer p_467013_, ColorModifier.BlendToGray p_468333_) {
            int i = ARGB.scaleRGB(ARGB.greyscale(p_467013_), p_468333_.brightness);
            return ARGB.srgbLerp(p_468333_.factor, p_467013_, i);
        }

        @Override
        public Codec<ColorModifier.BlendToGray> argumentCodec(EnvironmentAttribute<Integer> p_468324_) {
            return ColorModifier.BlendToGray.CODEC;
        }

        @Override
        public LerpFunction<ColorModifier.BlendToGray> argumentKeyframeLerp(EnvironmentAttribute<Integer> p_467711_) {
            return (p_467770_, p_467674_, p_467136_) -> new ColorModifier.BlendToGray(
                Mth.lerp(p_467770_, p_467674_.brightness, p_467136_.brightness), Mth.lerp(p_467770_, p_467674_.factor, p_467136_.factor)
            );
        }
    };

    @FunctionalInterface
    public interface ArgbModifier extends ColorModifier<Integer> {
        @Override
        default Codec<Integer> argumentCodec(EnvironmentAttribute<Integer> p_461215_) {
            return Codec.either(ExtraCodecs.STRING_ARGB_COLOR, ExtraCodecs.RGB_COLOR_CODEC)
                .xmap(Either::unwrap, p_461094_ -> ARGB.alpha(p_461094_) == 255 ? Either.right(p_461094_) : Either.left(p_461094_));
        }

        @Override
        default LerpFunction<Integer> argumentKeyframeLerp(EnvironmentAttribute<Integer> p_468908_) {
            return LerpFunction.ofColor();
        }
    }

    public record BlendToGray(float brightness, float factor) {
        public static final Codec<ColorModifier.BlendToGray> CODEC = RecordCodecBuilder.create(
            p_466874_ -> p_466874_.group(
                    Codec.floatRange(0.0F, 1.0F).fieldOf("brightness").forGetter(ColorModifier.BlendToGray::brightness),
                    Codec.floatRange(0.0F, 1.0F).fieldOf("factor").forGetter(ColorModifier.BlendToGray::factor)
                )
                .apply(p_466874_, ColorModifier.BlendToGray::new)
        );
    }

    @FunctionalInterface
    public interface RgbModifier extends ColorModifier<Integer> {
        @Override
        default Codec<Integer> argumentCodec(EnvironmentAttribute<Integer> p_457709_) {
            return ExtraCodecs.STRING_RGB_COLOR;
        }

        @Override
        default LerpFunction<Integer> argumentKeyframeLerp(EnvironmentAttribute<Integer> p_469444_) {
            return LerpFunction.ofColor();
        }
    }
}
