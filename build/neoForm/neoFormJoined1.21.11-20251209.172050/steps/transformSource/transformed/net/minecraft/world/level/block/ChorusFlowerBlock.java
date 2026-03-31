package net.minecraft.world.level.block;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.mojang.serialization.codecs.RecordCodecBuilder.Instance;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jspecify.annotations.Nullable;

public class ChorusFlowerBlock extends Block {
    public static final MapCodec<ChorusFlowerBlock> CODEC = RecordCodecBuilder.mapCodec(
        p_432653_ -> p_432653_.group(BuiltInRegistries.BLOCK.byNameCodec().fieldOf("plant").forGetter(p_304498_ -> p_304498_.plant), propertiesCodec())
            .apply(p_432653_, ChorusFlowerBlock::new)
    );
    public static final int DEAD_AGE = 5;
    public static final IntegerProperty AGE = BlockStateProperties.AGE_5;
    private static final VoxelShape SHAPE_BLOCK_SUPPORT = Block.column(14.0, 0.0, 15.0);
    private final Block plant;

    @Override
    public MapCodec<ChorusFlowerBlock> codec() {
        return CODEC;
    }

    public ChorusFlowerBlock(Block plant, BlockBehaviour.Properties properties) {
        super(properties);
        this.plant = plant;
        this.registerDefaultState(this.stateDefinition.any().setValue(AGE, 0));
    }

    @Override
    protected void tick(BlockState p_220975_, ServerLevel p_220976_, BlockPos p_220977_, RandomSource p_220978_) {
        if (!p_220975_.canSurvive(p_220976_, p_220977_)) {
            p_220976_.destroyBlock(p_220977_, true);
        }
    }

    /**
     * Returns whether this block is of a type that needs random ticking. Called for ref-counting purposes by {@code ExtendedBlockStorage} in order to broadly cull a chunk from the random chunk update list for efficiency's sake.
     */
    @Override
    protected boolean isRandomlyTicking(BlockState state) {
        return state.getValue(AGE) < 5;
    }

    @Override
    public VoxelShape getBlockSupportShape(BlockState p_294099_, BlockGetter p_294531_, BlockPos p_295431_) {
        return SHAPE_BLOCK_SUPPORT;
    }

    @Override
    protected void randomTick(BlockState p_220980_, ServerLevel p_220981_, BlockPos p_220982_, RandomSource p_220983_) {
        BlockPos blockpos = p_220982_.above();
        if (p_220981_.isEmptyBlock(blockpos) && blockpos.getY() <= p_220981_.getMaxY()) {
            int i = p_220980_.getValue(AGE);
            if (i < 5 && net.neoforged.neoforge.common.CommonHooks.canCropGrow(p_220981_, blockpos, p_220980_, true)) {
                boolean flag = false;
                boolean flag1 = false;
                BlockState blockstate = p_220981_.getBlockState(p_220982_.below());
                var soilDecision = blockstate.canSustainPlant(p_220981_, p_220982_.below(), Direction.UP, p_220980_);
                if (!soilDecision.isDefault()) flag = soilDecision.isTrue();
                else
                if (blockstate.is(Blocks.END_STONE)) {
                    flag = true;
                } else if (blockstate.is(this.plant)) {
                    int j = 1;

                    for (int k = 0; k < 4; k++) {
                        BlockState blockstate1 = p_220981_.getBlockState(p_220982_.below(j + 1));
                        if (!blockstate1.is(this.plant)) {
                            var soilDecision2 = blockstate1.canSustainPlant(p_220981_, p_220982_.below(j + 1), Direction.UP, p_220980_);
                            if (!soilDecision2.isDefault()) flag1 = soilDecision2.isTrue();
                            if (blockstate1.is(Blocks.END_STONE)) {
                                flag1 = true;
                            }
                            break;
                        }

                        j++;
                    }

                    if (j < 2 || j <= p_220983_.nextInt(flag1 ? 5 : 4)) {
                        flag = true;
                    }
                } else if (blockstate.isAir()) {
                    flag = true;
                }

                if (flag && allNeighborsEmpty(p_220981_, blockpos, null) && p_220981_.isEmptyBlock(p_220982_.above(2))) {
                    p_220981_.setBlock(p_220982_, ChorusPlantBlock.getStateWithConnections(p_220981_, p_220982_, this.plant.defaultBlockState()), 2);
                    this.placeGrownFlower(p_220981_, blockpos, i);
                } else if (i < 4) {
                    int l = p_220983_.nextInt(4);
                    if (flag1) {
                        l++;
                    }

                    boolean flag2 = false;

                    for (int i1 = 0; i1 < l; i1++) {
                        Direction direction = Direction.Plane.HORIZONTAL.getRandomDirection(p_220983_);
                        BlockPos blockpos1 = p_220982_.relative(direction);
                        if (p_220981_.isEmptyBlock(blockpos1)
                            && p_220981_.isEmptyBlock(blockpos1.below())
                            && allNeighborsEmpty(p_220981_, blockpos1, direction.getOpposite())) {
                            this.placeGrownFlower(p_220981_, blockpos1, i + 1);
                            flag2 = true;
                        }
                    }

                    if (flag2) {
                        p_220981_.setBlock(p_220982_, ChorusPlantBlock.getStateWithConnections(p_220981_, p_220982_, this.plant.defaultBlockState()), 2);
                    } else {
                        this.placeDeadFlower(p_220981_, p_220982_);
                    }
                } else {
                    this.placeDeadFlower(p_220981_, p_220982_);
                }
                net.neoforged.neoforge.common.CommonHooks.fireCropGrowPost(p_220981_, p_220982_, p_220980_);
            }
        }
    }

