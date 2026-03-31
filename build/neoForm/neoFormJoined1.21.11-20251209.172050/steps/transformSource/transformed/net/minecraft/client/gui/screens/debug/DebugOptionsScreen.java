package net.minecraft.client.gui.screens.debug;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import it.unimi.dsi.fastutil.floats.FloatComparators;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.ContainerObjectSelectionList;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.MultiLineTextWidget;
import net.minecraft.client.gui.components.StringWidget;
import net.minecraft.client.gui.components.debug.DebugEntryCategory;
import net.minecraft.client.gui.components.debug.DebugScreenEntries;
import net.minecraft.client.gui.components.debug.DebugScreenEntry;
import net.minecraft.client.gui.components.debug.DebugScreenEntryStatus;
import net.minecraft.client.gui.components.debug.DebugScreenProfile;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.layouts.HeaderAndFooterLayout;
import net.minecraft.client.gui.layouts.LayoutSettings;
import net.minecraft.client.gui.layouts.LinearLayout;
import net.minecraft.client.gui.layouts.SpacerElement;
import net.minecraft.client.gui.narration.NarratableEntry;
import net.minecraft.client.gui.narration.NarratedElementType;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.Identifier;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.jspecify.annotations.Nullable;

@OnlyIn(Dist.CLIENT)
public class DebugOptionsScreen extends Screen {
    private static final Component TITLE = Component.translatable("debug.options.title");
    private static final Component SUBTITLE = Component.translatable("debug.options.warning").withColor(-2142128);
    static final Component ENABLED_TEXT = Component.translatable("debug.entry.always");
    static final Component IN_OVERLAY_TEXT = Component.translatable("debug.entry.overlay");
    static final Component DISABLED_TEXT = CommonComponents.OPTION_OFF;
    static final Component NOT_ALLOWED_TOOLTIP = Component.translatable("debug.options.notAllowed.tooltip");
    private static final Component SEARCH = Component.translatable("debug.options.search").withStyle(EditBox.SEARCH_HINT_STYLE);
    final HeaderAndFooterLayout layout = new HeaderAndFooterLayout(this, 61, 33);
    private DebugOptionsScreen.@Nullable OptionList optionList;
    private EditBox searchBox;
    final List<Button> profileButtons = new ArrayList<>();

    public DebugOptionsScreen() {
        super(TITLE);
    }

    @Override
    protected void init() {
        LinearLayout linearlayout = this.layout.addToHeader(LinearLayout.vertical().spacing(8));
        this.optionList = new DebugOptionsScreen.OptionList();
        int i = this.optionList.getRowWidth();
        LinearLayout linearlayout1 = LinearLayout.horizontal().spacing(8);
        linearlayout1.addChild(new SpacerElement(i / 3, 1));
        linearlayout1.addChild(new StringWidget(TITLE, this.font), linearlayout1.newCellSettings().alignVerticallyMiddle());
        this.searchBox = new EditBox(this.font, 0, 0, i / 3, 20, this.searchBox, SEARCH);
        this.searchBox.setResponder(p_435276_ -> this.optionList.updateSearch(p_435276_));
        this.searchBox.setHint(SEARCH);
        linearlayout1.addChild(this.searchBox);
        linearlayout.addChild(linearlayout1, LayoutSettings::alignHorizontallyCenter);
        linearlayout.addChild(new MultiLineTextWidget(SUBTITLE, this.font).setMaxWidth(i).setCentered(true), LayoutSettings::alignHorizontallyCenter);
        this.layout.addToContents(this.optionList);
        LinearLayout linearlayout2 = this.layout.addToFooter(LinearLayout.horizontal().spacing(8));
        this.addProfileButton(DebugScreenProfile.DEFAULT, linearlayout2);
        this.addProfileButton(DebugScreenProfile.PERFORMANCE, linearlayout2);
        linearlayout2.addChild(Button.builder(CommonComponents.GUI_DONE, p_433376_ -> this.onClose()).width(60).build());
        this.layout.visitWidgets(p_434745_ -> {
            AbstractWidget abstractwidget = this.addRenderableWidget(p_434745_);
        });
        this.repositionElements();
    }

    @Override
    public void renderBlurredBackground(GuiGraphics p_443530_) {
        this.minecraft.gui.renderDebugOverlay(p_443530_);
        super.renderBlurredBackground(p_443530_);
    }

    @Override
    protected void setInitialFocus() {
        this.setInitialFocus(this.searchBox);
    }

    private void addProfileButton(DebugScreenProfile profile, LinearLayout layout) {
        Button button = Button.builder(Component.translatable(profile.translationKey()), p_434423_ -> {
            this.minecraft.debugEntries.loadProfile(profile);
            this.minecraft.debugEntries.save();
            this.optionList.refreshEntries();

            for (Button button1 : this.profileButtons) {
                button1.active = true;
            }

            p_434423_.active = false;
        }).width(120).build();
        button.active = !this.minecraft.debugEntries.isUsingProfile(profile);
        this.profileButtons.add(button);
        layout.addChild(button);
    }

