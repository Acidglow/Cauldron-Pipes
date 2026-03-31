package net.minecraft.client.gui.components;

import net.minecraft.client.gui.ActiveTextCollector;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.jspecify.annotations.Nullable;

@OnlyIn(Dist.CLIENT)
public abstract class SpriteIconButton extends Button {
    protected final WidgetSprites sprite;
    protected final int spriteWidth;
    protected final int spriteHeight;

    SpriteIconButton(
        int width,
        int height,
        Component message,
        int spriteWidth,
        int spriteHeight,
        WidgetSprites sprite,
        Button.OnPress onPress,
        @Nullable Component tooltip,
        Button.@Nullable CreateNarration createNarration
    ) {
        super(0, 0, width, height, message, onPress, createNarration == null ? DEFAULT_NARRATION : createNarration);
        if (tooltip != null) {
            this.setTooltip(Tooltip.create(tooltip));
        }

        this.spriteWidth = spriteWidth;
        this.spriteHeight = spriteHeight;
        this.sprite = sprite;
    }

    protected void renderSprite(GuiGraphics guiGraphics, int x, int y) {
        guiGraphics.blitSprite(
            RenderPipelines.GUI_TEXTURED,
            this.sprite.get(this.isActive(), this.isHoveredOrFocused()),
            x,
            y,
            this.spriteWidth,
            this.spriteHeight,
            this.alpha
        );
    }

    public static SpriteIconButton.Builder builder(Component message, Button.OnPress onPress, boolean iconOnly) {
        return new SpriteIconButton.Builder(message, onPress, iconOnly);
    }

    @OnlyIn(Dist.CLIENT)
    public static class Builder {
        private final Component message;
        private final Button.OnPress onPress;
        private final boolean iconOnly;
        private int width = 150;
        private int height = 20;
        private @Nullable WidgetSprites sprite;
        private int spriteWidth;
        private int spriteHeight;
        private @Nullable Component tooltip;
        private Button.@Nullable CreateNarration narration;

        public Builder(Component message, Button.OnPress onPress, boolean iconOnly) {
            this.message = message;
            this.onPress = onPress;
            this.iconOnly = iconOnly;
        }

        public SpriteIconButton.Builder width(int width) {
            this.width = width;
            return this;
        }

        public SpriteIconButton.Builder size(int width, int height) {
            this.width = width;
            this.height = height;
            return this;
        }

        public SpriteIconButton.Builder sprite(Identifier sprite, int spriteWidth, int spriteHeight) {
            this.sprite = new WidgetSprites(sprite);
            this.spriteWidth = spriteWidth;
            this.spriteHeight = spriteHeight;
            return this;
        }

        public SpriteIconButton.Builder sprite(WidgetSprites sprite, int spriteWidth, int spriteHeight) {
            this.sprite = sprite;
            this.spriteWidth = spriteWidth;
            this.spriteHeight = spriteHeight;
            return this;
        }

        public SpriteIconButton.Builder withTootip() {
            this.tooltip = this.message;
            return this;
        }

        public SpriteIconButton.Builder narration(Button.CreateNarration narration) {
            this.narration = narration;
            return this;
        }

        public SpriteIconButton build() {
            if (this.sprite == null) {
                throw new IllegalStateException("Sprite not set");
            } else {
                return (SpriteIconButton)(this.iconOnly
                    ? new SpriteIconButton.CenteredIcon(
                        this.width, this.height, this.message, this.spriteWidth, this.spriteHeight, this.sprite, this.onPress, this.tooltip, this.narration
                    )
                    : new SpriteIconButton.TextAndIcon(
                        this.width, this.height, this.message, this.spriteWidth, this.spriteHeight, this.sprite, this.onPress, this.tooltip, this.narration
                    ));
            }
        }
    }

    @OnlyIn(Dist.CLIENT)
    public static class CenteredIcon extends SpriteIconButton {
        protected CenteredIcon(
            int p_295914_,
            int p_294852_,
            Component p_295609_,
            int p_294922_,
            int p_296462_,
            WidgetSprites p_440019_,
            Button.OnPress p_294427_,
            @Nullable Component p_440407_,
            Button.@Nullable CreateNarration p_330653_
        ) {
            super(p_295914_, p_294852_, p_295609_, p_294922_, p_296462_, p_440019_, p_294427_, p_440407_, p_330653_);
        }

        @Override
        public void renderContents(GuiGraphics p_458138_, int p_457527_, int p_457926_, float p_457737_) {
            this.renderDefaultSprite(p_458138_);
            int i = this.getX() + this.getWidth() / 2 - this.spriteWidth / 2;
            int j = this.getY() + this.getHeight() / 2 - this.spriteHeight / 2;
            this.renderSprite(p_458138_, i, j);
        }
    }

    @OnlyIn(Dist.CLIENT)
    public static class TextAndIcon extends SpriteIconButton {
        protected TextAndIcon(
            int p_296442_,
            int p_294340_,
            Component p_296265_,
            int p_294900_,
            int p_295900_,
            WidgetSprites p_439719_,
            Button.OnPress p_295566_,
            @Nullable Component p_439158_,
            Button.@Nullable CreateNarration p_330735_
        ) {
            super(p_296442_, p_294340_, p_296265_, p_294900_, p_295900_, p_439719_, p_295566_, p_439158_, p_330735_);
        }

        @Override
        public void renderContents(GuiGraphics p_458096_, int p_457648_, int p_457987_, float p_457713_) {
            this.renderDefaultSprite(p_458096_);
            int i = this.getX() + 2;
            int j = this.getX() + this.getWidth() - this.spriteWidth - 4;
            int k = this.getX() + this.getWidth() / 2;
            ActiveTextCollector activetextcollector = p_458096_.textRendererForWidget(this, GuiGraphics.HoveredTextEffects.NONE);
            activetextcollector.acceptScrolling(this.getMessage(), k, i, j, this.getY(), this.getY() + this.getHeight());
            int l = this.getX() + this.getWidth() - this.spriteWidth - 2;
            int i1 = this.getY() + this.getHeight() / 2 - this.spriteHeight / 2;
            this.renderSprite(p_458096_, l, i1);
        }
    }
}
