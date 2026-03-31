package net.minecraft.server.jsonrpc.api;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.mojang.serialization.codecs.RecordCodecBuilder.Instance;

public record ParamInfo<Param>(String name, Schema<Param> schema, boolean required) {
    public ParamInfo(String p_442876_, Schema<Param> p_443570_) {
        this(p_442876_, p_443570_, true);
    }

    public static <Param> MapCodec<ParamInfo<Param>> typedCodec() {
        return RecordCodecBuilder.mapCodec(
            p_457372_ -> p_457372_.group(
                    Codec.STRING.fieldOf("name").forGetter(ParamInfo::name),
                    Schema.<Param>typedCodec().fieldOf("schema").forGetter(ParamInfo::schema),
                    Codec.BOOL.fieldOf("required").forGetter(ParamInfo::required)
                )
                .apply(p_457372_, ParamInfo::new)
        );
    }
}
