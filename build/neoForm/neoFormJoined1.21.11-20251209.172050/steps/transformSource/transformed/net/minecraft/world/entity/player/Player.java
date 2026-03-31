package net.minecraft.world.entity.player;

import com.google.common.base.MoreObjects;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.common.math.IntMath;
import com.mojang.authlib.GameProfile;
import com.mojang.datafixers.util.Either;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.function.Predicate;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.dialog.Dialog;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.permissions.PermissionSet;
import net.minecraft.server.permissions.Permissions;
import net.minecraft.server.players.NameAndId;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stat;
import net.minecraft.stats.Stats;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.tags.EntityTypeTags;
import net.minecraft.tags.FluidTags;
import net.minecraft.tags.ItemTags;
import net.minecraft.util.Mth;
import net.minecraft.util.Unit;
import net.minecraft.util.Util;
import net.minecraft.world.Container;
import net.minecraft.world.Difficulty;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemStackWithSlot;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.attribute.EnvironmentAttributes;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffectUtil;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Avatar;
import net.minecraft.world.entity.ContainerUser;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityEquipment;
import net.minecraft.world.entity.EntityReference;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.SlotAccess;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.equine.AbstractHorse;
import net.minecraft.world.entity.animal.nautilus.AbstractNautilus;
import net.minecraft.world.entity.animal.parrot.Parrot;
import net.minecraft.world.entity.boss.enderdragon.EnderDragonPart;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.warden.WardenSpawnTracker;
import net.minecraft.world.entity.projectile.FishingHook;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.ProjectileDeflection;
import net.minecraft.world.entity.vehicle.minecart.MinecartCommandBlock;
import net.minecraft.world.food.FoodData;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ClickAction;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.inventory.PlayerEnderChestContainer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemCooldowns;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.ProjectileWeaponItem;
import net.minecraft.world.item.component.BlocksAttacks;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.enchantment.EnchantmentEffectComponents;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.equipment.Equippable;
import net.minecraft.world.item.trading.MerchantOffers;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.CommandBlockEntity;
import net.minecraft.world.level.block.entity.ContainerOpenersCounter;
import net.minecraft.world.level.block.entity.JigsawBlockEntity;
import net.minecraft.world.level.block.entity.SignBlockEntity;
import net.minecraft.world.level.block.entity.StructureBlockEntity;
import net.minecraft.world.level.block.entity.TestBlockEntity;
import net.minecraft.world.level.block.entity.TestInstanceBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.pattern.BlockInWorld;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.gamerules.GameRules;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.scores.PlayerTeam;
import net.minecraft.world.scores.Team;
import org.jspecify.annotations.Nullable;

