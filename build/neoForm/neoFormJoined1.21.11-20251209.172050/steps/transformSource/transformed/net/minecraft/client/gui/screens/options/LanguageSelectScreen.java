package net.minecraft.client.gui.screens.options;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.SortedMap;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Options;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.ObjectSelectionList;
import net.minecraft.client.gui.components.StringWidget;
import net.minecraft.client.gui.layouts.LinearLayout;
import net.minecraft.client.gui.screens.AccessibilityOnboardingScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.resources.language.LanguageInfo;
import net.minecraft.client.resources.language.LanguageManager;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.jspecify.annotations.Nullable;

@OnlyIn(Dist.CLIENT)
public class LanguageSelectScreen extends OptionsSubScreen {
    private static final Component WARNING_LABEL = Component.translatable("options.languageAccuracyWarning").withColor(-4539718);
    private static final int FOOTER_HEIGHT = 53;
    private static final Component SEARCH_HINT = Component.translatable("gui.language.search").withStyle(EditBox.SEARCH_HINT_STYLE);
    private static final int SEARCH_BOX_HEIGHT = 15;
    final LanguageManager languageManager;
    private LanguageSelectScreen.@Nullable LanguageSelectionList languageSelectionList;
    private @Nullable EditBox search;

    public LanguageSelectScreen(Screen lastScreen, Options options, LanguageManager languageManager) {
        super(lastScreen, options, Component.translatable("options.language.title"));
        this.languageManager = languageManager;
        this.layout.setFooterHeight(53);
    }

    @Override
    protected void addTitle() {
        LinearLayout linearlayout = this.layout.addToHeader(LinearLayout.vertical().spacing(4));
        linearlayout.defaultCellSetting().alignHorizontallyCenter();
        linearlayout.addChild(new StringWidget(this.title, this.font));
        this.search = linearlayout.addChild(new EditBox(this.font, 0, 0, 200, 15, Component.empty()));
        this.search.setHint(SEARCH_HINT);
        this.search.setResponder(p_465533_ -> {
            if (this.languageSelectionList != null) {
                this.languageSelectionList.filterEntries(p_465533_);
            }
        });
        this.layout.setHeaderHeight((int)(12.0 + 9.0 + 15.0));
    }

    @Override
    protected void setInitialFocus() {
        if (this.search != null) {
            this.setInitialFocus(this.search);
        } else {
            super.setInitialFocus();
        }
    }

    @Override
    protected void addContents() {
        this.languageSelectionList = this.layout.addToContents(new LanguageSelectScreen.LanguageSelectionList(this.minecraft));
    }

    @Override
    protected void addOptions() {
    }

    @Override
    protected void addFooter() {
        LinearLayout linearlayout = this.layout.addToFooter(LinearLayout.vertical()).spacing(8);
        linearlayout.defaultCellSetting().alignHorizontallyCenter();
        linearlayout.addChild(new StringWidget(WARNING_LABEL, this.font));
        LinearLayout linearlayout1 = linearlayout.addChild(LinearLayout.horizontal().spacing(8));
        linearlayout1.addChild(
            Button.builder(Component.translatable("options.font"), p_346158_ -> this.minecraft.setScreen(new FontOptionsScreen(this, this.options))).build()
        );
        linearlayout1.addChild(Button.builder(CommonComponents.GUI_DONE, p_346094_ -> this.onDone()).build());
    }

    @Override
    protected void repositionElements() {
        super.repositionElements();
        if (this.languageSelectionList != null) {
            this.languageSelectionList.updateSize(this.width, this.layout);
        }
    }

    void onDone() {
        if (this.languageSelectionList != null
            && this.languageSelectionList.getSelected() instanceof LanguageSelectScreen.LanguageSelectionList.Entry languageselectscreen$languageselectionlist$entry
            && !languageselectscreen$languageselectionlist$entry.code.equals(this.languageManager.getSelected())) {
            this.languageManager.setSelected(languageselectscreen$languageselectionlist$entry.code);
            this.options.languageCode = languageselectscreen$languageselectionlist$entry.code;
            this.minecraft.reloadResourcePacks();
        }

        this.minecraft.setScreen(this.lastScreen);
    }

