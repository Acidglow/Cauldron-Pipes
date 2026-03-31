package net.minecraft.client.renderer.entity;

import com.google.common.collect.Maps;
import com.mojang.blaze3d.vertex.PoseStack;
import java.util.Map;
import net.minecraft.client.model.animal.nautilus.NautilusArmorModel;
import net.minecraft.client.model.animal.nautilus.NautilusModel;
import net.minecraft.client.model.animal.nautilus.NautilusSaddleModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.monster.nautilus.ZombieNautilusCoralModel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.layers.SimpleEquipmentLayer;
import net.minecraft.client.renderer.entity.state.NautilusRenderState;
import net.minecraft.client.renderer.state.CameraRenderState;
import net.minecraft.client.renderer.texture.MissingTextureAtlasSprite;
import net.minecraft.client.resources.model.EquipmentClientInfo;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.animal.nautilus.ZombieNautilus;
import net.minecraft.world.entity.animal.nautilus.ZombieNautilusVariant;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class ZombieNautilusRenderer extends MobRenderer<ZombieNautilus, NautilusRenderState, NautilusModel> {
    private final Map<ZombieNautilusVariant.ModelType, NautilusModel> models;

    public ZombieNautilusRenderer(EntityRendererProvider.Context p_468400_) {
        super(p_468400_, new NautilusModel(p_468400_.bakeLayer(ModelLayers.ZOMBIE_NAUTILUS)), 0.7F);
        this.addLayer(
            new SimpleEquipmentLayer<>(
                this,
                p_468400_.getEquipmentRenderer(),
                EquipmentClientInfo.LayerType.NAUTILUS_BODY,
                p_467050_ -> p_467050_.bodyArmorItem,
                new NautilusArmorModel(p_468400_.bakeLayer(ModelLayers.NAUTILUS_ARMOR)),
                null
            )
        );
        this.addLayer(
            new SimpleEquipmentLayer<>(
                this,
                p_468400_.getEquipmentRenderer(),
                EquipmentClientInfo.LayerType.NAUTILUS_SADDLE,
                p_469324_ -> p_469324_.saddle,
                new NautilusSaddleModel(p_468400_.bakeLayer(ModelLayers.NAUTILUS_SADDLE)),
                null
            )
        );
        this.models = bakeModels(p_468400_);
    }

    private static Map<ZombieNautilusVariant.ModelType, NautilusModel> bakeModels(EntityRendererProvider.Context context) {
        return Maps.newEnumMap(
            Map.of(
                ZombieNautilusVariant.ModelType.NORMAL,
                new NautilusModel(context.bakeLayer(ModelLayers.ZOMBIE_NAUTILUS)),
                ZombieNautilusVariant.ModelType.WARM,
                new ZombieNautilusCoralModel(context.bakeLayer(ModelLayers.ZOMBIE_NAUTILUS_CORAL))
            )
        );
    }

    public void submit(NautilusRenderState p_467445_, PoseStack p_468834_, SubmitNodeCollector p_468319_, CameraRenderState p_468416_) {
        if (p_467445_.variant != null) {
            this.model = this.models.get(p_467445_.variant.modelAndTexture().model());
            super.submit(p_467445_, p_468834_, p_468319_, p_468416_);
        }
    }

    public Identifier getTextureLocation(NautilusRenderState p_468116_) {
        return p_468116_.variant == null ? MissingTextureAtlasSprite.getLocation() : p_468116_.variant.modelAndTexture().asset().texturePath();
    }

    public NautilusRenderState createRenderState() {
        return new NautilusRenderState();
    }

    public void extractRenderState(ZombieNautilus p_468097_, NautilusRenderState p_467333_, float p_468745_) {
        super.extractRenderState(p_468097_, p_467333_, p_468745_);
        p_467333_.saddle = p_468097_.getItemBySlot(EquipmentSlot.SADDLE).copy();
        p_467333_.bodyArmorItem = p_468097_.getBodyArmorItem().copy();
        p_467333_.variant = p_468097_.getVariant().value();
    }
}
