package net.minecraft.world.item.slot;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.mojang.serialization.codecs.RecordCodecBuilder.Instance;
import net.minecraft.advancements.criterion.ItemPredicate;

public class FilteredSlotSource extends TransformedSlotSource {
    public static final MapCodec<FilteredSlotSource> MAP_CODEC = RecordCodecBuilder.mapCodec(
        p_466676_ -> commonFields(p_466676_)
            .and(ItemPredicate.CODEC.fieldOf("item_filter").forGetter(p_466675_ -> p_466675_.filter))
            .apply(p_466676_, FilteredSlotSource::new)
    );
    private final ItemPredicate filter;

    private FilteredSlotSource(SlotSource slotSource, ItemPredicate filter) {
        super(slotSource);
        this.filter = filter;
    }

    @Override
    public MapCodec<FilteredSlotSource> codec() {
        return MAP_CODEC;
    }

    @Override
    protected SlotCollection transform(SlotCollection p_460749_) {
        return p_460749_.filter(this.filter);
    }
}
