package net.minecraft.world.level.levelgen.structure.structures;

import com.google.common.collect.Lists;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.BiomeTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.vehicle.minecart.MinecartChest;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.FallingBlock;
import net.minecraft.world.level.block.FenceBlock;
import net.minecraft.world.level.block.RailBlock;
import net.minecraft.world.level.block.WallTorchBlock;
import net.minecraft.world.level.block.entity.SpawnerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.RailShape;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.StructurePieceAccessor;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceType;
import net.minecraft.world.level.storage.loot.BuiltInLootTables;
import net.minecraft.world.level.storage.loot.LootTable;
import org.jspecify.annotations.Nullable;

public class MineshaftPieces {
    private static final int DEFAULT_SHAFT_WIDTH = 3;
    private static final int DEFAULT_SHAFT_HEIGHT = 3;
    private static final int DEFAULT_SHAFT_LENGTH = 5;
    private static final int MAX_PILLAR_HEIGHT = 20;
    private static final int MAX_CHAIN_HEIGHT = 50;
    private static final int MAX_DEPTH = 8;
    public static final int MAGIC_START_Y = 50;

    private static MineshaftPieces.@Nullable MineShaftPiece createRandomShaftPiece(
        StructurePieceAccessor pieces,
        RandomSource random,
        int x,
        int y,
        int z,
        Direction orientation,
        int genDepth,
        MineshaftStructure.Type type
    ) {
        int i = random.nextInt(100);
        if (i >= 80) {
            BoundingBox boundingbox = MineshaftPieces.MineShaftCrossing.findCrossing(pieces, random, x, y, z, orientation);
            if (boundingbox != null) {
                return new MineshaftPieces.MineShaftCrossing(genDepth, boundingbox, orientation, type);
            }
        } else if (i >= 70) {
            BoundingBox boundingbox1 = MineshaftPieces.MineShaftStairs.findStairs(pieces, random, x, y, z, orientation);
            if (boundingbox1 != null) {
                return new MineshaftPieces.MineShaftStairs(genDepth, boundingbox1, orientation, type);
            }
        } else {
            BoundingBox boundingbox2 = MineshaftPieces.MineShaftCorridor.findCorridorSize(pieces, random, x, y, z, orientation);
            if (boundingbox2 != null) {
                return new MineshaftPieces.MineShaftCorridor(genDepth, random, boundingbox2, orientation, type);
            }
        }

        return null;
    }

    static MineshaftPieces.@Nullable MineShaftPiece generateAndAddPiece(
        StructurePiece piece,
        StructurePieceAccessor pieces,
        RandomSource random,
        int x,
        int y,
        int z,
        Direction direction,
        int genDepth
    ) {
        if (genDepth > 8) {
            return null;
        } else if (Math.abs(x - piece.getBoundingBox().minX()) <= 80 && Math.abs(z - piece.getBoundingBox().minZ()) <= 80) {
            MineshaftStructure.Type mineshaftstructure$type = ((MineshaftPieces.MineShaftPiece)piece).type;
            MineshaftPieces.MineShaftPiece mineshaftpieces$mineshaftpiece = createRandomShaftPiece(
                pieces, random, x, y, z, direction, genDepth + 1, mineshaftstructure$type
            );
            if (mineshaftpieces$mineshaftpiece != null) {
                pieces.addPiece(mineshaftpieces$mineshaftpiece);
                mineshaftpieces$mineshaftpiece.addChildren(piece, pieces, random);
            }

            return mineshaftpieces$mineshaftpiece;
        } else {
            return null;
        }
    }

    public static class MineShaftCorridor extends MineshaftPieces.MineShaftPiece {
        private final boolean hasRails;
        private final boolean spiderCorridor;
        private boolean hasPlacedSpider;
        private final int numSections;

        public MineShaftCorridor(CompoundTag tag) {
            super(StructurePieceType.MINE_SHAFT_CORRIDOR, tag);
            this.hasRails = tag.getBooleanOr("hr", false);
            this.spiderCorridor = tag.getBooleanOr("sc", false);
            this.hasPlacedSpider = tag.getBooleanOr("hps", false);
            this.numSections = tag.getIntOr("Num", 0);
        }

        @Override
        protected void addAdditionalSaveData(StructurePieceSerializationContext p_227806_, CompoundTag p_227807_) {
            super.addAdditionalSaveData(p_227806_, p_227807_);
            p_227807_.putBoolean("hr", this.hasRails);
            p_227807_.putBoolean("sc", this.spiderCorridor);
            p_227807_.putBoolean("hps", this.hasPlacedSpider);
            p_227807_.putInt("Num", this.numSections);
        }

        public MineShaftCorridor(int genDepth, RandomSource random, BoundingBox boundingBox, Direction orientation, MineshaftStructure.Type type) {
            super(StructurePieceType.MINE_SHAFT_CORRIDOR, genDepth, type, boundingBox);
            this.setOrientation(orientation);
            this.hasRails = random.nextInt(3) == 0;
            this.spiderCorridor = !this.hasRails && random.nextInt(23) == 0;
            if (this.getOrientation().getAxis() == Direction.Axis.Z) {
                this.numSections = boundingBox.getZSpan() / 5;
            } else {
                this.numSections = boundingBox.getXSpan() / 5;
            }
        }

        public static @Nullable BoundingBox findCorridorSize(
            StructurePieceAccessor pieces, RandomSource random, int x, int y, int z, Direction direction
        ) {
            for (int i = random.nextInt(3) + 2; i > 0; i--) {
                int j = i * 5;

                BoundingBox $$11 = switch (direction) {
                    default -> new BoundingBox(0, 0, -(j - 1), 2, 2, 0);
                    case SOUTH -> new BoundingBox(0, 0, 0, 2, 2, j - 1);
                    case WEST -> new BoundingBox(-(j - 1), 0, 0, 0, 2, 2);
                    case EAST -> new BoundingBox(0, 0, 0, j - 1, 2, 2);
                };
                $$11.move(x, y, z);
                if (pieces.findCollisionPiece($$11) == null) {
                    return $$11;
                }
            }

            return null;
        }

