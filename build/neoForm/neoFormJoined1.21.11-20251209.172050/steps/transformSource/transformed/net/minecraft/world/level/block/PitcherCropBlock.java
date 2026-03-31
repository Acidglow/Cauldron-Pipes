package net.minecraft.world.level.block;

import com.mojang.serialization.MapCodec;
import java.util.function.Function;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.InsideBlockEffectApplier;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Ravager;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.gamerules.GameRules;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jspecify.annotations.Nullable;

public class PitcherCropBlock extends DoublePlantBlock implements BonemealableBlock {
    public static final MapCodec<PitcherCropBlock> CODEC = simpleCodec(PitcherCropBlock::new);
    public static final int MAX_AGE = 4;
    public static final IntegerProperty AGE = BlockStateProperties.AGE_4;
    public static final EnumProperty<DoubleBlockHalf> HALF = DoublePlantBlock.HALF;
    private static final int DOUBLE_PLANT_AGE_INTERSECTION = 3;
    private static final int BONEMEAL_INCREASE = 1;
    private static final VoxelShape SHAPE_BULB = Block.column(6.0, -1.0, 3.0);
    private static final VoxelShape SHAPE_CROP = Block.column(10.0, -1.0, 5.0);
    private final Function<BlockState, VoxelShape> shapes = this.makeShapes();

    @Override
    public MapCodec<PitcherCropBlock> codec() {
        return CODEC;
    }

    public PitcherCropBlock(BlockBehaviour.Properties p_277780_) {
        super(p_277780_);
    }

    private Function<BlockState, VoxelShape> makeShapes() {
        int[] aint = new int[]{0, 9, 11, 22, 26};
        return this.getShapeForEachState(p_394141_ -> {
            int i = (p_394141_.getValue(AGE) == 0 ? 4 : 6) + aint[p_394141_.getValue(AGE)];
            int j = p_394141_.getValue(AGE) == 0 ? 6 : 10;

            return switch ((DoubleBlockHalf)p_394141_.getValue(HALF)) {
                case LOWER -> Block.column(j, -1.0, Math.min(16, -1 + i));
                case UPPER -> Block.column(j, 0.0, Math.max(0, -1 + i - 16));
            };
        });
    }

    @Override
    public @Nullable BlockState getStateForPlacement(BlockPlaceContext p_277448_) {
        return this.defaultBlockState();
    }

    @Override
    public VoxelShape getShape(BlockState p_277602_, BlockGetter p_277617_, BlockPos p_278005_, CollisionContext p_277514_) {
        return this.shapes.apply(p_277602_);
    }

    @Override
    public VoxelShape getCollisionShape(BlockState p_277609_, BlockGetter p_277398_, BlockPos p_278042_, CollisionContext p_277995_) {
        if (p_277609_.getValue(HALF) == DoubleBlockHalf.LOWER) {
            return p_277609_.getValue(AGE) == 0 ? SHAPE_BULB : SHAPE_CROP;
        } else {
            return Shapes.empty();
        }
    }

    @Override
    public BlockState updateShape(
        BlockState p_277518_,
        LevelReader p_374059_,
        ScheduledTickAccess p_374076_,
        BlockPos p_277982_,
        Direction p_277700_,
        BlockPos p_278106_,
        BlockState p_277660_,
        RandomSource p_374409_
    ) {
        if (isDouble(p_277518_.getValue(AGE))) {
            return super.updateShape(p_277518_, p_374059_, p_374076_, p_277982_, p_277700_, p_278106_, p_277660_, p_374409_);
        } else {
            return p_277518_.canSurvive(p_374059_, p_277982_) ? p_277518_ : Blocks.AIR.defaultBlockState();
        }
    }

    @Override
    public boolean canSurvive(BlockState p_277671_, LevelReader p_277477_, BlockPos p_278085_) {
        var soilDecision = p_277477_.getBlockState(p_278085_.below()).canSustainPlant(p_277477_, p_278085_.below(), Direction.UP, p_277671_);
        return isLower(p_277671_) && !sufficientLight(p_277477_, p_278085_) ? soilDecision.isTrue() : super.canSurvive(p_277671_, p_277477_, p_278085_);
    }

    @Override
    protected boolean mayPlaceOn(BlockState p_277418_, BlockGetter p_277461_, BlockPos p_277608_) {
        return p_277418_.getBlock() instanceof net.minecraft.world.level.block.FarmBlock;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> p_277573_) {
        p_277573_.add(AGE);
        super.createBlockStateDefinition(p_277573_);
    }

