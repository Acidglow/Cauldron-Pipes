package net.minecraft.world.entity;

import com.mojang.logging.LogUtils;
import com.mojang.math.Transformation;
import com.mojang.serialization.Codec;
import it.unimi.dsi.fastutil.ints.IntSet;
import java.util.List;
import java.util.Optional;
import java.util.function.IntFunction;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.network.chat.ComponentUtils;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.permissions.LevelBasedPermissionSet;
import net.minecraft.util.ARGB;
import net.minecraft.util.Brightness;
import net.minecraft.util.ByIdMap;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.PushReaction;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;
import org.joml.Quaternionf;
import org.joml.Quaternionfc;
import org.joml.Vector3f;
import org.joml.Vector3fc;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

public abstract class Display extends Entity {
    static final Logger LOGGER = LogUtils.getLogger();
    public static final int NO_BRIGHTNESS_OVERRIDE = -1;
    private static final EntityDataAccessor<Integer> DATA_TRANSFORMATION_INTERPOLATION_START_DELTA_TICKS_ID = SynchedEntityData.defineId(
        Display.class, EntityDataSerializers.INT
    );
    private static final EntityDataAccessor<Integer> DATA_TRANSFORMATION_INTERPOLATION_DURATION_ID = SynchedEntityData.defineId(
        Display.class, EntityDataSerializers.INT
    );
    private static final EntityDataAccessor<Integer> DATA_POS_ROT_INTERPOLATION_DURATION_ID = SynchedEntityData.defineId(
        Display.class, EntityDataSerializers.INT
    );
    private static final EntityDataAccessor<Vector3fc> DATA_TRANSLATION_ID = SynchedEntityData.defineId(Display.class, EntityDataSerializers.VECTOR3);
    private static final EntityDataAccessor<Vector3fc> DATA_SCALE_ID = SynchedEntityData.defineId(Display.class, EntityDataSerializers.VECTOR3);
    private static final EntityDataAccessor<Quaternionfc> DATA_LEFT_ROTATION_ID = SynchedEntityData.defineId(Display.class, EntityDataSerializers.QUATERNION);
    private static final EntityDataAccessor<Quaternionfc> DATA_RIGHT_ROTATION_ID = SynchedEntityData.defineId(Display.class, EntityDataSerializers.QUATERNION);
    private static final EntityDataAccessor<Byte> DATA_BILLBOARD_RENDER_CONSTRAINTS_ID = SynchedEntityData.defineId(Display.class, EntityDataSerializers.BYTE);
    private static final EntityDataAccessor<Integer> DATA_BRIGHTNESS_OVERRIDE_ID = SynchedEntityData.defineId(Display.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Float> DATA_VIEW_RANGE_ID = SynchedEntityData.defineId(Display.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> DATA_SHADOW_RADIUS_ID = SynchedEntityData.defineId(Display.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> DATA_SHADOW_STRENGTH_ID = SynchedEntityData.defineId(Display.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> DATA_WIDTH_ID = SynchedEntityData.defineId(Display.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> DATA_HEIGHT_ID = SynchedEntityData.defineId(Display.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Integer> DATA_GLOW_COLOR_OVERRIDE_ID = SynchedEntityData.defineId(Display.class, EntityDataSerializers.INT);
    private static final IntSet RENDER_STATE_IDS = IntSet.of(
        DATA_TRANSLATION_ID.id(),
        DATA_SCALE_ID.id(),
        DATA_LEFT_ROTATION_ID.id(),
        DATA_RIGHT_ROTATION_ID.id(),
        DATA_BILLBOARD_RENDER_CONSTRAINTS_ID.id(),
        DATA_BRIGHTNESS_OVERRIDE_ID.id(),
        DATA_SHADOW_RADIUS_ID.id(),
        DATA_SHADOW_STRENGTH_ID.id()
    );
    private static final int INITIAL_TRANSFORMATION_INTERPOLATION_DURATION = 0;
    private static final int INITIAL_TRANSFORMATION_START_INTERPOLATION = 0;
    private static final int INITIAL_POS_ROT_INTERPOLATION_DURATION = 0;
    private static final float INITIAL_SHADOW_RADIUS = 0.0F;
    private static final float INITIAL_SHADOW_STRENGTH = 1.0F;
    private static final float INITIAL_VIEW_RANGE = 1.0F;
    private static final float INITIAL_WIDTH = 0.0F;
    private static final float INITIAL_HEIGHT = 0.0F;
    private static final int NO_GLOW_COLOR_OVERRIDE = -1;
    public static final String TAG_POS_ROT_INTERPOLATION_DURATION = "teleport_duration";
    public static final String TAG_TRANSFORMATION_INTERPOLATION_DURATION = "interpolation_duration";
    public static final String TAG_TRANSFORMATION_START_INTERPOLATION = "start_interpolation";
    public static final String TAG_TRANSFORMATION = "transformation";
    public static final String TAG_BILLBOARD = "billboard";
    public static final String TAG_BRIGHTNESS = "brightness";
    public static final String TAG_VIEW_RANGE = "view_range";
    public static final String TAG_SHADOW_RADIUS = "shadow_radius";
    public static final String TAG_SHADOW_STRENGTH = "shadow_strength";
    public static final String TAG_WIDTH = "width";
    public static final String TAG_HEIGHT = "height";
    public static final String TAG_GLOW_COLOR_OVERRIDE = "glow_color_override";
    private long interpolationStartClientTick = -2147483648L;
    private int interpolationDuration;
    private float lastProgress;
    private AABB cullingBoundingBox;
    private boolean noCulling = true;
    protected boolean updateRenderState;
    private boolean updateStartTick;
    private boolean updateInterpolationDuration;
    private Display.@Nullable RenderState renderState;
    private final InterpolationHandler interpolation = new InterpolationHandler(this, 0);

    public Display(EntityType<?> p_270360_, Level p_270280_) {
        super(p_270360_, p_270280_);
        this.noPhysics = true;
        this.cullingBoundingBox = this.getBoundingBox();
    }

    @Override
    public void onSyncedDataUpdated(EntityDataAccessor<?> p_270275_) {
        super.onSyncedDataUpdated(p_270275_);
        if (DATA_HEIGHT_ID.equals(p_270275_) || DATA_WIDTH_ID.equals(p_270275_)) {
            this.updateCulling();
        }

        if (DATA_TRANSFORMATION_INTERPOLATION_START_DELTA_TICKS_ID.equals(p_270275_)) {
            this.updateStartTick = true;
        }

        if (DATA_POS_ROT_INTERPOLATION_DURATION_ID.equals(p_270275_)) {
            this.interpolation.setInterpolationLength(this.getPosRotInterpolationDuration());
        }

        if (DATA_TRANSFORMATION_INTERPOLATION_DURATION_ID.equals(p_270275_)) {
            this.updateInterpolationDuration = true;
        }

        if (RENDER_STATE_IDS.contains(p_270275_.id())) {
            this.updateRenderState = true;
        }
    }

    @Override
    public final boolean hurtServer(ServerLevel p_376368_, DamageSource p_376135_, float p_376332_) {
        return false;
    }

    private static Transformation createTransformation(SynchedEntityData synchedEntityData) {
        Vector3fc vector3fc = synchedEntityData.get(DATA_TRANSLATION_ID);
        Quaternionfc quaternionfc = synchedEntityData.get(DATA_LEFT_ROTATION_ID);
        Vector3fc vector3fc1 = synchedEntityData.get(DATA_SCALE_ID);
        Quaternionfc quaternionfc1 = synchedEntityData.get(DATA_RIGHT_ROTATION_ID);
        return new Transformation(vector3fc, quaternionfc, vector3fc1, quaternionfc1);
    }

    @Override
    public void tick() {
        Entity entity = this.getVehicle();
        if (entity != null && entity.isRemoved()) {
            this.stopRiding();
        }

        if (this.level().isClientSide()) {
            if (this.updateStartTick) {
                this.updateStartTick = false;
                int i = this.getTransformationInterpolationDelay();
                this.interpolationStartClientTick = this.tickCount + i;
            }

            if (this.updateInterpolationDuration) {
                this.updateInterpolationDuration = false;
                this.interpolationDuration = this.getTransformationInterpolationDuration();
            }

            if (this.updateRenderState) {
                this.updateRenderState = false;
                boolean flag = this.interpolationDuration != 0;
                if (flag && this.renderState != null) {
                    this.renderState = this.createInterpolatedRenderState(this.renderState, this.lastProgress);
                } else {
                    this.renderState = this.createFreshRenderState();
                }

                this.updateRenderSubState(flag, this.lastProgress);
            }

            this.interpolation.interpolate();
        }
    }

    @Override
    public InterpolationHandler getInterpolation() {
        return this.interpolation;
    }

    protected abstract void updateRenderSubState(boolean interpolate, float partialTick);

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder p_325924_) {
        p_325924_.define(DATA_POS_ROT_INTERPOLATION_DURATION_ID, 0);
        p_325924_.define(DATA_TRANSFORMATION_INTERPOLATION_START_DELTA_TICKS_ID, 0);
        p_325924_.define(DATA_TRANSFORMATION_INTERPOLATION_DURATION_ID, 0);
        p_325924_.define(DATA_TRANSLATION_ID, new Vector3f());
        p_325924_.define(DATA_SCALE_ID, new Vector3f(1.0F, 1.0F, 1.0F));
        p_325924_.define(DATA_RIGHT_ROTATION_ID, new Quaternionf());
        p_325924_.define(DATA_LEFT_ROTATION_ID, new Quaternionf());
        p_325924_.define(DATA_BILLBOARD_RENDER_CONSTRAINTS_ID, Display.BillboardConstraints.FIXED.getId());
        p_325924_.define(DATA_BRIGHTNESS_OVERRIDE_ID, -1);
        p_325924_.define(DATA_VIEW_RANGE_ID, 1.0F);
        p_325924_.define(DATA_SHADOW_RADIUS_ID, 0.0F);
        p_325924_.define(DATA_SHADOW_STRENGTH_ID, 1.0F);
        p_325924_.define(DATA_WIDTH_ID, 0.0F);
        p_325924_.define(DATA_HEIGHT_ID, 0.0F);
        p_325924_.define(DATA_GLOW_COLOR_OVERRIDE_ID, -1);
    }

    @Override
    protected void readAdditionalSaveData(ValueInput p_421584_) {
        this.setTransformation(p_421584_.read("transformation", Transformation.EXTENDED_CODEC).orElse(Transformation.identity()));
        this.setTransformationInterpolationDuration(p_421584_.getIntOr("interpolation_duration", 0));
        this.setTransformationInterpolationDelay(p_421584_.getIntOr("start_interpolation", 0));
        int i = p_421584_.getIntOr("teleport_duration", 0);
        this.setPosRotInterpolationDuration(Mth.clamp(i, 0, 59));
        this.setBillboardConstraints(p_421584_.read("billboard", Display.BillboardConstraints.CODEC).orElse(Display.BillboardConstraints.FIXED));
        this.setViewRange(p_421584_.getFloatOr("view_range", 1.0F));
        this.setShadowRadius(p_421584_.getFloatOr("shadow_radius", 0.0F));
        this.setShadowStrength(p_421584_.getFloatOr("shadow_strength", 1.0F));
        this.setWidth(p_421584_.getFloatOr("width", 0.0F));
        this.setHeight(p_421584_.getFloatOr("height", 0.0F));
        this.setGlowColorOverride(p_421584_.getIntOr("glow_color_override", -1));
        this.setBrightnessOverride(p_421584_.read("brightness", Brightness.CODEC).orElse(null));
    }

    private void setTransformation(Transformation transformation) {
        this.entityData.set(DATA_TRANSLATION_ID, transformation.getTranslation());
        this.entityData.set(DATA_LEFT_ROTATION_ID, transformation.getLeftRotation());
        this.entityData.set(DATA_SCALE_ID, transformation.getScale());
        this.entityData.set(DATA_RIGHT_ROTATION_ID, transformation.getRightRotation());
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput p_422542_) {
        p_422542_.store("transformation", Transformation.EXTENDED_CODEC, createTransformation(this.entityData));
        p_422542_.store("billboard", Display.BillboardConstraints.CODEC, this.getBillboardConstraints());
        p_422542_.putInt("interpolation_duration", this.getTransformationInterpolationDuration());
        p_422542_.putInt("teleport_duration", this.getPosRotInterpolationDuration());
        p_422542_.putFloat("view_range", this.getViewRange());
        p_422542_.putFloat("shadow_radius", this.getShadowRadius());
        p_422542_.putFloat("shadow_strength", this.getShadowStrength());
        p_422542_.putFloat("width", this.getWidth());
        p_422542_.putFloat("height", this.getHeight());
        p_422542_.putInt("glow_color_override", this.getGlowColorOverride());
        p_422542_.storeNullable("brightness", Brightness.CODEC, this.getBrightnessOverride());
    }

    public AABB getBoundingBoxForCulling() {
        return this.cullingBoundingBox;
    }

    public boolean affectedByCulling() {
        return !this.noCulling;
    }

    @Override
    public PushReaction getPistonPushReaction() {
        return PushReaction.IGNORE;
    }

    @Override
    public boolean isIgnoringBlockTriggers() {
        return true;
    }

    public Display.@Nullable RenderState renderState() {
        return this.renderState;
    }

    private void setTransformationInterpolationDuration(int transformationInterpolationDuration) {
        this.entityData.set(DATA_TRANSFORMATION_INTERPOLATION_DURATION_ID, transformationInterpolationDuration);
    }

    private int getTransformationInterpolationDuration() {
        return this.entityData.get(DATA_TRANSFORMATION_INTERPOLATION_DURATION_ID);
    }

    private void setTransformationInterpolationDelay(int transformationInterpolationDelay) {
        this.entityData.set(DATA_TRANSFORMATION_INTERPOLATION_START_DELTA_TICKS_ID, transformationInterpolationDelay, true);
    }

    private int getTransformationInterpolationDelay() {
        return this.entityData.get(DATA_TRANSFORMATION_INTERPOLATION_START_DELTA_TICKS_ID);
    }

    private void setPosRotInterpolationDuration(int posRotInterpolationDuration) {
        this.entityData.set(DATA_POS_ROT_INTERPOLATION_DURATION_ID, posRotInterpolationDuration);
    }

    private int getPosRotInterpolationDuration() {
        return this.entityData.get(DATA_POS_ROT_INTERPOLATION_DURATION_ID);
    }

    private void setBillboardConstraints(Display.BillboardConstraints billboardConstraints) {
        this.entityData.set(DATA_BILLBOARD_RENDER_CONSTRAINTS_ID, billboardConstraints.getId());
    }

    private Display.BillboardConstraints getBillboardConstraints() {
        return Display.BillboardConstraints.BY_ID.apply(this.entityData.get(DATA_BILLBOARD_RENDER_CONSTRAINTS_ID));
    }

    private void setBrightnessOverride(@Nullable Brightness brightnessOverride) {
        this.entityData.set(DATA_BRIGHTNESS_OVERRIDE_ID, brightnessOverride != null ? brightnessOverride.pack() : -1);
    }

    private @Nullable Brightness getBrightnessOverride() {
        int i = this.entityData.get(DATA_BRIGHTNESS_OVERRIDE_ID);
        return i != -1 ? Brightness.unpack(i) : null;
    }

    private int getPackedBrightnessOverride() {
        return this.entityData.get(DATA_BRIGHTNESS_OVERRIDE_ID);
    }

    private void setViewRange(float viewRange) {
        this.entityData.set(DATA_VIEW_RANGE_ID, viewRange);
    }

    private float getViewRange() {
        return this.entityData.get(DATA_VIEW_RANGE_ID);
    }

    private void setShadowRadius(float shadowRadius) {
        this.entityData.set(DATA_SHADOW_RADIUS_ID, shadowRadius);
    }

    private float getShadowRadius() {
        return this.entityData.get(DATA_SHADOW_RADIUS_ID);
    }

    private void setShadowStrength(float shadowStrength) {
        this.entityData.set(DATA_SHADOW_STRENGTH_ID, shadowStrength);
    }

    private float getShadowStrength() {
        return this.entityData.get(DATA_SHADOW_STRENGTH_ID);
    }

    private void setWidth(float width) {
        this.entityData.set(DATA_WIDTH_ID, width);
    }

    private float getWidth() {
        return this.entityData.get(DATA_WIDTH_ID);
    }

    private void setHeight(float height) {
        this.entityData.set(DATA_HEIGHT_ID, height);
    }

    private int getGlowColorOverride() {
        return this.entityData.get(DATA_GLOW_COLOR_OVERRIDE_ID);
    }

    private void setGlowColorOverride(int glowColorOverride) {
        this.entityData.set(DATA_GLOW_COLOR_OVERRIDE_ID, glowColorOverride);
    }

    public float calculateInterpolationProgress(float partialTick) {
        int i = this.interpolationDuration;
        if (i <= 0) {
            return 1.0F;
        } else {
            float f = (float)(this.tickCount - this.interpolationStartClientTick);
            float f1 = f + partialTick;
            float f2 = Mth.clamp(Mth.inverseLerp(f1, 0.0F, (float)i), 0.0F, 1.0F);
            this.lastProgress = f2;
            return f2;
        }
    }

    private float getHeight() {
        return this.entityData.get(DATA_HEIGHT_ID);
    }

    @Override
    public void setPos(double p_270091_, double p_270983_, double p_270419_) {
        super.setPos(p_270091_, p_270983_, p_270419_);
        this.updateCulling();
    }

    private void updateCulling() {
        float f = this.getWidth();
        float f1 = this.getHeight();
        this.noCulling = f == 0.0F || f1 == 0.0F;
        float f2 = f / 2.0F;
        double d0 = this.getX();
        double d1 = this.getY();
        double d2 = this.getZ();
        this.cullingBoundingBox = new AABB(d0 - f2, d1, d2 - f2, d0 + f2, d1 + f1, d2 + f2);
    }

    @Override
    public boolean shouldRenderAtSqrDistance(double p_270991_) {
        return p_270991_ < Mth.square(this.getViewRange() * 64.0 * getViewScale());
    }

    @Override
    public int getTeamColor() {
        int i = this.getGlowColorOverride();
        return i != -1 ? i : super.getTeamColor();
    }

    private Display.RenderState createFreshRenderState() {
        return new Display.RenderState(
            Display.GenericInterpolator.constant(createTransformation(this.entityData)),
            this.getBillboardConstraints(),
            this.getPackedBrightnessOverride(),
            Display.FloatInterpolator.constant(this.getShadowRadius()),
            Display.FloatInterpolator.constant(this.getShadowStrength()),
            this.getGlowColorOverride()
        );
    }

    private Display.RenderState createInterpolatedRenderState(Display.RenderState renderState, float partialTick) {
        Transformation transformation = renderState.transformation.get(partialTick);
        float f = renderState.shadowRadius.get(partialTick);
        float f1 = renderState.shadowStrength.get(partialTick);
        return new Display.RenderState(
            new Display.TransformationInterpolator(transformation, createTransformation(this.entityData)),
            this.getBillboardConstraints(),
            this.getPackedBrightnessOverride(),
            new Display.LinearFloatInterpolator(f, this.getShadowRadius()),
            new Display.LinearFloatInterpolator(f1, this.getShadowStrength()),
            this.getGlowColorOverride()
        );
    }

    public static enum BillboardConstraints implements StringRepresentable {
        FIXED((byte)0, "fixed"),
        VERTICAL((byte)1, "vertical"),
        HORIZONTAL((byte)2, "horizontal"),
        CENTER((byte)3, "center");

        public static final Codec<Display.BillboardConstraints> CODEC = StringRepresentable.fromEnum(Display.BillboardConstraints::values);
        public static final IntFunction<Display.BillboardConstraints> BY_ID = ByIdMap.continuous(
            Display.BillboardConstraints::getId, values(), ByIdMap.OutOfBoundsStrategy.ZERO
        );
        private final byte id;
        private final String name;

        private BillboardConstraints(byte id, String name) {
            this.name = name;
            this.id = id;
        }

        @Override
        public String getSerializedName() {
            return this.name;
        }

        byte getId() {
            return this.id;
        }
    }

    public static class BlockDisplay extends Display {
        public static final String TAG_BLOCK_STATE = "block_state";
        private static final EntityDataAccessor<BlockState> DATA_BLOCK_STATE_ID = SynchedEntityData.defineId(
            Display.BlockDisplay.class, EntityDataSerializers.BLOCK_STATE
        );
        private Display.BlockDisplay.@Nullable BlockRenderState blockRenderState;

        public BlockDisplay(EntityType<?> p_271022_, Level p_270442_) {
            super(p_271022_, p_270442_);
        }

        @Override
        protected void defineSynchedData(SynchedEntityData.Builder p_326452_) {
            super.defineSynchedData(p_326452_);
            p_326452_.define(DATA_BLOCK_STATE_ID, Blocks.AIR.defaultBlockState());
        }

        @Override
        public void onSyncedDataUpdated(EntityDataAccessor<?> p_277476_) {
            super.onSyncedDataUpdated(p_277476_);
            if (p_277476_.equals(DATA_BLOCK_STATE_ID)) {
                this.updateRenderState = true;
            }
        }

        private BlockState getBlockState() {
            return this.entityData.get(DATA_BLOCK_STATE_ID);
        }

        private void setBlockState(BlockState blockState) {
            this.entityData.set(DATA_BLOCK_STATE_ID, blockState);
        }

        @Override
        protected void readAdditionalSaveData(ValueInput p_422259_) {
            super.readAdditionalSaveData(p_422259_);
            this.setBlockState(p_422259_.read("block_state", BlockState.CODEC).orElse(Blocks.AIR.defaultBlockState()));
        }

        @Override
        protected void addAdditionalSaveData(ValueOutput p_422670_) {
            super.addAdditionalSaveData(p_422670_);
            p_422670_.store("block_state", BlockState.CODEC, this.getBlockState());
        }

        public Display.BlockDisplay.@Nullable BlockRenderState blockRenderState() {
            return this.blockRenderState;
        }

        @Override
        protected void updateRenderSubState(boolean p_277802_, float p_277688_) {
            this.blockRenderState = new Display.BlockDisplay.BlockRenderState(this.getBlockState());
        }

        public record BlockRenderState(BlockState blockState) {
        }
    }

    record ColorInterpolator(int previous, int current) implements Display.IntInterpolator {
        @Override
        public int get(float p_278012_) {
            return ARGB.srgbLerp(p_278012_, this.previous, this.current);
        }
    }

    @FunctionalInterface
    public interface FloatInterpolator {
        static Display.FloatInterpolator constant(float value) {
            return p_278040_ -> value;
        }

        float get(float partialTick);
    }

    @FunctionalInterface
    public interface GenericInterpolator<T> {
        static <T> Display.GenericInterpolator<T> constant(T value) {
            return p_277907_ -> value;
        }

        T get(float partialTick);
    }

    @FunctionalInterface
    public interface IntInterpolator {
        static Display.IntInterpolator constant(int value) {
            return p_277356_ -> value;
        }

        int get(float partialTick);
    }

    public static class ItemDisplay extends Display {
        private static final String TAG_ITEM = "item";
        private static final String TAG_ITEM_DISPLAY = "item_display";
        private static final EntityDataAccessor<ItemStack> DATA_ITEM_STACK_ID = SynchedEntityData.defineId(
            Display.ItemDisplay.class, EntityDataSerializers.ITEM_STACK
        );
        private static final EntityDataAccessor<Byte> DATA_ITEM_DISPLAY_ID = SynchedEntityData.defineId(Display.ItemDisplay.class, EntityDataSerializers.BYTE);
        private final SlotAccess slot = SlotAccess.of(this::getItemStack, this::setItemStack);
        private Display.ItemDisplay.@Nullable ItemRenderState itemRenderState;

        public ItemDisplay(EntityType<?> p_270104_, Level p_270735_) {
            super(p_270104_, p_270735_);
        }

        @Override
        protected void defineSynchedData(SynchedEntityData.Builder p_325946_) {
            super.defineSynchedData(p_325946_);
            p_325946_.define(DATA_ITEM_STACK_ID, ItemStack.EMPTY);
            p_325946_.define(DATA_ITEM_DISPLAY_ID, ItemDisplayContext.NONE.getId());
        }

        @Override
        public void onSyncedDataUpdated(EntityDataAccessor<?> p_277793_) {
            super.onSyncedDataUpdated(p_277793_);
            if (DATA_ITEM_STACK_ID.equals(p_277793_) || DATA_ITEM_DISPLAY_ID.equals(p_277793_)) {
                this.updateRenderState = true;
            }
        }

        private ItemStack getItemStack() {
            return this.entityData.get(DATA_ITEM_STACK_ID);
        }

        private void setItemStack(ItemStack itemStack) {
            this.entityData.set(DATA_ITEM_STACK_ID, itemStack);
        }

        private void setItemTransform(ItemDisplayContext itemTransform) {
            this.entityData.set(DATA_ITEM_DISPLAY_ID, itemTransform.getId());
        }

        private ItemDisplayContext getItemTransform() {
            return ItemDisplayContext.BY_ID.apply(this.entityData.get(DATA_ITEM_DISPLAY_ID));
        }

        @Override
        protected void readAdditionalSaveData(ValueInput p_421758_) {
            super.readAdditionalSaveData(p_421758_);
            this.setItemStack(p_421758_.read("item", ItemStack.CODEC).orElse(ItemStack.EMPTY));
            this.setItemTransform(p_421758_.read("item_display", ItemDisplayContext.CODEC).orElse(ItemDisplayContext.NONE));
        }

        @Override
        protected void addAdditionalSaveData(ValueOutput p_422130_) {
            super.addAdditionalSaveData(p_422130_);
            ItemStack itemstack = this.getItemStack();
            if (!itemstack.isEmpty()) {
                p_422130_.store("item", ItemStack.CODEC, itemstack);
            }

            p_422130_.store("item_display", ItemDisplayContext.CODEC, this.getItemTransform());
        }

        @Override
        public @Nullable SlotAccess getSlot(int p_270599_) {
            return p_270599_ == 0 ? this.slot : null;
        }

        public Display.ItemDisplay.@Nullable ItemRenderState itemRenderState() {
            return this.itemRenderState;
        }

        @Override
        protected void updateRenderSubState(boolean p_277976_, float p_277708_) {
            ItemStack itemstack = this.getItemStack();
            itemstack.setEntityRepresentation(this);
            this.itemRenderState = new Display.ItemDisplay.ItemRenderState(itemstack, this.getItemTransform());
        }

        public record ItemRenderState(ItemStack itemStack, ItemDisplayContext itemTransform) {
        }
    }

    record LinearFloatInterpolator(float previous, float current) implements Display.FloatInterpolator {
        @Override
        public float get(float p_277511_) {
            return Mth.lerp(p_277511_, this.previous, this.current);
        }
    }

    record LinearIntInterpolator(int previous, int current) implements Display.IntInterpolator {
        @Override
        public int get(float p_277960_) {
            return Mth.lerpInt(p_277960_, this.previous, this.current);
        }
    }

    public record RenderState(
        Display.GenericInterpolator<Transformation> transformation,
        Display.BillboardConstraints billboardConstraints,
        int brightnessOverride,
        Display.FloatInterpolator shadowRadius,
        Display.FloatInterpolator shadowStrength,
        int glowColorOverride
    ) {
    }

    public static class TextDisplay extends Display {
        public static final String TAG_TEXT = "text";
        private static final String TAG_LINE_WIDTH = "line_width";
        private static final String TAG_TEXT_OPACITY = "text_opacity";
        private static final String TAG_BACKGROUND_COLOR = "background";
        private static final String TAG_SHADOW = "shadow";
        private static final String TAG_SEE_THROUGH = "see_through";
        private static final String TAG_USE_DEFAULT_BACKGROUND = "default_background";
        private static final String TAG_ALIGNMENT = "alignment";
        public static final byte FLAG_SHADOW = 1;
        public static final byte FLAG_SEE_THROUGH = 2;
        public static final byte FLAG_USE_DEFAULT_BACKGROUND = 4;
        public static final byte FLAG_ALIGN_LEFT = 8;
        public static final byte FLAG_ALIGN_RIGHT = 16;
        private static final byte INITIAL_TEXT_OPACITY = -1;
        public static final int INITIAL_BACKGROUND = 1073741824;
        private static final int INITIAL_LINE_WIDTH = 200;
        private static final EntityDataAccessor<Component> DATA_TEXT_ID = SynchedEntityData.defineId(Display.TextDisplay.class, EntityDataSerializers.COMPONENT);
        private static final EntityDataAccessor<Integer> DATA_LINE_WIDTH_ID = SynchedEntityData.defineId(Display.TextDisplay.class, EntityDataSerializers.INT);
        private static final EntityDataAccessor<Integer> DATA_BACKGROUND_COLOR_ID = SynchedEntityData.defineId(
            Display.TextDisplay.class, EntityDataSerializers.INT
        );
        private static final EntityDataAccessor<Byte> DATA_TEXT_OPACITY_ID = SynchedEntityData.defineId(Display.TextDisplay.class, EntityDataSerializers.BYTE);
        private static final EntityDataAccessor<Byte> DATA_STYLE_FLAGS_ID = SynchedEntityData.defineId(Display.TextDisplay.class, EntityDataSerializers.BYTE);
        private static final IntSet TEXT_RENDER_STATE_IDS = IntSet.of(
            DATA_TEXT_ID.id(), DATA_LINE_WIDTH_ID.id(), DATA_BACKGROUND_COLOR_ID.id(), DATA_TEXT_OPACITY_ID.id(), DATA_STYLE_FLAGS_ID.id()
        );
        private Display.TextDisplay.@Nullable CachedInfo clientDisplayCache;
        private Display.TextDisplay.@Nullable TextRenderState textRenderState;

        public TextDisplay(EntityType<?> p_270708_, Level p_270736_) {
            super(p_270708_, p_270736_);
        }

        @Override
        protected void defineSynchedData(SynchedEntityData.Builder p_326450_) {
            super.defineSynchedData(p_326450_);
            p_326450_.define(DATA_TEXT_ID, Component.empty());
            p_326450_.define(DATA_LINE_WIDTH_ID, 200);
            p_326450_.define(DATA_BACKGROUND_COLOR_ID, 1073741824);
            p_326450_.define(DATA_TEXT_OPACITY_ID, (byte)-1);
            p_326450_.define(DATA_STYLE_FLAGS_ID, (byte)0);
        }

        @Override
        public void onSyncedDataUpdated(EntityDataAccessor<?> p_270797_) {
            super.onSyncedDataUpdated(p_270797_);
            if (TEXT_RENDER_STATE_IDS.contains(p_270797_.id())) {
                this.updateRenderState = true;
            }
        }

        private Component getText() {
            return this.entityData.get(DATA_TEXT_ID);
        }

        private void setText(Component text) {
            this.entityData.set(DATA_TEXT_ID, text);
        }

        private int getLineWidth() {
            return this.entityData.get(DATA_LINE_WIDTH_ID);
        }

        private void setLineWidth(int lineWidth) {
            this.entityData.set(DATA_LINE_WIDTH_ID, lineWidth);
        }

        private byte getTextOpacity() {
            return this.entityData.get(DATA_TEXT_OPACITY_ID);
        }

        private void setTextOpacity(byte textOpacity) {
            this.entityData.set(DATA_TEXT_OPACITY_ID, textOpacity);
        }

        private int getBackgroundColor() {
            return this.entityData.get(DATA_BACKGROUND_COLOR_ID);
        }

        private void setBackgroundColor(int backgroundColor) {
            this.entityData.set(DATA_BACKGROUND_COLOR_ID, backgroundColor);
        }

        private byte getFlags() {
            return this.entityData.get(DATA_STYLE_FLAGS_ID);
        }

        private void setFlags(byte flags) {
            this.entityData.set(DATA_STYLE_FLAGS_ID, flags);
        }

        private static byte loadFlag(byte flags, ValueInput input, String key, byte mask) {
            return input.getBooleanOr(key, false) ? (byte)(flags | mask) : flags;
        }

        @Override
        protected void readAdditionalSaveData(ValueInput p_422293_) {
            super.readAdditionalSaveData(p_422293_);
            this.setLineWidth(p_422293_.getIntOr("line_width", 200));
            this.setTextOpacity(p_422293_.getByteOr("text_opacity", (byte)-1));
            this.setBackgroundColor(p_422293_.getIntOr("background", 1073741824));
            byte b0 = loadFlag((byte)0, p_422293_, "shadow", (byte)1);
            b0 = loadFlag(b0, p_422293_, "see_through", (byte)2);
            b0 = loadFlag(b0, p_422293_, "default_background", (byte)4);
            Optional<Display.TextDisplay.Align> optional = p_422293_.read("alignment", Display.TextDisplay.Align.CODEC);
            if (optional.isPresent()) {
                b0 = switch ((Display.TextDisplay.Align)optional.get()) {
                    case CENTER -> b0;
                    case LEFT -> (byte)(b0 | 8);
                    case RIGHT -> (byte)(b0 | 16);
                };
            }

            this.setFlags(b0);
            Optional<Component> optional1 = p_422293_.read("text", ComponentSerialization.CODEC);
            if (optional1.isPresent()) {
                try {
                    if (this.level() instanceof ServerLevel serverlevel) {
                        CommandSourceStack commandsourcestack = this.createCommandSourceStackForNameResolution(serverlevel)
                            .withPermission(LevelBasedPermissionSet.GAMEMASTER);
                        Component component = ComponentUtils.updateForEntity(commandsourcestack, optional1.get(), this, 0);
                        this.setText(component);
                    } else {
                        this.setText(Component.empty());
                    }
                } catch (Exception exception) {
                    Display.LOGGER.warn("Failed to parse display entity text {}", optional1, exception);
                }
            }
        }

        private static void storeFlag(byte flags, ValueOutput output, String key, byte name) {
            output.putBoolean(key, (flags & name) != 0);
        }

        @Override
        protected void addAdditionalSaveData(ValueOutput p_421726_) {
            super.addAdditionalSaveData(p_421726_);
            p_421726_.store("text", ComponentSerialization.CODEC, this.getText());
            p_421726_.putInt("line_width", this.getLineWidth());
            p_421726_.putInt("background", this.getBackgroundColor());
            p_421726_.putByte("text_opacity", this.getTextOpacity());
            byte b0 = this.getFlags();
            storeFlag(b0, p_421726_, "shadow", (byte)1);
            storeFlag(b0, p_421726_, "see_through", (byte)2);
            storeFlag(b0, p_421726_, "default_background", (byte)4);
            p_421726_.store("alignment", Display.TextDisplay.Align.CODEC, getAlign(b0));
        }

        @Override
        protected void updateRenderSubState(boolean p_277565_, float p_277967_) {
            if (p_277565_ && this.textRenderState != null) {
                this.textRenderState = this.createInterpolatedTextRenderState(this.textRenderState, p_277967_);
            } else {
                this.textRenderState = this.createFreshTextRenderState();
            }

            this.clientDisplayCache = null;
        }

        public Display.TextDisplay.@Nullable TextRenderState textRenderState() {
            return this.textRenderState;
        }

        private Display.TextDisplay.TextRenderState createFreshTextRenderState() {
            return new Display.TextDisplay.TextRenderState(
                this.getText(),
                this.getLineWidth(),
                Display.IntInterpolator.constant(this.getTextOpacity()),
                Display.IntInterpolator.constant(this.getBackgroundColor()),
                this.getFlags()
            );
        }

        private Display.TextDisplay.TextRenderState createInterpolatedTextRenderState(Display.TextDisplay.TextRenderState renderState, float partialTick) {
            int i = renderState.backgroundColor.get(partialTick);
            int j = renderState.textOpacity.get(partialTick);
            return new Display.TextDisplay.TextRenderState(
                this.getText(),
                this.getLineWidth(),
                new Display.LinearIntInterpolator(j, this.getTextOpacity()),
                new Display.ColorInterpolator(i, this.getBackgroundColor()),
                this.getFlags()
            );
        }

        public Display.TextDisplay.CachedInfo cacheDisplay(Display.TextDisplay.LineSplitter splitter) {
            if (this.clientDisplayCache == null) {
                if (this.textRenderState != null) {
                    this.clientDisplayCache = splitter.split(this.textRenderState.text(), this.textRenderState.lineWidth());
                } else {
                    this.clientDisplayCache = new Display.TextDisplay.CachedInfo(List.of(), 0);
                }
            }

            return this.clientDisplayCache;
        }

        public static Display.TextDisplay.Align getAlign(byte flags) {
            if ((flags & 8) != 0) {
                return Display.TextDisplay.Align.LEFT;
            } else {
                return (flags & 16) != 0 ? Display.TextDisplay.Align.RIGHT : Display.TextDisplay.Align.CENTER;
            }
        }

        public static enum Align implements StringRepresentable {
            CENTER("center"),
            LEFT("left"),
            RIGHT("right");

            public static final Codec<Display.TextDisplay.Align> CODEC = StringRepresentable.fromEnum(Display.TextDisplay.Align::values);
            private final String name;

            private Align(String name) {
                this.name = name;
            }

            @Override
            public String getSerializedName() {
                return this.name;
            }
        }

        public record CachedInfo(List<Display.TextDisplay.CachedLine> lines, int width) {
        }

        public record CachedLine(FormattedCharSequence contents, int width) {
        }

        @FunctionalInterface
        public interface LineSplitter {
            Display.TextDisplay.CachedInfo split(Component text, int maxWidth);
        }

        public record TextRenderState(Component text, int lineWidth, Display.IntInterpolator textOpacity, Display.IntInterpolator backgroundColor, byte flags) {
        }
    }

    record TransformationInterpolator(Transformation previous, Transformation current) implements Display.GenericInterpolator<Transformation> {
        public Transformation get(float p_278027_) {
            return p_278027_ >= 1.0 ? this.current : this.previous.slerp(this.current, p_278027_);
        }
    }
}
