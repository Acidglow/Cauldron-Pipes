package net.minecraft.world.level.block;

import com.mojang.serialization.MapCodec;
import java.util.function.BiConsumer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Containers;
import net.minecraft.world.attribute.EnvironmentAttributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.ServerExplosion;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.entity.CreakingHeartBlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.CreakingHeartState;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import org.jspecify.annotations.Nullable;

public class CreakingHeartBlock extends BaseEntityBlock {
    public static final MapCodec<CreakingHeartBlock> CODEC = simpleCodec(CreakingHeartBlock::new);
    public static final EnumProperty<Direction.Axis> AXIS = BlockStateProperties.AXIS;
    public static final EnumProperty<CreakingHeartState> STATE = BlockStateProperties.CREAKING_HEART_STATE;
    public static final BooleanProperty NATURAL = BlockStateProperties.NATURAL;

    @Override
    public MapCodec<CreakingHeartBlock> codec() {
        return CODEC;
    }

    public CreakingHeartBlock(BlockBehaviour.Properties p_380228_) {
        super(p_380228_);
        this.registerDefaultState(
            this.defaultBlockState().setValue(AXIS, Direction.Axis.Y).setValue(STATE, CreakingHeartState.UPROOTED).setValue(NATURAL, false)
        );
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos p_380178_, BlockState p_380317_) {
        return new CreakingHeartBlockEntity(p_380178_, p_380317_);
    }

    @Override
    public <T extends BlockEntity> @Nullable BlockEntityTicker<T> getTicker(Level p_379447_, BlockState p_379641_, BlockEntityType<T> p_380325_) {
        if (p_379447_.isClientSide()) {
            return null;
        } else {
            return p_379641_.getValue(STATE) != CreakingHeartState.UPROOTED
                ? createTickerHelper(p_380325_, BlockEntityType.CREAKING_HEART, CreakingHeartBlockEntity::serverTick)
                : null;
        }
    }

    @Override
    public void animateTick(BlockState p_379556_, Level p_379594_, BlockPos p_379297_, RandomSource p_379301_) {
        if (p_379594_.environmentAttributes().getValue(EnvironmentAttributes.CREAKING_ACTIVE, p_379297_)) {
            if (p_379556_.getValue(STATE) != CreakingHeartState.UPROOTED) {
                if (p_379301_.nextInt(16) == 0 && isSurroundedByLogs(p_379594_, p_379297_)) {
                    p_379594_.playLocalSound(
                        p_379297_.getX(), p_379297_.getY(), p_379297_.getZ(), SoundEvents.CREAKING_HEART_IDLE, SoundSource.BLOCKS, 1.0F, 1.0F, false
                    );
                }
            }
        }
    }

    @Override
    protected BlockState updateShape(
        BlockState p_379552_,
        LevelReader p_379446_,
        ScheduledTickAccess p_379318_,
        BlockPos p_379343_,
        Direction p_380340_,
        BlockPos p_380150_,
        BlockState p_379791_,
        RandomSource p_379888_
    ) {
        p_379318_.scheduleTick(p_379343_, this, 1);
        return super.updateShape(p_379552_, p_379446_, p_379318_, p_379343_, p_380340_, p_380150_, p_379791_, p_379888_);
    }

    @Override
    protected void tick(BlockState p_394029_, ServerLevel p_394479_, BlockPos p_394306_, RandomSource p_393870_) {
        BlockState blockstate = updateState(p_394029_, p_394479_, p_394306_);
        if (blockstate != p_394029_) {
            p_394479_.setBlock(p_394306_, blockstate, 3);
        }
    }

    private static BlockState updateState(BlockState state, Level level, BlockPos pos) {
        boolean flag = hasRequiredLogs(state, level, pos);
        boolean flag1 = state.getValue(STATE) == CreakingHeartState.UPROOTED;
        return flag && flag1
            ? state.setValue(
                STATE,
                level.environmentAttributes().getValue(EnvironmentAttributes.CREAKING_ACTIVE, pos)
                    ? CreakingHeartState.AWAKE
                    : CreakingHeartState.DORMANT
            )
            : state;
    }

