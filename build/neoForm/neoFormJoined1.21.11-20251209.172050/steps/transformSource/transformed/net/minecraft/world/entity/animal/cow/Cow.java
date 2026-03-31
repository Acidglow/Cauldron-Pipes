package net.minecraft.world.entity.animal.cow;

import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponentGetter;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.variant.SpawnContext;
import net.minecraft.world.entity.variant.VariantUtils;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jspecify.annotations.Nullable;

public class Cow extends AbstractCow {
    private static final EntityDataAccessor<Holder<CowVariant>> DATA_VARIANT_ID = SynchedEntityData.defineId(Cow.class, EntityDataSerializers.COW_VARIANT);

    public Cow(EntityType<? extends Cow> p_480651_, Level p_482005_) {
        super(p_480651_, p_482005_);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder p_478475_) {
        super.defineSynchedData(p_478475_);
        p_478475_.define(DATA_VARIANT_ID, VariantUtils.getDefaultOrAny(this.registryAccess(), CowVariants.TEMPERATE));
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput p_477974_) {
        super.addAdditionalSaveData(p_477974_);
        VariantUtils.writeVariant(p_477974_, this.getVariant());
    }

    @Override
    protected void readAdditionalSaveData(ValueInput p_478434_) {
        super.readAdditionalSaveData(p_478434_);
        VariantUtils.readVariant(p_478434_, Registries.COW_VARIANT).ifPresent(this::setVariant);
    }

    public @Nullable Cow getBreedOffspring(ServerLevel p_481452_, AgeableMob p_480169_) {
        Cow cow = EntityType.COW.create(p_481452_, EntitySpawnReason.BREEDING);
        if (cow != null && p_480169_ instanceof Cow cow1) {
            cow.setVariant(this.random.nextBoolean() ? this.getVariant() : cow1.getVariant());
        }

        return cow;
    }

    @Override
    public SpawnGroupData finalizeSpawn(
        ServerLevelAccessor p_479111_, DifficultyInstance p_480094_, EntitySpawnReason p_481877_, @Nullable SpawnGroupData p_481471_
    ) {
        VariantUtils.selectVariantToSpawn(SpawnContext.create(p_479111_, this.blockPosition()), Registries.COW_VARIANT).ifPresent(this::setVariant);
        return super.finalizeSpawn(p_479111_, p_480094_, p_481877_, p_481471_);
    }

    public void setVariant(Holder<CowVariant> variant) {
        this.entityData.set(DATA_VARIANT_ID, variant);
    }

    public Holder<CowVariant> getVariant() {
        return this.entityData.get(DATA_VARIANT_ID);
    }

    @Override
    public <T> @Nullable T get(DataComponentType<? extends T> p_479308_) {
        return p_479308_ == DataComponents.COW_VARIANT ? castComponentValue((DataComponentType<T>)p_479308_, this.getVariant()) : super.get(p_479308_);
    }

    @Override
    protected void applyImplicitComponents(DataComponentGetter p_480529_) {
        this.applyImplicitComponentIfPresent(p_480529_, DataComponents.COW_VARIANT);
        super.applyImplicitComponents(p_480529_);
    }

    @Override
    protected <T> boolean applyImplicitComponent(DataComponentType<T> p_478238_, T p_481396_) {
        if (p_478238_ == DataComponents.COW_VARIANT) {
            this.setVariant(castComponentValue(DataComponents.COW_VARIANT, p_481396_));
            return true;
        } else {
            return super.applyImplicitComponent(p_478238_, p_481396_);
        }
    }
}
