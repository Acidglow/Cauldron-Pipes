package net.minecraft.world.level.block;

import com.google.common.collect.Maps;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.mojang.serialization.codecs.RecordCodecBuilder.Instance;
import java.util.List;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

public class CandleCakeBlock extends AbstractCandleBlock {
    public static final MapCodec<CandleCakeBlock> CODEC = RecordCodecBuilder.mapCodec(
        p_432640_ -> p_432640_.group(BuiltInRegistries.BLOCK.byNameCodec().fieldOf("candle").forGetter(p_316072_ -> p_316072_.candleBlock), propertiesCodec())
            .apply(p_432640_, CandleCakeBlock::new)
    );
    public static final BooleanProperty LIT = AbstractCandleBlock.LIT;
    private static final VoxelShape SHAPE = Shapes.or(Block.column(2.0, 8.0, 14.0), Block.column(14.0, 0.0, 8.0));
    private static final Map<CandleBlock, CandleCakeBlock> BY_CANDLE = Maps.newHashMap();
    private static final Iterable<Vec3> PARTICLE_OFFSETS = List.of(new Vec3(8.0, 16.0, 8.0).scale(0.0625));
    private final CandleBlock candleBlock;

    @Override
    public MapCodec<CandleCakeBlock> codec() {
        return CODEC;
    }

    public CandleCakeBlock(Block candleBlock, BlockBehaviour.Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any().setValue(LIT, false));
        if (candleBlock instanceof CandleBlock candleblock) {
            BY_CANDLE.put(candleblock, this);
            this.candleBlock = candleblock;
        } else {
            throw new IllegalArgumentException("Expected block to be of " + CandleBlock.class + " was " + candleBlock.getClass());
        }
    }

    @Override
    protected Iterable<Vec3> getParticleOffsets(BlockState p_152868_) {
        return PARTICLE_OFFSETS;
    }

    @Override
    protected VoxelShape getShape(BlockState p_152875_, BlockGetter p_152876_, BlockPos p_152877_, CollisionContext p_152878_) {
        return SHAPE;
    }

    @Override
    protected InteractionResult useItemOn(
        ItemStack p_316571_, BlockState p_316514_, Level p_316171_, BlockPos p_316112_, Player p_316172_, InteractionHand p_316257_, BlockHitResult p_316286_
    ) {
        if (p_316571_.is(Items.FLINT_AND_STEEL) || p_316571_.is(Items.FIRE_CHARGE)) {
            return InteractionResult.PASS;
        } else if (candleHit(p_316286_) && p_316571_.isEmpty() && p_316514_.getValue(LIT)) {
            extinguish(p_316172_, p_316514_, p_316171_, p_316112_);
            return InteractionResult.SUCCESS;
        } else {
            return super.useItemOn(p_316571_, p_316514_, p_316171_, p_316112_, p_316172_, p_316257_, p_316286_);
        }
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState p_316519_, Level p_316226_, BlockPos p_316122_, Player p_316438_, BlockHitResult p_316849_) {
        InteractionResult interactionresult = CakeBlock.eat(p_316226_, p_316122_, Blocks.CAKE.defaultBlockState(), p_316438_);
        if (interactionresult.consumesAction()) {
            dropResources(p_316519_, p_316226_, p_316122_);
        }

        return interactionresult;
    }

    private static boolean candleHit(BlockHitResult hit) {
        return hit.getLocation().y - hit.getBlockPos().getY() > 0.5;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> p_152905_) {
        p_152905_.add(LIT);
    }

    @Override
    protected ItemStack getCloneItemStack(LevelReader p_304662_, BlockPos p_152863_, BlockState p_152864_, boolean p_387122_) {
        return new ItemStack(Blocks.CAKE);
    }

    @Override
    protected BlockState updateShape(
        BlockState p_152898_,
        LevelReader p_374136_,
        ScheduledTickAccess p_374358_,
        BlockPos p_152902_,
        Direction p_152899_,
        BlockPos p_152903_,
        BlockState p_152900_,
        RandomSource p_374518_
    ) {
        return p_152899_ == Direction.DOWN && !p_152898_.canSurvive(p_374136_, p_152902_)
            ? Blocks.AIR.defaultBlockState()
            : super.updateShape(p_152898_, p_374136_, p_374358_, p_152902_, p_152899_, p_152903_, p_152900_, p_374518_);
    }

    @Override
    protected boolean canSurvive(BlockState p_152891_, LevelReader p_152892_, BlockPos p_152893_) {
        return p_152892_.getBlockState(p_152893_.below()).isSolid();
    }

    @Override
    protected int getAnalogOutputSignal(BlockState p_152880_, Level p_152881_, BlockPos p_152882_, Direction p_435090_) {
        return CakeBlock.FULL_CAKE_SIGNAL;
    }

    @Override
    protected boolean hasAnalogOutputSignal(BlockState p_152909_) {
        return true;
    }

    @Override
    protected boolean isPathfindable(BlockState p_152870_, PathComputationType p_152873_) {
        return false;
    }

    public static BlockState byCandle(CandleBlock candle) {
        return BY_CANDLE.get(candle).defaultBlockState();
    }

    public static boolean canLight(BlockState state) {
        return state.is(BlockTags.CANDLE_CAKES, p_152896_ -> p_152896_.hasProperty(LIT) && !state.getValue(LIT));
    }
}
