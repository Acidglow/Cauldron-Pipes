package net.minecraft.world.level.block.piston;

import com.mojang.serialization.MapCodec;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DirectionalBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.PistonType;
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraft.world.level.redstone.ExperimentalRedstoneUtils;
import net.minecraft.world.level.redstone.Orientation;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jspecify.annotations.Nullable;

public class PistonHeadBlock extends DirectionalBlock {
    public static final MapCodec<PistonHeadBlock> CODEC = simpleCodec(PistonHeadBlock::new);
    public static final EnumProperty<PistonType> TYPE = BlockStateProperties.PISTON_TYPE;
    public static final BooleanProperty SHORT = BlockStateProperties.SHORT;
    public static final int PLATFORM_THICKNESS = 4;
    private static final VoxelShape SHAPE_PLATFORM = Block.boxZ(16.0, 0.0, 4.0);
    private static final Map<Direction, VoxelShape> SHAPES_SHORT = Shapes.rotateAll(Shapes.or(SHAPE_PLATFORM, Block.boxZ(4.0, 4.0, 16.0)));
    private static final Map<Direction, VoxelShape> SHAPES = Shapes.rotateAll(Shapes.or(SHAPE_PLATFORM, Block.boxZ(4.0, 4.0, 20.0)));

    @Override
    protected MapCodec<PistonHeadBlock> codec() {
        return CODEC;
    }

    public PistonHeadBlock(BlockBehaviour.Properties p_60259_) {
        super(p_60259_);
        this.registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(TYPE, PistonType.DEFAULT).setValue(SHORT, false));
    }

    @Override
    protected boolean useShapeForLightOcclusion(BlockState state) {
        return true;
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return (state.getValue(SHORT) ? SHAPES_SHORT : SHAPES).get(state.getValue(FACING));
    }

    private boolean isFittingBase(BlockState baseState, BlockState extendedState) {
        Block block = baseState.getValue(TYPE) == PistonType.DEFAULT ? Blocks.PISTON : Blocks.STICKY_PISTON;
        return extendedState.is(block) && extendedState.getValue(PistonBaseBlock.EXTENDED) && extendedState.getValue(FACING) == baseState.getValue(FACING);
    }

    @Override
    public BlockState playerWillDestroy(Level p_60265_, BlockPos p_60266_, BlockState p_60267_, Player p_60268_) {
        if (!p_60265_.isClientSide() && p_60268_.preventsBlockDrops()) {
            BlockPos blockpos = p_60266_.relative(p_60267_.getValue(FACING).getOpposite());
            if (this.isFittingBase(p_60267_, p_60265_.getBlockState(blockpos))) {
                p_60265_.destroyBlock(blockpos, false);
            }
        }

        return super.playerWillDestroy(p_60265_, p_60266_, p_60267_, p_60268_);
    }

    @Override
    protected void affectNeighborsAfterRemoval(BlockState p_393727_, ServerLevel p_394573_, BlockPos p_393756_, boolean p_394300_) {
        BlockPos blockpos = p_393756_.relative(p_393727_.getValue(FACING).getOpposite());
        if (this.isFittingBase(p_393727_, p_394573_.getBlockState(blockpos))) {
            p_394573_.destroyBlock(blockpos, true);
        }
    }

    @Override
    protected BlockState updateShape(
        BlockState p_60301_,
        LevelReader p_374256_,
        ScheduledTickAccess p_374426_,
        BlockPos p_60305_,
        Direction p_60302_,
        BlockPos p_60306_,
        BlockState p_60303_,
        RandomSource p_374478_
    ) {
        return p_60302_.getOpposite() == p_60301_.getValue(FACING) && !p_60301_.canSurvive(p_374256_, p_60305_)
            ? Blocks.AIR.defaultBlockState()
            : super.updateShape(p_60301_, p_374256_, p_374426_, p_60305_, p_60302_, p_60306_, p_60303_, p_374478_);
    }

    @Override
    protected boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        BlockState blockstate = level.getBlockState(pos.relative(state.getValue(FACING).getOpposite()));
        return this.isFittingBase(state, blockstate) || blockstate.is(Blocks.MOVING_PISTON) && blockstate.getValue(FACING) == state.getValue(FACING);
    }

    @Override
    protected void neighborChanged(BlockState p_60275_, Level p_60276_, BlockPos p_60277_, Block p_60278_, @Nullable Orientation p_363965_, boolean p_60280_) {
        if (p_60275_.canSurvive(p_60276_, p_60277_)) {
            p_60276_.neighborChanged(
                p_60277_.relative(p_60275_.getValue(FACING).getOpposite()),
                p_60278_,
                ExperimentalRedstoneUtils.withFront(p_363965_, p_60275_.getValue(FACING).getOpposite())
            );
        }
    }

    @Override
    protected ItemStack getCloneItemStack(LevelReader p_304638_, BlockPos p_60262_, BlockState p_60263_, boolean p_386559_) {
        return new ItemStack(p_60263_.getValue(TYPE) == PistonType.STICKY ? Blocks.STICKY_PISTON : Blocks.PISTON);
    }

    @Override
    protected BlockState rotate(BlockState state, Rotation rot) {
        return state.setValue(FACING, rot.rotate(state.getValue(FACING)));
    }

    @Override
    protected BlockState mirror(BlockState state, Mirror mirror) {
        return state.rotate(mirror.getRotation(state.getValue(FACING)));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, TYPE, SHORT);
    }

    @Override
    protected boolean isPathfindable(BlockState p_60270_, PathComputationType p_60273_) {
        return false;
    }
}
