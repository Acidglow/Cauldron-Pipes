package net.minecraft.client.gui.components;

import net.minecraft.client.gui.ComponentPath;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.navigation.FocusNavigationEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.sounds.SoundManager;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.resources.Identifier;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.jspecify.annotations.Nullable;

@OnlyIn(Dist.CLIENT)
public abstract class ImageWidget extends AbstractWidget {
    ImageWidget(int x, int y, int width, int height) {
        super(x, y, width, height, CommonComponents.EMPTY);
    }

    public static ImageWidget texture(int width, int height, Identifier texture, int textureWidth, int textureHeight) {
        return new ImageWidget.Texture(0, 0, width, height, texture, textureWidth, textureHeight);
    }

    public static ImageWidget sprite(int width, int height, Identifier sprite) {
        return new ImageWidget.Sprite(0, 0, width, height, sprite);
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput p_275454_) {
    }

    @Override
    public void playDownSound(SoundManager p_295108_) {
    }

    @Override
    public boolean isActive() {
        return false;
    }

    public abstract void updateResource(Identifier resource);

    @Override
    public @Nullable ComponentPath nextFocusPath(FocusNavigationEvent p_296129_) {
        return null;
    }

    @OnlyIn(Dist.CLIENT)
    static class Sprite extends ImageWidget {
        private Identifier sprite;

        public Sprite(int x, int y, int width, int height, Identifier sprite) {
            super(x, y, width, height);
            this.sprite = sprite;
        }

        @Override
        public void renderWidget(GuiGraphics p_295869_, int p_295287_, int p_294110_, float p_296031_) {
            p_295869_.blitSprite(RenderPipelines.GUI_TEXTURED, this.sprite, this.getX(), this.getY(), this.getWidth(), this.getHeight());
        }

        @Override
        public void updateResource(Identifier p_469954_) {
            this.sprite = p_469954_;
        }
    }

    @OnlyIn(Dist.CLIENT)
    static class Texture extends ImageWidget {
        private Identifier texture;
        private final int textureWidth;
        private final int textureHeight;

        public Texture(int x, int y, int width, int height, Identifier texture, int textureWidth, int textureHeight) {
            super(x, y, width, height);
            this.texture = texture;
            this.textureWidth = textureWidth;
            this.textureHeight = textureHeight;
        }

        @Override
        protected void renderWidget(GuiGraphics p_294145_, int p_294755_, int p_294985_, float p_294245_) {
            p_294145_.blit(
                RenderPipelines.GUI_TEXTURED,
                this.texture,
                this.getX(),
                this.getY(),
                0.0F,
                0.0F,
                this.getWidth(),
                this.getHeight(),
                this.textureWidth,
                this.textureHeight
            );
        }

        @Override
        public void updateResource(Identifier p_469405_) {
            this.texture = p_469405_;
        }
    }
}