        @Override
        public void addChildren(StructurePiece p_227795_, StructurePieceAccessor p_227796_, RandomSource p_227797_) {
            int i = this.getGenDepth();
            int j = p_227797_.nextInt(4);
            Direction direction = this.getOrientation();
            if (direction != null) {
                switch (direction) {
                    case NORTH:
                    default:
                        if (j <= 1) {
                            MineshaftPieces.generateAndAddPiece(
                                p_227795_,
                                p_227796_,
                                p_227797_,
                                this.boundingBox.minX(),
                                this.boundingBox.minY() - 1 + p_227797_.nextInt(3),
                                this.boundingBox.minZ() - 1,
                                direction,
                                i
                            );
                        } else if (j == 2) {
                            MineshaftPieces.generateAndAddPiece(
                                p_227795_,
                                p_227796_,
                                p_227797_,
                                this.boundingBox.minX() - 1,
                                this.boundingBox.minY() - 1 + p_227797_.nextInt(3),
                                this.boundingBox.minZ(),
                                Direction.WEST,
                                i
                            );
                        } else {
                            MineshaftPieces.generateAndAddPiece(
                                p_227795_,
                                p_227796_,
                                p_227797_,
                                this.boundingBox.maxX() + 1,
                                this.boundingBox.minY() - 1 + p_227797_.nextInt(3),
                                this.boundingBox.minZ(),
                                Direction.EAST,
                                i
                            );
                        }
                        break;
                    case SOUTH:
                        if (j <= 1) {
                            MineshaftPieces.generateAndAddPiece(
                                p_227795_,
                                p_227796_,
                                p_227797_,
                                this.boundingBox.minX(),
                                this.boundingBox.minY() - 1 + p_227797_.nextInt(3),
                                this.boundingBox.maxZ() + 1,
                                direction,
                                i
                            );
                        } else if (j == 2) {
                            MineshaftPieces.generateAndAddPiece(
                                p_227795_,
                                p_227796_,
                                p_227797_,
                                this.boundingBox.minX() - 1,
                                this.boundingBox.minY() - 1 + p_227797_.nextInt(3),
                                this.boundingBox.maxZ() - 3,
                                Direction.WEST,
                                i
                            );
                        } else {
                            MineshaftPieces.generateAndAddPiece(
                                p_227795_,
                                p_227796_,
                                p_227797_,
                                this.boundingBox.maxX() + 1,
                                this.boundingBox.minY() - 1 + p_227797_.nextInt(3),
                                this.boundingBox.maxZ() - 3,
                                Direction.EAST,
                                i
                            );
                        }
                        break;
                    case WEST:
                        if (j <= 1) {
                            MineshaftPieces.generateAndAddPiece(
                                p_227795_,
                                p_227796_,
                                p_227797_,
                                this.boundingBox.minX() - 1,
                                this.boundingBox.minY() - 1 + p_227797_.nextInt(3),
                                this.boundingBox.minZ(),
                                direction,
                                i
                            );
                        } else if (j == 2) {
                            MineshaftPieces.generateAndAddPiece(
                                p_227795_,
                                p_227796_,
                                p_227797_,
                                this.boundingBox.minX(),
                                this.boundingBox.minY() - 1 + p_227797_.nextInt(3),
                                this.boundingBox.minZ() - 1,
                                Direction.NORTH,
                                i
                            );
                        } else {
                            MineshaftPieces.generateAndAddPiece(
                                p_227795_,
                                p_227796_,
                                p_227797_,
                                this.boundingBox.minX(),
                                this.boundingBox.minY() - 1 + p_227797_.nextInt(3),
                                this.boundingBox.maxZ() + 1,
                                Direction.SOUTH,
                                i
                            );
                        }
                        break;
                    case EAST:
                        if (j <= 1) {
                            MineshaftPieces.generateAndAddPiece(
                                p_227795_,
                                p_227796_,
                                p_227797_,
                                this.boundingBox.maxX() + 1,
                                this.boundingBox.minY() - 1 + p_227797_.nextInt(3),
                                this.boundingBox.minZ(),
                                direction,
                                i
                            );
                        } else if (j == 2) {
                            MineshaftPieces.generateAndAddPiece(
                                p_227795_,
                                p_227796_,
                                p_227797_,
                                this.boundingBox.maxX() - 3,
                                this.boundingBox.minY() - 1 + p_227797_.nextInt(3),
                                this.boundingBox.minZ() - 1,
                                Direction.NORTH,
                                i
                            );
                        } else {
                            MineshaftPieces.generateAndAddPiece(
                                p_227795_,
                                p_227796_,
                                p_227797_,
                                this.boundingBox.maxX() - 3,
                                this.boundingBox.minY() - 1 + p_227797_.nextInt(3),
                                this.boundingBox.maxZ() + 1,
                                Direction.SOUTH,
                                i
                            );
                        }
                }
            }

            if (i < 8) {
                if (direction != Direction.NORTH && direction != Direction.SOUTH) {
                    for (int i1 = this.boundingBox.minX() + 3; i1 + 3 <= this.boundingBox.maxX(); i1 += 5) {
                        int j1 = p_227797_.nextInt(5);
                        if (j1 == 0) {
                            MineshaftPieces.generateAndAddPiece(
                                p_227795_, p_227796_, p_227797_, i1, this.boundingBox.minY(), this.boundingBox.minZ() - 1, Direction.NORTH, i + 1
                            );
                        } else if (j1 == 1) {
                            MineshaftPieces.generateAndAddPiece(
                                p_227795_, p_227796_, p_227797_, i1, this.boundingBox.minY(), this.boundingBox.maxZ() + 1, Direction.SOUTH, i + 1
                            );
                        }
                    }
                } else {
                    for (int k = this.boundingBox.minZ() + 3; k + 3 <= this.boundingBox.maxZ(); k += 5) {
                        int l = p_227797_.nextInt(5);
                        if (l == 0) {
                            MineshaftPieces.generateAndAddPiece(
                                p_227795_, p_227796_, p_227797_, this.boundingBox.minX() - 1, this.boundingBox.minY(), k, Direction.WEST, i + 1
                            );
                        } else if (l == 1) {
                            MineshaftPieces.generateAndAddPiece(
                                p_227795_, p_227796_, p_227797_, this.boundingBox.maxX() + 1, this.boundingBox.minY(), k, Direction.EAST, i + 1
                            );
                        }
                    }
                }
            }
        }

        @Override
        protected boolean createChest(
            WorldGenLevel p_227787_,
            BoundingBox p_227788_,
            RandomSource p_227789_,
            int p_227790_,
            int p_227791_,
            int p_227792_,
            ResourceKey<LootTable> p_335869_
        ) {
            BlockPos blockpos = this.getWorldPos(p_227790_, p_227791_, p_227792_);
            if (p_227788_.isInside(blockpos) && p_227787_.getBlockState(blockpos).isAir() && !p_227787_.getBlockState(blockpos.below()).isAir()) {
                BlockState blockstate = Blocks.RAIL
                    .defaultBlockState()
                    .setValue(RailBlock.SHAPE, p_227789_.nextBoolean() ? RailShape.NORTH_SOUTH : RailShape.EAST_WEST);
                this.placeBlock(p_227787_, blockstate, p_227790_, p_227791_, p_227792_, p_227788_);
                MinecartChest minecartchest = EntityType.CHEST_MINECART.create(p_227787_.getLevel(), EntitySpawnReason.CHUNK_GENERATION);
                if (minecartchest != null) {
                    minecartchest.setInitialPos(blockpos.getX() + 0.5, blockpos.getY() + 0.5, blockpos.getZ() + 0.5);
                    minecartchest.setLootTable(p_335869_, p_227789_.nextLong());
                    p_227787_.addFreshEntity(minecartchest);
                }

                return true;
            } else {
                return false;
            }
        }

