package net.minecraft.server.jsonrpc.internalapi;

import java.util.Collection;
import net.minecraft.server.dedicated.DedicatedServer;
import net.minecraft.server.jsonrpc.JsonRpcLogger;
import net.minecraft.server.jsonrpc.methods.ClientInfo;
import net.minecraft.server.players.NameAndId;
import net.minecraft.server.players.UserWhiteListEntry;

public class MinecraftAllowListServiceImpl implements MinecraftAllowListService {
    private final DedicatedServer server;
    private final JsonRpcLogger jsonrpcLogger;

    public MinecraftAllowListServiceImpl(DedicatedServer server, JsonRpcLogger jsonrpcLogger) {
        this.server = server;
        this.jsonrpcLogger = jsonrpcLogger;
    }

    @Override
    public Collection<UserWhiteListEntry> getEntries() {
        return this.server.getPlayerList().getWhiteList().getEntries();
    }

    @Override
    public boolean add(UserWhiteListEntry p_449112_, ClientInfo p_449757_) {
        this.jsonrpcLogger.log(p_449757_, "Add player '{}' to allowlist", p_449112_.getUser());
        return this.server.getPlayerList().getWhiteList().add(p_449112_);
    }

    @Override
    public void clear(ClientInfo p_449708_) {
        this.jsonrpcLogger.log(p_449708_, "Clear allowlist");
        this.server.getPlayerList().getWhiteList().clear();
    }

    @Override
    public void remove(NameAndId p_449664_, ClientInfo p_449053_) {
        this.jsonrpcLogger.log(p_449053_, "Remove player '{}' from allowlist", p_449664_);
        this.server.getPlayerList().getWhiteList().remove(p_449664_);
    }

    @Override
    public void kickUnlistedPlayers(ClientInfo p_449108_) {
        this.jsonrpcLogger.log(p_449108_, "Kick unlisted players");
        this.server.kickUnlistedPlayers();
    }
}
