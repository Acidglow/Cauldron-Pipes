package net.minecraft.world.level.storage;

import com.google.common.collect.AbstractIterator;
import com.google.common.collect.Streams;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.MapLike;
import com.mojang.serialization.DataResult.Error;
import com.mojang.serialization.DataResult.Success;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.IntArrayTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.NumericTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.nbt.TagType;
import net.minecraft.util.ProblemReporter;
import org.jspecify.annotations.Nullable;

public class TagValueInput implements ValueInput {
    private final ProblemReporter problemReporter;
    private final ValueInputContextHelper context;
    private final CompoundTag input;

    private TagValueInput(ProblemReporter problemReporter, ValueInputContextHelper context, CompoundTag input) {
        this.problemReporter = problemReporter;
        this.context = context;
        this.input = input;
    }

    public static ValueInput create(ProblemReporter problemReporter, HolderLookup.Provider lookup, CompoundTag input) {
        return new TagValueInput(problemReporter, new ValueInputContextHelper(lookup, NbtOps.INSTANCE), input);
    }

    public static ValueInput.ValueInputList create(ProblemReporter problemReporter, HolderLookup.Provider lookup, List<CompoundTag> input) {
        return new TagValueInput.CompoundListWrapper(problemReporter, new ValueInputContextHelper(lookup, NbtOps.INSTANCE), input);
    }

    @Override
    public <T> Optional<T> read(String p_422121_, Codec<T> p_421846_) {
        Tag tag = this.input.get(p_422121_);
        if (tag == null) {
            return Optional.empty();
        } else {
            return switch (p_421846_.parse(this.context.ops(), tag)) {
                case Success<T> success -> Optional.of(success.value());
                case Error<T> error -> {
                    this.problemReporter.report(new TagValueInput.DecodeFromFieldFailedProblem(p_422121_, tag, error));
                    yield error.partialValue();
                }
                default -> throw new MatchException(null, null);
            };
        }
    }

    @Override
    public <T> Optional<T> read(MapCodec<T> p_422606_) {
        DynamicOps<Tag> dynamicops = this.context.ops();

        return switch (dynamicops.getMap(this.input).flatMap(p_422400_ -> p_422606_.decode(dynamicops, (MapLike<Tag>)p_422400_))) {
            case Success<T> success -> Optional.of(success.value());
            case Error<T> error -> {
                this.problemReporter.report(new TagValueInput.DecodeFromMapFailedProblem(error));
                yield error.partialValue();
            }
            default -> throw new MatchException(null, null);
        };
    }

    private <T extends Tag> @Nullable T getOptionalTypedTag(String key, TagType<T> type) {
        Tag tag = this.input.get(key);
        if (tag == null) {
            return null;
        } else {
            TagType<?> tagtype = tag.getType();
            if (tagtype != type) {
                this.problemReporter.report(new TagValueInput.UnexpectedTypeProblem(key, type, tagtype));
                return null;
            } else {
                return (T)tag;
            }
        }
    }

    private @Nullable NumericTag getNumericTag(String key) {
        Tag tag = this.input.get(key);
        if (tag == null) {
            return null;
        } else if (tag instanceof NumericTag numerictag) {
            return numerictag;
        } else {
            this.problemReporter.report(new TagValueInput.UnexpectedNonNumberProblem(key, tag.getType()));
            return null;
        }
    }

    @Override
    public Optional<ValueInput> child(String p_422520_) {
        CompoundTag compoundtag = this.getOptionalTypedTag(p_422520_, CompoundTag.TYPE);
        return compoundtag != null ? Optional.of(this.wrapChild(p_422520_, compoundtag)) : Optional.empty();
    }

    @Override
    public ValueInput childOrEmpty(String p_422555_) {
        CompoundTag compoundtag = this.getOptionalTypedTag(p_422555_, CompoundTag.TYPE);
        return compoundtag != null ? this.wrapChild(p_422555_, compoundtag) : this.context.empty();
    }

    @Override
    public ValueInput rawChildOrEmpty(String key) {
        CompoundTag compoundtag = this.getOptionalTypedTag(key, CompoundTag.TYPE);
        return compoundtag != null ? new TagValueInput(this.problemReporter.forChild(new ProblemReporter.FieldPathElement(key)), this.context, compoundtag) : this.context.empty();
    }

