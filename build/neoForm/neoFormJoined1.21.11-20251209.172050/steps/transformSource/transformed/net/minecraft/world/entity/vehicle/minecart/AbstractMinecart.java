package net.minecraft.world.entity.vehicle.minecart;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import com.mojang.datafixers.util.Pair;
import java.util.Map;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;
import net.minecraft.network.protocol.game.ClientboundAddEntityPacket;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.BlockUtil;
import net.minecraft.util.Mth;
import net.minecraft.util.Util;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.InterpolationHandler;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.entity.npc.wanderingtrader.WanderingTrader;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.vehicle.DismountHelper;
import net.minecraft.world.entity.vehicle.VehicleEntity;
import net.minecraft.world.entity.vehicle.boat.AbstractBoat;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseRailBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.PoweredRailBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.RailShape;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

public abstract class AbstractMinecart extends VehicleEntity {
    private static final Vec3 LOWERED_PASSENGER_ATTACHMENT = new Vec3(0.0, 0.0, 0.0);
    private static final EntityDataAccessor<Optional<BlockState>> DATA_ID_CUSTOM_DISPLAY_BLOCK = SynchedEntityData.defineId(
        AbstractMinecart.class, EntityDataSerializers.OPTIONAL_BLOCK_STATE
    );
    private static final EntityDataAccessor<Integer> DATA_ID_DISPLAY_OFFSET = SynchedEntityData.defineId(AbstractMinecart.class, EntityDataSerializers.INT);
    private static final ImmutableMap<Pose, ImmutableList<Integer>> POSE_DISMOUNT_HEIGHTS = ImmutableMap.of(
        Pose.STANDING, ImmutableList.of(0, 1, -1), Pose.CROUCHING, ImmutableList.of(0, 1, -1), Pose.SWIMMING, ImmutableList.of(0, 1)
    );
    protected static final float WATER_SLOWDOWN_FACTOR = 0.95F;
    private static final boolean DEFAULT_FLIPPED_ROTATION = false;
    private boolean onRails;
    private boolean flipped = false;
    private final MinecartBehavior behavior;
    private static final Map<RailShape, Pair<Vec3i, Vec3i>> EXITS = Maps.newEnumMap(
        Util.make(
            () -> {
                Vec3i vec3i = Direction.WEST.getUnitVec3i();
                Vec3i vec3i1 = Direction.EAST.getUnitVec3i();
                Vec3i vec3i2 = Direction.NORTH.getUnitVec3i();
                Vec3i vec3i3 = Direction.SOUTH.getUnitVec3i();
                Vec3i vec3i4 = vec3i.below();
                Vec3i vec3i5 = vec3i1.below();
                Vec3i vec3i6 = vec3i2.below();
                Vec3i vec3i7 = vec3i3.below();
                return ImmutableMap.of(
                    RailShape.NORTH_SOUTH,
                    Pair.of(vec3i2, vec3i3),
                    RailShape.EAST_WEST,
                    Pair.of(vec3i, vec3i1),
                    RailShape.ASCENDING_EAST,
                    Pair.of(vec3i4, vec3i1),
                    RailShape.ASCENDING_WEST,
                    Pair.of(vec3i, vec3i5),
                    RailShape.ASCENDING_NORTH,
                    Pair.of(vec3i2, vec3i7),
                    RailShape.ASCENDING_SOUTH,
                    Pair.of(vec3i6, vec3i3),
                    RailShape.SOUTH_EAST,
                    Pair.of(vec3i3, vec3i1),
                    RailShape.SOUTH_WEST,
                    Pair.of(vec3i3, vec3i),
                    RailShape.NORTH_WEST,
                    Pair.of(vec3i2, vec3i),
                    RailShape.NORTH_EAST,
                    Pair.of(vec3i2, vec3i1)
                );
            }
        )
    );

    protected AbstractMinecart(EntityType<?> p_480411_, Level p_478375_) {
        super(p_480411_, p_478375_);
        this.blocksBuilding = true;
        if (useExperimentalMovement(p_478375_)) {
            this.behavior = new NewMinecartBehavior(this);
        } else {
            this.behavior = new OldMinecartBehavior(this);
        }
    }

    protected AbstractMinecart(EntityType<?> entityType, Level level, double x, double y, double z) {
        this(entityType, level);
        this.setInitialPos(x, y, z);
    }

    public void setInitialPos(double x, double y, double z) {
        this.setPos(x, y, z);
        this.xo = x;
        this.yo = y;
        this.zo = z;
    }

