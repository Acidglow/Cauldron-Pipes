package net.minecraft.world.attribute;

import com.google.common.annotations.VisibleForTesting;
import it.unimi.dsi.fastutil.objects.Reference2ObjectOpenHashMap;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.LongSupplier;
import java.util.stream.Stream;
import net.minecraft.SharedConstants;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeManager;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.timeline.Timeline;
import org.jspecify.annotations.Nullable;

public class EnvironmentAttributeSystem implements EnvironmentAttributeReader {
    private final Map<EnvironmentAttribute<?>, EnvironmentAttributeSystem.ValueSampler<?>> attributeSamplers = new Reference2ObjectOpenHashMap<>();

    EnvironmentAttributeSystem(Map<EnvironmentAttribute<?>, List<EnvironmentAttributeLayer<?>>> attributes) {
        attributes.forEach(
            (p_466523_, p_466524_) -> this.attributeSamplers
                .put(
                    (EnvironmentAttribute<?>)p_466523_,
                    this.bakeLayerSampler((EnvironmentAttribute<?>)p_466523_, (List<? extends EnvironmentAttributeLayer<?>>)p_466524_)
                )
        );
    }

    private <Value> EnvironmentAttributeSystem.ValueSampler<Value> bakeLayerSampler(
        EnvironmentAttribute<Value> attribute, List<? extends EnvironmentAttributeLayer<?>> layers
    ) {
        List<EnvironmentAttributeLayer<Value>> list = new ArrayList<>((Collection<? extends EnvironmentAttributeLayer<Value>>)layers);
        Value value = attribute.defaultValue();

        while (!list.isEmpty()) {
            if (!(list.getFirst() instanceof EnvironmentAttributeLayer.Constant<Value> constant)) {
                break;
            }

            value = constant.applyConstant(value);
            list.removeFirst();
        }

        boolean flag = list.stream().anyMatch(p_466522_ -> p_466522_ instanceof EnvironmentAttributeLayer.Positional);
        return new EnvironmentAttributeSystem.ValueSampler<>(attribute, value, List.copyOf(list), flag);
    }

    public static EnvironmentAttributeSystem.Builder builder() {
        return new EnvironmentAttributeSystem.Builder();
    }

    static void addDefaultLayers(EnvironmentAttributeSystem.Builder builder, Level level) {
        RegistryAccess registryaccess = level.registryAccess();
        BiomeManager biomemanager = level.getBiomeManager();
        LongSupplier longsupplier = level::getDayTime;
        addDimensionLayer(builder, level.dimensionType());
        addBiomeLayer(builder, registryaccess.lookupOrThrow(Registries.BIOME), biomemanager);
        level.dimensionType().timelines().forEach(p_466516_ -> builder.addTimelineLayer((Holder<Timeline>)p_466516_, longsupplier));
        if (level.canHaveWeather()) {
            WeatherAttributes.addBuiltinLayers(builder, WeatherAttributes.WeatherAccess.from(level));
        }
    }

    private static void addDimensionLayer(EnvironmentAttributeSystem.Builder builder, DimensionType dimension) {
        builder.addConstantLayer(dimension.attributes());
    }

    private static void addBiomeLayer(EnvironmentAttributeSystem.Builder builder, HolderLookup<Biome> biome, BiomeManager biomeManager) {
        Stream<EnvironmentAttribute<?>> stream = biome.listElements().flatMap(p_457819_ -> p_457819_.value().getAttributes().keySet().stream()).distinct();
        stream.forEach(p_466513_ -> addBiomeLayerForAttribute(builder, (EnvironmentAttribute<?>)p_466513_, biomeManager));
    }

    private static <Value> void addBiomeLayerForAttribute(
        EnvironmentAttributeSystem.Builder builder, EnvironmentAttribute<Value> attribute, BiomeManager biomeManager
    ) {
        builder.addPositionalLayer(attribute, (p_466519_, p_466520_, p_466521_) -> {
            if (p_466521_ != null && attribute.isSpatiallyInterpolated()) {
                return p_466521_.applyAttributeLayer(attribute, p_466519_);
            } else {
                Holder<Biome> holder = biomeManager.getNoiseBiomeAtPosition(p_466520_.x, p_466520_.y, p_466520_.z);
                return holder.value().getAttributes().applyModifier(attribute, p_466519_);
            }
        });
    }

