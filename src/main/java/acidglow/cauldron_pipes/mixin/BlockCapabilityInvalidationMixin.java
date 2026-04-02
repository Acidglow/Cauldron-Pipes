package acidglow.cauldron_pipes.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.AbstractCauldronBlock;
import net.minecraft.world.level.block.state.BlockState;

@Mixin(AbstractCauldronBlock.class)
public class BlockCapabilityInvalidationMixin
{
	@Inject(method = "onPlace", at = @At("TAIL"))
	private void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston, CallbackInfo ci)
	{
		level.invalidateCapabilities(pos);
	}

	@Inject(method = "affectNeighborsAfterRemoval", at = @At("TAIL"))
	private void affectNeighborsAfterRemoval(BlockState state, ServerLevel level, BlockPos pos, boolean movedByPiston, CallbackInfo ci)
	{
		level.invalidateCapabilities(pos);
	}
}