    public static <T extends AbstractMinecart> @Nullable T createMinecart(
        Level level,
        double x,
        double y,
        double z,
        EntityType<T> entityType,
        EntitySpawnReason spawnReason,
        ItemStack stack,
        @Nullable Player player
    ) {
        T t = (T)entityType.create(level, spawnReason);
        if (t != null) {
            t.setInitialPos(x, y, z);
            EntityType.createDefaultStackConfig(level, stack, player).accept(t);
            if (t.getBehavior() instanceof NewMinecartBehavior newminecartbehavior) {
                BlockPos blockpos = t.getCurrentBlockPosOrRailBelow();
                BlockState blockstate = level.getBlockState(blockpos);
                newminecartbehavior.adjustToRails(blockpos, blockstate, true);
            }
        }

        return t;
    }

    public MinecartBehavior getBehavior() {
        return this.behavior;
    }

    @Override
    protected Entity.MovementEmission getMovementEmission() {
        return Entity.MovementEmission.EVENTS;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder p_478599_) {
        super.defineSynchedData(p_478599_);
        p_478599_.define(DATA_ID_CUSTOM_DISPLAY_BLOCK, Optional.empty());
        p_478599_.define(DATA_ID_DISPLAY_OFFSET, this.getDefaultDisplayOffset());
    }

    @Override
    public boolean canCollideWith(Entity entity) {
        return AbstractBoat.canVehicleCollide(this, entity);
    }

    @Override
    public boolean isPushable() {
        return true;
    }

    @Override
    public Vec3 getRelativePortalPosition(Direction.Axis p_481417_, BlockUtil.FoundRectangle p_478366_) {
        return LivingEntity.resetForwardDirectionOfRelativePortalPosition(super.getRelativePortalPosition(p_481417_, p_478366_));
    }

    @Override
    protected Vec3 getPassengerAttachmentPoint(Entity p_479844_, EntityDimensions p_481765_, float p_481069_) {
        boolean flag = p_479844_ instanceof Villager || p_479844_ instanceof WanderingTrader;
        return flag ? LOWERED_PASSENGER_ATTACHMENT : super.getPassengerAttachmentPoint(p_479844_, p_481765_, p_481069_);
    }

    @Override
    public Vec3 getDismountLocationForPassenger(LivingEntity livingEntity) {
        Direction direction = this.getMotionDirection();
        if (direction.getAxis() == Direction.Axis.Y) {
            return super.getDismountLocationForPassenger(livingEntity);
        } else {
            int[][] aint = DismountHelper.offsetsForDirection(direction);
            BlockPos blockpos = this.blockPosition();
            BlockPos.MutableBlockPos blockpos$mutableblockpos = new BlockPos.MutableBlockPos();
            ImmutableList<Pose> immutablelist = livingEntity.getDismountPoses();

            for (Pose pose : immutablelist) {
                EntityDimensions entitydimensions = livingEntity.getDimensions(pose);
                float f = Math.min(entitydimensions.width(), 1.0F) / 2.0F;

                for (int i : POSE_DISMOUNT_HEIGHTS.get(pose)) {
                    for (int[] aint1 : aint) {
                        blockpos$mutableblockpos.set(blockpos.getX() + aint1[0], blockpos.getY() + i, blockpos.getZ() + aint1[1]);
                        double d0 = this.level()
                            .getBlockFloorHeight(
                                DismountHelper.nonClimbableShape(this.level(), blockpos$mutableblockpos),
                                () -> DismountHelper.nonClimbableShape(this.level(), blockpos$mutableblockpos.below())
                            );
                        if (DismountHelper.isBlockFloorValid(d0)) {
                            AABB aabb = new AABB(-f, 0.0, -f, f, entitydimensions.height(), f);
                            Vec3 vec3 = Vec3.upFromBottomCenterOf(blockpos$mutableblockpos, d0);
                            if (DismountHelper.canDismountTo(this.level(), livingEntity, aabb.move(vec3))) {
                                livingEntity.setPose(pose);
                                return vec3;
                            }
                        }
                    }
                }
            }

            double d1 = this.getBoundingBox().maxY;
            blockpos$mutableblockpos.set((double)blockpos.getX(), d1, (double)blockpos.getZ());

            for (Pose pose1 : immutablelist) {
                double d2 = livingEntity.getDimensions(pose1).height();
                int j = Mth.ceil(d1 - blockpos$mutableblockpos.getY() + d2);
                double d3 = DismountHelper.findCeilingFrom(
                    blockpos$mutableblockpos, j, p_480735_ -> this.level().getBlockState(p_480735_).getCollisionShape(this.level(), p_480735_)
                );
                if (d1 + d2 <= d3) {
                    livingEntity.setPose(pose1);
                    break;
                }
            }

            return super.getDismountLocationForPassenger(livingEntity);
        }
    }

