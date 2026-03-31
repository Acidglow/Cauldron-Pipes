package net.minecraft.client.server;

import com.google.common.base.MoreObjects;
import com.google.common.collect.Lists;
import com.mojang.authlib.GameProfile;
import com.mojang.logging.LogUtils;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import java.util.function.BooleanSupplier;
import net.minecraft.CrashReport;
import net.minecraft.SharedConstants;
import net.minecraft.SystemReport;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.toasts.SystemToast;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.gizmos.Gizmos;
import net.minecraft.gizmos.SimpleGizmoCollector;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.Services;
import net.minecraft.server.WorldStem;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.progress.LevelLoadListener;
import net.minecraft.server.packs.repository.PackRepository;
import net.minecraft.server.permissions.LevelBasedPermissionSet;
import net.minecraft.server.permissions.PermissionSet;
import net.minecraft.server.players.NameAndId;
import net.minecraft.stats.Stats;
import net.minecraft.util.ModCheck;
import net.minecraft.util.ProblemReporter;
import net.minecraft.util.debugchart.LocalSampleLogger;
import net.minecraft.util.profiling.Profiler;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.chunk.storage.RegionStorageInfo;
import net.minecraft.world.level.storage.LevelStorageSource;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.level.storage.ValueInput;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

@OnlyIn(Dist.CLIENT)
public class IntegratedServer extends MinecraftServer {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final int MIN_SIM_DISTANCE = 2;
    public static final int MAX_PLAYERS = 8;
    private final Minecraft minecraft;
    private boolean paused = true;
    private int publishedPort = -1;
    private @Nullable GameType publishedGameType;
    private @Nullable LanServerPinger lanPinger;
    private @Nullable UUID uuid;
    private int previousSimulationDistance = 0;
    private volatile List<SimpleGizmoCollector.GizmoInstance> latestTicksGizmos = new ArrayList<>();
    private final SimpleGizmoCollector gizmoCollector = new SimpleGizmoCollector();

    public IntegratedServer(
        Thread serverThread,
        Minecraft minecraft,
        LevelStorageSource.LevelStorageAccess storageSource,
        PackRepository packRepository,
        WorldStem worldStem,
        Services services,
        LevelLoadListener levelLoadListener
    ) {
        super(serverThread, storageSource, packRepository, worldStem, minecraft.getProxy(), minecraft.getFixerUpper(), services, levelLoadListener);
        this.setSingleplayerProfile(minecraft.getGameProfile());
        this.setDemo(minecraft.isDemo());
        this.setPlayerList(new IntegratedPlayerList(this, this.registries(), this.playerDataStorage));
        this.minecraft = minecraft;
    }

    @Override
    public boolean initServer() {
        LOGGER.info("Starting integrated minecraft server version {}", SharedConstants.getCurrentVersion().name());
        this.setUsesAuthentication(true);
        this.initializeKeyPair();
        net.neoforged.neoforge.server.ServerLifecycleHooks.handleServerAboutToStart(this);
        this.loadLevel();
        GameProfile gameprofile = this.getSingleplayerProfile();
        String s = this.getWorldData().getLevelName();
        this.setMotd(gameprofile != null ? gameprofile.name() + " - " + s : s);
        net.neoforged.neoforge.server.ServerLifecycleHooks.handleServerStarting(this);
        return true;
    }

    @Override
    public boolean isPaused() {
        return this.paused;
    }

    @Override
    public void processPacketsAndTick(boolean p_470835_) {
        try (Gizmos.TemporaryCollection gizmos$temporarycollection = Gizmos.withCollector(this.gizmoCollector)) {
            super.processPacketsAndTick(p_470835_);
        }

        if (this.tickRateManager().runsNormally()) {
            this.latestTicksGizmos = this.gizmoCollector.drainGizmos();
        }
    }

