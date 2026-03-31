package net.minecraft.client.particle;

import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.state.QuadParticleRenderState;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.particles.ShriekParticleOption;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.joml.Quaternionf;

@OnlyIn(Dist.CLIENT)
public class ShriekParticle extends SingleQuadParticle {
    private static final float MAGICAL_X_ROT = 1.0472F;
    private int delay;

    public ShriekParticle(ClientLevel level, double x, double y, double z, int delay, TextureAtlasSprite sprite) {
        super(level, x, y, z, 0.0, 0.0, 0.0, sprite);
        this.quadSize = 0.85F;
        this.delay = delay;
        this.lifetime = 30;
        this.gravity = 0.0F;
        this.xd = 0.0;
        this.yd = 0.1;
        this.zd = 0.0;
    }

    @Override
    public float getQuadSize(float p_234003_) {
        return this.quadSize * Mth.clamp((this.age + p_234003_) / this.lifetime * 0.75F, 0.0F, 1.0F);
    }

    @Override
    public void extract(QuadParticleRenderState p_451512_, Camera p_446810_, float p_446438_) {
        if (this.delay <= 0) {
            this.alpha = 1.0F - Mth.clamp((this.age + p_446438_) / this.lifetime, 0.0F, 1.0F);
            Quaternionf quaternionf = new Quaternionf();
            quaternionf.rotationX(-1.0472F);
            this.extractRotatedQuad(p_451512_, p_446810_, quaternionf, p_446438_);
            quaternionf.rotationYXZ((float) -Math.PI, 1.0472F, 0.0F);
            this.extractRotatedQuad(p_451512_, p_446810_, quaternionf, p_446438_);
        }
    }

    @Override
    public int getLightColor(float p_233983_) {
        return 240;
    }

    @Override
    public SingleQuadParticle.Layer getLayer() {
        return SingleQuadParticle.Layer.TRANSLUCENT;
    }

    @Override
    public void tick() {
        if (this.delay > 0) {
            this.delay--;
        } else {
            super.tick();
        }
    }

    @OnlyIn(Dist.CLIENT)
    public static class Provider implements ParticleProvider<ShriekParticleOption> {
        private final SpriteSet sprite;

        public Provider(SpriteSet sprite) {
            this.sprite = sprite;
        }

        public Particle createParticle(
            ShriekParticleOption p_446188_,
            ClientLevel p_234011_,
            double p_234012_,
            double p_234013_,
            double p_234014_,
            double p_234015_,
            double p_234016_,
            double p_234017_,
            RandomSource p_445665_
        ) {
            ShriekParticle shriekparticle = new ShriekParticle(p_234011_, p_234012_, p_234013_, p_234014_, p_446188_.getDelay(), this.sprite.get(p_445665_));
            shriekparticle.setAlpha(1.0F);
            return shriekparticle;
        }
    }
}
