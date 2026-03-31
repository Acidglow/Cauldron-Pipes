package net.minecraft.world.entity.projectile;

import com.mojang.datafixers.util.Either;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.arrow.AbstractArrow;
import net.minecraft.world.item.ArrowItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.AttackRange;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

public final class ProjectileUtil {
    public static final float DEFAULT_ENTITY_HIT_RESULT_MARGIN = 0.3F;

    public static HitResult getHitResultOnMoveVector(Entity projectile, Predicate<Entity> filter) {
        Vec3 vec3 = projectile.getDeltaMovement();
        Level level = projectile.level();
        Vec3 vec31 = projectile.position();
        return getHitResult(vec31, projectile, filter, vec3, level, computeMargin(projectile), ClipContext.Block.COLLIDER);
    }

    public static Either<BlockHitResult, Collection<EntityHitResult>> getHitEntitiesAlong(
        Entity entity, AttackRange attackRange, Predicate<Entity> predicate, ClipContext.Block clipContext
    ) {
        Vec3 vec3 = entity.getHeadLookAngle();
        Vec3 vec31 = entity.getEyePosition();
        Vec3 vec32 = vec31.add(vec3.scale(attackRange.effectiveMinRange(entity)));
        double d0 = entity.getKnownMovement().dot(vec3);
        Vec3 vec33 = vec31.add(vec3.scale(attackRange.effectiveMaxRange(entity) + Math.max(0.0, d0)));
        return getHitEntitiesAlong(entity, vec31, vec32, predicate, vec33, attackRange.hitboxMargin(), clipContext);
    }

    public static HitResult getHitResultOnMoveVector(Entity projectile, Predicate<Entity> filter, ClipContext.Block clipContext) {
        Vec3 vec3 = projectile.getDeltaMovement();
        Level level = projectile.level();
        Vec3 vec31 = projectile.position();
        return getHitResult(vec31, projectile, filter, vec3, level, computeMargin(projectile), clipContext);
    }

    public static HitResult getHitResultOnViewVector(Entity projectile, Predicate<Entity> filter, double scale) {
        Vec3 vec3 = projectile.getViewVector(0.0F).scale(scale);
        Level level = projectile.level();
        Vec3 vec31 = projectile.getEyePosition();
        return getHitResult(vec31, projectile, filter, vec3, level, 0.0F, ClipContext.Block.COLLIDER);
    }

    private static HitResult getHitResult(
        Vec3 pos, Entity projectile, Predicate<Entity> filter, Vec3 deltaMovement, Level level, float margin, ClipContext.Block clipContext
    ) {
        Vec3 vec3 = pos.add(deltaMovement);
        HitResult hitresult = level.clipIncludingBorder(new ClipContext(pos, vec3, clipContext, ClipContext.Fluid.NONE, projectile));
        if (hitresult.getType() != HitResult.Type.MISS) {
            vec3 = hitresult.getLocation();
        }

        HitResult hitresult1 = getEntityHitResult(
            level, projectile, pos, vec3, projectile.getBoundingBox().expandTowards(deltaMovement).inflate(1.0), filter, margin
        );
        if (hitresult1 != null) {
            hitresult = hitresult1;
        }

        return hitresult;
    }

    private static Either<BlockHitResult, Collection<EntityHitResult>> getHitEntitiesAlong(
        Entity entity, Vec3 pos, Vec3 start, Predicate<Entity> predicate, Vec3 end, float hitboxMargin, ClipContext.Block clipContext
    ) {
        Level level = entity.level();
        BlockHitResult blockhitresult = level.clipIncludingBorder(new ClipContext(pos, end, clipContext, ClipContext.Fluid.NONE, entity));
        if (blockhitresult.getType() != HitResult.Type.MISS) {
            end = blockhitresult.getLocation();
            if (pos.distanceToSqr(end) < pos.distanceToSqr(start)) {
                return Either.left(blockhitresult);
            }
        }

        AABB aabb = AABB.ofSize(start, hitboxMargin, hitboxMargin, hitboxMargin).expandTowards(end.subtract(start)).inflate(1.0);
        Collection<EntityHitResult> collection = getManyEntityHitResult(level, entity, start, end, aabb, predicate, hitboxMargin, clipContext, true);
        return !collection.isEmpty() ? Either.right(collection) : Either.left(blockhitresult);
    }

