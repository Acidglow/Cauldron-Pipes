package net.minecraft.world.attribute;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.mojang.serialization.codecs.RecordCodecBuilder.Instance;
import net.minecraft.core.Holder;
import net.minecraft.sounds.SoundEvent;

public record AmbientAdditionsSettings(Holder<SoundEvent> soundEvent, double tickChance) {
    public static final Codec<AmbientAdditionsSettings> CODEC = RecordCodecBuilder.create(
        p_458062_ -> p_458062_.group(
                SoundEvent.CODEC.fieldOf("sound").forGetter(p_458206_ -> p_458206_.soundEvent),
                Codec.DOUBLE.fieldOf("tick_chance").forGetter(p_457605_ -> p_457605_.tickChance)
            )
            .apply(p_458062_, AmbientAdditionsSettings::new)
    );
}
