package net.minecraft.client.renderer.special;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.mojang.serialization.codecs.RecordCodecBuilder.Instance;
import java.util.function.Consumer;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.object.statue.CopperGolemStatueModel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.animal.golem.CopperGolemOxidationLevels;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.level.block.CopperGolemStatueBlock;
import net.minecraft.world.level.block.WeatheringCopper;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.joml.Vector3fc;

@OnlyIn(Dist.CLIENT)
public class CopperGolemStatueSpecialRenderer implements NoDataSpecialModelRenderer {
    private static final Direction MODEL_STATE = Direction.SOUTH;
    private final CopperGolemStatueModel model;
    private final Identifier texture;

    public CopperGolemStatueSpecialRenderer(CopperGolemStatueModel model, Identifier texture) {
        this.model = model;
        this.texture = texture;
    }

    @Override
    public void submit(
        ItemDisplayContext p_439153_, PoseStack p_440674_, SubmitNodeCollector p_439014_, int p_439345_, int p_440397_, boolean p_440452_, int p_451699_
    ) {
        positionModel(p_440674_);
        p_439014_.submitModel(
            this.model, Direction.SOUTH, p_440674_, RenderTypes.entityCutoutNoCull(this.texture), p_439345_, p_440397_, -1, null, p_451699_, null
        );
    }

    @Override
    public void getExtents(Consumer<Vector3fc> p_470775_) {
        PoseStack posestack = new PoseStack();
        positionModel(posestack);
        this.model.setupAnim(MODEL_STATE);
        this.model.root().getExtentsForGui(posestack, p_470775_);
    }

    private static void positionModel(PoseStack poseStack) {
        poseStack.translate(0.5F, 1.5F, 0.5F);
        poseStack.scale(-1.0F, -1.0F, 1.0F);
    }

    @OnlyIn(Dist.CLIENT)
    public record Unbaked(Identifier texture, CopperGolemStatueBlock.Pose pose) implements SpecialModelRenderer.Unbaked {
        public static final MapCodec<CopperGolemStatueSpecialRenderer.Unbaked> MAP_CODEC = RecordCodecBuilder.mapCodec(
            p_465705_ -> p_465705_.group(
                    Identifier.CODEC.fieldOf("texture").forGetter(CopperGolemStatueSpecialRenderer.Unbaked::texture),
                    CopperGolemStatueBlock.Pose.CODEC.fieldOf("pose").forGetter(CopperGolemStatueSpecialRenderer.Unbaked::pose)
                )
                .apply(p_465705_, CopperGolemStatueSpecialRenderer.Unbaked::new)
        );

        public Unbaked(WeatheringCopper.WeatherState p_447117_, CopperGolemStatueBlock.Pose p_445375_) {
            this(CopperGolemOxidationLevels.getOxidationLevel(p_447117_).texture(), p_445375_);
        }

        @Override
        public MapCodec<CopperGolemStatueSpecialRenderer.Unbaked> type() {
            return MAP_CODEC;
        }

        @Override
        public SpecialModelRenderer<?> bake(SpecialModelRenderer.BakingContext p_435794_) {
            CopperGolemStatueModel coppergolemstatuemodel = new CopperGolemStatueModel(p_435794_.entityModelSet().bakeLayer(getModel(this.pose)));
            return new CopperGolemStatueSpecialRenderer(coppergolemstatuemodel, this.texture);
        }

        private static ModelLayerLocation getModel(CopperGolemStatueBlock.Pose pose) {
            return switch (pose) {
                case STANDING -> ModelLayers.COPPER_GOLEM;
                case SITTING -> ModelLayers.COPPER_GOLEM_SITTING;
                case STAR -> ModelLayers.COPPER_GOLEM_STAR;
                case RUNNING -> ModelLayers.COPPER_GOLEM_RUNNING;
            };
        }
    }
}