    @Override
    protected boolean panoramaShouldSpin() {
        return !(this.lastScreen instanceof AccessibilityOnboardingScreen);
    }

    @OnlyIn(Dist.CLIENT)
    class LanguageSelectionList extends ObjectSelectionList<LanguageSelectScreen.LanguageSelectionList.Entry> {
        public LanguageSelectionList(Minecraft minecraft) {
            super(minecraft, LanguageSelectScreen.this.width, LanguageSelectScreen.this.height - 33 - 53, 33, 18);
            String s = LanguageSelectScreen.this.languageManager.getSelected();
            LanguageSelectScreen.this.languageManager
                .getLanguages()
                .forEach(
                    (p_346086_, p_345379_) -> {
                        LanguageSelectScreen.LanguageSelectionList.Entry languageselectscreen$languageselectionlist$entry = new LanguageSelectScreen.LanguageSelectionList.Entry(
                            p_346086_, p_345379_
                        );
                        this.addEntry(languageselectscreen$languageselectionlist$entry);
                        if (s.equals(p_346086_)) {
                            this.setSelected(languageselectscreen$languageselectionlist$entry);
                        }
                    }
                );
            if (this.getSelected() != null) {
                this.centerScrollOn(this.getSelected());
            }
        }

        void filterEntries(String query) {
            SortedMap<String, LanguageInfo> sortedmap = LanguageSelectScreen.this.languageManager.getLanguages();
            List<LanguageSelectScreen.LanguageSelectionList.Entry> list = sortedmap.entrySet()
                .stream()
                .filter(
                    p_460244_ -> query.isEmpty()
                        || p_460244_.getValue().name().toLowerCase(Locale.ROOT).contains(query.toLowerCase(Locale.ROOT))
                        || p_460244_.getValue().region().toLowerCase(Locale.ROOT).contains(query.toLowerCase(Locale.ROOT))
                )
                .map(p_460242_ -> new LanguageSelectScreen.LanguageSelectionList.Entry(p_460242_.getKey(), p_460242_.getValue()))
                .toList();
            this.replaceEntries(list);
            this.refreshScrollAmount();
        }

        @Override
        public int getRowWidth() {
            return super.getRowWidth() + 50;
        }

        @OnlyIn(Dist.CLIENT)
        public class Entry extends ObjectSelectionList.Entry<LanguageSelectScreen.LanguageSelectionList.Entry> {
            final String code;
            private final Component language;

            public Entry(String code, LanguageInfo languageInfo) {
                this.code = code;
                this.language = languageInfo.toComponent();
            }

            @Override
            public void renderContent(GuiGraphics p_440595_, int p_440193_, int p_440169_, boolean p_440373_, float p_439659_) {
                p_440595_.drawCenteredString(
                    LanguageSelectScreen.this.font, this.language, LanguageSelectionList.this.width / 2, this.getContentYMiddle() - 9 / 2, -1
                );
            }

            @Override
            public boolean keyPressed(KeyEvent p_445511_) {
                if (p_445511_.isSelection()) {
                    this.select();
                    LanguageSelectScreen.this.onDone();
                    return true;
                } else {
                    return super.keyPressed(p_445511_);
                }
            }

            @Override
            public boolean mouseClicked(MouseButtonEvent p_447191_, boolean p_435547_) {
                this.select();
                if (p_435547_) {
                    LanguageSelectScreen.this.onDone();
                }

                return super.mouseClicked(p_447191_, p_435547_);
            }

            private void select() {
                LanguageSelectionList.this.setSelected(this);
            }

            @Override
            public Component getNarration() {
                return Component.translatable("narrator.select", this.language);
            }
        }
    }
}
