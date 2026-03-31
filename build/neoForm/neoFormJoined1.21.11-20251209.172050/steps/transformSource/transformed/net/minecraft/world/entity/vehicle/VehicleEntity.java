package net.minecraft.world.entity.vehicle;

import net.minecraft.core.component.DataComponents;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.gamerules.GameRules;

public abstract class VehicleEntity extends Entity {
    protected static final EntityDataAccessor<Integer> DATA_ID_HURT = SynchedEntityData.defineId(VehicleEntity.class, EntityDataSerializers.INT);
    protected static final EntityDataAccessor<Integer> DATA_ID_HURTDIR = SynchedEntityData.defineId(VehicleEntity.class, EntityDataSerializers.INT);
    protected static final EntityDataAccessor<Float> DATA_ID_DAMAGE = SynchedEntityData.defineId(VehicleEntity.class, EntityDataSerializers.FLOAT);

    public VehicleEntity(EntityType<?> p_306130_, Level p_306167_) {
        super(p_306130_, p_306167_);
    }

    @Override
    public boolean hurtClient(DamageSource p_376811_) {
        return true;
    }

    @Override
    public boolean hurtServer(ServerLevel p_376703_, DamageSource p_376603_, float p_376371_) {
        if (this.isRemoved()) {
            return true;
        } else if (this.isInvulnerableToBase(p_376603_)) {
            return false;
        } else {
            this.setHurtDir(-this.getHurtDir());
            this.setHurtTime(10);
            this.markHurt();
            this.setDamage(this.getDamage() + p_376371_ * 10.0F);
            this.gameEvent(GameEvent.ENTITY_DAMAGE, p_376603_.getEntity());
            boolean flag = p_376603_.getEntity() instanceof Player player && player.getAbilities().instabuild;
            if ((flag || !(this.getDamage() > 40.0F)) && !this.shouldSourceDestroy(p_376603_)) {
                if (flag) {
                    this.discard();
                }
            } else {
                this.destroy(p_376703_, p_376603_);
            }

            return true;
        }
    }

    protected boolean shouldSourceDestroy(DamageSource source) {
        return false;
    }

    @Override
    public boolean ignoreExplosion(Explosion p_368563_) {
        return p_368563_.getIndirectSourceEntity() instanceof Mob && !p_368563_.level().getGameRules().get(GameRules.MOB_GRIEFING);
    }

    public void destroy(ServerLevel level, Item dropItem) {
        this.kill(level);
        if (level.getGameRules().get(GameRules.ENTITY_DROPS)) {
            ItemStack itemstack = new ItemStack(dropItem);
            itemstack.set(DataComponents.CUSTOM_NAME, this.getCustomName());
            this.spawnAtLocation(level, itemstack);
        }
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder p_326046_) {
        p_326046_.define(DATA_ID_HURT, 0);
        p_326046_.define(DATA_ID_HURTDIR, 1);
        p_326046_.define(DATA_ID_DAMAGE, 0.0F);
    }

    public void setHurtTime(int hurtTime) {
        this.entityData.set(DATA_ID_HURT, hurtTime);
    }

    public void setHurtDir(int hurtDir) {
        this.entityData.set(DATA_ID_HURTDIR, hurtDir);
    }

    public void setDamage(float damage) {
        this.entityData.set(DATA_ID_DAMAGE, damage);
    }

    public float getDamage() {
        return this.entityData.get(DATA_ID_DAMAGE);
    }

    public int getHurtTime() {
        return this.entityData.get(DATA_ID_HURT);
    }

    public int getHurtDir() {
        return this.entityData.get(DATA_ID_HURTDIR);
    }

    protected void destroy(ServerLevel level, DamageSource damageSource) {
        this.destroy(level, this.getDropItem());
    }

    @Override
    public int getDimensionChangingDelay() {
        return 10;
    }

    protected abstract Item getDropItem();
}
