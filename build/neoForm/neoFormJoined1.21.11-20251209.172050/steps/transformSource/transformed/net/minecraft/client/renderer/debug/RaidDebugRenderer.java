package net.minecraft.client.renderer.debug;

import java.util.List;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.core.BlockPos;
import net.minecraft.gizmos.GizmoStyle;
import net.minecraft.gizmos.Gizmos;
import net.minecraft.gizmos.TextGizmo;
import net.minecraft.util.ARGB;
import net.minecraft.util.debug.DebugSubscriptions;
import net.minecraft.util.debug.DebugValueAccess;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class RaidDebugRenderer implements DebugRenderer.SimpleDebugRenderer {
    private static final int MAX_RENDER_DIST = 160;
    private static final float TEXT_SCALE = 0.64F;
    private final Minecraft minecraft;

    public RaidDebugRenderer(Minecraft minecraft) {
        this.minecraft = minecraft;
    }

    @Override
    public void emitGizmos(double p_454918_, double p_454978_, double p_456119_, DebugValueAccess p_454948_, Frustum p_456248_, float p_455724_) {
        BlockPos blockpos = this.getCamera().blockPosition();
        p_454948_.forEachChunk(DebugSubscriptions.RAIDS, (p_454344_, p_454345_) -> {
            for (BlockPos blockpos1 : p_454345_) {
                if (blockpos.closerThan(blockpos1, 160.0)) {
                    highlightRaidCenter(blockpos1);
                }
            }
        });
    }

    private static void highlightRaidCenter(BlockPos pos) {
        Gizmos.cuboid(pos, GizmoStyle.fill(ARGB.colorFromFloat(0.15F, 1.0F, 0.0F, 0.0F)));
        renderTextOverBlock("Raid center", pos, -65536);
    }

    private static void renderTextOverBlock(String text, BlockPos pos, int color) {
        Gizmos.billboardText(text, Vec3.atLowerCornerWithOffset(pos, 0.5, 1.3, 0.5), TextGizmo.Style.forColor(color).withScale(0.64F))
            .setAlwaysOnTop();
    }

    private Camera getCamera() {
        return this.minecraft.gameRenderer.getMainCamera();
    }
}
