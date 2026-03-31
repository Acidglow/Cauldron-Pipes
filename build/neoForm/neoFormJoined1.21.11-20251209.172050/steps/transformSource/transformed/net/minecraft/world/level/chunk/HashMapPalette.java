package net.minecraft.world.level.chunk;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;
import net.minecraft.core.IdMap;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.VarInt;
import net.minecraft.util.CrudeIncrementalIntIdentityHashBiMap;

public class HashMapPalette<T> implements Palette<T> {
    private final CrudeIncrementalIntIdentityHashBiMap<T> values;
    private final int bits;

    public HashMapPalette(int bits, List<T> values) {
        this(bits);
        values.forEach(this.values::add);
    }

    public HashMapPalette(int bits) {
        this(bits, CrudeIncrementalIntIdentityHashBiMap.create(1 << bits));
    }

    private HashMapPalette(int bits, CrudeIncrementalIntIdentityHashBiMap<T> values) {
        this.bits = bits;
        this.values = values;
    }

    public static <A> Palette<A> create(int bits, List<A> values) {
        return new HashMapPalette<>(bits, values);
    }

    @Override
    public int idFor(T p_62673_, PaletteResize<T> p_445988_) {
        int i = this.values.getId(p_62673_);
        if (i == -1) {
            i = this.values.add(p_62673_);
            if (i >= 1 << this.bits) {
                i = p_445988_.onResize(this.bits + 1, p_62673_);
            }
        }

        return i;
    }

    @Override
    public boolean maybeHas(Predicate<T> p_62675_) {
        for (int i = 0; i < this.getSize(); i++) {
            if (p_62675_.test(this.values.byId(i))) {
                return true;
            }
        }

        return false;
    }

    @Override
    public T valueFor(int p_62671_) {
        T t = this.values.byId(p_62671_);
        if (t == null) {
            throw new MissingPaletteEntryException(p_62671_);
        } else {
            return t;
        }
    }

    @Override
    public void read(FriendlyByteBuf p_62679_, IdMap<T> p_446983_) {
        this.values.clear();
        int i = p_62679_.readVarInt();

        for (int j = 0; j < i; j++) {
            this.values.add(p_446983_.byIdOrThrow(p_62679_.readVarInt()));
        }
    }

    @Override
    public void write(FriendlyByteBuf p_62684_, IdMap<T> p_446147_) {
        int i = this.getSize();
        p_62684_.writeVarInt(i);

        for (int j = 0; j < i; j++) {
            p_62684_.writeVarInt(p_446147_.getId(this.values.byId(j)));
        }
    }

    @Override
    public int getSerializedSize(IdMap<T> p_446087_) {
        int i = VarInt.getByteSize(this.getSize());

        for (int j = 0; j < this.getSize(); j++) {
            i += VarInt.getByteSize(p_446087_.getId(this.values.byId(j)));
        }

        return i;
    }

    public List<T> getEntries() {
        ArrayList<T> arraylist = new ArrayList<>();
        this.values.iterator().forEachRemaining(arraylist::add);
        return arraylist;
    }

    @Override
    public int getSize() {
        return this.values.size();
    }

    @Override
    public Palette<T> copy() {
        return new HashMapPalette<>(this.bits, this.values.copy());
    }
}
