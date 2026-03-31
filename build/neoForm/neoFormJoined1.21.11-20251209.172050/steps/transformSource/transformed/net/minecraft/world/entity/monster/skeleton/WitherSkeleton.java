package net.minecraft.world.entity.monster.skeleton;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
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
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.piglin.AbstractPiglin;
import net.minecraft.world.entity.projectile.arrow.AbstractArrow;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.pathfinder.PathType;
import org.jspecify.annotations.Nullable;

public class WitherSkeleton extends AbstractSkeleton {
    public WitherSkeleton(EntityType<? extends WitherSkeleton> p_478683_, Level p_480672_) {
        super(p_478683_, p_480672_);
        this.setPathfindingMalus(PathType.LAVA, 8.0F);
    }

    @Override
    protected void registerGoals() {
        this.targetSelector.addGoal(3, new NearestAttackableTargetGoal<>(this, AbstractPiglin.class, true));
        super.registerGoals();
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return SoundEvents.WITHER_SKELETON_AMBIENT;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource damageSource) {
        return SoundEvents.WITHER_SKELETON_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.WITHER_SKELETON_DEATH;
    }

    @Override
    protected SoundEvent getStepSound() {
        return SoundEvents.WITHER_SKELETON_STEP;
    }

    @Override
    public TagKey<Item> getPreferredWeaponType() {
        return null;
    }

    @Override
    public boolean canHoldItem(ItemStack p_481295_) {
        return !p_481295_.is(ItemTags.WITHER_SKELETON_DISLIKED_WEAPONS) && super.canHoldItem(p_481295_);
    }

    @Override
    protected void populateDefaultEquipmentSlots(RandomSource p_481541_, DifficultyInstance p_478176_) {
        this.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.STONE_SWORD));
    }

    @Override
    protected void populateDefaultEquipmentEnchantments(ServerLevelAccessor p_480793_, RandomSource p_481983_, DifficultyInstance p_480847_) {
    }

    @Override
    public @Nullable SpawnGroupData finalizeSpawn(
        ServerLevelAccessor p_479867_, DifficultyInstance p_478360_, EntitySpawnReason p_481272_, @Nullable SpawnGroupData p_481025_
    ) {
        SpawnGroupData spawngroupdata = super.finalizeSpawn(p_479867_, p_478360_, p_481272_, p_481025_);
        this.getAttribute(Attributes.ATTACK_DAMAGE).setBaseValue(4.0);
        this.reassessWeaponGoal();
        return spawngroupdata;
    }

    @Override
    public boolean doHurtTarget(ServerLevel p_480447_, Entity p_482120_) {
        if (!super.doHurtTarget(p_480447_, p_482120_)) {
            return false;
        } else {
            if (p_482120_ instanceof LivingEntity) {
                ((LivingEntity)p_482120_).addEffect(new MobEffectInstance(MobEffects.WITHER, 200), this);
            }

            return true;
        }
    }

    @Override
    protected AbstractArrow getArrow(ItemStack p_479857_, float p_478742_, @Nullable ItemStack p_479462_) {
        AbstractArrow abstractarrow = super.getArrow(p_479857_, p_478742_, p_479462_);
        abstractarrow.igniteForSeconds(100.0F);
        return abstractarrow;
    }

    @Override
    public boolean canBeAffected(MobEffectInstance potionEffect) {
        return potionEffect.is(MobEffects.WITHER) ? false : super.canBeAffected(potionEffect);
    }
}
