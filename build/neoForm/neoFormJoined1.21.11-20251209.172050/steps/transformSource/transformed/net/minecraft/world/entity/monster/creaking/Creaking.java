package net.minecraft.world.entity.monster.creaking;

import com.mojang.serialization.Dynamic;
import java.util.List;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.util.profiling.Profiler;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.AnimationState;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.Brain;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.control.BodyRotationControl;
import net.minecraft.world.entity.ai.control.JumpControl;
import net.minecraft.world.entity.ai.control.LookControl;
import net.minecraft.world.entity.ai.control.MoveControl;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.navigation.GroundPathNavigation;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CreakingHeartBlock;
import net.minecraft.world.level.block.entity.CreakingHeartBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.CreakingHeartState;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.pathfinder.PathFinder;
import net.minecraft.world.level.pathfinder.PathType;
import net.minecraft.world.level.pathfinder.PathfindingContext;
import net.minecraft.world.level.pathfinder.WalkNodeEvaluator;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

public class Creaking extends Monster {
    private static final EntityDataAccessor<Boolean> CAN_MOVE = SynchedEntityData.defineId(Creaking.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> IS_ACTIVE = SynchedEntityData.defineId(Creaking.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> IS_TEARING_DOWN = SynchedEntityData.defineId(Creaking.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Optional<BlockPos>> HOME_POS = SynchedEntityData.defineId(Creaking.class, EntityDataSerializers.OPTIONAL_BLOCK_POS);
    private static final int ATTACK_ANIMATION_DURATION = 15;
    private static final int MAX_HEALTH = 1;
    private static final float ATTACK_DAMAGE = 3.0F;
    private static final float FOLLOW_RANGE = 32.0F;
    private static final float ACTIVATION_RANGE_SQ = 144.0F;
    public static final int ATTACK_INTERVAL = 40;
    private static final float MOVEMENT_SPEED_WHEN_FIGHTING = 0.4F;
    public static final float SPEED_MULTIPLIER_WHEN_IDLING = 0.3F;
    public static final int CREAKING_ORANGE = 16545810;
    public static final int CREAKING_GRAY = 6250335;
    public static final int INVULNERABILITY_ANIMATION_DURATION = 8;
    public static final int TWITCH_DEATH_DURATION = 45;
    private static final int MAX_PLAYER_STUCK_COUNTER = 4;
    private int attackAnimationRemainingTicks;
    public final AnimationState attackAnimationState = new AnimationState();
    public final AnimationState invulnerabilityAnimationState = new AnimationState();
    public final AnimationState deathAnimationState = new AnimationState();
    private int invulnerabilityAnimationRemainingTicks;
    private boolean eyesGlowing;
    private int nextFlickerTime;
    private int playerStuckCounter;

    public Creaking(EntityType<? extends Creaking> p_380212_, Level p_379294_) {
        super(p_380212_, p_379294_);
        this.lookControl = new Creaking.CreakingLookControl(this);
        this.moveControl = new Creaking.CreakingMoveControl(this);
        this.jumpControl = new Creaking.CreakingJumpControl(this);
        GroundPathNavigation groundpathnavigation = (GroundPathNavigation)this.getNavigation();
        groundpathnavigation.setCanFloat(true);
        this.xpReward = 0;
    }

    public void setTransient(BlockPos homePos) {
        this.setHomePos(homePos);
        this.setPathfindingMalus(PathType.DAMAGE_OTHER, 8.0F);
        this.setPathfindingMalus(PathType.POWDER_SNOW, 8.0F);
        this.setPathfindingMalus(PathType.LAVA, 8.0F);
        this.setPathfindingMalus(PathType.DAMAGE_FIRE, 0.0F);
        this.setPathfindingMalus(PathType.DANGER_FIRE, 0.0F);
    }

    public boolean isHeartBound() {
        return this.getHomePos() != null;
    }

    @Override
    protected BodyRotationControl createBodyControl() {
        return new Creaking.CreakingBodyRotationControl(this);
    }

    @Override
    protected Brain.Provider<Creaking> brainProvider() {
        return CreakingAi.brainProvider();
    }

    @Override
    protected Brain<?> makeBrain(Dynamic<?> p_380078_) {
        return CreakingAi.makeBrain(this, this.brainProvider().makeBrain(p_380078_));
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder p_379982_) {
        super.defineSynchedData(p_379982_);
        p_379982_.define(CAN_MOVE, true);
        p_379982_.define(IS_ACTIVE, false);
        p_379982_.define(IS_TEARING_DOWN, false);
        p_379982_.define(HOME_POS, Optional.empty());
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
            .add(Attributes.MAX_HEALTH, 1.0)
            .add(Attributes.MOVEMENT_SPEED, 0.4F)
            .add(Attributes.ATTACK_DAMAGE, 3.0)
            .add(Attributes.FOLLOW_RANGE, 32.0)
            .add(Attributes.STEP_HEIGHT, 1.0625);
    }

    public boolean canMove() {
        return this.entityData.get(CAN_MOVE);
    }

    @Override
    public boolean doHurtTarget(ServerLevel p_379943_, Entity p_379911_) {
        if (!(p_379911_ instanceof LivingEntity)) {
            return false;
        } else {
            this.attackAnimationRemainingTicks = 15;
            this.level().broadcastEntityEvent(this, (byte)4);
            return super.doHurtTarget(p_379943_, p_379911_);
        }
    }

    @Override
    public boolean hurtServer(ServerLevel p_389623_, DamageSource p_389564_, float p_389723_) {
        BlockPos blockpos = this.getHomePos();
        if (blockpos == null || p_389564_.is(DamageTypeTags.BYPASSES_INVULNERABILITY)) {
            return super.hurtServer(p_389623_, p_389564_, p_389723_);
        } else if (!this.isInvulnerableTo(p_389623_, p_389564_) && this.invulnerabilityAnimationRemainingTicks <= 0 && !this.isDeadOrDying()) {
            Player player = this.blameSourceForDamage(p_389564_);
            Entity entity = p_389564_.getDirectEntity();
            if (!(entity instanceof LivingEntity) && !(entity instanceof Projectile) && player == null) {
                return false;
            } else {
                this.invulnerabilityAnimationRemainingTicks = 8;
                this.level().broadcastEntityEvent(this, (byte)66);
                this.gameEvent(GameEvent.ENTITY_ACTION);
                if (this.level().getBlockEntity(blockpos) instanceof CreakingHeartBlockEntity creakingheartblockentity
                    && creakingheartblockentity.isProtector(this)) {
                    if (player != null) {
                        creakingheartblockentity.creakingHurt();
                    }

                    this.playHurtSound(p_389564_);
                }

                return true;
            }
        } else {
            return false;
        }
    }

    public Player blameSourceForDamage(DamageSource damageSource) {
        this.resolveMobResponsibleForDamage(damageSource);
        return this.resolvePlayerResponsibleForDamage(damageSource);
    }

    @Override
    public boolean isPushable() {
        return super.isPushable() && this.canMove();
    }

    @Override
    public void push(double p_388562_, double p_388936_, double p_387604_) {
        if (this.canMove()) {
            super.push(p_388562_, p_388936_, p_387604_);
        }
    }

    @Override
    public Brain<Creaking> getBrain() {
        return (Brain<Creaking>)super.getBrain();
    }

    @Override
    protected void customServerAiStep(ServerLevel p_379858_) {
        ProfilerFiller profilerfiller = Profiler.get();
        profilerfiller.push("creakingBrain");
        this.getBrain().tick((ServerLevel)this.level(), this);
        profilerfiller.pop();
        CreakingAi.updateActivity(this);
    }

    @Override
    public void aiStep() {
        if (this.invulnerabilityAnimationRemainingTicks > 0) {
            this.invulnerabilityAnimationRemainingTicks--;
        }

        if (this.attackAnimationRemainingTicks > 0) {
            this.attackAnimationRemainingTicks--;
        }

        if (!this.level().isClientSide()) {
            boolean flag = this.entityData.get(CAN_MOVE);
            boolean flag1 = this.checkCanMove();
            if (flag1 != flag) {
                this.gameEvent(GameEvent.ENTITY_ACTION);
                if (flag1) {
                    this.makeSound(SoundEvents.CREAKING_UNFREEZE);
                } else {
                    this.stopInPlace();
                    this.makeSound(SoundEvents.CREAKING_FREEZE);
                }
            }

            this.entityData.set(CAN_MOVE, flag1);
        }

        super.aiStep();
    }

    @Override
    public void tick() {
        if (!this.level().isClientSide()) {
            BlockPos blockpos = this.getHomePos();
            if (blockpos != null) {
                boolean flag = this.level().getBlockEntity(blockpos) instanceof CreakingHeartBlockEntity creakingheartblockentity
                    && creakingheartblockentity.isProtector(this);
                if (!flag) {
                    this.setHealth(0.0F);
                }
            }
        }

        super.tick();
        if (this.level().isClientSide()) {
            this.setupAnimationStates();
            this.checkEyeBlink();
        }
    }

    @Override
    protected void tickDeath() {
        if (this.isHeartBound() && this.isTearingDown()) {
            this.deathTime++;
            if (!this.level().isClientSide() && this.deathTime > 45 && !this.isRemoved()) {
                this.tearDown();
            }
        } else {
            super.tickDeath();
        }
    }

    @Override
    protected void updateWalkAnimation(float p_382793_) {
        float f = Math.min(p_382793_ * 25.0F, 3.0F);
        this.walkAnimation.update(f, 0.4F, 1.0F);
    }

    private void setupAnimationStates() {
        this.attackAnimationState.animateWhen(this.attackAnimationRemainingTicks > 0, this.tickCount);
        this.invulnerabilityAnimationState.animateWhen(this.invulnerabilityAnimationRemainingTicks > 0, this.tickCount);
        this.deathAnimationState.animateWhen(this.isTearingDown(), this.tickCount);
    }

    public void tearDown() {
        if (this.level() instanceof ServerLevel serverlevel) {
            AABB aabb = this.getBoundingBox();
            Vec3 vec3 = aabb.getCenter();
            double d0 = aabb.getXsize() * 0.3;
            double d1 = aabb.getYsize() * 0.3;
            double d2 = aabb.getZsize() * 0.3;
            serverlevel.sendParticles(
                new BlockParticleOption(ParticleTypes.BLOCK_CRUMBLE, Blocks.PALE_OAK_WOOD.defaultBlockState()), vec3.x, vec3.y, vec3.z, 100, d0, d1, d2, 0.0
            );
            serverlevel.sendParticles(
                new BlockParticleOption(
                    ParticleTypes.BLOCK_CRUMBLE, Blocks.CREAKING_HEART.defaultBlockState().setValue(CreakingHeartBlock.STATE, CreakingHeartState.AWAKE)
                ),
                vec3.x,
                vec3.y,
                vec3.z,
                10,
                d0,
                d1,
                d2,
                0.0
            );
        }

        this.makeSound(this.getDeathSound());
        this.remove(Entity.RemovalReason.DISCARDED);
    }

    public void creakingDeathEffects(DamageSource damageSource) {
        this.blameSourceForDamage(damageSource);
        this.die(damageSource);
        this.makeSound(SoundEvents.CREAKING_TWITCH);
    }

    @Override
    public void handleEntityEvent(byte p_379620_) {
        if (p_379620_ == 66) {
            this.invulnerabilityAnimationRemainingTicks = 8;
            this.playHurtSound(this.damageSources().generic());
        } else if (p_379620_ == 4) {
            this.attackAnimationRemainingTicks = 15;
            this.playAttackSound();
        } else {
            super.handleEntityEvent(p_379620_);
        }
    }

    @Override
    public boolean fireImmune() {
        return this.isHeartBound() || super.fireImmune();
    }

    @Override
    public boolean canUsePortal(boolean p_389552_) {
        return !this.isHeartBound() && super.canUsePortal(p_389552_);
    }

    @Override
    protected PathNavigation createNavigation(Level p_389684_) {
        return new Creaking.CreakingPathNavigation(this, p_389684_);
    }

    public boolean playerIsStuckInYou() {
        List<Player> list = this.brain.getMemory(MemoryModuleType.NEAREST_PLAYERS).orElse(List.of());
        if (list.isEmpty()) {
            this.playerStuckCounter = 0;
            return false;
        } else {
            AABB aabb = this.getBoundingBox();

            for (Player player : list) {
                if (aabb.contains(player.getEyePosition())) {
                    this.playerStuckCounter++;
                    return this.playerStuckCounter > 4;
                }
            }

            this.playerStuckCounter = 0;
            return false;
        }
    }

    @Override
    protected void readAdditionalSaveData(ValueInput p_421642_) {
        super.readAdditionalSaveData(p_421642_);
        p_421642_.read("home_pos", BlockPos.CODEC).ifPresent(this::setTransient);
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput p_421824_) {
        super.addAdditionalSaveData(p_421824_);
        p_421824_.storeNullable("home_pos", BlockPos.CODEC, this.getHomePos());
    }

    public void setHomePos(BlockPos homePos) {
        this.entityData.set(HOME_POS, Optional.of(homePos));
    }

    public @Nullable BlockPos getHomePos() {
        return this.entityData.get(HOME_POS).orElse(null);
    }

    public void setTearingDown() {
        this.entityData.set(IS_TEARING_DOWN, true);
    }

    public boolean isTearingDown() {
        return this.entityData.get(IS_TEARING_DOWN);
    }

    public boolean hasGlowingEyes() {
        return this.eyesGlowing;
    }

    public void checkEyeBlink() {
        if (this.deathTime > this.nextFlickerTime) {
            this.nextFlickerTime = this.deathTime
                + this.getRandom().nextIntBetweenInclusive(this.eyesGlowing ? 2 : this.deathTime / 4, this.eyesGlowing ? 8 : this.deathTime / 2);
            this.eyesGlowing = !this.eyesGlowing;
        }
    }

    @Override
    public void playAttackSound() {
        this.makeSound(SoundEvents.CREAKING_ATTACK);
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return this.isActive() ? null : SoundEvents.CREAKING_AMBIENT;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource p_380378_) {
        return this.isHeartBound() ? SoundEvents.CREAKING_SWAY : super.getHurtSound(p_380378_);
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.CREAKING_DEATH;
    }

    @Override
    protected void playStepSound(BlockPos p_379428_, BlockState p_380060_) {
        this.playSound(SoundEvents.CREAKING_STEP, 0.15F, 1.0F);
    }

    @Override
    public @Nullable LivingEntity getTarget() {
        return this.getTargetFromBrain();
    }

    @Override
    public void knockback(double p_379489_, double p_380324_, double p_379735_) {
        if (this.canMove()) {
            super.knockback(p_379489_, p_380324_, p_379735_);
        }
    }

    public boolean checkCanMove() {
        List<Player> list = this.brain.getMemory(MemoryModuleType.NEAREST_PLAYERS).orElse(List.of());
        boolean flag = this.isActive();
        if (list.isEmpty()) {
            if (flag) {
                this.deactivate();
            }

            return true;
        } else {
            boolean flag1 = false;

            for (Player player : list) {
                if (this.canAttack(player) && !this.isAlliedTo(player)) {
                    flag1 = true;
                    // Neo: provide entity being looked at to disguise check
                    if ((!flag || LivingEntity.PLAYER_NOT_WEARING_DISGUISE_ITEM_FOR_TARGET.test(player, this))
                        && this.isLookingAtMe(
                            player, 0.5, false, true, this.getEyeY(), this.getY() + 0.5 * this.getScale(), (this.getEyeY() + this.getY()) / 2.0
                        )) {
                        if (flag) {
                            return false;
                        }

                        if (player.distanceToSqr(this) < 144.0) {
                            this.activate(player);
                            return false;
                        }
                    }
                }
            }

            if (!flag1 && flag) {
                this.deactivate();
            }

            return true;
        }
    }

    public void activate(Player player) {
        this.getBrain().setMemory(MemoryModuleType.ATTACK_TARGET, player);
        this.gameEvent(GameEvent.ENTITY_ACTION);
        this.makeSound(SoundEvents.CREAKING_ACTIVATE);
        this.setIsActive(true);
    }

    public void deactivate() {
        this.getBrain().eraseMemory(MemoryModuleType.ATTACK_TARGET);
        this.gameEvent(GameEvent.ENTITY_ACTION);
        this.makeSound(SoundEvents.CREAKING_DEACTIVATE);
        this.setIsActive(false);
    }

    public void setIsActive(boolean isActive) {
        this.entityData.set(IS_ACTIVE, isActive);
    }

    public boolean isActive() {
        return this.entityData.get(IS_ACTIVE);
    }

    @Override
    public float getWalkTargetValue(BlockPos p_379756_, LevelReader p_380102_) {
        return 0.0F;
    }

    class CreakingBodyRotationControl extends BodyRotationControl {
        public CreakingBodyRotationControl(Creaking creaking) {
            super(creaking);
        }

        @Override
        public void clientTick() {
            if (Creaking.this.canMove()) {
                super.clientTick();
            }
        }
    }

    class CreakingJumpControl extends JumpControl {
        public CreakingJumpControl(Creaking creaking) {
            super(creaking);
        }

        @Override
        public void tick() {
            if (Creaking.this.canMove()) {
                super.tick();
            } else {
                Creaking.this.setJumping(false);
            }
        }
    }

    class CreakingLookControl extends LookControl {
        public CreakingLookControl(Creaking creaking) {
            super(creaking);
        }

        @Override
        public void tick() {
            if (Creaking.this.canMove()) {
                super.tick();
            }
        }
    }

    class CreakingMoveControl extends MoveControl {
        public CreakingMoveControl(Creaking creaking) {
            super(creaking);
        }

        @Override
        public void tick() {
            if (Creaking.this.canMove()) {
                super.tick();
            }
        }
    }

    class CreakingPathNavigation extends GroundPathNavigation {
        CreakingPathNavigation(Creaking creaking, Level level) {
            super(creaking, level);
        }

        @Override
        public void tick() {
            if (Creaking.this.canMove()) {
                super.tick();
            }
        }

        @Override
        protected PathFinder createPathFinder(int p_389538_) {
            this.nodeEvaluator = Creaking.this.new HomeNodeEvaluator();
            this.nodeEvaluator.setCanPassDoors(true);
            return new PathFinder(this.nodeEvaluator, p_389538_);
        }
    }

    class HomeNodeEvaluator extends WalkNodeEvaluator {
        private static final int MAX_DISTANCE_TO_HOME_SQ = 1024;

        @Override
        public PathType getPathType(PathfindingContext p_389549_, int p_389638_, int p_389512_, int p_389613_) {
            BlockPos blockpos = Creaking.this.getHomePos();
            if (blockpos == null) {
                return super.getPathType(p_389549_, p_389638_, p_389512_, p_389613_);
            } else {
                double d0 = blockpos.distSqr(new Vec3i(p_389638_, p_389512_, p_389613_));
                return d0 > 1024.0 && d0 >= blockpos.distSqr(p_389549_.mobPosition())
                    ? PathType.BLOCKED
                    : super.getPathType(p_389549_, p_389638_, p_389512_, p_389613_);
            }
        }
    }
}
