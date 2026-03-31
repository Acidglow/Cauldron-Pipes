package net.minecraft.world.level.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.attribute.EnvironmentAttributes;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ambient.Bat;
import net.minecraft.world.entity.animal.turtle.Turtle;
import net.minecraft.world.entity.monster.zombie.Zombie;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.gamerules.GameRules;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jspecify.annotations.Nullable;

public class TurtleEggBlock extends Block {
    public static final MapCodec<TurtleEggBlock> CODEC = simpleCodec(TurtleEggBlock::new);
    public static final IntegerProperty HATCH = BlockStateProperties.HATCH;
    public static final IntegerProperty EGGS = BlockStateProperties.EGGS;
    public static final int MAX_HATCH_LEVEL = 2;
    public static final int MIN_EGGS = 1;
    public static final int MAX_EGGS = 4;
    private static final VoxelShape SHAPE_SINGLE = Block.box(3.0, 0.0, 3.0, 12.0, 7.0, 12.0);
    private static final VoxelShape SHAPE_MULTIPLE = Block.column(14.0, 0.0, 7.0);

    @Override
    public MapCodec<TurtleEggBlock> codec() {
        return CODEC;
    }

    public TurtleEggBlock(BlockBehaviour.Properties p_57759_) {
        super(p_57759_);
        this.registerDefaultState(this.stateDefinition.any().setValue(HATCH, 0).setValue(EGGS, 1));
    }

    @Override
    public void stepOn(Level p_154857_, BlockPos p_154858_, BlockState p_154859_, Entity p_154860_) {
        if (!p_154860_.isSteppingCarefully()) {
            this.destroyEgg(p_154857_, p_154859_, p_154858_, p_154860_, 100);
        }

        super.stepOn(p_154857_, p_154858_, p_154859_, p_154860_);
    }

    @Override
    public void fallOn(Level p_154845_, BlockState p_154846_, BlockPos p_154847_, Entity p_154848_, double p_397908_) {
        if (!(p_154848_ instanceof Zombie)) {
            this.destroyEgg(p_154845_, p_154846_, p_154847_, p_154848_, 3);
        }

        super.fallOn(p_154845_, p_154846_, p_154847_, p_154848_, p_397908_);
    }

    private void destroyEgg(Level level, BlockState state, BlockPos pos, Entity entity, int chance) {
        if (state.is(Blocks.TURTLE_EGG)
            && level instanceof ServerLevel serverlevel
            && this.canDestroyEgg(serverlevel, entity)
            && level.random.nextInt(chance) == 0) {
            this.decreaseEggs(serverlevel, pos, state);
        }
    }

    private void decreaseEggs(Level level, BlockPos pos, BlockState state) {
        level.playSound(null, pos, SoundEvents.TURTLE_EGG_BREAK, SoundSource.BLOCKS, 0.7F, 0.9F + level.random.nextFloat() * 0.2F);
        int i = state.getValue(EGGS);
        if (i <= 1) {
            level.destroyBlock(pos, false);
        } else {
            level.setBlock(pos, state.setValue(EGGS, i - 1), 2);
            level.gameEvent(GameEvent.BLOCK_DESTROY, pos, GameEvent.Context.of(state));
            level.levelEvent(2001, pos, Block.getId(state));
        }
    }

    @Override
    protected void randomTick(BlockState p_222644_, ServerLevel p_222645_, BlockPos p_222646_, RandomSource p_222647_) {
        if (this.shouldUpdateHatchLevel(p_222645_, p_222646_) && onSand(p_222645_, p_222646_)) {
            int i = p_222644_.getValue(HATCH);
            if (i < 2) {
                p_222645_.playSound(null, p_222646_, SoundEvents.TURTLE_EGG_CRACK, SoundSource.BLOCKS, 0.7F, 0.9F + p_222647_.nextFloat() * 0.2F);
                p_222645_.setBlock(p_222646_, p_222644_.setValue(HATCH, i + 1), 2);
                p_222645_.gameEvent(GameEvent.BLOCK_CHANGE, p_222646_, GameEvent.Context.of(p_222644_));
            } else {
                p_222645_.playSound(null, p_222646_, SoundEvents.TURTLE_EGG_HATCH, SoundSource.BLOCKS, 0.7F, 0.9F + p_222647_.nextFloat() * 0.2F);
                p_222645_.removeBlock(p_222646_, false);
                p_222645_.gameEvent(GameEvent.BLOCK_DESTROY, p_222646_, GameEvent.Context.of(p_222644_));

                for (int j = 0; j < p_222644_.getValue(EGGS); j++) {
                    p_222645_.levelEvent(2001, p_222646_, Block.getId(p_222644_));
                    Turtle turtle = EntityType.TURTLE.create(p_222645_, EntitySpawnReason.BREEDING);
                    if (turtle != null) {
                        turtle.setAge(-24000);
                        turtle.setHomePos(p_222646_);
                        turtle.snapTo(p_222646_.getX() + 0.3 + j * 0.2, p_222646_.getY(), p_222646_.getZ() + 0.3, 0.0F, 0.0F);
                        p_222645_.addFreshEntity(turtle);
                    }
                }
            }
        }
    }

    public static boolean onSand(BlockGetter level, BlockPos pos) {
        return isSand(level, pos.below());
    }

    public static boolean isSand(BlockGetter reader, BlockPos pos) {
        return reader.getBlockState(pos).is(BlockTags.SAND);
    }

    @Override
    protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean isMoving) {
        if (onSand(level, pos) && !level.isClientSide()) {
            level.levelEvent(2012, pos, 15);
        }
    }

    private boolean shouldUpdateHatchLevel(Level level, BlockPos pos) {
        float f = level.environmentAttributes().getValue(EnvironmentAttributes.TURTLE_EGG_HATCH_CHANCE, pos);
        return f > 0.0F && level.random.nextFloat() < f;
    }

    @Override
    public void playerDestroy(Level level, Player player, BlockPos pos, BlockState state, @Nullable BlockEntity te, ItemStack stack) {
        super.playerDestroy(level, player, pos, state, te, stack);
        this.decreaseEggs(level, pos, state);
    }

    @Override
    protected boolean canBeReplaced(BlockState state, BlockPlaceContext useContext) {
        return !useContext.isSecondaryUseActive() && useContext.getItemInHand().is(this.asItem()) && state.getValue(EGGS) < 4
            ? true
            : super.canBeReplaced(state, useContext);
    }

    @Override
    public @Nullable BlockState getStateForPlacement(BlockPlaceContext context) {
        BlockState blockstate = context.getLevel().getBlockState(context.getClickedPos());
        return blockstate.is(this) ? blockstate.setValue(EGGS, Math.min(4, blockstate.getValue(EGGS) + 1)) : super.getStateForPlacement(context);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return state.getValue(EGGS) == 1 ? SHAPE_SINGLE : SHAPE_MULTIPLE;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(HATCH, EGGS);
    }

    private boolean canDestroyEgg(ServerLevel level, Entity entity) {
        if (entity instanceof Turtle || entity instanceof Bat) {
            return false;
        } else {
            return !(entity instanceof LivingEntity) ? false : entity instanceof Player || net.neoforged.neoforge.event.EventHooks.canEntityGrief(level, entity);
        }
    }
}
