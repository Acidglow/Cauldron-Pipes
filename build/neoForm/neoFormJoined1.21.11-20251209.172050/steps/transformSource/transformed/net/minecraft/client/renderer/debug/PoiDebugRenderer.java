package net.minecraft.client.renderer.debug;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.SharedConstants;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.core.BlockPos;
import net.minecraft.gizmos.GizmoStyle;
import net.minecraft.gizmos.Gizmos;
import net.minecraft.network.protocol.game.DebugEntityNameGenerator;
import net.minecraft.util.ARGB;
import net.minecraft.util.debug.DebugBrainDump;
import net.minecraft.util.debug.DebugPoiInfo;
import net.minecraft.util.debug.DebugSubscriptions;
import net.minecraft.util.debug.DebugValueAccess;
import net.minecraft.world.entity.Entity;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class PoiDebugRenderer implements DebugRenderer.SimpleDebugRenderer {
    private static final int MAX_RENDER_DIST_FOR_POI_INFO = 30;
    private static final float TEXT_SCALE = 0.32F;
    private static final int ORANGE = -23296;
    private final BrainDebugRenderer brainRenderer;

    public PoiDebugRenderer(BrainDebugRenderer brainRenderer) {
        this.brainRenderer = brainRenderer;
    }

    @Override
    public void emitGizmos(double p_455929_, double p_455458_, double p_456088_, DebugValueAccess p_455495_, Frustum p_455636_, float p_454695_) {
        BlockPos blockpos = BlockPos.containing(p_455929_, p_455458_, p_456088_);
        p_455495_.forEachBlock(DebugSubscriptions.POIS, (p_454341_, p_454342_) -> {
            if (blockpos.closerThan(p_454341_, 30.0)) {
                highlightPoi(p_454341_);
                this.renderPoiInfo(p_454342_, p_455495_);
            }
        });
        this.brainRenderer.getGhostPois(p_455495_).forEach((p_454337_, p_454338_) -> {
            if (p_455495_.getBlockValue(DebugSubscriptions.POIS, p_454337_) == null) {
                if (blockpos.closerThan(p_454337_, 30.0)) {
                    this.renderGhostPoi(p_454337_, (List<String>)p_454338_);
                }
            }
        });
    }

    private static void highlightPoi(BlockPos pos) {
        float f = 0.05F;
        Gizmos.cuboid(pos, 0.05F, GizmoStyle.fill(ARGB.colorFromFloat(0.3F, 0.2F, 0.2F, 1.0F)));
    }

    private void renderGhostPoi(BlockPos pos, List<String> entities) {
        float f = 0.05F;
        Gizmos.cuboid(pos, 0.05F, GizmoStyle.fill(ARGB.colorFromFloat(0.3F, 0.2F, 0.2F, 1.0F)));
        Gizmos.billboardTextOverBlock(entities.toString(), pos, 0, -256, 0.32F);
        Gizmos.billboardTextOverBlock("Ghost POI", pos, 1, -65536, 0.32F);
    }

    private void renderPoiInfo(DebugPoiInfo poiInfo, DebugValueAccess debugValueAccess) {
        int i = 0;
        if (SharedConstants.DEBUG_BRAIN) {
            List<String> list = this.getTicketHolderNames(poiInfo, false, debugValueAccess);
            if (list.size() < 4) {
                renderTextOverPoi("Owners: " + list, poiInfo, i, -256);
            } else {
                renderTextOverPoi(list.size() + " ticket holders", poiInfo, i, -256);
            }

            i++;
            List<String> list1 = this.getTicketHolderNames(poiInfo, true, debugValueAccess);
            if (list1.size() < 4) {
                renderTextOverPoi("Candidates: " + list1, poiInfo, i, -23296);
            } else {
                renderTextOverPoi(list1.size() + " potential owners", poiInfo, i, -23296);
            }

            i++;
        }

        renderTextOverPoi("Free tickets: " + poiInfo.freeTicketCount(), poiInfo, i, -256);
        renderTextOverPoi(poiInfo.poiType().getRegisteredName(), poiInfo, ++i, -1);
    }

    private static void renderTextOverPoi(String text, DebugPoiInfo poiInfo, int line, int color) {
        Gizmos.billboardTextOverBlock(text, poiInfo.pos(), line, color, 0.32F);
    }

    private List<String> getTicketHolderNames(DebugPoiInfo poiInfo, boolean potential, DebugValueAccess debugValueAccess) {
        List<String> list = new ArrayList<>();
        debugValueAccess.forEachEntity(DebugSubscriptions.BRAINS, (p_449314_, p_449925_) -> {
            boolean flag = potential ? p_449925_.hasPotentialPoi(poiInfo.pos()) : p_449925_.hasPoi(poiInfo.pos());
            if (flag) {
                list.add(DebugEntityNameGenerator.getEntityName(p_449314_.getUUID()));
            }
        });
        return list;
    }
}
