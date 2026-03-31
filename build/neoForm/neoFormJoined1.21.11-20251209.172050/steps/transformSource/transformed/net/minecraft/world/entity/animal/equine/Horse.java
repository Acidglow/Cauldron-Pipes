package net.minecraft.world.entity.animal.equine;

import net.minecraft.core.component.DataComponentGetter;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.RandomSource;
import net.minecraft.util.Util;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.EntityAttachment;
import net.minecraft.world.entity.EntityAttachments;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.pathfinder.PathType;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jspecify.annotations.Nullable;

public class Horse extends AbstractHorse {
    private static final EntityDataAccessor<Integer> DATA_ID_TYPE_VARIANT = SynchedEntityData.defineId(Horse.class, EntityDataSerializers.INT);
    private static final EntityDimensions BABY_DIMENSIONS = EntityType.HORSE
        .getDimensions()
        .withAttachments(EntityAttachments.builder().attach(EntityAttachment.PASSENGER, 0.0F, EntityType.HORSE.getHeight() + 0.125F, 0.0F))
        .scale(0.5F);
    private static final int DEFAULT_VARIANT = 0;

    public Horse(EntityType<? extends Horse> p_479599_, Level p_478116_) {
        super(p_479599_, p_478116_);
        this.setPathfindingMalus(PathType.DANGER_OTHER, -1.0F);
        this.setPathfindingMalus(PathType.DAMAGE_OTHER, -1.0F);
    }

    @Override
    protected void randomizeAttributes(RandomSource p_480031_) {
        this.getAttribute(Attributes.MAX_HEALTH).setBaseValue(generateMaxHealth(p_480031_::nextInt));
        this.getAttribute(Attributes.MOVEMENT_SPEED).setBaseValue(generateSpeed(p_480031_::nextDouble));
        this.getAttribute(Attributes.JUMP_STRENGTH).setBaseValue(generateJumpStrength(p_480031_::nextDouble));
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder p_481349_) {
        super.defineSynchedData(p_481349_);
        p_481349_.define(DATA_ID_TYPE_VARIANT, 0);
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput p_478032_) {
        super.addAdditionalSaveData(p_478032_);
        p_478032_.putInt("Variant", this.getTypeVariant());
    }

    @Override
    protected void readAdditionalSaveData(ValueInput p_477977_) {
        super.readAdditionalSaveData(p_477977_);
        this.setTypeVariant(p_477977_.getIntOr("Variant", 0));
    }

    private void setTypeVariant(int typeVariant) {
        this.entityData.set(DATA_ID_TYPE_VARIANT, typeVariant);
    }

    private int getTypeVariant() {
        return this.entityData.get(DATA_ID_TYPE_VARIANT);
    }

    private void setVariantAndMarkings(Variant variant, Markings markings) {
        this.setTypeVariant(variant.getId() & 0xFF | markings.getId() << 8 & 0xFF00);
    }

    public Variant getVariant() {
        return Variant.byId(this.getTypeVariant() & 0xFF);
    }

    private void setVariant(Variant variant) {
        this.setTypeVariant(variant.getId() & 0xFF | this.getTypeVariant() & -256);
    }

    @Override
    public <T> @Nullable T get(DataComponentType<? extends T> p_478724_) {
        return p_478724_ == DataComponents.HORSE_VARIANT ? castComponentValue((DataComponentType<T>)p_478724_, this.getVariant()) : super.get(p_478724_);
    }

    @Override
    protected void applyImplicitComponents(DataComponentGetter p_479573_) {
        this.applyImplicitComponentIfPresent(p_479573_, DataComponents.HORSE_VARIANT);
        super.applyImplicitComponents(p_479573_);
    }

    @Override
    protected <T> boolean applyImplicitComponent(DataComponentType<T> p_481151_, T p_478796_) {
        if (p_481151_ == DataComponents.HORSE_VARIANT) {
            this.setVariant(castComponentValue(DataComponents.HORSE_VARIANT, p_478796_));
            return true;
        } else {
            return super.applyImplicitComponent(p_481151_, p_478796_);
        }
    }

    public Markings getMarkings() {
        return Markings.byId((this.getTypeVariant() & 0xFF00) >> 8);
    }

