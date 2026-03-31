package net.minecraft.client.particle;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class FlameParticle extends RisingParticle {
    public FlameParticle(
        ClientLevel p_106800_,
        double p_106801_,
        double p_106802_,
        double p_106803_,
        double p_106804_,
        double p_106805_,
        double p_106806_,
        TextureAtlasSprite p_446489_
    ) {
        super(p_106800_, p_106801_, p_106802_, p_106803_, p_106804_, p_106805_, p_106806_, p_446489_);
    }

    @Override
    public SingleQuadParticle.Layer getLayer() {
        return SingleQuadParticle.Layer.OPAQUE;
    }

    @Override
    public void move(double x, double y, double z) {
        this.setBoundingBox(this.getBoundingBox().move(x, y, z));
        this.setLocationFromBoundingbox();
    }

    @Override
    public float getQuadSize(float scaleFactor) {
        float f = (this.age + scaleFactor) / this.lifetime;
        return this.quadSize * (1.0F - f * f * 0.5F);
    }

    @Override
    public int getLightColor(float partialTick) {
        float f = (this.age + partialTick) / this.lifetime;
        f = Mth.clamp(f, 0.0F, 1.0F);
        int i = super.getLightColor(partialTick);
        int j = i & 0xFF;
        int k = i >> 16 & 0xFF;
        j += (int)(f * 15.0F * 16.0F);
        if (j > 240) {
            j = 240;
        }

        return j | k << 16;
    }

    @OnlyIn(Dist.CLIENT)
    public static class Provider implements ParticleProvider<SimpleParticleType> {
        private final SpriteSet sprite;

        public Provider(SpriteSet sprites) {
            this.sprite = sprites;
        }

        public Particle createParticle(
            SimpleParticleType p_445473_,
            ClientLevel p_106830_,
            double p_106831_,
            double p_106832_,
            double p_106833_,
            double p_106834_,
            double p_106835_,
            double p_106836_,
            RandomSource p_445967_
        ) {
            return new FlameParticle(p_106830_, p_106831_, p_106832_, p_106833_, p_106834_, p_106835_, p_106836_, this.sprite.get(p_445967_));
        }
    }

    @OnlyIn(Dist.CLIENT)
    public static class SmallFlameProvider implements ParticleProvider<SimpleParticleType> {
        private final SpriteSet sprite;

        public SmallFlameProvider(SpriteSet sprites) {
            this.sprite = sprites;
        }

        public Particle createParticle(
            SimpleParticleType p_445848_,
            ClientLevel p_172116_,
            double p_172117_,
            double p_172118_,
            double p_172119_,
            double p_172120_,
            double p_172121_,
            double p_172122_,
            RandomSource p_446469_
        ) {
            FlameParticle flameparticle = new FlameParticle(
                p_172116_, p_172117_, p_172118_, p_172119_, p_172120_, p_172121_, p_172122_, this.sprite.get(p_446469_)
            );
            flameparticle.scale(0.5F);
            return flameparticle;
        }
    }
}
