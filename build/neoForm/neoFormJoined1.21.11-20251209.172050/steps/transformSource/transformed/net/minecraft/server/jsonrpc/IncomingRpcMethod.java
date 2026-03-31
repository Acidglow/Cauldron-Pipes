package net.minecraft.server.jsonrpc;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.mojang.serialization.JsonOps;
import java.util.Locale;
import java.util.function.Function;
import net.minecraft.core.Registry;
import net.minecraft.resources.Identifier;
import net.minecraft.server.jsonrpc.api.MethodInfo;
import net.minecraft.server.jsonrpc.api.ParamInfo;
import net.minecraft.server.jsonrpc.api.ResultInfo;
import net.minecraft.server.jsonrpc.api.Schema;
import net.minecraft.server.jsonrpc.internalapi.MinecraftApi;
import net.minecraft.server.jsonrpc.methods.ClientInfo;
import net.minecraft.server.jsonrpc.methods.EncodeJsonRpcException;
import net.minecraft.server.jsonrpc.methods.InvalidParameterJsonRpcException;
import org.jspecify.annotations.Nullable;

public interface IncomingRpcMethod<Params, Result> {
    MethodInfo<Params, Result> info();

    IncomingRpcMethod.Attributes attributes();

    JsonElement apply(MinecraftApi api, @Nullable JsonElement json, ClientInfo clientInfo);

    static <Result> IncomingRpcMethod.IncomingRpcMethodBuilder<Void, Result> method(IncomingRpcMethod.ParameterlessRpcMethodFunction<Result> function) {
        return new IncomingRpcMethod.IncomingRpcMethodBuilder<>(function);
    }

    static <Params, Result> IncomingRpcMethod.IncomingRpcMethodBuilder<Params, Result> method(IncomingRpcMethod.RpcMethodFunction<Params, Result> function) {
        return new IncomingRpcMethod.IncomingRpcMethodBuilder<>(function);
    }

    static <Result> IncomingRpcMethod.IncomingRpcMethodBuilder<Void, Result> method(Function<MinecraftApi, Result> function) {
        return new IncomingRpcMethod.IncomingRpcMethodBuilder<>(function);
    }

    public record Attributes(boolean runOnMainThread, boolean discoverable) {
    }

    public static class IncomingRpcMethodBuilder<Params, Result> {
        private String description = "";
        private @Nullable ParamInfo<Params> paramInfo;
        private @Nullable ResultInfo<Result> resultInfo;
        private boolean discoverable = true;
        private boolean runOnMainThread = true;
        private IncomingRpcMethod.@Nullable ParameterlessRpcMethodFunction<Result> parameterlessFunction;
        private IncomingRpcMethod.@Nullable RpcMethodFunction<Params, Result> parameterFunction;

        public IncomingRpcMethodBuilder(IncomingRpcMethod.ParameterlessRpcMethodFunction<Result> function) {
            this.parameterlessFunction = function;
        }

        public IncomingRpcMethodBuilder(IncomingRpcMethod.RpcMethodFunction<Params, Result> function) {
            this.parameterFunction = function;
        }

        public IncomingRpcMethodBuilder(Function<MinecraftApi, Result> function) {
            this.parameterlessFunction = (p_457689_, p_457693_) -> function.apply(p_457689_);
        }

        public IncomingRpcMethod.IncomingRpcMethodBuilder<Params, Result> description(String description) {
            this.description = description;
            return this;
        }

        public IncomingRpcMethod.IncomingRpcMethodBuilder<Params, Result> response(String name, Schema<Result> schema) {
            this.resultInfo = new ResultInfo<>(name, schema.info());
            return this;
        }

        public IncomingRpcMethod.IncomingRpcMethodBuilder<Params, Result> param(String name, Schema<Params> schema) {
            this.paramInfo = new ParamInfo<>(name, schema.info());
            return this;
        }

        public IncomingRpcMethod.IncomingRpcMethodBuilder<Params, Result> undiscoverable() {
            this.discoverable = false;
            return this;
        }

        public IncomingRpcMethod.IncomingRpcMethodBuilder<Params, Result> notOnMainThread() {
            this.runOnMainThread = false;
            return this;
        }

