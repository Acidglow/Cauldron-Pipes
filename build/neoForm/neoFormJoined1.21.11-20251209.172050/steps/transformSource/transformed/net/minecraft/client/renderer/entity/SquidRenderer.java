package net.minecraft.client.renderer.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.model.animal.squid.SquidModel;
import net.minecraft.client.renderer.entity.state.SquidRenderState;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.animal.squid.Squid;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class SquidRenderer<T extends Squid> extends AgeableMobRenderer<T, SquidRenderState, SquidModel> {
    private static final Identifier SQUID_LOCATION = Identifier.withDefaultNamespace("textures/entity/squid/squid.png");

    public SquidRenderer(EntityRendererProvider.Context context, SquidModel adultModel, SquidModel babyModel) {
        super(context, adultModel, babyModel, 0.7F);
    }

    public Identifier getTextureLocation(SquidRenderState p_467483_) {
        return SQUID_LOCATION;
    }

    public SquidRenderState createRenderState() {
        return new SquidRenderState();
    }

    public void extractRenderState(T p_480753_, SquidRenderState p_360822_, float p_363384_) {
        super.extractRenderState(p_480753_, p_360822_, p_363384_);
        p_360822_.tentacleAngle = Mth.lerp(p_363384_, p_480753_.oldTentacleAngle, p_480753_.tentacleAngle);
        p_360822_.xBodyRot = Mth.lerp(p_363384_, p_480753_.xBodyRotO, p_480753_.xBodyRot);
        p_360822_.zBodyRot = Mth.lerp(p_363384_, p_480753_.zBodyRotO, p_480753_.zBodyRot);
    }

    protected void setupRotations(SquidRenderState p_360924_, PoseStack p_116025_, float p_116026_, float p_116027_) {
        p_116025_.translate(0.0F, p_360924_.isBaby ? 0.25F : 0.5F, 0.0F);
        p_116025_.mulPose(Axis.YP.rotationDegrees(180.0F - p_116026_));
        p_116025_.mulPose(Axis.XP.rotationDegrees(p_360924_.xBodyRot));
        p_116025_.mulPose(Axis.YP.rotationDegrees(p_360924_.zBodyRot));
        p_116025_.translate(0.0F, p_360924_.isBaby ? -0.6F : -1.2F, 0.0F);
    }
}
