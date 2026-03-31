package net.minecraft.world.level.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.redstone.Orientation;
import org.jspecify.annotations.Nullable;

public class CopperBulbBlock extends Block {
    public static final MapCodec<CopperBulbBlock> CODEC = simpleCodec(CopperBulbBlock::new);
    public static final BooleanProperty POWERED = BlockStateProperties.POWERED;
    public static final BooleanProperty LIT = BlockStateProperties.LIT;

    @Override
    protected MapCodec<? extends CopperBulbBlock> codec() {
        return CODEC;
    }

    public CopperBulbBlock(BlockBehaviour.Properties p_308970_) {
        super(p_308970_);
        this.registerDefaultState(this.defaultBlockState().setValue(LIT, false).setValue(POWERED, false));
    }

    @Override
    protected void onPlace(BlockState p_309207_, Level p_309068_, BlockPos p_309087_, BlockState p_308908_, boolean p_308906_) {
        if (p_308908_.getBlock() != p_309207_.getBlock() && p_309068_ instanceof ServerLevel serverlevel) {
            this.checkAndFlip(p_309207_, serverlevel, p_309087_);
        }
    }

    @Override
    protected void neighborChanged(
        BlockState p_309025_, Level p_308955_, BlockPos p_309153_, Block p_308949_, @Nullable Orientation p_364418_, boolean p_309085_
    ) {
        if (p_308955_ instanceof ServerLevel serverlevel) {
            this.checkAndFlip(p_309025_, serverlevel, p_309153_);
        }
    }

    public void checkAndFlip(BlockState state, ServerLevel level, BlockPos pos) {
        boolean flag = level.hasNeighborSignal(pos);
        if (flag != state.getValue(POWERED)) {
            BlockState blockstate = state;
            if (!state.getValue(POWERED)) {
                blockstate = state.cycle(LIT);
                level.playSound(
                    null, pos, blockstate.getValue(LIT) ? SoundEvents.COPPER_BULB_TURN_ON : SoundEvents.COPPER_BULB_TURN_OFF, SoundSource.BLOCKS
                );
            }

            level.setBlock(pos, blockstate.setValue(POWERED, flag), 3);
        }
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> p_308903_) {
        p_308903_.add(LIT, POWERED);
    }

    @Override
    protected boolean hasAnalogOutputSignal(BlockState p_308965_) {
        return true;
    }

    @Override
    protected int getAnalogOutputSignal(BlockState p_308938_, Level p_309149_, BlockPos p_309114_, Direction p_434085_) {
        return p_309149_.getBlockState(p_309114_).getValue(LIT) ? 15 : 0;
    }
}
