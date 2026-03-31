package net.minecraft.commands.arguments.item;

import com.mojang.brigadier.ImmutableStringReader;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.Dynamic2CommandExceptionType;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.Decoder;
import com.mojang.serialization.Dynamic;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import net.minecraft.advancements.criterion.MinMaxBounds;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.HolderSet;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.component.predicates.DataComponentPredicate;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.RegistryOps;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.TagKey;
import net.minecraft.util.Unit;
import net.minecraft.util.Util;
import net.minecraft.util.parsing.packrat.commands.ParserBasedArgument;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

public class ItemPredicateArgument extends ParserBasedArgument<ItemPredicateArgument.Result> {
    private static final Collection<String> EXAMPLES = Arrays.asList("stick", "minecraft:stick", "#stick", "#stick{foo:'bar'}");
    static final DynamicCommandExceptionType ERROR_UNKNOWN_ITEM = new DynamicCommandExceptionType(
        p_335502_ -> Component.translatableEscape("argument.item.id.invalid", p_335502_)
    );
    static final DynamicCommandExceptionType ERROR_UNKNOWN_TAG = new DynamicCommandExceptionType(
        p_335527_ -> Component.translatableEscape("arguments.item.tag.unknown", p_335527_)
    );
    static final DynamicCommandExceptionType ERROR_UNKNOWN_COMPONENT = new DynamicCommandExceptionType(
        p_335843_ -> Component.translatableEscape("arguments.item.component.unknown", p_335843_)
    );
    static final Dynamic2CommandExceptionType ERROR_MALFORMED_COMPONENT = new Dynamic2CommandExceptionType(
        (p_335483_, p_335643_) -> Component.translatableEscape("arguments.item.component.malformed", p_335483_, p_335643_)
    );
    static final DynamicCommandExceptionType ERROR_UNKNOWN_PREDICATE = new DynamicCommandExceptionType(
        p_335658_ -> Component.translatableEscape("arguments.item.predicate.unknown", p_335658_)
    );
    static final Dynamic2CommandExceptionType ERROR_MALFORMED_PREDICATE = new Dynamic2CommandExceptionType(
        (p_336040_, p_335526_) -> Component.translatableEscape("arguments.item.predicate.malformed", p_336040_, p_335526_)
    );
    private static final Identifier COUNT_ID = Identifier.withDefaultNamespace("count");
    static final Map<Identifier, ItemPredicateArgument.ComponentWrapper> PSEUDO_COMPONENTS = Stream.of(
            new ItemPredicateArgument.ComponentWrapper(
                COUNT_ID, p_335429_ -> true, MinMaxBounds.Ints.CODEC.map(p_468784_ -> p_465860_ -> p_468784_.matches(p_465860_.getCount()))
            )
        )
        .collect(Collectors.toUnmodifiableMap(ItemPredicateArgument.ComponentWrapper::id, p_335476_ -> (ItemPredicateArgument.ComponentWrapper)p_335476_));
    static final Map<Identifier, ItemPredicateArgument.PredicateWrapper> PSEUDO_PREDICATES = Stream.of(
            new ItemPredicateArgument.PredicateWrapper(COUNT_ID, MinMaxBounds.Ints.CODEC.map(p_468964_ -> p_465857_ -> p_468964_.matches(p_465857_.getCount())))
        )
        .collect(Collectors.toUnmodifiableMap(ItemPredicateArgument.PredicateWrapper::id, p_454377_ -> (ItemPredicateArgument.PredicateWrapper)p_454377_));

    private static ItemPredicateArgument.PredicateWrapper createComponentExistencePredicate(Holder.Reference<DataComponentType<?>> component) {
        Predicate<ItemStack> predicate = p_454376_ -> p_454376_.has(component.value());
        return new ItemPredicateArgument.PredicateWrapper(component.key().identifier(), Unit.CODEC.map(p_456278_ -> predicate));
    }

    public ItemPredicateArgument(CommandBuildContext context) {
        super(ComponentPredicateParser.createGrammar(new ItemPredicateArgument.Context(context)).mapResult(p_465858_ -> Util.allOf(p_465858_)::test));
    }

