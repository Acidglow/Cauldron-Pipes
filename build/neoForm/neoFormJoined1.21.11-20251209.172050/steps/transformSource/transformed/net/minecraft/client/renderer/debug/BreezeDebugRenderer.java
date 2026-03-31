package net.minecraft.client.renderer.debug;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.core.BlockPos;
import net.minecraft.gizmos.GizmoStyle;
import net.minecraft.gizmos.Gizmos;
import net.minecraft.util.ARGB;
import net.minecraft.util.debug.DebugBreezeInfo;
import net.minecraft.util.debug.DebugSubscriptions;
import net.minecraft.util.debug.DebugValueAccess;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class BreezeDebugRenderer implements DebugRenderer.SimpleDebugRenderer {
    private static final int JUMP_TARGET_LINE_COLOR = ARGB.color(255, 255, 100, 255);
    private static final int TARGET_LINE_COLOR = ARGB.color(255, 100, 255, 255);
    private static final int INNER_CIRCLE_COLOR = ARGB.color(255, 0, 255, 0);
    private static final int MIDDLE_CIRCLE_COLOR = ARGB.color(255, 255, 165, 0);
    private static final int OUTER_CIRCLE_COLOR = ARGB.color(255, 255, 0, 0);
    private final Minecraft minecraft;

    public BreezeDebugRenderer(Minecraft minecraft) {
        this.minecraft = minecraft;
    }

    @Override
    public void emitGizmos(double p_456288_, double p_456002_, double p_455897_, DebugValueAccess p_455064_, Frustum p_455599_, float p_455650_) {
        ClientLevel clientlevel = this.minecraft.level;
        p_455064_.forEachEntity(
            DebugSubscriptions.BREEZES,
            (p_454284_, p_454285_) -> {
                p_454285_.attackTarget()
                    .map(clientlevel::getEntity)
                    .map(p_359255_ -> p_359255_.getPosition(this.minecraft.getDeltaTracker().getGameTimeDeltaPartialTick(true)))
                    .ifPresent(p_454287_ -> {
                        Gizmos.arrow(p_454284_.position(), p_454287_, TARGET_LINE_COLOR);
                        Vec3 vec3 = p_454287_.add(0.0, 0.01F, 0.0);
                        Gizmos.circle(vec3, 4.0F, GizmoStyle.stroke(INNER_CIRCLE_COLOR));
                        Gizmos.circle(vec3, 8.0F, GizmoStyle.stroke(MIDDLE_CIRCLE_COLOR));
                        Gizmos.circle(vec3, 24.0F, GizmoStyle.stroke(OUTER_CIRCLE_COLOR));
                    });
                p_454285_.jumpTarget().ifPresent(p_454289_ -> {
                    Gizmos.arrow(p_454284_.position(), p_454289_.getCenter(), JUMP_TARGET_LINE_COLOR);
                    Gizmos.cuboid(AABB.unitCubeFromLowerCorner(Vec3.atLowerCornerOf(p_454289_)), GizmoStyle.fill(ARGB.colorFromFloat(1.0F, 1.0F, 0.0F, 0.0F)));
                });
            }
        );
    }
}
