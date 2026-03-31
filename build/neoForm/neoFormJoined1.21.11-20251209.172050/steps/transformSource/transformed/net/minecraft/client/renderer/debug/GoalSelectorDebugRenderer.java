package net.minecraft.client.renderer.debug;

import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.core.BlockPos;
import net.minecraft.gizmos.Gizmos;
import net.minecraft.gizmos.TextGizmo;
import net.minecraft.util.debug.DebugGoalInfo;
import net.minecraft.util.debug.DebugSubscriptions;
import net.minecraft.util.debug.DebugValueAccess;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class GoalSelectorDebugRenderer implements DebugRenderer.SimpleDebugRenderer {
    private static final int MAX_RENDER_DIST = 160;
    private final Minecraft minecraft;

    public GoalSelectorDebugRenderer(Minecraft minecraft) {
        this.minecraft = minecraft;
    }

    @Override
    public void emitGizmos(double p_454809_, double p_454816_, double p_454812_, DebugValueAccess p_456063_, Frustum p_456106_, float p_455915_) {
        Camera camera = this.minecraft.gameRenderer.getMainCamera();
        BlockPos blockpos = BlockPos.containing(camera.position().x, 0.0, camera.position().z);
        p_456063_.forEachEntity(DebugSubscriptions.GOAL_SELECTORS, (p_454304_, p_454305_) -> {
            if (blockpos.closerThan(p_454304_.blockPosition(), 160.0)) {
                for (int i = 0; i < p_454305_.goals().size(); i++) {
                    DebugGoalInfo.DebugGoal debuggoalinfo$debuggoal = p_454305_.goals().get(i);
                    double d0 = p_454304_.getBlockX() + 0.5;
                    double d1 = p_454304_.getY() + 2.0 + i * 0.25;
                    double d2 = p_454304_.getBlockZ() + 0.5;
                    int j = debuggoalinfo$debuggoal.isRunning() ? -16711936 : -3355444;
                    Gizmos.billboardText(debuggoalinfo$debuggoal.name(), new Vec3(d0, d1, d2), TextGizmo.Style.forColorAndCentered(j));
                }
            }
        });
    }
}
