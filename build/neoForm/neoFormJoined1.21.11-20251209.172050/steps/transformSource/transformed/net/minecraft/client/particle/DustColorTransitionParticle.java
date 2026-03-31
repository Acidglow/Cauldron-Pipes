package net.minecraft.client.particle;

import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.state.QuadParticleRenderState;
import net.minecraft.core.particles.DustColorTransitionOptions;
import net.minecraft.util.RandomSource;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.joml.Vector3f;

@OnlyIn(Dist.CLIENT)
public class DustColorTransitionParticle extends DustParticleBase<DustColorTransitionOptions> {
    private final Vector3f fromColor;
    private final Vector3f toColor;

    public DustColorTransitionParticle(
        ClientLevel level,
        double x,
        double y,
        double z,
        double xSpeed,
        double ySpeed,
        double zSpeed,
        DustColorTransitionOptions options,
        SpriteSet sprites
    ) {
        super(level, x, y, z, xSpeed, ySpeed, zSpeed, options, sprites);
        float f = this.random.nextFloat() * 0.4F + 0.6F;
        this.fromColor = this.randomizeColor(options.getFromColor(), f);
        this.toColor = this.randomizeColor(options.getToColor(), f);
    }

    private Vector3f randomizeColor(Vector3f vector, float multiplier) {
        return new Vector3f(
            this.randomizeColor(vector.x(), multiplier), this.randomizeColor(vector.y(), multiplier), this.randomizeColor(vector.z(), multiplier)
        );
    }

    private void lerpColors(float partialTick) {
        float f = (this.age + partialTick) / (this.lifetime + 1.0F);
        Vector3f vector3f = new Vector3f(this.fromColor).lerp(this.toColor, f);
        this.rCol = vector3f.x();
        this.gCol = vector3f.y();
        this.bCol = vector3f.z();
    }

    @Override
    public void extract(QuadParticleRenderState p_451050_, Camera p_447195_, float p_446547_) {
        this.lerpColors(p_446547_);
        super.extract(p_451050_, p_447195_, p_446547_);
    }

    @OnlyIn(Dist.CLIENT)
    public static class Provider implements ParticleProvider<DustColorTransitionOptions> {
        private final SpriteSet sprites;

        public Provider(SpriteSet sprites) {
            this.sprites = sprites;
        }

        public Particle createParticle(
            DustColorTransitionOptions p_172075_,
            ClientLevel p_172076_,
            double p_172077_,
            double p_172078_,
            double p_172079_,
            double p_172080_,
            double p_172081_,
            double p_172082_,
            RandomSource p_445963_
        ) {
            return new DustColorTransitionParticle(p_172076_, p_172077_, p_172078_, p_172079_, p_172080_, p_172081_, p_172082_, p_172075_, this.sprites);
        }
    }
}