        @Override
        public void postProcess(
            WorldGenLevel p_227743_,
            StructureManager p_227744_,
            ChunkGenerator p_227745_,
            RandomSource p_227746_,
            BoundingBox p_227747_,
            ChunkPos p_227748_,
            BlockPos p_227749_
        ) {
            if (!this.isInInvalidLocation(p_227743_, p_227747_)) {
                int i = 0;
                int j = 2;
                int k = 0;
                int l = 2;
                int i1 = this.numSections * 5 - 1;
                BlockState blockstate = this.type.getPlanksState();
                this.generateBox(p_227743_, p_227747_, 0, 0, 0, 2, 1, i1, CAVE_AIR, CAVE_AIR, false);
                this.generateMaybeBox(p_227743_, p_227747_, p_227746_, 0.8F, 0, 2, 0, 2, 2, i1, CAVE_AIR, CAVE_AIR, false, false);
                if (this.spiderCorridor) {
                    this.generateMaybeBox(p_227743_, p_227747_, p_227746_, 0.6F, 0, 0, 0, 2, 1, i1, Blocks.COBWEB.defaultBlockState(), CAVE_AIR, false, true);
                }

                for (int j1 = 0; j1 < this.numSections; j1++) {
                    int k1 = 2 + j1 * 5;
                    this.placeSupport(p_227743_, p_227747_, 0, 0, k1, 2, 2, p_227746_);
                    this.maybePlaceCobWeb(p_227743_, p_227747_, p_227746_, 0.1F, 0, 2, k1 - 1);
                    this.maybePlaceCobWeb(p_227743_, p_227747_, p_227746_, 0.1F, 2, 2, k1 - 1);
                    this.maybePlaceCobWeb(p_227743_, p_227747_, p_227746_, 0.1F, 0, 2, k1 + 1);
                    this.maybePlaceCobWeb(p_227743_, p_227747_, p_227746_, 0.1F, 2, 2, k1 + 1);
                    this.maybePlaceCobWeb(p_227743_, p_227747_, p_227746_, 0.05F, 0, 2, k1 - 2);
                    this.maybePlaceCobWeb(p_227743_, p_227747_, p_227746_, 0.05F, 2, 2, k1 - 2);
                    this.maybePlaceCobWeb(p_227743_, p_227747_, p_227746_, 0.05F, 0, 2, k1 + 2);
                    this.maybePlaceCobWeb(p_227743_, p_227747_, p_227746_, 0.05F, 2, 2, k1 + 2);
                    if (p_227746_.nextInt(100) == 0) {
                        this.createChest(p_227743_, p_227747_, p_227746_, 2, 0, k1 - 1, BuiltInLootTables.ABANDONED_MINESHAFT);
                    }

                    if (p_227746_.nextInt(100) == 0) {
                        this.createChest(p_227743_, p_227747_, p_227746_, 0, 0, k1 + 1, BuiltInLootTables.ABANDONED_MINESHAFT);
                    }

                    if (this.spiderCorridor && !this.hasPlacedSpider) {
                        int l1 = 1;
                        int i2 = k1 - 1 + p_227746_.nextInt(3);
                        BlockPos blockpos = this.getWorldPos(1, 0, i2);
                        if (p_227747_.isInside(blockpos) && this.isInterior(p_227743_, 1, 0, i2, p_227747_)) {
                            this.hasPlacedSpider = true;
                            p_227743_.setBlock(blockpos, Blocks.SPAWNER.defaultBlockState(), 2);
                            if (p_227743_.getBlockEntity(blockpos) instanceof SpawnerBlockEntity spawnerblockentity) {
                                spawnerblockentity.setEntityId(EntityType.CAVE_SPIDER, p_227746_);
                            }
                        }
                    }
                }

                for (int j2 = 0; j2 <= 2; j2++) {
                    for (int l2 = 0; l2 <= i1; l2++) {
                        this.setPlanksBlock(p_227743_, p_227747_, blockstate, j2, -1, l2);
                    }
                }

                int k2 = 2;
                this.placeDoubleLowerOrUpperSupport(p_227743_, p_227747_, 0, -1, 2);
                if (this.numSections > 1) {
                    int i3 = i1 - 2;
                    this.placeDoubleLowerOrUpperSupport(p_227743_, p_227747_, 0, -1, i3);
                }

                if (this.hasRails) {
                    BlockState blockstate1 = Blocks.RAIL.defaultBlockState().setValue(RailBlock.SHAPE, RailShape.NORTH_SOUTH);

                    for (int j3 = 0; j3 <= i1; j3++) {
                        BlockState blockstate2 = this.getBlock(p_227743_, 1, -1, j3, p_227747_);
                        if (!blockstate2.isAir() && blockstate2.isSolidRender()) {
                            float f = this.isInterior(p_227743_, 1, 0, j3, p_227747_) ? 0.7F : 0.9F;
                            this.maybeGenerateBlock(p_227743_, p_227747_, p_227746_, f, 1, 0, j3, blockstate1);
                        }
                    }
                }
            }
        }

        private void placeDoubleLowerOrUpperSupport(WorldGenLevel level, BoundingBox box, int x, int y, int z) {
            BlockState blockstate = this.type.getWoodState();
            BlockState blockstate1 = this.type.getPlanksState();
            if (this.getBlock(level, x, y, z, box).is(blockstate1.getBlock())) {
                this.fillPillarDownOrChainUp(level, blockstate, x, y, z, box);
            }

            if (this.getBlock(level, x + 2, y, z, box).is(blockstate1.getBlock())) {
                this.fillPillarDownOrChainUp(level, blockstate, x + 2, y, z, box);
            }
        }

        @Override
        protected void fillColumnDown(WorldGenLevel p_227813_, BlockState p_227814_, int p_227815_, int p_227816_, int p_227817_, BoundingBox p_227818_) {
            BlockPos.MutableBlockPos blockpos$mutableblockpos = this.getWorldPos(p_227815_, p_227816_, p_227817_);
            if (p_227818_.isInside(blockpos$mutableblockpos)) {
                int i = blockpos$mutableblockpos.getY();

                while (
                    this.isReplaceableByStructures(p_227813_.getBlockState(blockpos$mutableblockpos))
                        && blockpos$mutableblockpos.getY() > p_227813_.getMinY() + 1
                ) {
                    blockpos$mutableblockpos.move(Direction.DOWN);
                }

                if (this.canPlaceColumnOnTopOf(p_227813_, blockpos$mutableblockpos, p_227813_.getBlockState(blockpos$mutableblockpos))) {
                    while (blockpos$mutableblockpos.getY() < i) {
                        blockpos$mutableblockpos.move(Direction.UP);
                        p_227813_.setBlock(blockpos$mutableblockpos, p_227814_, 2);
                    }
                }
            }
        }

