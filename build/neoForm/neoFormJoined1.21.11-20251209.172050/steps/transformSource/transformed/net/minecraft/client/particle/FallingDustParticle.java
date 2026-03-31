package net.minecraft.client.particle;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.FallingBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.jspecify.annotations.Nullable;

@OnlyIn(Dist.CLIENT)
public class FallingDustParticle extends SingleQuadParticle {
    private final float rotSpeed;
    private final SpriteSet sprites;

    public FallingDustParticle(
        ClientLevel level, double x, double y, double z, float xSpeed, float ySpeed, float zSpeed, SpriteSet sprites
    ) {
        super(level, x, y, z, sprites.first());
        this.sprites = sprites;
        this.rCol = xSpeed;
        this.gCol = ySpeed;
        this.bCol = zSpeed;
        float f = 0.9F;
        this.quadSize *= 0.67499995F;
        int i = (int)(32.0 / (this.random.nextFloat() * 0.8 + 0.2));
        this.lifetime = (int)Math.max(i * 0.9F, 1.0F);
        this.setSpriteFromAge(sprites);
        this.rotSpeed = (this.random.nextFloat() - 0.5F) * 0.1F;
        this.roll = this.random.nextFloat() * (float) (Math.PI * 2);
    }

    @Override
    public SingleQuadParticle.Layer getLayer() {
        return SingleQuadParticle.Layer.OPAQUE;
    }

    @Override
    public float getQuadSize(float scaleFactor) {
        return this.quadSize * Mth.clamp((this.age + scaleFactor) / this.lifetime * 32.0F, 0.0F, 1.0F);
    }

    @Override
    public void tick() {
        this.xo = this.x;
        this.yo = this.y;
        this.zo = this.z;
        if (this.age++ >= this.lifetime) {
            this.remove();
        } else {
            this.setSpriteFromAge(this.sprites);
            this.oRoll = this.roll;
            this.roll = this.roll + (float) Math.PI * this.rotSpeed * 2.0F;
            if (this.onGround) {
                this.oRoll = this.roll = 0.0F;
            }

            this.move(this.xd, this.yd, this.zd);
            this.yd -= 0.003F;
            this.yd = Math.max(this.yd, -0.14F);
        }
    }

    @OnlyIn(Dist.CLIENT)
    public static class Provider implements ParticleProvider<BlockParticleOption> {
        private final SpriteSet sprite;

        public Provider(SpriteSet sprites) {
            this.sprite = sprites;
        }

        public @Nullable Particle createParticle(
            BlockParticleOption p_106636_,
            ClientLevel p_106637_,
            double p_106638_,
            double p_106639_,
            double p_106640_,
            double p_106641_,
            double p_106642_,
            double p_106643_,
            RandomSource p_446040_
        ) {
            BlockState blockstate = p_106636_.getState();
            if (!blockstate.isAir() && blockstate.getRenderShape() == RenderShape.INVISIBLE) {
                return null;
            } else {
                BlockPos blockpos = BlockPos.containing(p_106638_, p_106639_, p_106640_);
                int i = Minecraft.getInstance().getBlockColors().getColor(blockstate, p_106637_, blockpos);
                if (blockstate.getBlock() instanceof FallingBlock) {
                    i = ((FallingBlock)blockstate.getBlock()).getDustColor(blockstate, p_106637_, blockpos);
                }

                float f = (i >> 16 & 0xFF) / 255.0F;
                float f1 = (i >> 8 & 0xFF) / 255.0F;
                float f2 = (i & 0xFF) / 255.0F;
                return new FallingDustParticle(p_106637_, p_106638_, p_106639_, p_106640_, f, f1, f2, this.sprite);
            }
        }
    }
}
