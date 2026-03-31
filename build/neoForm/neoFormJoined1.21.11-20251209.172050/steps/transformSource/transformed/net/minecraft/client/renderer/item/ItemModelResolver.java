package net.minecraft.client.renderer.item;

import java.util.function.Function;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.resources.model.ModelManager;
import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ItemOwner;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.jspecify.annotations.Nullable;

@OnlyIn(Dist.CLIENT)
public class ItemModelResolver {
    private final Function<Identifier, ItemModel> modelGetter;
    private final Function<Identifier, ClientItem.Properties> clientProperties;

    public ItemModelResolver(ModelManager modelManager) {
        this.modelGetter = modelManager::getItemModel;
        this.clientProperties = modelManager::getItemProperties;
    }

    public void updateForLiving(ItemStackRenderState renderState, ItemStack stack, ItemDisplayContext displayContext, LivingEntity entity) {
        this.updateForTopItem(renderState, stack, displayContext, entity.level(), entity, entity.getId() + displayContext.ordinal());
    }

    public void updateForNonLiving(ItemStackRenderState renderState, ItemStack stack, ItemDisplayContext displayContext, Entity entity) {
        this.updateForTopItem(renderState, stack, displayContext, entity.level(), null, entity.getId());
    }

    public void updateForTopItem(
        ItemStackRenderState renderState,
        ItemStack stack,
        ItemDisplayContext displayContext,
        @Nullable Level level,
        @Nullable ItemOwner owner,
        int seed
    ) {
        renderState.clear();
        if (!stack.isEmpty()) {
            renderState.displayContext = displayContext;
            this.appendItemLayers(renderState, stack, displayContext, level, owner, seed);
        }
    }

    public void appendItemLayers(
        ItemStackRenderState renderState,
        ItemStack stack,
        ItemDisplayContext displayContext,
        @Nullable Level level,
        @Nullable ItemOwner owner,
        int seed
    ) {
        Identifier identifier = stack.get(DataComponents.ITEM_MODEL);
        if (identifier != null) {
            renderState.setOversizedInGui(this.clientProperties.apply(identifier).oversizedInGui());
            this.modelGetter
                .apply(identifier)
                .update(renderState, stack, this, displayContext, level instanceof ClientLevel clientlevel ? clientlevel : null, owner, seed);
        }
    }

    public boolean shouldPlaySwapAnimation(ItemStack stack) {
        Identifier identifier = stack.get(DataComponents.ITEM_MODEL);
        return identifier == null ? true : this.clientProperties.apply(identifier).handAnimationOnSwap();
    }

    public float swapAnimationScale(ItemStack stack) {
        Identifier identifier = stack.get(DataComponents.ITEM_MODEL);
        return identifier == null ? 1.0F : this.clientProperties.apply(identifier).swapAnimationScale();
    }
}
