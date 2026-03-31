package net.minecraft.client.renderer.blockentity;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.state.SpawnerRenderState;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.state.CameraRenderState;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.entity.TrialSpawnerBlockEntity;
import net.minecraft.world.level.block.entity.trialspawner.TrialSpawner;
import net.minecraft.world.level.block.entity.trialspawner.TrialSpawnerStateData;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.jspecify.annotations.Nullable;

@OnlyIn(Dist.CLIENT)
public class TrialSpawnerRenderer implements BlockEntityRenderer<TrialSpawnerBlockEntity, SpawnerRenderState> {
    private final EntityRenderDispatcher entityRenderer;

    public TrialSpawnerRenderer(BlockEntityRendererProvider.Context context) {
        this.entityRenderer = context.entityRenderer();
    }

    public SpawnerRenderState createRenderState() {
        return new SpawnerRenderState();
    }

    public void extractRenderState(
        TrialSpawnerBlockEntity p_446337_,
        SpawnerRenderState p_445637_,
        float p_446553_,
        Vec3 p_446985_,
        ModelFeatureRenderer.@Nullable CrumblingOverlay p_447263_
    ) {
        BlockEntityRenderer.super.extractRenderState(p_446337_, p_445637_, p_446553_, p_446985_, p_447263_);
        if (p_446337_.getLevel() != null) {
            TrialSpawner trialspawner = p_446337_.getTrialSpawner();
            TrialSpawnerStateData trialspawnerstatedata = trialspawner.getStateData();
            Entity entity = trialspawnerstatedata.getOrCreateDisplayEntity(trialspawner, p_446337_.getLevel(), trialspawner.getState());
            extractSpawnerData(p_445637_, p_446553_, entity, this.entityRenderer, trialspawnerstatedata.getOSpin(), trialspawnerstatedata.getSpin());
        }
    }

    static void extractSpawnerData(
        SpawnerRenderState renderState, float partialTick, @Nullable Entity entity, EntityRenderDispatcher entityRenderer, double oSpin, double spin
    ) {
        if (entity != null) {
            renderState.displayEntity = entityRenderer.extractEntity(entity, partialTick);
            renderState.displayEntity.lightCoords = renderState.lightCoords;
            renderState.spin = (float)Mth.lerp((double)partialTick, oSpin, spin) * 10.0F;
            renderState.scale = 0.53125F;
            float f = Math.max(entity.getBbWidth(), entity.getBbHeight());
            if (f > 1.0) {
                renderState.scale /= f;
            }
        }
    }

    public void submit(SpawnerRenderState p_446605_, PoseStack p_438985_, SubmitNodeCollector p_440689_, CameraRenderState p_451456_) {
        if (p_446605_.displayEntity != null) {
            SpawnerRenderer.submitEntityInSpawner(
                p_438985_, p_440689_, p_446605_.displayEntity, this.entityRenderer, p_446605_.spin, p_446605_.scale, p_451456_
            );
        }
    }

    @Override
    public net.minecraft.world.phys.AABB getRenderBoundingBox(TrialSpawnerBlockEntity blockEntity) {
        net.minecraft.core.BlockPos pos = blockEntity.getBlockPos();
        return new net.minecraft.world.phys.AABB(pos.getX() - 1.0, pos.getY() - 1.0, pos.getZ() - 1.0, pos.getX() + 2.0, pos.getY() + 2.0, pos.getZ() + 2.0);
    }
}
