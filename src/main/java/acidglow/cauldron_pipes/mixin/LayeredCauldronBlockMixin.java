package acidglow.cauldron_pipes.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import acidglow.cauldron_pipes.common.CauldronPipes;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.InsideBlockEffectApplier;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.LayeredCauldronBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;

@Mixin(LayeredCauldronBlock.class)
public class LayeredCauldronBlockMixin
{
	@Inject(method = "handlePrecipitation", at = @At("HEAD"), cancellable = true)
	private void handlePrecipitation(BlockState state, Level level, BlockPos pos, Biome.Precipitation precipitation, CallbackInfo ci)
	{
		if (CauldronPipes.shouldBlockVanillaWorldMutation(level, pos, state))
		{
			ci.cancel();
		}
	}

	@Inject(method = "receiveStalactiteDrip", at = @At("HEAD"), cancellable = true)
	private void receiveStalactiteDrip(BlockState state, Level level, BlockPos pos, Fluid fluid, CallbackInfo ci)
	{
		if (CauldronPipes.shouldBlockVanillaWorldMutation(level, pos, state))
		{
			ci.cancel();
		}
	}

	@Inject(method = "lowerFillLevel", at = @At("HEAD"), cancellable = true)
	private static void lowerFillLevel(BlockState state, Level level, BlockPos pos, CallbackInfo ci)
	{
		if (CauldronPipes.shouldBlockVanillaWorldMutation(level, pos, state))
		{
			ci.cancel();
		}
	}

	@Inject(method = "entityInside", at = @At("HEAD"), cancellable = true)
	private void entityInside(BlockState state, Level level, BlockPos pos, Entity entity, InsideBlockEffectApplier effectApplier, boolean insideOpenBlock,
		CallbackInfo ci)
	{
		if (entity.isOnFire() && CauldronPipes.shouldBlockVanillaWorldMutation(level, pos, state))
		{
			ci.cancel();
		}
	}
}
