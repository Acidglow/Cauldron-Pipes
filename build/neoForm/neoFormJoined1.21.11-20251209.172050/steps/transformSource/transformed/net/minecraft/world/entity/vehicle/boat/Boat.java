package net.minecraft.world.entity.vehicle.boat;

import java.util.function.Supplier;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;

public class Boat extends AbstractBoat {
    public Boat(EntityType<? extends Boat> p_478759_, Level p_478717_, Supplier<Item> p_481824_) {
        super(p_478759_, p_478717_, p_481824_);
    }

    @Override
    protected double rideHeight(EntityDimensions p_478934_) {
        return p_478934_.height() / 3.0F;
    }
}
