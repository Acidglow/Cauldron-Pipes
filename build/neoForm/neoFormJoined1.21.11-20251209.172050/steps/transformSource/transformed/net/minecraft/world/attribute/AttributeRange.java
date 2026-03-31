package net.minecraft.world.attribute;

import com.mojang.serialization.DataResult;
import net.minecraft.util.Mth;

public interface AttributeRange<Value> {
    AttributeRange<Float> UNIT_FLOAT = ofFloat(0.0F, 1.0F);
    AttributeRange<Float> NON_NEGATIVE_FLOAT = ofFloat(0.0F, Float.POSITIVE_INFINITY);

    static <Value> AttributeRange<Value> any() {
        return new AttributeRange<Value>() {
            @Override
            public DataResult<Value> validate(Value p_457845_) {
                return DataResult.success(p_457845_);
            }

            @Override
            public Value sanitize(Value p_457594_) {
                return p_457594_;
            }
        };
    }

    static AttributeRange<Float> ofFloat(final float min, final float max) {
        return new AttributeRange<Float>() {
            public DataResult<Float> validate(Float p_458024_) {
                return p_458024_ >= min && p_458024_ <= max
                    ? DataResult.success(p_458024_)
                    : DataResult.error(() -> p_458024_ + " is not in range [" + min + "; " + max + "]");
            }

            public Float sanitize(Float p_457929_) {
                return p_457929_ >= min && p_457929_ <= max ? p_457929_ : Mth.clamp(p_457929_, min, max);
            }
        };
    }

    DataResult<Value> validate(Value value);

    Value sanitize(Value value);
}
