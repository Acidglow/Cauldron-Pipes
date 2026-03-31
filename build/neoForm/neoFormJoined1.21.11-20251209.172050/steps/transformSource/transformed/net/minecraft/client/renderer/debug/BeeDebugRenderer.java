package net.minecraft.client.renderer.debug;

import com.google.common.collect.Lists;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.core.BlockPos;
import net.minecraft.gizmos.GizmoStyle;
import net.minecraft.gizmos.Gizmos;
import net.minecraft.network.protocol.game.DebugEntityNameGenerator;
import net.minecraft.util.ARGB;
import net.minecraft.util.debug.DebugBeeInfo;
import net.minecraft.util.debug.DebugGoalInfo;
import net.minecraft.util.debug.DebugHiveInfo;
import net.minecraft.util.debug.DebugSubscriptions;
import net.minecraft.util.debug.DebugValueAccess;
import net.minecraft.world.entity.Entity;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.jspecify.annotations.Nullable;

@OnlyIn(Dist.CLIENT)
public class BeeDebugRenderer implements DebugRenderer.SimpleDebugRenderer {
    private static final boolean SHOW_GOAL_FOR_ALL_BEES = true;
    private static final boolean SHOW_NAME_FOR_ALL_BEES = true;
    private static final boolean SHOW_HIVE_FOR_ALL_BEES = true;
    private static final boolean SHOW_FLOWER_POS_FOR_ALL_BEES = true;
    private static final boolean SHOW_TRAVEL_TICKS_FOR_ALL_BEES = true;
    private static final boolean SHOW_GOAL_FOR_SELECTED_BEE = true;
    private static final boolean SHOW_NAME_FOR_SELECTED_BEE = true;
    private static final boolean SHOW_HIVE_FOR_SELECTED_BEE = true;
    private static final boolean SHOW_FLOWER_POS_FOR_SELECTED_BEE = true;
    private static final boolean SHOW_TRAVEL_TICKS_FOR_SELECTED_BEE = true;
    private static final boolean SHOW_HIVE_MEMBERS = true;
    private static final boolean SHOW_BLACKLISTS = true;
    private static final int MAX_RENDER_DIST_FOR_HIVE_OVERLAY = 30;
    private static final int MAX_RENDER_DIST_FOR_BEE_OVERLAY = 30;
    private static final int MAX_TARGETING_DIST = 8;
    private static final float TEXT_SCALE = 0.32F;
    private static final int ORANGE = -23296;
    private static final int GRAY = -3355444;
    private static final int PINK = -98404;
    private final Minecraft minecraft;
    private @Nullable UUID lastLookedAtUuid;

    public BeeDebugRenderer(Minecraft minecraft) {
        this.minecraft = minecraft;
    }

    @Override
    public void emitGizmos(double p_456220_, double p_455610_, double p_455534_, DebugValueAccess p_455739_, Frustum p_454831_, float p_455624_) {
        this.doRender(p_455739_);
        if (!this.minecraft.player.isSpectator()) {
            this.updateLastLookedAtUuid();
        }
    }

    private void doRender(DebugValueAccess debugValueAccess) {
        BlockPos blockpos = this.getCamera().blockPosition();
        debugValueAccess.forEachEntity(DebugSubscriptions.BEES, (p_454269_, p_454270_) -> {
            if (this.minecraft.player.closerThan(p_454269_, 30.0)) {
                DebugGoalInfo debuggoalinfo = debugValueAccess.getEntityValue(DebugSubscriptions.GOAL_SELECTORS, p_454269_);
                this.renderBeeInfo(p_454269_, p_454270_, debuggoalinfo);
            }
        });
        this.renderFlowerInfos(debugValueAccess);
        Map<BlockPos, Set<UUID>> map = this.createHiveBlacklistMap(debugValueAccess);
        debugValueAccess.forEachBlock(DebugSubscriptions.BEE_HIVES, (p_454279_, p_454280_) -> {
            if (blockpos.closerThan(p_454279_, 30.0)) {
                highlightHive(p_454279_);
                Set<UUID> set = map.getOrDefault(p_454279_, Set.of());
                this.renderHiveInfo(p_454279_, p_454280_, set, debugValueAccess);
            }
        });
        this.getGhostHives(debugValueAccess).forEach((p_454272_, p_454273_) -> {
            if (blockpos.closerThan(p_454272_, 30.0)) {
                this.renderGhostHive(p_454272_, (List<String>)p_454273_);
            }
        });
    }

