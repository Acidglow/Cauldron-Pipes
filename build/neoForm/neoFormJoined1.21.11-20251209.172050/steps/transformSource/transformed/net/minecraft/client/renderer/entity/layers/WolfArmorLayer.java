package net.minecraft.client.renderer.entity.layers;

import com.mojang.blaze3d.vertex.PoseStack;
import java.util.Map;
import net.minecraft.client.model.Model;
import net.minecraft.client.model.animal.wolf.WolfModel;
import net.minecraft.client.model.geom.EntityModelSet;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.state.WolfRenderState;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.model.EquipmentClientInfo;
import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.Crackiness;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.equipment.Equippable;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class WolfArmorLayer extends RenderLayer<WolfRenderState, WolfModel> {
    private final WolfModel adultModel;
    private final WolfModel babyModel;
    private final EquipmentLayerRenderer equipmentRenderer;
    private static final Map<Crackiness.Level, Identifier> ARMOR_CRACK_LOCATIONS = Map.of(
        Crackiness.Level.LOW,
        Identifier.withDefaultNamespace("textures/entity/wolf/wolf_armor_crackiness_low.png"),
        Crackiness.Level.MEDIUM,
        Identifier.withDefaultNamespace("textures/entity/wolf/wolf_armor_crackiness_medium.png"),
        Crackiness.Level.HIGH,
        Identifier.withDefaultNamespace("textures/entity/wolf/wolf_armor_crackiness_high.png")
    );

    public WolfArmorLayer(RenderLayerParent<WolfRenderState, WolfModel> renderer, EntityModelSet entityModels, EquipmentLayerRenderer equipmentRenderer) {
        super(renderer);
        this.adultModel = new WolfModel(entityModels.bakeLayer(ModelLayers.WOLF_ARMOR));
        this.babyModel = new WolfModel(entityModels.bakeLayer(ModelLayers.WOLF_BABY_ARMOR));
        this.equipmentRenderer = equipmentRenderer;
    }

    public void submit(PoseStack p_436050_, SubmitNodeCollector p_434212_, int p_433618_, WolfRenderState p_435660_, float p_435015_, float p_434923_) {
        ItemStack itemstack = p_435660_.bodyArmorItem;
        Equippable equippable = itemstack.get(DataComponents.EQUIPPABLE);
        if (equippable != null && !equippable.assetId().isEmpty()) {
            WolfModel wolfmodel = p_435660_.isBaby ? this.babyModel : this.adultModel;
            this.equipmentRenderer
                .renderLayers(
                    EquipmentClientInfo.LayerType.WOLF_BODY,
                    equippable.assetId().get(),
                    wolfmodel,
                    p_435660_,
                    itemstack,
                    p_436050_,
                    p_434212_,
                    p_433618_,
                    p_435660_.outlineColor
                );
            this.maybeRenderCracks(p_436050_, p_434212_, p_433618_, itemstack, wolfmodel, p_435660_);
        }
    }

    private void maybeRenderCracks(
        PoseStack poseStack, SubmitNodeCollector nodeCollector, int packedLight, ItemStack armorStack, Model<WolfRenderState> model, WolfRenderState renderState
    ) {
        Crackiness.Level crackiness$level = Crackiness.WOLF_ARMOR.byDamage(armorStack);
        if (crackiness$level != Crackiness.Level.NONE) {
            Identifier identifier = ARMOR_CRACK_LOCATIONS.get(crackiness$level);
            nodeCollector.submitModel(
                model, renderState, poseStack, RenderTypes.armorTranslucent(identifier), packedLight, OverlayTexture.NO_OVERLAY, renderState.outlineColor, null
            );
        }
    }
}
