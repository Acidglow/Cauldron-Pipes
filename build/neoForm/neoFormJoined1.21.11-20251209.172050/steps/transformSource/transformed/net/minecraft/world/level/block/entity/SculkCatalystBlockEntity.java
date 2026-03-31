package net.minecraft.world.level.block.entity;

import com.google.common.annotations.VisibleForTesting;
import net.minecraft.Optionull;
import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.SculkCatalystBlock;
import net.minecraft.world.level.block.SculkSpreader;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.BlockPositionSource;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.gameevent.GameEventListener;
import net.minecraft.world.level.gameevent.PositionSource;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;

public class SculkCatalystBlockEntity extends BlockEntity implements GameEventListener.Provider<SculkCatalystBlockEntity.CatalystListener> {
    private final SculkCatalystBlockEntity.CatalystListener catalystListener;

    public SculkCatalystBlockEntity(BlockPos pos, BlockState blockState) {
        super(BlockEntityType.SCULK_CATALYST, pos, blockState);
        this.catalystListener = new SculkCatalystBlockEntity.CatalystListener(blockState, new BlockPositionSource(pos));
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, SculkCatalystBlockEntity sculkCatalyst) {
        sculkCatalyst.catalystListener.getSculkSpreader().updateCursors(level, pos, level.getRandom(), true);
    }

    @Override
    protected void loadAdditional(ValueInput p_421771_) {
        super.loadAdditional(p_421771_);
        this.catalystListener.sculkSpreader.load(p_421771_);
    }

    @Override
    protected void saveAdditional(ValueOutput p_422464_) {
        this.catalystListener.sculkSpreader.save(p_422464_);
        super.saveAdditional(p_422464_);
    }

    public SculkCatalystBlockEntity.CatalystListener getListener() {
        return this.catalystListener;
    }

    public static class CatalystListener implements GameEventListener {
        public static final int PULSE_TICKS = 8;
        final SculkSpreader sculkSpreader;
        private final BlockState blockState;
        private final PositionSource positionSource;

        public CatalystListener(BlockState blockState, PositionSource positionSource) {
            this.blockState = blockState;
            this.positionSource = positionSource;
            this.sculkSpreader = SculkSpreader.createLevelSpreader();
        }

        @Override
        public PositionSource getListenerSource() {
            return this.positionSource;
        }

        @Override
        public int getListenerRadius() {
            return 8;
        }

        @Override
        public GameEventListener.DeliveryMode getDeliveryMode() {
            return GameEventListener.DeliveryMode.BY_DISTANCE;
        }

        @Override
        public boolean handleGameEvent(ServerLevel p_283470_, Holder<GameEvent> p_316661_, GameEvent.Context p_283014_, Vec3 p_282350_) {
            if (p_316661_.is(GameEvent.ENTITY_DIE) && p_283014_.sourceEntity() instanceof LivingEntity livingentity) {
                if (!livingentity.wasExperienceConsumed()) {
                    DamageSource damagesource = livingentity.getLastDamageSource();
                    int i = livingentity.getExperienceReward(p_283470_, Optionull.map(damagesource, DamageSource::getEntity));
                    if (livingentity.shouldDropExperience() && i > 0) {
                        this.sculkSpreader.addCursors(BlockPos.containing(p_282350_.relative(Direction.UP, 0.5)), i);
                        this.tryAwardItSpreadsAdvancement(p_283470_, livingentity);
                    }

                    livingentity.skipDropExperience();
                    this.positionSource
                        .getPosition(p_283470_)
                        .ifPresent(p_432712_ -> this.bloom(p_283470_, BlockPos.containing(p_432712_), this.blockState, p_283470_.getRandom()));
                }

                return true;
            } else {
                return false;
            }
        }

        @VisibleForTesting
        public SculkSpreader getSculkSpreader() {
            return this.sculkSpreader;
        }

        private void bloom(ServerLevel level, BlockPos pos, BlockState state, RandomSource random) {
            level.setBlock(pos, state.setValue(SculkCatalystBlock.PULSE, true), 3);
            level.scheduleTick(pos, state.getBlock(), 8);
            level.sendParticles(ParticleTypes.SCULK_SOUL, pos.getX() + 0.5, pos.getY() + 1.15, pos.getZ() + 0.5, 2, 0.2, 0.0, 0.2, 0.0);
            level.playSound(null, pos, SoundEvents.SCULK_CATALYST_BLOOM, SoundSource.BLOCKS, 2.0F, 0.6F + random.nextFloat() * 0.4F);
        }

        private void tryAwardItSpreadsAdvancement(Level level, LivingEntity entity) {
            if (entity.getLastHurtByMob() instanceof ServerPlayer serverplayer) {
                DamageSource damagesource = entity.getLastDamageSource() == null
                    ? level.damageSources().playerAttack(serverplayer)
                    : entity.getLastDamageSource();
                CriteriaTriggers.KILL_MOB_NEAR_SCULK_CATALYST.trigger(serverplayer, entity, damagesource);
            }
        }
    }
}
