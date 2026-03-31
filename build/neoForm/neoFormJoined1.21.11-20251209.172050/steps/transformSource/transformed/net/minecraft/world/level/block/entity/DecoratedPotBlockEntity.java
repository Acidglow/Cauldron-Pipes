package net.minecraft.world.level.block.entity;

import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponentGetter;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.RandomizableContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.ticks.ContainerSingleItem;
import org.jspecify.annotations.Nullable;

public class DecoratedPotBlockEntity extends BlockEntity implements RandomizableContainer, ContainerSingleItem.BlockContainerSingleItem {
    public static final String TAG_SHERDS = "sherds";
    public static final String TAG_ITEM = "item";
    public static final int EVENT_POT_WOBBLES = 1;
    public long wobbleStartedAtTick;
    public DecoratedPotBlockEntity.@Nullable WobbleStyle lastWobbleStyle;
    private PotDecorations decorations;
    private ItemStack item = ItemStack.EMPTY;
    protected @Nullable ResourceKey<LootTable> lootTable;
    protected long lootTableSeed;

    public DecoratedPotBlockEntity(BlockPos pos, BlockState state) {
        super(BlockEntityType.DECORATED_POT, pos, state);
        this.decorations = PotDecorations.EMPTY;
    }

    @Override
    protected void saveAdditional(ValueOutput p_421506_) {
        super.saveAdditional(p_421506_);
        if (!this.decorations.equals(PotDecorations.EMPTY)) {
            p_421506_.store("sherds", PotDecorations.CODEC, this.decorations);
        }

        if (!this.trySaveLootTable(p_421506_) && !this.item.isEmpty()) {
            p_421506_.store("item", ItemStack.CODEC, this.item);
        }
    }

    @Override
    protected void loadAdditional(ValueInput p_421562_) {
        super.loadAdditional(p_421562_);
        this.decorations = p_421562_.read("sherds", PotDecorations.CODEC).orElse(PotDecorations.EMPTY);
        if (!this.tryLoadLootTable(p_421562_)) {
            this.item = p_421562_.read("item", ItemStack.CODEC).orElse(ItemStack.EMPTY);
        } else {
            this.item = ItemStack.EMPTY;
        }
    }

    public ClientboundBlockEntityDataPacket getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider p_324359_) {
        return this.saveCustomOnly(p_324359_);
    }

    public Direction getDirection() {
        return this.getBlockState().getValue(BlockStateProperties.HORIZONTAL_FACING);
    }

    public PotDecorations getDecorations() {
        return this.decorations;
    }

    public static ItemStack createDecoratedPotItem(PotDecorations decorations) {
        ItemStack itemstack = Items.DECORATED_POT.getDefaultInstance();
        itemstack.set(DataComponents.POT_DECORATIONS, decorations);
        return itemstack;
    }

    @Override
    public @Nullable ResourceKey<LootTable> getLootTable() {
        return this.lootTable;
    }

    @Override
    public void setLootTable(@Nullable ResourceKey<LootTable> p_336080_) {
        this.lootTable = p_336080_;
    }

    @Override
    public long getLootTableSeed() {
        return this.lootTableSeed;
    }

    @Override
    public void setLootTableSeed(long p_309580_) {
        this.lootTableSeed = p_309580_;
    }

    @Override
    protected void collectImplicitComponents(DataComponentMap.Builder p_338608_) {
        super.collectImplicitComponents(p_338608_);
        p_338608_.set(DataComponents.POT_DECORATIONS, this.decorations);
        p_338608_.set(DataComponents.CONTAINER, ItemContainerContents.fromItems(List.of(this.item)));
    }

    @Override
    protected void applyImplicitComponents(DataComponentGetter p_397357_) {
        super.applyImplicitComponents(p_397357_);
        this.decorations = p_397357_.getOrDefault(DataComponents.POT_DECORATIONS, PotDecorations.EMPTY);
        this.item = p_397357_.getOrDefault(DataComponents.CONTAINER, ItemContainerContents.EMPTY).copyOne();
    }

    @Override
    public void removeComponentsFromTag(ValueOutput p_421737_) {
        super.removeComponentsFromTag(p_421737_);
        p_421737_.discard("sherds");
        p_421737_.discard("item");
    }

    @Override
    public ItemStack getTheItem() {
        this.unpackLootTable(null);
        return this.item;
    }

    @Override
    public ItemStack splitTheItem(int p_305991_) {
        this.unpackLootTable(null);
        ItemStack itemstack = this.item.split(p_305991_);
        if (this.item.isEmpty()) {
            this.item = ItemStack.EMPTY;
        }

        return itemstack;
    }

    @Override
    public void setTheItem(ItemStack p_305817_) {
        this.unpackLootTable(null);
        this.item = p_305817_;
    }

    @Override
    public BlockEntity getContainerBlockEntity() {
        return this;
    }

    public void wobble(DecoratedPotBlockEntity.WobbleStyle style) {
        if (this.level != null && !this.level.isClientSide()) {
            this.level.blockEvent(this.getBlockPos(), this.getBlockState().getBlock(), 1, style.ordinal());
        }
    }

    @Override
    public boolean triggerEvent(int p_306146_, int p_305858_) {
        if (this.level != null && p_306146_ == 1 && p_305858_ >= 0 && p_305858_ < DecoratedPotBlockEntity.WobbleStyle.values().length) {
            this.wobbleStartedAtTick = this.level.getGameTime();
            this.lastWobbleStyle = DecoratedPotBlockEntity.WobbleStyle.values()[p_305858_];
            return true;
        } else {
            return super.triggerEvent(p_306146_, p_305858_);
        }
    }

    public static enum WobbleStyle {
        POSITIVE(7),
        NEGATIVE(10);

        public final int duration;

        private WobbleStyle(int duration) {
            this.duration = duration;
        }
    }
}
