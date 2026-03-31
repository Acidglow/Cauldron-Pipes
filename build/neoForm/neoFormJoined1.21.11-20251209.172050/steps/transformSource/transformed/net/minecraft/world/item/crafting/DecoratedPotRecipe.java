package net.minecraft.world.item.crafting;

import net.minecraft.core.HolderLookup;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.DecoratedPotBlockEntity;
import net.minecraft.world.level.block.entity.PotDecorations;

public class DecoratedPotRecipe extends CustomRecipe {
    public DecoratedPotRecipe(CraftingBookCategory p_273056_) {
        super(p_273056_);
    }

    private static ItemStack back(CraftingInput input) {
        return input.getItem(1, 0);
    }

    private static ItemStack left(CraftingInput input) {
        return input.getItem(0, 1);
    }

    private static ItemStack right(CraftingInput input) {
        return input.getItem(2, 1);
    }

    private static ItemStack front(CraftingInput input) {
        return input.getItem(1, 2);
    }

    public boolean matches(CraftingInput p_344915_, Level p_272812_) {
        return p_344915_.width() == 3 && p_344915_.height() == 3 && p_344915_.ingredientCount() == 4
            ? back(p_344915_).is(ItemTags.DECORATED_POT_INGREDIENTS)
                && left(p_344915_).is(ItemTags.DECORATED_POT_INGREDIENTS)
                && right(p_344915_).is(ItemTags.DECORATED_POT_INGREDIENTS)
                && front(p_344915_).is(ItemTags.DECORATED_POT_INGREDIENTS)
            : false;
    }

    public ItemStack assemble(CraftingInput p_345761_, HolderLookup.Provider p_335840_) {
        PotDecorations potdecorations = new PotDecorations(
            back(p_345761_).getItem(), left(p_345761_).getItem(), right(p_345761_).getItem(), front(p_345761_).getItem()
        );
        return DecoratedPotBlockEntity.createDecoratedPotItem(potdecorations);
    }

    @Override
    public RecipeSerializer<DecoratedPotRecipe> getSerializer() {
        return RecipeSerializer.DECORATED_POT_RECIPE;
    }
}