        protected void fillPillarDownOrChainUp(
            WorldGenLevel level, BlockState state, int x, int y, int z, BoundingBox box
        ) {
            BlockPos.MutableBlockPos blockpos$mutableblockpos = this.getWorldPos(x, y, z);
            if (box.isInside(blockpos$mutableblockpos)) {
                int i = blockpos$mutableblockpos.getY();
                int j = 1;
                boolean flag = true;

                for (boolean flag1 = true; flag || flag1; j++) {
                    if (flag) {
                        blockpos$mutableblockpos.setY(i - j);
                        BlockState blockstate = level.getBlockState(blockpos$mutableblockpos);
                        boolean flag2 = this.isReplaceableByStructures(blockstate) && !blockstate.is(Blocks.LAVA);
                        if (!flag2 && this.canPlaceColumnOnTopOf(level, blockpos$mutableblockpos, blockstate)) {
                            fillColumnBetween(level, state, blockpos$mutableblockpos, i - j + 1, i);
                            return;
                        }

                        flag = j <= 20 && flag2 && blockpos$mutableblockpos.getY() > level.getMinY() + 1;
                    }

                    if (flag1) {
                        blockpos$mutableblockpos.setY(i + j);
                        BlockState blockstate1 = level.getBlockState(blockpos$mutableblockpos);
                        boolean flag3 = this.isReplaceableByStructures(blockstate1);
                        if (!flag3 && this.canHangChainBelow(level, blockpos$mutableblockpos, blockstate1)) {
                            level.setBlock(blockpos$mutableblockpos.setY(i + 1), this.type.getFenceState(), 2);
                            fillColumnBetween(level, Blocks.IRON_CHAIN.defaultBlockState(), blockpos$mutableblockpos, i + 2, i + j);
                            return;
                        }

                        flag1 = j <= 50 && flag3 && blockpos$mutableblockpos.getY() < level.getMaxY();
                    }
                }
            }
        }

        private static void fillColumnBetween(WorldGenLevel level, BlockState state, BlockPos.MutableBlockPos pos, int minY, int maxY) {
            for (int i = minY; i < maxY; i++) {
                level.setBlock(pos.setY(i), state, 2);
            }
        }

        private boolean canPlaceColumnOnTopOf(LevelReader level, BlockPos pos, BlockState state) {
            return state.isFaceSturdy(level, pos, Direction.UP);
        }

        private boolean canHangChainBelow(LevelReader level, BlockPos pos, BlockState state) {
            return Block.canSupportCenter(level, pos, Direction.DOWN) && !(state.getBlock() instanceof FallingBlock);
        }

        private void placeSupport(
            WorldGenLevel level, BoundingBox box, int minX, int minY, int z, int maxY, int maxX, RandomSource random
        ) {
            if (this.isSupportingBox(level, box, minX, maxX, maxY, z)) {
                BlockState blockstate = this.type.getPlanksState();
                BlockState blockstate1 = this.type.getFenceState();
                this.generateBox(
                    level,
                    box,
                    minX,
                    minY,
                    z,
                    minX,
                    maxY - 1,
                    z,
                    blockstate1.setValue(FenceBlock.WEST, true),
                    CAVE_AIR,
                    false
                );
                this.generateBox(
                    level,
                    box,
                    maxX,
                    minY,
                    z,
                    maxX,
                    maxY - 1,
                    z,
                    blockstate1.setValue(FenceBlock.EAST, true),
                    CAVE_AIR,
                    false
                );
                if (random.nextInt(4) == 0) {
                    this.generateBox(level, box, minX, maxY, z, minX, maxY, z, blockstate, CAVE_AIR, false);
                    this.generateBox(level, box, maxX, maxY, z, maxX, maxY, z, blockstate, CAVE_AIR, false);
                } else {
                    this.generateBox(level, box, minX, maxY, z, maxX, maxY, z, blockstate, CAVE_AIR, false);
                    this.maybeGenerateBlock(
                        level,
                        box,
                        random,
                        0.05F,
                        minX + 1,
                        maxY,
                        z - 1,
                        Blocks.WALL_TORCH.defaultBlockState().setValue(WallTorchBlock.FACING, Direction.SOUTH)
                    );
                    this.maybeGenerateBlock(
                        level,
                        box,
                        random,
                        0.05F,
                        minX + 1,
                        maxY,
                        z + 1,
                        Blocks.WALL_TORCH.defaultBlockState().setValue(WallTorchBlock.FACING, Direction.NORTH)
                    );
                }
            }
        }

        private void maybePlaceCobWeb(
            WorldGenLevel level, BoundingBox box, RandomSource random, float chance, int x, int y, int z
        ) {
            if (this.isInterior(level, x, y, z, box)
                && random.nextFloat() < chance
                && this.hasSturdyNeighbours(level, box, x, y, z, 2)) {
                this.placeBlock(level, Blocks.COBWEB.defaultBlockState(), x, y, z, box);
            }
        }

        private boolean hasSturdyNeighbours(WorldGenLevel level, BoundingBox box, int x, int y, int z, int required) {
            BlockPos.MutableBlockPos blockpos$mutableblockpos = this.getWorldPos(x, y, z);
            int i = 0;

            for (Direction direction : Direction.values()) {
                blockpos$mutableblockpos.move(direction);
                if (box.isInside(blockpos$mutableblockpos)
                    && level.getBlockState(blockpos$mutableblockpos).isFaceSturdy(level, blockpos$mutableblockpos, direction.getOpposite())) {
                    if (++i >= required) {
                        return true;
                    }
                }

                blockpos$mutableblockpos.move(direction.getOpposite());
            }

            return false;
        }
    }

    public static class MineShaftCrossing extends MineshaftPieces.MineShaftPiece {
        private final Direction direction;
        private final boolean isTwoFloored;

        public MineShaftCrossing(CompoundTag tag) {
            super(StructurePieceType.MINE_SHAFT_CROSSING, tag);
            this.isTwoFloored = tag.getBooleanOr("tf", false);
            this.direction = tag.read("D", Direction.LEGACY_ID_CODEC_2D).orElse(Direction.SOUTH);
        }

        @Override
        protected void addAdditionalSaveData(StructurePieceSerializationContext p_227862_, CompoundTag p_227863_) {
            super.addAdditionalSaveData(p_227862_, p_227863_);
            p_227863_.putBoolean("tf", this.isTwoFloored);
            p_227863_.store("D", Direction.LEGACY_ID_CODEC_2D, this.direction);
        }

