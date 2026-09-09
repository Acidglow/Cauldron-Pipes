package acidglow.cauldron_pipes.common;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.minecraft.core.BlockPos;
import net.minecraft.core.dispenser.BlockSource;
import net.minecraft.core.dispenser.DispenseItemBehavior;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.BucketItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.AbstractCauldronBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DispenserBlock;
import net.minecraft.world.level.block.LayeredCauldronBlock;
import net.minecraft.world.level.block.LevelEvent;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.fluids.FluidType;
import acidglow.cauldron_pipes.common.tile.CauldronBlockEntity;

public record CauldronFluidTransfom(Fluid fluid, BlockState blockState)
{
	private static final List<CauldronFluidTransfom> LIST = new ArrayList<>();
	private static final List<CauldronFluidTransfom> LIST_READONLY = Collections.unmodifiableList(LIST);
	private static final Map<Fluid, CauldronFluidTransfom> BY_FLUIDS = new HashMap<>();

	public static void bootStrap()
	{
		register(Fluids.WATER, Blocks.WATER_CAULDRON.defaultBlockState().setValue(LayeredCauldronBlock.LEVEL, LayeredCauldronBlock.MAX_FILL_LEVEL));
		register(Fluids.LAVA, Blocks.LAVA_CAULDRON.defaultBlockState());
	}

	public static CauldronFluidTransfom getTransform(BlockState cauldronBlockState)
	{
		if (cauldronBlockState.getBlock() instanceof AbstractCauldronBlock cauldronBlock && cauldronBlock.isFull(cauldronBlockState) == true)
		{
			for (CauldronFluidTransfom transform : CauldronFluidTransfom.values())
			{
				if (cauldronBlock == transform.blockState().getBlock())
				{
					return transform;
				}

			}

		}

		return null;
	}

	public static int getFluidAmount(BlockState state)
	{
		if (state.is(Blocks.WATER_CAULDRON))
		{
			return FluidType.BUCKET_VOLUME * state.getValue(LayeredCauldronBlock.LEVEL) / LayeredCauldronBlock.MAX_FILL_LEVEL;
		}

		return getTransform(state) != null ? FluidType.BUCKET_VOLUME : 0;
	}

	public static int getLayeredFillLevel(int amount)
	{
		if (amount <= 0)
		{
			return 0;
		}

		return amount >= FluidType.BUCKET_VOLUME
			? LayeredCauldronBlock.MAX_FILL_LEVEL
			: Math.max(1, (amount * LayeredCauldronBlock.MAX_FILL_LEVEL + FluidType.BUCKET_VOLUME - 1) / FluidType.BUCKET_VOLUME);
	}

	public static float getLayeredContentHeight(int amount)
	{
		int level = getLayeredFillLevel(amount);
		return level <= 0 ? 0.0F : (float) (6.0D + level * 3.0D) / 16.0F;
	}

	public static DispenseItemBehavior wrapDispenseItemBehavior(BucketItem bucket, DispenseItemBehavior fallback)
	{
		if (bucket == Items.BUCKET)
		{
			return wrapFillBucket(fallback);
		}

		CauldronFluidTransfom byFluid = CauldronFluidTransfom.byFluid(bucket.content);

		if (byFluid != null)
		{
			return byFluid.wrapEmptyBucket(fallback);
		}
		else
		{
			return fallback;
		}

	}

	public static DispenseItemBehavior wrapFillBucket(DispenseItemBehavior fallback)
	{
		return new DispenseItemBehavior()
		{
			@Override
			public ItemStack dispense(BlockSource source, ItemStack item)
			{
				Level level = source.level();
				BlockPos pos = source.pos().relative(source.state().getValue(DispenserBlock.FACING));
				BlockEntity blockEntity = level.getBlockEntity(pos);
				if (blockEntity instanceof CauldronBlockEntity cauldron)
				{
					int amount = cauldron.getFluidTank().getAmount();
					if (amount > 0 && amount < FluidType.BUCKET_VOLUME)
					{
						return item;
					}
				}

				CauldronFluidTransfom transform = blockEntity instanceof CauldronBlockEntity cauldron && cauldron.getFluidTank().getAmount() >= FluidType.BUCKET_VOLUME
					? CauldronFluidTransfom.byFluid(cauldron.getFluidTank().getStoredFluid())
					: null;

				if (transform != null)
				{
					level.setBlockAndUpdate(pos, Blocks.CAULDRON.defaultBlockState());
					level.levelEvent(LevelEvent.SOUND_DISPENSER_DISPENSE, source.pos(), 0);
					return new ItemStack(transform.fluid().getBucket());
				}

				return fallback.dispense(source, item);
			}

		};

	}

	public static List<CauldronFluidTransfom> values()
	{
		return LIST_READONLY;
	}

	public static CauldronFluidTransfom byFluid(Fluid fluid)
	{
		return BY_FLUIDS.get(fluid);
	}

	public static void register(Fluid fluid, BlockState blockState)
	{
		register(new CauldronFluidTransfom(fluid, blockState));
	}

	public static void register(CauldronFluidTransfom transfom)
	{
		if (transfom.blockState().getBlock() instanceof AbstractCauldronBlock)
		{
			if (BY_FLUIDS.containsKey(transfom.fluid()) == false)
			{
				LIST.add(transfom);
				BY_FLUIDS.put(transfom.fluid(), transfom);
			}
			else
			{
				throw new IllegalArgumentException("already registered fluid: " + BuiltInRegistries.FLUID.getKey(transfom.fluid()));
			}

		}
		else
		{
			throw new IllegalArgumentException("blockState.getBlock() should be extends from " + AbstractCauldronBlock.class.getName());
		}

	}

	public DispenseItemBehavior wrapEmptyBucket(DispenseItemBehavior fallback)
	{
		return new DispenseItemBehavior()
		{
			@Override
			public ItemStack dispense(BlockSource source, ItemStack item)
			{
				Level level = source.level();
				BlockPos pos = source.pos().relative(source.state().getValue(DispenserBlock.FACING));
				BlockEntity blockEntity = level.getBlockEntity(pos);

				BlockState state = level.getBlockState(pos);
				boolean canUseBucket = blockEntity instanceof CauldronBlockEntity cauldronBlockEntity ? cauldronBlockEntity.getFluidTank().getAmount() <= 0 : true;
				if (canUseBucket && state.getBlock() instanceof AbstractCauldronBlock cauldron && cauldron.isFull(state) == false)
				{
					level.setBlockAndUpdate(pos, blockState());
					level.levelEvent(LevelEvent.SOUND_DISPENSER_DISPENSE, source.pos(), 0);
					return new ItemStack(Items.BUCKET);
				}

				return fallback.dispense(source, item);
			}

		};

	}

	public BlockState createBlockState(int amount)
	{
		if (amount <= 0)
		{
			return Blocks.CAULDRON.defaultBlockState();
		}

		if (this.fluid == Fluids.WATER)
		{
			int level = getLayeredFillLevel(amount);
			return Blocks.WATER_CAULDRON.defaultBlockState().setValue(LayeredCauldronBlock.LEVEL, Math.min(level, LayeredCauldronBlock.MAX_FILL_LEVEL));
		}

		if (amount < FluidType.BUCKET_VOLUME)
		{
			return Blocks.CAULDRON.defaultBlockState();
		}

		return this.blockState();
	}

}
