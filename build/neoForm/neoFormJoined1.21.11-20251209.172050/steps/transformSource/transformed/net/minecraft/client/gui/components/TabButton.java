package net.minecraft.client.gui.components;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ActiveTextCollector;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.tabs.Tab;
import net.minecraft.client.gui.components.tabs.TabManager;
import net.minecraft.client.gui.narration.NarratedElementType;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.sounds.SoundManager;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class TabButton extends AbstractWidget.WithInactiveMessage {
    private static final WidgetSprites SPRITES = new WidgetSprites(
        Identifier.withDefaultNamespace("widget/tab_selected"),
        Identifier.withDefaultNamespace("widget/tab"),
        Identifier.withDefaultNamespace("widget/tab_selected_highlighted"),
        Identifier.withDefaultNamespace("widget/tab_highlighted")
    );
    private static final int SELECTED_OFFSET = 3;
    private static final int TEXT_MARGIN = 1;
    private static final int UNDERLINE_HEIGHT = 1;
    private static final int UNDERLINE_MARGIN_X = 4;
    private static final int UNDERLINE_MARGIN_BOTTOM = 2;
    private final TabManager tabManager;
    private final Tab tab;

    public TabButton(TabManager tabManager, Tab tab, int width, int height) {
        super(0, 0, width, height, tab.getTabTitle());
        this.tabManager = tabManager;
        this.tab = tab;
    }

    @Override
    public void renderWidget(GuiGraphics p_283350_, int p_283437_, int p_281595_, float p_282117_) {
        p_283350_.blitSprite(
            RenderPipelines.GUI_TEXTURED, SPRITES.get(this.isSelected(), this.isHoveredOrFocused()), this.getX(), this.getY(), this.width, this.height
        );
        Font font = Minecraft.getInstance().font;
        int i = this.active ? -1 : -6250336;
        if (this.isSelected()) {
            this.renderMenuBackground(p_283350_, this.getX() + 2, this.getY() + 2, this.getRight() - 2, this.getBottom());
            this.renderFocusUnderline(p_283350_, font, i);
        }

        this.renderLabel(p_283350_.textRendererForWidget(this, GuiGraphics.HoveredTextEffects.NONE));
        this.handleCursor(p_283350_);
    }

    protected void renderMenuBackground(GuiGraphics guiGraphics, int minX, int minY, int maxX, int maxY) {
        Screen.renderMenuBackgroundTexture(guiGraphics, Screen.MENU_BACKGROUND, minX, minY, 0.0F, 0.0F, maxX - minX, maxY - minY);
    }

    private void renderLabel(ActiveTextCollector activeTextCollector) {
        int i = this.getX() + 1;
        int j = this.getY() + (this.isSelected() ? 0 : 3);
        int k = this.getX() + this.getWidth() - 1;
        int l = this.getY() + this.getHeight();
        activeTextCollector.acceptScrollingWithDefaultCenter(this.getMessage(), i, k, j, l);
    }

    private void renderFocusUnderline(GuiGraphics guiGraphics, Font font, int color) {
        int i = Math.min(font.width(this.getMessage()), this.getWidth() - 4);
        int j = this.getX() + (this.getWidth() - i) / 2;
        int k = this.getY() + this.getHeight() - 2;
        guiGraphics.fill(j, k, j + i, k + 1, color);
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput p_275465_) {
        p_275465_.add(NarratedElementType.TITLE, Component.translatable("gui.narrate.tab", this.tab.getTabTitle()));
        p_275465_.add(NarratedElementType.HINT, this.tab.getTabExtraNarration());
    }

    @Override
    public void playDownSound(SoundManager p_276302_) {
    }

    public Tab tab() {
        return this.tab;
    }

    public boolean isSelected() {
        return this.tabManager.getCurrentTab() == this.tab;
    }
}
