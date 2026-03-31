package net.minecraft.world.entity.vehicle.boat;

import java.util.function.Supplier;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;

public class ChestRaft extends AbstractChestBoat {
    public ChestRaft(EntityType<? extends ChestRaft> p_481570_, Level p_481082_, Supplier<Item> p_479889_) {
        super(p_481570_, p_481082_, p_479889_);
    }

    @Override
    protected double rideHeight(EntityDimensions p_479881_) {
        return p_479881_.height() * 0.8888889F;
    }
}
