package net.minecraft.client.renderer.special;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.serialization.MapCodec;
import java.util.function.Consumer;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.object.projectile.TridentModel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.world.item.ItemDisplayContext;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.joml.Vector3fc;

@OnlyIn(Dist.CLIENT)
public class TridentSpecialRenderer implements NoDataSpecialModelRenderer {
    private final TridentModel model;

    public TridentSpecialRenderer(TridentModel model) {
        this.model = model;
    }

    @Override
    public void submit(
        ItemDisplayContext p_439415_, PoseStack p_440271_, SubmitNodeCollector p_439029_, int p_440435_, int p_440161_, boolean p_439212_, int p_451671_
    ) {
        p_440271_.pushPose();
        p_440271_.scale(1.0F, -1.0F, -1.0F);
        p_439029_.submitModelPart(
            this.model.root(), p_440271_, this.model.renderType(TridentModel.TEXTURE), p_440435_, p_440161_, null, false, p_439212_, -1, null, p_451671_
        );
        p_440271_.popPose();
    }

    @Override
    public void getExtents(Consumer<Vector3fc> p_470818_) {
        PoseStack posestack = new PoseStack();
        posestack.scale(1.0F, -1.0F, -1.0F);
        this.model.root().getExtentsForGui(posestack, p_470818_);
    }

    @OnlyIn(Dist.CLIENT)
    public record Unbaked() implements SpecialModelRenderer.Unbaked {
        public static final MapCodec<TridentSpecialRenderer.Unbaked> MAP_CODEC = MapCodec.unit(new TridentSpecialRenderer.Unbaked());

        @Override
        public MapCodec<TridentSpecialRenderer.Unbaked> type() {
            return MAP_CODEC;
        }

        @Override
        public SpecialModelRenderer<?> bake(SpecialModelRenderer.BakingContext p_434124_) {
            return new TridentSpecialRenderer(new TridentModel(p_434124_.entityModelSet().bakeLayer(ModelLayers.TRIDENT)));
        }
    }
}
