package net.minecraft.world.level.levelgen.structure.structures;

import com.mojang.serialization.MapCodec;
import java.util.List;
import java.util.Optional;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.StructureType;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePiecesBuilder;

public class StrongholdStructure extends Structure {
    public static final MapCodec<StrongholdStructure> CODEC = simpleCodec(StrongholdStructure::new);

    public StrongholdStructure(Structure.StructureSettings p_229939_) {
        super(p_229939_);
    }

    @Override
    public Optional<Structure.GenerationStub> findGenerationPoint(Structure.GenerationContext p_229941_) {
        return Optional.of(new Structure.GenerationStub(p_229941_.chunkPos().getWorldPosition(), p_229944_ -> generatePieces(p_229944_, p_229941_)));
    }

    private static void generatePieces(StructurePiecesBuilder builder, Structure.GenerationContext context) {
        int i = 0;

        StrongholdPieces.StartPiece strongholdpieces$startpiece;
        do {
            builder.clear();
            context.random().setLargeFeatureSeed(context.seed() + i++, context.chunkPos().x, context.chunkPos().z);
            StrongholdPieces.resetPieces();
            strongholdpieces$startpiece = new StrongholdPieces.StartPiece(
                context.random(), context.chunkPos().getBlockX(2), context.chunkPos().getBlockZ(2)
            );
            builder.addPiece(strongholdpieces$startpiece);
            strongholdpieces$startpiece.addChildren(strongholdpieces$startpiece, builder, context.random());
            List<StructurePiece> list = strongholdpieces$startpiece.pendingChildren;

            while (!list.isEmpty()) {
                int j = context.random().nextInt(list.size());
                StructurePiece structurepiece = list.remove(j);
                structurepiece.addChildren(strongholdpieces$startpiece, builder, context.random());
            }

            builder.moveBelowSeaLevel(context.chunkGenerator().getSeaLevel(), context.chunkGenerator().getMinY(), context.random(), 10);
        } while (builder.isEmpty() || strongholdpieces$startpiece.portalRoomPiece == null);
    }

    @Override
    public StructureType<?> type() {
        return StructureType.STRONGHOLD;
    }
}
