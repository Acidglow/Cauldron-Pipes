package net.minecraft.world.level.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.BlockItemStateProperties;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.TestBlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.TestBlockMode;
import net.minecraft.world.level.redstone.Orientation;
import net.minecraft.world.phys.BlockHitResult;
import org.jspecify.annotations.Nullable;

public class TestBlock extends BaseEntityBlock implements GameMasterBlock {
    public static final MapCodec<TestBlock> CODEC = simpleCodec(TestBlock::new);
    public static final EnumProperty<TestBlockMode> MODE = BlockStateProperties.TEST_BLOCK_MODE;

    public TestBlock(BlockBehaviour.Properties p_397223_) {
        super(p_397223_);
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos p_397356_, BlockState p_397468_) {
        return new TestBlockEntity(p_397356_, p_397468_);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext p_397700_) {
        BlockItemStateProperties blockitemstateproperties = p_397700_.getItemInHand().get(DataComponents.BLOCK_STATE);
        BlockState blockstate = this.defaultBlockState();
        if (blockitemstateproperties != null) {
            TestBlockMode testblockmode = blockitemstateproperties.get(MODE);
            if (testblockmode != null) {
                blockstate = blockstate.setValue(MODE, testblockmode);
            }
        }

        return blockstate;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> p_397099_) {
        p_397099_.add(MODE);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState p_397020_, Level p_397230_, BlockPos p_397100_, Player p_397362_, BlockHitResult p_397202_) {
        if (p_397230_.getBlockEntity(p_397100_) instanceof TestBlockEntity testblockentity) {
            if (!p_397362_.canUseGameMasterBlocks()) {
                return InteractionResult.PASS;
            } else {
                if (p_397230_.isClientSide()) {
                    p_397362_.openTestBlock(testblockentity);
                }

                return InteractionResult.SUCCESS;
            }
        } else {
            return InteractionResult.PASS;
        }
    }

    @Override
    protected void tick(BlockState p_397712_, ServerLevel p_397112_, BlockPos p_397466_, RandomSource p_397531_) {
        TestBlockEntity testblockentity = getServerTestBlockEntity(p_397112_, p_397466_);
        if (testblockentity != null) {
            testblockentity.reset();
        }
    }

    @Override
    protected void neighborChanged(
        BlockState p_397457_, Level p_397572_, BlockPos p_397104_, Block p_397813_, @Nullable Orientation p_397759_, boolean p_397459_
    ) {
        TestBlockEntity testblockentity = getServerTestBlockEntity(p_397572_, p_397104_);
        if (testblockentity != null) {
            if (testblockentity.getMode() != TestBlockMode.START) {
                boolean flag = p_397572_.hasNeighborSignal(p_397104_);
                boolean flag1 = testblockentity.isPowered();
                if (flag && !flag1) {
                    testblockentity.setPowered(true);
                    testblockentity.trigger();
                } else if (!flag && flag1) {
                    testblockentity.setPowered(false);
                }
            }
        }
    }

    private static @Nullable TestBlockEntity getServerTestBlockEntity(Level level, BlockPos pos) {
        return level instanceof ServerLevel serverlevel && serverlevel.getBlockEntity(pos) instanceof TestBlockEntity testblockentity
            ? testblockentity
            : null;
    }

    @Override
    public int getSignal(BlockState p_397637_, BlockGetter p_397297_, BlockPos p_397948_, Direction p_397493_) {
        if (p_397637_.getValue(MODE) != TestBlockMode.START) {
            return 0;
        } else if (p_397297_.getBlockEntity(p_397948_) instanceof TestBlockEntity testblockentity) {
            return testblockentity.isPowered() ? 15 : 0;
        } else {
            return 0;
        }
    }

    @Override
    protected ItemStack getCloneItemStack(LevelReader p_397697_, BlockPos p_397696_, BlockState p_397513_, boolean p_398035_) {
        ItemStack itemstack = super.getCloneItemStack(p_397697_, p_397696_, p_397513_, p_398035_);
        return setModeOnStack(itemstack, p_397513_.getValue(MODE));
    }

    public static ItemStack setModeOnStack(ItemStack stack, TestBlockMode mode) {
        stack.set(DataComponents.BLOCK_STATE, stack.getOrDefault(DataComponents.BLOCK_STATE, BlockItemStateProperties.EMPTY).with(MODE, mode));
        return stack;
    }

    @Override
    protected MapCodec<TestBlock> codec() {
        return CODEC;
    }
}
