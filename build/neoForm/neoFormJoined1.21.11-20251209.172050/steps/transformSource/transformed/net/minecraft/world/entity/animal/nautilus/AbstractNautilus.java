package net.minecraft.world.entity.animal.nautilus;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.FluidTags;
import net.minecraft.tags.ItemTags;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Container;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.HasCustomInventoryScreen;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.PlayerRideableJumping;
import net.minecraft.world.entity.SlotAccess;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.control.SmoothSwimmingLookControl;
import net.minecraft.world.entity.ai.control.SmoothSwimmingMoveControl;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.ai.navigation.WaterBoundPathNavigation;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.inventory.AbstractMountInventoryMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUtils;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.equipment.Equippable;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.pathfinder.PathType;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

public abstract class AbstractNautilus extends TamableAnimal implements HasCustomInventoryScreen, PlayerRideableJumping {
    public static final int INVENTORY_SLOT_OFFSET = 500;
    public static final int INVENTORY_ROWS = 3;
    public static final int SMALL_RESTRICTION_RADIUS = 16;
    public static final int LARGE_RESTRICTION_RADIUS = 32;
    public static final int RESTRICTION_RADIUS_BUFFER = 8;
    private static final int EFFECT_DURATION = 60;
    private static final int EFFECT_REFRESH_RATE = 40;
    private static final double NAUTILUS_WATER_RESISTANCE = 0.9;
    private static final float IN_WATER_SPEED_MODIFIER = 0.011F;
    private static final float RIDDEN_SPEED_MODIFIER_IN_WATER = 0.0325F;
    private static final float RIDDEN_SPEED_MODIFIER_ON_LAND = 0.02F;
    private static final EntityDataAccessor<Boolean> DASH = SynchedEntityData.defineId(AbstractNautilus.class, EntityDataSerializers.BOOLEAN);
    private static final int DASH_COOLDOWN_TICKS = 40;
    private static final int DASH_MINIMUM_DURATION_TICKS = 5;
    private static final float DASH_MOMENTUM_IN_WATER = 1.2F;
    private static final float DASH_MOMENTUM_ON_LAND = 0.5F;
    private int dashCooldown = 0;
    protected float playerJumpPendingScale;
    protected SimpleContainer inventory;
    private static final double BUBBLE_SPREAD_FACTOR = 0.8;
    private static final double BUBBLE_DIRECTION_SCALE = 1.1;
    private static final double BUBBLE_Y_OFFSET = 0.25;
    private static final double BUBBLE_PROBABILITY_MULTIPLIER = 2.0;
    private static final float BUBBLE_PROBABILITY_MIN = 0.15F;
    private static final float BUBBLE_PROBABILITY_MAX = 1.0F;

    protected AbstractNautilus(EntityType<? extends AbstractNautilus> p_455598_, Level p_454853_) {
        super(p_455598_, p_454853_);
        this.moveControl = new SmoothSwimmingMoveControl(this, 85, 10, 0.011F, 0.0F, true);
        this.lookControl = new SmoothSwimmingLookControl(this, 10);
        this.setPathfindingMalus(PathType.WATER, 0.0F);
        this.createInventory();
    }

    @Override
    public boolean isFood(ItemStack p_455355_) {
        return !this.isTame() && !this.isBaby() ? p_455355_.is(ItemTags.NAUTILUS_TAMING_ITEMS) : p_455355_.is(ItemTags.NAUTILUS_FOOD);
    }

    @Override
    protected void usePlayerItem(Player p_455137_, InteractionHand p_455733_, ItemStack p_456229_) {
        if (p_456229_.is(ItemTags.NAUTILUS_BUCKET_FOOD)) {
            p_455137_.setItemInHand(p_455733_, ItemUtils.createFilledResult(p_456229_, p_455137_, new ItemStack(Items.WATER_BUCKET)));
        } else {
            super.usePlayerItem(p_455137_, p_455733_, p_456229_);
        }
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Animal.createAnimalAttributes()
            .add(Attributes.MAX_HEALTH, 15.0)
            .add(Attributes.MOVEMENT_SPEED, 1.0)
            .add(Attributes.ATTACK_DAMAGE, 3.0)
            .add(Attributes.KNOCKBACK_RESISTANCE, 0.3F);
    }

    @Override
    public boolean isPushedByFluid() {
        return false;
    }

    @Override
    protected PathNavigation createNavigation(Level p_456160_) {
        return new WaterBoundPathNavigation(this, p_456160_);
    }

    @Override
    public float getWalkTargetValue(BlockPos p_455080_, LevelReader p_455086_) {
        return 0.0F;
    }

