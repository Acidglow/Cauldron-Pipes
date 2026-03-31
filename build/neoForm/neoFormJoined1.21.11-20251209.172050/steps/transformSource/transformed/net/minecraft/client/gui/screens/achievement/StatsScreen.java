package net.minecraft.client.gui.screens.achievement;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractSelectionList;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.ContainerObjectSelectionList;
import net.minecraft.client.gui.components.ImageButton;
import net.minecraft.client.gui.components.ItemDisplayWidget;
import net.minecraft.client.gui.components.ObjectSelectionList;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.components.WidgetSprites;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.components.tabs.GridLayoutTab;
import net.minecraft.client.gui.components.tabs.LoadingTab;
import net.minecraft.client.gui.components.tabs.Tab;
import net.minecraft.client.gui.components.tabs.TabManager;
import net.minecraft.client.gui.components.tabs.TabNavigationBar;
import net.minecraft.client.gui.layouts.HeaderAndFooterLayout;
import net.minecraft.client.gui.narration.NarratableEntry;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.worldselection.CreateWorldScreen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ServerboundClientCommandPacket;
import net.minecraft.resources.Identifier;
import net.minecraft.stats.Stat;
import net.minecraft.stats.StatType;
import net.minecraft.stats.Stats;
import net.minecraft.stats.StatsCounter;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.jspecify.annotations.Nullable;

@OnlyIn(Dist.CLIENT)
public class StatsScreen extends Screen {
    private static final Component TITLE = Component.translatable("gui.stats");
    static final Identifier SLOT_SPRITE = Identifier.withDefaultNamespace("container/slot");
    static final Identifier HEADER_SPRITE = Identifier.withDefaultNamespace("statistics/header");
    static final Identifier SORT_UP_SPRITE = Identifier.withDefaultNamespace("statistics/sort_up");
    static final Identifier SORT_DOWN_SPRITE = Identifier.withDefaultNamespace("statistics/sort_down");
    private static final Component PENDING_TEXT = Component.translatable("multiplayer.downloadingStats");
    static final Component NO_VALUE_DISPLAY = Component.translatable("stats.none");
    private static final Component GENERAL_BUTTON = Component.translatable("stat.generalButton");
    private static final Component ITEMS_BUTTON = Component.translatable("stat.itemsButton");
    private static final Component MOBS_BUTTON = Component.translatable("stat.mobsButton");
    protected final Screen lastScreen;
    private static final int LIST_WIDTH = 280;
    final HeaderAndFooterLayout layout = new HeaderAndFooterLayout(this);
    private final TabManager tabManager = new TabManager(p_329726_ -> {
        AbstractWidget abstractwidget = this.addRenderableWidget(p_329726_);
    }, p_438733_ -> this.removeWidget(p_438733_));
    private @Nullable TabNavigationBar tabNavigationBar;
    final StatsCounter stats;
    /**
     * When true, the game will be paused when the gui is shown
     */
    private boolean isLoading = true;

    public StatsScreen(Screen lastScreen, StatsCounter stats) {
        super(TITLE);
        this.lastScreen = lastScreen;
        this.stats = stats;
    }

    @Override
    protected void init() {
        Component component = PENDING_TEXT;
        this.tabNavigationBar = TabNavigationBar.builder(this.tabManager, this.width)
            .addTabs(
                new LoadingTab(this.getFont(), GENERAL_BUTTON, component),
                new LoadingTab(this.getFont(), ITEMS_BUTTON, component),
                new LoadingTab(this.getFont(), MOBS_BUTTON, component)
            )
            .build();
        this.addRenderableWidget(this.tabNavigationBar);
        this.layout.addToFooter(Button.builder(CommonComponents.GUI_DONE, p_329727_ -> this.onClose()).width(200).build());
        this.tabNavigationBar.setTabActiveState(0, true);
        this.tabNavigationBar.setTabActiveState(1, false);
        this.tabNavigationBar.setTabActiveState(2, false);
        this.layout.visitWidgets(p_438736_ -> {
            p_438736_.setTabOrderGroup(1);
            this.addRenderableWidget(p_438736_);
        });
        this.tabNavigationBar.selectTab(0, false);
        this.repositionElements();
        this.minecraft.getConnection().send(new ServerboundClientCommandPacket(ServerboundClientCommandPacket.Action.REQUEST_STATS));
    }

