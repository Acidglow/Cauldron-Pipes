package net.minecraft.client.renderer;

import com.google.common.collect.Maps;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.client.renderer.chunk.ChunkSectionLayer;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.util.Util;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class ItemBlockRenderTypes {
    @Deprecated
    private static final Map<Block, ChunkSectionLayer> TYPE_BY_BLOCK = Util.make(Maps.newHashMap(), p_470488_ -> {
        ChunkSectionLayer chunksectionlayer = ChunkSectionLayer.TRIPWIRE;
        p_470488_.put(Blocks.TRIPWIRE, chunksectionlayer);
        ChunkSectionLayer chunksectionlayer1 = ChunkSectionLayer.CUTOUT;
        p_470488_.put(Blocks.GRASS_BLOCK, chunksectionlayer1);
        p_470488_.put(Blocks.IRON_BARS, chunksectionlayer1);
        Blocks.COPPER_BARS.forEach(p_436501_ -> p_470488_.put(p_436501_, chunksectionlayer1));
        p_470488_.put(Blocks.TRIPWIRE_HOOK, chunksectionlayer1);
        p_470488_.put(Blocks.HOPPER, chunksectionlayer1);
        p_470488_.put(Blocks.IRON_CHAIN, chunksectionlayer1);
        Blocks.COPPER_CHAIN.forEach(p_436498_ -> p_470488_.put(p_436498_, chunksectionlayer1));
        p_470488_.put(Blocks.JUNGLE_LEAVES, chunksectionlayer1);
        p_470488_.put(Blocks.OAK_LEAVES, chunksectionlayer1);
        p_470488_.put(Blocks.SPRUCE_LEAVES, chunksectionlayer1);
        p_470488_.put(Blocks.ACACIA_LEAVES, chunksectionlayer1);
        p_470488_.put(Blocks.CHERRY_LEAVES, chunksectionlayer1);
        p_470488_.put(Blocks.BIRCH_LEAVES, chunksectionlayer1);
        p_470488_.put(Blocks.DARK_OAK_LEAVES, chunksectionlayer1);
        p_470488_.put(Blocks.PALE_OAK_LEAVES, chunksectionlayer1);
        p_470488_.put(Blocks.AZALEA_LEAVES, chunksectionlayer1);
        p_470488_.put(Blocks.FLOWERING_AZALEA_LEAVES, chunksectionlayer1);
        p_470488_.put(Blocks.MANGROVE_ROOTS, chunksectionlayer1);
        p_470488_.put(Blocks.MANGROVE_LEAVES, chunksectionlayer1);
        p_470488_.put(Blocks.OAK_SAPLING, chunksectionlayer1);
        p_470488_.put(Blocks.SPRUCE_SAPLING, chunksectionlayer1);
        p_470488_.put(Blocks.BIRCH_SAPLING, chunksectionlayer1);
        p_470488_.put(Blocks.JUNGLE_SAPLING, chunksectionlayer1);
        p_470488_.put(Blocks.ACACIA_SAPLING, chunksectionlayer1);
        p_470488_.put(Blocks.CHERRY_SAPLING, chunksectionlayer1);
        p_470488_.put(Blocks.DARK_OAK_SAPLING, chunksectionlayer1);
        p_470488_.put(Blocks.PALE_OAK_SAPLING, chunksectionlayer1);
        p_470488_.put(Blocks.WHITE_BED, chunksectionlayer1);
        p_470488_.put(Blocks.ORANGE_BED, chunksectionlayer1);
        p_470488_.put(Blocks.MAGENTA_BED, chunksectionlayer1);
        p_470488_.put(Blocks.LIGHT_BLUE_BED, chunksectionlayer1);
        p_470488_.put(Blocks.YELLOW_BED, chunksectionlayer1);
        p_470488_.put(Blocks.LIME_BED, chunksectionlayer1);
        p_470488_.put(Blocks.PINK_BED, chunksectionlayer1);
        p_470488_.put(Blocks.GRAY_BED, chunksectionlayer1);
        p_470488_.put(Blocks.LIGHT_GRAY_BED, chunksectionlayer1);
        p_470488_.put(Blocks.CYAN_BED, chunksectionlayer1);
        p_470488_.put(Blocks.PURPLE_BED, chunksectionlayer1);
        p_470488_.put(Blocks.BLUE_BED, chunksectionlayer1);
        p_470488_.put(Blocks.BROWN_BED, chunksectionlayer1);
        p_470488_.put(Blocks.GREEN_BED, chunksectionlayer1);
        p_470488_.put(Blocks.RED_BED, chunksectionlayer1);
        p_470488_.put(Blocks.BLACK_BED, chunksectionlayer1);
        p_470488_.put(Blocks.POWERED_RAIL, chunksectionlayer1);
        p_470488_.put(Blocks.DETECTOR_RAIL, chunksectionlayer1);
        p_470488_.put(Blocks.COBWEB, chunksectionlayer1);
        p_470488_.put(Blocks.SHORT_GRASS, chunksectionlayer1);
        p_470488_.put(Blocks.FERN, chunksectionlayer1);
        p_470488_.put(Blocks.BUSH, chunksectionlayer1);
        p_470488_.put(Blocks.DEAD_BUSH, chunksectionlayer1);
        p_470488_.put(Blocks.SHORT_DRY_GRASS, chunksectionlayer1);
        p_470488_.put(Blocks.TALL_DRY_GRASS, chunksectionlayer1);
        p_470488_.put(Blocks.SEAGRASS, chunksectionlayer1);
        p_470488_.put(Blocks.TALL_SEAGRASS, chunksectionlayer1);
        p_470488_.put(Blocks.DANDELION, chunksectionlayer1);
        p_470488_.put(Blocks.OPEN_EYEBLOSSOM, chunksectionlayer1);
        p_470488_.put(Blocks.CLOSED_EYEBLOSSOM, chunksectionlayer1);
        p_470488_.put(Blocks.POPPY, chunksectionlayer1);
        p_470488_.put(Blocks.BLUE_ORCHID, chunksectionlayer1);
        p_470488_.put(Blocks.ALLIUM, chunksectionlayer1);
        p_470488_.put(Blocks.AZURE_BLUET, chunksectionlayer1);
        p_470488_.put(Blocks.RED_TULIP, chunksectionlayer1);
        p_470488_.put(Blocks.ORANGE_TULIP, chunksectionlayer1);
        p_470488_.put(Blocks.WHITE_TULIP, chunksectionlayer1);
        p_470488_.put(Blocks.PINK_TULIP, chunksectionlayer1);
        p_470488_.put(Blocks.OXEYE_DAISY, chunksectionlayer1);
        p_470488_.put(Blocks.CORNFLOWER, chunksectionlayer1);
        p_470488_.put(Blocks.WITHER_ROSE, chunksectionlayer1);
        p_470488_.put(Blocks.LILY_OF_THE_VALLEY, chunksectionlayer1);
        p_470488_.put(Blocks.BROWN_MUSHROOM, chunksectionlayer1);
        p_470488_.put(Blocks.RED_MUSHROOM, chunksectionlayer1);
        p_470488_.put(Blocks.TORCH, chunksectionlayer1);
        p_470488_.put(Blocks.WALL_TORCH, chunksectionlayer1);
        p_470488_.put(Blocks.SOUL_TORCH, chunksectionlayer1);
        p_470488_.put(Blocks.SOUL_WALL_TORCH, chunksectionlayer1);
        p_470488_.put(Blocks.COPPER_TORCH, chunksectionlayer1);
        p_470488_.put(Blocks.COPPER_WALL_TORCH, chunksectionlayer1);
        p_470488_.put(Blocks.FIRE, chunksectionlayer1);
        p_470488_.put(Blocks.SOUL_FIRE, chunksectionlayer1);
        p_470488_.put(Blocks.SPAWNER, chunksectionlayer1);
        p_470488_.put(Blocks.TRIAL_SPAWNER, chunksectionlayer1);
        p_470488_.put(Blocks.VAULT, chunksectionlayer1);
        p_470488_.put(Blocks.WHEAT, chunksectionlayer1);
        p_470488_.put(Blocks.OAK_DOOR, chunksectionlayer1);
        p_470488_.put(Blocks.LADDER, chunksectionlayer1);
        p_470488_.put(Blocks.RAIL, chunksectionlayer1);
        p_470488_.put(Blocks.IRON_DOOR, chunksectionlayer1);
        p_470488_.put(Blocks.REDSTONE_TORCH, chunksectionlayer1);
        p_470488_.put(Blocks.REDSTONE_WALL_TORCH, chunksectionlayer1);
        p_470488_.put(Blocks.CACTUS, chunksectionlayer1);
        p_470488_.put(Blocks.SUGAR_CANE, chunksectionlayer1);
        p_470488_.put(Blocks.REPEATER, chunksectionlayer1);
        p_470488_.put(Blocks.OAK_TRAPDOOR, chunksectionlayer1);
        p_470488_.put(Blocks.SPRUCE_TRAPDOOR, chunksectionlayer1);
        p_470488_.put(Blocks.BIRCH_TRAPDOOR, chunksectionlayer1);
        p_470488_.put(Blocks.JUNGLE_TRAPDOOR, chunksectionlayer1);
        p_470488_.put(Blocks.ACACIA_TRAPDOOR, chunksectionlayer1);
        p_470488_.put(Blocks.CHERRY_TRAPDOOR, chunksectionlayer1);
        p_470488_.put(Blocks.DARK_OAK_TRAPDOOR, chunksectionlayer1);
        p_470488_.put(Blocks.PALE_OAK_TRAPDOOR, chunksectionlayer1);
        p_470488_.put(Blocks.CRIMSON_TRAPDOOR, chunksectionlayer1);
        p_470488_.put(Blocks.WARPED_TRAPDOOR, chunksectionlayer1);
        p_470488_.put(Blocks.MANGROVE_TRAPDOOR, chunksectionlayer1);
        p_470488_.put(Blocks.BAMBOO_TRAPDOOR, chunksectionlayer1);
        p_470488_.put(Blocks.COPPER_TRAPDOOR, chunksectionlayer1);
        p_470488_.put(Blocks.EXPOSED_COPPER_TRAPDOOR, chunksectionlayer1);
        p_470488_.put(Blocks.WEATHERED_COPPER_TRAPDOOR, chunksectionlayer1);
        p_470488_.put(Blocks.OXIDIZED_COPPER_TRAPDOOR, chunksectionlayer1);
        p_470488_.put(Blocks.WAXED_COPPER_TRAPDOOR, chunksectionlayer1);
        p_470488_.put(Blocks.WAXED_EXPOSED_COPPER_TRAPDOOR, chunksectionlayer1);
        p_470488_.put(Blocks.WAXED_WEATHERED_COPPER_TRAPDOOR, chunksectionlayer1);
        p_470488_.put(Blocks.WAXED_OXIDIZED_COPPER_TRAPDOOR, chunksectionlayer1);
        p_470488_.put(Blocks.ATTACHED_PUMPKIN_STEM, chunksectionlayer1);
        p_470488_.put(Blocks.ATTACHED_MELON_STEM, chunksectionlayer1);
        p_470488_.put(Blocks.PUMPKIN_STEM, chunksectionlayer1);
        p_470488_.put(Blocks.MELON_STEM, chunksectionlayer1);
        p_470488_.put(Blocks.VINE, chunksectionlayer1);
        p_470488_.put(Blocks.PALE_MOSS_CARPET, chunksectionlayer1);
        p_470488_.put(Blocks.PALE_HANGING_MOSS, chunksectionlayer1);
        p_470488_.put(Blocks.GLOW_LICHEN, chunksectionlayer1);
        p_470488_.put(Blocks.RESIN_CLUMP, chunksectionlayer1);
        p_470488_.put(Blocks.LILY_PAD, chunksectionlayer1);
        p_470488_.put(Blocks.NETHER_WART, chunksectionlayer1);
        p_470488_.put(Blocks.BREWING_STAND, chunksectionlayer1);
        p_470488_.put(Blocks.COCOA, chunksectionlayer1);
        p_470488_.put(Blocks.FLOWER_POT, chunksectionlayer1);
        p_470488_.put(Blocks.POTTED_OAK_SAPLING, chunksectionlayer1);
        p_470488_.put(Blocks.POTTED_SPRUCE_SAPLING, chunksectionlayer1);
        p_470488_.put(Blocks.POTTED_BIRCH_SAPLING, chunksectionlayer1);
        p_470488_.put(Blocks.POTTED_JUNGLE_SAPLING, chunksectionlayer1);
        p_470488_.put(Blocks.POTTED_ACACIA_SAPLING, chunksectionlayer1);
        p_470488_.put(Blocks.POTTED_CHERRY_SAPLING, chunksectionlayer1);
        p_470488_.put(Blocks.POTTED_DARK_OAK_SAPLING, chunksectionlayer1);
        p_470488_.put(Blocks.POTTED_PALE_OAK_SAPLING, chunksectionlayer1);
        p_470488_.put(Blocks.POTTED_MANGROVE_PROPAGULE, chunksectionlayer1);
        p_470488_.put(Blocks.POTTED_FERN, chunksectionlayer1);
        p_470488_.put(Blocks.POTTED_DANDELION, chunksectionlayer1);
        p_470488_.put(Blocks.POTTED_POPPY, chunksectionlayer1);
        p_470488_.put(Blocks.POTTED_OPEN_EYEBLOSSOM, chunksectionlayer1);
        p_470488_.put(Blocks.POTTED_CLOSED_EYEBLOSSOM, chunksectionlayer1);
        p_470488_.put(Blocks.POTTED_BLUE_ORCHID, chunksectionlayer1);
        p_470488_.put(Blocks.POTTED_ALLIUM, chunksectionlayer1);
        p_470488_.put(Blocks.POTTED_AZURE_BLUET, chunksectionlayer1);
        p_470488_.put(Blocks.POTTED_RED_TULIP, chunksectionlayer1);
        p_470488_.put(Blocks.POTTED_ORANGE_TULIP, chunksectionlayer1);
        p_470488_.put(Blocks.POTTED_WHITE_TULIP, chunksectionlayer1);
        p_470488_.put(Blocks.POTTED_PINK_TULIP, chunksectionlayer1);
        p_470488_.put(Blocks.POTTED_OXEYE_DAISY, chunksectionlayer1);
        p_470488_.put(Blocks.POTTED_CORNFLOWER, chunksectionlayer1);
        p_470488_.put(Blocks.POTTED_LILY_OF_THE_VALLEY, chunksectionlayer1);
        p_470488_.put(Blocks.POTTED_WITHER_ROSE, chunksectionlayer1);
        p_470488_.put(Blocks.POTTED_RED_MUSHROOM, chunksectionlayer1);
        p_470488_.put(Blocks.POTTED_BROWN_MUSHROOM, chunksectionlayer1);
        p_470488_.put(Blocks.POTTED_DEAD_BUSH, chunksectionlayer1);
        p_470488_.put(Blocks.POTTED_CACTUS, chunksectionlayer1);
        p_470488_.put(Blocks.POTTED_AZALEA, chunksectionlayer1);
        p_470488_.put(Blocks.POTTED_FLOWERING_AZALEA, chunksectionlayer1);
        p_470488_.put(Blocks.POTTED_TORCHFLOWER, chunksectionlayer1);
        p_470488_.put(Blocks.CARROTS, chunksectionlayer1);
        p_470488_.put(Blocks.POTATOES, chunksectionlayer1);
        p_470488_.put(Blocks.COMPARATOR, chunksectionlayer1);
        p_470488_.put(Blocks.ACTIVATOR_RAIL, chunksectionlayer1);
        p_470488_.put(Blocks.IRON_TRAPDOOR, chunksectionlayer1);
        p_470488_.put(Blocks.SUNFLOWER, chunksectionlayer1);
        p_470488_.put(Blocks.LILAC, chunksectionlayer1);
        p_470488_.put(Blocks.ROSE_BUSH, chunksectionlayer1);
        p_470488_.put(Blocks.PEONY, chunksectionlayer1);
        p_470488_.put(Blocks.TALL_GRASS, chunksectionlayer1);
        p_470488_.put(Blocks.LARGE_FERN, chunksectionlayer1);
        p_470488_.put(Blocks.SPRUCE_DOOR, chunksectionlayer1);
        p_470488_.put(Blocks.BIRCH_DOOR, chunksectionlayer1);
        p_470488_.put(Blocks.JUNGLE_DOOR, chunksectionlayer1);
        p_470488_.put(Blocks.ACACIA_DOOR, chunksectionlayer1);
        p_470488_.put(Blocks.CHERRY_DOOR, chunksectionlayer1);
        p_470488_.put(Blocks.DARK_OAK_DOOR, chunksectionlayer1);
        p_470488_.put(Blocks.PALE_OAK_DOOR, chunksectionlayer1);
        p_470488_.put(Blocks.MANGROVE_DOOR, chunksectionlayer1);
        p_470488_.put(Blocks.BAMBOO_DOOR, chunksectionlayer1);
        p_470488_.put(Blocks.COPPER_DOOR, chunksectionlayer1);
        p_470488_.put(Blocks.EXPOSED_COPPER_DOOR, chunksectionlayer1);
        p_470488_.put(Blocks.WEATHERED_COPPER_DOOR, chunksectionlayer1);
        p_470488_.put(Blocks.OXIDIZED_COPPER_DOOR, chunksectionlayer1);
        p_470488_.put(Blocks.WAXED_COPPER_DOOR, chunksectionlayer1);
        p_470488_.put(Blocks.WAXED_EXPOSED_COPPER_DOOR, chunksectionlayer1);
        p_470488_.put(Blocks.WAXED_WEATHERED_COPPER_DOOR, chunksectionlayer1);
        p_470488_.put(Blocks.WAXED_OXIDIZED_COPPER_DOOR, chunksectionlayer1);
        p_470488_.put(Blocks.END_ROD, chunksectionlayer1);
        p_470488_.put(Blocks.CHORUS_PLANT, chunksectionlayer1);
        p_470488_.put(Blocks.CHORUS_FLOWER, chunksectionlayer1);
        p_470488_.put(Blocks.TORCHFLOWER, chunksectionlayer1);
        p_470488_.put(Blocks.TORCHFLOWER_CROP, chunksectionlayer1);
        p_470488_.put(Blocks.PITCHER_PLANT, chunksectionlayer1);
        p_470488_.put(Blocks.PITCHER_CROP, chunksectionlayer1);
        p_470488_.put(Blocks.BEETROOTS, chunksectionlayer1);
        p_470488_.put(Blocks.KELP, chunksectionlayer1);
        p_470488_.put(Blocks.KELP_PLANT, chunksectionlayer1);
        p_470488_.put(Blocks.TURTLE_EGG, chunksectionlayer1);
        p_470488_.put(Blocks.DEAD_TUBE_CORAL, chunksectionlayer1);
        p_470488_.put(Blocks.DEAD_BRAIN_CORAL, chunksectionlayer1);
        p_470488_.put(Blocks.DEAD_BUBBLE_CORAL, chunksectionlayer1);
        p_470488_.put(Blocks.DEAD_FIRE_CORAL, chunksectionlayer1);
        p_470488_.put(Blocks.DEAD_HORN_CORAL, chunksectionlayer1);
        p_470488_.put(Blocks.TUBE_CORAL, chunksectionlayer1);
        p_470488_.put(Blocks.BRAIN_CORAL, chunksectionlayer1);
        p_470488_.put(Blocks.BUBBLE_CORAL, chunksectionlayer1);
        p_470488_.put(Blocks.FIRE_CORAL, chunksectionlayer1);
        p_470488_.put(Blocks.HORN_CORAL, chunksectionlayer1);
        p_470488_.put(Blocks.DEAD_TUBE_CORAL_FAN, chunksectionlayer1);
        p_470488_.put(Blocks.DEAD_BRAIN_CORAL_FAN, chunksectionlayer1);
        p_470488_.put(Blocks.DEAD_BUBBLE_CORAL_FAN, chunksectionlayer1);
        p_470488_.put(Blocks.DEAD_FIRE_CORAL_FAN, chunksectionlayer1);
        p_470488_.put(Blocks.DEAD_HORN_CORAL_FAN, chunksectionlayer1);
        p_470488_.put(Blocks.TUBE_CORAL_FAN, chunksectionlayer1);
        p_470488_.put(Blocks.BRAIN_CORAL_FAN, chunksectionlayer1);
        p_470488_.put(Blocks.BUBBLE_CORAL_FAN, chunksectionlayer1);
        p_470488_.put(Blocks.FIRE_CORAL_FAN, chunksectionlayer1);
        p_470488_.put(Blocks.HORN_CORAL_FAN, chunksectionlayer1);
        p_470488_.put(Blocks.DEAD_TUBE_CORAL_WALL_FAN, chunksectionlayer1);
        p_470488_.put(Blocks.DEAD_BRAIN_CORAL_WALL_FAN, chunksectionlayer1);
        p_470488_.put(Blocks.DEAD_BUBBLE_CORAL_WALL_FAN, chunksectionlayer1);
        p_470488_.put(Blocks.DEAD_FIRE_CORAL_WALL_FAN, chunksectionlayer1);
        p_470488_.put(Blocks.DEAD_HORN_CORAL_WALL_FAN, chunksectionlayer1);
        p_470488_.put(Blocks.TUBE_CORAL_WALL_FAN, chunksectionlayer1);
        p_470488_.put(Blocks.BRAIN_CORAL_WALL_FAN, chunksectionlayer1);
        p_470488_.put(Blocks.BUBBLE_CORAL_WALL_FAN, chunksectionlayer1);
        p_470488_.put(Blocks.FIRE_CORAL_WALL_FAN, chunksectionlayer1);
        p_470488_.put(Blocks.HORN_CORAL_WALL_FAN, chunksectionlayer1);
        p_470488_.put(Blocks.SEA_PICKLE, chunksectionlayer1);
        p_470488_.put(Blocks.CONDUIT, chunksectionlayer1);
        p_470488_.put(Blocks.BAMBOO_SAPLING, chunksectionlayer1);
        p_470488_.put(Blocks.BAMBOO, chunksectionlayer1);
        p_470488_.put(Blocks.POTTED_BAMBOO, chunksectionlayer1);
        p_470488_.put(Blocks.SCAFFOLDING, chunksectionlayer1);
        p_470488_.put(Blocks.STONECUTTER, chunksectionlayer1);
        p_470488_.put(Blocks.LANTERN, chunksectionlayer1);
        p_470488_.put(Blocks.SOUL_LANTERN, chunksectionlayer1);
        Blocks.COPPER_LANTERN.forEach(p_436494_ -> p_470488_.put(p_436494_, chunksectionlayer1));
        p_470488_.put(Blocks.CAMPFIRE, chunksectionlayer1);
        p_470488_.put(Blocks.SOUL_CAMPFIRE, chunksectionlayer1);
        p_470488_.put(Blocks.SWEET_BERRY_BUSH, chunksectionlayer1);
        p_470488_.put(Blocks.WEEPING_VINES, chunksectionlayer1);
        p_470488_.put(Blocks.WEEPING_VINES_PLANT, chunksectionlayer1);
        p_470488_.put(Blocks.TWISTING_VINES, chunksectionlayer1);
        p_470488_.put(Blocks.TWISTING_VINES_PLANT, chunksectionlayer1);
        p_470488_.put(Blocks.NETHER_SPROUTS, chunksectionlayer1);
        p_470488_.put(Blocks.CRIMSON_FUNGUS, chunksectionlayer1);
        p_470488_.put(Blocks.WARPED_FUNGUS, chunksectionlayer1);
        p_470488_.put(Blocks.CRIMSON_ROOTS, chunksectionlayer1);
        p_470488_.put(Blocks.WARPED_ROOTS, chunksectionlayer1);
        p_470488_.put(Blocks.POTTED_CRIMSON_FUNGUS, chunksectionlayer1);
        p_470488_.put(Blocks.POTTED_WARPED_FUNGUS, chunksectionlayer1);
        p_470488_.put(Blocks.POTTED_CRIMSON_ROOTS, chunksectionlayer1);
        p_470488_.put(Blocks.POTTED_WARPED_ROOTS, chunksectionlayer1);
        p_470488_.put(Blocks.CRIMSON_DOOR, chunksectionlayer1);
        p_470488_.put(Blocks.WARPED_DOOR, chunksectionlayer1);
        p_470488_.put(Blocks.POINTED_DRIPSTONE, chunksectionlayer1);
        p_470488_.put(Blocks.SMALL_AMETHYST_BUD, chunksectionlayer1);
        p_470488_.put(Blocks.MEDIUM_AMETHYST_BUD, chunksectionlayer1);
        p_470488_.put(Blocks.LARGE_AMETHYST_BUD, chunksectionlayer1);
        p_470488_.put(Blocks.AMETHYST_CLUSTER, chunksectionlayer1);
        p_470488_.put(Blocks.CAVE_VINES, chunksectionlayer1);
        p_470488_.put(Blocks.CAVE_VINES_PLANT, chunksectionlayer1);
        p_470488_.put(Blocks.SPORE_BLOSSOM, chunksectionlayer1);
        p_470488_.put(Blocks.FLOWERING_AZALEA, chunksectionlayer1);
        p_470488_.put(Blocks.AZALEA, chunksectionlayer1);
        p_470488_.put(Blocks.PINK_PETALS, chunksectionlayer1);
        p_470488_.put(Blocks.WILDFLOWERS, chunksectionlayer1);
        p_470488_.put(Blocks.LEAF_LITTER, chunksectionlayer1);
        p_470488_.put(Blocks.BIG_DRIPLEAF, chunksectionlayer1);
        p_470488_.put(Blocks.BIG_DRIPLEAF_STEM, chunksectionlayer1);
        p_470488_.put(Blocks.SMALL_DRIPLEAF, chunksectionlayer1);
        p_470488_.put(Blocks.HANGING_ROOTS, chunksectionlayer1);
        p_470488_.put(Blocks.SCULK_SENSOR, chunksectionlayer1);
        p_470488_.put(Blocks.CALIBRATED_SCULK_SENSOR, chunksectionlayer1);
        p_470488_.put(Blocks.SCULK_VEIN, chunksectionlayer1);
        p_470488_.put(Blocks.SCULK_SHRIEKER, chunksectionlayer1);
        p_470488_.put(Blocks.MANGROVE_PROPAGULE, chunksectionlayer1);
        p_470488_.put(Blocks.FROGSPAWN, chunksectionlayer1);
        p_470488_.put(Blocks.COPPER_GRATE, chunksectionlayer1);
        p_470488_.put(Blocks.EXPOSED_COPPER_GRATE, chunksectionlayer1);
        p_470488_.put(Blocks.WEATHERED_COPPER_GRATE, chunksectionlayer1);
        p_470488_.put(Blocks.OXIDIZED_COPPER_GRATE, chunksectionlayer1);
        p_470488_.put(Blocks.WAXED_COPPER_GRATE, chunksectionlayer1);
        p_470488_.put(Blocks.WAXED_EXPOSED_COPPER_GRATE, chunksectionlayer1);
        p_470488_.put(Blocks.WAXED_WEATHERED_COPPER_GRATE, chunksectionlayer1);
        p_470488_.put(Blocks.WAXED_OXIDIZED_COPPER_GRATE, chunksectionlayer1);
        p_470488_.put(Blocks.FIREFLY_BUSH, chunksectionlayer1);
        p_470488_.put(Blocks.CACTUS_FLOWER, chunksectionlayer1);
        p_470488_.put(Blocks.BEACON, chunksectionlayer1);
        ChunkSectionLayer chunksectionlayer2 = ChunkSectionLayer.TRANSLUCENT;
        p_470488_.put(Blocks.ICE, chunksectionlayer2);
        p_470488_.put(Blocks.NETHER_PORTAL, chunksectionlayer2);
        p_470488_.put(Blocks.GLASS, chunksectionlayer2);
        p_470488_.put(Blocks.GLASS_PANE, chunksectionlayer2);
        p_470488_.put(Blocks.WHITE_STAINED_GLASS, chunksectionlayer2);
        p_470488_.put(Blocks.ORANGE_STAINED_GLASS, chunksectionlayer2);
        p_470488_.put(Blocks.MAGENTA_STAINED_GLASS, chunksectionlayer2);
        p_470488_.put(Blocks.LIGHT_BLUE_STAINED_GLASS, chunksectionlayer2);
        p_470488_.put(Blocks.YELLOW_STAINED_GLASS, chunksectionlayer2);
        p_470488_.put(Blocks.LIME_STAINED_GLASS, chunksectionlayer2);
        p_470488_.put(Blocks.PINK_STAINED_GLASS, chunksectionlayer2);
        p_470488_.put(Blocks.GRAY_STAINED_GLASS, chunksectionlayer2);
        p_470488_.put(Blocks.LIGHT_GRAY_STAINED_GLASS, chunksectionlayer2);
        p_470488_.put(Blocks.CYAN_STAINED_GLASS, chunksectionlayer2);
        p_470488_.put(Blocks.PURPLE_STAINED_GLASS, chunksectionlayer2);
        p_470488_.put(Blocks.BLUE_STAINED_GLASS, chunksectionlayer2);
        p_470488_.put(Blocks.BROWN_STAINED_GLASS, chunksectionlayer2);
        p_470488_.put(Blocks.GREEN_STAINED_GLASS, chunksectionlayer2);
        p_470488_.put(Blocks.REDSTONE_WIRE, chunksectionlayer2);
        p_470488_.put(Blocks.RED_STAINED_GLASS, chunksectionlayer2);
        p_470488_.put(Blocks.BLACK_STAINED_GLASS, chunksectionlayer2);
        p_470488_.put(Blocks.WHITE_STAINED_GLASS_PANE, chunksectionlayer2);
        p_470488_.put(Blocks.ORANGE_STAINED_GLASS_PANE, chunksectionlayer2);
        p_470488_.put(Blocks.MAGENTA_STAINED_GLASS_PANE, chunksectionlayer2);
        p_470488_.put(Blocks.LIGHT_BLUE_STAINED_GLASS_PANE, chunksectionlayer2);
        p_470488_.put(Blocks.YELLOW_STAINED_GLASS_PANE, chunksectionlayer2);
        p_470488_.put(Blocks.LIME_STAINED_GLASS_PANE, chunksectionlayer2);
        p_470488_.put(Blocks.PINK_STAINED_GLASS_PANE, chunksectionlayer2);
        p_470488_.put(Blocks.GRAY_STAINED_GLASS_PANE, chunksectionlayer2);
        p_470488_.put(Blocks.LIGHT_GRAY_STAINED_GLASS_PANE, chunksectionlayer2);
        p_470488_.put(Blocks.CYAN_STAINED_GLASS_PANE, chunksectionlayer2);
        p_470488_.put(Blocks.PURPLE_STAINED_GLASS_PANE, chunksectionlayer2);
        p_470488_.put(Blocks.BLUE_STAINED_GLASS_PANE, chunksectionlayer2);
        p_470488_.put(Blocks.BROWN_STAINED_GLASS_PANE, chunksectionlayer2);
        p_470488_.put(Blocks.GREEN_STAINED_GLASS_PANE, chunksectionlayer2);
        p_470488_.put(Blocks.RED_STAINED_GLASS_PANE, chunksectionlayer2);
        p_470488_.put(Blocks.BLACK_STAINED_GLASS_PANE, chunksectionlayer2);
        p_470488_.put(Blocks.SLIME_BLOCK, chunksectionlayer2);
        p_470488_.put(Blocks.HONEY_BLOCK, chunksectionlayer2);
        p_470488_.put(Blocks.FROSTED_ICE, chunksectionlayer2);
        p_470488_.put(Blocks.BUBBLE_COLUMN, chunksectionlayer2);
        p_470488_.put(Blocks.TINTED_GLASS, chunksectionlayer2);
    });
    private static final Map<Fluid, ChunkSectionLayer> LAYER_BY_FLUID = Util.make(Maps.newHashMap(), p_426907_ -> {
        p_426907_.put(Fluids.FLOWING_WATER, ChunkSectionLayer.TRANSLUCENT);
        p_426907_.put(Fluids.WATER, ChunkSectionLayer.TRANSLUCENT);
    });
    private static boolean cutoutLeaves;

    /**
 * @deprecated Neo: Use {@link
 *             net.minecraft.client.renderer.block.model.BlockModelPart#
 *             getRenderType(BlockState)}.
 */
    @Deprecated // Note: this method does NOT support model-based render types
    public static ChunkSectionLayer getChunkRenderType(BlockState state) {
        Block block = state.getBlock();
        if (block instanceof LeavesBlock) {
            return cutoutLeaves ? ChunkSectionLayer.CUTOUT : ChunkSectionLayer.SOLID;
        } else {
            ChunkSectionLayer chunksectionlayer = TYPE_BY_BLOCK.get(block);
            return chunksectionlayer != null ? chunksectionlayer : ChunkSectionLayer.SOLID;
        }
    }

    /**
 * @deprecated Neo: Use {@link net.neoforged.neoforge.client.RenderTypeHelper#
 *             getMovingBlockRenderType(ChunkSectionLayer)} with the result of {@
 *             link net.minecraft.client.renderer.block.model.BlockModelPart#
 *             getRenderType(BlockState)}.
 */
    @Deprecated // Note: this method does NOT support model-based render types
    public static RenderType getMovingBlockRenderType(BlockState state) {
        Block block = state.getBlock();
        if (block instanceof LeavesBlock) {
            return cutoutLeaves ? RenderTypes.cutoutMovingBlock() : RenderTypes.solidMovingBlock();
        } else {
            ChunkSectionLayer chunksectionlayer = TYPE_BY_BLOCK.get(block);
            if (chunksectionlayer != null) {
                return switch (chunksectionlayer) {
                    case SOLID -> RenderTypes.solidMovingBlock();
                    case CUTOUT -> RenderTypes.cutoutMovingBlock();
                    case TRANSLUCENT -> RenderTypes.translucentMovingBlock();
                    case TRIPWIRE -> RenderTypes.tripwireMovingBlock();
                };
            } else {
                return RenderTypes.solidMovingBlock();
            }
        }
    }

    /**
 * @deprecated Neo: Use {@link net.neoforged.neoforge.client.RenderTypeHelper#
 *             getEntityRenderType(ChunkSectionLayer)} with the result of {@link
 *             net.minecraft.client.renderer.block.model.BlockModelPart#
 *             getRenderType(BlockState)}.
 */
    @Deprecated // Note: this method does NOT support model-based render types
    public static RenderType getRenderType(BlockState state) {
        ChunkSectionLayer chunksectionlayer = getChunkRenderType(state);
        return chunksectionlayer == ChunkSectionLayer.TRANSLUCENT ? Sheets.translucentBlockItemSheet() : Sheets.cutoutBlockSheet();
    }

    public static ChunkSectionLayer getRenderLayer(FluidState state) {
        ChunkSectionLayer chunksectionlayer = LAYER_BY_FLUID.get(state.getType());
        return chunksectionlayer != null ? chunksectionlayer : ChunkSectionLayer.SOLID;
    }

    public static void setCutoutLeaves(boolean p_cutoutLeaves) {
        cutoutLeaves = p_cutoutLeaves;
    }

    // Neo: RenderType for block setters injected
    /**
     * Helper to set the RenderType for Blocks
     * @deprecated Set your render type in your block model's JSON (e.g. {@code "render_type": "cutout"}) or override {@link net.minecraft.client.renderer.block.model.BlockModelPart#getRenderType(BlockState)}
     * */
    @Deprecated(since = "1.19")
    public static synchronized void setRenderLayer(Block block, ChunkSectionLayer layer) {
        checkClientLoading();
        TYPE_BY_BLOCK.put(block, layer);
    }

    /**
     * Helper to set the RenderType for Fluids
     */
    public static synchronized void setRenderLayer(Fluid fluid, ChunkSectionLayer layer) {
        checkClientLoading();
        LAYER_BY_FLUID.put(fluid, layer);
    }

    private static void checkClientLoading() {
        com.google.common.base.Preconditions.checkState(net.neoforged.neoforge.client.loading.ClientModLoader.isLoading(),
                  "Render layers can only be set during client loading! This should ideally be done from `FMLClientSetupEvent`."
        );
    }
}
