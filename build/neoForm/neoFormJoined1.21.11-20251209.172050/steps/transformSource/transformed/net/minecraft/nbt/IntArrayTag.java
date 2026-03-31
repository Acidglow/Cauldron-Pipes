package net.minecraft.nbt;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.Arrays;
import java.util.Optional;
import org.apache.commons.lang3.ArrayUtils;

public final class IntArrayTag implements CollectionTag {
    private static final int SELF_SIZE_IN_BYTES = 24;
    public static final TagType<IntArrayTag> TYPE = new TagType.VariableSize<IntArrayTag>() {
        public IntArrayTag load(DataInput p_128662_, NbtAccounter p_128664_) throws IOException {
            return new IntArrayTag(readAccounted(p_128662_, p_128664_));
        }

        @Override
        public StreamTagVisitor.ValueResult parse(DataInput p_197478_, StreamTagVisitor p_197479_, NbtAccounter p_302360_) throws IOException {
            return p_197479_.visit(readAccounted(p_197478_, p_302360_));
        }

        private static int[] readAccounted(DataInput input, NbtAccounter accounter) throws IOException {
            accounter.accountBytes(24L);
            int i = input.readInt();
            accounter.accountBytes(4L, i);
            int[] aint = new int[i];

            for (int j = 0; j < i; j++) {
                aint[j] = input.readInt();
            }

            return aint;
        }

        @Override
        public void skip(DataInput p_197476_, NbtAccounter p_302380_) throws IOException {
            p_197476_.skipBytes(p_197476_.readInt() * 4);
        }

        @Override
        public String getName() {
            return "INT[]";
        }

        @Override
        public String getPrettyName() {
            return "TAG_Int_Array";
        }
    };
    private int[] data;

    public IntArrayTag(int[] data) {
        this.data = data;
    }

    @Override
    public void write(DataOutput output) throws IOException {
        output.writeInt(this.data.length);

        for (int i : this.data) {
            output.writeInt(i);
        }
    }

    @Override
    public int sizeInBytes() {
        return 24 + 4 * this.data.length;
    }

    @Override
    public byte getId() {
        return 11;
    }

    @Override
    public TagType<IntArrayTag> getType() {
        return TYPE;
    }

    @Override
    public String toString() {
        StringTagVisitor stringtagvisitor = new StringTagVisitor();
        stringtagvisitor.visitIntArray(this);
        return stringtagvisitor.build();
    }

    public IntArrayTag copy() {
        int[] aint = new int[this.data.length];
        System.arraycopy(this.data, 0, aint, 0, this.data.length);
        return new IntArrayTag(aint);
    }

    @Override
    public boolean equals(Object other) {
        return this == other ? true : other instanceof IntArrayTag && Arrays.equals(this.data, ((IntArrayTag)other).data);
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(this.data);
    }

    public int[] getAsIntArray() {
        return this.data;
    }

    @Override
    public void accept(TagVisitor p_177869_) {
        p_177869_.visitIntArray(this);
    }

    @Override
    public int size() {
        return this.data.length;
    }

    public IntTag get(int index) {
        return IntTag.valueOf(this.data[index]);
    }

    @Override
    public boolean setTag(int index, Tag nbt) {
        if (nbt instanceof NumericTag numerictag) {
            this.data[index] = numerictag.intValue();
            return true;
        } else {
            return false;
        }
    }

    @Override
    public boolean addTag(int index, Tag nbt) {
        if (nbt instanceof NumericTag numerictag) {
            this.data = ArrayUtils.add(this.data, index, numerictag.intValue());
            return true;
        } else {
            return false;
        }
    }

    public IntTag remove(int p_128627_) {
        int i = this.data[p_128627_];
        this.data = ArrayUtils.remove(this.data, p_128627_);
        return IntTag.valueOf(i);
    }

    @Override
    public void clear() {
        this.data = new int[0];
    }

    @Override
    public Optional<int[]> asIntArray() {
        return Optional.of(this.data);
    }

    @Override
    public StreamTagVisitor.ValueResult accept(StreamTagVisitor p_197474_) {
        return p_197474_.visit(this.data);
    }
}