    public void onStatsUpdated() {
        if (this.isLoading) {
            if (this.tabNavigationBar != null) {
                this.removeWidget(this.tabNavigationBar);
            }

            this.tabNavigationBar = TabNavigationBar.builder(this.tabManager, this.width)
                .addTabs(
                    new StatsScreen.StatisticsTab(GENERAL_BUTTON, new StatsScreen.GeneralStatisticsList(this.minecraft)),
                    new StatsScreen.StatisticsTab(ITEMS_BUTTON, new StatsScreen.ItemStatisticsList(this.minecraft)),
                    new StatsScreen.StatisticsTab(MOBS_BUTTON, new StatsScreen.MobsStatisticsList(this.minecraft))
                )
                .build();
            this.setFocused(this.tabNavigationBar);
            this.addRenderableWidget(this.tabNavigationBar);
            this.setTabActiveStateAndTooltip(1);
            this.setTabActiveStateAndTooltip(2);
            this.tabNavigationBar.selectTab(0, false);
            this.repositionElements();
            this.isLoading = false;
        }
    }

    private void setTabActiveStateAndTooltip(int index) {
        if (this.tabNavigationBar != null) {
            boolean flag = this.tabNavigationBar.getTabs().get(index) instanceof StatsScreen.StatisticsTab statsscreen$statisticstab
                && !statsscreen$statisticstab.list.children().isEmpty();
            this.tabNavigationBar.setTabActiveState(index, flag);
            if (flag) {
                this.tabNavigationBar.setTabTooltip(index, null);
            } else {
                this.tabNavigationBar.setTabTooltip(index, Tooltip.create(Component.translatable("gui.stats.none_found")));
            }
        }
    }

    @Override
    protected void repositionElements() {
        if (this.tabNavigationBar != null) {
            this.tabNavigationBar.setWidth(this.width);
            this.tabNavigationBar.arrangeElements();
            int i = this.tabNavigationBar.getRectangle().bottom();
            ScreenRectangle screenrectangle = new ScreenRectangle(0, i, this.width, this.height - this.layout.getFooterHeight() - i);
            this.tabNavigationBar.getTabs().forEach(p_438735_ -> p_438735_.visitChildren(p_438738_ -> p_438738_.setHeight(screenrectangle.height())));
            this.tabManager.setTabArea(screenrectangle);
            this.layout.setHeaderHeight(i);
            this.layout.arrangeElements();
        }
    }

    @Override
    public boolean keyPressed(KeyEvent p_446909_) {
        return this.tabNavigationBar != null && this.tabNavigationBar.keyPressed(p_446909_) ? true : super.keyPressed(p_446909_);
    }

    @Override
    public void render(GuiGraphics p_440644_, int p_440632_, int p_440325_, float p_440049_) {
        super.render(p_440644_, p_440632_, p_440325_, p_440049_);
        p_440644_.blit(RenderPipelines.GUI_TEXTURED, Screen.FOOTER_SEPARATOR, 0, this.height - this.layout.getFooterHeight(), 0.0F, 0.0F, this.width, 2, 32, 2);
    }

    @Override
    protected void renderMenuBackground(GuiGraphics p_439627_) {
        p_439627_.blit(
            RenderPipelines.GUI_TEXTURED, CreateWorldScreen.TAB_HEADER_BACKGROUND, 0, 0, 0.0F, 0.0F, this.width, this.layout.getHeaderHeight(), 16, 16
        );
        this.renderMenuBackground(p_439627_, 0, this.layout.getHeaderHeight(), this.width, this.height);
    }

    @Override
    public void onClose() {
        this.minecraft.setScreen(this.lastScreen);
    }

    static String getTranslationKey(Stat<Identifier> stat) {
        return "stat." + stat.getValue().toString().replace(':', '.');
    }

    @OnlyIn(Dist.CLIENT)
    class GeneralStatisticsList extends ObjectSelectionList<StatsScreen.GeneralStatisticsList.Entry> {
        public GeneralStatisticsList(Minecraft minecraft) {
            super(minecraft, StatsScreen.this.width, StatsScreen.this.layout.getContentHeight(), 33, 14);
            ObjectArrayList<Stat<Identifier>> objectarraylist = new ObjectArrayList<>(Stats.CUSTOM.iterator());
            objectarraylist.sort(Comparator.comparing(p_96997_ -> I18n.get(StatsScreen.getTranslationKey((Stat<Identifier>)p_96997_))));

            for (Stat<Identifier> stat : objectarraylist) {
                this.addEntry(new StatsScreen.GeneralStatisticsList.Entry(stat));
            }
        }

