package net.minecraft.world.level.gamerules;

import com.mojang.serialization.Codec;
import it.unimi.dsi.fastutil.objects.Reference2ObjectMap;
import it.unimi.dsi.fastutil.objects.Reference2ObjectOpenHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Stream;
import net.minecraft.core.registries.BuiltInRegistries;
import org.jspecify.annotations.Nullable;

public final class GameRuleMap {
    public static final Codec<GameRuleMap> CODEC = Codec.<GameRule<?>, Object>dispatchedMap(BuiltInRegistries.GAME_RULE.byNameCodec(), GameRule::valueCodec)
        .xmap(GameRuleMap::ofTrusted, GameRuleMap::map);
    private final Reference2ObjectMap<GameRule<?>, Object> map;

    GameRuleMap(Reference2ObjectMap<GameRule<?>, Object> map) {
        this.map = map;
    }

    private static GameRuleMap ofTrusted(Map<GameRule<?>, Object> map) {
        return new GameRuleMap(new Reference2ObjectOpenHashMap<>(map));
    }

    public static GameRuleMap of() {
        return new GameRuleMap(new Reference2ObjectOpenHashMap<>());
    }

    public static GameRuleMap of(Stream<GameRule<?>> gameRules) {
        Reference2ObjectOpenHashMap<GameRule<?>, Object> reference2objectopenhashmap = new Reference2ObjectOpenHashMap<>();
        gameRules.forEach(p_461069_ -> reference2objectopenhashmap.put((GameRule<?>)p_461069_, p_461069_.defaultValue()));
        return new GameRuleMap(reference2objectopenhashmap);
    }

    public static GameRuleMap copyOf(GameRuleMap other) {
        return new GameRuleMap(new Reference2ObjectOpenHashMap<>(other.map));
    }

    public boolean has(GameRule<?> gameRule) {
        return this.map.containsKey(gameRule);
    }

    public <T> @Nullable T get(GameRule<T> gameRule) {
        return (T)this.map.get(gameRule);
    }

    public <T> void set(GameRule<T> gameRule, T value) {
        this.map.put(gameRule, value);
    }

    public <T> @Nullable T remove(GameRule<T> gameRule) {
        return (T)this.map.remove(gameRule);
    }

    public Set<GameRule<?>> keySet() {
        return this.map.keySet();
    }

    public int size() {
        return this.map.size();
    }

    @Override
    public String toString() {
        return this.map.toString();
    }

    public GameRuleMap withOther(GameRuleMap other) {
        GameRuleMap gamerulemap = copyOf(this);
        gamerulemap.setFromIf(other, p_460809_ -> true);
        return gamerulemap;
    }

    public void setFromIf(GameRuleMap map, Predicate<GameRule<?>> predicate) {
        for (GameRule<?> gamerule : map.keySet()) {
            if (predicate.test(gamerule)) {
                setGameRule(map, gamerule, this);
            }
        }
    }

    private static <T> void setGameRule(GameRuleMap map, GameRule<T> gameRule, GameRuleMap source) {
        source.set(gameRule, Objects.requireNonNull(map.get(gameRule)));
    }

    private Reference2ObjectMap<GameRule<?>, Object> map() {
        return this.map;
    }

    @Override
    public boolean equals(Object other) {
        if (other == this) {
            return true;
        } else if (other != null && other.getClass() == this.getClass()) {
            GameRuleMap gamerulemap = (GameRuleMap)other;
            return Objects.equals(this.map, gamerulemap.map);
        } else {
            return false;
        }
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.map);
    }

    public static class Builder {
        final Reference2ObjectMap<GameRule<?>, Object> map = new Reference2ObjectOpenHashMap<>();

        public <T> GameRuleMap.Builder set(GameRule<T> gameRule, T value) {
            this.map.put(gameRule, value);
            return this;
        }

        public GameRuleMap build() {
            return new GameRuleMap(this.map);
        }
    }
}
