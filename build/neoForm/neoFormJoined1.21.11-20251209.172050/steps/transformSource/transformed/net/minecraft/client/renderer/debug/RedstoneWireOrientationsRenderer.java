package net.minecraft.client.renderer.debug;

import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.core.BlockPos;
import net.minecraft.gizmos.Gizmos;
import net.minecraft.util.debug.DebugSubscriptions;
import net.minecraft.util.debug.DebugValueAccess;
import net.minecraft.world.level.redstone.Orientation;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class RedstoneWireOrientationsRenderer implements DebugRenderer.SimpleDebugRenderer {
    @Override
    public void emitGizmos(double p_455927_, double p_455619_, double p_455110_, DebugValueAccess p_454840_, Frustum p_455155_, float p_455098_) {
        p_454840_.forEachBlock(DebugSubscriptions.REDSTONE_WIRE_ORIENTATIONS, (p_454346_, p_454347_) -> {
            Vec3 vec3 = p_454346_.getBottomCenter().subtract(0.0, 0.1, 0.0);
            Gizmos.arrow(vec3, vec3.add(p_454347_.getFront().getUnitVec3().scale(0.5)), -16776961);
            Gizmos.arrow(vec3, vec3.add(p_454347_.getUp().getUnitVec3().scale(0.4)), -65536);
            Gizmos.arrow(vec3, vec3.add(p_454347_.getSide().getUnitVec3().scale(0.3)), -256);
        });
    }
}
