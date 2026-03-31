package net.minecraft.client.particle;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.util.RandomSource;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class SoulParticle extends RisingParticle {
    private final SpriteSet sprites;
    protected boolean isGlowing;

    public SoulParticle(
        ClientLevel level, double x, double y, double z, double xSpeed, double ySpeed, double zSpeed, SpriteSet sprites
    ) {
        super(level, x, y, z, xSpeed, ySpeed, zSpeed, sprites.first());
        this.sprites = sprites;
        this.scale(1.5F);
        this.setSpriteFromAge(sprites);
    }

    @Override
    public int getLightColor(float p_234080_) {
        return this.isGlowing ? 240 : super.getLightColor(p_234080_);
    }

    @Override
    public SingleQuadParticle.Layer getLayer() {
        return SingleQuadParticle.Layer.TRANSLUCENT;
    }

    @Override
    public void tick() {
        super.tick();
        this.setSpriteFromAge(this.sprites);
    }

    @OnlyIn(Dist.CLIENT)
    public static class EmissiveProvider implements ParticleProvider<SimpleParticleType> {
        private final SpriteSet sprite;

        public EmissiveProvider(SpriteSet sprite) {
            this.sprite = sprite;
        }

        public Particle createParticle(
            SimpleParticleType p_234094_,
            ClientLevel p_234095_,
            double p_234096_,
            double p_234097_,
            double p_234098_,
            double p_234099_,
            double p_234100_,
            double p_234101_,
            RandomSource p_445970_
        ) {
            SoulParticle soulparticle = new SoulParticle(p_234095_, p_234096_, p_234097_, p_234098_, p_234099_, p_234100_, p_234101_, this.sprite);
            soulparticle.setAlpha(1.0F);
            soulparticle.isGlowing = true;
            return soulparticle;
        }
    }

    @OnlyIn(Dist.CLIENT)
    public static class Provider implements ParticleProvider<SimpleParticleType> {
        private final SpriteSet sprite;

        public Provider(SpriteSet sprites) {
            this.sprite = sprites;
        }

        public Particle createParticle(
            SimpleParticleType p_107750_,
            ClientLevel p_107751_,
            double p_107752_,
            double p_107753_,
            double p_107754_,
            double p_107755_,
            double p_107756_,
            double p_107757_,
            RandomSource p_445496_
        ) {
            SoulParticle soulparticle = new SoulParticle(p_107751_, p_107752_, p_107753_, p_107754_, p_107755_, p_107756_, p_107757_, this.sprite);
            soulparticle.setAlpha(1.0F);
            return soulparticle;
        }
    }
}
