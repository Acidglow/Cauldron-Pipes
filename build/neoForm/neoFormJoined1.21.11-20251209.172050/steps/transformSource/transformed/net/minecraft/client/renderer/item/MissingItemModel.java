package net.minecraft.client.renderer.item;

import com.google.common.base.Suppliers;
import java.util.List;
import java.util.function.Supplier;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.world.entity.ItemOwner;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.joml.Vector3fc;
import org.jspecify.annotations.Nullable;

@OnlyIn(Dist.CLIENT)
public class MissingItemModel implements ItemModel {
    private final List<BakedQuad> quads;
    private final Supplier<Vector3fc[]> extents;
    private final ModelRenderProperties properties;

    public MissingItemModel(List<BakedQuad> quads, ModelRenderProperties properties) {
        this.quads = quads;
        this.properties = properties;
        this.extents = Suppliers.memoize(() -> BlockModelWrapper.computeExtents(this.quads));
    }

    @Override
    public void update(
        ItemStackRenderState p_386627_,
        ItemStack p_388292_,
        ItemModelResolver p_388302_,
        ItemDisplayContext p_388518_,
        @Nullable ClientLevel p_387367_,
        @Nullable ItemOwner p_435350_,
        int p_388913_
    ) {
        p_386627_.appendModelIdentityElement(this);
        ItemStackRenderState.LayerRenderState itemstackrenderstate$layerrenderstate = p_386627_.newLayer();
        itemstackrenderstate$layerrenderstate.setRenderType(Sheets.cutoutBlockSheet());
        this.properties.applyToLayer(itemstackrenderstate$layerrenderstate, p_388518_);
        itemstackrenderstate$layerrenderstate.setExtents(this.extents);
        itemstackrenderstate$layerrenderstate.prepareQuadList().addAll(this.quads);
    }
}
