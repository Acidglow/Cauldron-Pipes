package net.minecraft.world.attribute.modifier;

import com.mojang.serialization.Codec;
import net.minecraft.world.attribute.EnvironmentAttribute;
import net.minecraft.world.attribute.LerpFunction;

public enum BooleanModifier implements AttributeModifier<Boolean, Boolean> {
    AND,
    NAND,
    OR,
    NOR,
    XOR,
    XNOR;

    public Boolean apply(Boolean p_457532_, Boolean p_457692_) {
        return switch (this) {
            case AND -> p_457692_ && p_457532_;
            case NAND -> !p_457692_ || !p_457532_;
            case OR -> p_457692_ || p_457532_;
            case NOR -> !p_457692_ && !p_457532_;
            case XOR -> p_457692_ ^ p_457532_;
            case XNOR -> p_457692_ == p_457532_;
        };
    }

    @Override
    public Codec<Boolean> argumentCodec(EnvironmentAttribute<Boolean> p_457516_) {
        return Codec.BOOL;
    }

    @Override
    public LerpFunction<Boolean> argumentKeyframeLerp(EnvironmentAttribute<Boolean> p_469095_) {
        return LerpFunction.ofConstant();
    }
}
