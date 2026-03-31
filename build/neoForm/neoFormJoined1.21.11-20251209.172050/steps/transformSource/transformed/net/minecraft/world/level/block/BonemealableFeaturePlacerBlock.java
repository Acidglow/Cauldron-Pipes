package net.minecraft.world.level.block;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.mojang.serialization.codecs.RecordCodecBuilder.Instance;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;

public class BonemealableFeaturePlacerBlock extends Block implements BonemealableBlock {
    public static final MapCodec<BonemealableFeaturePlacerBlock> CODEC = RecordCodecBuilder.mapCodec(
        p_432632_ -> p_432632_.group(
                ResourceKey.codec(Registries.CONFIGURED_FEATURE).fieldOf("feature").forGetter(p_380141_ -> p_380141_.feature), propertiesCodec()
            )
            .apply(p_432632_, BonemealableFeaturePlacerBlock::new)
    );
    private final ResourceKey<ConfiguredFeature<?, ?>> feature;

    @Override
    public MapCodec<BonemealableFeaturePlacerBlock> codec() {
        return CODEC;
    }

    public BonemealableFeaturePlacerBlock(ResourceKey<ConfiguredFeature<?, ?>> feature, BlockBehaviour.Properties properties) {
        super(properties);
        this.feature = feature;
    }

    @Override
    public boolean isValidBonemealTarget(LevelReader p_379414_, BlockPos p_379932_, BlockState p_379449_) {
        return p_379414_.getBlockState(p_379932_.above()).isAir();
    }

    @Override
    public boolean isBonemealSuccess(Level p_379855_, RandomSource p_380339_, BlockPos p_379438_, BlockState p_379693_) {
        return true;
    }

    @Override
    public void performBonemeal(ServerLevel p_380244_, RandomSource p_379395_, BlockPos p_380245_, BlockState p_379519_) {
        p_380244_.registryAccess()
            .lookup(Registries.CONFIGURED_FEATURE)
            .flatMap(p_379951_ -> p_379951_.get(this.feature))
            .ifPresent(p_380225_ -> p_380225_.value().place(p_380244_, p_380244_.getChunkSource().getGenerator(), p_379395_, p_380245_.above()));
    }

    @Override
    public BonemealableBlock.Type getType() {
        return BonemealableBlock.Type.NEIGHBOR_SPREADER;
    }
}
