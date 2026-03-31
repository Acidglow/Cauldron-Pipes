package net.minecraft.client.gui.screens.advancements;

import com.google.common.collect.Maps;
import com.mojang.blaze3d.platform.cursor.CursorTypes;
import java.util.Map;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.advancements.AdvancementNode;
import net.minecraft.advancements.AdvancementProgress;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.layouts.HeaderAndFooterLayout;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.multiplayer.ClientAdvancements;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ServerboundSeenAdvancementsPacket;
import net.minecraft.resources.Identifier;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.jspecify.annotations.Nullable;

@OnlyIn(Dist.CLIENT)
public class AdvancementsScreen extends Screen implements ClientAdvancements.Listener {
    private static final Identifier WINDOW_LOCATION = Identifier.withDefaultNamespace("textures/gui/advancements/window.png");
    public static final int WINDOW_WIDTH = 252;
    public static final int WINDOW_HEIGHT = 140;
    private static final int WINDOW_INSIDE_X = 9;
    private static final int WINDOW_INSIDE_Y = 18;
    public static final int WINDOW_INSIDE_WIDTH = 234;
    public static final int WINDOW_INSIDE_HEIGHT = 113;
    private static final int WINDOW_TITLE_X = 8;
    private static final int WINDOW_TITLE_Y = 6;
    private static final int BACKGROUND_TEXTURE_WIDTH = 256;
    private static final int BACKGROUND_TEXTURE_HEIGHT = 256;
    public static final int BACKGROUND_TILE_WIDTH = 16;
    public static final int BACKGROUND_TILE_HEIGHT = 16;
    public static final int BACKGROUND_TILE_COUNT_X = 14;
    public static final int BACKGROUND_TILE_COUNT_Y = 7;
    private static final double SCROLL_SPEED = 16.0;
    private static final Component VERY_SAD_LABEL = Component.translatable("advancements.sad_label");
    private static final Component NO_ADVANCEMENTS_LABEL = Component.translatable("advancements.empty");
    private static final Component TITLE = Component.translatable("gui.advancements");
    private final HeaderAndFooterLayout layout = new HeaderAndFooterLayout(this);
    private final @Nullable Screen lastScreen;
    private final ClientAdvancements advancements;
    private final Map<AdvancementHolder, AdvancementTab> tabs = Maps.newLinkedHashMap();
    private @Nullable AdvancementTab selectedTab;
    private boolean isScrolling;
    private static int tabPage, maxPages;

    public AdvancementsScreen(ClientAdvancements advancements) {
        this(advancements, null);
    }

    public AdvancementsScreen(ClientAdvancements advancements, @Nullable Screen lastScreen) {
        super(TITLE);
        this.advancements = advancements;
        this.lastScreen = lastScreen;
    }

    @Override
    protected void init() {
        this.layout.addTitleHeader(TITLE, this.font);
        this.tabs.clear();
        this.selectedTab = null;
        this.advancements.setListener(this);
        if (this.selectedTab == null && !this.tabs.isEmpty()) {
            AdvancementTab advancementtab = this.tabs.values().iterator().next();
            this.advancements.setSelectedTab(advancementtab.getRootNode().holder(), true);
        } else {
            this.advancements.setSelectedTab(this.selectedTab == null ? null : this.selectedTab.getRootNode().holder(), true);
        }

        if (this.tabs.size() > AdvancementTabType.MAX_TABS) {
            int guiLeft = (this.width - 252) / 2;
            int guiTop = (this.height - 140) / 2;
            addRenderableWidget(net.minecraft.client.gui.components.Button.builder(Component.literal("<"), b -> tabPage = Math.max(tabPage - 1, 0         ))
                    .pos(guiLeft, guiTop - 50).size(20, 20).build());
            addRenderableWidget(net.minecraft.client.gui.components.Button.builder(Component.literal(">"), b -> tabPage = Math.min(tabPage + 1, maxPages))
                    .pos(guiLeft + WINDOW_WIDTH - 20, guiTop - 50).size(20, 20).build());
            maxPages = this.tabs.size() / AdvancementTabType.MAX_TABS;
        }

        this.layout.addToFooter(Button.builder(CommonComponents.GUI_DONE, p_331557_ -> this.onClose()).width(200).build());
        this.layout.visitWidgets(p_332019_ -> {
            AbstractWidget abstractwidget = this.addRenderableWidget(p_332019_);
        });
        this.repositionElements();
    }

