package net.minecraft.client.gui.render.state;

import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.renderer.item.TrackingItemStackRenderState;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.AABB;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.joml.Matrix3x2f;
import org.jspecify.annotations.Nullable;

@OnlyIn(Dist.CLIENT)
public final class GuiItemRenderState implements ScreenArea {
    private final String name;
    private final Matrix3x2f pose;
    private final TrackingItemStackRenderState itemStackRenderState;
    private final int x;
    private final int y;
    private final @Nullable ScreenRectangle scissorArea;
    private final @Nullable ScreenRectangle oversizedItemBounds;
    private final @Nullable ScreenRectangle bounds;

    public GuiItemRenderState(
        String name, Matrix3x2f pose, TrackingItemStackRenderState itemStackRenderState, int x, int y, @Nullable ScreenRectangle scissorArea
    ) {
        this.name = name;
        this.pose = pose;
        this.itemStackRenderState = itemStackRenderState;
        this.x = x;
        this.y = y;
        this.scissorArea = scissorArea;
        this.oversizedItemBounds = this.itemStackRenderState().isOversizedInGui() ? this.calculateOversizedItemBounds() : null;
        this.bounds = this.calculateBounds(this.oversizedItemBounds != null ? this.oversizedItemBounds : new ScreenRectangle(this.x, this.y, 16, 16));
    }

    private @Nullable ScreenRectangle calculateOversizedItemBounds() {
        AABB aabb = this.itemStackRenderState.getModelBoundingBox();
        int i = Mth.ceil(aabb.getXsize() * 16.0);
        int j = Mth.ceil(aabb.getYsize() * 16.0);
        if (i <= 16 && j <= 16) {
            return null;
        } else {
            float f = (float)(aabb.minX * 16.0);
            float f1 = (float)(aabb.maxY * 16.0);
            int k = Mth.floor(f);
            int l = Mth.floor(f1);
            int i1 = this.x + k + 8;
            int j1 = this.y - l + 8;
            return new ScreenRectangle(i1, j1, i, j);
        }
    }

    private @Nullable ScreenRectangle calculateBounds(ScreenRectangle rectangle) {
        ScreenRectangle screenrectangle = rectangle.transformMaxBounds(this.pose);
        return this.scissorArea != null ? this.scissorArea.intersection(screenrectangle) : screenrectangle;
    }

    public String name() {
        return this.name;
    }

    public Matrix3x2f pose() {
        return this.pose;
    }

    public TrackingItemStackRenderState itemStackRenderState() {
        return this.itemStackRenderState;
    }

    public int x() {
        return this.x;
    }

    public int y() {
        return this.y;
    }

    public @Nullable ScreenRectangle scissorArea() {
        return this.scissorArea;
    }

    public @Nullable ScreenRectangle oversizedItemBounds() {
        return this.oversizedItemBounds;
    }

    @Override
    public @Nullable ScreenRectangle bounds() {
        return this.bounds;
    }
}
