package net.minecraft.util.profiling.metrics;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.WeakHashMap;
import java.util.Map.Entry;
import java.util.stream.Collectors;
import org.jspecify.annotations.Nullable;

public class MetricsRegistry {
    public static final MetricsRegistry INSTANCE = new MetricsRegistry();
    private final WeakHashMap<ProfilerMeasured, Void> measuredInstances = new WeakHashMap<>();

    private MetricsRegistry() {
    }

    public void add(ProfilerMeasured key) {
        this.measuredInstances.put(key, null);
    }

    public List<MetricSampler> getRegisteredSamplers() {
        Map<String, List<MetricSampler>> map = this.measuredInstances
            .keySet()
            .stream()
            .flatMap(p_146079_ -> p_146079_.profiledMetrics().stream())
            .collect(Collectors.groupingBy(MetricSampler::getName));
        return aggregateDuplicates(map);
    }

    private static List<MetricSampler> aggregateDuplicates(Map<String, List<MetricSampler>> samplers) {
        return samplers.entrySet().stream().map(p_146075_ -> {
            String s = p_146075_.getKey();
            List<MetricSampler> list = p_146075_.getValue();
            return (MetricSampler)(list.size() > 1 ? new MetricsRegistry.AggregatedMetricSampler(s, list) : list.get(0));
        }).collect(Collectors.toList());
    }

    static class AggregatedMetricSampler extends MetricSampler {
        private final List<MetricSampler> delegates;

        AggregatedMetricSampler(String name, List<MetricSampler> delegates) {
            super(name, delegates.get(0).getCategory(), () -> averageValueFromDelegates(delegates), () -> beforeTick(delegates), thresholdTest(delegates));
            this.delegates = delegates;
        }

        private static MetricSampler.ThresholdTest thresholdTest(List<MetricSampler> samplers) {
            return p_146091_ -> samplers.stream().anyMatch(p_146086_ -> p_146086_.thresholdTest != null ? p_146086_.thresholdTest.test(p_146091_) : false);
        }

        private static void beforeTick(List<MetricSampler> samplers) {
            for (MetricSampler metricsampler : samplers) {
                metricsampler.onStartTick();
            }
        }

        private static double averageValueFromDelegates(List<MetricSampler> samplers) {
            double d0 = 0.0;

            for (MetricSampler metricsampler : samplers) {
                d0 += metricsampler.getSampler().getAsDouble();
            }

            return d0 / samplers.size();
        }

        @Override
        public boolean equals(@Nullable Object p_146101_) {
            if (this == p_146101_) {
                return true;
            } else if (p_146101_ == null || this.getClass() != p_146101_.getClass()) {
                return false;
            } else if (!super.equals(p_146101_)) {
                return false;
            } else {
                MetricsRegistry.AggregatedMetricSampler metricsregistry$aggregatedmetricsampler = (MetricsRegistry.AggregatedMetricSampler)p_146101_;
                return this.delegates.equals(metricsregistry$aggregatedmetricsampler.delegates);
            }
        }

        @Override
        public int hashCode() {
            return Objects.hash(super.hashCode(), this.delegates);
        }
    }
}
