package net.minecraft.world.level.block;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.mojang.serialization.codecs.RecordCodecBuilder.Instance;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.cauldron.CauldronInteraction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Util;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.InsideBlockEffectApplier;
import net.minecraft.world.entity.InsideBlockEffectType;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

public class LayeredCauldronBlock extends AbstractCauldronBlock {
    public static final MapCodec<LayeredCauldronBlock> CODEC = RecordCodecBuilder.mapCodec(
        p_432670_ -> p_432670_.group(
                Biome.Precipitation.CODEC.fieldOf("precipitation").forGetter(p_304371_ -> p_304371_.precipitationType),
                CauldronInteraction.CODEC.fieldOf("interactions").forGetter(p_304369_ -> p_304369_.interactions),
                propertiesCodec()
            )
            .apply(p_432670_, LayeredCauldronBlock::new)
    );
    public static final int MIN_FILL_LEVEL = 1;
    public static final int MAX_FILL_LEVEL = 3;
    public static final IntegerProperty LEVEL = BlockStateProperties.LEVEL_CAULDRON;
    private static final int BASE_CONTENT_HEIGHT = 6;
    private static final double HEIGHT_PER_LEVEL = 3.0;
    private static final VoxelShape[] FILLED_SHAPES = Util.make(
        () -> Block.boxes(2, p_423403_ -> Shapes.or(AbstractCauldronBlock.SHAPE, Block.column(12.0, 4.0, getPixelContentHeight(p_423403_ + 1))))
    );
    private final Biome.Precipitation precipitationType;

    @Override
    public MapCodec<LayeredCauldronBlock> codec() {
        return CODEC;
    }

    public LayeredCauldronBlock(Biome.Precipitation precipitationType, CauldronInteraction.InteractionMap interactions, BlockBehaviour.Properties properties) {
        super(properties, interactions);
        this.precipitationType = precipitationType;
        this.registerDefaultState(this.stateDefinition.any().setValue(LEVEL, 1));
    }

    @Override
    public boolean isFull(BlockState p_153555_) {
        return p_153555_.getValue(LEVEL) == 3;
    }

    @Override
    protected boolean canReceiveStalactiteDrip(Fluid p_153551_) {
        return p_153551_ == Fluids.WATER && this.precipitationType == Biome.Precipitation.RAIN;
    }

    @Override
    protected double getContentHeight(BlockState p_153528_) {
        return getPixelContentHeight(p_153528_.getValue(LEVEL)) / 16.0;
    }

    private static double getPixelContentHeight(int level) {
        return 6.0 + level * 3.0;
    }

    @Override
    protected VoxelShape getEntityInsideCollisionShape(BlockState p_423555_, BlockGetter p_423560_, BlockPos p_423649_, Entity p_423623_) {
        return FILLED_SHAPES[p_423555_.getValue(LEVEL) - 1];
    }

    @Override
    protected void entityInside(
        BlockState p_153534_, Level p_153535_, BlockPos p_153536_, Entity p_153537_, InsideBlockEffectApplier p_405060_, boolean p_451753_
    ) {
        if (p_153535_ instanceof ServerLevel serverlevel) {
            BlockPos blockpos = p_153536_.immutable();
            p_405060_.runBefore(InsideBlockEffectType.EXTINGUISH, p_423402_ -> {
                if (p_423402_.isOnFire() && p_423402_.mayInteract(serverlevel, blockpos)) {
                    this.handleEntityOnFireInside(p_153534_, p_153535_, blockpos);
                }
            });
        }

        p_405060_.apply(InsideBlockEffectType.EXTINGUISH);
    }

    private void handleEntityOnFireInside(BlockState state, Level level, BlockPos pos) {
        if (this.precipitationType == Biome.Precipitation.SNOW) {
            lowerFillLevel(Blocks.WATER_CAULDRON.defaultBlockState().setValue(LEVEL, state.getValue(LEVEL)), level, pos);
        } else {
            lowerFillLevel(state, level, pos);
        }
    }

    public static void lowerFillLevel(BlockState state, Level level, BlockPos pos) {
        int i = state.getValue(LEVEL) - 1;
        BlockState blockstate = i == 0 ? Blocks.CAULDRON.defaultBlockState() : state.setValue(LEVEL, i);
        level.setBlockAndUpdate(pos, blockstate);
        level.gameEvent(GameEvent.BLOCK_CHANGE, pos, GameEvent.Context.of(blockstate));
    }

    @Override
    public void handlePrecipitation(BlockState p_153539_, Level p_153540_, BlockPos p_153541_, Biome.Precipitation p_153542_) {
        if (CauldronBlock.shouldHandlePrecipitation(p_153540_, p_153542_) && p_153539_.getValue(LEVEL) != 3 && p_153542_ == this.precipitationType) {
            BlockState blockstate = p_153539_.cycle(LEVEL);
            p_153540_.setBlockAndUpdate(p_153541_, blockstate);
            p_153540_.gameEvent(GameEvent.BLOCK_CHANGE, p_153541_, GameEvent.Context.of(blockstate));
        }
    }

    @Override
    protected int getAnalogOutputSignal(BlockState p_153530_, Level p_153531_, BlockPos p_153532_, Direction p_432844_) {
        return p_153530_.getValue(LEVEL);
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> p_153549_) {
        p_153549_.add(LEVEL);
    }

    @Override
    protected void receiveStalactiteDrip(BlockState p_153544_, Level p_153545_, BlockPos p_153546_, Fluid p_153547_) {
        if (!this.isFull(p_153544_)) {
            BlockState blockstate = p_153544_.setValue(LEVEL, p_153544_.getValue(LEVEL) + 1);
            p_153545_.setBlockAndUpdate(p_153546_, blockstate);
            p_153545_.gameEvent(GameEvent.BLOCK_CHANGE, p_153546_, GameEvent.Context.of(blockstate));
            p_153545_.levelEvent(1047, p_153546_, 0);
        }
    }
}
