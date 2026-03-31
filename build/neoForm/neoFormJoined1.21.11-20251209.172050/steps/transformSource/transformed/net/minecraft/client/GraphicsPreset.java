package net.minecraft.client;

import com.mojang.blaze3d.GraphicsWorkarounds;
import com.mojang.blaze3d.systems.GpuDevice;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.serialization.Codec;
import net.minecraft.client.gui.screens.options.OptionsSubScreen;
import net.minecraft.server.level.ParticleStatus;
import net.minecraft.util.StringRepresentable;
import net.minecraft.util.Util;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.jspecify.annotations.Nullable;

@OnlyIn(Dist.CLIENT)
public enum GraphicsPreset implements StringRepresentable {
    FAST("fast", "options.graphics.fast"),
    FANCY("fancy", "options.graphics.fancy"),
    FABULOUS("fabulous", "options.graphics.fabulous"),
    CUSTOM("custom", "options.graphics.custom");

    private final String serializedName;
    private final String key;
    public static final Codec<GraphicsPreset> CODEC = StringRepresentable.fromEnum(GraphicsPreset::values);

    private GraphicsPreset(String serializedName, String key) {
        this.serializedName = serializedName;
        this.key = key;
    }

    @Override
    public String getSerializedName() {
        return this.serializedName;
    }

    public String getKey() {
        return this.key;
    }

    public void apply(Minecraft minecraft) {
        OptionsSubScreen optionssubscreen = minecraft.screen instanceof OptionsSubScreen ? (OptionsSubScreen)minecraft.screen : null;
        GpuDevice gpudevice = RenderSystem.getDevice();
        switch (this) {
            case FAST:
                int k = 8;
                this.set(optionssubscreen, minecraft.options.biomeBlendRadius(), 1);
                this.set(optionssubscreen, minecraft.options.renderDistance(), 8);
                this.set(optionssubscreen, minecraft.options.prioritizeChunkUpdates(), PrioritizeChunkUpdates.NONE);
                this.set(optionssubscreen, minecraft.options.simulationDistance(), 6);
                this.set(optionssubscreen, minecraft.options.ambientOcclusion(), false);
                this.set(optionssubscreen, minecraft.options.cloudStatus(), CloudStatus.FAST);
                this.set(optionssubscreen, minecraft.options.particles(), ParticleStatus.DECREASED);
                this.set(optionssubscreen, minecraft.options.mipmapLevels(), 2);
                this.set(optionssubscreen, minecraft.options.entityShadows(), false);
                this.set(optionssubscreen, minecraft.options.entityDistanceScaling(), 0.75);
                this.set(optionssubscreen, minecraft.options.menuBackgroundBlurriness(), 2);
                this.set(optionssubscreen, minecraft.options.cloudRange(), 32);
                this.set(optionssubscreen, minecraft.options.cutoutLeaves(), false);
                this.set(optionssubscreen, minecraft.options.improvedTransparency(), false);
                this.set(optionssubscreen, minecraft.options.weatherRadius(), 5);
                this.set(optionssubscreen, minecraft.options.maxAnisotropyBit(), 1);
                this.set(optionssubscreen, minecraft.options.textureFiltering(), TextureFilteringMethod.NONE);
                break;
            case FANCY:
                int j = 16;
                this.set(optionssubscreen, minecraft.options.biomeBlendRadius(), 2);
                this.set(optionssubscreen, minecraft.options.renderDistance(), 16);
                this.set(optionssubscreen, minecraft.options.prioritizeChunkUpdates(), PrioritizeChunkUpdates.PLAYER_AFFECTED);
                this.set(optionssubscreen, minecraft.options.simulationDistance(), 12);
                this.set(optionssubscreen, minecraft.options.ambientOcclusion(), true);
                this.set(optionssubscreen, minecraft.options.cloudStatus(), CloudStatus.FANCY);
                this.set(optionssubscreen, minecraft.options.particles(), ParticleStatus.ALL);
                this.set(optionssubscreen, minecraft.options.mipmapLevels(), 4);
                this.set(optionssubscreen, minecraft.options.entityShadows(), true);
                this.set(optionssubscreen, minecraft.options.entityDistanceScaling(), 1.0);
                this.set(optionssubscreen, minecraft.options.menuBackgroundBlurriness(), 5);
                this.set(optionssubscreen, minecraft.options.cloudRange(), 64);
                this.set(optionssubscreen, minecraft.options.cutoutLeaves(), true);
                this.set(optionssubscreen, minecraft.options.improvedTransparency(), false);
                this.set(optionssubscreen, minecraft.options.weatherRadius(), 10);
                this.set(optionssubscreen, minecraft.options.maxAnisotropyBit(), 1);
                this.set(optionssubscreen, minecraft.options.textureFiltering(), TextureFilteringMethod.RGSS);
                break;
            case FABULOUS:
                int i = 32;
                this.set(optionssubscreen, minecraft.options.biomeBlendRadius(), 2);
                this.set(optionssubscreen, minecraft.options.renderDistance(), 32);
                this.set(optionssubscreen, minecraft.options.prioritizeChunkUpdates(), PrioritizeChunkUpdates.PLAYER_AFFECTED);
                this.set(optionssubscreen, minecraft.options.simulationDistance(), 12);
                this.set(optionssubscreen, minecraft.options.ambientOcclusion(), true);
                this.set(optionssubscreen, minecraft.options.cloudStatus(), CloudStatus.FANCY);
                this.set(optionssubscreen, minecraft.options.particles(), ParticleStatus.ALL);
                this.set(optionssubscreen, minecraft.options.mipmapLevels(), 4);
                this.set(optionssubscreen, minecraft.options.entityShadows(), true);
                this.set(optionssubscreen, minecraft.options.entityDistanceScaling(), 1.25);
                this.set(optionssubscreen, minecraft.options.menuBackgroundBlurriness(), 5);
                this.set(optionssubscreen, minecraft.options.cloudRange(), 128);
                this.set(optionssubscreen, minecraft.options.cutoutLeaves(), true);
                this.set(optionssubscreen, minecraft.options.improvedTransparency(), Util.getPlatform() != Util.OS.OSX);
                this.set(optionssubscreen, minecraft.options.weatherRadius(), 10);
                this.set(optionssubscreen, minecraft.options.maxAnisotropyBit(), 2);
                if (GraphicsWorkarounds.get(gpudevice).isAmd()) {
                    this.set(optionssubscreen, minecraft.options.textureFiltering(), TextureFilteringMethod.RGSS);
                } else {
                    this.set(optionssubscreen, minecraft.options.textureFiltering(), TextureFilteringMethod.ANISOTROPIC);
                }
        }
    }

    <T> void set(@Nullable OptionsSubScreen screen, OptionInstance<T> option, T value) {
        if (option.get() != value) {
            option.set(value);
            if (screen != null) {
                screen.resetOption(option);
            }
        }
    }
}
