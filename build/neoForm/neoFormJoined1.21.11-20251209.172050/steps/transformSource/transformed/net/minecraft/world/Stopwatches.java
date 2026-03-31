package net.minecraft.world;

import com.mojang.serialization.Codec;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.function.UnaryOperator;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Util;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;
import org.jspecify.annotations.Nullable;

public class Stopwatches extends SavedData {
    private static final Codec<Stopwatches> CODEC = Codec.unboundedMap(Identifier.CODEC, Codec.LONG)
        .fieldOf("stopwatches")
        .codec()
        .xmap(Stopwatches::unpack, Stopwatches::pack);
    public static final SavedDataType<Stopwatches> TYPE = new SavedDataType<>("stopwatches", Stopwatches::new, CODEC, DataFixTypes.SAVED_DATA_STOPWATCHES);
    private final Map<Identifier, Stopwatch> stopwatches = new Object2ObjectOpenHashMap<>();

    private Stopwatches() {
    }

    private static Stopwatches unpack(Map<Identifier, Long> map) {
        Stopwatches stopwatches = new Stopwatches();
        long i = currentTime();
        map.forEach((p_469675_, p_455604_) -> stopwatches.stopwatches.put(p_469675_, new Stopwatch(i, p_455604_)));
        return stopwatches;
    }

    private Map<Identifier, Long> pack() {
        long i = currentTime();
        Map<Identifier, Long> map = new TreeMap<>();
        this.stopwatches.forEach((p_469798_, p_457398_) -> map.put(p_469798_, p_457398_.elapsedMilliseconds(i)));
        return map;
    }

    public @Nullable Stopwatch get(Identifier id) {
        return this.stopwatches.get(id);
    }

    public boolean add(Identifier id, Stopwatch stopwatch) {
        if (this.stopwatches.putIfAbsent(id, stopwatch) == null) {
            this.setDirty();
            return true;
        } else {
            return false;
        }
    }

    public boolean update(Identifier id, UnaryOperator<Stopwatch> updater) {
        if (this.stopwatches.computeIfPresent(id, (p_468534_, p_455351_) -> updater.apply(p_455351_)) != null) {
            this.setDirty();
            return true;
        } else {
            return false;
        }
    }

    public boolean remove(Identifier id) {
        boolean flag = this.stopwatches.remove(id) != null;
        if (flag) {
            this.setDirty();
        }

        return flag;
    }

    @Override
    public boolean isDirty() {
        return super.isDirty() || !this.stopwatches.isEmpty();
    }

    public List<Identifier> ids() {
        return List.copyOf(this.stopwatches.keySet());
    }

    public static long currentTime() {
        return Util.getMillis();
    }
}
