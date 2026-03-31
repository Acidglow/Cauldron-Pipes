package net.minecraft.client.particle;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.util.ARGB;
import net.minecraft.util.RandomSource;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class DustPlumeParticle extends BaseAshSmokeParticle {
    private static final int COLOR_RGB24 = 12235202;

    public DustPlumeParticle(
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
        super(level, x, y, z, 0.7F, 0.6F, 0.7F, xSpeed, ySpeed + 0.15F, zSpeed, quadSizeMultiplier, sprites, 0.5F, 7, 0.5F, false);
        float f = this.random.nextFloat() * 0.2F;
        this.rCol = ARGB.red(12235202) / 255.0F - f;
        this.gCol = ARGB.green(12235202) / 255.0F - f;
        this.bCol = ARGB.blue(12235202) / 255.0F - f;
    }

    @Override
    public void tick() {
        this.gravity = 0.88F * this.gravity;
        this.friction = 0.92F * this.friction;
        super.tick();
    }

    @OnlyIn(Dist.CLIENT)
    public static class Provider implements ParticleProvider<SimpleParticleType> {
        private final SpriteSet sprites;

        public Provider(SpriteSet sprites) {
            this.sprites = sprites;
        }

        public Particle createParticle(
            SimpleParticleType p_306321_,
            ClientLevel p_306062_,
            double p_306327_,
            double p_305987_,
            double p_306266_,
            double p_306120_,
            double p_306315_,
            double p_306033_,
            RandomSource p_445437_
        ) {
            return new DustPlumeParticle(p_306062_, p_306327_, p_305987_, p_306266_, p_306120_, p_306315_, p_306033_, 1.0F, this.sprites);
        }
    }
}
