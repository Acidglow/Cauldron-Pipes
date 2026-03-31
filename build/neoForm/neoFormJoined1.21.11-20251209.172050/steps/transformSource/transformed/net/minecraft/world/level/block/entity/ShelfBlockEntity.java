package net.minecraft.world.level.block.entity;

import com.mojang.logging.LogUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.core.component.DataComponentGetter;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.Container;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.entity.ItemOwner;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.ShelfBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.storage.TagValueOutput;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

public class ShelfBlockEntity extends BlockEntity implements ItemOwner, ListBackedContainer {
    public static final int MAX_ITEMS = 3;
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final String ALIGN_ITEMS_TO_BOTTOM_TAG = "align_items_to_bottom";
    private final NonNullList<ItemStack> items = NonNullList.withSize(3, ItemStack.EMPTY);
    private boolean alignItemsToBottom;

    public ShelfBlockEntity(BlockPos pos, BlockState state) {
        super(BlockEntityType.SHELF, pos, state);
    }

    @Override
    protected void loadAdditional(ValueInput p_433152_) {
        super.loadAdditional(p_433152_);
        this.items.clear();
        ContainerHelper.loadAllItems(p_433152_, this.items);
        this.alignItemsToBottom = p_433152_.getBooleanOr("align_items_to_bottom", false);
    }

    @Override
    protected void saveAdditional(ValueOutput p_435995_) {
        super.saveAdditional(p_435995_);
        ContainerHelper.saveAllItems(p_435995_, this.items, true);
        p_435995_.putBoolean("align_items_to_bottom", this.alignItemsToBottom);
    }

    public ClientboundBlockEntityDataPacket getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider p_433122_) {
        CompoundTag compoundtag;
        try (ProblemReporter.ScopedCollector problemreporter$scopedcollector = new ProblemReporter.ScopedCollector(this.problemPath(), LOGGER)) {
            TagValueOutput tagvalueoutput = TagValueOutput.createWithContext(problemreporter$scopedcollector, p_433122_);
            ContainerHelper.saveAllItems(tagvalueoutput, this.items, true);
            tagvalueoutput.putBoolean("align_items_to_bottom", this.alignItemsToBottom);
            compoundtag = tagvalueoutput.buildResult();
        }

        return compoundtag;
    }

    @Override
    public NonNullList<ItemStack> getItems() {
        return this.items;
    }

    @Override
    public boolean stillValid(Player p_433189_) {
        return Container.stillValidBlockEntity(this, p_433189_);
    }

    public ItemStack swapItemNoUpdate(int slot, ItemStack stack) {
        ItemStack itemstack = this.removeItemNoUpdate(slot);
        this.setItemNoUpdate(slot, stack);
        return itemstack;
    }

    public void setChanged(Holder.@Nullable Reference<GameEvent> gameEvent) {
        super.setChanged();
        if (this.level != null) {
            if (gameEvent != null) {
                this.level.gameEvent(gameEvent, this.worldPosition, GameEvent.Context.of(this.getBlockState()));
            }

            this.getLevel().sendBlockUpdated(this.getBlockPos(), this.getBlockState(), this.getBlockState(), 3);
        }
    }

    @Override
    public void setChanged() {
        this.setChanged(GameEvent.BLOCK_ACTIVATE);
    }

    @Override
    protected void applyImplicitComponents(DataComponentGetter p_435051_) {
        super.applyImplicitComponents(p_435051_);
        p_435051_.getOrDefault(DataComponents.CONTAINER, ItemContainerContents.EMPTY).copyInto(this.items);
    }

    @Override
    protected void collectImplicitComponents(DataComponentMap.Builder p_435586_) {
        super.collectImplicitComponents(p_435586_);
        p_435586_.set(DataComponents.CONTAINER, ItemContainerContents.fromItems(this.items));
    }

    @Override
    public void removeComponentsFromTag(ValueOutput p_433784_) {
        p_433784_.discard("Items");
    }

    @Override
    public Level level() {
        return this.level;
    }

    @Override
    public Vec3 position() {
        return this.getBlockPos().getCenter();
    }

    @Override
    public float getVisualRotationYInDegrees() {
        return this.getBlockState().getValue(ShelfBlock.FACING).getOpposite().toYRot();
    }

    public boolean getAlignItemsToBottom() {
        return this.alignItemsToBottom;
    }
}
