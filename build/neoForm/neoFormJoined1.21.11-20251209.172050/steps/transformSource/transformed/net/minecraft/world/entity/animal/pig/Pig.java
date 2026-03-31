package net.minecraft.world.entity.animal.pig;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponentGetter;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.Difficulty;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.ConversionParams;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ItemBasedSteering;
import net.minecraft.world.entity.ItemSteerable;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.BreedGoal;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.FollowParentGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.PanicGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.TemptGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.monster.zombie.ZombifiedPiglin;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.variant.SpawnContext;
import net.minecraft.world.entity.variant.VariantUtils;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.equipment.Equippable;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

public class Pig extends Animal implements ItemSteerable {
    private static final EntityDataAccessor<Integer> DATA_BOOST_TIME = SynchedEntityData.defineId(Pig.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Holder<PigVariant>> DATA_VARIANT_ID = SynchedEntityData.defineId(Pig.class, EntityDataSerializers.PIG_VARIANT);
    private final ItemBasedSteering steering = new ItemBasedSteering(this.entityData, DATA_BOOST_TIME);

    public Pig(EntityType<? extends Pig> p_481572_, Level p_479012_) {
        super(p_481572_, p_479012_);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(1, new PanicGoal(this, 1.25));
        this.goalSelector.addGoal(3, new BreedGoal(this, 1.0));
        this.goalSelector.addGoal(4, new TemptGoal(this, 1.2, p_479093_ -> p_479093_.is(Items.CARROT_ON_A_STICK), false));
        this.goalSelector.addGoal(4, new TemptGoal(this, 1.2, p_481560_ -> p_481560_.is(ItemTags.PIG_FOOD), false));
        this.goalSelector.addGoal(5, new FollowParentGoal(this, 1.1));
        this.goalSelector.addGoal(6, new WaterAvoidingRandomStrollGoal(this, 1.0));
        this.goalSelector.addGoal(7, new LookAtPlayerGoal(this, Player.class, 6.0F));
        this.goalSelector.addGoal(8, new RandomLookAroundGoal(this));
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Animal.createAnimalAttributes().add(Attributes.MAX_HEALTH, 10.0).add(Attributes.MOVEMENT_SPEED, 0.25);
    }

    @Override
    public @Nullable LivingEntity getControllingPassenger() {
        return (LivingEntity)(this.isSaddled() && this.getFirstPassenger() instanceof Player player && player.isHolding(Items.CARROT_ON_A_STICK)
            ? player
            : super.getControllingPassenger());
    }

    @Override
    public void onSyncedDataUpdated(EntityDataAccessor<?> key) {
        if (DATA_BOOST_TIME.equals(key) && this.level().isClientSide()) {
            this.steering.onSynced();
        }

        super.onSyncedDataUpdated(key);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder p_478499_) {
        super.defineSynchedData(p_478499_);
        p_478499_.define(DATA_BOOST_TIME, 0);
        p_478499_.define(DATA_VARIANT_ID, VariantUtils.getDefaultOrAny(this.registryAccess(), PigVariants.DEFAULT));
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput p_478763_) {
        super.addAdditionalSaveData(p_478763_);
        VariantUtils.writeVariant(p_478763_, this.getVariant());
    }

    @Override
    protected void readAdditionalSaveData(ValueInput p_478738_) {
        super.readAdditionalSaveData(p_478738_);
        VariantUtils.readVariant(p_478738_, Registries.PIG_VARIANT).ifPresent(this::setVariant);
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return SoundEvents.PIG_AMBIENT;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource damageSource) {
        return SoundEvents.PIG_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.PIG_DEATH;
    }

    @Override
    protected void playStepSound(BlockPos pos, BlockState block) {
        this.playSound(SoundEvents.PIG_STEP, 0.15F, 1.0F);
    }

    @Override
    public InteractionResult mobInteract(Player player, InteractionHand hand) {
        boolean flag = this.isFood(player.getItemInHand(hand));
        if (!flag && this.isSaddled() && !this.isVehicle() && !player.isSecondaryUseActive()) {
            if (!this.level().isClientSide()) {
                player.startRiding(this);
            }

            return InteractionResult.SUCCESS;
        } else {
            InteractionResult interactionresult = super.mobInteract(player, hand);
            if (!interactionresult.consumesAction()) {
                ItemStack itemstack = player.getItemInHand(hand);
                return (InteractionResult)(this.isEquippableInSlot(itemstack, EquipmentSlot.SADDLE)
                    ? itemstack.interactLivingEntity(player, this, hand)
                    : InteractionResult.PASS);
            } else {
                return interactionresult;
            }
        }
    }

    @Override
    public boolean canUseSlot(EquipmentSlot p_478461_) {
        return p_478461_ != EquipmentSlot.SADDLE ? super.canUseSlot(p_478461_) : this.isAlive() && !this.isBaby();
    }

    @Override
    protected boolean canDispenserEquipIntoSlot(EquipmentSlot p_481533_) {
        return p_481533_ == EquipmentSlot.SADDLE || super.canDispenserEquipIntoSlot(p_481533_);
    }

    @Override
    protected Holder<SoundEvent> getEquipSound(EquipmentSlot p_477944_, ItemStack p_479907_, Equippable p_479723_) {
        return (Holder<SoundEvent>)(p_477944_ == EquipmentSlot.SADDLE ? SoundEvents.PIG_SADDLE : super.getEquipSound(p_477944_, p_479907_, p_479723_));
    }

    @Override
    public void thunderHit(ServerLevel level, LightningBolt lightning) {
        if (level.getDifficulty() != Difficulty.PEACEFUL && net.neoforged.neoforge.event.EventHooks.canLivingConvert(this, EntityType.ZOMBIFIED_PIGLIN, (timer) -> {})) {
            ZombifiedPiglin zombifiedpiglin = this.convertTo(EntityType.ZOMBIFIED_PIGLIN, ConversionParams.single(this, false, true), p_480835_ -> {
                p_480835_.populateDefaultEquipmentSlots(this.getRandom(), level.getCurrentDifficultyAt(this.blockPosition()));
                p_480835_.setPersistenceRequired();
                net.neoforged.neoforge.event.EventHooks.onLivingConvert(this, p_480835_);
            });
            if (zombifiedpiglin == null) {
                super.thunderHit(level, lightning);
            }
        } else {
            super.thunderHit(level, lightning);
        }
    }

    @Override
    protected void tickRidden(Player p_481148_, Vec3 p_479823_) {
        super.tickRidden(p_481148_, p_479823_);
        this.setRot(p_481148_.getYRot(), p_481148_.getXRot() * 0.5F);
        this.yRotO = this.yBodyRot = this.yHeadRot = this.getYRot();
        this.steering.tickBoost();
    }

    @Override
    protected Vec3 getRiddenInput(Player p_479334_, Vec3 p_478978_) {
        return new Vec3(0.0, 0.0, 1.0);
    }

    @Override
    protected float getRiddenSpeed(Player p_478057_) {
        return (float)(this.getAttributeValue(Attributes.MOVEMENT_SPEED) * 0.225 * this.steering.boostFactor());
    }

    @Override
    public boolean boost() {
        return this.steering.boost(this.getRandom());
    }

    public @Nullable Pig getBreedOffspring(ServerLevel p_480054_, AgeableMob p_480322_) {
        Pig pig = EntityType.PIG.create(p_480054_, EntitySpawnReason.BREEDING);
        if (pig != null && p_480322_ instanceof Pig pig1) {
            pig.setVariant(this.random.nextBoolean() ? this.getVariant() : pig1.getVariant());
        }

        return pig;
    }

    /**
     * Checks if the parameter is an item which this animal can be fed to breed it (wheat, carrots or seeds depending on the animal type)
     */
    @Override
    public boolean isFood(ItemStack stack) {
        return stack.is(ItemTags.PIG_FOOD);
    }

    @Override
    public Vec3 getLeashOffset() {
        return new Vec3(0.0, 0.6F * this.getEyeHeight(), this.getBbWidth() * 0.4F);
    }

    private void setVariant(Holder<PigVariant> variant) {
        this.entityData.set(DATA_VARIANT_ID, variant);
    }

    public Holder<PigVariant> getVariant() {
        return this.entityData.get(DATA_VARIANT_ID);
    }

    @Override
    public <T> @Nullable T get(DataComponentType<? extends T> p_480015_) {
        return p_480015_ == DataComponents.PIG_VARIANT ? castComponentValue((DataComponentType<T>)p_480015_, this.getVariant()) : super.get(p_480015_);
    }

    @Override
    protected void applyImplicitComponents(DataComponentGetter p_480171_) {
        this.applyImplicitComponentIfPresent(p_480171_, DataComponents.PIG_VARIANT);
        super.applyImplicitComponents(p_480171_);
    }

    @Override
    protected <T> boolean applyImplicitComponent(DataComponentType<T> p_478920_, T p_481074_) {
        if (p_478920_ == DataComponents.PIG_VARIANT) {
            this.setVariant(castComponentValue(DataComponents.PIG_VARIANT, p_481074_));
            return true;
        } else {
            return super.applyImplicitComponent(p_478920_, p_481074_);
        }
    }

    @Override
    public SpawnGroupData finalizeSpawn(
        ServerLevelAccessor p_478479_, DifficultyInstance p_481884_, EntitySpawnReason p_480771_, @Nullable SpawnGroupData p_478284_
    ) {
        VariantUtils.selectVariantToSpawn(SpawnContext.create(p_478479_, this.blockPosition()), Registries.PIG_VARIANT).ifPresent(this::setVariant);
        return super.finalizeSpawn(p_478479_, p_481884_, p_480771_, p_478284_);
    }
}
