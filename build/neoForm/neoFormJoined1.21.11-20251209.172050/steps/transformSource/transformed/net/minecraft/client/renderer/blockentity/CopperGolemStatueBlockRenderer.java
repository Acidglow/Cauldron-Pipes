package net.minecraft.client.renderer.blockentity;

import com.mojang.blaze3d.vertex.PoseStack;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.client.model.geom.EntityModelSet;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.object.statue.CopperGolemStatueModel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.state.CopperGolemStatueRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.animal.golem.CopperGolemOxidationLevels;
import net.minecraft.world.level.block.CopperGolemStatueBlock;
import net.minecraft.world.level.block.entity.CopperGolemStatueBlockEntity;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.jspecify.annotations.Nullable;

@OnlyIn(Dist.CLIENT)
public class CopperGolemStatueBlockRenderer implements BlockEntityRenderer<CopperGolemStatueBlockEntity, CopperGolemStatueRenderState> {
    private final Map<CopperGolemStatueBlock.Pose, CopperGolemStatueModel> models = new HashMap<>();

    public CopperGolemStatueBlockRenderer(BlockEntityRendererProvider.Context context) {
        EntityModelSet entitymodelset = context.entityModelSet();
        this.models.put(CopperGolemStatueBlock.Pose.STANDING, new CopperGolemStatueModel(entitymodelset.bakeLayer(ModelLayers.COPPER_GOLEM)));
        this.models.put(CopperGolemStatueBlock.Pose.RUNNING, new CopperGolemStatueModel(entitymodelset.bakeLayer(ModelLayers.COPPER_GOLEM_RUNNING)));
        this.models.put(CopperGolemStatueBlock.Pose.SITTING, new CopperGolemStatueModel(entitymodelset.bakeLayer(ModelLayers.COPPER_GOLEM_SITTING)));
        this.models.put(CopperGolemStatueBlock.Pose.STAR, new CopperGolemStatueModel(entitymodelset.bakeLayer(ModelLayers.COPPER_GOLEM_STAR)));
    }

    public CopperGolemStatueRenderState createRenderState() {
        return new CopperGolemStatueRenderState();
    }

    public void extractRenderState(
        CopperGolemStatueBlockEntity p_446789_,
        CopperGolemStatueRenderState p_445801_,
        float p_445752_,
        Vec3 p_446054_,
        ModelFeatureRenderer.@Nullable CrumblingOverlay p_446325_
    ) {
        BlockEntityRenderer.super.extractRenderState(p_446789_, p_445801_, p_445752_, p_446054_, p_446325_);
        p_445801_.direction = p_446789_.getBlockState().getValue(CopperGolemStatueBlock.FACING);
        p_445801_.pose = p_446789_.getBlockState().getValue(BlockStateProperties.COPPER_GOLEM_POSE);
    }

    public void submit(CopperGolemStatueRenderState p_445773_, PoseStack p_438983_, SubmitNodeCollector p_439995_, CameraRenderState p_451109_) {
        if (p_445773_.blockState.getBlock() instanceof CopperGolemStatueBlock coppergolemstatueblock) {
            p_438983_.pushPose();
            p_438983_.translate(0.5F, 0.0F, 0.5F);
            CopperGolemStatueModel coppergolemstatuemodel = this.models.get(p_445773_.pose);
            Direction direction = p_445773_.direction;
            RenderType rendertype = RenderTypes.entityCutoutNoCull(
                CopperGolemOxidationLevels.getOxidationLevel(coppergolemstatueblock.getWeatheringState()).texture()
            );
            p_439995_.submitModel(
                coppergolemstatuemodel, direction, p_438983_, rendertype, p_445773_.lightCoords, OverlayTexture.NO_OVERLAY, 0, p_445773_.breakProgress
            );
            p_438983_.popPose();
        }
    }

    @Override
    public net.minecraft.world.phys.AABB getRenderBoundingBox(CopperGolemStatueBlockEntity blockEntity) {
        net.minecraft.core.BlockPos pos = blockEntity.getBlockPos();
        return new net.minecraft.world.phys.AABB(pos.getX(), pos.getY(), pos.getZ(), pos.getX() + 1, pos.getY() + 1.5, pos.getZ() + 1);
    }
}
