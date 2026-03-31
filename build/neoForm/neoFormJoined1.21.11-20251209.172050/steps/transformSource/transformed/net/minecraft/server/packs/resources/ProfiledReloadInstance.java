package net.minecraft.server.packs.resources;

import com.google.common.base.Stopwatch;
import com.mojang.logging.LogUtils;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import net.minecraft.util.Unit;
import net.minecraft.util.Util;
import net.minecraft.util.profiling.Profiler;
import net.minecraft.util.profiling.ProfilerFiller;
import org.slf4j.Logger;

public class ProfiledReloadInstance extends SimpleReloadInstance<ProfiledReloadInstance.State> {
    private static final Logger LOGGER = LogUtils.getLogger();
    private final Stopwatch total = Stopwatch.createUnstarted();

    public static ReloadInstance of(
        ResourceManager resourceManager, List<PreparableReloadListener> listeners, Executor backgroundExecutor, Executor gameExecutor, CompletableFuture<Unit> alsoWaitedFor
    ) {
        ProfiledReloadInstance profiledreloadinstance = new ProfiledReloadInstance(listeners);
        profiledreloadinstance.startTasks(
            backgroundExecutor,
            gameExecutor,
            resourceManager,
            listeners,
            (p_432487_, p_432488_, p_432489_, p_432490_, p_432491_) -> {
                AtomicLong atomiclong = new AtomicLong();
                AtomicLong atomiclong1 = new AtomicLong();
                AtomicLong atomiclong2 = new AtomicLong();
                AtomicLong atomiclong3 = new AtomicLong();
                CompletableFuture<Void> completablefuture = p_432489_.reload(
                    p_432487_,
                    profiledExecutor(p_432490_, atomiclong, atomiclong1, p_432489_.getName()),
                    p_432488_,
                    profiledExecutor(p_432491_, atomiclong2, atomiclong3, p_432489_.getName())
                );
                return completablefuture.thenApplyAsync(p_404215_ -> {
                    LOGGER.debug("Finished reloading {}", p_432489_.getName());
                    return new ProfiledReloadInstance.State(p_432489_.getName(), atomiclong, atomiclong1, atomiclong2, atomiclong3);
                }, gameExecutor);
            },
            alsoWaitedFor
        );
        return profiledreloadinstance;
    }

    private ProfiledReloadInstance(List<PreparableReloadListener> listeners) {
        super(listeners);
        this.total.start();
    }

    @Override
    protected CompletableFuture<List<ProfiledReloadInstance.State>> prepareTasks(
        Executor p_405318_,
        Executor p_405642_,
        ResourceManager p_404932_,
        List<PreparableReloadListener> p_404797_,
        SimpleReloadInstance.StateFactory<ProfiledReloadInstance.State> p_405213_,
        CompletableFuture<?> p_405774_
    ) {
        return super.prepareTasks(p_405318_, p_405642_, p_404932_, p_404797_, p_405213_, p_405774_).thenApplyAsync(this::finish, p_405642_);
    }

    private static Executor profiledExecutor(Executor executor, AtomicLong timeTaken, AtomicLong timesRun, String name) {
        return p_404205_ -> executor.execute(() -> {
            ProfilerFiller profilerfiller = Profiler.get();
            profilerfiller.push(name);
            long i = Util.getNanos();
            p_404205_.run();
            timeTaken.addAndGet(Util.getNanos() - i);
            timesRun.incrementAndGet();
            profilerfiller.pop();
        });
    }

    private List<ProfiledReloadInstance.State> finish(List<ProfiledReloadInstance.State> dataPoints) {
        this.total.stop();
        long i = 0L;
        LOGGER.info("Resource reload finished after {} ms", this.total.elapsed(TimeUnit.MILLISECONDS));

        for (ProfiledReloadInstance.State profiledreloadinstance$state : dataPoints) {
            long j = TimeUnit.NANOSECONDS.toMillis(profiledreloadinstance$state.preparationNanos.get());
            long k = profiledreloadinstance$state.preparationCount.get();
            long l = TimeUnit.NANOSECONDS.toMillis(profiledreloadinstance$state.reloadNanos.get());
            long i1 = profiledreloadinstance$state.reloadCount.get();
            long j1 = j + l;
            long k1 = k + i1;
            String s = profiledreloadinstance$state.name;
            LOGGER.info("{} took approximately {} tasks/{} ms ({} tasks/{} ms preparing, {} tasks/{} ms applying)", s, k1, j1, k, j, i1, l);
            i += l;
        }

        LOGGER.info("Total blocking time: {} ms", i);
        return dataPoints;
    }

    public record State(String name, AtomicLong preparationNanos, AtomicLong preparationCount, AtomicLong reloadNanos, AtomicLong reloadCount) {
    }
}