    public void invalidateTickCache() {
        this.attributeSamplers.values().forEach(EnvironmentAttributeSystem.ValueSampler::invalidateTickCache);
    }

    private <Value> EnvironmentAttributeSystem.@Nullable ValueSampler<Value> getValueSampler(EnvironmentAttribute<Value> attribute) {
        return (EnvironmentAttributeSystem.ValueSampler<Value>)this.attributeSamplers.get(attribute);
    }

    @Override
    public <Value> Value getDimensionValue(EnvironmentAttribute<Value> p_457805_) {
        if (SharedConstants.IS_RUNNING_IN_IDE && p_457805_.isPositional()) {
            throw new IllegalStateException("Position must always be provided for positional attribute " + p_457805_);
        } else {
            EnvironmentAttributeSystem.ValueSampler<Value> valuesampler = this.getValueSampler(p_457805_);
            return valuesampler == null ? p_457805_.defaultValue() : valuesampler.getDimensionValue();
        }
    }

    @Override
    public <Value> Value getValue(EnvironmentAttribute<Value> p_458091_, Vec3 p_458226_, @Nullable SpatialAttributeInterpolator p_458292_) {
        EnvironmentAttributeSystem.ValueSampler<Value> valuesampler = this.getValueSampler(p_458091_);
        return valuesampler == null ? p_458091_.defaultValue() : valuesampler.getValue(p_458226_, p_458292_);
    }

    @VisibleForTesting
    <Value> Value getConstantBaseValue(EnvironmentAttribute<Value> attribute) {
        EnvironmentAttributeSystem.ValueSampler<Value> valuesampler = this.getValueSampler(attribute);
        return valuesampler != null ? valuesampler.baseValue : attribute.defaultValue();
    }

    @VisibleForTesting
    boolean isAffectedByPosition(EnvironmentAttribute<?> attribute) {
        EnvironmentAttributeSystem.ValueSampler<?> valuesampler = this.getValueSampler(attribute);
        return valuesampler != null && valuesampler.isAffectedByPosition;
    }

    public static class Builder {
        private final Map<EnvironmentAttribute<?>, List<EnvironmentAttributeLayer<?>>> layersByAttribute = new HashMap<>();

        Builder() {
        }

        public EnvironmentAttributeSystem.Builder addDefaultLayers(Level level) {
            EnvironmentAttributeSystem.addDefaultLayers(this, level);
            return this;
        }

        public EnvironmentAttributeSystem.Builder addConstantLayer(EnvironmentAttributeMap map) {
            for (EnvironmentAttribute<?> environmentattribute : map.keySet()) {
                this.addConstantEntry(environmentattribute, map);
            }

            return this;
        }

        private <Value> EnvironmentAttributeSystem.Builder addConstantEntry(EnvironmentAttribute<Value> attribute, EnvironmentAttributeMap map) {
            EnvironmentAttributeMap.Entry<Value, ?> entry = map.get(attribute);
            if (entry == null) {
                throw new IllegalArgumentException("Missing attribute " + attribute);
            } else {
                return this.addConstantLayer(attribute, entry::applyModifier);
            }
        }

        public <Value> EnvironmentAttributeSystem.Builder addConstantLayer(
            EnvironmentAttribute<Value> attribute, EnvironmentAttributeLayer.Constant<Value> layer
        ) {
            return this.addLayer(attribute, layer);
        }

        public <Value> EnvironmentAttributeSystem.Builder addTimeBasedLayer(
            EnvironmentAttribute<Value> attribute, EnvironmentAttributeLayer.TimeBased<Value> layer
        ) {
            return this.addLayer(attribute, layer);
        }

