package net.minecraft.world.level.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.PrimedTnt;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.gamerules.GameRules;
import net.minecraft.world.level.redstone.Orientation;
import net.minecraft.world.phys.BlockHitResult;
import org.jspecify.annotations.Nullable;

public class TntBlock extends Block {
    public static final MapCodec<TntBlock> CODEC = simpleCodec(TntBlock::new);
    public static final BooleanProperty UNSTABLE = BlockStateProperties.UNSTABLE;

    @Override
    public MapCodec<TntBlock> codec() {
        return CODEC;
    }

    public TntBlock(BlockBehaviour.Properties p_57422_) {
        super(p_57422_);
        this.registerDefaultState(this.defaultBlockState().setValue(UNSTABLE, false));
    }

    @Override
    protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean isMoving) {
        if (!oldState.is(state.getBlock())) {
            if (level.hasNeighborSignal(pos) && onCaughtFire(state, level, pos, null, null)) {
                level.removeBlock(pos, false);
            }
        }
    }

    @Override
    protected void neighborChanged(BlockState p_57457_, Level p_57458_, BlockPos p_57459_, Block p_57460_, @Nullable Orientation p_364510_, boolean p_57462_) {
        if (p_57458_.hasNeighborSignal(p_57459_) && onCaughtFire(p_57457_, p_57458_, p_57459_, null, null)) {
            p_57458_.removeBlock(p_57459_, false);
        }
    }

    @Override
    public BlockState playerWillDestroy(Level p_57445_, BlockPos p_57446_, BlockState p_57447_, Player p_57448_) {
        if (!p_57445_.isClientSide() && !p_57448_.getAbilities().instabuild && p_57447_.getValue(UNSTABLE)) {
            onCaughtFire(p_57447_, p_57445_, p_57446_, null, null);
        }

        return super.playerWillDestroy(p_57445_, p_57446_, p_57447_, p_57448_);
    }

    @Override
    public void wasExploded(ServerLevel p_364953_, BlockPos p_57442_, Explosion p_57443_) {
        if (p_364953_.getGameRules().get(GameRules.TNT_EXPLODES)) {
            PrimedTnt primedtnt = new PrimedTnt(p_364953_, p_57442_.getX() + 0.5, p_57442_.getY(), p_57442_.getZ() + 0.5, p_57443_.getIndirectSourceEntity());
            int i = primedtnt.getFuse();
            primedtnt.setFuse((short)(p_364953_.random.nextInt(i / 4) + i / 8));
            p_364953_.addFreshEntity(primedtnt);
        }
    }

    /**
 * @deprecated Neo: use {@link
 *             net.neoforged.neoforge.common.extensions.IBlockStateExtension#
 *             onCaughtFire(Level,BlockPos,net.minecraft.core.Direction,
 *             LivingEntity)} instead
 */
    @Deprecated
    public static boolean prime(Level level, BlockPos pos) {
        return prime(level, pos, null);
    }

    /**
 * @deprecated Neo: use {@link
 *             net.neoforged.neoforge.common.extensions.IBlockStateExtension#
 *             onCaughtFire(Level,BlockPos,net.minecraft.core.Direction,
 *             LivingEntity)} instead
 */
    @Deprecated
    private static boolean prime(Level level, BlockPos pos, @Nullable LivingEntity entity) {
        if (level instanceof ServerLevel serverlevel && serverlevel.getGameRules().get(GameRules.TNT_EXPLODES)) {
            PrimedTnt primedtnt = new PrimedTnt(level, pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, entity);
            level.addFreshEntity(primedtnt);
            level.playSound(null, primedtnt.getX(), primedtnt.getY(), primedtnt.getZ(), SoundEvents.TNT_PRIMED, SoundSource.BLOCKS, 1.0F, 1.0F);
            level.gameEvent(entity, GameEvent.PRIME_FUSE, pos);
            return true;
        } else {
            return false;
        }
    }

    @Override
    protected InteractionResult useItemOn(
        ItemStack p_316149_, BlockState p_316217_, Level p_316520_, BlockPos p_316601_, Player p_316770_, InteractionHand p_316393_, BlockHitResult p_316532_
    ) {
        if (!p_316149_.is(Items.FLINT_AND_STEEL) && !p_316149_.is(Items.FIRE_CHARGE)) {
            return super.useItemOn(p_316149_, p_316217_, p_316520_, p_316601_, p_316770_, p_316393_, p_316532_);
        } else {
            if (onCaughtFire(p_316217_, p_316520_, p_316601_, p_316532_.getDirection(), p_316770_)) {
                p_316520_.setBlock(p_316601_, Blocks.AIR.defaultBlockState(), 11);
                Item item = p_316149_.getItem();
                if (p_316149_.is(Items.FLINT_AND_STEEL)) {
                    p_316149_.hurtAndBreak(1, p_316770_, p_316393_.asEquipmentSlot());
                } else {
                    p_316149_.consume(1, p_316770_);
                }

                p_316770_.awardStat(Stats.ITEM_USED.get(item));
            } else if (p_316520_ instanceof ServerLevel serverlevel && !serverlevel.getGameRules().get(GameRules.TNT_EXPLODES)) {
                p_316770_.displayClientMessage(Component.translatable("block.minecraft.tnt.disabled"), true);
                return InteractionResult.PASS;
            }

            return InteractionResult.SUCCESS;
        }
    }

    @Override
    protected void onProjectileHit(Level level, BlockState state, BlockHitResult hit, Projectile projectile) {
        if (level instanceof ServerLevel serverlevel) {
            BlockPos blockpos = hit.getBlockPos();
            Entity entity = projectile.getOwner();
            if (projectile.isOnFire()
                && projectile.mayInteract(serverlevel, blockpos)
                && onCaughtFire(state, level, blockpos, null, entity instanceof LivingEntity ? (LivingEntity)entity : null)) {
                level.removeBlock(blockpos, false);
            }
        }
    }

    @Override
    public boolean dropFromExplosion(Explosion explosion) {
        return false;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(UNSTABLE);
    }

    @Override
    public boolean onCaughtFire(BlockState state, Level world, BlockPos pos, net.minecraft.core.@Nullable Direction face, @Nullable LivingEntity igniter) {
        return prime(world, pos, igniter);
    }
}
