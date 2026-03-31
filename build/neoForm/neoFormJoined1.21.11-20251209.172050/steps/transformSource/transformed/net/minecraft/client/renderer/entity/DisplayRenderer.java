package net.minecraft.client.renderer.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.mojang.math.Transformation;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.OrderedSubmitNodeCollector;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.state.BlockDisplayEntityRenderState;
import net.minecraft.client.renderer.entity.state.DisplayEntityRenderState;
import net.minecraft.client.renderer.entity.state.ItemDisplayEntityRenderState;
import net.minecraft.client.renderer.entity.state.TextDisplayEntityRenderState;
import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.entity.Display;
import net.minecraft.world.phys.AABB;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.joml.Matrix4f;
import org.joml.Quaternionf;

@OnlyIn(Dist.CLIENT)
public abstract class DisplayRenderer<T extends Display, S, ST extends DisplayEntityRenderState> extends EntityRenderer<T, ST> {
    private final EntityRenderDispatcher entityRenderDispatcher;

    protected DisplayRenderer(EntityRendererProvider.Context p_270168_) {
        super(p_270168_);
        this.entityRenderDispatcher = p_270168_.getEntityRenderDispatcher();
    }

    protected AABB getBoundingBoxForCulling(T p_363062_) {
        return p_363062_.getBoundingBoxForCulling();
    }

    protected boolean affectedByCulling(T p_362589_) {
        return p_362589_.affectedByCulling();
    }

    private static int getBrightnessOverride(Display display) {
        Display.RenderState display$renderstate = display.renderState();
        return display$renderstate != null ? display$renderstate.brightnessOverride() : -1;
    }

    protected int getSkyLightLevel(T p_368549_, BlockPos p_368562_) {
        int i = getBrightnessOverride(p_368549_);
        return i != -1 ? LightTexture.sky(i) : super.getSkyLightLevel(p_368549_, p_368562_);
    }

    protected int getBlockLightLevel(T p_368656_, BlockPos p_368591_) {
        int i = getBrightnessOverride(p_368656_);
        return i != -1 ? LightTexture.block(i) : super.getBlockLightLevel(p_368656_, p_368591_);
    }

    protected float getShadowRadius(ST p_382866_) {
        Display.RenderState display$renderstate = p_382866_.renderState;
        return display$renderstate == null ? 0.0F : display$renderstate.shadowRadius().get(p_382866_.interpolationProgress);
    }

    protected float getShadowStrength(ST p_383074_) {
        Display.RenderState display$renderstate = p_383074_.renderState;
        return display$renderstate == null ? 0.0F : display$renderstate.shadowStrength().get(p_383074_.interpolationProgress);
    }

    public void submit(ST p_360624_, PoseStack p_270117_, SubmitNodeCollector p_435230_, CameraRenderState p_451213_) {
        Display.RenderState display$renderstate = p_360624_.renderState;
        if (display$renderstate != null && p_360624_.hasSubState()) {
            float f = p_360624_.interpolationProgress;
            super.submit(p_360624_, p_270117_, p_435230_, p_451213_);
            p_270117_.pushPose();
            p_270117_.mulPose(this.calculateOrientation(display$renderstate, p_360624_, new Quaternionf()));
            Transformation transformation = display$renderstate.transformation().get(f);
            p_270117_.mulPose(transformation.getMatrix());
            this.submitInner(p_360624_, p_270117_, p_435230_, p_360624_.lightCoords, f);
            p_270117_.popPose();
        }
    }

    private Quaternionf calculateOrientation(Display.RenderState renderState, ST entityRenderState, Quaternionf quaternion) {
        return switch (renderState.billboardConstraints()) {
            case FIXED -> quaternion.rotationYXZ((float) (-Math.PI / 180.0) * entityRenderState.entityYRot, (float) (Math.PI / 180.0) * entityRenderState.entityXRot, 0.0F);
            case HORIZONTAL -> quaternion.rotationYXZ(
                (float) (-Math.PI / 180.0) * entityRenderState.entityYRot, (float) (Math.PI / 180.0) * transformXRot(entityRenderState.cameraXRot), 0.0F
            );
            case VERTICAL -> quaternion.rotationYXZ(
                (float) (-Math.PI / 180.0) * transformYRot(entityRenderState.cameraYRot), (float) (Math.PI / 180.0) * entityRenderState.entityXRot, 0.0F
            );
            case CENTER -> quaternion.rotationYXZ(
                (float) (-Math.PI / 180.0) * transformYRot(entityRenderState.cameraYRot), (float) (Math.PI / 180.0) * transformXRot(entityRenderState.cameraXRot), 0.0F
            );
        };
    }

