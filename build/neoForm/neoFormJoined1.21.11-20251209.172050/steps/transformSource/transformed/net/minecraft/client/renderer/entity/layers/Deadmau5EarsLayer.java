package net.minecraft.client.renderer.entity.layers;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.EntityModelSet;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.player.PlayerEarsModel;
import net.minecraft.client.model.player.PlayerModel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class Deadmau5EarsLayer extends RenderLayer<AvatarRenderState, PlayerModel> {
    private final HumanoidModel<AvatarRenderState> model;

    public Deadmau5EarsLayer(RenderLayerParent<AvatarRenderState, PlayerModel> renderer, EntityModelSet modelSet) {
        super(renderer);
        this.model = new PlayerEarsModel(modelSet.bakeLayer(ModelLayers.PLAYER_EARS));
    }

    public void submit(PoseStack p_433932_, SubmitNodeCollector p_435533_, int p_434365_, AvatarRenderState p_446247_, float p_435134_, float p_435800_) {
        if (p_446247_.showExtraEars && !p_446247_.isInvisible) {
            int i = LivingEntityRenderer.getOverlayCoords(p_446247_, 0.0F);
            p_435533_.submitModel(
                this.model, p_446247_, p_433932_, RenderTypes.entitySolid(p_446247_.skin.body().texturePath()), p_434365_, i, p_446247_.outlineColor, null
            );
        }
    }
}