    public static boolean checkNautilusSpawnRules(
        EntityType<? extends AbstractNautilus> entityType, LevelAccessor level, EntitySpawnReason spawnReason, BlockPos pos, RandomSource random
    ) {
        int i = level.getSeaLevel();
        int j = i - 25;
        return pos.getY() >= j
            && pos.getY() <= i - 5
            && level.getFluidState(pos.below()).is(FluidTags.WATER)
            && level.getBlockState(pos.above()).is(Blocks.WATER);
    }

    @Override
    public boolean checkSpawnObstruction(LevelReader p_454797_) {
        return p_454797_.isUnobstructed(this);
    }

    @Override
    public boolean canUseSlot(EquipmentSlot p_454817_) {
        return p_454817_ != EquipmentSlot.SADDLE && p_454817_ != EquipmentSlot.BODY
            ? super.canUseSlot(p_454817_)
            : this.isAlive() && !this.isBaby() && this.isTame();
    }

    @Override
    protected boolean canDispenserEquipIntoSlot(EquipmentSlot p_482363_) {
        return p_482363_ == EquipmentSlot.BODY || p_482363_ == EquipmentSlot.SADDLE || super.canDispenserEquipIntoSlot(p_482363_);
    }

    @Override
    protected boolean canAddPassenger(Entity p_456039_) {
        return !this.isVehicle();
    }

    @Override
    public @Nullable LivingEntity getControllingPassenger() {
        return (LivingEntity)(this.isSaddled() && this.getFirstPassenger() instanceof Player player ? player : super.getControllingPassenger());
    }

    @Override
    protected Vec3 getRiddenInput(Player p_455696_, Vec3 p_454886_) {
        float f = p_455696_.xxa;
        float f1 = 0.0F;
        float f2 = 0.0F;
        if (p_455696_.zza != 0.0F) {
            float f3 = Mth.cos(p_455696_.getXRot() * (float) (Math.PI / 180.0));
            float f4 = -Mth.sin(p_455696_.getXRot() * (float) (Math.PI / 180.0));
            if (p_455696_.zza < 0.0F) {
                f3 *= -0.5F;
                f4 *= -0.5F;
            }

            f2 = f4;
            f1 = f3;
        }

        return new Vec3(f, f2, f1);
    }

    protected Vec2 getRiddenRotation(LivingEntity entity) {
        return new Vec2(entity.getXRot() * 0.5F, entity.getYRot());
    }

    @Override
    protected void tickRidden(Player p_455939_, Vec3 p_454667_) {
        super.tickRidden(p_455939_, p_454667_);
        Vec2 vec2 = this.getRiddenRotation(p_455939_);
        float f = this.getYRot();
        float f1 = Mth.wrapDegrees(vec2.y - f);
        float f2 = 0.5F;
        f += f1 * 0.5F;
        this.setRot(f, vec2.x);
        this.yRotO = this.yBodyRot = this.yHeadRot = f;
        if (this.isLocalInstanceAuthoritative()) {
            if (this.playerJumpPendingScale > 0.0F && !this.isJumping()) {
                this.executeRidersJump(this.playerJumpPendingScale, p_455939_);
            }

            this.playerJumpPendingScale = 0.0F;
        }
    }

    @Override
    protected void travelInWater(Vec3 p_460802_, double p_460644_, boolean p_461141_, double p_460662_) {
        float f = this.getSpeed();
        this.moveRelative(f, p_460802_);
        this.move(MoverType.SELF, this.getDeltaMovement());
        this.setDeltaMovement(this.getDeltaMovement().scale(0.9));
    }

    @Override
    protected float getRiddenSpeed(Player p_455508_) {
        return this.isInWater()
            ? 0.0325F * (float)this.getAttributeValue(Attributes.MOVEMENT_SPEED)
            : 0.02F * (float)this.getAttributeValue(Attributes.MOVEMENT_SPEED);
    }

    protected void doPlayerRide(Player player) {
        if (!this.level().isClientSide()) {
            player.startRiding(this);
            if (!this.isVehicle()) {
                this.clearHome();
            }
        }
    }

    private int getNautilusRestrictionRadius() {
        return !this.isBaby() && this.getItemBySlot(EquipmentSlot.SADDLE).isEmpty() ? 32 : 16;
    }

    protected void checkRestriction() {
        if (!this.isLeashed() && !this.isVehicle() && this.isTame()) {
            int i = this.getNautilusRestrictionRadius();
            if (!this.hasHome() || !this.getHomePosition().closerThan(this.blockPosition(), i + 8) || i != this.getHomeRadius()) {
                this.setHomeTo(this.blockPosition(), i);
            }
        }
    }

    @Override
    protected void customServerAiStep(ServerLevel p_454720_) {
        this.checkRestriction();
        super.customServerAiStep(p_454720_);
    }

