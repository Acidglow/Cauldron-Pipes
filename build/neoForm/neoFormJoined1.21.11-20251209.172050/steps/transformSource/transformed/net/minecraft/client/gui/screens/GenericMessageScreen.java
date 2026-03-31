package net.minecraft.client.gui.screens;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.FocusableTextWidget;
import net.minecraft.network.chat.Component;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.jspecify.annotations.Nullable;

@OnlyIn(Dist.CLIENT)
public class GenericMessageScreen extends Screen {
    private @Nullable FocusableTextWidget textWidget;

    public GenericMessageScreen(Component p_331916_) {
        super(p_331916_);
    }

    @Override
    protected void init() {
        this.textWidget = this.addRenderableWidget(FocusableTextWidget.builder(this.title, this.font, 12).textWidth(this.font.width(this.title)).build());
        this.repositionElements();
    }

    @Override
    protected void repositionElements() {
        if (this.textWidget != null) {
            this.textWidget.setPosition(this.width / 2 - this.textWidget.getWidth() / 2, this.height / 2 - 9 / 2);
        }
    }

    @Override
    public boolean shouldCloseOnEsc() {
        return false;
    }

    @Override
    protected boolean shouldNarrateNavigation() {
        return false;
    }

    @Override
    public void renderBackground(GuiGraphics p_330526_, int p_330256_, int p_331601_, float p_331163_) {
        this.renderPanorama(p_330526_, p_331163_);
        this.renderBlurredBackground(p_330526_);
        this.renderMenuBackground(p_330526_);
    }
}
