package net.minecraft.world.level.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.InsideBlockEffectApplier;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.storage.loot.BuiltInLootTables;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

public class SweetBerryBushBlock extends VegetationBlock implements BonemealableBlock {
    public static final MapCodec<SweetBerryBushBlock> CODEC = simpleCodec(SweetBerryBushBlock::new);
    private static final float HURT_SPEED_THRESHOLD = 0.003F;
    public static final int MAX_AGE = 3;
    public static final IntegerProperty AGE = BlockStateProperties.AGE_3;
    private static final VoxelShape SHAPE_SAPLING = Block.column(10.0, 0.0, 8.0);
    private static final VoxelShape SHAPE_GROWING = Block.column(14.0, 0.0, 16.0);

    @Override
    public MapCodec<SweetBerryBushBlock> codec() {
        return CODEC;
    }

    public SweetBerryBushBlock(BlockBehaviour.Properties p_57249_) {
        super(p_57249_);
        this.registerDefaultState(this.stateDefinition.any().setValue(AGE, 0));
    }

    @Override
    protected ItemStack getCloneItemStack(LevelReader p_304655_, BlockPos p_57257_, BlockState p_57258_, boolean p_388022_) {
        return new ItemStack(Items.SWEET_BERRIES);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return switch (state.getValue(AGE)) {
            case 0 -> SHAPE_SAPLING;
            case 3 -> Shapes.block();
            default -> SHAPE_GROWING;
        };
    }

    @Override
    protected boolean isRandomlyTicking(BlockState state) {
        return state.getValue(AGE) < 3;
    }

    @Override
    protected void randomTick(BlockState p_222563_, ServerLevel p_222564_, BlockPos p_222565_, RandomSource p_222566_) {
        int i = p_222563_.getValue(AGE);
        if (i < 3 && p_222564_.getRawBrightness(p_222565_.above(), 0) >= 9 && net.neoforged.neoforge.common.CommonHooks.canCropGrow(p_222564_, p_222565_, p_222563_, p_222566_.nextInt(5) == 0)) {
            BlockState blockstate = p_222563_.setValue(AGE, i + 1);
            p_222564_.setBlock(p_222565_, blockstate, 2);
            net.neoforged.neoforge.common.CommonHooks.fireCropGrowPost(p_222564_, p_222565_, p_222563_);
            p_222564_.gameEvent(GameEvent.BLOCK_CHANGE, p_222565_, GameEvent.Context.of(blockstate));
        }
    }

    @Override
    protected void entityInside(BlockState p_57270_, Level p_57271_, BlockPos p_57272_, Entity p_57273_, InsideBlockEffectApplier p_405414_, boolean p_451767_) {
        if (p_57273_ instanceof LivingEntity && p_57273_.getType() != EntityType.FOX && p_57273_.getType() != EntityType.BEE) {
            p_57273_.makeStuckInBlock(p_57270_, new Vec3(0.8F, 0.75, 0.8F));
            if (p_57271_ instanceof ServerLevel serverlevel && p_57270_.getValue(AGE) != 0) {
                Vec3 vec3 = p_57273_.isClientAuthoritative() ? p_57273_.getKnownMovement() : p_57273_.oldPosition().subtract(p_57273_.position());
                if (vec3.horizontalDistanceSqr() > 0.0) {
                    double d0 = Math.abs(vec3.x());
                    double d1 = Math.abs(vec3.z());
                    if (d0 >= 0.003F || d1 >= 0.003F) {
                        p_57273_.hurtServer(serverlevel, p_57271_.damageSources().sweetBerryBush(), 1.0F);
                    }
                }
            }
        }
    }

    @Override
    protected InteractionResult useItemOn(
        ItemStack p_316636_, BlockState p_316295_, Level p_316812_, BlockPos p_316380_, Player p_316731_, InteractionHand p_316188_, BlockHitResult p_316626_
    ) {
        int i = p_316295_.getValue(AGE);
        boolean flag = i == 3;
        return (InteractionResult)(!flag && p_316636_.is(Items.BONE_MEAL)
            ? InteractionResult.PASS
            : super.useItemOn(p_316636_, p_316295_, p_316812_, p_316380_, p_316731_, p_316188_, p_316626_));
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState p_316134_, Level p_316429_, BlockPos p_316748_, Player p_316431_, BlockHitResult p_316474_) {
        if (p_316134_.getValue(AGE) > 1) {
            if (p_316429_ instanceof ServerLevel serverlevel) {
                Block.dropFromBlockInteractLootTable(
                    serverlevel,
                    BuiltInLootTables.HARVEST_SWEET_BERRY_BUSH,
                    p_316134_,
                    p_316429_.getBlockEntity(p_316748_),
                    null,
                    p_316431_,
                    (p_436765_, p_436687_) -> Block.popResource(p_436765_, p_316748_, p_436687_)
                );
                serverlevel.playSound(
                    null, p_316748_, SoundEvents.SWEET_BERRY_BUSH_PICK_BERRIES, SoundSource.BLOCKS, 1.0F, 0.8F + serverlevel.random.nextFloat() * 0.4F
                );
                BlockState blockstate = p_316134_.setValue(AGE, 1);
                serverlevel.setBlock(p_316748_, blockstate, 2);
                serverlevel.gameEvent(GameEvent.BLOCK_CHANGE, p_316748_, GameEvent.Context.of(p_316431_, blockstate));
            }

            return InteractionResult.SUCCESS;
        } else {
            return super.useWithoutItem(p_316134_, p_316429_, p_316748_, p_316431_, p_316474_);
        }
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(AGE);
    }

    @Override
    public boolean isValidBonemealTarget(LevelReader p_256056_, BlockPos p_57261_, BlockState p_57262_) {
        return p_57262_.getValue(AGE) < 3;
    }

    @Override
    public boolean isBonemealSuccess(Level p_222558_, RandomSource p_222559_, BlockPos p_222560_, BlockState p_222561_) {
        return true;
    }

    @Override
    public void performBonemeal(ServerLevel p_222553_, RandomSource p_222554_, BlockPos p_222555_, BlockState p_222556_) {
        int i = Math.min(3, p_222556_.getValue(AGE) + 1);
        p_222553_.setBlock(p_222555_, p_222556_.setValue(AGE, i), 2);
    }
}
