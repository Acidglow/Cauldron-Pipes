package net.minecraft.server.jsonrpc;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.util.List;
import net.minecraft.resources.Identifier;
import net.minecraft.util.GsonHelper;
import org.jspecify.annotations.Nullable;

public class JsonRPCUtils {
    public static final String JSON_RPC_VERSION = "2.0";
    public static final String OPEN_RPC_VERSION = "1.3.2";

    public static JsonObject createSuccessResult(JsonElement requestId, JsonElement result) {
        JsonObject jsonobject = new JsonObject();
        jsonobject.addProperty("jsonrpc", "2.0");
        jsonobject.add("id", requestId);
        jsonobject.add("result", result);
        return jsonobject;
    }

    public static JsonObject createRequest(@Nullable Integer requestId, Identifier methodName, List<JsonElement> params) {
        JsonObject jsonobject = new JsonObject();
        jsonobject.addProperty("jsonrpc", "2.0");
        if (requestId != null) {
            jsonobject.addProperty("id", requestId);
        }

        jsonobject.addProperty("method", methodName.toString());
        if (!params.isEmpty()) {
            JsonArray jsonarray = new JsonArray(params.size());

            for (JsonElement jsonelement : params) {
                jsonarray.add(jsonelement);
            }

            jsonobject.add("params", jsonarray);
        }

        return jsonobject;
    }

    public static JsonObject createError(JsonElement requestId, String message, int code, @Nullable String data) {
        JsonObject jsonobject = new JsonObject();
        jsonobject.addProperty("jsonrpc", "2.0");
        jsonobject.add("id", requestId);
        JsonObject jsonobject1 = new JsonObject();
        jsonobject1.addProperty("code", code);
        jsonobject1.addProperty("message", message);
        if (data != null && !data.isBlank()) {
            jsonobject1.addProperty("data", data);
        }

        jsonobject.add("error", jsonobject1);
        return jsonobject;
    }

    public static @Nullable JsonElement getRequestId(JsonObject json) {
        return json.get("id");
    }

    public static @Nullable String getMethodName(JsonObject json) {
        return GsonHelper.getAsString(json, "method", null);
    }

    public static @Nullable JsonElement getParams(JsonObject json) {
        return json.get("params");
    }

    public static @Nullable JsonElement getResult(JsonObject json) {
        return json.get("result");
    }

    public static @Nullable JsonObject getError(JsonObject json) {
        return GsonHelper.getAsJsonObject(json, "error", null);
    }
}
