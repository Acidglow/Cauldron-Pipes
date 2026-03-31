package net.minecraft.server.jsonrpc;

import com.google.gson.JsonElement;
import com.mojang.serialization.JsonOps;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.jsonrpc.api.MethodInfo;
import net.minecraft.server.jsonrpc.api.ParamInfo;
import net.minecraft.server.jsonrpc.api.ResultInfo;
import net.minecraft.server.jsonrpc.api.Schema;
import org.jspecify.annotations.Nullable;

public interface OutgoingRpcMethod<Params, Result> {
    String NOTIFICATION_PREFIX = "notification/";

    MethodInfo<Params, Result> info();

    OutgoingRpcMethod.Attributes attributes();

    default @Nullable JsonElement encodeParams(Params params) {
        return null;
    }

    default @Nullable Result decodeResult(JsonElement json) {
        return null;
    }

    static OutgoingRpcMethod.OutgoingRpcMethodBuilder<Void, Void> notification() {
        return new OutgoingRpcMethod.OutgoingRpcMethodBuilder<>(OutgoingRpcMethod.ParmeterlessNotification::new);
    }

    static <Params> OutgoingRpcMethod.OutgoingRpcMethodBuilder<Params, Void> notificationWithParams() {
        return new OutgoingRpcMethod.OutgoingRpcMethodBuilder<>(OutgoingRpcMethod.Notification::new);
    }

    static <Result> OutgoingRpcMethod.OutgoingRpcMethodBuilder<Void, Result> request() {
        return new OutgoingRpcMethod.OutgoingRpcMethodBuilder<>(OutgoingRpcMethod.ParameterlessMethod::new);
    }

    static <Params, Result> OutgoingRpcMethod.OutgoingRpcMethodBuilder<Params, Result> requestWithParams() {
        return new OutgoingRpcMethod.OutgoingRpcMethodBuilder<>(OutgoingRpcMethod.Method::new);
    }

    public record Attributes(boolean discoverable) {
    }

    @FunctionalInterface
    public interface Factory<Params, Result> {
        OutgoingRpcMethod<Params, Result> create(MethodInfo<Params, Result> info, OutgoingRpcMethod.Attributes attributes);
    }

    public record Method<Params, Result>(MethodInfo<Params, Result> info, OutgoingRpcMethod.Attributes attributes) implements OutgoingRpcMethod<Params, Result> {
        @Override
        public @Nullable JsonElement encodeParams(Params p_442798_) {
            if (this.info.params().isEmpty()) {
                throw new IllegalStateException("Method defined as having no parameters");
            } else {
                return this.info.params().get().schema().codec().encodeStart(JsonOps.INSTANCE, p_442798_).getOrThrow();
            }
        }

        @Override
        public Result decodeResult(JsonElement p_442832_) {
            if (this.info.result().isEmpty()) {
                throw new IllegalStateException("Method defined as having no result");
            } else {
                return this.info.result().get().schema().codec().parse(JsonOps.INSTANCE, p_442832_).getOrThrow();
            }
        }
    }

    public record Notification<Params>(MethodInfo<Params, Void> info, OutgoingRpcMethod.Attributes attributes) implements OutgoingRpcMethod<Params, Void> {
        @Override
        public @Nullable JsonElement encodeParams(Params p_443326_) {
            if (this.info.params().isEmpty()) {
                throw new IllegalStateException("Method defined as having no parameters");
            } else {
                return this.info.params().get().schema().codec().encodeStart(JsonOps.INSTANCE, p_443326_).getOrThrow();
            }
        }
    }

    public static class OutgoingRpcMethodBuilder<Params, Result> {
        public static final OutgoingRpcMethod.Attributes DEFAULT_ATTRIBUTES = new OutgoingRpcMethod.Attributes(true);
        private final OutgoingRpcMethod.Factory<Params, Result> method;
        private String description = "";
        private @Nullable ParamInfo<Params> paramInfo;
        private @Nullable ResultInfo<Result> resultInfo;

        public OutgoingRpcMethodBuilder(OutgoingRpcMethod.Factory<Params, Result> method) {
            this.method = method;
        }

        public OutgoingRpcMethod.OutgoingRpcMethodBuilder<Params, Result> description(String description) {
            this.description = description;
            return this;
        }

        public OutgoingRpcMethod.OutgoingRpcMethodBuilder<Params, Result> response(String name, Schema<Result> schema) {
            this.resultInfo = new ResultInfo<>(name, schema);
            return this;
        }

        public OutgoingRpcMethod.OutgoingRpcMethodBuilder<Params, Result> param(String name, Schema<Params> schema) {
            this.paramInfo = new ParamInfo<>(name, schema);
            return this;
        }

        private OutgoingRpcMethod<Params, Result> build() {
            MethodInfo<Params, Result> methodinfo = new MethodInfo<>(this.description, this.paramInfo, this.resultInfo);
            return this.method.create(methodinfo, DEFAULT_ATTRIBUTES);
        }

        public Holder.Reference<OutgoingRpcMethod<Params, Result>> register(String name) {
            return this.register(Identifier.withDefaultNamespace("notification/" + name));
        }

        private Holder.Reference<OutgoingRpcMethod<Params, Result>> register(Identifier name) {
            return Registry.registerForHolder(BuiltInRegistries.OUTGOING_RPC_METHOD, name, this.build());
        }
    }

    public record ParameterlessMethod<Result>(MethodInfo<Void, Result> info, OutgoingRpcMethod.Attributes attributes)
        implements OutgoingRpcMethod<Void, Result> {
        @Override
        public Result decodeResult(JsonElement p_442885_) {
            if (this.info.result().isEmpty()) {
                throw new IllegalStateException("Method defined as having no result");
            } else {
                return this.info.result().get().schema().codec().parse(JsonOps.INSTANCE, p_442885_).getOrThrow();
            }
        }
    }

    public record ParmeterlessNotification(MethodInfo<Void, Void> info, OutgoingRpcMethod.Attributes attributes) implements OutgoingRpcMethod<Void, Void> {
    }
}
