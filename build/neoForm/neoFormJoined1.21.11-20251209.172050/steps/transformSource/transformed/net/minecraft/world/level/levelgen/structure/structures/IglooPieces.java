package net.minecraft.world.level.levelgen.structure.structures;

import com.google.common.collect.ImmutableMap;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.Identifier;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.StructurePieceAccessor;
import net.minecraft.world.level.levelgen.structure.TemplateStructurePiece;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceType;
import net.minecraft.world.level.levelgen.structure.templatesystem.BlockIgnoreProcessor;
import net.minecraft.world.level.levelgen.structure.templatesystem.LiquidSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplateManager;
import net.minecraft.world.level.storage.loot.BuiltInLootTables;

public class IglooPieces {
    public static final int GENERATION_HEIGHT = 90;
    static final Identifier STRUCTURE_LOCATION_IGLOO = Identifier.withDefaultNamespace("igloo/top");
    private static final Identifier STRUCTURE_LOCATION_LADDER = Identifier.withDefaultNamespace("igloo/middle");
    private static final Identifier STRUCTURE_LOCATION_LABORATORY = Identifier.withDefaultNamespace("igloo/bottom");
    static final Map<Identifier, BlockPos> PIVOTS = ImmutableMap.of(
        STRUCTURE_LOCATION_IGLOO, new BlockPos(3, 5, 5), STRUCTURE_LOCATION_LADDER, new BlockPos(1, 3, 1), STRUCTURE_LOCATION_LABORATORY, new BlockPos(3, 6, 7)
    );
    static final Map<Identifier, BlockPos> OFFSETS = ImmutableMap.of(
        STRUCTURE_LOCATION_IGLOO, BlockPos.ZERO, STRUCTURE_LOCATION_LADDER, new BlockPos(2, -3, 4), STRUCTURE_LOCATION_LABORATORY, new BlockPos(0, -3, -2)
    );

    public static void addPieces(
        StructureTemplateManager structureTemplateManager, BlockPos startPos, Rotation rotation, StructurePieceAccessor pieces, RandomSource random
    ) {
        if (random.nextDouble() < 0.5) {
            int i = random.nextInt(8) + 4;
            pieces.addPiece(new IglooPieces.IglooPiece(structureTemplateManager, STRUCTURE_LOCATION_LABORATORY, startPos, rotation, i * 3));

            for (int j = 0; j < i - 1; j++) {
                pieces.addPiece(new IglooPieces.IglooPiece(structureTemplateManager, STRUCTURE_LOCATION_LADDER, startPos, rotation, j * 3));
            }
        }

        pieces.addPiece(new IglooPieces.IglooPiece(structureTemplateManager, STRUCTURE_LOCATION_IGLOO, startPos, rotation, 0));
    }

    public static class IglooPiece extends TemplateStructurePiece {
        public IglooPiece(StructureTemplateManager structureTemplateManager, Identifier location, BlockPos startPos, Rotation rotation, int down) {
            super(
                StructurePieceType.IGLOO,
                0,
                structureTemplateManager,
                location,
                location.toString(),
                makeSettings(rotation, location),
                makePosition(location, startPos, down)
            );
        }

        public IglooPiece(StructureTemplateManager structureTemplateManager, CompoundTag tag) {
            super(
                StructurePieceType.IGLOO,
                tag,
                structureTemplateManager,
                p_466748_ -> makeSettings(tag.read("Rot", Rotation.LEGACY_CODEC).orElseThrow(), p_466748_)
            );
        }

        private static StructurePlaceSettings makeSettings(Rotation rotation, Identifier location) {
            return new StructurePlaceSettings()
                .setRotation(rotation)
                .setMirror(Mirror.NONE)
                .setRotationPivot(IglooPieces.PIVOTS.get(location))
                .addProcessor(BlockIgnoreProcessor.STRUCTURE_BLOCK)
                .setLiquidSettings(LiquidSettings.IGNORE_WATERLOGGING);
        }

        private static BlockPos makePosition(Identifier location, BlockPos pos, int down) {
            return pos.offset(IglooPieces.OFFSETS.get(location)).below(down);
        }

        @Override
        protected void addAdditionalSaveData(StructurePieceSerializationContext p_227579_, CompoundTag p_227580_) {
            super.addAdditionalSaveData(p_227579_, p_227580_);
            p_227580_.store("Rot", Rotation.LEGACY_CODEC, this.placeSettings.getRotation());
        }

        @Override
        protected void handleDataMarker(String p_227582_, BlockPos p_227583_, ServerLevelAccessor p_227584_, RandomSource p_227585_, BoundingBox p_227586_) {
            if ("chest".equals(p_227582_)) {
                p_227584_.setBlock(p_227583_, Blocks.AIR.defaultBlockState(), 3);
                BlockEntity blockentity = p_227584_.getBlockEntity(p_227583_.below());
                if (blockentity instanceof ChestBlockEntity) {
                    ((ChestBlockEntity)blockentity).setLootTable(BuiltInLootTables.IGLOO_CHEST, p_227585_.nextLong());
                }
            }
        }

        @Override
        public void postProcess(
            WorldGenLevel p_227568_,
            StructureManager p_227569_,
            ChunkGenerator p_227570_,
            RandomSource p_227571_,
            BoundingBox p_227572_,
            ChunkPos p_227573_,
            BlockPos p_227574_
        ) {
            Identifier identifier = Identifier.parse(this.templateName);
            StructurePlaceSettings structureplacesettings = makeSettings(this.placeSettings.getRotation(), identifier);
            BlockPos blockpos = IglooPieces.OFFSETS.get(identifier);
            BlockPos blockpos1 = this.templatePosition
                .offset(StructureTemplate.calculateRelativePosition(structureplacesettings, new BlockPos(3 - blockpos.getX(), 0, -blockpos.getZ())));
            int i = p_227568_.getHeight(Heightmap.Types.WORLD_SURFACE_WG, blockpos1.getX(), blockpos1.getZ());
            BlockPos blockpos2 = this.templatePosition;
            this.templatePosition = this.templatePosition.offset(0, i - 90 - 1, 0);
            super.postProcess(p_227568_, p_227569_, p_227570_, p_227571_, p_227572_, p_227573_, p_227574_);
            if (identifier.equals(IglooPieces.STRUCTURE_LOCATION_IGLOO)) {
                BlockPos blockpos3 = this.templatePosition.offset(StructureTemplate.calculateRelativePosition(structureplacesettings, new BlockPos(3, 0, 5)));
                BlockState blockstate = p_227568_.getBlockState(blockpos3.below());
                if (!blockstate.isAir() && !blockstate.is(Blocks.LADDER)) {
                    p_227568_.setBlock(blockpos3, Blocks.SNOW_BLOCK.defaultBlockState(), 3);
                }
            }

            this.templatePosition = blockpos2;
        }
    }
}
