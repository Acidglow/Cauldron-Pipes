package acidglow.cauldron_pipes.common.gametest;

import java.util.List;
import java.util.function.Consumer;

import acidglow.cauldron_pipes.common.CauldronFluidTransfom;
import acidglow.cauldron_pipes.common.CauldronPipes;
import acidglow.cauldron_pipes.common.tile.CauldronBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.dispenser.BlockSource;
import net.minecraft.core.dispenser.DispenseItemBehavior;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.FunctionGameTestInstance;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.TestData;
import net.minecraft.gametest.framework.TestEnvironmentDefinition;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DispenserBlock;
import net.minecraft.world.level.block.LayeredCauldronBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.DispenserBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;
import net.neoforged.neoforge.capabilities.BlockCapabilityCache;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class CauldronPipesGameTests
{
	private static final Identifier EMPTY_STRUCTURE = Identifier.withDefaultNamespace("empty");
	private static final int DEFAULT_MAX_TICKS = 40;
	private static final BlockPos CAULDRON_POS = new BlockPos(1, 2, 1);
	private static final BlockPos DISPENSER_POS = new BlockPos(1, 2, 0);

	public static final DeferredRegister<Consumer<GameTestHelper>> TEST_FUNCTIONS = DeferredRegister.create(BuiltInRegistries.TEST_FUNCTION,
			CauldronPipes.MODID);

	static
	{
		registerFunction("water_bottle_fill_steps", CauldronPipesGameTests::waterBottleFillSteps);
		registerFunction("partial_bucket_top_up", CauldronPipesGameTests::partialBucketTopUp);
		registerFunction("partial_lava_persists_through_save", CauldronPipesGameTests::partialLavaPersistsThroughSave);
		registerFunction("dispenser_only_extracts_full_bucket", CauldronPipesGameTests::dispenserOnlyExtractsFullBucket);
		registerFunction("cauldron_capability_cache_invalidates_on_block_changes", CauldronPipesGameTests::cauldronCapabilityCacheInvalidatesOnBlockChanges);
	}

	private CauldronPipesGameTests()
	{
	}

	public static void registerTests(RegisterGameTestsEvent event)
	{
		var environment = event.registerEnvironment(CauldronPipes.rl("gametest"), new TestEnvironmentDefinition.AllOf(List.of()));
		registerTest(event, environment, "water_bottle_fill_steps");
		registerTest(event, environment, "partial_bucket_top_up");
		registerTest(event, environment, "partial_lava_persists_through_save");
		registerTest(event, environment, "dispenser_only_extracts_full_bucket");
		registerTest(event, environment, "cauldron_capability_cache_invalidates_on_block_changes");
	}

	private static void registerFunction(String path, Consumer<GameTestHelper> function)
	{
		TEST_FUNCTIONS.register(path, () -> function);
	}

	private static void registerTest(RegisterGameTestsEvent event, net.minecraft.core.Holder<TestEnvironmentDefinition<?>> environment, String path)
	{
		Identifier id = CauldronPipes.rl(path);
		event.registerTest(id, new FunctionGameTestInstance(ResourceKey.create(Registries.TEST_FUNCTION, id),
				new TestData<>(environment, EMPTY_STRUCTURE, DEFAULT_MAX_TICKS, 0, true)));
	}

	private static void waterBottleFillSteps(GameTestHelper helper)
	{
		helper.setBlock(CAULDRON_POS, Blocks.CAULDRON);
		Player player = helper.makeMockPlayer(GameType.SURVIVAL);

		useOnCauldron(helper, player, waterBottle());
		assertCauldron(helper, Fluids.WATER, 333, Blocks.WATER_CAULDRON.defaultBlockState().setValue(LayeredCauldronBlock.LEVEL, 1));
		assertPlayerHeldItem(helper, player, Items.GLASS_BOTTLE, "First water bottle should become a glass bottle");

		useOnCauldron(helper, player, waterBottle());
		assertCauldron(helper, Fluids.WATER, 666, Blocks.WATER_CAULDRON.defaultBlockState().setValue(LayeredCauldronBlock.LEVEL, 2));
		assertPlayerHeldItem(helper, player, Items.GLASS_BOTTLE, "Second water bottle should become a glass bottle");

		useOnCauldron(helper, player, waterBottle());
		assertCauldron(helper, Fluids.WATER, 1000, Blocks.WATER_CAULDRON.defaultBlockState().setValue(LayeredCauldronBlock.LEVEL, 3));
		assertPlayerHeldItem(helper, player, Items.GLASS_BOTTLE, "Third water bottle should become a glass bottle");
		helper.succeed();
	}

	private static void partialBucketTopUp(GameTestHelper helper)
	{
		helper.setBlock(CAULDRON_POS, Blocks.WATER_CAULDRON.defaultBlockState().setValue(LayeredCauldronBlock.LEVEL, 3));
		CauldronBlockEntity cauldron = helper.getBlockEntity(CAULDRON_POS, CauldronBlockEntity.class);
		cauldron.getFluidTank().load(Fluids.WATER, 850);

		Player player = helper.makeMockPlayer(GameType.SURVIVAL);
		useOnCauldron(helper, player, new ItemStack(Items.WATER_BUCKET));

		assertCauldron(helper, Fluids.WATER, 1000, Blocks.WATER_CAULDRON.defaultBlockState().setValue(LayeredCauldronBlock.LEVEL, 3));
		assertPlayerHeldItem(helper, player, Items.BUCKET, "Water bucket top-up should return an empty bucket");
		helper.succeed();
	}

	private static void partialLavaPersistsThroughSave(GameTestHelper helper)
	{
		helper.setBlock(CAULDRON_POS, Blocks.CAULDRON);
		CauldronBlockEntity cauldron = helper.getBlockEntity(CAULDRON_POS, CauldronBlockEntity.class);
		cauldron.getFluidTank().fill(Fluids.LAVA, 500);

		CompoundTag saved = cauldron.saveWithFullMetadata(helper.getLevel().registryAccess());
		BlockEntity loaded = BlockEntity.loadStatic(helper.absolutePos(CAULDRON_POS), helper.getBlockState(CAULDRON_POS), saved, helper.getLevel().registryAccess());

		if (!(loaded instanceof CauldronBlockEntity loadedCauldron))
		{
			helper.fail(Component.literal("Expected a cauldron block entity after reloading partial lava"), CAULDRON_POS);
			return;
		}

		if (loadedCauldron.getFluidTank().getStoredFluid() != Fluids.LAVA || loadedCauldron.getFluidTank().getAmount() != 500)
		{
			helper.fail(Component.literal("Partial lava did not survive block entity save/load"), CAULDRON_POS);
			return;
		}

		helper.succeed();
	}

	private static void dispenserOnlyExtractsFullBucket(GameTestHelper helper)
	{
		helper.setBlock(CAULDRON_POS, Blocks.WATER_CAULDRON.defaultBlockState().setValue(LayeredCauldronBlock.LEVEL, 3));
		helper.setBlock(DISPENSER_POS, Blocks.DISPENSER.defaultBlockState().setValue(DispenserBlock.FACING, Direction.SOUTH));

		CauldronBlockEntity cauldron = helper.getBlockEntity(CAULDRON_POS, CauldronBlockEntity.class);
		cauldron.getFluidTank().load(Fluids.WATER, 850);

		DispenserBlockEntity dispenser = helper.getBlockEntity(DISPENSER_POS, DispenserBlockEntity.class);
		BlockSource source = new BlockSource(helper.getLevel(), helper.absolutePos(DISPENSER_POS), helper.getBlockState(DISPENSER_POS), dispenser);
		DispenseItemBehavior vanillaBucketBehavior = DispenserBlock.DISPENSER_REGISTRY.get(Items.BUCKET);

		ItemStack partialResult = CauldronFluidTransfom.wrapFillBucket(vanillaBucketBehavior).dispense(source, new ItemStack(Items.BUCKET));
		if (!partialResult.is(Items.BUCKET))
		{
			helper.fail(Component.literal("Partial cauldrons must not dispense filled buckets"), CAULDRON_POS);
			return;
		}

		assertCauldron(helper, Fluids.WATER, 850, Blocks.WATER_CAULDRON.defaultBlockState().setValue(LayeredCauldronBlock.LEVEL, 3));
		cauldron.getFluidTank().fill(Fluids.WATER, 150);

		ItemStack fullResult = CauldronFluidTransfom.wrapFillBucket(vanillaBucketBehavior).dispense(source, new ItemStack(Items.BUCKET));
		if (!fullResult.is(Items.WATER_BUCKET))
		{
			helper.fail(Component.literal("Full cauldrons must dispense a filled bucket"), CAULDRON_POS);
			return;
		}

		assertCauldron(helper, Fluids.EMPTY, 0, Blocks.CAULDRON.defaultBlockState());
		helper.succeed();
	}

	private static void cauldronCapabilityCacheInvalidatesOnBlockChanges(GameTestHelper helper)
	{
		helper.setBlock(CAULDRON_POS, Blocks.CAULDRON);

		BlockCapabilityCache<?, Direction> cache = BlockCapabilityCache.create(Capabilities.Fluid.BLOCK, helper.getLevel(), helper.absolutePos(CAULDRON_POS), Direction.UP);
		if (cache.getCapability() == null)
		{
			helper.fail(Component.literal("Expected a fluid capability for a placed cauldron"), CAULDRON_POS);
			return;
		}

		helper.setBlock(CAULDRON_POS, Blocks.AIR);
		if (cache.getCapability() != null)
		{
			helper.fail(Component.literal("Capability cache stayed populated after the cauldron was removed"), CAULDRON_POS);
			return;
		}

		helper.setBlock(CAULDRON_POS, Blocks.CAULDRON);
		if (cache.getCapability() == null)
		{
			helper.fail(Component.literal("Capability cache was not refreshed after the cauldron was placed again"), CAULDRON_POS);
			return;
		}

		helper.succeed();
	}

	private static void useOnCauldron(GameTestHelper helper, Player player, ItemStack stack)
	{
		player.setItemInHand(InteractionHand.MAIN_HAND, stack);
		helper.useBlock(CAULDRON_POS, player);
	}

	private static ItemStack waterBottle()
	{
		return PotionContents.createItemStack(Items.POTION, Potions.WATER);
	}

	private static void assertCauldron(GameTestHelper helper, Fluid expectedFluid, int expectedAmount, BlockState expectedState)
	{
		CauldronBlockEntity cauldron = helper.getBlockEntity(CAULDRON_POS, CauldronBlockEntity.class);
		if (cauldron.getFluidTank().getStoredFluid() != expectedFluid || cauldron.getFluidTank().getAmount() != expectedAmount)
		{
			helper.fail(Component.literal("Expected " + expectedAmount + "mb of " + expectedFluid + " but found " + cauldron.getFluidTank().getAmount() + "mb of "
					+ cauldron.getFluidTank().getStoredFluid()), CAULDRON_POS);
			return;
		}

		BlockState actualState = helper.getBlockState(CAULDRON_POS);
		if (!actualState.equals(expectedState))
		{
			helper.fail(Component.literal("Expected block state " + expectedState + " but found " + actualState), CAULDRON_POS);
		}
	}

	private static void assertPlayerHeldItem(GameTestHelper helper, Player player, net.minecraft.world.item.Item expectedItem, String message)
	{
		if (!player.getItemInHand(InteractionHand.MAIN_HAND).is(expectedItem))
		{
			helper.fail(Component.literal(message), CAULDRON_POS);
		}
	}

}
