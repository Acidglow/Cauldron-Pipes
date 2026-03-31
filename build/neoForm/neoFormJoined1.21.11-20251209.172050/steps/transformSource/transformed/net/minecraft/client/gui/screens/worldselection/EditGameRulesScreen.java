package net.minecraft.client.gui.screens.worldselection;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.google.common.collect.ImmutableList.Builder;
import com.mojang.serialization.DataResult;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.ContainerObjectSelectionList;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.layouts.HeaderAndFooterLayout;
import net.minecraft.client.gui.layouts.LinearLayout;
import net.minecraft.client.gui.narration.NarratableEntry;
import net.minecraft.client.gui.narration.NarratedElementType;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.level.gamerules.GameRule;
import net.minecraft.world.level.gamerules.GameRuleCategory;
import net.minecraft.world.level.gamerules.GameRuleTypeVisitor;
import net.minecraft.world.level.gamerules.GameRules;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.jspecify.annotations.Nullable;

@OnlyIn(Dist.CLIENT)
public class EditGameRulesScreen extends Screen {
    private static final Component TITLE = Component.translatable("editGamerule.title");
    private static final int SPACING = 8;
    final HeaderAndFooterLayout layout = new HeaderAndFooterLayout(this);
    private final Consumer<Optional<GameRules>> exitCallback;
    private final Set<EditGameRulesScreen.RuleEntry> invalidEntries = Sets.newHashSet();
    final GameRules gameRules;
    private EditGameRulesScreen.@Nullable RuleList ruleList;
    private @Nullable Button doneButton;

    public EditGameRulesScreen(GameRules gameRules, Consumer<Optional<GameRules>> exitCallback) {
        super(TITLE);
        this.gameRules = gameRules;
        this.exitCallback = exitCallback;
    }

    @Override
    protected void init() {
        this.layout.addTitleHeader(TITLE, this.font);
        this.ruleList = this.layout.addToContents(new EditGameRulesScreen.RuleList(this.gameRules));
        LinearLayout linearlayout = this.layout.addToFooter(LinearLayout.horizontal().spacing(8));
        this.doneButton = linearlayout.addChild(
            Button.builder(CommonComponents.GUI_DONE, p_460251_ -> this.exitCallback.accept(Optional.of(this.gameRules))).build()
        );
        linearlayout.addChild(Button.builder(CommonComponents.GUI_CANCEL, p_329749_ -> this.onClose()).build());
        this.layout.visitWidgets(p_321377_ -> {
            AbstractWidget abstractwidget = this.addRenderableWidget(p_321377_);
        });
        this.repositionElements();
    }

    @Override
    protected void repositionElements() {
        this.layout.arrangeElements();
        if (this.ruleList != null) {
            this.ruleList.updateSize(this.width, this.layout);
        }
    }

    @Override
    public void onClose() {
        this.exitCallback.accept(Optional.empty());
    }

    private void updateDoneButton() {
        if (this.doneButton != null) {
            this.doneButton.active = this.invalidEntries.isEmpty();
        }
    }

    void markInvalid(EditGameRulesScreen.RuleEntry ruleEntry) {
        this.invalidEntries.add(ruleEntry);
        this.updateDoneButton();
    }

    void clearInvalid(EditGameRulesScreen.RuleEntry ruleEntry) {
        this.invalidEntries.remove(ruleEntry);
        this.updateDoneButton();
    }

    @OnlyIn(Dist.CLIENT)
    public class BooleanRuleEntry extends EditGameRulesScreen.GameRuleEntry {
        private final CycleButton<Boolean> checkbox;

        public BooleanRuleEntry(Component label, List<FormattedCharSequence> tooltip, String description, GameRule<Boolean> gameRule) {
            super(tooltip, label);
            this.checkbox = CycleButton.onOffBuilder(EditGameRulesScreen.this.gameRules.get(gameRule))
                .displayOnlyValue()
                .withCustomNarration(p_170219_ -> p_170219_.createDefaultNarrationMessage().append("\n").append(description))
                .create(10, 5, 44, 20, label, (p_460253_, p_460254_) -> EditGameRulesScreen.this.gameRules.set(gameRule, p_460254_, null));
            this.children.add(this.checkbox);
        }