    /**
     * Main function called by run() every loop.
     */
    @Override
    public void tickServer(BooleanSupplier hasTimeLeft) {
        boolean flag = this.paused;
        this.paused = Minecraft.getInstance().isPaused() || this.getPlayerList().getPlayers().isEmpty();
        ProfilerFiller profilerfiller = Profiler.get();
        if (!flag && this.paused) {
            profilerfiller.push("autoSave");
            LOGGER.info("Saving and pausing game...");
            this.saveEverything(false, false, false);
            profilerfiller.pop();
        }

        if (this.paused) {
            this.tickPaused();
        } else {
            if (flag) {
                this.forceTimeSynchronization();
            }

            super.tickServer(hasTimeLeft);
            int i = Math.max(2, this.minecraft.options.renderDistance().get());
            if (i != this.getPlayerList().getViewDistance()) {
                LOGGER.info("Changing view distance to {}, from {}", i, this.getPlayerList().getViewDistance());
                this.getPlayerList().setViewDistance(i);
            }

            int j = Math.max(2, this.minecraft.options.simulationDistance().get());
            if (j != this.previousSimulationDistance) {
                LOGGER.info("Changing simulation distance to {}, from {}", j, this.previousSimulationDistance);
                this.getPlayerList().setSimulationDistance(j);
                this.previousSimulationDistance = j;
            }
        }
    }

    protected LocalSampleLogger getTickTimeLogger() {
        return this.minecraft.getDebugOverlay().getTickTimeLogger();
    }

    @Override
    public boolean isTickTimeLoggingEnabled() {
        return true;
    }

    private void tickPaused() {
        this.tickConnection();

        for (ServerPlayer serverplayer : this.getPlayerList().getPlayers()) {
            serverplayer.awardStat(Stats.TOTAL_WORLD_TIME);
        }
    }

    @Override
    public boolean shouldRconBroadcast() {
        return true;
    }

    @Override
    public boolean shouldInformAdmins() {
        return true;
    }

    @Override
    public Path getServerDirectory() {
        return this.minecraft.gameDirectory.toPath();
    }

    @Override
    public boolean isDedicatedServer() {
        return false;
    }

    @Override
    public int getRateLimitPacketsPerSecond() {
        return 0;
    }

    @Override
    public boolean useNativeTransport() {
        return this.minecraft.options.useNativeTransport();
    }

    /**
     * Called on exit from the main run() loop.
     */
    @Override
    public void onServerCrash(CrashReport report) {
        this.minecraft.delayCrashRaw(report);
    }

    @Override
    public SystemReport fillServerSystemReport(SystemReport p_174970_) {
        p_174970_.setDetail("Type", "Integrated Server (map_client.txt)");
        p_174970_.setDetail("Is Modded", () -> this.getModdedStatus().fullDescription());
        p_174970_.setDetail("Launched Version", this.minecraft::getLaunchedVersion);
        return p_174970_;
    }

    @Override
    public ModCheck getModdedStatus() {
        return Minecraft.checkModStatus().merge(super.getModdedStatus());
    }

    @Override
    public boolean publishServer(@Nullable GameType gameMode, boolean cheats, int port) {
        try {
            this.minecraft.prepareForMultiplayer();
            this.minecraft.getConnection().prepareKeyPair();
            this.getConnection().startTcpServerListener(null, port);
            LOGGER.info("Started serving on {}", port);
            this.publishedPort = port;
            this.lanPinger = new LanServerPinger(this.getMotd(), port + "");
            this.lanPinger.start();
            this.publishedGameType = gameMode;
            this.getPlayerList().setAllowCommandsForAllPlayers(cheats);
            PermissionSet permissionset = this.getProfilePermissions(this.minecraft.player.nameAndId());
            this.minecraft.player.setPermissions(permissionset);

            for (ServerPlayer serverplayer : this.getPlayerList().getPlayers()) {
                this.getCommands().sendCommands(serverplayer);
            }

            return true;
        } catch (IOException ioexception) {
            return false;
        }
    }

    @Override
    public void stopServer() {
        super.stopServer();
        if (this.lanPinger != null) {
            this.lanPinger.interrupt();
            this.lanPinger = null;
        }
    }

    /**
     * Sets the serverRunning variable to false, in order to get the server to shut down.
     */
    @Override
    public void halt(boolean waitForServer) {
        if (isRunning())
        this.executeBlocking(() -> {
            for (ServerPlayer serverplayer : Lists.newArrayList(this.getPlayerList().getPlayers())) {
                if (!serverplayer.getUUID().equals(this.uuid)) {
                    this.getPlayerList().remove(serverplayer);
                }
            }
        });
        super.halt(waitForServer);
        if (this.lanPinger != null) {
            this.lanPinger.interrupt();
            this.lanPinger = null;
        }
    }

