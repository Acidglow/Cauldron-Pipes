package net.minecraft.world.phys.shapes;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.vehicle.minecart.AbstractMinecart;
import net.minecraft.world.level.CollisionGetter;
import net.minecraft.world.level.block.BaseRailBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.RailShape;
import org.jspecify.annotations.Nullable;

public class MinecartCollisionContext extends EntityCollisionContext {
    private @Nullable BlockPos ingoreBelow;
    private @Nullable BlockPos slopeIgnore;

    protected MinecartCollisionContext(AbstractMinecart minecart, boolean alwaysCollideWithFluid) {
        super(minecart, alwaysCollideWithFluid, false);
        this.setupContext(minecart);
    }

    private void setupContext(AbstractMinecart minecart) {
        BlockPos blockpos = minecart.getCurrentBlockPosOrRailBelow();
        BlockState blockstate = minecart.level().getBlockState(blockpos);
        boolean flag = BaseRailBlock.isRail(blockstate);
        if (flag) {
            this.ingoreBelow = blockpos.below();
            RailShape railshape = ((BaseRailBlock)blockstate.getBlock()).getRailDirection(blockstate, minecart.level(), blockpos, minecart);
            if (railshape.isSlope()) {
                this.slopeIgnore = switch (railshape) {
                    case ASCENDING_EAST -> blockpos.east();
                    case ASCENDING_WEST -> blockpos.west();
                    case ASCENDING_NORTH -> blockpos.north();
                    case ASCENDING_SOUTH -> blockpos.south();
                    default -> null;
                };
            }
        }
    }

    @Override
    public VoxelShape getCollisionShape(BlockState p_366641_, CollisionGetter p_366650_, BlockPos p_366424_) {
        return !p_366424_.equals(this.ingoreBelow) && !p_366424_.equals(this.slopeIgnore)
            ? super.getCollisionShape(p_366641_, p_366650_, p_366424_)
            : Shapes.empty();
    }
}
