package net.minecraft.world.level.block.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

public class ComparatorBlockEntity extends BlockEntity {
    private static final int DEFAULT_OUTPUT = 0;
    private int output = 0;

    public ComparatorBlockEntity(BlockPos pos, BlockState blockState) {
        super(BlockEntityType.COMPARATOR, pos, blockState);
    }

    @Override
    protected void saveAdditional(ValueOutput p_421778_) {
        super.saveAdditional(p_421778_);
        p_421778_.putInt("OutputSignal", this.output);
    }

    @Override
    protected void loadAdditional(ValueInput p_422269_) {
        super.loadAdditional(p_422269_);
        this.output = p_422269_.getIntOr("OutputSignal", 0);
    }

    public int getOutputSignal() {
        return this.output;
    }

    public void setOutputSignal(int output) {
        this.output = output;
    }
}
