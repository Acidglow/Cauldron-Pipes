package net.minecraft.client.gui.screens.inventory;

import com.google.common.collect.Ordering;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.StringWidget;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.Identifier;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffectUtil;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class EffectsInInventory {
    private static final Identifier EFFECT_BACKGROUND_SPRITE = Identifier.withDefaultNamespace("container/inventory/effect_background");
    private static final Identifier EFFECT_BACKGROUND_AMBIENT_SPRITE = Identifier.withDefaultNamespace("container/inventory/effect_background_ambient");
    private static final int ICON_SIZE = 18;
    public static final int SPACING = 7;
    private static final int TEXT_X_OFFSET = 32;
    public static final int SPRITE_SQUARE_SIZE = 32;
    private final AbstractContainerScreen<?> screen;
    private final Minecraft minecraft;

    public EffectsInInventory(AbstractContainerScreen<?> screen) {
        this.screen = screen;
        this.minecraft = Minecraft.getInstance();
    }

    public boolean canSeeEffects() {
        int i = this.screen.leftPos + this.screen.imageWidth + 2;
        int j = this.screen.width - i;
        return j >= 32;
    }

    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        int i = this.screen.leftPos + this.screen.imageWidth + 2;
        int j = this.screen.width - i;
        Collection<MobEffectInstance> collection = this.minecraft.player.getActiveEffects();
        if (!collection.isEmpty() && j >= 32) {
            boolean flag = j >= 120;
            var event = net.neoforged.neoforge.client.ClientHooks.onScreenPotionSize(screen, j, !flag, i);
            if (event.isCanceled()) return;
            flag = !event.isCompact();
            i = event.getHorizontalOffset();

            collection = collection.stream().filter(net.neoforged.neoforge.client.ClientHooks::shouldRenderEffect).sorted().collect(java.util.stream.Collectors.toList());

            int k = flag ? j - 7 : 32;
            int l = 33;
            if (collection.size() > 5) {
                l = 132 / (collection.size() - 1);
            }

            this.renderEffects(guiGraphics, collection, i, l, mouseX, mouseY, k);
        }
    }

    private void renderEffects(
        GuiGraphics guiGraphics, Collection<MobEffectInstance> effects, int x, int y, int mouseX, int mouseY, int minWidth
    ) {
        Iterable<MobEffectInstance> iterable = Ordering.natural().sortedCopy(effects);
        int i = this.screen.topPos;
        Font font = this.screen.getFont();

        for (MobEffectInstance mobeffectinstance : iterable) {
            var renderer = net.neoforged.neoforge.client.extensions.common.IClientMobEffectExtensions.of(mobeffectinstance);
            boolean flag = mobeffectinstance.isAmbient();
            Component component = this.getEffectName(mobeffectinstance);
            Component component1 = MobEffectUtil.formatDuration(mobeffectinstance, 1.0F, this.minecraft.level.tickRateManager().tickrate());
            int j = this.renderBackground(guiGraphics, font, component, component1, x, i, flag, minWidth);
            if (!renderer.renderInventoryText(mobeffectinstance, screen, guiGraphics, x, i, 0))
            this.renderText(guiGraphics, component, component1, font, x, i, j, y, mouseX, mouseY, mobeffectinstance);
            if (!renderer.renderInventoryIcon(mobeffectinstance, screen, guiGraphics, x + 7, i, 0))
            guiGraphics.blitSprite(RenderPipelines.GUI_TEXTURED, Gui.getMobEffectSprite(mobeffectinstance.getEffect()), x + 7, i + 7, 18, 18);
            i += y;
        }
    }

    private int renderBackground(
        GuiGraphics guiGraphics, Font font, Component text, Component duration, int x, int y, boolean ambient, int minWidth
    ) {
        int i = 32 + font.width(text) + 7;
        int j = 32 + font.width(duration) + 7;
        int k = Math.min(minWidth, Math.max(i, j));
        guiGraphics.blitSprite(RenderPipelines.GUI_TEXTURED, ambient ? EFFECT_BACKGROUND_AMBIENT_SPRITE : EFFECT_BACKGROUND_SPRITE, x, y, k, 32);
        return k;
    }

    // Neo: Call the method that takes the effect instance for which the text is being rendered
    @Deprecated
    private void renderText(
        GuiGraphics guiGraphics,
        Component effect,
        Component name,
        Font font,
        int x,
        int y,
        int width,
        int height,
        int mouseX,
        int mouseY
    ) {
        renderText(guiGraphics, effect, name, font, x, y, width, height, mouseX, mouseY, null);
    }

    private void renderText(
        GuiGraphics guiGraphics,
        Component effect,
        Component name,
        Font font,
        int x,
        int y,
        int width,
        int height,
        int mouseX,
        int mouseY
        , @org.jspecify.annotations.Nullable MobEffectInstance effectInstance
    ) {
        int i = x + 32;
        int j = y + 7;
        int k = width - 32 - 7;
        boolean flag;
        if (k > 0) {
            boolean flag1 = font.width(effect) > k;
            FormattedCharSequence formattedcharsequence = flag1 ? StringWidget.clipText(effect, font, k) : effect.getVisualOrderText();
            guiGraphics.drawString(font, formattedcharsequence, i, j, -1);
            guiGraphics.drawString(font, name, i, j + 9, -8355712);
            flag = flag1;
        } else {
            flag = true;
        }

        if (flag && mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + height) {
            // Neo: Allow mods to adjust the tooltip shown when hovering a mob effect.
            var list = List.of(effect, name);
            if (effectInstance != null) {
                list = net.neoforged.neoforge.client.ClientHooks.getEffectTooltip(screen, effectInstance, List.of(effect, name));
            }
            guiGraphics.setTooltipForNextFrame(this.screen.getFont(), list, Optional.empty(), mouseX, mouseY);
        }
    }

    private Component getEffectName(MobEffectInstance effect) {
        MutableComponent mutablecomponent = effect.getEffect().value().getDisplayName().copy();
        if (effect.getAmplifier() >= 1 && effect.getAmplifier() <= 9) {
            mutablecomponent.append(CommonComponents.SPACE).append(Component.translatable("enchantment.level." + (effect.getAmplifier() + 1)));
        }

        return mutablecomponent;
    }
}
