package net.minecraft.client.gui.screens.options;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.mojang.blaze3d.platform.Monitor;
import com.mojang.blaze3d.platform.VideoMode;
import com.mojang.blaze3d.platform.Window;
import java.util.List;
import java.util.Optional;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.OptionInstance;
import net.minecraft.client.Options;
import net.minecraft.client.TextureFilteringMethod;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.GpuWarnlistManager;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class VideoSettingsScreen extends OptionsSubScreen {
    private static final Component TITLE = Component.translatable("options.videoTitle");
    private static final Component IMPROVED_TRANSPARENCY = Component.translatable("options.improvedTransparency").withStyle(ChatFormatting.ITALIC);
    private static final Component WARNING_MESSAGE = Component.translatable("options.graphics.warning.message", IMPROVED_TRANSPARENCY, IMPROVED_TRANSPARENCY);
    private static final Component WARNING_TITLE = Component.translatable("options.graphics.warning.title").withStyle(ChatFormatting.RED);
    private static final Component BUTTON_ACCEPT = Component.translatable("options.graphics.warning.accept");
    private static final Component BUTTON_CANCEL = Component.translatable("options.graphics.warning.cancel");
    private static final Component DISPLAY_HEADER = Component.translatable("options.video.display.header");
    private static final Component QUALITY_HEADER = Component.translatable("options.video.quality.header");
    private static final Component PREFERENCES_HEADER = Component.translatable("options.video.preferences.header");
    private final GpuWarnlistManager gpuWarnlistManager;
    private final int oldMipmaps;
    private final int oldAnisotropyBit;
    private final TextureFilteringMethod oldTextureFiltering;

    private static OptionInstance<?>[] qualityOptions(Options options) {
        return new OptionInstance[]{
            options.biomeBlendRadius(),
            options.renderDistance(),
            options.prioritizeChunkUpdates(),
            options.simulationDistance(),
            options.ambientOcclusion(),
            options.cloudStatus(),
            options.particles(),
            options.mipmapLevels(),
            options.entityShadows(),
            options.entityDistanceScaling(),
            options.menuBackgroundBlurriness(),
            options.cloudRange(),
            options.cutoutLeaves(),
            options.improvedTransparency(),
            options.textureFiltering(),
            options.maxAnisotropyBit(),
            options.weatherRadius()
        };
    }

    private static OptionInstance<?>[] displayOptions(Options options) {
        return new OptionInstance[]{
            options.framerateLimit(),
            options.enableVsync(),
            options.inactivityFpsLimit(),
            options.guiScale(),
            options.fullscreen(),
            options.gamma()
        };
    }

    private static OptionInstance<?>[] preferenceOptions(Options options) {
        return new OptionInstance[]{options.showAutosaveIndicator(), options.vignette(), options.attackIndicator(), options.chunkSectionFadeInTime()};
    }

    public VideoSettingsScreen(Screen lastScreen, Minecraft minecraft, Options options) {
        super(lastScreen, options, TITLE);
        this.gpuWarnlistManager = minecraft.getGpuWarnlistManager();
        this.gpuWarnlistManager.resetWarnings();
        if (options.improvedTransparency().get()) {
            this.gpuWarnlistManager.dismissWarning();
        }

        this.oldMipmaps = options.mipmapLevels().get();
        this.oldAnisotropyBit = options.maxAnisotropyBit().get();
        this.oldTextureFiltering = options.textureFiltering().get();
    }

    @Override
    protected void addOptions() {
        int i = -1;
        Window window = this.minecraft.getWindow();
        Monitor monitor = window.findBestMonitor();
        int j;
        if (monitor == null) {
            j = -1;
        } else {
            Optional<VideoMode> optional = window.getPreferredFullscreenVideoMode();
            j = optional.map(monitor::getVideoModeIndex).orElse(-1);
        }

        OptionInstance<Integer> optioninstance = new OptionInstance<>(
            "options.fullscreen.resolution",
            OptionInstance.noTooltip(),
            (p_346436_, p_344747_) -> {
                if (monitor == null) {
                    return Component.translatable("options.fullscreen.unavailable");
                } else if (p_344747_ == -1) {
                    return Options.genericValueLabel(p_346436_, Component.translatable("options.fullscreen.current"));
                } else {
                    VideoMode videomode = monitor.getMode(p_344747_);
                    return Options.genericValueLabel(
                        p_346436_,
                        Component.translatable(
                            "options.fullscreen.entry",
                            videomode.getWidth(),
                            videomode.getHeight(),
                            videomode.getRefreshRate(),
                            videomode.getRedBits() + videomode.getGreenBits() + videomode.getBlueBits()
                        )
                    );
                }
            },
            new OptionInstance.IntRange(-1, monitor != null ? monitor.getModeCount() - 1 : -1),
            j,
            p_345666_ -> {
                if (monitor != null) {
                    window.setPreferredFullscreenVideoMode(p_345666_ == -1 ? Optional.empty() : Optional.of(monitor.getMode(p_345666_)));
                }
            }
        );
        this.list.addHeader(DISPLAY_HEADER);
        this.list.addBig(optioninstance);
        this.list.addSmall(displayOptions(this.options));
        this.list.addHeader(QUALITY_HEADER);
        this.list.addBig(this.options.graphicsPreset());
        this.list.addSmall(qualityOptions(this.options));
        this.list.addHeader(PREFERENCES_HEADER);
        this.list.addSmall(preferenceOptions(this.options));
    }

    @Override
    public void tick() {
        if (this.list != null && this.list.findOption(this.options.maxAnisotropyBit()) instanceof AbstractSliderButton abstractsliderbutton) {
            abstractsliderbutton.active = this.options.textureFiltering().get() == TextureFilteringMethod.ANISOTROPIC;
        }

        super.tick();
    }

    @Override
    public void onClose() {
        this.minecraft.getWindow().changeFullscreenVideoMode();
        super.onClose();
    }

    @Override
    public void removed() {
        if (this.options.mipmapLevels().get() != this.oldMipmaps
            || this.options.maxAnisotropyBit().get() != this.oldAnisotropyBit
            || this.options.textureFiltering().get() != this.oldTextureFiltering) {
            this.minecraft.updateMaxMipLevel(this.options.mipmapLevels().get());
            this.minecraft.delayTextureReload();
        }

        super.removed();
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent p_446300_, boolean p_434065_) {
        if (super.mouseClicked(p_446300_, p_434065_)) {
            if (this.gpuWarnlistManager.isShowingWarning()) {
                List<Component> list = Lists.newArrayList(WARNING_MESSAGE, CommonComponents.NEW_LINE);
                String s = this.gpuWarnlistManager.getRendererWarnings();
                if (s != null) {
                    list.add(CommonComponents.NEW_LINE);
                    list.add(Component.translatable("options.graphics.warning.renderer", s).withStyle(ChatFormatting.GRAY));
                }

                String s1 = this.gpuWarnlistManager.getVendorWarnings();
                if (s1 != null) {
                    list.add(CommonComponents.NEW_LINE);
                    list.add(Component.translatable("options.graphics.warning.vendor", s1).withStyle(ChatFormatting.GRAY));
                }

                String s2 = this.gpuWarnlistManager.getVersionWarnings();
                if (s2 != null) {
                    list.add(CommonComponents.NEW_LINE);
                    list.add(Component.translatable("options.graphics.warning.version", s2).withStyle(ChatFormatting.GRAY));
                }

                this.minecraft
                    .setScreen(
                        new UnsupportedGraphicsWarningScreen(
                            WARNING_TITLE, list, ImmutableList.of(new UnsupportedGraphicsWarningScreen.ButtonOption(BUTTON_ACCEPT, p_454176_ -> {
                                this.options.improvedTransparency().set(true);
                                Minecraft.getInstance().levelRenderer.allChanged();
                                this.gpuWarnlistManager.dismissWarning();
                                this.minecraft.setScreen(this);
                            }), new UnsupportedGraphicsWarningScreen.ButtonOption(BUTTON_CANCEL, p_454175_ -> {
                                this.gpuWarnlistManager.dismissWarning();
                                this.options.improvedTransparency().set(false);
                                this.updateTransparencyButton();
                                this.minecraft.setScreen(this);
                            }))
                        )
                    );
            }

            return true;
        } else {
            return false;
        }
    }

    @Override
    public boolean mouseScrolled(double p_344913_, double p_346159_, double p_345166_, double p_345130_) {
        if (this.minecraft.hasControlDown()) {
            OptionInstance<Integer> optioninstance = this.options.guiScale();
            if (optioninstance.values() instanceof OptionInstance.ClampingLazyMaxIntRange optioninstance$clampinglazymaxintrange) {
                int k = optioninstance.get();
                int i = k == 0 ? optioninstance$clampinglazymaxintrange.maxInclusive() + 1 : k;
                int j = i + (int)Math.signum(p_345130_);
                if (j != 0 && j <= optioninstance$clampinglazymaxintrange.maxInclusive() && j >= optioninstance$clampinglazymaxintrange.minInclusive()) {
                    CycleButton<Integer> cyclebutton = (CycleButton<Integer>)this.list.findOption(optioninstance);
                    if (cyclebutton != null) {
                        optioninstance.set(j);
                        cyclebutton.setValue(j);
                        this.list.setScrollAmount(0.0);
                        return true;
                    }
                }
            }

            return false;
        } else {
            return super.mouseScrolled(p_344913_, p_346159_, p_345166_, p_345130_);
        }
    }

    public void updateFullscreenButton(boolean isFullscreen) {
        if (this.list != null) {
            AbstractWidget abstractwidget = this.list.findOption(this.options.fullscreen());
            if (abstractwidget != null) {
                CycleButton<Boolean> cyclebutton = (CycleButton<Boolean>)abstractwidget;
                cyclebutton.setValue(isFullscreen);
            }
        }
    }

    public void updateTransparencyButton() {
        if (this.list != null) {
            OptionInstance<Boolean> optioninstance = this.options.improvedTransparency();
            AbstractWidget abstractwidget = this.list.findOption(optioninstance);
            if (abstractwidget != null) {
                CycleButton<Boolean> cyclebutton = (CycleButton<Boolean>)abstractwidget;
                cyclebutton.setValue(optioninstance.get());
            }
        }
    }
}
