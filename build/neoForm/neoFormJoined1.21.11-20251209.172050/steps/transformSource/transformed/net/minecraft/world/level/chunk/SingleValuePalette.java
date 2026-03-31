package net.minecraft.world.level.chunk;

import java.util.List;
import java.util.function.Predicate;
import net.minecraft.core.IdMap;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.VarInt;
import org.apache.commons.lang3.Validate;
import org.jspecify.annotations.Nullable;

public class SingleValuePalette<T> implements Palette<T> {
    private @Nullable T value;

    public SingleValuePalette(List<T> values) {
        if (!values.isEmpty()) {
            Validate.isTrue(values.size() <= 1, "Can't initialize SingleValuePalette with %d values.", (long)values.size());
            this.value = values.getFirst();
        }
    }

    public static <A> Palette<A> create(int bits, List<A> values) {
        return new SingleValuePalette<>(values);
    }

    @Override
    public int idFor(T p_188219_, PaletteResize<T> p_446869_) {
        if (this.value != null && this.value != p_188219_) {
            return p_446869_.onResize(1, p_188219_);
        } else {
            this.value = p_188219_;
            return 0;
        }
    }

    @Override
    public boolean maybeHas(Predicate<T> p_188221_) {
        if (this.value == null) {
            throw new IllegalStateException("Use of an uninitialized palette");
        } else {
            return p_188221_.test(this.value);
        }
    }

    @Override
    public T valueFor(int p_188212_) {
        if (this.value != null && p_188212_ == 0) {
            return this.value;
        } else {
            throw new IllegalStateException("Missing Palette entry for id " + p_188212_ + ".");
        }
    }

    @Override
    public void read(FriendlyByteBuf p_188223_, IdMap<T> p_446078_) {
        this.value = p_446078_.byIdOrThrow(p_188223_.readVarInt());
    }

    @Override
    public void write(FriendlyByteBuf p_188226_, IdMap<T> p_446461_) {
        if (this.value == null) {
            throw new IllegalStateException("Use of an uninitialized palette");
        } else {
            p_188226_.writeVarInt(p_446461_.getId(this.value));
        }
    }

    @Override
    public int getSerializedSize(IdMap<T> p_446532_) {
        if (this.value == null) {
            throw new IllegalStateException("Use of an uninitialized palette");
        } else {
            return VarInt.getByteSize(p_446532_.getId(this.value));
        }
    }

    @Override
    public int getSize() {
        return 1;
    }

    @Override
    public Palette<T> copy() {
        if (this.value == null) {
            throw new IllegalStateException("Use of an uninitialized palette");
        } else {
            return this;
        }
    }
}
