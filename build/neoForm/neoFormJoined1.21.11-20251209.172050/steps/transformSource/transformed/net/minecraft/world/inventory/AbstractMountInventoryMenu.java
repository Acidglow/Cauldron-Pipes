package net.minecraft.world.inventory;

import net.minecraft.world.Container;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

public abstract class AbstractMountInventoryMenu extends AbstractContainerMenu {
    protected final Container mountContainer;
    protected final LivingEntity mount;
    protected final int SLOT_SADDLE = 0;
    protected final int SLOT_BODY_ARMOR = 1;
    protected final int SLOT_INVENTORY_START = 2;
    protected static final int INVENTORY_ROWS = 3;

    protected AbstractMountInventoryMenu(int containerId, Inventory playerInventory, Container mountContainer, LivingEntity mount) {
        super(null, containerId);
        this.mountContainer = mountContainer;
        this.mount = mount;
        mountContainer.startOpen(playerInventory.player);
    }

    protected abstract boolean hasInventoryChanged(Container inventory);

    @Override
    public boolean stillValid(Player p_470629_) {
        return !this.hasInventoryChanged(this.mountContainer)
            && this.mountContainer.stillValid(p_470629_)
            && this.mount.isAlive()
            && p_470629_.isWithinEntityInteractionRange(this.mount, 4.0);
    }

    @Override
    public void removed(Player p_470680_) {
        super.removed(p_470680_);
        this.mountContainer.stopOpen(p_470680_);
    }

    @Override
    public ItemStack quickMoveStack(Player p_470679_, int p_470684_) {
        ItemStack itemstack = ItemStack.EMPTY;
        Slot slot = this.slots.get(p_470684_);
        if (slot != null && slot.hasItem()) {
            ItemStack itemstack1 = slot.getItem();
            itemstack = itemstack1.copy();
            int i = 2 + this.mountContainer.getContainerSize();
            if (p_470684_ < i) {
                if (!this.moveItemStackTo(itemstack1, i, this.slots.size(), true)) {
                    return ItemStack.EMPTY;
                }
            } else if (this.getSlot(1).mayPlace(itemstack1) && !this.getSlot(1).hasItem()) {
                if (!this.moveItemStackTo(itemstack1, 1, 2, false)) {
                    return ItemStack.EMPTY;
                }
            } else if (this.getSlot(0).mayPlace(itemstack1) && !this.getSlot(0).hasItem()) {
                if (!this.moveItemStackTo(itemstack1, 0, 1, false)) {
                    return ItemStack.EMPTY;
                }
            } else if (this.mountContainer.getContainerSize() == 0 || !this.moveItemStackTo(itemstack1, 2, i, false)) {
                int j = i + 27;
                int k = j + 9;
                if (p_470684_ >= j && p_470684_ < k) {
                    if (!this.moveItemStackTo(itemstack1, i, j, false)) {
                        return ItemStack.EMPTY;
                    }
                } else if (p_470684_ >= i && p_470684_ < j) {
                    if (!this.moveItemStackTo(itemstack1, j, k, false)) {
                        return ItemStack.EMPTY;
                    }
                } else if (!this.moveItemStackTo(itemstack1, j, j, false)) {
                    return ItemStack.EMPTY;
                }

                return ItemStack.EMPTY;
            }

            if (itemstack1.isEmpty()) {
                slot.setByPlayer(ItemStack.EMPTY);
            } else {
                slot.setChanged();
            }
        }

        return itemstack;
    }

    public static int getInventorySize(int rows) {
        return rows * 3;
    }
}
