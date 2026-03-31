package net.minecraft.client.gui.components;

import com.google.common.collect.Lists;
import com.mojang.blaze3d.platform.cursor.CursorTypes;
import com.mojang.logging.LogUtils;
import java.util.ArrayList;
import java.util.List;
import java.util.ListIterator;
import java.util.Optional;
import java.util.function.Consumer;
import net.minecraft.ChatFormatting;
import net.minecraft.Optionull;
import net.minecraft.client.GuiMessage;
import net.minecraft.client.GuiMessageTag;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ActiveTextCollector;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.TextAlignment;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MessageSignature;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ARGB;
import net.minecraft.util.ArrayListDeque;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;
import net.minecraft.util.profiling.Profiler;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.entity.player.ChatVisiblity;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.joml.Matrix3x2f;
import org.joml.Vector2f;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

@OnlyIn(Dist.CLIENT)
public class ChatComponent {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final int MAX_CHAT_HISTORY = 100;
    private static final int MESSAGE_INDENT = 4;
    private static final int BOTTOM_MARGIN = 40;
    private static final int TOOLTIP_MAX_WIDTH = 210;
    private static final int TIME_BEFORE_MESSAGE_DELETION = 60;
    private static final Component DELETED_CHAT_MESSAGE = Component.translatable("chat.deleted_marker").withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC);
    public static final int MESSAGE_BOTTOM_TO_MESSAGE_TOP = 8;
    public static final Identifier QUEUE_EXPAND_ID = Identifier.withDefaultNamespace("internal/expand_chat_queue");
    private static final Style QUEUE_EXPAND_TEXT_STYLE = Style.EMPTY
        .withClickEvent(new ClickEvent.Custom(QUEUE_EXPAND_ID, Optional.empty()))
        .withHoverEvent(new HoverEvent.ShowText(Component.translatable("chat.queue.tooltip")));
    final Minecraft minecraft;
    /**
     * A list of messages previously sent through the chat GUI
     */
    private final ArrayListDeque<String> recentChat = new ArrayListDeque<>(100);
    /**
     * Chat lines to be displayed in the chat box
     */
    private final List<GuiMessage> allMessages = Lists.newArrayList();
    /**
     * List of the ChatLines currently drawn
     */
    private final List<GuiMessage.Line> trimmedMessages = Lists.newArrayList();
    private int chatScrollbarPos;
    private boolean newMessageSinceScroll;
    private ChatComponent.@Nullable Draft latestDraft;
    private @Nullable ChatScreen preservedScreen;
    private final List<ChatComponent.DelayedMessageDeletion> messageDeletionQueue = new ArrayList<>();

    public ChatComponent(Minecraft minecraft) {
        this.minecraft = minecraft;
        this.recentChat.addAll(minecraft.commandHistory().history());
    }

    public void tick() {
        if (!this.messageDeletionQueue.isEmpty()) {
            this.processMessageDeletionQueue();
        }
    }

    private int forEachLine(ChatComponent.AlphaCalculator alphaCalculator, ChatComponent.LineConsumer action) {
        int i = this.getLinesPerPage();
        int j = 0;

        for (int k = Math.min(this.trimmedMessages.size() - this.chatScrollbarPos, i) - 1; k >= 0; k--) {
            int l = k + this.chatScrollbarPos;
            GuiMessage.Line guimessage$line = this.trimmedMessages.get(l);
            float f = alphaCalculator.calculate(guimessage$line);
            if (f > 1.0E-5F) {
                j++;
                action.accept(guimessage$line, k, f);
            }
        }

        return j;
    }

    public void render(GuiGraphics graphics, Font font, int tickCount, int globalMouseX, int globalMouseY, boolean focused, boolean changeCursorOnInsertions) {
        graphics.pose().pushMatrix();
        this.render(
            (ChatComponent.ChatGraphicsAccess)(focused
                ? new ChatComponent.DrawingFocusedGraphicsAccess(graphics, font, globalMouseX, globalMouseY, changeCursorOnInsertions)
                : new ChatComponent.DrawingBackgroundGraphicsAccess(graphics)),
            graphics.guiHeight(),
            tickCount,
            focused
        );
        graphics.pose().popMatrix();
    }

    public void captureClickableText(ActiveTextCollector activeTextCollector, int height, int tickCount, boolean focused) {
        this.render(new ChatComponent.ClickableTextOnlyGraphicsAccess(activeTextCollector), height, tickCount, focused);
    }

    private void render(final ChatComponent.ChatGraphicsAccess chatGraphicsAccess, int height, int tickCount, boolean focused) {
        if (!this.isChatHidden()) {
            int i = this.trimmedMessages.size();
            if (i > 0) {
                ProfilerFiller profilerfiller = Profiler.get();
                profilerfiller.push("chat");
                float f = (float)this.getScale();
                int j = Mth.ceil(this.getWidth() / f);
                final int k = Mth.floor((height - 40) / f);
                final float f1 = this.minecraft.options.chatOpacity().get().floatValue() * 0.9F + 0.1F;
                float f2 = this.minecraft.options.textBackgroundOpacity().get().floatValue();
                final int l = 9;
                int i1 = 8;
                double d0 = this.minecraft.options.chatLineSpacing().get();
                final int j1 = (int)(l * (d0 + 1.0));
                final int k1 = (int)Math.round(8.0 * (d0 + 1.0) - 4.0 * d0);
                long l1 = this.minecraft.getChatListener().queueSize();
                ChatComponent.AlphaCalculator chatcomponent$alphacalculator = focused
                    ? ChatComponent.AlphaCalculator.FULLY_VISIBLE
                    : ChatComponent.AlphaCalculator.timeBased(tickCount);
                chatGraphicsAccess.updatePose(p_457333_ -> {
                    p_457333_.scale(f, f);
                    p_457333_.translate(4.0F, 0.0F);
                });
                this.forEachLine(chatcomponent$alphacalculator, (p_457339_, p_457340_, p_457341_) -> {
                    int j4 = k - p_457340_ * j1;
                    int k4 = j4 - j1;
                    chatGraphicsAccess.fill(-4, k4, j + 4 + 4, j4, ARGB.black(p_457341_ * f2));
                });
                if (l1 > 0L) {
                    chatGraphicsAccess.fill(-2, k, j + 4, k + l, ARGB.black(f2));
                }

                int i2 = this.forEachLine(chatcomponent$alphacalculator, new ChatComponent.LineConsumer() {
                    boolean hoveredOverCurrentMessage;

                    @Override
                    public void accept(GuiMessage.Line p_457674_, int p_457678_, float p_458251_) {
                        int j4 = k - p_457678_ * j1;
                        int k4 = j4 - j1;
                        int l4 = j4 - k1;
                        boolean flag = chatGraphicsAccess.handleMessage(l4, p_458251_ * f1, p_457674_.content());
                        this.hoveredOverCurrentMessage |= flag;
                        boolean flag1;
                        if (p_457674_.endOfEntry()) {
                            flag1 = this.hoveredOverCurrentMessage;
                            this.hoveredOverCurrentMessage = false;
                        } else {
                            flag1 = false;
                        }

                        GuiMessageTag guimessagetag = p_457674_.tag();
                        if (guimessagetag != null) {
                            chatGraphicsAccess.handleTag(-4, k4, -2, j4, p_458251_ * f1, guimessagetag);
                            if (guimessagetag.icon() != null) {
                                int i5 = p_457674_.getTagIconLeft(ChatComponent.this.minecraft.font);
                                int j5 = l4 + l;
                                chatGraphicsAccess.handleTagIcon(i5, j5, flag1, guimessagetag, guimessagetag.icon());
                            }
                        }
                    }
                });
                if (l1 > 0L) {
                    int j2 = k + l;
                    Component component = Component.translatable("chat.queue", l1).setStyle(QUEUE_EXPAND_TEXT_STYLE);
                    chatGraphicsAccess.handleMessage(j2 - 8, 0.5F * f1, component.getVisualOrderText());
                }

                if (focused) {
                    int l3 = i * j1;
                    int i4 = i2 * j1;
                    int k2 = this.chatScrollbarPos * i4 / i - k;
                    int l2 = i4 * i4 / l3;
                    if (l3 != i4) {
                        int i3 = k2 > 0 ? 170 : 96;
                        int j3 = this.newMessageSinceScroll ? 13382451 : 3355562;
                        int k3 = j + 4;
                        chatGraphicsAccess.fill(k3, -k2, k3 + 2, -k2 - l2, ARGB.color(i3, j3));
                        chatGraphicsAccess.fill(k3 + 2, -k2, k3 + 1, -k2 - l2, ARGB.color(i3, 13421772));
                    }
                }

                profilerfiller.pop();
            }
        }
    }

    private boolean isChatHidden() {
        return this.minecraft.options.chatVisibility().get() == ChatVisiblity.HIDDEN;
    }

    /**
     * Clears the chat.
     *
     * @param clearSentMsgHistory Whether to clear the user's sent message history
     */
    public void clearMessages(boolean clearSentMsgHistory) {
        this.minecraft.getChatListener().flushQueue();
        this.messageDeletionQueue.clear();
        this.trimmedMessages.clear();
        this.allMessages.clear();
        if (clearSentMsgHistory) {
            this.recentChat.clear();
            this.recentChat.addAll(this.minecraft.commandHistory().history());
        }
    }

    public void addMessage(Component chatComponent) {
        this.addMessage(chatComponent, null, this.minecraft.isSingleplayer() ? GuiMessageTag.systemSinglePlayer() : GuiMessageTag.system());
    }

    public void addMessage(Component chatComponent, @Nullable MessageSignature headerSignature, @Nullable GuiMessageTag tag) {
        GuiMessage guimessage = new GuiMessage(this.minecraft.gui.getGuiTicks(), chatComponent, headerSignature, tag);
        this.logChatMessage(guimessage);
        this.addMessageToDisplayQueue(guimessage);
        this.addMessageToQueue(guimessage);
    }

    private void logChatMessage(GuiMessage message) {
        String s = message.content().getString().replaceAll("\r", "\\\\r").replaceAll("\n", "\\\\n");
        String s1 = Optionull.map(message.tag(), GuiMessageTag::logTag);
        if (s1 != null) {
            LOGGER.info("[{}] [CHAT] {}", s1, s);
        } else {
            LOGGER.info("[CHAT] {}", s);
        }
    }

    private void addMessageToDisplayQueue(GuiMessage message) {
        int i = Mth.floor(this.getWidth() / this.getScale());
        List<FormattedCharSequence> list = message.splitLines(this.minecraft.font, i);
        boolean flag = this.isChatFocused();

        for (int j = 0; j < list.size(); j++) {
            FormattedCharSequence formattedcharsequence = list.get(j);
            if (flag && this.chatScrollbarPos > 0) {
                this.newMessageSinceScroll = true;
                this.scrollChat(1);
            }

            boolean flag1 = j == list.size() - 1;
            this.trimmedMessages.addFirst(new GuiMessage.Line(message.addedTime(), formattedcharsequence, message.tag(), flag1));
        }

        while (this.trimmedMessages.size() > 100) {
            this.trimmedMessages.removeLast();
        }
    }

    private void addMessageToQueue(GuiMessage message) {
        this.allMessages.addFirst(message);

        while (this.allMessages.size() > 100) {
            this.allMessages.removeLast();
        }
    }

    private void processMessageDeletionQueue() {
        int i = this.minecraft.gui.getGuiTicks();
        this.messageDeletionQueue.removeIf(p_250713_ -> i >= p_250713_.deletableAfter() ? this.deleteMessageOrDelay(p_250713_.signature()) == null : false);
    }

    public void deleteMessage(MessageSignature messageSignature) {
        ChatComponent.DelayedMessageDeletion chatcomponent$delayedmessagedeletion = this.deleteMessageOrDelay(messageSignature);
        if (chatcomponent$delayedmessagedeletion != null) {
            this.messageDeletionQueue.add(chatcomponent$delayedmessagedeletion);
        }
    }

    private ChatComponent.@Nullable DelayedMessageDeletion deleteMessageOrDelay(MessageSignature messageSignature) {
        int i = this.minecraft.gui.getGuiTicks();
        ListIterator<GuiMessage> listiterator = this.allMessages.listIterator();

        while (listiterator.hasNext()) {
            GuiMessage guimessage = listiterator.next();
            if (messageSignature.equals(guimessage.signature())) {
                int j = guimessage.addedTime() + 60;
                if (i >= j) {
                    listiterator.set(this.createDeletedMarker(guimessage));
                    this.refreshTrimmedMessages();
                    return null;
                }

                return new ChatComponent.DelayedMessageDeletion(messageSignature, j);
            }
        }

        return null;
    }

    private GuiMessage createDeletedMarker(GuiMessage message) {
        return new GuiMessage(message.addedTime(), DELETED_CHAT_MESSAGE, null, GuiMessageTag.system());
    }

    public void rescaleChat() {
        this.resetChatScroll();
        this.refreshTrimmedMessages();
    }

    private void refreshTrimmedMessages() {
        this.trimmedMessages.clear();

        for (GuiMessage guimessage : Lists.reverse(this.allMessages)) {
            this.addMessageToDisplayQueue(guimessage);
        }
    }

    public ArrayListDeque<String> getRecentChat() {
        return this.recentChat;
    }

    /**
     * Adds this string to the list of sent messages, for recall using the up/down arrow keys
     */
    public void addRecentChat(String message) {
        if (!message.equals(this.recentChat.peekLast())) {
            if (this.recentChat.size() >= 100) {
                this.recentChat.removeFirst();
            }

            this.recentChat.addLast(message);
        }

        if (message.startsWith("/")) {
            this.minecraft.commandHistory().addCommand(message);
        }
    }

    public void resetChatScroll() {
        this.chatScrollbarPos = 0;
        this.newMessageSinceScroll = false;
    }

    public void scrollChat(int posInc) {
        this.chatScrollbarPos += posInc;
        int i = this.trimmedMessages.size();
        if (this.chatScrollbarPos > i - this.getLinesPerPage()) {
            this.chatScrollbarPos = i - this.getLinesPerPage();
        }

        if (this.chatScrollbarPos <= 0) {
            this.chatScrollbarPos = 0;
            this.newMessageSinceScroll = false;
        }
    }

    public boolean isChatFocused() {
        return this.minecraft.screen instanceof ChatScreen;
    }

    private int getWidth() {
        return getWidth(this.minecraft.options.chatWidth().get());
    }

    private int getHeight() {
        return getHeight(this.isChatFocused() ? this.minecraft.options.chatHeightFocused().get() : this.minecraft.options.chatHeightUnfocused().get());
    }

    public double getScale() {
        return this.minecraft.options.chatScale().get();
    }

    public static int getWidth(double width) {
        int i = 320;
        int j = 40;
        return Mth.floor(width * 280.0 + 40.0);
    }

    public static int getHeight(double height) {
        int i = 180;
        int j = 20;
        return Mth.floor(height * 160.0 + 20.0);
    }

    public static double defaultUnfocusedPct() {
        int i = 180;
        int j = 20;
        return 70.0 / (getHeight(1.0) - 20);
    }

    public int getLinesPerPage() {
        return this.getHeight() / this.getLineHeight();
    }

    private int getLineHeight() {
        return (int)(9.0 * (this.minecraft.options.chatLineSpacing().get() + 1.0));
    }

    public void saveAsDraft(String text) {
        boolean flag = text.startsWith("/");
        this.latestDraft = new ChatComponent.Draft(text, flag ? ChatComponent.ChatMethod.COMMAND : ChatComponent.ChatMethod.MESSAGE);
    }

    public void discardDraft() {
        this.latestDraft = null;
    }

    public <T extends ChatScreen> T createScreen(ChatComponent.ChatMethod method, ChatScreen.ChatConstructor<T> constructor) {
        return this.latestDraft != null && method.isDraftRestorable(this.latestDraft)
            ? constructor.create(this.latestDraft.text(), true)
            : constructor.create(method.prefix(), false);
    }

    public void openScreen(ChatComponent.ChatMethod method, ChatScreen.ChatConstructor<?> constructor) {
        this.minecraft.setScreen(this.createScreen(method, constructor));
    }

    public void preserveCurrentChatScreen() {
        if (this.minecraft.screen instanceof ChatScreen chatscreen) {
            this.preservedScreen = chatscreen;
        }
    }

    public @Nullable ChatScreen restoreChatScreen() {
        ChatScreen chatscreen = this.preservedScreen;
        this.preservedScreen = null;
        return chatscreen;
    }

    public ChatComponent.State storeState() {
        return new ChatComponent.State(List.copyOf(this.allMessages), List.copyOf(this.recentChat), List.copyOf(this.messageDeletionQueue));
    }

    public void restoreState(ChatComponent.State state) {
        this.recentChat.clear();
        this.recentChat.addAll(state.history);
        this.messageDeletionQueue.clear();
        this.messageDeletionQueue.addAll(state.delayedMessageDeletions);
        this.allMessages.clear();
        this.allMessages.addAll(state.messages);
        this.refreshTrimmedMessages();
    }

    @FunctionalInterface
    @OnlyIn(Dist.CLIENT)
    interface AlphaCalculator {
        ChatComponent.AlphaCalculator FULLY_VISIBLE = p_458184_ -> 1.0F;

        static ChatComponent.AlphaCalculator timeBased(int tickCount) {
            return p_458056_ -> {
                int i = tickCount - p_458056_.addedTime();
                double d0 = i / 200.0;
                d0 = 1.0 - d0;
                d0 *= 10.0;
                d0 = Mth.clamp(d0, 0.0, 1.0);
                d0 *= d0;
                return (float)d0;
            };
        }

        float calculate(GuiMessage.Line line);
    }

    @OnlyIn(Dist.CLIENT)
    public interface ChatGraphicsAccess {
        void updatePose(Consumer<Matrix3x2f> updater);

        void fill(int minX, int minY, int maxX, int maxY, int color);

        boolean handleMessage(int y, float opacity, FormattedCharSequence text);

        void handleTag(int minX, int minY, int maxX, int maxY, float opacity, GuiMessageTag tag);

        void handleTagIcon(int x, int y, boolean drawIcon, GuiMessageTag tag, GuiMessageTag.Icon icon);
    }

    @OnlyIn(Dist.CLIENT)
    public static enum ChatMethod {
        MESSAGE("") {
            @Override
            public boolean isDraftRestorable(ChatComponent.Draft p_437208_) {
                return true;
            }
        },
        COMMAND("/") {
            @Override
            public boolean isDraftRestorable(ChatComponent.Draft p_437306_) {
                return this == p_437306_.chatMethod();
            }
        };

        private final String prefix;

        ChatMethod(String prefix) {
            this.prefix = prefix;
        }

        public String prefix() {
            return this.prefix;
        }

        public abstract boolean isDraftRestorable(ChatComponent.Draft draft);
    }

    @OnlyIn(Dist.CLIENT)
    static class ClickableTextOnlyGraphicsAccess implements ChatComponent.ChatGraphicsAccess {
        private final ActiveTextCollector output;

        public ClickableTextOnlyGraphicsAccess(ActiveTextCollector output) {
            this.output = output;
        }

        @Override
        public void updatePose(Consumer<Matrix3x2f> p_458155_) {
            ActiveTextCollector.Parameters activetextcollector$parameters = this.output.defaultParameters();
            Matrix3x2f matrix3x2f = new Matrix3x2f(activetextcollector$parameters.pose());
            p_458155_.accept(matrix3x2f);
            this.output.defaultParameters(activetextcollector$parameters.withPose(matrix3x2f));
        }

        @Override
        public void fill(int p_458287_, int p_457905_, int p_458000_, int p_457740_, int p_458112_) {
        }

        @Override
        public boolean handleMessage(int p_457906_, float p_458178_, FormattedCharSequence p_457578_) {
            this.output.accept(TextAlignment.LEFT, 0, p_457906_, p_457578_);
            return false;
        }

        @Override
        public void handleTag(int p_457827_, int p_457944_, int p_457877_, int p_458280_, float p_458186_, GuiMessageTag p_458254_) {
        }

        @Override
        public void handleTagIcon(int p_457913_, int p_457716_, boolean p_458037_, GuiMessageTag p_458041_, GuiMessageTag.Icon p_458129_) {
        }
    }

    @OnlyIn(Dist.CLIENT)
    record DelayedMessageDeletion(MessageSignature signature, int deletableAfter) {
    }

    @OnlyIn(Dist.CLIENT)
    public record Draft(String text, ChatComponent.ChatMethod chatMethod) {
    }

    @OnlyIn(Dist.CLIENT)
    static class DrawingBackgroundGraphicsAccess implements ChatComponent.ChatGraphicsAccess {
        private final GuiGraphics graphics;
        private final ActiveTextCollector textRenderer;
        private ActiveTextCollector.Parameters parameters;

        public DrawingBackgroundGraphicsAccess(GuiGraphics graphics) {
            this.graphics = graphics;
            this.textRenderer = graphics.textRenderer(GuiGraphics.HoveredTextEffects.NONE, null);
            this.parameters = this.textRenderer.defaultParameters();
        }

        @Override
        public void updatePose(Consumer<Matrix3x2f> p_466976_) {
            p_466976_.accept(this.graphics.pose());
            this.parameters = this.parameters.withPose(new Matrix3x2f(this.graphics.pose()));
        }

        @Override
        public void fill(int p_468326_, int p_469160_, int p_468044_, int p_469611_, int p_469579_) {
            this.graphics.fill(p_468326_, p_469160_, p_468044_, p_469611_, p_469579_);
        }

        @Override
        public boolean handleMessage(int p_467398_, float p_468512_, FormattedCharSequence p_467937_) {
            this.textRenderer.accept(TextAlignment.LEFT, 0, p_467398_, this.parameters.withOpacity(p_468512_), p_467937_);
            return false;
        }

        @Override
        public void handleTag(int p_469205_, int p_467121_, int p_469773_, int p_467576_, float p_467290_, GuiMessageTag p_469281_) {
            int i = ARGB.color(p_467290_, p_469281_.indicatorColor());
            this.graphics.fill(p_469205_, p_467121_, p_469773_, p_467576_, i);
        }

        @Override
        public void handleTagIcon(int p_468887_, int p_469864_, boolean p_467710_, GuiMessageTag p_467238_, GuiMessageTag.Icon p_469770_) {
        }
    }

    @OnlyIn(Dist.CLIENT)
    static class DrawingFocusedGraphicsAccess implements ChatComponent.ChatGraphicsAccess, Consumer<Style> {
        private final GuiGraphics graphics;
        private final Font font;
        private final ActiveTextCollector textRenderer;
        private ActiveTextCollector.Parameters parameters;
        private final int globalMouseX;
        private final int globalMouseY;
        private final Vector2f localMousePos = new Vector2f();
        private @Nullable Style hoveredStyle;
        private final boolean changeCursorOnInsertions;

        public DrawingFocusedGraphicsAccess(GuiGraphics graphics, Font font, int globalMouseX, int globalMouseY, boolean changeCursorOnInsertions) {
            this.graphics = graphics;
            this.font = font;
            this.textRenderer = graphics.textRenderer(GuiGraphics.HoveredTextEffects.TOOLTIP_AND_CURSOR, this);
            this.globalMouseX = globalMouseX;
            this.globalMouseY = globalMouseY;
            this.changeCursorOnInsertions = changeCursorOnInsertions;
            this.parameters = this.textRenderer.defaultParameters();
            this.updateLocalMousePos();
        }

        private void updateLocalMousePos() {
            this.graphics.pose().invert(new Matrix3x2f()).transformPosition(this.globalMouseX, this.globalMouseY, this.localMousePos);
        }

        @Override
        public void updatePose(Consumer<Matrix3x2f> p_467899_) {
            p_467899_.accept(this.graphics.pose());
            this.parameters = this.parameters.withPose(new Matrix3x2f(this.graphics.pose()));
            this.updateLocalMousePos();
        }

        @Override
        public void fill(int p_467756_, int p_467968_, int p_469302_, int p_467383_, int p_468283_) {
            this.graphics.fill(p_467756_, p_467968_, p_469302_, p_467383_, p_468283_);
        }

        public void accept(Style p_466846_) {
            this.hoveredStyle = p_466846_;
        }

        @Override
        public boolean handleMessage(int p_467745_, float p_469150_, FormattedCharSequence p_467444_) {
            this.hoveredStyle = null;
            this.textRenderer.accept(TextAlignment.LEFT, 0, p_467745_, this.parameters.withOpacity(p_469150_), p_467444_);
            if (this.changeCursorOnInsertions && this.hoveredStyle != null && this.hoveredStyle.getInsertion() != null) {
                this.graphics.requestCursor(CursorTypes.POINTING_HAND);
            }

            return this.hoveredStyle != null;
        }

        private boolean isMouseOver(int minX, int minY, int maxX, int maxY) {
            return ActiveTextCollector.isPointInRectangle(this.localMousePos.x, this.localMousePos.y, minX, minY, maxX, maxY);
        }

        @Override
        public void handleTag(int p_467877_, int p_466920_, int p_468297_, int p_466977_, float p_468579_, GuiMessageTag p_468230_) {
            int i = ARGB.color(p_468579_, p_468230_.indicatorColor());
            this.graphics.fill(p_467877_, p_466920_, p_468297_, p_466977_, i);
            if (this.isMouseOver(p_467877_, p_466920_, p_468297_, p_466977_)) {
                this.showTooltip(p_468230_);
            }
        }

        @Override
        public void handleTagIcon(int p_469427_, int p_469103_, boolean p_467801_, GuiMessageTag p_467777_, GuiMessageTag.Icon p_469613_) {
            int i = p_469103_ - p_469613_.height - 1;
            int j = p_469427_ + p_469613_.width;
            boolean flag = this.isMouseOver(p_469427_, i, j, p_469103_);
            if (flag) {
                this.showTooltip(p_467777_);
            }

            if (p_467801_ || flag) {
                p_469613_.draw(this.graphics, p_469427_, i);
            }
        }

        private void showTooltip(GuiMessageTag tag) {
            if (tag.text() != null) {
                this.graphics.setTooltipForNextFrame(this.font, this.font.split(tag.text(), 210), this.globalMouseX, this.globalMouseY);
            }
        }
    }

    @FunctionalInterface
    @OnlyIn(Dist.CLIENT)
    interface LineConsumer {
        void accept(GuiMessage.Line line, int index, float opacity);
    }

    @OnlyIn(Dist.CLIENT)
    public static class State {
        final List<GuiMessage> messages;
        final List<String> history;
        final List<ChatComponent.DelayedMessageDeletion> delayedMessageDeletions;

        public State(List<GuiMessage> messages, List<String> history, List<ChatComponent.DelayedMessageDeletion> delayedMessageDeletions) {
            this.messages = messages;
            this.history = history;
            this.delayedMessageDeletions = delayedMessageDeletions;
        }
    }
}
