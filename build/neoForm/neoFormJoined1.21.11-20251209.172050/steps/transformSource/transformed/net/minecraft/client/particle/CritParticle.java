package net.minecraft.client.particle;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class CritParticle extends SingleQuadParticle {
    public CritParticle(
        ClientLevel p_105919_,
        double p_105920_,
        double p_105921_,
        double p_105922_,
        double p_105923_,
        double p_105924_,
        double p_105925_,
        TextureAtlasSprite p_445674_
    ) {
        super(p_105919_, p_105920_, p_105921_, p_105922_, 0.0, 0.0, 0.0, p_445674_);
        this.friction = 0.7F;
        this.gravity = 0.5F;
        this.xd *= 0.1F;
        this.yd *= 0.1F;
        this.zd *= 0.1F;
        this.xd += p_105923_ * 0.4;
        this.yd += p_105924_ * 0.4;
        this.zd += p_105925_ * 0.4;
        float f = this.random.nextFloat() * 0.3F + 0.6F;
        this.rCol = f;
        this.gCol = f;
        this.bCol = f;
        this.quadSize *= 0.75F;
        this.lifetime = Math.max((int)(6.0 / (this.random.nextFloat() * 0.8 + 0.6)), 1);
        this.hasPhysics = false;
        this.tick();
    }

    @Override
    public float getQuadSize(float scaleFactor) {
        return this.quadSize * Mth.clamp((this.age + scaleFactor) / this.lifetime * 32.0F, 0.0F, 1.0F);
    }

    @Override
    public void tick() {
        super.tick();
        this.gCol *= 0.96F;
        this.bCol *= 0.9F;
    }

    @Override
    public SingleQuadParticle.Layer getLayer() {
        return SingleQuadParticle.Layer.OPAQUE;
    }

    @OnlyIn(Dist.CLIENT)
    public static class DamageIndicatorProvider implements ParticleProvider<SimpleParticleType> {
        private final SpriteSet sprite;

        public DamageIndicatorProvider(SpriteSet sprites) {
            this.sprite = sprites;
        }

        public Particle createParticle(
            SimpleParticleType p_446341_,
            ClientLevel p_105944_,
            double p_105945_,
            double p_105946_,
            double p_105947_,
            double p_105948_,
            double p_105949_,
            double p_105950_,
            RandomSource p_445374_
        ) {
            CritParticle critparticle = new CritParticle(
                p_105944_, p_105945_, p_105946_, p_105947_, p_105948_, p_105949_ + 1.0, p_105950_, this.sprite.get(p_445374_)
            );
            critparticle.setLifetime(20);
            return critparticle;
        }
    }

    @OnlyIn(Dist.CLIENT)
    public static class MagicProvider implements ParticleProvider<SimpleParticleType> {
        private final SpriteSet sprite;

        public MagicProvider(SpriteSet sprites) {
            this.sprite = sprites;
        }

        public Particle createParticle(
            SimpleParticleType p_447046_,
            ClientLevel p_105965_,
            double p_105966_,
            double p_105967_,
            double p_105968_,
            double p_105969_,
            double p_105970_,
            double p_105971_,
            RandomSource p_447058_
        ) {
            CritParticle critparticle = new CritParticle(
                p_105965_, p_105966_, p_105967_, p_105968_, p_105969_, p_105970_, p_105971_, this.sprite.get(p_447058_)
            );
            critparticle.rCol *= 0.3F;
            critparticle.gCol *= 0.8F;
            return critparticle;
        }
    }

    @OnlyIn(Dist.CLIENT)
    public static class Provider implements ParticleProvider<SimpleParticleType> {
        private final SpriteSet sprite;

        public Provider(SpriteSet sprites) {
            this.sprite = sprites;
        }

        public Particle createParticle(
            SimpleParticleType p_105994_,
            ClientLevel p_105995_,
            double p_105996_,
            double p_105997_,
            double p_105998_,
            double p_105999_,
            double p_106000_,
            double p_106001_,
            RandomSource p_445943_
        ) {
            return new CritParticle(p_105995_, p_105996_, p_105997_, p_105998_, p_105999_, p_106000_, p_106001_, this.sprite.get(p_445943_));
        }
    }
}
