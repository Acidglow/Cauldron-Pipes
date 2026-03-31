package net.minecraft.client.gui.screens.reporting;

import com.mojang.authlib.GameProfile;
import com.mojang.authlib.minecraft.report.AbuseReportLimits;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.function.Supplier;
import net.minecraft.Optionull;
import net.minecraft.client.GuiMessageTag;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ActiveTextCollector;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.TextAlignment;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.MultiLineLabel;
import net.minecraft.client.gui.components.ObjectSelectionList;
import net.minecraft.client.gui.components.PlayerFaceRenderer;
import net.minecraft.client.gui.navigation.ScreenDirection;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.multiplayer.chat.ChatTrustLevel;
import net.minecraft.client.multiplayer.chat.LoggedChatMessage;
import net.minecraft.client.multiplayer.chat.report.ChatReport;
import net.minecraft.client.multiplayer.chat.report.ReportingContext;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.locale.Language;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.resources.Identifier;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.PlayerSkin;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.jspecify.annotations.Nullable;

@OnlyIn(Dist.CLIENT)
public class ChatSelectionScreen extends Screen {
    static final Identifier CHECKMARK_SPRITE = Identifier.withDefaultNamespace("icon/checkmark");
    private static final Component TITLE = Component.translatable("gui.chatSelection.title");
    private static final Component CONTEXT_INFO = Component.translatable("gui.chatSelection.context");
    private final @Nullable Screen lastScreen;
    private final ReportingContext reportingContext;
    private Button confirmSelectedButton;
    private MultiLineLabel contextInfoLabel;
    private ChatSelectionScreen.@Nullable ChatSelectionList chatSelectionList;
    final ChatReport.Builder report;
    private final Consumer<ChatReport.Builder> onSelected;
    private ChatSelectionLogFiller chatLogFiller;

    public ChatSelectionScreen(@Nullable Screen lastScreen, ReportingContext reportingContext, ChatReport.Builder report, Consumer<ChatReport.Builder> onSelected) {
        super(TITLE);
        this.lastScreen = lastScreen;
        this.reportingContext = reportingContext;
        this.report = report.copy();
        this.onSelected = onSelected;
    }

    @Override
    protected void init() {
        this.chatLogFiller = new ChatSelectionLogFiller(this.reportingContext, this::canReport);
        this.contextInfoLabel = MultiLineLabel.create(this.font, CONTEXT_INFO, this.width - 16);
        this.chatSelectionList = this.addRenderableWidget(
            new ChatSelectionScreen.ChatSelectionList(this.minecraft, (this.contextInfoLabel.getLineCount() + 1) * 9)
        );
        this.addRenderableWidget(
            Button.builder(CommonComponents.GUI_BACK, p_239860_ -> this.onClose()).bounds(this.width / 2 - 155, this.height - 32, 150, 20).build()
        );
        this.confirmSelectedButton = this.addRenderableWidget(Button.builder(CommonComponents.GUI_DONE, p_299799_ -> {
            this.onSelected.accept(this.report);
            this.onClose();
        }).bounds(this.width / 2 - 155 + 160, this.height - 32, 150, 20).build());
        this.updateConfirmSelectedButton();
        this.extendLog();
        this.chatSelectionList.setScrollAmount(this.chatSelectionList.maxScrollAmount());
    }

    private boolean canReport(LoggedChatMessage message) {
        return message.canReport(this.report.reportedProfileId());
    }

    private void extendLog() {
        int i = this.chatSelectionList.getMaxVisibleEntries();
        this.chatLogFiller.fillNextPage(i, this.chatSelectionList);
    }

    void onReachedScrollTop() {
        this.extendLog();
    }

    void updateConfirmSelectedButton() {
        this.confirmSelectedButton.active = !this.report.reportedMessages().isEmpty();
    }

    @Override
    public void render(GuiGraphics p_282899_, int p_239287_, int p_239288_, float p_239289_) {
        super.render(p_282899_, p_239287_, p_239288_, p_239289_);
        ActiveTextCollector activetextcollector = p_282899_.textRenderer();
        p_282899_.drawCenteredString(this.font, this.title, this.width / 2, 10, -1);
        AbuseReportLimits abusereportlimits = this.reportingContext.sender().reportLimits();
        int i = this.report.reportedMessages().size();
        int j = abusereportlimits.maxReportedMessageCount();
        Component component = Component.translatable("gui.chatSelection.selected", i, j);
        p_282899_.drawCenteredString(this.font, component, this.width / 2, 26, -1);
        int k = this.chatSelectionList.getFooterTop();
        this.contextInfoLabel.visitLines(TextAlignment.CENTER, this.width / 2, k, 9, activetextcollector);
    }