    @Override
    protected void repositionElements() {
        this.layout.arrangeElements();
    }

    @Override
    public void onClose() {
        this.minecraft.setScreen(this.lastScreen);
    }

    @Override
    public void removed() {
        this.advancements.setListener(null);
        ClientPacketListener clientpacketlistener = this.minecraft.getConnection();
        if (clientpacketlistener != null) {
            clientpacketlistener.send(ServerboundSeenAdvancementsPacket.closedScreen());
        }
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent p_447309_, boolean p_433867_) {
        if (p_447309_.button() == 0) {
            int i = (this.width - 252) / 2;
            int j = (this.height - 140) / 2;

            for (AdvancementTab advancementtab : this.tabs.values()) {
                if (advancementtab.getPage() == tabPage && advancementtab.isMouseOver(i, j, p_447309_.x(), p_447309_.y())) {
                    this.advancements.setSelectedTab(advancementtab.getRootNode().holder(), true);
                    break;
                }
            }
        }

        return super.mouseClicked(p_447309_, p_433867_);
    }

    @Override
    public boolean keyPressed(KeyEvent p_445445_) {
        if (this.minecraft.options.keyAdvancements.matches(p_445445_)) {
            this.minecraft.setScreen(null);
            this.minecraft.mouseHandler.grabMouse();
            return true;
        } else {
            return super.keyPressed(p_445445_);
        }
    }

    @Override
    public void render(GuiGraphics p_282589_, int p_282255_, int p_283354_, float p_283123_) {
        super.render(p_282589_, p_282255_, p_283354_, p_283123_);
        int i = (this.width - 252) / 2;
        int j = (this.height - 140) / 2;
        if (maxPages != 0) {
            net.minecraft.network.chat.Component page = Component.literal(String.format("%d / %d", tabPage + 1, maxPages + 1));
            int width = this.font.width(page);
            p_282589_.drawString(this.font, page.getVisualOrderText(), i + (252 / 2) - (width / 2), j - 44, -1);
        }
        p_282589_.nextStratum();
        this.renderInside(p_282589_, i, j);
        p_282589_.nextStratum();
        this.renderWindow(p_282589_, i, j, p_282255_, p_283354_);
        if (this.isScrolling && this.selectedTab != null) {
            if (this.selectedTab.canScrollHorizontally() && this.selectedTab.canScrollVertically()) {
                p_282589_.requestCursor(CursorTypes.RESIZE_ALL);
            } else if (this.selectedTab.canScrollHorizontally()) {
                p_282589_.requestCursor(CursorTypes.RESIZE_EW);
            } else if (this.selectedTab.canScrollVertically()) {
                p_282589_.requestCursor(CursorTypes.RESIZE_NS);
            }
        }

        this.renderTooltips(p_282589_, p_282255_, p_283354_, i, j);
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent p_446146_, double p_97347_, double p_97348_) {
        if (p_446146_.button() != 0) {
            this.isScrolling = false;
            return false;
        } else {
            if (!this.isScrolling) {
                this.isScrolling = true;
            } else if (this.selectedTab != null) {
                this.selectedTab.scroll(p_97347_, p_97348_);
            }

            return true;
        }
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent p_460901_) {
        this.isScrolling = false;
        return super.mouseReleased(p_460901_);
    }

    @Override
    public boolean mouseScrolled(double p_295690_, double p_295286_, double p_295339_, double p_296270_) {
        if (this.selectedTab != null) {
            this.selectedTab.scroll(p_295339_ * 16.0, p_296270_ * 16.0);
            return true;
        } else {
            return false;
        }
    }

