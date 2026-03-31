package net.minecraft.client.gui.screens.inventory;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractMountInventoryMenu;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.jspecify.annotations.Nullable;

@OnlyIn(Dist.CLIENT)
public abstract class AbstractMountInventoryScreen<T extends AbstractMountInventoryMenu> extends AbstractContainerScreen<T> {
    protected final int inventoryColumns;
    protected float xMouse;
    protected float yMouse;
    protected LivingEntity mount;

    public AbstractMountInventoryScreen(T menu, Inventory playerInventory, Component title, int inventoryColumns, LivingEntity mount) {
        super(menu, playerInventory, title);
        this.inventoryColumns = inventoryColumns;
        this.mount = mount;
    }

    @Override
    protected void renderBg(GuiGraphics p_470698_, float p_470831_, int p_470675_, int p_470799_) {
        int i = (this.width - this.imageWidth) / 2;
        int j = (this.height - this.imageHeight) / 2;
        p_470698_.blit(RenderPipelines.GUI_TEXTURED, this.getBackgroundTextureLocation(), i, j, 0.0F, 0.0F, this.imageWidth, this.imageHeight, 256, 256);
        if (this.inventoryColumns > 0 && this.getChestSlotsSpriteLocation() != null) {
            p_470698_.blitSprite(RenderPipelines.GUI_TEXTURED, this.getChestSlotsSpriteLocation(), 90, 54, 0, 0, i + 79, j + 17, this.inventoryColumns * 18, 54);
        }

        if (this.shouldRenderSaddleSlot()) {
            this.drawSlot(p_470698_, i + 7, j + 35 - 18);
        }

        if (this.shouldRenderArmorSlot()) {
            this.drawSlot(p_470698_, i + 7, j + 35);
        }

        InventoryScreen.renderEntityInInventoryFollowsMouse(p_470698_, i + 26, j + 18, i + 78, j + 70, 17, 0.25F, this.xMouse, this.yMouse, this.mount);
    }

    protected void drawSlot(GuiGraphics guiGraphics, int x, int y) {
        guiGraphics.blitSprite(RenderPipelines.GUI_TEXTURED, this.getSlotSpriteLocation(), x, y, 18, 18);
    }

    @Override
    public void render(GuiGraphics p_470541_, int p_470731_, int p_470784_, float p_470749_) {
        this.xMouse = p_470731_;
        this.yMouse = p_470784_;
        super.render(p_470541_, p_470731_, p_470784_, p_470749_);
        this.renderTooltip(p_470541_, p_470731_, p_470784_);
    }

    protected abstract Identifier getBackgroundTextureLocation();

    protected abstract Identifier getSlotSpriteLocation();

    protected abstract @Nullable Identifier getChestSlotsSpriteLocation();

    protected abstract boolean shouldRenderSaddleSlot();

    protected abstract boolean shouldRenderArmorSlot();
}