    /**
     * Gets the {@link EntityHitResult} representing the entity hit
     */
    public static @Nullable EntityHitResult getEntityHitResult(
        Entity shooter, Vec3 startVec, Vec3 endVec, AABB boundingBox, Predicate<Entity> filter, double distance
    ) {
        Level level = shooter.level();
        double d0 = distance;
        Entity entity = null;
        Vec3 vec3 = null;

        for (Entity entity1 : level.getEntities(shooter, boundingBox, filter)) {
            AABB aabb = entity1.getBoundingBox().inflate(entity1.getPickRadius());
            Optional<Vec3> optional = aabb.clip(startVec, endVec);
            if (aabb.contains(startVec)) {
                if (d0 >= 0.0) {
                    entity = entity1;
                    vec3 = optional.orElse(startVec);
                    d0 = 0.0;
                }
            } else if (optional.isPresent()) {
                Vec3 vec31 = optional.get();
                double d1 = startVec.distanceToSqr(vec31);
                if (d1 < d0 || d0 == 0.0) {
                    if (entity1.getRootVehicle() == shooter.getRootVehicle() && !entity1.canRiderInteract()) {
                        if (d0 == 0.0) {
                            entity = entity1;
                            vec3 = vec31;
                        }
                    } else {
                        entity = entity1;
                        vec3 = vec31;
                        d0 = d1;
                    }
                }
            }
        }

        return entity == null ? null : new EntityHitResult(entity, vec3);
    }

    public static @Nullable EntityHitResult getEntityHitResult(
        Level level, Projectile projectile, Vec3 startVec, Vec3 endVec, AABB boundingBox, Predicate<Entity> filter
    ) {
        return getEntityHitResult(level, projectile, startVec, endVec, boundingBox, filter, computeMargin(projectile));
    }

    public static float computeMargin(Entity entity) {
        return Math.max(0.0F, Math.min(0.3F, (entity.tickCount - 2) / 20.0F));
    }

    /**
     * Gets the EntityHitResult representing the entity hit
     */
    public static @Nullable EntityHitResult getEntityHitResult(
        Level level, Entity projectile, Vec3 startVec, Vec3 endVec, AABB boundingBox, Predicate<Entity> filter, float inflationAmount
    ) {
        double d0 = Double.MAX_VALUE;
        Optional<Vec3> optional = Optional.empty();
        Entity entity = null;

        for (Entity entity1 : level.getEntities(projectile, boundingBox, filter)) {
            AABB aabb = entity1.getBoundingBox().inflate(inflationAmount);
            Optional<Vec3> optional1 = aabb.clip(startVec, endVec);
            if (optional1.isPresent()) {
                double d1 = startVec.distanceToSqr(optional1.get());
                if (d1 < d0) {
                    entity = entity1;
                    d0 = d1;
                    optional = optional1;
                }
            }
        }

        return entity == null ? null : new EntityHitResult(entity, optional.get());
    }

    public static Collection<EntityHitResult> getManyEntityHitResult(
        Level level, Entity entity, Vec3 start, Vec3 end, AABB box, Predicate<Entity> predicate, boolean hitIfInside
    ) {
        return getManyEntityHitResult(
            level, entity, start, end, box, predicate, computeMargin(entity), ClipContext.Block.COLLIDER, hitIfInside
        );
    }