    private void renderInside(GuiGraphics guiGraphics, int x, int y) {
        AdvancementTab advancementtab = this.selectedTab;
        if (advancementtab == null) {
            guiGraphics.fill(x + 9, y + 18, x + 9 + 234, y + 18 + 113, -16777216);
            int i = x + 9 + 117;
            guiGraphics.drawCenteredString(this.font, NO_ADVANCEMENTS_LABEL, i, y + 18 + 56 - 9 / 2, -1);
            guiGraphics.drawCenteredString(this.font, VERY_SAD_LABEL, i, y + 18 + 113 - 9, -1);
        } else {
            advancementtab.drawContents(guiGraphics, x + 9, y + 18);
        }
    }

    public void renderWindow(GuiGraphics guiGraphics, int x, int y, int mouseX, int mouseY) {
        guiGraphics.blit(RenderPipelines.GUI_TEXTURED, WINDOW_LOCATION, x, y, 0.0F, 0.0F, 252, 140, 256, 256);
        if (this.tabs.size() > 1) {
            for (AdvancementTab advancementtab : this.tabs.values()) {
                if (advancementtab.getPage() == tabPage)
                advancementtab.drawTab(guiGraphics, x, y, mouseX, mouseY, advancementtab == this.selectedTab);
            }

            for (AdvancementTab advancementtab1 : this.tabs.values()) {
                if (advancementtab1.getPage() == tabPage)
                advancementtab1.drawIcon(guiGraphics, x, y);
            }
        }

        guiGraphics.drawString(this.font, this.selectedTab != null ? this.selectedTab.getTitle() : TITLE, x + 8, y + 6, -12566464, false);
    }

    private void renderTooltips(GuiGraphics guiGraphics, int mouseX, int mouseY, int offsetX, int offsetY) {
        if (this.selectedTab != null) {
            guiGraphics.pose().pushMatrix();
            guiGraphics.pose().translate(offsetX + 9, offsetY + 18);
            guiGraphics.nextStratum();
            this.selectedTab.drawTooltips(guiGraphics, mouseX - offsetX - 9, mouseY - offsetY - 18, offsetX, offsetY);
            guiGraphics.pose().popMatrix();
        }

        if (this.tabs.size() > 1) {
            for (AdvancementTab advancementtab : this.tabs.values()) {
                if (advancementtab.getPage() == tabPage && advancementtab.isMouseOver(offsetX, offsetY, (double)mouseX, (double)mouseY)) {
                    guiGraphics.setTooltipForNextFrame(this.font, advancementtab.getTitle(), mouseX, mouseY);
                }
            }
        }
    }

    @Override
    public void onAddAdvancementRoot(AdvancementNode p_301276_) {
        AdvancementTab advancementtab = AdvancementTab.create(this.minecraft, this, this.tabs.size(), p_301276_);
        if (advancementtab != null) {
            this.tabs.put(p_301276_.holder(), advancementtab);
        }
    }

    @Override
    public void onRemoveAdvancementRoot(AdvancementNode p_301028_) {
    }

    @Override
    public void onAddAdvancementTask(AdvancementNode p_301205_) {
        AdvancementTab advancementtab = this.getTab(p_301205_);
        if (advancementtab != null) {
            advancementtab.addAdvancement(p_301205_);
        }
    }

    @Override
    public void onRemoveAdvancementTask(AdvancementNode p_301004_) {
    }

    @Override
    public void onUpdateAdvancementProgress(AdvancementNode p_301161_, AdvancementProgress p_97369_) {
        AdvancementWidget advancementwidget = this.getAdvancementWidget(p_301161_);
        if (advancementwidget != null) {
            advancementwidget.setProgress(p_97369_);
        }
    }

    @Override
    public void onSelectedTabChanged(@Nullable AdvancementHolder p_301084_) {
        this.selectedTab = this.tabs.get(p_301084_);
    }

    @Override
    public void onAdvancementsCleared() {
        this.tabs.clear();
        this.selectedTab = null;
    }

    public @Nullable AdvancementWidget getAdvancementWidget(AdvancementNode advancement) {
        AdvancementTab advancementtab = this.getTab(advancement);
        return advancementtab == null ? null : advancementtab.getWidget(advancement.holder());
    }

    private @Nullable AdvancementTab getTab(AdvancementNode advancement) {
        AdvancementNode advancementnode = advancement.root();
        return this.tabs.get(advancementnode.holder());
    }
}
