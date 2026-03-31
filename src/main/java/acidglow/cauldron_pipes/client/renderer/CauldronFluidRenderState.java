package acidglow.cauldron_pipes.client.renderer;

import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;

public class CauldronFluidRenderState extends BlockEntityRenderState
{
	public Fluid fluid = Fluids.EMPTY;
	public int amount = 0;
	public float surfaceHeight = 0.0F;

	public boolean shouldRenderFluid()
	{
		return this.fluid != Fluids.EMPTY && this.amount > 0 && this.surfaceHeight > 0.0F;
	}

}
