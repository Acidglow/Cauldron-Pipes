package net.minecraft.client.particle;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.particles.ColorParticleOption;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.core.particles.SpellParticleOption;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class SpellParticle extends SingleQuadParticle {
    private static final RandomSource RANDOM = RandomSource.create();
    private final SpriteSet sprites;
    private float originalAlpha = 1.0F;

    public SpellParticle(
        ClientLevel level, double x, double y, double z, double xSpeed, double ySpeed, double zSpeed, SpriteSet sprites
    ) {
        super(level, x, y, z, 0.5 - RANDOM.nextDouble(), ySpeed, 0.5 - RANDOM.nextDouble(), sprites.first());
        this.friction = 0.96F;
        this.gravity = -0.1F;
        this.speedUpWhenYMotionIsBlocked = true;
        this.sprites = sprites;
        this.yd *= 0.2F;
        if (xSpeed == 0.0 && zSpeed == 0.0) {
            this.xd *= 0.1F;
            this.zd *= 0.1F;
        }

        this.quadSize *= 0.75F;
        this.lifetime = (int)(8.0 / (this.random.nextFloat() * 0.8 + 0.2));
        this.hasPhysics = false;
        this.setSpriteFromAge(sprites);
        if (this.isCloseToScopingPlayer()) {
            this.setAlpha(0.0F);
        }
    }

    @Override
    public SingleQuadParticle.Layer getLayer() {
        return SingleQuadParticle.Layer.TRANSLUCENT;
    }

    @Override
    public void tick() {
        super.tick();
        this.setSpriteFromAge(this.sprites);
        if (this.isCloseToScopingPlayer()) {
            this.alpha = 0.0F;
        } else {
            this.alpha = Mth.lerp(0.05F, this.alpha, this.originalAlpha);
        }
    }

    @Override
    protected void setAlpha(float p_340807_) {
        super.setAlpha(p_340807_);
        this.originalAlpha = p_340807_;
    }

    private boolean isCloseToScopingPlayer() {
        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer localplayer = minecraft.player;
        return localplayer != null
            && localplayer.getEyePosition().distanceToSqr(this.x, this.y, this.z) <= 9.0
            && minecraft.options.getCameraType().isFirstPerson()
            && localplayer.isScoping();
    }

    @OnlyIn(Dist.CLIENT)
    public static class InstantProvider implements ParticleProvider<SpellParticleOption> {
        private final SpriteSet sprite;

        public InstantProvider(SpriteSet sprites) {
            this.sprite = sprites;
        }

        public Particle createParticle(
            SpellParticleOption p_446342_,
            ClientLevel p_107817_,
            double p_107818_,
            double p_107819_,
            double p_107820_,
            double p_107821_,
            double p_107822_,
            double p_107823_,
            RandomSource p_446711_
        ) {
            SpellParticle spellparticle = new SpellParticle(p_107817_, p_107818_, p_107819_, p_107820_, p_107821_, p_107822_, p_107823_, this.sprite);
            spellparticle.setColor(p_446342_.getRed(), p_446342_.getGreen(), p_446342_.getBlue());
            spellparticle.setPower(p_446342_.getPower());
            return spellparticle;
        }
    }

    @OnlyIn(Dist.CLIENT)
    public static class MobEffectProvider implements ParticleProvider<ColorParticleOption> {
        private final SpriteSet sprite;

        public MobEffectProvider(SpriteSet sprite) {
            this.sprite = sprite;
        }

        public Particle createParticle(
            ColorParticleOption p_445723_,
            ClientLevel p_334055_,
            double p_334040_,
            double p_333846_,
            double p_333947_,
            double p_333819_,
            double p_333860_,
            double p_333737_,
            RandomSource p_445751_
        ) {
            SpellParticle spellparticle = new SpellParticle(p_334055_, p_334040_, p_333846_, p_333947_, p_333819_, p_333860_, p_333737_, this.sprite);
            spellparticle.setColor(p_445723_.getRed(), p_445723_.getGreen(), p_445723_.getBlue());
            spellparticle.setAlpha(p_445723_.getAlpha());
            return spellparticle;
        }
    }

    @OnlyIn(Dist.CLIENT)
    public static class Provider implements ParticleProvider<SimpleParticleType> {
        private final SpriteSet sprite;

        public Provider(SpriteSet sprites) {
            this.sprite = sprites;
        }

        public Particle createParticle(
            SimpleParticleType p_107858_,
            ClientLevel p_107859_,
            double p_107860_,
            double p_107861_,
            double p_107862_,
            double p_107863_,
            double p_107864_,
            double p_107865_,
            RandomSource p_445851_
        ) {
            return new SpellParticle(p_107859_, p_107860_, p_107861_, p_107862_, p_107863_, p_107864_, p_107865_, this.sprite);
        }
    }

    @OnlyIn(Dist.CLIENT)
    public static class WitchProvider implements ParticleProvider<SimpleParticleType> {
        private final SpriteSet sprite;

        public WitchProvider(SpriteSet sprites) {
            this.sprite = sprites;
        }

        public Particle createParticle(
            SimpleParticleType p_445971_,
            ClientLevel p_107871_,
            double p_107872_,
            double p_107873_,
            double p_107874_,
            double p_107875_,
            double p_107876_,
            double p_107877_,
            RandomSource p_445767_
        ) {
            SpellParticle spellparticle = new SpellParticle(p_107871_, p_107872_, p_107873_, p_107874_, p_107875_, p_107876_, p_107877_, this.sprite);
            float f = p_445767_.nextFloat() * 0.5F + 0.35F;
            spellparticle.setColor(1.0F * f, 0.0F * f, 1.0F * f);
            return spellparticle;
        }
    }
}
