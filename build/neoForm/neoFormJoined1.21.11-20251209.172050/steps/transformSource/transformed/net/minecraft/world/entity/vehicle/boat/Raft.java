package net.minecraft.world.entity.vehicle.boat;

import java.util.function.Supplier;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;

public class Raft extends AbstractBoat {
    public Raft(EntityType<? extends Raft> p_481058_, Level p_480525_, Supplier<Item> p_479753_) {
        super(p_481058_, p_480525_, p_479753_);
    }

    @Override
    protected double rideHeight(EntityDimensions p_478705_) {
        return p_478705_.height() * 0.8888889F;
    }
}
