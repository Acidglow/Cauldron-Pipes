package net.minecraft.gametest.framework;

import com.mojang.logging.LogUtils;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.mojang.serialization.codecs.RecordCodecBuilder.Instance;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.functions.CommandFunction;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.RegistryFileCodec;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.ServerFunctionManager;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.permissions.LevelBasedPermissionSet;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.level.gamerules.GameRule;
import net.minecraft.world.level.gamerules.GameRuleMap;
import net.minecraft.world.level.gamerules.GameRules;
import org.slf4j.Logger;

public interface TestEnvironmentDefinition {
    Codec<TestEnvironmentDefinition> DIRECT_CODEC = BuiltInRegistries.TEST_ENVIRONMENT_DEFINITION_TYPE
        .byNameCodec()
        .dispatch(TestEnvironmentDefinition::codec, p_397290_ -> p_397290_);
    Codec<Holder<TestEnvironmentDefinition>> CODEC = RegistryFileCodec.create(Registries.TEST_ENVIRONMENT, DIRECT_CODEC);

    static MapCodec<? extends TestEnvironmentDefinition> bootstrap(Registry<MapCodec<? extends TestEnvironmentDefinition>> registry) {
        Registry.register(registry, "all_of", TestEnvironmentDefinition.AllOf.CODEC);
        Registry.register(registry, "game_rules", TestEnvironmentDefinition.SetGameRules.CODEC);
        Registry.register(registry, "time_of_day", TestEnvironmentDefinition.TimeOfDay.CODEC);
        Registry.register(registry, "weather", TestEnvironmentDefinition.Weather.CODEC);
        return Registry.register(registry, "function", TestEnvironmentDefinition.Functions.CODEC);
    }

    void setup(ServerLevel level);

    default void teardown(ServerLevel level) {
    }

    MapCodec<? extends TestEnvironmentDefinition> codec();

    public record AllOf(List<Holder<TestEnvironmentDefinition>> definitions) implements TestEnvironmentDefinition {
        public static final MapCodec<TestEnvironmentDefinition.AllOf> CODEC = RecordCodecBuilder.mapCodec(
            p_397209_ -> p_397209_.group(
                    TestEnvironmentDefinition.CODEC.listOf().fieldOf("definitions").forGetter(TestEnvironmentDefinition.AllOf::definitions)
                )
                .apply(p_397209_, TestEnvironmentDefinition.AllOf::new)
        );

        public AllOf(TestEnvironmentDefinition... p_398055_) {
            this(Arrays.stream(p_398055_).map(Holder::direct).toList());
        }

        @Override
        public void setup(ServerLevel p_397476_) {
            this.definitions.forEach(p_397273_ -> p_397273_.value().setup(p_397476_));
        }

        @Override
        public void teardown(ServerLevel p_397650_) {
            this.definitions.forEach(p_397475_ -> p_397475_.value().teardown(p_397650_));
        }

        @Override
        public MapCodec<TestEnvironmentDefinition.AllOf> codec() {
            return CODEC;
        }
    }

    public record Functions(Optional<Identifier> setupFunction, Optional<Identifier> teardownFunction) implements TestEnvironmentDefinition {
        private static final Logger LOGGER = LogUtils.getLogger();
        public static final MapCodec<TestEnvironmentDefinition.Functions> CODEC = RecordCodecBuilder.mapCodec(
            p_466102_ -> p_466102_.group(
                    Identifier.CODEC.optionalFieldOf("setup").forGetter(TestEnvironmentDefinition.Functions::setupFunction),
                    Identifier.CODEC.optionalFieldOf("teardown").forGetter(TestEnvironmentDefinition.Functions::teardownFunction)
                )
                .apply(p_466102_, TestEnvironmentDefinition.Functions::new)
        );

        @Override
        public void setup(ServerLevel p_397955_) {
            this.setupFunction.ifPresent(p_466104_ -> run(p_397955_, p_466104_));
        }

        @Override
        public void teardown(ServerLevel p_397731_) {
            this.teardownFunction.ifPresent(p_466106_ -> run(p_397731_, p_466106_));
        }

