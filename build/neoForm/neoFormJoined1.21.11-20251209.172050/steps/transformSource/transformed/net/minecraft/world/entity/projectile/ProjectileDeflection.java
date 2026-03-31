package net.minecraft.world.entity.projectile;

import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

@FunctionalInterface
public interface ProjectileDeflection {
    ProjectileDeflection NONE = (p_320379_, p_320626_, p_320122_) -> {};
    ProjectileDeflection REVERSE = (p_466614_, p_466615_, p_466616_) -> {
        float f = 170.0F + p_466616_.nextFloat() * 20.0F;
        p_466614_.setDeltaMovement(p_466614_.getDeltaMovement().scale(-0.5));
        p_466614_.setYRot(p_466614_.getYRot() + f);
        p_466614_.yRotO += f;
        p_466614_.needsSync = true;
    };
    ProjectileDeflection AIM_DEFLECT = (p_466611_, p_466612_, p_466613_) -> {
        if (p_466612_ != null) {
            Vec3 vec3 = p_466612_.getLookAngle();
            p_466611_.setDeltaMovement(vec3);
            p_466611_.needsSync = true;
        }
    };
    ProjectileDeflection MOMENTUM_DEFLECT = (p_466617_, p_466618_, p_466619_) -> {
        if (p_466618_ != null) {
            Vec3 vec3 = p_466618_.getDeltaMovement().normalize();
            p_466617_.setDeltaMovement(vec3);
            p_466617_.needsSync = true;
        }
    };

    void deflect(Projectile projectile, @Nullable Entity entity, RandomSource random);
}
