package net.minecraft.client.gui.screens.inventory;

import com.mojang.blaze3d.platform.cursor.CursorTypes;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.CrafterMenu;
import net.minecraft.world.inventory.CrafterSlot;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class CrafterScreen extends AbstractContainerScreen<CrafterMenu> {
    private static final Identifier DISABLED_SLOT_LOCATION_SPRITE = Identifier.withDefaultNamespace("container/crafter/disabled_slot");
    private static final Identifier POWERED_REDSTONE_LOCATION_SPRITE = Identifier.withDefaultNamespace("container/crafter/powered_redstone");
    private static final Identifier UNPOWERED_REDSTONE_LOCATION_SPRITE = Identifier.withDefaultNamespace("container/crafter/unpowered_redstone");
    private static final Identifier CONTAINER_LOCATION = Identifier.withDefaultNamespace("textures/gui/container/crafter.png");
    private static final Component DISABLED_SLOT_TOOLTIP = Component.translatable("gui.togglable_slot");
    private final Player player;

    public CrafterScreen(CrafterMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.player = playerInventory.player;
    }

    @Override
    protected void init() {
        super.init();
        this.titleLabelX = (this.imageWidth - this.font.width(this.title)) / 2;
    }

    @Override
    protected void slotClicked(Slot p_307465_, int p_307203_, int p_307325_, ClickType p_307680_) {
        if (p_307465_ instanceof CrafterSlot && !p_307465_.hasItem() && !this.player.isSpectator()) {
            switch (p_307680_) {
                case PICKUP:
                    if (this.menu.isSlotDisabled(p_307203_)) {
                        this.enableSlot(p_307203_);
                    } else if (this.menu.getCarried().isEmpty()) {
                        this.disableSlot(p_307203_);
                    }
                    break;
                case SWAP:
                    ItemStack itemstack = this.player.getInventory().getItem(p_307325_);
                    if (this.menu.isSlotDisabled(p_307203_) && !itemstack.isEmpty()) {
                        this.enableSlot(p_307203_);
                    }
            }
        }

        super.slotClicked(p_307465_, p_307203_, p_307325_, p_307680_);
    }

    private void enableSlot(int slot) {
        this.updateSlotState(slot, true);
    }

    private void disableSlot(int slot) {
        this.updateSlotState(slot, false);
    }

    private void updateSlotState(int slot, boolean state) {
        this.menu.setSlotState(slot, state);
        super.handleSlotStateChanged(slot, this.menu.containerId, state);
        float f = state ? 1.0F : 0.75F;
        this.player.playSound(SoundEvents.UI_BUTTON_CLICK.value(), 0.4F, f);
    }

    @Override
    public void renderSlot(GuiGraphics p_307608_, Slot p_307570_, int p_470762_, int p_470617_) {
        if (p_307570_ instanceof CrafterSlot crafterslot) {
            if (this.menu.isSlotDisabled(p_307570_.index)) {
                this.renderDisabledSlot(p_307608_, crafterslot);
            } else {
                super.renderSlot(p_307608_, p_307570_, p_470762_, p_470617_);
            }

            int i = this.leftPos + crafterslot.x - 2;
            int j = this.topPos + crafterslot.y - 2;
            if (p_470762_ > i && p_470617_ > j && p_470762_ < i + 19 && p_470617_ < j + 19) {
                p_307608_.requestCursor(CursorTypes.POINTING_HAND);
            }
        } else {
            super.renderSlot(p_307608_, p_307570_, p_470762_, p_470617_);
        }
    }

    private void renderDisabledSlot(GuiGraphics guiGraphics, CrafterSlot slot) {
        guiGraphics.blitSprite(RenderPipelines.GUI_TEXTURED, DISABLED_SLOT_LOCATION_SPRITE, slot.x - 1, slot.y - 1, 18, 18);
    }

    @Override
    public void render(GuiGraphics p_307196_, int p_307586_, int p_307288_, float p_307623_) {
        super.render(p_307196_, p_307586_, p_307288_, p_307623_);
        this.renderRedstone(p_307196_);
        this.renderTooltip(p_307196_, p_307586_, p_307288_);
        if (this.hoveredSlot instanceof CrafterSlot
            && !this.menu.isSlotDisabled(this.hoveredSlot.index)
            && this.menu.getCarried().isEmpty()
            && !this.hoveredSlot.hasItem()
            && !this.player.isSpectator()) {
            p_307196_.setTooltipForNextFrame(this.font, DISABLED_SLOT_TOOLTIP, p_307586_, p_307288_);
        }
    }

    private void renderRedstone(GuiGraphics guiGraphics) {
        int i = this.width / 2 + 9;
        int j = this.height / 2 - 48;
        Identifier identifier;
        if (this.menu.isPowered()) {
            identifier = POWERED_REDSTONE_LOCATION_SPRITE;
        } else {
            identifier = UNPOWERED_REDSTONE_LOCATION_SPRITE;
        }

        guiGraphics.blitSprite(RenderPipelines.GUI_TEXTURED, identifier, i, j, 16, 16);
    }

    @Override
    protected void renderBg(GuiGraphics p_307513_, float p_307580_, int p_307561_, int p_307248_) {
        int i = (this.width - this.imageWidth) / 2;
        int j = (this.height - this.imageHeight) / 2;
        p_307513_.blit(RenderPipelines.GUI_TEXTURED, CONTAINER_LOCATION, i, j, 0.0F, 0.0F, this.imageWidth, this.imageHeight, 256, 256);
    }
}
