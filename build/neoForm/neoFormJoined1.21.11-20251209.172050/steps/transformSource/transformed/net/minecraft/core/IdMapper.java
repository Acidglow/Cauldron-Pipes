package net.minecraft.core;

import com.google.common.collect.Iterators;
import com.google.common.collect.Lists;
import it.unimi.dsi.fastutil.objects.Reference2IntMap;
import it.unimi.dsi.fastutil.objects.Reference2IntOpenHashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import org.jspecify.annotations.Nullable;

public class IdMapper<T> implements IdMap<T> {
    protected int nextId;
    protected final Reference2IntMap<T> tToId;
    protected final List<T> idToT;

    public IdMapper() {
        this(512);
    }

    public IdMapper(int expectedSize) {
        this.idToT = Lists.newArrayListWithExpectedSize(expectedSize);
        this.tToId = new Reference2IntOpenHashMap<>(expectedSize);
        this.tToId.defaultReturnValue(-1);
    }

    public void addMapping(T key, int value) {
        this.tToId.put(key, value);

        while (this.idToT.size() <= value) {
            this.idToT.add(null);
        }

        this.idToT.set(value, key);
        if (this.nextId <= value) {
            this.nextId = value + 1;
        }
    }

    public void add(T key) {
        this.addMapping(key, this.nextId);
    }

    @Override
    public int getId(T p_122663_) {
        return this.tToId.getInt(p_122663_);
    }

    @Override
    public final @Nullable T byId(int p_122661_) {
        return p_122661_ >= 0 && p_122661_ < this.idToT.size() ? this.idToT.get(p_122661_) : null;
    }

    @Override
    public Iterator<T> iterator() {
        return Iterators.filter(this.idToT.iterator(), Objects::nonNull);
    }

    public boolean contains(int id) {
        return this.byId(id) != null;
    }

    @Override
    public int size() {
        return this.tToId.size();
    }
}
