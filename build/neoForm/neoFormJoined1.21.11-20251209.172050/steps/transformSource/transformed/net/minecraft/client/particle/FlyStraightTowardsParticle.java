package net.minecraft.client.particle;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.util.ARGB;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class FlyStraightTowardsParticle extends SingleQuadParticle {
    private final double xStart;
    private final double yStart;
    private final double zStart;
    private final int startColor;
    private final int endColor;

    public FlyStraightTowardsParticle(
        ClientLevel level,
        double x,
        double y,
        double z,
        double xSpeed,
        double ySpeed,
        double zSpeed,
        int startColor,
        int endColor,
        TextureAtlasSprite sprite
    ) {
        super(level, x, y, z, sprite);
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
        this.hasPhysics = false;
        this.lifetime = (int)(this.random.nextFloat() * 5.0F) + 25;
        this.startColor = startColor;
        this.endColor = endColor;
    }

    @Override
    public SingleQuadParticle.Layer getLayer() {
        return SingleQuadParticle.Layer.OPAQUE;
    }

    @Override
    public void move(double p_338805_, double p_338843_, double p_338720_) {
    }

    @Override
    public int getLightColor(float p_338732_) {
        return 240;
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
            float f1 = 1.0F - f;
            this.x = this.xStart + this.xd * f1;
            this.y = this.yStart + this.yd * f1;
            this.z = this.zStart + this.zd * f1;
            int i = ARGB.srgbLerp(f, this.startColor, this.endColor);
            this.setColor(ARGB.red(i) / 255.0F, ARGB.green(i) / 255.0F, ARGB.blue(i) / 255.0F);
            this.setAlpha(ARGB.alpha(i) / 255.0F);
        }
    }

    @OnlyIn(Dist.CLIENT)
    public static class OminousSpawnProvider implements ParticleProvider<SimpleParticleType> {
        private final SpriteSet sprite;

        public OminousSpawnProvider(SpriteSet sprite) {
            this.sprite = sprite;
        }

        public Particle createParticle(
            SimpleParticleType p_338365_,
            ClientLevel p_338448_,
            double p_338829_,
            double p_338561_,
            double p_338765_,
            double p_338694_,
            double p_338802_,
            double p_338768_,
            RandomSource p_445859_
        ) {
            FlyStraightTowardsParticle flystraighttowardsparticle = new FlyStraightTowardsParticle(
                p_338448_, p_338829_, p_338561_, p_338765_, p_338694_, p_338802_, p_338768_, -12210434, -1, this.sprite.get(p_445859_)
            );
            flystraighttowardsparticle.scale(Mth.randomBetween(p_338448_.getRandom(), 3.0F, 5.0F));
            return flystraighttowardsparticle;
        }
    }
}