    private Map<BlockPos, Set<UUID>> createHiveBlacklistMap(DebugValueAccess debugValueAccess) {
        Map<BlockPos, Set<UUID>> map = new HashMap<>();
        debugValueAccess.forEachEntity(DebugSubscriptions.BEES, (p_448716_, p_448717_) -> {
            for (BlockPos blockpos : p_448717_.blacklistedHives()) {
                map.computeIfAbsent(blockpos, p_293649_ -> new HashSet<>()).add(p_448716_.getUUID());
            }
        });
        return map;
    }

    private void renderFlowerInfos(DebugValueAccess debugValueAccess) {
        Map<BlockPos, Set<UUID>> map = new HashMap<>();
        debugValueAccess.forEachEntity(DebugSubscriptions.BEES, (p_448743_, p_448744_) -> {
            if (p_448744_.flowerPos().isPresent()) {
                map.computeIfAbsent(p_448744_.flowerPos().get(), p_448745_ -> new HashSet<>()).add(p_448743_.getUUID());
            }
        });
        map.forEach((p_454274_, p_454275_) -> {
            Set<String> set = p_454275_.stream().map(DebugEntityNameGenerator::getEntityName).collect(Collectors.toSet());
            int i = 1;
            Gizmos.billboardTextOverBlock(set.toString(), p_454274_, i++, -256, 0.32F);
            Gizmos.billboardTextOverBlock("Flower", p_454274_, i++, -1, 0.32F);
            Gizmos.cuboid(p_454274_, 0.05F, GizmoStyle.fill(ARGB.colorFromFloat(0.3F, 0.8F, 0.8F, 0.0F)));
        });
    }

    private static String getBeeUuidsAsString(Collection<UUID> beeUuids) {
        if (beeUuids.isEmpty()) {
            return "-";
        } else {
            return beeUuids.size() > 3
                ? beeUuids.size() + " bees"
                : beeUuids.stream().map(DebugEntityNameGenerator::getEntityName).collect(Collectors.toSet()).toString();
        }
    }

    private static void highlightHive(BlockPos pos) {
        float f = 0.05F;
        Gizmos.cuboid(pos, 0.05F, GizmoStyle.fill(ARGB.colorFromFloat(0.3F, 0.2F, 0.2F, 1.0F)));
    }

    private void renderGhostHive(BlockPos pos, List<String> ghostHives) {
        float f = 0.05F;
        Gizmos.cuboid(pos, 0.05F, GizmoStyle.fill(ARGB.colorFromFloat(0.3F, 0.2F, 0.2F, 1.0F)));
        Gizmos.billboardTextOverBlock(ghostHives.toString(), pos, 0, -256, 0.32F);
        Gizmos.billboardTextOverBlock("Ghost Hive", pos, 1, -65536, 0.32F);
    }

    private void renderHiveInfo(BlockPos pos, DebugHiveInfo hiveInfo, Collection<UUID> blacklistedBees, DebugValueAccess debugValueAccess) {
        int i = 0;
        if (!blacklistedBees.isEmpty()) {
            renderTextOverHive("Blacklisted by " + getBeeUuidsAsString(blacklistedBees), pos, i++, -65536);
        }

        renderTextOverHive("Out: " + getBeeUuidsAsString(this.getHiveMembers(pos, debugValueAccess)), pos, i++, -3355444);
        if (hiveInfo.occupantCount() == 0) {
            renderTextOverHive("In: -", pos, i++, -256);
        } else if (hiveInfo.occupantCount() == 1) {
            renderTextOverHive("In: 1 bee", pos, i++, -256);
        } else {
            renderTextOverHive("In: " + hiveInfo.occupantCount() + " bees", pos, i++, -256);
        }

        renderTextOverHive("Honey: " + hiveInfo.honeyLevel(), pos, i++, -23296);
        renderTextOverHive(hiveInfo.type().getName().getString() + (hiveInfo.sedated() ? " (sedated)" : ""), pos, i++, -1);
    }

