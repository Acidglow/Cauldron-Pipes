package net.minecraft.util;

import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import java.util.function.Function;

public enum TriState implements StringRepresentable {
    TRUE("true"),
    FALSE("false"),
    DEFAULT("default");

    public static final Codec<TriState> CODEC = Codec.either(Codec.BOOL, StringRepresentable.fromEnum(TriState::values))
        .xmap(p_467682_ -> p_467682_.map(TriState::from, Function.identity()), p_468600_ -> {
            return switch (p_468600_) {
                case TRUE -> Either.left(true);
                case FALSE -> Either.left(false);
                case DEFAULT -> Either.right(p_468600_);
            };
        });
    private final String name;

    private TriState(String name) {
        this.name = name;
    }

    public static TriState from(boolean value) {
        return value ? TRUE : FALSE;
    }

    public boolean toBoolean(boolean defaultValue) {
        return switch (this) {
            case TRUE -> true;
            case FALSE -> false;
            default -> defaultValue;
        };
    }

    @Override
    public String getSerializedName() {
        return this.name;
    }

    // Neo: Helper methods for use in patches
    public boolean isTrue() {
        return this == TRUE;
    }

    public boolean isDefault() {
        return this == DEFAULT;
    }

    public boolean isFalse() {
        return this == FALSE;
    }
}
