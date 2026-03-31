package net.minecraft.world.item.context;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

public class BlockPlaceContext extends UseOnContext {
    private final BlockPos relativePos;
    protected boolean replaceClicked = true;

    public BlockPlaceContext(Player player, InteractionHand hand, ItemStack itemStack, BlockHitResult hitResult) {
        this(player.level(), player, hand, itemStack, hitResult);
    }

    public BlockPlaceContext(UseOnContext context) {
        this(context.getLevel(), context.getPlayer(), context.getHand(), context.getItemInHand(), context.getHitResult());
    }

    public BlockPlaceContext(Level p_43638_, @Nullable Player p_43639_, InteractionHand p_43640_, ItemStack p_43641_, BlockHitResult p_43642_) {
        super(p_43638_, p_43639_, p_43640_, p_43641_, p_43642_);
        this.relativePos = p_43642_.getBlockPos().relative(p_43642_.getDirection());
        this.replaceClicked = p_43638_.getBlockState(p_43642_.getBlockPos()).canBeReplaced(this);
    }

    public static BlockPlaceContext at(BlockPlaceContext context, BlockPos pos, Direction direction) {
        return new BlockPlaceContext(
            context.getLevel(),
            context.getPlayer(),
            context.getHand(),
            context.getItemInHand(),
            new BlockHitResult(
                new Vec3(
                    pos.getX() + 0.5 + direction.getStepX() * 0.5,
                    pos.getY() + 0.5 + direction.getStepY() * 0.5,
                    pos.getZ() + 0.5 + direction.getStepZ() * 0.5
                ),
                direction,
                pos,
                false
            )
        );
    }

    @Override
    public BlockPos getClickedPos() {
        return this.replaceClicked ? super.getClickedPos() : this.relativePos;
    }

    public boolean canPlace() {
        return this.replaceClicked || this.getLevel().getBlockState(this.getClickedPos()).canBeReplaced(this);
    }

    public boolean replacingClickedOnBlock() {
        return this.replaceClicked;
    }

    public Direction getNearestLookingDirection() {
        return Direction.orderedByNearest(this.getPlayer())[0];
    }

    public Direction getNearestLookingVerticalDirection() {
        return Direction.getFacingAxis(this.getPlayer(), Direction.Axis.Y);
    }

    public Direction[] getNearestLookingDirections() {
        Direction[] adirection = Direction.orderedByNearest(this.getPlayer());
        if (this.replaceClicked) {
            return adirection;
        } else {
            Direction direction = this.getClickedFace();
            int i = 0;

            while (i < adirection.length && adirection[i] != direction.getOpposite()) {
                i++;
            }

            if (i > 0) {
                System.arraycopy(adirection, 0, adirection, 1, i);
                adirection[0] = direction.getOpposite();
            }

            return adirection;
        }
    }
}
