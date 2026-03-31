package net.minecraft.server.jsonrpc;

import net.minecraft.core.Holder;
import net.minecraft.server.jsonrpc.api.PlayerDto;
import net.minecraft.server.jsonrpc.internalapi.MinecraftApi;
import net.minecraft.server.jsonrpc.methods.BanlistService;
import net.minecraft.server.jsonrpc.methods.GameRulesService;
import net.minecraft.server.jsonrpc.methods.IpBanlistService;
import net.minecraft.server.jsonrpc.methods.OperatorService;
import net.minecraft.server.jsonrpc.methods.ServerStateService;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.notifications.NotificationService;
import net.minecraft.server.players.IpBanListEntry;
import net.minecraft.server.players.NameAndId;
import net.minecraft.server.players.ServerOpListEntry;
import net.minecraft.server.players.UserBanListEntry;
import net.minecraft.world.level.gamerules.GameRule;

public class JsonRpcNotificationService implements NotificationService {
    private final ManagementServer managementServer;
    private final MinecraftApi minecraftApi;

    public JsonRpcNotificationService(MinecraftApi minecraftApi, ManagementServer managementServer) {
        this.minecraftApi = minecraftApi;
        this.managementServer = managementServer;
    }

    @Override
    public void playerJoined(ServerPlayer p_442849_) {
        this.broadcastNotification(OutgoingRpcMethods.PLAYER_JOINED, PlayerDto.from(p_442849_));
    }

    @Override
    public void playerLeft(ServerPlayer p_443228_) {
        this.broadcastNotification(OutgoingRpcMethods.PLAYER_LEFT, PlayerDto.from(p_443228_));
    }

    @Override
    public void serverStarted() {
        this.broadcastNotification(OutgoingRpcMethods.SERVER_STARTED);
    }

    @Override
    public void serverShuttingDown() {
        this.broadcastNotification(OutgoingRpcMethods.SERVER_SHUTTING_DOWN);
    }

    @Override
    public void serverSaveStarted() {
        this.broadcastNotification(OutgoingRpcMethods.SERVER_SAVE_STARTED);
    }

    @Override
    public void serverSaveCompleted() {
        this.broadcastNotification(OutgoingRpcMethods.SERVER_SAVE_COMPLETED);
    }

    @Override
    public void serverActivityOccured() {
        this.broadcastNotification(OutgoingRpcMethods.SERVER_ACTIVITY_OCCURRED);
    }

    @Override
    public void playerOped(ServerOpListEntry p_443573_) {
        this.broadcastNotification(OutgoingRpcMethods.PLAYER_OPED, OperatorService.OperatorDto.from(p_443573_));
    }

    @Override
    public void playerDeoped(ServerOpListEntry p_443150_) {
        this.broadcastNotification(OutgoingRpcMethods.PLAYER_DEOPED, OperatorService.OperatorDto.from(p_443150_));
    }

    @Override
    public void playerAddedToAllowlist(NameAndId p_443066_) {
        this.broadcastNotification(OutgoingRpcMethods.PLAYER_ADDED_TO_ALLOWLIST, PlayerDto.from(p_443066_));
    }

    @Override
    public void playerRemovedFromAllowlist(NameAndId p_443379_) {
        this.broadcastNotification(OutgoingRpcMethods.PLAYER_REMOVED_FROM_ALLOWLIST, PlayerDto.from(p_443379_));
    }

    @Override
    public void ipBanned(IpBanListEntry p_442552_) {
        this.broadcastNotification(OutgoingRpcMethods.IP_BANNED, IpBanlistService.IpBanDto.from(p_442552_));
    }

    @Override
    public void ipUnbanned(String p_443355_) {
        this.broadcastNotification(OutgoingRpcMethods.IP_UNBANNED, p_443355_);
    }

    @Override
    public void playerBanned(UserBanListEntry p_442976_) {
        this.broadcastNotification(OutgoingRpcMethods.PLAYER_BANNED, BanlistService.UserBanDto.from(p_442976_));
    }

    @Override
    public void playerUnbanned(NameAndId p_443351_) {
        this.broadcastNotification(OutgoingRpcMethods.PLAYER_UNBANNED, PlayerDto.from(p_443351_));
    }

    @Override
    public <T> void onGameRuleChanged(GameRule<T> p_461175_, T p_460639_) {
        this.broadcastNotification(OutgoingRpcMethods.GAMERULE_CHANGED, GameRulesService.getTypedRule(this.minecraftApi, p_461175_, p_460639_));
    }

    @Override
    public void statusHeartbeat() {
        this.broadcastNotification(OutgoingRpcMethods.STATUS_HEARTBEAT, ServerStateService.status(this.minecraftApi));
    }

    private void broadcastNotification(Holder.Reference<? extends OutgoingRpcMethod<Void, ?>> method) {
        this.managementServer.forEachConnection(p_450829_ -> p_450829_.sendNotification(method));
    }

    private <Params> void broadcastNotification(Holder.Reference<? extends OutgoingRpcMethod<Params, ?>> method, Params params) {
        this.managementServer.forEachConnection(p_450827_ -> p_450827_.sendNotification(method, params));
    }
}
