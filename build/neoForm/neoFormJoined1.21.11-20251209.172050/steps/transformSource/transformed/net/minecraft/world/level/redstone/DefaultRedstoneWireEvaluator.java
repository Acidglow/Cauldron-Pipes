package net.minecraft.world.level.redstone;

import com.google.common.collect.Sets;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.RedStoneWireBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;

public class DefaultRedstoneWireEvaluator extends RedstoneWireEvaluator {
    public DefaultRedstoneWireEvaluator(RedStoneWireBlock p_363937_) {
        super(p_363937_);
    }

    @Override
    public void updatePowerStrength(Level p_362666_, BlockPos p_365481_, BlockState p_361516_, @Nullable Orientation p_362350_, boolean p_366607_) {
        int i = this.calculateTargetStrength(p_362666_, p_365481_);
        if (p_361516_.getValue(RedStoneWireBlock.POWER) != i) {
            if (p_362666_.getBlockState(p_365481_) == p_361516_) {
                p_362666_.setBlock(p_365481_, p_361516_.setValue(RedStoneWireBlock.POWER, i), 2);
            }

            Set<BlockPos> set = Sets.newHashSet();
            set.add(p_365481_);

            for (Direction direction : Direction.values()) {
                set.add(p_365481_.relative(direction));
            }

            for (BlockPos blockpos : set) {
                p_362666_.updateNeighborsAt(blockpos, this.wireBlock);
            }
        }
    }

    private int calculateTargetStrength(Level level, BlockPos pos) {
        int i = this.getBlockSignal(level, pos);
        return i == 15 ? i : Math.max(i, this.getIncomingWireSignal(level, pos));
    }
}