    private static float transformYRot(float yRot) {
        return yRot - 180.0F;
    }

    private static float transformXRot(float xRot) {
        return -xRot;
    }

    private static <T extends Display> float entityYRot(T entity, float partialTick) {
        return entity.getYRot(partialTick);
    }

    private static <T extends Display> float entityXRot(T entity, float partialTick) {
        return entity.getXRot(partialTick);
    }

    protected abstract void submitInner(ST renderState, PoseStack poseStack, SubmitNodeCollector nodeCollector, int packedLight, float partialTick);

    public void extractRenderState(T p_362672_, ST p_361329_, float p_365301_) {
        super.extractRenderState(p_362672_, p_361329_, p_365301_);
        p_361329_.renderState = p_362672_.renderState();
        p_361329_.interpolationProgress = p_362672_.calculateInterpolationProgress(p_365301_);
        p_361329_.entityYRot = entityYRot(p_362672_, p_365301_);
        p_361329_.entityXRot = entityXRot(p_362672_, p_365301_);
        Camera camera = this.entityRenderDispatcher.camera;
        p_361329_.cameraXRot = camera.xRot();
        p_361329_.cameraYRot = camera.yRot();
    }

    @OnlyIn(Dist.CLIENT)
    public static class BlockDisplayRenderer
        extends DisplayRenderer<Display.BlockDisplay, Display.BlockDisplay.BlockRenderState, BlockDisplayEntityRenderState> {
        protected BlockDisplayRenderer(EntityRendererProvider.Context p_270283_) {
            super(p_270283_);
        }

        public BlockDisplayEntityRenderState createRenderState() {
            return new BlockDisplayEntityRenderState();
        }

        public void extractRenderState(Display.BlockDisplay p_362697_, BlockDisplayEntityRenderState p_363759_, float p_360854_) {
            super.extractRenderState(p_362697_, p_363759_, p_360854_);
            p_363759_.blockRenderState = p_362697_.blockRenderState();
        }

        public void submitInner(BlockDisplayEntityRenderState p_432901_, PoseStack p_434089_, SubmitNodeCollector p_433174_, int p_435266_, float p_435422_) {
            p_433174_.submitBlock(p_434089_, p_432901_.blockRenderState.blockState(), p_435266_, OverlayTexture.NO_OVERLAY, p_432901_.outlineColor);
        }
    }

    @OnlyIn(Dist.CLIENT)
    public static class ItemDisplayRenderer extends DisplayRenderer<Display.ItemDisplay, Display.ItemDisplay.ItemRenderState, ItemDisplayEntityRenderState> {
        private final ItemModelResolver itemModelResolver;

        protected ItemDisplayRenderer(EntityRendererProvider.Context p_270110_) {
            super(p_270110_);
            this.itemModelResolver = p_270110_.getItemModelResolver();
        }

        public ItemDisplayEntityRenderState createRenderState() {
            return new ItemDisplayEntityRenderState();
        }

        public void extractRenderState(Display.ItemDisplay p_360671_, ItemDisplayEntityRenderState p_361611_, float p_361257_) {
            super.extractRenderState(p_360671_, p_361611_, p_361257_);
            Display.ItemDisplay.ItemRenderState display$itemdisplay$itemrenderstate = p_360671_.itemRenderState();
            if (display$itemdisplay$itemrenderstate != null) {
                this.itemModelResolver
                    .updateForNonLiving(
                        p_361611_.item, display$itemdisplay$itemrenderstate.itemStack(), display$itemdisplay$itemrenderstate.itemTransform(), p_360671_
                    );
            } else {
                p_361611_.item.clear();
            }
        }

        public void submitInner(ItemDisplayEntityRenderState p_433571_, PoseStack p_432839_, SubmitNodeCollector p_433402_, int p_434368_, float p_433057_) {
            if (!p_433571_.item.isEmpty()) {
                p_432839_.mulPose(Axis.YP.rotation((float) Math.PI));
                p_433571_.item.submit(p_432839_, p_433402_, p_434368_, OverlayTexture.NO_OVERLAY, p_433571_.outlineColor);
            }
        }
    }

    @OnlyIn(Dist.CLIENT)
    public static class TextDisplayRenderer extends DisplayRenderer<Display.TextDisplay, Display.TextDisplay.TextRenderState, TextDisplayEntityRenderState> {
        private final Font font;

        protected TextDisplayRenderer(EntityRendererProvider.Context p_271012_) {
            super(p_271012_);
            this.font = p_271012_.getFont();
        }

