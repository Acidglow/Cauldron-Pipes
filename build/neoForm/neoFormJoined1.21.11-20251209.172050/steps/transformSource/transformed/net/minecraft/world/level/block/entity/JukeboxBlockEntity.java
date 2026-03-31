package net.minecraft.world.level.block.entity;

import com.google.common.annotations.VisibleForTesting;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.Container;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.JukeboxSong;
import net.minecraft.world.item.JukeboxSongPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.JukeboxBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.ticks.ContainerSingleItem;

public class JukeboxBlockEntity extends BlockEntity implements ContainerSingleItem.BlockContainerSingleItem {
    public static final String SONG_ITEM_TAG_ID = "RecordItem";
    public static final String TICKS_SINCE_SONG_STARTED_TAG_ID = "ticks_since_song_started";
    private ItemStack item = ItemStack.EMPTY;
    private final JukeboxSongPlayer jukeboxSongPlayer = new JukeboxSongPlayer(this::onSongChanged, this.getBlockPos());

    public JukeboxBlockEntity(BlockPos pos, BlockState blockState) {
        super(BlockEntityType.JUKEBOX, pos, blockState);
    }

    public JukeboxSongPlayer getSongPlayer() {
        return this.jukeboxSongPlayer;
    }

    public void onSongChanged() {
        this.level.updateNeighborsAt(this.getBlockPos(), this.getBlockState().getBlock());
        this.setChanged();
    }

    private void notifyItemChangedInJukebox(boolean hasRecord) {
        if (this.level != null && this.level.getBlockState(this.getBlockPos()) == this.getBlockState()) {
            this.level.setBlock(this.getBlockPos(), this.getBlockState().setValue(JukeboxBlock.HAS_RECORD, hasRecord), 2);
            this.level.gameEvent(GameEvent.BLOCK_CHANGE, this.getBlockPos(), GameEvent.Context.of(this.getBlockState()));
        }
    }

    public void popOutTheItem() {
        if (this.level != null && !this.level.isClientSide()) {
            BlockPos blockpos = this.getBlockPos();
            ItemStack itemstack = this.getTheItem();
            if (!itemstack.isEmpty()) {
                this.removeTheItem();
                Vec3 vec3 = Vec3.atLowerCornerWithOffset(blockpos, 0.5, 1.01, 0.5).offsetRandomXZ(this.level.random, 0.7F);
                ItemStack itemstack1 = itemstack.copy();
                ItemEntity itementity = new ItemEntity(this.level, vec3.x(), vec3.y(), vec3.z(), itemstack1);
                itementity.setDefaultPickUpDelay();
                this.level.addFreshEntity(itementity);
                this.onSongChanged();
            }
        }
    }

    public static void tick(Level level, BlockPos pos, BlockState state, JukeboxBlockEntity jukebox) {
        jukebox.jukeboxSongPlayer.tick(level, state);
    }

    public int getComparatorOutput() {
        return JukeboxSong.fromStack(this.level.registryAccess(), this.item).map(Holder::value).map(JukeboxSong::comparatorOutput).orElse(0);
    }

    @Override
    protected void loadAdditional(ValueInput p_422647_) {
        super.loadAdditional(p_422647_);
        ItemStack itemstack = p_422647_.read("RecordItem", ItemStack.CODEC).orElse(ItemStack.EMPTY);
        if (!this.item.isEmpty() && !ItemStack.isSameItemSameComponents(itemstack, this.item)) {
            this.jukeboxSongPlayer.stop(this.level, this.getBlockState());
        }

        this.item = itemstack;
        p_422647_.getLong("ticks_since_song_started")
            .ifPresent(
                p_421419_ -> JukeboxSong.fromStack(p_422647_.lookup(), this.item)
                    .ifPresent(p_409491_ -> this.jukeboxSongPlayer.setSongWithoutPlaying((Holder<JukeboxSong>)p_409491_, p_421419_))
            );
    }

