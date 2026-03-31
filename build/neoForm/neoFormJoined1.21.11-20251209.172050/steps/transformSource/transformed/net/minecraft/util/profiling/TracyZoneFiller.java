package net.minecraft.util.profiling;

import com.mojang.jtracy.Plot;
import com.mojang.jtracy.TracyClient;
import com.mojang.logging.LogUtils;
import java.lang.StackWalker.Option;
import java.lang.StackWalker.StackFrame;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Stream;
import net.minecraft.SharedConstants;
import net.minecraft.util.profiling.metrics.MetricCategory;
import org.slf4j.Logger;

public class TracyZoneFiller implements ProfilerFiller {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final StackWalker STACK_WALKER = StackWalker.getInstance(Set.of(Option.RETAIN_CLASS_REFERENCE), 5);
    private final List<com.mojang.jtracy.Zone> activeZones = new ArrayList<>();
    private final Map<String, TracyZoneFiller.PlotAndValue> plots = new HashMap<>();
    private final String name = Thread.currentThread().getName();

    @Override
    public void startTick() {
    }

    @Override
    public void endTick() {
        for (TracyZoneFiller.PlotAndValue tracyzonefiller$plotandvalue : this.plots.values()) {
            tracyzonefiller$plotandvalue.set(0);
        }
    }

    @Override
    public void push(String p_373085_) {
        String s = "";
        String s1 = "";
        int i = 0;
        if (SharedConstants.IS_RUNNING_IN_IDE) {
            Optional<StackFrame> optional = STACK_WALKER.walk(
                p_372968_ -> p_372968_.filter(
                        p_372812_ -> p_372812_.getDeclaringClass() != TracyZoneFiller.class
                            && p_372812_.getDeclaringClass() != ProfilerFiller.CombinedProfileFiller.class
                    )
                    .findFirst()
            );
            if (optional.isPresent()) {
                StackFrame stackframe = optional.get();
                s = stackframe.getMethodName();
                s1 = stackframe.getFileName();
                i = stackframe.getLineNumber();
            }
        }

        com.mojang.jtracy.Zone zone = TracyClient.beginZone(p_373085_, s, s1, i);
        this.activeZones.add(zone);
    }

    @Override
    public void push(Supplier<String> p_372939_) {
        this.push(p_372939_.get());
    }

    @Override
    public void pop() {
        if (this.activeZones.isEmpty()) {
            LOGGER.error("Tried to pop one too many times! Mismatched push() and pop()?");
        } else {
            com.mojang.jtracy.Zone zone = this.activeZones.removeLast();
            zone.close();
        }
    }

    @Override
    public void popPush(String p_373009_) {
        this.pop();
        this.push(p_373009_);
    }

    @Override
    public void popPush(Supplier<String> p_373088_) {
        this.pop();
        this.push(p_373088_.get());
    }

    @Override
    public void markForCharting(MetricCategory p_373090_) {
    }

    @Override
    public void incrementCounter(String p_372894_, int p_372810_) {
        this.plots.computeIfAbsent(p_372894_, p_372885_ -> new TracyZoneFiller.PlotAndValue(this.name + " " + p_372894_)).add(p_372810_);
    }

    @Override
    public void incrementCounter(Supplier<String> p_372847_, int p_372796_) {
        this.incrementCounter(p_372847_.get(), p_372796_);
    }

    private com.mojang.jtracy.Zone activeZone() {
        return this.activeZones.getLast();
    }

    @Override
    public void addZoneText(String p_372877_) {
        this.activeZone().addText(p_372877_);
    }

    @Override
    public void addZoneValue(long p_373111_) {
        this.activeZone().addValue(p_373111_);
    }

    @Override
    public void setZoneColor(int p_373019_) {
        this.activeZone().setColor(p_373019_);
    }

    static final class PlotAndValue {
        private final Plot plot;
        private int value;

        PlotAndValue(String name) {
            this.plot = TracyClient.createPlot(name);
            this.value = 0;
        }

        void set(int value) {
            this.value = value;
            this.plot.setValue(value);
        }

        void add(int value) {
            this.set(this.value + value);
        }
    }
}
