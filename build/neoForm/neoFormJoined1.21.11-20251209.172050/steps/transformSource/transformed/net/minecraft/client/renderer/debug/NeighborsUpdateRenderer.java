package net.minecraft.client.renderer.debug;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.core.BlockPos;
import net.minecraft.gizmos.GizmoStyle;
import net.minecraft.gizmos.Gizmos;
import net.minecraft.gizmos.TextGizmo;
import net.minecraft.util.debug.DebugSubscriptions;
import net.minecraft.util.debug.DebugValueAccess;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class NeighborsUpdateRenderer implements DebugRenderer.SimpleDebugRenderer {
    @Override
    public void emitGizmos(double p_455978_, double p_455486_, double p_455773_, DebugValueAccess p_455134_, Frustum p_455998_, float p_455747_) {
        int i = DebugSubscriptions.NEIGHBOR_UPDATES.expireAfterTicks();
        double d0 = 1.0 / (i * 2);
        Map<BlockPos, NeighborsUpdateRenderer.LastUpdate> map = new HashMap<>();
        p_455134_.forEachEvent(DebugSubscriptions.NEIGHBOR_UPDATES, (p_448797_, p_448798_, p_448799_) -> {
            long j = p_448799_ - p_448798_;
            NeighborsUpdateRenderer.LastUpdate neighborsupdaterenderer$lastupdate2 = map.getOrDefault(p_448797_, NeighborsUpdateRenderer.LastUpdate.NONE);
            map.put(p_448797_, neighborsupdaterenderer$lastupdate2.tryCount((int)j));
        });

        for (Entry<BlockPos, NeighborsUpdateRenderer.LastUpdate> entry : map.entrySet()) {
            BlockPos blockpos = entry.getKey();
            NeighborsUpdateRenderer.LastUpdate neighborsupdaterenderer$lastupdate = entry.getValue();
            AABB aabb = new AABB(blockpos).inflate(0.002).deflate(d0 * neighborsupdaterenderer$lastupdate.age);
            Gizmos.cuboid(aabb, GizmoStyle.stroke(-1));
        }

        for (Entry<BlockPos, NeighborsUpdateRenderer.LastUpdate> entry1 : map.entrySet()) {
            BlockPos blockpos1 = entry1.getKey();
            NeighborsUpdateRenderer.LastUpdate neighborsupdaterenderer$lastupdate1 = entry1.getValue();
            Gizmos.billboardText(String.valueOf(neighborsupdaterenderer$lastupdate1.count), Vec3.atCenterOf(blockpos1), TextGizmo.Style.whiteAndCentered());
        }
    }

    @OnlyIn(Dist.CLIENT)
    record LastUpdate(int count, int age) {
        static final NeighborsUpdateRenderer.LastUpdate NONE = new NeighborsUpdateRenderer.LastUpdate(0, Integer.MAX_VALUE);

        public NeighborsUpdateRenderer.LastUpdate tryCount(int age) {
            if (age == this.age) {
                return new NeighborsUpdateRenderer.LastUpdate(this.count + 1, age);
            } else {
                return age < this.age ? new NeighborsUpdateRenderer.LastUpdate(1, age) : this;
            }
        }
    }
}
