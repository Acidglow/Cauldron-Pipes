package net.minecraft.client.renderer.debug;

import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.core.BlockPos;
import net.minecraft.gizmos.Gizmos;
import net.minecraft.util.debug.DebugBrainDump;
import net.minecraft.util.debug.DebugSubscriptions;
import net.minecraft.util.debug.DebugValueAccess;
import net.minecraft.world.entity.Entity;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.jspecify.annotations.Nullable;

@OnlyIn(Dist.CLIENT)
public class BrainDebugRenderer implements DebugRenderer.SimpleDebugRenderer {
    private static final boolean SHOW_NAME_FOR_ALL = true;
    private static final boolean SHOW_PROFESSION_FOR_ALL = false;
    private static final boolean SHOW_BEHAVIORS_FOR_ALL = false;
    private static final boolean SHOW_ACTIVITIES_FOR_ALL = false;
    private static final boolean SHOW_INVENTORY_FOR_ALL = false;
    private static final boolean SHOW_GOSSIPS_FOR_ALL = false;
    private static final boolean SHOW_HEALTH_FOR_ALL = false;
    private static final boolean SHOW_WANTS_GOLEM_FOR_ALL = true;
    private static final boolean SHOW_ANGER_LEVEL_FOR_ALL = false;
    private static final boolean SHOW_NAME_FOR_SELECTED = true;
    private static final boolean SHOW_PROFESSION_FOR_SELECTED = true;
    private static final boolean SHOW_BEHAVIORS_FOR_SELECTED = true;
    private static final boolean SHOW_ACTIVITIES_FOR_SELECTED = true;
    private static final boolean SHOW_MEMORIES_FOR_SELECTED = true;
    private static final boolean SHOW_INVENTORY_FOR_SELECTED = true;
    private static final boolean SHOW_GOSSIPS_FOR_SELECTED = true;
    private static final boolean SHOW_HEALTH_FOR_SELECTED = true;
    private static final boolean SHOW_WANTS_GOLEM_FOR_SELECTED = true;
    private static final boolean SHOW_ANGER_LEVEL_FOR_SELECTED = true;
    private static final int MAX_RENDER_DIST_FOR_BRAIN_INFO = 30;
    private static final int MAX_TARGETING_DIST = 8;
    private static final float TEXT_SCALE = 0.32F;
    private static final int CYAN = -16711681;
    private static final int GRAY = -3355444;
    private static final int PINK = -98404;
    private static final int ORANGE = -23296;
    private final Minecraft minecraft;
    private @Nullable UUID lastLookedAtUuid;

    public BrainDebugRenderer(Minecraft minecraft) {
        this.minecraft = minecraft;
    }

    @Override
    public void emitGizmos(double p_455712_, double p_456061_, double p_455671_, DebugValueAccess p_455104_, Frustum p_455735_, float p_455023_) {
        this.doRender(p_455104_);
        if (!this.minecraft.player.isSpectator()) {
            this.updateLastLookedAtUuid();
        }
    }

    private void doRender(DebugValueAccess debugValueAccess) {
        debugValueAccess.forEachEntity(DebugSubscriptions.BRAINS, (p_454281_, p_454282_) -> {
            if (this.minecraft.player.closerThan(p_454281_, 30.0)) {
                this.renderBrainInfo(p_454281_, p_454282_);
            }
        });
    }

    private void renderBrainInfo(Entity entity, DebugBrainDump brainDump) {
        boolean flag = this.isMobSelected(entity);
        int i = 0;
        Gizmos.billboardTextOverMob(entity, i, brainDump.name(), -1, 0.48F);
        i++;
        if (flag) {
            Gizmos.billboardTextOverMob(entity, i, brainDump.profession() + " " + brainDump.xp() + " xp", -1, 0.32F);
            i++;
        }

        if (flag) {
            int j = brainDump.health() < brainDump.maxHealth() ? -23296 : -1;
            Gizmos.billboardTextOverMob(
                entity,
                i,
                "health: " + String.format(Locale.ROOT, "%.1f", brainDump.health()) + " / " + String.format(Locale.ROOT, "%.1f", brainDump.maxHealth()),
                j,
                0.32F
            );
            i++;
        }

        if (flag && !brainDump.inventory().equals("")) {
            Gizmos.billboardTextOverMob(entity, i, brainDump.inventory(), -98404, 0.32F);
            i++;
        }

        if (flag) {
            for (String s : brainDump.behaviors()) {
                Gizmos.billboardTextOverMob(entity, i, s, -16711681, 0.32F);
                i++;
            }
        }

        if (flag) {
            for (String s1 : brainDump.activities()) {
                Gizmos.billboardTextOverMob(entity, i, s1, -16711936, 0.32F);
                i++;
            }
        }

        if (brainDump.wantsGolem()) {
            Gizmos.billboardTextOverMob(entity, i, "Wants Golem", -23296, 0.32F);
            i++;
        }

        if (flag && brainDump.angerLevel() != -1) {
            Gizmos.billboardTextOverMob(entity, i, "Anger Level: " + brainDump.angerLevel(), -98404, 0.32F);
            i++;
        }

        if (flag) {
            for (String s2 : brainDump.gossips()) {
                if (s2.startsWith(brainDump.name())) {
                    Gizmos.billboardTextOverMob(entity, i, s2, -1, 0.32F);
                } else {
                    Gizmos.billboardTextOverMob(entity, i, s2, -23296, 0.32F);
                }

                i++;
            }
        }

        if (flag) {
            for (String s3 : Lists.reverse(brainDump.memories())) {
                Gizmos.billboardTextOverMob(entity, i, s3, -3355444, 0.32F);
                i++;
            }
        }
    }

    private boolean isMobSelected(Entity entity) {
        return Objects.equals(this.lastLookedAtUuid, entity.getUUID());
    }

    public Map<BlockPos, List<String>> getGhostPois(DebugValueAccess debugValueAccess) {
        Map<BlockPos, List<String>> map = Maps.newHashMap();
        debugValueAccess.forEachEntity(DebugSubscriptions.BRAINS, (p_448754_, p_448755_) -> {
            for (BlockPos blockpos : Iterables.concat(p_448755_.pois(), p_448755_.potentialPois())) {
                map.computeIfAbsent(blockpos, p_113292_ -> Lists.newArrayList()).add(p_448755_.name());
            }
        });
        return map;
    }

    private void updateLastLookedAtUuid() {
        DebugRenderer.getTargetedEntity(this.minecraft.getCameraEntity(), 8).ifPresent(p_113212_ -> this.lastLookedAtUuid = p_113212_.getUUID());
    }
}
