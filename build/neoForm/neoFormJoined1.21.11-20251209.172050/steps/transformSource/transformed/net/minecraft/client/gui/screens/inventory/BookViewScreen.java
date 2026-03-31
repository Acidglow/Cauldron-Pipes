package net.minecraft.client.gui.screens.inventory;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ActiveTextCollector;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.TextAlignment;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentUtils;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.Identifier;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.WritableBookContent;
import net.minecraft.world.item.component.WrittenBookContent;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.jspecify.annotations.Nullable;

@OnlyIn(Dist.CLIENT)
public class BookViewScreen extends Screen {
    public static final int PAGE_INDICATOR_TEXT_Y_OFFSET = 16;
    public static final int PAGE_TEXT_X_OFFSET = 36;
    public static final int PAGE_TEXT_Y_OFFSET = 30;
    private static final int BACKGROUND_TEXTURE_WIDTH = 256;
    private static final int BACKGROUND_TEXTURE_HEIGHT = 256;
    private static final Component TITLE = Component.translatable("book.view.title");
    private static final Style PAGE_TEXT_STYLE = Style.EMPTY.withoutShadow().withColor(-16777216);
    public static final BookViewScreen.BookAccess EMPTY_ACCESS = new BookViewScreen.BookAccess(List.of());
    public static final Identifier BOOK_LOCATION = Identifier.withDefaultNamespace("textures/gui/book.png");
    protected static final int TEXT_WIDTH = 114;
    protected static final int TEXT_HEIGHT = 128;
    protected static final int IMAGE_WIDTH = 192;
    private static final int PAGE_INDICATOR_X_OFFSET = 148;
    protected static final int IMAGE_HEIGHT = 192;
    private static final int PAGE_BUTTON_Y = 157;
    private static final int PAGE_BACK_BUTTON_X = 43;
    private static final int PAGE_FORWARD_BUTTON_X = 116;
    private BookViewScreen.BookAccess bookAccess;
    private int currentPage;
    /**
     * Holds a copy of the page text, split into page width lines
     */
    private List<FormattedCharSequence> cachedPageComponents = Collections.emptyList();
    private int cachedPage = -1;
    private Component pageMsg = CommonComponents.EMPTY;
    private PageButton forwardButton;
    private PageButton backButton;
    /**
     * Determines if a sound is played when the page is turned
     */
    private final boolean playTurnSound;

    public BookViewScreen(BookViewScreen.BookAccess bookAccess) {
        this(bookAccess, true);
    }

    public BookViewScreen() {
        this(EMPTY_ACCESS, false);
    }

    private BookViewScreen(BookViewScreen.BookAccess bookAccess, boolean playTurnSound) {
        super(TITLE);
        this.bookAccess = bookAccess;
        this.playTurnSound = playTurnSound;
    }

    public void setBookAccess(BookViewScreen.BookAccess bookAccess) {
        this.bookAccess = bookAccess;
        this.currentPage = Mth.clamp(this.currentPage, 0, bookAccess.getPageCount());
        this.updateButtonVisibility();
        this.cachedPage = -1;
    }

    /**
     * Moves the book to the specified page and returns true if it exists, {@code false} otherwise.
     */
    public boolean setPage(int pageNum) {
        int i = Mth.clamp(pageNum, 0, this.bookAccess.getPageCount() - 1);
        if (i != this.currentPage) {
            this.currentPage = i;
            this.updateButtonVisibility();
            this.cachedPage = -1;
            return true;
        } else {
            return false;
        }
    }

    /**
     * I'm not sure why this exists. The function it calls is public and does all the work.
     */
    protected boolean forcePage(int pageNum) {
        return this.setPage(pageNum);
    }

    @Override
    protected void init() {
        this.createMenuControls();
        this.createPageControlButtons();
    }

    @Override
    public Component getNarrationMessage() {
        return CommonComponents.joinLines(super.getNarrationMessage(), this.getPageNumberMessage(), this.bookAccess.getPage(this.currentPage));
    }

    private Component getPageNumberMessage() {
        return Component.translatable("book.pageIndicator", this.currentPage + 1, Math.max(this.getNumPages(), 1)).withStyle(PAGE_TEXT_STYLE);
    }

    protected void createMenuControls() {
        this.addRenderableWidget(
            Button.builder(CommonComponents.GUI_DONE, p_432224_ -> this.onClose()).pos((this.width - 200) / 2, this.menuControlsTop()).width(200).build()
        );
    }

    protected void createPageControlButtons() {
        int i = this.backgroundLeft();
        int j = this.backgroundTop();
        this.forwardButton = this.addRenderableWidget(new PageButton(i + 116, j + 157, true, p_98297_ -> this.pageForward(), this.playTurnSound));
        this.backButton = this.addRenderableWidget(new PageButton(i + 43, j + 157, false, p_98287_ -> this.pageBack(), this.playTurnSound));
        this.updateButtonVisibility();
    }

    private int getNumPages() {
        return this.bookAccess.getPageCount();
    }

    protected void pageBack() {
        if (this.currentPage > 0) {
            this.currentPage--;
        }

        this.updateButtonVisibility();
    }

