package net.minecraft.client.renderer.entity.layers;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.animal.llama.LlamaModel;
import net.minecraft.client.model.geom.EntityModelSet;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.state.LlamaRenderState;
import net.minecraft.client.resources.model.EquipmentClientInfo;
import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.equipment.EquipmentAsset;
import net.minecraft.world.item.equipment.EquipmentAssets;
import net.minecraft.world.item.equipment.Equippable;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class LlamaDecorLayer extends RenderLayer<LlamaRenderState, LlamaModel> {
    private final LlamaModel adultModel;
    private final LlamaModel babyModel;
    private final EquipmentLayerRenderer equipmentRenderer;

    public LlamaDecorLayer(RenderLayerParent<LlamaRenderState, LlamaModel> renderer, EntityModelSet models, EquipmentLayerRenderer equipmentRenderer) {
        super(renderer);
        this.equipmentRenderer = equipmentRenderer;
        this.adultModel = new LlamaModel(models.bakeLayer(ModelLayers.LLAMA_DECOR));
        this.babyModel = new LlamaModel(models.bakeLayer(ModelLayers.LLAMA_BABY_DECOR));
    }

    public void submit(PoseStack p_117232_, SubmitNodeCollector p_432905_, int p_117234_, LlamaRenderState p_364326_, float p_117236_, float p_117237_) {
        ItemStack itemstack = p_364326_.bodyItem;
        Equippable equippable = itemstack.get(DataComponents.EQUIPPABLE);
        if (equippable != null && equippable.assetId().isPresent()) {
            this.renderEquipment(p_117232_, p_432905_, p_364326_, itemstack, equippable.assetId().get(), p_117234_);
        } else if (p_364326_.isTraderLlama) {
            this.renderEquipment(p_117232_, p_432905_, p_364326_, ItemStack.EMPTY, EquipmentAssets.TRADER_LLAMA, p_117234_);
        }
    }

    private void renderEquipment(
        PoseStack poseStack,
        SubmitNodeCollector nodeCollector,
        LlamaRenderState renderState,
        ItemStack item,
        ResourceKey<EquipmentAsset> equipmentAseet,
        int packedLight
    ) {
        LlamaModel llamamodel = renderState.isBaby ? this.babyModel : this.adultModel;
        this.equipmentRenderer
            .renderLayers(
                EquipmentClientInfo.LayerType.LLAMA_BODY, equipmentAseet, llamamodel, renderState, item, poseStack, nodeCollector, packedLight, renderState.outlineColor
            );
    }
}
