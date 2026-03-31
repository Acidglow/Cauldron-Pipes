package net.minecraft.client.particle;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.RandomSource;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class BubbleColumnUpParticle extends SingleQuadParticle {
    public BubbleColumnUpParticle(
        ClientLevel p_105733_,
        double p_105734_,
        double p_105735_,
        double p_105736_,
        double p_105737_,
        double p_105738_,
        double p_105739_,
        TextureAtlasSprite p_446745_
    ) {
        super(p_105733_, p_105734_, p_105735_, p_105736_, p_446745_);
        this.gravity = -0.125F;
        this.friction = 0.85F;
        this.setSize(0.02F, 0.02F);
        this.quadSize = this.quadSize * (this.random.nextFloat() * 0.6F + 0.2F);
        this.xd = p_105737_ * 0.2F + (this.random.nextFloat() * 2.0F - 1.0F) * 0.02F;
        this.yd = p_105738_ * 0.2F + (this.random.nextFloat() * 2.0F - 1.0F) * 0.02F;
        this.zd = p_105739_ * 0.2F + (this.random.nextFloat() * 2.0F - 1.0F) * 0.02F;
        this.lifetime = (int)(40.0 / (this.random.nextFloat() * 0.8 + 0.2));
    }

    @Override
    public void tick() {
        super.tick();
        if (!this.removed && !this.level.getFluidState(BlockPos.containing(this.x, this.y, this.z)).is(FluidTags.WATER)) {
            this.remove();
        }
    }

    @Override
    public SingleQuadParticle.Layer getLayer() {
        return SingleQuadParticle.Layer.OPAQUE;
    }

    @OnlyIn(Dist.CLIENT)
    public static class Provider implements ParticleProvider<SimpleParticleType> {
        private final SpriteSet sprite;

        public Provider(SpriteSet sprites) {
            this.sprite = sprites;
        }

        public Particle createParticle(
            SimpleParticleType p_105764_,
            ClientLevel p_105765_,
            double p_105766_,
            double p_105767_,
            double p_105768_,
            double p_105769_,
            double p_105770_,
            double p_105771_,
            RandomSource p_446344_
        ) {
            return new BubbleColumnUpParticle(p_105765_, p_105766_, p_105767_, p_105768_, p_105769_, p_105770_, p_105771_, this.sprite.get(p_446344_));
        }
    }
}
