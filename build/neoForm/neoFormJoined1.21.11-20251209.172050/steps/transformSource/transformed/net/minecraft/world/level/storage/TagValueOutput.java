package net.minecraft.world.level.storage;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.DataResult.Error;
import com.mojang.serialization.DataResult.Success;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.util.ProblemReporter;
import org.jspecify.annotations.Nullable;

public class TagValueOutput implements ValueOutput {
    private final ProblemReporter problemReporter;
    private final DynamicOps<Tag> ops;
    private final CompoundTag output;

    TagValueOutput(ProblemReporter problemReporter, DynamicOps<Tag> ops, CompoundTag tag) {
        this.problemReporter = problemReporter;
        this.ops = ops;
        this.output = tag;
    }

    public static TagValueOutput createWithContext(ProblemReporter problemReporter, HolderLookup.Provider lookup) {
        return new TagValueOutput(problemReporter, lookup.createSerializationContext(NbtOps.INSTANCE), new CompoundTag());
    }

    public static TagValueOutput createWithoutContext(ProblemReporter problemReporter) {
        return new TagValueOutput(problemReporter, NbtOps.INSTANCE, new CompoundTag());
    }

    @Override
    public <T> void store(String p_422610_, Codec<T> p_421796_, T p_421836_) {
        switch (p_421796_.encodeStart(this.ops, p_421836_)) {
            case Success<Tag> success:
                this.output.put(p_422610_, success.value());
                break;
            case Error<Tag> error:
                this.problemReporter.report(new TagValueOutput.EncodeToFieldFailedProblem(p_422610_, p_421836_, error));
                error.partialValue().ifPresent(p_422643_ -> this.output.put(p_422610_, p_422643_));
                break;
            default:
                throw new MatchException(null, null);
        }
    }

    @Override
    public <T> void storeNullable(String p_422693_, Codec<T> p_422658_, @Nullable T p_422422_) {
        if (p_422422_ != null) {
            this.store(p_422693_, p_422658_, p_422422_);
        }
    }

    @Override
    public <T> void store(MapCodec<T> p_422011_, T p_422483_) {
        switch (p_422011_.encoder().encodeStart(this.ops, p_422483_)) {
            case Success<Tag> success:
                this.output.merge((CompoundTag)success.value());
                break;
            case Error<Tag> error:
                this.problemReporter.report(new TagValueOutput.EncodeToMapFailedProblem(p_422483_, error));
                error.partialValue().ifPresent(p_422456_ -> this.output.merge((CompoundTag)p_422456_));
                break;
            default:
                throw new MatchException(null, null);
        }
    }

    @Override
    public void store(CompoundTag tag) {
        for (var entry : tag.entrySet()) {
            output.put(entry.getKey(), entry.getValue());
        }
    }

    @Override
    public void putBoolean(String p_422027_, boolean p_422245_) {
        this.output.putBoolean(p_422027_, p_422245_);
    }

    @Override
    public void putByte(String p_422687_, byte p_422100_) {
        this.output.putByte(p_422687_, p_422100_);
    }

    @Override
    public void putShort(String p_422329_, short p_422059_) {
        this.output.putShort(p_422329_, p_422059_);
    }

    @Override
    public void putInt(String p_422172_, int p_422254_) {
        this.output.putInt(p_422172_, p_422254_);
    }

    @Override
    public void putLong(String p_422538_, long p_422228_) {
        this.output.putLong(p_422538_, p_422228_);
    }

    @Override
    public void putFloat(String p_421829_, float p_422156_) {
        this.output.putFloat(p_421829_, p_422156_);
    }

    @Override
    public void putDouble(String p_422050_, double p_421532_) {
        this.output.putDouble(p_422050_, p_421532_);
    }

    @Override
    public void putString(String p_422108_, String p_421861_) {
        this.output.putString(p_422108_, p_421861_);
    }

    @Override
    public void putIntArray(String p_422081_, int[] p_422087_) {
        this.output.putIntArray(p_422081_, p_422087_);
    }

