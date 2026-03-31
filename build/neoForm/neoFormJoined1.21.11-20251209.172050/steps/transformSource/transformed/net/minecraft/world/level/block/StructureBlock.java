package net.minecraft.world.level.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.StructureBlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.StructureMode;
import net.minecraft.world.level.redstone.Orientation;
import net.minecraft.world.phys.BlockHitResult;
import org.jspecify.annotations.Nullable;

public class StructureBlock extends BaseEntityBlock implements GameMasterBlock {
    public static final MapCodec<StructureBlock> CODEC = simpleCodec(StructureBlock::new);
    public static final EnumProperty<StructureMode> MODE = BlockStateProperties.STRUCTUREBLOCK_MODE;

    @Override
    public MapCodec<StructureBlock> codec() {
        return CODEC;
    }

    public StructureBlock(BlockBehaviour.Properties p_57113_) {
        super(p_57113_);
        this.registerDefaultState(this.stateDefinition.any().setValue(MODE, StructureMode.LOAD));
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos p_154732_, BlockState p_154733_) {
        return new StructureBlockEntity(p_154732_, p_154733_);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState p_57128_, Level p_57129_, BlockPos p_57130_, Player p_57131_, BlockHitResult p_57133_) {
        BlockEntity blockentity = p_57129_.getBlockEntity(p_57130_);
        if (blockentity instanceof StructureBlockEntity) {
            return (InteractionResult)(((StructureBlockEntity)blockentity).usedBy(p_57131_) ? InteractionResult.SUCCESS : InteractionResult.PASS);
        } else {
            return InteractionResult.PASS;
        }
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
        if (!level.isClientSide()) {
            if (placer != null) {
                BlockEntity blockentity = level.getBlockEntity(pos);
                if (blockentity instanceof StructureBlockEntity) {
                    ((StructureBlockEntity)blockentity).createdBy(placer);
                }
            }
        }
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(MODE);
    }

    @Override
    protected void neighborChanged(BlockState p_57135_, Level p_57136_, BlockPos p_57137_, Block p_57138_, @Nullable Orientation p_364789_, boolean p_57140_) {
        if (p_57136_ instanceof ServerLevel) {
            if (p_57136_.getBlockEntity(p_57137_) instanceof StructureBlockEntity structureblockentity) {
                boolean flag = p_57136_.hasNeighborSignal(p_57137_);
                boolean flag1 = structureblockentity.isPowered();
                if (flag && !flag1) {
                    structureblockentity.setPowered(true);
                    this.trigger((ServerLevel)p_57136_, structureblockentity);
                } else if (!flag && flag1) {
                    structureblockentity.setPowered(false);
                }
            }
        }
    }

    private void trigger(ServerLevel level, StructureBlockEntity blockEntity) {
        switch (blockEntity.getMode()) {
            case SAVE:
                blockEntity.saveStructure(false);
                break;
            case LOAD:
                blockEntity.placeStructure(level);
                break;
            case CORNER:
                blockEntity.unloadStructure();
            case DATA:
        }
    }
}