        public MineShaftCrossing(int genDepth, BoundingBox boundingBox, @Nullable Direction direction, MineshaftStructure.Type type) {
            super(StructurePieceType.MINE_SHAFT_CROSSING, genDepth, type, boundingBox);
            this.direction = direction;
            this.isTwoFloored = boundingBox.getYSpan() > 3;
        }

        public static @Nullable BoundingBox findCrossing(
            StructurePieceAccessor pieces, RandomSource random, int x, int y, int z, Direction direction
        ) {
            int i;
            if (random.nextInt(4) == 0) {
                i = 6;
            } else {
                i = 2;
            }
            BoundingBox $$11 = switch (direction) {
                default -> new BoundingBox(-1, 0, -4, 3, i, 0);
                case SOUTH -> new BoundingBox(-1, 0, 0, 3, i, 4);
                case WEST -> new BoundingBox(-4, 0, -1, 0, i, 3);
                case EAST -> new BoundingBox(0, 0, -1, 4, i, 3);
            };
            $$11.move(x, y, z);
            return pieces.findCollisionPiece($$11) != null ? null : $$11;
        }

        @Override
        public void addChildren(StructurePiece p_227851_, StructurePieceAccessor p_227852_, RandomSource p_227853_) {
            int i = this.getGenDepth();
            switch (this.direction) {
                case NORTH:
                default:
                    MineshaftPieces.generateAndAddPiece(
                        p_227851_, p_227852_, p_227853_, this.boundingBox.minX() + 1, this.boundingBox.minY(), this.boundingBox.minZ() - 1, Direction.NORTH, i
                    );
                    MineshaftPieces.generateAndAddPiece(
                        p_227851_, p_227852_, p_227853_, this.boundingBox.minX() - 1, this.boundingBox.minY(), this.boundingBox.minZ() + 1, Direction.WEST, i
                    );
                    MineshaftPieces.generateAndAddPiece(
                        p_227851_, p_227852_, p_227853_, this.boundingBox.maxX() + 1, this.boundingBox.minY(), this.boundingBox.minZ() + 1, Direction.EAST, i
                    );
                    break;
                case SOUTH:
                    MineshaftPieces.generateAndAddPiece(
                        p_227851_, p_227852_, p_227853_, this.boundingBox.minX() + 1, this.boundingBox.minY(), this.boundingBox.maxZ() + 1, Direction.SOUTH, i
                    );
                    MineshaftPieces.generateAndAddPiece(
                        p_227851_, p_227852_, p_227853_, this.boundingBox.minX() - 1, this.boundingBox.minY(), this.boundingBox.minZ() + 1, Direction.WEST, i
                    );
                    MineshaftPieces.generateAndAddPiece(
                        p_227851_, p_227852_, p_227853_, this.boundingBox.maxX() + 1, this.boundingBox.minY(), this.boundingBox.minZ() + 1, Direction.EAST, i
                    );
                    break;
                case WEST:
                    MineshaftPieces.generateAndAddPiece(
                        p_227851_, p_227852_, p_227853_, this.boundingBox.minX() + 1, this.boundingBox.minY(), this.boundingBox.minZ() - 1, Direction.NORTH, i
                    );
                    MineshaftPieces.generateAndAddPiece(
                        p_227851_, p_227852_, p_227853_, this.boundingBox.minX() + 1, this.boundingBox.minY(), this.boundingBox.maxZ() + 1, Direction.SOUTH, i
                    );
                    MineshaftPieces.generateAndAddPiece(
                        p_227851_, p_227852_, p_227853_, this.boundingBox.minX() - 1, this.boundingBox.minY(), this.boundingBox.minZ() + 1, Direction.WEST, i
                    );
                    break;
                case EAST:
                    MineshaftPieces.generateAndAddPiece(
                        p_227851_, p_227852_, p_227853_, this.boundingBox.minX() + 1, this.boundingBox.minY(), this.boundingBox.minZ() - 1, Direction.NORTH, i
                    );
                    MineshaftPieces.generateAndAddPiece(
                        p_227851_, p_227852_, p_227853_, this.boundingBox.minX() + 1, this.boundingBox.minY(), this.boundingBox.maxZ() + 1, Direction.SOUTH, i
                    );
                    MineshaftPieces.generateAndAddPiece(
                        p_227851_, p_227852_, p_227853_, this.boundingBox.maxX() + 1, this.boundingBox.minY(), this.boundingBox.minZ() + 1, Direction.EAST, i
                    );
            }

            if (this.isTwoFloored) {
                if (p_227853_.nextBoolean()) {
                    MineshaftPieces.generateAndAddPiece(
                        p_227851_,
                        p_227852_,
                        p_227853_,
                        this.boundingBox.minX() + 1,
                        this.boundingBox.minY() + 3 + 1,
                        this.boundingBox.minZ() - 1,
                        Direction.NORTH,
                        i
                    );
                }

                if (p_227853_.nextBoolean()) {
                    MineshaftPieces.generateAndAddPiece(
                        p_227851_,
                        p_227852_,
                        p_227853_,
                        this.boundingBox.minX() - 1,
                        this.boundingBox.minY() + 3 + 1,
                        this.boundingBox.minZ() + 1,
                        Direction.WEST,
                        i
                    );
                }

                if (p_227853_.nextBoolean()) {
                    MineshaftPieces.generateAndAddPiece(
                        p_227851_,
                        p_227852_,
                        p_227853_,
                        this.boundingBox.maxX() + 1,
                        this.boundingBox.minY() + 3 + 1,
                        this.boundingBox.minZ() + 1,
                        Direction.EAST,
                        i
                    );
                }

                if (p_227853_.nextBoolean()) {
                    MineshaftPieces.generateAndAddPiece(
                        p_227851_,
                        p_227852_,
                        p_227853_,
                        this.boundingBox.minX() + 1,
                        this.boundingBox.minY() + 3 + 1,
                        this.boundingBox.maxZ() + 1,
                        Direction.SOUTH,
                        i
                    );
                }
            }
        }

