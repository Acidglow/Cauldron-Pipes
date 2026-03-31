package net.minecraft.client.particle;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class FireflyParticle extends SingleQuadParticle {
    private static final float PARTICLE_FADE_OUT_LIGHT_TIME = 0.3F;
    private static final float PARTICLE_FADE_IN_LIGHT_TIME = 0.1F;
    private static final float PARTICLE_FADE_OUT_ALPHA_TIME = 0.5F;
    private static final float PARTICLE_FADE_IN_ALPHA_TIME = 0.3F;
    private static final int PARTICLE_MIN_LIFETIME = 200;
    private static final int PARTICLE_MAX_LIFETIME = 300;

    public FireflyParticle(
        ClientLevel p_401114_,
        double p_401068_,
        double p_401435_,
        double p_401238_,
        double p_401102_,
        double p_401330_,
        double p_401419_,
        TextureAtlasSprite p_446415_
    ) {
        super(p_401114_, p_401068_, p_401435_, p_401238_, p_401102_, p_401330_, p_401419_, p_446415_);
        this.speedUpWhenYMotionIsBlocked = true;
        this.friction = 0.96F;
        this.quadSize *= 0.75F;
        this.yd *= 0.8F;
        this.xd *= 0.8F;
        this.zd *= 0.8F;
    }

    @Override
    public SingleQuadParticle.Layer getLayer() {
        return SingleQuadParticle.Layer.TRANSLUCENT;
    }

    @Override
    public int getLightColor(float p_401057_) {
        return (int)(255.0F * getFadeAmount(this.getLifetimeProgress(this.age + p_401057_), 0.1F, 0.3F));
    }

    @Override
    public void tick() {
        super.tick();
        if (!this.level.getBlockState(BlockPos.containing(this.x, this.y, this.z)).isAir()) {
            this.remove();
        } else {
            this.setAlpha(getFadeAmount(this.getLifetimeProgress(this.age), 0.3F, 0.5F));
            if (this.random.nextFloat() > 0.95F || this.age == 1) {
                this.setParticleSpeed(-0.05F + 0.1F * this.random.nextFloat(), -0.05F + 0.1F * this.random.nextFloat(), -0.05F + 0.1F * this.random.nextFloat());
            }
        }
    }

    private float getLifetimeProgress(float age) {
        return Mth.clamp(age / this.lifetime, 0.0F, 1.0F);
    }

    private static float getFadeAmount(float lifetimeProgress, float fadeIn, float fadeOut) {
        if (lifetimeProgress >= 1.0F - fadeIn) {
            return (1.0F - lifetimeProgress) / fadeIn;
        } else {
            return lifetimeProgress <= fadeOut ? lifetimeProgress / fadeOut : 1.0F;
        }
    }

    @OnlyIn(Dist.CLIENT)
    public static class FireflyProvider implements ParticleProvider<SimpleParticleType> {
        private final SpriteSet sprite;

        public FireflyProvider(SpriteSet sprite) {
            this.sprite = sprite;
        }

        public Particle createParticle(
            SimpleParticleType p_446112_,
            ClientLevel p_401363_,
            double p_401005_,
            double p_401073_,
            double p_401316_,
            double p_401268_,
            double p_401386_,
            double p_401041_,
            RandomSource p_447278_
        ) {
            FireflyParticle fireflyparticle = new FireflyParticle(
                p_401363_,
                p_401005_,
                p_401073_,
                p_401316_,
                0.5 - p_447278_.nextDouble(),
                p_447278_.nextBoolean() ? p_401386_ : -p_401386_,
                0.5 - p_447278_.nextDouble(),
                this.sprite.get(p_447278_)
            );
            fireflyparticle.setLifetime(p_447278_.nextIntBetweenInclusive(200, 300));
            fireflyparticle.scale(1.5F);
            fireflyparticle.setAlpha(0.0F);
            return fireflyparticle;
        }
    }
}
