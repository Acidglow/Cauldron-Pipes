package acidglow.cauldron_pipes.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import acidglow.cauldron_pipes.common.CauldronFluidTransfom;
import acidglow.cauldron_pipes.common.tile.CauldronBlockEntity;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.NeoForgeRenderTypes;

public class CauldronFluidRenderer implements BlockEntityRenderer<CauldronBlockEntity, CauldronFluidRenderState>
{
	private static final float INNER_MIN = 2.0F / 16.0F + 0.001F;
	private static final float INNER_MAX = 14.0F / 16.0F - 0.001F;
	private static final float FLUID_FLOOR = 4.0F / 16.0F + 0.001F;
	private static final RenderType RENDER_TYPE = NeoForgeRenderTypes.getUnsortedTranslucent(Identifier.withDefaultNamespace("textures/atlas/blocks.png"));
	private final TextureAtlasSprite lavaStill;
	private final TextureAtlasSprite lavaFlow;

	public CauldronFluidRenderer(BlockEntityRendererProvider.Context context)
	{
		this.lavaStill = context.sprites().get(Sheets.BLOCKS_MAPPER.defaultNamespaceApply("lava_still"));
		this.lavaFlow = context.sprites().get(Sheets.BLOCKS_MAPPER.defaultNamespaceApply("lava_flow"));
	}

	@Override
	public CauldronFluidRenderState createRenderState()
	{
		return new CauldronFluidRenderState();
	}

	@Override
	public void extractRenderState(CauldronBlockEntity blockEntity, CauldronFluidRenderState renderState, float partialTick, Vec3 cameraPosition,
		net.minecraft.client.renderer.feature.ModelFeatureRenderer.CrumblingOverlay breakProgress)
	{
		BlockEntityRenderer.super.extractRenderState(blockEntity, renderState, partialTick, cameraPosition, breakProgress);
		var tank = blockEntity.getFluidTank();
		renderState.fluid = tank.getStoredFluid();
		renderState.amount = tank.getAmount();
		renderState.surfaceHeight = renderState.fluid == Fluids.LAVA && tank.hasPartialBucket()
			? CauldronFluidTransfom.getLayeredContentHeight(renderState.amount)
			: 0.0F;
	}

	@Override
	public void submit(CauldronFluidRenderState renderState, PoseStack poseStack, SubmitNodeCollector nodeCollector, CameraRenderState cameraRenderState)
	{
		if (!renderState.shouldRenderFluid())
		{
			return;
		}

		nodeCollector.submitCustomGeometry(poseStack, RENDER_TYPE, (pose, consumer) -> this.renderFluid(pose, consumer, renderState));
	}

	private void renderFluid(PoseStack.Pose pose, VertexConsumer consumer, CauldronFluidRenderState renderState)
	{
		float top = renderState.surfaceHeight;
		float wallHeight = Math.max(0.0F, top - FLUID_FLOOR);
		if (wallHeight <= 0.0F)
		{
			return;
		}

		this.renderTop(pose, consumer, top, renderState.lightCoords);
		this.renderNorthWall(pose, consumer, top, wallHeight, renderState.lightCoords);
		this.renderSouthWall(pose, consumer, top, wallHeight, renderState.lightCoords);
		this.renderWestWall(pose, consumer, top, wallHeight, renderState.lightCoords);
		this.renderEastWall(pose, consumer, top, wallHeight, renderState.lightCoords);
	}

	private void renderTop(PoseStack.Pose pose, VertexConsumer consumer, float y, int packedLight)
	{
		this.vertex(consumer, pose, INNER_MIN, y, INNER_MIN, this.lavaStill.getU0(), this.lavaStill.getV0(), packedLight, 0.0F, 1.0F, 0.0F);
		this.vertex(consumer, pose, INNER_MIN, y, INNER_MAX, this.lavaStill.getU0(), this.lavaStill.getV1(), packedLight, 0.0F, 1.0F, 0.0F);
		this.vertex(consumer, pose, INNER_MAX, y, INNER_MAX, this.lavaStill.getU1(), this.lavaStill.getV1(), packedLight, 0.0F, 1.0F, 0.0F);
		this.vertex(consumer, pose, INNER_MAX, y, INNER_MIN, this.lavaStill.getU1(), this.lavaStill.getV0(), packedLight, 0.0F, 1.0F, 0.0F);
	}

