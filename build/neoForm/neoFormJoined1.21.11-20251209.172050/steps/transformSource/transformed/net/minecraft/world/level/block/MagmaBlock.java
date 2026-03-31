package net.minecraft.world.level.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;

public class MagmaBlock extends Block {
    public static final MapCodec<MagmaBlock> CODEC = simpleCodec(MagmaBlock::new);
    private static final int BUBBLE_COLUMN_CHECK_DELAY = 20;

    @Override
    public MapCodec<MagmaBlock> codec() {
        return CODEC;
    }

    public MagmaBlock(BlockBehaviour.Properties p_54800_) {
        super(p_54800_);
    }

    @Override
    public void stepOn(Level p_153777_, BlockPos p_153778_, BlockState p_153779_, Entity p_153780_) {
        if (!p_153780_.isSteppingCarefully() && p_153780_ instanceof LivingEntity) {
            p_153780_.hurt(p_153777_.damageSources().hotFloor(), 1.0F);
        }

        super.stepOn(p_153777_, p_153778_, p_153779_, p_153780_);
    }

    @Override
    protected void tick(BlockState p_221415_, ServerLevel p_221416_, BlockPos p_221417_, RandomSource p_221418_) {
        BubbleColumnBlock.updateColumn(p_221416_, p_221417_.above(), p_221415_);
    }

    @Override
    protected BlockState updateShape(
        BlockState p_54811_,
        LevelReader p_374494_,
        ScheduledTickAccess p_374388_,
        BlockPos p_54815_,
        Direction p_54812_,
        BlockPos p_54816_,
        BlockState p_54813_,
        RandomSource p_374472_
    ) {
        if (p_54812_ == Direction.UP && p_54813_.is(Blocks.WATER)) {
            p_374388_.scheduleTick(p_54815_, this, 20);
        }

        return super.updateShape(p_54811_, p_374494_, p_374388_, p_54815_, p_54812_, p_54816_, p_54813_, p_374472_);
    }

    @Override
    protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean isMoving) {
        level.scheduleTick(pos, this, 20);
    }
}