    private ProblemReporter reporterForChild(String name) {
        return this.problemReporter.forChild(new ProblemReporter.FieldPathElement(name));
    }

    @Override
    public ValueOutput child(String p_422335_) {
        CompoundTag compoundtag = new CompoundTag();
        this.output.put(p_422335_, compoundtag);
        return new TagValueOutput(this.reporterForChild(p_422335_), this.ops, compoundtag);
    }

    @Override
    public ValueOutput.ValueOutputList childrenList(String p_422352_) {
        ListTag listtag = new ListTag();
        this.output.put(p_422352_, listtag);
        return new TagValueOutput.ListWrapper(p_422352_, this.problemReporter, this.ops, listtag);
    }

    @Override
    public <T> ValueOutput.TypedOutputList<T> list(String p_421955_, Codec<T> p_421545_) {
        ListTag listtag = new ListTag();
        this.output.put(p_421955_, listtag);
        return new TagValueOutput.TypedListWrapper<>(this.problemReporter, p_421955_, this.ops, p_421545_, listtag);
    }

    @Override
    public void discard(String p_422189_) {
        this.output.remove(p_422189_);
    }

    @Override
    public boolean isEmpty() {
        return this.output.isEmpty();
    }

    public CompoundTag buildResult() {
        return this.output;
    }

    public record EncodeToFieldFailedProblem(String name, Object value, Error<?> error) implements ProblemReporter.Problem {
        @Override
        public String description() {
            return "Failed to encode value '" + this.value + "' to field '" + this.name + "': " + this.error.message();
        }
    }

    public record EncodeToListFailedProblem(String name, Object value, Error<?> error) implements ProblemReporter.Problem {
        @Override
        public String description() {
            return "Failed to append value '" + this.value + "' to list '" + this.name + "': " + this.error.message();
        }
    }

    public record EncodeToMapFailedProblem(Object value, Error<?> error) implements ProblemReporter.Problem {
        @Override
        public String description() {
            return "Failed to merge value '" + this.value + "' to an object: " + this.error.message();
        }
    }

    static class ListWrapper implements ValueOutput.ValueOutputList {
        private final String fieldName;
        private final ProblemReporter problemReporter;
        private final DynamicOps<Tag> ops;
        private final ListTag output;

        ListWrapper(String fieldName, ProblemReporter problemReporter, DynamicOps<Tag> ops, ListTag output) {
            this.fieldName = fieldName;
            this.problemReporter = problemReporter;
            this.ops = ops;
            this.output = output;
        }

        @Override
        public ValueOutput addChild() {
            int i = this.output.size();
            CompoundTag compoundtag = new CompoundTag();
            this.output.add(compoundtag);
            return new TagValueOutput(this.problemReporter.forChild(new ProblemReporter.IndexedFieldPathElement(this.fieldName, i)), this.ops, compoundtag);
        }

        @Override
        public void discardLast() {
            this.output.removeLast();
        }

        @Override
        public boolean isEmpty() {
            return this.output.isEmpty();
        }
    }

    static class TypedListWrapper<T> implements ValueOutput.TypedOutputList<T> {
        private final ProblemReporter problemReporter;
        private final String name;
        private final DynamicOps<Tag> ops;
        private final Codec<T> codec;
        private final ListTag output;

        TypedListWrapper(ProblemReporter problemReporter, String name, DynamicOps<Tag> ops, Codec<T> codec, ListTag output) {
            this.problemReporter = problemReporter;
            this.name = name;
            this.ops = ops;
            this.codec = codec;
            this.output = output;
        }

        @Override
        public void add(T p_421641_) {
            switch (this.codec.encodeStart(this.ops, p_421641_)) {
                case Success<Tag> success:
                    this.output.add(success.value());
                    break;
                case Error<Tag> error:
                    this.problemReporter.report(new TagValueOutput.EncodeToListFailedProblem(this.name, p_421641_, error));
                    error.partialValue().ifPresent(this.output::add);
                    break;
                default:
                    throw new MatchException(null, null);
            }
        }

        @Override
        public boolean isEmpty() {
            return this.output.isEmpty();
        }
    }
}
