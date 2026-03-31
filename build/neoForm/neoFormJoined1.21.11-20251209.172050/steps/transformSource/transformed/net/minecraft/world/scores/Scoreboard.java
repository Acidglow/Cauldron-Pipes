package net.minecraft.world.scores;

import com.google.common.collect.Lists;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.mojang.serialization.codecs.RecordCodecBuilder.Instance;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntMaps;
import it.unimi.dsi.fastutil.objects.Object2ObjectMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.Reference2ObjectMap;
import it.unimi.dsi.fastutil.objects.Reference2ObjectOpenHashMap;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Map.Entry;
import java.util.function.Consumer;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.numbers.NumberFormat;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.scores.criteria.ObjectiveCriteria;
import org.apache.commons.lang3.mutable.MutableBoolean;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

public class Scoreboard {
    public static final String HIDDEN_SCORE_PREFIX = "#";
    private static final Logger LOGGER = LogUtils.getLogger();
    private final Object2ObjectMap<String, Objective> objectivesByName = new Object2ObjectOpenHashMap<>(16, 0.5F);
    private final Reference2ObjectMap<ObjectiveCriteria, List<Objective>> objectivesByCriteria = new Reference2ObjectOpenHashMap<>();
    private final Map<String, PlayerScores> playerScores = new Object2ObjectOpenHashMap<>(16, 0.5F);
    private final Map<DisplaySlot, Objective> displayObjectives = new EnumMap<>(DisplaySlot.class);
    private final Object2ObjectMap<String, PlayerTeam> teamsByName = new Object2ObjectOpenHashMap<>();
    private final Object2ObjectMap<String, PlayerTeam> teamsByPlayer = new Object2ObjectOpenHashMap<>();

    /**
     * Returns a ScoreObjective for the objective name
     */
    public @Nullable Objective getObjective(@Nullable String name) {
        return this.objectivesByName.get(name);
    }

    public Objective addObjective(
        String name,
        ObjectiveCriteria criteria,
        Component displayName,
        ObjectiveCriteria.RenderType renderType,
        boolean displayAutoUpdate,
        @Nullable NumberFormat numberFormat
    ) {
        if (this.objectivesByName.containsKey(name)) {
            throw new IllegalArgumentException("An objective with the name '" + name + "' already exists!");
        } else {
            Objective objective = new Objective(this, name, criteria, displayName, renderType, displayAutoUpdate, numberFormat);
            this.objectivesByCriteria.computeIfAbsent(criteria, p_314722_ -> Lists.newArrayList()).add(objective);
            this.objectivesByName.put(name, objective);
            this.onObjectiveAdded(objective);
            return objective;
        }
    }

    public final void forAllObjectives(ObjectiveCriteria criteria, ScoreHolder scoreHolder, Consumer<ScoreAccess> action) {
        this.objectivesByCriteria
            .getOrDefault(criteria, Collections.emptyList())
            .forEach(p_313676_ -> action.accept(this.getOrCreatePlayerScore(scoreHolder, p_313676_, true)));
    }

    private PlayerScores getOrCreatePlayerInfo(String username) {
        return this.playerScores.computeIfAbsent(username, p_313683_ -> new PlayerScores());
    }

    public ScoreAccess getOrCreatePlayerScore(ScoreHolder scoreHolder, Objective objective) {
        return this.getOrCreatePlayerScore(scoreHolder, objective, false);
    }

