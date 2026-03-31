package net.minecraft.world.level.block;

import com.mojang.serialization.MapCodec;
import java.util.List;
import java.util.function.BiConsumer;
import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.EnchantmentTags;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.util.Util;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.animal.bee.Bee;
import net.minecraft.world.entity.boss.wither.WitherBoss;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.item.PrimedTnt;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.hurtingprojectile.WitherSkull;
import net.minecraft.world.entity.vehicle.minecart.MinecartTNT;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.BlockItemStateProperties;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.entity.BeehiveBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.gamerules.GameRules;
import net.minecraft.world.level.storage.loot.BuiltInLootTables;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jspecify.annotations.Nullable;

public class BeehiveBlock extends BaseEntityBlock {
    public static final MapCodec<BeehiveBlock> CODEC = simpleCodec(BeehiveBlock::new);
    public static final EnumProperty<Direction> FACING = HorizontalDirectionalBlock.FACING;
    public static final IntegerProperty HONEY_LEVEL = BlockStateProperties.LEVEL_HONEY;
    public static final int MAX_HONEY_LEVELS = 5;

    @Override
    public MapCodec<BeehiveBlock> codec() {
        return CODEC;
    }

    public BeehiveBlock(BlockBehaviour.Properties p_49568_) {
        super(p_49568_);
        this.registerDefaultState(this.stateDefinition.any().setValue(HONEY_LEVEL, 0).setValue(FACING, Direction.NORTH));
    }

    @Override
    protected boolean hasAnalogOutputSignal(BlockState state) {
        return true;
    }

    @Override
    protected int getAnalogOutputSignal(BlockState p_49620_, Level p_49621_, BlockPos p_49622_, Direction p_435261_) {
        return p_49620_.getValue(HONEY_LEVEL);
    }

    @Override
    public void playerDestroy(Level level, Player player, BlockPos pos, BlockState state, @Nullable BlockEntity te, ItemStack stack) {
        super.playerDestroy(level, player, pos, state, te, stack);
        if (!level.isClientSide() && te instanceof BeehiveBlockEntity beehiveblockentity) {
            if (!EnchantmentHelper.hasTag(stack, EnchantmentTags.PREVENTS_BEE_SPAWNS_WHEN_MINING)) {
                beehiveblockentity.emptyAllLivingFromHive(player, state, BeehiveBlockEntity.BeeReleaseStatus.EMERGENCY);
                Containers.updateNeighboursAfterDestroy(state, level, pos);
                this.angerNearbyBees(level, pos);
            }

            CriteriaTriggers.BEE_NEST_DESTROYED.trigger((ServerPlayer)player, state, stack, beehiveblockentity.getOccupantCount());
        }
    }

    @Override
    protected void onExplosionHit(
        BlockState p_364770_, ServerLevel p_364089_, BlockPos p_363677_, Explosion p_365390_, BiConsumer<ItemStack, BlockPos> p_360830_
    ) {
        super.onExplosionHit(p_364770_, p_364089_, p_363677_, p_365390_, p_360830_);
        this.angerNearbyBees(p_364089_, p_363677_);
    }

    private void angerNearbyBees(Level level, BlockPos pos) {
        AABB aabb = new AABB(pos).inflate(8.0, 6.0, 8.0);
        List<Bee> list = level.getEntitiesOfClass(Bee.class, aabb);
        if (!list.isEmpty()) {
            List<Player> list1 = level.getEntitiesOfClass(Player.class, aabb);
            if (list1.isEmpty()) {
                return;
            }

            for (Bee bee : list) {
                if (bee.getTarget() == null) {
                    Player player = Util.getRandom(list1, level.random);
                    bee.setTarget(player);
                }
            }
        }
    }

    public static void dropHoneycomb(
        ServerLevel level, ItemStack stack, BlockState state, @Nullable BlockEntity blockEntity, @Nullable Entity entity, BlockPos pos
    ) {
        dropFromBlockInteractLootTable(
            level,
            BuiltInLootTables.HARVEST_BEEHIVE,
            state,
            blockEntity,
            stack,
            entity,
            (p_434111_, p_434054_) -> popResource(p_434111_, pos, p_434054_)
        );
    }

