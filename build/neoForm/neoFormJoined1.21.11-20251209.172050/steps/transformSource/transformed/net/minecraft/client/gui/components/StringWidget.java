package net.minecraft.client.gui.components;

import net.minecraft.client.gui.ActiveTextCollector;
import net.minecraft.client.gui.Font;
import net.minecraft.locale.Language;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.util.FormattedCharSequence;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class StringWidget extends AbstractStringWidget {
    private static final int TEXT_MARGIN = 2;
    private int maxWidth = 0;
    private int cachedWidth = 0;
    private boolean cachedWidthDirty = true;
    private StringWidget.TextOverflow textOverflow = StringWidget.TextOverflow.CLAMPED;

    public StringWidget(Component message, Font font) {
        this(0, 0, font.width(message.getVisualOrderText()), 9, message, font);
    }

    public StringWidget(int width, int height, Component message, Font font) {
        this(0, 0, width, height, message, font);
    }

    public StringWidget(int p_268199_, int p_268137_, int p_268178_, int p_268169_, Component p_268285_, Font p_268047_) {
        super(p_268199_, p_268137_, p_268178_, p_268169_, p_268285_, p_268047_);
        this.active = false;
    }

    @Override
    public void setMessage(Component p_439249_) {
        super.setMessage(p_439249_);
        this.cachedWidthDirty = true;
    }

    public StringWidget setMaxWidth(int maxWidth) {
        return this.setMaxWidth(maxWidth, StringWidget.TextOverflow.CLAMPED);
    }

    public StringWidget setMaxWidth(int maxWidth, StringWidget.TextOverflow textOverflow) {
        this.maxWidth = maxWidth;
        this.textOverflow = textOverflow;
        return this;
    }

    @Override
    public int getWidth() {
        if (this.maxWidth > 0) {
            if (this.cachedWidthDirty) {
                this.cachedWidth = Math.min(this.maxWidth, this.getFont().width(this.getMessage().getVisualOrderText()));
                this.cachedWidthDirty = false;
            }

            return this.cachedWidth;
        } else {
            return super.getWidth();
        }
    }

    @Override
    public void visitLines(ActiveTextCollector p_457993_) {
        Component component = this.getMessage();
        Font font = this.getFont();
        int i = this.maxWidth > 0 ? this.maxWidth : this.getWidth();
        int j = font.width(component);
        int k = this.getX();
        int l = this.getY() + (this.getHeight() - 9) / 2;
        boolean flag = j > i;
        if (flag) {
            switch (this.textOverflow) {
                case CLAMPED:
                    p_457993_.accept(k, l, clipText(component, font, i));
                    break;
                case SCROLLING:
                    this.renderScrollingStringOverContents(p_457993_, component, 2);
            }
        } else {
            p_457993_.accept(k, l, component.getVisualOrderText());
        }
    }

    public static FormattedCharSequence clipText(Component text, Font font, int maxWidth) {
        FormattedText formattedtext = font.substrByWidth(text, maxWidth - font.width(CommonComponents.ELLIPSIS));
        return Language.getInstance().getVisualOrder(FormattedText.composite(formattedtext, CommonComponents.ELLIPSIS));
    }

    @OnlyIn(Dist.CLIENT)
    public static enum TextOverflow {
        CLAMPED,
        SCROLLING;
    }
}
