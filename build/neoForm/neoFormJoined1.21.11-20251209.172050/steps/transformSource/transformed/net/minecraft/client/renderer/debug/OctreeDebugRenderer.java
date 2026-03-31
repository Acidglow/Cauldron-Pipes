package net.minecraft.client.renderer.debug;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.Octree;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.gizmos.GizmoStyle;
import net.minecraft.gizmos.Gizmos;
import net.minecraft.gizmos.TextGizmo;
import net.minecraft.util.ARGB;
import net.minecraft.util.Mth;
import net.minecraft.util.debug.DebugValueAccess;
import net.minecraft.world.phys.AABB;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.apache.commons.lang3.mutable.MutableInt;

@OnlyIn(Dist.CLIENT)
public class OctreeDebugRenderer implements DebugRenderer.SimpleDebugRenderer {
    private final Minecraft minecraft;

    public OctreeDebugRenderer(Minecraft minecraft) {
        this.minecraft = minecraft;
    }

    @Override
    public void emitGizmos(double p_455660_, double p_456021_, double p_455019_, DebugValueAccess p_455038_, Frustum p_456165_, float p_456085_) {
        Octree octree = this.minecraft.levelRenderer.getSectionOcclusionGraph().getOctree();
        MutableInt mutableint = new MutableInt(0);
        octree.visitNodes(
            (p_454321_, p_454322_, p_454323_, p_454324_) -> this.renderNode(p_454321_, p_454323_, p_454322_, mutableint, p_454324_), p_456165_, 32
        );
    }

    private void renderNode(Octree.Node node, int recursionDepth, boolean isLeafNode, MutableInt nodesRendered, boolean isNearby) {
        AABB aabb = node.getAABB();
        double d0 = aabb.getXsize();
        long i = Math.round(d0 / 16.0);
        if (i == 1L) {
            nodesRendered.add(1);
            int j = isNearby ? -16711936 : -1;
            Gizmos.billboardText(String.valueOf(nodesRendered.intValue()), aabb.getCenter(), TextGizmo.Style.forColorAndCentered(j).withScale(4.8F));
        }

        long k = i + 5L;
        Gizmos.cuboid(
            aabb.deflate(0.1 * recursionDepth),
            GizmoStyle.stroke(ARGB.colorFromFloat(isLeafNode ? 0.4F : 1.0F, getColorComponent(k, 0.3F), getColorComponent(k, 0.8F), getColorComponent(k, 0.5F)))
        );
    }

    private static float getColorComponent(long value, float multiplier) {
        float f = 0.1F;
        return Mth.frac(multiplier * (float)value) * 0.9F + 0.1F;
    }
}
