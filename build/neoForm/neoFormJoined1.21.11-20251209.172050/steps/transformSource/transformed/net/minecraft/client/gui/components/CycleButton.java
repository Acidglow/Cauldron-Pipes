package net.minecraft.client.gui.components;

import com.google.common.collect.ImmutableList;
import java.util.Collection;
import java.util.List;
import java.util.function.BooleanSupplier;
import java.util.function.Function;
import java.util.function.Supplier;
import net.minecraft.client.Minecraft;
import net.minecraft.client.OptionInstance;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.narration.NarratedElementType;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.input.InputWithModifiers;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.jspecify.annotations.Nullable;

@OnlyIn(Dist.CLIENT)
public class CycleButton<T> extends AbstractButton implements ResettableOptionWidget {
    public static final BooleanSupplier DEFAULT_ALT_LIST_SELECTOR = () -> Minecraft.getInstance().hasAltDown();
    private static final List<Boolean> BOOLEAN_OPTIONS = ImmutableList.of(Boolean.TRUE, Boolean.FALSE);
    private final Supplier<T> defaultValueSupplier;
    private final Component name;
    private int index;
    private T value;
    private final CycleButton.ValueListSupplier<T> values;
    private final Function<T, Component> valueStringifier;
    private final Function<CycleButton<T>, MutableComponent> narrationProvider;
    private final CycleButton.OnValueChange<T> onValueChange;
    private final CycleButton.DisplayState displayState;
    private final OptionInstance.TooltipSupplier<T> tooltipSupplier;
    private final CycleButton.SpriteSupplier<T> spriteSupplier;

    CycleButton(
        int x,
        int y,
        int width,
        int height,
        Component message,
        Component name,
        int index,
        T value,
        Supplier<T> defaultValueSupplier,
        CycleButton.ValueListSupplier<T> values,
        Function<T, Component> valueStringifier,
        Function<CycleButton<T>, MutableComponent> narrationProvider,
        CycleButton.OnValueChange<T> onValueChange,
        OptionInstance.TooltipSupplier<T> tooltipSupplier,
        CycleButton.DisplayState displayState,
        CycleButton.SpriteSupplier<T> spriteSupplier
    ) {
        super(x, y, width, height, message);
        this.name = name;
        this.index = index;
        this.defaultValueSupplier = defaultValueSupplier;
        this.value = value;
        this.values = values;
        this.valueStringifier = valueStringifier;
        this.narrationProvider = narrationProvider;
        this.onValueChange = onValueChange;
        this.displayState = displayState;
        this.tooltipSupplier = tooltipSupplier;
        this.spriteSupplier = spriteSupplier;
        this.updateTooltip();
    }

    @Override
    protected void renderContents(GuiGraphics p_457908_, int p_457577_, int p_457983_, float p_457520_) {
        Identifier identifier = this.spriteSupplier.apply(this, this.getValue());
        if (identifier != null) {
            p_457908_.blitSprite(RenderPipelines.GUI_TEXTURED, identifier, this.getX(), this.getY(), this.getWidth(), this.getHeight());
        } else {
            this.renderDefaultSprite(p_457908_);
        }

        if (this.displayState != CycleButton.DisplayState.HIDE) {
            this.renderDefaultLabel(p_457908_.textRendererForWidget(this, GuiGraphics.HoveredTextEffects.NONE));
        }
    }

    private void updateTooltip() {
        this.setTooltip(this.tooltipSupplier.apply(this.value));
    }

    @Override
    public void onPress(InputWithModifiers p_445679_) {
        if (p_445679_.hasShiftDown()) {
            this.cycleValue(-1);
        } else {
            this.cycleValue(1);
        }
    }

    private void cycleValue(int delta) {
        List<T> list = this.values.getSelectedList();
        this.index = Mth.positiveModulo(this.index + delta, list.size());
        T t = list.get(this.index);
        this.updateValue(t);
        this.onValueChange.onValueChange(this, t);
    }

    private T getCycledValue(int delta) {
        List<T> list = this.values.getSelectedList();
        return list.get(Mth.positiveModulo(this.index + delta, list.size()));
    }

    @Override
    public boolean mouseScrolled(double p_168885_, double p_168886_, double p_168887_, double p_294881_) {
        if (p_294881_ > 0.0) {
            this.cycleValue(-1);
        } else if (p_294881_ < 0.0) {
            this.cycleValue(1);
        }

        return true;
    }

