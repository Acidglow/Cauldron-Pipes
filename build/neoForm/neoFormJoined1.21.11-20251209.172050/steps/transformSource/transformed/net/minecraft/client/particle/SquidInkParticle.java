package net.minecraft.client.particle;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.util.ARGB;
import net.minecraft.util.RandomSource;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class SquidInkParticle extends SimpleAnimatedParticle {
    public SquidInkParticle(
        ClientLevel level,
        double x,
        double y,
        double z,
        double xSpeed,
        double ySpeed,
        double zSpeed,
        int packedColor,
        SpriteSet sprites
    ) {
        super(level, x, y, z, sprites, 0.0F);
        this.friction = 0.92F;
        this.quadSize = 0.5F;
        this.setAlpha(1.0F);
        this.setColor(ARGB.redFloat(packedColor), ARGB.greenFloat(packedColor), ARGB.blueFloat(packedColor));
        this.lifetime = (int)(this.quadSize * 12.0F / (this.random.nextFloat() * 0.8F + 0.2F));
        this.setSpriteFromAge(sprites);
        this.hasPhysics = false;
        this.xd = xSpeed;
        this.yd = ySpeed;
        this.zd = zSpeed;
    }

    @Override
    public void tick() {
        super.tick();
        if (!this.removed) {
            this.setSpriteFromAge(this.sprites);
            if (this.age > this.lifetime / 2) {
                this.setAlpha(1.0F - ((float)this.age - this.lifetime / 2) / this.lifetime);
            }

            if (this.level.getBlockState(BlockPos.containing(this.x, this.y, this.z)).isAir()) {
                this.yd -= 0.0074F;
            }
        }
    }

    @OnlyIn(Dist.CLIENT)
    public static class GlowInkProvider implements ParticleProvider<SimpleParticleType> {
        private final SpriteSet sprites;

        public GlowInkProvider(SpriteSet sprites) {
            this.sprites = sprites;
        }

        public Particle createParticle(
            SimpleParticleType p_172347_,
            ClientLevel p_172348_,
            double p_172349_,
            double p_172350_,
            double p_172351_,
            double p_172352_,
            double p_172353_,
            double p_172354_,
            RandomSource p_446724_
        ) {
            return new SquidInkParticle(
                p_172348_, p_172349_, p_172350_, p_172351_, p_172352_, p_172353_, p_172354_, ARGB.colorFromFloat(1.0F, 0.2F, 0.8F, 0.6F), this.sprites
            );
        }
    }

    @OnlyIn(Dist.CLIENT)
    public static class Provider implements ParticleProvider<SimpleParticleType> {
        private final SpriteSet sprites;

        public Provider(SpriteSet sprites) {
            this.sprites = sprites;
        }

        public Particle createParticle(
            SimpleParticleType p_108002_,
            ClientLevel p_108003_,
            double p_108004_,
            double p_108005_,
            double p_108006_,
            double p_108007_,
            double p_108008_,
            double p_108009_,
            RandomSource p_445599_
        ) {
            return new SquidInkParticle(p_108003_, p_108004_, p_108005_, p_108006_, p_108007_, p_108008_, p_108009_, -16777216, this.sprites);
        }
    }
}