    @Override
    public Optional<ValueInput.ValueInputList> childrenList(String p_421700_) {
        ListTag listtag = this.getOptionalTypedTag(p_421700_, ListTag.TYPE);
        return listtag != null ? Optional.of(this.wrapList(p_421700_, this.context, listtag)) : Optional.empty();
    }

    @Override
    public ValueInput.ValueInputList childrenListOrEmpty(String p_422523_) {
        ListTag listtag = this.getOptionalTypedTag(p_422523_, ListTag.TYPE);
        return listtag != null ? this.wrapList(p_422523_, this.context, listtag) : this.context.emptyList();
    }

    @Override
    public <T> Optional<ValueInput.TypedInputList<T>> list(String p_422068_, Codec<T> p_421633_) {
        ListTag listtag = this.getOptionalTypedTag(p_422068_, ListTag.TYPE);
        return listtag != null ? Optional.of(this.wrapTypedList(p_422068_, listtag, p_421633_)) : Optional.empty();
    }

    @Override
    public <T> ValueInput.TypedInputList<T> listOrEmpty(String p_422509_, Codec<T> p_422700_) {
        ListTag listtag = this.getOptionalTypedTag(p_422509_, ListTag.TYPE);
        return listtag != null ? this.wrapTypedList(p_422509_, listtag, p_422700_) : this.context.emptyTypedList();
    }

    @Override
    public boolean getBooleanOr(String p_422592_, boolean p_422347_) {
        NumericTag numerictag = this.getNumericTag(p_422592_);
        return numerictag != null ? numerictag.byteValue() != 0 : p_422347_;
    }

    @Override
    public byte getByteOr(String p_422035_, byte p_422310_) {
        NumericTag numerictag = this.getNumericTag(p_422035_);
        return numerictag != null ? numerictag.byteValue() : p_422310_;
    }

    @Override
    public int getShortOr(String p_421616_, short p_421751_) {
        NumericTag numerictag = this.getNumericTag(p_421616_);
        return numerictag != null ? numerictag.shortValue() : p_421751_;
    }

    @Override
    public Optional<Integer> getInt(String p_421813_) {
        NumericTag numerictag = this.getNumericTag(p_421813_);
        return numerictag != null ? Optional.of(numerictag.intValue()) : Optional.empty();
    }

    @Override
    public int getIntOr(String p_422549_, int p_422510_) {
        NumericTag numerictag = this.getNumericTag(p_422549_);
        return numerictag != null ? numerictag.intValue() : p_422510_;
    }

    @Override
    public long getLongOr(String p_422010_, long p_421867_) {
        NumericTag numerictag = this.getNumericTag(p_422010_);
        return numerictag != null ? numerictag.longValue() : p_421867_;
    }

    @Override
    public Optional<Long> getLong(String p_422244_) {
        NumericTag numerictag = this.getNumericTag(p_422244_);
        return numerictag != null ? Optional.of(numerictag.longValue()) : Optional.empty();
    }

    @Override
    public float getFloatOr(String p_421957_, float p_422667_) {
        NumericTag numerictag = this.getNumericTag(p_421957_);
        return numerictag != null ? numerictag.floatValue() : p_422667_;
    }

    @Override
    public double getDoubleOr(String p_422065_, double p_421551_) {
        NumericTag numerictag = this.getNumericTag(p_422065_);
        return numerictag != null ? numerictag.doubleValue() : p_421551_;
    }

    @Override
    public Optional<String> getString(String p_421854_) {
        StringTag stringtag = this.getOptionalTypedTag(p_421854_, StringTag.TYPE);
        return stringtag != null ? Optional.of(stringtag.value()) : Optional.empty();
    }

    @Override
    public String getStringOr(String p_422061_, String p_421849_) {
        StringTag stringtag = this.getOptionalTypedTag(p_422061_, StringTag.TYPE);
        return stringtag != null ? stringtag.value() : p_421849_;
    }

    @Override
    public Optional<int[]> getIntArray(String p_422301_) {
        IntArrayTag intarraytag = this.getOptionalTypedTag(p_422301_, IntArrayTag.TYPE);
        return intarraytag != null ? Optional.of(intarraytag.getAsIntArray()) : Optional.empty();
    }

    @Override
    public HolderLookup.Provider lookup() {
        return this.context.lookup();
    }

    @Override
    public java.util.Set<String> keySet() {
        return java.util.Collections.unmodifiableSet(input.keySet());
    }