    public ScoreAccess getOrCreatePlayerScore(final ScoreHolder scoreHolder, final Objective objective, boolean readOnly) {
        final boolean flag = readOnly || !objective.getCriteria().isReadOnly();
        PlayerScores playerscores = this.getOrCreatePlayerInfo(scoreHolder.getScoreboardName());
        final MutableBoolean mutableboolean = new MutableBoolean();
        final Score score = playerscores.getOrCreate(objective, p_313682_ -> mutableboolean.setTrue());
        return new ScoreAccess() {
            @Override
            public int get() {
                return score.value();
            }

            @Override
            public void set(int p_313831_) {
                if (!flag) {
                    throw new IllegalStateException("Cannot modify read-only score");
                } else {
                    boolean flag1 = mutableboolean.isTrue();
                    if (objective.displayAutoUpdate()) {
                        Component component = scoreHolder.getDisplayName();
                        if (component != null && !component.equals(score.display())) {
                            score.display(component);
                            flag1 = true;
                        }
                    }

                    if (p_313831_ != score.value()) {
                        score.value(p_313831_);
                        flag1 = true;
                    }

                    if (flag1) {
                        this.sendScoreToPlayers();
                    }
                }
            }

            @Override
            public @Nullable Component display() {
                return score.display();
            }

            @Override
            public void display(@Nullable Component p_313826_) {
                if (mutableboolean.isTrue() || !Objects.equals(p_313826_, score.display())) {
                    score.display(p_313826_);
                    this.sendScoreToPlayers();
                }
            }

            @Override
            public void numberFormatOverride(@Nullable NumberFormat p_313875_) {
                score.numberFormat(p_313875_);
                this.sendScoreToPlayers();
            }

            @Override
            public boolean locked() {
                return score.isLocked();
            }

            @Override
            public void unlock() {
                this.setLocked(false);
            }

            @Override
            public void lock() {
                this.setLocked(true);
            }

            private void setLocked(boolean locked) {
                score.setLocked(locked);
                if (mutableboolean.isTrue()) {
                    this.sendScoreToPlayers();
                }

                Scoreboard.this.onScoreLockChanged(scoreHolder, objective);
            }

            private void sendScoreToPlayers() {
                Scoreboard.this.onScoreChanged(scoreHolder, objective, score);
                mutableboolean.setFalse();
            }
        };
    }

    public @Nullable ReadOnlyScoreInfo getPlayerScoreInfo(ScoreHolder scoreHolder, Objective objective) {
        PlayerScores playerscores = this.playerScores.get(scoreHolder.getScoreboardName());
        return playerscores != null ? playerscores.get(objective) : null;
    }

    public Collection<PlayerScoreEntry> listPlayerScores(Objective objective) {
        List<PlayerScoreEntry> list = new ArrayList<>();
        this.playerScores.forEach((p_313669_, p_313670_) -> {
            Score score = p_313670_.get(objective);
            if (score != null) {
                list.add(new PlayerScoreEntry(p_313669_, score.value(), score.display(), score.numberFormat()));
            }
        });
        return list;
    }

    public Collection<Objective> getObjectives() {
        return this.objectivesByName.values();
    }

    public Collection<String> getObjectiveNames() {
        return this.objectivesByName.keySet();
    }

    public Collection<ScoreHolder> getTrackedPlayers() {
        return this.playerScores.keySet().stream().map(ScoreHolder::forNameOnly).toList();
    }

    public void resetAllPlayerScores(ScoreHolder scoreHolder) {
        PlayerScores playerscores = this.playerScores.remove(scoreHolder.getScoreboardName());
        if (playerscores != null) {
            this.onPlayerRemoved(scoreHolder);
        }
    }

    public void resetSinglePlayerScore(ScoreHolder scoreHolder, Objective objective) {
        PlayerScores playerscores = this.playerScores.get(scoreHolder.getScoreboardName());
        if (playerscores != null) {
            boolean flag = playerscores.remove(objective);
            if (!playerscores.hasScores()) {
                PlayerScores playerscores1 = this.playerScores.remove(scoreHolder.getScoreboardName());
                if (playerscores1 != null) {
                    this.onPlayerRemoved(scoreHolder);
                }
            } else if (flag) {
                this.onPlayerScoreRemoved(scoreHolder, objective);
            }
        }
    }

    public Object2IntMap<Objective> listPlayerScores(ScoreHolder scoreHolder) {
        PlayerScores playerscores = this.playerScores.get(scoreHolder.getScoreboardName());
        return playerscores != null ? playerscores.listScores() : Object2IntMaps.emptyMap();
    }

