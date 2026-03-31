package net.minecraft.world.level.block;

import com.mojang.serialization.MapCodec;
import java.util.Arrays;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.PlainTextContents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.util.RandomSource;
import net.minecraft.util.Util;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SignApplicator;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.entity.SignBlockEntity;
import net.minecraft.world.level.block.entity.SignText;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.WoodType;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jspecify.annotations.Nullable;

public abstract class SignBlock extends BaseEntityBlock implements SimpleWaterloggedBlock {
    public static final BooleanProperty WATERLOGGED = BlockStateProperties.WATERLOGGED;
    private static final VoxelShape SHAPE = Block.column(8.0, 0.0, 16.0);
    private final WoodType type;

    public SignBlock(WoodType type, BlockBehaviour.Properties properties) {
        super(properties);
        this.type = type;
    }

    @Override
    protected abstract MapCodec<? extends SignBlock> codec();

    @Override
    protected BlockState updateShape(
        BlockState p_56285_,
        LevelReader p_374509_,
        ScheduledTickAccess p_374520_,
        BlockPos p_56289_,
        Direction p_56286_,
        BlockPos p_56290_,
        BlockState p_56287_,
        RandomSource p_374213_
    ) {
        if (p_56285_.getValue(WATERLOGGED)) {
            p_374520_.scheduleTick(p_56289_, Fluids.WATER, Fluids.WATER.getTickDelay(p_374509_));
        }

        return super.updateShape(p_56285_, p_374509_, p_374520_, p_56289_, p_56286_, p_56290_, p_56287_, p_374213_);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    public boolean isPossibleToRespawnInThis(BlockState p_279137_) {
        return true;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos p_154556_, BlockState p_154557_) {
        return new SignBlockEntity(p_154556_, p_154557_);
    }

    @Override
    protected InteractionResult useItemOn(
        ItemStack p_316116_, BlockState p_316899_, Level p_316692_, BlockPos p_316578_, Player p_316244_, InteractionHand p_316196_, BlockHitResult p_316744_
    ) {
        if (p_316692_.getBlockEntity(p_316578_) instanceof SignBlockEntity signblockentity) {
            SignApplicator signapplicator1 = p_316116_.getItem() instanceof SignApplicator signapplicator ? signapplicator : null;
            boolean flag1 = signapplicator1 != null && p_316244_.mayBuild();
            if (p_316692_ instanceof ServerLevel serverlevel) {
                if (flag1 && !signblockentity.isWaxed() && !this.otherPlayerIsEditingSign(p_316244_, signblockentity)) {
                    boolean flag = signblockentity.isFacingFrontText(p_316244_);
                    if (signapplicator1.canApplyToSign(signblockentity.getText(flag), p_316244_)
                        && signapplicator1.tryApplyToSign(serverlevel, signblockentity, flag, p_316244_)) {
                        signblockentity.executeClickCommandsIfPresent(serverlevel, p_316244_, p_316578_, flag);
                        p_316244_.awardStat(Stats.ITEM_USED.get(p_316116_.getItem()));
                        serverlevel.gameEvent(
                            GameEvent.BLOCK_CHANGE, signblockentity.getBlockPos(), GameEvent.Context.of(p_316244_, signblockentity.getBlockState())
                        );
                        p_316116_.consume(1, p_316244_);
                        return InteractionResult.SUCCESS;
                    } else {
                        return InteractionResult.TRY_WITH_EMPTY_HAND;
                    }
                } else {
                    return InteractionResult.TRY_WITH_EMPTY_HAND;
                }
            } else {
                return !flag1 && !signblockentity.isWaxed() ? InteractionResult.CONSUME : InteractionResult.SUCCESS;
            }
        } else {
            return InteractionResult.PASS;
        }
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState p_316779_, Level p_316615_, BlockPos p_316127_, Player p_316173_, BlockHitResult p_316850_) {
        if (p_316615_.getBlockEntity(p_316127_) instanceof SignBlockEntity signblockentity) {
            if (p_316615_ instanceof ServerLevel serverlevel) {
                boolean $$9 = signblockentity.isFacingFrontText(p_316173_);
                boolean $$10 = signblockentity.executeClickCommandsIfPresent(serverlevel, p_316173_, p_316127_, $$9);
                if (signblockentity.isWaxed()) {
                    serverlevel.playSound(null, signblockentity.getBlockPos(), signblockentity.getSignInteractionFailedSoundEvent(), SoundSource.BLOCKS);
                    return InteractionResult.SUCCESS_SERVER;
                } else if ($$10) {
                    return InteractionResult.SUCCESS_SERVER;
                } else if (!this.otherPlayerIsEditingSign(p_316173_, signblockentity)
                    && p_316173_.mayBuild()
                    && this.hasEditableText(p_316173_, signblockentity, $$9)) {
                    this.openTextEdit(p_316173_, signblockentity, $$9);
                    return InteractionResult.SUCCESS_SERVER;
                } else {
                    return InteractionResult.PASS;
                }
            } else {
                Util.pauseInIde(new IllegalStateException("Expected to only call this on server"));
                return InteractionResult.CONSUME;
            }
        } else {
            return InteractionResult.PASS;
        }
    }

    private boolean hasEditableText(Player player, SignBlockEntity signEntity, boolean isFrontText) {
        SignText signtext = signEntity.getText(isFrontText);
        return Arrays.stream(signtext.getMessages(player.isTextFilteringEnabled()))
            .allMatch(p_339537_ -> p_339537_.equals(CommonComponents.EMPTY) || p_339537_.getContents() instanceof PlainTextContents);
    }

    public abstract float getYRotationDegrees(BlockState state);

    public Vec3 getSignHitboxCenterPosition(BlockState state) {
        return new Vec3(0.5, 0.5, 0.5);
    }

    @Override
    protected FluidState getFluidState(BlockState state) {
        return state.getValue(WATERLOGGED) ? Fluids.WATER.getSource(false) : super.getFluidState(state);
    }

    public WoodType type() {
        return this.type;
    }

    public static WoodType getWoodType(Block block) {
        WoodType woodtype;
        if (block instanceof SignBlock) {
            woodtype = ((SignBlock)block).type();
        } else {
            woodtype = WoodType.OAK;
        }

        return woodtype;
    }

    public void openTextEdit(Player player, SignBlockEntity signEntity, boolean isFrontText) {
        signEntity.setAllowedPlayerEditor(player.getUUID());
        player.openTextEdit(signEntity, isFrontText);
    }

    private boolean otherPlayerIsEditingSign(Player player, SignBlockEntity signEntity) {
        UUID uuid = signEntity.getPlayerWhoMayEdit();
        return uuid != null && !uuid.equals(player.getUUID());
    }

    @Override
    public <T extends BlockEntity> @Nullable BlockEntityTicker<T> getTicker(Level p_277367_, BlockState p_277896_, BlockEntityType<T> p_277724_) {
        return createTickerHelper(p_277724_, BlockEntityType.SIGN, SignBlockEntity::tick);
    }
}
