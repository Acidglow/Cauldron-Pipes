package net.minecraft.client;

import com.google.common.collect.ImmutableList;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.DoubleFunction;
import java.util.function.Function;
import java.util.function.IntFunction;
import java.util.function.IntSupplier;
import java.util.function.Supplier;
import java.util.function.ToDoubleFunction;
import java.util.function.ToIntFunction;
import java.util.stream.IntStream;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractOptionSliderButton;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.components.ResettableOptionWidget;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.util.Util;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

@OnlyIn(Dist.CLIENT)
public final class OptionInstance<T> {
    private static final Logger LOGGER = LogUtils.getLogger();
    public static final OptionInstance.Enum<Boolean> BOOLEAN_VALUES = new OptionInstance.Enum<>(ImmutableList.of(Boolean.TRUE, Boolean.FALSE), Codec.BOOL);
    public static final OptionInstance.CaptionBasedToString<Boolean> BOOLEAN_TO_STRING = (p_231544_, p_231545_) -> p_231545_
        ? CommonComponents.OPTION_ON
        : CommonComponents.OPTION_OFF;
    private final OptionInstance.TooltipSupplier<T> tooltip;
    public final Function<T, Component> toString;
    private final OptionInstance.ValueSet<T> values;
    private final Codec<T> codec;
    private final T initialValue;
    private final Consumer<T> onValueUpdate;
    public final Component caption;
    private T value;

    public static OptionInstance<Boolean> createBoolean(String key, boolean initialValue, Consumer<Boolean> onValueUpdate) {
        return createBoolean(key, noTooltip(), initialValue, onValueUpdate);
    }

    public static OptionInstance<Boolean> createBoolean(String key, boolean initialValue) {
        return createBoolean(key, noTooltip(), initialValue, p_231548_ -> {});
    }

    public static OptionInstance<Boolean> createBoolean(String caption, OptionInstance.TooltipSupplier<Boolean> tooltip, boolean initialValue) {
        return createBoolean(caption, tooltip, initialValue, p_231513_ -> {});
    }

    public static OptionInstance<Boolean> createBoolean(
        String caption, OptionInstance.TooltipSupplier<Boolean> tooltip, boolean initialValue, Consumer<Boolean> onValueUpdate
    ) {
        return createBoolean(caption, tooltip, BOOLEAN_TO_STRING, initialValue, onValueUpdate);
    }

    public static OptionInstance<Boolean> createBoolean(
        String caption,
        OptionInstance.TooltipSupplier<Boolean> tooltip,
        OptionInstance.CaptionBasedToString<Boolean> valueStringifier,
        boolean initialValue,
        Consumer<Boolean> onValueUpdate
    ) {
        return new OptionInstance<>(caption, tooltip, valueStringifier, BOOLEAN_VALUES, initialValue, onValueUpdate);
    }

    public OptionInstance(
        String caption,
        OptionInstance.TooltipSupplier<T> tooltip,
        OptionInstance.CaptionBasedToString<T> valueStringifier,
        OptionInstance.ValueSet<T> values,
        T initialValue,
        Consumer<T> onValueUpdate
    ) {
        this(caption, tooltip, valueStringifier, values, values.codec(), initialValue, onValueUpdate);
    }

    public OptionInstance(
        String caption,
        OptionInstance.TooltipSupplier<T> tooltip,
        OptionInstance.CaptionBasedToString<T> valueStringifier,
        OptionInstance.ValueSet<T> values,
        Codec<T> codec,
        T initialValue,
        Consumer<T> onValueUpdate
    ) {
        this.caption = Component.translatable(caption);
        this.tooltip = tooltip;
        this.toString = p_231506_ -> valueStringifier.toString(this.caption, p_231506_);
        this.values = values;
        this.codec = codec;
        this.initialValue = initialValue;
        this.onValueUpdate = onValueUpdate;
        this.value = this.initialValue;
    }

    public static <T> OptionInstance.TooltipSupplier<T> noTooltip() {
        return p_258114_ -> null;
    }

    public static <T> OptionInstance.TooltipSupplier<T> cachedConstantTooltip(Component message) {
        return p_258116_ -> Tooltip.create(message);
    }

    public AbstractWidget createButton(Options options) {
        return this.createButton(options, 0, 0, 150);
    }

    public AbstractWidget createButton(Options options, int x, int y, int width) {
        return this.createButton(options, x, y, width, p_261336_ -> {});
    }

    public AbstractWidget createButton(Options options, int x, int y, int width, Consumer<T> onValueChanged) {
        return this.values.createButton(this.tooltip, options, x, y, width, onValueChanged).apply(this);
    }

