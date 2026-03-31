package net.minecraft.world.level.levelgen.structure.structures;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.Identifier;
import net.minecraft.util.RandomSource;
import net.minecraft.util.Util;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.StructurePieceAccessor;
import net.minecraft.world.level.levelgen.structure.TemplateStructurePiece;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceType;
import net.minecraft.world.level.levelgen.structure.templatesystem.BlockIgnoreProcessor;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplateManager;

public class NetherFossilPieces {
    private static final Identifier[] FOSSILS = new Identifier[]{
        Identifier.withDefaultNamespace("nether_fossils/fossil_1"),
        Identifier.withDefaultNamespace("nether_fossils/fossil_2"),
        Identifier.withDefaultNamespace("nether_fossils/fossil_3"),
        Identifier.withDefaultNamespace("nether_fossils/fossil_4"),
        Identifier.withDefaultNamespace("nether_fossils/fossil_5"),
        Identifier.withDefaultNamespace("nether_fossils/fossil_6"),
        Identifier.withDefaultNamespace("nether_fossils/fossil_7"),
        Identifier.withDefaultNamespace("nether_fossils/fossil_8"),
        Identifier.withDefaultNamespace("nether_fossils/fossil_9"),
        Identifier.withDefaultNamespace("nether_fossils/fossil_10"),
        Identifier.withDefaultNamespace("nether_fossils/fossil_11"),
        Identifier.withDefaultNamespace("nether_fossils/fossil_12"),
        Identifier.withDefaultNamespace("nether_fossils/fossil_13"),
        Identifier.withDefaultNamespace("nether_fossils/fossil_14")
    };

    public static void addPieces(StructureTemplateManager structureManager, StructurePieceAccessor pieces, RandomSource random, BlockPos pos) {
        Rotation rotation = Rotation.getRandom(random);
        pieces.addPiece(new NetherFossilPieces.NetherFossilPiece(structureManager, Util.getRandom(FOSSILS, random), pos, rotation));
    }

    public static class NetherFossilPiece extends TemplateStructurePiece {
        public NetherFossilPiece(StructureTemplateManager structureManager, Identifier location, BlockPos pos, Rotation rotation) {
            super(StructurePieceType.NETHER_FOSSIL, 0, structureManager, location, location.toString(), makeSettings(rotation), pos);
        }

        public NetherFossilPiece(StructureTemplateManager structureManager, CompoundTag tag) {
            super(StructurePieceType.NETHER_FOSSIL, tag, structureManager, p_468813_ -> makeSettings(tag.read("Rot", Rotation.LEGACY_CODEC).orElseThrow()));
        }

        private static StructurePlaceSettings makeSettings(Rotation rotation) {
            return new StructurePlaceSettings().setRotation(rotation).setMirror(Mirror.NONE).addProcessor(BlockIgnoreProcessor.STRUCTURE_AND_AIR);
        }

        @Override
        protected void addAdditionalSaveData(StructurePieceSerializationContext p_228558_, CompoundTag p_228559_) {
            super.addAdditionalSaveData(p_228558_, p_228559_);
            p_228559_.store("Rot", Rotation.LEGACY_CODEC, this.placeSettings.getRotation());
        }

        @Override
        protected void handleDataMarker(String p_228561_, BlockPos p_228562_, ServerLevelAccessor p_228563_, RandomSource p_228564_, BoundingBox p_228565_) {
        }

        @Override
        public void postProcess(
            WorldGenLevel p_228548_,
            StructureManager p_228549_,
            ChunkGenerator p_228550_,
            RandomSource p_228551_,
            BoundingBox p_228552_,
            ChunkPos p_228553_,
            BlockPos p_228554_
        ) {
            BoundingBox boundingbox = this.template.getBoundingBox(this.placeSettings, this.templatePosition);
            p_228552_.encapsulate(boundingbox);
            super.postProcess(p_228548_, p_228549_, p_228550_, p_228551_, p_228552_, p_228553_, p_228554_);
            this.placeDriedGhast(p_228548_, p_228551_, boundingbox, p_228552_);
        }

        private void placeDriedGhast(WorldGenLevel level, RandomSource random, BoundingBox templateBox, BoundingBox box) {
            RandomSource randomsource = RandomSource.create(level.getSeed()).forkPositional().at(templateBox.getCenter());
            if (randomsource.nextFloat() < 0.5F) {
                int i = templateBox.minX() + randomsource.nextInt(templateBox.getXSpan());
                int j = templateBox.minY();
                int k = templateBox.minZ() + randomsource.nextInt(templateBox.getZSpan());
                BlockPos blockpos = new BlockPos(i, j, k);
                if (level.getBlockState(blockpos).isAir() && box.isInside(blockpos)) {
                    level.setBlock(blockpos, Blocks.DRIED_GHAST.defaultBlockState().rotate(Rotation.getRandom(randomsource)), 2);
                }
            }
        }
    }
}