    public void removeObjective(Objective objective) {
        this.objectivesByName.remove(objective.getName());

        for (DisplaySlot displayslot : DisplaySlot.values()) {
            if (this.getDisplayObjective(displayslot) == objective) {
                this.setDisplayObjective(displayslot, null);
            }
        }

        List<Objective> list = this.objectivesByCriteria.get(objective.getCriteria());
        if (list != null) {
            list.remove(objective);
        }

        for (PlayerScores playerscores : this.playerScores.values()) {
            playerscores.remove(objective);
        }

        this.onObjectiveRemoved(objective);
    }

    public void setDisplayObjective(DisplaySlot slot, @Nullable Objective objective) {
        this.displayObjectives.put(slot, objective);
    }

    public @Nullable Objective getDisplayObjective(DisplaySlot slot) {
        return this.displayObjectives.get(slot);
    }

    /**
     * Retrieve the ScorePlayerTeam instance identified by the passed team name
     */
    public @Nullable PlayerTeam getPlayerTeam(String teamName) {
        return this.teamsByName.get(teamName);
    }

    public PlayerTeam addPlayerTeam(String name) {
        PlayerTeam playerteam = this.getPlayerTeam(name);
        if (playerteam != null) {
            LOGGER.warn("Requested creation of existing team '{}'", name);
            return playerteam;
        } else {
            playerteam = new PlayerTeam(this, name);
            this.teamsByName.put(name, playerteam);
            this.onTeamAdded(playerteam);
            return playerteam;
        }
    }

    /**
     * Removes the team from the scoreboard, updates all player memberships and broadcasts the deletion to all players
     */
    public void removePlayerTeam(PlayerTeam playerTeam) {
        this.teamsByName.remove(playerTeam.getName());

        for (String s : playerTeam.getPlayers()) {
            this.teamsByPlayer.remove(s);
        }

        this.onTeamRemoved(playerTeam);
    }

    public boolean addPlayerToTeam(String playerName, PlayerTeam team) {
        if (this.getPlayersTeam(playerName) != null) {
            this.removePlayerFromTeam(playerName);
        }

        this.teamsByPlayer.put(playerName, team);
        return team.getPlayers().add(playerName);
    }

    public boolean removePlayerFromTeam(String playerName) {
        PlayerTeam playerteam = this.getPlayersTeam(playerName);
        if (playerteam != null) {
            this.removePlayerFromTeam(playerName, playerteam);
            return true;
        } else {
            return false;
        }
    }

    /**
     * Removes the given username from the given ScorePlayerTeam. If the player is not on the team then an IllegalStateException is thrown.
     */
    public void removePlayerFromTeam(String username, PlayerTeam playerTeam) {
        if (this.getPlayersTeam(username) != playerTeam) {
            throw new IllegalStateException("Player is either on another team or not on any team. Cannot remove from team '" + playerTeam.getName() + "'.");
        } else {
            this.teamsByPlayer.remove(username);
            playerTeam.getPlayers().remove(username);
        }
    }

    public Collection<String> getTeamNames() {
        return this.teamsByName.keySet();
    }

    public Collection<PlayerTeam> getPlayerTeams() {
        return this.teamsByName.values();
    }

    /**
     * Gets the ScorePlayerTeam object for the given username.
     */
    public @Nullable PlayerTeam getPlayersTeam(String username) {
        return this.teamsByPlayer.get(username);
    }

    public void onObjectiveAdded(Objective objective) {
    }

    public void onObjectiveChanged(Objective objective) {
    }

    public void onObjectiveRemoved(Objective objective) {
    }

    protected void onScoreChanged(ScoreHolder scoreHolder, Objective objective, Score score) {
    }

    protected void onScoreLockChanged(ScoreHolder scoreHolder, Objective objective) {
    }

    public void onPlayerRemoved(ScoreHolder scoreHolder) {
    }

