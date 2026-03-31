package net.minecraft.world.entity.boss.enderdragon;

import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerEntity;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jspecify.annotations.Nullable;

public class EnderDragonPart extends net.neoforged.neoforge.entity.PartEntity<EnderDragon> {
    public final EnderDragon parentMob;
    public final String name;
    private final EntityDimensions size;

    public EnderDragonPart(EnderDragon parentMob, String name, float width, float height) {
        super(parentMob);
        this.size = EntityDimensions.scalable(width, height);
        this.refreshDimensions();
        this.parentMob = parentMob;
        this.name = name;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder p_479568_) {
    }

    @Override
    protected void readAdditionalSaveData(ValueInput p_482000_) {
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput p_480358_) {
    }

    @Override
    public boolean isPickable() {
        return true;
    }

    @Override
    public @Nullable ItemStack getPickResult() {
        return this.parentMob.getPickResult();
    }

    @Override
    public final boolean hurtServer(ServerLevel p_481710_, DamageSource p_479045_, float p_479436_) {
        return this.isInvulnerableToBase(p_479045_) ? false : this.parentMob.hurt(p_481710_, this, p_479045_, p_479436_);
    }

    @Override
    public boolean is(Entity p_481044_) {
        return this == p_481044_ || this.parentMob == p_481044_;
    }

    @Override
    public Packet<ClientGamePacketListener> getAddEntityPacket(ServerEntity p_481174_) {
        throw new UnsupportedOperationException();
    }

    @Override
    public EntityDimensions getDimensions(Pose p_481490_) {
        return this.size;
    }

    @Override
    public boolean shouldBeSaved() {
        return false;
    }
}
