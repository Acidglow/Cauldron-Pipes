package net.minecraft.server.jsonrpc.internalapi;

import java.util.Collection;
import java.util.List;
import net.minecraft.network.chat.Component;
import net.minecraft.server.dedicated.DedicatedServer;
import net.minecraft.server.jsonrpc.JsonRpcLogger;
import net.minecraft.server.jsonrpc.methods.ClientInfo;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

public class MinecraftServerStateServiceImpl implements MinecraftServerStateService {
    private final DedicatedServer server;
    private final JsonRpcLogger jsonrpcLogger;

    public MinecraftServerStateServiceImpl(DedicatedServer server, JsonRpcLogger jsonrpcLogger) {
        this.server = server;
        this.jsonrpcLogger = jsonrpcLogger;
    }

    @Override
    public boolean isReady() {
        return this.server.isReady();
    }

    @Override
    public boolean saveEverything(boolean p_449876_, boolean p_449093_, boolean p_449327_, ClientInfo p_449187_) {
        this.jsonrpcLogger.log(p_449187_, "Save everything. SuppressLogs: {}, flush: {}, force: {}", p_449876_, p_449093_, p_449327_);
        return this.server.saveEverything(p_449876_, p_449093_, p_449327_);
    }

    @Override
    public void halt(boolean p_449962_, ClientInfo p_449535_) {
        this.jsonrpcLogger.log(p_449535_, "Halt server. WaitForShutdown: {}", p_449962_);
        this.server.halt(p_449962_);
    }

    @Override
    public void sendSystemMessage(Component p_449613_, ClientInfo p_449787_) {
        this.jsonrpcLogger.log(p_449787_, "Send system message: '{}'", p_449613_.getString());
        this.server.sendSystemMessage(p_449613_);
    }

    @Override
    public void sendSystemMessage(Component p_449547_, boolean p_449401_, Collection<ServerPlayer> p_449903_, ClientInfo p_449095_) {
        List<String> list = p_449903_.stream().map(Player::getPlainTextName).toList();
        this.jsonrpcLogger.log(p_449095_, "Send system message to '{}' players (overlay: {}): '{}'", list.size(), p_449401_, p_449547_.getString());

        for (ServerPlayer serverplayer : p_449903_) {
            if (p_449401_) {
                serverplayer.sendSystemMessage(p_449547_, true);
            } else {
                serverplayer.sendSystemMessage(p_449547_);
            }
        }
    }

    @Override
    public void broadcastSystemMessage(Component p_449138_, boolean p_449040_, ClientInfo p_449534_) {
        this.jsonrpcLogger.log(p_449534_, "Broadcast system message (overlay: {}): '{}'", p_449040_, p_449138_.getString());

        for (ServerPlayer serverplayer : this.server.getPlayerList().getPlayers()) {
            if (p_449040_) {
                serverplayer.sendSystemMessage(p_449138_, true);
            } else {
                serverplayer.sendSystemMessage(p_449138_);
            }
        }
    }
}
