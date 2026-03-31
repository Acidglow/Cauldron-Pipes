package net.minecraft.server;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientboundResetScorePacket;
import net.minecraft.network.protocol.game.ClientboundSetDisplayObjectivePacket;
import net.minecraft.network.protocol.game.ClientboundSetObjectivePacket;
import net.minecraft.network.protocol.game.ClientboundSetPlayerTeamPacket;
import net.minecraft.network.protocol.game.ClientboundSetScorePacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.scores.DisplaySlot;
import net.minecraft.world.scores.Objective;
import net.minecraft.world.scores.PlayerScoreEntry;
import net.minecraft.world.scores.PlayerTeam;
import net.minecraft.world.scores.Score;
import net.minecraft.world.scores.ScoreHolder;
import net.minecraft.world.scores.Scoreboard;
import net.minecraft.world.scores.ScoreboardSaveData;
import org.jspecify.annotations.Nullable;

public class ServerScoreboard extends Scoreboard {
    private final MinecraftServer server;
    private final Set<Objective> trackedObjectives = Sets.newHashSet();
    private boolean dirty;

    public ServerScoreboard(MinecraftServer server) {
        this.server = server;
    }

    public void load(ScoreboardSaveData.Packed data) {
        data.objectives().forEach(p_454402_ -> this.loadObjective(p_454402_));
        data.scores().forEach(p_454404_ -> this.loadPlayerScore(p_454404_));
        data.displaySlots().forEach((p_454405_, p_454406_) -> {
            Objective objective = this.getObjective(p_454406_);
            this.setDisplayObjective(p_454405_, objective);
        });
        data.teams().forEach(p_454408_ -> this.loadPlayerTeam(p_454408_));
    }

    private ScoreboardSaveData.Packed store() {
        return new ScoreboardSaveData.Packed(this.packObjectives(), this.packPlayerScores(), this.packDisplaySlots(), this.packPlayerTeams());
    }

    @Override
    protected void onScoreChanged(ScoreHolder p_313858_, Objective p_313953_, Score p_136206_) {
        super.onScoreChanged(p_313858_, p_313953_, p_136206_);
        if (this.trackedObjectives.contains(p_313953_)) {
            this.server
                .getPlayerList()
                .broadcastAll(
                    new ClientboundSetScorePacket(
                        p_313858_.getScoreboardName(),
                        p_313953_.getName(),
                        p_136206_.value(),
                        Optional.ofNullable(p_136206_.display()),
                        Optional.ofNullable(p_136206_.numberFormat())
                    )
                );
        }

        this.setDirty();
    }

    @Override
    protected void onScoreLockChanged(ScoreHolder p_313796_, Objective p_313806_) {
        super.onScoreLockChanged(p_313796_, p_313806_);
        this.setDirty();
    }

    @Override
    public void onPlayerRemoved(ScoreHolder p_313870_) {
        super.onPlayerRemoved(p_313870_);
        this.server.getPlayerList().broadcastAll(new ClientboundResetScorePacket(p_313870_.getScoreboardName(), null));
        this.setDirty();
    }

    @Override
    public void onPlayerScoreRemoved(ScoreHolder p_313777_, Objective p_136213_) {
        super.onPlayerScoreRemoved(p_313777_, p_136213_);
        if (this.trackedObjectives.contains(p_136213_)) {
            this.server.getPlayerList().broadcastAll(new ClientboundResetScorePacket(p_313777_.getScoreboardName(), p_136213_.getName()));
        }

        this.setDirty();
    }

    @Override
    public void setDisplayObjective(DisplaySlot p_294118_, @Nullable Objective p_136200_) {
        Objective objective = this.getDisplayObjective(p_294118_);
        super.setDisplayObjective(p_294118_, p_136200_);
        if (objective != p_136200_ && objective != null) {
            if (this.getObjectiveDisplaySlotCount(objective) > 0) {
                this.server.getPlayerList().broadcastAll(new ClientboundSetDisplayObjectivePacket(p_294118_, p_136200_));
            } else {
                this.stopTrackingObjective(objective);
            }
        }

        if (p_136200_ != null) {
            if (this.trackedObjectives.contains(p_136200_)) {
                this.server.getPlayerList().broadcastAll(new ClientboundSetDisplayObjectivePacket(p_294118_, p_136200_));
            } else {
                this.startTrackingObjective(p_136200_);
            }
        }

        this.setDirty();
    }

    @Override
    public boolean addPlayerToTeam(String p_136215_, PlayerTeam p_136216_) {
        if (super.addPlayerToTeam(p_136215_, p_136216_)) {
            this.server
                .getPlayerList()
                .broadcastAll(ClientboundSetPlayerTeamPacket.createPlayerPacket(p_136216_, p_136215_, ClientboundSetPlayerTeamPacket.Action.ADD));
            this.updatePlayerWaypoint(p_136215_);
            this.setDirty();
            return true;
        } else {
            return false;
        }
    }

    /**
     * Removes the given username from the given ScorePlayerTeam. If the player is not on the team then an IllegalStateException is thrown.
     */
    @Override
    public void removePlayerFromTeam(String username, PlayerTeam playerTeam) {
        super.removePlayerFromTeam(username, playerTeam);
        this.server
            .getPlayerList()
            .broadcastAll(ClientboundSetPlayerTeamPacket.createPlayerPacket(playerTeam, username, ClientboundSetPlayerTeamPacket.Action.REMOVE));
        this.updatePlayerWaypoint(username);
        this.setDirty();
    }