        @Override
        public int getRowWidth() {
            return 280;
        }

        @Override
        protected void renderListBackground(GuiGraphics p_440421_) {
        }

        @Override
        protected void renderListSeparators(GuiGraphics p_440335_) {
        }

        @OnlyIn(Dist.CLIENT)
        class Entry extends ObjectSelectionList.Entry<StatsScreen.GeneralStatisticsList.Entry> {
            private final Stat<Identifier> stat;
            private final Component statDisplay;

            Entry(Stat<Identifier> stat) {
                this.stat = stat;
                this.statDisplay = Component.translatable(StatsScreen.getTranslationKey(stat));
            }

            private String getValueText() {
                return this.stat.format(StatsScreen.this.stats.getValue(this.stat));
            }

            @Override
            public void renderContent(GuiGraphics p_440694_, int p_439631_, int p_439784_, boolean p_438916_, float p_440067_) {
                int i = this.getContentYMiddle() - 9 / 2;
                int j = GeneralStatisticsList.this.children().indexOf(this);
                int k = j % 2 == 0 ? -1 : -4539718;
                p_440694_.drawString(StatsScreen.this.font, this.statDisplay, this.getContentX() + 2, i, k);
                String s = this.getValueText();
                p_440694_.drawString(StatsScreen.this.font, s, this.getContentRight() - StatsScreen.this.font.width(s) - 4, i, k);
            }

            @Override
            public Component getNarration() {
                return Component.translatable(
                    "narrator.select", Component.empty().append(this.statDisplay).append(CommonComponents.SPACE).append(this.getValueText())
                );
            }
        }
    }

    @OnlyIn(Dist.CLIENT)
    class ItemStatisticsList extends ContainerObjectSelectionList<StatsScreen.ItemStatisticsList.Entry> {
        private static final int SLOT_BG_SIZE = 18;
        private static final int SLOT_STAT_HEIGHT = 22;
        private static final int SLOT_BG_Y = 1;
        private static final int SORT_NONE = 0;
        private static final int SORT_DOWN = -1;
        private static final int SORT_UP = 1;
        protected final List<StatType<Block>> blockColumns;
        protected final List<StatType<Item>> itemColumns;
        protected final Comparator<StatsScreen.ItemStatisticsList.ItemRow> itemStatSorter = new StatsScreen.ItemStatisticsList.ItemRowComparator();
        protected @Nullable StatType<?> sortColumn;
        protected int sortOrder;

        public ItemStatisticsList(Minecraft minecraft) {
            super(minecraft, StatsScreen.this.width, StatsScreen.this.layout.getContentHeight(), 33, 22);
            this.blockColumns = Lists.newArrayList();
            this.blockColumns.add(Stats.BLOCK_MINED);
            this.itemColumns = Lists.newArrayList(Stats.ITEM_BROKEN, Stats.ITEM_CRAFTED, Stats.ITEM_USED, Stats.ITEM_PICKED_UP, Stats.ITEM_DROPPED);
            Set<Item> set = Sets.newIdentityHashSet();

            for (Item item : BuiltInRegistries.ITEM) {
                boolean flag = false;

                for (StatType<Item> stattype : this.itemColumns) {
                    if (stattype.contains(item) && StatsScreen.this.stats.getValue(stattype.get(item)) > 0) {
                        flag = true;
                    }
                }

                if (flag) {
                    set.add(item);
                }
            }

            for (Block block : BuiltInRegistries.BLOCK) {
                boolean flag1 = false;

                for (StatType<Block> stattype1 : this.blockColumns) {
                    if (stattype1.contains(block) && StatsScreen.this.stats.getValue(stattype1.get(block)) > 0) {
                        flag1 = true;
                    }
                }

                if (flag1) {
                    set.add(block.asItem());
                }
            }

            set.remove(Items.AIR);
            if (!set.isEmpty()) {
                this.addEntry(new StatsScreen.ItemStatisticsList.HeaderEntry());

                for (Item item1 : set) {
                    this.addEntry(new StatsScreen.ItemStatisticsList.ItemRow(item1));
                }
            }
        }

        @Override
        protected void renderListBackground(GuiGraphics p_440100_) {
        }

        int getColumnX(int index) {
            return 75 + 40 * index;
        }

