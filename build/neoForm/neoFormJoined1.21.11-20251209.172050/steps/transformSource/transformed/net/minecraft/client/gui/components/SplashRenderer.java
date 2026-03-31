package net.minecraft.client.gui.components;

import net.minecraft.client.gui.ActiveTextCollector;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.TextAlignment;
import net.minecraft.client.resources.SplashManager;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.util.Util;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.joml.Matrix3x2f;

@OnlyIn(Dist.CLIENT)
public class SplashRenderer {
    public static final SplashRenderer CHRISTMAS = new SplashRenderer(SplashManager.CHRISTMAS);
    public static final SplashRenderer NEW_YEAR = new SplashRenderer(SplashManager.NEW_YEAR);
    public static final SplashRenderer HALLOWEEN = new SplashRenderer(SplashManager.HALLOWEEN);
    private static final int WIDTH_OFFSET = 123;
    private static final int HEIGH_OFFSET = 69;
    private static final float TEXT_ANGLE = (float) (-Math.PI / 9);
    private final Component splash;

    public SplashRenderer(Component splash) {
        this.splash = splash;
    }

    public void render(GuiGraphics guiGraphics, int width, Font font, float fade) {
        int i = font.width(this.splash);
        ActiveTextCollector activetextcollector = guiGraphics.textRenderer();
        float f = 1.8F - Mth.abs(Mth.sin((float)(Util.getMillis() % 1000L) / 1000.0F * (float) (Math.PI * 2)) * 0.1F);
        float f1 = f * 100.0F / (i + 32);
        Matrix3x2f matrix3x2f = new Matrix3x2f(activetextcollector.defaultParameters().pose())
            .translate(width / 2.0F + 123.0F, 69.0F)
            .rotate((float) (-Math.PI / 9))
            .scale(f1);
        ActiveTextCollector.Parameters activetextcollector$parameters = activetextcollector.defaultParameters().withOpacity(fade).withPose(matrix3x2f);
        activetextcollector.accept(TextAlignment.LEFT, -i / 2, -8, activetextcollector$parameters, this.splash);
    }
}