    @Override
    protected void repositionElements() {
        this.layout.arrangeElements();
        if (this.optionList != null) {
            this.optionList.updateSize(this.width, this.layout);
        }
    }

    public DebugOptionsScreen.@Nullable OptionList getOptionList() {
        return this.optionList;
    }

    @OnlyIn(Dist.CLIENT)
    public abstract static class AbstractOptionEntry extends ContainerObjectSelectionList.Entry<DebugOptionsScreen.AbstractOptionEntry> {
        public abstract void refreshEntry();
    }

    @OnlyIn(Dist.CLIENT)
    class CategoryEntry extends DebugOptionsScreen.AbstractOptionEntry {
        final Component category;

        public CategoryEntry(Component category) {
            this.category = category;
        }

        @Override
        public void renderContent(GuiGraphics p_439011_, int p_439743_, int p_440636_, boolean p_440020_, float p_438954_) {
            p_439011_.drawCenteredString(
                DebugOptionsScreen.this.minecraft.font, this.category, this.getContentX() + this.getContentWidth() / 2, this.getContentY() + 5, -1
            );
        }

        @Override
        public List<? extends GuiEventListener> children() {
            return ImmutableList.of();
        }

        @Override
        public List<? extends NarratableEntry> narratables() {
            return ImmutableList.of(new NarratableEntry() {
                @Override
                public NarratableEntry.NarrationPriority narrationPriority() {
                    return NarratableEntry.NarrationPriority.HOVERED;
                }

                @Override
                public void updateNarration(NarrationElementOutput p_434625_) {
                    p_434625_.add(NarratedElementType.TITLE, CategoryEntry.this.category);
                }
            });
        }

        @Override
        public void refreshEntry() {
        }
    }

    @OnlyIn(Dist.CLIENT)
    class OptionEntry extends DebugOptionsScreen.AbstractOptionEntry {
        private static final int BUTTON_WIDTH = 60;
        private final Identifier location;
        protected final List<AbstractWidget> children = Lists.newArrayList();
        private final CycleButton<Boolean> always;
        private final CycleButton<Boolean> overlay;
        private final CycleButton<Boolean> never;
        private final String name;
        private final boolean isAllowed;

        public OptionEntry(Identifier location) {
            this.location = location;
            DebugScreenEntry debugscreenentry = DebugScreenEntries.getEntry(location);
            this.isAllowed = debugscreenentry != null && debugscreenentry.isAllowed(DebugOptionsScreen.this.minecraft.showOnlyReducedInfo());
            String s = location.getPath();
            // Neo: Display the full RL for modded entries
            if(!location.getNamespace().equals(Identifier.DEFAULT_NAMESPACE)) {
                s = location.toString();
            }
            if (this.isAllowed) {
                this.name = s;
            } else {
                this.name = ChatFormatting.ITALIC + s;
            }

            this.always = CycleButton.booleanBuilder(
                    DebugOptionsScreen.ENABLED_TEXT.copy().withColor(-2142128), DebugOptionsScreen.ENABLED_TEXT.copy().withColor(-4539718), false
                )
                .displayOnlyValue()
                .withCustomNarration(this::narrateButton)
                .create(10, 5, 60, 16, Component.literal(s), (p_465512_, p_465513_) -> this.setValue(location, DebugScreenEntryStatus.ALWAYS_ON));
            this.overlay = CycleButton.booleanBuilder(
                    DebugOptionsScreen.IN_OVERLAY_TEXT.copy().withColor(-171), DebugOptionsScreen.IN_OVERLAY_TEXT.copy().withColor(-4539718), false
                )
                .displayOnlyValue()
                .withCustomNarration(this::narrateButton)
                .create(10, 5, 60, 16, Component.literal(s), (p_465515_, p_465516_) -> this.setValue(location, DebugScreenEntryStatus.IN_OVERLAY));
            this.never = CycleButton.booleanBuilder(
                    DebugOptionsScreen.DISABLED_TEXT.copy().withColor(-1), DebugOptionsScreen.DISABLED_TEXT.copy().withColor(-4539718), false
                )
                .displayOnlyValue()
                .withCustomNarration(this::narrateButton)
                .create(10, 5, 60, 16, Component.literal(s), (p_465518_, p_465519_) -> this.setValue(location, DebugScreenEntryStatus.NEVER));
            this.children.add(this.never);
            this.children.add(this.overlay);
            this.children.add(this.always);
            this.refreshEntry();
        }

        private MutableComponent narrateButton(CycleButton<Boolean> button) {
            DebugScreenEntryStatus debugscreenentrystatus = DebugOptionsScreen.this.minecraft.debugEntries.getStatus(this.location);
            MutableComponent mutablecomponent = Component.translatable("debug.entry.currently." + debugscreenentrystatus.getSerializedName(), this.name);
            return CommonComponents.optionNameValue(mutablecomponent, button.getMessage());
        }

        private void setValue(Identifier entry, DebugScreenEntryStatus status) {
            DebugOptionsScreen.this.minecraft.debugEntries.setStatus(entry, status);

            for (Button button : DebugOptionsScreen.this.profileButtons) {
                button.active = true;
            }

            this.refreshEntry();
        }

