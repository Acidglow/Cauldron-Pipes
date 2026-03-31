package net.minecraft.client.renderer.entity;

import net.minecraft.client.model.animal.camel.CamelModel;
import net.minecraft.client.model.animal.camel.CamelSaddleModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.layers.SimpleEquipmentLayer;
import net.minecraft.client.renderer.entity.state.CamelRenderState;
import net.minecraft.client.resources.model.EquipmentClientInfo;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.animal.camel.Camel;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class CamelRenderer extends AgeableMobRenderer<Camel, CamelRenderState, CamelModel> {
    private static final Identifier CAMEL_LOCATION = Identifier.withDefaultNamespace("textures/entity/camel/camel.png");

    public CamelRenderer(EntityRendererProvider.Context p_251790_) {
        super(p_251790_, new CamelModel(p_251790_.bakeLayer(ModelLayers.CAMEL)), new CamelModel(p_251790_.bakeLayer(ModelLayers.CAMEL_BABY)), 0.7F);
        this.addLayer(this.createCamelSaddleLayer(p_251790_));
    }

    protected SimpleEquipmentLayer<CamelRenderState, CamelModel, CamelSaddleModel> createCamelSaddleLayer(EntityRendererProvider.Context context) {
        return new SimpleEquipmentLayer<>(
            this,
            context.getEquipmentRenderer(),
            EquipmentClientInfo.LayerType.CAMEL_SADDLE,
            p_397528_ -> p_397528_.saddle,
            new CamelSaddleModel(context.bakeLayer(ModelLayers.CAMEL_SADDLE)),
            new CamelSaddleModel(context.bakeLayer(ModelLayers.CAMEL_BABY_SADDLE))
        );
    }

    public Identifier getTextureLocation(CamelRenderState p_466891_) {
        return CAMEL_LOCATION;
    }

    public CamelRenderState createRenderState() {
        return new CamelRenderState();
    }

    public void extractRenderState(Camel p_362547_, CamelRenderState p_362381_, float p_360896_) {
        super.extractRenderState(p_362547_, p_362381_, p_360896_);
        p_362381_.saddle = p_362547_.getItemBySlot(EquipmentSlot.SADDLE).copy();
        p_362381_.isRidden = p_362547_.isVehicle();
        p_362381_.jumpCooldown = Math.max(p_362547_.getJumpCooldown() - p_360896_, 0.0F);
        p_362381_.sitAnimationState.copyFrom(p_362547_.sitAnimationState);
        p_362381_.sitPoseAnimationState.copyFrom(p_362547_.sitPoseAnimationState);
        p_362381_.sitUpAnimationState.copyFrom(p_362547_.sitUpAnimationState);
        p_362381_.idleAnimationState.copyFrom(p_362547_.idleAnimationState);
        p_362381_.dashAnimationState.copyFrom(p_362547_.dashAnimationState);
    }
}
