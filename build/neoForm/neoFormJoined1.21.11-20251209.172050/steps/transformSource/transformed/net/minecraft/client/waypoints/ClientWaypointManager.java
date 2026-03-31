package net.minecraft.client.waypoints;

import com.mojang.datafixers.util.Either;
import java.util.Comparator;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.waypoints.TrackedWaypoint;
import net.minecraft.world.waypoints.TrackedWaypointManager;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class ClientWaypointManager implements TrackedWaypointManager {
    private final Map<Either<UUID, String>, TrackedWaypoint> waypoints = new ConcurrentHashMap<>();

    public void trackWaypoint(TrackedWaypoint p_416480_) {
        this.waypoints.put(p_416480_.id(), p_416480_);
    }

    public void updateWaypoint(TrackedWaypoint p_416549_) {
        this.waypoints.get(p_416549_.id()).update(p_416549_);
    }

    public void untrackWaypoint(TrackedWaypoint p_416593_) {
        this.waypoints.remove(p_416593_.id());
    }

    public boolean hasWaypoints() {
        return !this.waypoints.isEmpty();
    }

    public void forEachWaypoint(Entity entity, Consumer<TrackedWaypoint> action) {
        this.waypoints
            .values()
            .stream()
            .sorted(Comparator.<TrackedWaypoint>comparingDouble(p_415538_ -> p_415538_.distanceSquared(entity)).reversed())
            .forEachOrdered(action);
    }
}
