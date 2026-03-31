package net.minecraft.server.level;

import net.minecraft.world.level.TicketStorage;

class LoadingChunkTracker extends ChunkTracker {
    private static final int MAX_LEVEL = ChunkLevel.MAX_LEVEL + 1;
    private final DistanceManager distanceManager;
    private final TicketStorage ticketStorage;

    public LoadingChunkTracker(DistanceManager distanceManager, TicketStorage ticketStorage) {
        super(MAX_LEVEL + 1, 16, 256);
        this.distanceManager = distanceManager;
        this.ticketStorage = ticketStorage;
        ticketStorage.setLoadingChunkUpdatedListener(this::update);
    }

    @Override
    protected int getLevelFromSource(long p_394127_) {
        return this.ticketStorage.getTicketLevelAt(p_394127_, false);
    }

    @Override
    protected int getLevel(long p_394558_) {
        if (!this.distanceManager.isChunkToRemove(p_394558_)) {
            ChunkHolder chunkholder = this.distanceManager.getChunk(p_394558_);
            if (chunkholder != null) {
                return chunkholder.getTicketLevel();
            }
        }

        return MAX_LEVEL;
    }

    @Override
    protected void setLevel(long p_394407_, int p_393818_) {
        ChunkHolder chunkholder = this.distanceManager.getChunk(p_394407_);
        int i = chunkholder == null ? MAX_LEVEL : chunkholder.getTicketLevel();
        if (i != p_393818_) {
            chunkholder = this.distanceManager.updateChunkScheduling(p_394407_, p_393818_, chunkholder, i);
            if (chunkholder != null) {
                this.distanceManager.chunksToUpdateFutures.add(chunkholder);
            }
        }
    }

    public int runDistanceUpdates(int toUpdateCount) {
        return this.runUpdates(toUpdateCount);
    }
}