        @Override
        public int getRowWidth() {
            return 280;
        }

        StatType<?> getColumn(int index) {
            return index < this.blockColumns.size() ? this.blockColumns.get(index) : this.itemColumns.get(index - this.blockColumns.size());
        }

        int getColumnIndex(StatType<?> statType) {
            int i = this.blockColumns.indexOf(statType);
            if (i >= 0) {
                return i;
            } else {
                int j = this.itemColumns.indexOf(statType);
                return j >= 0 ? j + this.blockColumns.size() : -1;
            }
        }

        protected void sortByColumn(StatType<?> statType) {
            if (statType != this.sortColumn) {
                this.sortColumn = statType;
                this.sortOrder = -1;
            } else if (this.sortOrder == -1) {
                this.sortOrder = 1;
            } else {
                this.sortColumn = null;
                this.sortOrder = 0;
            }

            this.sortItems(this.itemStatSorter);
        }

        protected void sortItems(Comparator<StatsScreen.ItemStatisticsList.ItemRow> comparator) {
            List<StatsScreen.ItemStatisticsList.ItemRow> list = this.getItemRows();
            list.sort(comparator);
            this.clearEntriesExcept(this.children().getFirst());

            for (StatsScreen.ItemStatisticsList.ItemRow statsscreen$itemstatisticslist$itemrow : list) {
                this.addEntry(statsscreen$itemstatisticslist$itemrow);
            }
        }

        private List<StatsScreen.ItemStatisticsList.ItemRow> getItemRows() {
            List<StatsScreen.ItemStatisticsList.ItemRow> list = new ArrayList<>();
            this.children().forEach(p_440299_ -> {
                if (p_440299_ instanceof StatsScreen.ItemStatisticsList.ItemRow statsscreen$itemstatisticslist$itemrow) {
                    list.add(statsscreen$itemstatisticslist$itemrow);
                }
            });
            return list;
        }

        @Override
        protected void renderListSeparators(GuiGraphics p_439722_) {
        }

        @OnlyIn(Dist.CLIENT)
        abstract static class Entry extends ContainerObjectSelectionList.Entry<StatsScreen.ItemStatisticsList.Entry> {
        }

        @OnlyIn(Dist.CLIENT)
        class HeaderEntry extends StatsScreen.ItemStatisticsList.Entry {
            private static final Identifier BLOCK_MINED_SPRITE = Identifier.withDefaultNamespace("statistics/block_mined");
            private static final Identifier ITEM_BROKEN_SPRITE = Identifier.withDefaultNamespace("statistics/item_broken");
            private static final Identifier ITEM_CRAFTED_SPRITE = Identifier.withDefaultNamespace("statistics/item_crafted");
            private static final Identifier ITEM_USED_SPRITE = Identifier.withDefaultNamespace("statistics/item_used");
            private static final Identifier ITEM_PICKED_UP_SPRITE = Identifier.withDefaultNamespace("statistics/item_picked_up");
            private static final Identifier ITEM_DROPPED_SPRITE = Identifier.withDefaultNamespace("statistics/item_dropped");
            private final StatsScreen.ItemStatisticsList.HeaderEntry.StatSortButton blockMined;
            private final StatsScreen.ItemStatisticsList.HeaderEntry.StatSortButton itemBroken;
            private final StatsScreen.ItemStatisticsList.HeaderEntry.StatSortButton itemCrafted;
            private final StatsScreen.ItemStatisticsList.HeaderEntry.StatSortButton itemUsed;
            private final StatsScreen.ItemStatisticsList.HeaderEntry.StatSortButton itemPickedUp;
            private final StatsScreen.ItemStatisticsList.HeaderEntry.StatSortButton itemDropped;
            private final List<AbstractWidget> children = new ArrayList<>();

            HeaderEntry() {
                this.blockMined = new StatsScreen.ItemStatisticsList.HeaderEntry.StatSortButton(0, BLOCK_MINED_SPRITE);
                this.itemBroken = new StatsScreen.ItemStatisticsList.HeaderEntry.StatSortButton(1, ITEM_BROKEN_SPRITE);
                this.itemCrafted = new StatsScreen.ItemStatisticsList.HeaderEntry.StatSortButton(2, ITEM_CRAFTED_SPRITE);
                this.itemUsed = new StatsScreen.ItemStatisticsList.HeaderEntry.StatSortButton(3, ITEM_USED_SPRITE);
                this.itemPickedUp = new StatsScreen.ItemStatisticsList.HeaderEntry.StatSortButton(4, ITEM_PICKED_UP_SPRITE);
                this.itemDropped = new StatsScreen.ItemStatisticsList.HeaderEntry.StatSortButton(5, ITEM_DROPPED_SPRITE);
                this.children.addAll(List.of(this.blockMined, this.itemBroken, this.itemCrafted, this.itemUsed, this.itemPickedUp, this.itemDropped));
            }

