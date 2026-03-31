package net.minecraft.world.timeline;

import java.util.Optional;
import java.util.function.LongSupplier;
import net.minecraft.util.KeyframeTrack;
import net.minecraft.util.KeyframeTrackSampler;
import net.minecraft.world.attribute.EnvironmentAttributeLayer;
import net.minecraft.world.attribute.LerpFunction;
import net.minecraft.world.attribute.modifier.AttributeModifier;
import org.jspecify.annotations.Nullable;

public class AttributeTrackSampler<Value, Argument> implements EnvironmentAttributeLayer.TimeBased<Value> {
    private final AttributeModifier<Value, Argument> modifier;
    private final KeyframeTrackSampler<Argument> argumentSampler;
    private final LongSupplier dayTimeGetter;
    private int cachedTickId;
    private @Nullable Argument cachedArgument;

    public AttributeTrackSampler(
        Optional<Integer> periodTicks,
        AttributeModifier<Value, Argument> modifier,
        KeyframeTrack<Argument> track,
        LerpFunction<Argument> lerp,
        LongSupplier dayTimeGetter
    ) {
        this.modifier = modifier;
        this.dayTimeGetter = dayTimeGetter;
        this.argumentSampler = track.bakeSampler(periodTicks, lerp);
    }

    @Override
    public Value applyTimeBased(Value p_466881_, int p_470002_) {
        if (this.cachedArgument == null || p_470002_ != this.cachedTickId) {
            this.cachedTickId = p_470002_;
            this.cachedArgument = this.argumentSampler.sample(this.dayTimeGetter.getAsLong());
        }

        return this.modifier.apply(p_466881_, this.cachedArgument);
    }
}
