package net.minecraft.world.level.block;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.mojang.serialization.codecs.RecordCodecBuilder.Instance;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;

public class WeatheringLanternBlock extends LanternBlock implements WeatheringCopper {
    public static final MapCodec<WeatheringLanternBlock> CODEC = RecordCodecBuilder.mapCodec(
        p_436659_ -> p_436659_.group(
                WeatheringCopper.WeatherState.CODEC.fieldOf("weathering_state").forGetter(WeatheringLanternBlock::getAge), propertiesCodec()
            )
            .apply(p_436659_, WeatheringLanternBlock::new)
    );
    private final WeatheringCopper.WeatherState weatherState;

    @Override
    public MapCodec<WeatheringLanternBlock> codec() {
        return CODEC;
    }

    public WeatheringLanternBlock(WeatheringCopper.WeatherState weatherState, BlockBehaviour.Properties properties) {
        super(properties);
        this.weatherState = weatherState;
    }

    @Override
    protected void randomTick(BlockState p_436744_, ServerLevel p_436723_, BlockPos p_436680_, RandomSource p_436600_) {
        this.changeOverTime(p_436744_, p_436723_, p_436680_, p_436600_);
    }

    @Override
    protected boolean isRandomlyTicking(BlockState p_436737_) {
        return WeatheringCopper.getNext(p_436737_.getBlock()).isPresent();
    }

    public WeatheringCopper.WeatherState getAge() {
        return this.weatherState;
    }
}
