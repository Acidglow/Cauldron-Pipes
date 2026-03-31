package net.minecraft.client.renderer.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.monster.zombie.DrownedModel;
import net.minecraft.client.renderer.entity.layers.DrownedOuterLayer;
import net.minecraft.client.renderer.entity.state.ZombieRenderState;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.monster.zombie.Drowned;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class DrownedRenderer extends AbstractZombieRenderer<Drowned, ZombieRenderState, DrownedModel> {
    private static final Identifier DROWNED_LOCATION = Identifier.withDefaultNamespace("textures/entity/zombie/drowned.png");

    public DrownedRenderer(EntityRendererProvider.Context p_173964_) {
        super(
            p_173964_,
            new DrownedModel(p_173964_.bakeLayer(ModelLayers.DROWNED)),
            new DrownedModel(p_173964_.bakeLayer(ModelLayers.DROWNED_BABY)),
            ArmorModelSet.bake(ModelLayers.DROWNED_ARMOR, p_173964_.getModelSet(), DrownedModel::new),
            ArmorModelSet.bake(ModelLayers.DROWNED_BABY_ARMOR, p_173964_.getModelSet(), DrownedModel::new)
        );
        this.addLayer(new DrownedOuterLayer(this, p_173964_.getModelSet()));
    }

    public ZombieRenderState createRenderState() {
        return new ZombieRenderState();
    }

    @Override
    public Identifier getTextureLocation(ZombieRenderState p_361561_) {
        return DROWNED_LOCATION;
    }

    protected void setupRotations(ZombieRenderState p_361137_, PoseStack p_114104_, float p_114105_, float p_114106_) {
        super.setupRotations(p_361137_, p_114104_, p_114105_, p_114106_);
        float f = p_361137_.swimAmount;
        if (f > 0.0F) {
            float f1 = -10.0F - p_361137_.xRot;
            float f2 = Mth.lerp(f, 0.0F, f1);
            p_114104_.rotateAround(Axis.XP.rotationDegrees(f2), 0.0F, p_361137_.boundingBoxHeight / 2.0F / p_114106_, 0.0F);
        }
    }

    protected HumanoidModel.ArmPose getArmPose(Drowned p_480260_, HumanoidArm p_454906_) {
        ItemStack itemstack = p_480260_.getItemHeldByArm(p_454906_);
        return p_480260_.getMainArm() == p_454906_ && p_480260_.isAggressive() && itemstack.is(Items.TRIDENT)
            ? HumanoidModel.ArmPose.THROW_TRIDENT
            : super.getArmPose(p_480260_, p_454906_);
    }
}
