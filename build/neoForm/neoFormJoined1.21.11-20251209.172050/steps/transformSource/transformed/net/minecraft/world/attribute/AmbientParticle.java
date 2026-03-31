package net.minecraft.world.attribute;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.mojang.serialization.codecs.RecordCodecBuilder.Instance;
import java.util.List;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.RandomSource;

public record AmbientParticle(ParticleOptions particle, float probability) {
    public static final Codec<AmbientParticle> CODEC = RecordCodecBuilder.create(
        p_458012_ -> p_458012_.group(
                ParticleTypes.CODEC.fieldOf("particle").forGetter(p_458260_ -> p_458260_.particle),
                Codec.floatRange(0.0F, 1.0F).fieldOf("probability").forGetter(p_457956_ -> p_457956_.probability)
            )
            .apply(p_458012_, AmbientParticle::new)
    );

    public boolean canSpawn(RandomSource random) {
        return random.nextFloat() <= this.probability;
    }

    public static List<AmbientParticle> of(ParticleOptions particle, float probability) {
        return List.of(new AmbientParticle(particle, probability));
    }
}
