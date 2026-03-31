package net.minecraft.world.level.block;

import com.mojang.serialization.MapCodec;
import java.util.List;
import java.util.OptionalInt;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.ChiseledBookShelfBlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.BlockHitResult;
import org.jspecify.annotations.Nullable;

public class ChiseledBookShelfBlock extends BaseEntityBlock implements SelectableSlotContainer {
    public static final MapCodec<ChiseledBookShelfBlock> CODEC = simpleCodec(ChiseledBookShelfBlock::new);
    public static final EnumProperty<Direction> FACING = HorizontalDirectionalBlock.FACING;
    public static final BooleanProperty SLOT_0_OCCUPIED = BlockStateProperties.SLOT_0_OCCUPIED;
    public static final BooleanProperty SLOT_1_OCCUPIED = BlockStateProperties.SLOT_1_OCCUPIED;
    public static final BooleanProperty SLOT_2_OCCUPIED = BlockStateProperties.SLOT_2_OCCUPIED;
    public static final BooleanProperty SLOT_3_OCCUPIED = BlockStateProperties.SLOT_3_OCCUPIED;
    public static final BooleanProperty SLOT_4_OCCUPIED = BlockStateProperties.SLOT_4_OCCUPIED;
    public static final BooleanProperty SLOT_5_OCCUPIED = BlockStateProperties.SLOT_5_OCCUPIED;
    private static final int MAX_BOOKS_IN_STORAGE = 6;
    private static final int BOOKS_PER_ROW = 3;
    public static final List<BooleanProperty> SLOT_OCCUPIED_PROPERTIES = List.of(
        SLOT_0_OCCUPIED, SLOT_1_OCCUPIED, SLOT_2_OCCUPIED, SLOT_3_OCCUPIED, SLOT_4_OCCUPIED, SLOT_5_OCCUPIED
    );

    @Override
    public MapCodec<ChiseledBookShelfBlock> codec() {
        return CODEC;
    }

    @Override
    public int getRows() {
        return 2;
    }

    @Override
    public int getColumns() {
        return 3;
    }

    public ChiseledBookShelfBlock(BlockBehaviour.Properties p_249989_) {
        super(p_249989_);
        BlockState blockstate = this.stateDefinition.any().setValue(FACING, Direction.NORTH);

        for (BooleanProperty booleanproperty : SLOT_OCCUPIED_PROPERTIES) {
            blockstate = blockstate.setValue(booleanproperty, false);
        }

        this.registerDefaultState(blockstate);
    }

    @Override
    protected InteractionResult useItemOn(
        ItemStack p_316457_, BlockState p_316201_, Level p_316747_, BlockPos p_316462_, Player p_316228_, InteractionHand p_316721_, BlockHitResult p_316464_
    ) {
        if (p_316747_.getBlockEntity(p_316462_) instanceof ChiseledBookShelfBlockEntity chiseledbookshelfblockentity) {
            if (!p_316457_.is(ItemTags.BOOKSHELF_BOOKS)) {
                return InteractionResult.TRY_WITH_EMPTY_HAND;
            } else {
                OptionalInt optionalint = this.getHitSlot(p_316464_, p_316201_.getValue(FACING));
                if (optionalint.isEmpty()) {
                    return InteractionResult.PASS;
                } else if (p_316201_.getValue(SLOT_OCCUPIED_PROPERTIES.get(optionalint.getAsInt()))) {
                    return InteractionResult.TRY_WITH_EMPTY_HAND;
                } else {
                    addBook(p_316747_, p_316462_, p_316228_, chiseledbookshelfblockentity, p_316457_, optionalint.getAsInt());
                    return InteractionResult.SUCCESS;
                }
            }
        } else {
            return InteractionResult.PASS;
        }
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState p_316403_, Level p_316842_, BlockPos p_316539_, Player p_316349_, BlockHitResult p_316278_) {
        if (p_316842_.getBlockEntity(p_316539_) instanceof ChiseledBookShelfBlockEntity chiseledbookshelfblockentity) {
            OptionalInt optionalint = this.getHitSlot(p_316278_, p_316403_.getValue(FACING));
            if (optionalint.isEmpty()) {
                return InteractionResult.PASS;
            } else if (!p_316403_.getValue(SLOT_OCCUPIED_PROPERTIES.get(optionalint.getAsInt()))) {
                return InteractionResult.CONSUME;
            } else {
                removeBook(p_316842_, p_316539_, p_316349_, chiseledbookshelfblockentity, optionalint.getAsInt());
                return InteractionResult.SUCCESS;
            }
        } else {
            return InteractionResult.PASS;
        }
    }

