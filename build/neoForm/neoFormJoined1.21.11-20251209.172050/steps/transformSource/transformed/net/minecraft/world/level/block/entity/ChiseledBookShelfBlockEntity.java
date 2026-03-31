package net.minecraft.world.level.block.entity;

import com.mojang.logging.LogUtils;
import java.util.Objects;
import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.core.component.DataComponentGetter;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.component.DataComponents;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.Container;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.level.block.ChiseledBookShelfBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.slf4j.Logger;

public class ChiseledBookShelfBlockEntity extends BlockEntity implements ListBackedContainer {
    public static final int MAX_BOOKS_IN_STORAGE = 6;
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final int DEFAULT_LAST_INTERACTED_SLOT = -1;
    private final NonNullList<ItemStack> items = NonNullList.withSize(6, ItemStack.EMPTY);
    private int lastInteractedSlot = -1;

    public ChiseledBookShelfBlockEntity(BlockPos pos, BlockState state) {
        super(BlockEntityType.CHISELED_BOOKSHELF, pos, state);
    }

    private void updateState(int slot) {
        if (slot >= 0 && slot < 6) {
            this.lastInteractedSlot = slot;
            BlockState blockstate = this.getBlockState();

            for (int i = 0; i < ChiseledBookShelfBlock.SLOT_OCCUPIED_PROPERTIES.size(); i++) {
                boolean flag = !this.getItem(i).isEmpty();
                BooleanProperty booleanproperty = ChiseledBookShelfBlock.SLOT_OCCUPIED_PROPERTIES.get(i);
                blockstate = blockstate.setValue(booleanproperty, flag);
            }

            Objects.requireNonNull(this.level).setBlock(this.worldPosition, blockstate, 3);
            this.level.gameEvent(GameEvent.BLOCK_CHANGE, this.worldPosition, GameEvent.Context.of(blockstate));
        } else {
            LOGGER.error("Expected slot 0-5, got {}", slot);
        }
    }

    @Override
    protected void loadAdditional(ValueInput p_422122_) {
        super.loadAdditional(p_422122_);
        this.items.clear();
        ContainerHelper.loadAllItems(p_422122_, this.items);
        this.lastInteractedSlot = p_422122_.getIntOr("last_interacted_slot", -1);
    }

    @Override
    protected void saveAdditional(ValueOutput p_422265_) {
        super.saveAdditional(p_422265_);
        ContainerHelper.saveAllItems(p_422265_, this.items, true);
        p_422265_.putInt("last_interacted_slot", this.lastInteractedSlot);
    }

    @Override
    public int getMaxStackSize() {
        return 1;
    }

    @Override
    public boolean acceptsItemType(ItemStack p_433750_) {
        return p_433750_.is(ItemTags.BOOKSHELF_BOOKS);
    }

    @Override
    public ItemStack removeItem(int p_255828_, int p_255673_) {
        ItemStack itemstack = Objects.requireNonNullElse(this.getItems().get(p_255828_), ItemStack.EMPTY);
        this.getItems().set(p_255828_, ItemStack.EMPTY);
        if (!itemstack.isEmpty()) {
            this.updateState(p_255828_);
        }

        return itemstack;
    }

    @Override
    public void setItem(int p_256610_, ItemStack p_255789_) {
        setItem(p_256610_, p_255789_, false);
    }

    // Neo: Skip side-effects if insideTransaction is true so the caller can defer them until the transaction commits
    @Override
    public void setItem(int p_256610_, ItemStack p_255789_, boolean insideTransaction) {
        if (this.acceptsItemType(p_255789_)) {
            this.getItems().set(p_256610_, p_255789_);
            if (!insideTransaction) {
                this.updateState(p_256610_);
            }
        } else if (p_255789_.isEmpty()) {
            if (insideTransaction) {
                // Skip the updateState call in removeItem
                this.getItems().set(p_256610_, p_255789_);
                return;
            }
            this.removeItem(p_256610_, this.getMaxStackSize());
        }
    }

    // Neo: Make lastInteractedSlot transactional, and defer updateState until end of a transaction
    private final net.neoforged.neoforge.transfer.transaction.SnapshotJournal<Integer> lastInteractedSlotJournal = new net.neoforged.neoforge.transfer.transaction.SnapshotJournal<>() {
        @Override
        protected Integer createSnapshot() {
            return lastInteractedSlot;
        }
        @Override
        protected void revertToSnapshot(Integer snapshot) {
            lastInteractedSlot = snapshot;
        }
        @Override
        protected void onRootCommit(Integer originalState) {
            // If the block entity was removed, skip updateState to avoid the setBlock call that would overwrite the current block
            if (!isRemoved()) {
                updateState(lastInteractedSlot);
            }
        }
    };

    @Override
    public void onTransfer(int slot, int amountChange, net.neoforged.neoforge.transfer.transaction.TransactionContext transaction) {
        this.lastInteractedSlotJournal.updateSnapshots(transaction);
        lastInteractedSlot = slot;
    }

    @Override
    public boolean canTakeItem(Container p_437377_, int p_437398_, ItemStack p_437308_) {
        return p_437377_.hasAnyMatching(
            p_437333_ -> p_437333_.isEmpty()
                ? true
                : ItemStack.isSameItemSameComponents(p_437308_, p_437333_)
                    && p_437333_.getCount() + p_437308_.getCount() <= p_437377_.getMaxStackSize(p_437333_)
        );
    }

    @Override
    public NonNullList<ItemStack> getItems() {
        return this.items;
    }

    @Override
    public boolean stillValid(Player p_256481_) {
        return Container.stillValidBlockEntity(this, p_256481_);
    }

    public int getLastInteractedSlot() {
        return this.lastInteractedSlot;
    }

    @Override
    protected void applyImplicitComponents(DataComponentGetter p_396989_) {
        super.applyImplicitComponents(p_396989_);
        p_396989_.getOrDefault(DataComponents.CONTAINER, ItemContainerContents.EMPTY).copyInto(this.items);
    }

    @Override
    protected void collectImplicitComponents(DataComponentMap.Builder p_338540_) {
        super.collectImplicitComponents(p_338540_);
        p_338540_.set(DataComponents.CONTAINER, ItemContainerContents.fromItems(this.items));
    }

    @Override
    public void removeComponentsFromTag(ValueOutput p_421863_) {
        p_421863_.discard("Items");
    }
}