    @Override
    public void onClose() {
        this.minecraft.setScreen(this.lastScreen);
    }

    @Override
    public Component getNarrationMessage() {
        return CommonComponents.joinForNarration(super.getNarrationMessage(), CONTEXT_INFO);
    }

    @OnlyIn(Dist.CLIENT)
    public class ChatSelectionList extends ObjectSelectionList<ChatSelectionScreen.ChatSelectionList.Entry> implements ChatSelectionLogFiller.Output {
        public static final int ITEM_HEIGHT = 16;
        private ChatSelectionScreen.ChatSelectionList.@Nullable Heading previousHeading;

        public ChatSelectionList(Minecraft minecraft, int height) {
            super(minecraft, ChatSelectionScreen.this.width, ChatSelectionScreen.this.height - height - 80, 40, 16);
        }

        @Override
        public void setScrollAmount(double p_239021_) {
            double d0 = this.scrollAmount();
            super.setScrollAmount(p_239021_);
            if (this.maxScrollAmount() > 1.0E-5F && p_239021_ <= 1.0E-5F && !Mth.equal(p_239021_, d0)) {
                ChatSelectionScreen.this.onReachedScrollTop();
            }
        }

        @Override
        public void acceptMessage(int p_242846_, LoggedChatMessage.Player p_242909_) {
            boolean flag = p_242909_.canReport(ChatSelectionScreen.this.report.reportedProfileId());
            ChatTrustLevel chattrustlevel = p_242909_.trustLevel();
            GuiMessageTag guimessagetag = chattrustlevel.createTag(p_242909_.message());
            ChatSelectionScreen.ChatSelectionList.Entry chatselectionscreen$chatselectionlist$entry = new ChatSelectionScreen.ChatSelectionList.MessageEntry(
                p_242846_, p_242909_.toContentComponent(), p_242909_.toNarrationComponent(), guimessagetag, flag, true
            );
            this.addEntryToTop(chatselectionscreen$chatselectionlist$entry);
            this.updateHeading(p_242909_, flag);
        }

        private void updateHeading(LoggedChatMessage.Player loggedPlayerChatMessage, boolean canReport) {
            ChatSelectionScreen.ChatSelectionList.Entry chatselectionscreen$chatselectionlist$entry = new ChatSelectionScreen.ChatSelectionList.MessageHeadingEntry(
                loggedPlayerChatMessage.profile(), loggedPlayerChatMessage.toHeadingComponent(), canReport
            );
            this.addEntryToTop(chatselectionscreen$chatselectionlist$entry);
            ChatSelectionScreen.ChatSelectionList.Heading chatselectionscreen$chatselectionlist$heading = new ChatSelectionScreen.ChatSelectionList.Heading(
                loggedPlayerChatMessage.profileId(), chatselectionscreen$chatselectionlist$entry
            );
            if (this.previousHeading != null && this.previousHeading.canCombine(chatselectionscreen$chatselectionlist$heading)) {
                this.removeEntryFromTop(this.previousHeading.entry());
            }

            this.previousHeading = chatselectionscreen$chatselectionlist$heading;
        }

        @Override
        public void acceptDivider(Component p_239876_) {
            this.addEntryToTop(new ChatSelectionScreen.ChatSelectionList.PaddingEntry());
            this.addEntryToTop(new ChatSelectionScreen.ChatSelectionList.DividerEntry(p_239876_));
            this.addEntryToTop(new ChatSelectionScreen.ChatSelectionList.PaddingEntry());
            this.previousHeading = null;
        }

        @Override
        public int getRowWidth() {
            return Math.min(350, this.width - 50);
        }

        public int getMaxVisibleEntries() {
            return Mth.positiveCeilDiv(this.height, 16);
        }

        protected void renderItem(GuiGraphics p_281532_, int p_239775_, int p_239776_, float p_239777_, ChatSelectionScreen.ChatSelectionList.Entry p_439092_) {
            if (this.shouldHighlightEntry(p_439092_)) {
                boolean flag = this.getSelected() == p_439092_;
                int i = this.isFocused() && flag ? -1 : -8355712;
                this.renderSelection(p_281532_, p_439092_, i);
            }

            p_439092_.renderContent(p_281532_, p_239775_, p_239776_, this.getHovered() == p_439092_, p_239777_);
        }

        private boolean shouldHighlightEntry(ChatSelectionScreen.ChatSelectionList.Entry entry) {
            if (entry.canSelect()) {
                boolean flag = this.getSelected() == entry;
                boolean flag1 = this.getSelected() == null;
                boolean flag2 = this.getHovered() == entry;
                return flag || flag1 && flag2 && entry.canReport();
            } else {
                return false;
            }
        }

