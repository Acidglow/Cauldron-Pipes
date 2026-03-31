package net.minecraft.client.renderer.blockentity;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.state.BeaconRenderState;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.blockentity.state.BlockEntityWithBoundingBoxRenderState;
import net.minecraft.client.renderer.blockentity.state.TestInstanceRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.state.CameraRenderState;
import net.minecraft.core.BlockPos;
import net.minecraft.gizmos.GizmoStyle;
import net.minecraft.gizmos.Gizmos;
import net.minecraft.gizmos.TextGizmo;
import net.minecraft.util.ARGB;
import net.minecraft.world.level.block.entity.TestInstanceBlockEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.jspecify.annotations.Nullable;

@OnlyIn(Dist.CLIENT)
public class TestInstanceRenderer implements BlockEntityRenderer<TestInstanceBlockEntity, TestInstanceRenderState> {
    private static final float ERROR_PADDING = 0.02F;
    private final BeaconRenderer<TestInstanceBlockEntity> beacon = new BeaconRenderer<>();
    private final BlockEntityWithBoundingBoxRenderer<TestInstanceBlockEntity> box = new BlockEntityWithBoundingBoxRenderer<>();

    public TestInstanceRenderState createRenderState() {
        return new TestInstanceRenderState();
    }

    public void extractRenderState(
        TestInstanceBlockEntity p_445503_,
        TestInstanceRenderState p_446597_,
        float p_446183_,
        Vec3 p_445695_,
        ModelFeatureRenderer.@Nullable CrumblingOverlay p_445824_
    ) {
        BlockEntityRenderer.super.extractRenderState(p_445503_, p_446597_, p_446183_, p_445695_, p_445824_);
        p_446597_.beaconRenderState = new BeaconRenderState();
        BlockEntityRenderState.extractBase(p_445503_, p_446597_.beaconRenderState, p_445824_);
        BeaconRenderer.extract(p_445503_, p_446597_.beaconRenderState, p_446183_, p_445695_);
        p_446597_.blockEntityWithBoundingBoxRenderState = new BlockEntityWithBoundingBoxRenderState();
        BlockEntityRenderState.extractBase(p_445503_, p_446597_.blockEntityWithBoundingBoxRenderState, p_445824_);
        BlockEntityWithBoundingBoxRenderer.extract(p_445503_, p_446597_.blockEntityWithBoundingBoxRenderState);
        p_446597_.errorMarkers.clear();

        for (TestInstanceBlockEntity.ErrorMarker testinstanceblockentity$errormarker : p_445503_.getErrorMarkers()) {
            p_446597_.errorMarkers
                .add(new TestInstanceBlockEntity.ErrorMarker(testinstanceblockentity$errormarker.pos(), testinstanceblockentity$errormarker.text()));
        }
    }

    public void submit(TestInstanceRenderState p_451379_, PoseStack p_440717_, SubmitNodeCollector p_439591_, CameraRenderState p_451002_) {
        this.beacon.submit(p_451379_.beaconRenderState, p_440717_, p_439591_, p_451002_);
        this.box.submit(p_451379_.blockEntityWithBoundingBoxRenderState, p_440717_, p_439591_, p_451002_);

        for (TestInstanceBlockEntity.ErrorMarker testinstanceblockentity$errormarker : p_451379_.errorMarkers) {
            this.submitErrorMarker(testinstanceblockentity$errormarker);
        }
    }

    private void submitErrorMarker(TestInstanceBlockEntity.ErrorMarker errorMarker) {
        BlockPos blockpos = errorMarker.pos();
        Gizmos.cuboid(new AABB(blockpos).inflate(0.02F), GizmoStyle.fill(ARGB.colorFromFloat(0.375F, 1.0F, 0.0F, 0.0F)));
        String s = errorMarker.text().getString();
        float f = 0.16F;
        Gizmos.billboardText(s, Vec3.atLowerCornerWithOffset(blockpos, 0.5, 1.2, 0.5), TextGizmo.Style.whiteAndCentered().withScale(0.16F)).setAlwaysOnTop();
    }

    @Override
    public boolean shouldRenderOffScreen() {
        return this.beacon.shouldRenderOffScreen() || this.box.shouldRenderOffScreen();
    }

    @Override
    public int getViewDistance() {
        return Math.max(this.beacon.getViewDistance(), this.box.getViewDistance());
    }

    public boolean shouldRender(TestInstanceBlockEntity p_397042_, Vec3 p_397171_) {
        return this.beacon.shouldRender(p_397042_, p_397171_) || this.box.shouldRender(p_397042_, p_397171_);
    }
}