        @Override
        public void postProcess(
            WorldGenLevel p_227836_,
            StructureManager p_227837_,
            ChunkGenerator p_227838_,
            RandomSource p_227839_,
            BoundingBox p_227840_,
            ChunkPos p_227841_,
            BlockPos p_227842_
        ) {
            if (!this.isInInvalidLocation(p_227836_, p_227840_)) {
                BlockState blockstate = this.type.getPlanksState();
                if (this.isTwoFloored) {
                    this.generateBox(
                        p_227836_,
                        p_227840_,
                        this.boundingBox.minX() + 1,
                        this.boundingBox.minY(),
                        this.boundingBox.minZ(),
                        this.boundingBox.maxX() - 1,
                        this.boundingBox.minY() + 3 - 1,
                        this.boundingBox.maxZ(),
                        CAVE_AIR,
                        CAVE_AIR,
                        false
                    );
                    this.generateBox(
                        p_227836_,
                        p_227840_,
                        this.boundingBox.minX(),
                        this.boundingBox.minY(),
                        this.boundingBox.minZ() + 1,
                        this.boundingBox.maxX(),
                        this.boundingBox.minY() + 3 - 1,
                        this.boundingBox.maxZ() - 1,
                        CAVE_AIR,
                        CAVE_AIR,
                        false
                    );
                    this.generateBox(
                        p_227836_,
                        p_227840_,
                        this.boundingBox.minX() + 1,
                        this.boundingBox.maxY() - 2,
                        this.boundingBox.minZ(),
                        this.boundingBox.maxX() - 1,
                        this.boundingBox.maxY(),
                        this.boundingBox.maxZ(),
                        CAVE_AIR,
                        CAVE_AIR,
                        false
                    );
                    this.generateBox(
                        p_227836_,
                        p_227840_,
                        this.boundingBox.minX(),
                        this.boundingBox.maxY() - 2,
                        this.boundingBox.minZ() + 1,
                        this.boundingBox.maxX(),
                        this.boundingBox.maxY(),
                        this.boundingBox.maxZ() - 1,
                        CAVE_AIR,
                        CAVE_AIR,
                        false
                    );
                    this.generateBox(
                        p_227836_,
                        p_227840_,
                        this.boundingBox.minX() + 1,
                        this.boundingBox.minY() + 3,
                        this.boundingBox.minZ() + 1,
                        this.boundingBox.maxX() - 1,
                        this.boundingBox.minY() + 3,
                        this.boundingBox.maxZ() - 1,
                        CAVE_AIR,
                        CAVE_AIR,
                        false
                    );
                } else {
                    this.generateBox(
                        p_227836_,
                        p_227840_,
                        this.boundingBox.minX() + 1,
                        this.boundingBox.minY(),
                        this.boundingBox.minZ(),
                        this.boundingBox.maxX() - 1,
                        this.boundingBox.maxY(),
                        this.boundingBox.maxZ(),
                        CAVE_AIR,
                        CAVE_AIR,
                        false
                    );
                    this.generateBox(
                        p_227836_,
                        p_227840_,
                        this.boundingBox.minX(),
                        this.boundingBox.minY(),
                        this.boundingBox.minZ() + 1,
                        this.boundingBox.maxX(),
                        this.boundingBox.maxY(),
                        this.boundingBox.maxZ() - 1,
                        CAVE_AIR,
                        CAVE_AIR,
                        false
                    );
                }

                this.placeSupportPillar(
                    p_227836_, p_227840_, this.boundingBox.minX() + 1, this.boundingBox.minY(), this.boundingBox.minZ() + 1, this.boundingBox.maxY()
                );
                this.placeSupportPillar(
                    p_227836_, p_227840_, this.boundingBox.minX() + 1, this.boundingBox.minY(), this.boundingBox.maxZ() - 1, this.boundingBox.maxY()
                );
                this.placeSupportPillar(
                    p_227836_, p_227840_, this.boundingBox.maxX() - 1, this.boundingBox.minY(), this.boundingBox.minZ() + 1, this.boundingBox.maxY()
                );
                this.placeSupportPillar(
                    p_227836_, p_227840_, this.boundingBox.maxX() - 1, this.boundingBox.minY(), this.boundingBox.maxZ() - 1, this.boundingBox.maxY()
                );
                int i = this.boundingBox.minY() - 1;

                for (int j = this.boundingBox.minX(); j <= this.boundingBox.maxX(); j++) {
                    for (int k = this.boundingBox.minZ(); k <= this.boundingBox.maxZ(); k++) {
                        this.setPlanksBlock(p_227836_, p_227840_, blockstate, j, i, k);
                    }
                }
            }
        }

        private void placeSupportPillar(WorldGenLevel level, BoundingBox box, int x, int y, int z, int maxY) {
            if (!this.getBlock(level, x, maxY + 1, z, box).isAir()) {
                this.generateBox(
                    level, box, x, y, z, x, maxY, z, this.type.getPlanksState(), CAVE_AIR, false
                );
            }
        }
    }

    abstract static class MineShaftPiece extends StructurePiece {
        protected MineshaftStructure.Type type;

        public MineShaftPiece(StructurePieceType structurePieceType, int genDepth, MineshaftStructure.Type type, BoundingBox boundingBox) {
            super(structurePieceType, genDepth, boundingBox);
            this.type = type;
        }

        public MineShaftPiece(StructurePieceType p_227872_, CompoundTag p_227873_) {
            super(p_227872_, p_227873_);
            this.type = MineshaftStructure.Type.byId(p_227873_.getIntOr("MST", 0));
        }

        @Override
        protected boolean canBeReplaced(LevelReader p_227885_, int p_227886_, int p_227887_, int p_227888_, BoundingBox p_227889_) {
            BlockState blockstate = this.getBlock(p_227885_, p_227886_, p_227887_, p_227888_, p_227889_);
            return !blockstate.is(this.type.getPlanksState().getBlock())
                && !blockstate.is(this.type.getWoodState().getBlock())
                && !blockstate.is(this.type.getFenceState().getBlock())
                && !blockstate.is(Blocks.IRON_CHAIN);
        }

        @Override
        protected void addAdditionalSaveData(StructurePieceSerializationContext p_227898_, CompoundTag p_227899_) {
            p_227899_.putInt("MST", this.type.ordinal());
        }

        protected boolean isSupportingBox(BlockGetter level, BoundingBox box, int xStart, int xEnd, int y, int z) {
            for (int i = xStart; i <= xEnd; i++) {
                if (this.getBlock(level, i, y + 1, z, box).isAir()) {
                    return false;
                }
            }

            return true;
        }