    @Override
    protected InteractionResult useItemOn(
        ItemStack p_316844_, BlockState p_316365_, Level p_316306_, BlockPos p_316497_, Player p_316824_, InteractionHand p_316436_, BlockHitResult p_316125_
    ) {
        int i = p_316365_.getValue(HONEY_LEVEL);
        boolean flag = false;
        if (i >= 5) {
            Item item = p_316844_.getItem();
            if (p_316306_ instanceof ServerLevel serverlevel && p_316844_.canPerformAction(net.neoforged.neoforge.common.ItemAbilities.SHEARS_HARVEST)) {
                dropHoneycomb(serverlevel, p_316844_, p_316365_, p_316306_.getBlockEntity(p_316497_), p_316824_, p_316497_);
                p_316306_.playSound(null, p_316824_.getX(), p_316824_.getY(), p_316824_.getZ(), SoundEvents.BEEHIVE_SHEAR, SoundSource.BLOCKS, 1.0F, 1.0F);
                p_316844_.hurtAndBreak(1, p_316824_, p_316436_.asEquipmentSlot());
                flag = true;
                p_316306_.gameEvent(p_316824_, GameEvent.SHEAR, p_316497_);
            } else if (p_316844_.is(Items.GLASS_BOTTLE)) {
                p_316844_.shrink(1);
                p_316306_.playSound(p_316824_, p_316824_.getX(), p_316824_.getY(), p_316824_.getZ(), SoundEvents.BOTTLE_FILL, SoundSource.BLOCKS, 1.0F, 1.0F);
                if (p_316844_.isEmpty()) {
                    p_316824_.setItemInHand(p_316436_, new ItemStack(Items.HONEY_BOTTLE));
                } else if (!p_316824_.getInventory().add(new ItemStack(Items.HONEY_BOTTLE))) {
                    p_316824_.drop(new ItemStack(Items.HONEY_BOTTLE), false);
                }

                flag = true;
                p_316306_.gameEvent(p_316824_, GameEvent.FLUID_PICKUP, p_316497_);
            }

            if (!p_316306_.isClientSide() && flag) {
                p_316824_.awardStat(Stats.ITEM_USED.get(item));
            }
        }

        if (flag) {
            if (!CampfireBlock.isSmokeyPos(p_316306_, p_316497_)) {
                if (this.hiveContainsBees(p_316306_, p_316497_)) {
                    this.angerNearbyBees(p_316306_, p_316497_);
                }

                this.releaseBeesAndResetHoneyLevel(p_316306_, p_316365_, p_316497_, p_316824_, BeehiveBlockEntity.BeeReleaseStatus.EMERGENCY);
            } else {
                this.resetHoneyLevel(p_316306_, p_316365_, p_316497_);
            }

            return InteractionResult.SUCCESS;
        } else {
            return super.useItemOn(p_316844_, p_316365_, p_316306_, p_316497_, p_316824_, p_316436_, p_316125_);
        }
    }

    private boolean hiveContainsBees(Level level, BlockPos pos) {
        return level.getBlockEntity(pos) instanceof BeehiveBlockEntity beehiveblockentity ? !beehiveblockentity.isEmpty() : false;
    }

    public void releaseBeesAndResetHoneyLevel(
        Level level, BlockState state, BlockPos pos, @Nullable Player player, BeehiveBlockEntity.BeeReleaseStatus beeReleaseStatus
    ) {
        this.resetHoneyLevel(level, state, pos);
        if (level.getBlockEntity(pos) instanceof BeehiveBlockEntity beehiveblockentity) {
            beehiveblockentity.emptyAllLivingFromHive(player, state, beeReleaseStatus);
        }
    }

    public void resetHoneyLevel(Level level, BlockState state, BlockPos pos) {
        level.setBlock(pos, state.setValue(HONEY_LEVEL, 0), 3);
    }

    @Override
    public void animateTick(BlockState p_220773_, Level p_220774_, BlockPos p_220775_, RandomSource p_220776_) {
        if (p_220773_.getValue(HONEY_LEVEL) >= 5) {
            for (int i = 0; i < p_220776_.nextInt(1) + 1; i++) {
                this.trySpawnDripParticles(p_220774_, p_220775_, p_220773_);
            }
        }
    }

    private void trySpawnDripParticles(Level level, BlockPos pos, BlockState state) {
        if (state.getFluidState().isEmpty() && !(level.random.nextFloat() < 0.3F)) {
            VoxelShape voxelshape = state.getCollisionShape(level, pos);
            double d0 = voxelshape.max(Direction.Axis.Y);
            if (d0 >= 1.0 && !state.is(BlockTags.IMPERMEABLE)) {
                double d1 = voxelshape.min(Direction.Axis.Y);
                if (d1 > 0.0) {
                    this.spawnParticle(level, pos, voxelshape, pos.getY() + d1 - 0.05);
                } else {
                    BlockPos blockpos = pos.below();
                    BlockState blockstate = level.getBlockState(blockpos);
                    VoxelShape voxelshape1 = blockstate.getCollisionShape(level, blockpos);
                    double d2 = voxelshape1.max(Direction.Axis.Y);
                    if ((d2 < 1.0 || !blockstate.isCollisionShapeFullBlock(level, blockpos)) && blockstate.getFluidState().isEmpty()) {
                        this.spawnParticle(level, pos, voxelshape, pos.getY() - 0.05);
                    }
                }
            }
        }
    }

    private void spawnParticle(Level level, BlockPos pos, VoxelShape shape, double y) {
        this.spawnFluidParticle(
            level,
            pos.getX() + shape.min(Direction.Axis.X),
            pos.getX() + shape.max(Direction.Axis.X),
            pos.getZ() + shape.min(Direction.Axis.Z),
            pos.getZ() + shape.max(Direction.Axis.Z),
            y
        );
    }