            @Override
            public void renderContent(GuiGraphics p_440439_, int p_440493_, int p_439039_, boolean p_438946_, float p_438988_) {
                this.blockMined.setPosition(this.getContentX() + ItemStatisticsList.this.getColumnX(0) - 18, this.getContentY() + 1);
                this.blockMined.render(p_440439_, p_440493_, p_439039_, p_438988_);
                this.itemBroken.setPosition(this.getContentX() + ItemStatisticsList.this.getColumnX(1) - 18, this.getContentY() + 1);
                this.itemBroken.render(p_440439_, p_440493_, p_439039_, p_438988_);
                this.itemCrafted.setPosition(this.getContentX() + ItemStatisticsList.this.getColumnX(2) - 18, this.getContentY() + 1);
                this.itemCrafted.render(p_440439_, p_440493_, p_439039_, p_438988_);
                this.itemUsed.setPosition(this.getContentX() + ItemStatisticsList.this.getColumnX(3) - 18, this.getContentY() + 1);
                this.itemUsed.render(p_440439_, p_440493_, p_439039_, p_438988_);
                this.itemPickedUp.setPosition(this.getContentX() + ItemStatisticsList.this.getColumnX(4) - 18, this.getContentY() + 1);
                this.itemPickedUp.render(p_440439_, p_440493_, p_439039_, p_438988_);
                this.itemDropped.setPosition(this.getContentX() + ItemStatisticsList.this.getColumnX(5) - 18, this.getContentY() + 1);
                this.itemDropped.render(p_440439_, p_440493_, p_439039_, p_438988_);
                if (ItemStatisticsList.this.sortColumn != null) {
                    int i = ItemStatisticsList.this.getColumnX(ItemStatisticsList.this.getColumnIndex(ItemStatisticsList.this.sortColumn)) - 36;
                    Identifier identifier = ItemStatisticsList.this.sortOrder == 1 ? StatsScreen.SORT_UP_SPRITE : StatsScreen.SORT_DOWN_SPRITE;
                    p_440439_.blitSprite(RenderPipelines.GUI_TEXTURED, identifier, this.getContentX() + i, this.getContentY() + 1, 18, 18);
                }
            }

            @Override
            public List<? extends GuiEventListener> children() {
                return this.children;
            }

            @Override
            public List<? extends NarratableEntry> narratables() {
                return this.children;
            }

            @OnlyIn(Dist.CLIENT)
            class StatSortButton extends ImageButton {
                private final Identifier sprite;

                StatSortButton(int column, Identifier sprite) {
                    super(
                        18,
                        18,
                        new WidgetSprites(StatsScreen.HEADER_SPRITE, StatsScreen.SLOT_SPRITE),
                        p_440669_ -> ItemStatisticsList.this.sortByColumn(ItemStatisticsList.this.getColumn(column)),
                        ItemStatisticsList.this.getColumn(column).getDisplayName()
                    );
                    this.sprite = sprite;
                    this.setTooltip(Tooltip.create(this.getMessage()));
                }

                @Override
                public void renderContents(GuiGraphics p_440197_, int p_439800_, int p_440460_, float p_439309_) {
                    Identifier identifier = this.sprites.get(this.isActive(), this.isHoveredOrFocused());
                    p_440197_.blitSprite(RenderPipelines.GUI_TEXTURED, identifier, this.getX(), this.getY(), this.width, this.height);
                    p_440197_.blitSprite(RenderPipelines.GUI_TEXTURED, this.sprite, this.getX(), this.getY(), this.width, this.height);
                }
            }
        }

        @OnlyIn(Dist.CLIENT)
        class ItemRow extends StatsScreen.ItemStatisticsList.Entry {
            private final Item item;
            private final StatsScreen.ItemStatisticsList.ItemRow.ItemRowWidget itemRowWidget;