    @Override
    public boolean isPublished() {
        return this.publishedPort > -1;
    }

    @Override
    public int getPort() {
        return this.publishedPort;
    }

    /**
     * Sets the game type for all worlds.
     */
    @Override
    public void setDefaultGameType(GameType gameMode) {
        super.setDefaultGameType(gameMode);
        this.publishedGameType = null;
    }

    @Override
    public LevelBasedPermissionSet operatorUserPermissions() {
        return LevelBasedPermissionSet.GAMEMASTER;
    }

    public LevelBasedPermissionSet getFunctionCompilationPermissions() {
        return LevelBasedPermissionSet.GAMEMASTER;
    }

    public void setUUID(UUID uuid) {
        this.uuid = uuid;
    }

    @Override
    public boolean isSingleplayerOwner(NameAndId p_433537_) {
        return this.getSingleplayerProfile() != null && p_433537_.name().equalsIgnoreCase(this.getSingleplayerProfile().name());
    }

    @Override
    public int getScaledTrackingDistance(int p_120056_) {
        return (int)(this.minecraft.options.entityDistanceScaling().get() * p_120056_);
    }

    @Override
    public boolean forceSynchronousWrites() {
        return this.minecraft.options.syncWrites;
    }

    @Override
    public @Nullable GameType getForcedGameType() {
        return this.isPublished() && !this.isHardcore() ? MoreObjects.firstNonNull(this.publishedGameType, this.worldData.getGameType()) : null;
    }

    @Override
    public GlobalPos selectLevelLoadFocusPos() {
        CompoundTag compoundtag = this.worldData.getLoadedPlayerTag();
        if (compoundtag == null) {
            return super.selectLevelLoadFocusPos();
        } else {
            try (ProblemReporter.ScopedCollector problemreporter$scopedcollector = new ProblemReporter.ScopedCollector(LOGGER)) {
                ValueInput valueinput = TagValueInput.create(problemreporter$scopedcollector, this.registryAccess(), compoundtag);
                ServerPlayer.SavedPosition serverplayer$savedposition = valueinput.read(ServerPlayer.SavedPosition.MAP_CODEC)
                    .orElse(ServerPlayer.SavedPosition.EMPTY);
                if (serverplayer$savedposition.dimension().isPresent() && serverplayer$savedposition.position().isPresent()) {
                    return new GlobalPos(serverplayer$savedposition.dimension().get(), BlockPos.containing(serverplayer$savedposition.position().get()));
                }
            }

            return super.selectLevelLoadFocusPos();
        }
    }

    @Override
    public boolean saveEverything(boolean p_332035_, boolean p_331844_, boolean p_330847_) {
        boolean flag = super.saveEverything(p_332035_, p_331844_, p_330847_);
        this.warnOnLowDiskSpace();
        return flag;
    }

    private void warnOnLowDiskSpace() {
        if (this.storageSource.checkForLowDiskSpace()) {
            this.minecraft.execute(() -> SystemToast.onLowDiskSpace(this.minecraft));
        }
    }

    @Override
    public void reportChunkLoadFailure(Throwable p_352390_, RegionStorageInfo p_352401_, ChunkPos p_330829_) {
        super.reportChunkLoadFailure(p_352390_, p_352401_, p_330829_);
        this.warnOnLowDiskSpace();
        this.minecraft.execute(() -> SystemToast.onChunkLoadFailure(this.minecraft, p_330829_));
    }

    @Override
    public void reportChunkSaveFailure(Throwable p_352264_, RegionStorageInfo p_352355_, ChunkPos p_331440_) {
        super.reportChunkSaveFailure(p_352264_, p_352355_, p_331440_);
        this.warnOnLowDiskSpace();
        this.minecraft.execute(() -> SystemToast.onChunkSaveFailure(this.minecraft, p_331440_));
    }

    @Override
    public int getMaxPlayers() {
        return 8;
    }

    public Collection<SimpleGizmoCollector.GizmoInstance> getPerTickGizmos() {
        return this.latestTicksGizmos;
    }
}
