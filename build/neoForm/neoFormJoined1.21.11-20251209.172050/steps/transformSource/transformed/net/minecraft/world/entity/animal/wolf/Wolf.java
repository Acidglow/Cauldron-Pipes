package net.minecraft.world.entity.animal.wolf;

import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponentGetter;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.ItemParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.tags.ItemTags;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.util.TimeUtil;
import net.minecraft.util.valueproviders.UniformInt;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.Crackiness;
import net.minecraft.world.entity.EntityReference;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.NeutralMob;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.AvoidEntityGoal;
import net.minecraft.world.entity.ai.goal.BegGoal;
import net.minecraft.world.entity.ai.goal.BreedGoal;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.FollowOwnerGoal;
import net.minecraft.world.entity.ai.goal.LeapAtTargetGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.SitWhenOrderedToGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NonTameRandomTargetGoal;
import net.minecraft.world.entity.ai.goal.target.OwnerHurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.OwnerHurtTargetGoal;
import net.minecraft.world.entity.ai.goal.target.ResetUniversalAngerTargetGoal;
import net.minecraft.world.entity.ai.targeting.TargetingConditions;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.animal.equine.AbstractHorse;
import net.minecraft.world.entity.animal.equine.Llama;
import net.minecraft.world.entity.animal.turtle.Turtle;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.monster.Ghast;
import net.minecraft.world.entity.monster.skeleton.AbstractSkeleton;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.variant.SpawnContext;
import net.minecraft.world.entity.variant.VariantUtils;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.DyeItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.pathfinder.PathType;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