    public T get() {
        return this.value;
    }

    public Codec<T> codec() {
        return this.codec;
    }

    @Override
    public String toString() {
        return this.caption.getString();
    }

    public void set(T value) {
        T t = this.values.validateValue(value).orElseGet(() -> {
            LOGGER.error("Illegal option value {} for {}", value, this.caption.getString());
            return this.initialValue;
        });
        if (!Minecraft.getInstance().isRunning()) {
            this.value = t;
        } else {
            if (!Objects.equals(this.value, t)) {
                this.value = t;
                this.onValueUpdate.accept(this.value);
            }
        }
    }

    public OptionInstance.ValueSet<T> values() {
        return this.values;
    }

    @OnlyIn(Dist.CLIENT)
    public record AltEnum<T>(
        List<T> values, List<T> altValues, BooleanSupplier altCondition, OptionInstance.CycleableValueSet.ValueSetter<T> valueSetter, Codec<T> codec
    ) implements OptionInstance.CycleableValueSet<T> {
        @Override
        public CycleButton.ValueListSupplier<T> valueListSupplier() {
            return CycleButton.ValueListSupplier.create(this.altCondition, this.values, this.altValues);
        }

        @Override
        public Optional<T> validateValue(T p_231570_) {
            return (this.altCondition.getAsBoolean() ? this.altValues : this.values).contains(p_231570_) ? Optional.of(p_231570_) : Optional.empty();
        }
    }

    @OnlyIn(Dist.CLIENT)
    public interface CaptionBasedToString<T> {
        Component toString(Component caption, T value);
    }

    @OnlyIn(Dist.CLIENT)
    public record ClampingLazyMaxIntRange(int minInclusive, IntSupplier maxSupplier, int encodableMaxInclusive)
        implements OptionInstance.IntRangeBase,
        OptionInstance.SliderableOrCyclableValueSet<Integer> {
        public Optional<Integer> validateValue(Integer p_231590_) {
            return Optional.of(Mth.clamp(p_231590_, this.minInclusive(), this.maxInclusive()));
        }

        @Override
        public int maxInclusive() {
            return this.maxSupplier.getAsInt();
        }

        @Override
        public Codec<Integer> codec() {
            return Codec.INT
                .validate(
                    p_276098_ -> {
                        int i = this.encodableMaxInclusive + 1;
                        return p_276098_.compareTo(this.minInclusive) >= 0 && p_276098_.compareTo(i) <= 0
                            ? DataResult.success(p_276098_)
                            : DataResult.error(() -> "Value " + p_276098_ + " outside of range [" + this.minInclusive + ":" + i + "]", p_276098_);
                    }
                );
        }

        @Override
        public boolean createCycleButton() {
            return true;
        }

        @Override
        public CycleButton.ValueListSupplier<Integer> valueListSupplier() {
            return CycleButton.ValueListSupplier.create(IntStream.range(this.minInclusive, this.maxInclusive() + 1).boxed().toList());
        }
    }

    @OnlyIn(Dist.CLIENT)
    interface CycleableValueSet<T> extends OptionInstance.ValueSet<T> {
        CycleButton.ValueListSupplier<T> valueListSupplier();

        default OptionInstance.CycleableValueSet.ValueSetter<T> valueSetter() {
            return OptionInstance::set;
        }

        @Override
        default Function<OptionInstance<T>, AbstractWidget> createButton(
            OptionInstance.TooltipSupplier<T> p_261801_, Options p_261824_, int p_261649_, int p_262114_, int p_261536_, Consumer<T> p_261642_
        ) {
            return p_454137_ -> CycleButton.<T>builder(p_454137_.toString, p_454137_::get)
                .withValues(this.valueListSupplier())
                .withTooltip(p_261801_)
                .create(p_261649_, p_262114_, p_261536_, 20, p_454137_.caption, (p_261347_, p_261348_) -> {
                    this.valueSetter().set(p_454137_, p_261348_);
                    p_261824_.save();
                    p_261642_.accept(p_261348_);
                });
        }

        @OnlyIn(Dist.CLIENT)
        public interface ValueSetter<T> {
            void set(OptionInstance<T> instance, T value);
        }
    }

    @OnlyIn(Dist.CLIENT)
    public record Enum<T>(List<T> values, Codec<T> codec) implements OptionInstance.CycleableValueSet<T> {
        @Override
        public Optional<T> validateValue(T p_231632_) {
            return this.values.contains(p_231632_) ? Optional.of(p_231632_) : Optional.empty();
        }

