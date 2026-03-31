package net.minecraft.world.entity.monster.breeze;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.Lists;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import java.util.Map;
import java.util.Optional;
import net.minecraft.commands.arguments.EntityAnchorArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.RandomSource;
import net.minecraft.util.Unit;
import net.minecraft.util.Util;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.behavior.Behavior;
import net.minecraft.world.entity.ai.behavior.LongJumpUtil;
import net.minecraft.world.entity.ai.behavior.Swim;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.MemoryStatus;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

public class LongJump extends Behavior<Breeze> {
    private static final int REQUIRED_AIR_BLOCKS_ABOVE = 4;
    private static final int JUMP_COOLDOWN_TICKS = 10;
    private static final int JUMP_COOLDOWN_WHEN_HURT_TICKS = 2;
    private static final int INHALING_DURATION_TICKS = Math.round(10.0F);
    private static final float DEFAULT_FOLLOW_RANGE = 24.0F;
    private static final float DEFAULT_MAX_JUMP_VELOCITY = 1.4F;
    private static final float MAX_JUMP_VELOCITY_MULTIPLIER = 0.058333334F;
    private static final ObjectArrayList<Integer> ALLOWED_ANGLES = new ObjectArrayList<>(Lists.newArrayList(40, 55, 60, 75, 80));

    @VisibleForTesting
    public LongJump() {
        super(
            Map.of(
                MemoryModuleType.ATTACK_TARGET,
                MemoryStatus.VALUE_PRESENT,
                MemoryModuleType.BREEZE_JUMP_COOLDOWN,
                MemoryStatus.VALUE_ABSENT,
                MemoryModuleType.BREEZE_JUMP_INHALING,
                MemoryStatus.REGISTERED,
                MemoryModuleType.BREEZE_JUMP_TARGET,
                MemoryStatus.REGISTERED,
                MemoryModuleType.BREEZE_SHOOT,
                MemoryStatus.VALUE_ABSENT,
                MemoryModuleType.WALK_TARGET,
                MemoryStatus.VALUE_ABSENT,
                MemoryModuleType.BREEZE_LEAVING_WATER,
                MemoryStatus.REGISTERED
            ),
            200
        );
    }

    public static boolean canRun(ServerLevel level, Breeze breeze) {
        if (!breeze.onGround() && !breeze.isInWater()) {
            return false;
        } else if (Swim.shouldSwim(breeze)) {
            return false;
        } else if (breeze.getBrain().checkMemory(MemoryModuleType.BREEZE_JUMP_TARGET, MemoryStatus.VALUE_PRESENT)) {
            return true;
        } else {
            LivingEntity livingentity = breeze.getBrain().getMemory(MemoryModuleType.ATTACK_TARGET).orElse(null);
            if (livingentity == null) {
                return false;
            } else if (outOfAggroRange(breeze, livingentity)) {
                breeze.getBrain().eraseMemory(MemoryModuleType.ATTACK_TARGET);
                return false;
            } else if (tooCloseForJump(breeze, livingentity)) {
                return false;
            } else if (!canJumpFromCurrentPosition(level, breeze)) {
                return false;
            } else {
                BlockPos blockpos = snapToSurface(breeze, BreezeUtil.randomPointBehindTarget(livingentity, breeze.getRandom()));
                if (blockpos == null) {
                    return false;
                } else {
                    BlockState blockstate = level.getBlockState(blockpos.below());
                    if (breeze.getType().isBlockDangerous(blockstate)) {
                        return false;
                    } else if (!BreezeUtil.hasLineOfSight(breeze, blockpos.getCenter())
                        && !BreezeUtil.hasLineOfSight(breeze, blockpos.above(4).getCenter())) {
                        return false;
                    } else {
                        breeze.getBrain().setMemory(MemoryModuleType.BREEZE_JUMP_TARGET, blockpos);
                        return true;
                    }
                }
            }
        }
    }

    protected boolean checkExtraStartConditions(ServerLevel p_312131_, Breeze p_312686_) {
        return canRun(p_312131_, p_312686_);
    }

    protected boolean canStillUse(ServerLevel p_312482_, Breeze p_312019_, long p_312448_) {
        return p_312019_.getPose() != Pose.STANDING && !p_312019_.getBrain().hasMemoryValue(MemoryModuleType.BREEZE_JUMP_COOLDOWN);
    }

    protected void start(ServerLevel p_312817_, Breeze p_311902_, long p_312420_) {
        if (p_311902_.getBrain().checkMemory(MemoryModuleType.BREEZE_JUMP_INHALING, MemoryStatus.VALUE_ABSENT)) {
            p_311902_.getBrain().setMemoryWithExpiry(MemoryModuleType.BREEZE_JUMP_INHALING, Unit.INSTANCE, INHALING_DURATION_TICKS);
        }

        p_311902_.setPose(Pose.INHALING);
        p_312817_.playSound(null, p_311902_, SoundEvents.BREEZE_CHARGE, SoundSource.HOSTILE, 1.0F, 1.0F);
        p_311902_.getBrain()
            .getMemory(MemoryModuleType.BREEZE_JUMP_TARGET)
            .ifPresent(p_312818_ -> p_311902_.lookAt(EntityAnchorArgument.Anchor.EYES, p_312818_.getCenter()));
    }