    @Override
    public void onObjectiveAdded(Objective objective) {
        super.onObjectiveAdded(objective);
        this.setDirty();
    }

    @Override
    public void onObjectiveChanged(Objective objective) {
        super.onObjectiveChanged(objective);
        if (this.trackedObjectives.contains(objective)) {
            this.server.getPlayerList().broadcastAll(new ClientboundSetObjectivePacket(objective, 2));
        }

        this.setDirty();
    }

    @Override
    public void onObjectiveRemoved(Objective objective) {
        super.onObjectiveRemoved(objective);
        if (this.trackedObjectives.contains(objective)) {
            this.stopTrackingObjective(objective);
        }

        this.setDirty();
    }

    @Override
    public void onTeamAdded(PlayerTeam playerTeam) {
        super.onTeamAdded(playerTeam);
        this.server.getPlayerList().broadcastAll(ClientboundSetPlayerTeamPacket.createAddOrModifyPacket(playerTeam, true));
        this.setDirty();
    }

    @Override
    public void onTeamChanged(PlayerTeam playerTeam) {
        super.onTeamChanged(playerTeam);
        this.server.getPlayerList().broadcastAll(ClientboundSetPlayerTeamPacket.createAddOrModifyPacket(playerTeam, false));
        this.updateTeamWaypoints(playerTeam);
        this.setDirty();
    }

    @Override
    public void onTeamRemoved(PlayerTeam playerTeam) {
        super.onTeamRemoved(playerTeam);
        this.server.getPlayerList().broadcastAll(ClientboundSetPlayerTeamPacket.createRemovePacket(playerTeam));
        this.updateTeamWaypoints(playerTeam);
        this.setDirty();
    }

    protected void setDirty() {
        this.dirty = true;
    }

    public void storeToSaveDataIfDirty(ScoreboardSaveData saveData) {
        if (this.dirty) {
            this.dirty = false;
            saveData.setData(this.store());
        }
    }

    public List<Packet<?>> getStartTrackingPackets(Objective objective) {
        List<Packet<?>> list = Lists.newArrayList();
        list.add(new ClientboundSetObjectivePacket(objective, 0));

        for (DisplaySlot displayslot : DisplaySlot.values()) {
            if (this.getDisplayObjective(displayslot) == objective) {
                list.add(new ClientboundSetDisplayObjectivePacket(displayslot, objective));
            }
        }

        for (PlayerScoreEntry playerscoreentry : this.listPlayerScores(objective)) {
            list.add(
                new ClientboundSetScorePacket(
                    playerscoreentry.owner(),
                    objective.getName(),
                    playerscoreentry.value(),
                    Optional.ofNullable(playerscoreentry.display()),
                    Optional.ofNullable(playerscoreentry.numberFormatOverride())
                )
            );
        }

        return list;
    }

    public void startTrackingObjective(Objective objective) {
        List<Packet<?>> list = this.getStartTrackingPackets(objective);

        for (ServerPlayer serverplayer : this.server.getPlayerList().getPlayers()) {
            for (Packet<?> packet : list) {
                serverplayer.connection.send(packet);
            }
        }

        this.trackedObjectives.add(objective);
    }

    public List<Packet<?>> getStopTrackingPackets(Objective objective) {
        List<Packet<?>> list = Lists.newArrayList();
        list.add(new ClientboundSetObjectivePacket(objective, 1));

        for (DisplaySlot displayslot : DisplaySlot.values()) {
            if (this.getDisplayObjective(displayslot) == objective) {
                list.add(new ClientboundSetDisplayObjectivePacket(displayslot, objective));
            }
        }

        return list;
    }

    public void stopTrackingObjective(Objective objective) {
        List<Packet<?>> list = this.getStopTrackingPackets(objective);

        for (ServerPlayer serverplayer : this.server.getPlayerList().getPlayers()) {
            for (Packet<?> packet : list) {
                serverplayer.connection.send(packet);
            }
        }

        this.trackedObjectives.remove(objective);
    }

    public int getObjectiveDisplaySlotCount(Objective objective) {
        int i = 0;

        for (DisplaySlot displayslot : DisplaySlot.values()) {
            if (this.getDisplayObjective(displayslot) == objective) {
                i++;
            }
        }

        return i;
    }

    private void updatePlayerWaypoint(String playerName) {
        ServerPlayer serverplayer = this.server.getPlayerList().getPlayerByName(playerName);
        if (serverplayer != null) {
            serverplayer.level().getWaypointManager().remakeConnections(serverplayer);
        }
    }

    private void updateTeamWaypoints(PlayerTeam team) {
        for (ServerLevel serverlevel : this.server.getAllLevels()) {
            team.getPlayers()
                .stream()
                .map(p_415011_ -> this.server.getPlayerList().getPlayerByName(p_415011_))
                .filter(Objects::nonNull)
                .forEach(p_415010_ -> serverlevel.getWaypointManager().remakeConnections(p_415010_));
        }
    }
}
