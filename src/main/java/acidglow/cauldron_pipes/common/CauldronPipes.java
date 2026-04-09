package acidglow.cauldron_pipes.common;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import acidglow.cauldron_pipes.common.gametest.CauldronPipesGameTests;
import acidglow.cauldron_pipes.common.registries.ModBlockEntityTypes;
import acidglow.cauldron_pipes.common.tile.CauldronBlockEntity;
import acidglow.cauldron_pipes.mixin.CauldronInteractionDispatcherAccessor;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.BlockPos;
import net.minecraft.core.cauldron.CauldronInteraction;
import net.minecraft.core.cauldron.CauldronInteractions;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BucketItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUtils;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.fml.ModLoadingContext;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.fluids.FluidType;

@Mod(CauldronPipes.MODID)
public class CauldronPipes
{
	public static final String MODID = "cauldron_pipes";
	public static final Logger LOGGER = LogManager.getLogger();

	public CauldronPipes()
	{
		IEventBus fml_bus = ModLoadingContext.get().getActiveContainer().getEventBus();
		fml_bus.addListener(this::onCommonSetup);
		fml_bus.addListener(EventPriority.HIGHEST, this::onRegisterCapabilities);
		fml_bus.addListener(CauldronPipesGameTests::registerTests);
		ModBlockEntityTypes.BLOCK_ENTITY_TYPES.register(fml_bus);
		CauldronPipesGameTests.TEST_FUNCTIONS.register(fml_bus);
	}

	public void onCommonSetup(FMLCommonSetupEvent e)
	{
		e.enqueueWork(() ->
		{
			CauldronFluidTransfom.bootStrap();
			this.wrapBucketInteractions();
		});
	}

	public void onRegisterCapabilities(RegisterCapabilitiesEvent e)
	{
		e.registerBlock(Capabilities.Fluid.BLOCK, (level, pos, state, blockEntity, context) ->
		{
			return blockEntity instanceof CauldronBlockEntity cauldronBlockEntity ? cauldronBlockEntity.getFluidTank() : null;
		}, Blocks.CAULDRON, Blocks.WATER_CAULDRON, Blocks.LAVA_CAULDRON);
	}

	public static Identifier rl(String path)
	{
		return Identifier.fromNamespaceAndPath(MODID, path);
	}

	public static InteractionResult tryHandleManagedCauldronItemInteraction(Level level, BlockPos pos, BlockState state, Player player, InteractionHand hand, ItemStack stack)
	{
		BlockEntity blockEntity = level.getBlockEntity(pos);
		if (!(blockEntity instanceof CauldronBlockEntity cauldronBlockEntity))
		{
			return null;
		}

		var fluidTank = cauldronBlockEntity.getFluidTank();
		if (!fluidTank.hasManagedFluid() || fluidTank.getAmount() >= FluidType.BUCKET_VOLUME)
		{
			return null;
		}

		Fluid storedFluid = fluidTank.getStoredFluid();
		if (tryTopUpBucket(level, pos, player, hand, stack, fluidTank, storedFluid))
		{
			return InteractionResult.SUCCESS;
		}

		if (storedFluid == Fluids.WATER && isWaterPotion(stack))
		{
			if (!level.isClientSide())
			{
				if (fluidTank.fill(Fluids.WATER, getWaterBottleFillAmount(fluidTank.getAmount())) <= 0)
				{
					return null;
				}

				Item item = stack.getItem();
				player.setItemInHand(hand, ItemUtils.createFilledResult(stack, player, new ItemStack(Items.GLASS_BOTTLE)));
				player.awardStat(Stats.USE_CAULDRON);
				player.awardStat(Stats.ITEM_USED.get(item));
				level.playSound(null, pos, SoundEvents.BOTTLE_EMPTY, SoundSource.BLOCKS, 1.0F, 1.0F);
				level.gameEvent(null, GameEvent.FLUID_PLACE, pos);
			}

			return InteractionResult.SUCCESS;
		}

		return null;
	}

