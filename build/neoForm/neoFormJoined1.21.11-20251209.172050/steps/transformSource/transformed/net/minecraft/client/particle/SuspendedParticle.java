package net.minecraft.client.particle;

import java.util.Optional;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.particles.ParticleLimit;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class SuspendedParticle extends SingleQuadParticle {
    public SuspendedParticle(ClientLevel p_172403_, double p_172405_, double p_172406_, double p_172407_, TextureAtlasSprite p_447166_) {
        super(p_172403_, p_172405_, p_172406_ - 0.125, p_172407_, p_447166_);
        this.setSize(0.01F, 0.01F);
        this.quadSize = this.quadSize * (this.random.nextFloat() * 0.6F + 0.2F);
        this.lifetime = (int)(16.0 / (this.random.nextFloat() * 0.8 + 0.2));
        this.hasPhysics = false;
        this.friction = 1.0F;
        this.gravity = 0.0F;
    }

    public SuspendedParticle(
        ClientLevel p_172409_,
        double p_172411_,
        double p_172412_,
        double p_172413_,
        double p_172414_,
        double p_172415_,
        double p_172416_,
        TextureAtlasSprite p_446184_
    ) {
        super(p_172409_, p_172411_, p_172412_ - 0.125, p_172413_, p_172414_, p_172415_, p_172416_, p_446184_);
        this.setSize(0.01F, 0.01F);
        this.quadSize = this.quadSize * (this.random.nextFloat() * 0.6F + 0.6F);
        this.lifetime = (int)(16.0 / (this.random.nextFloat() * 0.8 + 0.2));
        this.hasPhysics = false;
        this.friction = 1.0F;
        this.gravity = 0.0F;
    }

    @Override
    public SingleQuadParticle.Layer getLayer() {
        return SingleQuadParticle.Layer.OPAQUE;
    }

    @OnlyIn(Dist.CLIENT)
    public static class CrimsonSporeProvider implements ParticleProvider<SimpleParticleType> {
        private final SpriteSet sprite;

        public CrimsonSporeProvider(SpriteSet sprites) {
            this.sprite = sprites;
        }

        public Particle createParticle(
            SimpleParticleType p_445644_,
            ClientLevel p_108045_,
            double p_108046_,
            double p_108047_,
            double p_108048_,
            double p_108049_,
            double p_108050_,
            double p_108051_,
            RandomSource p_447344_
        ) {
            double d0 = p_447344_.nextGaussian() * 1.0E-6F;
            double d1 = p_447344_.nextGaussian() * 1.0E-4F;
            double d2 = p_447344_.nextGaussian() * 1.0E-6F;
            SuspendedParticle suspendedparticle = new SuspendedParticle(p_108045_, p_108046_, p_108047_, p_108048_, d0, d1, d2, this.sprite.get(p_447344_));
            suspendedparticle.setColor(0.9F, 0.4F, 0.5F);
            return suspendedparticle;
        }
    }

    @OnlyIn(Dist.CLIENT)
    public static class SporeBlossomAirProvider implements ParticleProvider<SimpleParticleType> {
        private final SpriteSet sprite;

        public SporeBlossomAirProvider(SpriteSet sprites) {
            this.sprite = sprites;
        }

        public Particle createParticle(
            SimpleParticleType p_445768_,
            ClientLevel p_172422_,
            double p_172423_,
            double p_172424_,
            double p_172425_,
            double p_172426_,
            double p_172427_,
            double p_172428_,
            RandomSource p_446265_
        ) {
            SuspendedParticle suspendedparticle = new SuspendedParticle(p_172422_, p_172423_, p_172424_, p_172425_, 0.0, -0.8F, 0.0, this.sprite.get(p_446265_)) {
                @Override
                public Optional<ParticleLimit> getParticleLimit() {
                    return Optional.of(ParticleLimit.SPORE_BLOSSOM);
                }
            };
            suspendedparticle.lifetime = Mth.randomBetweenInclusive(p_446265_, 500, 1000);
            suspendedparticle.gravity = 0.01F;
            suspendedparticle.setColor(0.32F, 0.5F, 0.22F);
            return suspendedparticle;
        }
    }

    @OnlyIn(Dist.CLIENT)
    public static class UnderwaterProvider implements ParticleProvider<SimpleParticleType> {
        private final SpriteSet sprite;

        public UnderwaterProvider(SpriteSet sprites) {
            this.sprite = sprites;
        }

        public Particle createParticle(
            SimpleParticleType p_108074_,
            ClientLevel p_108075_,
            double p_108076_,
            double p_108077_,
            double p_108078_,
            double p_108079_,
            double p_108080_,
            double p_108081_,
            RandomSource p_447049_
        ) {
            SuspendedParticle suspendedparticle = new SuspendedParticle(p_108075_, p_108076_, p_108077_, p_108078_, this.sprite.get(p_447049_));
            suspendedparticle.setColor(0.4F, 0.4F, 0.7F);
            return suspendedparticle;
        }
    }

    @OnlyIn(Dist.CLIENT)
    public static class WarpedSporeProvider implements ParticleProvider<SimpleParticleType> {
        private final SpriteSet sprite;

        public WarpedSporeProvider(SpriteSet sprites) {
            this.sprite = sprites;
        }

        public Particle createParticle(
            SimpleParticleType p_108095_,
            ClientLevel p_108096_,
            double p_108097_,
            double p_108098_,
            double p_108099_,
            double p_108100_,
            double p_108101_,
            double p_108102_,
            RandomSource p_447353_
        ) {
            double d0 = p_447353_.nextFloat() * -1.9 * p_447353_.nextFloat() * 0.1;
            SuspendedParticle suspendedparticle = new SuspendedParticle(p_108096_, p_108097_, p_108098_, p_108099_, 0.0, d0, 0.0, this.sprite.get(p_447353_));
            suspendedparticle.setColor(0.1F, 0.1F, 0.3F);
            suspendedparticle.setSize(0.001F, 0.001F);
            return suspendedparticle;
        }
    }
}
