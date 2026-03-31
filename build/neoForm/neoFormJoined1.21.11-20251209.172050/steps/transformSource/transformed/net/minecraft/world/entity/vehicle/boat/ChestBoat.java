package net.minecraft.world.entity.vehicle.boat;

import java.util.function.Supplier;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;

public class ChestBoat extends AbstractChestBoat {
    public ChestBoat(EntityType<? extends ChestBoat> p_480561_, Level p_478636_, Supplier<Item> p_480311_) {
        super(p_480561_, p_478636_, p_480311_);
    }

    @Override
    protected double rideHeight(EntityDimensions p_478308_) {
        return p_478308_.height() / 3.0F;
    }
}
