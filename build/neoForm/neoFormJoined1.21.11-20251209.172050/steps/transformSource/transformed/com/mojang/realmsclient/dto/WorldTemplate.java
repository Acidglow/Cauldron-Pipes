package com.mojang.realmsclient.dto;

import com.google.gson.JsonObject;
import com.mojang.logging.LogUtils;
import com.mojang.realmsclient.util.JsonUtils;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

@OnlyIn(Dist.CLIENT)
public record WorldTemplate(
    String id,
    String name,
    String version,
    String author,
    String link,
    @Nullable String image,
    String trailer,
    String recommendedPlayers,
    WorldTemplate.WorldTemplateType type
) {
    private static final Logger LOGGER = LogUtils.getLogger();

    public static @Nullable WorldTemplate parse(JsonObject json) {
        try {
            String s = JsonUtils.getStringOr("type", json, null);
            return new WorldTemplate(
                JsonUtils.getStringOr("id", json, ""),
                JsonUtils.getStringOr("name", json, ""),
                JsonUtils.getStringOr("version", json, ""),
                JsonUtils.getStringOr("author", json, ""),
                JsonUtils.getStringOr("link", json, ""),
                JsonUtils.getStringOr("image", json, null),
                JsonUtils.getStringOr("trailer", json, ""),
                JsonUtils.getStringOr("recommendedPlayers", json, ""),
                s == null ? WorldTemplate.WorldTemplateType.WORLD_TEMPLATE : WorldTemplate.WorldTemplateType.valueOf(s)
            );
        } catch (Exception exception) {
            LOGGER.error("Could not parse WorldTemplate", (Throwable)exception);
            return null;
        }
    }

    @OnlyIn(Dist.CLIENT)
    public static enum WorldTemplateType {
        WORLD_TEMPLATE,
        MINIGAME,
        ADVENTUREMAP,
        EXPERIENCE,
        INSPIRATION;
    }
}
