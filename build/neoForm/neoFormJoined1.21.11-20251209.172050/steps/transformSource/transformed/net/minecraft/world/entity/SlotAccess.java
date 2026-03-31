package net.minecraft.world.entity;

import java.util.List;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.function.Supplier;
import net.minecraft.world.item.ItemStack;

public interface SlotAccess {
    ItemStack get();

    boolean set(ItemStack item);

    static SlotAccess of(final Supplier<ItemStack> getter, final Consumer<ItemStack> setter) {
        return new SlotAccess() {
            @Override
            public ItemStack get() {
                return getter.get();
            }

            @Override
            public boolean set(ItemStack p_147314_) {
                setter.accept(p_147314_);
                return true;
            }
        };
    }

    static SlotAccess forEquipmentSlot(final LivingEntity entity, final EquipmentSlot slot, final Predicate<ItemStack> stackFilter) {
        return new SlotAccess() {
            @Override
            public ItemStack get() {
                return entity.getItemBySlot(slot);
            }

            @Override
            public boolean set(ItemStack p_147324_) {
                if (!stackFilter.test(p_147324_)) {
                    return false;
                } else {
                    entity.setItemSlot(slot, p_147324_);
                    return true;
                }
            }
        };
    }

    static SlotAccess forEquipmentSlot(LivingEntity entity, EquipmentSlot slot) {
        return forEquipmentSlot(entity, slot, p_147310_ -> true);
    }

    static SlotAccess forListElement(final List<ItemStack> items, final int index) {
        return new SlotAccess() {
            @Override
            public ItemStack get() {
                return items.get(index);
            }

            @Override
            public boolean set(ItemStack p_147334_) {
                items.set(index, p_147334_);
                return true;
            }
        };
    }
}
