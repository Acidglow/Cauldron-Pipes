package net.minecraft.world.attribute;

import net.minecraft.util.ARGB;
import net.minecraft.util.Mth;

public interface LerpFunction<T> {
    static LerpFunction<Float> ofFloat() {
        return Mth::lerp;
    }

    static LerpFunction<Float> ofDegrees(float degrees) {
        return (p_466529_, p_466530_, p_466531_) -> {
            float f = Mth.wrapDegrees(p_466531_ - p_466530_);
            return Math.abs(f) >= degrees ? p_466531_ : p_466530_ + p_466529_ * f;
        };
    }

    static <T> LerpFunction<T> ofConstant() {
        return (p_466525_, p_466526_, p_466527_) -> p_466526_;
    }

    static <T> LerpFunction<T> ofStep(float step) {
        return (p_457536_, p_457660_, p_458126_) -> p_457536_ >= step ? p_458126_ : p_457660_;
    }

    static LerpFunction<Integer> ofColor() {
        return ARGB::srgbLerp;
    }

    T apply(float delta, T min, T max);
}
