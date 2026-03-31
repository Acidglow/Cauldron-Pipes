package net.minecraft.client.renderer.blockentity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.model.Model;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.state.SignRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.state.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.model.Material;
import net.minecraft.client.resources.model.MaterialSet;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.util.ARGB;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;
import net.minecraft.util.Unit;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.block.SignBlock;
import net.minecraft.world.level.block.entity.SignBlockEntity;
import net.minecraft.world.level.block.entity.SignText;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.WoodType;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.jspecify.annotations.Nullable;

@OnlyIn(Dist.CLIENT)
public abstract class AbstractSignRenderer implements BlockEntityRenderer<SignBlockEntity, SignRenderState> {
    private static final int BLACK_TEXT_OUTLINE_COLOR = -988212;
    private static final int OUTLINE_RENDER_DISTANCE = Mth.square(16);
    private final Font font;
    private final MaterialSet materials;

    public AbstractSignRenderer(BlockEntityRendererProvider.Context context) {
        this.font = context.font();
        this.materials = context.materials();
    }

    protected abstract Model.Simple getSignModel(BlockState blockState, WoodType woodType);

    protected abstract Material getSignMaterial(WoodType woodType);

    protected abstract float getSignModelRenderScale();

    protected abstract float getSignTextRenderScale();

    protected abstract Vec3 getTextOffset();

    protected abstract void translateSign(PoseStack poseStack, float yRot, BlockState state);

    public void submit(SignRenderState p_446471_, PoseStack p_440400_, SubmitNodeCollector p_439180_, CameraRenderState p_450989_) {
        BlockState blockstate = p_446471_.blockState;
        SignBlock signblock = (SignBlock)blockstate.getBlock();
        Model.Simple model$simple = this.getSignModel(blockstate, signblock.type());
        this.submitSignWithText(p_446471_, p_440400_, blockstate, signblock, signblock.type(), model$simple, p_446471_.breakProgress, p_439180_);
    }

    private void submitSignWithText(
        SignRenderState renderState,
        PoseStack poseStack,
        BlockState blockState,
        SignBlock sign,
        WoodType woodType,
        Model.Simple model,
        ModelFeatureRenderer.@Nullable CrumblingOverlay crumblingOverlay,
        SubmitNodeCollector nodeCollector
    ) {
        poseStack.pushPose();
        this.translateSign(poseStack, -sign.getYRotationDegrees(blockState), blockState);
        this.submitSign(poseStack, renderState.lightCoords, woodType, model, crumblingOverlay, nodeCollector);
        this.submitSignText(renderState, poseStack, nodeCollector, true);
        this.submitSignText(renderState, poseStack, nodeCollector, false);
        poseStack.popPose();
    }

    protected void submitSign(
        PoseStack poseStack,
        int packedLight,
        WoodType woodType,
        Model.Simple model,
        ModelFeatureRenderer.@Nullable CrumblingOverlay crumblingOverlay,
        SubmitNodeCollector nodeCollector
    ) {
        poseStack.pushPose();
        float f = this.getSignModelRenderScale();
        poseStack.scale(f, -f, -f);
        Material material = this.getSignMaterial(woodType);
        RenderType rendertype = material.renderType(model::renderType);
        nodeCollector.submitModel(
            model, Unit.INSTANCE, poseStack, rendertype, packedLight, OverlayTexture.NO_OVERLAY, -1, this.materials.get(material), 0, crumblingOverlay
        );
        poseStack.popPose();
    }

    private void submitSignText(SignRenderState renderState, PoseStack poseStack, SubmitNodeCollector nodeCollector, boolean isFront) {
        SignText signtext = isFront ? renderState.frontText : renderState.backText;
        if (signtext != null) {
            poseStack.pushPose();
            this.translateSignText(poseStack, isFront, this.getTextOffset());
            int i = getDarkColor(signtext);
            int j = 4 * renderState.textLineHeight / 2;
            FormattedCharSequence[] aformattedcharsequence = signtext.getRenderMessages(renderState.isTextFilteringEnabled, p_445219_ -> {
                List<FormattedCharSequence> list = this.font.split(p_445219_, renderState.maxTextLineWidth);
                return list.isEmpty() ? FormattedCharSequence.EMPTY : list.get(0);
            });
            int k;
            boolean flag;
            int l;
            if (signtext.hasGlowingText()) {
                k = signtext.getColor().getTextColor();
                flag = k == DyeColor.BLACK.getTextColor() || renderState.drawOutline;
                l = 15728880;
            } else {
                k = i;
                flag = false;
                l = renderState.lightCoords;
            }

            for (int i1 = 0; i1 < 4; i1++) {
                FormattedCharSequence formattedcharsequence = aformattedcharsequence[i1];
                float f = -this.font.width(formattedcharsequence) / 2;
                nodeCollector.submitText(
                    poseStack, f, i1 * renderState.textLineHeight - j, formattedcharsequence, false, Font.DisplayMode.POLYGON_OFFSET, l, k, 0, flag ? i : 0
                );
            }

            poseStack.popPose();
        }
    }

    private void translateSignText(PoseStack poseStack, boolean isFront, Vec3 offset) {
        if (!isFront) {
            poseStack.mulPose(Axis.YP.rotationDegrees(180.0F));
        }

        float f = 0.015625F * this.getSignTextRenderScale();
        poseStack.translate(offset);
        poseStack.scale(f, -f, f);
    }

    private static boolean isOutlineVisible(BlockPos pos) {
        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer localplayer = minecraft.player;
        if (localplayer != null && minecraft.options.getCameraType().isFirstPerson() && localplayer.isScoping()) {
            return true;
        } else {
            Entity entity = minecraft.getCameraEntity();
            return entity != null && entity.distanceToSqr(Vec3.atCenterOf(pos)) < OUTLINE_RENDER_DISTANCE;
        }
    }

    public static int getDarkColor(SignText text) {
        int i = text.getColor().getTextColor();
        return i == DyeColor.BLACK.getTextColor() && text.hasGlowingText() ? -988212 : ARGB.scaleRGB(i, 0.4F);
    }

    public SignRenderState createRenderState() {
        return new SignRenderState();
    }

    public void extractRenderState(
        SignBlockEntity p_446457_, SignRenderState p_446340_, float p_445525_, Vec3 p_445758_, ModelFeatureRenderer.@Nullable CrumblingOverlay p_446708_
    ) {
        BlockEntityRenderer.super.extractRenderState(p_446457_, p_446340_, p_445525_, p_445758_, p_446708_);
        p_446340_.maxTextLineWidth = p_446457_.getMaxTextLineWidth();
        p_446340_.textLineHeight = p_446457_.getTextLineHeight();
        p_446340_.frontText = p_446457_.getFrontText();
        p_446340_.backText = p_446457_.getBackText();
        p_446340_.isTextFilteringEnabled = Minecraft.getInstance().isTextFilteringEnabled();
        p_446340_.drawOutline = isOutlineVisible(p_446457_.getBlockPos());
    }

    @Override
    public net.minecraft.world.phys.AABB getRenderBoundingBox(SignBlockEntity blockEntity) {
        if (blockEntity.getBlockState().getBlock() instanceof net.minecraft.world.level.block.StandingSignBlock) {
            BlockPos pos = blockEntity.getBlockPos();
            return new net.minecraft.world.phys.AABB(pos.getX(), pos.getY(), pos.getZ(), pos.getX() + 1.0, pos.getY() + 1.125, pos.getZ() + 1.0);
        }
        return BlockEntityRenderer.super.getRenderBoundingBox(blockEntity);
    }
}