    private void placeGrownFlower(Level level, BlockPos pos, int age) {
        level.setBlock(pos, this.defaultBlockState().setValue(AGE, age), 2);
        level.levelEvent(1033, pos, 0);
    }

    private void placeDeadFlower(Level level, BlockPos pos) {
        level.setBlock(pos, this.defaultBlockState().setValue(AGE, 5), 2);
        level.levelEvent(1034, pos, 0);
    }

    private static boolean allNeighborsEmpty(LevelReader level, BlockPos pos, @Nullable Direction excludingSide) {
        for (Direction direction : Direction.Plane.HORIZONTAL) {
            if (direction != excludingSide && !level.isEmptyBlock(pos.relative(direction))) {
                return false;
            }
        }

        return true;
    }

    @Override
    protected BlockState updateShape(
        BlockState p_51687_,
        LevelReader p_374269_,
        ScheduledTickAccess p_374493_,
        BlockPos p_51691_,
        Direction p_51688_,
        BlockPos p_51692_,
        BlockState p_51689_,
        RandomSource p_374130_
    ) {
        if (p_51688_ != Direction.UP && !p_51687_.canSurvive(p_374269_, p_51691_)) {
            p_374493_.scheduleTick(p_51691_, this, 1);
        }

        return super.updateShape(p_51687_, p_374269_, p_374493_, p_51691_, p_51688_, p_51692_, p_51689_, p_374130_);
    }

    @Override
    protected boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        BlockState blockstate = level.getBlockState(pos.below());
        var soilDecision = blockstate.canSustainPlant(level, pos.below(), Direction.UP, state);
        if (!soilDecision.isDefault()) return soilDecision.isTrue();
        if (!blockstate.is(this.plant) && !blockstate.is(Blocks.END_STONE)) {
            if (!blockstate.isAir()) {
                return false;
            } else {
                boolean flag = false;

                for (Direction direction : Direction.Plane.HORIZONTAL) {
                    BlockState blockstate1 = level.getBlockState(pos.relative(direction));
                    if (blockstate1.is(this.plant)) {
                        if (flag) {
                            return false;
                        }

                        flag = true;
                    } else if (!blockstate1.isAir()) {
                        return false;
                    }
                }

                return flag;
            }
        } else {
            return true;
        }
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(AGE);
    }

    public static void generatePlant(LevelAccessor level, BlockPos pos, RandomSource random, int maxHorizontalDistance) {
        level.setBlock(pos, ChorusPlantBlock.getStateWithConnections(level, pos, Blocks.CHORUS_PLANT.defaultBlockState()), 2);
        growTreeRecursive(level, pos, random, pos, maxHorizontalDistance, 0);
    }

    private static void growTreeRecursive(LevelAccessor level, BlockPos branchPos, RandomSource random, BlockPos originalBranchPos, int maxHorizontalDistance, int iterations) {
        Block block = Blocks.CHORUS_PLANT;
        int i = random.nextInt(4) + 1;
        if (iterations == 0) {
            i++;
        }

        for (int j = 0; j < i; j++) {
            BlockPos blockpos = branchPos.above(j + 1);
            if (!allNeighborsEmpty(level, blockpos, null)) {
                return;
            }

            level.setBlock(blockpos, ChorusPlantBlock.getStateWithConnections(level, blockpos, block.defaultBlockState()), 2);
            level.setBlock(blockpos.below(), ChorusPlantBlock.getStateWithConnections(level, blockpos.below(), block.defaultBlockState()), 2);
        }

        boolean flag = false;
        if (iterations < 4) {
            int l = random.nextInt(4);
            if (iterations == 0) {
                l++;
            }

            for (int k = 0; k < l; k++) {
                Direction direction = Direction.Plane.HORIZONTAL.getRandomDirection(random);
                BlockPos blockpos1 = branchPos.above(i).relative(direction);
                if (Math.abs(blockpos1.getX() - originalBranchPos.getX()) < maxHorizontalDistance
                    && Math.abs(blockpos1.getZ() - originalBranchPos.getZ()) < maxHorizontalDistance
                    && level.isEmptyBlock(blockpos1)
                    && level.isEmptyBlock(blockpos1.below())
                    && allNeighborsEmpty(level, blockpos1, direction.getOpposite())) {
                    flag = true;
                    level.setBlock(blockpos1, ChorusPlantBlock.getStateWithConnections(level, blockpos1, block.defaultBlockState()), 2);
                    level.setBlock(
                        blockpos1.relative(direction.getOpposite()),
                        ChorusPlantBlock.getStateWithConnections(level, blockpos1.relative(direction.getOpposite()), block.defaultBlockState()),
                        2
                    );
                    growTreeRecursive(level, blockpos1, random, originalBranchPos, maxHorizontalDistance, iterations + 1);
                }
            }
        }

        if (!flag) {
            level.setBlock(branchPos.above(i), Blocks.CHORUS_FLOWER.defaultBlockState().setValue(AGE, 5), 2);
        }
    }

    @Override
    protected void onProjectileHit(Level level, BlockState state, BlockHitResult hit, Projectile projectile) {
        BlockPos blockpos = hit.getBlockPos();
        if (level instanceof ServerLevel serverlevel && projectile.mayInteract(serverlevel, blockpos) && projectile.mayBreak(serverlevel)) {
            level.destroyBlock(blockpos, true, projectile);
        }
    }
}
