package net.minecraft.client.gui.components;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.narration.NarratedElementType;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.sounds.SoundManager;
import net.minecraft.network.chat.Component;
import net.minecraft.util.ARGB;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class FocusableTextWidget extends MultiLineTextWidget {
    public static final int DEFAULT_PADDING = 4;
    private final int padding;
    private final int maxWidth;
    private final boolean alwaysShowBorder;
    private final FocusableTextWidget.BackgroundFill backgroundFill;

    FocusableTextWidget(Component message, Font font, int padding, int maxWidth, FocusableTextWidget.BackgroundFill backgroundFill, boolean alwaysShowBorder) {
        super(message, font);
        this.active = true;
        this.padding = padding;
        this.maxWidth = maxWidth;
        this.alwaysShowBorder = alwaysShowBorder;
        this.backgroundFill = backgroundFill;
        this.updateWidth();
        this.updateHeight();
        this.setCentered(true);
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput p_295798_) {
        p_295798_.add(NarratedElementType.TITLE, this.getMessage());
    }

    @Override
    public void renderWidget(GuiGraphics p_296375_, int p_295686_, int p_295354_, float p_295563_) {
        int i = this.alwaysShowBorder && !this.isFocused() ? ARGB.color(this.alpha, -6250336) : ARGB.white(this.alpha);
        switch (this.backgroundFill) {
            case ALWAYS:
                p_296375_.fill(this.getX() + 1, this.getY(), this.getRight(), this.getBottom(), ARGB.black(this.alpha));
                break;
            case ON_FOCUS:
                if (this.isFocused()) {
                    p_296375_.fill(this.getX() + 1, this.getY(), this.getRight(), this.getBottom(), ARGB.black(this.alpha));
                }
            case NEVER:
        }

        if (this.isFocused() || this.alwaysShowBorder) {
            p_296375_.renderOutline(this.getX(), this.getY(), this.getWidth(), this.getHeight(), i);
        }

        super.renderWidget(p_296375_, p_295686_, p_295354_, p_295563_);
    }

    @Override
    protected int getTextX() {
        return this.getX() + this.padding;
    }

    @Override
    protected int getTextY() {
        return super.getTextY() + this.padding;
    }

    @Override
    public MultiLineTextWidget setMaxWidth(int p_455173_) {
        return super.setMaxWidth(p_455173_ - this.padding * 2);
    }

    @Override
    public int getWidth() {
        return this.width;
    }

    @Override
    public int getHeight() {
        return this.height;
    }

    public int getPadding() {
        return this.padding;
    }

    public void updateWidth() {
        if (this.maxWidth != -1) {
            this.setWidth(this.maxWidth);
            this.setMaxWidth(this.maxWidth);
        } else {
            this.setWidth(this.getFont().width(this.getMessage()) + this.padding * 2);
        }
    }

    public void updateHeight() {
        int i = 9 * this.getFont().split(this.getMessage(), super.getWidth()).size();
        this.setHeight(i + this.padding * 2);
    }

    @Override
    public void setMessage(Component p_455477_) {
        this.message = p_455477_;
        int i;
        if (this.maxWidth != -1) {
            i = this.maxWidth;
        } else {
            i = this.getFont().width(p_455477_) + this.padding * 2;
        }

        this.setWidth(i);
        this.updateHeight();
    }

    @Override
    public void playDownSound(SoundManager p_295576_) {
    }

    public static FocusableTextWidget.Builder builder(Component message, Font font) {
        return new FocusableTextWidget.Builder(message, font);
    }

    public static FocusableTextWidget.Builder builder(Component message, Font font, int padding) {
        return new FocusableTextWidget.Builder(message, font, padding);
    }

    @OnlyIn(Dist.CLIENT)
    public static enum BackgroundFill {
        ALWAYS,
        ON_FOCUS,
        NEVER;
    }

    @OnlyIn(Dist.CLIENT)
    public static class Builder {
        private final Component message;
        private final Font font;
        private final int padding;
        private int maxWidth = -1;
        private boolean alwaysShowBorder = true;
        private FocusableTextWidget.BackgroundFill backgroundFill = FocusableTextWidget.BackgroundFill.ALWAYS;

        Builder(Component message, Font font) {
            this(message, font, 4);
        }

        Builder(Component message, Font font, int padding) {
            this.message = message;
            this.font = font;
            this.padding = padding;
        }

        public FocusableTextWidget.Builder maxWidth(int maxWidth) {
            this.maxWidth = maxWidth;
            return this;
        }

        public FocusableTextWidget.Builder textWidth(int textWidth) {
            this.maxWidth = textWidth + this.padding * 2;
            return this;
        }

        public FocusableTextWidget.Builder alwaysShowBorder(boolean alwaysShowBorder) {
            this.alwaysShowBorder = alwaysShowBorder;
            return this;
        }

        public FocusableTextWidget.Builder backgroundFill(FocusableTextWidget.BackgroundFill backgroundFill) {
            this.backgroundFill = backgroundFill;
            return this;
        }

        public FocusableTextWidget build() {
            return new FocusableTextWidget(this.message, this.font, this.padding, this.maxWidth, this.backgroundFill, this.alwaysShowBorder);
        }
    }
}
