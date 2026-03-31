package net.minecraft.util.profiling;

import java.util.function.Supplier;
import net.minecraft.util.profiling.metrics.MetricCategory;

public interface ProfilerFiller {
    String ROOT = "root";

    void startTick();

    void endTick();

    /**
     * Start section
     */
    void push(String name);

    void push(Supplier<String> nameSupplier);

    void pop();

    void popPush(String name);

    void popPush(Supplier<String> nameSupplier);

    default void addZoneText(String text) {
    }

    default void addZoneValue(long value) {
    }

    default void setZoneColor(int color) {
    }

    default Zone zone(String name) {
        this.push(name);
        return new Zone(this);
    }

    default Zone zone(Supplier<String> name) {
        this.push(name);
        return new Zone(this);
    }

    void markForCharting(MetricCategory category);

    default void incrementCounter(String entryId) {
        this.incrementCounter(entryId, 1);
    }

    void incrementCounter(String counterName, int increment);

    default void incrementCounter(Supplier<String> entryIdSupplier) {
        this.incrementCounter(entryIdSupplier, 1);
    }

    void incrementCounter(Supplier<String> counterNameSupplier, int increment);

    static ProfilerFiller combine(ProfilerFiller first, ProfilerFiller second) {
        if (first == InactiveProfiler.INSTANCE) {
            return second;
        } else {
            return (ProfilerFiller)(second == InactiveProfiler.INSTANCE ? first : new ProfilerFiller.CombinedProfileFiller(first, second));
        }
    }

    public static class CombinedProfileFiller implements ProfilerFiller {
        private final ProfilerFiller first;
        private final ProfilerFiller second;

        public CombinedProfileFiller(ProfilerFiller first, ProfilerFiller second) {
            this.first = first;
            this.second = second;
        }

        @Override
        public void startTick() {
            this.first.startTick();
            this.second.startTick();
        }

        @Override
        public void endTick() {
            this.first.endTick();
            this.second.endTick();
        }

        @Override
        public void push(String p_372926_) {
            this.first.push(p_372926_);
            this.second.push(p_372926_);
        }

        @Override
        public void push(Supplier<String> p_372910_) {
            this.first.push(p_372910_);
            this.second.push(p_372910_);
        }

        @Override
        public void markForCharting(MetricCategory p_373064_) {
            this.first.markForCharting(p_373064_);
            this.second.markForCharting(p_373064_);
        }

        @Override
        public void pop() {
            this.first.pop();
            this.second.pop();
        }

        @Override
        public void popPush(String p_372955_) {
            this.first.popPush(p_372955_);
            this.second.popPush(p_372955_);
        }

        @Override
        public void popPush(Supplier<String> p_372989_) {
            this.first.popPush(p_372989_);
            this.second.popPush(p_372989_);
        }

        @Override
        public void incrementCounter(String p_372854_, int p_372995_) {
            this.first.incrementCounter(p_372854_, p_372995_);
            this.second.incrementCounter(p_372854_, p_372995_);
        }

        @Override
        public void incrementCounter(Supplier<String> p_373119_, int p_372889_) {
            this.first.incrementCounter(p_373119_, p_372889_);
            this.second.incrementCounter(p_373119_, p_372889_);
        }

        @Override
        public void addZoneText(String p_373071_) {
            this.first.addZoneText(p_373071_);
            this.second.addZoneText(p_373071_);
        }

        @Override
        public void addZoneValue(long p_373118_) {
            this.first.addZoneValue(p_373118_);
            this.second.addZoneValue(p_373118_);
        }

        @Override
        public void setZoneColor(int p_373025_) {
            this.first.setZoneColor(p_373025_);
            this.second.setZoneColor(p_373025_);
        }
    }
}
