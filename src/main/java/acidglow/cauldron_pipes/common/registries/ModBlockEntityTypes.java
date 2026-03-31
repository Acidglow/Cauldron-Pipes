package acidglow.cauldron_pipes.common.registries;

import acidglow.cauldron_pipes.common.CauldronPipes;
import acidglow.cauldron_pipes.common.tile.CauldronBlockEntity;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModBlockEntityTypes
{
	public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITY_TYPES = DeferredRegister.create(BuiltInRegistries.BLOCK_ENTITY_TYPE, CauldronPipes.MODID);

	public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<CauldronBlockEntity>> CAULDRON = BLOCK_ENTITY_TYPES.register("cauldron",
			() -> new BlockEntityType<>(CauldronBlockEntity::new, cauldronBlocks()));

	private static Block[] cauldronBlocks()
	{
		return new Block[]
		{
				Blocks.CAULDRON,
				Blocks.WATER_CAULDRON,
				Blocks.LAVA_CAULDRON,
				Blocks.POWDER_SNOW_CAULDRON
		};
	}
}
