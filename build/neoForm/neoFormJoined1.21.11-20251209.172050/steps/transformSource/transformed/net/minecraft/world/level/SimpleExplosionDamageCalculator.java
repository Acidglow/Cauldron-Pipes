package net.minecraft.world.level;

import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderSet;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;

public class SimpleExplosionDamageCalculator extends ExplosionDamageCalculator {
    private final boolean explodesBlocks;
    private final boolean damagesEntities;
    private final Optional<Float> knockbackMultiplier;
    private final Optional<HolderSet<Block>> immuneBlocks;

    public SimpleExplosionDamageCalculator(boolean explodesBlocks, boolean damagesEntities, Optional<Float> knockbackMultiplier, Optional<HolderSet<Block>> immuneBlocks) {
        this.explodesBlocks = explodesBlocks;
        this.damagesEntities = damagesEntities;
        this.knockbackMultiplier = knockbackMultiplier;
        this.immuneBlocks = immuneBlocks;
    }

    @Override
    public Optional<Float> getBlockExplosionResistance(
        Explosion p_346109_, BlockGetter p_345381_, BlockPos p_344921_, BlockState p_346239_, FluidState p_345105_
    ) {
        if (this.immuneBlocks.isPresent()) {
            return p_346239_.is(this.immuneBlocks.get()) ? Optional.of(3600000.0F) : Optional.empty();
        } else {
            return super.getBlockExplosionResistance(p_346109_, p_345381_, p_344921_, p_346239_, p_345105_);
        }
    }

    @Override
    public boolean shouldBlockExplode(Explosion p_345994_, BlockGetter p_345042_, BlockPos p_345057_, BlockState p_345932_, float p_345776_) {
        return this.explodesBlocks;
    }

    @Override
    public boolean shouldDamageEntity(Explosion p_346248_, Entity p_344983_) {
        return this.damagesEntities;
    }

    @Override
    public float getKnockbackMultiplier(Entity p_345651_) {
        boolean flag = p_345651_ instanceof Player player && player.getAbilities().flying;
        return flag ? 0.0F : this.knockbackMultiplier.orElseGet(() -> super.getKnockbackMultiplier(p_345651_));
    }
}