    public static ItemPredicateArgument itemPredicate(CommandBuildContext context) {
        return new ItemPredicateArgument(context);
    }

    public static ItemPredicateArgument.Result getItemPredicate(CommandContext<CommandSourceStack> context, String name) {
        return context.getArgument(name, ItemPredicateArgument.Result.class);
    }

    @Override
    public Collection<String> getExamples() {
        return EXAMPLES;
    }

    record ComponentWrapper(Identifier id, Predicate<ItemStack> presenceChecker, Decoder<? extends Predicate<ItemStack>> valueChecker) {
        public static <T> ItemPredicateArgument.ComponentWrapper create(ImmutableStringReader reader, Identifier id, DataComponentType<T> componentType) throws CommandSyntaxException {
            Codec<T> codec = componentType.codec();
            if (codec == null) {
                throw ItemPredicateArgument.ERROR_UNKNOWN_COMPONENT.createWithContext(reader, id);
            } else {
                return new ItemPredicateArgument.ComponentWrapper(id, p_396321_ -> p_396321_.has(componentType), codec.map(p_335913_ -> p_335541_ -> {
                    T t = p_335541_.get(componentType);
                    return Objects.equals(p_335913_, t);
                }));
            }
        }

        public Predicate<ItemStack> decode(ImmutableStringReader reader, Dynamic<?> data) throws CommandSyntaxException {
            DataResult<? extends Predicate<ItemStack>> dataresult = this.valueChecker.parse(data);
            return (Predicate<ItemStack>)dataresult.getOrThrow(
                p_465862_ -> ItemPredicateArgument.ERROR_MALFORMED_COMPONENT.createWithContext(reader, this.id.toString(), p_465862_)
            );
        }
    }

