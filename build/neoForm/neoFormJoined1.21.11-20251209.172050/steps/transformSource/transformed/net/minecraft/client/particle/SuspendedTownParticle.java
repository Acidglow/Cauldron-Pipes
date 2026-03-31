package net.minecraft.client.particle;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.util.RandomSource;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class SuspendedTownParticle extends SingleQuadParticle {
    public SuspendedTownParticle(
        ClientLevel p_108104_,
        double p_108105_,
        double p_108106_,
        double p_108107_,
        double p_108108_,
        double p_108109_,
        double p_108110_,
        TextureAtlasSprite p_446058_
    ) {
        super(p_108104_, p_108105_, p_108106_, p_108107_, p_108108_, p_108109_, p_108110_, p_446058_);
        float f = this.random.nextFloat() * 0.1F + 0.2F;
        this.rCol = f;
        this.gCol = f;
        this.bCol = f;
        this.setSize(0.02F, 0.02F);
        this.quadSize = this.quadSize * (this.random.nextFloat() * 0.6F + 0.5F);
        this.xd *= 0.02F;
        this.yd *= 0.02F;
        this.zd *= 0.02F;
        this.lifetime = (int)(20.0 / (this.random.nextFloat() * 0.8 + 0.2));
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
    public void tick() {
        this.xo = this.x;
        this.yo = this.y;
        this.zo = this.z;
        if (this.lifetime-- <= 0) {
            this.remove();
        } else {
            this.move(this.xd, this.yd, this.zd);
            this.xd *= 0.99;
            this.yd *= 0.99;
            this.zd *= 0.99;
        }
    }

    @OnlyIn(Dist.CLIENT)
    public static class ComposterFillProvider implements ParticleProvider<SimpleParticleType> {
        private final SpriteSet sprite;

        public ComposterFillProvider(SpriteSet sprites) {
            this.sprite = sprites;
        }

        public Particle createParticle(
            SimpleParticleType p_108139_,
            ClientLevel p_108140_,
            double p_108141_,
            double p_108142_,
            double p_108143_,
            double p_108144_,
            double p_108145_,
            double p_108146_,
            RandomSource p_445430_
        ) {
            SuspendedTownParticle suspendedtownparticle = new SuspendedTownParticle(
                p_108140_, p_108141_, p_108142_, p_108143_, p_108144_, p_108145_, p_108146_, this.sprite.get(p_445430_)
            );
            suspendedtownparticle.setColor(1.0F, 1.0F, 1.0F);
            suspendedtownparticle.setLifetime(3 + p_108140_.getRandom().nextInt(5));
            return suspendedtownparticle;
        }
    }

    @OnlyIn(Dist.CLIENT)
    public static class DolphinSpeedProvider implements ParticleProvider<SimpleParticleType> {
        private final SpriteSet sprite;

        public DolphinSpeedProvider(SpriteSet sprites) {
            this.sprite = sprites;
        }

        public Particle createParticle(
            SimpleParticleType p_108160_,
            ClientLevel p_108161_,
            double p_108162_,
            double p_108163_,
            double p_108164_,
            double p_108165_,
            double p_108166_,
            double p_108167_,
            RandomSource p_445810_
        ) {
            SuspendedTownParticle suspendedtownparticle = new SuspendedTownParticle(
                p_108161_, p_108162_, p_108163_, p_108164_, p_108165_, p_108166_, p_108167_, this.sprite.get(p_445810_)
            );
            suspendedtownparticle.setColor(0.3F, 0.5F, 1.0F);
            suspendedtownparticle.setAlpha(1.0F - p_445810_.nextFloat() * 0.7F);
            suspendedtownparticle.setLifetime(suspendedtownparticle.getLifetime() / 2);
            return suspendedtownparticle;
        }
    }

    @OnlyIn(Dist.CLIENT)
    public static class EggCrackProvider implements ParticleProvider<SimpleParticleType> {
        private final SpriteSet sprite;

        public EggCrackProvider(SpriteSet sprite) {
            this.sprite = sprite;
        }

        public Particle createParticle(
            SimpleParticleType p_446750_,
            ClientLevel p_277576_,
            double p_277798_,
            double p_277560_,
            double p_277731_,
            double p_277543_,
            double p_277890_,
            double p_277605_,
            RandomSource p_447131_
        ) {
            SuspendedTownParticle suspendedtownparticle = new SuspendedTownParticle(
                p_277576_, p_277798_, p_277560_, p_277731_, p_277543_, p_277890_, p_277605_, this.sprite.get(p_447131_)
            );
            suspendedtownparticle.setColor(1.0F, 1.0F, 1.0F);
            return suspendedtownparticle;
        }
    }

    @OnlyIn(Dist.CLIENT)
    public static class HappyVillagerProvider implements ParticleProvider<SimpleParticleType> {
        private final SpriteSet sprite;

        public HappyVillagerProvider(SpriteSet sprites) {
            this.sprite = sprites;
        }

        public Particle createParticle(
            SimpleParticleType p_447077_,
            ClientLevel p_108173_,
            double p_108174_,
            double p_108175_,
            double p_108176_,
            double p_108177_,
            double p_108178_,
            double p_108179_,
            RandomSource p_446253_
        ) {
            SuspendedTownParticle suspendedtownparticle = new SuspendedTownParticle(
                p_108173_, p_108174_, p_108175_, p_108176_, p_108177_, p_108178_, p_108179_, this.sprite.get(p_446253_)
            );
            suspendedtownparticle.setColor(1.0F, 1.0F, 1.0F);
            return suspendedtownparticle;
        }
    }

    @OnlyIn(Dist.CLIENT)
    public static class Provider implements ParticleProvider<SimpleParticleType> {
        private final SpriteSet sprite;

        public Provider(SpriteSet sprites) {
            this.sprite = sprites;
        }

        public Particle createParticle(
            SimpleParticleType p_108202_,
            ClientLevel p_108203_,
            double p_108204_,
            double p_108205_,
            double p_108206_,
            double p_108207_,
            double p_108208_,
            double p_108209_,
            RandomSource p_445616_
        ) {
            return new SuspendedTownParticle(p_108203_, p_108204_, p_108205_, p_108206_, p_108207_, p_108208_, p_108209_, this.sprite.get(p_445616_));
        }
    }
}
