package net.minecraft.client.particle;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.util.ARGB;
import net.minecraft.util.RandomSource;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class WhiteAshParticle extends BaseAshSmokeParticle {
    private static final int COLOR_RGB24 = 12235202;

    public WhiteAshParticle(
        ClientLevel level,
        double x,
        double y,
        double z,
        double xSpeed,
        double ySpeed,
        double zSpeed,
        float quadSizeMultiplier,
        SpriteSet sprites
    ) {
        super(level, x, y, z, 0.1F, -0.1F, 0.1F, xSpeed, ySpeed, zSpeed, quadSizeMultiplier, sprites, 0.0F, 20, 0.0125F, false);
        this.rCol = ARGB.red(12235202) / 255.0F;
        this.gCol = ARGB.green(12235202) / 255.0F;
        this.bCol = ARGB.blue(12235202) / 255.0F;
    }

    @OnlyIn(Dist.CLIENT)
    public static class Provider implements ParticleProvider<SimpleParticleType> {
        private final SpriteSet sprites;

        public Provider(SpriteSet sprites) {
            this.sprites = sprites;
        }

        public Particle createParticle(
            SimpleParticleType p_446679_,
            ClientLevel p_108526_,
            double p_108527_,
            double p_108528_,
            double p_108529_,
            double p_108530_,
            double p_108531_,
            double p_108532_,
            RandomSource p_446647_
        ) {
            double d0 = p_446647_.nextFloat() * -1.9 * p_446647_.nextFloat() * 0.1;
            double d1 = p_446647_.nextFloat() * -0.5 * p_446647_.nextFloat() * 0.1 * 5.0;
            double d2 = p_446647_.nextFloat() * -1.9 * p_446647_.nextFloat() * 0.1;
            return new WhiteAshParticle(p_108526_, p_108527_, p_108528_, p_108529_, d0, d1, d2, 1.0F, this.sprites);
        }
    }
}
