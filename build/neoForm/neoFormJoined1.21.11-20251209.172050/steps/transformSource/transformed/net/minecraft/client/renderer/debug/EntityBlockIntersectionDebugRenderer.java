package net.minecraft.client.renderer.debug;

import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.core.BlockPos;
import net.minecraft.gizmos.GizmoStyle;
import net.minecraft.gizmos.Gizmos;
import net.minecraft.util.debug.DebugEntityBlockIntersection;
import net.minecraft.util.debug.DebugSubscriptions;
import net.minecraft.util.debug.DebugValueAccess;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class EntityBlockIntersectionDebugRenderer implements DebugRenderer.SimpleDebugRenderer {
    private static final float PADDING = 0.02F;

    @Override
    public void emitGizmos(double p_456194_, double p_455783_, double p_455314_, DebugValueAccess p_454762_, Frustum p_455159_, float p_455236_) {
        p_454762_.forEachBlock(
            DebugSubscriptions.ENTITY_BLOCK_INTERSECTIONS, (p_454290_, p_454291_) -> Gizmos.cuboid(p_454290_, 0.02F, GizmoStyle.fill(p_454291_.color()))
        );
    }
}
