package net.minecraft.client.particle;

import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.state.QuadParticleRenderState;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.util.RandomSource;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class FlyTowardsPositionParticle extends SingleQuadParticle {
    private final double xStart;
    private final double yStart;
    private final double zStart;
    private final boolean isGlowing;
    private final Particle.LifetimeAlpha lifetimeAlpha;

    public FlyTowardsPositionParticle(
        ClientLevel p_323938_,
        double p_323720_,
        double p_324407_,
        double p_324020_,
        double p_323737_,
        double p_323883_,
        double p_324615_,
        TextureAtlasSprite p_447374_
    ) {
        this(p_323938_, p_323720_, p_324407_, p_324020_, p_323737_, p_323883_, p_324615_, false, Particle.LifetimeAlpha.ALWAYS_OPAQUE, p_447374_);
    }

    public FlyTowardsPositionParticle(
        ClientLevel level,
        double x,
        double y,
        double z,
        double xSpeed,
        double ySpeed,
        double zSpeed,
        boolean isGlowing,
        Particle.LifetimeAlpha lifetimeAlpha,
        TextureAtlasSprite sprite
    ) {
        super(level, x, y, z, sprite);
        this.isGlowing = isGlowing;
        this.lifetimeAlpha = lifetimeAlpha;
        this.setAlpha(lifetimeAlpha.startAlpha());
        this.xd = xSpeed;
        this.yd = ySpeed;
        this.zd = zSpeed;
        this.xStart = x;
        this.yStart = y;
        this.zStart = z;
        this.xo = x + xSpeed;
        this.yo = y + ySpeed;
        this.zo = z + zSpeed;
        this.x = this.xo;
        this.y = this.yo;
        this.z = this.zo;
        this.quadSize = 0.1F * (this.random.nextFloat() * 0.5F + 0.2F);
        float f = this.random.nextFloat() * 0.6F + 0.4F;
        this.rCol = 0.9F * f;
        this.gCol = 0.9F * f;
        this.bCol = f;
        this.hasPhysics = false;
        this.lifetime = (int)(this.random.nextFloat() * 10.0F) + 30;
    }

    @Override
    public SingleQuadParticle.Layer getLayer() {
        return this.lifetimeAlpha.isOpaque() ? SingleQuadParticle.Layer.OPAQUE : SingleQuadParticle.Layer.TRANSLUCENT;
    }

    @Override
    public void move(double p_324487_, double p_323538_, double p_324364_) {
        this.setBoundingBox(this.getBoundingBox().move(p_324487_, p_323538_, p_324364_));
        this.setLocationFromBoundingbox();
    }

    @Override
    public int getLightColor(float p_323664_) {
        if (this.isGlowing) {
            return 240;
        } else {
            int i = super.getLightColor(p_323664_);
            float f = (float)this.age / this.lifetime;
            f *= f;
            f *= f;
            int j = i & 0xFF;
            int k = i >> 16 & 0xFF;
            k += (int)(f * 15.0F * 16.0F);
            if (k > 240) {
                k = 240;
            }

            return j | k << 16;
        }
    }

    @Override
    public void tick() {
        this.xo = this.x;
        this.yo = this.y;
        this.zo = this.z;
        if (this.age++ >= this.lifetime) {
            this.remove();
        } else {
            float f = (float)this.age / this.lifetime;
            f = 1.0F - f;
            float f1 = 1.0F - f;
            f1 *= f1;
            f1 *= f1;
            this.x = this.xStart + this.xd * f;
            this.y = this.yStart + this.yd * f - f1 * 1.2F;
            this.z = this.zStart + this.zd * f;
        }
    }

    @Override
    public void extract(QuadParticleRenderState p_451183_, Camera p_445787_, float p_445368_) {
        this.setAlpha(this.lifetimeAlpha.currentAlphaForAge(this.age, this.lifetime, p_445368_));
        super.extract(p_451183_, p_445787_, p_445368_);
    }

    @OnlyIn(Dist.CLIENT)
    public static class EnchantProvider implements ParticleProvider<SimpleParticleType> {
        private final SpriteSet sprite;

        public EnchantProvider(SpriteSet sprite) {
            this.sprite = sprite;
        }

        public Particle createParticle(
            SimpleParticleType p_323913_,
            ClientLevel p_323933_,
            double p_324281_,
            double p_323543_,
            double p_324051_,
            double p_323907_,
            double p_324082_,
            double p_323993_,
            RandomSource p_445586_
        ) {
            return new FlyTowardsPositionParticle(p_323933_, p_324281_, p_323543_, p_324051_, p_323907_, p_324082_, p_323993_, this.sprite.get(p_445586_));
        }
    }

    @OnlyIn(Dist.CLIENT)
    public static class NautilusProvider implements ParticleProvider<SimpleParticleType> {
        private final SpriteSet sprite;

        public NautilusProvider(SpriteSet sprite) {
            this.sprite = sprite;
        }

        public Particle createParticle(
            SimpleParticleType p_447078_,
            ClientLevel p_324405_,
            double p_324340_,
            double p_324490_,
            double p_324492_,
            double p_323916_,
            double p_323608_,
            double p_324252_,
            RandomSource p_446331_
        ) {
            return new FlyTowardsPositionParticle(p_324405_, p_324340_, p_324490_, p_324492_, p_323916_, p_323608_, p_324252_, this.sprite.get(p_446331_));
        }
    }

    @OnlyIn(Dist.CLIENT)
    public static class VaultConnectionProvider implements ParticleProvider<SimpleParticleType> {
        private final SpriteSet sprite;

        public VaultConnectionProvider(SpriteSet sprite) {
            this.sprite = sprite;
        }

        public Particle createParticle(
            SimpleParticleType p_445566_,
            ClientLevel p_323512_,
            double p_323694_,
            double p_323553_,
            double p_324089_,
            double p_323684_,
            double p_323670_,
            double p_324554_,
            RandomSource p_446650_
        ) {
            FlyTowardsPositionParticle flytowardspositionparticle = new FlyTowardsPositionParticle(
                p_323512_,
                p_323694_,
                p_323553_,
                p_324089_,
                p_323684_,
                p_323670_,
                p_324554_,
                true,
                new Particle.LifetimeAlpha(0.0F, 0.6F, 0.25F, 1.0F),
                this.sprite.get(p_446650_)
            );
            flytowardspositionparticle.scale(1.5F);
            return flytowardspositionparticle;
        }
    }
}