    @Override
    protected float getBlockSpeedFactor() {
        BlockState blockstate = this.level().getBlockState(this.blockPosition());
        return blockstate.is(BlockTags.RAILS) ? 1.0F : super.getBlockSpeedFactor();
    }

    @Override
    public void animateHurt(float p_480003_) {
        this.setHurtDir(-this.getHurtDir());
        this.setHurtTime(10);
        this.setDamage(this.getDamage() + this.getDamage() * 10.0F);
    }

    @Override
    public boolean isPickable() {
        return !this.isRemoved();
    }

    public static Pair<Vec3i, Vec3i> exits(RailShape shape) {
        return EXITS.get(shape);
    }

    @Override
    public Direction getMotionDirection() {
        return this.behavior.getMotionDirection();
    }

    @Override
    protected double getDefaultGravity() {
        return this.isInWater() ? 0.005 : 0.04;
    }

    @Override
    public void tick() {
        if (this.getHurtTime() > 0) {
            this.setHurtTime(this.getHurtTime() - 1);
        }

        if (this.getDamage() > 0.0F) {
            this.setDamage(this.getDamage() - 1.0F);
        }

        this.checkBelowWorld();
        this.computeSpeed();
        this.handlePortal();
        this.behavior.tick();
        this.updateInWaterStateAndDoFluidPushing();
        if (this.isInLava()) {
            this.lavaIgnite();
            this.lavaHurt();
            this.fallDistance *= 0.5;
        }

        this.firstTick = false;
    }

    public boolean isFirstTick() {
        return this.firstTick;
    }

    public BlockPos getCurrentBlockPosOrRailBelow() {
        int i = Mth.floor(this.getX());
        int j = Mth.floor(this.getY());
        int k = Mth.floor(this.getZ());
        if (useExperimentalMovement(this.level())) {
            double d0 = this.getY() - 0.1 - 1.0E-5F;
            if (this.level().getBlockState(BlockPos.containing(i, d0, k)).is(BlockTags.RAILS)) {
                j = Mth.floor(d0);
            }
        } else if (this.level().getBlockState(new BlockPos(i, j - 1, k)).is(BlockTags.RAILS)) {
            j--;
        }

        return new BlockPos(i, j, k);
    }

    protected double getMaxSpeed(ServerLevel level) {
        return this.behavior.getMaxSpeed(level);
    }

    public void activateMinecart(ServerLevel level, int x, int y, int z, boolean isPowered) {
    }

    @Override
    public void lerpPositionAndRotationStep(int p_479804_, double p_478696_, double p_479883_, double p_478833_, double p_480759_, double p_481321_) {
        super.lerpPositionAndRotationStep(p_479804_, p_478696_, p_479883_, p_478833_, p_480759_, p_481321_);
    }

    @Override
    public void applyGravity() {
        super.applyGravity();
    }

    @Override
    public void reapplyPosition() {
        super.reapplyPosition();
    }

    @Override
    public boolean updateInWaterStateAndDoFluidPushing() {
        return super.updateInWaterStateAndDoFluidPushing();
    }

    @Override
    public Vec3 getKnownMovement() {
        return this.behavior.getKnownMovement(super.getKnownMovement());
    }

    @Override
    public InterpolationHandler getInterpolation() {
        return this.behavior.getInterpolation();
    }

    @Override
    public void recreateFromPacket(ClientboundAddEntityPacket p_479751_) {
        super.recreateFromPacket(p_479751_);
        this.behavior.lerpMotion(this.getDeltaMovement());
    }

    @Override
    public void lerpMotion(Vec3 p_479142_) {
        this.behavior.lerpMotion(p_479142_);
    }

    protected void moveAlongTrack(ServerLevel level) {
        this.behavior.moveAlongTrack(level);
    }

    protected void comeOffTrack(ServerLevel level) {
        double d0 = this.getMaxSpeed(level);
        Vec3 vec3 = this.getDeltaMovement();
        this.setDeltaMovement(Mth.clamp(vec3.x, -d0, d0), vec3.y, Mth.clamp(vec3.z, -d0, d0));
        if (this.onGround()) {
            this.setDeltaMovement(this.getDeltaMovement().scale(0.5));
        }

        this.move(MoverType.SELF, this.getDeltaMovement());
        if (!this.onGround()) {
            this.setDeltaMovement(this.getDeltaMovement().scale(0.95));
        }
    }

