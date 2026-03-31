package net.minecraft.server.jsonrpc.internalapi;

import net.minecraft.server.dedicated.DedicatedServer;
import net.minecraft.server.jsonrpc.JsonRpcLogger;
import net.minecraft.server.jsonrpc.methods.ClientInfo;
import net.minecraft.server.permissions.LevelBasedPermissionSet;
import net.minecraft.world.Difficulty;
import net.minecraft.world.level.GameType;

public class MinecraftServerSettingsServiceImpl implements MinecraftServerSettingsService {
    private final DedicatedServer server;
    private final JsonRpcLogger jsonrpcLogger;

    public MinecraftServerSettingsServiceImpl(DedicatedServer server, JsonRpcLogger jsonrpcLogger) {
        this.server = server;
        this.jsonrpcLogger = jsonrpcLogger;
    }

    @Override
    public boolean isAutoSave() {
        return this.server.isAutoSave();
    }

    @Override
    public boolean setAutoSave(boolean p_449572_, ClientInfo p_449740_) {
        this.jsonrpcLogger.log(p_449740_, "Update autosave from {} to {}", this.isAutoSave(), p_449572_);
        this.server.setAutoSave(p_449572_);
        return this.isAutoSave();
    }

    @Override
    public Difficulty getDifficulty() {
        return this.server.getWorldData().getDifficulty();
    }

    @Override
    public Difficulty setDifficulty(Difficulty p_449284_, ClientInfo p_449272_) {
        this.jsonrpcLogger.log(p_449272_, "Update difficulty from '{}' to '{}'", this.getDifficulty(), p_449284_);
        this.server.setDifficulty(p_449284_);
        return this.getDifficulty();
    }

    @Override
    public boolean isEnforceWhitelist() {
        return this.server.isEnforceWhitelist();
    }

    @Override
    public boolean setEnforceWhitelist(boolean p_449089_, ClientInfo p_449567_) {
        this.jsonrpcLogger.log(p_449567_, "Update enforce allowlist from {} to {}", this.isEnforceWhitelist(), p_449089_);
        this.server.setEnforceWhitelist(p_449089_);
        this.server.kickUnlistedPlayers();
        return this.isEnforceWhitelist();
    }

    @Override
    public boolean isUsingWhitelist() {
        return this.server.isUsingWhitelist();
    }

    @Override
    public boolean setUsingWhitelist(boolean p_449350_, ClientInfo p_449226_) {
        this.jsonrpcLogger.log(p_449226_, "Update using allowlist from {} to {}", this.isUsingWhitelist(), p_449350_);
        this.server.setUsingWhitelist(p_449350_);
        this.server.kickUnlistedPlayers();
        return this.isUsingWhitelist();
    }

    @Override
    public int getMaxPlayers() {
        return this.server.getMaxPlayers();
    }

    @Override
    public int setMaxPlayers(int p_449083_, ClientInfo p_449268_) {
        this.jsonrpcLogger.log(p_449268_, "Update max players from {} to {}", this.getMaxPlayers(), p_449083_);
        this.server.setMaxPlayers(p_449083_);
        return this.getMaxPlayers();
    }

    @Override
    public int getPauseWhenEmptySeconds() {
        return this.server.pauseWhenEmptySeconds();
    }

    @Override
    public int setPauseWhenEmptySeconds(int p_449063_, ClientInfo p_449539_) {
        this.jsonrpcLogger.log(p_449539_, "Update pause when empty from {} seconds to {} seconds", this.getPauseWhenEmptySeconds(), p_449063_);
        this.server.setPauseWhenEmptySeconds(p_449063_);
        return this.getPauseWhenEmptySeconds();
    }

    @Override
    public int getPlayerIdleTimeout() {
        return this.server.playerIdleTimeout();
    }

    @Override
    public int setPlayerIdleTimeout(int p_449383_, ClientInfo p_449553_) {
        this.jsonrpcLogger.log(p_449553_, "Update player idle timeout from {} minutes to {} minutes", this.getPlayerIdleTimeout(), p_449383_);
        this.server.setPlayerIdleTimeout(p_449383_);
        return this.getPlayerIdleTimeout();
    }

    @Override
    public boolean allowFlight() {
        return this.server.allowFlight();
    }

    @Override
    public boolean setAllowFlight(boolean p_449362_, ClientInfo p_449265_) {
        this.jsonrpcLogger.log(p_449265_, "Update allow flight from {} to {}", this.allowFlight(), p_449362_);
        this.server.setAllowFlight(p_449362_);
        return this.allowFlight();
    }

    @Override
    public int getSpawnProtectionRadius() {
        return this.server.spawnProtectionRadius();
    }

    @Override
    public int setSpawnProtectionRadius(int p_449901_, ClientInfo p_449480_) {
        this.jsonrpcLogger.log(p_449480_, "Update spawn protection radius from {} to {}", this.getSpawnProtectionRadius(), p_449901_);
        this.server.setSpawnProtectionRadius(p_449901_);
        return this.getSpawnProtectionRadius();
    }

