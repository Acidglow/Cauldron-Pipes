package net.minecraft.server.jsonrpc.api;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.mojang.serialization.codecs.RecordCodecBuilder.Instance;
import java.util.List;
import java.util.Optional;
import net.minecraft.resources.Identifier;
import org.jspecify.annotations.Nullable;

public record MethodInfo<Params, Result>(String description, Optional<ParamInfo<Params>> params, Optional<ResultInfo<Result>> result) {
    public MethodInfo(String p_442898_, @Nullable ParamInfo<Params> p_451419_, @Nullable ResultInfo<Result> p_450926_) {
        this(p_442898_, Optional.ofNullable(p_451419_), Optional.ofNullable(p_450926_));
    }

    private static <Params> Optional<ParamInfo<Params>> toOptional(List<ParamInfo<Params>> params) {
        return params.isEmpty() ? Optional.empty() : Optional.of(params.getFirst());
    }

    private static <Params> List<ParamInfo<Params>> toList(Optional<ParamInfo<Params>> params) {
        return params.isPresent() ? List.of(params.get()) : List.of();
    }

    private static <Params> Codec<Optional<ParamInfo<Params>>> paramsTypedCodec() {
        return ParamInfo.<Params>typedCodec().codec().listOf().xmap(MethodInfo::toOptional, MethodInfo::toList);
    }

    static <Params, Result> MapCodec<MethodInfo<Params, Result>> typedCodec() {
        return RecordCodecBuilder.mapCodec(
            p_457370_ -> p_457370_.group(
                    Codec.STRING.fieldOf("description").forGetter(MethodInfo::description),
                    MethodInfo.<Params>paramsTypedCodec().fieldOf("params").forGetter(MethodInfo::params),
                    ResultInfo.<Result>typedCodec().optionalFieldOf("result").forGetter(MethodInfo::result)
                )
                .apply(p_457370_, MethodInfo::new)
        );
    }

    public MethodInfo.Named<Params, Result> named(Identifier name) {
        return new MethodInfo.Named<>(name, this);
    }

    public record Named<Params, Result>(Identifier name, MethodInfo<Params, Result> contents) {
        public static final Codec<MethodInfo.Named<?, ?>> CODEC = (Codec<MethodInfo.Named<?, ?>>) (Codec<?>) typedCodec();

        public static <Params, Result> Codec<MethodInfo.Named<Params, Result>> typedCodec() {
            return RecordCodecBuilder.create(
                p_466328_ -> p_466328_.group(
                        Identifier.CODEC.fieldOf("name").forGetter(MethodInfo.Named::name),
                        MethodInfo.<Params, Result>typedCodec().forGetter(MethodInfo.Named::contents)
                    )
                    .apply(p_466328_, MethodInfo.Named::new)
            );
        }
    }
}
