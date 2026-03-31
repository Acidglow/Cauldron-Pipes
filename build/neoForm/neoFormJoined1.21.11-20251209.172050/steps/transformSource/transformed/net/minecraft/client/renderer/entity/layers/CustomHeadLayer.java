package net.minecraft.client.renderer.entity.layers;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import java.util.function.Function;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.HeadedModel;
import net.minecraft.client.model.geom.EntityModelSet;
import net.minecraft.client.model.object.skull.SkullModelBase;
import net.minecraft.client.renderer.PlayerSkinRenderCache;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.SkullBlockRenderer;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.util.Util;
import net.minecraft.world.item.component.ResolvableProfile;
import net.minecraft.world.level.block.SkullBlock;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class CustomHeadLayer<S extends LivingEntityRenderState, M extends EntityModel<S> & HeadedModel> extends RenderLayer<S, M> {
    private static final float ITEM_SCALE = 0.625F;
    private static final float SKULL_SCALE = 1.1875F;
    private final CustomHeadLayer.Transforms transforms;
    private final Function<SkullBlock.Type, SkullModelBase> skullModels;
    private final PlayerSkinRenderCache playerSkinRenderCache;

    public CustomHeadLayer(RenderLayerParent<S, M> renderer, EntityModelSet modelSet, PlayerSkinRenderCache playerSkinRenderCache) {
        this(renderer, modelSet, playerSkinRenderCache, CustomHeadLayer.Transforms.DEFAULT);
    }

    public CustomHeadLayer(RenderLayerParent<S, M> renderer, EntityModelSet modelSet, PlayerSkinRenderCache playerSkinRenderCache, CustomHeadLayer.Transforms transforms) {
        super(renderer);
        this.transforms = transforms;
        this.skullModels = Util.memoize(p_477738_ -> SkullBlockRenderer.createModel(modelSet, p_477738_));
        this.playerSkinRenderCache = playerSkinRenderCache;
    }

    public void submit(PoseStack p_433907_, SubmitNodeCollector p_433246_, int p_435428_, S p_434789_, float p_434590_, float p_433023_) {
        if (!p_434789_.headItem.isEmpty() || p_434789_.wornHeadType != null) {
            p_433907_.pushPose();
            p_433907_.scale(this.transforms.horizontalScale(), 1.0F, this.transforms.horizontalScale());
            M m = this.getParentModel();
            m.root().translateAndRotate(p_433907_);
            m.translateToHead(p_433907_);
            if (p_434789_.wornHeadType != null) {
                p_433907_.translate(0.0F, this.transforms.skullYOffset(), 0.0F);
                p_433907_.scale(1.1875F, -1.1875F, -1.1875F);
                p_433907_.translate(-0.5, 0.0, -0.5);
                SkullBlock.Type skullblock$type = p_434789_.wornHeadType;
                SkullModelBase skullmodelbase = this.skullModels.apply(skullblock$type);
                RenderType rendertype = this.resolveSkullRenderType(p_434789_, skullblock$type);
                SkullBlockRenderer.submitSkull(
                    null, 180.0F, p_434789_.wornHeadAnimationPos, p_433907_, p_433246_, p_435428_, skullmodelbase, rendertype, p_434789_.outlineColor, null
                );
            } else {
                translateToHead(p_433907_, this.transforms);
                p_434789_.headItem.submit(p_433907_, p_433246_, p_435428_, OverlayTexture.NO_OVERLAY, p_434789_.outlineColor);
            }

            p_433907_.popPose();
        }
    }

    private RenderType resolveSkullRenderType(LivingEntityRenderState renderState, SkullBlock.Type skullType) {
        if (skullType == SkullBlock.Types.PLAYER) {
            ResolvableProfile resolvableprofile = renderState.wornHeadProfile;
            if (resolvableprofile != null) {
                return this.playerSkinRenderCache.getOrDefault(resolvableprofile).renderType();
            }
        }

        return SkullBlockRenderer.getSkullRenderType(skullType, null);
    }

    public static void translateToHead(PoseStack poseStack, CustomHeadLayer.Transforms transforms) {
        poseStack.translate(0.0F, -0.25F + transforms.yOffset(), 0.0F);
        poseStack.mulPose(Axis.YP.rotationDegrees(180.0F));
        poseStack.scale(0.625F, -0.625F, -0.625F);
    }

    @OnlyIn(Dist.CLIENT)
    public record Transforms(float yOffset, float skullYOffset, float horizontalScale) {
        public static final CustomHeadLayer.Transforms DEFAULT = new CustomHeadLayer.Transforms(0.0F, 0.0F, 1.0F);
    }
}
