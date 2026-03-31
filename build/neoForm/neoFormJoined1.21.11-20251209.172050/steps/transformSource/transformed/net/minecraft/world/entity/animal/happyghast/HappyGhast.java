package net.minecraft.world.entity.animal.happyghast;

import com.mojang.serialization.Dynamic;
import net.minecraft.core.BlockPos;
import net.minecraft.network.protocol.game.ClientboundEntityPositionSyncPacket;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.ItemTags;
import net.minecraft.util.Mth;
import net.minecraft.util.profiling.Profiler;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.Leashable;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.Brain;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.control.BodyRotationControl;
import net.minecraft.world.entity.ai.control.FlyingMoveControl;
import net.minecraft.world.entity.ai.control.LookControl;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.TemptGoal;
import net.minecraft.world.entity.ai.navigation.FlyingPathNavigation;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.monster.Ghast;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

public class HappyGhast extends Animal {
    public static final float BABY_SCALE = 0.2375F;
    public static final int WANDER_GROUND_DISTANCE = 16;
    public static final int SMALL_RESTRICTION_RADIUS = 32;
    public static final int LARGE_RESTRICTION_RADIUS = 64;
    public static final int RESTRICTION_RADIUS_BUFFER = 16;
    public static final int FAST_HEALING_TICKS = 20;
    public static final int SLOW_HEALING_TICKS = 600;
    public static final int MAX_PASSANGERS = 4;
    private static final int STILL_TIMEOUT_ON_LOAD_GRACE_PERIOD = 60;
    private static final int MAX_STILL_TIMEOUT = 10;
    public static final float SPEED_MULTIPLIER_WHEN_PANICKING = 2.0F;
    private int leashHolderTime = 0;
    private int serverStillTimeout;
    private static final EntityDataAccessor<Boolean> IS_LEASH_HOLDER = SynchedEntityData.defineId(HappyGhast.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> STAYS_STILL = SynchedEntityData.defineId(HappyGhast.class, EntityDataSerializers.BOOLEAN);
    private static final float MAX_SCALE = 1.0F;

    public HappyGhast(EntityType<? extends HappyGhast> p_482008_, Level p_478275_) {
        super(p_482008_, p_478275_);
        this.moveControl = new Ghast.GhastMoveControl(this, true, this::isOnStillTimeout);
        this.lookControl = new HappyGhast.HappyGhastLookControl();
    }

    private void setServerStillTimeout(int serverStillTimeout) {
        if (this.serverStillTimeout <= 0 && serverStillTimeout > 0 && this.level() instanceof ServerLevel serverlevel) {
            this.syncPacketPositionCodec(this.getX(), this.getY(), this.getZ());
            serverlevel.getChunkSource().chunkMap.sendToTrackingPlayers(this, ClientboundEntityPositionSyncPacket.of(this));
        }

        this.serverStillTimeout = serverStillTimeout;
        this.syncStayStillFlag();
    }

    private PathNavigation createBabyNavigation(Level level) {
        return new HappyGhast.BabyFlyingPathNavigation(this, level);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(3, new HappyGhast.HappyGhastFloatGoal());
        this.goalSelector
            .addGoal(
                4,
                new TemptGoal.ForNonPathfinders(
                    this,
                    1.0,
                    p_478656_ -> !this.isWearingBodyArmor() && !this.isBaby()
                        ? p_478656_.is(ItemTags.HAPPY_GHAST_TEMPT_ITEMS)
                        : p_478656_.is(ItemTags.HAPPY_GHAST_FOOD),
                    false,
                    7.0
                )
            );
        this.goalSelector.addGoal(5, new Ghast.RandomFloatAroundGoal(this, 16));
    }

    private void adultGhastSetup() {
        this.moveControl = new Ghast.GhastMoveControl(this, true, this::isOnStillTimeout);
        this.lookControl = new HappyGhast.HappyGhastLookControl();
        this.navigation = this.createNavigation(this.level());
        if (this.level() instanceof ServerLevel serverlevel) {
            this.removeAllGoals(p_479206_ -> true);
            this.registerGoals();
            ((Brain<HappyGhast>)this.brain).stopAll(serverlevel, this);
            this.brain.clearMemories();
        }
    }

    private void babyGhastSetup() {
        this.moveControl = new FlyingMoveControl(this, 180, true);
        this.lookControl = new LookControl(this);
        this.navigation = this.createBabyNavigation(this.level());
        this.setServerStillTimeout(0);
        this.removeAllGoals(p_481532_ -> true);
    }

    @Override
    protected void ageBoundaryReached() {
        if (this.isBaby()) {
            this.babyGhastSetup();
        } else {
            this.adultGhastSetup();
        }

        super.ageBoundaryReached();
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Animal.createAnimalAttributes()
            .add(Attributes.MAX_HEALTH, 20.0)
            .add(Attributes.TEMPT_RANGE, 16.0)
            .add(Attributes.FLYING_SPEED, 0.05)
            .add(Attributes.MOVEMENT_SPEED, 0.05)
            .add(Attributes.FOLLOW_RANGE, 16.0)
            .add(Attributes.CAMERA_DISTANCE, 8.0);
    }

    @Override
    protected float sanitizeScale(float p_480784_) {
        return Math.min(p_480784_, 1.0F);
    }

    @Override
    protected void checkFallDamage(double p_478349_, boolean p_478523_, BlockState p_478527_, BlockPos p_480177_) {
    }

    @Override
    public boolean onClimbable() {
        return false;
    }

    @Override
    public void travel(Vec3 p_478483_) {
        float f = (float)this.getAttributeValue(Attributes.FLYING_SPEED) * 5.0F / 3.0F;
        this.travelFlying(p_478483_, f, f, f);
    }

    @Override
    public float getWalkTargetValue(BlockPos p_479882_, LevelReader p_479173_) {
        if (!p_479173_.isEmptyBlock(p_479882_)) {
            return 0.0F;
        } else {
            return p_479173_.isEmptyBlock(p_479882_.below()) && !p_479173_.isEmptyBlock(p_479882_.below(2)) ? 10.0F : 5.0F;
        }
    }

    @Override
    public boolean canBreatheUnderwater() {
        return this.isBaby() ? true : super.canBreatheUnderwater();
    }

    @Override
    protected boolean shouldStayCloseToLeashHolder() {
        return false;
    }

    @Override
    protected void playStepSound(BlockPos p_481178_, BlockState p_481221_) {
    }

    @Override
    public float getVoicePitch() {
        return 1.0F;
    }

    @Override
    public SoundSource getSoundSource() {
        return SoundSource.NEUTRAL;
    }

    @Override
    public int getAmbientSoundInterval() {
        int i = super.getAmbientSoundInterval();
        return this.isVehicle() ? i * 6 : i;
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return this.isBaby() ? SoundEvents.GHASTLING_AMBIENT : SoundEvents.HAPPY_GHAST_AMBIENT;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource p_478380_) {
        return this.isBaby() ? SoundEvents.GHASTLING_HURT : SoundEvents.HAPPY_GHAST_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return this.isBaby() ? SoundEvents.GHASTLING_DEATH : SoundEvents.HAPPY_GHAST_DEATH;
    }

    @Override
    protected float getSoundVolume() {
        return this.isBaby() ? 1.0F : 4.0F;
    }

    @Override
    public int getMaxSpawnClusterSize() {
        return 1;
    }

    @Override
    public @Nullable AgeableMob getBreedOffspring(ServerLevel p_477970_, AgeableMob p_479512_) {
        return EntityType.HAPPY_GHAST.create(p_477970_, EntitySpawnReason.BREEDING);
    }

    @Override
    public boolean canFallInLove() {
        return false;
    }

    @Override
    public float getAgeScale() {
        return this.isBaby() ? 0.2375F : 1.0F;
    }

    @Override
    public boolean isFood(ItemStack p_480189_) {
        return p_480189_.is(ItemTags.HAPPY_GHAST_FOOD);
    }

    @Override
    public boolean canUseSlot(EquipmentSlot p_482138_) {
        return p_482138_ != EquipmentSlot.BODY ? super.canUseSlot(p_482138_) : this.isAlive() && !this.isBaby();
    }

    @Override
    protected boolean canDispenserEquipIntoSlot(EquipmentSlot p_481830_) {
        return p_481830_ == EquipmentSlot.BODY;
    }

    @Override
    public InteractionResult mobInteract(Player p_481488_, InteractionHand p_479863_) {
        if (this.isBaby()) {
            return super.mobInteract(p_481488_, p_479863_);
        } else {
            ItemStack itemstack = p_481488_.getItemInHand(p_479863_);
            if (!itemstack.isEmpty()) {
                InteractionResult interactionresult = itemstack.interactLivingEntity(p_481488_, this, p_479863_);
                if (interactionresult.consumesAction()) {
                    return interactionresult;
                }
            }

            if (this.isWearingBodyArmor() && !p_481488_.isSecondaryUseActive()) {
                this.doPlayerRide(p_481488_);
                return InteractionResult.SUCCESS;
            } else {
                return super.mobInteract(p_481488_, p_479863_);
            }
        }
    }

    private void doPlayerRide(Player player) {
        if (!this.level().isClientSide()) {
            player.startRiding(this);
        }
    }

    @Override
    protected void addPassenger(Entity p_481270_) {
        if (!this.isVehicle()) {
            this.level().playSound(null, this.getX(), this.getY(), this.getZ(), SoundEvents.HARNESS_GOGGLES_DOWN, this.getSoundSource(), 1.0F, 1.0F);
        }

        super.addPassenger(p_481270_);
        if (!this.level().isClientSide()) {
            if (!this.scanPlayerAboveGhast()) {
                this.setServerStillTimeout(0);
            } else if (this.serverStillTimeout > 10) {
                this.setServerStillTimeout(10);
            }
        }
    }

    @Override
    protected void removePassenger(Entity p_478923_) {
        super.removePassenger(p_478923_);
        if (!this.level().isClientSide()) {
            this.setServerStillTimeout(10);
        }

        if (!this.isVehicle()) {
            this.clearHome();
            this.level().playSound(null, this.getX(), this.getY(), this.getZ(), SoundEvents.HARNESS_GOGGLES_UP, this.getSoundSource(), 1.0F, 1.0F);
        }
    }

    @Override
    protected boolean canAddPassenger(Entity p_479872_) {
        return this.getPassengers().size() < 4;
    }

    @Override
    public @Nullable LivingEntity getControllingPassenger() {
        return (LivingEntity)(this.isWearingBodyArmor() && !this.isOnStillTimeout() && this.getFirstPassenger() instanceof Player player
            ? player
            : super.getControllingPassenger());
    }

    @Override
    protected Vec3 getRiddenInput(Player p_478979_, Vec3 p_481601_) {
        float f = p_478979_.xxa;
        float f1 = 0.0F;
        float f2 = 0.0F;
        if (p_478979_.zza != 0.0F) {
            float f3 = Mth.cos(p_478979_.getXRot() * (float) (Math.PI / 180.0));
            float f4 = -Mth.sin(p_478979_.getXRot() * (float) (Math.PI / 180.0));
            if (p_478979_.zza < 0.0F) {
                f3 *= -0.5F;
                f4 *= -0.5F;
            }

            f2 = f4;
            f1 = f3;
        }

        if (p_478979_.isJumping()) {
            f2 += 0.5F;
        }

        return new Vec3(f, f2, f1).scale(3.9F * this.getAttributeValue(Attributes.FLYING_SPEED));
    }

    protected Vec2 getRiddenRotation(LivingEntity entity) {
        return new Vec2(entity.getXRot() * 0.5F, entity.getYRot());
    }

    @Override
    protected void tickRidden(Player p_479364_, Vec3 p_480110_) {
        super.tickRidden(p_479364_, p_480110_);
        Vec2 vec2 = this.getRiddenRotation(p_479364_);
        float f = this.getYRot();
        float f1 = Mth.wrapDegrees(vec2.y - f);
        float f2 = 0.08F;
        f += f1 * 0.08F;
        this.setRot(f, vec2.x);
        this.yRotO = this.yBodyRot = this.yHeadRot = f;
    }

    @Override
    protected Brain.Provider<HappyGhast> brainProvider() {
        return HappyGhastAi.brainProvider();
    }

    @Override
    protected Brain<?> makeBrain(Dynamic<?> p_479542_) {
        return HappyGhastAi.makeBrain(this.brainProvider().makeBrain(p_479542_));
    }

    @Override
    protected void customServerAiStep(ServerLevel p_478997_) {
        if (this.isBaby()) {
            ProfilerFiller profilerfiller = Profiler.get();
            profilerfiller.push("happyGhastBrain");
            ((Brain<HappyGhast>)this.brain).tick(p_478997_, this);
            profilerfiller.pop();
            profilerfiller.push("happyGhastActivityUpdate");
            HappyGhastAi.updateActivity(this);
            profilerfiller.pop();
        }

        this.checkRestriction();
        super.customServerAiStep(p_478997_);
    }

    @Override
    public void tick() {
        super.tick();
        if (!this.level().isClientSide()) {
            if (this.leashHolderTime > 0) {
                this.leashHolderTime--;
            }

            this.setLeashHolder(this.leashHolderTime > 0);
            if (this.serverStillTimeout > 0) {
                if (this.tickCount > 60) {
                    this.serverStillTimeout--;
                }

                this.setServerStillTimeout(this.serverStillTimeout);
            }

            if (this.scanPlayerAboveGhast()) {
                this.setServerStillTimeout(10);
            }
        }
    }

    @Override
    public void aiStep() {
        if (!this.level().isClientSide()) {
            this.setRequiresPrecisePosition(this.isOnStillTimeout());
        }

        super.aiStep();
        this.continuousHeal();
    }

    private int getHappyGhastRestrictionRadius() {
        return !this.isBaby() && this.getItemBySlot(EquipmentSlot.BODY).isEmpty() ? 64 : 32;
    }

    private void checkRestriction() {
        if (!this.isLeashed() && !this.isVehicle()) {
            int i = this.getHappyGhastRestrictionRadius();
            if (!this.hasHome() || !this.getHomePosition().closerThan(this.blockPosition(), i + 16) || i != this.getHomeRadius()) {
                this.setHomeTo(this.blockPosition(), i);
            }
        }
    }

    private void continuousHeal() {
        if (this.level() instanceof ServerLevel serverlevel && this.isAlive() && this.deathTime == 0 && this.getMaxHealth() != this.getHealth()) {
            boolean flag = this.isInClouds() || serverlevel.precipitationAt(this.blockPosition()) != Biome.Precipitation.NONE;
            if (this.tickCount % (flag ? 20 : 600) == 0) {
                this.heal(1.0F);
            }
        }
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder p_479317_) {
        super.defineSynchedData(p_479317_);
        p_479317_.define(IS_LEASH_HOLDER, false);
        p_479317_.define(STAYS_STILL, false);
    }

    private void setLeashHolder(boolean leashHolder) {
        this.entityData.set(IS_LEASH_HOLDER, leashHolder);
    }

    public boolean isLeashHolder() {
        return this.entityData.get(IS_LEASH_HOLDER);
    }

    private void syncStayStillFlag() {
        this.entityData.set(STAYS_STILL, this.serverStillTimeout > 0);
    }

    public boolean staysStill() {
        return this.entityData.get(STAYS_STILL);
    }

    @Override
    public boolean supportQuadLeashAsHolder() {
        return true;
    }

    @Override
    public Vec3[] getQuadLeashHolderOffsets() {
        return Leashable.createQuadLeashOffsets(this, -0.03125, 0.4375, 0.46875, 0.03125);
    }

    @Override
    public Vec3 getLeashOffset() {
        return Vec3.ZERO;
    }

    @Override
    public double leashElasticDistance() {
        return 10.0;
    }

    @Override
    public double leashSnapDistance() {
        return 16.0;
    }

    @Override
    public void onElasticLeashPull() {
        super.onElasticLeashPull();
        this.getMoveControl().setWait();
    }

    @Override
    public void notifyLeashHolder(Leashable p_479989_) {
        if (p_479989_.supportQuadLeash()) {
            this.leashHolderTime = 5;
        }
    }

    @Override
    public void addAdditionalSaveData(ValueOutput p_480965_) {
        super.addAdditionalSaveData(p_480965_);
        p_480965_.putInt("still_timeout", this.serverStillTimeout);
    }

    @Override
    public void readAdditionalSaveData(ValueInput p_481226_) {
        super.readAdditionalSaveData(p_481226_);
        this.setServerStillTimeout(p_481226_.getIntOr("still_timeout", 0));
    }

    public boolean isOnStillTimeout() {
        return this.staysStill() || this.serverStillTimeout > 0;
    }

    private boolean scanPlayerAboveGhast() {
        AABB aabb = this.getBoundingBox();
        AABB aabb1 = new AABB(aabb.minX - 1.0, aabb.maxY - 1.0E-5F, aabb.minZ - 1.0, aabb.maxX + 1.0, aabb.maxY + aabb.getYsize() / 2.0, aabb.maxZ + 1.0);

        for (Player player : this.level().players()) {
            if (!player.isSpectator()) {
                Entity entity = player.getRootVehicle();
                if (!(entity instanceof HappyGhast) && aabb1.contains(entity.position())) {
                    return true;
                }
            }
        }

        return false;
    }

    @Override
    protected BodyRotationControl createBodyControl() {
        return new HappyGhast.HappyGhastBodyRotationControl();
    }

    @Override
    public boolean canBeCollidedWith(@Nullable Entity p_480927_) {
        if (!this.isBaby() && this.isAlive()) {
            if (this.level().isClientSide() && p_480927_ instanceof Player && p_480927_.position().y >= this.getBoundingBox().maxY) {
                return true;
            } else {
                return this.isVehicle() && p_480927_ instanceof HappyGhast ? true : this.isOnStillTimeout();
            }
        } else {
            return false;
        }
    }

    @Override
    public boolean isFlyingVehicle() {
        return !this.isBaby();
    }

    @Override
    public Vec3 getDismountLocationForPassenger(LivingEntity p_482379_) {
        return new Vec3(this.getX(), this.getBoundingBox().maxY, this.getZ());
    }

    static class BabyFlyingPathNavigation extends FlyingPathNavigation {
        public BabyFlyingPathNavigation(HappyGhast happyGhast, Level level) {
            super(happyGhast, level);
            this.setCanOpenDoors(false);
            this.setCanFloat(true);
            this.setRequiredPathLength(48.0F);
        }

        @Override
        protected boolean canMoveDirectly(Vec3 p_478192_, Vec3 p_478265_) {
            return isClearForMovementBetween(this.mob, p_478192_, p_478265_, false);
        }
    }

    class HappyGhastBodyRotationControl extends BodyRotationControl {
        public HappyGhastBodyRotationControl() {
            super(HappyGhast.this);
        }

        @Override
        public void clientTick() {
            if (HappyGhast.this.isVehicle()) {
                HappyGhast.this.yHeadRot = HappyGhast.this.getYRot();
                HappyGhast.this.yBodyRot = HappyGhast.this.yHeadRot;
            }

            super.clientTick();
        }
    }

    class HappyGhastFloatGoal extends FloatGoal {
        public HappyGhastFloatGoal() {
            super(HappyGhast.this);
        }

        @Override
        public boolean canUse() {
            return !HappyGhast.this.isOnStillTimeout() && super.canUse();
        }
    }

    class HappyGhastLookControl extends LookControl {
        HappyGhastLookControl() {
            super(HappyGhast.this);
        }

        @Override
        public void tick() {
            if (HappyGhast.this.isOnStillTimeout()) {
                float f = wrapDegrees90(HappyGhast.this.getYRot());
                HappyGhast.this.setYRot(HappyGhast.this.getYRot() - f);
                HappyGhast.this.setYHeadRot(HappyGhast.this.getYRot());
            } else if (this.lookAtCooldown > 0) {
                this.lookAtCooldown--;
                double d0 = this.wantedX - HappyGhast.this.getX();
                double d1 = this.wantedZ - HappyGhast.this.getZ();
                HappyGhast.this.setYRot(-((float)Mth.atan2(d0, d1)) * (180.0F / (float)Math.PI));
                HappyGhast.this.yBodyRot = HappyGhast.this.getYRot();
                HappyGhast.this.yHeadRot = HappyGhast.this.yBodyRot;
            } else {
                Ghast.faceMovementDirection(this.mob);
            }
        }

        public static float wrapDegrees90(float degrees) {
            float f = degrees % 90.0F;
            if (f >= 45.0F) {
                f -= 90.0F;
            }

            if (f < -45.0F) {
                f += 90.0F;
            }

            return f;
        }
    }
}
