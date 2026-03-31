package net.minecraft.client.particle;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.util.RandomSource;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class LargeSmokeParticle extends SmokeParticle {
    public LargeSmokeParticle(
        ClientLevel level, double x, double y, double z, double xSpeed, double ySpeed, double zSpeed, SpriteSet sprites
    ) {
        super(level, x, y, z, xSpeed, ySpeed, zSpeed, 2.5F, sprites);
    }

    @OnlyIn(Dist.CLIENT)
    public static class Provider implements ParticleProvider<SimpleParticleType> {
        private final SpriteSet sprites;

        public Provider(SpriteSet sprites) {
            this.sprites = sprites;
        }

        public Particle createParticle(
            SimpleParticleType p_107065_,
            ClientLevel p_107066_,
            double p_107067_,
            double p_107068_,
            double p_107069_,
            double p_107070_,
            double p_107071_,
            double p_107072_,
            RandomSource p_446222_
        ) {
            return new LargeSmokeParticle(p_107066_, p_107067_, p_107068_, p_107069_, p_107070_, p_107071_, p_107072_, this.sprites);
        }
    }
}