    private ValueInput wrapChild(String key, CompoundTag tag) {
        return (ValueInput)(tag.isEmpty()
            ? this.context.empty()
            : new TagValueInput(this.problemReporter.forChild(new ProblemReporter.FieldPathElement(key)), this.context, tag));
    }

    static ValueInput wrapChild(ProblemReporter problemReporter, ValueInputContextHelper context, CompoundTag tag) {
        return (ValueInput)(tag.isEmpty() ? context.empty() : new TagValueInput(problemReporter, context, tag));
    }

    private ValueInput.ValueInputList wrapList(String key, ValueInputContextHelper context, ListTag tag) {
        return (ValueInput.ValueInputList)(tag.isEmpty()
            ? context.emptyList()
            : new TagValueInput.ListWrapper(this.problemReporter, key, context, tag));
    }

    private <T> ValueInput.TypedInputList<T> wrapTypedList(String key, ListTag tag, Codec<T> codec) {
        return (ValueInput.TypedInputList<T>)(tag.isEmpty()
            ? this.context.emptyTypedList()
            : new TagValueInput.TypedListWrapper<>(this.problemReporter, key, this.context, codec, tag));
    }

    static class CompoundListWrapper implements ValueInput.ValueInputList {
        private final ProblemReporter problemReporter;
        private final ValueInputContextHelper context;
        private final List<CompoundTag> list;

        public CompoundListWrapper(ProblemReporter problemReporter, ValueInputContextHelper context, List<CompoundTag> list) {
            this.problemReporter = problemReporter;
            this.context = context;
            this.list = list;
        }

        ValueInput wrapChild(int index, CompoundTag tag) {
            return TagValueInput.wrapChild(this.problemReporter.forChild(new ProblemReporter.IndexedPathElement(index)), this.context, tag);
        }

        @Override
        public boolean isEmpty() {
            return this.list.isEmpty();
        }

        @Override
        public Stream<ValueInput> stream() {
            return Streams.mapWithIndex(this.list.stream(), (p_422311_, p_421760_) -> this.wrapChild((int)p_421760_, p_422311_));
        }

        @Override
        public Iterator<ValueInput> iterator() {
            final ListIterator<CompoundTag> listiterator = this.list.listIterator();
            return new AbstractIterator<ValueInput>() {
                protected @Nullable ValueInput computeNext() {
                    if (listiterator.hasNext()) {
                        int i = listiterator.nextIndex();
                        CompoundTag compoundtag = listiterator.next();
                        return CompoundListWrapper.this.wrapChild(i, compoundtag);
                    } else {
                        return this.endOfData();
                    }
                }
            };
        }
    }

    public record DecodeFromFieldFailedProblem(String name, Tag tag, Error<?> error) implements ProblemReporter.Problem {
        @Override
        public String description() {
            return "Failed to decode value '" + this.tag + "' from field '" + this.name + "': " + this.error.message();
        }
    }

    public record DecodeFromListFailedProblem(String name, int index, Tag tag, Error<?> error) implements ProblemReporter.Problem {
        @Override
        public String description() {
            return "Failed to decode value '" + this.tag + "' from field '" + this.name + "' at index " + this.index + "': " + this.error.message();
        }
    }

    public record DecodeFromMapFailedProblem(Error<?> error) implements ProblemReporter.Problem {
        @Override
        public String description() {
            return "Failed to decode from map: " + this.error.message();
        }
    }

    static class ListWrapper implements ValueInput.ValueInputList {
        private final ProblemReporter problemReporter;
        private final String name;
        final ValueInputContextHelper context;
        private final ListTag list;

        ListWrapper(ProblemReporter problemReporter, String name, ValueInputContextHelper context, ListTag list) {
            this.problemReporter = problemReporter;
            this.name = name;
            this.context = context;
            this.list = list;
        }

        @Override
        public boolean isEmpty() {
            return this.list.isEmpty();
        }

        ProblemReporter reporterForChild(int index) {
            return this.problemReporter.forChild(new ProblemReporter.IndexedFieldPathElement(this.name, index));
        }

        void reportIndexUnwrapProblem(int index, Tag tag) {
            this.problemReporter.report(new TagValueInput.UnexpectedListElementTypeProblem(this.name, index, CompoundTag.TYPE, tag.getType()));
        }

