package net.minecraft.client.gui.components;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.gui.ActiveTextCollector;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.TextAlignment;
import net.minecraft.locale.Language;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.util.FormattedCharSequence;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.jspecify.annotations.Nullable;

@OnlyIn(Dist.CLIENT)
public interface MultiLineLabel {
    MultiLineLabel EMPTY = new MultiLineLabel() {
        @Override
        public int visitLines(TextAlignment p_458262_, int p_94389_, int p_94390_, int p_94391_, ActiveTextCollector p_457837_) {
            return p_94390_;
        }

        @Override
        public int getLineCount() {
            return 0;
        }

        @Override
        public int getWidth() {
            return 0;
        }
    };

    static MultiLineLabel create(Font font, Component... components) {
        return create(font, Integer.MAX_VALUE, Integer.MAX_VALUE, components);
    }

    static MultiLineLabel create(Font font, int maxWidth, Component... components) {
        return create(font, maxWidth, Integer.MAX_VALUE, components);
    }

    static MultiLineLabel create(Font font, Component component, int maxWidth) {
        return create(font, maxWidth, Integer.MAX_VALUE, component);
    }

    static MultiLineLabel create(final Font font, final int maxWidth, final int maxRows, final Component... components) {
        return components.length == 0
            ? EMPTY
            : new MultiLineLabel() {
                private @Nullable List<MultiLineLabel.TextAndWidth> cachedTextAndWidth;
                private @Nullable Language splitWithLanguage;

                @Override
                public int visitLines(TextAlignment p_457779_, int p_458259_, int p_458018_, int p_457763_, ActiveTextCollector p_458016_) {
                    int i = p_458018_;

                    for (MultiLineLabel.TextAndWidth multilinelabel$textandwidth : this.getSplitMessage()) {
                        int j = p_457779_.calculateLeft(p_458259_, multilinelabel$textandwidth.width);
                        p_458016_.accept(j, i, multilinelabel$textandwidth.text);
                        i += p_457763_;
                    }

                    return i;
                }

                private List<MultiLineLabel.TextAndWidth> getSplitMessage() {
                    Language language = Language.getInstance();
                    if (this.cachedTextAndWidth != null && language == this.splitWithLanguage) {
                        return this.cachedTextAndWidth;
                    } else {
                        this.splitWithLanguage = language;
                        List<FormattedText> list = new ArrayList<>();

                        for (Component component : components) {
                            list.addAll(font.splitIgnoringLanguage(component, maxWidth));
                        }

                        this.cachedTextAndWidth = new ArrayList<>();
                        int i = Math.min(list.size(), maxRows);
                        List<FormattedText> list1 = list.subList(0, i);

                        for (int j = 0; j < list1.size(); j++) {
                            FormattedText formattedtext2 = list1.get(j);
                            FormattedCharSequence formattedcharsequence = Language.getInstance().getVisualOrder(formattedtext2);
                            if (j == list1.size() - 1 && i == maxRows && i != list.size()) {
                                FormattedText formattedtext = font.substrByWidth(
                                    formattedtext2, font.width(formattedtext2) - font.width(CommonComponents.ELLIPSIS)
                                );
                                FormattedText formattedtext1 = FormattedText.composite(
                                    formattedtext, CommonComponents.ELLIPSIS.copy().withStyle(components[components.length - 1].getStyle())
                                );
                                this.cachedTextAndWidth
                                    .add(new MultiLineLabel.TextAndWidth(Language.getInstance().getVisualOrder(formattedtext1), font.width(formattedtext1)));
                            } else {
                                this.cachedTextAndWidth.add(new MultiLineLabel.TextAndWidth(formattedcharsequence, font.width(formattedcharsequence)));
                            }
                        }

                        return this.cachedTextAndWidth;
                    }
                }

                @Override
                public int getLineCount() {
                    return this.getSplitMessage().size();
                }

                @Override
                public int getWidth() {
                    return Math.min(maxWidth, this.getSplitMessage().stream().mapToInt(MultiLineLabel.TextAndWidth::width).max().orElse(0));
                }
            };
    }

    int visitLines(TextAlignment textAlignment, int x, int y, int lineHeight, ActiveTextCollector activeTextCollector);

    int getLineCount();

    int getWidth();

    @OnlyIn(Dist.CLIENT)
    public record TextAndWidth(FormattedCharSequence text, int width) {
    }
}