            ItemRow(Item item) {
                this.item = item;
                this.itemRowWidget = new StatsScreen.ItemStatisticsList.ItemRow.ItemRowWidget(item.getDefaultInstance());
            }

            protected Item getItem() {
                return this.item;
            }

            @Override
            public void renderContent(GuiGraphics p_439213_, int p_439189_, int p_439456_, boolean p_438914_, float p_440285_) {
                this.itemRowWidget.setPosition(this.getContentX(), this.getContentY());
                this.itemRowWidget.render(p_439213_, p_439189_, p_439456_, p_440285_);
                StatsScreen.ItemStatisticsList statsscreen$itemstatisticslist = ItemStatisticsList.this;
                int i = statsscreen$itemstatisticslist.children().indexOf(this);

                for (int j = 0; j < statsscreen$itemstatisticslist.blockColumns.size(); j++) {
                    Stat<Block> stat;
                    if (this.item instanceof BlockItem blockitem) {
                        stat = statsscreen$itemstatisticslist.blockColumns.get(j).get(blockitem.getBlock());
                    } else {
                        stat = null;
                    }

                    this.renderStat(p_439213_, stat, this.getContentX() + ItemStatisticsList.this.getColumnX(j), this.getContentYMiddle() - 9 / 2, i % 2 == 0);
                }

                for (int k = 0; k < statsscreen$itemstatisticslist.itemColumns.size(); k++) {
                    this.renderStat(
                        p_439213_,
                        statsscreen$itemstatisticslist.itemColumns.get(k).get(this.item),
                        this.getContentX() + ItemStatisticsList.this.getColumnX(k + statsscreen$itemstatisticslist.blockColumns.size()),
                        this.getContentYMiddle() - 9 / 2,
                        i % 2 == 0
                    );
                }
            }

            protected void renderStat(GuiGraphics guiGraphics, @Nullable Stat<?> stat, int x, int y, boolean evenRow) {
                Component component = (Component)(stat == null
                    ? StatsScreen.NO_VALUE_DISPLAY
                    : Component.literal(stat.format(StatsScreen.this.stats.getValue(stat))));
                guiGraphics.drawString(StatsScreen.this.font, component, x - StatsScreen.this.font.width(component), y, evenRow ? -1 : -4539718);
            }

            @Override
            public List<? extends NarratableEntry> narratables() {
                return List.of(this.itemRowWidget);
            }

            @Override
            public List<? extends GuiEventListener> children() {
                return List.of(this.itemRowWidget);
            }

            @OnlyIn(Dist.CLIENT)
            class ItemRowWidget extends ItemDisplayWidget {
                ItemRowWidget(ItemStack stack) {
                    super(ItemStatisticsList.this.minecraft, 1, 1, 18, 18, stack.getHoverName(), stack, false, true);
                }

                @Override
                protected void renderWidget(GuiGraphics p_440697_, int p_439428_, int p_438999_, float p_440615_) {
                    p_440697_.blitSprite(RenderPipelines.GUI_TEXTURED, StatsScreen.SLOT_SPRITE, ItemRow.this.getContentX(), ItemRow.this.getContentY(), 18, 18);
                    super.renderWidget(p_440697_, p_439428_, p_438999_, p_440615_);
                }

                @Override
                protected void renderTooltip(GuiGraphics p_440443_, int p_440023_, int p_440265_) {
                    super.renderTooltip(p_440443_, ItemRow.this.getContentX() + 18, ItemRow.this.getContentY() + 18);
                }
            }
        }

        @OnlyIn(Dist.CLIENT)
        class ItemRowComparator implements Comparator<StatsScreen.ItemStatisticsList.ItemRow> {
            public int compare(StatsScreen.ItemStatisticsList.ItemRow row1, StatsScreen.ItemStatisticsList.ItemRow row2) {
                Item item = row1.getItem();
                Item item1 = row2.getItem();
                int i;
                int j;
                if (ItemStatisticsList.this.sortColumn == null) {
                    i = 0;
                    j = 0;
                } else if (ItemStatisticsList.this.blockColumns.contains(ItemStatisticsList.this.sortColumn)) {
                    StatType<Block> stattype = (StatType<Block>)ItemStatisticsList.this.sortColumn;
                    i = item instanceof BlockItem ? StatsScreen.this.stats.getValue(stattype, ((BlockItem)item).getBlock()) : -1;
                    j = item1 instanceof BlockItem ? StatsScreen.this.stats.getValue(stattype, ((BlockItem)item1).getBlock()) : -1;
                } else {
                    StatType<Item> stattype1 = (StatType<Item>)ItemStatisticsList.this.sortColumn;
                    i = StatsScreen.this.stats.getValue(stattype1, item);
                    j = StatsScreen.this.stats.getValue(stattype1, item1);
                }

                return i == j
                    ? ItemStatisticsList.this.sortOrder * Integer.compare(Item.getId(item), Item.getId(item1))
                    : ItemStatisticsList.this.sortOrder * Integer.compare(i, j);
            }
        }
    }

