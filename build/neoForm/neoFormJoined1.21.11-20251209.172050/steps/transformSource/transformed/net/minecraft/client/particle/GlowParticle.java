package net.minecraft.client.particle;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class GlowParticle extends SingleQuadParticle {
    private final SpriteSet sprites;

    public GlowParticle(
        ClientLevel level, double x, double y, double z, double xSpeed, double ySpeed, double zSpeed, SpriteSet sprites
    ) {
        super(level, x, y, z, xSpeed, ySpeed, zSpeed, sprites.first());
        this.friction = 0.96F;
        this.speedUpWhenYMotionIsBlocked = true;
        this.sprites = sprites;
        this.quadSize *= 0.75F;
        this.hasPhysics = false;
        this.setSpriteFromAge(sprites);
    }

    @Override
    public SingleQuadParticle.Layer getLayer() {
        return SingleQuadParticle.Layer.TRANSLUCENT;
    }

    @Override
    public int getLightColor(float p_172146_) {
        float f = (this.age + p_172146_) / this.lifetime;
        f = Mth.clamp(f, 0.0F, 1.0F);
        int i = super.getLightColor(p_172146_);
        int j = i & 0xFF;
        int k = i >> 16 & 0xFF;
        j += (int)(f * 15.0F * 16.0F);
        if (j > 240) {
            j = 240;
        }

        return j | k << 16;
    }

    @Override
    public void tick() {
        super.tick();
        this.setSpriteFromAge(this.sprites);
    }

    @OnlyIn(Dist.CLIENT)
    public static class ElectricSparkProvider implements ParticleProvider<SimpleParticleType> {
        private static final double SPEED_FACTOR = 0.25;
        private final SpriteSet sprite;

        public ElectricSparkProvider(SpriteSet sprites) {
            this.sprite = sprites;
        }

        public Particle createParticle(
            SimpleParticleType p_172162_,
            ClientLevel p_172163_,
            double p_172164_,
            double p_172165_,
            double p_172166_,
            double p_172167_,
            double p_172168_,
            double p_172169_,
            RandomSource p_446022_
        ) {
            GlowParticle glowparticle = new GlowParticle(p_172163_, p_172164_, p_172165_, p_172166_, 0.0, 0.0, 0.0, this.sprite);
            glowparticle.setColor(1.0F, 0.9F, 1.0F);
            glowparticle.setParticleSpeed(p_172167_ * 0.25, p_172168_ * 0.25, p_172169_ * 0.25);
            int i = 2;
            int j = 4;
            glowparticle.setLifetime(p_446022_.nextInt(2) + 2);
            return glowparticle;
        }
    }

    @OnlyIn(Dist.CLIENT)
    public static class GlowSquidProvider implements ParticleProvider<SimpleParticleType> {
        private final SpriteSet sprite;

        public GlowSquidProvider(SpriteSet sprites) {
            this.sprite = sprites;
        }

        public Particle createParticle(
            SimpleParticleType p_445833_,
            ClientLevel p_172175_,
            double p_172176_,
            double p_172177_,
            double p_172178_,
            double p_172179_,
            double p_172180_,
            double p_172181_,
            RandomSource p_445649_
        ) {
            GlowParticle glowparticle = new GlowParticle(
                p_172175_, p_172176_, p_172177_, p_172178_, 0.5 - p_445649_.nextDouble(), p_172180_, 0.5 - p_445649_.nextDouble(), this.sprite
            );
            if (p_445649_.nextBoolean()) {
                glowparticle.setColor(0.6F, 1.0F, 0.8F);
            } else {
                glowparticle.setColor(0.08F, 0.4F, 0.4F);
            }

            glowparticle.yd *= 0.2F;
            if (p_172179_ == 0.0 && p_172181_ == 0.0) {
                glowparticle.xd *= 0.1F;
                glowparticle.zd *= 0.1F;
            }

            glowparticle.setLifetime((int)(8.0 / (p_445649_.nextDouble() * 0.8 + 0.2)));
            return glowparticle;
        }
    }

    @OnlyIn(Dist.CLIENT)
    public static class ScrapeProvider implements ParticleProvider<SimpleParticleType> {
        private static final double SPEED_FACTOR = 0.01;
        private final SpriteSet sprite;

        public ScrapeProvider(SpriteSet sprites) {
            this.sprite = sprites;
        }

        public Particle createParticle(
            SimpleParticleType p_172205_,
            ClientLevel p_172206_,
            double p_172207_,
            double p_172208_,
            double p_172209_,
            double p_172210_,
            double p_172211_,
            double p_172212_,
            RandomSource p_447003_
        ) {
            GlowParticle glowparticle = new GlowParticle(p_172206_, p_172207_, p_172208_, p_172209_, 0.0, 0.0, 0.0, this.sprite);
            if (p_447003_.nextBoolean()) {
                glowparticle.setColor(0.29F, 0.58F, 0.51F);
            } else {
                glowparticle.setColor(0.43F, 0.77F, 0.62F);
            }

            glowparticle.setParticleSpeed(p_172210_ * 0.01, p_172211_ * 0.01, p_172212_ * 0.01);
            int i = 10;
            int j = 40;
            glowparticle.setLifetime(p_447003_.nextInt(30) + 10);
            return glowparticle;
        }
    }

    @OnlyIn(Dist.CLIENT)
    public static class WaxOffProvider implements ParticleProvider<SimpleParticleType> {
        private static final double SPEED_FACTOR = 0.01;
        private final SpriteSet sprite;

        public WaxOffProvider(SpriteSet sprites) {
            this.sprite = sprites;
        }

        public Particle createParticle(
            SimpleParticleType p_445867_,
            ClientLevel p_172219_,
            double p_172220_,
            double p_172221_,
            double p_172222_,
            double p_172223_,
            double p_172224_,
            double p_172225_,
            RandomSource p_446464_
        ) {
            GlowParticle glowparticle = new GlowParticle(p_172219_, p_172220_, p_172221_, p_172222_, 0.0, 0.0, 0.0, this.sprite);
            glowparticle.setColor(1.0F, 0.9F, 1.0F);
            glowparticle.setParticleSpeed(p_172223_ * 0.01 / 2.0, p_172224_ * 0.01, p_172225_ * 0.01 / 2.0);
            int i = 10;
            int j = 40;
            glowparticle.setLifetime(p_446464_.nextInt(30) + 10);
            return glowparticle;
        }
    }

    @OnlyIn(Dist.CLIENT)
    public static class WaxOnProvider implements ParticleProvider<SimpleParticleType> {
        private static final double SPEED_FACTOR = 0.01;
        private final SpriteSet sprite;

        public WaxOnProvider(SpriteSet sprites) {
            this.sprite = sprites;
        }

        public Particle createParticle(
            SimpleParticleType p_172249_,
            ClientLevel p_172250_,
            double p_172251_,
            double p_172252_,
            double p_172253_,
            double p_172254_,
            double p_172255_,
            double p_172256_,
            RandomSource p_446627_
        ) {
            GlowParticle glowparticle = new GlowParticle(p_172250_, p_172251_, p_172252_, p_172253_, 0.0, 0.0, 0.0, this.sprite);
            glowparticle.setColor(0.91F, 0.55F, 0.08F);
            glowparticle.setParticleSpeed(p_172254_ * 0.01 / 2.0, p_172255_ * 0.01, p_172256_ * 0.01 / 2.0);
            int i = 10;
            int j = 40;
            glowparticle.setLifetime(p_446627_.nextInt(30) + 10);
            return glowparticle;
        }
    }
}
