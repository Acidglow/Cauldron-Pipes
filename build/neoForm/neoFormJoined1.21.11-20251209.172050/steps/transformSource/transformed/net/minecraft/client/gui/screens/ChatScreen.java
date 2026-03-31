package net.minecraft.client.gui.screens;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.ActiveTextCollector;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.ChatComponent;
import net.minecraft.client.gui.components.CommandSuggestions;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.narration.NarratedElementType;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.multiplayer.chat.ChatListener;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;
import net.minecraft.util.StringUtil;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.apache.commons.lang3.StringUtils;
import org.jspecify.annotations.Nullable;

@OnlyIn(Dist.CLIENT)
public class ChatScreen extends Screen {
    public static final double MOUSE_SCROLL_SPEED = 7.0;
    private static final Component USAGE_TEXT = Component.translatable("chat_screen.usage");
    private String historyBuffer = "";
    /**
     * keeps position of which chat message you will select when you press up, (does not increase for duplicated messages sent immediately after each other)
     */
    private int historyPos = -1;
    /**
     * Chat entry field
     */
    protected EditBox input;
    /**
     * is the text that appears when you press the chat key and the input box appears pre-filled
     */
    protected String initial;
    protected boolean isDraft;
    protected ChatScreen.ExitReason exitReason = ChatScreen.ExitReason.INTERRUPTED;
    CommandSuggestions commandSuggestions;

    public ChatScreen(String initial, boolean isDraft) {
        super(Component.translatable("chat_screen.title"));
        this.initial = initial;
        this.isDraft = isDraft;
    }

    @Override
    protected void init() {
        this.historyPos = this.minecraft.gui.getChat().getRecentChat().size();
        this.input = new EditBox(this.minecraft.fontFilterFishy, 4, this.height - 12, this.width - 4, 12, Component.translatable("chat.editBox")) {
            @Override
            protected MutableComponent createNarrationMessage() {
                return super.createNarrationMessage().append(ChatScreen.this.commandSuggestions.getNarrationMessage());
            }
        };
        this.input.setMaxLength(256);
        this.input.setBordered(false);
        this.input.setValue(this.initial);
        this.input.setResponder(this::onEdited);
        this.input.addFormatter(this::formatChat);
        this.input.setCanLoseFocus(false);
        this.addRenderableWidget(this.input);
        this.commandSuggestions = new CommandSuggestions(this.minecraft, this, this.input, this.font, false, false, 1, 10, true, -805306368);
        this.commandSuggestions.setAllowHiding(false);
        this.commandSuggestions.setAllowSuggestions(false);
        this.commandSuggestions.updateCommandInfo();
    }

    @Override
    protected void setInitialFocus() {
        this.setInitialFocus(this.input);
    }

    @Override
    public void resize(int p_95601_, int p_95602_) {
        this.initial = this.input.getValue();
        this.init(p_95601_, p_95602_);
    }

    @Override
    public void onClose() {
        this.exitReason = ChatScreen.ExitReason.INTENTIONAL;
        super.onClose();
    }

    @Override
    public void removed() {
        this.minecraft.gui.getChat().resetChatScroll();
        this.initial = this.input.getValue();
        if (this.shouldDiscardDraft() || StringUtils.isBlank(this.initial)) {
            this.minecraft.gui.getChat().discardDraft();
        } else if (!this.isDraft) {
            this.minecraft.gui.getChat().saveAsDraft(this.initial);
        }
    }

    protected boolean shouldDiscardDraft() {
        return this.exitReason != ChatScreen.ExitReason.INTERRUPTED
            && (this.exitReason != ChatScreen.ExitReason.INTENTIONAL || !this.minecraft.options.saveChatDrafts().get());
    }

    private void onEdited(String value) {
        this.commandSuggestions.setAllowSuggestions(true);
        this.commandSuggestions.updateCommandInfo();
        this.isDraft = false;
    }

    @Override
    public boolean keyPressed(KeyEvent p_446285_) {
        if (this.commandSuggestions.keyPressed(p_446285_)) {
            return true;
        } else if (this.isDraft && p_446285_.key() == 259) {
            this.input.setValue("");
            this.isDraft = false;
            return true;
        } else if (super.keyPressed(p_446285_)) {
            return true;
        } else if (p_446285_.isConfirmation()) {
            this.handleChatInput(this.input.getValue(), true);
            this.exitReason = ChatScreen.ExitReason.DONE;
            // FORGE: Prevent closing the screen if another screen has been opened.
            if (minecraft.screen == this) {
                this.minecraft.setScreen(null);
            }
            return true;
        } else {
            switch (p_446285_.key()) {
                case 264:
                    this.moveInHistory(1);
                    break;
                case 265:
                    this.moveInHistory(-1);
                    break;
                case 266:
                    this.minecraft.gui.getChat().scrollChat(this.minecraft.gui.getChat().getLinesPerPage() - 1);
                    break;
                case 267:
                    this.minecraft.gui.getChat().scrollChat(-this.minecraft.gui.getChat().getLinesPerPage() + 1);
                    break;
                default:
                    return false;
            }

            return true;
        }
    }

