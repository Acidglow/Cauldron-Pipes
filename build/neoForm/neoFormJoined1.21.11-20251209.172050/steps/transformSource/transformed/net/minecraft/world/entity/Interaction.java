package net.minecraft.world.entity;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.mojang.serialization.codecs.RecordCodecBuilder.Instance;
import java.util.UUID;
import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.PushReaction;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

public class Interaction extends Entity implements Attackable, Targeting {
    private static final EntityDataAccessor<Float> DATA_WIDTH_ID = SynchedEntityData.defineId(Interaction.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> DATA_HEIGHT_ID = SynchedEntityData.defineId(Interaction.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Boolean> DATA_RESPONSE_ID = SynchedEntityData.defineId(Interaction.class, EntityDataSerializers.BOOLEAN);
    private static final String TAG_WIDTH = "width";
    private static final String TAG_HEIGHT = "height";
    private static final String TAG_ATTACK = "attack";
    private static final String TAG_INTERACTION = "interaction";
    private static final String TAG_RESPONSE = "response";
    private static final float DEFAULT_WIDTH = 1.0F;
    private static final float DEFAULT_HEIGHT = 1.0F;
    private static final boolean DEFAULT_RESPONSE = false;
    private Interaction.@Nullable PlayerAction attack;
    private Interaction.@Nullable PlayerAction interaction;

    public Interaction(EntityType<?> p_273319_, Level p_272713_) {
        super(p_273319_, p_272713_);
        this.noPhysics = true;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder p_326193_) {
        p_326193_.define(DATA_WIDTH_ID, 1.0F);
        p_326193_.define(DATA_HEIGHT_ID, 1.0F);
        p_326193_.define(DATA_RESPONSE_ID, false);
    }

    @Override
    protected void readAdditionalSaveData(ValueInput p_422502_) {
        this.setWidth(p_422502_.getFloatOr("width", 1.0F));
        this.setHeight(p_422502_.getFloatOr("height", 1.0F));
        this.attack = p_422502_.read("attack", Interaction.PlayerAction.CODEC).orElse(null);
        this.interaction = p_422502_.read("interaction", Interaction.PlayerAction.CODEC).orElse(null);
        this.setResponse(p_422502_.getBooleanOr("response", false));
        this.setBoundingBox(this.makeBoundingBox());
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput p_421568_) {
        p_421568_.putFloat("width", this.getWidth());
        p_421568_.putFloat("height", this.getHeight());
        p_421568_.storeNullable("attack", Interaction.PlayerAction.CODEC, this.attack);
        p_421568_.storeNullable("interaction", Interaction.PlayerAction.CODEC, this.interaction);
        p_421568_.putBoolean("response", this.getResponse());
    }

    @Override
    public void onSyncedDataUpdated(EntityDataAccessor<?> p_272722_) {
        super.onSyncedDataUpdated(p_272722_);
        if (DATA_HEIGHT_ID.equals(p_272722_) || DATA_WIDTH_ID.equals(p_272722_)) {
            this.refreshDimensions();
        }
    }

    @Override
    public boolean canBeHitByProjectile() {
        return false;
    }

    @Override
    public boolean isPickable() {
        return true;
    }

    @Override
    public PushReaction getPistonPushReaction() {
        return PushReaction.IGNORE;
    }

    @Override
    public boolean isIgnoringBlockTriggers() {
        return true;
    }

    @Override
    public boolean skipAttackInteraction(Entity p_273553_) {
        if (p_273553_ instanceof Player player) {
            this.attack = new Interaction.PlayerAction(player.getUUID(), this.level().getGameTime());
            if (player instanceof ServerPlayer serverplayer) {
                CriteriaTriggers.PLAYER_HURT_ENTITY.trigger(serverplayer, this, player.damageSources().generic(), 1.0F, 1.0F, false);
            }

            return !this.getResponse();
        } else {
            return false;
        }
    }

    @Override
    public final boolean hurtServer(ServerLevel p_376324_, DamageSource p_376830_, float p_376086_) {
        return false;
    }

    @Override
    public InteractionResult interact(Player p_273507_, InteractionHand p_273048_) {
        if (this.level().isClientSide()) {
            return this.getResponse() ? InteractionResult.SUCCESS : InteractionResult.CONSUME;
        } else {
            this.interaction = new Interaction.PlayerAction(p_273507_.getUUID(), this.level().getGameTime());
            return InteractionResult.CONSUME;
        }
    }

    @Override
    public void tick() {
    }

    @Override
    public @Nullable LivingEntity getLastAttacker() {
        return this.attack != null ? this.level().getPlayerByUUID(this.attack.player()) : null;
    }

    @Override
    public @Nullable LivingEntity getTarget() {
        return this.interaction != null ? this.level().getPlayerByUUID(this.interaction.player()) : null;
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

    private float getHeight() {
        return this.entityData.get(DATA_HEIGHT_ID);
    }

    private void setResponse(boolean response) {
        this.entityData.set(DATA_RESPONSE_ID, response);
    }

    private boolean getResponse() {
        return this.entityData.get(DATA_RESPONSE_ID);
    }

    private EntityDimensions getDimensions() {
        return EntityDimensions.scalable(this.getWidth(), this.getHeight());
    }

    @Override
    public EntityDimensions getDimensions(Pose p_273111_) {
        return this.getDimensions();
    }

    @Override
    protected AABB makeBoundingBox(Vec3 p_390454_) {
        return this.getDimensions().makeBoundingBox(p_390454_);
    }

    record PlayerAction(UUID player, long timestamp) {
        public static final Codec<Interaction.PlayerAction> CODEC = RecordCodecBuilder.create(
            p_273237_ -> p_273237_.group(
                    UUIDUtil.CODEC.fieldOf("player").forGetter(Interaction.PlayerAction::player),
                    Codec.LONG.fieldOf("timestamp").forGetter(Interaction.PlayerAction::timestamp)
                )
                .apply(p_273237_, Interaction.PlayerAction::new)
        );
    }
}
