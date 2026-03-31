package net.minecraft.world.level.block;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.mojang.serialization.codecs.RecordCodecBuilder.Instance;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.HoneycombItem;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.ChestType;

public class CopperChestBlock extends ChestBlock {
    public static final MapCodec<CopperChestBlock> CODEC = RecordCodecBuilder.mapCodec(
        p_434693_ -> p_434693_.group(
                WeatheringCopper.WeatherState.CODEC.fieldOf("weathering_state").forGetter(CopperChestBlock::getState),
                BuiltInRegistries.SOUND_EVENT.byNameCodec().fieldOf("open_sound").forGetter(ChestBlock::getOpenChestSound),
                BuiltInRegistries.SOUND_EVENT.byNameCodec().fieldOf("close_sound").forGetter(ChestBlock::getCloseChestSound),
                propertiesCodec()
            )
            .apply(p_434693_, CopperChestBlock::new)
    );
    private static final Map<Block, Supplier<Block>> COPPER_TO_COPPER_CHEST_MAPPING = Map.of(
        Blocks.COPPER_BLOCK,
        () -> Blocks.COPPER_CHEST,
        Blocks.EXPOSED_COPPER,
        () -> Blocks.EXPOSED_COPPER_CHEST,
        Blocks.WEATHERED_COPPER,
        () -> Blocks.WEATHERED_COPPER_CHEST,
        Blocks.OXIDIZED_COPPER,
        () -> Blocks.OXIDIZED_COPPER_CHEST,
        Blocks.WAXED_COPPER_BLOCK,
        () -> Blocks.COPPER_CHEST,
        Blocks.WAXED_EXPOSED_COPPER,
        () -> Blocks.EXPOSED_COPPER_CHEST,
        Blocks.WAXED_WEATHERED_COPPER,
        () -> Blocks.WEATHERED_COPPER_CHEST,
        Blocks.WAXED_OXIDIZED_COPPER,
        () -> Blocks.OXIDIZED_COPPER_CHEST
    );
    private final WeatheringCopper.WeatherState weatherState;

    @Override
    public MapCodec<? extends CopperChestBlock> codec() {
        return CODEC;
    }

    public CopperChestBlock(WeatheringCopper.WeatherState weatherState, SoundEvent openSound, SoundEvent closeSound, BlockBehaviour.Properties properties) {
        super(() -> BlockEntityType.CHEST, openSound, closeSound, properties);
        this.weatherState = weatherState;
    }

    @Override
    public boolean chestCanConnectTo(BlockState p_436019_) {
        return p_436019_.is(BlockTags.COPPER_CHESTS) && p_436019_.hasProperty(ChestBlock.TYPE);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext p_435006_) {
        BlockState blockstate = super.getStateForPlacement(p_435006_);
        return getLeastOxidizedChestOfConnectedBlocks(blockstate, p_435006_.getLevel(), p_435006_.getClickedPos());
    }

    private static BlockState getLeastOxidizedChestOfConnectedBlocks(BlockState state, Level level, BlockPos pos) {
        BlockState blockstate = level.getBlockState(pos.relative(getConnectedDirection(state)));
        if (!state.getValue(ChestBlock.TYPE).equals(ChestType.SINGLE)
            && state.getBlock() instanceof CopperChestBlock copperchestblock
            && blockstate.getBlock() instanceof CopperChestBlock copperchestblock1) {
            BlockState blockstate2 = state;
            BlockState blockstate1 = blockstate;
            if (copperchestblock.isWaxed() != copperchestblock1.isWaxed()) {
                blockstate2 = unwaxBlock(copperchestblock, state).orElse(state);
                blockstate1 = unwaxBlock(copperchestblock1, blockstate).orElse(blockstate);
            }

            Block block = copperchestblock.weatherState.ordinal() <= copperchestblock1.weatherState.ordinal() ? blockstate2.getBlock() : blockstate1.getBlock();
            return block.withPropertiesOf(blockstate2);
        } else {
            return state;
        }
    }

    @Override
    protected BlockState updateShape(
        BlockState p_434501_,
        LevelReader p_435363_,
        ScheduledTickAccess p_435571_,
        BlockPos p_434601_,
        Direction p_435158_,
        BlockPos p_434233_,
        BlockState p_435964_,
        RandomSource p_432879_
    ) {
        BlockState blockstate = super.updateShape(p_434501_, p_435363_, p_435571_, p_434601_, p_435158_, p_434233_, p_435964_, p_432879_);
        if (this.chestCanConnectTo(p_435964_)) {
            ChestType chesttype = blockstate.getValue(ChestBlock.TYPE);
            if (!chesttype.equals(ChestType.SINGLE) && getConnectedDirection(blockstate) == p_435158_) {
                return p_435964_.getBlock().withPropertiesOf(blockstate);
            }
        }

        return blockstate;
    }

    private static Optional<BlockState> unwaxBlock(CopperChestBlock block, BlockState state) {
        return !block.isWaxed()
            ? Optional.of(state)
            : Optional.ofNullable(HoneycombItem.WAX_OFF_BY_BLOCK.get().get(state.getBlock())).map(p_435226_ -> p_435226_.withPropertiesOf(state));
    }

    public WeatheringCopper.WeatherState getState() {
        return this.weatherState;
    }

    public static BlockState getFromCopperBlock(Block block, Direction direction, Level level, BlockPos pos) {
        CopperChestBlock copperchestblock = (CopperChestBlock)COPPER_TO_COPPER_CHEST_MAPPING.getOrDefault(block, Blocks.COPPER_CHEST::asBlock).get();
        ChestType chesttype = copperchestblock.getChestType(level, pos, direction);
        BlockState blockstate = copperchestblock.defaultBlockState().setValue(FACING, direction).setValue(TYPE, chesttype);
        return getLeastOxidizedChestOfConnectedBlocks(blockstate, level, pos);
    }

    public boolean isWaxed() {
        return true;
    }

    @Override
    public boolean shouldChangedStateKeepBlockEntity(BlockState p_433440_) {
        return p_433440_.is(BlockTags.COPPER_CHESTS);
    }
}
