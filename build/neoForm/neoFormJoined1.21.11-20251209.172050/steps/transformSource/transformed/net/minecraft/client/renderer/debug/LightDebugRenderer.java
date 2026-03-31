package net.minecraft.client.renderer.debug;

import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSet;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.gizmos.Gizmos;
import net.minecraft.gizmos.TextGizmo;
import net.minecraft.util.ARGB;
import net.minecraft.util.debug.DebugValueAccess;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class LightDebugRenderer implements DebugRenderer.SimpleDebugRenderer {
    private final Minecraft minecraft;
    private final boolean showBlockLight;
    private final boolean showSkyLight;
    private static final int MAX_RENDER_DIST = 10;

    public LightDebugRenderer(Minecraft minecraft, boolean showBlockLight, boolean showSkyLight) {
        this.minecraft = minecraft;
        this.showBlockLight = showBlockLight;
        this.showSkyLight = showSkyLight;
    }

    @Override
    public void emitGizmos(double p_454973_, double p_455778_, double p_456025_, DebugValueAccess p_455209_, Frustum p_454713_, float p_455391_) {
        Level level = this.minecraft.level;
        BlockPos blockpos = BlockPos.containing(p_454973_, p_455778_, p_456025_);
        LongSet longset = new LongOpenHashSet();

        for (BlockPos blockpos1 : BlockPos.betweenClosed(blockpos.offset(-10, -10, -10), blockpos.offset(10, 10, 10))) {
            int i = level.getBrightness(LightLayer.SKY, blockpos1);
            long j = SectionPos.blockToSection(blockpos1.asLong());
            if (longset.add(j)) {
                Gizmos.billboardText(
                    level.getChunkSource().getLightEngine().getDebugData(LightLayer.SKY, SectionPos.of(j)),
                    new Vec3(
                        SectionPos.sectionToBlockCoord(SectionPos.x(j), 8),
                        SectionPos.sectionToBlockCoord(SectionPos.y(j), 8),
                        SectionPos.sectionToBlockCoord(SectionPos.z(j), 8)
                    ),
                    TextGizmo.Style.forColorAndCentered(-65536).withScale(4.8F)
                );
            }

            if (i != 15 && this.showSkyLight) {
                int k = ARGB.srgbLerp(i / 15.0F, -16776961, -16711681);
                Gizmos.billboardText(String.valueOf(i), Vec3.atLowerCornerWithOffset(blockpos1, 0.5, 0.25, 0.5), TextGizmo.Style.forColorAndCentered(k));
            }

            if (this.showBlockLight) {
                int i1 = level.getBrightness(LightLayer.BLOCK, blockpos1);
                if (i1 != 0) {
                    int l = ARGB.srgbLerp(i1 / 15.0F, -5636096, -256);
                    Gizmos.billboardText(
                        String.valueOf(level.getBrightness(LightLayer.BLOCK, blockpos1)), Vec3.atCenterOf(blockpos1), TextGizmo.Style.forColorAndCentered(l)
                    );
                }
            }
        }
    }
}
