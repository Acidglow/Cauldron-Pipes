package net.minecraft.world.entity.ai.behavior;

import com.google.common.collect.ImmutableMap;
import it.unimi.dsi.fastutil.ints.IntList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.util.Util;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.entity.projectile.FireworkRocketEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.raid.Raid;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.FireworkExplosion;
import net.minecraft.world.item.component.Fireworks;
import org.jspecify.annotations.Nullable;

public class CelebrateVillagersSurvivedRaid extends Behavior<Villager> {
    private @Nullable Raid currentRaid;

    public CelebrateVillagersSurvivedRaid(int minDuration, int maxDuration) {
        super(ImmutableMap.of(), minDuration, maxDuration);
    }

    protected boolean checkExtraStartConditions(ServerLevel p_22690_, Villager p_478489_) {
        BlockPos blockpos = p_478489_.blockPosition();
        this.currentRaid = p_22690_.getRaidAt(blockpos);
        return this.currentRaid != null && this.currentRaid.isVictory() && MoveToSkySeeingSpot.hasNoBlocksAbove(p_22690_, p_478489_, blockpos);
    }

    protected boolean canStillUse(ServerLevel p_22693_, Villager p_481379_, long p_22695_) {
        return this.currentRaid != null && !this.currentRaid.isStopped();
    }

    protected void stop(ServerLevel p_22704_, Villager p_481789_, long p_22706_) {
        this.currentRaid = null;
        p_481789_.getBrain().updateActivityFromSchedule(p_22704_.environmentAttributes(), p_22704_.getGameTime(), p_481789_.position());
    }

    protected void tick(ServerLevel p_22712_, Villager p_481581_, long p_22714_) {
        RandomSource randomsource = p_481581_.getRandom();
        if (randomsource.nextInt(100) == 0) {
            p_481581_.playCelebrateSound();
        }

        if (randomsource.nextInt(200) == 0 && MoveToSkySeeingSpot.hasNoBlocksAbove(p_22712_, p_481581_, p_481581_.blockPosition())) {
            DyeColor dyecolor = Util.getRandom(DyeColor.values(), randomsource);
            int i = randomsource.nextInt(3);
            ItemStack itemstack = this.getFirework(dyecolor, i);
            Projectile.spawnProjectile(
                new FireworkRocketEntity(p_481581_.level(), p_481581_, p_481581_.getX(), p_481581_.getEyeY(), p_481581_.getZ(), itemstack), p_22712_, itemstack
            );
        }
    }

    private ItemStack getFirework(DyeColor color, int flightTime) {
        ItemStack itemstack = new ItemStack(Items.FIREWORK_ROCKET);
        itemstack.set(
            DataComponents.FIREWORKS,
            new Fireworks(
                (byte)flightTime,
                List.of(new FireworkExplosion(FireworkExplosion.Shape.BURST, IntList.of(color.getFireworkColor()), IntList.of(), false, false))
            )
        );
        return itemstack;
    }
}
