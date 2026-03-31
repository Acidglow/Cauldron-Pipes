package net.minecraft.world;

public record Stopwatch(long creationTime, long accumulatedElapsedTime) {
    public Stopwatch(long p_455889_) {
        this(p_455889_, 0L);
    }

    public long elapsedMilliseconds(long currentTime) {
        long i = currentTime - this.creationTime;
        return this.accumulatedElapsedTime + i;
    }

    public double elapsedSeconds(long currentTime) {
        return this.elapsedMilliseconds(currentTime) / 1000.0;
    }
}