    protected double makeStepAlongTrack(BlockPos pos, RailShape railShape, double speed) {
        return this.behavior.stepAlongTrack(pos, railShape, speed);
    }

    @Override
    public void move(MoverType p_478878_, Vec3 p_478468_) {
        if (useExperimentalMovement(this.level())) {
            Vec3 vec3 = this.position().add(p_478468_);
            super.move(p_478878_, p_478468_);
            boolean flag = this.behavior.pushAndPickupEntities();
            if (flag) {
                super.move(p_478878_, vec3.subtract(this.position()));
            }

            if (p_478878_.equals(MoverType.PISTON)) {
                this.onRails = false;
            }
        } else {
            super.move(p_478878_, p_478468_);
            this.applyEffectsFromBlocks();
        }
    }

    @Override
    public void applyEffectsFromBlocks() {
        if (useExperimentalMovement(this.level())) {
            super.applyEffectsFromBlocks();
        } else {
            this.applyEffectsFromBlocks(this.position(), this.position());
            this.clearMovementThisTick();
        }
    }

    @Override
    public boolean isOnRails() {
        return this.onRails;
    }

    public void setOnRails(boolean onRails) {
        this.onRails = onRails;
    }

    public boolean isFlipped() {
        return this.flipped;
    }

    public void setFlipped(boolean flipped) {
        this.flipped = flipped;
    }

    public Vec3 getRedstoneDirection(BlockPos pos) {
        BlockState blockstate = this.level().getBlockState(pos);
        if (blockstate.getBlock() instanceof PoweredRailBlock poweredRail && !poweredRail.isActivatorRail() && blockstate.getValue(PoweredRailBlock.POWERED)) {
            RailShape railshape = ((BaseRailBlock)blockstate.getBlock()).getRailDirection(blockstate, this.level(), pos, this);
            if (railshape == RailShape.EAST_WEST) {
                if (this.isRedstoneConductor(pos.west())) {
                    return new Vec3(1.0, 0.0, 0.0);
                }

                if (this.isRedstoneConductor(pos.east())) {
                    return new Vec3(-1.0, 0.0, 0.0);
                }
            } else if (railshape == RailShape.NORTH_SOUTH) {
                if (this.isRedstoneConductor(pos.north())) {
                    return new Vec3(0.0, 0.0, 1.0);
                }

                if (this.isRedstoneConductor(pos.south())) {
                    return new Vec3(0.0, 0.0, -1.0);
                }
            }

            return Vec3.ZERO;
        } else {
            return Vec3.ZERO;
        }
    }

    public boolean isRedstoneConductor(BlockPos pos) {
        return this.level().getBlockState(pos).isRedstoneConductor(this.level(), pos);
    }

    protected Vec3 applyNaturalSlowdown(Vec3 speed) {
        double d0 = this.behavior.getSlowdownFactor();
        Vec3 vec3 = speed.multiply(d0, 0.0, d0);
        if (this.isInWater()) {
            vec3 = vec3.scale(0.95F);
        }

        return vec3;
    }

    @Override
    protected void readAdditionalSaveData(ValueInput p_478118_) {
        this.setCustomDisplayBlockState(p_478118_.read("DisplayState", BlockState.CODEC));
        this.setDisplayOffset(p_478118_.getIntOr("DisplayOffset", this.getDefaultDisplayOffset()));
        this.flipped = p_478118_.getBooleanOr("FlippedRotation", false);
        this.firstTick = p_478118_.getBooleanOr("HasTicked", false);
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput p_479247_) {
        this.getCustomDisplayBlockState().ifPresent(p_481977_ -> p_479247_.store("DisplayState", BlockState.CODEC, p_481977_));
        int i = this.getDisplayOffset();
        if (i != this.getDefaultDisplayOffset()) {
            p_479247_.putInt("DisplayOffset", i);
        }

        p_479247_.putBoolean("FlippedRotation", this.flipped);
        p_479247_.putBoolean("HasTicked", this.firstTick);
    }

