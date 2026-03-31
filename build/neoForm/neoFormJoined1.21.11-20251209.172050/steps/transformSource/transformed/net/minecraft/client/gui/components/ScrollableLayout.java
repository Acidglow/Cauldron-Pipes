package net.minecraft.client.gui.components;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.Consumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.layouts.Layout;
import net.minecraft.client.gui.layouts.LayoutElement;
import net.minecraft.client.gui.narration.NarratableEntry;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.navigation.ScreenDirection;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.network.chat.CommonComponents;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.jspecify.annotations.Nullable;

@OnlyIn(Dist.CLIENT)
public class ScrollableLayout implements Layout {
    private static final int SCROLLBAR_SPACING = 4;
    private static final int SCROLLBAR_RESERVE = 10;
    final Layout content;
    private final ScrollableLayout.Container container;
    private int minWidth;
    private int maxHeight;

    public ScrollableLayout(Minecraft minecraft, Layout content, int height) {
        this.content = content;
        this.container = new ScrollableLayout.Container(minecraft, 0, height);
    }

    public void setMinWidth(int minWidth) {
        this.minWidth = minWidth;
        this.container.setWidth(Math.max(this.content.getWidth(), minWidth));
    }

    public void setMaxHeight(int maxHeight) {
        this.maxHeight = maxHeight;
        this.container.setHeight(Math.min(this.content.getHeight(), maxHeight));
        this.container.refreshScrollAmount();
    }

    @Override
    public void arrangeElements() {
        this.content.arrangeElements();
        int i = this.content.getWidth();
        this.container.setWidth(Math.max(i + 20, this.minWidth));
        this.container.setHeight(Math.min(this.content.getHeight(), this.maxHeight));
        this.container.refreshScrollAmount();
    }

    @Override
    public void visitChildren(Consumer<LayoutElement> p_425954_) {
        p_425954_.accept(this.container);
    }

    @Override
    public void setX(int p_425725_) {
        this.container.setX(p_425725_);
    }

    @Override
    public void setY(int p_426034_) {
        this.container.setY(p_426034_);
    }

    @Override
    public int getX() {
        return this.container.getX();
    }

    @Override
    public int getY() {
        return this.container.getY();
    }

    @Override
    public int getWidth() {
        return this.container.getWidth();
    }

    @Override
    public int getHeight() {
        return this.container.getHeight();
    }

    @OnlyIn(Dist.CLIENT)
    class Container extends AbstractContainerWidget {
        private final Minecraft minecraft;
        private final List<AbstractWidget> children = new ArrayList<>();

        public Container(Minecraft minecraft, int width, int height) {
            super(0, 0, width, height, CommonComponents.EMPTY);
            this.minecraft = minecraft;
            ScrollableLayout.this.content.visitWidgets(this.children::add);
        }

        @Override
        protected int contentHeight() {
            return ScrollableLayout.this.content.getHeight();
        }

        @Override
        protected double scrollRate() {
            return 10.0;
        }

        @Override
        protected void renderWidget(GuiGraphics p_426247_, int p_426123_, int p_425847_, float p_426024_) {
            p_426247_.enableScissor(this.getX(), this.getY(), this.getX() + this.width, this.getY() + this.height);

            for (AbstractWidget abstractwidget : this.children) {
                abstractwidget.render(p_426247_, p_426123_, p_425847_, p_426024_);
            }

            p_426247_.disableScissor();
            this.renderScrollbar(p_426247_, p_426123_, p_425847_);
        }

        @Override
        protected void updateWidgetNarration(NarrationElementOutput p_425517_) {
        }

        @Override
        public ScreenRectangle getBorderForArrowNavigation(ScreenDirection p_425900_) {
            return new ScreenRectangle(this.getX(), this.getY(), this.width, this.contentHeight());
        }

        @Override
        public void setFocused(@Nullable GuiEventListener p_425886_) {
            super.setFocused(p_425886_);
            if (p_425886_ != null && this.minecraft.getLastInputType().isKeyboard()) {
                ScreenRectangle screenrectangle = this.getRectangle();
                ScreenRectangle screenrectangle1 = p_425886_.getRectangle();
                int i = screenrectangle1.top() - screenrectangle.top();
                int j = screenrectangle1.bottom() - screenrectangle.bottom();
                if (i < 0) {
                    this.setScrollAmount(this.scrollAmount() + i - 14.0);
                } else if (j > 0) {
                    this.setScrollAmount(this.scrollAmount() + j + 14.0);
                }
            }
        }

        @Override
        public void setX(int p_425593_) {
            super.setX(p_425593_);
            ScrollableLayout.this.content.setX(p_425593_ + 10);
        }

        @Override
        public void setY(int p_425526_) {
            super.setY(p_425526_);
            ScrollableLayout.this.content.setY(p_425526_ - (int)this.scrollAmount());
        }

        @Override
        public void setScrollAmount(double p_426096_) {
            super.setScrollAmount(p_426096_);
            ScrollableLayout.this.content.setY(this.getRectangle().top() - (int)this.scrollAmount());
        }

        @Override
        public List<? extends GuiEventListener> children() {
            return this.children;
        }

        @Override
        public Collection<? extends NarratableEntry> getNarratables() {
            return this.children;
        }
    }
}
