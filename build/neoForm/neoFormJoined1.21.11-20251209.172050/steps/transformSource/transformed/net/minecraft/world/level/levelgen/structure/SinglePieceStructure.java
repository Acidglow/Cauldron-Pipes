package net.minecraft.world.level.levelgen.structure;

import java.util.Optional;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.WorldgenRandom;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePiecesBuilder;

public abstract class SinglePieceStructure extends Structure {
    private final SinglePieceStructure.PieceConstructor constructor;
    private final int width;
    private final int depth;

    protected SinglePieceStructure(SinglePieceStructure.PieceConstructor constructor, int width, int depth, Structure.StructureSettings settings) {
        super(settings);
        this.constructor = constructor;
        this.width = width;
        this.depth = depth;
    }

    @Override
    public Optional<Structure.GenerationStub> findGenerationPoint(Structure.GenerationContext p_226542_) {
        return getLowestY(p_226542_, this.width, this.depth) < p_226542_.chunkGenerator().getSeaLevel()
            ? Optional.empty()
            : onTopOfChunkCenter(p_226542_, Heightmap.Types.WORLD_SURFACE_WG, p_226545_ -> this.generatePieces(p_226545_, p_226542_));
    }

    private void generatePieces(StructurePiecesBuilder builder, Structure.GenerationContext context) {
        ChunkPos chunkpos = context.chunkPos();
        builder.addPiece(this.constructor.construct(context.random(), chunkpos.getMinBlockX(), chunkpos.getMinBlockZ()));
    }

    @FunctionalInterface
    protected interface PieceConstructor {
        StructurePiece construct(WorldgenRandom random, int minBlockX, int minBlockZ);
    }
}
