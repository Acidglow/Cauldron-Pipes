package net.minecraft.world.entity.animal.sheep;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponentGetter;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.ItemTags;
import net.minecraft.util.Mth;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Shearable;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.BreedGoal;
import net.minecraft.world.entity.ai.goal.EatBlockGoal;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.FollowParentGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.PanicGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.TemptGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.level.storage.loot.BuiltInLootTables;
import org.jspecify.annotations.Nullable;

public class Sheep extends Animal implements Shearable {
    private static final int EAT_ANIMATION_TICKS = 40;
    private static final EntityDataAccessor<Byte> DATA_WOOL_ID = SynchedEntityData.defineId(Sheep.class, EntityDataSerializers.BYTE);
    private static final DyeColor DEFAULT_COLOR = DyeColor.WHITE;
    private static final boolean DEFAULT_SHEARED = false;
    private int eatAnimationTick;
    private EatBlockGoal eatBlockGoal;

    public Sheep(EntityType<? extends Sheep> p_405549_, Level p_405858_) {
        super(p_405549_, p_405858_);
    }

    @Override
    protected void registerGoals() {
        this.eatBlockGoal = new EatBlockGoal(this);
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(1, new PanicGoal(this, 1.25));
        this.goalSelector.addGoal(2, new BreedGoal(this, 1.0));
        this.goalSelector.addGoal(3, new TemptGoal(this, 1.1, p_404879_ -> p_404879_.is(ItemTags.SHEEP_FOOD), false));
        this.goalSelector.addGoal(4, new FollowParentGoal(this, 1.1));
        this.goalSelector.addGoal(5, this.eatBlockGoal);
        this.goalSelector.addGoal(6, new WaterAvoidingRandomStrollGoal(this, 1.0));
        this.goalSelector.addGoal(7, new LookAtPlayerGoal(this, Player.class, 6.0F));
        this.goalSelector.addGoal(8, new RandomLookAroundGoal(this));
    }

    @Override
    public boolean isFood(ItemStack p_404944_) {
        return p_404944_.is(ItemTags.SHEEP_FOOD);
    }

    @Override
    protected void customServerAiStep(ServerLevel p_404959_) {
        this.eatAnimationTick = this.eatBlockGoal.getEatAnimationTick();
        super.customServerAiStep(p_404959_);
    }

    @Override
    public void aiStep() {
        if (this.level().isClientSide()) {
            this.eatAnimationTick = Math.max(0, this.eatAnimationTick - 1);
        }

        super.aiStep();
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Animal.createAnimalAttributes().add(Attributes.MAX_HEALTH, 8.0).add(Attributes.MOVEMENT_SPEED, 0.23F);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder p_404828_) {
        super.defineSynchedData(p_404828_);
        p_404828_.define(DATA_WOOL_ID, (byte)0);
    }

    @Override
    public void handleEntityEvent(byte p_405380_) {
        if (p_405380_ == 10) {
            this.eatAnimationTick = 40;
        } else {
            super.handleEntityEvent(p_405380_);
        }
    }

    public float getHeadEatPositionScale(float partialTick) {
        if (this.eatAnimationTick <= 0) {
            return 0.0F;
        } else if (this.eatAnimationTick >= 4 && this.eatAnimationTick <= 36) {
            return 1.0F;
        } else {
            return this.eatAnimationTick < 4 ? (this.eatAnimationTick - partialTick) / 4.0F : -(this.eatAnimationTick - 40 - partialTick) / 4.0F;
        }
    }

    public float getHeadEatAngleScale(float partialTick) {
        if (this.eatAnimationTick > 4 && this.eatAnimationTick <= 36) {
            float f = (this.eatAnimationTick - 4 - partialTick) / 32.0F;
            return (float) (Math.PI / 5) + 0.21991149F * Mth.sin(f * 28.7F);
        } else {
            return this.eatAnimationTick > 0 ? (float) (Math.PI / 5) : this.getXRot(partialTick) * (float) (Math.PI / 180.0);
        }
    }

    @Override
    public InteractionResult mobInteract(Player p_404982_, InteractionHand p_404782_) {
        ItemStack itemstack = p_404982_.getItemInHand(p_404782_);
        if (false && itemstack.is(Items.SHEARS)) { // Neo: Shear logic is handled by IShearable
            if (this.level() instanceof ServerLevel serverlevel && this.readyForShearing()) {
                this.shear(serverlevel, SoundSource.PLAYERS, itemstack);
                this.gameEvent(GameEvent.SHEAR, p_404982_);
                itemstack.hurtAndBreak(1, p_404982_, p_404782_.asEquipmentSlot());
                return InteractionResult.SUCCESS_SERVER;
            } else {
                return InteractionResult.CONSUME;
            }
        } else {
            return super.mobInteract(p_404982_, p_404782_);
        }
    }

