package net.minecraft.world.level.block;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableList.Builder;
import com.mojang.serialization.MapCodec;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.attribute.EnvironmentAttributes;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.vehicle.DismountHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.CollisionGetter;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.ExplosionDamageCalculator;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraft.world.level.storage.LevelData;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

public class RespawnAnchorBlock extends Block {
    public static final MapCodec<RespawnAnchorBlock> CODEC = simpleCodec(RespawnAnchorBlock::new);
    public static final int MIN_CHARGES = 0;
    public static final int MAX_CHARGES = 4;
    public static final IntegerProperty CHARGE = BlockStateProperties.RESPAWN_ANCHOR_CHARGES;
    private static final ImmutableList<Vec3i> RESPAWN_HORIZONTAL_OFFSETS = ImmutableList.of(
        new Vec3i(0, 0, -1),
        new Vec3i(-1, 0, 0),
        new Vec3i(0, 0, 1),
        new Vec3i(1, 0, 0),
        new Vec3i(-1, 0, -1),
        new Vec3i(1, 0, -1),
        new Vec3i(-1, 0, 1),
        new Vec3i(1, 0, 1)
    );
    private static final ImmutableList<Vec3i> RESPAWN_OFFSETS = new Builder<Vec3i>()
        .addAll(RESPAWN_HORIZONTAL_OFFSETS)
        .addAll(RESPAWN_HORIZONTAL_OFFSETS.stream().map(Vec3i::below).iterator())
        .addAll(RESPAWN_HORIZONTAL_OFFSETS.stream().map(Vec3i::above).iterator())
        .add(new Vec3i(0, 1, 0))
        .build();

    @Override
    public MapCodec<RespawnAnchorBlock> codec() {
        return CODEC;
    }

    public RespawnAnchorBlock(BlockBehaviour.Properties p_55838_) {
        super(p_55838_);
        this.registerDefaultState(this.stateDefinition.any().setValue(CHARGE, 0));
    }

    @Override
    protected InteractionResult useItemOn(
        ItemStack p_316531_, BlockState p_316665_, Level p_316165_, BlockPos p_316402_, Player p_316556_, InteractionHand p_316586_, BlockHitResult p_316326_
    ) {
        if (isRespawnFuel(p_316531_) && canBeCharged(p_316665_)) {
            charge(p_316556_, p_316165_, p_316402_, p_316665_);
            p_316531_.consume(1, p_316556_);
            return InteractionResult.SUCCESS;
        } else {
            return (InteractionResult)(p_316586_ == InteractionHand.MAIN_HAND
                    && isRespawnFuel(p_316556_.getItemInHand(InteractionHand.OFF_HAND))
                    && canBeCharged(p_316665_)
                ? InteractionResult.PASS
                : InteractionResult.TRY_WITH_EMPTY_HAND);
        }
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState p_316377_, Level p_316150_, BlockPos p_316161_, Player p_316889_, BlockHitResult p_316358_) {
        if (p_316377_.getValue(CHARGE) == 0) {
            return InteractionResult.PASS;
        } else if (p_316150_ instanceof ServerLevel serverlevel) {
            if (!canSetSpawn(serverlevel, p_316161_)) {
                this.explode(p_316377_, serverlevel, p_316161_);
                return InteractionResult.SUCCESS_SERVER;
            } else {
                if (p_316889_ instanceof ServerPlayer serverplayer) {
                    ServerPlayer.RespawnConfig serverplayer$respawnconfig = serverplayer.getRespawnConfig();
                    ServerPlayer.RespawnConfig serverplayer$respawnconfig1 = new ServerPlayer.RespawnConfig(
                        LevelData.RespawnData.of(serverlevel.dimension(), p_316161_, 0.0F, 0.0F), false
                    );
                    if (serverplayer$respawnconfig == null || !serverplayer$respawnconfig.isSamePosition(serverplayer$respawnconfig1)) {
                        serverplayer.setRespawnPosition(serverplayer$respawnconfig1, true);
                        serverlevel.playSound(
                            null,
                            p_316161_.getX() + 0.5,
                            p_316161_.getY() + 0.5,
                            p_316161_.getZ() + 0.5,
                            SoundEvents.RESPAWN_ANCHOR_SET_SPAWN,
                            SoundSource.BLOCKS,
                            1.0F,
                            1.0F
                        );
                        return InteractionResult.SUCCESS_SERVER;
                    }
                }

                return InteractionResult.CONSUME;
            }
        } else {
            return InteractionResult.CONSUME;
        }
    }

    private static boolean isRespawnFuel(ItemStack stack) {
        return stack.is(Items.GLOWSTONE);
    }

    private static boolean canBeCharged(BlockState state) {
        return state.getValue(CHARGE) < 4;
    }