    /**
     * Applies a velocity to the entities, to push them away from each other.
     */
    @Override
    public void push(Entity entity) {
        if (!this.level().isClientSide()) {
            if (!entity.noPhysics && !this.noPhysics) {
                if (!this.hasPassenger(entity)) {
                    double d0 = entity.getX() - this.getX();
                    double d1 = entity.getZ() - this.getZ();
                    double d2 = d0 * d0 + d1 * d1;
                    if (d2 >= 1.0E-4F) {
                        d2 = Math.sqrt(d2);
                        d0 /= d2;
                        d1 /= d2;
                        double d3 = 1.0 / d2;
                        if (d3 > 1.0) {
                            d3 = 1.0;
                        }

                        d0 *= d3;
                        d1 *= d3;
                        d0 *= 0.1F;
                        d1 *= 0.1F;
                        d0 *= 0.5;
                        d1 *= 0.5;
                        if (entity instanceof AbstractMinecart abstractminecart) {
                            this.pushOtherMinecart(abstractminecart, d0, d1);
                        } else {
                            this.push(-d0, 0.0, -d1);
                            entity.push(d0 / 4.0, 0.0, d1 / 4.0);
                        }
                    }
                }
            }
        }
    }

    private void pushOtherMinecart(AbstractMinecart otherMinecart, double deltaX, double deltaZ) {
        double d0;
        double d1;
        if (useExperimentalMovement(this.level())) {
            d0 = this.getDeltaMovement().x;
            d1 = this.getDeltaMovement().z;
        } else {
            d0 = otherMinecart.getX() - this.getX();
            d1 = otherMinecart.getZ() - this.getZ();
        }

        Vec3 vec3 = new Vec3(d0, 0.0, d1).normalize();
        Vec3 vec31 = new Vec3(Mth.cos(this.getYRot() * (float) (Math.PI / 180.0)), 0.0, Mth.sin(this.getYRot() * (float) (Math.PI / 180.0))).normalize();
        double d2 = Math.abs(vec3.dot(vec31));
        if (!(d2 < 0.8F) || useExperimentalMovement(this.level())) {
            Vec3 vec32 = this.getDeltaMovement();
            Vec3 vec33 = otherMinecart.getDeltaMovement();
            if (otherMinecart.isFurnace() && !this.isFurnace()) {
                this.setDeltaMovement(vec32.multiply(0.2, 1.0, 0.2));
                this.push(vec33.x - deltaX, 0.0, vec33.z - deltaZ);
                otherMinecart.setDeltaMovement(vec33.multiply(0.95, 1.0, 0.95));
            } else if (!otherMinecart.isFurnace() && this.isFurnace()) {
                otherMinecart.setDeltaMovement(vec33.multiply(0.2, 1.0, 0.2));
                otherMinecart.push(vec32.x + deltaX, 0.0, vec32.z + deltaZ);
                this.setDeltaMovement(vec32.multiply(0.95, 1.0, 0.95));
            } else {
                double d3 = (vec33.x + vec32.x) / 2.0;
                double d4 = (vec33.z + vec32.z) / 2.0;
                this.setDeltaMovement(vec32.multiply(0.2, 1.0, 0.2));
                this.push(d3 - deltaX, 0.0, d4 - deltaZ);
                otherMinecart.setDeltaMovement(vec33.multiply(0.2, 1.0, 0.2));
                otherMinecart.push(d3 + deltaX, 0.0, d4 + deltaZ);
            }
        }
    }

    public BlockState getDisplayBlockState() {
        return this.getCustomDisplayBlockState().orElseGet(this::getDefaultDisplayBlockState);
    }

    private Optional<BlockState> getCustomDisplayBlockState() {
        return this.getEntityData().get(DATA_ID_CUSTOM_DISPLAY_BLOCK);
    }

    public BlockState getDefaultDisplayBlockState() {
        return Blocks.AIR.defaultBlockState();
    }

    public int getDisplayOffset() {
        return this.getEntityData().get(DATA_ID_DISPLAY_OFFSET);
    }

    public int getDefaultDisplayOffset() {
        return 6;
    }

    public void setCustomDisplayBlockState(Optional<BlockState> customDisplayBlockState) {
        this.getEntityData().set(DATA_ID_CUSTOM_DISPLAY_BLOCK, customDisplayBlockState);
    }

    public void setDisplayOffset(int displayOffset) {
        this.getEntityData().set(DATA_ID_DISPLAY_OFFSET, displayOffset);
    }

    public static boolean useExperimentalMovement(Level level) {
        return level.enabledFeatures().contains(FeatureFlags.MINECART_IMPROVEMENTS);
    }

    @Override
    public abstract ItemStack getPickResult();

    public boolean isRideable() {
        return false;
    }

    public boolean isFurnace() {
        return false;
    }
}
