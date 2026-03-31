package net.minecraft.world.entity.ai.behavior;

import com.google.common.collect.ImmutableMap;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ai.Brain;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.MemoryStatus;
import net.minecraft.world.entity.ai.memory.WalkTarget;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.entity.player.Player;

public class LookAndFollowTradingPlayerSink extends Behavior<Villager> {
    private final float speedModifier;

    public LookAndFollowTradingPlayerSink(float speedModifier) {
        super(ImmutableMap.of(MemoryModuleType.WALK_TARGET, MemoryStatus.REGISTERED, MemoryModuleType.LOOK_TARGET, MemoryStatus.REGISTERED), Integer.MAX_VALUE);
        this.speedModifier = speedModifier;
    }

    protected boolean checkExtraStartConditions(ServerLevel p_23445_, Villager p_479064_) {
        Player player = p_479064_.getTradingPlayer();
        return p_479064_.isAlive() && player != null && !p_479064_.isInWater() && !p_479064_.hurtMarked && p_479064_.distanceToSqr(player) <= 16.0;
    }

    protected boolean canStillUse(ServerLevel p_23448_, Villager p_478246_, long p_23450_) {
        return this.checkExtraStartConditions(p_23448_, p_478246_);
    }

    protected void start(ServerLevel p_23458_, Villager p_481858_, long p_23460_) {
        this.followPlayer(p_481858_);
    }

    protected void stop(ServerLevel p_23466_, Villager p_482172_, long p_23468_) {
        Brain<?> brain = p_482172_.getBrain();
        brain.eraseMemory(MemoryModuleType.WALK_TARGET);
        brain.eraseMemory(MemoryModuleType.LOOK_TARGET);
    }

    protected void tick(ServerLevel p_23474_, Villager p_478606_, long p_23476_) {
        this.followPlayer(p_478606_);
    }

    @Override
    protected boolean timedOut(long gameTime) {
        return false;
    }

    private void followPlayer(Villager villager) {
        Brain<?> brain = villager.getBrain();
        brain.setMemory(MemoryModuleType.WALK_TARGET, new WalkTarget(new EntityTracker(villager.getTradingPlayer(), false), this.speedModifier, 2));
        brain.setMemory(MemoryModuleType.LOOK_TARGET, new EntityTracker(villager.getTradingPlayer(), true));
    }
}