    public void onPlayerScoreRemoved(ScoreHolder scoreHolder, Objective objective) {
    }

    public void onTeamAdded(PlayerTeam playerTeam) {
    }

    public void onTeamChanged(PlayerTeam playerTeam) {
    }

    public void onTeamRemoved(PlayerTeam playerTeam) {
    }

    public void entityRemoved(Entity entity) {
        if (!(entity instanceof Player) && !entity.isAlive()) {
            this.resetAllPlayerScores(entity);
            this.removePlayerFromTeam(entity.getScoreboardName());
        }
    }

    protected List<Scoreboard.PackedScore> packPlayerScores() {
        return this.playerScores
            .entrySet()
            .stream()
            .flatMap(
                p_400991_ -> {
                    String s = p_400991_.getKey();
                    return p_400991_.getValue()
                        .listRawScores()
                        .entrySet()
                        .stream()
                        .map(p_457509_ -> new Scoreboard.PackedScore(s, p_457509_.getKey().getName(), p_457509_.getValue().pack()));
                }
            )
            .toList();
    }

    protected void loadPlayerScore(Scoreboard.PackedScore score) {
        Objective objective = this.getObjective(score.objective);
        if (objective == null) {
            LOGGER.error("Unknown objective {} for name {}, ignoring", score.objective, score.owner);
        } else {
            this.getOrCreatePlayerInfo(score.owner).setScore(objective, new Score(score.score));
        }
    }

    protected List<PlayerTeam.Packed> packPlayerTeams() {
        return this.getPlayerTeams().stream().map(PlayerTeam::pack).toList();
    }

    protected void loadPlayerTeam(PlayerTeam.Packed packed) {
        PlayerTeam playerteam = this.addPlayerTeam(packed.name());
        packed.displayName().ifPresent(playerteam::setDisplayName);
        packed.color().ifPresent(playerteam::setColor);
        playerteam.setAllowFriendlyFire(packed.allowFriendlyFire());
        playerteam.setSeeFriendlyInvisibles(packed.seeFriendlyInvisibles());
        playerteam.setPlayerPrefix(packed.memberNamePrefix());
        playerteam.setPlayerSuffix(packed.memberNameSuffix());
        playerteam.setNameTagVisibility(packed.nameTagVisibility());
        playerteam.setDeathMessageVisibility(packed.deathMessageVisibility());
        playerteam.setCollisionRule(packed.collisionRule());

        for (String s : packed.players()) {
            this.addPlayerToTeam(s, playerteam);
        }
    }

    protected List<Objective.Packed> packObjectives() {
        return this.getObjectives().stream().map(Objective::pack).toList();
    }

    protected void loadObjective(Objective.Packed packed) {
        this.addObjective(
            packed.name(),
            packed.criteria(),
            packed.displayName(),
            packed.renderType(),
            packed.displayAutoUpdate(),
            packed.numberFormat().orElse(null)
        );
    }

    protected Map<DisplaySlot, String> packDisplaySlots() {
        Map<DisplaySlot, String> map = new EnumMap<>(DisplaySlot.class);

        for (DisplaySlot displayslot : DisplaySlot.values()) {
            Objective objective = this.getDisplayObjective(displayslot);
            if (objective != null) {
                map.put(displayslot, objective.getName());
            }
        }

        return map;
    }

    public record PackedScore(String owner, String objective, Score.Packed score) {
        public static final Codec<Scoreboard.PackedScore> CODEC = RecordCodecBuilder.create(
            p_457510_ -> p_457510_.group(
                    Codec.STRING.fieldOf("Name").forGetter(Scoreboard.PackedScore::owner),
                    Codec.STRING.fieldOf("Objective").forGetter(Scoreboard.PackedScore::objective),
                    Score.Packed.MAP_CODEC.forGetter(Scoreboard.PackedScore::score)
                )
                .apply(p_457510_, Scoreboard.PackedScore::new)
        );
    }
}
