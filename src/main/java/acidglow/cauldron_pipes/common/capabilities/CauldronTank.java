package acidglow.cauldron_pipes.common.capabilities;

import java.util.Objects;

import acidglow.cauldron_pipes.common.CauldronFluidTransfom;
import acidglow.cauldron_pipes.common.tile.CauldronBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.fluids.FluidType;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.TransferPreconditions;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import net.neoforged.neoforge.transfer.transaction.SnapshotJournal;
import net.neoforged.neoforge.transfer.transaction.TransactionContext;

public class CauldronTank extends SnapshotJournal<CauldronTank.Snapshot> implements ResourceHandler<FluidResource>
{
	static final class Snapshot
	{
		private final FluidResource fluid;
		private final int amount;

		private Snapshot(FluidResource fluid, int amount)
		{
			this.fluid = fluid;
			this.amount = amount;
		}
	}

	private final CauldronBlockEntity blockEntity;
	private FluidResource fluid = FluidResource.EMPTY;
	private int amount = 0;

	public CauldronTank(CauldronBlockEntity blockEntity)
	{
		this.blockEntity = blockEntity;
		this.syncFromBlockState(blockEntity.getBlockState());
	}

	@Override
	public int size()
	{
		return 1;
	}

	@Override
	public FluidResource getResource(int index)
	{
		Objects.checkIndex(index, size());
		return this.getFluid();
	}

	public FluidResource getFluid()
	{
		return this.fluid;
	}

	@Override
	public long getAmountAsLong(int index)
	{
		Objects.checkIndex(index, size());
		if (this.isRemoved() == true || this.getFluid().isEmpty() == true)
		{
			return 0;
		}

		return this.amount;
	}

	@Override
	public long getCapacityAsLong(int index, FluidResource resource)
	{
		Objects.checkIndex(index, size());
		if (resource.isEmpty())
		{
			return this.getTankCapacity();
		}

		return this.isFluidValid(resource) == true ? this.getTankCapacity() : 0;
	}

	@Override
	public boolean isValid(int index, FluidResource resource)
	{
		Objects.checkIndex(index, size());
		TransferPreconditions.checkNonEmpty(resource);
		return this.isFluidValid(resource);
	}

	@Override
	public int insert(int index, FluidResource resource, int amount, TransactionContext transaction)
	{
		Objects.checkIndex(index, size());
		TransferPreconditions.checkNonEmptyNonNegative(resource, amount);

		if (this.isRemoved() == true || this.isFluidValid(resource) == false)
		{
			return 0;
		}

		FluidResource current = this.getFluid();
		if (current.isEmpty() == false && current.equals(resource) == false)
		{
			return 0;
		}

		int filling = Math.min(amount, this.getTankCapacity() - this.amount);
		if (filling <= 0)
		{
			return 0;
		}

		this.updateSnapshots(transaction);
		this.fluid = resource;
		this.amount += filling;
		return filling;
	}

	@Override
	public int extract(int index, FluidResource resource, int amount, TransactionContext transaction)
	{
		Objects.checkIndex(index, size());
		TransferPreconditions.checkNonEmptyNonNegative(resource, amount);

		if (this.isRemoved() == true)
		{
			return 0;
		}

		FluidResource current = this.getFluid();
		if (current.isEmpty() == true || current.equals(resource) == false)
		{
			return 0;
		}

		int draining = Math.min(amount, this.amount);
		if (draining <= 0)
		{
			return 0;
		}

		this.updateSnapshots(transaction);
		this.amount -= draining;

		if (this.amount <= 0)
		{
			this.fluid = FluidResource.EMPTY;
			this.amount = 0;
		}

		return draining;
	}

	private int getTankCapacity()
	{
		return FluidType.BUCKET_VOLUME;
	}

	public boolean hasPartialBucket()
	{
		return this.amount > 0 && this.amount < this.getTankCapacity();
	}

	public int getAmount()
	{
		return this.amount;
	}

	public int fill(Fluid fluid, int amount)
	{
		if (this.isRemoved() == true || amount <= 0)
		{
			return 0;
		}

		CauldronFluidTransfom transform = fluid != Fluids.EMPTY ? CauldronFluidTransfom.byFluid(fluid) : null;
		if (transform == null)
		{
			return 0;
		}

		Fluid storedFluid = this.getStoredFluid();
		if (storedFluid != Fluids.EMPTY && storedFluid != fluid)
		{
			return 0;
		}

		int filling = Math.min(amount, this.getTankCapacity() - this.amount);
		if (filling <= 0)
		{
			return 0;
		}

		this.fluid = FluidResource.of(fluid);
		this.amount += filling;
		this.commitStateChange();
		return filling;
	}