    private static void addBook(
        Level level, BlockPos pos, Player player, ChiseledBookShelfBlockEntity blockEntity, ItemStack bookStack, int slot
    ) {
        if (!level.isClientSide()) {
            player.awardStat(Stats.ITEM_USED.get(bookStack.getItem()));
            SoundEvent soundevent = bookStack.is(Items.ENCHANTED_BOOK)
                ? SoundEvents.CHISELED_BOOKSHELF_INSERT_ENCHANTED
                : SoundEvents.CHISELED_BOOKSHELF_INSERT;
            blockEntity.setItem(slot, bookStack.consumeAndReturn(1, player));
            level.playSound(null, pos, soundevent, SoundSource.BLOCKS, 1.0F, 1.0F);
        }
    }

    private static void removeBook(Level level, BlockPos pos, Player player, ChiseledBookShelfBlockEntity blockEntity, int slot) {
        if (!level.isClientSide()) {
            ItemStack itemstack = blockEntity.removeItem(slot, 1);
            SoundEvent soundevent = itemstack.is(Items.ENCHANTED_BOOK)
                ? SoundEvents.CHISELED_BOOKSHELF_PICKUP_ENCHANTED
                : SoundEvents.CHISELED_BOOKSHELF_PICKUP;
            level.playSound(null, pos, soundevent, SoundSource.BLOCKS, 1.0F, 1.0F);
            if (!player.getInventory().add(itemstack)) {
                player.drop(itemstack, false);
            }

            level.gameEvent(player, GameEvent.BLOCK_CHANGE, pos);
        }
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos p_250440_, BlockState p_248729_) {
        return new ChiseledBookShelfBlockEntity(p_250440_, p_248729_);
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> p_250973_) {
        p_250973_.add(FACING);
        SLOT_OCCUPIED_PROPERTIES.forEach(p_261456_ -> p_250973_.add(p_261456_));
    }

    @Override
    protected void affectNeighborsAfterRemoval(BlockState p_394006_, ServerLevel p_394108_, BlockPos p_394432_, boolean p_393697_) {
        Containers.updateNeighboursAfterDestroy(p_394006_, p_394108_, p_394432_);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext p_251318_) {
        return this.defaultBlockState().setValue(FACING, p_251318_.getHorizontalDirection().getOpposite());
    }

    @Override
    public BlockState rotate(BlockState p_288975_, Rotation p_288993_) {
        return p_288975_.setValue(FACING, p_288993_.rotate(p_288975_.getValue(FACING)));
    }

    @Override
    public BlockState mirror(BlockState p_289000_, Mirror p_288962_) {
        return p_289000_.rotate(p_288962_.getRotation(p_289000_.getValue(FACING)));
    }

    @Override
    protected boolean hasAnalogOutputSignal(BlockState p_249302_) {
        return true;
    }

    @Override
    protected int getAnalogOutputSignal(BlockState p_249192_, Level p_252207_, BlockPos p_248999_, Direction p_433264_) {
        if (p_252207_.isClientSide()) {
            return 0;
        } else {
            return p_252207_.getBlockEntity(p_248999_) instanceof ChiseledBookShelfBlockEntity chiseledbookshelfblockentity
                ? chiseledbookshelfblockentity.getLastInteractedSlot() + 1
                : 0;
        }
    }
}