    @Override
    protected void playGallopSound(SoundType p_479320_) {
        super.playGallopSound(p_479320_);
        if (this.random.nextInt(10) == 0) {
            this.playSound(SoundEvents.HORSE_BREATHE, p_479320_.getVolume() * 0.6F, p_479320_.getPitch());
        }
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return SoundEvents.HORSE_AMBIENT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.HORSE_DEATH;
    }

    @Override
    protected SoundEvent getEatingSound() {
        return SoundEvents.HORSE_EAT;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource damageSource) {
        return SoundEvents.HORSE_HURT;
    }

    @Override
    protected SoundEvent getAngrySound() {
        return SoundEvents.HORSE_ANGRY;
    }

    @Override
    public InteractionResult mobInteract(Player player, InteractionHand hand) {
        boolean flag = !this.isBaby() && this.isTamed() && player.isSecondaryUseActive();
        if (!this.isVehicle() && !flag) {
            ItemStack itemstack = player.getItemInHand(hand);
            if (!itemstack.isEmpty()) {
                if (this.isFood(itemstack)) {
                    return this.fedFood(player, itemstack);
                }

                if (!this.isTamed()) {
                    this.makeMad();
                    return InteractionResult.SUCCESS;
                }
            }

            return super.mobInteract(player, hand);
        } else {
            return super.mobInteract(player, hand);
        }
    }

    /**
     * Returns {@code true} if the mob is currently able to mate with the specified mob.
     */
    @Override
    public boolean canMate(Animal otherAnimal) {
        if (otherAnimal == this) {
            return false;
        } else {
            return !(otherAnimal instanceof Donkey) && !(otherAnimal instanceof Horse) ? false : this.canParent() && ((AbstractHorse)otherAnimal).canParent();
        }
    }

    @Override
    public @Nullable AgeableMob getBreedOffspring(ServerLevel p_480113_, AgeableMob p_480988_) {
        if (p_480988_ instanceof Donkey) {
            Mule mule = EntityType.MULE.create(p_480113_, EntitySpawnReason.BREEDING);
            if (mule != null) {
                this.setOffspringAttributes(p_480988_, mule);
            }

            return mule;
        } else {
            Horse horse = (Horse)p_480988_;
            Horse horse1 = EntityType.HORSE.create(p_480113_, EntitySpawnReason.BREEDING);
            if (horse1 != null) {
                int i = this.random.nextInt(9);
                Variant variant;
                if (i < 4) {
                    variant = this.getVariant();
                } else if (i < 8) {
                    variant = horse.getVariant();
                } else {
                    variant = Util.getRandom(Variant.values(), this.random);
                }

                int j = this.random.nextInt(5);
                Markings markings;
                if (j < 2) {
                    markings = this.getMarkings();
                } else if (j < 4) {
                    markings = horse.getMarkings();
                } else {
                    markings = Util.getRandom(Markings.values(), this.random);
                }

                horse1.setVariantAndMarkings(variant, markings);
                this.setOffspringAttributes(p_480988_, horse1);
            }

            return horse1;
        }
    }

    @Override
    public boolean canUseSlot(EquipmentSlot p_479791_) {
        return true;
    }

    @Override
    protected void hurtArmor(DamageSource p_479382_, float p_478845_) {
        this.doHurtEquipment(p_479382_, p_478845_, EquipmentSlot.BODY);
    }

    @Override
    public @Nullable SpawnGroupData finalizeSpawn(
        ServerLevelAccessor p_479107_, DifficultyInstance p_479178_, EntitySpawnReason p_479054_, @Nullable SpawnGroupData p_479757_
    ) {
        RandomSource randomsource = p_479107_.getRandom();
        Variant variant;
        if (p_479757_ instanceof Horse.HorseGroupData) {
            variant = ((Horse.HorseGroupData)p_479757_).variant;
        } else {
            variant = Util.getRandom(Variant.values(), randomsource);
            p_479757_ = new Horse.HorseGroupData(variant);
        }

        this.setVariantAndMarkings(variant, Util.getRandom(Markings.values(), randomsource));
        return super.finalizeSpawn(p_479107_, p_479178_, p_479054_, p_479757_);
    }

    @Override
    public EntityDimensions getDefaultDimensions(Pose p_479592_) {
        return this.isBaby() ? BABY_DIMENSIONS : super.getDefaultDimensions(p_479592_);
    }

    public static class HorseGroupData extends AgeableMob.AgeableMobGroupData {
        public final Variant variant;

        public HorseGroupData(Variant variant) {
            super(true);
            this.variant = variant;
        }
    }
}
