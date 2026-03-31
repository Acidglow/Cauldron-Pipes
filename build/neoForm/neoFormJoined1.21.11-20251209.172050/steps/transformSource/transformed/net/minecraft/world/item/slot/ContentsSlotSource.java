package net.minecraft.world.item.slot;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.mojang.serialization.codecs.RecordCodecBuilder.Instance;
import net.minecraft.world.level.storage.loot.ContainerComponentManipulator;
import net.minecraft.world.level.storage.loot.ContainerComponentManipulators;

public class ContentsSlotSource extends TransformedSlotSource {
    public static final MapCodec<ContentsSlotSource> MAP_CODEC = RecordCodecBuilder.mapCodec(
        p_460909_ -> commonFields(p_460909_)
            .and(ContainerComponentManipulators.CODEC.fieldOf("component").forGetter(p_461135_ -> p_461135_.component))
            .apply(p_460909_, ContentsSlotSource::new)
    );
    private final ContainerComponentManipulator<?> component;

    private ContentsSlotSource(SlotSource slotSource, ContainerComponentManipulator<?> component) {
        super(slotSource);
        this.component = component;
    }

    @Override
    public MapCodec<ContentsSlotSource> codec() {
        return MAP_CODEC;
    }

    @Override
    protected SlotCollection transform(SlotCollection p_460811_) {
        return p_460811_.flatMap(this.component::getSlots);
    }
}
