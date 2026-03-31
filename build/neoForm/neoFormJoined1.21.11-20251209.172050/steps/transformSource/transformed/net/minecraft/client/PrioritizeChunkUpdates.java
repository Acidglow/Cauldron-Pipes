package net.minecraft.client;

import com.mojang.serialization.Codec;
import java.util.function.IntFunction;
import net.minecraft.network.chat.Component;
import net.minecraft.util.ByIdMap;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public enum PrioritizeChunkUpdates {
    NONE(0, "options.prioritizeChunkUpdates.none"),
    PLAYER_AFFECTED(1, "options.prioritizeChunkUpdates.byPlayer"),
    NEARBY(2, "options.prioritizeChunkUpdates.nearby");

    private static final IntFunction<PrioritizeChunkUpdates> BY_ID = ByIdMap.continuous(p_468528_ -> p_468528_.id, values(), ByIdMap.OutOfBoundsStrategy.WRAP);
    public static final Codec<PrioritizeChunkUpdates> LEGACY_CODEC = Codec.INT.xmap(BY_ID::apply, p_468782_ -> p_468782_.id);
    private final int id;
    private final Component caption;

    private PrioritizeChunkUpdates(int id, String key) {
        this.id = id;
        this.caption = Component.translatable(key);
    }

    public Component caption() {
        return this.caption;
    }
}
