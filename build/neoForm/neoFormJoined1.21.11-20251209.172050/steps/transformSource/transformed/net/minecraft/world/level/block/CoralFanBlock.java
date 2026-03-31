package net.minecraft.world.level.block;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.mojang.serialization.codecs.RecordCodecBuilder.Instance;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluids;

public class CoralFanBlock extends BaseCoralFanBlock {
    public static final MapCodec<CoralFanBlock> CODEC = RecordCodecBuilder.mapCodec(
        p_432658_ -> p_432658_.group(CoralBlock.DEAD_CORAL_FIELD.forGetter(p_304769_ -> p_304769_.deadBlock), propertiesCodec())
            .apply(p_432658_, CoralFanBlock::new)
    );
    private final Block deadBlock;

    @Override
    public MapCodec<CoralFanBlock> codec() {
        return CODEC;
    }

    public CoralFanBlock(Block deadBlock, BlockBehaviour.Properties properties) {
        super(properties);
        this.deadBlock = deadBlock;
    }

    @Override
    protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean isMoving) {
        this.tryScheduleDieTick(state, level, level, level.random, pos);
    }

    @Override
    protected void tick(BlockState p_221025_, ServerLevel p_221026_, BlockPos p_221027_, RandomSource p_221028_) {
        if (!scanForWater(p_221025_, p_221026_, p_221027_)) {
            p_221026_.setBlock(p_221027_, this.deadBlock.defaultBlockState().setValue(WATERLOGGED, false), 2);
        }
    }

    @Override
    protected BlockState updateShape(
        BlockState p_52159_,
        LevelReader p_374555_,
        ScheduledTickAccess p_374197_,
        BlockPos p_52163_,
        Direction p_52160_,
        BlockPos p_52164_,
        BlockState p_52161_,
        RandomSource p_374083_
    ) {
        if (p_52160_ == Direction.DOWN && !p_52159_.canSurvive(p_374555_, p_52163_)) {
            return Blocks.AIR.defaultBlockState();
        } else {
            this.tryScheduleDieTick(p_52159_, p_374555_, p_374197_, p_374083_, p_52163_);
            if (p_52159_.getValue(WATERLOGGED)) {
                p_374197_.scheduleTick(p_52163_, Fluids.WATER, Fluids.WATER.getTickDelay(p_374555_));
            }

            return super.updateShape(p_52159_, p_374555_, p_374197_, p_52163_, p_52160_, p_52164_, p_52161_, p_374083_);
        }
    }
}
