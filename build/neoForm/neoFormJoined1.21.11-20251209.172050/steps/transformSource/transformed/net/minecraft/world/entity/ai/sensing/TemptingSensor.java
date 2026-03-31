package net.minecraft.world.entity.ai.sensing;

import com.google.common.collect.ImmutableSet;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.function.BiPredicate;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntitySelector;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.Brain;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.targeting.TargetingConditions;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

public class TemptingSensor extends Sensor<PathfinderMob> {
    private static final TargetingConditions TEMPT_TARGETING = TargetingConditions.forNonCombat().ignoreLineOfSight();
    private final BiPredicate<PathfinderMob, ItemStack> temptations;

    public TemptingSensor(Predicate<ItemStack> temptations) {
        this((p_460532_, p_460533_) -> temptations.test(p_460533_));
    }

    public static TemptingSensor forAnimal() {
        return new TemptingSensor((p_460529_, p_460530_) -> p_460529_ instanceof Animal animal ? animal.isFood(p_460530_) : false);
    }

    private TemptingSensor(BiPredicate<PathfinderMob, ItemStack> temptations) {
        this.temptations = temptations;
    }

    protected void doTick(ServerLevel p_148331_, PathfinderMob p_148332_) {
        Brain<?> brain = p_148332_.getBrain();
        TargetingConditions targetingconditions = TEMPT_TARGETING.copy().range((float)p_148332_.getAttributeValue(Attributes.TEMPT_RANGE));
        List<Player> list = p_148331_.players()
            .stream()
            .filter(EntitySelector.NO_SPECTATORS)
            .filter(p_375769_ -> targetingconditions.test(p_148331_, p_148332_, p_375769_))
            .filter(p_460535_ -> this.playerHoldingTemptation(p_148332_, p_460535_))
            .filter(p_423271_ -> !p_148332_.hasPassenger(p_423271_))
            .sorted(Comparator.comparingDouble(p_148332_::distanceToSqr))
            .collect(Collectors.toList());
        if (!list.isEmpty()) {
            Player player = list.get(0);
            brain.setMemory(MemoryModuleType.TEMPTING_PLAYER, player);
        } else {
            brain.eraseMemory(MemoryModuleType.TEMPTING_PLAYER);
        }
    }

    private boolean playerHoldingTemptation(PathfinderMob mob, Player player) {
        return this.isTemptation(mob, player.getMainHandItem()) || this.isTemptation(mob, player.getOffhandItem());
    }

    private boolean isTemptation(PathfinderMob mob, ItemStack item) {
        return this.temptations.test(mob, item);
    }

    @Override
    public Set<MemoryModuleType<?>> requires() {
        return ImmutableSet.of(MemoryModuleType.TEMPTING_PLAYER);
    }
}