        public TextDisplayEntityRenderState createRenderState() {
            return new TextDisplayEntityRenderState();
        }

        public void extractRenderState(Display.TextDisplay p_362264_, TextDisplayEntityRenderState p_365023_, float p_365012_) {
            super.extractRenderState(p_362264_, p_365023_, p_365012_);
            p_365023_.textRenderState = p_362264_.textRenderState();
            p_365023_.cachedInfo = p_362264_.cacheDisplay(this::splitLines);
        }

        private Display.TextDisplay.CachedInfo splitLines(Component text, int maxWidth) {
            List<FormattedCharSequence> list = this.font.split(text, maxWidth);
            List<Display.TextDisplay.CachedLine> list1 = new ArrayList<>(list.size());
            int i = 0;

            for (FormattedCharSequence formattedcharsequence : list) {
                int j = this.font.width(formattedcharsequence);
                i = Math.max(i, j);
                list1.add(new Display.TextDisplay.CachedLine(formattedcharsequence, j));
            }

            return new Display.TextDisplay.CachedInfo(list1, i);
        }

        public void submitInner(TextDisplayEntityRenderState p_436041_, PoseStack p_433746_, SubmitNodeCollector p_434372_, int p_435117_, float p_434778_) {
            Display.TextDisplay.TextRenderState display$textdisplay$textrenderstate = p_436041_.textRenderState;
            byte b0 = display$textdisplay$textrenderstate.flags();
            boolean flag = (b0 & 2) != 0;
            boolean flag1 = (b0 & 4) != 0;
            boolean flag2 = (b0 & 1) != 0;
            Display.TextDisplay.Align display$textdisplay$align = Display.TextDisplay.getAlign(b0);
            byte b1 = (byte)display$textdisplay$textrenderstate.textOpacity().get(p_434778_);
            int i;
            if (flag1) {
                float f = Minecraft.getInstance().options.getBackgroundOpacity(0.25F);
                i = (int)(f * 255.0F) << 24;
            } else {
                i = display$textdisplay$textrenderstate.backgroundColor().get(p_434778_);
            }

            float f2 = 0.0F;
            Matrix4f matrix4f = p_433746_.last().pose();
            matrix4f.rotate((float) Math.PI, 0.0F, 1.0F, 0.0F);
            matrix4f.scale(-0.025F, -0.025F, -0.025F);
            Display.TextDisplay.CachedInfo display$textdisplay$cachedinfo = p_436041_.cachedInfo;
            int j = 1;
            int k = 9 + 1;
            int l = display$textdisplay$cachedinfo.width();
            int i1 = display$textdisplay$cachedinfo.lines().size() * k - 1;
            matrix4f.translate(1.0F - l / 2.0F, -i1, 0.0F);
            if (i != 0) {
                p_434372_.submitCustomGeometry(
                    p_433746_, flag ? RenderTypes.textBackgroundSeeThrough() : RenderTypes.textBackground(), (p_434840_, p_435597_) -> {
                        p_435597_.addVertex(p_434840_, -1.0F, -1.0F, 0.0F).setColor(i).setLight(p_435117_);
                        p_435597_.addVertex(p_434840_, -1.0F, (float)i1, 0.0F).setColor(i).setLight(p_435117_);
                        p_435597_.addVertex(p_434840_, (float)l, (float)i1, 0.0F).setColor(i).setLight(p_435117_);
                        p_435597_.addVertex(p_434840_, (float)l, -1.0F, 0.0F).setColor(i).setLight(p_435117_);
                    }
                );
            }

            OrderedSubmitNodeCollector orderedsubmitnodecollector = p_434372_.order(i != 0 ? 1 : 0);

            for (Display.TextDisplay.CachedLine display$textdisplay$cachedline : display$textdisplay$cachedinfo.lines()) {
                float f1 = switch (display$textdisplay$align) {
                    case LEFT -> 0.0F;
                    case RIGHT -> l - display$textdisplay$cachedline.width();
                    case CENTER -> l / 2.0F - display$textdisplay$cachedline.width() / 2.0F;
                };
                orderedsubmitnodecollector.submitText(
                    p_433746_,
                    f1,
                    f2,
                    display$textdisplay$cachedline.contents(),
                    flag2,
                    flag ? Font.DisplayMode.SEE_THROUGH : Font.DisplayMode.POLYGON_OFFSET,
                    p_435117_,
                    b1 << 24 | 16777215,
                    0,
                    0
                );
                f2 += k;
            }
        }
    }
}