	public boolean hasManagedFluid()
	{
		return this.amount > 0 && this.getStoredFluid() != Fluids.EMPTY;
	}

	public Fluid getStoredFluid()
	{
		return this.fluid.isEmpty() ? Fluids.EMPTY : this.fluid.getFluid();
	}

	public boolean isVanillaStateCompatible(BlockState state)
	{
		Fluid stateFluid = Fluids.EMPTY;
		int stateAmount = CauldronFluidTransfom.getFluidAmount(state);

		if (stateAmount > 0)
		{
			CauldronFluidTransfom transform = CauldronFluidTransfom.getTransform(state);
			if (transform != null)
			{
				stateFluid = transform.fluid();
			}
			else if (state.is(Blocks.WATER_CAULDRON))
			{
				stateFluid = Fluids.WATER;
			}
		}

		return this.amount == stateAmount && this.getStoredFluid() == stateFluid;
	}

	public void load(Fluid fluid, int amount)
	{
		this.setStored(fluid, amount);
	}

	public void syncFromBlockState(BlockState state)
	{
		Fluid fluid = Fluids.EMPTY;
		int amount = CauldronFluidTransfom.getFluidAmount(state);

		if (amount > 0)
		{
			CauldronFluidTransfom transform = CauldronFluidTransfom.getTransform(state);
			if (transform != null)
			{
				fluid = transform.fluid();
			}
			else if (state.is(Blocks.WATER_CAULDRON))
			{
				fluid = Fluids.WATER;
			}
		}

		this.setStored(fluid, amount);
	}

	public BlockState createRenderedBlockState()
	{
		CauldronFluidTransfom transform = this.fluid.isEmpty() ? null : CauldronFluidTransfom.byFluid(this.fluid.getFluid());
		return transform != null ? transform.createBlockState(this.amount) : Blocks.CAULDRON.defaultBlockState();
	}

	private boolean isFluidValid(FluidResource resource)
	{
		return resource.isComponentsPatchEmpty() == true && CauldronFluidTransfom.byFluid(resource.getFluid()) != null;
	}

	private void setStored(Fluid fluid, int amount)
	{
		int clampedAmount = Math.clamp(amount, 0, this.getTankCapacity());
		CauldronFluidTransfom transform = fluid != Fluids.EMPTY ? CauldronFluidTransfom.byFluid(fluid) : null;
		if (clampedAmount <= 0 || transform == null)
		{
			this.fluid = FluidResource.EMPTY;
			this.amount = 0;
			return;
		}

		this.fluid = FluidResource.of(fluid);
		this.amount = clampedAmount;
	}

	@Override
	protected Snapshot createSnapshot()
	{
		return new Snapshot(this.fluid, this.amount);
	}

	@Override
	protected void revertToSnapshot(Snapshot snapshot)
	{
		this.fluid = snapshot.fluid;
		this.amount = snapshot.amount;
	}

	@Override
	protected void onRootCommit(Snapshot snapshot)
	{
		this.commitStateChange();
	}

	private void commitStateChange()
	{
		Level level = this.getLevel();
		if (level == null || this.isRemoved() == true)
		{
			return;
		}

		BlockPos pos = this.getBlockPos();
		BlockState oldState = this.blockEntity.getBlockState();
		BlockState newState = this.createRenderedBlockState();
		this.blockEntity.setChanged();

		if (oldState.equals(newState) == false)
		{
			level.setBlock(pos, newState, Block.UPDATE_ALL);
			level.invalidateCapabilities(pos);
		}
		else
		{
			level.sendBlockUpdated(pos, oldState, newState, Block.UPDATE_ALL);
		}
	}

	public CauldronBlockEntity getBlockEntity()
	{
		return this.blockEntity;
	}

	private boolean isRemoved()
	{
		return this.blockEntity.isRemoved();
	}

	private Level getLevel()
	{
		return this.blockEntity.getLevel();
	}

	private BlockPos getBlockPos()
	{
		return this.blockEntity.getBlockPos();
	}

}
