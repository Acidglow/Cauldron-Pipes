package net.minecraft.world.entity.monster.zombie;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.RandomSource;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.animal.camel.CamelHusk;
import net.minecraft.world.entity.monster.skeleton.Parched;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import org.jspecify.annotations.Nullable;

public class Husk extends Zombie {
    public Husk(EntityType<? extends Husk> p_481818_, Level p_479962_) {
        super(p_481818_, p_479962_);
    }

    @Override
    protected boolean isSunSensitive() {
        return false;
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return SoundEvents.HUSK_AMBIENT;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource damageSource) {
        return SoundEvents.HUSK_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.HUSK_DEATH;
    }

    @Override
    protected SoundEvent getStepSound() {
        return SoundEvents.HUSK_STEP;
    }

    @Override
    public boolean doHurtTarget(ServerLevel p_478036_, Entity p_480930_) {
        boolean flag = super.doHurtTarget(p_478036_, p_480930_);
        if (flag && this.getMainHandItem().isEmpty() && p_480930_ instanceof LivingEntity) {
            float f = p_478036_.getCurrentDifficultyAt(this.blockPosition()).getEffectiveDifficulty();
            ((LivingEntity)p_480930_).addEffect(new MobEffectInstance(MobEffects.HUNGER, 140 * (int)f), this);
        }

        return flag;
    }

    @Override
    protected boolean convertsInWater() {
        return true;
    }

    @Override
    protected void doUnderWaterConversion(ServerLevel p_478247_) {
        if (!net.neoforged.neoforge.event.EventHooks.canLivingConvert(this, EntityType.ZOMBIE, (timer) -> this.conversionTime = timer)) return;
        this.convertToZombieType(p_478247_, EntityType.ZOMBIE);
        if (!this.isSilent()) {
            p_478247_.levelEvent(null, 1041, this.blockPosition(), 0);
        }
    }

    @Override
    public @Nullable SpawnGroupData finalizeSpawn(
        ServerLevelAccessor p_481590_, DifficultyInstance p_480270_, EntitySpawnReason p_478215_, @Nullable SpawnGroupData p_480515_
    ) {
        RandomSource randomsource = p_481590_.getRandom();
        p_480515_ = super.finalizeSpawn(p_481590_, p_480270_, p_478215_, p_480515_);
        float f = p_480270_.getSpecialMultiplier();
        if (p_478215_ != EntitySpawnReason.CONVERSION) {
            this.setCanPickUpLoot(randomsource.nextFloat() < 0.55F * f);
        }

        if (p_480515_ != null) {
            p_480515_ = new Husk.HuskGroupData((Zombie.ZombieGroupData)p_480515_);
            ((Husk.HuskGroupData)p_480515_).triedToSpawnCamelHusk = p_478215_ != EntitySpawnReason.NATURAL;
        }

        if (p_480515_ instanceof Husk.HuskGroupData husk$huskgroupdata && !husk$huskgroupdata.triedToSpawnCamelHusk) {
            BlockPos blockpos = this.blockPosition();
            if (p_481590_.noCollision(EntityType.CAMEL_HUSK.getSpawnAABB(blockpos.getX() + 0.5, blockpos.getY(), blockpos.getZ() + 0.5))) {
                husk$huskgroupdata.triedToSpawnCamelHusk = true;
                if (randomsource.nextFloat() < 0.1F) {
                    this.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.IRON_SPEAR));
                    CamelHusk camelhusk = EntityType.CAMEL_HUSK.create(this.level(), EntitySpawnReason.NATURAL);
                    if (camelhusk != null) {
                        camelhusk.setPos(this.getX(), this.getY(), this.getZ());
                        camelhusk.finalizeSpawn(p_481590_, p_480270_, p_478215_, null);
                        this.startRiding(camelhusk, true, true);
                        p_481590_.addFreshEntity(camelhusk);
                        Parched parched = EntityType.PARCHED.create(this.level(), EntitySpawnReason.NATURAL);
                        if (parched != null) {
                            parched.snapTo(this.getX(), this.getY(), this.getZ(), this.getYRot(), 0.0F);
                            parched.finalizeSpawn(p_481590_, p_480270_, p_478215_, null);
                            parched.startRiding(camelhusk, false, false);
                            p_481590_.addFreshEntityWithPassengers(parched);
                        }
                    }
                }
            }
        }

        return p_480515_;
    }

    public static class HuskGroupData extends Zombie.ZombieGroupData {
        public boolean triedToSpawnCamelHusk = false;

        public HuskGroupData(Zombie.ZombieGroupData groupData) {
            super(groupData.isBaby, groupData.canSpawnJockey);
        }
    }
}
