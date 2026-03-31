package net.minecraft.world.level.block.entity;

import java.util.function.Predicate;
import net.minecraft.core.NonNullList;
import net.minecraft.world.Container;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.item.ItemStack;

public interface ListBackedContainer extends Container {
    NonNullList<ItemStack> getItems();

    default int count() {
        return (int)this.getItems().stream().filter(Predicate.not(ItemStack::isEmpty)).count();
    }

    @Override
    default int getContainerSize() {
        return this.getItems().size();
    }

    @Override
    default void clearContent() {
        this.getItems().clear();
    }

    @Override
    default boolean isEmpty() {
        return this.getItems().stream().allMatch(ItemStack::isEmpty);
    }

    @Override
    default ItemStack getItem(int p_435306_) {
        return this.getItems().get(p_435306_);
    }

    @Override
    default ItemStack removeItem(int p_433717_, int p_433464_) {
        ItemStack itemstack = ContainerHelper.removeItem(this.getItems(), p_433717_, p_433464_);
        if (!itemstack.isEmpty()) {
            this.setChanged();
        }

        return itemstack;
    }

    @Override
    default ItemStack removeItemNoUpdate(int p_435020_) {
        return ContainerHelper.removeItem(this.getItems(), p_435020_, this.getMaxStackSize());
    }

    @Override
    default boolean canPlaceItem(int p_434183_, ItemStack p_435584_) {
        return this.acceptsItemType(p_435584_) && (this.getItem(p_434183_).isEmpty() || this.getItem(p_434183_).getCount() < this.getMaxStackSize(p_435584_));
    }

    default boolean acceptsItemType(ItemStack stack) {
        return true;
    }

    @Override
    default void setItem(int p_434398_, ItemStack p_434868_) {
        this.setItemNoUpdate(p_434398_, p_434868_);
        this.setChanged();
    }

    default void setItemNoUpdate(int slot, ItemStack stack) {
        this.getItems().set(slot, stack);
        stack.limitSize(this.getMaxStackSize(stack));
    }
}
