package net.minecraft.world.level.block;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.mojang.serialization.codecs.RecordCodecBuilder.Instance;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.util.ParticleUtils;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockBehaviour;

public class UntintedParticleLeavesBlock extends LeavesBlock {
    public static final MapCodec<UntintedParticleLeavesBlock> CODEC = RecordCodecBuilder.mapCodec(
        p_432689_ -> p_432689_.group(
                ExtraCodecs.floatRange(0.0F, 1.0F).fieldOf("leaf_particle_chance").forGetter(p_399708_ -> p_399708_.leafParticleChance),
                ParticleTypes.CODEC.fieldOf("leaf_particle").forGetter(p_399817_ -> p_399817_.leafParticle),
                propertiesCodec()
            )
            .apply(p_432689_, UntintedParticleLeavesBlock::new)
    );
    protected final ParticleOptions leafParticle;

    public UntintedParticleLeavesBlock(float leafParticleChance, ParticleOptions leafParticle, BlockBehaviour.Properties properties) {
        super(leafParticleChance, properties);
        this.leafParticle = leafParticle;
    }

    @Override
    protected void spawnFallingLeavesParticle(Level p_399654_, BlockPos p_399778_, RandomSource p_400146_) {
        ParticleUtils.spawnParticleBelow(p_399654_, p_399778_, p_400146_, this.leafParticle);
    }

    @Override
    public MapCodec<UntintedParticleLeavesBlock> codec() {
        return CODEC;
    }
}
