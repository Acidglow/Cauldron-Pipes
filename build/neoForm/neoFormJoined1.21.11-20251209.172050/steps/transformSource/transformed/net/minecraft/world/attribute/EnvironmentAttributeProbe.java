package net.minecraft.world.attribute;

import it.unimi.dsi.fastutil.objects.Reference2ObjectOpenHashMap;
import java.util.Map;
import java.util.function.Function;
import net.minecraft.core.Holder;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

public class EnvironmentAttributeProbe {
    private final Map<EnvironmentAttribute<?>, EnvironmentAttributeProbe.ValueProbe<?>> valueProbes = new Reference2ObjectOpenHashMap<>();
    private final Function<EnvironmentAttribute<?>, EnvironmentAttributeProbe.ValueProbe<?>> valueProbeFactory = p_457807_ -> new EnvironmentAttributeProbe.ValueProbe<>(
        p_457807_
    );
    @Nullable Level level;
    @Nullable Vec3 position;
    final SpatialAttributeInterpolator biomeInterpolator = new SpatialAttributeInterpolator();

    public void reset() {
        this.level = null;
        this.position = null;
        this.biomeInterpolator.clear();
        this.valueProbes.clear();
    }

    public void tick(Level level, Vec3 position) {
        this.level = level;
        this.position = position;
        this.valueProbes.values().removeIf(EnvironmentAttributeProbe.ValueProbe::tick);
        this.biomeInterpolator.clear();
        GaussianSampler.sample(
            position.scale(0.25),
            level.getBiomeManager()::getNoiseBiomeAtQuart,
            (p_458003_, p_458163_) -> this.biomeInterpolator.accumulate(p_458003_, p_458163_.value().getAttributes())
        );
    }

    public <Value> Value getValue(EnvironmentAttribute<Value> attribute, float partialTick) {
        EnvironmentAttributeProbe.ValueProbe<Value> valueprobe = (EnvironmentAttributeProbe.ValueProbe<Value>)this.valueProbes
            .computeIfAbsent(attribute, this.valueProbeFactory);
        return valueprobe.get(attribute, partialTick);
    }

    class ValueProbe<Value> {
        private Value lastValue;
        private @Nullable Value newValue;

        public ValueProbe(EnvironmentAttribute<Value> attribute) {
            Value value = this.getValueFromLevel(attribute);
            this.lastValue = value;
            this.newValue = value;
        }

        private Value getValueFromLevel(EnvironmentAttribute<Value> attribute) {
            return EnvironmentAttributeProbe.this.level != null && EnvironmentAttributeProbe.this.position != null
                ? EnvironmentAttributeProbe.this.level
                    .environmentAttributes()
                    .getValue(attribute, EnvironmentAttributeProbe.this.position, EnvironmentAttributeProbe.this.biomeInterpolator)
                : attribute.defaultValue();
        }

        public boolean tick() {
            if (this.newValue == null) {
                return true;
            } else {
                this.lastValue = this.newValue;
                this.newValue = null;
                return false;
            }
        }

        public Value get(EnvironmentAttribute<Value> attribute, float partialTick) {
            if (this.newValue == null) {
                this.newValue = this.getValueFromLevel(attribute);
            }

            return attribute.type().partialTickLerp().apply(partialTick, this.lastValue, this.newValue);
        }
    }
}
