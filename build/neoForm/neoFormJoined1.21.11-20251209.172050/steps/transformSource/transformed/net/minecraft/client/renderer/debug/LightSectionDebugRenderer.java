package net.minecraft.client.renderer.debug;

import java.time.Duration;
import java.time.Instant;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.core.Direction;
import net.minecraft.core.SectionPos;
import net.minecraft.gizmos.GizmoStyle;
import net.minecraft.gizmos.Gizmos;
import net.minecraft.util.ARGB;
import net.minecraft.util.debug.DebugValueAccess;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.lighting.LayerLightSectionStorage;
import net.minecraft.world.level.lighting.LevelLightEngine;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.BitSetDiscreteVoxelShape;
import net.minecraft.world.phys.shapes.DiscreteVoxelShape;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.jspecify.annotations.Nullable;

@OnlyIn(Dist.CLIENT)
public class LightSectionDebugRenderer implements DebugRenderer.SimpleDebugRenderer {
    private static final Duration REFRESH_INTERVAL = Duration.ofMillis(500L);
    private static final int RADIUS = 10;
    private static final int LIGHT_AND_BLOCKS_COLOR = ARGB.colorFromFloat(0.25F, 1.0F, 1.0F, 0.0F);
    private static final int LIGHT_ONLY_COLOR = ARGB.colorFromFloat(0.125F, 0.25F, 0.125F, 0.0F);
    private final Minecraft minecraft;
    private final LightLayer lightLayer;
    private Instant lastUpdateTime = Instant.now();
    private LightSectionDebugRenderer.@Nullable SectionData data;

    public LightSectionDebugRenderer(Minecraft minecraft, LightLayer lightLayer) {
        this.minecraft = minecraft;
        this.lightLayer = lightLayer;
    }

    @Override
    public void emitGizmos(double p_454694_, double p_455000_, double p_456110_, DebugValueAccess p_455821_, Frustum p_454892_, float p_455573_) {
        Instant instant = Instant.now();
        if (this.data == null || Duration.between(this.lastUpdateTime, instant).compareTo(REFRESH_INTERVAL) > 0) {
            this.lastUpdateTime = instant;
            this.data = new LightSectionDebugRenderer.SectionData(
                this.minecraft.level.getLightEngine(), SectionPos.of(this.minecraft.player.blockPosition()), 10, this.lightLayer
            );
        }

        renderEdges(this.data.lightAndBlocksShape, this.data.minPos, LIGHT_AND_BLOCKS_COLOR);
        renderEdges(this.data.lightShape, this.data.minPos, LIGHT_ONLY_COLOR);
        renderFaces(this.data.lightAndBlocksShape, this.data.minPos, LIGHT_AND_BLOCKS_COLOR);
        renderFaces(this.data.lightShape, this.data.minPos, LIGHT_ONLY_COLOR);
    }

    private static void renderFaces(DiscreteVoxelShape shape, SectionPos pos, int color) {
        shape.forAllFaces((p_454316_, p_454317_, p_454318_, p_454319_) -> {
            int i = p_454317_ + pos.getX();
            int j = p_454318_ + pos.getY();
            int k = p_454319_ + pos.getZ();
            renderFace(p_454316_, i, j, k, color);
        });
    }

    private static void renderEdges(DiscreteVoxelShape shape, SectionPos pos, int color) {
        shape.forAllEdges((p_454308_, p_454309_, p_454310_, p_454311_, p_454312_, p_454313_) -> {
            int i = p_454308_ + pos.getX();
            int j = p_454309_ + pos.getY();
            int k = p_454310_ + pos.getZ();
            int l = p_454311_ + pos.getX();
            int i1 = p_454312_ + pos.getY();
            int j1 = p_454313_ + pos.getZ();
            renderEdge(i, j, k, l, i1, j1, color);
        }, true);
    }

    private static void renderFace(Direction direction, int x, int y, int z, int color) {
        Vec3 vec3 = new Vec3(SectionPos.sectionToBlockCoord(x), SectionPos.sectionToBlockCoord(y), SectionPos.sectionToBlockCoord(z));
        Vec3 vec31 = vec3.add(16.0, 16.0, 16.0);
        Gizmos.rect(vec3, vec31, direction, GizmoStyle.fill(color));
    }

    private static void renderEdge(int x1, int y1, int z1, int x2, int y2, int z2, int color) {
        double d0 = SectionPos.sectionToBlockCoord(x1);
        double d1 = SectionPos.sectionToBlockCoord(y1);
        double d2 = SectionPos.sectionToBlockCoord(z1);
        double d3 = SectionPos.sectionToBlockCoord(x2);
        double d4 = SectionPos.sectionToBlockCoord(y2);
        double d5 = SectionPos.sectionToBlockCoord(z2);
        int i = ARGB.opaque(color);
        Gizmos.line(new Vec3(d0, d1, d2), new Vec3(d3, d4, d5), i);
    }

    @OnlyIn(Dist.CLIENT)
    static final class SectionData {
        final DiscreteVoxelShape lightAndBlocksShape;
        final DiscreteVoxelShape lightShape;
        final SectionPos minPos;

        SectionData(LevelLightEngine levelLightEngine, SectionPos pos, int radius, LightLayer lightLayer) {
            int i = radius * 2 + 1;
            this.lightAndBlocksShape = new BitSetDiscreteVoxelShape(i, i, i);
            this.lightShape = new BitSetDiscreteVoxelShape(i, i, i);

            for (int j = 0; j < i; j++) {
                for (int k = 0; k < i; k++) {
                    for (int l = 0; l < i; l++) {
                        SectionPos sectionpos = SectionPos.of(pos.x() + l - radius, pos.y() + k - radius, pos.z() + j - radius);
                        LayerLightSectionStorage.SectionType layerlightsectionstorage$sectiontype = levelLightEngine.getDebugSectionType(lightLayer, sectionpos);
                        if (layerlightsectionstorage$sectiontype == LayerLightSectionStorage.SectionType.LIGHT_AND_DATA) {
                            this.lightAndBlocksShape.fill(l, k, j);
                            this.lightShape.fill(l, k, j);
                        } else if (layerlightsectionstorage$sectiontype == LayerLightSectionStorage.SectionType.LIGHT_ONLY) {
                            this.lightShape.fill(l, k, j);
                        }
                    }
                }
            }

            this.minPos = SectionPos.of(pos.x() - radius, pos.y() - radius, pos.z() - radius);
        }
    }
}
