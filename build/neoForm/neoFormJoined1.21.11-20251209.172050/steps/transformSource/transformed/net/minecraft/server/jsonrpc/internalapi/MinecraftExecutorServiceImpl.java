package net.minecraft.server.jsonrpc.internalapi;

import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;
import net.minecraft.server.dedicated.DedicatedServer;

public class MinecraftExecutorServiceImpl implements MinecraftExecutorService {
    private final DedicatedServer server;

    public MinecraftExecutorServiceImpl(DedicatedServer server) {
        this.server = server;
    }

    @Override
    public <V> CompletableFuture<V> submit(Supplier<V> p_449344_) {
        return this.server.submit(p_449344_);
    }

    @Override
    public CompletableFuture<Void> submit(Runnable p_449818_) {
        return this.server.submit(p_449818_);
    }
}
