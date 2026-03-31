package net.minecraft.world.item.slot;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.Function;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.storage.loot.LootContext;

public interface SlotSources {
    Codec<SlotSource> TYPED_CODEC = BuiltInRegistries.SLOT_SOURCE_TYPE.byNameCodec().dispatch(SlotSource::codec, p_460905_ -> p_460905_);
    Codec<SlotSource> CODEC = Codec.lazyInitialized(() -> Codec.withAlternative(TYPED_CODEC, GroupSlotSource.INLINE_CODEC));

    static MapCodec<? extends SlotSource> bootstrap(Registry<MapCodec<? extends SlotSource>> registry) {
        Registry.register(registry, "group", GroupSlotSource.MAP_CODEC);
        Registry.register(registry, "filtered", FilteredSlotSource.MAP_CODEC);
        Registry.register(registry, "limit_slots", LimitSlotSource.MAP_CODEC);
        Registry.register(registry, "slot_range", RangeSlotSource.MAP_CODEC);
        Registry.register(registry, "contents", ContentsSlotSource.MAP_CODEC);
        return Registry.register(registry, "empty", EmptySlotSource.MAP_CODEC);
    }

    static Function<LootContext, SlotCollection> group(Collection<? extends SlotSource> slotSources) {
        List<SlotSource> list = List.copyOf(slotSources);

        return switch (list.size()) {
            case 0 -> p_461126_ -> SlotCollection.EMPTY;
            case 1 -> list.getFirst()::provide;
            case 2 -> {
                SlotSource slotsource = list.get(0);
                SlotSource slotsource1 = list.get(1);
                yield p_460654_ -> SlotCollection.concat(slotsource.provide(p_460654_), slotsource1.provide(p_460654_));
            }
            default -> p_460761_ -> {
                List<SlotCollection> list1 = new ArrayList<>();

                for (SlotSource slotsource2 : list) {
                    list1.add(slotsource2.provide(p_460761_));
                }

                return SlotCollection.concat(list1);
            };
        };
    }
}
