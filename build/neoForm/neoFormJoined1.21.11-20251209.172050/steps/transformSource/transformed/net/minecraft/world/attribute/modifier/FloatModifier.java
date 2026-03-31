package net.minecraft.world.attribute.modifier;

import com.mojang.serialization.Codec;
import net.minecraft.util.Mth;
import net.minecraft.world.attribute.EnvironmentAttribute;
import net.minecraft.world.attribute.LerpFunction;

public interface FloatModifier<Argument> extends AttributeModifier<Float, Argument> {
    FloatModifier<FloatWithAlpha> ALPHA_BLEND = new FloatModifier<FloatWithAlpha>() {
        public Float apply(Float p_457802_, FloatWithAlpha p_457622_) {
            return Mth.lerp(p_457622_.alpha(), p_457802_, p_457622_.value());
        }

        @Override
        public Codec<FloatWithAlpha> argumentCodec(EnvironmentAttribute<Float> p_457643_) {
            return FloatWithAlpha.CODEC;
        }

        @Override
        public LerpFunction<FloatWithAlpha> argumentKeyframeLerp(EnvironmentAttribute<Float> p_467236_) {
            return (p_468857_, p_469424_, p_467605_) -> new FloatWithAlpha(
                Mth.lerp(p_468857_, p_469424_.value(), p_467605_.value()), Mth.lerp(p_468857_, p_469424_.alpha(), p_467605_.alpha())
            );
        }
    };
    FloatModifier<Float> ADD = (Simple) Float::sum;
    FloatModifier<Float> SUBTRACT = (FloatModifier.Simple)(p_458036_, p_458119_) -> p_458036_ - p_458119_;
    FloatModifier<Float> MULTIPLY = (FloatModifier.Simple)(p_457586_, p_458256_) -> p_457586_ * p_458256_;
    FloatModifier<Float> MINIMUM = (Simple) Math::min;
    FloatModifier<Float> MAXIMUM = (Simple) Math::max;

    @FunctionalInterface
    public interface Simple extends FloatModifier<Float> {
        @Override
        default Codec<Float> argumentCodec(EnvironmentAttribute<Float> p_457826_) {
            return Codec.FLOAT;
        }

        @Override
        default LerpFunction<Float> argumentKeyframeLerp(EnvironmentAttribute<Float> p_469144_) {
            return LerpFunction.ofFloat();
        }
    }
}