    @Override
    public boolean mouseScrolled(double p_95581_, double p_95582_, double p_95583_, double p_295977_) {
        p_295977_ = Mth.clamp(p_295977_, -1.0, 1.0);
        if (this.commandSuggestions.mouseScrolled(p_295977_)) {
            return true;
        } else {
            if (!this.minecraft.hasShiftDown()) {
                p_295977_ *= 7.0;
            }

            this.minecraft.gui.getChat().scrollChat((int)p_295977_);
            return true;
        }
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent p_446006_, boolean p_434924_) {
        if (this.commandSuggestions.mouseClicked(p_446006_)) {
            return true;
        } else {
            if (p_446006_.button() == 0) {
                int i = this.minecraft.getWindow().getGuiScaledHeight();
                ActiveTextCollector.ClickableStyleFinder activetextcollector$clickablestylefinder = new ActiveTextCollector.ClickableStyleFinder(
                        this.getFont(), (int)p_446006_.x(), (int)p_446006_.y()
                    )
                    .includeInsertions(this.insertionClickMode());
                this.minecraft.gui.getChat().captureClickableText(activetextcollector$clickablestylefinder, i, this.minecraft.gui.getGuiTicks(), true);
                Style style = activetextcollector$clickablestylefinder.result();
                if (style != null && this.handleComponentClicked(style, this.insertionClickMode())) {
                    this.initial = this.input.getValue();
                    return true;
                }
            }

            return super.mouseClicked(p_446006_, p_434924_);
        }
    }

    private boolean insertionClickMode() {
        return this.minecraft.hasShiftDown();
    }

    private boolean handleComponentClicked(Style style, boolean insert) {
        ClickEvent clickevent = style.getClickEvent();
        if (insert) {
            if (style.getInsertion() != null) {
                this.insertText(style.getInsertion(), false);
            }
        } else if (clickevent != null) {
            if (clickevent instanceof ClickEvent.Custom clickevent$custom && clickevent$custom.id().equals(ChatComponent.QUEUE_EXPAND_ID)) {
                ChatListener chatlistener = this.minecraft.getChatListener();
                if (chatlistener.queueSize() != 0L) {
                    chatlistener.acceptNextDelayedMessage();
                }
            } else {
                defaultHandleGameClickEvent(clickevent, this.minecraft, this);
            }

            return true;
        }

        return false;
    }

    @Override
    public void insertText(String text, boolean overwrite) {
        if (overwrite) {
            this.input.setValue(text);
        } else {
            this.input.insertText(text);
        }
    }

    /**
     * Input is relative and is applied directly to the sentHistoryCursor so -1 is the previous message, 1 is the next message from the current cursor position.
     */
    public void moveInHistory(int msgPos) {
        int i = this.historyPos + msgPos;
        int j = this.minecraft.gui.getChat().getRecentChat().size();
        i = Mth.clamp(i, 0, j);
        if (i != this.historyPos) {
            if (i == j) {
                this.historyPos = j;
                this.input.setValue(this.historyBuffer);
            } else {
                if (this.historyPos == j) {
                    this.historyBuffer = this.input.getValue();
                }

                this.input.setValue(this.minecraft.gui.getChat().getRecentChat().get(i));
                this.commandSuggestions.setAllowSuggestions(false);
                this.historyPos = i;
            }
        }
    }

    private @Nullable FormattedCharSequence formatChat(String text, int displayPos) {
        return this.isDraft ? FormattedCharSequence.forward(text, Style.EMPTY.withColor(ChatFormatting.GRAY).withItalic(true)) : null;
    }

    @Override
    public void render(GuiGraphics p_282470_, int p_282674_, int p_282014_, float p_283132_) {
        p_282470_.fill(2, this.height - 14, this.width - 2, this.height - 2, this.minecraft.options.getBackgroundColor(Integer.MIN_VALUE));
        this.minecraft.gui.getChat().render(p_282470_, this.font, this.minecraft.gui.getGuiTicks(), p_282674_, p_282014_, true, this.insertionClickMode());
        super.render(p_282470_, p_282674_, p_282014_, p_283132_);
        this.commandSuggestions.render(p_282470_, p_282674_, p_282014_);
    }

    @Override
    public void renderBackground(GuiGraphics p_295929_, int p_296130_, int p_296353_, float p_294668_) {
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public boolean isAllowedInPortal() {
        return true;
    }

    @Override
    protected void updateNarrationState(NarrationElementOutput p_169238_) {
        p_169238_.add(NarratedElementType.TITLE, this.getTitle());
        p_169238_.add(NarratedElementType.USAGE, USAGE_TEXT);
        String s = this.input.getValue();
        if (!s.isEmpty()) {
            p_169238_.nest().add(NarratedElementType.TITLE, Component.translatable("chat_screen.message", s));
        }
    }

    public void handleChatInput(String message, boolean addToRecentChat) {
        message = this.normalizeChatMessage(message);
        if (!message.isEmpty()) {
            if (addToRecentChat) {
                this.minecraft.gui.getChat().addRecentChat(message);
            }

            if (message.startsWith("/")) {
                this.minecraft.player.connection.sendCommand(message.substring(1));
            } else {
                this.minecraft.player.connection.sendChat(message);
            }
        }
    }

    public String normalizeChatMessage(String message) {
        return StringUtil.trimChatMessage(StringUtils.normalizeSpace(message.trim()));
    }

    @FunctionalInterface
    @OnlyIn(Dist.CLIENT)
    public interface ChatConstructor<T extends ChatScreen> {
        T create(String initial, boolean isDraft);
    }

    @OnlyIn(Dist.CLIENT)
    protected static enum ExitReason {
        INTENTIONAL,
        INTERRUPTED,
        DONE;
    }
}
