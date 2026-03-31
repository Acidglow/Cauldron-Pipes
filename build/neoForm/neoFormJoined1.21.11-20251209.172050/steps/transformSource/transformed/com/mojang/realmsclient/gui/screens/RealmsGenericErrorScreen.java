package com.mojang.realmsclient.gui.screens;

import com.mojang.realmsclient.client.RealmsError;
import com.mojang.realmsclient.exception.RealmsServiceException;
import net.minecraft.client.gui.ActiveTextCollector;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.TextAlignment;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.MultiLineLabel;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentUtils;
import net.minecraft.network.chat.Style;
import net.minecraft.realms.RealmsScreen;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class RealmsGenericErrorScreen extends RealmsScreen {
    private static final Component GENERIC_TITLE = Component.translatable("mco.errorMessage.generic");
    private final Screen nextScreen;
    private final Component detail;
    private MultiLineLabel splitDetail = MultiLineLabel.EMPTY;

    public RealmsGenericErrorScreen(RealmsServiceException serviceException, Screen nextScreen) {
        this(RealmsGenericErrorScreen.ErrorMessage.forServiceError(serviceException), nextScreen);
    }

    public RealmsGenericErrorScreen(Component detail, Screen nextScreen) {
        this(new RealmsGenericErrorScreen.ErrorMessage(GENERIC_TITLE, detail), nextScreen);
    }

    public RealmsGenericErrorScreen(Component title, Component detail, Screen message) {
        this(new RealmsGenericErrorScreen.ErrorMessage(title, detail), message);
    }

    private RealmsGenericErrorScreen(RealmsGenericErrorScreen.ErrorMessage error, Screen nextScreen) {
        super(error.title);
        this.nextScreen = nextScreen;
        this.detail = ComponentUtils.mergeStyles(error.detail, Style.EMPTY.withColor(-2142128));
    }

    @Override
    public void init() {
        this.addRenderableWidget(
            Button.builder(CommonComponents.GUI_OK, p_315811_ -> this.onClose()).bounds(this.width / 2 - 100, this.height - 52, 200, 20).build()
        );
        this.splitDetail = MultiLineLabel.create(this.font, this.detail, this.width * 3 / 4);
    }

    @Override
    public void onClose() {
        this.minecraft.setScreen(this.nextScreen);
    }

    @Override
    public Component getNarrationMessage() {
        return CommonComponents.joinForNarration(super.getNarrationMessage(), this.detail);
    }

    @Override
    public boolean keyPressed(net.minecraft.client.input.KeyEvent keyEvent) {
        if (keyEvent.isEscape()) {
            minecraft.setScreen(this.nextScreen);
            return true;
        }
        return super.keyPressed(keyEvent);
    }

    @Override
    public void render(GuiGraphics p_283497_, int p_88680_, int p_88681_, float p_88682_) {
        super.render(p_283497_, p_88680_, p_88681_, p_88682_);
        p_283497_.drawCenteredString(this.font, this.title, this.width / 2, 80, -1);
        ActiveTextCollector activetextcollector = p_283497_.textRenderer();
        this.splitDetail.visitLines(TextAlignment.CENTER, this.width / 2, 100, 9, activetextcollector);
    }

    @OnlyIn(Dist.CLIENT)
    record ErrorMessage(Component title, Component detail) {
        static RealmsGenericErrorScreen.ErrorMessage forServiceError(RealmsServiceException exception) {
            RealmsError realmserror = exception.realmsError;
            return new RealmsGenericErrorScreen.ErrorMessage(
                Component.translatable("mco.errorMessage.realmsService.realmsError", realmserror.errorCode()), realmserror.errorMessage()
            );
        }
    }
}
