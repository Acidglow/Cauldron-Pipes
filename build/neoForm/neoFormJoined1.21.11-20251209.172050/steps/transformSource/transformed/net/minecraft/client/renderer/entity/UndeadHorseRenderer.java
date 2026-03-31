package net.minecraft.client.renderer.entity;

import net.minecraft.client.model.animal.equine.AbstractEquineModel;
import net.minecraft.client.model.animal.equine.EquineSaddleModel;
import net.minecraft.client.model.animal.equine.HorseModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.layers.SimpleEquipmentLayer;
import net.minecraft.client.renderer.entity.state.EquineRenderState;
import net.minecraft.client.resources.model.EquipmentClientInfo;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.animal.equine.AbstractHorse;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class UndeadHorseRenderer extends AbstractHorseRenderer<AbstractHorse, EquineRenderState, AbstractEquineModel<EquineRenderState>> {
    private final Identifier texture;

    public UndeadHorseRenderer(EntityRendererProvider.Context context, UndeadHorseRenderer.Type type) {
        super(context, new HorseModel(context.bakeLayer(type.model)), new HorseModel(context.bakeLayer(type.babyModel)));
        this.texture = type.texture;
        this.addLayer(
            new SimpleEquipmentLayer<>(
                this,
                context.getEquipmentRenderer(),
                EquipmentClientInfo.LayerType.HORSE_BODY,
                p_454364_ -> p_454364_.bodyArmorItem,
                new HorseModel(context.bakeLayer(ModelLayers.UNDEAD_HORSE_ARMOR)),
                new HorseModel(context.bakeLayer(ModelLayers.UNDEAD_HORSE_BABY_ARMOR))
            )
        );
        this.addLayer(
            new SimpleEquipmentLayer<>(
                this,
                context.getEquipmentRenderer(),
                type.saddleLayer,
                p_397407_ -> p_397407_.saddle,
                new EquineSaddleModel(context.bakeLayer(type.saddleModel)),
                new EquineSaddleModel(context.bakeLayer(type.babySaddleModel))
            )
        );
    }

    public Identifier getTextureLocation(EquineRenderState p_362480_) {
        return this.texture;
    }

    public EquineRenderState createRenderState() {
        return new EquineRenderState();
    }

    @OnlyIn(Dist.CLIENT)
    public static enum Type {
        SKELETON(
            Identifier.withDefaultNamespace("textures/entity/horse/horse_skeleton.png"),
            ModelLayers.SKELETON_HORSE,
            ModelLayers.SKELETON_HORSE_BABY,
            EquipmentClientInfo.LayerType.SKELETON_HORSE_SADDLE,
            ModelLayers.SKELETON_HORSE_SADDLE,
            ModelLayers.SKELETON_HORSE_BABY_SADDLE
        ),
        ZOMBIE(
            Identifier.withDefaultNamespace("textures/entity/horse/horse_zombie.png"),
            ModelLayers.ZOMBIE_HORSE,
            ModelLayers.ZOMBIE_HORSE_BABY,
            EquipmentClientInfo.LayerType.ZOMBIE_HORSE_SADDLE,
            ModelLayers.ZOMBIE_HORSE_SADDLE,
            ModelLayers.ZOMBIE_HORSE_BABY_SADDLE
        );

        final Identifier texture;
        final ModelLayerLocation model;
        final ModelLayerLocation babyModel;
        final EquipmentClientInfo.LayerType saddleLayer;
        final ModelLayerLocation saddleModel;
        final ModelLayerLocation babySaddleModel;

        private Type(
            Identifier texture,
            ModelLayerLocation model,
            ModelLayerLocation babyModel,
            EquipmentClientInfo.LayerType saddleLayer,
            ModelLayerLocation saddleModel,
            ModelLayerLocation babySaddleModel
        ) {
            this.texture = texture;
            this.model = model;
            this.babyModel = babyModel;
            this.saddleLayer = saddleLayer;
            this.saddleModel = saddleModel;
            this.babySaddleModel = babySaddleModel;
        }
    }
}
