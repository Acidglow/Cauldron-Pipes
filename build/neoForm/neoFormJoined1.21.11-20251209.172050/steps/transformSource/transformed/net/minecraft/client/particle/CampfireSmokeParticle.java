package net.minecraft.client.particle;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.util.RandomSource;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class CampfireSmokeParticle extends SingleQuadParticle {
    public CampfireSmokeParticle(
        ClientLevel level,
        double x,
        double y,
        double z,
        double xSpeed,
        double ySpeed,
        double zSpeed,
        boolean boosted,
        TextureAtlasSprite sprite
    ) {
        super(level, x, y, z, sprite);
        this.scale(3.0F);
        this.setSize(0.25F, 0.25F);
        if (boosted) {
            this.lifetime = this.random.nextInt(50) + 280;
        } else {
            this.lifetime = this.random.nextInt(50) + 80;
        }

        this.gravity = 3.0E-6F;
        this.xd = xSpeed;
        this.yd = ySpeed + this.random.nextFloat() / 500.0F;
        this.zd = zSpeed;
    }

    @Override
    public void tick() {
        this.xo = this.x;
        this.yo = this.y;
        this.zo = this.z;
        if (this.age++ < this.lifetime && !(this.alpha <= 0.0F)) {
            this.xd = this.xd + this.random.nextFloat() / 5000.0F * (this.random.nextBoolean() ? 1 : -1);
            this.zd = this.zd + this.random.nextFloat() / 5000.0F * (this.random.nextBoolean() ? 1 : -1);
            this.yd = this.yd - this.gravity;
            this.move(this.xd, this.yd, this.zd);
            if (this.age >= this.lifetime - 60 && this.alpha > 0.01F) {
                this.alpha -= 0.015F;
            }
        } else {
            this.remove();
        }
    }

    @Override
    public SingleQuadParticle.Layer getLayer() {
        return SingleQuadParticle.Layer.TRANSLUCENT;
    }

    @OnlyIn(Dist.CLIENT)
    public static class CosyProvider implements ParticleProvider<SimpleParticleType> {
        private final SpriteSet sprites;

        public CosyProvider(SpriteSet sprites) {
            this.sprites = sprites;
        }

        public Particle createParticle(
            SimpleParticleType p_447182_,
            ClientLevel p_105881_,
            double p_105882_,
            double p_105883_,
            double p_105884_,
            double p_105885_,
            double p_105886_,
            double p_105887_,
            RandomSource p_446507_
        ) {
            CampfireSmokeParticle campfiresmokeparticle = new CampfireSmokeParticle(
                p_105881_, p_105882_, p_105883_, p_105884_, p_105885_, p_105886_, p_105887_, false, this.sprites.get(p_446507_)
            );
            campfiresmokeparticle.setAlpha(0.9F);
            return campfiresmokeparticle;
        }
    }

    @OnlyIn(Dist.CLIENT)
    public static class SignalProvider implements ParticleProvider<SimpleParticleType> {
        private final SpriteSet sprites;

        public SignalProvider(SpriteSet sprites) {
            this.sprites = sprites;
        }

        public Particle createParticle(
            SimpleParticleType p_105910_,
            ClientLevel p_105911_,
            double p_105912_,
            double p_105913_,
            double p_105914_,
            double p_105915_,
            double p_105916_,
            double p_105917_,
            RandomSource p_445608_
        ) {
            CampfireSmokeParticle campfiresmokeparticle = new CampfireSmokeParticle(
                p_105911_, p_105912_, p_105913_, p_105914_, p_105915_, p_105916_, p_105917_, true, this.sprites.get(p_445608_)
            );
            campfiresmokeparticle.setAlpha(0.95F);
            return campfiresmokeparticle;
        }
    }
}
