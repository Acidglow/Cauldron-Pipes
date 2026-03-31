package net.minecraft.client.renderer.debug;

import com.google.common.collect.ImmutableList;
import java.util.Collections;
import java.util.List;
import java.util.function.DoubleSupplier;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.core.BlockPos;
import net.minecraft.gizmos.GizmoStyle;
import net.minecraft.gizmos.Gizmos;
import net.minecraft.util.ARGB;
import net.minecraft.util.Util;
import net.minecraft.util.debug.DebugValueAccess;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class SupportBlockRenderer implements DebugRenderer.SimpleDebugRenderer {
    private final Minecraft minecraft;
    private double lastUpdateTime = Double.MIN_VALUE;
    private List<Entity> surroundEntities = Collections.emptyList();

    public SupportBlockRenderer(Minecraft minecraft) {
        this.minecraft = minecraft;
    }

    @Override
    public void emitGizmos(double p_455112_, double p_455359_, double p_455001_, DebugValueAccess p_454873_, Frustum p_456279_, float p_455493_) {
        double d0 = Util.getNanos();
        if (d0 - this.lastUpdateTime > 1.0E8) {
            this.lastUpdateTime = d0;
            Entity entity = this.minecraft.gameRenderer.getMainCamera().entity();
            this.surroundEntities = ImmutableList.copyOf(entity.level().getEntities(entity, entity.getBoundingBox().inflate(16.0)));
        }

        Player player = this.minecraft.player;
        if (player != null && player.mainSupportingBlockPos.isPresent()) {
            this.drawHighlights(player, () -> 0.0, -65536);
        }

        for (Entity entity1 : this.surroundEntities) {
            if (entity1 != player) {
                this.drawHighlights(entity1, () -> this.getBias(entity1), -16711936);
            }
        }
    }

    private void drawHighlights(Entity entity, DoubleSupplier biasGetter, int color) {
        entity.mainSupportingBlockPos.ifPresent(p_454353_ -> {
            double d0 = biasGetter.getAsDouble();
            BlockPos blockpos = entity.getOnPos();
            this.highlightPosition(blockpos, 0.02 + d0, color);
            BlockPos blockpos1 = entity.getOnPosLegacy();
            if (!blockpos1.equals(blockpos)) {
                this.highlightPosition(blockpos1, 0.04 + d0, -16711681);
            }
        });
    }

    private double getBias(Entity entity) {
        return 0.02 * (String.valueOf(entity.getId() + 0.132453657).hashCode() % 1000) / 1000.0;
    }

    private void highlightPosition(BlockPos pos, double bias, int color) {
        double d0 = pos.getX() - 2.0 * bias;
        double d1 = pos.getY() - 2.0 * bias;
        double d2 = pos.getZ() - 2.0 * bias;
        double d3 = d0 + 1.0 + 4.0 * bias;
        double d4 = d1 + 1.0 + 4.0 * bias;
        double d5 = d2 + 1.0 + 4.0 * bias;
        Gizmos.cuboid(new AABB(d0, d1, d2, d3, d4, d5), GizmoStyle.stroke(ARGB.color(0.4F, color)));
        VoxelShape voxelshape = this.minecraft
            .level
            .getBlockState(pos)
            .getCollisionShape(this.minecraft.level, pos, CollisionContext.empty())
            .move(pos);
        GizmoStyle gizmostyle = GizmoStyle.stroke(color);

        for (AABB aabb : voxelshape.toAabbs()) {
            Gizmos.cuboid(aabb, gizmostyle);
        }
    }
}
