package net.minecraft.client.renderer.debug;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.debug.DebugScreenEntries;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.SectionOcclusionGraph;
import net.minecraft.client.renderer.chunk.SectionRenderDispatcher;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gizmos.GizmoStyle;
import net.minecraft.gizmos.Gizmos;
import net.minecraft.util.ARGB;
import net.minecraft.util.Mth;
import net.minecraft.util.debug.DebugValueAccess;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.joml.Vector4f;

@OnlyIn(Dist.CLIENT)
public class ChunkCullingDebugRenderer implements DebugRenderer.SimpleDebugRenderer {
    public static final Direction[] DIRECTIONS = Direction.values();
    private final Minecraft minecraft;

    public ChunkCullingDebugRenderer(Minecraft minecraft) {
        this.minecraft = minecraft;
    }

    @Override
    public void emitGizmos(double p_456245_, double p_455113_, double p_455772_, DebugValueAccess p_456284_, Frustum p_455014_, float p_455874_) {
        LevelRenderer levelrenderer = this.minecraft.levelRenderer;
        boolean flag = this.minecraft.debugEntries.isCurrentlyEnabled(DebugScreenEntries.CHUNK_SECTION_PATHS);
        boolean flag1 = this.minecraft.debugEntries.isCurrentlyEnabled(DebugScreenEntries.CHUNK_SECTION_VISIBILITY);
        if (flag || flag1) {
            SectionOcclusionGraph sectionocclusiongraph = levelrenderer.getSectionOcclusionGraph();

            for (SectionRenderDispatcher.RenderSection sectionrenderdispatcher$rendersection : levelrenderer.getVisibleSections()) {
                SectionOcclusionGraph.Node sectionocclusiongraph$node = sectionocclusiongraph.getNode(sectionrenderdispatcher$rendersection);
                if (sectionocclusiongraph$node != null) {
                    BlockPos blockpos = sectionrenderdispatcher$rendersection.getRenderOrigin();
                    if (flag) {
                        int i = sectionocclusiongraph$node.step == 0 ? 0 : Mth.hsvToRgb(sectionocclusiongraph$node.step / 50.0F, 0.9F, 0.9F);

                        for (int j = 0; j < DIRECTIONS.length; j++) {
                            if (sectionocclusiongraph$node.hasSourceDirection(j)) {
                                Direction direction = DIRECTIONS[j];
                                Gizmos.line(
                                    Vec3.atLowerCornerWithOffset(blockpos, 8.0, 8.0, 8.0),
                                    Vec3.atLowerCornerWithOffset(
                                        blockpos, 8 - 16 * direction.getStepX(), 8 - 16 * direction.getStepY(), 8 - 16 * direction.getStepZ()
                                    ),
                                    ARGB.opaque(i)
                                );
                            }
                        }
                    }

                    if (flag1 && sectionrenderdispatcher$rendersection.getSectionMesh().hasRenderableLayers()) {
                        int k = 0;

                        for (Direction direction1 : DIRECTIONS) {
                            for (Direction direction2 : DIRECTIONS) {
                                boolean flag2 = sectionrenderdispatcher$rendersection.getSectionMesh().facesCanSeeEachother(direction1, direction2);
                                if (!flag2) {
                                    k++;
                                    Gizmos.line(
                                        Vec3.atLowerCornerWithOffset(
                                            blockpos, 8 + 8 * direction1.getStepX(), 8 + 8 * direction1.getStepY(), 8 + 8 * direction1.getStepZ()
                                        ),
                                        Vec3.atLowerCornerWithOffset(
                                            blockpos, 8 + 8 * direction2.getStepX(), 8 + 8 * direction2.getStepY(), 8 + 8 * direction2.getStepZ()
                                        ),
                                        ARGB.color(255, 255, 0, 0)
                                    );
                                }
                            }
                        }

                        if (k > 0) {
                            float f = 0.5F;
                            float f1 = 0.2F;
                            Gizmos.cuboid(
                                sectionrenderdispatcher$rendersection.getBoundingBox().deflate(0.5),
                                GizmoStyle.fill(ARGB.colorFromFloat(0.2F, 0.9F, 0.9F, 0.0F))
                            );
                        }
                    }
                }
            }
        }

        Frustum frustum = levelrenderer.getCapturedFrustum();
        if (frustum != null) {
            Vec3 vec3 = new Vec3(frustum.getCamX(), frustum.getCamY(), frustum.getCamZ());
            Vector4f[] avector4f = frustum.getFrustumPoints();
            this.addFrustumQuad(vec3, avector4f, 0, 1, 2, 3, 0, 1, 1);
            this.addFrustumQuad(vec3, avector4f, 4, 5, 6, 7, 1, 0, 0);
            this.addFrustumQuad(vec3, avector4f, 0, 1, 5, 4, 1, 1, 0);
            this.addFrustumQuad(vec3, avector4f, 2, 3, 7, 6, 0, 0, 1);
            this.addFrustumQuad(vec3, avector4f, 0, 4, 7, 3, 0, 1, 0);
            this.addFrustumQuad(vec3, avector4f, 1, 5, 6, 2, 1, 0, 1);
            this.addFrustumLine(vec3, avector4f[0], avector4f[1]);
            this.addFrustumLine(vec3, avector4f[1], avector4f[2]);
            this.addFrustumLine(vec3, avector4f[2], avector4f[3]);
            this.addFrustumLine(vec3, avector4f[3], avector4f[0]);
            this.addFrustumLine(vec3, avector4f[4], avector4f[5]);
            this.addFrustumLine(vec3, avector4f[5], avector4f[6]);
            this.addFrustumLine(vec3, avector4f[6], avector4f[7]);
            this.addFrustumLine(vec3, avector4f[7], avector4f[4]);
            this.addFrustumLine(vec3, avector4f[0], avector4f[4]);
            this.addFrustumLine(vec3, avector4f[1], avector4f[5]);
            this.addFrustumLine(vec3, avector4f[2], avector4f[6]);
            this.addFrustumLine(vec3, avector4f[3], avector4f[7]);
        }
    }

    private void addFrustumLine(Vec3 cameraPos, Vector4f start, Vector4f end) {
        Gizmos.line(
            new Vec3(cameraPos.x + start.x, cameraPos.y + start.y, cameraPos.z + start.z),
            new Vec3(cameraPos.x + end.x, cameraPos.y + end.y, cameraPos.z + end.z),
            -16777216
        );
    }

    private void addFrustumQuad(
        Vec3 cameraPos, Vector4f[] frustumPoints, int point1, int point2, int point3, int point4, int red, int green, int blue
    ) {
        float f = 0.25F;
        Gizmos.rect(
            new Vec3(frustumPoints[point1].x(), frustumPoints[point1].y(), frustumPoints[point1].z()).add(cameraPos),
            new Vec3(frustumPoints[point2].x(), frustumPoints[point2].y(), frustumPoints[point2].z()).add(cameraPos),
            new Vec3(frustumPoints[point3].x(), frustumPoints[point3].y(), frustumPoints[point3].z()).add(cameraPos),
            new Vec3(frustumPoints[point4].x(), frustumPoints[point4].y(), frustumPoints[point4].z()).add(cameraPos),
            GizmoStyle.fill(ARGB.colorFromFloat(0.25F, red, green, blue))
        );
    }
}
