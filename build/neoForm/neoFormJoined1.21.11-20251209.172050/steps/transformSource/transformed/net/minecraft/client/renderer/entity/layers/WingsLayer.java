package net.minecraft.client.renderer.entity.layers;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.EntityModelSet;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.object.equipment.ElytraModel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.client.renderer.entity.state.HumanoidRenderState;
import net.minecraft.client.resources.model.EquipmentClientInfo;
import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.PlayerSkin;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.equipment.Equippable;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.jspecify.annotations.Nullable;

@OnlyIn(Dist.CLIENT)
public class WingsLayer<S extends HumanoidRenderState, M extends EntityModel<S>> extends RenderLayer<S, M> {
    private final ElytraModel elytraModel;
    private final ElytraModel elytraBabyModel;
    private final EquipmentLayerRenderer equipmentRenderer;

    public WingsLayer(RenderLayerParent<S, M> renderer, EntityModelSet models, EquipmentLayerRenderer equipmentRenderer) {
        super(renderer);
        this.elytraModel = new ElytraModel(models.bakeLayer(ModelLayers.ELYTRA));
        this.elytraBabyModel = new ElytraModel(models.bakeLayer(ModelLayers.ELYTRA_BABY));
        this.equipmentRenderer = equipmentRenderer;
    }

    public void submit(PoseStack p_435137_, SubmitNodeCollector p_434138_, int p_434689_, S p_434317_, float p_433309_, float p_432928_) {
        ItemStack itemstack = p_434317_.chestEquipment;
        Equippable equippable = itemstack.get(DataComponents.EQUIPPABLE);
        if (equippable != null && !equippable.assetId().isEmpty()) {
            Identifier identifier = getPlayerElytraTexture(p_434317_);
            ElytraModel elytramodel = p_434317_.isBaby ? this.elytraBabyModel : this.elytraModel;
            p_435137_.pushPose();
            p_435137_.translate(0.0F, 0.0F, 0.125F);
            this.equipmentRenderer
                .renderLayers(
                    EquipmentClientInfo.LayerType.WINGS,
                    equippable.assetId().get(),
                    elytramodel,
                    p_434317_,
                    itemstack,
                    p_435137_,
                    p_434138_,
                    p_434689_,
                    identifier,
                    p_434317_.outlineColor,
                    0
                );
            p_435137_.popPose();
        }
    }

    private static @Nullable Identifier getPlayerElytraTexture(HumanoidRenderState renderState) {
        if (renderState instanceof AvatarRenderState avatarrenderstate) {
            PlayerSkin playerskin = avatarrenderstate.skin;
            if (playerskin.elytra() != null) {
                return playerskin.elytra().texturePath();
            }

            if (playerskin.cape() != null && avatarrenderstate.showCape) {
                return playerskin.cape().texturePath();
            }
        }

        return null;
    }
}
