package net.minecraft.world.level.levelgen.feature;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.mojang.logging.LogUtils;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntMaps;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import java.util.Locale;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;
import org.apache.commons.lang3.mutable.MutableInt;
import org.slf4j.Logger;

public class FeatureCountTracker {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final LoadingCache<ServerLevel, FeatureCountTracker.LevelData> data = CacheBuilder.newBuilder()
        .weakKeys()
        .expireAfterAccess(5L, TimeUnit.MINUTES)
        .build(new CacheLoader<ServerLevel, FeatureCountTracker.LevelData>() {
            public FeatureCountTracker.LevelData load(ServerLevel level) {
                return new FeatureCountTracker.LevelData(Object2IntMaps.synchronize(new Object2IntOpenHashMap<>()), new MutableInt(0));
            }
        });

    public static void chunkDecorated(ServerLevel level) {
        try {
            data.get(level).chunksWithFeatures().increment();
        } catch (Exception exception) {
            LOGGER.error("Failed to increment chunk count", (Throwable)exception);
        }
    }

    public static void featurePlaced(ServerLevel level, ConfiguredFeature<?, ?> feature, Optional<PlacedFeature> topFeature) {
        try {
            data.get(level)
                .featureData()
                .computeInt(new FeatureCountTracker.FeatureData(feature, topFeature), (p_190891_, p_190892_) -> p_190892_ == null ? 1 : p_190892_ + 1);
        } catch (Exception exception) {
            LOGGER.error("Failed to increment feature count", (Throwable)exception);
        }
    }

    public static void clearCounts() {
        data.invalidateAll();
        LOGGER.debug("Cleared feature counts");
    }

    public static void logCounts() {
        LOGGER.debug("Logging feature counts:");
        data.asMap()
            .forEach(
                (p_466718_, p_466719_) -> {
                    String s = p_466718_.dimension().identifier().toString();
                    boolean flag = p_466718_.getServer().isRunning();
                    Registry<PlacedFeature> registry = p_466718_.registryAccess().lookupOrThrow(Registries.PLACED_FEATURE);
                    String s1 = (flag ? "running" : "dead") + " " + s;
                    int i = p_466719_.chunksWithFeatures().intValue();
                    LOGGER.debug("{} total_chunks: {}", s1, i);
                    p_466719_.featureData()
                        .forEach(
                            (p_460586_, p_460587_) -> LOGGER.debug(
                                "{} {} {} {} {} {}",
                                s1,
                                String.format(Locale.ROOT, "%10d", p_460587_),
                                String.format(Locale.ROOT, "%10f", (double)p_460587_ / i),
                                p_460586_.topFeature().flatMap(registry::getResourceKey).map(ResourceKey::identifier),
                                p_460586_.feature().feature(),
                                p_460586_.feature()
                            )
                        );
                }
            );
    }

    record FeatureData(ConfiguredFeature<?, ?> feature, Optional<PlacedFeature> topFeature) {
    }

    record LevelData(Object2IntMap<FeatureCountTracker.FeatureData> featureData, MutableInt chunksWithFeatures) {
    }
}
