package net.minecraft.client.gui.components.tabs;

import com.google.common.collect.ImmutableList;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import net.minecraft.client.gui.ComponentPath;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.gui.components.TabButton;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.components.events.AbstractContainerEventHandler;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.layouts.LinearLayout;
import net.minecraft.client.gui.narration.NarratableEntry;
import net.minecraft.client.gui.narration.NarratedElementType;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.navigation.FocusNavigationEvent;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.jspecify.annotations.Nullable;

@OnlyIn(Dist.CLIENT)
public class TabNavigationBar extends AbstractContainerEventHandler implements Renderable, NarratableEntry {
    private static final int NO_TAB = -1;
    private static final int MAX_WIDTH = 400;
    private static final int HEIGHT = 24;
    private static final int MARGIN = 14;
    private static final Component USAGE_NARRATION = Component.translatable("narration.tab_navigation.usage");
    private final LinearLayout layout = LinearLayout.horizontal();
    private int width;
    private final TabManager tabManager;
    private final ImmutableList<Tab> tabs;
    private final ImmutableList<TabButton> tabButtons;

    TabNavigationBar(int width, TabManager tabManager, Iterable<Tab> tabs) {
        this.width = width;
        this.tabManager = tabManager;
        this.tabs = ImmutableList.copyOf(tabs);
        this.layout.defaultCellSetting().alignHorizontallyCenter();
        ImmutableList.Builder<TabButton> builder = ImmutableList.builder();

        for (Tab tab : tabs) {
            builder.add(this.layout.addChild(new TabButton(tabManager, tab, 0, 24)));
        }

        this.tabButtons = builder.build();
    }

    public static TabNavigationBar.Builder builder(TabManager tabManager, int width) {
        return new TabNavigationBar.Builder(tabManager, width);
    }

    public void setWidth(int width) {
        this.width = width;
    }

    @Override
    public boolean isMouseOver(double p_387504_, double p_386568_) {
        return p_387504_ >= this.layout.getX()
            && p_386568_ >= this.layout.getY()
            && p_387504_ < this.layout.getX() + this.layout.getWidth()
            && p_386568_ < this.layout.getY() + this.layout.getHeight();
    }

    /**
     * Sets the focus state of the GUI element.
     */
    @Override
    public void setFocused(boolean p_275488_) {
        super.setFocused(p_275488_);
        if (this.getFocused() != null) {
            this.setFocused(null);
        }
    }

    /**
     * Sets the focus state of the GUI element.
     */
    @Override
    public void setFocused(@Nullable GuiEventListener p_275675_) {
        super.setFocused(p_275675_);
        if (p_275675_ instanceof TabButton tabbutton && tabbutton.isActive()) {
            this.tabManager.setCurrentTab(tabbutton.tab(), true);
        }
    }

    /**
     * Retrieves the next focus path based on the given focus navigation event.
     * <p>
     * @return the next focus path as a ComponentPath, or {@code null} if there is no next focus path.
     */
    @Override
    public @Nullable ComponentPath nextFocusPath(FocusNavigationEvent p_275418_) {
        if (!this.isFocused()) {
            TabButton tabbutton = this.currentTabButton();
            if (tabbutton != null) {
                return ComponentPath.path(this, ComponentPath.leaf(tabbutton));
            }
        }

        return p_275418_ instanceof FocusNavigationEvent.TabNavigation ? null : super.nextFocusPath(p_275418_);
    }

    @Override
    public List<? extends GuiEventListener> children() {
        return this.tabButtons;
    }

    public List<Tab> getTabs() {
        return this.tabs;
    }

    @Override
    public NarratableEntry.NarrationPriority narrationPriority() {
        return this.tabButtons.stream().map(AbstractWidget::narrationPriority).max(Comparator.naturalOrder()).orElse(NarratableEntry.NarrationPriority.NONE);
    }

    /**
     * Updates the narration output with the current narration information.
     */
    @Override
    public void updateNarration(NarrationElementOutput p_275583_) {
        Optional<TabButton> optional = this.tabButtons
            .stream()
            .filter(AbstractWidget::isHovered)
            .findFirst()
            .or(() -> Optional.ofNullable(this.currentTabButton()));
        optional.ifPresent(p_274663_ -> {
            this.narrateListElementPosition(p_275583_.nest(), p_274663_);
            p_274663_.updateNarration(p_275583_);
        });
        if (this.isFocused()) {
            p_275583_.add(NarratedElementType.USAGE, USAGE_NARRATION);
        }
    }

    /**
     * Narrates the position of a list element (tab button).
     *
     * @param narrationElementOutput the narration output to update.
     * @param tabButton              the tab button whose position is being narrated.
     */
    protected void narrateListElementPosition(NarrationElementOutput narrationElementOutput, TabButton tabButton) {
        if (this.tabs.size() > 1) {
            int i = this.tabButtons.indexOf(tabButton);
            if (i != -1) {
                narrationElementOutput.add(NarratedElementType.POSITION, Component.translatable("narrator.position.tab", i + 1, this.tabs.size()));
            }
        }
    }