    @Override
    public void entityInside(BlockState p_279266_, Level p_279469_, BlockPos p_279119_, Entity p_279372_, InsideBlockEffectApplier p_404719_, boolean p_451761_) {
        if (p_279469_ instanceof ServerLevel serverlevel && p_279372_ instanceof Ravager && serverlevel.getGameRules().get(GameRules.MOB_GRIEFING)) {
            serverlevel.destroyBlock(p_279119_, true, p_279372_);
        }
    }

    @Override
    public boolean canBeReplaced(BlockState p_277627_, BlockPlaceContext p_277759_) {
        return false;
    }

    @Override
    public void setPlacedBy(Level p_277432_, BlockPos p_277632_, BlockState p_277479_, @Nullable LivingEntity p_277805_, ItemStack p_277663_) {
    }

    @Override
    public boolean isRandomlyTicking(BlockState p_277483_) {
        return p_277483_.getValue(HALF) == DoubleBlockHalf.LOWER && !this.isMaxAge(p_277483_);
    }

    @Override
    public void randomTick(BlockState p_277950_, ServerLevel p_277589_, BlockPos p_277937_, RandomSource p_277887_) {
        float f = CropBlock.getGrowthSpeed(p_277950_, p_277589_, p_277937_);
        boolean flag = p_277887_.nextInt((int)(25.0F / f) + 1) == 0;
        if (flag) {
            this.grow(p_277589_, p_277950_, p_277937_, 1);
        }
    }

    private void grow(ServerLevel level, BlockState state, BlockPos pos, int ageIncrement) {
        int i = Math.min(state.getValue(AGE) + ageIncrement, 4);
        if (this.canGrow(level, pos, state, i)) {
            BlockState blockstate = state.setValue(AGE, i);
            level.setBlock(pos, blockstate, 2);
            if (isDouble(i)) {
                level.setBlock(pos.above(), blockstate.setValue(HALF, DoubleBlockHalf.UPPER), 3);
            }
        }
    }

    private static boolean canGrowInto(LevelReader level, BlockPos pos) {
        BlockState blockstate = level.getBlockState(pos);
        return blockstate.isAir() || blockstate.is(Blocks.PITCHER_CROP);
    }

    private static boolean sufficientLight(LevelReader level, BlockPos pos) {
        return CropBlock.hasSufficientLight(level, pos);
    }

    private static boolean isLower(BlockState state) {
        return state.is(Blocks.PITCHER_CROP) && state.getValue(HALF) == DoubleBlockHalf.LOWER;
    }

    private static boolean isDouble(int age) {
        return age >= 3;
    }

    private boolean canGrow(LevelReader reader, BlockPos pos, BlockState state, int age) {
        return !this.isMaxAge(state) && sufficientLight(reader, pos) && (!isDouble(age) || canGrowInto(reader, pos.above()));
    }

    private boolean isMaxAge(BlockState state) {
        return state.getValue(AGE) >= 4;
    }

    private PitcherCropBlock.@Nullable PosAndState getLowerHalf(LevelReader level, BlockPos pos, BlockState state) {
        if (isLower(state)) {
            return new PitcherCropBlock.PosAndState(pos, state);
        } else {
            BlockPos blockpos = pos.below();
            BlockState blockstate = level.getBlockState(blockpos);
            return isLower(blockstate) ? new PitcherCropBlock.PosAndState(blockpos, blockstate) : null;
        }
    }

    @Override
    public boolean isValidBonemealTarget(LevelReader p_277380_, BlockPos p_277500_, BlockState p_277715_) {
        PitcherCropBlock.PosAndState pitchercropblock$posandstate = this.getLowerHalf(p_277380_, p_277500_, p_277715_);
        return pitchercropblock$posandstate == null
            ? false
            : this.canGrow(
                p_277380_, pitchercropblock$posandstate.pos, pitchercropblock$posandstate.state, pitchercropblock$posandstate.state.getValue(AGE) + 1
            );
    }

    @Override
    public boolean isBonemealSuccess(Level p_277920_, RandomSource p_277594_, BlockPos p_277401_, BlockState p_277434_) {
        return true;
    }

    @Override
    public void performBonemeal(ServerLevel p_277717_, RandomSource p_277870_, BlockPos p_277836_, BlockState p_278034_) {
        PitcherCropBlock.PosAndState pitchercropblock$posandstate = this.getLowerHalf(p_277717_, p_277836_, p_278034_);
        if (pitchercropblock$posandstate != null) {
            this.grow(p_277717_, pitchercropblock$posandstate.state, pitchercropblock$posandstate.pos, 1);
        }
    }

    record PosAndState(BlockPos pos, BlockState state) {
    }
}
