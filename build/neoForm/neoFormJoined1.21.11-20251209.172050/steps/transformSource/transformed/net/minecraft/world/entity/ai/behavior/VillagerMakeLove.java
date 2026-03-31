package net.minecraft.world.entity.ai.behavior;

import com.google.common.collect.ImmutableMap;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.Holder;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.Brain;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.MemoryStatus;
import net.minecraft.world.entity.ai.village.poi.PoiType;
import net.minecraft.world.entity.ai.village.poi.PoiTypes;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.level.pathfinder.Path;

public class VillagerMakeLove extends Behavior<Villager> {
    private long birthTimestamp;

    public VillagerMakeLove() {
        super(
            ImmutableMap.of(
                MemoryModuleType.BREED_TARGET, MemoryStatus.VALUE_PRESENT, MemoryModuleType.NEAREST_VISIBLE_LIVING_ENTITIES, MemoryStatus.VALUE_PRESENT
            ),
            350,
            350
        );
    }

    protected boolean checkExtraStartConditions(ServerLevel p_24623_, Villager p_482178_) {
        return this.isBreedingPossible(p_482178_);
    }

    protected boolean canStillUse(ServerLevel p_24626_, Villager p_481028_, long p_24628_) {
        return p_24628_ <= this.birthTimestamp && this.isBreedingPossible(p_481028_);
    }

    protected void start(ServerLevel p_24652_, Villager p_481220_, long p_24654_) {
        AgeableMob ageablemob = p_481220_.getBrain().getMemory(MemoryModuleType.BREED_TARGET).get();
        BehaviorUtils.lockGazeAndWalkToEachOther(p_481220_, ageablemob, 0.5F, 2);
        p_24652_.broadcastEntityEvent(ageablemob, (byte)18);
        p_24652_.broadcastEntityEvent(p_481220_, (byte)18);
        int i = 275 + p_481220_.getRandom().nextInt(50);
        this.birthTimestamp = p_24654_ + i;
    }

    protected void tick(ServerLevel p_24667_, Villager p_481591_, long p_24669_) {
        Villager villager = (Villager)p_481591_.getBrain().getMemory(MemoryModuleType.BREED_TARGET).get();
        if (!(p_481591_.distanceToSqr(villager) > 5.0)) {
            BehaviorUtils.lockGazeAndWalkToEachOther(p_481591_, villager, 0.5F, 2);
            if (p_24669_ >= this.birthTimestamp) {
                p_481591_.eatAndDigestFood();
                villager.eatAndDigestFood();
                this.tryToGiveBirth(p_24667_, p_481591_, villager);
            } else if (p_481591_.getRandom().nextInt(35) == 0) {
                p_24667_.broadcastEntityEvent(villager, (byte)12);
                p_24667_.broadcastEntityEvent(p_481591_, (byte)12);
            }
        }
    }

    private void tryToGiveBirth(ServerLevel level, Villager villager, Villager partner) {
        Optional<BlockPos> optional = this.takeVacantBed(level, villager);
        if (optional.isEmpty()) {
            level.broadcastEntityEvent(partner, (byte)13);
            level.broadcastEntityEvent(villager, (byte)13);
        } else {
            Optional<Villager> optional1 = this.breed(level, villager, partner);
            if (optional1.isPresent()) {
                this.giveBedToChild(level, optional1.get(), optional.get());
            } else {
                level.getPoiManager().release(optional.get());
                level.debugSynchronizers().updatePoi(optional.get());
            }
        }
    }

    protected void stop(ServerLevel p_24675_, Villager p_480694_, long p_24677_) {
        p_480694_.getBrain().eraseMemory(MemoryModuleType.BREED_TARGET);
    }

    private boolean isBreedingPossible(Villager villager) {
        Brain<Villager> brain = villager.getBrain();
        Optional<AgeableMob> optional = brain.getMemory(MemoryModuleType.BREED_TARGET).filter(p_423262_ -> p_423262_.getType() == EntityType.VILLAGER);
        return optional.isEmpty()
            ? false
            : BehaviorUtils.targetIsValid(brain, MemoryModuleType.BREED_TARGET, EntityType.VILLAGER) && villager.canBreed() && optional.get().canBreed();
    }

    private Optional<BlockPos> takeVacantBed(ServerLevel level, Villager villager) {
        return level.getPoiManager()
            .take(
                p_217509_ -> p_217509_.is(PoiTypes.HOME),
                (p_477852_, p_477853_) -> this.canReach(villager, p_477853_, p_477852_),
                villager.blockPosition(),
                48
            );
    }

    private boolean canReach(Villager villager, BlockPos pos, Holder<PoiType> poiType) {
        Path path = villager.getNavigation().createPath(pos, poiType.value().validRange());
        return path != null && path.canReach();
    }

    private Optional<Villager> breed(ServerLevel level, Villager p_villager, Villager partner) {
        Villager villager = p_villager.getBreedOffspring(level, partner);
        if (villager == null) {
            return Optional.empty();
        } else {
            p_villager.setAge(6000);
            partner.setAge(6000);
            villager.setAge(-24000);
            villager.snapTo(p_villager.getX(), p_villager.getY(), p_villager.getZ(), 0.0F, 0.0F);
            level.addFreshEntityWithPassengers(villager);
            // Neo: If villager is blocked from spawning (e.g., FinalizeSpawnEvent), then breed should be unsuccessful
            if (!villager.isAddedToLevel()) return Optional.empty();
            level.broadcastEntityEvent(villager, (byte)12);
            return Optional.of(villager);
        }
    }

    private void giveBedToChild(ServerLevel level, Villager child, BlockPos pos) {
        GlobalPos globalpos = GlobalPos.of(level.dimension(), pos);
        child.getBrain().setMemory(MemoryModuleType.HOME, globalpos);
    }
}