	private void renderNorthWall(PoseStack.Pose pose, VertexConsumer consumer, float top, float wallHeight, int packedLight)
	{
		float minV = this.lavaFlow.getV(1.0F - wallHeight);
		float maxV = this.lavaFlow.getV1();
		this.vertex(consumer, pose, INNER_MAX, FLUID_FLOOR, INNER_MIN, this.lavaFlow.getU0(), maxV, packedLight, 0.0F, 0.0F, 1.0F);
		this.vertex(consumer, pose, INNER_MAX, top, INNER_MIN, this.lavaFlow.getU0(), minV, packedLight, 0.0F, 0.0F, 1.0F);
		this.vertex(consumer, pose, INNER_MIN, top, INNER_MIN, this.lavaFlow.getU1(), minV, packedLight, 0.0F, 0.0F, 1.0F);
		this.vertex(consumer, pose, INNER_MIN, FLUID_FLOOR, INNER_MIN, this.lavaFlow.getU1(), maxV, packedLight, 0.0F, 0.0F, 1.0F);
	}

	private void renderSouthWall(PoseStack.Pose pose, VertexConsumer consumer, float top, float wallHeight, int packedLight)
	{
		float minV = this.lavaFlow.getV(1.0F - wallHeight);
		float maxV = this.lavaFlow.getV1();
		this.vertex(consumer, pose, INNER_MIN, FLUID_FLOOR, INNER_MAX, this.lavaFlow.getU0(), maxV, packedLight, 0.0F, 0.0F, -1.0F);
		this.vertex(consumer, pose, INNER_MIN, top, INNER_MAX, this.lavaFlow.getU0(), minV, packedLight, 0.0F, 0.0F, -1.0F);
		this.vertex(consumer, pose, INNER_MAX, top, INNER_MAX, this.lavaFlow.getU1(), minV, packedLight, 0.0F, 0.0F, -1.0F);
		this.vertex(consumer, pose, INNER_MAX, FLUID_FLOOR, INNER_MAX, this.lavaFlow.getU1(), maxV, packedLight, 0.0F, 0.0F, -1.0F);
	}

	private void renderWestWall(PoseStack.Pose pose, VertexConsumer consumer, float top, float wallHeight, int packedLight)
	{
		float minV = this.lavaFlow.getV(1.0F - wallHeight);
		float maxV = this.lavaFlow.getV1();
		this.vertex(consumer, pose, INNER_MIN, FLUID_FLOOR, INNER_MIN, this.lavaFlow.getU0(), maxV, packedLight, 1.0F, 0.0F, 0.0F);
		this.vertex(consumer, pose, INNER_MIN, top, INNER_MIN, this.lavaFlow.getU0(), minV, packedLight, 1.0F, 0.0F, 0.0F);
		this.vertex(consumer, pose, INNER_MIN, top, INNER_MAX, this.lavaFlow.getU1(), minV, packedLight, 1.0F, 0.0F, 0.0F);
		this.vertex(consumer, pose, INNER_MIN, FLUID_FLOOR, INNER_MAX, this.lavaFlow.getU1(), maxV, packedLight, 1.0F, 0.0F, 0.0F);
	}

	private void renderEastWall(PoseStack.Pose pose, VertexConsumer consumer, float top, float wallHeight, int packedLight)
	{
		float minV = this.lavaFlow.getV(1.0F - wallHeight);
		float maxV = this.lavaFlow.getV1();
		this.vertex(consumer, pose, INNER_MAX, FLUID_FLOOR, INNER_MAX, this.lavaFlow.getU0(), maxV, packedLight, -1.0F, 0.0F, 0.0F);
		this.vertex(consumer, pose, INNER_MAX, top, INNER_MAX, this.lavaFlow.getU0(), minV, packedLight, -1.0F, 0.0F, 0.0F);
		this.vertex(consumer, pose, INNER_MAX, top, INNER_MIN, this.lavaFlow.getU1(), minV, packedLight, -1.0F, 0.0F, 0.0F);
		this.vertex(consumer, pose, INNER_MAX, FLUID_FLOOR, INNER_MIN, this.lavaFlow.getU1(), maxV, packedLight, -1.0F, 0.0F, 0.0F);
	}

	private void vertex(VertexConsumer consumer, PoseStack.Pose pose, float x, float y, float z, float u, float v, int packedLight, float normalX, float normalY,
		float normalZ)
	{
		consumer.addVertex(pose, x, y, z)
			.setColor(1.0F, 1.0F, 1.0F, 1.0F)
			.setUv(u, v)
			.setOverlay(OverlayTexture.NO_OVERLAY)
			.setLight(packedLight)
			.setNormal(pose, normalX, normalY, normalZ);
	}

}