        protected boolean isInInvalidLocation(LevelAccessor level, BoundingBox boundingBox) {
            int i = Math.max(this.boundingBox.minX() - 1, boundingBox.minX());
            int j = Math.max(this.boundingBox.minY() - 1, boundingBox.minY());
            int k = Math.max(this.boundingBox.minZ() - 1, boundingBox.minZ());
            int l = Math.min(this.boundingBox.maxX() + 1, boundingBox.maxX());
            int i1 = Math.min(this.boundingBox.maxY() + 1, boundingBox.maxY());
            int j1 = Math.min(this.boundingBox.maxZ() + 1, boundingBox.maxZ());
            BlockPos.MutableBlockPos blockpos$mutableblockpos = new BlockPos.MutableBlockPos((i + l) / 2, (j + i1) / 2, (k + j1) / 2);
            if (level.getBiome(blockpos$mutableblockpos).is(BiomeTags.MINESHAFT_BLOCKING)) {
                return true;
            } else {
                for (int k1 = i; k1 <= l; k1++) {
                    for (int l1 = k; l1 <= j1; l1++) {
                        if (level.getBlockState(blockpos$mutableblockpos.set(k1, j, l1)).liquid()) {
                            return true;
                        }

                        if (level.getBlockState(blockpos$mutableblockpos.set(k1, i1, l1)).liquid()) {
                            return true;
                        }
                    }
                }

                for (int i2 = i; i2 <= l; i2++) {
                    for (int k2 = j; k2 <= i1; k2++) {
                        if (level.getBlockState(blockpos$mutableblockpos.set(i2, k2, k)).liquid()) {
                            return true;
                        }

                        if (level.getBlockState(blockpos$mutableblockpos.set(i2, k2, j1)).liquid()) {
                            return true;
                        }
                    }
                }

                for (int j2 = k; j2 <= j1; j2++) {
                    for (int l2 = j; l2 <= i1; l2++) {
                        if (level.getBlockState(blockpos$mutableblockpos.set(i, l2, j2)).liquid()) {
                            return true;
                        }

                        if (level.getBlockState(blockpos$mutableblockpos.set(l, l2, j2)).liquid()) {
                            return true;
                        }
                    }
                }

                return false;
            }
        }

        protected void setPlanksBlock(WorldGenLevel level, BoundingBox box, BlockState plankState, int x, int y, int z) {
            if (this.isInterior(level, x, y, z, box)) {
                BlockPos blockpos = this.getWorldPos(x, y, z);
                BlockState blockstate = level.getBlockState(blockpos);
                if (!blockstate.isFaceSturdy(level, blockpos, Direction.UP)) {
                    level.setBlock(blockpos, plankState, 2);
                }
            }
        }
    }

    public static class MineShaftRoom extends MineshaftPieces.MineShaftPiece {
        private final List<BoundingBox> childEntranceBoxes = Lists.newLinkedList();

        public MineShaftRoom(int genDepth, RandomSource random, int x, int z, MineshaftStructure.Type type) {
            super(
                StructurePieceType.MINE_SHAFT_ROOM,
                genDepth,
                type,
                new BoundingBox(x, 50, z, x + 7 + random.nextInt(6), 54 + random.nextInt(6), z + 7 + random.nextInt(6))
            );
            this.type = type;
        }

        public MineShaftRoom(CompoundTag tag) {
            super(StructurePieceType.MINE_SHAFT_ROOM, tag);
            this.childEntranceBoxes.addAll(tag.read("Entrances", BoundingBox.CODEC.listOf()).orElse(List.of()));
        }

        @Override
        public void addChildren(StructurePiece p_227922_, StructurePieceAccessor p_227923_, RandomSource p_227924_) {
            int i = this.getGenDepth();
            int k = this.boundingBox.getYSpan() - 3 - 1;
            if (k <= 0) {
                k = 1;
            }

            int j = 0;

            while (j < this.boundingBox.getXSpan()) {
                j += p_227924_.nextInt(this.boundingBox.getXSpan());
                if (j + 3 > this.boundingBox.getXSpan()) {
                    break;
                }

                MineshaftPieces.MineShaftPiece mineshaftpieces$mineshaftpiece = MineshaftPieces.generateAndAddPiece(
                    p_227922_,
                    p_227923_,
                    p_227924_,
                    this.boundingBox.minX() + j,
                    this.boundingBox.minY() + p_227924_.nextInt(k) + 1,
                    this.boundingBox.minZ() - 1,
                    Direction.NORTH,
                    i
                );
                if (mineshaftpieces$mineshaftpiece != null) {
                    BoundingBox boundingbox = mineshaftpieces$mineshaftpiece.getBoundingBox();
                    this.childEntranceBoxes
                        .add(
                            new BoundingBox(
                                boundingbox.minX(),
                                boundingbox.minY(),
                                this.boundingBox.minZ(),
                                boundingbox.maxX(),
                                boundingbox.maxY(),
                                this.boundingBox.minZ() + 1
                            )
                        );
                }

                j += 4;
            }

            j = 0;

            while (j < this.boundingBox.getXSpan()) {
                j += p_227924_.nextInt(this.boundingBox.getXSpan());
                if (j + 3 > this.boundingBox.getXSpan()) {
                    break;
                }

                MineshaftPieces.MineShaftPiece mineshaftpieces$mineshaftpiece1 = MineshaftPieces.generateAndAddPiece(
                    p_227922_,
                    p_227923_,
                    p_227924_,
                    this.boundingBox.minX() + j,
                    this.boundingBox.minY() + p_227924_.nextInt(k) + 1,
                    this.boundingBox.maxZ() + 1,
                    Direction.SOUTH,
                    i
                );
                if (mineshaftpieces$mineshaftpiece1 != null) {
                    BoundingBox boundingbox1 = mineshaftpieces$mineshaftpiece1.getBoundingBox();
                    this.childEntranceBoxes
                        .add(
                            new BoundingBox(
                                boundingbox1.minX(),
                                boundingbox1.minY(),
                                this.boundingBox.maxZ() - 1,
                                boundingbox1.maxX(),
                                boundingbox1.maxY(),
                                this.boundingBox.maxZ()
                            )
                        );
                }

                j += 4;
            }

            j = 0;

            while (j < this.boundingBox.getZSpan()) {
                j += p_227924_.nextInt(this.boundingBox.getZSpan());
                if (j + 3 > this.boundingBox.getZSpan()) {
                    break;
                }

                MineshaftPieces.MineShaftPiece mineshaftpieces$mineshaftpiece2 = MineshaftPieces.generateAndAddPiece(
                    p_227922_,
                    p_227923_,
                    p_227924_,
                    this.boundingBox.minX() - 1,
                    this.boundingBox.minY() + p_227924_.nextInt(k) + 1,
                    this.boundingBox.minZ() + j,
                    Direction.WEST,
                    i
                );
                if (mineshaftpieces$mineshaftpiece2 != null) {
                    BoundingBox boundingbox2 = mineshaftpieces$mineshaftpiece2.getBoundingBox();
                    this.childEntranceBoxes
                        .add(
                            new BoundingBox(
                                this.boundingBox.minX(),
                                boundingbox2.minY(),
                                boundingbox2.minZ(),
                                this.boundingBox.minX() + 1,
                                boundingbox2.maxY(),
                                boundingbox2.maxZ()
                            )
                        );
                }

                j += 4;
            }

            j = 0;

            while (j < this.boundingBox.getZSpan()) {
                j += p_227924_.nextInt(this.boundingBox.getZSpan());
                if (j + 3 > this.boundingBox.getZSpan()) {
                    break;
                }

                StructurePiece structurepiece = MineshaftPieces.generateAndAddPiece(
                    p_227922_,
                    p_227923_,
                    p_227924_,
                    this.boundingBox.maxX() + 1,
                    this.boundingBox.minY() + p_227924_.nextInt(k) + 1,
                    this.boundingBox.minZ() + j,
                    Direction.EAST,
                    i
                );
                if (structurepiece != null) {
                    BoundingBox boundingbox3 = structurepiece.getBoundingBox();
                    this.childEntranceBoxes
                        .add(
                            new BoundingBox(
                                this.boundingBox.maxX() - 1,
                                boundingbox3.minY(),
                                boundingbox3.minZ(),
                                this.boundingBox.maxX(),
                                boundingbox3.maxY(),
                                boundingbox3.maxZ()
                            )
                        );
                }

                j += 4;
            }
        }