    public static Collection<EntityHitResult> getManyEntityHitResult(
        Level level,
        Entity p_entity,
        Vec3 start,
        Vec3 end,
        AABB box,
        Predicate<Entity> predicate,
        float margin,
        ClipContext.Block clipContext,
        boolean hitIfInside
    ) {
        List<EntityHitResult> list = new ArrayList<>();

        for (Entity entity : level.getEntities(p_entity, box, predicate)) {
            AABB aabb = entity.getBoundingBox();
            if (hitIfInside && aabb.contains(start)) {
                list.add(new EntityHitResult(entity, start));
            } else {
                Optional<Vec3> optional = aabb.clip(start, end);
                if (optional.isPresent()) {
                    list.add(new EntityHitResult(entity, optional.get()));
                } else if (!(margin <= 0.0)) {
                    Optional<Vec3> optional1 = aabb.inflate(margin).clip(start, end);
                    if (!optional1.isEmpty()) {
                        Vec3 vec3 = optional1.get();
                        Vec3 vec31 = aabb.getCenter();
                        BlockHitResult blockhitresult = level.clipIncludingBorder(
                            new ClipContext(vec3, vec31, clipContext, ClipContext.Fluid.NONE, p_entity)
                        );
                        if (blockhitresult.getType() != HitResult.Type.MISS) {
                            vec31 = blockhitresult.getLocation();
                        }

                        Optional<Vec3> optional2 = entity.getBoundingBox().clip(vec3, vec31);
                        if (optional2.isPresent()) {
                            list.add(new EntityHitResult(entity, optional2.get()));
                        }
                    }
                }
            }
        }

        return list;
    }

    public static void rotateTowardsMovement(Entity projectile, float rotationSpeed) {
        Vec3 vec3 = projectile.getDeltaMovement();
        if (vec3.lengthSqr() != 0.0) {
            double d0 = vec3.horizontalDistance();
            projectile.setYRot((float)(Mth.atan2(vec3.z, vec3.x) * 180.0F / (float)Math.PI) + 90.0F);
            projectile.setXRot((float)(Mth.atan2(d0, vec3.y) * 180.0F / (float)Math.PI) - 90.0F);

            while (projectile.getXRot() - projectile.xRotO < -180.0F) {
                projectile.xRotO -= 360.0F;
            }

            while (projectile.getXRot() - projectile.xRotO >= 180.0F) {
                projectile.xRotO += 360.0F;
            }

            while (projectile.getYRot() - projectile.yRotO < -180.0F) {
                projectile.yRotO -= 360.0F;
            }

            while (projectile.getYRot() - projectile.yRotO >= 180.0F) {
                projectile.yRotO += 360.0F;
            }

            projectile.setXRot(Mth.lerp(rotationSpeed, projectile.xRotO, projectile.getXRot()));
            projectile.setYRot(Mth.lerp(rotationSpeed, projectile.yRotO, projectile.getYRot()));
        }
    }

    @Deprecated // Forge: Use the version below that takes in a Predicate<Item> instead of an Item
    public static InteractionHand getWeaponHoldingHand(LivingEntity shooter, Item weapon) {
        return shooter.getMainHandItem().is(weapon) ? InteractionHand.MAIN_HAND : InteractionHand.OFF_HAND;
    }

    public static InteractionHand getWeaponHoldingHand(LivingEntity livingEntity, Predicate<Item> itemPredicate) {
        return itemPredicate.test(livingEntity.getMainHandItem().getItem()) ? InteractionHand.MAIN_HAND : InteractionHand.OFF_HAND;
    }

    public static AbstractArrow getMobArrow(LivingEntity owner, ItemStack pickupItemStack, float velocity, @Nullable ItemStack weapon) {
        ArrowItem arrowitem = (ArrowItem)(pickupItemStack.getItem() instanceof ArrowItem ? pickupItemStack.getItem() : Items.ARROW);
        AbstractArrow abstractarrow = arrowitem.createArrow(owner.level(), pickupItemStack, owner, weapon);
        abstractarrow.setBaseDamageFromMob(velocity);
        return abstractarrow;
    }
}