        @Override
        public CycleButton.ValueListSupplier<T> valueListSupplier() {
            return CycleButton.ValueListSupplier.create(this.values);
        }
    }

    @OnlyIn(Dist.CLIENT)
    public record IntRange(int minInclusive, int maxInclusive, boolean applyValueImmediately) implements OptionInstance.IntRangeBase {
        public IntRange(int p_231642_, int p_231643_) {
            this(p_231642_, p_231643_, true);
        }

        public Optional<Integer> validateValue(Integer p_231645_) {
            return p_231645_.compareTo(this.minInclusive()) >= 0 && p_231645_.compareTo(this.maxInclusive()) <= 0 ? Optional.of(p_231645_) : Optional.empty();
        }

        @Override
        public Codec<Integer> codec() {
            return Codec.intRange(this.minInclusive, this.maxInclusive + 1);
        }
    }

    @OnlyIn(Dist.CLIENT)
    interface IntRangeBase extends OptionInstance.SliderableValueSet<Integer> {
        int minInclusive();

        int maxInclusive();

        default Optional<Integer> next(Integer p_455753_) {
            return Optional.of(p_455753_ + 1);
        }

        default Optional<Integer> previous(Integer p_454655_) {
            return Optional.of(p_454655_ - 1);
        }

        default double toSliderValue(Integer p_231663_) {
            if (p_231663_ == this.minInclusive()) {
                return 0.0;
            } else {
                return p_231663_ == this.maxInclusive()
                    ? 1.0
                    : Mth.map(p_231663_.intValue() + 0.5, (double)this.minInclusive(), this.maxInclusive() + 1.0, 0.0, 1.0);
            }
        }

        default Integer fromSliderValue(double p_231656_) {
            if (p_231656_ >= 1.0) {
                p_231656_ = 0.99999F;
            }

            return Mth.floor(Mth.map(p_231656_, 0.0, 1.0, (double)this.minInclusive(), this.maxInclusive() + 1.0));
        }

        default <R> OptionInstance.SliderableValueSet<R> xmap(
            final IntFunction<? extends R> encoder, final ToIntFunction<? super R> decoder, final boolean allowStepping
        ) {
            return new OptionInstance.SliderableValueSet<R>() {
                @Override
                public Optional<R> validateValue(R p_231674_) {
                    return IntRangeBase.this.validateValue(decoder.applyAsInt(p_231674_)).map(encoder::apply);
                }

                @Override
                public double toSliderValue(R p_231678_) {
                    return IntRangeBase.this.toSliderValue(decoder.applyAsInt(p_231678_));
                }

                @Override
                public Optional<R> next(R p_454789_) {
                    if (!allowStepping) {
                        return Optional.empty();
                    } else {
                        int i = decoder.applyAsInt(p_454789_);
                        return (Optional<R>)Optional.of(encoder.apply(IntRangeBase.this.validateValue(i + 1).orElse(i)));
                    }
                }

                @Override
                public Optional<R> previous(R p_454833_) {
                    if (!allowStepping) {
                        return Optional.empty();
                    } else {
                        int i = decoder.applyAsInt(p_454833_);
                        return (Optional<R>)Optional.of(encoder.apply(IntRangeBase.this.validateValue(i - 1).orElse(i)));
                    }
                }

                @Override
                public R fromSliderValue(double p_231676_) {
                    return (R)encoder.apply(IntRangeBase.this.fromSliderValue(p_231676_));
                }

                @Override
                public Codec<R> codec() {
                    return IntRangeBase.this.codec().xmap(encoder::apply, decoder::applyAsInt);
                }
            };
        }
    }

    @OnlyIn(Dist.CLIENT)
    public record LazyEnum<T>(Supplier<List<T>> values, Function<T, Optional<T>> validateValue, Codec<T> codec) implements OptionInstance.CycleableValueSet<T> {
        @Override
        public Optional<T> validateValue(T p_231689_) {
            return this.validateValue.apply(p_231689_);
        }

        @Override
        public CycleButton.ValueListSupplier<T> valueListSupplier() {
            return CycleButton.ValueListSupplier.create(this.values.get());
        }
    }

    @OnlyIn(Dist.CLIENT)
    public static final class OptionInstanceSliderButton<N> extends AbstractOptionSliderButton implements ResettableOptionWidget {
        private final OptionInstance<N> instance;
        private final OptionInstance.SliderableValueSet<N> values;
        private final OptionInstance.TooltipSupplier<N> tooltipSupplier;
        private final Consumer<N> onValueChanged;
        private @Nullable Long delayedApplyAt;
        private final boolean applyValueImmediately;