public class Wolf extends TamableAnimal implements NeutralMob {
    private static final EntityDataAccessor<Boolean> DATA_INTERESTED_ID = SynchedEntityData.defineId(Wolf.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Integer> DATA_COLLAR_COLOR = SynchedEntityData.defineId(Wolf.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Long> DATA_ANGER_END_TIME = SynchedEntityData.defineId(Wolf.class, EntityDataSerializers.LONG);
    private static final EntityDataAccessor<Holder<WolfVariant>> DATA_VARIANT_ID = SynchedEntityData.defineId(Wolf.class, EntityDataSerializers.WOLF_VARIANT);
    private static final EntityDataAccessor<Holder<WolfSoundVariant>> DATA_SOUND_VARIANT_ID = SynchedEntityData.defineId(
        Wolf.class, EntityDataSerializers.WOLF_SOUND_VARIANT
    );
    public static final TargetingConditions.Selector PREY_SELECTOR = (p_423321_, p_423322_) -> {
        EntityType<?> entitytype = p_423321_.getType();
        return entitytype == EntityType.SHEEP || entitytype == EntityType.RABBIT || entitytype == EntityType.FOX;
    };
    private static final float START_HEALTH = 8.0F;
    private static final float TAME_HEALTH = 40.0F;
    private static final float ARMOR_REPAIR_UNIT = 0.125F;
    public static final float DEFAULT_TAIL_ANGLE = (float) (Math.PI / 5);
    private static final DyeColor DEFAULT_COLLAR_COLOR = DyeColor.RED;
    private float interestedAngle;
    private float interestedAngleO;
    private boolean isWet;
    private boolean isShaking;
    private float shakeAnim;
    private float shakeAnimO;
    private static final UniformInt PERSISTENT_ANGER_TIME = TimeUtil.rangeOfSeconds(20, 39);
    private @Nullable EntityReference<LivingEntity> persistentAngerTarget;

    public Wolf(EntityType<? extends Wolf> p_406225_, Level p_406236_) {
        super(p_406225_, p_406236_);
        this.setTame(false, false);
        this.setPathfindingMalus(PathType.POWDER_SNOW, -1.0F);
        this.setPathfindingMalus(PathType.DANGER_POWDER_SNOW, -1.0F);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(1, new FloatGoal(this));
        this.goalSelector.addGoal(1, new TamableAnimal.TamableAnimalPanicGoal(1.5, DamageTypeTags.PANIC_ENVIRONMENTAL_CAUSES));
        this.goalSelector.addGoal(2, new SitWhenOrderedToGoal(this));
        this.goalSelector.addGoal(3, new Wolf.WolfAvoidEntityGoal<>(this, Llama.class, 24.0F, 1.5, 1.5));
        this.goalSelector.addGoal(4, new LeapAtTargetGoal(this, 0.4F));
        this.goalSelector.addGoal(5, new MeleeAttackGoal(this, 1.0, true));
        this.goalSelector.addGoal(6, new FollowOwnerGoal(this, 1.0, 10.0F, 2.0F));
        this.goalSelector.addGoal(7, new BreedGoal(this, 1.0));
        this.goalSelector.addGoal(8, new WaterAvoidingRandomStrollGoal(this, 1.0));
        this.goalSelector.addGoal(9, new BegGoal(this, 8.0F));
        this.goalSelector.addGoal(10, new LookAtPlayerGoal(this, Player.class, 8.0F));
        this.goalSelector.addGoal(10, new RandomLookAroundGoal(this));
        this.targetSelector.addGoal(1, new OwnerHurtByTargetGoal(this));
        this.targetSelector.addGoal(2, new OwnerHurtTargetGoal(this));
        this.targetSelector.addGoal(3, new HurtByTargetGoal(this).setAlertOthers());
        this.targetSelector.addGoal(4, new NearestAttackableTargetGoal<>(this, Player.class, 10, true, false, this::isAngryAt));
        this.targetSelector.addGoal(5, new NonTameRandomTargetGoal<>(this, Animal.class, false, PREY_SELECTOR));
        this.targetSelector.addGoal(6, new NonTameRandomTargetGoal<>(this, Turtle.class, false, Turtle.BABY_ON_LAND_SELECTOR));
        this.targetSelector.addGoal(7, new NearestAttackableTargetGoal<>(this, AbstractSkeleton.class, false));
        this.targetSelector.addGoal(8, new ResetUniversalAngerTargetGoal<>(this, true));
    }

    public Identifier getTexture() {
        WolfVariant wolfvariant = this.getVariant().value();
        if (this.isTame()) {
            return wolfvariant.assetInfo().tame().texturePath();
        } else {
            return this.isAngry() ? wolfvariant.assetInfo().angry().texturePath() : wolfvariant.assetInfo().wild().texturePath();
        }
    }

    private Holder<WolfVariant> getVariant() {
        return this.entityData.get(DATA_VARIANT_ID);
    }

    private void setVariant(Holder<WolfVariant> variant) {
        this.entityData.set(DATA_VARIANT_ID, variant);
    }

    private Holder<WolfSoundVariant> getSoundVariant() {
        return this.entityData.get(DATA_SOUND_VARIANT_ID);
    }

    private void setSoundVariant(Holder<WolfSoundVariant> soundVariant) {
        this.entityData.set(DATA_SOUND_VARIANT_ID, soundVariant);
    }

    @Override
    public <T> @Nullable T get(DataComponentType<? extends T> p_406363_) {
        if (p_406363_ == DataComponents.WOLF_VARIANT) {
            return castComponentValue((DataComponentType<T>)p_406363_, this.getVariant());
        } else if (p_406363_ == DataComponents.WOLF_SOUND_VARIANT) {
            return castComponentValue((DataComponentType<T>)p_406363_, this.getSoundVariant());
        } else {
            return p_406363_ == DataComponents.WOLF_COLLAR ? castComponentValue((DataComponentType<T>)p_406363_, this.getCollarColor()) : super.get(p_406363_);
        }
    }

    @Override
    protected void applyImplicitComponents(DataComponentGetter p_406306_) {
        this.applyImplicitComponentIfPresent(p_406306_, DataComponents.WOLF_VARIANT);
        this.applyImplicitComponentIfPresent(p_406306_, DataComponents.WOLF_SOUND_VARIANT);
        this.applyImplicitComponentIfPresent(p_406306_, DataComponents.WOLF_COLLAR);
        super.applyImplicitComponents(p_406306_);
    }

    @Override
    protected <T> boolean applyImplicitComponent(DataComponentType<T> p_406368_, T p_406235_) {
        if (p_406368_ == DataComponents.WOLF_VARIANT) {
            this.setVariant(castComponentValue(DataComponents.WOLF_VARIANT, p_406235_));
            return true;
        } else if (p_406368_ == DataComponents.WOLF_SOUND_VARIANT) {
            this.setSoundVariant(castComponentValue(DataComponents.WOLF_SOUND_VARIANT, p_406235_));
            return true;
        } else if (p_406368_ == DataComponents.WOLF_COLLAR) {
            this.setCollarColor(castComponentValue(DataComponents.WOLF_COLLAR, p_406235_));
            return true;
        } else {
            return super.applyImplicitComponent(p_406368_, p_406235_);
        }
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Animal.createAnimalAttributes().add(Attributes.MOVEMENT_SPEED, 0.3F).add(Attributes.MAX_HEALTH, 8.0).add(Attributes.ATTACK_DAMAGE, 4.0);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder p_406275_) {
        super.defineSynchedData(p_406275_);
        Registry<WolfSoundVariant> registry = this.registryAccess().lookupOrThrow(Registries.WOLF_SOUND_VARIANT);
        p_406275_.define(DATA_VARIANT_ID, VariantUtils.getDefaultOrAny(this.registryAccess(), WolfVariants.DEFAULT));
        p_406275_.define(DATA_SOUND_VARIANT_ID, registry.get(WolfSoundVariants.CLASSIC).or(registry::getAny).orElseThrow());
        p_406275_.define(DATA_INTERESTED_ID, false);
        p_406275_.define(DATA_COLLAR_COLOR, DEFAULT_COLLAR_COLOR.getId());
        p_406275_.define(DATA_ANGER_END_TIME, -1L);
    }

    @Override
    protected void playStepSound(BlockPos p_406221_, BlockState p_406277_) {
        this.playSound(SoundEvents.WOLF_STEP, 0.15F, 1.0F);
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput p_422504_) {
        super.addAdditionalSaveData(p_422504_);
        p_422504_.store("CollarColor", DyeColor.LEGACY_ID_CODEC, this.getCollarColor());
        VariantUtils.writeVariant(p_422504_, this.getVariant());
        this.addPersistentAngerSaveData(p_422504_);
        this.getSoundVariant()
            .unwrapKey()
            .ifPresent(
                p_421374_ -> p_422504_.store("sound_variant", ResourceKey.codec(Registries.WOLF_SOUND_VARIANT), (ResourceKey<WolfSoundVariant>)p_421374_)
            );
    }

    @Override
    protected void readAdditionalSaveData(ValueInput p_421669_) {
        super.readAdditionalSaveData(p_421669_);
        VariantUtils.readVariant(p_421669_, Registries.WOLF_VARIANT).ifPresent(this::setVariant);
        this.setCollarColor(p_421669_.read("CollarColor", DyeColor.LEGACY_ID_CODEC).orElse(DEFAULT_COLLAR_COLOR));
        this.readPersistentAngerSaveData(this.level(), p_421669_);
        p_421669_.read("sound_variant", ResourceKey.codec(Registries.WOLF_SOUND_VARIANT))
            .flatMap(p_427093_ -> this.registryAccess().lookupOrThrow(Registries.WOLF_SOUND_VARIANT).get((ResourceKey<WolfSoundVariant>)p_427093_))
            .ifPresent(this::setSoundVariant);
    }

    @Override
    public @Nullable SpawnGroupData finalizeSpawn(
        ServerLevelAccessor p_406251_, DifficultyInstance p_406357_, EntitySpawnReason p_406333_, @Nullable SpawnGroupData p_406273_
    ) {
        if (p_406273_ instanceof Wolf.WolfPackData wolf$wolfpackdata) {
            this.setVariant(wolf$wolfpackdata.type);
        } else {
            Optional<? extends Holder<WolfVariant>> optional = VariantUtils.selectVariantToSpawn(
                SpawnContext.create(p_406251_, this.blockPosition()), Registries.WOLF_VARIANT
            );
            if (optional.isPresent()) {
                this.setVariant((Holder<WolfVariant>)optional.get());
                p_406273_ = new Wolf.WolfPackData((Holder<WolfVariant>)optional.get());
            }
        }

        this.setSoundVariant(WolfSoundVariants.pickRandomSoundVariant(this.registryAccess(), p_406251_.getRandom()));
        return super.finalizeSpawn(p_406251_, p_406357_, p_406333_, p_406273_);
    }

    @Override
    protected SoundEvent getAmbientSound() {
        if (this.isAngry()) {
            return this.getSoundVariant().value().growlSound().value();
        } else if (this.random.nextInt(3) == 0) {
            return this.isTame() && this.getHealth() < 20.0F
                ? this.getSoundVariant().value().whineSound().value()
                : this.getSoundVariant().value().pantSound().value();
        } else {
            return this.getSoundVariant().value().ambientSound().value();
        }
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource p_406243_) {
        return this.canArmorAbsorb(p_406243_) ? SoundEvents.WOLF_ARMOR_DAMAGE : this.getSoundVariant().value().hurtSound().value();
    }

    @Override
    protected SoundEvent getDeathSound() {
        return this.getSoundVariant().value().deathSound().value();
    }

    @Override
    protected float getSoundVolume() {
        return 0.4F;
    }

    @Override
    public void aiStep() {
        super.aiStep();
        if (!this.level().isClientSide() && this.isWet && !this.isShaking && !this.isPathFinding() && this.onGround()) {
            this.isShaking = true;
            this.shakeAnim = 0.0F;
            this.shakeAnimO = 0.0F;
            this.level().broadcastEntityEvent(this, (byte)8);
        }

        if (!this.level().isClientSide()) {
            this.updatePersistentAnger((ServerLevel)this.level(), true);
        }
    }

    @Override
    public void tick() {
        super.tick();
        if (this.isAlive()) {
            this.interestedAngleO = this.interestedAngle;
            if (this.isInterested()) {
                this.interestedAngle = this.interestedAngle + (1.0F - this.interestedAngle) * 0.4F;
            } else {
                this.interestedAngle = this.interestedAngle + (0.0F - this.interestedAngle) * 0.4F;
            }

            if (this.isInWaterOrRain()) {
                this.isWet = true;
                if (this.isShaking && !this.level().isClientSide()) {
                    this.level().broadcastEntityEvent(this, (byte)56);
                    this.cancelShake();
                }
            } else if ((this.isWet || this.isShaking) && this.isShaking) {
                if (this.shakeAnim == 0.0F) {
                    this.playSound(SoundEvents.WOLF_SHAKE, this.getSoundVolume(), (this.random.nextFloat() - this.random.nextFloat()) * 0.2F + 1.0F);
                    this.gameEvent(GameEvent.ENTITY_ACTION);
                }

                this.shakeAnimO = this.shakeAnim;
                this.shakeAnim += 0.05F;
                if (this.shakeAnimO >= 2.0F) {
                    this.isWet = false;
                    this.isShaking = false;
                    this.shakeAnimO = 0.0F;
                    this.shakeAnim = 0.0F;
                }

                if (this.shakeAnim > 0.4F) {
                    float f = (float)this.getY();
                    int i = (int)(Mth.sin((this.shakeAnim - 0.4F) * (float) Math.PI) * 7.0F);
                    Vec3 vec3 = this.getDeltaMovement();

                    for (int j = 0; j < i; j++) {
                        float f1 = (this.random.nextFloat() * 2.0F - 1.0F) * this.getBbWidth() * 0.5F;
                        float f2 = (this.random.nextFloat() * 2.0F - 1.0F) * this.getBbWidth() * 0.5F;
                        this.level().addParticle(ParticleTypes.SPLASH, this.getX() + f1, f + 0.8F, this.getZ() + f2, vec3.x, vec3.y, vec3.z);
                    }
                }
            }
        }
    }

    private void cancelShake() {
        this.isShaking = false;
        this.shakeAnim = 0.0F;
        this.shakeAnimO = 0.0F;
    }

    @Override
    public void die(DamageSource p_406227_) {
        this.isWet = false;
        this.isShaking = false;
        this.shakeAnimO = 0.0F;
        this.shakeAnim = 0.0F;
        super.die(p_406227_);
    }

    public float getWetShade(float partialTick) {
        return !this.isWet ? 1.0F : Math.min(0.75F + Mth.lerp(partialTick, this.shakeAnimO, this.shakeAnim) / 2.0F * 0.25F, 1.0F);
    }

    public float getShakeAnim(float partialTick) {
        return Mth.lerp(partialTick, this.shakeAnimO, this.shakeAnim);
    }

    public float getHeadRollAngle(float partialTick) {
        return Mth.lerp(partialTick, this.interestedAngleO, this.interestedAngle) * 0.15F * (float) Math.PI;
    }

    @Override
    public int getMaxHeadXRot() {
        return this.isInSittingPose() ? 20 : super.getMaxHeadXRot();
    }

    @Override
    public boolean hurtServer(ServerLevel p_406240_, DamageSource p_406339_, float p_406257_) {
        if (this.isInvulnerableTo(p_406240_, p_406339_)) {
            return false;
        } else {
            this.setOrderedToSit(false);
            return super.hurtServer(p_406240_, p_406339_, p_406257_);
        }
    }

    @Override
    protected void actuallyHurt(ServerLevel p_406271_, DamageSource p_406248_, float p_406370_) {
        if (!this.canArmorAbsorb(p_406248_)) {
            super.actuallyHurt(p_406271_, p_406248_, p_406370_);
        } else {
            ItemStack itemstack = this.getBodyArmorItem();
            int i = itemstack.getDamageValue();
            int j = itemstack.getMaxDamage();
            itemstack.hurtAndBreak(Mth.ceil(p_406370_), this, EquipmentSlot.BODY);
            if (Crackiness.WOLF_ARMOR.byDamage(i, j) != Crackiness.WOLF_ARMOR.byDamage(this.getBodyArmorItem())) {
                this.playSound(SoundEvents.WOLF_ARMOR_CRACK);
                p_406271_.sendParticles(
                    new ItemParticleOption(ParticleTypes.ITEM, Items.ARMADILLO_SCUTE.getDefaultInstance()),
                    this.getX(),
                    this.getY() + 1.0,
                    this.getZ(),
                    20,
                    0.2,
                    0.1,
                    0.2,
                    0.1
                );
            }
        }
    }

    private boolean canArmorAbsorb(DamageSource damageSource) {
        return this.getBodyArmorItem().is(Items.WOLF_ARMOR) && !damageSource.is(DamageTypeTags.BYPASSES_WOLF_ARMOR);
    }

    @Override
    protected void applyTamingSideEffects() {
        if (this.isTame()) {
            this.getAttribute(Attributes.MAX_HEALTH).setBaseValue(40.0);
            this.setHealth(40.0F);
        } else {
            this.getAttribute(Attributes.MAX_HEALTH).setBaseValue(8.0);
        }
    }

    @Override
    protected void hurtArmor(DamageSource p_406334_, float p_406321_) {
        this.doHurtEquipment(p_406334_, p_406321_, EquipmentSlot.BODY);
    }

    @Override
    protected boolean canShearEquipment(Player p_426231_) {
        return this.isOwnedBy(p_426231_);
    }

    @Override
    public InteractionResult mobInteract(Player p_406380_, InteractionHand p_406261_) {
        ItemStack itemstack = p_406380_.getItemInHand(p_406261_);
        Item item = itemstack.getItem();
        if (this.isTame()) {
            if (this.isFood(itemstack) && this.getHealth() < this.getMaxHealth()) {
                FoodProperties foodproperties = itemstack.get(DataComponents.FOOD);
                float f = foodproperties != null ? foodproperties.nutrition() : 1.0F;
                this.heal(2.0F * f);
                this.usePlayerItem(p_406380_, p_406261_, itemstack);
                this.gameEvent(GameEvent.EAT); // Neo: add EAT game event
                return InteractionResult.SUCCESS;
            }

            if (!(item instanceof DyeItem dyeitem && this.isOwnedBy(p_406380_))) {
                if (this.isEquippableInSlot(itemstack, EquipmentSlot.BODY) && !this.isWearingBodyArmor() && this.isOwnedBy(p_406380_) && !this.isBaby()) {
                    this.setBodyArmorItem(itemstack.copyWithCount(1));
                    itemstack.consume(1, p_406380_);
                    return InteractionResult.SUCCESS;
                }

                if (this.isInSittingPose()
                    && this.isWearingBodyArmor()
                    && this.isOwnedBy(p_406380_)
                    && this.getBodyArmorItem().isDamaged()
                    && this.getBodyArmorItem().isValidRepairItem(itemstack)) {
                    itemstack.shrink(1);
                    this.playSound(SoundEvents.WOLF_ARMOR_REPAIR);
                    ItemStack itemstack1 = this.getBodyArmorItem();
                    int i = (int)(itemstack1.getMaxDamage() * 0.125F);
                    itemstack1.setDamageValue(Math.max(0, itemstack1.getDamageValue() - i));
                    return InteractionResult.SUCCESS;
                }

                InteractionResult interactionresult = super.mobInteract(p_406380_, p_406261_);
                if (!interactionresult.consumesAction() && this.isOwnedBy(p_406380_)) {
                    this.setOrderedToSit(!this.isOrderedToSit());
                    this.jumping = false;
                    this.navigation.stop();
                    this.setTarget(null);
                    return InteractionResult.SUCCESS.withoutItem();
                }

                return interactionresult;
            }

            DyeColor dyecolor = dyeitem.getDyeColor();
            if (dyecolor != this.getCollarColor()) {
                this.setCollarColor(dyecolor);
                itemstack.consume(1, p_406380_);
                return InteractionResult.SUCCESS;
            }
        } else if (!this.level().isClientSide() && itemstack.is(Items.BONE) && !this.isAngry()) {
            itemstack.consume(1, p_406380_);
            this.tryToTame(p_406380_);
            return InteractionResult.SUCCESS_SERVER;
        }

        return super.mobInteract(p_406380_, p_406261_);
    }

    private void tryToTame(Player player) {
        if (this.random.nextInt(3) == 0  && !net.neoforged.neoforge.event.EventHooks.onAnimalTame(this, player)) {
            this.tame(player);
            this.navigation.stop();
            this.setTarget(null);
            this.setOrderedToSit(true);
            this.level().broadcastEntityEvent(this, (byte)7);
        } else {
            this.level().broadcastEntityEvent(this, (byte)6);
        }
    }

    @Override
    public void handleEntityEvent(byte p_406350_) {
        if (p_406350_ == 8) {
            this.isShaking = true;
            this.shakeAnim = 0.0F;
            this.shakeAnimO = 0.0F;
        } else if (p_406350_ == 56) {
            this.cancelShake();
        } else {
            super.handleEntityEvent(p_406350_);
        }
    }

    public float getTailAngle() {
        if (this.isAngry()) {
            return 1.5393804F;
        } else if (this.isTame()) {
            float f = this.getMaxHealth();
            float f1 = (f - this.getHealth()) / f;
            return (0.55F - f1 * 0.4F) * (float) Math.PI;
        } else {
            return (float) (Math.PI / 5);
        }
    }

    @Override
    public boolean isFood(ItemStack p_406272_) {
        return p_406272_.is(ItemTags.WOLF_FOOD);
    }

    @Override
    public int getMaxSpawnClusterSize() {
        return 8;
    }

    @Override
    public long getPersistentAngerEndTime() {
        return this.entityData.get(DATA_ANGER_END_TIME);
    }

    @Override
    public void setPersistentAngerEndTime(long p_455794_) {
        this.entityData.set(DATA_ANGER_END_TIME, p_455794_);
    }

    @Override
    public void startPersistentAngerTimer() {
        this.setTimeToRemainAngry(PERSISTENT_ANGER_TIME.sample(this.random));
    }

    @Override
    public @Nullable EntityReference<LivingEntity> getPersistentAngerTarget() {
        return this.persistentAngerTarget;
    }

    @Override
    public void setPersistentAngerTarget(@Nullable EntityReference<LivingEntity> p_455947_) {
        this.persistentAngerTarget = p_455947_;
    }

    public DyeColor getCollarColor() {
        return DyeColor.byId(this.entityData.get(DATA_COLLAR_COLOR));
    }

    private void setCollarColor(DyeColor color) {
        this.entityData.set(DATA_COLLAR_COLOR, color.getId());
    }

    public @Nullable Wolf getBreedOffspring(ServerLevel p_406360_, AgeableMob p_406340_) {
        Wolf wolf = EntityType.WOLF.create(p_406360_, EntitySpawnReason.BREEDING);
        if (wolf != null && p_406340_ instanceof Wolf wolf1) {
            if (this.random.nextBoolean()) {
                wolf.setVariant(this.getVariant());
            } else {
                wolf.setVariant(wolf1.getVariant());
            }

            if (this.isTame()) {
                wolf.setOwnerReference(this.getOwnerReference());
                wolf.setTame(true, true);
                DyeColor dyecolor = this.getCollarColor();
                DyeColor dyecolor1 = wolf1.getCollarColor();
                wolf.setCollarColor(DyeColor.getMixedColor(p_406360_, dyecolor, dyecolor1));
            }

            wolf.setSoundVariant(WolfSoundVariants.pickRandomSoundVariant(this.registryAccess(), this.random));
        }

        return wolf;
    }

    public void setIsInterested(boolean isInterested) {
        this.entityData.set(DATA_INTERESTED_ID, isInterested);
    }

    @Override
    public boolean canMate(Animal p_406266_) {
        if (p_406266_ == this) {
            return false;
        } else if (!this.isTame()) {
            return false;
        } else if (!(p_406266_ instanceof Wolf wolf)) {
            return false;
        } else if (!wolf.isTame()) {
            return false;
        } else {
            return wolf.isInSittingPose() ? false : this.isInLove() && wolf.isInLove();
        }
    }

    public boolean isInterested() {
        return this.entityData.get(DATA_INTERESTED_ID);
    }

    @Override
    public boolean wantsToAttack(LivingEntity p_406214_, LivingEntity p_406223_) {
        if (p_406214_ instanceof Creeper || p_406214_ instanceof Ghast || p_406214_ instanceof ArmorStand) {
            return false;
        } else if (p_406214_ instanceof Wolf wolf) {
            return !wolf.isTame() || wolf.getOwner() != p_406223_;
        } else if (p_406214_ instanceof Player player && p_406223_ instanceof Player player1 && !player1.canHarmPlayer(player)) {
            return false;
        } else {
            return p_406214_ instanceof AbstractHorse abstracthorse && abstracthorse.isTamed()
                ? false
                : !(p_406214_ instanceof TamableAnimal tamableanimal && tamableanimal.isTame());
        }
    }

    @Override
    public boolean canBeLeashed() {
        return !this.isAngry();
    }

    @Override
    public Vec3 getLeashOffset() {
        return new Vec3(0.0, 0.6F * this.getEyeHeight(), this.getBbWidth() * 0.4F);
    }

    public static boolean checkWolfSpawnRules(
        EntityType<Wolf> entityType, LevelAccessor level, EntitySpawnReason spawnReason, BlockPos pos, RandomSource random
    ) {
        return level.getBlockState(pos.below()).is(BlockTags.WOLVES_SPAWNABLE_ON) && isBrightEnoughToSpawn(level, pos);
    }

    class WolfAvoidEntityGoal<T extends LivingEntity> extends AvoidEntityGoal<T> {
        private final Wolf wolf;

        public WolfAvoidEntityGoal(Wolf wolf, Class<T> avoidClass, float maxDistance, double walkSpeedModifier, double sprintSpeedModifier) {
            super(wolf, avoidClass, maxDistance, walkSpeedModifier, sprintSpeedModifier);
            this.wolf = wolf;
        }

        @Override
        public boolean canUse() {
            return super.canUse() && this.toAvoid instanceof Llama ? !this.wolf.isTame() && this.avoidLlama((Llama)this.toAvoid) : false;
        }

        private boolean avoidLlama(Llama llama) {
            return llama.getStrength() >= Wolf.this.random.nextInt(5);
        }

        @Override
        public void start() {
            Wolf.this.setTarget(null);
            super.start();
        }

        @Override
        public void tick() {
            Wolf.this.setTarget(null);
            super.tick();
        }
    }

    public static class WolfPackData extends AgeableMob.AgeableMobGroupData {
        public final Holder<WolfVariant> type;

        public WolfPackData(Holder<WolfVariant> type) {
            super(false);
            this.type = type;
        }
    }
}