        @Override
        public void postProcess(
            WorldGenLevel p_227914_,
            StructureManager p_227915_,
            ChunkGenerator p_227916_,
            RandomSource p_227917_,
            BoundingBox p_227918_,
            ChunkPos p_227919_,
            BlockPos p_227920_
        ) {
            if (!this.isInInvalidLocation(p_227914_, p_227918_)) {
                this.generateBox(
                    p_227914_,
                    p_227918_,
                    this.boundingBox.minX(),
                    this.boundingBox.minY() + 1,
                    this.boundingBox.minZ(),
                    this.boundingBox.maxX(),
                    Math.min(this.boundingBox.minY() + 3, this.boundingBox.maxY()),
                    this.boundingBox.maxZ(),
                    CAVE_AIR,
                    CAVE_AIR,
                    false
                );

                for (BoundingBox boundingbox : this.childEntranceBoxes) {
                    this.generateBox(
                        p_227914_,
                        p_227918_,
                        boundingbox.minX(),
                        boundingbox.maxY() - 2,
                        boundingbox.minZ(),
                        boundingbox.maxX(),
                        boundingbox.maxY(),
                        boundingbox.maxZ(),
                        CAVE_AIR,
                        CAVE_AIR,
                        false
                    );
                }

                this.generateUpperHalfSphere(
                    p_227914_,
                    p_227918_,
                    this.boundingBox.minX(),
                    this.boundingBox.minY() + 4,
                    this.boundingBox.minZ(),
                    this.boundingBox.maxX(),
                    this.boundingBox.maxY(),
                    this.boundingBox.maxZ(),
                    CAVE_AIR,
                    false
                );
            }
        }

        @Override
        public void move(int p_227910_, int p_227911_, int p_227912_) {
            super.move(p_227910_, p_227911_, p_227912_);

            for (BoundingBox boundingbox : this.childEntranceBoxes) {
                boundingbox.move(p_227910_, p_227911_, p_227912_);
            }
        }

        @Override
        protected void addAdditionalSaveData(StructurePieceSerializationContext p_227926_, CompoundTag p_227927_) {
            super.addAdditionalSaveData(p_227926_, p_227927_);
            p_227927_.store("Entrances", BoundingBox.CODEC.listOf(), this.childEntranceBoxes);
        }
    }

    public static class MineShaftStairs extends MineshaftPieces.MineShaftPiece {
        public MineShaftStairs(int genDepth, BoundingBox boundingBox, Direction orientation, MineshaftStructure.Type type) {
            super(StructurePieceType.MINE_SHAFT_STAIRS, genDepth, type, boundingBox);
            this.setOrientation(orientation);
        }

        public MineShaftStairs(CompoundTag tag) {
            super(StructurePieceType.MINE_SHAFT_STAIRS, tag);
        }

        public static @Nullable BoundingBox findStairs(
            StructurePieceAccessor pieces, RandomSource random, int x, int y, int z, Direction direction
        ) {
            BoundingBox $$9 = switch (direction) {
                default -> new BoundingBox(0, -5, -8, 2, 2, 0);
                case SOUTH -> new BoundingBox(0, -5, 0, 2, 2, 8);
                case WEST -> new BoundingBox(-8, -5, 0, 0, 2, 2);
                case EAST -> new BoundingBox(0, -5, 0, 8, 2, 2);
            };
            $$9.move(x, y, z);
            return pieces.findCollisionPiece($$9) != null ? null : $$9;
        }

        @Override
        public void addChildren(StructurePiece p_227947_, StructurePieceAccessor p_227948_, RandomSource p_227949_) {
            int i = this.getGenDepth();
            Direction direction = this.getOrientation();
            if (direction != null) {
                switch (direction) {
                    case NORTH:
                    default:
                        MineshaftPieces.generateAndAddPiece(
                            p_227947_, p_227948_, p_227949_, this.boundingBox.minX(), this.boundingBox.minY(), this.boundingBox.minZ() - 1, Direction.NORTH, i
                        );
                        break;
                    case SOUTH:
                        MineshaftPieces.generateAndAddPiece(
                            p_227947_, p_227948_, p_227949_, this.boundingBox.minX(), this.boundingBox.minY(), this.boundingBox.maxZ() + 1, Direction.SOUTH, i
                        );
                        break;
                    case WEST:
                        MineshaftPieces.generateAndAddPiece(
                            p_227947_, p_227948_, p_227949_, this.boundingBox.minX() - 1, this.boundingBox.minY(), this.boundingBox.minZ(), Direction.WEST, i
                        );
                        break;
                    case EAST:
                        MineshaftPieces.generateAndAddPiece(
                            p_227947_, p_227948_, p_227949_, this.boundingBox.maxX() + 1, this.boundingBox.minY(), this.boundingBox.minZ(), Direction.EAST, i
                        );
                }
            }
        }

        @Override
        public void postProcess(
            WorldGenLevel p_227939_,
            StructureManager p_227940_,
            ChunkGenerator p_227941_,
            RandomSource p_227942_,
            BoundingBox p_227943_,
            ChunkPos p_227944_,
            BlockPos p_227945_
        ) {
            if (!this.isInInvalidLocation(p_227939_, p_227943_)) {
                this.generateBox(p_227939_, p_227943_, 0, 5, 0, 2, 7, 1, CAVE_AIR, CAVE_AIR, false);
                this.generateBox(p_227939_, p_227943_, 0, 0, 7, 2, 2, 8, CAVE_AIR, CAVE_AIR, false);

                for (int i = 0; i < 5; i++) {
                    this.generateBox(p_227939_, p_227943_, 0, 5 - i - (i < 4 ? 1 : 0), 2 + i, 2, 7 - i, 2 + i, CAVE_AIR, CAVE_AIR, false);
                }
            }
        }
    }
}
