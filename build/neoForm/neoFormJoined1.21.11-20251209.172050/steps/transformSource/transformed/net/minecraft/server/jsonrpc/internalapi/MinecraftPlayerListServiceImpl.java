package net.minecraft.server.jsonrpc.internalapi;

import com.mojang.authlib.yggdrasil.ProfileResult;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.server.dedicated.DedicatedServer;
import net.minecraft.server.jsonrpc.JsonRpcLogger;
import net.minecraft.server.jsonrpc.methods.ClientInfo;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.NameAndId;
import org.jspecify.annotations.Nullable;

public class MinecraftPlayerListServiceImpl implements MinecraftPlayerListService {
    private final JsonRpcLogger jsonRpcLogger;
    private final DedicatedServer server;

    public MinecraftPlayerListServiceImpl(DedicatedServer server, JsonRpcLogger jsonrpcLogger) {
        this.jsonRpcLogger = jsonrpcLogger;
        this.server = server;
    }

    @Override
    public List<ServerPlayer> getPlayers() {
        return this.server.getPlayerList().getPlayers();
    }

    @Override
    public @Nullable ServerPlayer getPlayer(UUID p_449149_) {
        return this.server.getPlayerList().getPlayer(p_449149_);
    }

    @Override
    public Optional<NameAndId> fetchUserByName(String p_449968_) {
        return this.server.services().nameToIdCache().get(p_449968_);
    }

    @Override
    public Optional<NameAndId> fetchUserById(UUID p_449062_) {
        return Optional.ofNullable(this.server.services().sessionService().fetchProfile(p_449062_, true)).map(p_449360_ -> new NameAndId(p_449360_.profile()));
    }

    @Override
    public Optional<NameAndId> getCachedUserById(UUID p_449602_) {
        return this.server.services().nameToIdCache().get(p_449602_);
    }

    @Override
    public Optional<ServerPlayer> getPlayer(Optional<UUID> p_449225_, Optional<String> p_449869_) {
        if (p_449225_.isPresent()) {
            return Optional.ofNullable(this.server.getPlayerList().getPlayer(p_449225_.get()));
        } else {
            return p_449869_.isPresent() ? Optional.ofNullable(this.server.getPlayerList().getPlayerByName(p_449869_.get())) : Optional.empty();
        }
    }

    @Override
    public List<ServerPlayer> getPlayersWithAddress(String p_449455_) {
        return this.server.getPlayerList().getPlayersWithAddress(p_449455_);
    }

    @Override
    public void remove(ServerPlayer p_449719_, ClientInfo p_449429_) {
        this.server.getPlayerList().remove(p_449719_);
        this.jsonRpcLogger.log(p_449429_, "Remove player '{}'", p_449719_.getPlainTextName());
    }

    @Override
    public @Nullable ServerPlayer getPlayerByName(String p_449366_) {
        return this.server.getPlayerList().getPlayerByName(p_449366_);
    }
}
