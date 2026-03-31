package net.minecraft.client.particle;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.util.RandomSource;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.joml.Vector3f;

@OnlyIn(Dist.CLIENT)
public class DustParticle extends DustParticleBase<DustParticleOptions> {
    public DustParticle(
        ClientLevel level,
        double x,
        double y,
        double z,
        double xSpeed,
        double ySpeed,
        double zSpeed,
        DustParticleOptions options,
        SpriteSet sprites
    ) {
        super(level, x, y, z, xSpeed, ySpeed, zSpeed, options, sprites);
        float f = this.random.nextFloat() * 0.4F + 0.6F;
        Vector3f vector3f = options.getColor();
        this.rCol = this.randomizeColor(vector3f.x(), f);
        this.gCol = this.randomizeColor(vector3f.y(), f);
        this.bCol = this.randomizeColor(vector3f.z(), f);
    }

    @OnlyIn(Dist.CLIENT)
    public static class Provider implements ParticleProvider<DustParticleOptions> {
        private final SpriteSet sprites;

        public Provider(SpriteSet sprites) {
            this.sprites = sprites;
        }

        public Particle createParticle(
            DustParticleOptions p_106443_,
            ClientLevel p_106444_,
            double p_106445_,
            double p_106446_,
            double p_106447_,
            double p_106448_,
            double p_106449_,
            double p_106450_,
            RandomSource p_447325_
        ) {
            return new DustParticle(p_106444_, p_106445_, p_106446_, p_106447_, p_106448_, p_106449_, p_106450_, p_106443_, this.sprites);
        }
    }
}