    public static boolean hasRequiredLogs(BlockState state, LevelReader level, BlockPos pos) {
        Direction.Axis direction$axis = state.getValue(AXIS);

        for (Direction direction : direction$axis.getDirections()) {
            BlockState blockstate = level.getBlockState(pos.relative(direction));
            if (!blockstate.is(BlockTags.PALE_OAK_LOGS) || blockstate.getValue(AXIS) != direction$axis) {
                return false;
            }
        }

        return true;
    }

    private static boolean isSurroundedByLogs(LevelAccessor level, BlockPos pos) {
        for (Direction direction : Direction.values()) {
            BlockPos blockpos = pos.relative(direction);
            BlockState blockstate = level.getBlockState(blockpos);
            if (!blockstate.is(BlockTags.PALE_OAK_LOGS)) {
                return false;
            }
        }

        return true;
    }

    @Override
    public @Nullable BlockState getStateForPlacement(BlockPlaceContext p_379431_) {
        return updateState(this.defaultBlockState().setValue(AXIS, p_379431_.getClickedFace().getAxis()), p_379431_.getLevel(), p_379431_.getClickedPos());
    }

    @Override
    protected BlockState rotate(BlockState p_380251_, Rotation p_379529_) {
        return RotatedPillarBlock.rotatePillar(p_380251_, p_379529_);
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> p_379898_) {
        p_379898_.add(AXIS, STATE, NATURAL);
    }

    @Override
    protected void affectNeighborsAfterRemoval(BlockState p_393635_, ServerLevel p_394550_, BlockPos p_394080_, boolean p_394343_) {
        Containers.updateNeighboursAfterDestroy(p_393635_, p_394550_, p_394080_);
    }

    @Override
    protected void onExplosionHit(
        BlockState p_382935_, ServerLevel p_382804_, BlockPos p_383050_, Explosion p_383064_, BiConsumer<ItemStack, BlockPos> p_383124_
    ) {
        if (p_382804_.getBlockEntity(p_383050_) instanceof CreakingHeartBlockEntity creakingheartblockentity
            && p_383064_ instanceof ServerExplosion serverexplosion
            && p_383064_.getBlockInteraction().shouldAffectBlocklikeEntities()) {
            creakingheartblockentity.removeProtector(serverexplosion.getDamageSource());
            if (p_383064_.getIndirectSourceEntity() instanceof Player player && p_383064_.getBlockInteraction().shouldAffectBlocklikeEntities()) {
                this.tryAwardExperience(player, p_382935_, p_382804_, p_383050_);
            }
        }

        super.onExplosionHit(p_382935_, p_382804_, p_383050_, p_383064_, p_383124_);
    }

    @Override
    public BlockState playerWillDestroy(Level p_380319_, BlockPos p_379939_, BlockState p_379928_, Player p_380097_) {
        if (p_380319_.getBlockEntity(p_379939_) instanceof CreakingHeartBlockEntity creakingheartblockentity) {
            creakingheartblockentity.removeProtector(p_380097_.damageSources().playerAttack(p_380097_));
            this.tryAwardExperience(p_380097_, p_379928_, p_380319_, p_379939_);
        }

        return super.playerWillDestroy(p_380319_, p_379939_, p_379928_, p_380097_);
    }

    private void tryAwardExperience(Player player, BlockState state, Level level, BlockPos pos) {
        if (!player.preventsBlockDrops() && !player.isSpectator() && state.getValue(NATURAL) && level instanceof ServerLevel serverlevel) {
            this.popExperience(serverlevel, pos, level.random.nextIntBetweenInclusive(20, 24));
        }
    }

    @Override
    protected boolean hasAnalogOutputSignal(BlockState p_380993_) {
        return true;
    }

    @Override
    protected int getAnalogOutputSignal(BlockState p_381152_, Level p_381142_, BlockPos p_381148_, Direction p_432849_) {
        if (p_381152_.getValue(STATE) == CreakingHeartState.UPROOTED) {
            return 0;
        } else {
            return p_381142_.getBlockEntity(p_381148_) instanceof CreakingHeartBlockEntity creakingheartblockentity
                ? creakingheartblockentity.getAnalogOutputSignal()
                : 0;
        }
    }
}
