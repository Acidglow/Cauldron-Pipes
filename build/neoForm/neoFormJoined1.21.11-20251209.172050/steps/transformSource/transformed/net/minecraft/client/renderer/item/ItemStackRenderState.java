package net.minecraft.client.renderer.item;

import com.mojang.blaze3d.vertex.PoseStack;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.ItemTransform;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.special.SpecialModelRenderer;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.phys.AABB;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector3fc;
import org.jspecify.annotations.Nullable;

@OnlyIn(Dist.CLIENT)
public class ItemStackRenderState {
    ItemDisplayContext displayContext = ItemDisplayContext.NONE;
    private int activeLayerCount;
    private boolean animated;
    private boolean oversizedInGui;
    private @Nullable AABB cachedModelBoundingBox;
    private ItemStackRenderState.LayerRenderState[] layers = new ItemStackRenderState.LayerRenderState[]{new ItemStackRenderState.LayerRenderState()};

    public void ensureCapacity(int expectedSize) {
        int i = this.layers.length;
        int j = this.activeLayerCount + expectedSize;
        if (j > i) {
            this.layers = Arrays.copyOf(this.layers, j);

            for (int k = i; k < j; k++) {
                this.layers[k] = new ItemStackRenderState.LayerRenderState();
            }
        }
    }

    public ItemStackRenderState.LayerRenderState newLayer() {
        this.ensureCapacity(1);
        return this.layers[this.activeLayerCount++];
    }

    public void clear() {
        this.displayContext = ItemDisplayContext.NONE;

        for (int i = 0; i < this.activeLayerCount; i++) {
            this.layers[i].clear();
        }

        this.activeLayerCount = 0;
        this.animated = false;
        this.oversizedInGui = false;
        this.cachedModelBoundingBox = null;
    }

    public void setAnimated() {
        this.animated = true;
    }

    public boolean isAnimated() {
        return this.animated;
    }

    public void appendModelIdentityElement(Object modelIdentityElement) {
    }

    private ItemStackRenderState.LayerRenderState firstLayer() {
        return this.layers[0];
    }

    public boolean isEmpty() {
        return this.activeLayerCount == 0;
    }

    public boolean usesBlockLight() {
        return this.firstLayer().usesBlockLight;
    }

    public @Nullable TextureAtlasSprite pickParticleIcon(RandomSource random) {
        return this.activeLayerCount == 0 ? null : this.layers[random.nextInt(this.activeLayerCount)].particleIcon;
    }

    public void visitExtents(Consumer<Vector3fc> visitor) {
        Vector3f vector3f = new Vector3f();
        PoseStack.Pose posestack$pose = new PoseStack.Pose();

        for (int i = 0; i < this.activeLayerCount; i++) {
            ItemStackRenderState.LayerRenderState itemstackrenderstate$layerrenderstate = this.layers[i];
            itemstackrenderstate$layerrenderstate.transform.apply(this.displayContext.leftHand(), posestack$pose);
            Matrix4f matrix4f = posestack$pose.pose();
            Vector3fc[] avector3fc = itemstackrenderstate$layerrenderstate.extents.get();

            for (Vector3fc vector3fc : avector3fc) {
                visitor.accept(vector3f.set(vector3fc).mulPosition(matrix4f));
            }

            posestack$pose.setIdentity();
        }
    }

    public void submit(PoseStack poseStack, SubmitNodeCollector nodeCollector, int packedLight, int packedOverlay, int outlineColor) {
        for (int i = 0; i < this.activeLayerCount; i++) {
            this.layers[i].submit(poseStack, nodeCollector, packedLight, packedOverlay, outlineColor);
        }
    }

    public AABB getModelBoundingBox() {
        if (this.cachedModelBoundingBox != null) {
            return this.cachedModelBoundingBox;
        } else {
            AABB.Builder aabb$builder = new AABB.Builder();
            this.visitExtents(aabb$builder::include);
            AABB aabb = aabb$builder.build();
            this.cachedModelBoundingBox = aabb;
            return aabb;
        }
    }

