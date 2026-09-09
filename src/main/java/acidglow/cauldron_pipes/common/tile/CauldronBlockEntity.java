package acidglow.cauldron_pipes.common.tile;

import acidglow.cauldron_pipes.common.capabilities.CauldronTank;
import acidglow.cauldron_pipes.common.registries.ModBlockEntityTypes;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

public class CauldronBlockEntity extends BlockEntity
{
	private static final String FLUID_TAG = "Fluid";
	private static final String AMOUNT_TAG = "Amount";
	private final CauldronTank fluidTank = new CauldronTank(this);

	public CauldronBlockEntity(BlockPos blockPos, BlockState state)
	{
		super(ModBlockEntityTypes.CAULDRON.get(), blockPos, state);
	}

	public CauldronTank getFluidTank()
	{
		return this.fluidTank;
	}

	@Override
	protected void saveAdditional(ValueOutput output)
	{
		super.saveAdditional(output);

		if (this.fluidTank.getStoredFluid() != Fluids.EMPTY)
		{
			output.store(FLUID_TAG, BuiltInRegistries.FLUID.byNameCodec(), this.fluidTank.getStoredFluid());
		}

		output.putInt(AMOUNT_TAG, this.fluidTank.getAmount());
	}

	@Override
	protected void loadAdditional(ValueInput input)
	{
		super.loadAdditional(input);
		Fluid fluid = input.read(FLUID_TAG, BuiltInRegistries.FLUID.byNameCodec()).orElse(Fluids.EMPTY);
		int amount = input.getIntOr(AMOUNT_TAG, -1);

		if (amount >= 0)
		{
			this.fluidTank.load(fluid, amount);
		}
		else
		{
			this.fluidTank.syncFromBlockState(this.getBlockState());
		}
	}

	@Override
	@SuppressWarnings("deprecation") // BlockEntity#setBlockState remains the block-state synchronization hook.
	public void setBlockState(BlockState state)
	{
		super.setBlockState(state);

		if (state.equals(this.fluidTank.createRenderedBlockState()) == false)
		{
			this.fluidTank.syncFromBlockState(state);
		}
	}

	@Override
	public Packet<ClientGamePacketListener> getUpdatePacket()
	{
		return ClientboundBlockEntityDataPacket.create(this);
	}

	@Override
	public CompoundTag getUpdateTag(HolderLookup.Provider registries)
	{
		return this.saveWithoutMetadata(registries);
	}

}
