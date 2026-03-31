package net.minecraft.world.attribute;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.mojang.serialization.codecs.RecordCodecBuilder.Instance;
import net.minecraft.core.Holder;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;

public record AmbientMoodSettings(Holder<SoundEvent> soundEvent, int tickDelay, int blockSearchExtent, double soundPositionOffset) {
    public static final Codec<AmbientMoodSettings> CODEC = RecordCodecBuilder.create(
        p_457834_ -> p_457834_.group(
                SoundEvent.CODEC.fieldOf("sound").forGetter(p_458238_ -> p_458238_.soundEvent),
                Codec.INT.fieldOf("tick_delay").forGetter(p_458275_ -> p_458275_.tickDelay),
                Codec.INT.fieldOf("block_search_extent").forGetter(p_458027_ -> p_458027_.blockSearchExtent),
                Codec.DOUBLE.fieldOf("offset").forGetter(p_457998_ -> p_457998_.soundPositionOffset)
            )
            .apply(p_457834_, AmbientMoodSettings::new)
    );
    public static final AmbientMoodSettings LEGACY_CAVE_SETTINGS = new AmbientMoodSettings(SoundEvents.AMBIENT_CAVE, 6000, 8, 2.0);
}
