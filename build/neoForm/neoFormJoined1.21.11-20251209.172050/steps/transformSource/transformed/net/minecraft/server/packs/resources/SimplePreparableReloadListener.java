package net.minecraft.server.packs.resources;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import net.minecraft.util.profiling.Profiler;
import net.minecraft.util.profiling.ProfilerFiller;

public abstract class SimplePreparableReloadListener<T> extends net.neoforged.neoforge.resource.ContextAwareReloadListener implements PreparableReloadListener {
    @Override
    public final CompletableFuture<Void> reload(
        PreparableReloadListener.SharedState p_434245_, Executor p_10784_, PreparableReloadListener.PreparationBarrier p_10780_, Executor p_10785_
    ) {
        ResourceManager resourcemanager = p_434245_.resourceManager();
        return CompletableFuture.<T>supplyAsync(() -> this.prepare(resourcemanager, Profiler.get()), p_10784_)
            .thenCompose(p_10780_::wait)
            .thenAcceptAsync(p_372693_ -> this.apply((T)p_372693_, resourcemanager, Profiler.get()), p_10785_);
    }

    /**
     * Performs any reloading that can be done off-thread, such as file IO
     */
    protected abstract T prepare(ResourceManager resourceManager, ProfilerFiller profiler);

    protected abstract void apply(T object, ResourceManager resourceManager, ProfilerFiller profiler);
}
