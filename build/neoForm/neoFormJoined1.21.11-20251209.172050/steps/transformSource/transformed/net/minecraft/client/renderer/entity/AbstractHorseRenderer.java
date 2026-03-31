package net.minecraft.client.renderer.entity;

import net.minecraft.client.model.EntityModel;
import net.minecraft.client.renderer.entity.state.EquineRenderState;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.animal.equine.AbstractHorse;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public abstract class AbstractHorseRenderer<T extends AbstractHorse, S extends EquineRenderState, M extends EntityModel<? super S>>
    extends AgeableMobRenderer<T, S, M> {
    public AbstractHorseRenderer(EntityRendererProvider.Context context, M adultModel, M babyModel) {
        super(context, adultModel, babyModel, 0.75F);
    }

    public void extractRenderState(T p_477933_, S p_362644_, float p_361007_) {
        super.extractRenderState(p_477933_, p_362644_, p_361007_);
        p_362644_.saddle = p_477933_.getItemBySlot(EquipmentSlot.SADDLE).copy();
        p_362644_.bodyArmorItem = p_477933_.getBodyArmorItem().copy();
        p_362644_.isRidden = p_477933_.isVehicle();
        p_362644_.eatAnimation = p_477933_.getEatAnim(p_361007_);
        p_362644_.standAnimation = p_477933_.getStandAnim(p_361007_);
        p_362644_.feedingAnimation = p_477933_.getMouthAnim(p_361007_);
        p_362644_.animateTail = p_477933_.tailCounter > 0;
    }
}
