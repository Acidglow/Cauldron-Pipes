package net.minecraft.world.entity.decoration.painting;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponentGetter;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundAddEntityPacket;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerEntity;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.PaintingVariantTags;
import net.minecraft.util.Util;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.decoration.HangingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.variant.VariantUtils;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.gamerules.GameRules;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

public class Painting extends HangingEntity {
    private static final EntityDataAccessor<Holder<PaintingVariant>> DATA_PAINTING_VARIANT_ID = SynchedEntityData.defineId(
        Painting.class, EntityDataSerializers.PAINTING_VARIANT
    );
    public static final float DEPTH = 0.0625F;

    public Painting(EntityType<? extends Painting> p_477920_, Level p_480591_) {
        super(p_477920_, p_480591_);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder p_480117_) {
        super.defineSynchedData(p_480117_);
        p_480117_.define(DATA_PAINTING_VARIANT_ID, VariantUtils.getAny(this.registryAccess(), Registries.PAINTING_VARIANT));
    }

    @Override
    public void onSyncedDataUpdated(EntityDataAccessor<?> p_478654_) {
        super.onSyncedDataUpdated(p_478654_);
        if (DATA_PAINTING_VARIANT_ID.equals(p_478654_)) {
            this.recalculateBoundingBox();
        }
    }

    private void setVariant(Holder<PaintingVariant> variant) {
        this.entityData.set(DATA_PAINTING_VARIANT_ID, variant);
    }

    public Holder<PaintingVariant> getVariant() {
        return this.entityData.get(DATA_PAINTING_VARIANT_ID);
    }

    @Override
    public <T> @Nullable T get(DataComponentType<? extends T> p_479414_) {
        return p_479414_ == DataComponents.PAINTING_VARIANT ? castComponentValue((DataComponentType<T>)p_479414_, this.getVariant()) : super.get(p_479414_);
    }

    @Override
    protected void applyImplicitComponents(DataComponentGetter p_482017_) {
        this.applyImplicitComponentIfPresent(p_482017_, DataComponents.PAINTING_VARIANT);
        super.applyImplicitComponents(p_482017_);
    }

    @Override
    protected <T> boolean applyImplicitComponent(DataComponentType<T> p_482074_, T p_478555_) {
        if (p_482074_ == DataComponents.PAINTING_VARIANT) {
            this.setVariant(castComponentValue(DataComponents.PAINTING_VARIANT, p_478555_));
            return true;
        } else {
            return super.applyImplicitComponent(p_482074_, p_478555_);
        }
    }

    public static Optional<Painting> create(Level level, BlockPos pos, Direction direction) {
        Painting painting = new Painting(level, pos);
        List<Holder<PaintingVariant>> list = new ArrayList<>();
        level.registryAccess().lookupOrThrow(Registries.PAINTING_VARIANT).getTagOrEmpty(PaintingVariantTags.PLACEABLE).forEach(list::add);
        if (list.isEmpty()) {
            return Optional.empty();
        } else {
            painting.setDirection(direction);
            list.removeIf(p_480355_ -> {
                painting.setVariant((Holder<PaintingVariant>)p_480355_);
                return !painting.survives();
            });
            if (list.isEmpty()) {
                return Optional.empty();
            } else {
                int i = list.stream().mapToInt(Painting::variantArea).max().orElse(0);
                list.removeIf(p_480727_ -> variantArea((Holder<PaintingVariant>)p_480727_) < i);
                Optional<Holder<PaintingVariant>> optional = Util.getRandomSafe(list, painting.random);
                if (optional.isEmpty()) {
                    return Optional.empty();
                } else {
                    painting.setVariant(optional.get());
                    painting.setDirection(direction);
                    return Optional.of(painting);
                }
            }
        }
    }

    private static int variantArea(Holder<PaintingVariant> variant) {
        return variant.value().area();
    }

    private Painting(Level level, BlockPos pos) {
        super(EntityType.PAINTING, level, pos);
    }

    public Painting(Level level, BlockPos pos, Direction direction, Holder<PaintingVariant> variant) {
        this(level, pos);
        this.setVariant(variant);
        this.setDirection(direction);
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput p_478166_) {
        p_478166_.store("facing", Direction.LEGACY_ID_CODEC_2D, this.getDirection());
        super.addAdditionalSaveData(p_478166_);
        VariantUtils.writeVariant(p_478166_, this.getVariant());
    }

    @Override
    protected void readAdditionalSaveData(ValueInput p_480277_) {
        Direction direction = p_480277_.read("facing", Direction.LEGACY_ID_CODEC_2D).orElse(Direction.SOUTH);
        super.readAdditionalSaveData(p_480277_);
        this.setDirection(direction);
        VariantUtils.readVariant(p_480277_, Registries.PAINTING_VARIANT).ifPresent(this::setVariant);
    }

    @Override
    protected AABB calculateBoundingBox(BlockPos p_478171_, Direction p_480432_) {
        float f = 0.46875F;
        Vec3 vec3 = Vec3.atCenterOf(p_478171_).relative(p_480432_, -0.46875);
        PaintingVariant paintingvariant = this.getVariant().value();
        double d0 = this.offsetForPaintingSize(paintingvariant.width());
        double d1 = this.offsetForPaintingSize(paintingvariant.height());
        Direction direction = p_480432_.getCounterClockWise();
        Vec3 vec31 = vec3.relative(direction, d0).relative(Direction.UP, d1);
        Direction.Axis direction$axis = p_480432_.getAxis();
        double d2 = direction$axis == Direction.Axis.X ? 0.0625 : paintingvariant.width();
        double d3 = paintingvariant.height();
        double d4 = direction$axis == Direction.Axis.Z ? 0.0625 : paintingvariant.width();
        return AABB.ofSize(vec31, d2, d3, d4);
    }

    private double offsetForPaintingSize(int size) {
        return size % 2 == 0 ? 0.5 : 0.0;
    }

    @Override
    public void dropItem(ServerLevel p_481337_, @Nullable Entity p_480824_) {
        if (p_481337_.getGameRules().get(GameRules.ENTITY_DROPS)) {
            this.playSound(SoundEvents.PAINTING_BREAK, 1.0F, 1.0F);
            if (!(p_480824_ instanceof Player player && player.hasInfiniteMaterials())) {
                this.spawnAtLocation(p_481337_, Items.PAINTING);
            }
        }
    }

    @Override
    public void playPlacementSound() {
        this.playSound(SoundEvents.PAINTING_PLACE, 1.0F, 1.0F);
    }

    @Override
    public void snapTo(double p_480845_, double p_479059_, double p_479924_, float p_481472_, float p_477916_) {
        this.setPos(p_480845_, p_479059_, p_479924_);
    }

    @Override
    public Vec3 trackingPosition() {
        return Vec3.atLowerCornerOf(this.pos);
    }

    @Override
    public Packet<ClientGamePacketListener> getAddEntityPacket(ServerEntity p_482150_) {
        return new ClientboundAddEntityPacket(this, this.getDirection().get3DDataValue(), this.getPos());
    }

    @Override
    public void recreateFromPacket(ClientboundAddEntityPacket p_480178_) {
        super.recreateFromPacket(p_480178_);
        this.setDirection(Direction.from3DDataValue(p_480178_.getData()));
    }

    @Override
    public ItemStack getPickResult() {
        return new ItemStack(Items.PAINTING);
    }
}