    @Override
    public void shear(ServerLevel p_405075_, SoundSource p_405077_, ItemStack p_405709_) {
        p_405075_.playSound(null, this, SoundEvents.SHEEP_SHEAR, p_405077_, 1.0F, 1.0F);
        this.dropFromShearingLootTable(
            p_405075_,
            BuiltInLootTables.SHEAR_SHEEP,
            p_405709_,
            (p_405522_, p_405241_) -> {
                for (int i = 0; i < p_405241_.getCount(); i++) {
                    ItemEntity itementity = this.spawnAtLocation(p_405522_, p_405241_.copyWithCount(1), 1.0F);
                    if (itementity != null) {
                        itementity.setDeltaMovement(
                            itementity.getDeltaMovement()
                                .add(
                                    (this.random.nextFloat() - this.random.nextFloat()) * 0.1F,
                                    this.random.nextFloat() * 0.05F,
                                    (this.random.nextFloat() - this.random.nextFloat()) * 0.1F
                                )
                        );
                    }
                }
            }
        );
        this.setSheared(true);
    }

    @Override
    public boolean readyForShearing() {
        return this.isAlive() && !this.isSheared() && !this.isBaby();
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput p_422587_) {
        super.addAdditionalSaveData(p_422587_);
        p_422587_.putBoolean("Sheared", this.isSheared());
        p_422587_.store("Color", DyeColor.LEGACY_ID_CODEC, this.getColor());
    }

    @Override
    protected void readAdditionalSaveData(ValueInput p_421604_) {
        super.readAdditionalSaveData(p_421604_);
        this.setSheared(p_421604_.getBooleanOr("Sheared", false));
        this.setColor(p_421604_.read("Color", DyeColor.LEGACY_ID_CODEC).orElse(DEFAULT_COLOR));
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return SoundEvents.SHEEP_AMBIENT;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource p_405795_) {
        return SoundEvents.SHEEP_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.SHEEP_DEATH;
    }

    @Override
    protected void playStepSound(BlockPos p_405483_, BlockState p_405036_) {
        this.playSound(SoundEvents.SHEEP_STEP, 0.15F, 1.0F);
    }

    public DyeColor getColor() {
        return DyeColor.byId(this.entityData.get(DATA_WOOL_ID) & 15);
    }

    public void setColor(DyeColor color) {
        byte b0 = this.entityData.get(DATA_WOOL_ID);
        this.entityData.set(DATA_WOOL_ID, (byte)(b0 & 240 | color.getId() & 15));
    }

    @Override
    public <T> @Nullable T get(DataComponentType<? extends T> p_405771_) {
        return p_405771_ == DataComponents.SHEEP_COLOR ? castComponentValue((DataComponentType<T>)p_405771_, this.getColor()) : super.get(p_405771_);
    }

    @Override
    protected void applyImplicitComponents(DataComponentGetter p_404955_) {
        this.applyImplicitComponentIfPresent(p_404955_, DataComponents.SHEEP_COLOR);
        super.applyImplicitComponents(p_404955_);
    }

    @Override
    protected <T> boolean applyImplicitComponent(DataComponentType<T> p_405881_, T p_405639_) {
        if (p_405881_ == DataComponents.SHEEP_COLOR) {
            this.setColor(castComponentValue(DataComponents.SHEEP_COLOR, p_405639_));
            return true;
        } else {
            return super.applyImplicitComponent(p_405881_, p_405639_);
        }
    }

    public boolean isSheared() {
        return (this.entityData.get(DATA_WOOL_ID) & 16) != 0;
    }

    public void setSheared(boolean sheared) {
        byte b0 = this.entityData.get(DATA_WOOL_ID);
        if (sheared) {
            this.entityData.set(DATA_WOOL_ID, (byte)(b0 | 16));
        } else {
            this.entityData.set(DATA_WOOL_ID, (byte)(b0 & -17));
        }
    }

    public static DyeColor getRandomSheepColor(ServerLevelAccessor level, BlockPos pos) {
        Holder<Biome> holder = level.getBiome(pos);
        return SheepColorSpawnRules.getSheepColor(holder, level.getRandom());
    }

    public @Nullable Sheep getBreedOffspring(ServerLevel p_405425_, AgeableMob p_404874_) {
        Sheep sheep = EntityType.SHEEP.create(p_405425_, EntitySpawnReason.BREEDING);
        if (sheep != null) {
            DyeColor dyecolor = this.getColor();
            DyeColor dyecolor1 = ((Sheep)p_404874_).getColor();
            sheep.setColor(DyeColor.getMixedColor(p_405425_, dyecolor, dyecolor1));
        }

        return sheep;
    }

    @Override
    public void ate() {
        super.ate();
        this.setSheared(false);
        if (this.isBaby()) {
            this.ageUp(60);
        }
    }

    @Override
    public @Nullable SpawnGroupData finalizeSpawn(
        ServerLevelAccessor p_404937_, DifficultyInstance p_405503_, EntitySpawnReason p_405554_, @Nullable SpawnGroupData p_405662_
    ) {
        this.setColor(getRandomSheepColor(p_404937_, this.blockPosition()));
        return super.finalizeSpawn(p_404937_, p_405503_, p_405554_, p_405662_);
    }
}