        OptionInstanceSliderButton(
            Options options,
            int x,
            int y,
            int width,
            int height,
            OptionInstance<N> instance,
            OptionInstance.SliderableValueSet<N> values,
            OptionInstance.TooltipSupplier<N> tooltipSupplier,
            Consumer<N> onValueChanged,
            boolean applyValueImmediately
        ) {
            super(options, x, y, width, height, values.toSliderValue(instance.get()));
            this.instance = instance;
            this.values = values;
            this.tooltipSupplier = tooltipSupplier;
            this.onValueChanged = onValueChanged;
            this.applyValueImmediately = applyValueImmediately;
            this.updateMessage();
        }

        @Override
        protected void updateMessage() {
            this.setMessage(this.instance.toString.apply(this.values.fromSliderValue(this.value)));
            this.setTooltip(this.tooltipSupplier.apply(this.values.fromSliderValue(this.value)));
        }

        @Override
        protected void applyValue() {
            if (this.applyValueImmediately) {
                this.applyUnsavedValue();
            } else {
                this.delayedApplyAt = Util.getMillis() + 600L;
            }
        }

        public void applyUnsavedValue() {
            N n = this.values.fromSliderValue(this.value);
            if (!Objects.equals(n, this.instance.get())) {
                this.instance.set(n);
                this.onValueChanged.accept(this.instance.get());
            }
        }

        @Override
        public void resetValue() {
            if (this.value != this.values.toSliderValue(this.instance.get())) {
                this.value = this.values.toSliderValue(this.instance.get());
                this.delayedApplyAt = null;
                this.updateMessage();
            }
        }

        @Override
        public void renderWidget(GuiGraphics p_341923_, int p_341938_, int p_341892_, float p_341933_) {
            super.renderWidget(p_341923_, p_341938_, p_341892_, p_341933_);
            if (this.delayedApplyAt != null && Util.getMillis() >= this.delayedApplyAt) {
                this.delayedApplyAt = null;
                this.applyUnsavedValue();
                this.resetValue();
            }
        }

        @Override
        public void onRelease(MouseButtonEvent p_455677_) {
            super.onRelease(p_455677_);
            if (this.applyValueImmediately) {
                this.resetValue();
            }
        }

        @Override
        public boolean keyPressed(KeyEvent p_455136_) {
            if (p_455136_.isSelection()) {
                this.canChangeValue = !this.canChangeValue;
                return true;
            } else {
                if (this.canChangeValue) {
                    boolean flag = p_455136_.isLeft();
                    boolean flag1 = p_455136_.isRight();
                    if (flag) {
                        Optional<N> optional = this.values.previous(this.values.fromSliderValue(this.value));
                        if (optional.isPresent()) {
                            this.setValue(this.values.toSliderValue(optional.get()));
                            return true;
                        }
                    }

                    if (flag1) {
                        Optional<N> optional1 = this.values.next(this.values.fromSliderValue(this.value));
                        if (optional1.isPresent()) {
                            this.setValue(this.values.toSliderValue(optional1.get()));
                            return true;
                        }
                    }

                    if (flag || flag1) {
                        float f = flag ? -1.0F : 1.0F;
                        this.setValue(this.value + f / (this.width - 8));
                        return true;
                    }
                }

                return false;
            }
        }
    }

    @OnlyIn(Dist.CLIENT)
    public record SliderableEnum<T>(List<T> values, Codec<T> codec) implements OptionInstance.SliderableValueSet<T> {
        @Override
        public double toSliderValue(T p_455348_) {
            if (p_455348_ == this.values.getFirst()) {
                return 0.0;
            } else {
                return p_455348_ == this.values.getLast()
                    ? 1.0
                    : Mth.map((double)this.values.indexOf(p_455348_), 0.0, (double)(this.values.size() - 1), 0.0, 1.0);
            }
        }

        @Override
        public Optional<T> next(T p_455290_) {
            int i = this.values.indexOf(p_455290_);
            int j = Mth.clamp(i + 1, 0, this.values.size() - 1);
            return Optional.of(this.values.get(j));
        }

        @Override
        public Optional<T> previous(T p_454996_) {
            int i = this.values.indexOf(p_454996_);
            int j = Mth.clamp(i - 1, 0, this.values.size() - 1);
            return Optional.of(this.values.get(j));
        }

