package net.minecraft.client.renderer.entity;

import net.minecraft.client.model.animal.nautilus.NautilusArmorModel;
import net.minecraft.client.model.animal.nautilus.NautilusModel;
import net.minecraft.client.model.animal.nautilus.NautilusSaddleModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.layers.SimpleEquipmentLayer;
import net.minecraft.client.renderer.entity.state.NautilusRenderState;
import net.minecraft.client.resources.model.EquipmentClientInfo;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.animal.nautilus.AbstractNautilus;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class NautilusRenderer<T extends AbstractNautilus> extends AgeableMobRenderer<T, NautilusRenderState, NautilusModel> {
    private static final Identifier NAUTILUS_LOCATION = Identifier.withDefaultNamespace("textures/entity/nautilus/nautilus.png");
    private static final Identifier NAUTILUS_BABY_LOCATION = Identifier.withDefaultNamespace("textures/entity/nautilus/nautilus_baby.png");

    public NautilusRenderer(EntityRendererProvider.Context p_455622_) {
        super(p_455622_, new NautilusModel(p_455622_.bakeLayer(ModelLayers.NAUTILUS)), new NautilusModel(p_455622_.bakeLayer(ModelLayers.NAUTILUS_BABY)), 0.7F);
        this.addLayer(
            new SimpleEquipmentLayer<>(
                this,
                p_455622_.getEquipmentRenderer(),
                EquipmentClientInfo.LayerType.NAUTILUS_BODY,
                p_455317_ -> p_455317_.bodyArmorItem,
                new NautilusArmorModel(p_455622_.bakeLayer(ModelLayers.NAUTILUS_ARMOR)),
                null
            )
        );
        this.addLayer(
            new SimpleEquipmentLayer<>(
                this,
                p_455622_.getEquipmentRenderer(),
                EquipmentClientInfo.LayerType.NAUTILUS_SADDLE,
                p_455617_ -> p_455617_.saddle,
                new NautilusSaddleModel(p_455622_.bakeLayer(ModelLayers.NAUTILUS_SADDLE)),
                null
            )
        );
    }

    public Identifier getTextureLocation(NautilusRenderState p_469694_) {
        return p_469694_.isBaby ? NAUTILUS_BABY_LOCATION : NAUTILUS_LOCATION;
    }

    public NautilusRenderState createRenderState() {
        return new NautilusRenderState();
    }

    public void extractRenderState(T p_454899_, NautilusRenderState p_456199_, float p_455176_) {
        super.extractRenderState(p_454899_, p_456199_, p_455176_);
        p_456199_.saddle = p_454899_.getItemBySlot(EquipmentSlot.SADDLE).copy();
        p_456199_.bodyArmorItem = p_454899_.getBodyArmorItem().copy();
    }
}
