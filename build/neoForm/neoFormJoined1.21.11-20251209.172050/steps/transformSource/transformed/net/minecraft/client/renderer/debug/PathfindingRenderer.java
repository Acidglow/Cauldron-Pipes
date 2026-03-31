package net.minecraft.client.renderer.debug;

import java.util.Locale;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.core.BlockPos;
import net.minecraft.gizmos.GizmoStyle;
import net.minecraft.gizmos.Gizmos;
import net.minecraft.gizmos.TextGizmo;
import net.minecraft.util.ARGB;
import net.minecraft.util.Mth;
import net.minecraft.util.debug.DebugPathInfo;
import net.minecraft.util.debug.DebugSubscriptions;
import net.minecraft.util.debug.DebugValueAccess;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.pathfinder.Node;
import net.minecraft.world.level.pathfinder.Path;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class PathfindingRenderer implements DebugRenderer.SimpleDebugRenderer {
    private static final float MAX_RENDER_DIST = 80.0F;
    private static final int MAX_TARGETING_DIST = 8;
    private static final boolean SHOW_ONLY_SELECTED = false;
    private static final boolean SHOW_OPEN_CLOSED = true;
    private static final boolean SHOW_OPEN_CLOSED_COST_MALUS = false;
    private static final boolean SHOW_OPEN_CLOSED_NODE_TYPE_WITH_TEXT = false;
    private static final boolean SHOW_OPEN_CLOSED_NODE_TYPE_WITH_BOX = true;
    private static final boolean SHOW_GROUND_LABELS = true;
    private static final float TEXT_SCALE = 0.32F;

    @Override
    public void emitGizmos(double p_454876_, double p_455700_, double p_456095_, DebugValueAccess p_456203_, Frustum p_456018_, float p_455251_) {
        p_456203_.forEachEntity(
            DebugSubscriptions.ENTITY_PATHS,
            (p_454333_, p_454334_) -> renderPath(p_454876_, p_455700_, p_456095_, p_454334_.path(), p_454334_.maxNodeDistance())
        );
    }

    private static void renderPath(double camX, double camY, double camZ, Path path, float maxNodeDistance) {
        renderPath(path, maxNodeDistance, true, true, camX, camY, camZ);
    }

    public static void renderPath(Path path, float maxNodeDistance, boolean renderNodes, boolean renderInfo, double camX, double camY, double camZ) {
        renderPathLine(path, camX, camY, camZ);
        BlockPos blockpos = path.getTarget();
        if (distanceToCamera(blockpos, camX, camY, camZ) <= 80.0F) {
            Gizmos.cuboid(
                new AABB(
                    blockpos.getX() + 0.25F,
                    blockpos.getY() + 0.25F,
                    blockpos.getZ() + 0.25,
                    blockpos.getX() + 0.75F,
                    blockpos.getY() + 0.75F,
                    blockpos.getZ() + 0.75F
                ),
                GizmoStyle.fill(ARGB.colorFromFloat(0.5F, 0.0F, 1.0F, 0.0F))
            );

            for (int i = 0; i < path.getNodeCount(); i++) {
                Node node = path.getNode(i);
                if (distanceToCamera(node.asBlockPos(), camX, camY, camZ) <= 80.0F) {
                    float f = i == path.getNextNodeIndex() ? 1.0F : 0.0F;
                    float f1 = i == path.getNextNodeIndex() ? 0.0F : 1.0F;
                    AABB aabb = new AABB(
                        node.x + 0.5F - maxNodeDistance,
                        node.y + 0.01F * i,
                        node.z + 0.5F - maxNodeDistance,
                        node.x + 0.5F + maxNodeDistance,
                        node.y + 0.25F + 0.01F * i,
                        node.z + 0.5F + maxNodeDistance
                    );
                    Gizmos.cuboid(aabb, GizmoStyle.fill(ARGB.colorFromFloat(0.5F, f, 0.0F, f1)));
                }
            }
        }

        Path.DebugData path$debugdata = path.debugData();
        if (renderNodes && path$debugdata != null) {
            for (Node node2 : path$debugdata.closedSet()) {
                if (distanceToCamera(node2.asBlockPos(), camX, camY, camZ) <= 80.0F) {
                    Gizmos.cuboid(
                        new AABB(
                            node2.x + 0.5F - maxNodeDistance / 2.0F,
                            node2.y + 0.01F,
                            node2.z + 0.5F - maxNodeDistance / 2.0F,
                            node2.x + 0.5F + maxNodeDistance / 2.0F,
                            node2.y + 0.1,
                            node2.z + 0.5F + maxNodeDistance / 2.0F
                        ),
                        GizmoStyle.fill(ARGB.colorFromFloat(0.5F, 1.0F, 0.8F, 0.8F))
                    );
                }
            }

            for (Node node3 : path$debugdata.openSet()) {
                if (distanceToCamera(node3.asBlockPos(), camX, camY, camZ) <= 80.0F) {
                    Gizmos.cuboid(
                        new AABB(
                            node3.x + 0.5F - maxNodeDistance / 2.0F,
                            node3.y + 0.01F,
                            node3.z + 0.5F - maxNodeDistance / 2.0F,
                            node3.x + 0.5F + maxNodeDistance / 2.0F,
                            node3.y + 0.1,
                            node3.z + 0.5F + maxNodeDistance / 2.0F
                        ),
                        GizmoStyle.fill(ARGB.colorFromFloat(0.5F, 0.8F, 1.0F, 1.0F))
                    );
                }
            }
        }

        if (renderInfo) {
            for (int j = 0; j < path.getNodeCount(); j++) {
                Node node1 = path.getNode(j);
                if (distanceToCamera(node1.asBlockPos(), camX, camY, camZ) <= 80.0F) {
                    Gizmos.billboardText(
                            String.valueOf(node1.type),
                            new Vec3(node1.x + 0.5, node1.y + 0.75, node1.z + 0.5),
                            TextGizmo.Style.whiteAndCentered().withScale(0.32F)
                        )
                        .setAlwaysOnTop();
                    Gizmos.billboardText(
                            String.format(Locale.ROOT, "%.2f", node1.costMalus),
                            new Vec3(node1.x + 0.5, node1.y + 0.25, node1.z + 0.5),
                            TextGizmo.Style.whiteAndCentered().withScale(0.32F)
                        )
                        .setAlwaysOnTop();
                }
            }
        }
    }

    public static void renderPathLine(Path path, double camX, double camY, double camZ) {
        if (path.getNodeCount() >= 2) {
            Vec3 vec3 = path.getNode(0).asVec3();

            for (int i = 1; i < path.getNodeCount(); i++) {
                Node node = path.getNode(i);
                if (distanceToCamera(node.asBlockPos(), camX, camY, camZ) > 80.0F) {
                    vec3 = node.asVec3();
                } else {
                    float f = (float)i / path.getNodeCount() * 0.33F;
                    int j = ARGB.opaque(Mth.hsvToRgb(f, 0.9F, 0.9F));
                    Gizmos.arrow(vec3.add(0.5, 0.5, 0.5), node.asVec3().add(0.5, 0.5, 0.5), j);
                    vec3 = node.asVec3();
                }
            }
        }
    }

    private static float distanceToCamera(BlockPos pos, double x, double y, double z) {
        return (float)(Math.abs(pos.getX() - x) + Math.abs(pos.getY() - y) + Math.abs(pos.getZ() - z));
    }
}
