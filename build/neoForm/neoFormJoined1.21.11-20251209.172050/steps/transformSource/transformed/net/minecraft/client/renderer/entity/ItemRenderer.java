package net.minecraft.client.renderer.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.SheetedDecalTextureGenerator;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexMultiConsumer;
import com.mojang.math.MatrixUtil;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ARGB;
import net.minecraft.world.item.ItemDisplayContext;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class ItemRenderer {
    public static final Identifier ENCHANTED_GLINT_ARMOR = Identifier.withDefaultNamespace("textures/misc/enchanted_glint_armor.png");
    public static final Identifier ENCHANTED_GLINT_ITEM = Identifier.withDefaultNamespace("textures/misc/enchanted_glint_item.png");
    public static final float SPECIAL_FOIL_UI_SCALE = 0.5F;
    public static final float SPECIAL_FOIL_FIRST_PERSON_SCALE = 0.75F;
    public static final float SPECIAL_FOIL_TEXTURE_SCALE = 0.0078125F;
    public static final int NO_TINT = -1;

    public static void renderItem(
        ItemDisplayContext displayContext,
        PoseStack poseStack,
        MultiBufferSource bufferSource,
        int packedLight,
        int packedOverlay,
        int[] tintLayers,
        List<BakedQuad> quads,
        RenderType renderType,
        ItemStackRenderState.FoilType foilType
    ) {
        VertexConsumer vertexconsumer;
        if (foilType == ItemStackRenderState.FoilType.SPECIAL) {
            PoseStack.Pose posestack$pose = poseStack.last().copy();
            if (displayContext == ItemDisplayContext.GUI) {
                MatrixUtil.mulComponentWise(posestack$pose.pose(), 0.5F);
            } else if (displayContext.firstPerson()) {
                MatrixUtil.mulComponentWise(posestack$pose.pose(), 0.75F);
            }

            vertexconsumer = getSpecialFoilBuffer(bufferSource, renderType, posestack$pose);
        } else {
            vertexconsumer = getFoilBuffer(bufferSource, renderType, true, foilType != ItemStackRenderState.FoilType.NONE);
        }

        renderQuadList(poseStack, vertexconsumer, quads, tintLayers, packedLight, packedOverlay);
    }

    private static VertexConsumer getSpecialFoilBuffer(MultiBufferSource bufferSource, RenderType renderType, PoseStack.Pose pose) {
        return VertexMultiConsumer.create(
            new SheetedDecalTextureGenerator(
                bufferSource.getBuffer(useTransparentGlint(renderType) ? RenderTypes.glintTranslucent() : RenderTypes.glint()), pose, 0.0078125F
            ),
            bufferSource.getBuffer(renderType)
        );
    }

    public static VertexConsumer getFoilBuffer(MultiBufferSource bufferSource, RenderType renderType, boolean isItem, boolean glint) {
        if (glint) {
            return useTransparentGlint(renderType)
                ? VertexMultiConsumer.create(bufferSource.getBuffer(RenderTypes.glintTranslucent()), bufferSource.getBuffer(renderType))
                : VertexMultiConsumer.create(bufferSource.getBuffer(isItem ? RenderTypes.glint() : RenderTypes.entityGlint()), bufferSource.getBuffer(renderType));
        } else {
            return bufferSource.getBuffer(renderType);
        }
    }

    public static List<RenderType> getFoilRenderTypes(RenderType renderType, boolean isItem, boolean glint) {
        if (glint) {
            return useTransparentGlint(renderType)
                ? List.of(renderType, RenderTypes.glintTranslucent())
                : List.of(renderType, isItem ? RenderTypes.glint() : RenderTypes.entityGlint());
        } else {
            return List.of(renderType);
        }
    }

    private static boolean useTransparentGlint(RenderType renderType) {
        return Minecraft.useShaderTransparency() && (renderType == Sheets.translucentItemSheet() || renderType == Sheets.translucentBlockItemSheet());
    }

    private static int getLayerColorSafe(int[] tintLayers, int index) {
        return index >= 0 && index < tintLayers.length ? tintLayers[index] : -1;
    }

    private static void renderQuadList(PoseStack poseStack, VertexConsumer consumer, List<BakedQuad> quads, int[] tintLayers, int packedLight, int packedOverlay) {
        PoseStack.Pose posestack$pose = poseStack.last();

        for (BakedQuad bakedquad : quads) {
            float f;
            float f1;
            float f2;
            float f3;
            if (bakedquad.isTinted()) {
                int i = getLayerColorSafe(tintLayers, bakedquad.tintIndex());
                f = ARGB.alpha(i) / 255.0F;
                f1 = ARGB.red(i) / 255.0F;
                f2 = ARGB.green(i) / 255.0F;
                f3 = ARGB.blue(i) / 255.0F;
            } else {
                f = 1.0F;
                f1 = 1.0F;
                f2 = 1.0F;
                f3 = 1.0F;
            }

            consumer.putBulkData(posestack$pose, bakedquad, f1, f2, f3, f, packedLight, packedOverlay);
        }
    }
}
