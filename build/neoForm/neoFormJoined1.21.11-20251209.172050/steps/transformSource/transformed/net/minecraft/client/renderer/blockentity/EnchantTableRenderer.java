package net.minecraft.client.renderer.blockentity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.object.book.BookModel;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.state.EnchantTableRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.model.Material;
import net.minecraft.client.resources.model.MaterialSet;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.entity.EnchantingTableBlockEntity;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.jspecify.annotations.Nullable;

@OnlyIn(Dist.CLIENT)
public class EnchantTableRenderer implements BlockEntityRenderer<EnchantingTableBlockEntity, EnchantTableRenderState> {
    public static final Material BOOK_TEXTURE = Sheets.BLOCK_ENTITIES_MAPPER.defaultNamespaceApply("enchanting_table_book");
    private final MaterialSet materials;
    private final BookModel bookModel;

    public EnchantTableRenderer(BlockEntityRendererProvider.Context context) {
        this.materials = context.materials();
        this.bookModel = new BookModel(context.bakeLayer(ModelLayers.BOOK));
    }

    public EnchantTableRenderState createRenderState() {
        return new EnchantTableRenderState();
    }

    public void extractRenderState(
        EnchantingTableBlockEntity p_446778_,
        EnchantTableRenderState p_445593_,
        float p_447234_,
        Vec3 p_446321_,
        ModelFeatureRenderer.@Nullable CrumblingOverlay p_446024_
    ) {
        BlockEntityRenderer.super.extractRenderState(p_446778_, p_445593_, p_447234_, p_446321_, p_446024_);
        p_445593_.flip = Mth.lerp(p_447234_, p_446778_.oFlip, p_446778_.flip);
        p_445593_.open = Mth.lerp(p_447234_, p_446778_.oOpen, p_446778_.open);
        p_445593_.time = p_446778_.time + p_447234_;
        float f = p_446778_.rot - p_446778_.oRot;

        while (f >= (float) Math.PI) {
            f -= (float) (Math.PI * 2);
        }

        while (f < (float) -Math.PI) {
            f += (float) (Math.PI * 2);
        }

        p_445593_.yRot = p_446778_.oRot + f * p_447234_;
    }

    public void submit(EnchantTableRenderState p_446504_, PoseStack p_439439_, SubmitNodeCollector p_440350_, CameraRenderState p_450969_) {
        p_439439_.pushPose();
        p_439439_.translate(0.5F, 0.75F, 0.5F);
        p_439439_.translate(0.0F, 0.1F + Mth.sin(p_446504_.time * 0.1F) * 0.01F, 0.0F);
        float f = p_446504_.yRot;
        p_439439_.mulPose(Axis.YP.rotation(-f));
        p_439439_.mulPose(Axis.ZP.rotationDegrees(80.0F));
        float f1 = Mth.frac(p_446504_.flip + 0.25F) * 1.6F - 0.3F;
        float f2 = Mth.frac(p_446504_.flip + 0.75F) * 1.6F - 0.3F;
        BookModel.State bookmodel$state = new BookModel.State(p_446504_.time, Mth.clamp(f1, 0.0F, 1.0F), Mth.clamp(f2, 0.0F, 1.0F), p_446504_.open);
        p_440350_.submitModel(
            this.bookModel,
            bookmodel$state,
            p_439439_,
            BOOK_TEXTURE.renderType(RenderTypes::entitySolid),
            p_446504_.lightCoords,
            OverlayTexture.NO_OVERLAY,
            -1,
            this.materials.get(BOOK_TEXTURE),
            0,
            p_446504_.breakProgress
        );
        p_439439_.popPose();
    }

    @Override
    public net.minecraft.world.phys.AABB getRenderBoundingBox(EnchantingTableBlockEntity blockEntity) {
        net.minecraft.core.BlockPos pos = blockEntity.getBlockPos();
        return new net.minecraft.world.phys.AABB(pos.getX(), pos.getY(), pos.getZ(), pos.getX() + 1., pos.getY() + 1.5, pos.getZ() + 1.);
    }
}
