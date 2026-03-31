package net.minecraft.client.renderer.debug;

import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.gizmos.GizmoStyle;
import net.minecraft.gizmos.Gizmos;
import net.minecraft.util.ARGB;
import net.minecraft.util.Unit;
import net.minecraft.util.debug.DebugSubscriptions;
import net.minecraft.util.debug.DebugValueAccess;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class VillageSectionsDebugRenderer implements DebugRenderer.SimpleDebugRenderer {
    @Override
    public void emitGizmos(double p_454777_, double p_454998_, double p_455194_, DebugValueAccess p_456230_, Frustum p_456270_, float p_454753_) {
        p_456230_.forEachBlock(DebugSubscriptions.VILLAGE_SECTIONS, (p_454354_, p_454355_) -> {
            SectionPos sectionpos = SectionPos.of(p_454354_);
            Gizmos.cuboid(sectionpos.center(), GizmoStyle.fill(ARGB.colorFromFloat(0.15F, 0.2F, 1.0F, 0.2F)));
        });
    }
}
