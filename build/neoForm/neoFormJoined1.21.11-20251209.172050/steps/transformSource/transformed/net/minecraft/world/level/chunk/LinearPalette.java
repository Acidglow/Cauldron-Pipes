package net.minecraft.world.level.chunk;

import java.util.List;
import java.util.function.Predicate;
import net.minecraft.core.IdMap;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.VarInt;
import org.apache.commons.lang3.Validate;

public class LinearPalette<T> implements Palette<T> {
    private final T[] values;
    private final int bits;
    private int size;

    private LinearPalette(int bits, List<T> values) {
        this.values = (T[])(new Object[1 << bits]);
        this.bits = bits;
        Validate.isTrue(
            values.size() <= this.values.length, "Can't initialize LinearPalette of size %d with %d entries", this.values.length, values.size()
        );

        for (int i = 0; i < values.size(); i++) {
            this.values[i] = values.get(i);
        }

        this.size = values.size();
    }

    private LinearPalette(T[] values, int bits, int size) {
        this.values = values;
        this.bits = bits;
        this.size = size;
    }

    public static <A> Palette<A> create(int bits, List<A> values) {
        return new LinearPalette<>(bits, values);
    }

    @Override
    public int idFor(T p_63040_, PaletteResize<T> p_447282_) {
        for (int i = 0; i < this.size; i++) {
            if (this.values[i] == p_63040_) {
                return i;
            }
        }

        int j = this.size;
        if (j < this.values.length) {
            this.values[j] = p_63040_;
            this.size++;
            return j;
        } else {
            return p_447282_.onResize(this.bits + 1, p_63040_);
        }
    }

    @Override
    public boolean maybeHas(Predicate<T> p_63042_) {
        for (int i = 0; i < this.size; i++) {
            if (p_63042_.test(this.values[i])) {
                return true;
            }
        }

        return false;
    }

    @Override
    public T valueFor(int p_63038_) {
        if (p_63038_ >= 0 && p_63038_ < this.size) {
            return this.values[p_63038_];
        } else {
            throw new MissingPaletteEntryException(p_63038_);
        }
    }

    @Override
    public void read(FriendlyByteBuf p_63046_, IdMap<T> p_446753_) {
        this.size = p_63046_.readVarInt();

        for (int i = 0; i < this.size; i++) {
            this.values[i] = p_446753_.byIdOrThrow(p_63046_.readVarInt());
        }
    }

    @Override
    public void write(FriendlyByteBuf p_63049_, IdMap<T> p_446431_) {
        p_63049_.writeVarInt(this.size);

        for (int i = 0; i < this.size; i++) {
            p_63049_.writeVarInt(p_446431_.getId(this.values[i]));
        }
    }

    @Override
    public int getSerializedSize(IdMap<T> p_447247_) {
        int i = VarInt.getByteSize(this.getSize());

        for (int j = 0; j < this.getSize(); j++) {
            i += VarInt.getByteSize(p_447247_.getId(this.values[j]));
        }

        return i;
    }

    @Override
    public int getSize() {
        return this.size;
    }

    @Override
    public Palette<T> copy() {
        return new LinearPalette<>((T[])((Object[])this.values.clone()), this.bits, this.size);
    }
}
