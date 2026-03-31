package net.minecraft.world.entity.ai.behavior;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.behavior.declarative.BehaviorBuilder;
import net.minecraft.world.entity.ai.behavior.declarative.MemoryAccessor;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.WalkTarget;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.shapes.CollisionContext;
import org.apache.commons.lang3.mutable.MutableLong;

public class TryFindLandNearWater {
    public static BehaviorControl<PathfinderMob> create(int range, float speedModifier) {
        MutableLong mutablelong = new MutableLong(0L);
        return BehaviorBuilder.create(
            p_260348_ -> p_260348_.group(
                    p_260348_.absent(MemoryModuleType.ATTACK_TARGET),
                    p_260348_.absent(MemoryModuleType.WALK_TARGET),
                    p_260348_.registered(MemoryModuleType.LOOK_TARGET)
                )
                .apply(
                    p_260348_,
                    (p_259029_, p_259100_, p_259367_) -> (p_460516_, p_460517_, p_460518_) -> {
                        if (p_460516_.getFluidState(p_460517_.blockPosition()).is(FluidTags.WATER)) {
                            return false;
                        } else if (p_460518_ < mutablelong.longValue()) {
                            mutablelong.setValue(p_460518_ + 40L);
                            return true;
                        } else {
                            CollisionContext collisioncontext = CollisionContext.of(p_460517_);
                            BlockPos blockpos = p_460517_.blockPosition();
                            BlockPos.MutableBlockPos blockpos$mutableblockpos = new BlockPos.MutableBlockPos();

                            label45:
                            for (BlockPos blockpos1 : BlockPos.withinManhattan(blockpos, range, range, range)) {
                                if ((blockpos1.getX() != blockpos.getX() || blockpos1.getZ() != blockpos.getZ())
                                    && p_460516_.getBlockState(blockpos1).getCollisionShape(p_460516_, blockpos1, collisioncontext).isEmpty()
                                    && !p_460516_.getBlockState(blockpos$mutableblockpos.setWithOffset(blockpos1, Direction.DOWN))
                                        .getCollisionShape(p_460516_, blockpos1, collisioncontext)
                                        .isEmpty()) {
                                    for (Direction direction : Direction.Plane.HORIZONTAL) {
                                        blockpos$mutableblockpos.setWithOffset(blockpos1, direction);
                                        if (p_460516_.getBlockState(blockpos$mutableblockpos).isAir()
                                            && p_460516_.getBlockState(blockpos$mutableblockpos.move(Direction.DOWN)).is(Blocks.WATER)) {
                                            p_259367_.set(new BlockPosTracker(blockpos1));
                                            p_259100_.set(new WalkTarget(new BlockPosTracker(blockpos1), speedModifier, 0));
                                            break label45;
                                        }
                                    }
                                }
                            }

                            mutablelong.setValue(p_460518_ + 40L);
                            return true;
                        }
                    }
                )
        );
    }
}
