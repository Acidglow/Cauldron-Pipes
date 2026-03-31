package net.minecraft.server.jsonrpc.internalapi;

import java.util.Collection;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.jsonrpc.JsonRpcLogger;
import net.minecraft.server.jsonrpc.methods.ClientInfo;
import net.minecraft.server.players.IpBanListEntry;
import net.minecraft.server.players.NameAndId;
import net.minecraft.server.players.UserBanListEntry;

public class MinecraftBanListServiceImpl implements MinecraftBanListService {
    private final MinecraftServer server;
    private final JsonRpcLogger jsonrpcLogger;

    public MinecraftBanListServiceImpl(MinecraftServer server, JsonRpcLogger jsonrpcLogger) {
        this.server = server;
        this.jsonrpcLogger = jsonrpcLogger;
    }

    @Override
    public void addUserBan(UserBanListEntry p_449151_, ClientInfo p_449951_) {
        this.jsonrpcLogger.log(p_449951_, "Add player '{}' to banlist. Reason: '{}'", p_449151_.getDisplayName(), p_449151_.getReasonMessage().getString());
        this.server.getPlayerList().getBans().add(p_449151_);
    }

    @Override
    public void removeUserBan(NameAndId p_449835_, ClientInfo p_449514_) {
        this.jsonrpcLogger.log(p_449514_, "Remove player '{}' from banlist", p_449835_);
        this.server.getPlayerList().getBans().remove(p_449835_);
    }

    @Override
    public void clearUserBans(ClientInfo p_449440_) {
        this.server.getPlayerList().getBans().clear();
    }

    @Override
    public Collection<UserBanListEntry> getUserBanEntries() {
        return this.server.getPlayerList().getBans().getEntries();
    }

    @Override
    public Collection<IpBanListEntry> getIpBanEntries() {
        return this.server.getPlayerList().getIpBans().getEntries();
    }

    @Override
    public void addIpBan(IpBanListEntry p_449099_, ClientInfo p_449304_) {
        this.jsonrpcLogger.log(p_449304_, "Add ip '{}' to ban list", p_449099_.getUser());
        this.server.getPlayerList().getIpBans().add(p_449099_);
    }

    @Override
    public void clearIpBans(ClientInfo p_449795_) {
        this.jsonrpcLogger.log(p_449795_, "Clear ip ban list");
        this.server.getPlayerList().getIpBans().clear();
    }

    @Override
    public void removeIpBan(String p_449852_, ClientInfo p_449413_) {
        this.jsonrpcLogger.log(p_449413_, "Remove ip '{}' from ban list", p_449852_);
        this.server.getPlayerList().getIpBans().remove(p_449852_);
    }
}
