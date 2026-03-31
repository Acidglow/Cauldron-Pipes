package net.minecraft.world.level.block;

import com.mojang.serialization.MapCodec;
import java.util.List;
import java.util.Map;
import java.util.OptionalInt;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.ShelfBlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.SideChainPart;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraft.world.level.redstone.Orientation;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jspecify.annotations.Nullable;

public class ShelfBlock extends BaseEntityBlock implements SelectableSlotContainer, SideChainPartBlock, SimpleWaterloggedBlock {
    public static final MapCodec<ShelfBlock> CODEC = simpleCodec(ShelfBlock::new);
    public static final BooleanProperty POWERED = BlockStateProperties.POWERED;
    public static final EnumProperty<Direction> FACING = BlockStateProperties.HORIZONTAL_FACING;
    public static final EnumProperty<SideChainPart> SIDE_CHAIN_PART = BlockStateProperties.SIDE_CHAIN_PART;
    public static final BooleanProperty WATERLOGGED = BlockStateProperties.WATERLOGGED;
    private static final Map<Direction, VoxelShape> SHAPES = Shapes.rotateHorizontal(
        Shapes.or(Block.box(0.0, 12.0, 11.0, 16.0, 16.0, 13.0), Block.box(0.0, 0.0, 13.0, 16.0, 16.0, 16.0), Block.box(0.0, 0.0, 11.0, 16.0, 4.0, 13.0))
    );

    @Override
    public MapCodec<ShelfBlock> codec() {
        return CODEC;
    }

    public ShelfBlock(BlockBehaviour.Properties p_434315_) {
        super(p_434315_);
        this.registerDefaultState(
            this.stateDefinition
                .any()
                .setValue(FACING, Direction.NORTH)
                .setValue(POWERED, false)
                .setValue(SIDE_CHAIN_PART, SideChainPart.UNCONNECTED)
                .setValue(WATERLOGGED, false)
        );
    }

    @Override
    protected VoxelShape getShape(BlockState p_433292_, BlockGetter p_433892_, BlockPos p_435749_, CollisionContext p_435101_) {
        return SHAPES.get(p_433292_.getValue(FACING));
    }

    @Override
    protected boolean useShapeForLightOcclusion(BlockState p_434279_) {
        return true;
    }

    @Override
    protected boolean isPathfindable(BlockState p_433007_, PathComputationType p_433825_) {
        return p_433825_ == PathComputationType.WATER && p_433007_.getFluidState().is(FluidTags.WATER);
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos p_433693_, BlockState p_434499_) {
        return new ShelfBlockEntity(p_433693_, p_434499_);
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> p_433341_) {
        p_433341_.add(FACING, POWERED, SIDE_CHAIN_PART, WATERLOGGED);
    }

    @Override
    protected void affectNeighborsAfterRemoval(BlockState p_432992_, ServerLevel p_433135_, BlockPos p_434392_, boolean p_434559_) {
        Containers.updateNeighboursAfterDestroy(p_432992_, p_433135_, p_434392_);
        this.updateNeighborsAfterPoweringDown(p_433135_, p_434392_, p_432992_);
    }

    @Override
    protected void neighborChanged(
        BlockState p_433593_, Level p_433212_, BlockPos p_433073_, Block p_433504_, @Nullable Orientation p_433036_, boolean p_435480_
    ) {
        if (!p_433212_.isClientSide()) {
            boolean flag = p_433212_.hasNeighborSignal(p_433073_);
            if (p_433593_.getValue(POWERED) != flag) {
                BlockState blockstate = p_433593_.setValue(POWERED, flag);
                if (!flag) {
                    blockstate = blockstate.setValue(SIDE_CHAIN_PART, SideChainPart.UNCONNECTED);
                }

                p_433212_.setBlock(p_433073_, blockstate, 3);
                this.playSound(p_433212_, p_433073_, flag ? SoundEvents.SHELF_ACTIVATE : SoundEvents.SHELF_DEACTIVATE);
                p_433212_.gameEvent(flag ? GameEvent.BLOCK_ACTIVATE : GameEvent.BLOCK_DEACTIVATE, p_433073_, GameEvent.Context.of(blockstate));
            }
        }
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext p_434959_) {
        FluidState fluidstate = p_434959_.getLevel().getFluidState(p_434959_.getClickedPos());
        return this.defaultBlockState()
            .setValue(FACING, p_434959_.getHorizontalDirection().getOpposite())
            .setValue(POWERED, p_434959_.getLevel().hasNeighborSignal(p_434959_.getClickedPos()))
            .setValue(WATERLOGGED, fluidstate.getType() == Fluids.WATER);
    }

