package net.minecraft.commands.arguments;

import com.google.gson.JsonObject;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.Dynamic2CommandExceptionType;
import com.mojang.brigadier.exceptions.Dynamic3CommandExceptionType;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import com.mojang.datafixers.util.Either;
import java.util.Arrays;
import java.util.Collection;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.function.Predicate;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.synchronization.ArgumentTypeInfo;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.HolderSet;
import net.minecraft.core.Registry;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.TagKey;

public class ResourceOrTagArgument<T> implements ArgumentType<ResourceOrTagArgument.Result<T>> {
    private static final Collection<String> EXAMPLES = Arrays.asList("foo", "foo:bar", "012", "#skeletons", "#minecraft:skeletons");
    private static final Dynamic2CommandExceptionType ERROR_UNKNOWN_TAG = new Dynamic2CommandExceptionType(
        (p_304107_, p_304108_) -> Component.translatableEscape("argument.resource_tag.not_found", p_304107_, p_304108_)
    );
    private static final Dynamic3CommandExceptionType ERROR_INVALID_TAG_TYPE = new Dynamic3CommandExceptionType(
        (p_304109_, p_304110_, p_304111_) -> Component.translatableEscape("argument.resource_tag.invalid_type", p_304109_, p_304110_, p_304111_)
    );
    private final HolderLookup<T> registryLookup;
    final ResourceKey<? extends Registry<T>> registryKey;

    public ResourceOrTagArgument(CommandBuildContext context, ResourceKey<? extends Registry<T>> registryKey) {
        this.registryKey = registryKey;
        this.registryLookup = context.lookupOrThrow(registryKey);
    }

    public static <T> ResourceOrTagArgument<T> resourceOrTag(CommandBuildContext context, ResourceKey<? extends Registry<T>> registryKey) {
        return new ResourceOrTagArgument<>(context, registryKey);
    }

    public static <T> ResourceOrTagArgument.Result<T> getResourceOrTag(
        CommandContext<CommandSourceStack> context, String argument, ResourceKey<Registry<T>> registryKey
    ) throws CommandSyntaxException {
        ResourceOrTagArgument.Result<?> result = context.getArgument(argument, ResourceOrTagArgument.Result.class);
        Optional<ResourceOrTagArgument.Result<T>> optional = result.cast(registryKey);
        return optional.orElseThrow(() -> result.unwrap().map(p_465834_ -> {
            ResourceKey<?> resourcekey = p_465834_.key();
            return ResourceArgument.ERROR_INVALID_RESOURCE_TYPE.create(resourcekey.identifier(), resourcekey.registry(), registryKey.identifier());
        }, p_465832_ -> {
            TagKey<?> tagkey = p_465832_.key();
            return ERROR_INVALID_TAG_TYPE.create(tagkey.location(), tagkey.registry(), registryKey.identifier());
        }));
    }

    public ResourceOrTagArgument.Result<T> parse(StringReader reader) throws CommandSyntaxException {
        if (reader.canRead() && reader.peek() == '#') {
            int i = reader.getCursor();

            try {
                reader.skip();
                Identifier identifier1 = Identifier.read(reader);
                TagKey<T> tagkey = TagKey.create(this.registryKey, identifier1);
                HolderSet.Named<T> named = this.registryLookup
                    .get(tagkey)
                    .orElseThrow(() -> ERROR_UNKNOWN_TAG.createWithContext(reader, identifier1, this.registryKey.identifier()));
                return new ResourceOrTagArgument.TagResult<>(named);
            } catch (CommandSyntaxException commandsyntaxexception) {
                reader.setCursor(i);
                throw commandsyntaxexception;
            }
        } else {
            Identifier identifier = Identifier.read(reader);
            ResourceKey<T> resourcekey = ResourceKey.create(this.registryKey, identifier);
            Holder.Reference<T> reference = this.registryLookup
                .get(resourcekey)
                .orElseThrow(() -> ResourceArgument.ERROR_UNKNOWN_RESOURCE.createWithContext(reader, identifier, this.registryKey.identifier()));
            return new ResourceOrTagArgument.ResourceResult<>(reference);
        }
    }

