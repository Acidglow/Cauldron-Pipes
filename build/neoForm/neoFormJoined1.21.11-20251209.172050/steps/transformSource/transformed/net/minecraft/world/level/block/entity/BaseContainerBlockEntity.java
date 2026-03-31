package net.minecraft.world.level.block.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.core.component.DataComponentGetter;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Container;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.LockCode;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.Nameable;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

public abstract class BaseContainerBlockEntity extends BlockEntity implements Container, MenuProvider, Nameable {
    private LockCode lockKey = LockCode.NO_LOCK;
    private @Nullable Component name;

    protected BaseContainerBlockEntity(BlockEntityType<?> p_155076_, BlockPos p_155077_, BlockState p_155078_) {
        super(p_155076_, p_155077_, p_155078_);
    }

    @Override
    protected void loadAdditional(ValueInput p_422403_) {
        super.loadAdditional(p_422403_);
        this.lockKey = LockCode.fromTag(p_422403_);
        this.name = parseCustomNameSafe(p_422403_, "CustomName");
    }

    @Override
    protected void saveAdditional(ValueOutput p_422177_) {
        super.saveAdditional(p_422177_);
        this.lockKey.addToTag(p_422177_);
        p_422177_.storeNullable("CustomName", ComponentSerialization.CODEC, this.name);
    }

    @Override
    public Component getName() {
        return this.name != null ? this.name : this.getDefaultName();
    }

    @Override
    public Component getDisplayName() {
        return this.getName();
    }

    @Override
    public @Nullable Component getCustomName() {
        return this.name;
    }

    protected abstract Component getDefaultName();

    public boolean canOpen(Player player) {
        return this.lockKey.canUnlock(player);
    }

    public static void sendChestLockedNotifications(Vec3 pos, Player player, Component displayName) {
        Level level = player.level();
        player.displayClientMessage(Component.translatable("container.isLocked", displayName), true);
        if (!level.isClientSide()) {
            level.playSound(null, pos.x(), pos.y(), pos.z(), SoundEvents.CHEST_LOCKED, SoundSource.BLOCKS, 1.0F, 1.0F);
        }
    }

    public boolean isLocked() {
        return !this.lockKey.equals(LockCode.NO_LOCK);
    }

    protected abstract NonNullList<ItemStack> getItems();

    protected abstract void setItems(NonNullList<ItemStack> items);

    @Override
    public boolean isEmpty() {
        for (ItemStack itemstack : this.getItems()) {
            if (!itemstack.isEmpty()) {
                return false;
            }
        }

        return true;
    }

    @Override
    public ItemStack getItem(int p_332727_) {
        return this.getItems().get(p_332727_);
    }

    @Override
    public ItemStack removeItem(int p_332707_, int p_332672_) {
        ItemStack itemstack = ContainerHelper.removeItem(this.getItems(), p_332707_, p_332672_);
        if (!itemstack.isEmpty()) {
            this.setChanged();
        }

        return itemstack;
    }

    @Override
    public ItemStack removeItemNoUpdate(int p_332812_) {
        return ContainerHelper.takeItem(this.getItems(), p_332812_);
    }

    @Override
    public void setItem(int p_332705_, ItemStack p_332643_) {
        setItem(p_332705_, p_332643_, false);
    }

    // Neo: Skip side-effects if insideTransaction is true so the caller can defer them until the transaction commits
    @Override
    public void setItem(int p_332705_, ItemStack p_332643_, boolean insideTransaction) {
        this.getItems().set(p_332705_, p_332643_);
        p_332643_.limitSize(this.getMaxStackSize(p_332643_));
        if (!insideTransaction) {
            this.setChanged();
        }
    }

    @Override
    public boolean stillValid(Player p_332791_) {
        return Container.stillValidBlockEntity(this, p_332791_);
    }

    @Override
    public void clearContent() {
        this.getItems().clear();
    }

    @Override
    public @Nullable AbstractContainerMenu createMenu(int containerId, Inventory inventory, Player p_58643_) {
        if (this.canOpen(p_58643_)) {
            return this.createMenu(containerId, inventory);
        } else {
            sendChestLockedNotifications(this.getBlockPos().getCenter(), p_58643_, this.getDisplayName());
            return null;
        }
    }

    protected abstract AbstractContainerMenu createMenu(int containerId, Inventory inventory);

    @Override
    protected void applyImplicitComponents(DataComponentGetter p_397486_) {
        super.applyImplicitComponents(p_397486_);
        this.name = p_397486_.get(DataComponents.CUSTOM_NAME);
        this.lockKey = p_397486_.getOrDefault(DataComponents.LOCK, LockCode.NO_LOCK);
        p_397486_.getOrDefault(DataComponents.CONTAINER, ItemContainerContents.EMPTY).copyInto(this.getItems());
    }

    @Override
    protected void collectImplicitComponents(DataComponentMap.Builder p_338252_) {
        super.collectImplicitComponents(p_338252_);
        p_338252_.set(DataComponents.CUSTOM_NAME, this.name);
        if (this.isLocked()) {
            p_338252_.set(DataComponents.LOCK, this.lockKey);
        }

        p_338252_.set(DataComponents.CONTAINER, ItemContainerContents.fromItems(this.getItems()));
    }

    @Override
    public void removeComponentsFromTag(ValueOutput p_421741_) {
        p_421741_.discard("CustomName");
        p_421741_.discard("lock");
        p_421741_.discard("Items");
    }
}
