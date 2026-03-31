package net.minecraft.world.item.slot;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.mojang.serialization.codecs.RecordCodecBuilder.Instance;
import java.util.Set;
import net.minecraft.util.context.ContextKey;
import net.minecraft.world.entity.SlotProvider;
import net.minecraft.world.inventory.SlotRange;
import net.minecraft.world.inventory.SlotRanges;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.LootContextArg;

public class RangeSlotSource implements SlotSource {
    public static final MapCodec<RangeSlotSource> MAP_CODEC = RecordCodecBuilder.mapCodec(
        p_461152_ -> p_461152_.group(
                LootContextArg.ENTITY_OR_BLOCK.fieldOf("source").forGetter(p_460977_ -> p_460977_.source),
                SlotRanges.CODEC.fieldOf("slots").forGetter(p_460631_ -> p_460631_.slotRange)
            )
            .apply(p_461152_, RangeSlotSource::new)
    );
    private final LootContextArg<Object> source;
    private final SlotRange slotRange;

    private RangeSlotSource(LootContextArg<Object> source, SlotRange slotRange) {
        this.source = source;
        this.slotRange = slotRange;
    }

    @Override
    public MapCodec<RangeSlotSource> codec() {
        return MAP_CODEC;
    }

    @Override
    public Set<ContextKey<?>> getReferencedContextParams() {
        return Set.of(this.source.contextParam());
    }

    @Override
    public final SlotCollection provide(LootContext p_460706_) {
        return this.source.get(p_460706_) instanceof SlotProvider slotprovider ? slotprovider.getSlotsFromRange(this.slotRange.slots()) : SlotCollection.EMPTY;
    }
}
