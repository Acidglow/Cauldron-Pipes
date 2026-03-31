package net.minecraft.client.renderer.debug;

import com.google.common.collect.Maps;
import java.util.Map;
import java.util.Map.Entry;
import net.minecraft.core.BlockPos;
import net.minecraft.gizmos.GizmoStyle;
import net.minecraft.gizmos.Gizmos;
import net.minecraft.gizmos.TextGizmo;
import net.minecraft.util.Util;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class GameTestBlockHighlightRenderer {
    private static final int SHOW_POS_DURATION_MS = 10000;
    private static final float PADDING = 0.02F;
    private final Map<BlockPos, GameTestBlockHighlightRenderer.Marker> markers = Maps.newHashMap();

    public void highlightPos(BlockPos absolutePos, BlockPos relativePos) {
        String s = relativePos.toShortString();
        this.markers.put(absolutePos, new GameTestBlockHighlightRenderer.Marker(1610678016, s, Util.getMillis() + 10000L));
    }

    public void clear() {
        this.markers.clear();
    }

    public void emitGizmos() {
        long i = Util.getMillis();
        this.markers.entrySet().removeIf(p_451551_ -> i > p_451551_.getValue().removeAtTime);
        this.markers.forEach((p_454301_, p_454302_) -> this.renderMarker(p_454301_, p_454302_));
    }

    private void renderMarker(BlockPos pos, GameTestBlockHighlightRenderer.Marker marker) {
        Gizmos.cuboid(pos, 0.02F, GizmoStyle.fill(marker.color()));
        if (!marker.text.isEmpty()) {
            Gizmos.billboardText(marker.text, Vec3.atLowerCornerWithOffset(pos, 0.5, 1.2, 0.5), TextGizmo.Style.whiteAndCentered().withScale(0.16F))
                .setAlwaysOnTop();
        }
    }

    @OnlyIn(Dist.CLIENT)
    record Marker(int color, String text, long removeAtTime) {
    }
}
