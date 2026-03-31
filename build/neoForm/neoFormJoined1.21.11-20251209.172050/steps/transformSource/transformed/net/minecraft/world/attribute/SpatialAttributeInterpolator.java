package net.minecraft.world.attribute;

import it.unimi.dsi.fastutil.objects.Reference2DoubleArrayMap;
import it.unimi.dsi.fastutil.objects.Reference2DoubleMaps;
import it.unimi.dsi.fastutil.objects.Reference2DoubleMap.Entry;
import java.util.Objects;

public class SpatialAttributeInterpolator {
    private final Reference2DoubleArrayMap<EnvironmentAttributeMap> weightsBySource = new Reference2DoubleArrayMap<>();

    public void clear() {
        this.weightsBySource.clear();
    }

    public SpatialAttributeInterpolator accumulate(double weight, EnvironmentAttributeMap map) {
        this.weightsBySource.mergeDouble(map, weight, Double::sum);
        return this;
    }

    public <Value> Value applyAttributeLayer(EnvironmentAttribute<Value> attribute, Value p_value) {
        if (this.weightsBySource.isEmpty()) {
            return p_value;
        } else if (this.weightsBySource.size() == 1) {
            EnvironmentAttributeMap environmentattributemap1 = this.weightsBySource.keySet().iterator().next();
            return environmentattributemap1.applyModifier(attribute, p_value);
        } else {
            LerpFunction<Value> lerpfunction = attribute.type().spatialLerp();
            Value value = null;
            double d0 = 0.0;

            for (Entry<EnvironmentAttributeMap> entry : Reference2DoubleMaps.fastIterable(this.weightsBySource)) {
                EnvironmentAttributeMap environmentattributemap = entry.getKey();
                double d1 = entry.getDoubleValue();
                Value value1 = environmentattributemap.applyModifier(attribute, p_value);
                d0 += d1;
                if (value == null) {
                    value = value1;
                } else {
                    float f = (float)(d1 / d0);
                    value = lerpfunction.apply(f, value, value1);
                }
            }

            return Objects.requireNonNull(value);
        }
    }
}