        protected ChatSelectionScreen.ChatSelectionList.@Nullable Entry nextEntry(ScreenDirection p_265203_) {
            return this.nextEntry(p_265203_, ChatSelectionScreen.ChatSelectionList.Entry::canSelect);
        }

        public void setSelected(ChatSelectionScreen.ChatSelectionList.@Nullable Entry p_265249_) {
            super.setSelected(p_265249_);
            ChatSelectionScreen.ChatSelectionList.Entry chatselectionscreen$chatselectionlist$entry = this.nextEntry(ScreenDirection.UP);
            if (chatselectionscreen$chatselectionlist$entry == null) {
                ChatSelectionScreen.this.onReachedScrollTop();
            }
        }

        @Override
        public boolean keyPressed(KeyEvent p_447333_) {
            ChatSelectionScreen.ChatSelectionList.Entry chatselectionscreen$chatselectionlist$entry = this.getSelected();
            return chatselectionscreen$chatselectionlist$entry != null && chatselectionscreen$chatselectionlist$entry.keyPressed(p_447333_)
                ? true
                : super.keyPressed(p_447333_);
        }

        public int getFooterTop() {
            return this.getBottom() + 9;
        }

        @OnlyIn(Dist.CLIENT)
        public class DividerEntry extends ChatSelectionScreen.ChatSelectionList.Entry {
            private final Component text;

            public DividerEntry(Component text) {
                this.text = text;
            }

            @Override
            public void renderContent(GuiGraphics p_439949_, int p_439947_, int p_439491_, boolean p_439652_, float p_440556_) {
                int i = this.getContentYMiddle();
                int j = this.getContentRight() - 8;
                int k = ChatSelectionScreen.this.font.width(this.text);
                int l = (this.getContentX() + j - k) / 2;
                int i1 = i - 9 / 2;
                p_439949_.drawString(ChatSelectionScreen.this.font, this.text, l, i1, -6250336);
            }

            @Override
            public Component getNarration() {
                return this.text;
            }
        }

        @OnlyIn(Dist.CLIENT)
        public abstract static class Entry extends ObjectSelectionList.Entry<ChatSelectionScreen.ChatSelectionList.Entry> {
            @Override
            public Component getNarration() {
                return CommonComponents.EMPTY;
            }

            public boolean isSelected() {
                return false;
            }

            public boolean canSelect() {
                return false;
            }

            public boolean canReport() {
                return this.canSelect();
            }

            @Override
            public boolean mouseClicked(MouseButtonEvent p_445498_, boolean p_435944_) {
                return this.canSelect();
            }
        }

        @OnlyIn(Dist.CLIENT)
        record Heading(UUID sender, ChatSelectionScreen.ChatSelectionList.Entry entry) {
            public boolean canCombine(ChatSelectionScreen.ChatSelectionList.Heading other) {
                return other.sender.equals(this.sender);
            }
        }

        @OnlyIn(Dist.CLIENT)
        public class MessageEntry extends ChatSelectionScreen.ChatSelectionList.Entry {
            private static final int CHECKMARK_WIDTH = 9;
            private static final int CHECKMARK_HEIGHT = 8;
            private static final int INDENT_AMOUNT = 11;
            private static final int TAG_MARGIN_LEFT = 4;
            private final int chatId;
            private final FormattedText text;
            private final Component narration;
            private final @Nullable List<FormattedCharSequence> hoverText;
            private final GuiMessageTag.@Nullable Icon tagIcon;
            private final @Nullable List<FormattedCharSequence> tagHoverText;
            private final boolean canReport;
            private final boolean playerMessage;

            public MessageEntry(
                int chatId, Component text, Component narration, @Nullable GuiMessageTag tagIcon, boolean canReport, boolean playerMessage
            ) {
                this.chatId = chatId;
                this.tagIcon = Optionull.map(tagIcon, GuiMessageTag::icon);
                this.tagHoverText = tagIcon != null && tagIcon.text() != null
                    ? ChatSelectionScreen.this.font.split(tagIcon.text(), ChatSelectionList.this.getRowWidth())
                    : null;
                this.canReport = canReport;
                this.playerMessage = playerMessage;
                FormattedText formattedtext = ChatSelectionScreen.this.font
                    .substrByWidth(text, this.getMaximumTextWidth() - ChatSelectionScreen.this.font.width(CommonComponents.ELLIPSIS));
                if (text != formattedtext) {
                    this.text = FormattedText.composite(formattedtext, CommonComponents.ELLIPSIS);
                    this.hoverText = ChatSelectionScreen.this.font.split(text, ChatSelectionList.this.getRowWidth());
                } else {
                    this.text = text;
                    this.hoverText = null;
                }

                this.narration = narration;
            }