    @Override
    public BlockState rotate(BlockState p_433098_, Rotation p_435249_) {
        return p_433098_.setValue(FACING, p_435249_.rotate(p_433098_.getValue(FACING)));
    }

    @Override
    public BlockState mirror(BlockState p_433683_, Mirror p_433525_) {
        return p_433683_.rotate(p_433525_.getRotation(p_433683_.getValue(FACING)));
    }

    @Override
    public int getRows() {
        return 1;
    }

    @Override
    public int getColumns() {
        return 3;
    }

    @Override
    protected InteractionResult useItemOn(
        ItemStack p_433583_, BlockState p_433454_, Level p_435671_, BlockPos p_433939_, Player p_433327_, InteractionHand p_432907_, BlockHitResult p_433590_
    ) {
        if (p_435671_.getBlockEntity(p_433939_) instanceof ShelfBlockEntity shelfblockentity && !p_432907_.equals(InteractionHand.OFF_HAND)) {
            OptionalInt optionalint = this.getHitSlot(p_433590_, p_433454_.getValue(FACING));
            if (optionalint.isEmpty()) {
                return InteractionResult.PASS;
            } else {
                Inventory inventory = p_433327_.getInventory();
                if (p_435671_.isClientSide()) {
                    return (InteractionResult)(inventory.getSelectedItem().isEmpty() ? InteractionResult.PASS : InteractionResult.SUCCESS);
                } else if (!p_433454_.getValue(POWERED)) {
                    boolean flag1 = swapSingleItem(p_433583_, p_433327_, shelfblockentity, optionalint.getAsInt(), inventory);
                    if (flag1) {
                        this.playSound(p_435671_, p_433939_, p_433583_.isEmpty() ? SoundEvents.SHELF_TAKE_ITEM : SoundEvents.SHELF_SINGLE_SWAP);
                    } else {
                        if (p_433583_.isEmpty()) {
                            return InteractionResult.PASS;
                        }

                        this.playSound(p_435671_, p_433939_, SoundEvents.SHELF_PLACE_ITEM);
                    }

                    return InteractionResult.SUCCESS.heldItemTransformedTo(p_433583_);
                } else {
                    ItemStack itemstack = inventory.getSelectedItem();
                    boolean flag = this.swapHotbar(p_435671_, p_433939_, inventory);
                    if (!flag) {
                        return InteractionResult.CONSUME;
                    } else {
                        this.playSound(p_435671_, p_433939_, SoundEvents.SHELF_MULTI_SWAP);
                        return itemstack == inventory.getSelectedItem()
                            ? InteractionResult.SUCCESS
                            : InteractionResult.SUCCESS.heldItemTransformedTo(inventory.getSelectedItem());
                    }
                }
            }
        } else {
            return InteractionResult.PASS;
        }
    }

    private static boolean swapSingleItem(ItemStack stack, Player player, ShelfBlockEntity shelf, int index, Inventory inventory) {
        ItemStack itemstack = shelf.swapItemNoUpdate(index, stack);
        ItemStack itemstack1 = player.hasInfiniteMaterials() && itemstack.isEmpty() ? stack.copy() : itemstack;
        inventory.setItem(inventory.getSelectedSlot(), itemstack1);
        inventory.setChanged();
        shelf.setChanged(
            itemstack1.has(DataComponents.USE_EFFECTS) && !itemstack1.get(DataComponents.USE_EFFECTS).interactVibrations()
                ? null
                : GameEvent.ITEM_INTERACT_FINISH
        );
        return !itemstack.isEmpty();
    }