        @Override
        public T fromSliderValue(double p_455811_) {
            if (p_455811_ >= 1.0) {
                p_455811_ = 0.99999F;
            }

            int i = Mth.floor(Mth.map(p_455811_, 0.0, 1.0, 0.0, (double)this.values.size()));
            return this.values.get(Mth.clamp(i, 0, this.values.size() - 1));
        }

        @Override
        public Optional<T> validateValue(T p_455301_) {
            int i = this.values.indexOf(p_455301_);
            return i > -1 ? Optional.of(p_455301_) : Optional.empty();
        }
    }

    @OnlyIn(Dist.CLIENT)
    interface SliderableOrCyclableValueSet<T> extends OptionInstance.CycleableValueSet<T>, OptionInstance.SliderableValueSet<T> {
        boolean createCycleButton();

        @Override
        default Function<OptionInstance<T>, AbstractWidget> createButton(
            OptionInstance.TooltipSupplier<T> p_261786_, Options p_262030_, int p_261940_, int p_262149_, int p_261495_, Consumer<T> p_261881_
        ) {
            return this.createCycleButton()
                ? OptionInstance.CycleableValueSet.super.createButton(p_261786_, p_262030_, p_261940_, p_262149_, p_261495_, p_261881_)
                : OptionInstance.SliderableValueSet.super.createButton(p_261786_, p_262030_, p_261940_, p_262149_, p_261495_, p_261881_);
        }
    }

    @OnlyIn(Dist.CLIENT)
    interface SliderableValueSet<T> extends OptionInstance.ValueSet<T> {
        double toSliderValue(T value);

        default Optional<T> next(T current) {
            return Optional.empty();
        }

        default Optional<T> previous(T current) {
            return Optional.empty();
        }

        T fromSliderValue(double value);

        default boolean applyValueImmediately() {
            return true;
        }

        @Override
        default Function<OptionInstance<T>, AbstractWidget> createButton(
            OptionInstance.TooltipSupplier<T> p_261993_, Options p_262177_, int p_261706_, int p_261683_, int p_261573_, Consumer<T> p_261969_
        ) {
            return p_341831_ -> new OptionInstance.OptionInstanceSliderButton<>(
                p_262177_, p_261706_, p_261683_, p_261573_, 20, p_341831_, this, p_261993_, p_261969_, this.applyValueImmediately()
            );
        }
    }

    @FunctionalInterface
    @OnlyIn(Dist.CLIENT)
    public interface TooltipSupplier<T> {
        @Nullable Tooltip apply(T value);
    }

    @OnlyIn(Dist.CLIENT)
    public static enum UnitDouble implements OptionInstance.SliderableValueSet<Double> {
        INSTANCE;

        public Optional<Double> validateValue(Double p_231747_) {
            return p_231747_ >= 0.0 && p_231747_ <= 1.0 ? Optional.of(p_231747_) : Optional.empty();
        }

        public double toSliderValue(Double p_231756_) {
            return p_231756_;
        }

        public Double fromSliderValue(double p_231741_) {
            return p_231741_;
        }

        public <R> OptionInstance.SliderableValueSet<R> xmap(final DoubleFunction<? extends R> encoder, final ToDoubleFunction<? super R> decoder) {
            return new OptionInstance.SliderableValueSet<R>() {
                @Override
                public Optional<R> validateValue(R p_231773_) {
                    return UnitDouble.this.validateValue(decoder.applyAsDouble(p_231773_)).map(encoder::apply);
                }

                @Override
                public double toSliderValue(R p_231777_) {
                    return UnitDouble.this.toSliderValue(decoder.applyAsDouble(p_231777_));
                }

                @Override
                public R fromSliderValue(double p_231775_) {
                    return (R)encoder.apply(UnitDouble.this.fromSliderValue(p_231775_));
                }

                @Override
                public Codec<R> codec() {
                    return UnitDouble.this.codec().xmap(encoder::apply, decoder::applyAsDouble);
                }
            };
        }

        @Override
        public Codec<Double> codec() {
            return Codec.withAlternative(Codec.doubleRange(0.0, 1.0), Codec.BOOL, p_231745_ -> p_231745_ ? 1.0 : 0.0);
        }
    }

    @OnlyIn(Dist.CLIENT)
    public interface ValueSet<T> {
        Function<OptionInstance<T>, AbstractWidget> createButton(
            OptionInstance.TooltipSupplier<T> tooltipSupplier, Options options, int x, int y, int width, Consumer<T> onValueChanged
        );

        Optional<T> validateValue(T value);

        Codec<T> codec();
    }
}
