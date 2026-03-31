package net.minecraft.world.item;

import net.minecraft.network.protocol.game.ClientboundCooldownPacket;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;

public class ServerItemCooldowns extends ItemCooldowns {
    private final ServerPlayer player;

    public ServerItemCooldowns(ServerPlayer player) {
        this.player = player;
    }

    @Override
    protected void onCooldownStarted(Identifier p_467046_, int p_43070_) {
        super.onCooldownStarted(p_467046_, p_43070_);
        this.player.connection.send(new ClientboundCooldownPacket(p_467046_, p_43070_));
    }

    @Override
    protected void onCooldownEnded(Identifier p_469022_) {
        super.onCooldownEnded(p_469022_);
        this.player.connection.send(new ClientboundCooldownPacket(p_469022_, 0));
    }
}