        public IncomingRpcMethod<Params, Result> build() {
            if (this.resultInfo == null) {
                throw new IllegalStateException("No response defined");
            } else {
                IncomingRpcMethod.Attributes incomingrpcmethod$attributes = new IncomingRpcMethod.Attributes(this.runOnMainThread, this.discoverable);
                MethodInfo<Params, Result> methodinfo = new MethodInfo<>(this.description, this.paramInfo, this.resultInfo);
                if (this.parameterlessFunction != null) {
                    return new IncomingRpcMethod.ParameterlessMethod<>(methodinfo, incomingrpcmethod$attributes, this.parameterlessFunction);
                } else if (this.parameterFunction != null) {
                    if (this.paramInfo == null) {
                        throw new IllegalStateException("No param schema defined");
                    } else {
                        return new IncomingRpcMethod.Method<>(methodinfo, incomingrpcmethod$attributes, this.parameterFunction);
                    }
                } else {
                    throw new IllegalStateException("No method defined");
                }
            }
        }

        public IncomingRpcMethod<?, ?> register(Registry<IncomingRpcMethod<?, ?>> registry, String name) {
            return this.register(registry, Identifier.withDefaultNamespace(name));
        }

        private IncomingRpcMethod<?, ?> register(Registry<IncomingRpcMethod<?, ?>> registry, Identifier name) {
            return Registry.register(registry, name, this.build());
        }
    }

    public record Method<Params, Result>(
        MethodInfo<Params, Result> info, IncomingRpcMethod.Attributes attributes, IncomingRpcMethod.RpcMethodFunction<Params, Result> function
    ) implements IncomingRpcMethod<Params, Result> {
        @Override
        public JsonElement apply(MinecraftApi p_442932_, @Nullable JsonElement p_443044_, ClientInfo p_442918_) {
            if (p_443044_ != null && (p_443044_.isJsonArray() || p_443044_.isJsonObject())) {
                if (this.info.params().isEmpty()) {
                    throw new IllegalArgumentException("Method defined as having parameters without describing them");
                } else {
                    JsonElement jsonelement;
                    if (p_443044_.isJsonObject()) {
                        String s = this.info.params().get().name();
                        JsonElement jsonelement1 = p_443044_.getAsJsonObject().get(s);
                        if (jsonelement1 == null) {
                            throw new InvalidParameterJsonRpcException(
                                String.format(Locale.ROOT, "Params passed by-name, but expected param [%s] does not exist", s)
                            );
                        }

                        jsonelement = jsonelement1;
                    } else {
                        JsonArray jsonarray = p_443044_.getAsJsonArray();
                        if (jsonarray.isEmpty() || jsonarray.size() > 1) {
                            throw new InvalidParameterJsonRpcException("Expected exactly one element in the params array");
                        }

                        jsonelement = jsonarray.get(0);
                    }

                    Params params = this.info
                        .params()
                        .get()
                        .schema()
                        .codec()
                        .parse(JsonOps.INSTANCE, jsonelement)
                        .getOrThrow(InvalidParameterJsonRpcException::new);
                    Result result = this.function.apply(p_442932_, params, p_442918_);
                    if (this.info.result().isEmpty()) {
                        throw new IllegalStateException("No result codec defined");
                    } else {
                        return this.info.result().get().schema().codec().encodeStart(JsonOps.INSTANCE, result).getOrThrow(EncodeJsonRpcException::new);
                    }
                }
            } else {
                throw new InvalidParameterJsonRpcException("Expected params as array or named");
            }
        }
    }

    public record ParameterlessMethod<Params, Result>(
        MethodInfo<Params, Result> info, IncomingRpcMethod.Attributes attributes, IncomingRpcMethod.ParameterlessRpcMethodFunction<Result> supplier
    ) implements IncomingRpcMethod<Params, Result> {
        @Override
        public JsonElement apply(MinecraftApi p_443197_, @Nullable JsonElement p_442828_, ClientInfo p_443323_) {
            if (p_442828_ == null || p_442828_.isJsonArray() && p_442828_.getAsJsonArray().isEmpty()) {
                if (this.info.params().isPresent()) {
                    throw new IllegalArgumentException("Parameterless method unexpectedly has parameter description");
                } else {
                    Result result = this.supplier.apply(p_443197_, p_443323_);
                    if (this.info.result().isEmpty()) {
                        throw new IllegalStateException("No result codec defined");
                    } else {
                        return this.info
                            .result()
                            .get()
                            .schema()
                            .codec()
                            .encodeStart(JsonOps.INSTANCE, result)
                            .getOrThrow(InvalidParameterJsonRpcException::new);
                    }
                }
            } else {
                throw new InvalidParameterJsonRpcException("Expected no params, or an empty array");
            }
        }
    }

    @FunctionalInterface
    public interface ParameterlessRpcMethodFunction<Result> {
        Result apply(MinecraftApi api, ClientInfo clientInfo);
    }

    @FunctionalInterface
    public interface RpcMethodFunction<Params, Result> {
        Result apply(MinecraftApi api, Params params, ClientInfo clientInfo);
    }
}
