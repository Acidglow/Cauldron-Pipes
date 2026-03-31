package net.minecraft.world.entity.ai.behavior;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import java.util.List;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.Brain;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.MemoryStatus;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.trading.MerchantOffer;
import org.jspecify.annotations.Nullable;

public class ShowTradesToPlayer extends Behavior<Villager> {
    private static final int MAX_LOOK_TIME = 900;
    private static final int STARTING_LOOK_TIME = 40;
    private @Nullable ItemStack playerItemStack;
    private final List<ItemStack> displayItems = Lists.newArrayList();
    private int cycleCounter;
    private int displayIndex;
    private int lookTime;

    public ShowTradesToPlayer(int minDuration, int maxDuration) {
        super(ImmutableMap.of(MemoryModuleType.INTERACTION_TARGET, MemoryStatus.VALUE_PRESENT), minDuration, maxDuration);
    }

    public boolean checkExtraStartConditions(ServerLevel p_24106_, Villager p_481544_) {
        Brain<?> brain = p_481544_.getBrain();
        if (brain.getMemory(MemoryModuleType.INTERACTION_TARGET).isEmpty()) {
            return false;
        } else {
            LivingEntity livingentity = brain.getMemory(MemoryModuleType.INTERACTION_TARGET).get();
            return livingentity.getType() == EntityType.PLAYER
                && p_481544_.isAlive()
                && livingentity.isAlive()
                && !p_481544_.isBaby()
                && p_481544_.distanceToSqr(livingentity) <= 17.0;
        }
    }

    public boolean canStillUse(ServerLevel p_24109_, Villager p_481859_, long p_24111_) {
        return this.checkExtraStartConditions(p_24109_, p_481859_)
            && this.lookTime > 0
            && p_481859_.getBrain().getMemory(MemoryModuleType.INTERACTION_TARGET).isPresent();
    }

    public void start(ServerLevel p_24124_, Villager p_481725_, long p_24126_) {
        super.start(p_24124_, p_481725_, p_24126_);
        this.lookAtTarget(p_481725_);
        this.cycleCounter = 0;
        this.displayIndex = 0;
        this.lookTime = 40;
    }

    public void tick(ServerLevel p_24134_, Villager p_479720_, long p_24136_) {
        LivingEntity livingentity = this.lookAtTarget(p_479720_);
        this.findItemsToDisplay(livingentity, p_479720_);
        if (!this.displayItems.isEmpty()) {
            this.displayCyclingItems(p_479720_);
        } else {
            clearHeldItem(p_479720_);
            this.lookTime = Math.min(this.lookTime, 40);
        }

        this.lookTime--;
    }

    public void stop(ServerLevel p_24144_, Villager p_478501_, long p_24146_) {
        super.stop(p_24144_, p_478501_, p_24146_);
        p_478501_.getBrain().eraseMemory(MemoryModuleType.INTERACTION_TARGET);
        clearHeldItem(p_478501_);
        this.playerItemStack = null;
    }

    private void findItemsToDisplay(LivingEntity target, Villager villager) {
        boolean flag = false;
        ItemStack itemstack = target.getMainHandItem();
        if (this.playerItemStack == null || !ItemStack.isSameItem(this.playerItemStack, itemstack)) {
            this.playerItemStack = itemstack;
            flag = true;
            this.displayItems.clear();
        }

        if (flag && !this.playerItemStack.isEmpty()) {
            this.updateDisplayItems(villager);
            if (!this.displayItems.isEmpty()) {
                this.lookTime = 900;
                this.displayFirstItem(villager);
            }
        }
    }

    private void displayFirstItem(Villager villager) {
        displayAsHeldItem(villager, this.displayItems.get(0));
    }

    private void updateDisplayItems(Villager villager) {
        for (MerchantOffer merchantoffer : villager.getOffers()) {
            if (!merchantoffer.isOutOfStock() && this.playerItemStackMatchesCostOfOffer(merchantoffer)) {
                this.displayItems.add(merchantoffer.assemble());
            }
        }
    }

    private boolean playerItemStackMatchesCostOfOffer(MerchantOffer offer) {
        return ItemStack.isSameItem(this.playerItemStack, offer.getCostA()) || ItemStack.isSameItem(this.playerItemStack, offer.getCostB());
    }

    private static void clearHeldItem(Villager villager) {
        villager.setItemSlot(EquipmentSlot.MAINHAND, ItemStack.EMPTY);
        villager.setDropChance(EquipmentSlot.MAINHAND, 0.085F);
    }

    private static void displayAsHeldItem(Villager villager, ItemStack item) {
        villager.setItemSlot(EquipmentSlot.MAINHAND, item);
        villager.setDropChance(EquipmentSlot.MAINHAND, 0.0F);
    }

    private LivingEntity lookAtTarget(Villager villager) {
        Brain<?> brain = villager.getBrain();
        LivingEntity livingentity = brain.getMemory(MemoryModuleType.INTERACTION_TARGET).get();
        brain.setMemory(MemoryModuleType.LOOK_TARGET, new EntityTracker(livingentity, true));
        return livingentity;
    }

    private void displayCyclingItems(Villager villager) {
        if (this.displayItems.size() >= 2 && ++this.cycleCounter >= 40) {
            this.displayIndex++;
            this.cycleCounter = 0;
            if (this.displayIndex > this.displayItems.size() - 1) {
                this.displayIndex = 0;
            }

            displayAsHeldItem(villager, this.displayItems.get(this.displayIndex));
        }
    }
}
