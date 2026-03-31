package net.minecraft.client;

import com.mojang.serialization.Codec;
import java.util.function.IntFunction;
import net.minecraft.network.chat.Component;
import net.minecraft.util.ByIdMap;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public enum TextureFilteringMethod {
    NONE(0, "options.textureFiltering.none"),
    RGSS(1, "options.textureFiltering.rgss"),
    ANISOTROPIC(2, "options.textureFiltering.anisotropic");

    private static final IntFunction<TextureFilteringMethod> BY_ID = ByIdMap.continuous(p_478587_ -> p_478587_.id, values(), ByIdMap.OutOfBoundsStrategy.WRAP);
    public static final Codec<TextureFilteringMethod> LEGACY_CODEC = Codec.INT.xmap(BY_ID::apply, p_479033_ -> p_479033_.id);
    private final int id;
    private final Component caption;

    private TextureFilteringMethod(int id, String translationKey) {
        this.id = id;
        this.caption = Component.translatable(translationKey);
    }

    public Component caption() {
        return this.caption;
    }
}
