package net.minecraft.client.particle;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.util.RandomSource;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class ReversePortalParticle extends PortalParticle {
    public ReversePortalParticle(
        ClientLevel p_107590_,
        double p_107591_,
        double p_107592_,
        double p_107593_,
        double p_107594_,
        double p_107595_,
        double p_107596_,
        TextureAtlasSprite p_446660_
    ) {
        super(p_107590_, p_107591_, p_107592_, p_107593_, p_107594_, p_107595_, p_107596_, p_446660_);
        this.quadSize *= 1.5F;
        this.lifetime = (int)(this.random.nextFloat() * 2.0F) + 60;
    }

    @Override
    public float getQuadSize(float scaleFactor) {
        float f = 1.0F - (this.age + scaleFactor) / (this.lifetime * 1.5F);
        return this.quadSize * f;
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
            this.x = this.x + this.xd * f;
            this.y = this.y + this.yd * f;
            this.z = this.z + this.zd * f;
            this.setPos(this.x, this.y, this.z); // Neo: update the particle's bounding box
        }
    }

    @OnlyIn(Dist.CLIENT)
    public static class ReversePortalProvider implements ParticleProvider<SimpleParticleType> {
        private final SpriteSet sprite;

        public ReversePortalProvider(SpriteSet sprites) {
            this.sprite = sprites;
        }

        public Particle createParticle(
            SimpleParticleType p_107622_,
            ClientLevel p_107623_,
            double p_107624_,
            double p_107625_,
            double p_107626_,
            double p_107627_,
            double p_107628_,
            double p_107629_,
            RandomSource p_446071_
        ) {
            return new ReversePortalParticle(p_107623_, p_107624_, p_107625_, p_107626_, p_107627_, p_107628_, p_107629_, this.sprite.get(p_446071_));
        }
    }
}
