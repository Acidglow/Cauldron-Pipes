package net.minecraft.world.level.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.tags.ItemTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.entity.SkullBlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.block.state.properties.NoteBlockInstrument;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.redstone.Orientation;
import net.minecraft.world.phys.BlockHitResult;
import org.jspecify.annotations.Nullable;

public class NoteBlock extends Block {
    public static final MapCodec<NoteBlock> CODEC = simpleCodec(NoteBlock::new);
    public static final EnumProperty<NoteBlockInstrument> INSTRUMENT = BlockStateProperties.NOTEBLOCK_INSTRUMENT;
    public static final BooleanProperty POWERED = BlockStateProperties.POWERED;
    public static final IntegerProperty NOTE = BlockStateProperties.NOTE;
    public static final int NOTE_VOLUME = 3;

    @Override
    public MapCodec<NoteBlock> codec() {
        return CODEC;
    }

    public NoteBlock(BlockBehaviour.Properties p_55016_) {
        super(p_55016_);
        this.registerDefaultState(this.stateDefinition.any().setValue(INSTRUMENT, NoteBlockInstrument.HARP).setValue(NOTE, 0).setValue(POWERED, false));
    }

    private BlockState setInstrument(LevelReader level, BlockPos pos, BlockState state) {
        NoteBlockInstrument noteblockinstrument = level.getBlockState(pos.above()).instrument();
        if (noteblockinstrument.worksAboveNoteBlock()) {
            return state.setValue(INSTRUMENT, noteblockinstrument);
        } else {
            NoteBlockInstrument noteblockinstrument1 = level.getBlockState(pos.below()).instrument();
            NoteBlockInstrument noteblockinstrument2 = noteblockinstrument1.worksAboveNoteBlock() ? NoteBlockInstrument.HARP : noteblockinstrument1;
            return state.setValue(INSTRUMENT, noteblockinstrument2);
        }
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return this.setInstrument(context.getLevel(), context.getClickedPos(), this.defaultBlockState());
    }

    @Override
    protected BlockState updateShape(
        BlockState p_55048_,
        LevelReader p_374298_,
        ScheduledTickAccess p_374153_,
        BlockPos p_55052_,
        Direction p_55049_,
        BlockPos p_55053_,
        BlockState p_55050_,
        RandomSource p_374540_
    ) {
        boolean flag = p_55049_.getAxis() == Direction.Axis.Y;
        return flag
            ? this.setInstrument(p_374298_, p_55052_, p_55048_)
            : super.updateShape(p_55048_, p_374298_, p_374153_, p_55052_, p_55049_, p_55053_, p_55050_, p_374540_);
    }

    @Override
    protected void neighborChanged(BlockState p_55041_, Level p_55042_, BlockPos p_55043_, Block p_55044_, @Nullable Orientation p_361888_, boolean p_55046_) {
        boolean flag = p_55042_.hasNeighborSignal(p_55043_);
        if (flag != p_55041_.getValue(POWERED)) {
            if (flag) {
                this.playNote(null, p_55041_, p_55042_, p_55043_);
            }

            p_55042_.setBlock(p_55043_, p_55041_.setValue(POWERED, flag), 3);
        }
    }

    private void playNote(@Nullable Entity entity, BlockState state, Level level, BlockPos pos) {
        if (state.getValue(INSTRUMENT).worksAboveNoteBlock() || level.getBlockState(pos.above()).isAir()) {
            level.blockEvent(pos, this, 0, 0);
            level.gameEvent(entity, GameEvent.NOTE_BLOCK_PLAY, pos);
        }
    }

    @Override
    protected InteractionResult useItemOn(
        ItemStack p_316716_, BlockState p_316688_, Level p_316672_, BlockPos p_316355_, Player p_316822_, InteractionHand p_316505_, BlockHitResult p_316667_
    ) {
        return (InteractionResult)(p_316716_.is(ItemTags.NOTE_BLOCK_TOP_INSTRUMENTS) && p_316667_.getDirection() == Direction.UP
            ? InteractionResult.PASS
            : super.useItemOn(p_316716_, p_316688_, p_316672_, p_316355_, p_316822_, p_316505_, p_316667_));
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState p_316441_, Level p_316774_, BlockPos p_316344_, Player p_316884_, BlockHitResult p_316631_) {
        if (!p_316774_.isClientSide()) {
            int _new = net.neoforged.neoforge.common.CommonHooks.onNoteChange(p_316774_, p_316344_, p_316441_, p_316441_.getValue(NOTE), p_316441_.cycle(NOTE).getValue(NOTE));
            if (_new == -1) return InteractionResult.FAIL;
            p_316441_ = p_316441_.setValue(NOTE, _new);
            p_316774_.setBlock(p_316344_, p_316441_, 3);
            this.playNote(p_316884_, p_316441_, p_316774_, p_316344_);
            p_316884_.awardStat(Stats.TUNE_NOTEBLOCK);
        }

        return InteractionResult.SUCCESS;
    }

    @Override
    protected void attack(BlockState state, Level level, BlockPos pos, Player player) {
        if (!level.isClientSide()) {
            this.playNote(player, state, level, pos);
            player.awardStat(Stats.PLAY_NOTEBLOCK);
        }
    }

    public static float getPitchFromNote(int note) {
        return (float)Math.pow(2.0, (note - 12) / 12.0);
    }

    @Override
    protected boolean triggerEvent(BlockState state, Level level, BlockPos pos, int id, int param) {
        net.neoforged.neoforge.event.level.NoteBlockEvent.Play e = new net.neoforged.neoforge.event.level.NoteBlockEvent.Play(level, pos, state, state.getValue(NOTE), state.getValue(INSTRUMENT));
        if (net.neoforged.neoforge.common.NeoForge.EVENT_BUS.post(e).isCanceled()) return false;
        state = state.setValue(NOTE, e.getVanillaNoteId()).setValue(INSTRUMENT, e.getInstrument());
        NoteBlockInstrument noteblockinstrument = state.getValue(INSTRUMENT);
        float f;
        if (noteblockinstrument.isTunable()) {
            int i = state.getValue(NOTE);
            f = getPitchFromNote(i);
            level.addParticle(ParticleTypes.NOTE, pos.getX() + 0.5, pos.getY() + 1.2, pos.getZ() + 0.5, i / 24.0, 0.0, 0.0);
        } else {
            f = 1.0F;
        }

        Holder<SoundEvent> holder;
        if (noteblockinstrument.hasCustomSound()) {
            Identifier identifier = this.getCustomSoundId(level, pos);
            if (identifier == null) {
                return false;
            }

            holder = Holder.direct(SoundEvent.createVariableRangeEvent(identifier));
        } else {
            holder = noteblockinstrument.getSoundEvent();
        }

        level.playSeededSound(
            null, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, holder, SoundSource.RECORDS, 3.0F, f, level.random.nextLong()
        );
        return true;
    }

    private @Nullable Identifier getCustomSoundId(Level level, BlockPos pos) {
        return level.getBlockEntity(pos.above()) instanceof SkullBlockEntity skullblockentity ? skullblockentity.getNoteBlockSound() : null;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(INSTRUMENT, POWERED, NOTE);
    }
}
