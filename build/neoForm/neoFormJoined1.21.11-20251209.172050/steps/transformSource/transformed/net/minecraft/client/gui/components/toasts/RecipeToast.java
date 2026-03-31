package net.minecraft.client.gui.components.toasts;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.context.ContextMap;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.display.RecipeDisplay;
import net.minecraft.world.item.crafting.display.SlotDisplayContext;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class RecipeToast implements Toast {
    private static final Identifier BACKGROUND_SPRITE = Identifier.withDefaultNamespace("toast/recipe");
    private static final long DISPLAY_TIME = 5000L;
    private static final Component TITLE_TEXT = Component.translatable("recipe.toast.title");
    private static final Component DESCRIPTION_TEXT = Component.translatable("recipe.toast.description");
    private final List<RecipeToast.Entry> recipeItems = new ArrayList<>();
    private long lastChanged;
    private boolean changed;
    private Toast.Visibility wantedVisibility = Toast.Visibility.HIDE;
    private int displayedRecipeIndex;

    private RecipeToast() {
    }

    @Override
    public Toast.Visibility getWantedVisibility() {
        return this.wantedVisibility;
    }

    @Override
    public void update(ToastManager p_360532_, long p_360389_) {
        if (this.changed) {
            this.lastChanged = p_360389_;
            this.changed = false;
        }

        if (this.recipeItems.isEmpty()) {
            this.wantedVisibility = Toast.Visibility.HIDE;
        } else {
            this.wantedVisibility = p_360389_ - this.lastChanged >= 5000.0 * p_360532_.getNotificationDisplayTimeMultiplier()
                ? Toast.Visibility.HIDE
                : Toast.Visibility.SHOW;
        }

        this.displayedRecipeIndex = (int)(
            p_360389_ / Math.max(1.0, 5000.0 * p_360532_.getNotificationDisplayTimeMultiplier() / this.recipeItems.size()) % this.recipeItems.size()
        );
    }

    @Override
    public void render(GuiGraphics p_281667_, Font p_364091_, long p_281779_) {
        p_281667_.blitSprite(RenderPipelines.GUI_TEXTURED, BACKGROUND_SPRITE, 0, 0, this.width(), this.height());
        p_281667_.drawString(p_364091_, TITLE_TEXT, 30, 7, -11534256, false);
        p_281667_.drawString(p_364091_, DESCRIPTION_TEXT, 30, 18, -16777216, false);
        RecipeToast.Entry recipetoast$entry = this.recipeItems.get(this.displayedRecipeIndex);
        p_281667_.pose().pushMatrix();
        p_281667_.pose().scale(0.6F, 0.6F);
        p_281667_.renderFakeItem(recipetoast$entry.categoryItem(), 3, 3);
        p_281667_.pose().popMatrix();
        p_281667_.renderFakeItem(recipetoast$entry.unlockedItem(), 8, 8);
    }

    private void addItem(ItemStack categoryItem, ItemStack unlockedItem) {
        this.recipeItems.add(new RecipeToast.Entry(categoryItem, unlockedItem));
        this.changed = true;
    }

    public static void addOrUpdate(ToastManager toastManager, RecipeDisplay recipeDisplay) {
        RecipeToast recipetoast = toastManager.getToast(RecipeToast.class, NO_TOKEN);
        if (recipetoast == null) {
            recipetoast = new RecipeToast();
            toastManager.addToast(recipetoast);
        }

        ContextMap contextmap = SlotDisplayContext.fromLevel(toastManager.getMinecraft().level);
        ItemStack itemstack = recipeDisplay.craftingStation().resolveForFirstStack(contextmap);
        ItemStack itemstack1 = recipeDisplay.result().resolveForFirstStack(contextmap);
        recipetoast.addItem(itemstack, itemstack1);
    }

    @OnlyIn(Dist.CLIENT)
    record Entry(ItemStack categoryItem, ItemStack unlockedItem) {
    }
}