    private void renderBeeInfo(Entity bee, DebugBeeInfo beeInfo, @Nullable DebugGoalInfo goalInfo) {
        boolean flag = this.isBeeSelected(bee);
        int i = 0;
        Gizmos.billboardTextOverMob(bee, i++, beeInfo.toString(), -1, 0.48F);
        if (beeInfo.hivePos().isEmpty()) {
            Gizmos.billboardTextOverMob(bee, i++, "No hive", -98404, 0.32F);
        } else {
            Gizmos.billboardTextOverMob(bee, i++, "Hive: " + this.getPosDescription(bee, beeInfo.hivePos().get()), -256, 0.32F);
        }

        if (beeInfo.flowerPos().isEmpty()) {
            Gizmos.billboardTextOverMob(bee, i++, "No flower", -98404, 0.32F);
        } else {
            Gizmos.billboardTextOverMob(bee, i++, "Flower: " + this.getPosDescription(bee, beeInfo.flowerPos().get()), -256, 0.32F);
        }

        if (goalInfo != null) {
            for (DebugGoalInfo.DebugGoal debuggoalinfo$debuggoal : goalInfo.goals()) {
                if (debuggoalinfo$debuggoal.isRunning()) {
                    Gizmos.billboardTextOverMob(bee, i++, debuggoalinfo$debuggoal.name(), -16711936, 0.32F);
                }
            }
        }

        if (beeInfo.travelTicks() > 0) {
            int j = beeInfo.travelTicks() < 2400 ? -3355444 : -23296;
            Gizmos.billboardTextOverMob(bee, i++, "Travelling: " + beeInfo.travelTicks() + " ticks", j, 0.32F);
        }
    }

    private static void renderTextOverHive(String text, BlockPos pos, int line, int color) {
        Gizmos.billboardTextOverBlock(text, pos, line, color, 0.32F);
    }

    private Camera getCamera() {
        return this.minecraft.gameRenderer.getMainCamera();
    }

    private String getPosDescription(Entity entity, BlockPos pos) {
        double d0 = pos.distToCenterSqr(entity.position());
        double d1 = Math.round(d0 * 10.0) / 10.0;
        return pos.toShortString() + " (dist " + d1 + ")";
    }

    private boolean isBeeSelected(Entity entity) {
        return Objects.equals(this.lastLookedAtUuid, entity.getUUID());
    }

    private Collection<UUID> getHiveMembers(BlockPos pos, DebugValueAccess debugValueAccess) {
        Set<UUID> set = new HashSet<>();
        debugValueAccess.forEachEntity(DebugSubscriptions.BEES, (p_448725_, p_448726_) -> {
            if (p_448726_.hasHive(pos)) {
                set.add(p_448725_.getUUID());
            }
        });
        return set;
    }

    private Map<BlockPos, List<String>> getGhostHives(DebugValueAccess debugValueAccess) {
        Map<BlockPos, List<String>> map = new HashMap<>();
        debugValueAccess.forEachEntity(DebugSubscriptions.BEES, (p_448736_, p_448737_) -> {
            if (p_448737_.hivePos().isPresent() && debugValueAccess.getBlockValue(DebugSubscriptions.BEE_HIVES, p_448737_.hivePos().get()) == null) {
                map.computeIfAbsent(p_448737_.hivePos().get(), p_113140_ -> Lists.newArrayList()).add(DebugEntityNameGenerator.getEntityName(p_448736_));
            }
        });
        return map;
    }

    private void updateLastLookedAtUuid() {
        DebugRenderer.getTargetedEntity(this.minecraft.getCameraEntity(), 8).ifPresent(p_113059_ -> this.lastLookedAtUuid = p_113059_.getUUID());
    }
}
