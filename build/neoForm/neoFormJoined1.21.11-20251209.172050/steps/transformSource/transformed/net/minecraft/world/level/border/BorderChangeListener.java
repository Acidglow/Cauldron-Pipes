package net.minecraft.world.level.border;

public interface BorderChangeListener {
    void onSetSize(WorldBorder worldBorder, double size);

    void onLerpSize(WorldBorder border, double from, double to, long lerpTime, long lerpBegin);

    void onSetCenter(WorldBorder worldBorder, double x, double z);

    void onSetWarningTime(WorldBorder worldBorder, int warningTime);

    void onSetWarningBlocks(WorldBorder worldBorder, int warningBlocks);

    void onSetDamagePerBlock(WorldBorder worldBorder, double damagePerBlock);

    void onSetSafeZone(WorldBorder worldBorder, double safeSize);
}
