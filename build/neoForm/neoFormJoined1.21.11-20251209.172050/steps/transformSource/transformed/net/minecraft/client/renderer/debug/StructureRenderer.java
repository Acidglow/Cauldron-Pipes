package net.minecraft.client.renderer.debug;

import java.util.List;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.gizmos.GizmoStyle;
import net.minecraft.gizmos.Gizmos;
import net.minecraft.util.ARGB;
import net.minecraft.util.debug.DebugStructureInfo;
import net.minecraft.util.debug.DebugSubscriptions;
import net.minecraft.util.debug.DebugValueAccess;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.phys.AABB;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class StructureRenderer implements DebugRenderer.SimpleDebugRenderer {
    @Override
    public void emitGizmos(double p_454703_, double p_455336_, double p_455231_, DebugValueAccess p_455946_, Frustum p_454656_, float p_455738_) {
        p_455946_.forEachChunk(DebugSubscriptions.STRUCTURES, (p_454348_, p_454349_) -> {
            for (DebugStructureInfo debugstructureinfo : p_454349_) {
                Gizmos.cuboid(AABB.of(debugstructureinfo.boundingBox()), GizmoStyle.stroke(ARGB.colorFromFloat(1.0F, 1.0F, 1.0F, 1.0F)));

                for (DebugStructureInfo.Piece debugstructureinfo$piece : debugstructureinfo.pieces()) {
                    if (debugstructureinfo$piece.isStart()) {
                        Gizmos.cuboid(AABB.of(debugstructureinfo$piece.boundingBox()), GizmoStyle.stroke(ARGB.colorFromFloat(1.0F, 0.0F, 1.0F, 0.0F)));
                    } else {
                        Gizmos.cuboid(AABB.of(debugstructureinfo$piece.boundingBox()), GizmoStyle.stroke(ARGB.colorFromFloat(1.0F, 0.0F, 0.0F, 1.0F)));
                    }
                }
            }
        });
    }
}
