package net.minecraft.world.entity.projectile.throwableitemprojectile;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.AreaEffectCloud;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.HitResult;

public class ThrownLingeringPotion extends AbstractThrownPotion {
    public ThrownLingeringPotion(EntityType<? extends ThrownLingeringPotion> p_480890_, Level p_478046_) {
        super(p_480890_, p_478046_);
    }

    public ThrownLingeringPotion(Level level, LivingEntity owner, ItemStack item) {
        super(EntityType.LINGERING_POTION, level, owner, item);
    }

    public ThrownLingeringPotion(Level level, double x, double y, double z, ItemStack item) {
        super(EntityType.LINGERING_POTION, level, x, y, z, item);
    }

    @Override
    protected Item getDefaultItem() {
        return Items.LINGERING_POTION;
    }

    @Override
    public void onHitAsPotion(ServerLevel p_479772_, ItemStack p_481164_, HitResult p_478476_) {
        AreaEffectCloud areaeffectcloud = new AreaEffectCloud(this.level(), this.getX(), this.getY(), this.getZ());
        if (this.getOwner() instanceof LivingEntity livingentity) {
            areaeffectcloud.setOwner(livingentity);
        }

        areaeffectcloud.setRadius(3.0F);
        areaeffectcloud.setRadiusOnUse(-0.5F);
        areaeffectcloud.setDuration(600);
        areaeffectcloud.setWaitTime(10);
        areaeffectcloud.setRadiusPerTick(-areaeffectcloud.getRadius() / areaeffectcloud.getDuration());
        areaeffectcloud.applyComponentsFromItemStack(p_481164_);
        p_479772_.addFreshEntity(areaeffectcloud);
    }
}