    @OnlyIn(Dist.CLIENT)
    class MobsStatisticsList extends ObjectSelectionList<StatsScreen.MobsStatisticsList.MobRow> {
        public MobsStatisticsList(Minecraft minecraft) {
            super(minecraft, StatsScreen.this.width, StatsScreen.this.layout.getContentHeight(), 33, 9 * 4);

            for (EntityType<?> entitytype : BuiltInRegistries.ENTITY_TYPE) {
                if (StatsScreen.this.stats.getValue(Stats.ENTITY_KILLED.get(entitytype)) > 0
                    || StatsScreen.this.stats.getValue(Stats.ENTITY_KILLED_BY.get(entitytype)) > 0) {
                    this.addEntry(new StatsScreen.MobsStatisticsList.MobRow(entitytype));
                }
            }
        }

        @Override
        public int getRowWidth() {
            return 280;
        }

        @Override
        protected void renderListBackground(GuiGraphics p_439574_) {
        }

        @Override
        protected void renderListSeparators(GuiGraphics p_438993_) {
        }

        @OnlyIn(Dist.CLIENT)
        class MobRow extends ObjectSelectionList.Entry<StatsScreen.MobsStatisticsList.MobRow> {
            private final Component mobName;
            private final Component kills;
            private final Component killedBy;
            private final boolean hasKills;
            private final boolean wasKilledBy;

            public MobRow(EntityType<?> entityType) {
                this.mobName = entityType.getDescription();
                int i = StatsScreen.this.stats.getValue(Stats.ENTITY_KILLED.get(entityType));
                if (i == 0) {
                    this.kills = Component.translatable("stat_type.minecraft.killed.none", this.mobName);
                    this.hasKills = false;
                } else {
                    this.kills = Component.translatable("stat_type.minecraft.killed", i, this.mobName);
                    this.hasKills = true;
                }

                int j = StatsScreen.this.stats.getValue(Stats.ENTITY_KILLED_BY.get(entityType));
                if (j == 0) {
                    this.killedBy = Component.translatable("stat_type.minecraft.killed_by.none", this.mobName);
                    this.wasKilledBy = false;
                } else {
                    this.killedBy = Component.translatable("stat_type.minecraft.killed_by", this.mobName, j);
                    this.wasKilledBy = true;
                }
            }

            @Override
            public void renderContent(GuiGraphics p_439408_, int p_438883_, int p_438911_, boolean p_440696_, float p_439230_) {
                p_439408_.drawString(StatsScreen.this.font, this.mobName, this.getContentX() + 2, this.getContentY() + 1, -1);
                p_439408_.drawString(
                    StatsScreen.this.font, this.kills, this.getContentX() + 2 + 10, this.getContentY() + 1 + 9, this.hasKills ? -4539718 : -8355712
                );
                p_439408_.drawString(
                    StatsScreen.this.font, this.killedBy, this.getContentX() + 2 + 10, this.getContentY() + 1 + 9 * 2, this.wasKilledBy ? -4539718 : -8355712
                );
            }

            @Override
            public Component getNarration() {
                return Component.translatable("narrator.select", CommonComponents.joinForNarration(this.kills, this.killedBy));
            }
        }
    }

    @OnlyIn(Dist.CLIENT)
    class StatisticsTab extends GridLayoutTab {
        protected final AbstractSelectionList<?> list;

        public StatisticsTab(Component title, AbstractSelectionList<?> list) {
            super(title);
            this.layout.addChild(list, 1, 1);
            this.list = list;
        }

        @Override
        public void doLayout(ScreenRectangle p_439073_) {
            this.list.updateSizeAndPosition(StatsScreen.this.width, StatsScreen.this.layout.getContentHeight(), StatsScreen.this.layout.getHeaderHeight());
            super.doLayout(p_439073_);
        }
    }
}
