package net.minecraft.client.gui.components;

import java.util.function.Supplier;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.input.InputWithModifiers;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.jspecify.annotations.Nullable;

@OnlyIn(Dist.CLIENT)
public abstract class Button extends AbstractButton {
    public static final int SMALL_WIDTH = 120;
    public static final int DEFAULT_WIDTH = 150;
    public static final int BIG_WIDTH = 200;
    public static final int DEFAULT_HEIGHT = 20;
    public static final int DEFAULT_SPACING = 8;
    protected static final Button.CreateNarration DEFAULT_NARRATION = p_253298_ -> p_253298_.get();
    protected final Button.OnPress onPress;
    protected final Button.CreateNarration createNarration;

    public static Button.Builder builder(Component message, Button.OnPress onPress) {
        return new Button.Builder(message, onPress);
    }

    protected Button(
        int x, int y, int width, int height, Component message, Button.OnPress onPress, Button.CreateNarration createNarration
    ) {
        super(x, y, width, height, message);
        this.onPress = onPress;
        this.createNarration = createNarration;
    }

    protected Button(Builder builder) {
        this(builder.x, builder.y, builder.width, builder.height, builder.message, builder.onPress, builder.createNarration);
        setTooltip(builder.tooltip); // Forge: Make use of the Builder tooltip
    }

    @Override
    public void onPress(InputWithModifiers p_446034_) {
        this.onPress.onPress(this);
    }

    @Override
    protected MutableComponent createNarrationMessage() {
        return this.createNarration.createNarrationMessage(() -> super.createNarrationMessage());
    }

    @Override
    public void updateWidgetNarration(NarrationElementOutput p_259196_) {
        this.defaultButtonNarrationText(p_259196_);
    }

    @OnlyIn(Dist.CLIENT)
    public static class Builder {
        private final Component message;
        private final Button.OnPress onPress;
        private @Nullable Tooltip tooltip;
        private int x;
        private int y;
        private int width = 150;
        private int height = 20;
        private Button.CreateNarration createNarration = Button.DEFAULT_NARRATION;

        public Builder(Component message, Button.OnPress onPress) {
            this.message = message;
            this.onPress = onPress;
        }

        public Button.Builder pos(int x, int y) {
            this.x = x;
            this.y = y;
            return this;
        }

        public Button.Builder width(int width) {
            this.width = width;
            return this;
        }

        public Button.Builder size(int width, int height) {
            this.width = width;
            this.height = height;
            return this;
        }

        public Button.Builder bounds(int x, int y, int width, int height) {
            return this.pos(x, y).size(width, height);
        }

        public Button.Builder tooltip(@Nullable Tooltip tooltip) {
            this.tooltip = tooltip;
            return this;
        }

        public Button.Builder createNarration(Button.CreateNarration createNarration) {
            this.createNarration = createNarration;
            return this;
        }

        public Button build() {
            Button button = new Button.Plain(this.x, this.y, this.width, this.height, this.message, this.onPress, this.createNarration);
            button.setTooltip(this.tooltip);
            return button;
        }

        public Button build(java.util.function.Function<Builder, Button> builder) {
            return builder.apply(this);
        }
    }

    @OnlyIn(Dist.CLIENT)
    public interface CreateNarration {
        MutableComponent createNarrationMessage(Supplier<MutableComponent> messageSupplier);
    }

    @OnlyIn(Dist.CLIENT)
    public interface OnPress {
        void onPress(Button button);
    }

    @OnlyIn(Dist.CLIENT)
    public static class Plain extends Button {
        protected Plain(
            int p_457831_, int p_458177_, int p_458246_, int p_457590_, Component p_457882_, Button.OnPress p_457728_, Button.CreateNarration p_458093_
        ) {
            super(p_457831_, p_458177_, p_458246_, p_457590_, p_457882_, p_457728_, p_458093_);
        }

        protected Plain(Builder builder) {
            super(builder);
        }

        @Override
        protected void renderContents(GuiGraphics p_458247_, int p_457832_, int p_457537_, float p_457835_) {
            this.renderDefaultSprite(p_458247_);
            this.renderDefaultLabel(p_458247_.textRendererForWidget(this, GuiGraphics.HoveredTextEffects.NONE));
        }
    }
}
