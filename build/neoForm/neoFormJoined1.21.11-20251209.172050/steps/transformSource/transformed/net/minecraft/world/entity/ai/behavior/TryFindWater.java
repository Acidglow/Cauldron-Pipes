package net.minecraft.world.entity.ai.behavior;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.behavior.declarative.BehaviorBuilder;
import net.minecraft.world.entity.ai.behavior.declarative.MemoryAccessor;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.WalkTarget;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import org.apache.commons.lang3.mutable.MutableLong;

public class TryFindWater {
    public static BehaviorControl<PathfinderMob> create(int range, float speedModifier) {
        MutableLong mutablelong = new MutableLong(0L);
        return BehaviorBuilder.create(
            p_260101_ -> p_260101_.group(
                    p_260101_.absent(MemoryModuleType.ATTACK_TARGET),
                    p_260101_.absent(MemoryModuleType.WALK_TARGET),
                    p_260101_.registered(MemoryModuleType.LOOK_TARGET)
                )
                .apply(p_260101_, (p_259124_, p_259692_, p_259819_) -> (p_460524_, p_460525_, p_460526_) -> {
                    if (p_460524_.getFluidState(p_460525_.blockPosition()).is(FluidTags.WATER)) {
                        return false;
                    } else if (p_460526_ < mutablelong.longValue()) {
                        mutablelong.setValue(p_460526_ + 20L + 2L);
                        return true;
                    } else {
                        BlockPos blockpos = null;
                        BlockPos blockpos1 = null;
                        BlockPos blockpos2 = p_460525_.blockPosition();

                        for (BlockPos blockpos3 : BlockPos.withinManhattan(blockpos2, range, range, range)) {
                            if (blockpos3.getX() != blockpos2.getX() || blockpos3.getZ() != blockpos2.getZ()) {
                                BlockState blockstate = p_460525_.level().getBlockState(blockpos3.above());
                                BlockState blockstate1 = p_460525_.level().getBlockState(blockpos3);
                                if (blockstate1.is(Blocks.WATER)) {
                                    if (blockstate.isAir()) {
                                        blockpos = blockpos3.immutable();
                                        break;
                                    }

                                    if (blockpos1 == null && !blockpos3.closerToCenterThan(p_460525_.position(), 1.5)) {
                                        blockpos1 = blockpos3.immutable();
                                    }
                                }
                            }
                        }

                        if (blockpos == null) {
                            blockpos = blockpos1;
                        }

                        if (blockpos != null) {
                            p_259819_.set(new BlockPosTracker(blockpos));
                            p_259692_.set(new WalkTarget(new BlockPosTracker(blockpos), speedModifier, 0));
                        }

                        mutablelong.setValue(p_460526_ + 40L);
                        return true;
                    }
                })
        );
    }
}