    protected void pageForward() {
        if (this.currentPage < this.getNumPages() - 1) {
            this.currentPage++;
        }

        this.updateButtonVisibility();
    }

    private void updateButtonVisibility() {
        this.forwardButton.visible = this.currentPage < this.getNumPages() - 1;
        this.backButton.visible = this.currentPage > 0;
    }

    @Override
    public boolean keyPressed(KeyEvent p_445531_) {
        if (super.keyPressed(p_445531_)) {
            return true;
        } else {
            return switch (p_445531_.key()) {
                case 266 -> {
                    this.backButton.onPress(p_445531_);
                    yield true;
                }
                case 267 -> {
                    this.forwardButton.onPress(p_445531_);
                    yield true;
                }
                default -> false;
            };
        }
    }

    @Override
    public void render(GuiGraphics p_281997_, int p_281262_, int p_283321_, float p_282251_) {
        super.render(p_281997_, p_281262_, p_283321_, p_282251_);
        this.visitText(p_281997_.textRenderer(GuiGraphics.HoveredTextEffects.TOOLTIP_AND_CURSOR), false);
    }

    private void visitText(ActiveTextCollector activeTextCollector, boolean contentOnly) {
        if (this.cachedPage != this.currentPage) {
            FormattedText formattedtext = ComponentUtils.mergeStyles(this.bookAccess.getPage(this.currentPage), PAGE_TEXT_STYLE);
            this.cachedPageComponents = this.font.split(formattedtext, 114);
            this.pageMsg = this.getPageNumberMessage();
            this.cachedPage = this.currentPage;
        }

        int l = this.backgroundLeft();
        int i = this.backgroundTop();
        if (!contentOnly) {
            activeTextCollector.accept(TextAlignment.RIGHT, l + 148, i + 16, this.pageMsg);
        }

        int j = Math.min(128 / 9, this.cachedPageComponents.size());

        for (int k = 0; k < j; k++) {
            FormattedCharSequence formattedcharsequence = this.cachedPageComponents.get(k);
            activeTextCollector.accept(l + 36, i + 30 + k * 9, formattedcharsequence);
        }
    }

    @Override
    public void renderBackground(GuiGraphics p_295678_, int p_296491_, int p_294260_, float p_294869_) {
        super.renderBackground(p_295678_, p_296491_, p_294260_, p_294869_);
        p_295678_.blit(RenderPipelines.GUI_TEXTURED, BOOK_LOCATION, this.backgroundLeft(), this.backgroundTop(), 0.0F, 0.0F, 192, 192, 256, 256);
    }

    private int backgroundLeft() {
        return (this.width - 192) / 2;
    }

    private int backgroundTop() {
        return 2;
    }

    protected int menuControlsTop() {
        return this.backgroundTop() + 192 + 2;
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent p_446030_, boolean p_434370_) {
        if (p_446030_.button() == 0) {
            ActiveTextCollector.ClickableStyleFinder activetextcollector$clickablestylefinder = new ActiveTextCollector.ClickableStyleFinder(
                this.font, (int)p_446030_.x(), (int)p_446030_.y()
            );
            this.visitText(activetextcollector$clickablestylefinder, true);
            Style style = activetextcollector$clickablestylefinder.result();
            if (style != null && this.handleClickEvent(style.getClickEvent())) {
                return true;
            }
        }

        return super.mouseClicked(p_446030_, p_434370_);
    }

    protected boolean handleClickEvent(@Nullable ClickEvent clickEvent) {
        if (clickEvent == null) {
            return false;
        } else {
            LocalPlayer localplayer = Objects.requireNonNull(this.minecraft.player, "Player not available");
            switch (clickEvent) {
                case ClickEvent.ChangePage(int i):
                    this.forcePage(i - 1);
                    break;
                case ClickEvent.RunCommand(String s):
                    this.closeContainerOnServer();
                    clickCommandAction(localplayer, s, null);
                    break;
                default:
                    defaultHandleGameClickEvent(clickEvent, this.minecraft, this);
            }

            return true;
        }
    }

    protected void closeContainerOnServer() {
    }

    @Override
    public boolean isInGameUi() {
        return true;
    }

    @OnlyIn(Dist.CLIENT)
    public record BookAccess(List<Component> pages) {
        public int getPageCount() {
            return this.pages.size();
        }

        public Component getPage(int page) {
            return page >= 0 && page < this.getPageCount() ? this.pages.get(page) : CommonComponents.EMPTY;
        }

        public static BookViewScreen.@Nullable BookAccess fromItem(ItemStack stack) {
            boolean flag = Minecraft.getInstance().isTextFilteringEnabled();
            WrittenBookContent writtenbookcontent = stack.get(DataComponents.WRITTEN_BOOK_CONTENT);
            if (writtenbookcontent != null) {
                return new BookViewScreen.BookAccess(writtenbookcontent.getPages(flag));
            } else {
                WritableBookContent writablebookcontent = stack.get(DataComponents.WRITABLE_BOOK_CONTENT);
                return writablebookcontent != null ? new BookViewScreen.BookAccess(writablebookcontent.getPages(flag).<Component>map(Component::literal).toList()) : null;
            }
        }
    }
}