    public void setValue(T value) {
        List<T> list = this.values.getSelectedList();
        int i = list.indexOf(value);
        if (i != -1) {
            this.index = i;
        }

        this.updateValue(value);
    }

    @Override
    public void resetValue() {
        this.setValue(this.defaultValueSupplier.get());
    }

    private void updateValue(T value) {
        Component component = this.createLabelForValue(value);
        this.setMessage(component);
        this.value = value;
        this.updateTooltip();
    }

    private Component createLabelForValue(T value) {
        return (Component)(this.displayState == CycleButton.DisplayState.VALUE ? this.valueStringifier.apply(value) : this.createFullName(value));
    }

    private MutableComponent createFullName(T value) {
        return CommonComponents.optionNameValue(this.name, this.valueStringifier.apply(value));
    }

    public T getValue() {
        return this.value;
    }

    @Override
    protected MutableComponent createNarrationMessage() {
        return this.narrationProvider.apply(this);
    }

    @Override
    public void updateWidgetNarration(NarrationElementOutput p_168889_) {
        p_168889_.add(NarratedElementType.TITLE, this.createNarrationMessage());
        if (this.active) {
            T t = this.getCycledValue(1);
            Component component = this.createLabelForValue(t);
            if (this.isFocused()) {
                p_168889_.add(NarratedElementType.USAGE, Component.translatable("narration.cycle_button.usage.focused", component));
            } else {
                p_168889_.add(NarratedElementType.USAGE, Component.translatable("narration.cycle_button.usage.hovered", component));
            }
        }
    }

    public MutableComponent createDefaultNarrationMessage() {
        return wrapDefaultNarrationMessage(
            (Component)(this.displayState == CycleButton.DisplayState.VALUE ? this.createFullName(this.value) : this.getMessage())
        );
    }

    public static <T> CycleButton.Builder<T> builder(Function<T, Component> valueStringifier, Supplier<T> defaultValueSupplier) {
        return new CycleButton.Builder<>(valueStringifier, defaultValueSupplier);
    }

    public static <T> CycleButton.Builder<T> builder(Function<T, Component> valueStringifier, T defaultValue) {
        return new CycleButton.Builder<>(valueStringifier, () -> defaultValue);
    }

    public static CycleButton.Builder<Boolean> booleanBuilder(Component trueText, Component falseText, boolean defaultValue) {
        return new CycleButton.Builder<>(p_454162_ -> p_454162_ == Boolean.TRUE ? trueText : falseText, () -> defaultValue).withValues(BOOLEAN_OPTIONS);
    }

    public static CycleButton.Builder<Boolean> onOffBuilder(boolean defaultValue) {
        return new CycleButton.Builder<>(p_454166_ -> p_454166_ == Boolean.TRUE ? CommonComponents.OPTION_ON : CommonComponents.OPTION_OFF, () -> defaultValue)
            .withValues(BOOLEAN_OPTIONS);
    }

    @OnlyIn(Dist.CLIENT)
    public static class Builder<T> {
        private final Supplier<T> defaultValueSupplier;
        private final Function<T, Component> valueStringifier;
        private OptionInstance.TooltipSupplier<T> tooltipSupplier = p_168964_ -> null;
        private CycleButton.SpriteSupplier<T> spriteSupplier = (p_470475_, p_470476_) -> null;
        private Function<CycleButton<T>, MutableComponent> narrationProvider = CycleButton::createDefaultNarrationMessage;
        private CycleButton.ValueListSupplier<T> values = CycleButton.ValueListSupplier.create(ImmutableList.of());
        private CycleButton.DisplayState displayState = CycleButton.DisplayState.NAME_AND_VALUE;

        public Builder(Function<T, Component> valueStringifier, Supplier<T> defaultValueSupplier) {
            this.valueStringifier = valueStringifier;
            this.defaultValueSupplier = defaultValueSupplier;
        }

        public CycleButton.Builder<T> withValues(Collection<T> values) {
            return this.withValues(CycleButton.ValueListSupplier.create(values));
        }

        @SafeVarargs
        public final CycleButton.Builder<T> withValues(T... values) {
            return this.withValues(ImmutableList.copyOf(values));
        }