	private void wrapBucketInteractions()
	{
		this.wrapBucketInteraction(CauldronInteractions.WATER, Items.BUCKET);
		this.wrapBucketInteraction(CauldronInteractions.WATER, Items.WATER_BUCKET);
		this.wrapBucketInteraction(CauldronInteractions.WATER, Items.LAVA_BUCKET);
		this.wrapBucketInteraction(CauldronInteractions.WATER, Items.POWDER_SNOW_BUCKET);
		this.wrapBucketInteraction(CauldronInteractions.LAVA, Items.BUCKET);
		this.wrapBucketInteraction(CauldronInteractions.LAVA, Items.WATER_BUCKET);
		this.wrapBucketInteraction(CauldronInteractions.LAVA, Items.LAVA_BUCKET);
		this.wrapBucketInteraction(CauldronInteractions.LAVA, Items.POWDER_SNOW_BUCKET);
	}

	private void wrapBucketInteraction(CauldronInteraction.Dispatcher dispatcher, Item item)
	{
		var items = ((CauldronInteractionDispatcherAccessor)dispatcher).cauldron_pipes$getItems();
		CauldronInteraction interaction = items.getOrDefault(item, CauldronInteraction.DEFAULT);
		items.put(item, (state, level, pos, player, hand, stack) ->
		{
			return shouldBlockVanillaWorldMutation(level, pos, state) ? InteractionResult.TRY_WITH_EMPTY_HAND : interaction.interact(state, level, pos, player, hand, stack);
		});
	}

	public static boolean shouldBlockVanillaWorldMutation(Level level, BlockPos pos, BlockState state)
	{
		BlockEntity blockEntity = level.getBlockEntity(pos);
		if (blockEntity instanceof CauldronBlockEntity cauldronBlockEntity)
		{
			var fluidTank = cauldronBlockEntity.getFluidTank();
			return fluidTank.hasManagedFluid() && !fluidTank.isVanillaStateCompatible(state);
		}

		return false;
	}

	private static boolean tryTopUpBucket(Level level, BlockPos pos, Player player, InteractionHand hand, ItemStack stack,
		acidglow.cauldron_pipes.common.capabilities.CauldronTank fluidTank, Fluid storedFluid)
	{
		Fluid bucketFluid = getBucketFluid(stack.getItem());
		if (bucketFluid == Fluids.EMPTY || bucketFluid != storedFluid)
		{
			return false;
		}

		if (!level.isClientSide())
		{
			if (fluidTank.fill(bucketFluid, FluidType.BUCKET_VOLUME) <= 0)
			{
				return false;
			}

			Item item = stack.getItem();
			player.setItemInHand(hand, ItemUtils.createFilledResult(stack, player, new ItemStack(Items.BUCKET)));
			player.awardStat(Stats.FILL_CAULDRON);
			player.awardStat(Stats.ITEM_USED.get(item));
			level.playSound(null, pos, bucketFluid == Fluids.LAVA ? SoundEvents.BUCKET_EMPTY_LAVA : SoundEvents.BUCKET_EMPTY, SoundSource.BLOCKS, 1.0F, 1.0F);
			level.gameEvent(null, GameEvent.FLUID_PLACE, pos);
		}

		return true;
	}

	private static Fluid getBucketFluid(Item item)
	{
		if (item == Items.WATER_BUCKET)
		{
			return Fluids.WATER;
		}
		else if (item == Items.LAVA_BUCKET)
		{
			return Fluids.LAVA;
		}

		return Fluids.EMPTY;
	}

	private static boolean isWaterPotion(ItemStack stack)
	{
		if (!stack.is(Items.POTION))
		{
			return false;
		}

		PotionContents potionContents = stack.get(DataComponents.POTION_CONTENTS);
		return potionContents != null && potionContents.is(Potions.WATER);
	}

	private static int getWaterBottleFillAmount(int currentAmount)
	{
		if (currentAmount >= 666)
		{
			return FluidType.BUCKET_VOLUME - currentAmount;
		}

		return 333;
	}

}
