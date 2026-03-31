package net.minecraft.world.item.slot;

import java.util.Collection;
import java.util.List;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Stream;
import net.minecraft.world.entity.SlotAccess;
import net.minecraft.world.item.ItemStack;

public interface SlotCollection {
    SlotCollection EMPTY = Stream::empty;

    Stream<ItemStack> itemCopies();

    default SlotCollection filter(Predicate<ItemStack> filter) {
        return new SlotCollection.Filtered(this, filter);
    }

    default SlotCollection flatMap(Function<ItemStack, ? extends SlotCollection> mapper) {
        return new SlotCollection.FlatMapped(this, mapper);
    }

    default SlotCollection limit(int limit) {
        return new SlotCollection.Limited(this, limit);
    }

    static SlotCollection of(SlotAccess slot) {
        return () -> Stream.of(slot.get().copy());
    }

    static SlotCollection of(Collection<? extends SlotAccess> slot) {
        return switch (slot.size()) {
            case 0 -> EMPTY;
            case 1 -> of(slot.iterator().next());
            default -> () -> slot.stream().map(SlotAccess::get).map(ItemStack::copy);
        };
    }

    static SlotCollection concat(SlotCollection first, SlotCollection second) {
        return () -> Stream.concat(first.itemCopies(), second.itemCopies());
    }

    static SlotCollection concat(List<? extends SlotCollection> collections) {
        return switch (collections.size()) {
            case 0 -> EMPTY;
            case 1 -> (SlotCollection)collections.getFirst();
            case 2 -> concat(collections.get(0), collections.get(1));
            default -> () -> collections.stream().flatMap(SlotCollection::itemCopies);
        };
    }

    public record Filtered(SlotCollection slots, Predicate<ItemStack> filter) implements SlotCollection {
        @Override
        public Stream<ItemStack> itemCopies() {
            return this.slots.itemCopies().filter(this.filter);
        }

        @Override
        public SlotCollection filter(Predicate<ItemStack> p_461182_) {
            return new SlotCollection.Filtered(this.slots, this.filter.and(p_461182_));
        }
    }

    public record FlatMapped(SlotCollection slots, Function<ItemStack, ? extends SlotCollection> mapper) implements SlotCollection {
        @Override
        public Stream<ItemStack> itemCopies() {
            return this.slots.itemCopies().map(this.mapper).flatMap(SlotCollection::itemCopies);
        }
    }

    public record Limited(SlotCollection slots, int limit) implements SlotCollection {
        @Override
        public Stream<ItemStack> itemCopies() {
            return this.slots.itemCopies().limit(this.limit);
        }

        @Override
        public SlotCollection limit(int p_461097_) {
            return new SlotCollection.Limited(this.slots, Math.min(this.limit, p_461097_));
        }
    }
}
