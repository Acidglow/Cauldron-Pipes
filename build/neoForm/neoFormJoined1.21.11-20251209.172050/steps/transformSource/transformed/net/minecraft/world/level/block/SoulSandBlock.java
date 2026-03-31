package net.minecraft.world.level.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

public class SoulSandBlock extends Block {
    public static final MapCodec<SoulSandBlock> CODEC = simpleCodec(SoulSandBlock::new);
    private static final VoxelShape SHAPE = Block.column(16.0, 0.0, 14.0);
    private static final int BUBBLE_COLUMN_CHECK_DELAY = 20;

    @Override
    public MapCodec<SoulSandBlock> codec() {
        return CODEC;
    }

    public SoulSandBlock(BlockBehaviour.Properties p_56672_) {
        super(p_56672_);
    }

    @Override
    protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    protected VoxelShape getBlockSupportShape(BlockState state, BlockGetter reader, BlockPos pos) {
        return Shapes.block();
    }

    @Override
    protected VoxelShape getVisualShape(BlockState state, BlockGetter reader, BlockPos pos, CollisionContext context) {
        return Shapes.block();
    }

    @Override
    protected void tick(BlockState p_222457_, ServerLevel p_222458_, BlockPos p_222459_, RandomSource p_222460_) {
        BubbleColumnBlock.updateColumn(p_222458_, p_222459_.above(), p_222457_);
    }

    @Override
    protected BlockState updateShape(
        BlockState p_56689_,
        LevelReader p_374561_,
        ScheduledTickAccess p_374416_,
        BlockPos p_56693_,
        Direction p_56690_,
        BlockPos p_56694_,
        BlockState p_56691_,
        RandomSource p_374114_
    ) {
        if (p_56690_ == Direction.UP && p_56691_.is(Blocks.WATER)) {
            p_374416_.scheduleTick(p_56693_, this, 20);
        }

        return super.updateShape(p_56689_, p_374561_, p_374416_, p_56693_, p_56690_, p_56694_, p_56691_, p_374114_);
    }

    @Override
    protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean isMoving) {
        level.scheduleTick(pos, this, 20);
    }

    @Override
    protected boolean isPathfindable(BlockState p_56679_, PathComputationType p_56682_) {
        return false;
    }

    @Override
    protected float getShadeBrightness(BlockState p_222462_, BlockGetter p_222463_, BlockPos p_222464_) {
        return 0.2F;
    }
}