    private void applyEffects(Level level) {
        if (this.getFirstPassenger() instanceof Player player) {
            boolean flag = player.hasEffect(MobEffects.BREATH_OF_THE_NAUTILUS);
            boolean flag1 = level.getGameTime() % 40L == 0L;
            if (!flag || flag1) {
                player.addEffect(new MobEffectInstance(MobEffects.BREATH_OF_THE_NAUTILUS, 60, 0, true, true, true));
            }
        }
    }

    private void spawnBubbles() {
        double d0 = this.getDeltaMovement().length();
        double d1 = Mth.clamp(d0 * 2.0, 0.15F, 1.0);
        if (this.random.nextFloat() < d1) {
            float f = this.getYRot();
            float f1 = Mth.clamp(this.getXRot(), -10.0F, 10.0F);
            Vec3 vec3 = this.calculateViewVector(f1, f);
            double d2 = this.random.nextDouble() * 0.8 * (1.0 + d0);
            double d3 = (this.random.nextFloat() - 0.5) * d2;
            double d4 = (this.random.nextFloat() - 0.5) * d2;
            double d5 = (this.random.nextFloat() - 0.5) * d2;
            this.level().addParticle(ParticleTypes.BUBBLE, this.getX() - vec3.x * 1.1, this.getY() - vec3.y + 0.25, this.getZ() - vec3.z * 1.1, d3, d4, d5);
        }
    }

    @Override
    public void tick() {
        super.tick();
        if (!this.level().isClientSide()) {
            this.applyEffects(this.level());
        }

        if (this.isDashing() && this.dashCooldown < 35) {
            this.setDashing(false);
        }

        if (this.dashCooldown > 0) {
            this.dashCooldown--;
            if (this.dashCooldown == 0) {
                this.makeSound(this.getDashReadySound());
            }
        }

        if (this.isInWater()) {
            this.spawnBubbles();
        }
    }

    @Override
    public boolean canJump() {
        return this.isSaddled();
    }

    @Override
    public void onPlayerJump(int p_454766_) {
        if (this.isSaddled() && this.dashCooldown <= 0) {
            this.playerJumpPendingScale = this.getPlayerJumpPendingScale(p_454766_);
        }
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder p_455339_) {
        super.defineSynchedData(p_455339_);
        p_455339_.define(DASH, false);
    }

    public boolean isDashing() {
        return this.entityData.get(DASH);
    }

    public void setDashing(boolean dashing) {
        this.entityData.set(DASH, dashing);
    }

    protected void executeRidersJump(float scale, Player player) {
        this.addDeltaMovement(
            player.getLookAngle()
                .scale((this.isInWater() ? 1.2F : 0.5F) * scale * this.getAttributeValue(Attributes.MOVEMENT_SPEED) * this.getBlockSpeedFactor())
        );
        this.dashCooldown = 40;
        this.setDashing(true);
        this.needsSync = true;
    }

    @Override
    public void handleStartJump(int p_456272_) {
        this.makeSound(this.getDashSound());
        this.gameEvent(GameEvent.ENTITY_ACTION);
        this.setDashing(true);
    }

    @Override
    public int getJumpCooldown() {
        return this.dashCooldown;
    }

    @Override
    public void onSyncedDataUpdated(EntityDataAccessor<?> p_455984_) {
        if (!this.firstTick && DASH.equals(p_455984_)) {
            this.dashCooldown = this.dashCooldown == 0 ? 40 : this.dashCooldown;
        }

        super.onSyncedDataUpdated(p_455984_);
    }

    @Override
    public void handleStopJump() {
    }

    @Override
    protected void playStepSound(BlockPos p_454738_, BlockState p_455722_) {
    }

    protected @Nullable SoundEvent getDashSound() {
        return null;
    }

    protected @Nullable SoundEvent getDashReadySound() {
        return null;
    }

    @Override
    public InteractionResult interact(Player p_467073_, InteractionHand p_469343_) {
        this.setPersistenceRequired();
        return super.interact(p_467073_, p_469343_);
    }

