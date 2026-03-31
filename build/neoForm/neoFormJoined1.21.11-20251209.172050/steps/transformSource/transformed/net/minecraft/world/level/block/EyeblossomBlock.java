package net.minecraft.world.level.block;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.mojang.serialization.codecs.RecordCodecBuilder.Instance;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.particles.TrailParticleOption;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Difficulty;
import net.minecraft.world.attribute.EnvironmentAttributes;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.InsideBlockEffectApplier;
import net.minecraft.world.entity.animal.bee.Bee;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.Vec3;

public class EyeblossomBlock extends FlowerBlock {
    public static final MapCodec<EyeblossomBlock> CODEC = RecordCodecBuilder.mapCodec(
        p_432663_ -> p_432663_.group(Codec.BOOL.fieldOf("open").forGetter(p_383069_ -> p_383069_.type.open), propertiesCodec())
            .apply(p_432663_, EyeblossomBlock::new)
    );
    private static final int EYEBLOSSOM_XZ_RANGE = 3;
    private static final int EYEBLOSSOM_Y_RANGE = 2;
    private final EyeblossomBlock.Type type;

    @Override
    public MapCodec<? extends EyeblossomBlock> codec() {
        return CODEC;
    }

    public EyeblossomBlock(EyeblossomBlock.Type type, BlockBehaviour.Properties properties) {
        super(type.effect, type.effectDuration, properties);
        this.type = type;
    }

    public EyeblossomBlock(boolean open, BlockBehaviour.Properties properties) {
        super(EyeblossomBlock.Type.fromBoolean(open).effect, EyeblossomBlock.Type.fromBoolean(open).effectDuration, properties);
        this.type = EyeblossomBlock.Type.fromBoolean(open);
    }

    @Override
    public void animateTick(BlockState p_382850_, Level p_382933_, BlockPos p_382838_, RandomSource p_383190_) {
        if (this.type.emitSounds() && p_383190_.nextInt(700) == 0) {
            BlockState blockstate = p_382933_.getBlockState(p_382838_.below());
            if (blockstate.is(Blocks.PALE_MOSS_BLOCK)) {
                p_382933_.playLocalSound(
                    p_382838_.getX(), p_382838_.getY(), p_382838_.getZ(), SoundEvents.EYEBLOSSOM_IDLE, SoundSource.AMBIENT, 1.0F, 1.0F, false
                );
            }
        }
    }

    @Override
    protected void randomTick(BlockState p_382824_, ServerLevel p_382831_, BlockPos p_382957_, RandomSource p_382888_) {
        if (this.tryChangingState(p_382824_, p_382831_, p_382957_, p_382888_)) {
            p_382831_.playSound(null, p_382957_, this.type.transform().longSwitchSound, SoundSource.BLOCKS, 1.0F, 1.0F);
        }

        super.randomTick(p_382824_, p_382831_, p_382957_, p_382888_);
    }

    @Override
    protected void tick(BlockState p_382808_, ServerLevel p_383005_, BlockPos p_383211_, RandomSource p_383088_) {
        if (this.tryChangingState(p_382808_, p_383005_, p_383211_, p_383088_)) {
            p_383005_.playSound(null, p_383211_, this.type.transform().shortSwitchSound, SoundSource.BLOCKS, 1.0F, 1.0F);
        }

        super.tick(p_382808_, p_383005_, p_383211_, p_383088_);
    }

    private boolean tryChangingState(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        boolean flag = level.environmentAttributes().getValue(EnvironmentAttributes.EYEBLOSSOM_OPEN, pos).toBoolean(this.type.open);
        if (flag == this.type.open) {
            return false;
        } else {
            EyeblossomBlock.Type eyeblossomblock$type = this.type.transform();
            level.setBlock(pos, eyeblossomblock$type.state(), 3);
            level.gameEvent(GameEvent.BLOCK_CHANGE, pos, GameEvent.Context.of(state));
            eyeblossomblock$type.spawnTransformParticle(level, pos, random);
            BlockPos.betweenClosed(pos.offset(-3, -2, -3), pos.offset(3, 2, 3)).forEach(p_383198_ -> {
                BlockState blockstate = level.getBlockState(p_383198_);
                if (blockstate == state) {
                    double d0 = Math.sqrt(pos.distSqr(p_383198_));
                    int i = random.nextIntBetweenInclusive((int)(d0 * 5.0), (int)(d0 * 10.0));
                    level.scheduleTick(p_383198_, state.getBlock(), i);
                }
            });
            return true;
        }
    }

    @Override
    protected void entityInside(
        BlockState p_382817_, Level p_383060_, BlockPos p_383146_, Entity p_383054_, InsideBlockEffectApplier p_405247_, boolean p_451769_
    ) {
        if (!p_383060_.isClientSide()
            && p_383060_.getDifficulty() != Difficulty.PEACEFUL
            && p_383054_ instanceof Bee bee
            && Bee.attractsBees(p_382817_)
            && !bee.hasEffect(MobEffects.POISON)) {
            bee.addEffect(this.getBeeInteractionEffect());
        }
    }

    @Override
    public MobEffectInstance getBeeInteractionEffect() {
        return new MobEffectInstance(MobEffects.POISON, 25);
    }

    public static enum Type {
        OPEN(true, MobEffects.BLINDNESS, 11.0F, SoundEvents.EYEBLOSSOM_OPEN_LONG, SoundEvents.EYEBLOSSOM_OPEN, 16545810),
        CLOSED(false, MobEffects.NAUSEA, 7.0F, SoundEvents.EYEBLOSSOM_CLOSE_LONG, SoundEvents.EYEBLOSSOM_CLOSE, 6250335);

        final boolean open;
        final Holder<MobEffect> effect;
        final float effectDuration;
        final SoundEvent longSwitchSound;
        final SoundEvent shortSwitchSound;
        private final int particleColor;

        private Type(boolean open, Holder<MobEffect> effect, float effectDuration, SoundEvent longSwitchSound, SoundEvent shortSwitchSound, int particleColor) {
            this.open = open;
            this.effect = effect;
            this.effectDuration = effectDuration;
            this.longSwitchSound = longSwitchSound;
            this.shortSwitchSound = shortSwitchSound;
            this.particleColor = particleColor;
        }

        public Block block() {
            return this.open ? Blocks.OPEN_EYEBLOSSOM : Blocks.CLOSED_EYEBLOSSOM;
        }

        public BlockState state() {
            return this.block().defaultBlockState();
        }

        public EyeblossomBlock.Type transform() {
            return fromBoolean(!this.open);
        }

        public boolean emitSounds() {
            return this.open;
        }

        public static EyeblossomBlock.Type fromBoolean(boolean open) {
            return open ? OPEN : CLOSED;
        }

        public void spawnTransformParticle(ServerLevel level, BlockPos pos, RandomSource random) {
            Vec3 vec3 = pos.getCenter();
            double d0 = 0.5 + random.nextDouble();
            Vec3 vec31 = new Vec3(random.nextDouble() - 0.5, random.nextDouble() + 1.0, random.nextDouble() - 0.5);
            Vec3 vec32 = vec3.add(vec31.scale(d0));
            TrailParticleOption trailparticleoption = new TrailParticleOption(vec32, this.particleColor, (int)(20.0 * d0));
            level.sendParticles(trailparticleoption, vec3.x, vec3.y, vec3.z, 1, 0.0, 0.0, 0.0, 0.0);
        }

        public SoundEvent longSwitchSound() {
            return this.longSwitchSound;
        }
    }
}
