package net.minecraft.server.waypoints;

import com.google.common.collect.HashBasedTable;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import com.google.common.collect.Table;
import com.google.common.collect.Tables;
import com.google.common.collect.Sets.SetView;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.gamerules.GameRules;
import net.minecraft.world.waypoints.WaypointManager;
import net.minecraft.world.waypoints.WaypointTransmitter;

public class ServerWaypointManager implements WaypointManager<WaypointTransmitter> {
    private final Set<WaypointTransmitter> waypoints = new HashSet<>();
    private final Set<ServerPlayer> players = new HashSet<>();
    private final Table<ServerPlayer, WaypointTransmitter, WaypointTransmitter.Connection> connections = HashBasedTable.create();

    public void trackWaypoint(WaypointTransmitter p_415909_) {
        this.waypoints.add(p_415909_);

        for (ServerPlayer serverplayer : this.players) {
            this.createConnection(serverplayer, p_415909_);
        }
    }

    public void updateWaypoint(WaypointTransmitter p_415954_) {
        if (this.waypoints.contains(p_415954_)) {
            Map<ServerPlayer, WaypointTransmitter.Connection> map = Tables.transpose(this.connections).row(p_415954_);
            SetView<ServerPlayer> setview = Sets.difference(this.players, map.keySet());

            for (Entry<ServerPlayer, WaypointTransmitter.Connection> entry : ImmutableSet.copyOf(map.entrySet())) {
                this.updateConnection(entry.getKey(), p_415954_, entry.getValue());
            }

            for (ServerPlayer serverplayer : setview) {
                this.createConnection(serverplayer, p_415954_);
            }
        }
    }

    public void untrackWaypoint(WaypointTransmitter p_416348_) {
        this.connections.column(p_416348_).forEach((p_415970_, p_416453_) -> p_416453_.disconnect());
        Tables.transpose(this.connections).row(p_416348_).clear();
        this.waypoints.remove(p_416348_);
    }

    public void addPlayer(ServerPlayer player) {
        this.players.add(player);

        for (WaypointTransmitter waypointtransmitter : this.waypoints) {
            this.createConnection(player, waypointtransmitter);
        }

        if (player.isTransmittingWaypoint()) {
            this.trackWaypoint((WaypointTransmitter)player);
        }
    }

    public void updatePlayer(ServerPlayer player) {
        Map<WaypointTransmitter, WaypointTransmitter.Connection> map = this.connections.row(player);
        SetView<WaypointTransmitter> setview = Sets.difference(this.waypoints, map.keySet());

        for (Entry<WaypointTransmitter, WaypointTransmitter.Connection> entry : ImmutableSet.copyOf(map.entrySet())) {
            this.updateConnection(player, entry.getKey(), entry.getValue());
        }

        for (WaypointTransmitter waypointtransmitter : setview) {
            this.createConnection(player, waypointtransmitter);
        }
    }

    public void removePlayer(ServerPlayer player) {
        this.connections.row(player).values().removeIf(p_415833_ -> {
            p_415833_.disconnect();
            return true;
        });
        this.untrackWaypoint((WaypointTransmitter)player);
        this.players.remove(player);
    }

    public void breakAllConnections() {
        this.connections.values().forEach(WaypointTransmitter.Connection::disconnect);
        this.connections.clear();
    }

    public void remakeConnections(WaypointTransmitter waypoint) {
        for (ServerPlayer serverplayer : this.players) {
            this.createConnection(serverplayer, waypoint);
        }
    }

    public Set<WaypointTransmitter> transmitters() {
        return this.waypoints;
    }

    private static boolean isLocatorBarEnabledFor(ServerPlayer player) {
        return player.level().getGameRules().get(GameRules.LOCATOR_BAR);
    }

    private void createConnection(ServerPlayer player, WaypointTransmitter waypoint) {
        if (player != waypoint) {
            if (isLocatorBarEnabledFor(player)) {
                waypoint.makeWaypointConnectionWith(player).ifPresentOrElse(p_416381_ -> {
                    this.connections.put(player, waypoint, p_416381_);
                    p_416381_.connect();
                }, () -> {
                    WaypointTransmitter.Connection waypointtransmitter$connection = this.connections.remove(player, waypoint);
                    if (waypointtransmitter$connection != null) {
                        waypointtransmitter$connection.disconnect();
                    }
                });
            }
        }
    }

    private void updateConnection(ServerPlayer player, WaypointTransmitter waypoint, WaypointTransmitter.Connection connection) {
        if (player != waypoint) {
            if (isLocatorBarEnabledFor(player)) {
                if (!connection.isBroken()) {
                    connection.update();
                } else {
                    waypoint.makeWaypointConnectionWith(player).ifPresentOrElse(p_416199_ -> {
                        p_416199_.connect();
                        this.connections.put(player, waypoint, p_416199_);
                    }, () -> {
                        connection.disconnect();
                        this.connections.remove(player, waypoint);
                    });
                }
            }
        }
    }
}
