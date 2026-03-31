package net.minecraft.world.entity;

import java.util.Optional;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.gamerules.GameRules;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jspecify.annotations.Nullable;

public interface NeutralMob {
    String TAG_ANGER_END_TIME = "anger_end_time";
    String TAG_ANGRY_AT = "angry_at";
    long NO_ANGER_END_TIME = -1L;

    long getPersistentAngerEndTime();

    default void setTimeToRemainAngry(long timeToRemainAngry) {
        this.setPersistentAngerEndTime(this.level().getGameTime() + timeToRemainAngry);
    }

    void setPersistentAngerEndTime(long persistentAngerEndTime);

    @Nullable EntityReference<LivingEntity> getPersistentAngerTarget();

    void setPersistentAngerTarget(@Nullable EntityReference<LivingEntity> persistentAngerTarget);

    void startPersistentAngerTimer();

    Level level();

    default void addPersistentAngerSaveData(ValueOutput output) {
        output.putLong("anger_end_time", this.getPersistentAngerEndTime());
        output.storeNullable("angry_at", EntityReference.codec(), this.getPersistentAngerTarget());
    }

    default void readPersistentAngerSaveData(Level level, ValueInput input) {
        Optional<Long> optional = input.getLong("anger_end_time");
        if (optional.isPresent()) {
            this.setPersistentAngerEndTime(optional.get());
        } else {
            Optional<Integer> optional1 = input.getInt("AngerTime");
            if (optional1.isPresent()) {
                this.setTimeToRemainAngry(optional1.get().intValue());
            } else {
                this.setPersistentAngerEndTime(-1L);
            }
        }

        if (level instanceof ServerLevel) {
            this.setPersistentAngerTarget(EntityReference.read(input, "angry_at"));
            this.setTarget(EntityReference.getLivingEntity(this.getPersistentAngerTarget(), level));
        }
    }

    default void updatePersistentAnger(ServerLevel serverLevel, boolean updateAnger) {
        LivingEntity livingentity = this.getTarget();
        EntityReference<LivingEntity> entityreference = this.getPersistentAngerTarget();
        if (livingentity != null
            && livingentity.isDeadOrDying()
            && entityreference != null
            && entityreference.matches(livingentity)
            && livingentity instanceof Mob) {
            this.stopBeingAngry();
        } else {
            if (livingentity != null) {
                if (entityreference == null || !entityreference.matches(livingentity)) {
                    this.setPersistentAngerTarget(EntityReference.of(livingentity));
                }

                this.startPersistentAngerTimer();
            }

            if (entityreference != null && !this.isAngry() && (livingentity == null || !isValidPlayerTarget(livingentity) || !updateAnger)) {
                this.stopBeingAngry();
            }
        }
    }

    private static boolean isValidPlayerTarget(LivingEntity target) {
        return target instanceof Player player && !player.isCreative() && !player.isSpectator();
    }

    default boolean isAngryAt(LivingEntity entity, ServerLevel level) {
        if (!this.canAttack(entity)) {
            return false;
        } else if (isValidPlayerTarget(entity) && this.isAngryAtAllPlayers(level)) {
            return true;
        } else {
            EntityReference<LivingEntity> entityreference = this.getPersistentAngerTarget();
            return entityreference != null && entityreference.matches(entity);
        }
    }

    default boolean isAngryAtAllPlayers(ServerLevel level) {
        return level.getGameRules().get(GameRules.UNIVERSAL_ANGER) && this.isAngry() && this.getPersistentAngerTarget() == null;
    }

    default boolean isAngry() {
        long i = this.getPersistentAngerEndTime();
        if (i > 0L) {
            long j = i - this.level().getGameTime();
            return j > 0L;
        } else {
            return false;
        }
    }

    default void playerDied(ServerLevel level, Player player) {
        if (level.getGameRules().get(GameRules.FORGIVE_DEAD_PLAYERS)) {
            EntityReference<LivingEntity> entityreference = this.getPersistentAngerTarget();
            if (entityreference != null && entityreference.matches(player)) {
                this.stopBeingAngry();
            }
        }
    }

    default void forgetCurrentTargetAndRefreshUniversalAnger() {
        this.stopBeingAngry();
        this.startPersistentAngerTimer();
    }

    default void stopBeingAngry() {
        this.setLastHurtByMob(null);
        this.setPersistentAngerTarget(null);
        this.setTarget(null);
        this.setPersistentAngerEndTime(-1L);
    }

    @Nullable LivingEntity getLastHurtByMob();

    /**
     * Hint to AI tasks that we were attacked by the passed EntityLivingBase and should retaliate. Is not guaranteed to change our actual active target (for example if we are currently busy attacking someone else)
     */
    void setLastHurtByMob(@Nullable LivingEntity livingEntity);

    /**
     * Sets the active target the Task system uses for tracking
     */
    void setTarget(@Nullable LivingEntity livingEntity);

    boolean canAttack(LivingEntity entity);

    @Nullable LivingEntity getTarget();
}