public abstract class Player extends Avatar implements ContainerUser, net.neoforged.neoforge.common.extensions.IPlayerExtension {
    public static final String PERSISTED_NBT_TAG = "PlayerPersisted";
    public static final int MAX_HEALTH = 20;
    public static final int SLEEP_DURATION = 100;
    public static final int WAKE_UP_DURATION = 10;
    public static final int ENDER_SLOT_OFFSET = 200;
    public static final int HELD_ITEM_SLOT = 499;
    public static final int CRAFTING_SLOT_OFFSET = 500;
    public static final float DEFAULT_BLOCK_INTERACTION_RANGE = 4.5F;
    public static final float DEFAULT_ENTITY_INTERACTION_RANGE = 3.0F;
    private static final int CURRENT_IMPULSE_CONTEXT_RESET_GRACE_TIME_TICKS = 40;
    private static final EntityDataAccessor<Float> DATA_PLAYER_ABSORPTION_ID = SynchedEntityData.defineId(Player.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Integer> DATA_SCORE_ID = SynchedEntityData.defineId(Player.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<OptionalInt> DATA_SHOULDER_PARROT_LEFT = SynchedEntityData.defineId(
        Player.class, EntityDataSerializers.OPTIONAL_UNSIGNED_INT
    );
    private static final EntityDataAccessor<OptionalInt> DATA_SHOULDER_PARROT_RIGHT = SynchedEntityData.defineId(
        Player.class, EntityDataSerializers.OPTIONAL_UNSIGNED_INT
    );
    private static final short DEFAULT_SLEEP_TIMER = 0;
    private static final float DEFAULT_EXPERIENCE_PROGRESS = 0.0F;
    private static final int DEFAULT_EXPERIENCE_LEVEL = 0;
    private static final int DEFAULT_TOTAL_EXPERIENCE = 0;
    private static final int NO_ENCHANTMENT_SEED = 0;
    private static final int DEFAULT_SELECTED_SLOT = 0;
    private static final int DEFAULT_SCORE = 0;
    private static final boolean DEFAULT_IGNORE_FALL_DAMAGE_FROM_CURRENT_IMPULSE = false;
    private static final int DEFAULT_CURRENT_IMPULSE_CONTEXT_RESET_GRACE_TIME = 0;
    public static final float CREATIVE_ENTITY_INTERACTION_RANGE_MODIFIER_VALUE = 2.0F;
    final Inventory inventory;
    protected PlayerEnderChestContainer enderChestInventory = new PlayerEnderChestContainer();
    public final InventoryMenu inventoryMenu;
    public AbstractContainerMenu containerMenu;
    protected FoodData foodData = new FoodData();
    protected int jumpTriggerTime;
    public int takeXpDelay;
    private int sleepCounter = 0;
    protected boolean wasUnderwater;
    private final Abilities abilities = new Abilities();
    public int experienceLevel = 0;
    public int totalExperience = 0;
    public float experienceProgress = 0.0F;
    protected int enchantmentSeed = 0;
    protected final float defaultFlySpeed = 0.02F;
    private int lastLevelUpTime;
    /**
     * The player's unique game profile
     */
    private final GameProfile gameProfile;
    private boolean reducedDebugInfo;
    private ItemStack lastItemInMainHand = ItemStack.EMPTY;
    private final ItemCooldowns cooldowns = this.createItemCooldowns();
    private Optional<GlobalPos> lastDeathLocation = Optional.empty();
    public @Nullable FishingHook fishing;
    protected float hurtDir;
    public @Nullable Vec3 currentImpulseImpactPos;
    public @Nullable Entity currentExplosionCause;
    private boolean ignoreFallDamageFromCurrentImpulse = false;
    private int currentImpulseContextResetGraceTime = 0;
    private final java.util.Collection<MutableComponent> prefixes = new java.util.LinkedList<>();
    private final java.util.Collection<MutableComponent> suffixes = new java.util.LinkedList<>();
    @Nullable
    private Pose forcedPose = null;

    public Player(Level level, GameProfile gameProfile) {
        super(EntityType.PLAYER, level);
        this.setUUID(gameProfile.id());
        this.gameProfile = gameProfile;
        this.inventory = new Inventory(this, this.equipment);
        this.inventoryMenu = new InventoryMenu(this.inventory, !level.isClientSide(), this);
        this.containerMenu = this.inventoryMenu;
    }

    @Override
    protected EntityEquipment createEquipment() {
        return new PlayerEquipment(this);
    }

    public boolean blockActionRestricted(Level level, BlockPos pos, GameType gameMode) {
        if (!gameMode.isBlockPlacingRestricted()) {
            return false;
        } else if (gameMode == GameType.SPECTATOR) {
            return true;
        } else if (this.mayBuild()) {
            return false;
        } else {
            ItemStack itemstack = this.getMainHandItem();
            return itemstack.isEmpty() || !itemstack.canBreakBlockInAdventureMode(new BlockInWorld(level, pos, false));
        }
    }

    public static AttributeSupplier.Builder createAttributes() {
        return LivingEntity.createLivingAttributes()
            .add(Attributes.ATTACK_DAMAGE, 1.0)
            .add(Attributes.MOVEMENT_SPEED, 0.1F)
            .add(Attributes.ATTACK_SPEED)
            .add(Attributes.LUCK)
            .add(Attributes.BLOCK_INTERACTION_RANGE, 4.5)
            .add(Attributes.ENTITY_INTERACTION_RANGE, 3.0)
            .add(Attributes.BLOCK_BREAK_SPEED)
            .add(Attributes.SUBMERGED_MINING_SPEED)
            .add(Attributes.SNEAKING_SPEED)
            .add(Attributes.MINING_EFFICIENCY)
            .add(Attributes.SWEEPING_DAMAGE_RATIO)
            .add(Attributes.WAYPOINT_TRANSMIT_RANGE, 6.0E7)
            .add(Attributes.WAYPOINT_RECEIVE_RANGE, 6.0E7)
            .add(net.neoforged.neoforge.common.NeoForgeMod.CREATIVE_FLIGHT);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder p_326117_) {
        super.defineSynchedData(p_326117_);
        p_326117_.define(DATA_PLAYER_ABSORPTION_ID, 0.0F);
        p_326117_.define(DATA_SCORE_ID, 0);
        p_326117_.define(DATA_SHOULDER_PARROT_LEFT, OptionalInt.empty());
        p_326117_.define(DATA_SHOULDER_PARROT_RIGHT, OptionalInt.empty());
    }

    @Override
    public void tick() {
        net.neoforged.neoforge.event.EventHooks.firePlayerTickPre(this);
        this.noPhysics = this.isSpectator();
        if (this.isSpectator() || this.isPassenger()) {
            this.setOnGround(false);
        }

        if (this.takeXpDelay > 0) {
            this.takeXpDelay--;
        }

        if (this.isSleeping()) {
            this.sleepCounter++;
            if (this.sleepCounter > 100) {
                this.sleepCounter = 100;
            }

            if (!this.level().isClientSide()
                && !net.neoforged.neoforge.event.EventHooks.canEntityContinueSleeping(this, !this.level().environmentAttributes().getValue(EnvironmentAttributes.BED_RULE, this.position()).canSleep(this.level()) ? BedSleepingProblem.OTHER_PROBLEM : null)) {
                this.stopSleepInBed(false, true);
            }
        } else if (this.sleepCounter > 0) {
            this.sleepCounter++;
            if (this.sleepCounter >= 110) {
                this.sleepCounter = 0;
            }
        }

        this.updateIsUnderwater();
        super.tick();
        int i = 29999999;
        double d0 = Mth.clamp(this.getX(), -2.9999999E7, 2.9999999E7);
        double d1 = Mth.clamp(this.getZ(), -2.9999999E7, 2.9999999E7);
        if (d0 != this.getX() || d1 != this.getZ()) {
            this.setPos(d0, this.getY(), d1);
        }

        this.attackStrengthTicker++;
        this.itemSwapTicker++;
        ItemStack itemstack = this.getMainHandItem();
        if (!ItemStack.matches(this.lastItemInMainHand, itemstack)) {
            if (!ItemStack.isSameItem(this.lastItemInMainHand, itemstack)) {
                this.resetAttackStrengthTicker();
            }

            this.lastItemInMainHand = itemstack.copy();
        }

        if (!this.isEyeInFluid(FluidTags.WATER) && this.isEquipped(Items.TURTLE_HELMET)) {
            this.turtleHelmetTick();
        }

        this.cooldowns.tick();
        this.updatePlayerPose();
        if (this.currentImpulseContextResetGraceTime > 0) {
            this.currentImpulseContextResetGraceTime--;
        }
        net.neoforged.neoforge.event.EventHooks.firePlayerTickPost(this);
    }

    @Override
    protected float getMaxHeadRotationRelativeToBody() {
        return this.isBlocking() ? 15.0F : super.getMaxHeadRotationRelativeToBody();
    }

    public boolean isSecondaryUseActive() {
        return this.isShiftKeyDown();
    }

    protected boolean wantsToStopRiding() {
        return this.isShiftKeyDown();
    }

    protected boolean isStayingOnGroundSurface() {
        return this.isShiftKeyDown();
    }

    protected boolean updateIsUnderwater() {
        this.wasUnderwater = this.getEyeInFluidType() != null && this.getEyeInFluidType().canSwim(this);
        return this.wasUnderwater;
    }

    @Override
    public void onAboveBubbleColumn(boolean p_368525_, BlockPos p_393868_) {
        if (!this.getAbilities().flying) {
            super.onAboveBubbleColumn(p_368525_, p_393868_);
        }
    }

    @Override
    public void onInsideBubbleColumn(boolean p_368607_) {
        if (!this.getAbilities().flying) {
            super.onInsideBubbleColumn(p_368607_);
        }
    }

    private void turtleHelmetTick() {
        this.addEffect(new MobEffectInstance(MobEffects.WATER_BREATHING, 200, 0, false, false, true));
    }

    private boolean isEquipped(Item item) {
        for (EquipmentSlot equipmentslot : EquipmentSlot.VALUES) {
            ItemStack itemstack = this.getItemBySlot(equipmentslot);
            Equippable equippable = itemstack.get(DataComponents.EQUIPPABLE);
            if (itemstack.is(item) && equippable != null && equippable.slot() == equipmentslot) {
                return true;
            }
        }

        return false;
    }

    protected ItemCooldowns createItemCooldowns() {
        return new ItemCooldowns();
    }

    protected void updatePlayerPose() {
        if (forcedPose != null) {
            this.setPose(forcedPose);
            return;
        }
        if (this.canPlayerFitWithinBlocksAndEntitiesWhen(Pose.SWIMMING)) {
            Pose pose = this.getDesiredPose();
            Pose pose1;
            if (this.isSpectator() || this.isPassenger() || this.canPlayerFitWithinBlocksAndEntitiesWhen(pose)) {
                pose1 = pose;
            } else if (this.canPlayerFitWithinBlocksAndEntitiesWhen(Pose.CROUCHING)) {
                pose1 = Pose.CROUCHING;
            } else {
                pose1 = Pose.SWIMMING;
            }

            this.setPose(pose1);
        }
    }

    private Pose getDesiredPose() {
        if (this.isSleeping()) {
            return Pose.SLEEPING;
        } else if (this.isSwimming()) {
            return Pose.SWIMMING;
        } else if (this.isFallFlying()) {
            return Pose.FALL_FLYING;
        } else if (this.isAutoSpinAttack()) {
            return Pose.SPIN_ATTACK;
        } else {
            return this.isShiftKeyDown() && !this.abilities.flying ? Pose.CROUCHING : Pose.STANDING;
        }
    }

    protected boolean canPlayerFitWithinBlocksAndEntitiesWhen(Pose pose) {
        return this.level().noCollision(this, this.getDimensions(pose).makeBoundingBox(this.position()).deflate(1.0E-7));
    }

    @Override
    protected SoundEvent getSwimSound() {
        return SoundEvents.PLAYER_SWIM;
    }

    @Override
    protected SoundEvent getSwimSplashSound() {
        return SoundEvents.PLAYER_SPLASH;
    }

    @Override
    protected SoundEvent getSwimHighSpeedSplashSound() {
        return SoundEvents.PLAYER_SPLASH_HIGH_SPEED;
    }

    @Override
    public int getDimensionChangingDelay() {
        return 10;
    }

    @Override
    public void playSound(SoundEvent sound, float volume, float pitch) {
        this.level().playSound(this, this.getX(), this.getY(), this.getZ(), sound, this.getSoundSource(), volume, pitch);
    }

    @Override
    public SoundSource getSoundSource() {
        return SoundSource.PLAYERS;
    }

    @Override
    protected int getFireImmuneTicks() {
        return 20;
    }

    @Override
    public void handleEntityEvent(byte p_36120_) {
        if (p_36120_ == 9) {
            this.completeUsingItem();
        } else if (p_36120_ == 23) {
            this.setReducedDebugInfo(false);
        } else if (p_36120_ == 22) {
            this.setReducedDebugInfo(true);
        } else {
            super.handleEntityEvent(p_36120_);
        }
    }

    public void closeContainer() {
        this.containerMenu = this.inventoryMenu;
    }

    protected void doCloseContainer() {
    }

    @Override
    public void rideTick() {
        if (!this.level().isClientSide() && this.wantsToStopRiding() && this.isPassenger()) {
            this.stopRiding();
            this.setShiftKeyDown(false);
        } else {
            super.rideTick();
        }
    }

    @Override
    public void aiStep() {
        if (this.jumpTriggerTime > 0) {
            this.jumpTriggerTime--;
        }

        this.tickRegeneration();
        this.inventory.tick();
        if (this.abilities.flying && !this.isPassenger()) {
            this.resetFallDistance();
        }

        super.aiStep();
        this.updateSwingTime();
        this.yHeadRot = this.getYRot();
        this.setSpeed((float)this.getAttributeValue(Attributes.MOVEMENT_SPEED));
        if (this.getHealth() > 0.0F && !this.isSpectator()) {
            AABB aabb;
            if (this.isPassenger() && !this.getVehicle().isRemoved()) {
                aabb = this.getBoundingBox().minmax(this.getVehicle().getBoundingBox()).inflate(1.0, 0.0, 1.0);
            } else {
                aabb = this.getBoundingBox().inflate(1.0, 0.5, 1.0);
            }

            List<Entity> list = this.level().getEntities(this, aabb);
            List<Entity> list1 = Lists.newArrayList();

            for (Entity entity : list) {
                if (entity.getType() == EntityType.EXPERIENCE_ORB) {
                    list1.add(entity);
                } else if (!entity.isRemoved()) {
                    this.touch(entity);
                }
            }

            if (!list1.isEmpty()) {
                this.touch(Util.getRandom(list1, this.random));
            }
        }

        this.handleShoulderEntities();
    }

    protected void tickRegeneration() {
    }

    public void handleShoulderEntities() {
    }

    protected void removeEntitiesOnShoulder() {
    }

    private void touch(Entity entity) {
        entity.playerTouch(this);
    }

    public int getScore() {
        return this.entityData.get(DATA_SCORE_ID);
    }

    /**
     * Set player's score
     */
    public void setScore(int score) {
        this.entityData.set(DATA_SCORE_ID, score);
    }

    /**
     * Add to player's score
     */
    public void increaseScore(int score) {
        int i = this.getScore();
        this.entityData.set(DATA_SCORE_ID, i + score);
    }

    public void startAutoSpinAttack(int ticks, float damage, ItemStack itemStack) {
        this.autoSpinAttackTicks = ticks;
        this.autoSpinAttackDmg = damage;
        this.autoSpinAttackItemStack = itemStack;
        if (!this.level().isClientSide()) {
            this.removeEntitiesOnShoulder();
            this.setLivingEntityFlag(4, true);
        }
    }

    @Override
    public ItemStack getWeaponItem() {
        return this.isAutoSpinAttack() && this.autoSpinAttackItemStack != null ? this.autoSpinAttackItemStack : super.getWeaponItem();
    }

    /**
     * Called when the mob's health reaches 0.
     */
    @Override
    public void die(DamageSource cause) {
        if (net.neoforged.neoforge.common.CommonHooks.onLivingDeath(this, cause)) return;
        super.die(cause);
        this.reapplyPosition();
        if (!this.isSpectator() && this.level() instanceof ServerLevel serverlevel) {
            this.dropAllDeathLoot(serverlevel, cause);
        }

        if (cause != null) {
            this.setDeltaMovement(
                -Mth.cos((this.getHurtDir() + this.getYRot()) * (float) (Math.PI / 180.0)) * 0.1F,
                0.1F,
                -Mth.sin((this.getHurtDir() + this.getYRot()) * (float) (Math.PI / 180.0)) * 0.1F
            );
        } else {
            this.setDeltaMovement(0.0, 0.1, 0.0);
        }

        this.awardStat(Stats.DEATHS);
        this.resetStat(Stats.CUSTOM.get(Stats.TIME_SINCE_DEATH));
        this.resetStat(Stats.CUSTOM.get(Stats.TIME_SINCE_REST));
        this.clearFire();
        this.setSharedFlagOnFire(false);
        this.setLastDeathLocation(Optional.of(GlobalPos.of(this.level().dimension(), this.blockPosition())));
    }

    @Override
    protected void dropEquipment(ServerLevel p_376325_) {
        super.dropEquipment(p_376325_);
        if (!p_376325_.getGameRules().get(GameRules.KEEP_INVENTORY)) {
            this.destroyVanishingCursedItems();
            this.inventory.dropAll();
        }
    }

    protected void destroyVanishingCursedItems() {
        for (int i = 0; i < this.inventory.getContainerSize(); i++) {
            ItemStack itemstack = this.inventory.getItem(i);
            if (!itemstack.isEmpty() && EnchantmentHelper.has(itemstack, EnchantmentEffectComponents.PREVENT_EQUIPMENT_DROP)) {
                this.inventory.removeItemNoUpdate(i);
            }
        }
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource damageSource) {
        return damageSource.type().effects().sound();
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.PLAYER_DEATH;
    }

    public void handleCreativeModeItemDrop(ItemStack stack) {
    }

    /**
     * Drops an item into the world.
     */
    public @Nullable ItemEntity drop(ItemStack itemStack, boolean includeThrowerName) {
        return net.neoforged.neoforge.common.CommonHooks.onPlayerTossEvent(this, itemStack, false, includeThrowerName);
    }

    /**
     * @deprecated Neo: {@link #getDestroySpeed(BlockState, BlockPos)} instead
     */
    @Deprecated
    public float getDestroySpeed(BlockState state) {
        return getDestroySpeed(state, null);
    }

    public float getDestroySpeed(BlockState state, @Nullable BlockPos pos) {
        float f = this.inventory.getSelectedItem().getDestroySpeed(state);
        if (f > 1.0F) {
            f += (float)this.getAttributeValue(Attributes.MINING_EFFICIENCY);
        }

        if (MobEffectUtil.hasDigSpeed(this)) {
            f *= 1.0F + (MobEffectUtil.getDigSpeedAmplification(this) + 1) * 0.2F;
        }

        if (this.hasEffect(MobEffects.MINING_FATIGUE)) {
            float f1 = switch (this.getEffect(MobEffects.MINING_FATIGUE).getAmplifier()) {
                case 0 -> 0.3F;
                case 1 -> 0.09F;
                case 2 -> 0.0027F;
                default -> 8.1E-4F;
            };
            f *= f1;
        }

        f *= (float)this.getAttributeValue(Attributes.BLOCK_BREAK_SPEED);
        if (this.isEyeInFluid(FluidTags.WATER)) {
            f *= (float)this.getAttribute(Attributes.SUBMERGED_MINING_SPEED).getValue();
        }

        if (!this.onGround()) {
            f /= 5.0F;
        }

        f = net.neoforged.neoforge.event.EventHooks.getBreakSpeed(this, state, f, pos);
        return f;
    }

    @Deprecated // Neo: use position sensitive version below
    public boolean hasCorrectToolForDrops(BlockState state) {
        return !state.requiresCorrectToolForDrops() || this.inventory.getSelectedItem().isCorrectToolForDrops(state);
    }

    public boolean hasCorrectToolForDrops(BlockState p_state, Level level, BlockPos pos) {
        return net.neoforged.neoforge.event.EventHooks.doPlayerHarvestCheck(this, p_state, level, pos);
    }

    @Override
    protected void readAdditionalSaveData(ValueInput p_422427_) {
        super.readAdditionalSaveData(p_422427_);
        this.setUUID(this.gameProfile.id());
        this.inventory.load(p_422427_.listOrEmpty("Inventory", ItemStackWithSlot.CODEC));
        this.inventory.setSelectedSlot(p_422427_.getIntOr("SelectedItemSlot", 0));
        this.sleepCounter = p_422427_.getShortOr("SleepTimer", (short)0);
        this.experienceProgress = p_422427_.getFloatOr("XpP", 0.0F);
        this.experienceLevel = p_422427_.getIntOr("XpLevel", 0);
        this.totalExperience = p_422427_.getIntOr("XpTotal", 0);
        this.enchantmentSeed = p_422427_.getIntOr("XpSeed", 0);
        if (this.enchantmentSeed == 0) {
            this.enchantmentSeed = this.random.nextInt();
        }

        this.setScore(p_422427_.getIntOr("Score", 0));
        this.foodData.readAdditionalSaveData(p_422427_);
        p_422427_.read("abilities", Abilities.Packed.CODEC).ifPresent(this.abilities::apply);
        this.getAttribute(Attributes.MOVEMENT_SPEED).setBaseValue(this.abilities.getWalkingSpeed());
        this.enderChestInventory.fromSlots(p_422427_.listOrEmpty("EnderItems", ItemStackWithSlot.CODEC));
        this.setLastDeathLocation(p_422427_.read("LastDeathLocation", GlobalPos.CODEC));
        this.currentImpulseImpactPos = p_422427_.read("current_explosion_impact_pos", Vec3.CODEC).orElse(null);
        this.ignoreFallDamageFromCurrentImpulse = p_422427_.getBooleanOr("ignore_fall_damage_from_current_explosion", false);
        this.currentImpulseContextResetGraceTime = p_422427_.getIntOr("current_impulse_context_reset_grace_time", 0);
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput p_421801_) {
        super.addAdditionalSaveData(p_421801_);
        NbtUtils.addCurrentDataVersion(p_421801_);
        this.inventory.save(p_421801_.list("Inventory", ItemStackWithSlot.CODEC));
        p_421801_.putInt("SelectedItemSlot", this.inventory.getSelectedSlot());
        p_421801_.putShort("SleepTimer", (short)this.sleepCounter);
        p_421801_.putFloat("XpP", this.experienceProgress);
        p_421801_.putInt("XpLevel", this.experienceLevel);
        p_421801_.putInt("XpTotal", this.totalExperience);
        p_421801_.putInt("XpSeed", this.enchantmentSeed);
        p_421801_.putInt("Score", this.getScore());
        this.foodData.addAdditionalSaveData(p_421801_);
        p_421801_.store("abilities", Abilities.Packed.CODEC, this.abilities.pack());
        this.enderChestInventory.storeAsSlots(p_421801_.list("EnderItems", ItemStackWithSlot.CODEC));
        this.lastDeathLocation.ifPresent(p_421397_ -> p_421801_.store("LastDeathLocation", GlobalPos.CODEC, p_421397_));
        p_421801_.storeNullable("current_explosion_impact_pos", Vec3.CODEC, this.currentImpulseImpactPos);
        p_421801_.putBoolean("ignore_fall_damage_from_current_explosion", this.ignoreFallDamageFromCurrentImpulse);
        p_421801_.putInt("current_impulse_context_reset_grace_time", this.currentImpulseContextResetGraceTime);
    }

    @Override
    public boolean isInvulnerableTo(ServerLevel p_376263_, DamageSource p_36249_) {
        if (super.isInvulnerableTo(p_376263_, p_36249_)) {
            return true;
        } else if (p_36249_.is(DamageTypeTags.IS_DROWNING)) {
            return !p_376263_.getGameRules().get(GameRules.DROWNING_DAMAGE);
        } else if (p_36249_.is(DamageTypeTags.IS_FALL)) {
            return !p_376263_.getGameRules().get(GameRules.FALL_DAMAGE);
        } else if (p_36249_.is(DamageTypeTags.IS_FIRE)) {
            return !p_376263_.getGameRules().get(GameRules.FIRE_DAMAGE);
        } else {
            return p_36249_.is(DamageTypeTags.IS_FREEZING) ? !p_376263_.getGameRules().get(GameRules.FREEZE_DAMAGE) : false;
        }
    }

    @Override
    public boolean hurtServer(ServerLevel p_376451_, DamageSource p_376171_, float p_376389_) {
        if (this.isInvulnerableTo(p_376451_, p_376171_)) {
            return false;
        } else if (this.abilities.invulnerable && !p_376171_.is(DamageTypeTags.BYPASSES_INVULNERABILITY)) {
            return false;
        } else {
            this.noActionTime = 0;
            if (this.isDeadOrDying()) {
                return false;
            } else {
                this.removeEntitiesOnShoulder();
                p_376389_ = Math.max(0.0F, p_376171_.type().scaling().getScalingFunction().scaleDamage(p_376171_, this, p_376389_, this.level().getDifficulty()));
                if (false && p_376171_.scalesWithDifficulty()) {
                    if (p_376451_.getDifficulty() == Difficulty.PEACEFUL) {
                        p_376389_ = 0.0F;
                    }

                    if (p_376451_.getDifficulty() == Difficulty.EASY) {
                        p_376389_ = Math.min(p_376389_ / 2.0F + 1.0F, p_376389_);
                    }

                    if (p_376451_.getDifficulty() == Difficulty.HARD) {
                        p_376389_ = p_376389_ * 3.0F / 2.0F;
                    }
                }

                return p_376389_ == 0.0F ? false : super.hurtServer(p_376451_, p_376171_, p_376389_);
            }
        }
    }

    @Override
    protected void blockUsingItem(ServerLevel p_400223_, LivingEntity p_399637_) {
        super.blockUsingItem(p_400223_, p_399637_);
        ItemStack itemstack = this.getItemBlockingWith();
        BlocksAttacks blocksattacks = itemstack != null ? itemstack.get(DataComponents.BLOCKS_ATTACKS) : null;
        float f = p_399637_.getSecondsToDisableBlocking();
        if (f > 0.0F && blocksattacks != null) {
            blocksattacks.disable(p_400223_, this, f, itemstack);
        }
    }

    @Override
    public boolean canBeSeenAsEnemy() {
        return !this.getAbilities().invulnerable && super.canBeSeenAsEnemy();
    }

    public boolean canHarmPlayer(Player other) {
        Team team = this.getTeam();
        Team team1 = other.getTeam();
        if (team == null) {
            return true;
        } else {
            return !team.isAlliedTo(team1) ? true : team.isAllowFriendlyFire();
        }
    }

    @Override
    protected void hurtArmor(DamageSource damageSource, float damage) {
        this.doHurtEquipment(damageSource, damage, EquipmentSlot.FEET, EquipmentSlot.LEGS, EquipmentSlot.CHEST, EquipmentSlot.HEAD);
    }

    @Override
    protected void hurtHelmet(DamageSource p_150103_, float p_150104_) {
        this.doHurtEquipment(p_150103_, p_150104_, EquipmentSlot.HEAD);
    }

    @Override
    protected void actuallyHurt(ServerLevel p_376500_, DamageSource p_36312_, float p_36313_) {
        if (!this.isInvulnerableTo(p_376500_, p_36312_)) {
            this.damageContainers.peek().setReduction(net.neoforged.neoforge.common.damagesource.DamageContainer.Reduction.ARMOR, this.damageContainers.peek().getNewDamage() - this.getDamageAfterArmorAbsorb(p_36312_, this.damageContainers.peek().getNewDamage()));
            this.getDamageAfterMagicAbsorb(p_36312_, this.damageContainers.peek().getNewDamage());
            float damage = net.neoforged.neoforge.common.CommonHooks.onLivingDamagePre(this, this.damageContainers.peek());
            this.damageContainers.peek().setReduction(net.neoforged.neoforge.common.damagesource.DamageContainer.Reduction.ABSORPTION, Math.min(this.getAbsorptionAmount(), damage));
            float absorbed = Math.min(damage, this.damageContainers.peek().getReduction(net.neoforged.neoforge.common.damagesource.DamageContainer.Reduction.ABSORPTION));
            this.setAbsorptionAmount(Math.max(0, this.getAbsorptionAmount() - absorbed));
            float f1 = this.damageContainers.peek().getNewDamage();
            float f = absorbed;
            if (f > 0.0F && f < 3.4028235E37F) {
                this.awardStat(Stats.DAMAGE_ABSORBED, Math.round(f * 10.0F));
            }

            if (f1 != 0.0F) {
                this.causeFoodExhaustion(p_36312_.getFoodExhaustion());
                this.getCombatTracker().recordDamage(p_36312_, f1);
                this.setHealth(this.getHealth() - f1);
                if (f1 < 3.4028235E37F) {
                    this.awardStat(Stats.DAMAGE_TAKEN, Math.round(f1 * 10.0F));
                }

                this.gameEvent(GameEvent.ENTITY_DAMAGE);
                this.onDamageTaken(this.damageContainers.peek());
            }
            net.neoforged.neoforge.common.CommonHooks.onLivingDamagePost(this, this.damageContainers.peek());
        }
    }

    public boolean isTextFilteringEnabled() {
        return false;
    }

    public void openTextEdit(SignBlockEntity signEntity, boolean isFrontText) {
    }

    public void openMinecartCommandBlock(MinecartCommandBlock commandBlock) {
    }

    public void openCommandBlock(CommandBlockEntity commandBlockEntity) {
    }

    public void openStructureBlock(StructureBlockEntity structureEntity) {
    }

    public void openTestBlock(TestBlockEntity testBlockEntity) {
    }

    public void openTestInstanceBlock(TestInstanceBlockEntity testInstanceBlockEntity) {
    }

    public void openJigsawBlock(JigsawBlockEntity jigsawBlockEntity) {
    }

    public void openHorseInventory(AbstractHorse horse, Container inventory) {
    }

    public void openNautilusInventory(AbstractNautilus nautilus, Container inventory) {
    }

    public OptionalInt openMenu(@Nullable MenuProvider menu) {
        return OptionalInt.empty();
    }

    public void openDialog(Holder<Dialog> dialog) {
    }

    public void sendMerchantOffers(int containerId, MerchantOffers offers, int villagerLevel, int villagerXp, boolean showProgress, boolean canRestock) {
    }

    public void openItemGui(ItemStack stack, InteractionHand hand) {
    }

    public InteractionResult interactOn(Entity entityToInteractOn, InteractionHand hand) {
        if (this.isSpectator()) {
            if (entityToInteractOn instanceof MenuProvider) {
                this.openMenu((MenuProvider)entityToInteractOn);
            }

            return InteractionResult.PASS;
        } else {
            InteractionResult cancelResult = net.neoforged.neoforge.common.CommonHooks.onInteractEntity(this, entityToInteractOn, hand);
            if (cancelResult != null) return cancelResult;
            ItemStack itemstack = this.getItemInHand(hand);
            ItemStack itemstack1 = itemstack.copy();
            InteractionResult interactionresult = entityToInteractOn.interact(this, hand);
            if (interactionresult.consumesAction()) {
                if (this.hasInfiniteMaterials() && itemstack == this.getItemInHand(hand) && itemstack.getCount() < itemstack1.getCount()) {
                    itemstack.setCount(itemstack1.getCount());
                }

                if (!this.abilities.instabuild && itemstack.isEmpty()) {
                    net.neoforged.neoforge.event.EventHooks.onPlayerDestroyItem(this, itemstack1, hand);
                }
                return interactionresult;
            } else {
                if (!itemstack.isEmpty() && entityToInteractOn instanceof LivingEntity) {
                    if (this.hasInfiniteMaterials()) {
                        itemstack = itemstack1;
                    }

                    InteractionResult interactionresult1 = itemstack.interactLivingEntity(this, (LivingEntity)entityToInteractOn, hand);
                    if (interactionresult1.consumesAction()) {
                        this.level().gameEvent(GameEvent.ENTITY_INTERACT, entityToInteractOn.position(), GameEvent.Context.of(this));
                        if (itemstack.isEmpty() && !this.hasInfiniteMaterials()) {
                            net.neoforged.neoforge.event.EventHooks.onPlayerDestroyItem(this, itemstack1, hand);
                            this.setItemInHand(hand, ItemStack.EMPTY);
                        }

                        return interactionresult1;
                    }
                }

                return InteractionResult.PASS;
            }
        }
    }

    @Override
    public void removeVehicle() {
        super.removeVehicle();
        this.boardingCooldown = 0;
    }

    @Override
    protected boolean isImmobile() {
        return super.isImmobile() || this.isSleeping();
    }

    @Override
    public boolean isAffectedByFluids() {
        return !this.abilities.flying;
    }

    @Override
    // Forge: Don't update this method to use IForgeEntity#getStepHeight() - https://github.com/MinecraftForge/MinecraftForge/issues/8922
    protected Vec3 maybeBackOffFromEdge(Vec3 vec, MoverType mover) {
        float f = this.maxUpStep();
        if (!this.abilities.flying
            && !(vec.y > 0.0)
            && (mover == MoverType.SELF || mover == MoverType.PLAYER)
            && this.isStayingOnGroundSurface()
            && this.isAboveGround(f)) {
            double d0 = vec.x;
            double d1 = vec.z;
            double d2 = 0.05;
            double d3 = Math.signum(d0) * 0.05;

            double d4;
            for (d4 = Math.signum(d1) * 0.05; d0 != 0.0 && this.canFallAtLeast(d0, 0.0, f); d0 -= d3) {
                if (Math.abs(d0) <= 0.05) {
                    d0 = 0.0;
                    break;
                }
            }

            while (d1 != 0.0 && this.canFallAtLeast(0.0, d1, f)) {
                if (Math.abs(d1) <= 0.05) {
                    d1 = 0.0;
                    break;
                }

                d1 -= d4;
            }

            while (d0 != 0.0 && d1 != 0.0 && this.canFallAtLeast(d0, d1, f)) {
                if (Math.abs(d0) <= 0.05) {
                    d0 = 0.0;
                } else {
                    d0 -= d3;
                }

                if (Math.abs(d1) <= 0.05) {
                    d1 = 0.0;
                } else {
                    d1 -= d4;
                }
            }

            return new Vec3(d0, vec.y, d1);
        } else {
            return vec;
        }
    }

    // Forge: Don't update this method to use IForgeEntity#getStepHeight() - https://github.com/MinecraftForge/MinecraftForge/issues/9376
    private boolean isAboveGround(float maxUpStep) {
        return this.onGround() || this.fallDistance < maxUpStep && !this.canFallAtLeast(0.0, 0.0, maxUpStep - this.fallDistance);
    }

    private boolean canFallAtLeast(double x, double z, double distance) {
        AABB aabb = this.getBoundingBox();
        return this.level()
            .noCollision(
                this,
                new AABB(
                    aabb.minX + 1.0E-7 + x,
                    aabb.minY - distance - 1.0E-7,
                    aabb.minZ + 1.0E-7 + z,
                    aabb.maxX - 1.0E-7 + x,
                    aabb.minY,
                    aabb.maxZ - 1.0E-7 + z
                )
            );
    }

    /**
     * Attacks for the player the targeted entity with the currently equipped item.  The equipped item has hitEntity called on it. Args: targetEntity
     */
    public void attack(Entity target) {
        if (!net.neoforged.neoforge.common.CommonHooks.onPlayerAttackTarget(this, target)) return;
        if (!this.cannotAttack(target)) {
            float f = this.isAutoSpinAttack() ? this.autoSpinAttackDmg : (float)this.getAttributeValue(Attributes.ATTACK_DAMAGE);
            ItemStack itemstack = this.getWeaponItem();
            DamageSource damagesource = this.createAttackSource(itemstack);
            float f1 = this.getAttackStrengthScale(0.5F);
            float f2 = f1 * (this.getEnchantedDamage(target, f, damagesource) - f);
            f *= this.baseDamageScaleFactor();
            this.onAttack();
            if (!this.deflectProjectile(target)) {
                if (f > 0.0F || f2 > 0.0F) {
                    boolean flag = f1 > 0.9F;
                    boolean flag1;
                    if (this.isSprinting() && flag) {
                        this.playServerSideSound(SoundEvents.PLAYER_ATTACK_KNOCKBACK);
                        flag1 = true;
                    } else {
                        flag1 = false;
                    }

                    f += itemstack.getItem().getAttackDamageBonus(target, f, damagesource);
                    boolean flag2 = flag && this.canCriticalAttack(target);
                    // Neo: Fire the critical hit event and override the critical hit status and damage multiplier based on the event.
                    // The boolean local above (flag2) is the vanilla critical hit result.
                    var critEvent = net.neoforged.neoforge.common.CommonHooks.fireCriticalHit(this, target, flag2, flag2 ? 1.5F : 1.0F);
                    flag2 = critEvent.isCriticalHit();
                    if (flag2) {
                        f *= critEvent.getDamageMultiplier();
                    }

                    float f3 = f + f2;
                    // Neo: Replace !flag2 (!isCriticalHit) with the logic from the CriticalHitEvent.
                    boolean flag3 = this.isSweepAttack(flag, critEvent.isCriticalHit() && critEvent.disableSweep(), flag1);

                    // Neo: Fire the SweepAttackEvent and overwrite the value of flag3 (the local controlling if a sweep will occur).
                    var sweepEvent = net.neoforged.neoforge.common.CommonHooks.fireSweepAttack(this, target, flag3);
                    flag3 = sweepEvent.isSweeping();

                    float f4 = 0.0F;
                    if (target instanceof LivingEntity livingentity) {
                        f4 = livingentity.getHealth();
                    }

                    Vec3 vec3 = target.getDeltaMovement();
                    boolean flag4 = target.hurtOrSimulate(damagesource, f3);
                    if (flag4) {
                        this.causeExtraKnockback(target, this.getKnockback(target, damagesource) + (flag1 ? 0.5F : 0.0F), vec3);
                        if (flag3) {
                            var sweepHitBox = itemstack.getSweepHitBox(this, target);
                            this.doSweepAttack(target, f, damagesource, f1, sweepHitBox);
                        }

                        this.attackVisualEffects(target, flag2, flag3, flag, false, f2);
                        this.setLastHurtMob(target);
                        this.itemAttackInteraction(target, itemstack, damagesource, true);
                        this.damageStatsAndHearts(target, f4);
                        this.causeFoodExhaustion(0.1F);
                    } else {
                        this.playServerSideSound(SoundEvents.PLAYER_ATTACK_NODAMAGE);
                    }
                }

                this.lungeForwardMaybe();
            }
        }
    }

    private void playServerSideSound(SoundEvent sound) {
        this.level().playSound(null, this.getX(), this.getY(), this.getZ(), sound, this.getSoundSource(), 1.0F, 1.0F);
    }

    private DamageSource createAttackSource(ItemStack stack) {
        return stack.getDamageSource(this, () -> this.damageSources().playerAttack(this));
    }

    private boolean cannotAttack(Entity entity) {
        return !entity.isAttackable() ? true : entity.skipAttackInteraction(this);
    }

    private boolean deflectProjectile(Entity entity) {
        if (entity.getType().is(EntityTypeTags.REDIRECTABLE_PROJECTILE)
            && entity instanceof Projectile projectile
            && projectile.deflect(ProjectileDeflection.AIM_DEFLECT, this, EntityReference.of(this), true)) {
            this.level().playSound(null, this.getX(), this.getY(), this.getZ(), SoundEvents.PLAYER_ATTACK_NODAMAGE, this.getSoundSource());
            return true;
        } else {
            return false;
        }
    }

    private boolean canCriticalAttack(Entity entity) {
        return this.fallDistance > 0.0
            && !this.onGround()
            && !this.onClimbable()
            && !this.isInWater()
            && !this.isMobilityRestricted()
            && !this.isPassenger()
            && entity instanceof LivingEntity
            && !this.isSprinting();
    }

    private boolean isSweepAttack(boolean fullyCharged, boolean crit, boolean sprinting) {
        if (fullyCharged && !crit && !sprinting && this.onGround()) {
            double d0 = this.getKnownMovement().horizontalDistanceSqr();
            double d1 = this.getSpeed() * 2.5;
            if (d0 < Mth.square(d1)) {
                // Neo: Make sweep attacks check SWORD_SWEEP instead of ItemTags.SWORDS.
                return this.getItemInHand(InteractionHand.MAIN_HAND).canPerformAction(net.neoforged.neoforge.common.ItemAbilities.SWORD_SWEEP);
            }
        }

        return false;
    }

    private void attackVisualEffects(Entity target, boolean crit, boolean sweepAttack, boolean fullyCharged, boolean isStabAttack, float enchantedDamage) {
        if (crit) {
            this.playServerSideSound(SoundEvents.PLAYER_ATTACK_CRIT);
            this.crit(target);
        }

        if (!crit && !sweepAttack && !isStabAttack) {
            this.playServerSideSound(fullyCharged ? SoundEvents.PLAYER_ATTACK_STRONG : SoundEvents.PLAYER_ATTACK_WEAK);
        }

        if (enchantedDamage > 0.0F) {
            this.magicCrit(target);
        }
    }

    private void damageStatsAndHearts(Entity entity, float healthBeforeAttack) {
        if (entity instanceof LivingEntity) {
            float f = healthBeforeAttack - ((LivingEntity)entity).getHealth();
            this.awardStat(Stats.DAMAGE_DEALT, Math.round(f * 10.0F));
            if (this.level() instanceof ServerLevel && f > 2.0F) {
                int i = (int)(f * 0.5);
                ((ServerLevel)this.level())
                    .sendParticles(ParticleTypes.DAMAGE_INDICATOR, entity.getX(), entity.getY(0.5), entity.getZ(), i, 0.1, 0.0, 0.1, 0.2);
            }
        }
    }

    private void itemAttackInteraction(Entity target, ItemStack stack, DamageSource damageSource, boolean doPostAttack) {
        Entity entity = target;
        if (target instanceof net.neoforged.neoforge.entity.PartEntity) {
            entity = ((net.neoforged.neoforge.entity.PartEntity<?>) target).getParent();
        }

        boolean flag = false;
        ItemStack copy = stack.copy();
        if (this.level() instanceof ServerLevel serverlevel) {
            if (entity instanceof LivingEntity livingentity) {
                flag = stack.hurtEnemy(livingentity, this);
            }

            if (doPostAttack) {
                EnchantmentHelper.doPostAttackEffectsWithItemSource(serverlevel, target, damageSource, stack);
            }
        }

        if (!this.level().isClientSide() && !stack.isEmpty() && entity instanceof LivingEntity) {
            if (flag) {
                stack.postHurtEnemy((LivingEntity)entity, this);
            }

            if (stack.isEmpty()) {
                net.neoforged.neoforge.event.EventHooks.onPlayerDestroyItem(this, copy, stack == this.getMainHandItem() ? InteractionHand.MAIN_HAND : InteractionHand.OFF_HAND);
                if (stack == this.getMainHandItem()) {
                    this.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
                } else {
                    this.setItemInHand(InteractionHand.OFF_HAND, ItemStack.EMPTY);
                }
            }
        }
    }

    @Override
    public void causeExtraKnockback(Entity p_455908_, float p_454748_, Vec3 p_454874_) {
        if (p_454748_ > 0.0F) {
            if (p_455908_ instanceof LivingEntity livingentity) {
                livingentity.knockback(p_454748_, Mth.sin(this.getYRot() * (float) (Math.PI / 180.0)), -Mth.cos(this.getYRot() * (float) (Math.PI / 180.0)));
            } else {
                p_455908_.push(
                    -Mth.sin(this.getYRot() * (float) (Math.PI / 180.0)) * p_454748_, 0.1, Mth.cos(this.getYRot() * (float) (Math.PI / 180.0)) * p_454748_
                );
            }

            this.setDeltaMovement(this.getDeltaMovement().multiply(0.6, 1.0, 0.6));
            this.setSprinting(false);
        }

        if (p_455908_ instanceof ServerPlayer && p_455908_.hurtMarked) {
            ((ServerPlayer)p_455908_).connection.send(new ClientboundSetEntityMotionPacket(p_455908_));
            p_455908_.hurtMarked = false;
            p_455908_.setDeltaMovement(p_454874_);
        }
    }

    @Override
    public float getVoicePitch() {
        return 1.0F;
    }

    // Neo: Use version that allows the sweep hitbox to be specified
    @Deprecated
    private void doSweepAttack(Entity entity, float damage, DamageSource damageSource, float attackStrengthScale) {
        doSweepAttack(entity, damage, damageSource, attackStrengthScale, entity.getBoundingBox().inflate(1.0, 0.25, 1.0));
    }

    private void doSweepAttack(Entity entity, float damage, DamageSource damageSource, float attackStrengthScale, AABB sweepHitBox) {
        this.playServerSideSound(SoundEvents.PLAYER_ATTACK_SWEEP);
        if (this.level() instanceof ServerLevel serverlevel) {
            float f = 1.0F + (float)this.getAttributeValue(Attributes.SWEEPING_DAMAGE_RATIO) * damage;

            double entityReachSq = Mth.square(this.entityInteractionRange()); // Use entity reach instead of constant 9.0. Vanilla uses bottom center-to-center checks here, so don't update this to use canReach, since it uses closest-corner checks.
            for (LivingEntity livingentity : this.level().getEntitiesOfClass(LivingEntity.class, sweepHitBox)) {
                if (livingentity != this
                    && livingentity != entity
                    && !this.isAlliedTo(livingentity)
                    && !(livingentity instanceof ArmorStand armorstand && armorstand.isMarker())
                    && this.distanceToSqr(livingentity) < entityReachSq) {
                    float f1 = this.getEnchantedDamage(livingentity, f, damageSource) * attackStrengthScale;
                    if (livingentity.hurtServer(serverlevel, damageSource, f1)) {
                        livingentity.knockback(0.4F, Mth.sin(this.getYRot() * (float) (Math.PI / 180.0)), -Mth.cos(this.getYRot() * (float) (Math.PI / 180.0)));
                        EnchantmentHelper.doPostAttackEffects(serverlevel, livingentity, damageSource);
                    }
                }
            }

            double d0 = -Mth.sin(this.getYRot() * (float) (Math.PI / 180.0));
            double d1 = Mth.cos(this.getYRot() * (float) (Math.PI / 180.0));
            serverlevel.sendParticles(ParticleTypes.SWEEP_ATTACK, this.getX() + d0, this.getY(0.5), this.getZ() + d1, 0, d0, 0.0, d1, 0.0);
        }
    }

    protected float getEnchantedDamage(Entity entity, float damage, DamageSource damageSource) {
        return damage;
    }

    @Override
    protected void doAutoAttackOnTouch(LivingEntity p_36355_) {
        this.attack(p_36355_);
    }

    /**
     * Called when the entity is dealt a critical hit.
     */
    public void crit(Entity entityHit) {
    }

    private float baseDamageScaleFactor() {
        float f = this.getAttackStrengthScale(0.5F);
        return 0.2F + f * f * 0.8F;
    }

    @Override
    public boolean stabAttack(EquipmentSlot p_454961_, Entity p_455450_, float p_454993_, boolean p_455515_, boolean p_455235_, boolean p_454651_) {
        if (this.cannotAttack(p_455450_)) {
            return false;
        } else {
            ItemStack itemstack = this.getItemBySlot(p_454961_);
            DamageSource damagesource = this.createAttackSource(itemstack);
            float f = this.getEnchantedDamage(p_455450_, p_454993_, damagesource) - p_454993_;
            if (!this.isUsingItem() || this.getUsedItemHand().asEquipmentSlot() != p_454961_) {
                f *= this.getAttackStrengthScale(0.5F);
                p_454993_ *= this.baseDamageScaleFactor();
            }

            if (p_455235_ && this.deflectProjectile(p_455450_)) {
                return true;
            } else {
                float f1 = p_455515_ ? p_454993_ + f : 0.0F;
                float f2 = 0.0F;
                if (p_455450_ instanceof LivingEntity livingentity) {
                    f2 = livingentity.getHealth();
                }

                Vec3 vec3 = p_455450_.getDeltaMovement();
                boolean flag = p_455515_ && p_455450_.hurtOrSimulate(damagesource, f1);
                if (p_455235_) {
                    this.causeExtraKnockback(p_455450_, 0.4F + this.getKnockback(p_455450_, damagesource), vec3);
                }

                boolean flag1 = false;
                if (p_454651_ && p_455450_.isPassenger()) {
                    flag1 = true;
                    p_455450_.stopRiding();
                }

                if (!flag && !p_455235_ && !flag1) {
                    return false;
                } else {
                    this.attackVisualEffects(p_455450_, false, false, p_455515_, true, f);
                    this.setLastHurtMob(p_455450_);
                    this.itemAttackInteraction(p_455450_, itemstack, damagesource, flag);
                    this.damageStatsAndHearts(p_455450_, f2);
                    this.causeFoodExhaustion(0.1F);
                    return true;
                }
            }
        }
    }

    public void magicCrit(Entity entityHit) {
    }

    @Override
    public void remove(Entity.RemovalReason p_150097_) {
        super.remove(p_150097_);
        this.inventoryMenu.removed(this);
        if (this.hasContainerOpen()) {
            this.doCloseContainer();
        }
    }

    @Override
    public boolean isClientAuthoritative() {
        return true;
    }

    @Override
    protected boolean isLocalClientAuthoritative() {
        return this.isLocalPlayer();
    }

    public boolean isLocalPlayer() {
        return false;
    }

    @Override
    public boolean canSimulateMovement() {
        return !this.level().isClientSide() || this.isLocalPlayer();
    }

    @Override
    public boolean isEffectiveAi() {
        return !this.level().isClientSide() || this.isLocalPlayer();
    }

    public GameProfile getGameProfile() {
        return this.gameProfile;
    }

    public NameAndId nameAndId() {
        return new NameAndId(this.gameProfile);
    }

    public Inventory getInventory() {
        return this.inventory;
    }

    public Abilities getAbilities() {
        return this.abilities;
    }

    @Override
    public boolean hasInfiniteMaterials() {
        return this.abilities.instabuild;
    }

    public boolean preventsBlockDrops() {
        return this.abilities.instabuild;
    }

    public void updateTutorialInventoryAction(ItemStack carried, ItemStack clicked, ClickAction action) {
    }

    public boolean hasContainerOpen() {
        return this.containerMenu != this.inventoryMenu;
    }

    public boolean canDropItems() {
        return true;
    }

    public Either<Player.BedSleepingProblem, Unit> startSleepInBed(BlockPos bedPos) {
        this.startSleeping(bedPos);
        this.sleepCounter = 0;
        return Either.right(Unit.INSTANCE);
    }

    public void stopSleepInBed(boolean wakeImmediately, boolean updateLevelForSleepingPlayers) {
        net.neoforged.neoforge.event.EventHooks.onPlayerWakeup(this, wakeImmediately, updateLevelForSleepingPlayers);
        super.stopSleeping();
        if (this.level() instanceof ServerLevel && updateLevelForSleepingPlayers) {
            ((ServerLevel)this.level()).updateSleepingPlayerList();
        }

        this.sleepCounter = wakeImmediately ? 0 : 100;
    }

    @Override
    public void stopSleeping() {
        this.stopSleepInBed(true, true);
    }

    public boolean isSleepingLongEnough() {
        return this.isSleeping() && this.sleepCounter >= 100;
    }

    public int getSleepTimer() {
        return this.sleepCounter;
    }

    public void displayClientMessage(Component chatComponent, boolean actionBar) {
    }

    public void awardStat(Identifier statKey) {
        this.awardStat(Stats.CUSTOM.get(statKey));
    }

    public void awardStat(Identifier stat, int increment) {
        this.awardStat(Stats.CUSTOM.get(stat), increment);
    }

    /**
     * Add a stat once
     */
    public void awardStat(Stat<?> stat) {
        this.awardStat(stat, 1);
    }

    /**
     * Adds a value to a statistic field.
     */
    public void awardStat(Stat<?> stat, int increment) {
    }

    public void resetStat(Stat<?> stat) {
    }

    public int awardRecipes(Collection<RecipeHolder<?>> recipes) {
        return 0;
    }

    public void triggerRecipeCrafted(RecipeHolder<?> recipe, List<ItemStack> items) {
    }

    public void awardRecipesByKey(List<ResourceKey<Recipe<?>>> recipes) {
    }

    public int resetRecipes(Collection<RecipeHolder<?>> recipes) {
        return 0;
    }

    @Override
    public void travel(Vec3 travelVector) {
        if (this.isPassenger()) {
            super.travel(travelVector);
        } else {
            if (this.isSwimming()) {
                double d0 = this.getLookAngle().y;
                double d1 = d0 < -0.2 ? 0.085 : 0.06;
                if (d0 <= 0.0 || this.jumping || !this.level().getFluidState(BlockPos.containing(this.getX(), this.getY() + 1.0 - 0.1, this.getZ())).isEmpty()) {
                    Vec3 vec3 = this.getDeltaMovement();
                    this.setDeltaMovement(vec3.add(0.0, (d0 - vec3.y) * d1, 0.0));
                }
            }

            if (this.getAbilities().flying) {
                double d2 = this.getDeltaMovement().y;
                super.travel(travelVector);
                this.setDeltaMovement(this.getDeltaMovement().with(Direction.Axis.Y, d2 * 0.6));
            } else {
                super.travel(travelVector);
            }
        }
    }

    @Override
    protected boolean canGlide() {
        return !this.abilities.flying && super.canGlide();
    }

    @Override
    public void updateSwimming() {
        if (this.abilities.flying) {
            this.setSwimming(false);
        } else {
            super.updateSwimming();
        }
    }

    protected boolean freeAt(BlockPos pos) {
        return !this.level().getBlockState(pos).isSuffocating(this.level(), pos);
    }

    @Override
    public float getSpeed() {
        return (float)this.getAttributeValue(Attributes.MOVEMENT_SPEED);
    }

    @Override
    public boolean causeFallDamage(double p_397701_, float p_150093_, DamageSource p_150095_) {
        if (this.mayFly()) {
            net.neoforged.neoforge.event.EventHooks.onPlayerFall(this, p_150093_, p_150093_);
            return false;
        } else {
            if (p_397701_ >= 2.0) {
                this.awardStat(Stats.FALL_ONE_CM, (int)Math.round(p_397701_ * 100.0));
            }

            boolean flag = this.currentImpulseImpactPos != null && this.ignoreFallDamageFromCurrentImpulse;
            double d0;
            if (flag) {
                d0 = Math.min(p_397701_, this.currentImpulseImpactPos.y - this.getY());
                boolean flag1 = d0 <= 0.0;
                if (flag1) {
                    this.resetCurrentImpulseContext();
                } else {
                    this.tryResetCurrentImpulseContext();
                }
            } else {
                d0 = p_397701_;
            }

            if (d0 > 0.0 && super.causeFallDamage(d0, p_150093_, p_150095_)) {
                this.resetCurrentImpulseContext();
                return true;
            } else {
                this.propagateFallToPassengers(p_397701_, p_150093_, p_150095_);
                return false;
            }
        }
    }

    public boolean tryToStartFallFlying() {
        if (!this.isFallFlying() && this.canGlide() && !this.isInWater()) {
            this.startFallFlying();
            return true;
        } else {
            return false;
        }
    }

    public void startFallFlying() {
        this.setSharedFlag(7, true);
    }

    @Override
    protected void doWaterSplashEffect() {
        if (!this.isSpectator()) {
            super.doWaterSplashEffect();
        }
    }

    @Override
    protected void playStepSound(BlockPos p_282121_, BlockState p_282194_) {
        if (this.isInWater()) {
            this.waterSwimSound();
            this.playMuffledStepSound(p_282194_, p_282121_);
        } else {
            BlockPos blockpos = this.getPrimaryStepSoundBlockPos(p_282121_);
            if (!p_282121_.equals(blockpos)) {
                BlockState blockstate = this.level().getBlockState(blockpos);
                if (blockstate.is(BlockTags.COMBINATION_STEP_SOUND_BLOCKS)) {
                    this.playCombinationStepSounds(blockstate, p_282194_, blockpos, p_282121_);
                } else {
                    super.playStepSound(blockpos, blockstate);
                }
            } else {
                super.playStepSound(p_282121_, p_282194_);
            }
        }
    }

    @Override
    public LivingEntity.Fallsounds getFallSounds() {
        return new LivingEntity.Fallsounds(SoundEvents.PLAYER_SMALL_FALL, SoundEvents.PLAYER_BIG_FALL);
    }

    @Override
    public boolean killedEntity(ServerLevel p_219735_, LivingEntity p_219736_, DamageSource p_435371_) {
        this.awardStat(Stats.ENTITY_KILLED.get(p_219736_.getType()));
        return true;
    }

    @Override
    public void makeStuckInBlock(BlockState state, Vec3 motionMultiplier) {
        if (!this.abilities.flying) {
            super.makeStuckInBlock(state, motionMultiplier);
        }

        this.tryResetCurrentImpulseContext();
    }

    public void giveExperiencePoints(int xpPoints) {
        net.neoforged.neoforge.event.entity.player.PlayerXpEvent.XpChange event = new net.neoforged.neoforge.event.entity.player.PlayerXpEvent.XpChange(this, xpPoints);
        if (net.neoforged.neoforge.common.NeoForge.EVENT_BUS.post(event).isCanceled()) return;
        xpPoints = event.getAmount();

        this.increaseScore(xpPoints);
        this.experienceProgress = this.experienceProgress + (float)xpPoints / this.getXpNeededForNextLevel();
        this.totalExperience = Mth.clamp(this.totalExperience + xpPoints, 0, Integer.MAX_VALUE);

        while (this.experienceProgress < 0.0F) {
            float f = this.experienceProgress * this.getXpNeededForNextLevel();
            if (this.experienceLevel > 0) {
                this.giveExperienceLevels(-1);
                this.experienceProgress = 1.0F + f / this.getXpNeededForNextLevel();
            } else {
                this.giveExperienceLevels(-1);
                this.experienceProgress = 0.0F;
            }
        }

        while (this.experienceProgress >= 1.0F) {
            this.experienceProgress = (this.experienceProgress - 1.0F) * this.getXpNeededForNextLevel();
            this.giveExperienceLevels(1);
            this.experienceProgress = this.experienceProgress / this.getXpNeededForNextLevel();
        }
    }

    public int getEnchantmentSeed() {
        return this.enchantmentSeed;
    }

    public void onEnchantmentPerformed(ItemStack enchantedItem, int levelCost) {
        giveExperienceLevels(-levelCost);
        if (this.experienceLevel < 0) {
            this.experienceLevel = 0;
            this.experienceProgress = 0.0F;
            this.totalExperience = 0;
        }

        this.enchantmentSeed = this.random.nextInt();
    }

    /**
     * Add experience levels to this player.
     */
    public void giveExperienceLevels(int levels) {
        net.neoforged.neoforge.event.entity.player.PlayerXpEvent.LevelChange event = new net.neoforged.neoforge.event.entity.player.PlayerXpEvent.LevelChange(this, levels);
        if (net.neoforged.neoforge.common.NeoForge.EVENT_BUS.post(event).isCanceled()) return;
        levels = event.getLevels();

        this.experienceLevel = IntMath.saturatedAdd(this.experienceLevel, levels);
        if (this.experienceLevel < 0) {
            this.experienceLevel = 0;
            this.experienceProgress = 0.0F;
            this.totalExperience = 0;
        }

        if (levels > 0 && this.experienceLevel % 5 == 0 && this.lastLevelUpTime < this.tickCount - 100.0F) {
            float f = this.experienceLevel > 30 ? 1.0F : this.experienceLevel / 30.0F;
            this.level().playSound(null, this.getX(), this.getY(), this.getZ(), SoundEvents.PLAYER_LEVELUP, this.getSoundSource(), f * 0.75F, 1.0F);
            this.lastLevelUpTime = this.tickCount;
        }
    }

    public int getXpNeededForNextLevel() {
        if (this.experienceLevel >= 30) {
            return 112 + (this.experienceLevel - 30) * 9;
        } else {
            return this.experienceLevel >= 15 ? 37 + (this.experienceLevel - 15) * 5 : 7 + this.experienceLevel * 2;
        }
    }

    /**
     * Increases exhaustion level by the supplied amount.
     */
    public void causeFoodExhaustion(float exhaustion) {
        if (!this.abilities.invulnerable) {
            if (!this.level().isClientSide()) {
                this.foodData.addExhaustion(exhaustion);
            }
        }
    }

    @Override
    public void lungeForwardMaybe() {
        if (this.hasEnoughFoodToDoExhaustiveManoeuvres()) {
            super.lungeForwardMaybe();
        }
    }

    protected boolean hasEnoughFoodToDoExhaustiveManoeuvres() {
        return this.getFoodData().hasEnoughFood() || this.mayFly();
    }

    public Optional<WardenSpawnTracker> getWardenSpawnTracker() {
        return Optional.empty();
    }

    public FoodData getFoodData() {
        return this.foodData;
    }

    public boolean canEat(boolean canAlwaysEat) {
        return this.abilities.invulnerable || canAlwaysEat || this.foodData.needsFood();
    }

    public boolean isHurt() {
        return this.getHealth() > 0.0F && this.getHealth() < this.getMaxHealth();
    }

    public boolean mayBuild() {
        return this.abilities.mayBuild;
    }

    /**
     * Returns whether this player can modify the block at a certain location with the given stack.
     * <p>
     * The position being queried is {@code pos.offset(facing.getOpposite())}.
     *
     * @return Whether this player may modify the queried location in the current world
     * @see ItemStack#canPlaceOn(Block)
     * @see ItemStack#canEditBlocks()
     * @see PlayerCapabilities#allowEdit
     */
    public boolean mayUseItemAt(BlockPos pos, Direction facing, ItemStack stack) {
        if (this.abilities.mayBuild) {
            return true;
        } else {
            BlockPos blockpos = pos.relative(facing.getOpposite());
            BlockInWorld blockinworld = new BlockInWorld(this.level(), blockpos, false);
            return stack.canPlaceOnBlockInAdventureMode(blockinworld);
        }
    }

    @Override
    protected int getBaseExperienceReward(ServerLevel p_376359_) {
        return !p_376359_.getGameRules().get(GameRules.KEEP_INVENTORY) && !this.isSpectator() ? Math.min(this.experienceLevel * 7, 100) : 0;
    }

    @Override
    protected boolean isAlwaysExperienceDropper() {
        return true;
    }

    @Override
    public boolean shouldShowName() {
        return true;
    }

    @Override
    protected Entity.MovementEmission getMovementEmission() {
        return this.abilities.flying || this.onGround() && this.isDiscrete() ? Entity.MovementEmission.NONE : Entity.MovementEmission.ALL;
    }

    public void onUpdateAbilities() {
    }

    @Override
    public Component getName() {
        return Component.literal(this.gameProfile.name());
    }

    @Override
    public String getPlainTextName() {
        return this.gameProfile.name();
    }

    public PlayerEnderChestContainer getEnderChestInventory() {
        return this.enderChestInventory;
    }

    @Override
    protected boolean doesEmitEquipEvent(EquipmentSlot p_219741_) {
        return p_219741_.getType() == EquipmentSlot.Type.HUMANOID_ARMOR;
    }

    public boolean addItem(ItemStack stack) {
        return this.inventory.add(stack);
    }

    public abstract @Nullable GameType gameMode();

    @Override
    public boolean isSpectator() {
        return this.gameMode() == GameType.SPECTATOR;
    }

    @Override
    public boolean canBeHitByProjectile() {
        return !this.isSpectator() && super.canBeHitByProjectile();
    }

    @Override
    public boolean isSwimming() {
        return !this.abilities.flying && !this.isSpectator() && super.isSwimming();
    }

    public boolean isCreative() {
        return this.gameMode() == GameType.CREATIVE;
    }

    @Override
    public boolean isPushedByFluid() {
        return !this.abilities.flying;
    }

    @Override
    public Component getDisplayName() {
        if (this.displayname == null) this.displayname = net.neoforged.neoforge.event.EventHooks.getPlayerDisplayName(this, this.getName());
        MutableComponent mutablecomponent = Component.literal("");
        mutablecomponent = prefixes.stream().reduce(mutablecomponent, MutableComponent::append);
        mutablecomponent = mutablecomponent.append(PlayerTeam.formatNameForTeam(this.getTeam(), this.displayname));
        mutablecomponent = suffixes.stream().reduce(mutablecomponent, MutableComponent::append);
        return this.decorateDisplayNameComponent(mutablecomponent);
    }

    private MutableComponent decorateDisplayNameComponent(MutableComponent displayName) {
        String s = this.getGameProfile().name();
        return displayName.withStyle(
            p_427131_ -> p_427131_.withClickEvent(new ClickEvent.SuggestCommand("/tell " + s + " ")).withHoverEvent(this.createHoverEvent()).withInsertion(s)
        );
    }

    @Override
    public String getScoreboardName() {
        return this.getGameProfile().name();
    }

    @Override
    protected void internalSetAbsorptionAmount(float p_296426_) {
        this.getEntityData().set(DATA_PLAYER_ABSORPTION_ID, p_296426_);
    }

    @Override
    public float getAbsorptionAmount() {
        return this.getEntityData().get(DATA_PLAYER_ABSORPTION_ID);
    }

    @Override
    public @Nullable SlotAccess getSlot(int p_150112_) {
        if (p_150112_ == 499) {
            return new SlotAccess() {
                @Override
                public ItemStack get() {
                    return Player.this.containerMenu.getCarried();
                }

                @Override
                public boolean set(ItemStack p_332675_) {
                    Player.this.containerMenu.setCarried(p_332675_);
                    return true;
                }
            };
        } else {
            final int i = p_150112_ - 500;
            if (i >= 0 && i < 4) {
                return new SlotAccess() {
                    @Override
                    public ItemStack get() {
                        return Player.this.inventoryMenu.getCraftSlots().getItem(i);
                    }

                    @Override
                    public boolean set(ItemStack p_332810_) {
                        Player.this.inventoryMenu.getCraftSlots().setItem(i, p_332810_);
                        Player.this.inventoryMenu.slotsChanged(Player.this.inventory);
                        return true;
                    }
                };
            } else if (p_150112_ >= 0 && p_150112_ < this.inventory.getNonEquipmentItems().size()) {
                return this.inventory.getSlot(p_150112_);
            } else {
                int j = p_150112_ - 200;
                return j >= 0 && j < this.enderChestInventory.getContainerSize() ? this.enderChestInventory.getSlot(j) : super.getSlot(p_150112_);
            }
        }
    }

    public boolean isReducedDebugInfo() {
        return this.reducedDebugInfo;
    }

    public void setReducedDebugInfo(boolean reducedDebugInfo) {
        this.reducedDebugInfo = reducedDebugInfo;
    }

    @Override
    public void setRemainingFireTicks(int ticks) {
        super.setRemainingFireTicks(this.abilities.invulnerable ? Math.min(ticks, 1) : ticks);
    }

    protected static Optional<Parrot.Variant> extractParrotVariant(CompoundTag tag) {
        if (!tag.isEmpty()) {
            EntityType<?> entitytype = tag.read("id", EntityType.CODEC).orElse(null);
            if (entitytype == EntityType.PARROT) {
                return tag.read("Variant", Parrot.Variant.LEGACY_CODEC);
            }
        }

        return Optional.empty();
    }

    protected static OptionalInt convertParrotVariant(Optional<Parrot.Variant> variant) {
        return variant.<OptionalInt>map(p_477881_ -> OptionalInt.of(p_477881_.getId())).orElse(OptionalInt.empty());
    }

    private static Optional<Parrot.Variant> convertParrotVariant(OptionalInt id) {
        return id.isPresent() ? Optional.of(Parrot.Variant.byId(id.getAsInt())) : Optional.empty();
    }

    public void setShoulderParrotLeft(Optional<Parrot.Variant> variant) {
        this.entityData.set(DATA_SHOULDER_PARROT_LEFT, convertParrotVariant(variant));
    }

    public Optional<Parrot.Variant> getShoulderParrotLeft() {
        return convertParrotVariant(this.entityData.get(DATA_SHOULDER_PARROT_LEFT));
    }

    public void setShoulderParrotRight(Optional<Parrot.Variant> variant) {
        this.entityData.set(DATA_SHOULDER_PARROT_RIGHT, convertParrotVariant(variant));
    }

    public Optional<Parrot.Variant> getShoulderParrotRight() {
        return convertParrotVariant(this.entityData.get(DATA_SHOULDER_PARROT_RIGHT));
    }

    public float getCurrentItemAttackStrengthDelay() {
        return (float)(1.0 / this.getAttributeValue(Attributes.ATTACK_SPEED) * 20.0);
    }

    public boolean cannotAttackWithItem(ItemStack stack, int adjustTicks) {
        float f = stack.getOrDefault(DataComponents.MINIMUM_ATTACK_CHARGE, 0.0F);
        float f1 = (this.attackStrengthTicker + adjustTicks) / this.getCurrentItemAttackStrengthDelay();
        return f > 0.0F && f1 < f;
    }

    /**
     * Returns the percentage of attack power available based on the cooldown (zero to one).
     */
    public float getAttackStrengthScale(float adjustTicks) {
        return Mth.clamp((this.attackStrengthTicker + adjustTicks) / this.getCurrentItemAttackStrengthDelay(), 0.0F, 1.0F);
    }

    public float getItemSwapScale(float adjustTicks) {
        return Mth.clamp((this.itemSwapTicker + adjustTicks) / this.getCurrentItemAttackStrengthDelay(), 0.0F, 1.0F);
    }

    public void resetAttackStrengthTicker() {
        this.attackStrengthTicker = 0;
        this.itemSwapTicker = 0;
    }

    @Override
    public void onAttack() {
        this.resetOnlyAttackStrengthTicker();
        super.onAttack();
    }

    public void resetOnlyAttackStrengthTicker() {
        this.attackStrengthTicker = 0;
    }

    public ItemCooldowns getCooldowns() {
        return this.cooldowns;
    }

    @Override
    protected float getBlockSpeedFactor() {
        return !this.abilities.flying && !this.isFallFlying() ? super.getBlockSpeedFactor() : 1.0F;
    }

    @Override
    public float getLuck() {
        return (float)this.getAttributeValue(Attributes.LUCK);
    }

    public boolean canUseGameMasterBlocks() {
        return this.abilities.instabuild && this.permissions().hasPermission(Permissions.COMMANDS_GAMEMASTER);
    }

    public PermissionSet permissions() {
        return PermissionSet.NO_PERMISSIONS;
    }

    @Override
    public ImmutableList<Pose> getDismountPoses() {
        return ImmutableList.of(Pose.STANDING, Pose.CROUCHING, Pose.SWIMMING);
    }

    @Override
    public ItemStack getProjectile(ItemStack shootable) {
        if (!(shootable.getItem() instanceof ProjectileWeaponItem)) {
            return ItemStack.EMPTY;
        } else {
            Predicate<ItemStack> predicate = ((ProjectileWeaponItem)shootable.getItem()).getSupportedHeldProjectiles(shootable);
            ItemStack itemstack = ProjectileWeaponItem.getHeldProjectile(this, predicate);
            if (!itemstack.isEmpty()) {
                return net.neoforged.neoforge.common.CommonHooks.getProjectile(this, shootable, itemstack);
            } else {
                predicate = ((ProjectileWeaponItem)shootable.getItem()).getAllSupportedProjectiles(shootable);

                for (int i = 0; i < this.inventory.getContainerSize(); i++) {
                    ItemStack itemstack1 = this.inventory.getItem(i);
                    if (predicate.test(itemstack1)) {
                        return net.neoforged.neoforge.common.CommonHooks.getProjectile(this, shootable, itemstack1);
                    }
                }

                return net.neoforged.neoforge.common.CommonHooks.getProjectile(this, shootable, this.abilities.instabuild ? ((ProjectileWeaponItem)shootable.getItem()).getDefaultCreativeAmmo(this, shootable) : ItemStack.EMPTY);
            }
        }
    }

    @Override
    public Vec3 getRopeHoldPosition(float partialTicks) {
        double d0 = 0.22 * (this.getMainArm() == HumanoidArm.RIGHT ? -1.0 : 1.0);
        float f = Mth.lerp(partialTicks * 0.5F, this.getXRot(), this.xRotO) * (float) (Math.PI / 180.0);
        float f1 = Mth.lerp(partialTicks, this.yBodyRotO, this.yBodyRot) * (float) (Math.PI / 180.0);
        if (this.isFallFlying() || this.isAutoSpinAttack()) {
            Vec3 vec31 = this.getViewVector(partialTicks);
            Vec3 vec3 = this.getDeltaMovement();
            double d6 = vec3.horizontalDistanceSqr();
            double d3 = vec31.horizontalDistanceSqr();
            float f2;
            if (d6 > 0.0 && d3 > 0.0) {
                double d4 = (vec3.x * vec31.x + vec3.z * vec31.z) / Math.sqrt(d6 * d3);
                double d5 = vec3.x * vec31.z - vec3.z * vec31.x;
                f2 = (float)(Math.signum(d5) * Math.acos(d4));
            } else {
                f2 = 0.0F;
            }

            return this.getPosition(partialTicks).add(new Vec3(d0, -0.11, 0.85).zRot(-f2).xRot(-f).yRot(-f1));
        } else if (this.isVisuallySwimming()) {
            return this.getPosition(partialTicks).add(new Vec3(d0, 0.2, -0.15).xRot(-f).yRot(-f1));
        } else {
            double d1 = this.getBoundingBox().getYsize() - 1.0;
            double d2 = this.isCrouching() ? -0.2 : 0.07;
            return this.getPosition(partialTicks).add(new Vec3(d0, d1, d2).yRot(-f1));
        }
    }

    @Override
    public boolean isAlwaysTicking() {
        return true;
    }

    public boolean isScoping() {
        return this.isUsingItem() && this.getUseItem().canPerformAction(net.neoforged.neoforge.common.ItemAbilities.SPYGLASS_SCOPE);
    }

    @Override
    public boolean shouldBeSaved() {
        return false;
    }

    public Optional<GlobalPos> getLastDeathLocation() {
        return this.lastDeathLocation;
    }

    public void setLastDeathLocation(Optional<GlobalPos> lastDeathLocation) {
        this.lastDeathLocation = lastDeathLocation;
    }

    @Override
    public float getHurtDir() {
        return this.hurtDir;
    }

    @Override
    public void animateHurt(float p_265280_) {
        super.animateHurt(p_265280_);
        this.hurtDir = p_265280_;
    }

    public boolean isMobilityRestricted() {
        return this.hasEffect(MobEffects.BLINDNESS);
    }

    @Override
    public boolean canSprint() {
        return true;
    }

    @Override
    protected float getFlyingSpeed() {
        if (this.abilities.flying && !this.isPassenger()) {
            return this.isSprinting() ? this.abilities.getFlyingSpeed() * 2.0F : this.abilities.getFlyingSpeed();
        } else {
            return this.isSprinting() ? 0.025999999F : 0.02F;
        }
    }

    @Override
    public boolean hasContainerOpen(ContainerOpenersCounter p_434660_, BlockPos p_432903_) {
        return p_434660_.isOwnContainer(this);
    }

    @Override
    public double getContainerInteractionRange() {
        return this.blockInteractionRange();
    }

    public double blockInteractionRange() {
        return this.getAttributeValue(Attributes.BLOCK_INTERACTION_RANGE);
    }

    public double entityInteractionRange() {
        return this.getAttributeValue(Attributes.ENTITY_INTERACTION_RANGE);
    }

    public boolean isWithinEntityInteractionRange(Entity entity, double range) {
        return entity.isRemoved() ? false : this.isWithinEntityInteractionRange(entity.getBoundingBox(), range);
    }

    public boolean isWithinEntityInteractionRange(AABB box, double range) {
        double d0 = this.entityInteractionRange() + range;
        double d1 = box.distanceToSqr(this.getEyePosition());
        return d1 < d0 * d0;
    }

    public boolean isWithinAttackRange(AABB box, double range) {
        return this.entityAttackRange().isInRange(this, box, range);
    }

    public boolean isWithinBlockInteractionRange(BlockPos pos, double range) {
        double d0 = this.blockInteractionRange() + range;
        return new AABB(pos).distanceToSqr(this.getEyePosition()) < d0 * d0;
    }

    public void setIgnoreFallDamageFromCurrentImpulse(boolean ignoreFallDamageFromCurrentImpulse) {
        this.ignoreFallDamageFromCurrentImpulse = ignoreFallDamageFromCurrentImpulse;
        if (ignoreFallDamageFromCurrentImpulse) {
            this.applyPostImpulseGraceTime(40);
        } else {
            this.currentImpulseContextResetGraceTime = 0;
        }
    }

    public void applyPostImpulseGraceTime(int ticks) {
        this.currentImpulseContextResetGraceTime = Math.max(this.currentImpulseContextResetGraceTime, ticks);
    }

    public boolean isIgnoringFallDamageFromCurrentImpulse() {
        return this.ignoreFallDamageFromCurrentImpulse;
    }

    public void tryResetCurrentImpulseContext() {
        if (this.currentImpulseContextResetGraceTime == 0) {
            this.resetCurrentImpulseContext();
        }
    }

    public boolean isInPostImpulseGraceTime() {
        return this.currentImpulseContextResetGraceTime > 0;
    }

    public void resetCurrentImpulseContext() {
        this.currentImpulseContextResetGraceTime = 0;
        this.currentExplosionCause = null;
        this.currentImpulseImpactPos = null;
        this.ignoreFallDamageFromCurrentImpulse = false;
    }

    public boolean shouldRotateWithMinecart() {
        return false;
    }

    @Override
    public boolean onClimbable() {
        return this.abilities.flying ? false : super.onClimbable();
    }

    public String debugInfo() {
        return MoreObjects.toStringHelper(this)
            .add("name", this.getPlainTextName())
            .add("id", this.getId())
            .add("pos", this.position())
            .add("mode", this.gameMode())
            .add("permission", this.permissions())
            .toString();
    }

    public record BedSleepingProblem(@Nullable Component message) {
        public static final Player.BedSleepingProblem TOO_FAR_AWAY = new Player.BedSleepingProblem(Component.translatable("block.minecraft.bed.too_far_away"));
        public static final Player.BedSleepingProblem OBSTRUCTED = new Player.BedSleepingProblem(Component.translatable("block.minecraft.bed.obstructed"));
        public static final Player.BedSleepingProblem OTHER_PROBLEM = new Player.BedSleepingProblem(null);
        public static final Player.BedSleepingProblem NOT_SAFE = new Player.BedSleepingProblem(Component.translatable("block.minecraft.bed.not_safe"));
    }

    // Neo: Getters for the Player's name prefixes and suffixes
    public Collection<MutableComponent> getPrefixes() {
         return this.prefixes;
    }

    public Collection<MutableComponent> getSuffixes() {
         return this.suffixes;
    }

    private Component displayname = null;

    /**
     * Neo: Force the displayed name to refresh, by firing {@link net.neoforged.neoforge.event.entity.player.PlayerEvent.NameFormat}, using the real player name as event parameter.
     */
    public void refreshDisplayName() {
        this.displayname = net.neoforged.neoforge.event.EventHooks.getPlayerDisplayName(this, this.getName());
    }

    /**
     * Neo: Force a pose for the player. If set, the vanilla pose determination and clearance check is skipped. Make sure the pose is clear yourself (e.g. in PlayerTick).
     * This has to be set just once, do not set it every tick.
     * Make sure to clear (null) the pose if not required anymore and only use if necessary.
     */
    public void setForcedPose(@Nullable Pose pose) {
        this.forcedPose = pose;
    }

    /**
     * Neo:
     * @return The forced pose if set, null otherwise
     */
    @Nullable
    public Pose getForcedPose() {
        return this.forcedPose;
    }
}