            @Override
            public void renderContent(GuiGraphics p_439274_, int p_440292_, int p_439601_, boolean p_439565_, float p_439005_) {
                if (this.isSelected() && this.canReport) {
                    this.renderSelectedCheckmark(p_439274_, this.getContentY(), this.getContentX(), this.getContentHeight());
                }

                int i = this.getContentX() + this.getTextIndent();
                int j = this.getContentY() + 1 + (this.getContentHeight() - 9) / 2;
                p_439274_.drawString(ChatSelectionScreen.this.font, Language.getInstance().getVisualOrder(this.text), i, j, this.canReport ? -1 : -1593835521);
                if (this.hoverText != null && p_439565_) {
                    p_439274_.setTooltipForNextFrame(this.hoverText, p_440292_, p_439601_);
                }

                int k = ChatSelectionScreen.this.font.width(this.text);
                this.renderTag(p_439274_, i + k + 4, this.getContentY(), this.getContentHeight(), p_440292_, p_439601_);
            }

            private void renderTag(GuiGraphics guiGraphics, int x, int y, int height, int mouseX, int mouseY) {
                if (this.tagIcon != null) {
                    int i = y + (height - this.tagIcon.height) / 2;
                    this.tagIcon.draw(guiGraphics, x, i);
                    if (this.tagHoverText != null
                        && mouseX >= x
                        && mouseX <= x + this.tagIcon.width
                        && mouseY >= i
                        && mouseY <= i + this.tagIcon.height) {
                        guiGraphics.setTooltipForNextFrame(this.tagHoverText, mouseX, mouseY);
                    }
                }
            }

            private void renderSelectedCheckmark(GuiGraphics guiGraphics, int top, int left, int height) {
                int i = top + (height - 8) / 2;
                guiGraphics.blitSprite(RenderPipelines.GUI_TEXTURED, ChatSelectionScreen.CHECKMARK_SPRITE, left, i, 9, 8);
            }

            private int getMaximumTextWidth() {
                int i = this.tagIcon != null ? this.tagIcon.width + 4 : 0;
                return ChatSelectionList.this.getRowWidth() - this.getTextIndent() - 4 - i;
            }

            private int getTextIndent() {
                return this.playerMessage ? 11 : 0;
            }

            @Override
            public Component getNarration() {
                return (Component)(this.isSelected() ? Component.translatable("narrator.select", this.narration) : this.narration);
            }

            @Override
            public boolean mouseClicked(MouseButtonEvent p_445490_, boolean p_433510_) {
                ChatSelectionList.this.setSelected(null);
                return this.toggleReport();
            }

            @Override
            public boolean keyPressed(KeyEvent p_446141_) {
                return p_446141_.isSelection() ? this.toggleReport() : false;
            }

            @Override
            public boolean isSelected() {
                return ChatSelectionScreen.this.report.isReported(this.chatId);
            }

            @Override
            public boolean canSelect() {
                return true;
            }

            @Override
            public boolean canReport() {
                return this.canReport;
            }

            private boolean toggleReport() {
                if (this.canReport) {
                    ChatSelectionScreen.this.report.toggleReported(this.chatId);
                    ChatSelectionScreen.this.updateConfirmSelectedButton();
                    return true;
                } else {
                    return false;
                }
            }
        }

        @OnlyIn(Dist.CLIENT)
        public class MessageHeadingEntry extends ChatSelectionScreen.ChatSelectionList.Entry {
            private static final int FACE_SIZE = 12;
            private static final int PADDING = 4;
            private final Component heading;
            private final Supplier<PlayerSkin> skin;
            private final boolean canReport;

            public MessageHeadingEntry(GameProfile profile, Component heading, boolean canReport) {
                this.heading = heading;
                this.canReport = canReport;
                this.skin = ChatSelectionList.this.minecraft.getSkinManager().createLookup(profile, true);
            }

            @Override
            public void renderContent(GuiGraphics p_440316_, int p_439797_, int p_439660_, boolean p_440537_, float p_439843_) {
                int i = this.getContentX() - 12 + 4;
                int j = this.getContentY() + (this.getContentHeight() - 12) / 2;
                PlayerFaceRenderer.draw(p_440316_, this.skin.get(), i, j, 12);
                int k = this.getContentY() + 1 + (this.getContentHeight() - 9) / 2;
                p_440316_.drawString(ChatSelectionScreen.this.font, this.heading, i + 12 + 4, k, this.canReport ? -1 : -1593835521);
            }
        }

        @OnlyIn(Dist.CLIENT)
        public static class PaddingEntry extends ChatSelectionScreen.ChatSelectionList.Entry {
            @Override
            public void renderContent(GuiGraphics p_282007_, int p_240110_, int p_240111_, boolean p_240117_, float p_240118_) {
            }
        }
    }
}