        public CycleButton.Builder<T> withValues(List<T> defaultList, List<T> selectedList) {
            return this.withValues(CycleButton.ValueListSupplier.create(CycleButton.DEFAULT_ALT_LIST_SELECTOR, defaultList, selectedList));
        }

        public CycleButton.Builder<T> withValues(BooleanSupplier altListSelector, List<T> defaultList, List<T> selectedList) {
            return this.withValues(CycleButton.ValueListSupplier.create(altListSelector, defaultList, selectedList));
        }

        public CycleButton.Builder<T> withValues(CycleButton.ValueListSupplier<T> values) {
            this.values = values;
            return this;
        }

        public CycleButton.Builder<T> withTooltip(OptionInstance.TooltipSupplier<T> tooltipSupplier) {
            this.tooltipSupplier = tooltipSupplier;
            return this;
        }

        public CycleButton.Builder<T> withCustomNarration(Function<CycleButton<T>, MutableComponent> narrationProvider) {
            this.narrationProvider = narrationProvider;
            return this;
        }

        public CycleButton.Builder<T> withSprite(CycleButton.SpriteSupplier<T> spriteSupplier) {
            this.spriteSupplier = spriteSupplier;
            return this;
        }

        public CycleButton.Builder<T> displayState(CycleButton.DisplayState displayState) {
            this.displayState = displayState;
            return this;
        }

        public CycleButton.Builder<T> displayOnlyValue() {
            return this.displayState(CycleButton.DisplayState.VALUE);
        }

        public CycleButton<T> create(Component message, CycleButton.OnValueChange<T> onValueChange) {
            return this.create(0, 0, 150, 20, message, onValueChange);
        }

        public CycleButton<T> create(int x, int y, int width, int height, Component name) {
            return this.create(x, y, width, height, name, (p_168946_, p_168947_) -> {});
        }

        public CycleButton<T> create(int x, int y, int width, int height, Component name, CycleButton.OnValueChange<T> onValueChange) {
            List<T> list = this.values.getDefaultList();
            if (list.isEmpty()) {
                throw new IllegalStateException("No values for cycle button");
            } else {
                T t = this.defaultValueSupplier.get();
                int i = list.indexOf(t);
                Component component = this.valueStringifier.apply(t);
                Component component1 = (Component)(this.displayState == CycleButton.DisplayState.VALUE
                    ? component
                    : CommonComponents.optionNameValue(name, component));
                return new CycleButton<>(
                    x,
                    y,
                    width,
                    height,
                    component1,
                    name,
                    i,
                    t,
                    this.defaultValueSupplier,
                    this.values,
                    this.valueStringifier,
                    this.narrationProvider,
                    onValueChange,
                    this.tooltipSupplier,
                    this.displayState,
                    this.spriteSupplier
                );
            }
        }
    }

    @OnlyIn(Dist.CLIENT)
    public static enum DisplayState {
        NAME_AND_VALUE,
        VALUE,
        HIDE;
    }

    @FunctionalInterface
    @OnlyIn(Dist.CLIENT)
    public interface OnValueChange<T> {
        void onValueChange(CycleButton<T> cycleButton, T value);
    }

    @FunctionalInterface
    @OnlyIn(Dist.CLIENT)
    public interface SpriteSupplier<T> {
        @Nullable Identifier apply(CycleButton<T> button, T value);
    }

    @OnlyIn(Dist.CLIENT)
    public interface ValueListSupplier<T> {
        List<T> getSelectedList();

        List<T> getDefaultList();

        static <T> CycleButton.ValueListSupplier<T> create(Collection<T> values) {
            final List<T> list = ImmutableList.copyOf(values);
            return new CycleButton.ValueListSupplier<T>() {
                @Override
                public List<T> getSelectedList() {
                    return list;
                }

                @Override
                public List<T> getDefaultList() {
                    return list;
                }
            };
        }

        static <T> CycleButton.ValueListSupplier<T> create(final BooleanSupplier altListSelector, List<T> defaultList, List<T> selectedList) {
            final List<T> list = ImmutableList.copyOf(defaultList);
            final List<T> list1 = ImmutableList.copyOf(selectedList);
            return new CycleButton.ValueListSupplier<T>() {
                @Override
                public List<T> getSelectedList() {
                    return altListSelector.getAsBoolean() ? list1 : list;
                }

                @Override
                public List<T> getDefaultList() {
                    return list;
                }
            };
        }
    }
}
