package net.minecraft.client.renderer.special;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.serialization.MapCodec;
import java.util.function.Consumer;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.ConduitRenderer;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.resources.model.MaterialSet;
import net.minecraft.world.item.ItemDisplayContext;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.joml.Vector3fc;

@OnlyIn(Dist.CLIENT)
public class ConduitSpecialRenderer implements NoDataSpecialModelRenderer {
    private final MaterialSet materials;
    private final ModelPart model;

    public ConduitSpecialRenderer(MaterialSet materials, ModelPart model) {
        this.materials = materials;
        this.model = model;
    }

    @Override
    public void submit(
        ItemDisplayContext p_439378_, PoseStack p_440542_, SubmitNodeCollector p_440468_, int p_440438_, int p_440436_, boolean p_440547_, int p_451696_
    ) {
        p_440542_.pushPose();
        p_440542_.translate(0.5F, 0.5F, 0.5F);
        p_440468_.submitModelPart(
            this.model,
            p_440542_,
            ConduitRenderer.SHELL_TEXTURE.renderType(RenderTypes::entitySolid),
            p_440438_,
            p_440436_,
            this.materials.get(ConduitRenderer.SHELL_TEXTURE),
            false,
            false,
            -1,
            null,
            p_451696_
        );
        p_440542_.popPose();
    }

    @Override
    public void getExtents(Consumer<Vector3fc> p_470704_) {
        PoseStack posestack = new PoseStack();
        posestack.translate(0.5F, 0.5F, 0.5F);
        this.model.getExtentsForGui(posestack, p_470704_);
    }

    @OnlyIn(Dist.CLIENT)
    public record Unbaked() implements SpecialModelRenderer.Unbaked {
        public static final MapCodec<ConduitSpecialRenderer.Unbaked> MAP_CODEC = MapCodec.unit(new ConduitSpecialRenderer.Unbaked());

        @Override
        public MapCodec<ConduitSpecialRenderer.Unbaked> type() {
            return MAP_CODEC;
        }

        @Override
        public SpecialModelRenderer<?> bake(SpecialModelRenderer.BakingContext p_433995_) {
            return new ConduitSpecialRenderer(p_433995_.materials(), p_433995_.entityModelSet().bakeLayer(ModelLayers.CONDUIT_SHELL));
        }
    }
}