    /**
     * Renders the graphical user interface (GUI) element.
     */
    @Override
    public void render(GuiGraphics p_281720_, int p_282085_, int p_281687_, float p_283048_) {
        p_281720_.blit(
            RenderPipelines.GUI_TEXTURED,
            Screen.HEADER_SEPARATOR,
            0,
            this.layout.getY() + this.layout.getHeight() - 2,
            0.0F,
            0.0F,
            this.tabButtons.get(0).getX(),
            2,
            32,
            2
        );
        int i = this.tabButtons.get(this.tabButtons.size() - 1).getRight();
        p_281720_.blit(
            RenderPipelines.GUI_TEXTURED, Screen.HEADER_SEPARATOR, i, this.layout.getY() + this.layout.getHeight() - 2, 0.0F, 0.0F, this.width, 2, 32, 2
        );

        for (TabButton tabbutton : this.tabButtons) {
            tabbutton.render(p_281720_, p_282085_, p_281687_, p_283048_);
        }
    }

    @Override
    public ScreenRectangle getRectangle() {
        return this.layout.getRectangle();
    }

    public void arrangeElements() {
        int i = Math.min(400, this.width) - 28;
        int j = Mth.roundToward(i / this.tabs.size(), 2);

        for (TabButton tabbutton : this.tabButtons) {
            tabbutton.setWidth(j);
        }

        this.layout.arrangeElements();
        this.layout.setX(Mth.roundToward((this.width - i) / 2, 2));
        this.layout.setY(0);
    }

    /**
     * Selects the tab at the specified index.
     *
     * @param index          the index of the tab to select.
     * @param playClickSound whether to play a click sound when selecting the tab.
     */
    public void selectTab(int index, boolean playClickSound) {
        if (this.isFocused()) {
            this.setFocused(this.tabButtons.get(index));
        } else if (this.tabButtons.get(index).isActive()) {
            this.tabManager.setCurrentTab(this.tabs.get(index), playClickSound);
        }
    }

    public void setTabActiveState(int index, boolean active) {
        if (index >= 0 && index < this.tabButtons.size()) {
            this.tabButtons.get(index).active = active;
        }
    }

    public void setTabTooltip(int index, @Nullable Tooltip tooltip) {
        if (index >= 0 && index < this.tabButtons.size()) {
            this.tabButtons.get(index).setTooltip(tooltip);
        }
    }

    @Override
    public boolean keyPressed(KeyEvent p_445376_) {
        if (p_445376_.hasControlDownWithQuirk()) {
            int i = this.getNextTabIndex(p_445376_);
            if (i != -1) {
                this.selectTab(Mth.clamp(i, 0, this.tabs.size() - 1), true);
                return true;
            }
        }

        return false;
    }

    private int getNextTabIndex(KeyEvent event) {
        return this.getNextTabIndex(this.currentTabIndex(), event);
    }

    private int getNextTabIndex(int tabIndex, KeyEvent event) {
        int i = event.getDigit();
        if (i != -1) {
            return Math.floorMod(i - 1, 10);
        } else if (event.isCycleFocus() && tabIndex != -1) {
            int j = event.hasShiftDown() ? tabIndex - 1 : tabIndex + 1;
            int k = Math.floorMod(j, this.tabs.size());
            return this.tabButtons.get(k).active ? k : this.getNextTabIndex(k, event);
        } else {
            return -1;
        }
    }

    private int currentTabIndex() {
        Tab tab = this.tabManager.getCurrentTab();
        int i = this.tabs.indexOf(tab);
        return i != -1 ? i : -1;
    }

    private @Nullable TabButton currentTabButton() {
        int i = this.currentTabIndex();
        return i != -1 ? this.tabButtons.get(i) : null;
    }

    /**
     * Builder class for creating a TabNavigationBar instance.
     */
    @OnlyIn(Dist.CLIENT)
    public static class Builder {
        private final int width;
        private final TabManager tabManager;
        private final List<Tab> tabs = new ArrayList<>();

        Builder(TabManager tabManager, int width) {
            this.tabManager = tabManager;
            this.width = width;
        }

        /**
         * Adds multiple tabs to the TabNavigationBar.
         * <p>
         * @return the {@link Builder} instance.
         *
         * @param tabs the tabs to add.
         */
        public TabNavigationBar.Builder addTabs(Tab... tabs) {
            Collections.addAll(this.tabs, tabs);
            return this;
        }

        public TabNavigationBar build() {
            return new TabNavigationBar(this.width, this.tabManager, this.tabs);
        }
    }
}
