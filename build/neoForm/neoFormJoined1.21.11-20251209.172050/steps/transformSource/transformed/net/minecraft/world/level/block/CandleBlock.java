package net.minecraft.world.level.block;

import com.mojang.serialization.MapCodec;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import java.util.List;
import java.util.function.ToIntFunction;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.util.Util;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

public class CandleBlock extends AbstractCandleBlock implements SimpleWaterloggedBlock {
    public static final MapCodec<CandleBlock> CODEC = simpleCodec(CandleBlock::new);
    public static final int MIN_CANDLES = 1;
    public static final int MAX_CANDLES = 4;
    public static final IntegerProperty CANDLES = BlockStateProperties.CANDLES;
    public static final BooleanProperty LIT = AbstractCandleBlock.LIT;
    public static final BooleanProperty WATERLOGGED = BlockStateProperties.WATERLOGGED;
    public static final ToIntFunction<BlockState> LIGHT_EMISSION = p_152848_ -> p_152848_.getValue(LIT) ? 3 * p_152848_.getValue(CANDLES) : 0;
    private static final Int2ObjectMap<List<Vec3>> PARTICLE_OFFSETS = Util.make(
        new Int2ObjectOpenHashMap<>(4),
        p_393350_ -> {
            float f = 0.0625F;
            p_393350_.put(1, List.of(new Vec3(8.0, 8.0, 8.0).scale(0.0625)));
            p_393350_.put(2, List.of(new Vec3(6.0, 7.0, 8.0).scale(0.0625), new Vec3(10.0, 8.0, 7.0).scale(0.0625)));
            p_393350_.put(3, List.of(new Vec3(8.0, 5.0, 10.0).scale(0.0625), new Vec3(6.0, 7.0, 8.0).scale(0.0625), new Vec3(9.0, 8.0, 7.0).scale(0.0625)));
            p_393350_.put(
                4,
                List.of(
                    new Vec3(7.0, 5.0, 9.0).scale(0.0625),
                    new Vec3(10.0, 7.0, 9.0).scale(0.0625),
                    new Vec3(6.0, 7.0, 6.0).scale(0.0625),
                    new Vec3(9.0, 8.0, 6.0).scale(0.0625)
                )
            );
        }
    );
    private static final VoxelShape[] SHAPES = new VoxelShape[]{
        Block.column(2.0, 0.0, 6.0),
        Block.box(5.0, 0.0, 6.0, 11.0, 6.0, 9.0),
        Block.box(5.0, 0.0, 6.0, 10.0, 6.0, 11.0),
        Block.box(5.0, 0.0, 5.0, 11.0, 6.0, 10.0)
    };

    @Override
    public MapCodec<CandleBlock> codec() {
        return CODEC;
    }

    public CandleBlock(BlockBehaviour.Properties p_152801_) {
        super(p_152801_);
        this.registerDefaultState(this.stateDefinition.any().setValue(CANDLES, 1).setValue(LIT, false).setValue(WATERLOGGED, false));
    }

    @Override
    protected InteractionResult useItemOn(
        ItemStack p_316279_, BlockState p_316163_, Level p_316881_, BlockPos p_316480_, Player p_316191_, InteractionHand p_316669_, BlockHitResult p_316641_
    ) {
        if (p_316279_.isEmpty() && p_316191_.getAbilities().mayBuild && p_316163_.getValue(LIT)) {
            extinguish(p_316191_, p_316163_, p_316881_, p_316480_);
            return InteractionResult.SUCCESS;
        } else {
            return super.useItemOn(p_316279_, p_316163_, p_316881_, p_316480_, p_316191_, p_316669_, p_316641_);
        }
    }

    @Override
    protected boolean canBeReplaced(BlockState p_152814_, BlockPlaceContext p_152815_) {
        return !p_152815_.isSecondaryUseActive() && p_152815_.getItemInHand().getItem() == this.asItem() && p_152814_.getValue(CANDLES) < 4
            ? true
            : super.canBeReplaced(p_152814_, p_152815_);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext p_152803_) {
        BlockState blockstate = p_152803_.getLevel().getBlockState(p_152803_.getClickedPos());
        if (blockstate.is(this)) {
            return blockstate.cycle(CANDLES);
        } else {
            FluidState fluidstate = p_152803_.getLevel().getFluidState(p_152803_.getClickedPos());
            boolean flag = fluidstate.getType() == Fluids.WATER;
            return super.getStateForPlacement(p_152803_).setValue(WATERLOGGED, flag);
        }
    }

    @Override
    protected BlockState updateShape(
        BlockState p_152833_,
        LevelReader p_374497_,
        ScheduledTickAccess p_374340_,
        BlockPos p_152837_,
        Direction p_152834_,
        BlockPos p_152838_,
        BlockState p_152835_,
        RandomSource p_374318_
    ) {
        if (p_152833_.getValue(WATERLOGGED)) {
            p_374340_.scheduleTick(p_152837_, Fluids.WATER, Fluids.WATER.getTickDelay(p_374497_));
        }

        return super.updateShape(p_152833_, p_374497_, p_374340_, p_152837_, p_152834_, p_152838_, p_152835_, p_374318_);
    }

    @Override
    protected FluidState getFluidState(BlockState p_152844_) {
        return p_152844_.getValue(WATERLOGGED) ? Fluids.WATER.getSource(false) : super.getFluidState(p_152844_);
    }

    @Override
    protected VoxelShape getShape(BlockState p_152817_, BlockGetter p_152818_, BlockPos p_152819_, CollisionContext p_152820_) {
        return SHAPES[p_152817_.getValue(CANDLES) - 1];
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> p_152840_) {
        p_152840_.add(CANDLES, LIT, WATERLOGGED);
    }

    @Override
    public boolean placeLiquid(LevelAccessor p_152805_, BlockPos p_152806_, BlockState p_152807_, FluidState p_152808_) {
        if (!p_152807_.getValue(WATERLOGGED) && p_152808_.getType() == Fluids.WATER) {
            BlockState blockstate = p_152807_.setValue(WATERLOGGED, true);
            if (p_152807_.getValue(LIT)) {
                extinguish(null, blockstate, p_152805_, p_152806_);
            } else {
                p_152805_.setBlock(p_152806_, blockstate, 3);
            }

            p_152805_.scheduleTick(p_152806_, p_152808_.getType(), p_152808_.getType().getTickDelay(p_152805_));
            return true;
        } else {
            return false;
        }
    }

    public static boolean canLight(BlockState state) {
        return state.is(BlockTags.CANDLES, p_152810_ -> p_152810_.hasProperty(LIT) && p_152810_.hasProperty(WATERLOGGED))
            && !state.getValue(LIT)
            && !state.getValue(WATERLOGGED);
    }

    @Override
    protected Iterable<Vec3> getParticleOffsets(BlockState p_152812_) {
        return PARTICLE_OFFSETS.get(p_152812_.getValue(CANDLES).intValue());
    }

    @Override
    protected boolean canBeLit(BlockState p_152842_) {
        return !p_152842_.getValue(WATERLOGGED) && super.canBeLit(p_152842_);
    }

    @Override
    protected boolean canSurvive(BlockState p_152829_, LevelReader p_152830_, BlockPos p_152831_) {
        return Block.canSupportCenter(p_152830_, p_152831_.below(), Direction.UP);
    }
}