        @Override
        public Stream<ValueInput> stream() {
            return Streams.<Tag, ValueInput>mapWithIndex(this.list.stream(), (p_421560_, p_421602_) -> {
                if (p_421560_ instanceof CompoundTag compoundtag) {
                    return TagValueInput.wrapChild(this.reporterForChild((int)p_421602_), this.context, compoundtag);
                } else {
                    this.reportIndexUnwrapProblem((int)p_421602_, p_421560_);
                    return null;
                }
            }).filter(Objects::nonNull);
        }

        @Override
        public Iterator<ValueInput> iterator() {
            final Iterator<Tag> iterator = this.list.iterator();
            return new AbstractIterator<ValueInput>() {
                private int index;

                protected @Nullable ValueInput computeNext() {
                    while (iterator.hasNext()) {
                        Tag tag = iterator.next();
                        int i = this.index++;
                        if (tag instanceof CompoundTag compoundtag) {
                            return TagValueInput.wrapChild(ListWrapper.this.reporterForChild(i), ListWrapper.this.context, compoundtag);
                        }

                        ListWrapper.this.reportIndexUnwrapProblem(i, tag);
                    }

                    return this.endOfData();
                }
            };
        }
    }

    static class TypedListWrapper<T> implements ValueInput.TypedInputList<T> {
        private final ProblemReporter problemReporter;
        private final String name;
        final ValueInputContextHelper context;
        final Codec<T> codec;
        private final ListTag list;

        TypedListWrapper(ProblemReporter problemReporter, String name, ValueInputContextHelper context, Codec<T> codec, ListTag list) {
            this.problemReporter = problemReporter;
            this.name = name;
            this.context = context;
            this.codec = codec;
            this.list = list;
        }

        @Override
        public boolean isEmpty() {
            return this.list.isEmpty();
        }

        void reportIndexUnwrapProblem(int index, Tag tag, Error<?> error) {
            this.problemReporter.report(new TagValueInput.DecodeFromListFailedProblem(this.name, index, tag, error));
        }

        @Override
        public Stream<T> stream() {
            return Streams.<Tag, T>mapWithIndex(this.list.stream(), (p_421803_, p_422239_) -> {
                return (T)(switch (this.codec.parse(this.context.ops(), p_421803_)) {
                    case Success<T> success -> (Object)success.value();
                    case Error<T> error -> {
                        this.reportIndexUnwrapProblem((int)p_422239_, p_421803_, error);
                        yield error.partialValue().orElse(null);
                    }
                    default -> throw new MatchException(null, null);
                });
            }).filter(Objects::nonNull);
        }

        @Override
        public Iterator<T> iterator() {
            final ListIterator<Tag> listiterator = this.list.listIterator();
            return new AbstractIterator<T>() {
                @Override
                protected @Nullable T computeNext() {
                    while (listiterator.hasNext()) {
                        int i = listiterator.nextIndex();
                        Tag tag = listiterator.next();
                        switch (TypedListWrapper.this.codec.parse((DynamicOps<T>)TypedListWrapper.this.context.ops(), (T)tag)) {
                            case Success<T> success:
                                return success.value();
                            case Error<T> error:
                                TypedListWrapper.this.reportIndexUnwrapProblem(i, tag, error);
                                if (!error.partialValue().isPresent()) {
                                    break;
                                }

                                return error.partialValue().get();
                            default:
                                throw new MatchException(null, null);
                        }
                    }

                    return (T)this.endOfData();
                }
            };
        }
    }

    public record UnexpectedListElementTypeProblem(String name, int index, TagType<?> expected, TagType<?> actual) implements ProblemReporter.Problem {
        @Override
        public String description() {
            return "Expected list '"
                + this.name
                + "' to contain at index "
                + this.index
                + " value of type "
                + this.expected.getName()
                + ", but got "
                + this.actual.getName();
        }
    }

    public record UnexpectedNonNumberProblem(String name, TagType<?> actual) implements ProblemReporter.Problem {
        @Override
        public String description() {
            return "Expected field '" + this.name + "' to contain number, but got " + this.actual.getName();
        }
    }

    public record UnexpectedTypeProblem(String name, TagType<?> expected, TagType<?> actual) implements ProblemReporter.Problem {
        @Override
        public String description() {
            return "Expected field '" + this.name + "' to contain value of type " + this.expected.getName() + ", but got " + this.actual.getName();
        }
    }
}