    @Override
    public InteractionResult mobInteract(Player p_455571_, InteractionHand p_455460_) {
        ItemStack itemstack = p_455571_.getItemInHand(p_455460_);
        if (this.isBaby()) {
            return super.mobInteract(p_455571_, p_455460_);
        } else if (this.isTame() && p_455571_.isSecondaryUseActive()) {
            this.openCustomInventoryScreen(p_455571_);
            return InteractionResult.SUCCESS;
        } else {
            if (!itemstack.isEmpty()) {
                if (!this.level().isClientSide() && !this.isTame() && this.isFood(itemstack)) {
                    this.usePlayerItem(p_455571_, p_455460_, itemstack);
                    this.tryToTame(p_455571_);
                    return InteractionResult.SUCCESS_SERVER;
                }

                if (this.isFood(itemstack) && this.getHealth() < this.getMaxHealth()) {
                    FoodProperties foodproperties = itemstack.get(DataComponents.FOOD);
                    this.heal(foodproperties != null ? 2 * foodproperties.nutrition() : 1.0F);
                    this.usePlayerItem(p_455571_, p_455460_, itemstack);
                    this.playEatingSound();
                    return InteractionResult.SUCCESS;
                }

                InteractionResult interactionresult = itemstack.interactLivingEntity(p_455571_, this, p_455460_);
                if (interactionresult.consumesAction()) {
                    return interactionresult;
                }
            }

            if (this.isTame() && !p_455571_.isSecondaryUseActive() && !this.isFood(itemstack)) {
                this.doPlayerRide(p_455571_);
                return InteractionResult.SUCCESS;
            } else {
                return super.mobInteract(p_455571_, p_455460_);
            }
        }
    }

    private void tryToTame(Player player) {
        if (this.random.nextInt(3) == 0) {
            this.tame(player);
            this.navigation.stop();
            this.level().broadcastEntityEvent(this, (byte)7);
        } else {
            this.level().broadcastEntityEvent(this, (byte)6);
        }

        this.playEatingSound();
    }

    @Override
    public boolean removeWhenFarAway(double p_457873_) {
        return true;
    }

    @Override
    public boolean hurtServer(ServerLevel p_456240_, DamageSource p_455867_, float p_454712_) {
        boolean flag = super.hurtServer(p_456240_, p_455867_, p_454712_);
        if (flag && p_455867_.getEntity() instanceof LivingEntity livingentity) {
            NautilusAi.setAngerTarget(p_456240_, this, livingentity);
        }

        return flag;
    }

    @Override
    public boolean canBeAffected(MobEffectInstance p_456183_) {
        return p_456183_.getEffect() == MobEffects.POISON ? false : super.canBeAffected(p_456183_);
    }

    @Override
    public SpawnGroupData finalizeSpawn(
        ServerLevelAccessor p_456241_, DifficultyInstance p_455836_, EntitySpawnReason p_456131_, @Nullable SpawnGroupData p_455985_
    ) {
        RandomSource randomsource = p_456241_.getRandom();
        NautilusAi.initMemories(this, randomsource);
        return super.finalizeSpawn(p_456241_, p_455836_, p_456131_, p_455985_);
    }

    @Override
    protected Holder<SoundEvent> getEquipSound(EquipmentSlot p_459196_, ItemStack p_459040_, Equippable p_459134_) {
        if (p_459196_ == EquipmentSlot.SADDLE && this.isUnderWater()) {
            return SoundEvents.NAUTILUS_SADDLE_UNDERWATER_EQUIP;
        } else {
            return (Holder<SoundEvent>)(p_459196_ == EquipmentSlot.SADDLE
                ? SoundEvents.NAUTILUS_SADDLE_EQUIP
                : super.getEquipSound(p_459196_, p_459040_, p_459134_));
        }
    }

    public final int getInventorySize() {
        return AbstractMountInventoryMenu.getInventorySize(this.getInventoryColumns());
    }

    protected void createInventory() {
        SimpleContainer simplecontainer = this.inventory;
        this.inventory = new SimpleContainer(this.getInventorySize());
        if (simplecontainer != null) {
            int i = Math.min(simplecontainer.getContainerSize(), this.inventory.getContainerSize());

            for (int j = 0; j < i; j++) {
                ItemStack itemstack = simplecontainer.getItem(j);
                if (!itemstack.isEmpty()) {
                    this.inventory.setItem(j, itemstack.copy());
                }
            }
        }
    }

    @Override
    public void openCustomInventoryScreen(Player p_470759_) {
        if (!this.level().isClientSide() && (!this.isVehicle() || this.hasPassenger(p_470759_)) && this.isTame()) {
            p_470759_.openNautilusInventory(this, this.inventory);
        }
    }

    @Override
    public @Nullable SlotAccess getSlot(int p_470739_) {
        int i = p_470739_ - 500;
        return i >= 0 && i < this.inventory.getContainerSize() ? this.inventory.getSlot(i) : super.getSlot(p_470739_);
    }

    public boolean hasInventoryChanged(Container inventory) {
        return this.inventory != inventory;
    }

    public int getInventoryColumns() {
        return 0;
    }

    protected boolean isMobControlled() {
        return this.getFirstPassenger() instanceof Mob;
    }

    protected boolean isAggravated() {
        return this.getBrain().hasMemoryValue(MemoryModuleType.ANGRY_AT) || this.getBrain().hasMemoryValue(MemoryModuleType.ATTACK_TARGET);
    }
}
