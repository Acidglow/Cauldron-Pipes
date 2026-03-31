package net.minecraft.server.level.progress;

import net.minecraft.resources.ResourceKey;
import net.minecraft.util.Mth;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;

public class LevelLoadProgressTracker implements LevelLoadListener {
    private static final int PREPARE_SERVER_WEIGHT = 10;
    private static final int EXPECTED_PLAYER_CHUNKS = Mth.square(7);
    private final boolean includePlayerChunks;
    private int totalWeight;
    private int finalizedWeight;
    private int segmentWeight;
    private float segmentFraction;
    private volatile float progress;

    public LevelLoadProgressTracker(boolean includePlayerChunk) {
        this.includePlayerChunks = includePlayerChunk;
    }

    @Override
    public void start(LevelLoadListener.Stage p_435224_, int p_434461_) {
        if (this.tracksStage(p_435224_)) {
            switch (p_435224_) {
                case LOAD_INITIAL_CHUNKS:
                    int i = this.includePlayerChunks ? EXPECTED_PLAYER_CHUNKS : 0;
                    this.totalWeight = 10 + p_434461_ + i;
                    this.beginSegment(10);
                    this.finishSegment();
                    this.beginSegment(p_434461_);
                    break;
                case LOAD_PLAYER_CHUNKS:
                    this.beginSegment(EXPECTED_PLAYER_CHUNKS);
            }
        }
    }

    private void beginSegment(int weight) {
        this.segmentWeight = weight;
        this.segmentFraction = 0.0F;
        this.updateProgress();
    }

    @Override
    public void update(LevelLoadListener.Stage p_433767_, int p_433665_, int p_434737_) {
        if (this.tracksStage(p_433767_)) {
            this.segmentFraction = p_434737_ == 0 ? 0.0F : (float)p_433665_ / p_434737_;
            this.updateProgress();
        }
    }

    @Override
    public void finish(LevelLoadListener.Stage p_433121_) {
        if (this.tracksStage(p_433121_)) {
            this.finishSegment();
        }
    }

    private void finishSegment() {
        this.finalizedWeight = this.finalizedWeight + this.segmentWeight;
        this.segmentWeight = 0;
        this.updateProgress();
    }

    private boolean tracksStage(LevelLoadListener.Stage stage) {
        return switch (stage) {
            case LOAD_INITIAL_CHUNKS -> true;
            case LOAD_PLAYER_CHUNKS -> this.includePlayerChunks;
            default -> false;
        };
    }

    private void updateProgress() {
        if (this.totalWeight == 0) {
            this.progress = 0.0F;
        } else {
            float f = this.finalizedWeight + this.segmentFraction * this.segmentWeight;
            this.progress = f / this.totalWeight;
        }
    }

    public float get() {
        return this.progress;
    }

    @Override
    public void updateFocus(ResourceKey<Level> p_434050_, ChunkPos p_433386_) {
    }
}
