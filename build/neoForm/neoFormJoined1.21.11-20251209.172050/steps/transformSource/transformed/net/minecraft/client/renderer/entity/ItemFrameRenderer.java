package net.minecraft.client.renderer.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MapRenderer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.block.model.BlockStateModel;
import net.minecraft.client.renderer.entity.state.ItemFrameRenderState;
import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.resources.model.BlockStateDefinitions;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.decoration.ItemFrame;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.saveddata.maps.MapId;
import net.minecraft.world.level.saveddata.maps.MapItemSavedData;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class ItemFrameRenderer<T extends ItemFrame> extends EntityRenderer<T, ItemFrameRenderState> {
    public static final int GLOW_FRAME_BRIGHTNESS = 5;
    public static final int BRIGHT_MAP_LIGHT_ADJUSTMENT = 30;
    private final ItemModelResolver itemModelResolver;
    private final MapRenderer mapRenderer;
    private final BlockRenderDispatcher blockRenderer;

    public ItemFrameRenderer(EntityRendererProvider.Context p_174204_) {
        super(p_174204_);
        this.itemModelResolver = p_174204_.getItemModelResolver();
        this.mapRenderer = p_174204_.getMapRenderer();
        this.blockRenderer = p_174204_.getBlockRenderDispatcher();
    }

    protected int getBlockLightLevel(T p_174216_, BlockPos p_174217_) {
        return p_174216_.getType() == EntityType.GLOW_ITEM_FRAME
            ? Math.max(5, super.getBlockLightLevel(p_174216_, p_174217_))
            : super.getBlockLightLevel(p_174216_, p_174217_);
    }

    public void submit(ItemFrameRenderState p_433594_, PoseStack p_434437_, SubmitNodeCollector p_434593_, CameraRenderState p_451296_) {
        super.submit(p_433594_, p_434437_, p_434593_, p_451296_);
        p_434437_.pushPose();
        Direction direction = p_433594_.direction;
        Vec3 vec3 = this.getRenderOffset(p_433594_);
        p_434437_.translate(-vec3.x(), -vec3.y(), -vec3.z());
        double d0 = 0.46875;
        p_434437_.translate(direction.getStepX() * 0.46875, direction.getStepY() * 0.46875, direction.getStepZ() * 0.46875);
        float f;
        float f1;
        if (direction.getAxis().isHorizontal()) {
            f = 0.0F;
            f1 = 180.0F - direction.toYRot();
        } else {
            f = -90 * direction.getAxisDirection().getStep();
            f1 = 180.0F;
        }

        p_434437_.mulPose(Axis.XP.rotationDegrees(f));
        p_434437_.mulPose(Axis.YP.rotationDegrees(f1));
        if (!p_433594_.isInvisible) {
            BlockState blockstate = BlockStateDefinitions.getItemFrameFakeState(p_433594_.isGlowFrame, p_433594_.mapId != null);
            BlockStateModel blockstatemodel = this.blockRenderer.getBlockModel(blockstate);
            p_434437_.pushPose();
            p_434437_.translate(-0.5F, -0.5F, -0.5F);
            p_434593_.submitBlockModel(
                p_434437_,
                RenderTypes.entitySolidZOffsetForward(TextureAtlas.LOCATION_BLOCKS),
                blockstatemodel,
                1.0F,
                1.0F,
                1.0F,
                p_433594_.lightCoords,
                OverlayTexture.NO_OVERLAY,
                p_433594_.outlineColor
            );
            p_434437_.popPose();
        }

        if (p_433594_.isInvisible) {
            p_434437_.translate(0.0F, 0.0F, 0.5F);
        } else {
            p_434437_.translate(0.0F, 0.0F, 0.4375F);
        }

        if (!net.neoforged.neoforge.common.NeoForge.EVENT_BUS.post(new net.neoforged.neoforge.client.event.RenderItemInFrameEvent(p_433594_, this, p_434437_, p_434593_)).isCanceled()) {
        if (p_433594_.mapId != null) {
            int j = p_433594_.rotation % 4 * 2;
            p_434437_.mulPose(Axis.ZP.rotationDegrees(j * 360.0F / 8.0F));
            p_434437_.mulPose(Axis.ZP.rotationDegrees(180.0F));
            float f2 = 0.0078125F;
            p_434437_.scale(0.0078125F, 0.0078125F, 0.0078125F);
            p_434437_.translate(-64.0F, -64.0F, 0.0F);
            p_434437_.translate(0.0F, 0.0F, -1.0F);
            int i = this.getLightCoords(p_433594_.isGlowFrame, 15728850, p_433594_.lightCoords);
            this.mapRenderer.render(p_433594_.mapRenderState, p_434437_, p_434593_, true, i);
        } else if (!p_433594_.item.isEmpty()) {
            p_434437_.mulPose(Axis.ZP.rotationDegrees(p_433594_.rotation * 360.0F / 8.0F));
            int k = this.getLightCoords(p_433594_.isGlowFrame, 15728880, p_433594_.lightCoords);
            p_434437_.scale(0.5F, 0.5F, 0.5F);
            p_433594_.item.submit(p_434437_, p_434593_, k, OverlayTexture.NO_OVERLAY, p_433594_.outlineColor);
        }
        }

        p_434437_.popPose();
    }

    private int getLightCoords(boolean isGlowFrame, int glowLight, int normalLight) {
        return isGlowFrame ? glowLight : normalLight;
    }

    public Vec3 getRenderOffset(ItemFrameRenderState p_361106_) {
        return new Vec3(p_361106_.direction.getStepX() * 0.3F, -0.25, p_361106_.direction.getStepZ() * 0.3F);
    }

    protected boolean shouldShowName(T p_115091_, double p_361664_) {
        return Minecraft.renderNames() && this.entityRenderDispatcher.crosshairPickEntity == p_115091_ && p_115091_.getItem().getCustomName() != null;
    }

    protected Component getNameTag(T p_365048_) {
        return p_365048_.getItem().getHoverName();
    }

    public ItemFrameRenderState createRenderState() {
        return new ItemFrameRenderState();
    }

    public void extractRenderState(T p_363125_, ItemFrameRenderState p_362907_, float p_364471_) {
        super.extractRenderState(p_363125_, p_362907_, p_364471_);
        p_362907_.direction = p_363125_.getDirection();
        ItemStack itemstack = p_363125_.getItem();
        this.itemModelResolver.updateForNonLiving(p_362907_.item, itemstack, ItemDisplayContext.FIXED, p_363125_);
        p_362907_.rotation = p_363125_.getRotation();
        p_362907_.isGlowFrame = p_363125_.getType() == EntityType.GLOW_ITEM_FRAME;
        p_362907_.mapId = null;
        if (!itemstack.isEmpty()) {
            MapId mapid = p_363125_.getFramedMapId(itemstack);
            if (mapid != null) {
                MapItemSavedData mapitemsaveddata = net.minecraft.world.item.MapItem.getSavedData(itemstack, p_363125_.level());
                if (mapitemsaveddata != null) {
                    this.mapRenderer.extractRenderState(mapid, mapitemsaveddata, p_362907_.mapRenderState);
                    p_362907_.mapId = mapid;
                }
            }
        }
    }
}