    @Override
    public <S> CompletableFuture<Suggestions> listSuggestions(CommandContext<S> context, SuggestionsBuilder builder) {
        return SharedSuggestionProvider.listSuggestions(context, builder, this.registryKey, SharedSuggestionProvider.ElementSuggestionType.ALL);
    }

    @Override
    public Collection<String> getExamples() {
        return EXAMPLES;
    }

    public static class Info<T> implements ArgumentTypeInfo<ResourceOrTagArgument<T>, ResourceOrTagArgument.Info<T>.Template> {
        public void serializeToNetwork(ResourceOrTagArgument.Info<T>.Template p_250419_, FriendlyByteBuf p_249726_) {
            p_249726_.writeResourceKey(p_250419_.registryKey);
        }

        public ResourceOrTagArgument.Info<T>.Template deserializeFromNetwork(FriendlyByteBuf p_250205_) {
            return new ResourceOrTagArgument.Info.Template(p_250205_.readRegistryKey());
        }

        public void serializeToJson(ResourceOrTagArgument.Info<T>.Template p_251957_, JsonObject p_249067_) {
            p_249067_.addProperty("registry", p_251957_.registryKey.identifier().toString());
        }

        public ResourceOrTagArgument.Info<T>.Template unpack(ResourceOrTagArgument<T> p_252206_) {
            return new ResourceOrTagArgument.Info.Template(p_252206_.registryKey);
        }

        public final class Template implements ArgumentTypeInfo.Template<ResourceOrTagArgument<T>> {
            final ResourceKey<? extends Registry<T>> registryKey;

            Template(ResourceKey<? extends Registry<T>> registryKey) {
                this.registryKey = registryKey;
            }

            public ResourceOrTagArgument<T> instantiate(CommandBuildContext p_251386_) {
                return new ResourceOrTagArgument<>(p_251386_, this.registryKey);
            }

            @Override
            public ArgumentTypeInfo<ResourceOrTagArgument<T>, ?> type() {
                return Info.this;
            }
        }
    }

    record ResourceResult<T>(Holder.Reference<T> value) implements ResourceOrTagArgument.Result<T> {
        @Override
        public Either<Holder.Reference<T>, HolderSet.Named<T>> unwrap() {
            return Either.left(this.value);
        }

        @Override
        public <E> Optional<ResourceOrTagArgument.Result<E>> cast(ResourceKey<? extends Registry<E>> p_250007_) {
            return this.value.key().isFor(p_250007_) ? Optional.of((ResourceOrTagArgument.Result<E>)this) : Optional.empty();
        }

        public boolean test(Holder<T> holder) {
            return holder.equals(this.value);
        }

        @Override
        public String asPrintable() {
            return this.value.key().identifier().toString();
        }
    }

    public interface Result<T> extends Predicate<Holder<T>> {
        Either<Holder.Reference<T>, HolderSet.Named<T>> unwrap();

        <E> Optional<ResourceOrTagArgument.Result<E>> cast(ResourceKey<? extends Registry<E>> registryKey);

        String asPrintable();
    }

    record TagResult<T>(HolderSet.Named<T> tag) implements ResourceOrTagArgument.Result<T> {
        @Override
        public Either<Holder.Reference<T>, HolderSet.Named<T>> unwrap() {
            return Either.right(this.tag);
        }

        @Override
        public <E> Optional<ResourceOrTagArgument.Result<E>> cast(ResourceKey<? extends Registry<E>> p_250945_) {
            return this.tag.key().isFor(p_250945_) ? Optional.of((ResourceOrTagArgument.Result<E>)this) : Optional.empty();
        }

        public boolean test(Holder<T> holder) {
            return this.tag.contains(holder);
        }

        @Override
        public String asPrintable() {
            return "#" + this.tag.key().location();
        }
    }
}
