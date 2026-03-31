package net.minecraft.world.entity.animal.fish;

import io.netty.buffer.ByteBuf;
import java.util.function.IntFunction;
import net.minecraft.core.component.DataComponentGetter;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.ByIdMap;
import net.minecraft.util.StringRepresentable;
import net.minecraft.util.random.WeightedList;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.animal.Bucketable;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jspecify.annotations.Nullable;

public class Salmon extends AbstractSchoolingFish {
    private static final String TAG_TYPE = "type";
    private static final EntityDataAccessor<Integer> DATA_TYPE = SynchedEntityData.defineId(Salmon.class, EntityDataSerializers.INT);

    public Salmon(EntityType<? extends Salmon> p_477987_, Level p_478175_) {
        super(p_477987_, p_478175_);
        this.refreshDimensions();
    }

    @Override
    public int getMaxSchoolSize() {
        return 5;
    }

    @Override
    public ItemStack getBucketItemStack() {
        return new ItemStack(Items.SALMON_BUCKET);
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return SoundEvents.SALMON_AMBIENT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.SALMON_DEATH;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource damageSource) {
        return SoundEvents.SALMON_HURT;
    }

    @Override
    protected SoundEvent getFlopSound() {
        return SoundEvents.SALMON_FLOP;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder p_479259_) {
        super.defineSynchedData(p_479259_);
        p_479259_.define(DATA_TYPE, Salmon.Variant.DEFAULT.id());
    }

    @Override
    public void onSyncedDataUpdated(EntityDataAccessor<?> p_480162_) {
        super.onSyncedDataUpdated(p_480162_);
        if (DATA_TYPE.equals(p_480162_)) {
            this.refreshDimensions();
        }
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput p_479698_) {
        super.addAdditionalSaveData(p_479698_);
        p_479698_.store("type", Salmon.Variant.CODEC, this.getVariant());
    }

    @Override
    protected void readAdditionalSaveData(ValueInput p_482177_) {
        super.readAdditionalSaveData(p_482177_);
        this.setVariant(p_482177_.read("type", Salmon.Variant.CODEC).orElse(Salmon.Variant.DEFAULT));
    }

    @Override
    public void saveToBucketTag(ItemStack p_479109_) {
        Bucketable.saveDefaultDataToBucketTag(this, p_479109_);
        p_479109_.copyFrom(DataComponents.SALMON_SIZE, this);
    }

    private void setVariant(Salmon.Variant variant) {
        this.entityData.set(DATA_TYPE, variant.id);
    }

    public Salmon.Variant getVariant() {
        return Salmon.Variant.BY_ID.apply(this.entityData.get(DATA_TYPE));
    }

    @Override
    public <T> @Nullable T get(DataComponentType<? extends T> p_480611_) {
        return p_480611_ == DataComponents.SALMON_SIZE ? castComponentValue((DataComponentType<T>)p_480611_, this.getVariant()) : super.get(p_480611_);
    }

    @Override
    protected void applyImplicitComponents(DataComponentGetter p_479244_) {
        this.applyImplicitComponentIfPresent(p_479244_, DataComponents.SALMON_SIZE);
        super.applyImplicitComponents(p_479244_);
    }

    @Override
    protected <T> boolean applyImplicitComponent(DataComponentType<T> p_478272_, T p_478809_) {
        if (p_478272_ == DataComponents.SALMON_SIZE) {
            this.setVariant(castComponentValue(DataComponents.SALMON_SIZE, p_478809_));
            return true;
        } else {
            return super.applyImplicitComponent(p_478272_, p_478809_);
        }
    }

    @Override
    public @Nullable SpawnGroupData finalizeSpawn(
        ServerLevelAccessor p_479696_, DifficultyInstance p_479293_, EntitySpawnReason p_478583_, @Nullable SpawnGroupData p_481140_
    ) {
        WeightedList.Builder<Salmon.Variant> builder = WeightedList.builder();
        builder.add(Salmon.Variant.SMALL, 30);
        builder.add(Salmon.Variant.MEDIUM, 50);
        builder.add(Salmon.Variant.LARGE, 15);
        builder.build().getRandom(this.random).ifPresent(this::setVariant);
        return super.finalizeSpawn(p_479696_, p_479293_, p_478583_, p_481140_);
    }

    public float getSalmonScale() {
        return this.getVariant().boundingBoxScale;
    }

    @Override
    protected EntityDimensions getDefaultDimensions(Pose p_480682_) {
        return super.getDefaultDimensions(p_480682_).scale(this.getSalmonScale());
    }

    public static enum Variant implements StringRepresentable {
        SMALL("small", 0, 0.5F),
        MEDIUM("medium", 1, 1.0F),
        LARGE("large", 2, 1.5F);

        public static final Salmon.Variant DEFAULT = MEDIUM;
        public static final StringRepresentable.EnumCodec<Salmon.Variant> CODEC = StringRepresentable.fromEnum(Salmon.Variant::values);
        static final IntFunction<Salmon.Variant> BY_ID = ByIdMap.continuous(Salmon.Variant::id, values(), ByIdMap.OutOfBoundsStrategy.CLAMP);
        public static final StreamCodec<ByteBuf, Salmon.Variant> STREAM_CODEC = ByteBufCodecs.idMapper(BY_ID, Salmon.Variant::id);
        private final String name;
        final int id;
        final float boundingBoxScale;

        private Variant(String name, int id, float boundingBoxScale) {
            this.name = name;
            this.id = id;
            this.boundingBoxScale = boundingBoxScale;
        }

        @Override
        public String getSerializedName() {
            return this.name;
        }

        int id() {
            return this.id;
        }
    }
}
