package net.minecraft.world.entity.ai.behavior;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableMap.Builder;
import java.util.Optional;
import java.util.function.Function;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.util.Util;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.Brain;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.MemoryStatus;
import net.minecraft.world.entity.ai.memory.WalkTarget;
import net.minecraft.world.entity.player.Player;

public class FollowTemptation extends Behavior<PathfinderMob> {
    public static final int TEMPTATION_COOLDOWN = 100;
    public static final double DEFAULT_CLOSE_ENOUGH_DIST = 2.5;
    public static final double BACKED_UP_CLOSE_ENOUGH_DIST = 3.5;
    private final Function<LivingEntity, Float> speedModifier;
    private final Function<LivingEntity, Double> closeEnoughDistance;
    private final boolean lookInTheEyes;

    public FollowTemptation(Function<LivingEntity, Float> speedModifier) {
        this(speedModifier, p_288784_ -> 2.5);
    }

    public FollowTemptation(Function<LivingEntity, Float> speedModifier, Function<LivingEntity, Double> closeEnoughDistance) {
        this(speedModifier, closeEnoughDistance, false);
    }

    public FollowTemptation(Function<LivingEntity, Float> speedModifier, Function<LivingEntity, Double> closeEnoughDistance, boolean lookInTheEyes) {
        super(Util.make(() -> {
            Builder<MemoryModuleType<?>, MemoryStatus> builder = ImmutableMap.builder();
            builder.put(MemoryModuleType.LOOK_TARGET, MemoryStatus.REGISTERED);
            builder.put(MemoryModuleType.WALK_TARGET, MemoryStatus.REGISTERED);
            builder.put(MemoryModuleType.TEMPTATION_COOLDOWN_TICKS, MemoryStatus.VALUE_ABSENT);
            builder.put(MemoryModuleType.IS_TEMPTED, MemoryStatus.VALUE_ABSENT);
            builder.put(MemoryModuleType.TEMPTING_PLAYER, MemoryStatus.VALUE_PRESENT);
            builder.put(MemoryModuleType.BREED_TARGET, MemoryStatus.VALUE_ABSENT);
            builder.put(MemoryModuleType.IS_PANICKING, MemoryStatus.VALUE_ABSENT);
            return builder.build();
        }));
        this.speedModifier = speedModifier;
        this.closeEnoughDistance = closeEnoughDistance;
        this.lookInTheEyes = lookInTheEyes;
    }

    protected float getSpeedModifier(PathfinderMob pathfinder) {
        return this.speedModifier.apply(pathfinder);
    }

    private Optional<Player> getTemptingPlayer(PathfinderMob pathfinder) {
        return pathfinder.getBrain().getMemory(MemoryModuleType.TEMPTING_PLAYER);
    }

    @Override
    protected boolean timedOut(long p_147488_) {
        return false;
    }

    protected boolean canStillUse(ServerLevel p_147494_, PathfinderMob p_147495_, long p_147496_) {
        return this.getTemptingPlayer(p_147495_).isPresent()
            && !p_147495_.getBrain().hasMemoryValue(MemoryModuleType.BREED_TARGET)
            && !p_147495_.getBrain().hasMemoryValue(MemoryModuleType.IS_PANICKING);
    }

    protected void start(ServerLevel p_147505_, PathfinderMob p_147506_, long p_147507_) {
        p_147506_.getBrain().setMemory(MemoryModuleType.IS_TEMPTED, true);
    }

    protected void stop(ServerLevel p_147515_, PathfinderMob p_147516_, long p_147517_) {
        Brain<?> brain = p_147516_.getBrain();
        brain.setMemory(MemoryModuleType.TEMPTATION_COOLDOWN_TICKS, 100);
        brain.eraseMemory(MemoryModuleType.IS_TEMPTED);
        brain.eraseMemory(MemoryModuleType.WALK_TARGET);
        brain.eraseMemory(MemoryModuleType.LOOK_TARGET);
    }

    protected void tick(ServerLevel p_147523_, PathfinderMob p_147524_, long p_147525_) {
        Player player = this.getTemptingPlayer(p_147524_).get();
        Brain<?> brain = p_147524_.getBrain();
        brain.setMemory(MemoryModuleType.LOOK_TARGET, new EntityTracker(player, true));
        double d0 = this.closeEnoughDistance.apply(p_147524_);
        if (p_147524_.distanceToSqr(player) < Mth.square(d0)) {
            brain.eraseMemory(MemoryModuleType.WALK_TARGET);
        } else {
            brain.setMemory(
                MemoryModuleType.WALK_TARGET,
                new WalkTarget(new EntityTracker(player, this.lookInTheEyes, this.lookInTheEyes), this.getSpeedModifier(p_147524_), 2)
            );
        }
    }
}