        public <Value> EnvironmentAttributeSystem.Builder addPositionalLayer(
            EnvironmentAttribute<Value> attribute, EnvironmentAttributeLayer.Positional<Value> layer
        ) {
            return this.addLayer(attribute, layer);
        }

        private <Value> EnvironmentAttributeSystem.Builder addLayer(EnvironmentAttribute<Value> attribute, EnvironmentAttributeLayer<Value> layer) {
            this.layersByAttribute.computeIfAbsent(attribute, p_467515_ -> new ArrayList<>()).add(layer);
            return this;
        }

        public EnvironmentAttributeSystem.Builder addTimelineLayer(Holder<Timeline> timeline, LongSupplier dayTimeGetter) {
            for (EnvironmentAttribute<?> environmentattribute : timeline.value().attributes()) {
                this.addTimelineLayerForAttribute(timeline, environmentattribute, dayTimeGetter);
            }

            return this;
        }

        private <Value> void addTimelineLayerForAttribute(Holder<Timeline> timeline, EnvironmentAttribute<Value> attribute, LongSupplier dayTimeGetter) {
            this.addTimeBasedLayer(attribute, timeline.value().createTrackSampler(attribute, dayTimeGetter));
        }

        public EnvironmentAttributeSystem build() {
            return new EnvironmentAttributeSystem(this.layersByAttribute);
        }
    }

    static class ValueSampler<Value> {
        private final EnvironmentAttribute<Value> attribute;
        final Value baseValue;
        private final List<EnvironmentAttributeLayer<Value>> layers;
        final boolean isAffectedByPosition;
        private @Nullable Value cachedTickValue;
        private int cacheTickId;

        ValueSampler(EnvironmentAttribute<Value> attribute, Value baseValue, List<EnvironmentAttributeLayer<Value>> layers, boolean isAffectedByPosition) {
            this.attribute = attribute;
            this.baseValue = baseValue;
            this.layers = layers;
            this.isAffectedByPosition = isAffectedByPosition;
        }

        public void invalidateTickCache() {
            this.cachedTickValue = null;
            this.cacheTickId++;
        }

        public Value getDimensionValue() {
            if (this.cachedTickValue != null) {
                return this.cachedTickValue;
            } else {
                Value value = this.computeValueNotPositional();
                this.cachedTickValue = value;
                return value;
            }
        }

        public Value getValue(Vec3 pos, @Nullable SpatialAttributeInterpolator interpolator) {
            return !this.isAffectedByPosition ? this.getDimensionValue() : this.computeValuePositional(pos, interpolator);
        }

        private Value computeValuePositional(Vec3 pos, @Nullable SpatialAttributeInterpolator interpolator) {
            Value value = this.baseValue;

            for (EnvironmentAttributeLayer<Value> environmentattributelayer : this.layers) {
                value = (Value)(switch (environmentattributelayer) {
                    case EnvironmentAttributeLayer.Constant<Value> constant -> (Object)constant.applyConstant(value);
                    case EnvironmentAttributeLayer.TimeBased<Value> timebased -> (Object)timebased.applyTimeBased(value, this.cacheTickId);
                    case EnvironmentAttributeLayer.Positional<Value> positional -> (Object)positional.applyPositional(
                        value, Objects.requireNonNull(pos), interpolator
                    );
                    default -> throw new MatchException(null, null);
                });
            }

            return this.attribute.sanitizeValue(value);
        }

        private Value computeValueNotPositional() {
            Value value = this.baseValue;

            for (EnvironmentAttributeLayer<Value> environmentattributelayer : this.layers) {
                value = (Value)(switch (environmentattributelayer) {
                    case EnvironmentAttributeLayer.Constant<Value> constant -> (Object)constant.applyConstant(value);
                    case EnvironmentAttributeLayer.TimeBased<Value> timebased -> (Object)timebased.applyTimeBased(value, this.cacheTickId);
                    case EnvironmentAttributeLayer.Positional<Value> positional -> (Object)value;
                    default -> throw new MatchException(null, null);
                });
            }

            return this.attribute.sanitizeValue(value);
        }
    }
}
