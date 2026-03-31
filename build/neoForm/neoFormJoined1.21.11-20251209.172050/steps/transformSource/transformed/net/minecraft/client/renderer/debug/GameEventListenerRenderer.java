package net.minecraft.client.renderer.debug;

import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.core.BlockPos;
import net.minecraft.gizmos.GizmoStyle;
import net.minecraft.gizmos.Gizmos;
import net.minecraft.gizmos.TextGizmo;
import net.minecraft.util.ARGB;
import net.minecraft.util.debug.DebugGameEventInfo;
import net.minecraft.util.debug.DebugGameEventListenerInfo;
import net.minecraft.util.debug.DebugSubscriptions;
import net.minecraft.util.debug.DebugValueAccess;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class GameEventListenerRenderer implements DebugRenderer.SimpleDebugRenderer {
    private static final float BOX_HEIGHT = 1.0F;

    private void forEachListener(DebugValueAccess debugValueAccess, GameEventListenerRenderer.ListenerVisitor action) {
        debugValueAccess.forEachBlock(
            DebugSubscriptions.GAME_EVENT_LISTENERS, (p_448791_, p_448792_) -> action.accept(p_448791_.getCenter(), p_448792_.listenerRadius())
        );
        debugValueAccess.forEachEntity(
            DebugSubscriptions.GAME_EVENT_LISTENERS, (p_448794_, p_448795_) -> action.accept(p_448794_.position(), p_448795_.listenerRadius())
        );
    }

    @Override
    public void emitGizmos(double p_455631_, double p_456020_, double p_455608_, DebugValueAccess p_455370_, Frustum p_455164_, float p_456266_) {
        this.forEachListener(p_455370_, (p_454294_, p_454295_) -> {
            double d0 = p_454295_ * 2.0;
            Gizmos.cuboid(AABB.ofSize(p_454294_, d0, d0, d0), GizmoStyle.fill(ARGB.colorFromFloat(0.35F, 1.0F, 1.0F, 0.0F)));
        });
        this.forEachListener(
            p_455370_,
            (p_454296_, p_454297_) -> Gizmos.cuboid(
                AABB.ofSize(p_454296_, 0.5, 1.0, 0.5).move(0.0, 0.5, 0.0), GizmoStyle.fill(ARGB.colorFromFloat(0.35F, 1.0F, 1.0F, 0.0F))
            )
        );
        this.forEachListener(
            p_455370_,
            (p_454292_, p_454293_) -> {
                Gizmos.billboardText("Listener Origin", p_454292_.add(0.0, 1.8, 0.0), TextGizmo.Style.whiteAndCentered().withScale(0.4F));
                Gizmos.billboardText(
                    BlockPos.containing(p_454292_).toString(), p_454292_.add(0.0, 1.5, 0.0), TextGizmo.Style.forColorAndCentered(-6959665).withScale(0.4F)
                );
            }
        );
        p_455370_.forEachEvent(
            DebugSubscriptions.GAME_EVENTS,
            (p_454298_, p_454299_, p_454300_) -> {
                Vec3 vec3 = p_454298_.pos();
                double d0 = 0.4;
                AABB aabb = AABB.ofSize(vec3.add(0.0, 0.5, 0.0), 0.4, 0.9, 0.4);
                Gizmos.cuboid(aabb, GizmoStyle.fill(ARGB.colorFromFloat(0.2F, 1.0F, 1.0F, 1.0F)));
                Gizmos.billboardText(
                    p_454298_.event().getRegisteredName(), vec3.add(0.0, 0.85, 0.0), TextGizmo.Style.forColorAndCentered(-7564911).withScale(0.12F)
                );
            }
        );
    }

    @FunctionalInterface
    @OnlyIn(Dist.CLIENT)
    interface ListenerVisitor {
        void accept(Vec3 pos, int radius);
    }
}
