package net.minecraft.server.jsonrpc.internalapi;

import java.util.Collection;
import java.util.Optional;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.jsonrpc.JsonRpcLogger;
import net.minecraft.server.jsonrpc.methods.ClientInfo;
import net.minecraft.server.permissions.LevelBasedPermissionSet;
import net.minecraft.server.permissions.PermissionLevel;
import net.minecraft.server.players.NameAndId;
import net.minecraft.server.players.ServerOpListEntry;

public class MinecraftOperatorListServiceImpl implements MinecraftOperatorListService {
    private final MinecraftServer minecraftServer;
    private final JsonRpcLogger jsonrpcLogger;

    public MinecraftOperatorListServiceImpl(MinecraftServer server, JsonRpcLogger jsonrpcLogger) {
        this.minecraftServer = server;
        this.jsonrpcLogger = jsonrpcLogger;
    }

    @Override
    public Collection<ServerOpListEntry> getEntries() {
        return this.minecraftServer.getPlayerList().getOps().getEntries();
    }

    @Override
    public void op(NameAndId p_449132_, Optional<PermissionLevel> p_449016_, Optional<Boolean> p_449888_, ClientInfo p_449789_) {
        this.jsonrpcLogger.log(p_449789_, "Op '{}'", p_449132_);
        this.minecraftServer.getPlayerList().op(p_449132_, p_449016_.map(LevelBasedPermissionSet::forLevel), p_449888_);
    }

    @Override
    public void op(NameAndId p_449986_, ClientInfo p_449345_) {
        this.jsonrpcLogger.log(p_449345_, "Op '{}'", p_449986_);
        this.minecraftServer.getPlayerList().op(p_449986_);
    }

    @Override
    public void deop(NameAndId p_449033_, ClientInfo p_449483_) {
        this.jsonrpcLogger.log(p_449483_, "Deop '{}'", p_449033_);
        this.minecraftServer.getPlayerList().deop(p_449033_);
    }

    @Override
    public void clear(ClientInfo p_449425_) {
        this.jsonrpcLogger.log(p_449425_, "Clear operator list");
        this.minecraftServer.getPlayerList().getOps().clear();
    }
}
