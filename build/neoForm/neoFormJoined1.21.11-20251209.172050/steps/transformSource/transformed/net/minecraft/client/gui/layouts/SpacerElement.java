package net.minecraft.client.gui.layouts;

import java.util.function.Consumer;
import net.minecraft.client.gui.components.AbstractWidget;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class SpacerElement implements LayoutElement {
    private int x;
    private int y;
    private final int width;
    private final int height;

    public SpacerElement(int width, int height) {
        this(0, 0, width, height);
    }

    public SpacerElement(int x, int y, int width, int height) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
    }

    public static SpacerElement width(int width) {
        return new SpacerElement(width, 0);
    }

    public static SpacerElement height(int height) {
        return new SpacerElement(0, height);
    }

    @Override
    public void setX(int p_265605_) {
        this.x = p_265605_;
    }

    @Override
    public void setY(int p_265406_) {
        this.y = p_265406_;
    }

    @Override
    public int getX() {
        return this.x;
    }

    @Override
    public int getY() {
        return this.y;
    }

    @Override
    public int getWidth() {
        return this.width;
    }

    @Override
    public int getHeight() {
        return this.height;
    }

    @Override
    public void visitWidgets(Consumer<AbstractWidget> p_265477_) {
    }
}