    static class Context
        implements ComponentPredicateParser.Context<Predicate<ItemStack>, ItemPredicateArgument.ComponentWrapper, ItemPredicateArgument.PredicateWrapper> {
        private final HolderLookup.Provider registries;
        private final HolderLookup.RegistryLookup<Item> items;
        private final HolderLookup.RegistryLookup<DataComponentType<?>> components;
        private final HolderLookup.RegistryLookup<DataComponentPredicate.Type<?>> predicates;

        Context(HolderLookup.Provider registries) {
            this.registries = registries;
            this.items = registries.lookupOrThrow(Registries.ITEM);
            this.components = registries.lookupOrThrow(Registries.DATA_COMPONENT_TYPE);
            this.predicates = registries.lookupOrThrow(Registries.DATA_COMPONENT_PREDICATE_TYPE);
        }

        public Predicate<ItemStack> forElementType(ImmutableStringReader p_335407_, Identifier p_467639_) throws CommandSyntaxException {
            Holder.Reference<Item> reference = this.items
                .get(ResourceKey.create(Registries.ITEM, p_467639_))
                .orElseThrow(() -> ItemPredicateArgument.ERROR_UNKNOWN_ITEM.createWithContext(p_335407_, p_467639_));
            return p_335935_ -> p_335935_.is(reference);
        }

        public Predicate<ItemStack> forTagType(ImmutableStringReader p_335664_, Identifier p_468229_) throws CommandSyntaxException {
            HolderSet<Item> holderset = this.items
                .get(TagKey.create(Registries.ITEM, p_468229_))
                .orElseThrow(() -> ItemPredicateArgument.ERROR_UNKNOWN_TAG.createWithContext(p_335664_, p_468229_));
            return p_336090_ -> p_336090_.is(holderset);
        }

        public ItemPredicateArgument.ComponentWrapper lookupComponentType(ImmutableStringReader p_336180_, Identifier p_467090_) throws CommandSyntaxException {
            ItemPredicateArgument.ComponentWrapper itempredicateargument$componentwrapper = ItemPredicateArgument.PSEUDO_COMPONENTS.get(p_467090_);
            if (itempredicateargument$componentwrapper != null) {
                return itempredicateargument$componentwrapper;
            } else {
                DataComponentType<?> datacomponenttype = this.components
                    .get(ResourceKey.create(Registries.DATA_COMPONENT_TYPE, p_467090_))
                    .map(Holder::value)
                    .orElseThrow(() -> ItemPredicateArgument.ERROR_UNKNOWN_COMPONENT.createWithContext(p_336180_, p_467090_));
                return ItemPredicateArgument.ComponentWrapper.create(p_336180_, p_467090_, datacomponenttype);
            }
        }

        public Predicate<ItemStack> createComponentTest(ImmutableStringReader p_335953_, ItemPredicateArgument.ComponentWrapper p_335824_, Dynamic<?> p_399995_) throws CommandSyntaxException {
            return p_335824_.decode(p_335953_, RegistryOps.injectRegistryContext(p_399995_, this.registries));
        }

        public Predicate<ItemStack> createComponentTest(ImmutableStringReader p_335602_, ItemPredicateArgument.ComponentWrapper p_335607_) {
            return p_335607_.presenceChecker;
        }

        public ItemPredicateArgument.PredicateWrapper lookupPredicateType(ImmutableStringReader p_335631_, Identifier p_469252_) throws CommandSyntaxException {
            ItemPredicateArgument.PredicateWrapper itempredicateargument$predicatewrapper = ItemPredicateArgument.PSEUDO_PREDICATES.get(p_469252_);
            return itempredicateargument$predicatewrapper != null
                ? itempredicateargument$predicatewrapper
                : this.predicates
                    .get(ResourceKey.create(Registries.DATA_COMPONENT_PREDICATE_TYPE, p_469252_))
                    .map(ItemPredicateArgument.PredicateWrapper::new)
                    .or(
                        () -> this.components
                            .get(ResourceKey.create(Registries.DATA_COMPONENT_TYPE, p_469252_))
                            .map(ItemPredicateArgument::createComponentExistencePredicate)
                    )
                    .orElseThrow(() -> ItemPredicateArgument.ERROR_UNKNOWN_PREDICATE.createWithContext(p_335631_, p_469252_));
        }

        public Predicate<ItemStack> createPredicateTest(ImmutableStringReader p_335414_, ItemPredicateArgument.PredicateWrapper p_335561_, Dynamic<?> p_400185_) throws CommandSyntaxException {
            return p_335561_.decode(p_335414_, RegistryOps.injectRegistryContext(p_400185_, this.registries));
        }

        @Override
        public Stream<Identifier> listElementTypes() {
            return this.items.listElementIds().map(ResourceKey::identifier);
        }

        @Override
        public Stream<Identifier> listTagTypes() {
            return this.items.listTagIds().map(TagKey::location);
        }

        @Override
        public Stream<Identifier> listComponentTypes() {
            return Stream.concat(
                ItemPredicateArgument.PSEUDO_COMPONENTS.keySet().stream(),
                this.components.listElements().filter(p_335558_ -> !p_335558_.value().isTransient()).map(p_465864_ -> p_465864_.key().identifier())
            );
        }

        @Override
        public Stream<Identifier> listPredicateTypes() {
            return Stream.concat(ItemPredicateArgument.PSEUDO_PREDICATES.keySet().stream(), this.predicates.listElementIds().map(ResourceKey::identifier));
        }

        public Predicate<ItemStack> negate(Predicate<ItemStack> p_335412_) {
            return p_335412_.negate();
        }

        public Predicate<ItemStack> anyOf(List<Predicate<ItemStack>> p_336064_) {
            return Util.anyOf(p_336064_);
        }
    }

    record PredicateWrapper(Identifier id, Decoder<? extends Predicate<ItemStack>> type) {
        public PredicateWrapper(Holder.Reference<DataComponentPredicate.Type<?>> p_336100_) {
            this(p_336100_.key().identifier(), p_336100_.value().codec().map(p_400078_ -> p_400078_::matches));
        }

        public Predicate<ItemStack> decode(ImmutableStringReader reader, Dynamic<?> data) throws CommandSyntaxException {
            DataResult<? extends Predicate<ItemStack>> dataresult = this.type.parse(data);
            return (Predicate<ItemStack>)dataresult.getOrThrow(
                p_465866_ -> ItemPredicateArgument.ERROR_MALFORMED_PREDICATE.createWithContext(reader, this.id.toString(), p_465866_)
            );
        }
    }

    public interface Result extends Predicate<ItemStack> {
    }
}
