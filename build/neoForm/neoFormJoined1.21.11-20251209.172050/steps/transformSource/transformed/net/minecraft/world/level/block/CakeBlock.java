package net.minecraft.world.level.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.tags.ItemTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

public class CakeBlock extends Block {
    public static final MapCodec<CakeBlock> CODEC = simpleCodec(CakeBlock::new);
    public static final int MAX_BITES = 6;
    public static final IntegerProperty BITES = BlockStateProperties.BITES;
    public static final int FULL_CAKE_SIGNAL = getOutputSignal(0);
    private static final VoxelShape[] SHAPES = Block.boxes(6, p_394546_ -> Block.box(1 + p_394546_ * 2, 0.0, 1.0, 15.0, 8.0, 15.0));

    @Override
    public MapCodec<CakeBlock> codec() {
        return CODEC;
    }

    public CakeBlock(BlockBehaviour.Properties p_51184_) {
        super(p_51184_);
        this.registerDefaultState(this.stateDefinition.any().setValue(BITES, 0));
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPES[state.getValue(BITES)];
    }

    @Override
    protected InteractionResult useItemOn(
        ItemStack p_316238_, BlockState p_316837_, Level p_316766_, BlockPos p_316227_, Player p_316853_, InteractionHand p_316422_, BlockHitResult p_316869_
    ) {
        Item item = p_316238_.getItem();
        if (p_316238_.is(ItemTags.CANDLES) && p_316837_.getValue(BITES) == 0 && Block.byItem(item) instanceof CandleBlock candleblock) {
            p_316238_.consume(1, p_316853_);
            p_316766_.playSound(null, p_316227_, SoundEvents.CAKE_ADD_CANDLE, SoundSource.BLOCKS, 1.0F, 1.0F);
            p_316766_.setBlockAndUpdate(p_316227_, CandleCakeBlock.byCandle(candleblock));
            p_316766_.gameEvent(p_316853_, GameEvent.BLOCK_CHANGE, p_316227_);
            p_316853_.awardStat(Stats.ITEM_USED.get(item));
            return InteractionResult.SUCCESS;
        } else {
            return InteractionResult.TRY_WITH_EMPTY_HAND;
        }
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState p_316481_, Level p_316406_, BlockPos p_316218_, Player p_316212_, BlockHitResult p_316525_) {
        if (p_316406_.isClientSide()) {
            if (eat(p_316406_, p_316218_, p_316481_, p_316212_).consumesAction()) {
                return InteractionResult.SUCCESS;
            }

            if (p_316212_.getItemInHand(InteractionHand.MAIN_HAND).isEmpty()) {
                return InteractionResult.CONSUME;
            }
        }

        return eat(p_316406_, p_316218_, p_316481_, p_316212_);
    }

    protected static InteractionResult eat(LevelAccessor level, BlockPos pos, BlockState state, Player player) {
        if (!player.canEat(false)) {
            return InteractionResult.PASS;
        } else {
            player.awardStat(Stats.EAT_CAKE_SLICE);
            player.getFoodData().eat(2, 0.1F);
            int i = state.getValue(BITES);
            level.gameEvent(player, GameEvent.EAT, pos);
            if (i < 6) {
                level.setBlock(pos, state.setValue(BITES, i + 1), 3);
            } else {
                level.removeBlock(pos, false);
                level.gameEvent(player, GameEvent.BLOCK_DESTROY, pos);
            }

            return InteractionResult.SUCCESS;
        }
    }

    @Override
    protected BlockState updateShape(
        BlockState p_51213_,
        LevelReader p_374144_,
        ScheduledTickAccess p_374554_,
        BlockPos p_51217_,
        Direction p_51214_,
        BlockPos p_51218_,
        BlockState p_51215_,
        RandomSource p_374177_
    ) {
        return p_51214_ == Direction.DOWN && !p_51213_.canSurvive(p_374144_, p_51217_)
            ? Blocks.AIR.defaultBlockState()
            : super.updateShape(p_51213_, p_374144_, p_374554_, p_51217_, p_51214_, p_51218_, p_51215_, p_374177_);
    }

    @Override
    protected boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        return level.getBlockState(pos.below()).isSolid();
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(BITES);
    }

    @Override
    protected int getAnalogOutputSignal(BlockState p_51198_, Level p_51199_, BlockPos p_51200_, Direction p_432904_) {
        return getOutputSignal(p_51198_.getValue(BITES));
    }

    public static int getOutputSignal(int eaten) {
        return (7 - eaten) * 2;
    }

    @Override
    protected boolean hasAnalogOutputSignal(BlockState state) {
        return true;
    }

    @Override
    protected boolean isPathfindable(BlockState p_51193_, PathComputationType p_51196_) {
        return false;
    }
}