    public void setOversizedInGui(boolean oversizedInGui) {
        this.oversizedInGui = oversizedInGui;
    }

    public boolean isOversizedInGui() {
        return this.oversizedInGui;
    }

    @OnlyIn(Dist.CLIENT)
    public static enum FoilType {
        NONE,
        STANDARD,
        SPECIAL;
    }

    @OnlyIn(Dist.CLIENT)
    public class LayerRenderState {
        private static final Vector3fc[] NO_EXTENTS = new Vector3fc[0];
        public static final Supplier<Vector3fc[]> NO_EXTENTS_SUPPLIER = () -> NO_EXTENTS;
        private final List<BakedQuad> quads = new ArrayList<>();
        boolean usesBlockLight;
        @Nullable TextureAtlasSprite particleIcon;
        ItemTransform transform = ItemTransform.NO_TRANSFORM;
        private @Nullable RenderType renderType;
        private ItemStackRenderState.FoilType foilType = ItemStackRenderState.FoilType.NONE;
        private int[] tintLayers = new int[0];
        private @Nullable SpecialModelRenderer<Object> specialRenderer;
        private @Nullable Object argumentForSpecialRendering;
        Supplier<Vector3fc[]> extents = NO_EXTENTS_SUPPLIER;

        public void clear() {
            this.quads.clear();
            this.renderType = null;
            this.foilType = ItemStackRenderState.FoilType.NONE;
            this.specialRenderer = null;
            this.argumentForSpecialRendering = null;
            Arrays.fill(this.tintLayers, -1);
            this.usesBlockLight = false;
            this.particleIcon = null;
            this.transform = ItemTransform.NO_TRANSFORM;
            this.extents = NO_EXTENTS_SUPPLIER;
        }

        public List<BakedQuad> prepareQuadList() {
            return this.quads;
        }

        public void setRenderType(RenderType renderType) {
            this.renderType = renderType;
        }

        public void setUsesBlockLight(boolean usesBlockLight) {
            this.usesBlockLight = usesBlockLight;
        }

        public void setExtents(Supplier<Vector3fc[]> extents) {
            this.extents = extents;
        }

        public void setParticleIcon(TextureAtlasSprite particleIcon) {
            this.particleIcon = particleIcon;
        }

        public void setTransform(ItemTransform transform) {
            this.transform = transform;
        }

        public <T> void setupSpecialModel(SpecialModelRenderer<T> renderer, @Nullable T argument) {
            this.specialRenderer = eraseSpecialRenderer(renderer);
            this.argumentForSpecialRendering = argument;
        }

        private static SpecialModelRenderer<Object> eraseSpecialRenderer(SpecialModelRenderer<?> specialRenderer) {
            return (SpecialModelRenderer<Object>)specialRenderer;
        }

        public void setFoilType(ItemStackRenderState.FoilType foilType) {
            this.foilType = foilType;
        }

        public int[] prepareTintLayers(int count) {
            if (count > this.tintLayers.length) {
                this.tintLayers = new int[count];
                Arrays.fill(this.tintLayers, -1);
            }

            return this.tintLayers;
        }

        void submit(PoseStack poseStack, SubmitNodeCollector nodeCollector, int packedLight, int packedOverlay, int outlineColor) {
            poseStack.pushPose();
            this.transform.apply(ItemStackRenderState.this.displayContext.leftHand(), poseStack.last());
            if (this.specialRenderer != null) {
                this.specialRenderer
                    .submit(
                        this.argumentForSpecialRendering,
                        ItemStackRenderState.this.displayContext,
                        poseStack,
                        nodeCollector,
                        packedLight,
                        packedOverlay,
                        this.foilType != ItemStackRenderState.FoilType.NONE,
                        outlineColor
                    );
            } else if (this.renderType != null) {
                nodeCollector.submitItem(
                    poseStack,
                    ItemStackRenderState.this.displayContext,
                    packedLight,
                    packedOverlay,
                    outlineColor,
                    this.tintLayers,
                    this.quads,
                    this.renderType,
                    this.foilType
                );
            }

            poseStack.popPose();
        }
    }
}