        private static void run(ServerLevel level, Identifier function) {
            MinecraftServer minecraftserver = level.getServer();
            ServerFunctionManager serverfunctionmanager = minecraftserver.getFunctions();
            Optional<CommandFunction<CommandSourceStack>> optional = serverfunctionmanager.get(function);
            if (optional.isPresent()) {
                CommandSourceStack commandsourcestack = minecraftserver.createCommandSourceStack()
                    .withPermission(LevelBasedPermissionSet.GAMEMASTER)
                    .withSuppressedOutput()
                    .withLevel(level);
                serverfunctionmanager.execute(optional.get(), commandsourcestack);
            } else {
                LOGGER.error("Test Batch failed for non-existent function {}", function);
            }
        }

        @Override
        public MapCodec<TestEnvironmentDefinition.Functions> codec() {
            return CODEC;
        }
    }

    public record SetGameRules(GameRuleMap gameRulesMap) implements TestEnvironmentDefinition {
        public static final MapCodec<TestEnvironmentDefinition.SetGameRules> CODEC = RecordCodecBuilder.mapCodec(
            p_460315_ -> p_460315_.group(GameRuleMap.CODEC.fieldOf("rules").forGetter(TestEnvironmentDefinition.SetGameRules::gameRulesMap))
                .apply(p_460315_, TestEnvironmentDefinition.SetGameRules::new)
        );

        @Override
        public void setup(ServerLevel p_397170_) {
            GameRules gamerules = p_397170_.getGameRules();
            MinecraftServer minecraftserver = p_397170_.getServer();
            gamerules.setAll(this.gameRulesMap, minecraftserver);
        }

        @Override
        public void teardown(ServerLevel p_397717_) {
            this.gameRulesMap.keySet().forEach(p_460314_ -> this.resetRule(p_397717_, (GameRule<?>)p_460314_));
        }

        private <T> void resetRule(ServerLevel level, GameRule<T> gameRule) {
            level.getGameRules().set(gameRule, gameRule.defaultValue(), level.getServer());
        }

        @Override
        public MapCodec<TestEnvironmentDefinition.SetGameRules> codec() {
            return CODEC;
        }
    }

    public record TimeOfDay(int time) implements TestEnvironmentDefinition {
        public static final MapCodec<TestEnvironmentDefinition.TimeOfDay> CODEC = RecordCodecBuilder.mapCodec(
            p_397416_ -> p_397416_.group(ExtraCodecs.NON_NEGATIVE_INT.fieldOf("time").forGetter(TestEnvironmentDefinition.TimeOfDay::time))
                .apply(p_397416_, TestEnvironmentDefinition.TimeOfDay::new)
        );

        @Override
        public void setup(ServerLevel p_397305_) {
            p_397305_.setDayTime(this.time);
        }

        @Override
        public MapCodec<TestEnvironmentDefinition.TimeOfDay> codec() {
            return CODEC;
        }
    }

    public record Weather(TestEnvironmentDefinition.Weather.Type weather) implements TestEnvironmentDefinition {
        public static final MapCodec<TestEnvironmentDefinition.Weather> CODEC = RecordCodecBuilder.mapCodec(
            p_397274_ -> p_397274_.group(TestEnvironmentDefinition.Weather.Type.CODEC.fieldOf("weather").forGetter(TestEnvironmentDefinition.Weather::weather))
                .apply(p_397274_, TestEnvironmentDefinition.Weather::new)
        );

        @Override
        public void setup(ServerLevel p_397776_) {
            this.weather.apply(p_397776_);
        }

        @Override
        public void teardown(ServerLevel p_397150_) {
            p_397150_.resetWeatherCycle();
        }

        @Override
        public MapCodec<TestEnvironmentDefinition.Weather> codec() {
            return CODEC;
        }

        public static enum Type implements StringRepresentable {
            CLEAR("clear", 100000, 0, false, false),
            RAIN("rain", 0, 100000, true, false),
            THUNDER("thunder", 0, 100000, true, true);

            public static final Codec<TestEnvironmentDefinition.Weather.Type> CODEC = StringRepresentable.fromEnum(
                TestEnvironmentDefinition.Weather.Type::values
            );
            private final String id;
            private final int clearTime;
            private final int rainTime;
            private final boolean raining;
            private final boolean thundering;

            private Type(String id, int clearTime, int rainTime, boolean raining, boolean thundering) {
                this.id = id;
                this.clearTime = clearTime;
                this.rainTime = rainTime;
                this.raining = raining;
                this.thundering = thundering;
            }

            void apply(ServerLevel level) {
                level.setWeatherParameters(this.clearTime, this.rainTime, this.raining, this.thundering);
            }

            @Override
            public String getSerializedName() {
                return this.id;
            }
        }
    }
}
