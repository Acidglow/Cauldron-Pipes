package net.minecraft.client.particle;

import java.util.Optional;
import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.state.QuadParticleRenderState;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.particles.VibrationParticleOption;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.gameevent.PositionSource;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.joml.Quaternionf;

@OnlyIn(Dist.CLIENT)
public class VibrationSignalParticle extends SingleQuadParticle {
    private final PositionSource target;
    private float rot;
    private float rotO;
    private float pitch;
    private float pitchO;

    public VibrationSignalParticle(
        ClientLevel level, double x, double y, double z, PositionSource target, int lifetime, TextureAtlasSprite sprite
    ) {
        super(level, x, y, z, 0.0, 0.0, 0.0, sprite);
        this.quadSize = 0.3F;
        this.target = target;
        this.lifetime = lifetime;
        Optional<Vec3> optional = target.getPosition(level);
        if (optional.isPresent()) {
            Vec3 vec3 = optional.get();
            double d0 = x - vec3.x();
            double d1 = y - vec3.y();
            double d2 = z - vec3.z();
            this.rotO = this.rot = (float)Mth.atan2(d0, d2);
            this.pitchO = this.pitch = (float)Mth.atan2(d1, Math.sqrt(d0 * d0 + d2 * d2));
        }
    }

    @Override
    public void extract(QuadParticleRenderState p_450988_, Camera p_446291_, float p_446150_) {
        float f = Mth.sin((this.age + p_446150_ - (float) (Math.PI * 2)) * 0.05F) * 2.0F;
        float f1 = Mth.lerp(p_446150_, this.rotO, this.rot);
        float f2 = Mth.lerp(p_446150_, this.pitchO, this.pitch) + (float) (Math.PI / 2);
        Quaternionf quaternionf = new Quaternionf();
        quaternionf.rotationY(f1).rotateX(-f2).rotateY(f);
        this.extractRotatedQuad(p_450988_, p_446291_, quaternionf, p_446150_);
        quaternionf.rotationY((float) -Math.PI + f1).rotateX(f2).rotateY(f);
        this.extractRotatedQuad(p_450988_, p_446291_, quaternionf, p_446150_);
    }

    @Override
    public int getLightColor(float p_172469_) {
        return 240;
    }

    @Override
    public SingleQuadParticle.Layer getLayer() {
        return SingleQuadParticle.Layer.TRANSLUCENT;
    }

    @Override
    public void tick() {
        this.xo = this.x;
        this.yo = this.y;
        this.zo = this.z;
        if (this.age++ >= this.lifetime) {
            this.remove();
        } else {
            Optional<Vec3> optional = this.target.getPosition(this.level);
            if (optional.isEmpty()) {
                this.remove();
            } else {
                int i = this.lifetime - this.age;
                double d0 = 1.0 / i;
                Vec3 vec3 = optional.get();
                this.x = Mth.lerp(d0, this.x, vec3.x());
                this.y = Mth.lerp(d0, this.y, vec3.y());
                this.z = Mth.lerp(d0, this.z, vec3.z());
                this.setPos(this.x, this.y, this.z); // FORGE: Update the particle's bounding box
                double d1 = this.x - vec3.x();
                double d2 = this.y - vec3.y();
                double d3 = this.z - vec3.z();
                this.rotO = this.rot;
                this.rot = (float)Mth.atan2(d1, d3);
                this.pitchO = this.pitch;
                this.pitch = (float)Mth.atan2(d2, Math.sqrt(d1 * d1 + d3 * d3));
            }
        }
    }

    @OnlyIn(Dist.CLIENT)
    public static class Provider implements ParticleProvider<VibrationParticleOption> {
        private final SpriteSet sprite;

        public Provider(SpriteSet sprites) {
            this.sprite = sprites;
        }

        public Particle createParticle(
            VibrationParticleOption p_447329_,
            ClientLevel p_172493_,
            double p_172494_,
            double p_172495_,
            double p_172496_,
            double p_172497_,
            double p_172498_,
            double p_172499_,
            RandomSource p_447120_
        ) {
            VibrationSignalParticle vibrationsignalparticle = new VibrationSignalParticle(
                p_172493_, p_172494_, p_172495_, p_172496_, p_447329_.getDestination(), p_447329_.getArrivalInTicks(), this.sprite.get(p_447120_)
            );
            vibrationsignalparticle.setAlpha(1.0F);
            return vibrationsignalparticle;
        }
    }
}
