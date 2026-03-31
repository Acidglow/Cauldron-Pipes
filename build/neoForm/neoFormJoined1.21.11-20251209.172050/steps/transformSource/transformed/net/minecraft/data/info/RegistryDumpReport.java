package net.minecraft.data.info;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.nio.file.Path;
import java.util.concurrent.CompletableFuture;
import net.minecraft.core.DefaultedRegistry;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.data.CachedOutput;
import net.minecraft.data.DataProvider;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.Identifier;

public class RegistryDumpReport implements DataProvider {
    private final PackOutput output;

    public RegistryDumpReport(PackOutput output) {
        this.output = output;
    }

    @Override
    public CompletableFuture<?> run(CachedOutput p_253743_) {
        JsonObject jsonobject = new JsonObject();
        BuiltInRegistries.REGISTRY
            .listElements()
            .forEach(p_466008_ -> jsonobject.add(p_466008_.key().identifier().toString(), dumpRegistry((Registry<?>)p_466008_.value())));
        Path path = this.output.getOutputFolder(PackOutput.Target.REPORTS).resolve("registries.json");
        return DataProvider.saveStable(p_253743_, jsonobject, path);
    }

    private static <T> JsonElement dumpRegistry(Registry<T> registry) {
        JsonObject jsonobject = new JsonObject();
        if (registry instanceof DefaultedRegistry) {
            Identifier identifier = ((DefaultedRegistry)registry).getDefaultKey();
            jsonobject.addProperty("default", identifier.toString());
        }

        int i = ((Registry<Registry<T>>)BuiltInRegistries.REGISTRY).getId(registry);
        jsonobject.addProperty("protocol_id", i);
        JsonObject jsonobject1 = new JsonObject();
        registry.listElements().forEach(p_466006_ -> {
            T t = p_466006_.value();
            int j = registry.getId(t);
            JsonObject jsonobject2 = new JsonObject();
            jsonobject2.addProperty("protocol_id", j);
            jsonobject1.add(p_466006_.key().identifier().toString(), jsonobject2);
        });
        jsonobject.add("entries", jsonobject1);
        return jsonobject;
    }

    @Override
    public final String getName() {
        return "Registry Dump";
    }
}