    private static boolean isWaterThatWouldFlow(BlockPos pos, Level level) {
        FluidState fluidstate = level.getFluidState(pos);
        if (!fluidstate.is(FluidTags.WATER)) {
            return false;
        } else if (fluidstate.isSource()) {
            return true;
        } else {
            float f = fluidstate.getAmount();
            if (f < 2.0F) {
                return false;
            } else {
                FluidState fluidstate1 = level.getFluidState(pos.below());
                return !fluidstate1.is(FluidTags.WATER);
            }
        }
    }

    private void explode(BlockState state, ServerLevel level, final BlockPos pos) {
        level.removeBlock(pos, false);
        boolean flag = Direction.Plane.HORIZONTAL.stream().map(pos::relative).anyMatch(p_55854_ -> isWaterThatWouldFlow(p_55854_, level));
        final boolean flag1 = flag || level.getFluidState(pos.above()).is(FluidTags.WATER);
        ExplosionDamageCalculator explosiondamagecalculator = new ExplosionDamageCalculator() {
            @Override
            public Optional<Float> getBlockExplosionResistance(
                Explosion p_55904_, BlockGetter p_55905_, BlockPos p_55906_, BlockState p_55907_, FluidState p_55908_
            ) {
                return p_55906_.equals(pos) && flag1
                    ? Optional.of(Blocks.WATER.getExplosionResistance())
                    : super.getBlockExplosionResistance(p_55904_, p_55905_, p_55906_, p_55907_, p_55908_);
            }
        };
        Vec3 vec3 = pos.getCenter();
        level.explode(
            null, level.damageSources().badRespawnPointExplosion(vec3), explosiondamagecalculator, vec3, 5.0F, true, Level.ExplosionInteraction.BLOCK
        );
    }

    public static boolean canSetSpawn(ServerLevel level, BlockPos pos) {
        return level.environmentAttributes().getValue(EnvironmentAttributes.RESPAWN_ANCHOR_WORKS, pos);
    }

    public static void charge(@Nullable Entity entity, Level level, BlockPos pos, BlockState state) {
        BlockState blockstate = state.setValue(CHARGE, state.getValue(CHARGE) + 1);
        level.setBlock(pos, blockstate, 3);
        level.gameEvent(GameEvent.BLOCK_CHANGE, pos, GameEvent.Context.of(entity, blockstate));
        level.playSound(
            null, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, SoundEvents.RESPAWN_ANCHOR_CHARGE, SoundSource.BLOCKS, 1.0F, 1.0F
        );
    }

    @Override
    public void animateTick(BlockState p_221969_, Level p_221970_, BlockPos p_221971_, RandomSource p_221972_) {
        if (p_221969_.getValue(CHARGE) != 0) {
            if (p_221972_.nextInt(100) == 0) {
                p_221970_.playLocalSound(p_221971_, SoundEvents.RESPAWN_ANCHOR_AMBIENT, SoundSource.BLOCKS, 1.0F, 1.0F, false);
            }

            double d0 = p_221971_.getX() + 0.5 + (0.5 - p_221972_.nextDouble());
            double d1 = p_221971_.getY() + 1.0;
            double d2 = p_221971_.getZ() + 0.5 + (0.5 - p_221972_.nextDouble());
            double d3 = p_221972_.nextFloat() * 0.04;
            p_221970_.addParticle(ParticleTypes.REVERSE_PORTAL, d0, d1, d2, 0.0, d3, 0.0);
        }
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(CHARGE);
    }

    @Override
    protected boolean hasAnalogOutputSignal(BlockState state) {
        return true;
    }

    public static int getScaledChargeLevel(BlockState state, int scale) {
        return Mth.floor((state.getValue(CHARGE) - 0) / 4.0F * scale);
    }

    @Override
    protected int getAnalogOutputSignal(BlockState p_55870_, Level p_55871_, BlockPos p_55872_, Direction p_433202_) {
        return getScaledChargeLevel(p_55870_, 15);
    }

    public static Optional<Vec3> findStandUpPosition(EntityType<?> entityType, CollisionGetter level, BlockPos pos) {
        Optional<Vec3> optional = findStandUpPosition(entityType, level, pos, true);
        return optional.isPresent() ? optional : findStandUpPosition(entityType, level, pos, false);
    }

    private static Optional<Vec3> findStandUpPosition(EntityType<?> entityType, CollisionGetter level, BlockPos pos, boolean simulate) {
        BlockPos.MutableBlockPos blockpos$mutableblockpos = new BlockPos.MutableBlockPos();

        for (Vec3i vec3i : RESPAWN_OFFSETS) {
            blockpos$mutableblockpos.set(pos).move(vec3i);
            Vec3 vec3 = DismountHelper.findSafeDismountLocation(entityType, level, blockpos$mutableblockpos, simulate);
            if (vec3 != null) {
                return Optional.of(vec3);
            }
        }

        return Optional.empty();
    }

    @Override
    protected boolean isPathfindable(BlockState p_55865_, PathComputationType p_55868_) {
        return false;
    }
}