    @Override
    protected void saveAdditional(ValueOutput p_421534_) {
        super.saveAdditional(p_421534_);
        if (!this.getTheItem().isEmpty()) {
            p_421534_.store("RecordItem", ItemStack.CODEC, this.getTheItem());
        }

        if (this.jukeboxSongPlayer.getSong() != null) {
            p_421534_.putLong("ticks_since_song_started", this.jukeboxSongPlayer.getTicksSinceSongStarted());
        }
    }

    @Override
    public ItemStack getTheItem() {
        return this.item;
    }

    @Override
    public ItemStack splitTheItem(int p_304604_) {
        ItemStack itemstack = this.item;
        this.setTheItem(ItemStack.EMPTY);
        return itemstack;
    }

    // Neo: Support deferring non-transactional notification and song player update until the end of a transaction
    private final net.neoforged.neoforge.transfer.transaction.RootCommitJournal itemChangedJournal =
            new net.neoforged.neoforge.transfer.transaction.RootCommitJournal(() -> {
                if (!isRemoved()) {
                    itemChanged();
                }
            });
    @Override
    public void onTransfer(int slot, int amountChange, net.neoforged.neoforge.transfer.transaction.TransactionContext transaction) {
        itemChangedJournal.updateSnapshots(transaction);
    }

    @Override
    public void setItem(int slot, ItemStack stack, boolean insideTransaction) {
        if (slot == 0) {
            if (insideTransaction) {
                this.item = stack;
            } else {
                setTheItem(stack);
            }
        }
    }
    @Override
    public void setTheItem(ItemStack p_304781_) {
        this.item = p_304781_;
        itemChanged();
    }
    private void itemChanged() {
        boolean flag = !this.item.isEmpty();
        Optional<Holder<JukeboxSong>> optional = JukeboxSong.fromStack(this.level.registryAccess(), this.item);
        this.notifyItemChangedInJukebox(flag);
        if (flag && optional.isPresent()) {
            this.jukeboxSongPlayer.play(this.level, optional.get());
        } else {
            this.jukeboxSongPlayer.stop(this.level, this.getBlockState());
        }
    }

    @Override
    public void setRemoved() {
        super.setRemoved();
        this.level.gameEvent(GameEvent.JUKEBOX_STOP_PLAY, this.getBlockPos(), GameEvent.Context.of(this.getBlockState()));
        this.level.levelEvent(1011, this.getBlockPos(), 0);
    }

    @Override
    public int getMaxStackSize() {
        return 1;
    }

    @Override
    public BlockEntity getContainerBlockEntity() {
        return this;
    }

    @Override
    public boolean canPlaceItem(int p_273369_, ItemStack p_273689_) {
        return p_273689_.has(DataComponents.JUKEBOX_PLAYABLE) && this.getItem(p_273369_).isEmpty();
    }

    @Override
    public boolean canTakeItem(Container p_273497_, int p_273168_, ItemStack p_273785_) {
        return p_273497_.hasAnyMatching(ItemStack::isEmpty);
    }

    @Override
    public void preRemoveSideEffects(BlockPos p_394607_, BlockState p_393961_) {
        this.popOutTheItem();
    }

    @VisibleForTesting
    public void setSongItemWithoutPlaying(ItemStack stack) {
        this.item = stack;
        JukeboxSong.fromStack(this.level.registryAccess(), stack)
            .ifPresent(p_350672_ -> this.jukeboxSongPlayer.setSongWithoutPlaying((Holder<JukeboxSong>)p_350672_, 0L));
        this.level.updateNeighborsAt(this.getBlockPos(), this.getBlockState().getBlock());
        this.setChanged();
    }

    @VisibleForTesting
    public void tryForcePlaySong() {
        JukeboxSong.fromStack(this.level.registryAccess(), this.getTheItem())
            .ifPresent(p_350319_ -> this.jukeboxSongPlayer.play(this.level, (Holder<JukeboxSong>)p_350319_));
    }
}