    @Override
    public String getMotd() {
        return this.server.getMotd();
    }

    @Override
    public String setMotd(String p_449814_, ClientInfo p_449623_) {
        this.jsonrpcLogger.log(p_449623_, "Update MOTD from '{}' to '{}'", this.getMotd(), p_449814_);
        this.server.setMotd(p_449814_);
        return this.getMotd();
    }

    @Override
    public boolean forceGameMode() {
        return this.server.forceGameMode();
    }

    @Override
    public boolean setForceGameMode(boolean p_449742_, ClientInfo p_449617_) {
        this.jsonrpcLogger.log(p_449617_, "Update force game mode from {} to {}", this.forceGameMode(), p_449742_);
        this.server.setForceGameMode(p_449742_);
        return this.forceGameMode();
    }

    @Override
    public GameType getGameMode() {
        return this.server.gameMode();
    }

    @Override
    public GameType setGameMode(GameType p_449163_, ClientInfo p_449376_) {
        this.jsonrpcLogger.log(p_449376_, "Update game mode from '{}' to '{}'", this.getGameMode(), p_449163_);
        this.server.setGameMode(p_449163_);
        return this.getGameMode();
    }

    @Override
    public int getViewDistance() {
        return this.server.viewDistance();
    }

    @Override
    public int setViewDistance(int p_449899_, ClientInfo p_449679_) {
        this.jsonrpcLogger.log(p_449679_, "Update view distance from {} to {}", this.getViewDistance(), p_449899_);
        this.server.setViewDistance(p_449899_);
        return this.getViewDistance();
    }

    @Override
    public int getSimulationDistance() {
        return this.server.simulationDistance();
    }

    @Override
    public int setSimulationDistance(int p_449192_, ClientInfo p_449530_) {
        this.jsonrpcLogger.log(p_449530_, "Update simulation distance from {} to {}", this.getSimulationDistance(), p_449192_);
        this.server.setSimulationDistance(p_449192_);
        return this.getSimulationDistance();
    }

    @Override
    public boolean acceptsTransfers() {
        return this.server.acceptsTransfers();
    }

    @Override
    public boolean setAcceptsTransfers(boolean p_449336_, ClientInfo p_449105_) {
        this.jsonrpcLogger.log(p_449105_, "Update accepts transfers from {} to {}", this.acceptsTransfers(), p_449336_);
        this.server.setAcceptsTransfers(p_449336_);
        return this.acceptsTransfers();
    }

    @Override
    public int getStatusHeartbeatInterval() {
        return this.server.statusHeartbeatInterval();
    }

    @Override
    public int setStatusHeartbeatInterval(int p_449571_, ClientInfo p_449858_) {
        this.jsonrpcLogger.log(p_449858_, "Update status heartbeat interval from {} to {}", this.getStatusHeartbeatInterval(), p_449571_);
        this.server.setStatusHeartbeatInterval(p_449571_);
        return this.getStatusHeartbeatInterval();
    }

    @Override
    public LevelBasedPermissionSet getOperatorUserPermissions() {
        return this.server.operatorUserPermissions();
    }

    @Override
    public LevelBasedPermissionSet setOperatorUserPermissions(LevelBasedPermissionSet p_455845_, ClientInfo p_455711_) {
        this.jsonrpcLogger.log(p_455711_, "Update operator user permission level from {} to {}", this.getOperatorUserPermissions(), p_455845_.level());
        this.server.setOperatorUserPermissions(p_455845_);
        return this.getOperatorUserPermissions();
    }

    @Override
    public boolean hidesOnlinePlayers() {
        return this.server.hidesOnlinePlayers();
    }

    @Override
    public boolean setHidesOnlinePlayers(boolean p_449490_, ClientInfo p_449609_) {
        this.jsonrpcLogger.log(p_449609_, "Update hides online players from {} to {}", this.hidesOnlinePlayers(), p_449490_);
        this.server.setHidesOnlinePlayers(p_449490_);
        return this.hidesOnlinePlayers();
    }

    @Override
    public boolean repliesToStatus() {
        return this.server.repliesToStatus();
    }

    @Override
    public boolean setRepliesToStatus(boolean p_449554_, ClientInfo p_449924_) {
        this.jsonrpcLogger.log(p_449924_, "Update replies to status from {} to {}", this.repliesToStatus(), p_449554_);
        this.server.setRepliesToStatus(p_449554_);
        return this.repliesToStatus();
    }

    @Override
    public int getEntityBroadcastRangePercentage() {
        return this.server.entityBroadcastRangePercentage();
    }

    @Override
    public int setEntityBroadcastRangePercentage(int p_449141_, ClientInfo p_449538_) {
        this.jsonrpcLogger.log(p_449538_, "Update entity broadcast range percentage from {}% to {}%", this.getEntityBroadcastRangePercentage(), p_449141_);
        this.server.setEntityBroadcastRangePercentage(p_449141_);
        return this.getEntityBroadcastRangePercentage();
    }
}
