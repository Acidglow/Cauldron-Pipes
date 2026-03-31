package net.minecraft.client.particle;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class DripParticle extends SingleQuadParticle {
    private final Fluid type;
    protected boolean isGlowing;

    public DripParticle(ClientLevel level, double x, double y, double z, Fluid type, TextureAtlasSprite sprite) {
        super(level, x, y, z, sprite);
        this.setSize(0.01F, 0.01F);
        this.gravity = 0.06F;
        this.type = type;
    }

    protected Fluid getType() {
        return this.type;
    }

    @Override
    public SingleQuadParticle.Layer getLayer() {
        return SingleQuadParticle.Layer.OPAQUE;
    }

    @Override
    public int getLightColor(float partialTick) {
        return this.isGlowing ? 240 : super.getLightColor(partialTick);
    }

    @Override
    public void tick() {
        this.xo = this.x;
        this.yo = this.y;
        this.zo = this.z;
        this.preMoveUpdate();
        if (!this.removed) {
            this.yd = this.yd - this.gravity;
            this.move(this.xd, this.yd, this.zd);
            this.postMoveUpdate();
            if (!this.removed) {
                this.xd *= 0.98F;
                this.yd *= 0.98F;
                this.zd *= 0.98F;
                if (this.type != Fluids.EMPTY) {
                    BlockPos blockpos = BlockPos.containing(this.x, this.y, this.z);
                    FluidState fluidstate = this.level.getFluidState(blockpos);
                    if (fluidstate.getType() == this.type && this.y < blockpos.getY() + fluidstate.getHeight(this.level, blockpos)) {
                        this.remove();
                    }
                }
            }
        }
    }

    protected void preMoveUpdate() {
        if (this.lifetime-- <= 0) {
            this.remove();
        }
    }

    protected void postMoveUpdate() {
    }

    @OnlyIn(Dist.CLIENT)
    static class CoolingDripHangParticle extends DripParticle.DripHangParticle {
        public CoolingDripHangParticle(
            ClientLevel p_106068_,
            double p_106069_,
            double p_106070_,
            double p_106071_,
            Fluid p_106072_,
            ParticleOptions p_106073_,
            TextureAtlasSprite p_446665_
        ) {
            super(p_106068_, p_106069_, p_106070_, p_106071_, p_106072_, p_106073_, p_446665_);
        }

        @Override
        protected void preMoveUpdate() {
            this.rCol = 1.0F;
            this.gCol = 16.0F / (40 - this.lifetime + 16);
            this.bCol = 4.0F / (40 - this.lifetime + 8);
            super.preMoveUpdate();
        }
    }

    @OnlyIn(Dist.CLIENT)
    static class DripHangParticle extends DripParticle {
        private final ParticleOptions fallingParticle;

        public DripHangParticle(
            ClientLevel level,
            double x,
            double y,
            double z,
            Fluid type,
            ParticleOptions fallingParticle,
            TextureAtlasSprite sprite
        ) {
            super(level, x, y, z, type, sprite);
            this.fallingParticle = fallingParticle;
            this.gravity *= 0.02F;
            this.lifetime = 40;
        }

        @Override
        protected void preMoveUpdate() {
            if (this.lifetime-- <= 0) {
                this.remove();
                this.level.addParticle(this.fallingParticle, this.x, this.y, this.z, this.xd, this.yd, this.zd);
            }
        }

        @Override
        protected void postMoveUpdate() {
            this.xd *= 0.02;
            this.yd *= 0.02;
            this.zd *= 0.02;
        }
    }

    @OnlyIn(Dist.CLIENT)
    static class DripLandParticle extends DripParticle {
        public DripLandParticle(ClientLevel p_106102_, double p_106103_, double p_106104_, double p_106105_, Fluid p_106106_, TextureAtlasSprite p_445778_) {
            super(p_106102_, p_106103_, p_106104_, p_106105_, p_106106_, p_445778_);
            this.lifetime = (int)(16.0 / (this.random.nextFloat() * 0.8 + 0.2));
        }
    }

    @OnlyIn(Dist.CLIENT)
    static class DripstoneFallAndLandParticle extends DripParticle.FallAndLandParticle {
        public DripstoneFallAndLandParticle(
            ClientLevel p_171930_,
            double p_171931_,
            double p_171932_,
            double p_171933_,
            Fluid p_171934_,
            ParticleOptions p_171935_,
            TextureAtlasSprite p_445845_
        ) {
            super(p_171930_, p_171931_, p_171932_, p_171933_, p_171934_, p_171935_, p_445845_);
        }

        @Override
        protected void postMoveUpdate() {
            if (this.onGround) {
                this.remove();
                this.level.addParticle(this.landParticle, this.x, this.y, this.z, 0.0, 0.0, 0.0);
                SoundEvent soundevent = this.getType() == Fluids.LAVA ? SoundEvents.POINTED_DRIPSTONE_DRIP_LAVA : SoundEvents.POINTED_DRIPSTONE_DRIP_WATER;
                float f = Mth.randomBetween(this.random, 0.3F, 1.0F);
                this.level.playLocalSound(this.x, this.y, this.z, soundevent, SoundSource.BLOCKS, f, 1.0F, false);
            }
        }
    }

    @OnlyIn(Dist.CLIENT)
    public static class DripstoneLavaFallProvider implements ParticleProvider<SimpleParticleType> {
        private final SpriteSet sprite;

        public DripstoneLavaFallProvider(SpriteSet sprite) {
            this.sprite = sprite;
        }

        public Particle createParticle(
            SimpleParticleType p_446879_,
            ClientLevel p_446669_,
            double p_446808_,
            double p_446047_,
            double p_446455_,
            double p_447310_,
            double p_445791_,
            double p_446677_,
            RandomSource p_445879_
        ) {
            DripParticle dripparticle = new DripParticle.DripstoneFallAndLandParticle(
                p_446669_, p_446808_, p_446047_, p_446455_, Fluids.LAVA, ParticleTypes.LANDING_LAVA, this.sprite.get(p_445879_)
            );
            dripparticle.setColor(1.0F, 0.2857143F, 0.083333336F);
            return dripparticle;
        }
    }

    @OnlyIn(Dist.CLIENT)
    public static class DripstoneLavaHangProvider implements ParticleProvider<SimpleParticleType> {
        private final SpriteSet sprite;

        public DripstoneLavaHangProvider(SpriteSet sprite) {
            this.sprite = sprite;
        }

        public Particle createParticle(
            SimpleParticleType p_445709_,
            ClientLevel p_446208_,
            double p_445887_,
            double p_445964_,
            double p_446933_,
            double p_446570_,
            double p_446264_,
            double p_446900_,
            RandomSource p_446918_
        ) {
            return new DripParticle.CoolingDripHangParticle(
                p_446208_, p_445887_, p_445964_, p_446933_, Fluids.LAVA, ParticleTypes.FALLING_DRIPSTONE_LAVA, this.sprite.get(p_446918_)
            );
        }
    }

    @OnlyIn(Dist.CLIENT)
    public static class DripstoneWaterFallProvider implements ParticleProvider<SimpleParticleType> {
        private final SpriteSet sprite;

        public DripstoneWaterFallProvider(SpriteSet sprite) {
            this.sprite = sprite;
        }

        public Particle createParticle(
            SimpleParticleType p_446634_,
            ClientLevel p_445783_,
            double p_445429_,
            double p_445927_,
            double p_447053_,
            double p_447318_,
            double p_446004_,
            double p_447094_,
            RandomSource p_445882_
        ) {
            DripParticle dripparticle = new DripParticle.DripstoneFallAndLandParticle(
                p_445783_, p_445429_, p_445927_, p_447053_, Fluids.WATER, ParticleTypes.SPLASH, this.sprite.get(p_445882_)
            );
            dripparticle.setColor(0.2F, 0.3F, 1.0F);
            return dripparticle;
        }
    }

    @OnlyIn(Dist.CLIENT)
    public static class DripstoneWaterHangProvider implements ParticleProvider<SimpleParticleType> {
        private final SpriteSet sprite;

        public DripstoneWaterHangProvider(SpriteSet sprite) {
            this.sprite = sprite;
        }

        public Particle createParticle(
            SimpleParticleType p_447260_,
            ClientLevel p_445559_,
            double p_446886_,
            double p_446756_,
            double p_446907_,
            double p_446080_,
            double p_447350_,
            double p_446292_,
            RandomSource p_446052_
        ) {
            DripParticle dripparticle = new DripParticle.DripHangParticle(
                p_445559_, p_446886_, p_446756_, p_446907_, Fluids.WATER, ParticleTypes.FALLING_DRIPSTONE_WATER, this.sprite.get(p_446052_)
            );
            dripparticle.setColor(0.2F, 0.3F, 1.0F);
            return dripparticle;
        }
    }

    @OnlyIn(Dist.CLIENT)
    static class FallAndLandParticle extends DripParticle.FallingParticle {
        protected final ParticleOptions landParticle;

        public FallAndLandParticle(
            ClientLevel level,
            double x,
            double y,
            double z,
            Fluid type,
            ParticleOptions landParticle,
            TextureAtlasSprite sprite
        ) {
            super(level, x, y, z, type, sprite);
            this.lifetime = (int)(64.0 / (this.random.nextFloat() * 0.8 + 0.2));
            this.landParticle = landParticle;
        }

        @Override
        protected void postMoveUpdate() {
            if (this.onGround) {
                this.remove();
                this.level.addParticle(this.landParticle, this.x, this.y, this.z, 0.0, 0.0, 0.0);
            }
        }
    }

    @OnlyIn(Dist.CLIENT)
    static class FallingParticle extends DripParticle {
        public FallingParticle(ClientLevel p_106132_, double p_106133_, double p_106134_, double p_106135_, Fluid p_106136_, TextureAtlasSprite p_445872_) {
            super(p_106132_, p_106133_, p_106134_, p_106135_, p_106136_, p_445872_);
        }

        @Override
        protected void postMoveUpdate() {
            if (this.onGround) {
                this.remove();
            }
        }
    }

    @OnlyIn(Dist.CLIENT)
    static class HoneyFallAndLandParticle extends DripParticle.FallAndLandParticle {
        public HoneyFallAndLandParticle(
            ClientLevel p_106146_,
            double p_106147_,
            double p_106148_,
            double p_106149_,
            Fluid p_106150_,
            ParticleOptions p_106151_,
            TextureAtlasSprite p_447239_
        ) {
            super(p_106146_, p_106147_, p_106148_, p_106149_, p_106150_, p_106151_, p_447239_);
        }

        @Override
        protected void postMoveUpdate() {
            if (this.onGround) {
                this.remove();
                this.level.addParticle(this.landParticle, this.x, this.y, this.z, 0.0, 0.0, 0.0);
                float f = Mth.randomBetween(this.random, 0.3F, 1.0F);
                this.level.playLocalSound(this.x, this.y, this.z, SoundEvents.BEEHIVE_DRIP, SoundSource.BLOCKS, f, 1.0F, false);
            }
        }
    }

    @OnlyIn(Dist.CLIENT)
    public static class HoneyFallProvider implements ParticleProvider<SimpleParticleType> {
        private final SpriteSet sprite;

        public HoneyFallProvider(SpriteSet sprite) {
            this.sprite = sprite;
        }

        public Particle createParticle(
            SimpleParticleType p_447072_,
            ClientLevel p_446549_,
            double p_445371_,
            double p_447004_,
            double p_446367_,
            double p_447326_,
            double p_446137_,
            double p_445818_,
            RandomSource p_445913_
        ) {
            DripParticle dripparticle = new DripParticle.HoneyFallAndLandParticle(
                p_446549_, p_445371_, p_447004_, p_446367_, Fluids.EMPTY, ParticleTypes.LANDING_HONEY, this.sprite.get(p_445913_)
            );
            dripparticle.gravity = 0.01F;
            dripparticle.setColor(0.582F, 0.448F, 0.082F);
            return dripparticle;
        }
    }

    @OnlyIn(Dist.CLIENT)
    public static class HoneyHangProvider implements ParticleProvider<SimpleParticleType> {
        private final SpriteSet sprite;

        public HoneyHangProvider(SpriteSet sprite) {
            this.sprite = sprite;
        }

        public Particle createParticle(
            SimpleParticleType p_446140_,
            ClientLevel p_445984_,
            double p_445516_,
            double p_446515_,
            double p_446266_,
            double p_446001_,
            double p_445763_,
            double p_446176_,
            RandomSource p_446912_
        ) {
            DripParticle.DripHangParticle dripparticle$driphangparticle = new DripParticle.DripHangParticle(
                p_445984_, p_445516_, p_446515_, p_446266_, Fluids.EMPTY, ParticleTypes.FALLING_HONEY, this.sprite.get(p_446912_)
            );
            dripparticle$driphangparticle.gravity *= 0.01F;
            dripparticle$driphangparticle.lifetime = 100;
            dripparticle$driphangparticle.setColor(0.622F, 0.508F, 0.082F);
            return dripparticle$driphangparticle;
        }
    }

    @OnlyIn(Dist.CLIENT)
    public static class HoneyLandProvider implements ParticleProvider<SimpleParticleType> {
        private final SpriteSet sprite;

        public HoneyLandProvider(SpriteSet sprite) {
            this.sprite = sprite;
        }

        public Particle createParticle(
            SimpleParticleType p_446885_,
            ClientLevel p_445372_,
            double p_445519_,
            double p_445520_,
            double p_446956_,
            double p_447152_,
            double p_446680_,
            double p_446443_,
            RandomSource p_445675_
        ) {
            DripParticle dripparticle = new DripParticle.DripLandParticle(p_445372_, p_445519_, p_445520_, p_446956_, Fluids.EMPTY, this.sprite.get(p_445675_));
            dripparticle.lifetime = (int)(128.0 / (p_445675_.nextFloat() * 0.8 + 0.2));
            dripparticle.setColor(0.522F, 0.408F, 0.082F);
            return dripparticle;
        }
    }

    @OnlyIn(Dist.CLIENT)
    public static class LavaFallProvider implements ParticleProvider<SimpleParticleType> {
        private final SpriteSet sprite;

        public LavaFallProvider(SpriteSet sprite) {
            this.sprite = sprite;
        }

        public Particle createParticle(
            SimpleParticleType p_446635_,
            ClientLevel p_446212_,
            double p_446107_,
            double p_446362_,
            double p_446971_,
            double p_446591_,
            double p_445937_,
            double p_446202_,
            RandomSource p_446546_
        ) {
            DripParticle dripparticle = new DripParticle.FallAndLandParticle(
                p_446212_, p_446107_, p_446362_, p_446971_, Fluids.LAVA, ParticleTypes.LANDING_LAVA, this.sprite.get(p_446546_)
            );
            dripparticle.setColor(1.0F, 0.2857143F, 0.083333336F);
            return dripparticle;
        }
    }

    @OnlyIn(Dist.CLIENT)
    public static class LavaHangProvider implements ParticleProvider<SimpleParticleType> {
        private final SpriteSet sprite;

        public LavaHangProvider(SpriteSet sprite) {
            this.sprite = sprite;
        }

        public Particle createParticle(
            SimpleParticleType p_445949_,
            ClientLevel p_446804_,
            double p_447071_,
            double p_446293_,
            double p_445418_,
            double p_447298_,
            double p_446616_,
            double p_446314_,
            RandomSource p_447085_
        ) {
            return new DripParticle.CoolingDripHangParticle(
                p_446804_, p_447071_, p_446293_, p_445418_, Fluids.LAVA, ParticleTypes.FALLING_LAVA, this.sprite.get(p_447085_)
            );
        }
    }

    @OnlyIn(Dist.CLIENT)
    public static class LavaLandProvider implements ParticleProvider<SimpleParticleType> {
        private final SpriteSet sprite;

        public LavaLandProvider(SpriteSet sprite) {
            this.sprite = sprite;
        }

        public Particle createParticle(
            SimpleParticleType p_445743_,
            ClientLevel p_445706_,
            double p_447328_,
            double p_447062_,
            double p_445701_,
            double p_445863_,
            double p_447197_,
            double p_445407_,
            RandomSource p_445677_
        ) {
            DripParticle dripparticle = new DripParticle.DripLandParticle(p_445706_, p_447328_, p_447062_, p_445701_, Fluids.LAVA, this.sprite.get(p_445677_));
            dripparticle.setColor(1.0F, 0.2857143F, 0.083333336F);
            return dripparticle;
        }
    }

    @OnlyIn(Dist.CLIENT)
    public static class NectarFallProvider implements ParticleProvider<SimpleParticleType> {
        private final SpriteSet sprite;

        public NectarFallProvider(SpriteSet sprite) {
            this.sprite = sprite;
        }

        public Particle createParticle(
            SimpleParticleType p_446123_,
            ClientLevel p_445481_,
            double p_446513_,
            double p_447158_,
            double p_445549_,
            double p_446204_,
            double p_446822_,
            double p_446414_,
            RandomSource p_446645_
        ) {
            DripParticle dripparticle = new DripParticle.FallingParticle(p_445481_, p_446513_, p_447158_, p_445549_, Fluids.EMPTY, this.sprite.get(p_446645_));
            dripparticle.lifetime = (int)(16.0 / (p_446645_.nextFloat() * 0.8 + 0.2));
            dripparticle.gravity = 0.007F;
            dripparticle.setColor(0.92F, 0.782F, 0.72F);
            return dripparticle;
        }
    }

    @OnlyIn(Dist.CLIENT)
    public static class ObsidianTearFallProvider implements ParticleProvider<SimpleParticleType> {
        private final SpriteSet sprite;

        public ObsidianTearFallProvider(SpriteSet sprite) {
            this.sprite = sprite;
        }

        public Particle createParticle(
            SimpleParticleType p_447006_,
            ClientLevel p_447317_,
            double p_445444_,
            double p_446174_,
            double p_447370_,
            double p_447272_,
            double p_447307_,
            double p_445692_,
            RandomSource p_445965_
        ) {
            DripParticle dripparticle = new DripParticle.FallAndLandParticle(
                p_447317_, p_445444_, p_446174_, p_447370_, Fluids.EMPTY, ParticleTypes.LANDING_OBSIDIAN_TEAR, this.sprite.get(p_445965_)
            );
            dripparticle.isGlowing = true;
            dripparticle.gravity = 0.01F;
            dripparticle.setColor(0.51171875F, 0.03125F, 0.890625F);
            return dripparticle;
        }
    }

    @OnlyIn(Dist.CLIENT)
    public static class ObsidianTearHangProvider implements ParticleProvider<SimpleParticleType> {
        private final SpriteSet sprite;

        public ObsidianTearHangProvider(SpriteSet sprite) {
            this.sprite = sprite;
        }

        public Particle createParticle(
            SimpleParticleType p_445392_,
            ClientLevel p_446483_,
            double p_445858_,
            double p_447330_,
            double p_445440_,
            double p_446160_,
            double p_447261_,
            double p_445419_,
            RandomSource p_446426_
        ) {
            DripParticle.DripHangParticle dripparticle$driphangparticle = new DripParticle.DripHangParticle(
                p_446483_, p_445858_, p_447330_, p_445440_, Fluids.EMPTY, ParticleTypes.FALLING_OBSIDIAN_TEAR, this.sprite.get(p_446426_)
            );
            dripparticle$driphangparticle.isGlowing = true;
            dripparticle$driphangparticle.gravity *= 0.01F;
            dripparticle$driphangparticle.lifetime = 100;
            dripparticle$driphangparticle.setColor(0.51171875F, 0.03125F, 0.890625F);
            return dripparticle$driphangparticle;
        }
    }

    @OnlyIn(Dist.CLIENT)
    public static class ObsidianTearLandProvider implements ParticleProvider<SimpleParticleType> {
        private final SpriteSet sprite;

        public ObsidianTearLandProvider(SpriteSet sprite) {
            this.sprite = sprite;
        }

        public Particle createParticle(
            SimpleParticleType p_446596_,
            ClientLevel p_447369_,
            double p_445603_,
            double p_446999_,
            double p_445431_,
            double p_446059_,
            double p_446566_,
            double p_446749_,
            RandomSource p_445454_
        ) {
            DripParticle dripparticle = new DripParticle.DripLandParticle(p_447369_, p_445603_, p_446999_, p_445431_, Fluids.EMPTY, this.sprite.get(p_445454_));
            dripparticle.isGlowing = true;
            dripparticle.lifetime = (int)(28.0 / (p_445454_.nextFloat() * 0.8 + 0.2));
            dripparticle.setColor(0.51171875F, 0.03125F, 0.890625F);
            return dripparticle;
        }
    }

    @OnlyIn(Dist.CLIENT)
    public static class SporeBlossomFallProvider implements ParticleProvider<SimpleParticleType> {
        private final SpriteSet sprite;

        public SporeBlossomFallProvider(SpriteSet sprite) {
            this.sprite = sprite;
        }

        public Particle createParticle(
            SimpleParticleType p_446373_,
            ClientLevel p_446049_,
            double p_446979_,
            double p_445529_,
            double p_447321_,
            double p_446085_,
            double p_446628_,
            double p_446038_,
            RandomSource p_445836_
        ) {
            DripParticle dripparticle = new DripParticle.FallingParticle(p_446049_, p_446979_, p_445529_, p_447321_, Fluids.EMPTY, this.sprite.get(p_445836_));
            dripparticle.lifetime = (int)(64.0F / Mth.randomBetween(dripparticle.random, 0.1F, 0.9F));
            dripparticle.gravity = 0.005F;
            dripparticle.setColor(0.32F, 0.5F, 0.22F);
            return dripparticle;
        }
    }

    @OnlyIn(Dist.CLIENT)
    public static class WaterFallProvider implements ParticleProvider<SimpleParticleType> {
        private final SpriteSet sprite;

        public WaterFallProvider(SpriteSet sprite) {
            this.sprite = sprite;
        }

        public Particle createParticle(
            SimpleParticleType p_446339_,
            ClientLevel p_445676_,
            double p_447001_,
            double p_445841_,
            double p_446311_,
            double p_445884_,
            double p_445543_,
            double p_447168_,
            RandomSource p_445793_
        ) {
            DripParticle dripparticle = new DripParticle.FallAndLandParticle(
                p_445676_, p_447001_, p_445841_, p_446311_, Fluids.WATER, ParticleTypes.SPLASH, this.sprite.get(p_445793_)
            );
            dripparticle.setColor(0.2F, 0.3F, 1.0F);
            return dripparticle;
        }
    }

    @OnlyIn(Dist.CLIENT)
    public static class WaterHangProvider implements ParticleProvider<SimpleParticleType> {
        private final SpriteSet sprite;

        public WaterHangProvider(SpriteSet sprite) {
            this.sprite = sprite;
        }

        public Particle createParticle(
            SimpleParticleType p_446379_,
            ClientLevel p_445807_,
            double p_445523_,
            double p_446251_,
            double p_445509_,
            double p_445757_,
            double p_445507_,
            double p_446854_,
            RandomSource p_445380_
        ) {
            DripParticle dripparticle = new DripParticle.DripHangParticle(
                p_445807_, p_445523_, p_446251_, p_445509_, Fluids.WATER, ParticleTypes.FALLING_WATER, this.sprite.get(p_445380_)
            );
            dripparticle.setColor(0.2F, 0.3F, 1.0F);
            return dripparticle;
        }
    }
}
