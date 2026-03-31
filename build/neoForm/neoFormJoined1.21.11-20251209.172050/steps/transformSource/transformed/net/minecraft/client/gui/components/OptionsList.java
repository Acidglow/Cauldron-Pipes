package net.minecraft.client.gui.components;

import com.google.common.collect.Lists;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.OptionInstance;
import net.minecraft.client.Options;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarratableEntry;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.options.OptionsSubScreen;
import net.minecraft.network.chat.Component;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.jspecify.annotations.Nullable;

@OnlyIn(Dist.CLIENT)
public class OptionsList extends ContainerObjectSelectionList<OptionsList.AbstractEntry> {
    private static final int BIG_BUTTON_WIDTH = 310;
    private static final int DEFAULT_ITEM_HEIGHT = 25;
    private final OptionsSubScreen screen;

    public OptionsList(Minecraft minecraft, int width, OptionsSubScreen screen) {
        super(minecraft, width, screen.layout.getContentHeight(), screen.layout.getHeaderHeight(), 25);
        this.centerListVertically = false;
        this.screen = screen;
    }

    public void addBig(OptionInstance<?> option) {
        this.addEntry(OptionsList.Entry.big(this.minecraft.options, option, this.screen));
    }

    public void addSmall(OptionInstance<?>... options) {
        for (int i = 0; i < options.length; i += 2) {
            OptionInstance<?> optioninstance = i < options.length - 1 ? options[i + 1] : null;
            this.addEntry(OptionsList.Entry.small(this.minecraft.options, options[i], optioninstance, this.screen));
        }
    }

    public void addSmall(List<AbstractWidget> options) {
        for (int i = 0; i < options.size(); i += 2) {
            this.addSmall(options.get(i), i < options.size() - 1 ? options.get(i + 1) : null);
        }
    }

    public void addSmall(AbstractWidget leftOption, @Nullable AbstractWidget rightOption) {
        this.addEntry(OptionsList.Entry.small(leftOption, rightOption, this.screen));
    }

    public void addSmall(AbstractWidget leftOption, OptionInstance<?> leftOptionInstance, @Nullable AbstractWidget rightOption) {
        this.addEntry(OptionsList.Entry.small(leftOption, leftOptionInstance, rightOption, this.screen));
    }

    public void addHeader(Component text) {
        int i = 9;
        int j = this.children().isEmpty() ? 0 : i * 2;
        this.addEntry(new OptionsList.HeaderEntry(this.screen, text, j), j + i + 4);
    }

    @Override
    public int getRowWidth() {
        return 310;
    }

    public @Nullable AbstractWidget findOption(OptionInstance<?> option) {
        for (OptionsList.AbstractEntry optionslist$abstractentry : this.children()) {
            if (optionslist$abstractentry instanceof OptionsList.Entry optionslist$entry) {
                AbstractWidget abstractwidget = optionslist$entry.findOption(option);
                if (abstractwidget != null) {
                    return abstractwidget;
                }
            }
        }

        return null;
    }

    public void applyUnsavedChanges() {
        for (OptionsList.AbstractEntry optionslist$abstractentry : this.children()) {
            if (optionslist$abstractentry instanceof OptionsList.Entry optionslist$entry) {
                for (OptionsList.OptionInstanceWidget optionslist$optioninstancewidget : optionslist$entry.children) {
                    if (optionslist$optioninstancewidget.optionInstance() != null
                        && optionslist$optioninstancewidget.widget() instanceof OptionInstance.OptionInstanceSliderButton<?> optioninstancesliderbutton) {
                        optioninstancesliderbutton.applyUnsavedValue();
                    }
                }
            }
        }
    }

    public void resetOption(OptionInstance<?> option) {
        for (OptionsList.AbstractEntry optionslist$abstractentry : this.children()) {
            if (optionslist$abstractentry instanceof OptionsList.Entry optionslist$entry) {
                for (OptionsList.OptionInstanceWidget optionslist$optioninstancewidget : optionslist$entry.children) {
                    if (optionslist$optioninstancewidget.optionInstance() == option
                        && optionslist$optioninstancewidget.widget() instanceof ResettableOptionWidget resettableoptionwidget) {
                        resettableoptionwidget.resetValue();
                        return;
                    }
                }
            }
        }
    }

    @OnlyIn(Dist.CLIENT)
    protected abstract static class AbstractEntry extends ContainerObjectSelectionList.Entry<OptionsList.AbstractEntry> {
    }

    @OnlyIn(Dist.CLIENT)
    protected static class Entry extends OptionsList.AbstractEntry {
        final List<OptionsList.OptionInstanceWidget> children;
        private final Screen screen;
        private static final int X_OFFSET = 160;