        @Override
        public void renderContent(GuiGraphics p_439057_, int p_439789_, int p_439072_, boolean p_440132_, float p_440063_) {
            this.renderLabel(p_439057_, this.getContentY(), this.getContentX());
            this.checkbox.setX(this.getContentRight() - 45);
            this.checkbox.setY(this.getContentY());
            this.checkbox.render(p_439057_, p_439789_, p_439072_, p_440063_);
        }
    }

    @OnlyIn(Dist.CLIENT)
    public class CategoryRuleEntry extends EditGameRulesScreen.RuleEntry {
        final Component label;

        public CategoryRuleEntry(Component label) {
            super(null);
            this.label = label;
        }

        @Override
        public void renderContent(GuiGraphics p_440317_, int p_439215_, int p_440653_, boolean p_439060_, float p_439068_) {
            p_440317_.drawCenteredString(EditGameRulesScreen.this.minecraft.font, this.label, this.getContentXMiddle(), this.getContentY() + 5, -1);
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
                public void updateNarration(NarrationElementOutput p_170225_) {
                    p_170225_.add(NarratedElementType.TITLE, CategoryRuleEntry.this.label);
                }
            });
        }
    }

    @FunctionalInterface
    @OnlyIn(Dist.CLIENT)
    interface EntryFactory<T> {
        EditGameRulesScreen.RuleEntry create(Component label, List<FormattedCharSequence> tooltip, String description, GameRule<T> gameRule);
    }

    @OnlyIn(Dist.CLIENT)
    public abstract class GameRuleEntry extends EditGameRulesScreen.RuleEntry {
        private final List<FormattedCharSequence> label;
        protected final List<AbstractWidget> children = Lists.newArrayList();

        public GameRuleEntry(@Nullable List<FormattedCharSequence> tooltip, Component label) {
            super(tooltip);
            this.label = EditGameRulesScreen.this.minecraft.font.split(label, 175);
        }

        @Override
        public List<? extends GuiEventListener> children() {
            return this.children;
        }

        @Override
        public List<? extends NarratableEntry> narratables() {
            return this.children;
        }

        protected void renderLabel(GuiGraphics guiGraphics, int x, int y) {
            if (this.label.size() == 1) {
                guiGraphics.drawString(EditGameRulesScreen.this.minecraft.font, this.label.get(0), y, x + 5, -1);
            } else if (this.label.size() >= 2) {
                guiGraphics.drawString(EditGameRulesScreen.this.minecraft.font, this.label.get(0), y, x, -1);
                guiGraphics.drawString(EditGameRulesScreen.this.minecraft.font, this.label.get(1), y, x + 10, -1);
            }
        }
    }

    @OnlyIn(Dist.CLIENT)
    public class IntegerRuleEntry extends EditGameRulesScreen.GameRuleEntry {
        private final EditBox input;

        public IntegerRuleEntry(Component label, List<FormattedCharSequence> tooltip, String description, GameRule<Integer> gameRule) {
            super(tooltip, label);
            this.input = new EditBox(EditGameRulesScreen.this.minecraft.font, 10, 5, 44, 20, label.copy().append("\n").append(description).append("\n"));
            this.input.setValue(EditGameRulesScreen.this.gameRules.getAsString(gameRule));
            this.input.setResponder(p_460256_ -> {
                DataResult<Integer> dataresult = gameRule.deserialize(p_460256_);
                if (dataresult.isSuccess()) {
                    this.input.setTextColor(-2039584);
                    EditGameRulesScreen.this.clearInvalid(this);
                    EditGameRulesScreen.this.gameRules.set(gameRule, dataresult.getOrThrow(), null);
                } else {
                    this.input.setTextColor(-65536);
                    EditGameRulesScreen.this.markInvalid(this);
                }
            });
            this.children.add(this.input);
        }

        @Override
        public void renderContent(GuiGraphics p_439470_, int p_438942_, int p_439225_, boolean p_440092_, float p_440277_) {
            this.renderLabel(p_439470_, this.getContentY(), this.getContentX());
            this.input.setX(this.getContentRight() - 45);
            this.input.setY(this.getContentY());
            this.input.render(p_439470_, p_438942_, p_439225_, p_440277_);
        }
    }

    @OnlyIn(Dist.CLIENT)
    public abstract static class RuleEntry extends ContainerObjectSelectionList.Entry<EditGameRulesScreen.RuleEntry> {
        final @Nullable List<FormattedCharSequence> tooltip;

        public RuleEntry(@Nullable List<FormattedCharSequence> tooltip) {
            this.tooltip = tooltip;
        }
    }

    @OnlyIn(Dist.CLIENT)
    public class RuleList extends ContainerObjectSelectionList<EditGameRulesScreen.RuleEntry> {
        private static final int ITEM_HEIGHT = 24;

        public RuleList(GameRules gameRules) {
            super(
                Minecraft.getInstance(),
                EditGameRulesScreen.this.width,
                EditGameRulesScreen.this.layout.getContentHeight(),
                EditGameRulesScreen.this.layout.getHeaderHeight(),
                24
            );
            final Map<GameRuleCategory, Map<GameRule<?>, EditGameRulesScreen.RuleEntry>> map = Maps.newHashMap();
            gameRules.visitGameRuleTypes(
                new GameRuleTypeVisitor() {
                    @Override
                    public void visitBoolean(GameRule<Boolean> p_460836_) {
                        this.addEntry(
                            p_460836_,
                            (p_460262_, p_460263_, p_460264_, p_460265_) -> EditGameRulesScreen.this.new BooleanRuleEntry(
                                p_460262_, p_460263_, p_460264_, p_460265_
                            )
                        );
                    }

                    @Override
                    public void visitInteger(GameRule<Integer> p_461172_) {
                        this.addEntry(
                            p_461172_,
                            (p_460258_, p_460259_, p_460260_, p_460261_) -> EditGameRulesScreen.this.new IntegerRuleEntry(
                                p_460258_, p_460259_, p_460260_, p_460261_
                            )
                        );
                    }

                    private <T> void addEntry(GameRule<T> gameRule, EditGameRulesScreen.EntryFactory<T> factory) {
                        Component component = Component.translatable(gameRule.getDescriptionId());
                        Component component1 = Component.literal(gameRule.id()).withStyle(ChatFormatting.YELLOW);
                        Component component2 = Component.translatable("editGamerule.default", Component.literal(gameRule.serialize(gameRule.defaultValue())))
                            .withStyle(ChatFormatting.GRAY);
                        String s = gameRule.getDescriptionId() + ".description";
                        List<FormattedCharSequence> list;
                        String s1;
                        if (I18n.exists(s)) {
                            Builder<FormattedCharSequence> builder = ImmutableList.<FormattedCharSequence>builder().add(component1.getVisualOrderText());
                            Component component3 = Component.translatable(s);
                            EditGameRulesScreen.this.font.split(component3, 150).forEach(builder::add);
                            list = builder.add(component2.getVisualOrderText()).build();
                            s1 = component3.getString() + "\n" + component2.getString();
                        } else {
                            list = ImmutableList.of(component1.getVisualOrderText(), component2.getVisualOrderText());
                            s1 = component2.getString();
                        }

                        map.computeIfAbsent(gameRule.category(), p_461130_ -> Maps.newHashMap())
                            .put(gameRule, factory.create(component, list, s1, gameRule));
                    }
                }
            );
            map.entrySet()
                .stream()
                .sorted(Map.Entry.comparingByKey(Comparator.comparing(GameRuleCategory::getDescriptionId)))
                .forEach(
                    p_460257_ -> {
                        this.addEntry(
                            EditGameRulesScreen.this.new CategoryRuleEntry(p_460257_.getKey().label().withStyle(ChatFormatting.BOLD, ChatFormatting.YELLOW))
                        );
                        p_460257_.getValue()
                            .entrySet()
                            .stream()
                            .sorted(Map.Entry.comparingByKey(Comparator.comparing(GameRule::getDescriptionId)))
                            .forEach(p_170229_ -> this.addEntry(p_170229_.getValue()));
                    }
                );
        }

        @Override
        public void renderWidget(GuiGraphics p_313903_, int p_313824_, int p_313867_, float p_313845_) {
            super.renderWidget(p_313903_, p_313824_, p_313867_, p_313845_);
            EditGameRulesScreen.RuleEntry editgamerulesscreen$ruleentry = this.getHovered();
            if (editgamerulesscreen$ruleentry != null && editgamerulesscreen$ruleentry.tooltip != null) {
                p_313903_.setTooltipForNextFrame(editgamerulesscreen$ruleentry.tooltip, p_313824_, p_313867_);
            }
        }
    }
}
