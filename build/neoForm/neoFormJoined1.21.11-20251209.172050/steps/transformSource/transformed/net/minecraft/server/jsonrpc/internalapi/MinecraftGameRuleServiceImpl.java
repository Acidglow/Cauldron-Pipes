package net.minecraft.server.jsonrpc.internalapi;

import java.util.stream.Stream;
import net.minecraft.server.dedicated.DedicatedServer;
import net.minecraft.server.jsonrpc.JsonRpcLogger;
import net.minecraft.server.jsonrpc.methods.ClientInfo;
import net.minecraft.server.jsonrpc.methods.GameRulesService;
import net.minecraft.world.level.gamerules.GameRule;
import net.minecraft.world.level.gamerules.GameRules;

public class MinecraftGameRuleServiceImpl implements MinecraftGameRuleService {
    private final DedicatedServer server;
    private final GameRules gameRules;
    private final JsonRpcLogger jsonrpcLogger;

    public MinecraftGameRuleServiceImpl(DedicatedServer server, JsonRpcLogger jsonrpcLogger) {
        this.server = server;
        this.gameRules = server.getWorldData().getGameRules();
        this.jsonrpcLogger = jsonrpcLogger;
    }

    @Override
    public <T> GameRulesService.GameRuleUpdate<T> updateGameRule(GameRulesService.GameRuleUpdate<T> p_461142_, ClientInfo p_449823_) {
        GameRule<T> gamerule = p_461142_.gameRule();
        T t = this.gameRules.get(gamerule);
        T t1 = p_461142_.value();
        this.gameRules.set(gamerule, t1, this.server);
        this.jsonrpcLogger.log(p_449823_, "Game rule '{}' updated from '{}' to '{}'", gamerule.id(), gamerule.serialize(t), gamerule.serialize(t1));
        return p_461142_;
    }

    @Override
    public <T> GameRulesService.GameRuleUpdate<T> getTypedRule(GameRule<T> p_460702_, T p_460925_) {
        return new GameRulesService.GameRuleUpdate<>(p_460702_, p_460925_);
    }

    @Override
    public Stream<GameRule<?>> getAvailableGameRules() {
        return this.gameRules.availableRules();
    }

    @Override
    public <T> T getRuleValue(GameRule<T> p_461191_) {
        return this.gameRules.get(p_461191_);
    }
}