    private boolean swapHotbar(Level level, BlockPos pos, Inventory inventory) {
        List<BlockPos> list = this.getAllBlocksConnectedTo(level, pos);
        if (list.isEmpty()) {
            return false;
        } else {
            boolean flag = false;

            for (int i = 0; i < list.size(); i++) {
                ShelfBlockEntity shelfblockentity = (ShelfBlockEntity)level.getBlockEntity(list.get(i));
                if (shelfblockentity != null) {
                    for (int j = 0; j < shelfblockentity.getContainerSize(); j++) {
                        int k = 9 - (list.size() - i) * shelfblockentity.getContainerSize() + j;
                        if (k >= 0 && k <= inventory.getContainerSize()) {
                            ItemStack itemstack = inventory.removeItemNoUpdate(k);
                            ItemStack itemstack1 = shelfblockentity.swapItemNoUpdate(j, itemstack);
                            if (!itemstack.isEmpty() || !itemstack1.isEmpty()) {
                                inventory.setItem(k, itemstack1);
                                flag = true;
                            }
                        }
                    }

                    inventory.setChanged();
                    shelfblockentity.setChanged(GameEvent.ENTITY_INTERACT);
                }
            }

            return flag;
        }
    }

    @Override
    public SideChainPart getSideChainPart(BlockState p_432814_) {
        return p_432814_.getValue(SIDE_CHAIN_PART);
    }

    @Override
    public BlockState setSideChainPart(BlockState p_433421_, SideChainPart p_433762_) {
        return p_433421_.setValue(SIDE_CHAIN_PART, p_433762_);
    }

    @Override
    public Direction getFacing(BlockState p_433922_) {
        return p_433922_.getValue(FACING);
    }

    @Override
    public boolean isConnectable(BlockState p_434576_) {
        return p_434576_.is(BlockTags.WOODEN_SHELVES) && p_434576_.hasProperty(POWERED) && p_434576_.getValue(POWERED);
    }

    @Override
    public int getMaxChainLength() {
        return 3;
    }

    @Override
    protected void onPlace(BlockState p_434112_, Level p_435852_, BlockPos p_434206_, BlockState p_434335_, boolean p_434829_) {
        if (p_434112_.getValue(POWERED)) {
            this.updateSelfAndNeighborsOnPoweringUp(p_435852_, p_434206_, p_434112_, p_434335_);
        } else {
            this.updateNeighborsAfterPoweringDown(p_435852_, p_434206_, p_434112_);
        }
    }

    private void playSound(LevelAccessor level, BlockPos pos, SoundEvent sound) {
        level.playSound(null, pos, sound, SoundSource.BLOCKS, 1.0F, 1.0F);
    }

    @Override
    protected FluidState getFluidState(BlockState p_436034_) {
        return p_436034_.getValue(WATERLOGGED) ? Fluids.WATER.getSource(false) : super.getFluidState(p_436034_);
    }

    @Override
    protected BlockState updateShape(
        BlockState p_432911_,
        LevelReader p_436058_,
        ScheduledTickAccess p_433087_,
        BlockPos p_436032_,
        Direction p_433273_,
        BlockPos p_435870_,
        BlockState p_435564_,
        RandomSource p_435142_
    ) {
        if (p_432911_.getValue(WATERLOGGED)) {
            p_433087_.scheduleTick(p_436032_, Fluids.WATER, Fluids.WATER.getTickDelay(p_436058_));
        }

        return super.updateShape(p_432911_, p_436058_, p_433087_, p_436032_, p_433273_, p_435870_, p_435564_, p_435142_);
    }

    @Override
    protected boolean hasAnalogOutputSignal(BlockState p_435926_) {
        return true;
    }

    @Override
    protected int getAnalogOutputSignal(BlockState p_435034_, Level p_433944_, BlockPos p_433350_, Direction p_433486_) {
        if (p_433944_.isClientSide()) {
            return 0;
        } else if (p_433486_ != p_435034_.getValue(FACING).getOpposite()) {
            return 0;
        } else if (p_433944_.getBlockEntity(p_433350_) instanceof ShelfBlockEntity shelfblockentity) {
            int k = shelfblockentity.getItem(0).isEmpty() ? 0 : 1;
            int i = shelfblockentity.getItem(1).isEmpty() ? 0 : 1;
            int j = shelfblockentity.getItem(2).isEmpty() ? 0 : 1;
            return k | i << 1 | j << 2;
        } else {
            return 0;
        }
    }
}
