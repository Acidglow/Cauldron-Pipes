package net.minecraft.server.level.progress;

import com.mojang.logging.LogUtils;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.Mth;
import net.minecraft.util.Util;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import org.slf4j.Logger;

public class LoggingLevelLoadListener implements LevelLoadListener {
    private static final Logger LOGGER = LogUtils.getLogger();
    private final boolean includePlayerChunks;
    private final LevelLoadProgressTracker progressTracker;
    private boolean closed;
    private long startTime = Long.MAX_VALUE;
    private long nextLogTime = Long.MAX_VALUE;

    public LoggingLevelLoadListener(boolean includePlayerChunks) {
        this.includePlayerChunks = includePlayerChunks;
        this.progressTracker = new LevelLoadProgressTracker(includePlayerChunks);
    }

    public static LoggingLevelLoadListener forDedicatedServer() {
        return new LoggingLevelLoadListener(false);
    }

    public static LoggingLevelLoadListener forSingleplayer() {
        return new LoggingLevelLoadListener(true);
    }

    @Override
    public void start(LevelLoadListener.Stage p_433882_, int p_435368_) {
        if (!this.closed) {
            if (this.startTime == Long.MAX_VALUE) {
                long i = Util.getMillis();
                this.startTime = i;
                this.nextLogTime = i;
            }

            this.progressTracker.start(p_433882_, p_435368_);
            switch (p_433882_) {
                case PREPARE_GLOBAL_SPAWN:
                    LOGGER.info("Selecting global world spawn...");
                    break;
                case LOAD_INITIAL_CHUNKS:
                    LOGGER.info("Loading {} persistent chunks...", p_435368_);
                    break;
                case LOAD_PLAYER_CHUNKS:
                    LOGGER.info("Loading {} chunks for player spawn...", p_435368_);
            }
        }
    }

    @Override
    public void update(LevelLoadListener.Stage p_435570_, int p_433524_, int p_433140_) {
        if (!this.closed) {
            this.progressTracker.update(p_435570_, p_433524_, p_433140_);
            if (Util.getMillis() > this.nextLogTime) {
                this.nextLogTime += 500L;
                int i = Mth.floor(this.progressTracker.get() * 100.0F);
                LOGGER.info(Component.translatable("menu.preparingSpawn", i).getString());
            }
        }
    }

    @Override
    public void finish(LevelLoadListener.Stage p_434371_) {
        if (!this.closed) {
            this.progressTracker.finish(p_434371_);
            LevelLoadListener.Stage levelloadlistener$stage = this.includePlayerChunks
                ? LevelLoadListener.Stage.LOAD_PLAYER_CHUNKS
                : LevelLoadListener.Stage.LOAD_INITIAL_CHUNKS;
            if (p_434371_ == levelloadlistener$stage) {
                LOGGER.info("Time elapsed: {} ms", Util.getMillis() - this.startTime);
                this.nextLogTime = Long.MAX_VALUE;
                this.closed = true;
            }
        }
    }

    @Override
    public void updateFocus(ResourceKey<Level> p_435548_, ChunkPos p_435471_) {
    }
}