        private Entry(List<OptionsList.OptionInstanceWidget> children, Screen screen) {
            this.children = children;
            this.screen = screen;
        }

        public static OptionsList.Entry big(Options options, OptionInstance<?> option, Screen screen) {
            return new OptionsList.Entry(List.of(new OptionsList.OptionInstanceWidget(option.createButton(options, 0, 0, 310), option)), screen);
        }

        public static OptionsList.Entry small(AbstractWidget leftOption, @Nullable AbstractWidget rightOption, Screen screen) {
            return rightOption == null
                ? new OptionsList.Entry(List.of(new OptionsList.OptionInstanceWidget(leftOption)), screen)
                : new OptionsList.Entry(List.of(new OptionsList.OptionInstanceWidget(leftOption), new OptionsList.OptionInstanceWidget(rightOption)), screen);
        }

        public static OptionsList.Entry small(AbstractWidget leftOption, OptionInstance<?> leftOptionInstance, @Nullable AbstractWidget rightOption, Screen screen) {
            return rightOption == null
                ? new OptionsList.Entry(List.of(new OptionsList.OptionInstanceWidget(leftOption, leftOptionInstance)), screen)
                : new OptionsList.Entry(
                    List.of(new OptionsList.OptionInstanceWidget(leftOption, leftOptionInstance), new OptionsList.OptionInstanceWidget(rightOption)), screen
                );
        }

        public static OptionsList.Entry small(Options options, OptionInstance<?> leftOption, @Nullable OptionInstance<?> rightOption, OptionsSubScreen screen) {
            AbstractWidget abstractwidget = leftOption.createButton(options);
            return rightOption == null
                ? new OptionsList.Entry(List.of(new OptionsList.OptionInstanceWidget(abstractwidget, leftOption)), screen)
                : new OptionsList.Entry(
                    List.of(
                        new OptionsList.OptionInstanceWidget(abstractwidget, leftOption),
                        new OptionsList.OptionInstanceWidget(rightOption.createButton(options), rightOption)
                    ),
                    screen
                );
        }

        @Override
        public void renderContent(GuiGraphics p_440603_, int p_440287_, int p_439324_, boolean p_439478_, float p_440210_) {
            int i = 0;
            int j = this.screen.width / 2 - 155;

            for (OptionsList.OptionInstanceWidget optionslist$optioninstancewidget : this.children) {
                optionslist$optioninstancewidget.widget().setPosition(j + i, this.getContentY());
                optionslist$optioninstancewidget.widget().render(p_440603_, p_440287_, p_439324_, p_440210_);
                i += 160;
            }
        }

        @Override
        public List<? extends GuiEventListener> children() {
            return Lists.transform(this.children, OptionsList.OptionInstanceWidget::widget);
        }

        @Override
        public List<? extends NarratableEntry> narratables() {
            return Lists.transform(this.children, OptionsList.OptionInstanceWidget::widget);
        }

        public @Nullable AbstractWidget findOption(OptionInstance<?> optionInstance) {
            for (OptionsList.OptionInstanceWidget optionslist$optioninstancewidget : this.children) {
                if (optionslist$optioninstancewidget.optionInstance == optionInstance) {
                    return optionslist$optioninstancewidget.widget();
                }
            }

            return null;
        }
    }

    @OnlyIn(Dist.CLIENT)
    protected static class HeaderEntry extends OptionsList.AbstractEntry {
        private final Screen screen;
        private final int paddingTop;
        private final StringWidget widget;

        protected HeaderEntry(Screen screen, Component text, int paddingTop) {
            this.screen = screen;
            this.paddingTop = paddingTop;
            this.widget = new StringWidget(text, screen.getFont());
        }

        @Override
        public List<? extends NarratableEntry> narratables() {
            return List.of(this.widget);
        }

        @Override
        public void renderContent(GuiGraphics p_455586_, int p_456280_, int p_454936_, boolean p_455018_, float p_455521_) {
            this.widget.setPosition(this.screen.width / 2 - 155, this.getContentY() + this.paddingTop);
            this.widget.render(p_455586_, p_456280_, p_454936_, p_455521_);
        }

        @Override
        public List<? extends GuiEventListener> children() {
            return List.of(this.widget);
        }
    }

    @OnlyIn(Dist.CLIENT)
    public record OptionInstanceWidget(AbstractWidget widget, @Nullable OptionInstance<?> optionInstance) {
        public OptionInstanceWidget(AbstractWidget p_460993_) {
            this(p_460993_, null);
        }
    }
}