    private void spawnFluidParticle(Level particleData, double x1, double x2, double z1, double z2, double y) {
        particleData.addParticle(
            ParticleTypes.DRIPPING_HONEY,
            Mth.lerp(particleData.random.nextDouble(), x1, x2),
            y,
            Mth.lerp(particleData.random.nextDouble(), z1, z2),
            0.0,
            0.0,
            0.0
        );
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return this.defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(HONEY_LEVEL, FACING);
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos p_152184_, BlockState p_152185_) {
        return new BeehiveBlockEntity(p_152184_, p_152185_);
    }

    @Override
    public <T extends BlockEntity> @Nullable BlockEntityTicker<T> getTicker(Level p_152180_, BlockState p_152181_, BlockEntityType<T> p_152182_) {
        return p_152180_.isClientSide() ? null : createTickerHelper(p_152182_, BlockEntityType.BEEHIVE, BeehiveBlockEntity::serverTick);
    }

    @Override
    public BlockState playerWillDestroy(Level p_49608_, BlockPos p_49609_, BlockState p_49610_, Player p_49611_) {
        if (p_49608_ instanceof ServerLevel serverlevel
            && p_49611_.preventsBlockDrops()
            && serverlevel.getGameRules().get(GameRules.BLOCK_DROPS)
            && p_49608_.getBlockEntity(p_49609_) instanceof BeehiveBlockEntity beehiveblockentity) {
            int i = p_49610_.getValue(HONEY_LEVEL);
            boolean flag = !beehiveblockentity.isEmpty();
            if (flag || i > 0) {
                ItemStack itemstack = new ItemStack(this);
                itemstack.applyComponents(beehiveblockentity.collectComponents());
                itemstack.set(DataComponents.BLOCK_STATE, BlockItemStateProperties.EMPTY.with(HONEY_LEVEL, i));
                ItemEntity itementity = new ItemEntity(p_49608_, p_49609_.getX(), p_49609_.getY(), p_49609_.getZ(), itemstack);
                itementity.setDefaultPickUpDelay();
                p_49608_.addFreshEntity(itementity);
            }
        }

        return super.playerWillDestroy(p_49608_, p_49609_, p_49610_, p_49611_);
    }

    @Override
    protected List<ItemStack> getDrops(BlockState p_49636_, LootParams.Builder p_287581_) {
        Entity entity = p_287581_.getOptionalParameter(LootContextParams.THIS_ENTITY);
        if (entity instanceof PrimedTnt
            || entity instanceof Creeper
            || entity instanceof WitherSkull
            || entity instanceof WitherBoss
            || entity instanceof MinecartTNT) {
            BlockEntity blockentity = p_287581_.getOptionalParameter(LootContextParams.BLOCK_ENTITY);
            if (blockentity instanceof BeehiveBlockEntity beehiveblockentity) {
                beehiveblockentity.emptyAllLivingFromHive(null, p_49636_, BeehiveBlockEntity.BeeReleaseStatus.EMERGENCY);
            }
        }

        return super.getDrops(p_49636_, p_287581_);
    }

    @Override
    protected ItemStack getCloneItemStack(LevelReader p_386459_, BlockPos p_387055_, BlockState p_387788_, boolean p_387391_) {
        ItemStack itemstack = super.getCloneItemStack(p_386459_, p_387055_, p_387788_, p_387391_);
        if (p_387391_) {
            itemstack.set(DataComponents.BLOCK_STATE, BlockItemStateProperties.EMPTY.with(HONEY_LEVEL, p_387788_.getValue(HONEY_LEVEL)));
        }

        return itemstack;
    }

    @Override
    protected BlockState updateShape(
        BlockState p_49639_,
        LevelReader p_374043_,
        ScheduledTickAccess p_374351_,
        BlockPos p_49643_,
        Direction p_49640_,
        BlockPos p_49644_,
        BlockState p_49641_,
        RandomSource p_374258_
    ) {
        if (p_374043_.getBlockState(p_49644_).getBlock() instanceof FireBlock
            && p_374043_.getBlockEntity(p_49643_) instanceof BeehiveBlockEntity beehiveblockentity) {
            beehiveblockentity.emptyAllLivingFromHive(null, p_49639_, BeehiveBlockEntity.BeeReleaseStatus.EMERGENCY);
        }

        return super.updateShape(p_49639_, p_374043_, p_374351_, p_49643_, p_49640_, p_49644_, p_49641_, p_374258_);
    }

    @Override
    public BlockState rotate(BlockState p_304785_, Rotation p_304624_) {
        return p_304785_.setValue(FACING, p_304624_.rotate(p_304785_.getValue(FACING)));
    }

    @Override
    public BlockState mirror(BlockState p_304677_, Mirror p_304660_) {
        return p_304677_.rotate(p_304660_.getRotation(p_304677_.getValue(FACING)));
    }
}
