package net.minecraft.world.attribute;

import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

public sealed interface EnvironmentAttributeLayer<Value>
    permits EnvironmentAttributeLayer.Constant,
    EnvironmentAttributeLayer.TimeBased,
    EnvironmentAttributeLayer.Positional {
    @FunctionalInterface
    public non-sealed interface Constant<Value> extends EnvironmentAttributeLayer<Value> {
        Value applyConstant(Value oldValue);
    }

    @FunctionalInterface
    public non-sealed interface Positional<Value> extends EnvironmentAttributeLayer<Value> {
        Value applyPositional(Value oldValue, Vec3 pos, @Nullable SpatialAttributeInterpolator interpolator);
    }

    @FunctionalInterface
    public non-sealed interface TimeBased<Value> extends EnvironmentAttributeLayer<Value> {
        Value applyTimeBased(Value oldValue, int dayTime);
    }
}