        @Override
        public List<? extends GuiEventListener> children() {
            return this.children;
        }

        @Override
        public List<? extends NarratableEntry> narratables() {
            return this.children;
        }

        @Override
        public void renderContent(GuiGraphics p_439423_, int p_439217_, int p_439543_, boolean p_439889_, float p_439319_) {
            int i = this.getContentX();
            int j = this.getContentY();
            // Neo: Moved 'k' up for 'maxX' calculation for scrolling string render
            int k = i + this.getContentWidth() - this.never.getWidth() - this.overlay.getWidth() - this.always.getWidth();
            // Neo: Scroll entry names that go out of bounds

            p_439423_.drawScrollingString(p_439423_.textRenderer(), DebugOptionsScreen.this.minecraft.font, Component.literal(this.name).withColor(this.isAllowed ? -1 : -8355712), i, k, j + 5);
            if (!this.isAllowed && p_439889_ && p_439217_ < k) {
                p_439423_.setTooltipForNextFrame(DebugOptionsScreen.NOT_ALLOWED_TOOLTIP, p_439217_, p_439543_);
            }

            this.never.setX(k);
            this.overlay.setX(this.never.getX() + this.never.getWidth());
            this.always.setX(this.overlay.getX() + this.overlay.getWidth());
            this.always.setY(j);
            this.overlay.setY(j);
            this.never.setY(j);
            this.always.render(p_439423_, p_439217_, p_439543_, p_439319_);
            this.overlay.render(p_439423_, p_439217_, p_439543_, p_439319_);
            this.never.render(p_439423_, p_439217_, p_439543_, p_439319_);
        }

        @Override
        public void refreshEntry() {
            DebugScreenEntryStatus debugscreenentrystatus = DebugOptionsScreen.this.minecraft.debugEntries.getStatus(this.location);
            this.always.setValue(debugscreenentrystatus == DebugScreenEntryStatus.ALWAYS_ON);
            this.overlay.setValue(debugscreenentrystatus == DebugScreenEntryStatus.IN_OVERLAY);
            this.never.setValue(debugscreenentrystatus == DebugScreenEntryStatus.NEVER);
            this.always.active = !this.always.getValue();
            this.overlay.active = !this.overlay.getValue();
            this.never.active = !this.never.getValue();
        }
    }

    @OnlyIn(Dist.CLIENT)
    public class OptionList extends ContainerObjectSelectionList<DebugOptionsScreen.AbstractOptionEntry> {
        private static final Comparator<Map.Entry<Identifier, DebugScreenEntry>> COMPARATOR = (p_465520_, p_465521_) -> {
            int i = FloatComparators.NATURAL_COMPARATOR.compare(p_465520_.getValue().category().sortKey(), p_465521_.getValue().category().sortKey());
            return i != 0 ? i : p_465520_.getKey().compareTo(p_465521_.getKey());
        };
        private static final int ITEM_HEIGHT = 20;

        public OptionList() {
            super(
                Minecraft.getInstance(),
                DebugOptionsScreen.this.width,
                DebugOptionsScreen.this.layout.getContentHeight(),
                DebugOptionsScreen.this.layout.getHeaderHeight(),
                20
            );
            this.updateSearch("");
        }

        @Override
        public void renderWidget(GuiGraphics p_436046_, int p_435705_, int p_434134_, float p_432783_) {
            super.renderWidget(p_436046_, p_435705_, p_434134_, p_432783_);
        }

        @Override
        public int getRowWidth() {
            return 350;
        }

        public void refreshEntries() {
            this.children().forEach(DebugOptionsScreen.AbstractOptionEntry::refreshEntry);
        }

        public void updateSearch(String query) {
            this.clearEntries();
            // Neo: Custom entry appending logic to better handle searching, categories and sorting
            net.neoforged.neoforge.client.ClientHooks.updateDebugScreenEntriesForSearch(query, category -> addEntry(new CategoryEntry(category.label())), id -> addEntry(new OptionEntry(id)));
            if(false){
            List<Map.Entry<Identifier, DebugScreenEntry>> list = new ArrayList<>(DebugScreenEntries.allEntries().entrySet());
            list.sort(COMPARATOR);
            DebugEntryCategory debugentrycategory = null;

            for (Map.Entry<Identifier, DebugScreenEntry> entry : list) {
                if (entry.getKey().getPath().contains(query)) {
                    DebugEntryCategory debugentrycategory1 = entry.getValue().category();
                    if (!debugentrycategory1.equals(debugentrycategory)) {
                        this.addEntry(DebugOptionsScreen.this.new CategoryEntry(debugentrycategory1.label()));
                        debugentrycategory = debugentrycategory1;
                    }

                    this.addEntry(DebugOptionsScreen.this.new OptionEntry(entry.getKey()));
                }
            }
            }

            this.notifyListUpdated();
        }

        private void notifyListUpdated() {
            this.refreshScrollAmount();
            DebugOptionsScreen.this.triggerImmediateNarration(true);
        }
    }
}
