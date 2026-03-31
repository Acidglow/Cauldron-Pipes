package net.minecraft.world.level.block;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.mojang.serialization.codecs.RecordCodecBuilder.Instance;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import org.jspecify.annotations.Nullable;

public class CoralBlock extends Block {
    public static final MapCodec<Block> DEAD_CORAL_FIELD = BuiltInRegistries.BLOCK.byNameCodec().fieldOf("dead");
    public static final MapCodec<CoralBlock> CODEC = RecordCodecBuilder.mapCodec(
        p_432657_ -> p_432657_.group(DEAD_CORAL_FIELD.forGetter(p_304453_ -> p_304453_.deadBlock), propertiesCodec()).apply(p_432657_, CoralBlock::new)
    );
    private final Block deadBlock;

    public CoralBlock(Block deadBlock, BlockBehaviour.Properties properties) {
        super(properties);
        this.deadBlock = deadBlock;
    }

    @Override
    public MapCodec<CoralBlock> codec() {
        return CODEC;
    }

    @Override
    protected void tick(BlockState p_221020_, ServerLevel p_221021_, BlockPos p_221022_, RandomSource p_221023_) {
        if (!this.scanForWater(p_221021_, p_221022_)) {
            p_221021_.setBlock(p_221022_, this.deadBlock.defaultBlockState(), 2);
        }
    }

    @Override
    protected BlockState updateShape(
        BlockState p_52143_,
        LevelReader p_374246_,
        ScheduledTickAccess p_374434_,
        BlockPos p_52147_,
        Direction p_52144_,
        BlockPos p_52148_,
        BlockState p_52145_,
        RandomSource p_374050_
    ) {
        if (!this.scanForWater(p_374246_, p_52147_)) {
            p_374434_.scheduleTick(p_52147_, this, 60 + p_374050_.nextInt(40));
        }

        return super.updateShape(p_52143_, p_374246_, p_374434_, p_52147_, p_52144_, p_52148_, p_52145_, p_374050_);
    }

    protected boolean scanForWater(BlockGetter level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        for (Direction direction : Direction.values()) {
            FluidState fluidstate = level.getFluidState(pos.relative(direction));
            if (state.canBeHydrated(level, pos, fluidstate, pos.relative(direction))) {
                return true;
            }
        }

        return false;
    }

    @Override
    public @Nullable BlockState getStateForPlacement(BlockPlaceContext context) {
        if (!this.scanForWater(context.getLevel(), context.getClickedPos())) {
            context.getLevel().scheduleTick(context.getClickedPos(), this, 60 + context.getLevel().getRandom().nextInt(40));
        }

        return this.defaultBlockState();
    }
}
