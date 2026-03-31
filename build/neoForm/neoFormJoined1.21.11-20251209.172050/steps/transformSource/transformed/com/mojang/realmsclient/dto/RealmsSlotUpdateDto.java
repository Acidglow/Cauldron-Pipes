package com.mojang.realmsclient.dto;

import com.google.gson.annotations.SerializedName;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.jspecify.annotations.Nullable;

@OnlyIn(Dist.CLIENT)
public record RealmsSlotUpdateDto(
    @SerializedName("slotId") int slotId,
    @SerializedName("spawnProtection") int spawnProtection,
    @SerializedName("forceGameMode") boolean forceGameMode,
    @SerializedName("difficulty") int difficulty,
    @SerializedName("gameMode") int gameMode,
    @SerializedName("slotName") String slotName,
    @SerializedName("version") String version,
    @SerializedName("compatibility") RealmsServer.Compatibility compatibility,
    @SerializedName("worldTemplateId") long templateId,
    @SerializedName("worldTemplateImage") @Nullable String templateImage,
    @SerializedName("hardcore") boolean hardcore
) implements ReflectionBasedSerialization {
    public RealmsSlotUpdateDto(int p_419486_, RealmsWorldOptions p_419985_, boolean p_419522_) {
        this(
            p_419486_,
            p_419985_.spawnProtection,
            p_419985_.forceGameMode,
            p_419985_.difficulty,
            p_419985_.gameMode,
            p_419985_.getSlotName(p_419486_),
            p_419985_.version,
            p_419985_.compatibility,
            p_419985_.templateId,
            p_419985_.templateImage,
            p_419522_
        );
    }
}