    protected void tick(ServerLevel p_312091_, Breeze p_312923_, long p_312404_) {
        boolean flag = p_312923_.isInWater();
        if (!flag && p_312923_.getBrain().checkMemory(MemoryModuleType.BREEZE_LEAVING_WATER, MemoryStatus.VALUE_PRESENT)) {
            p_312923_.getBrain().eraseMemory(MemoryModuleType.BREEZE_LEAVING_WATER);
        }

        if (isFinishedInhaling(p_312923_)) {
            Vec3 vec3 = p_312923_.getBrain()
                .getMemory(MemoryModuleType.BREEZE_JUMP_TARGET)
                .flatMap(p_427114_ -> calculateOptimalJumpVector(p_312923_, p_312923_.getRandom(), Vec3.atBottomCenterOf(p_427114_)))
                .orElse(null);
            if (vec3 == null) {
                p_312923_.setPose(Pose.STANDING);
                return;
            }

            if (flag) {
                p_312923_.getBrain().setMemory(MemoryModuleType.BREEZE_LEAVING_WATER, Unit.INSTANCE);
            }

            p_312923_.playSound(SoundEvents.BREEZE_JUMP, 1.0F, 1.0F);
            p_312923_.setPose(Pose.LONG_JUMPING);
            p_312923_.setYRot(p_312923_.yBodyRot);
            p_312923_.setDiscardFriction(true);
            p_312923_.setDeltaMovement(vec3);
        } else if (isFinishedJumping(p_312923_)) {
            p_312923_.playSound(SoundEvents.BREEZE_LAND, 1.0F, 1.0F);
            p_312923_.setPose(Pose.STANDING);
            p_312923_.setDiscardFriction(false);
            boolean flag1 = p_312923_.getBrain().hasMemoryValue(MemoryModuleType.HURT_BY);
            p_312923_.getBrain().setMemoryWithExpiry(MemoryModuleType.BREEZE_JUMP_COOLDOWN, Unit.INSTANCE, flag1 ? 2L : 10L);
            p_312923_.getBrain().setMemoryWithExpiry(MemoryModuleType.BREEZE_SHOOT, Unit.INSTANCE, 100L);
        }
    }

    protected void stop(ServerLevel p_312766_, Breeze p_312924_, long p_312793_) {
        if (p_312924_.getPose() == Pose.LONG_JUMPING || p_312924_.getPose() == Pose.INHALING) {
            p_312924_.setPose(Pose.STANDING);
        }

        p_312924_.getBrain().eraseMemory(MemoryModuleType.BREEZE_JUMP_TARGET);
        p_312924_.getBrain().eraseMemory(MemoryModuleType.BREEZE_JUMP_INHALING);
        p_312924_.getBrain().eraseMemory(MemoryModuleType.BREEZE_LEAVING_WATER);
    }

    private static boolean isFinishedInhaling(Breeze breeze) {
        return breeze.getBrain().getMemory(MemoryModuleType.BREEZE_JUMP_INHALING).isEmpty() && breeze.getPose() == Pose.INHALING;
    }

    private static boolean isFinishedJumping(Breeze breeze) {
        boolean flag = breeze.getPose() == Pose.LONG_JUMPING;
        boolean flag1 = breeze.onGround();
        boolean flag2 = breeze.isInWater() && breeze.getBrain().checkMemory(MemoryModuleType.BREEZE_LEAVING_WATER, MemoryStatus.VALUE_ABSENT);
        return flag && (flag1 || flag2);
    }

    private static @Nullable BlockPos snapToSurface(LivingEntity owner, Vec3 targetPos) {
        ClipContext clipcontext = new ClipContext(
            targetPos, targetPos.relative(Direction.DOWN, 10.0), ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, owner
        );
        HitResult hitresult = owner.level().clip(clipcontext);
        if (hitresult.getType() == HitResult.Type.BLOCK) {
            return BlockPos.containing(hitresult.getLocation()).above();
        } else {
            ClipContext clipcontext1 = new ClipContext(
                targetPos, targetPos.relative(Direction.UP, 10.0), ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, owner
            );
            HitResult hitresult1 = owner.level().clip(clipcontext1);
            return hitresult1.getType() == HitResult.Type.BLOCK ? BlockPos.containing(hitresult1.getLocation()).above() : null;
        }
    }

    private static boolean outOfAggroRange(Breeze breeze, LivingEntity target) {
        return !target.closerThan(breeze, breeze.getAttributeValue(Attributes.FOLLOW_RANGE));
    }

    private static boolean tooCloseForJump(Breeze breeze, LivingEntity target) {
        return target.distanceTo(breeze) - 4.0F <= 0.0F;
    }

    private static boolean canJumpFromCurrentPosition(ServerLevel level, Breeze breeze) {
        BlockPos blockpos = breeze.blockPosition();
        if (level.getBlockState(blockpos).is(Blocks.HONEY_BLOCK)) {
            return false;
        } else {
            for (int i = 1; i <= 4; i++) {
                BlockPos blockpos1 = blockpos.relative(Direction.UP, i);
                if (!level.getBlockState(blockpos1).isAir() && !level.getFluidState(blockpos1).is(FluidTags.WATER)) {
                    return false;
                }
            }

            return true;
        }
    }

    private static Optional<Vec3> calculateOptimalJumpVector(Breeze breeze, RandomSource random, Vec3 target) {
        for (int i : Util.shuffledCopy(ALLOWED_ANGLES, random)) {
            float f = 0.058333334F * (float)breeze.getAttributeValue(Attributes.FOLLOW_RANGE);
            Optional<Vec3> optional = LongJumpUtil.calculateJumpVectorForAngle(breeze, target, f, i, false);
            if (optional.isPresent()) {
                if (breeze.hasEffect(MobEffects.JUMP_BOOST)) {
                    double d0 = optional.get().normalize().y * breeze.getJumpBoostPower();
                    return optional.map(p_381521_ -> p_381521_.add(0.0, d0, 0.0));
                }

                return optional;
            }
        }

        return Optional.empty();
    }
}
