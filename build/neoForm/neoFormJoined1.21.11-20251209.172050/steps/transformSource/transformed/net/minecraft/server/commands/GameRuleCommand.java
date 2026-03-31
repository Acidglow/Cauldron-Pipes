package net.minecraft.server.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.gamerules.GameRule;
import net.minecraft.world.level.gamerules.GameRuleTypeVisitor;
import net.minecraft.world.level.gamerules.GameRules;

public class GameRuleCommand {
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher, CommandBuildContext commandBuildContext) {
        final LiteralArgumentBuilder<CommandSourceStack> literalargumentbuilder = Commands.literal("gamerule")
            .requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS));
        new GameRules(commandBuildContext.enabledFeatures())
            .visitGameRuleTypes(
                new GameRuleTypeVisitor() {
                    @Override
                    public <T> void visit(GameRule<T> p_460992_) {
                        LiteralArgumentBuilder<CommandSourceStack> literalargumentbuilder1 = Commands.literal(p_460992_.id());
                        LiteralArgumentBuilder<CommandSourceStack> literalargumentbuilder2 = Commands.literal(p_460992_.getIdentifier().toString());
                        literalargumentbuilder.then(GameRuleCommand.buildRuleArguments(p_460992_, literalargumentbuilder1))
                            .then(GameRuleCommand.buildRuleArguments(p_460992_, literalargumentbuilder2));
                    }
                }
            );
        dispatcher.register(literalargumentbuilder);
    }

    static <T> LiteralArgumentBuilder<CommandSourceStack> buildRuleArguments(GameRule<T> gameRule, LiteralArgumentBuilder<CommandSourceStack> builder) {
        return builder.executes(p_477765_ -> queryRule(p_477765_.getSource(), gameRule))
            .then(Commands.argument("value", gameRule.argument()).executes(p_477763_ -> setRule(p_477763_, gameRule)));
    }

    private static <T> int setRule(CommandContext<CommandSourceStack> context, GameRule<T> gameRule) {
        CommandSourceStack commandsourcestack = context.getSource();
        T t = context.getArgument("value", gameRule.valueClass());
        commandsourcestack.getLevel().getGameRules().set(gameRule, t, context.getSource().getServer());
        commandsourcestack.sendSuccess(() -> Component.translatable("commands.gamerule.set", gameRule.id(), gameRule.serialize(t)), true);
        return gameRule.getCommandResult(t);
    }

    private static <T> int queryRule(CommandSourceStack source, GameRule<T> gameRule) {
        T t = source.getLevel().getGameRules().get(gameRule);
        source.sendSuccess(() -> Component.translatable("commands.gamerule.query", gameRule.id(), gameRule.serialize(t)), false);
        return gameRule.getCommandResult(t);
    }
}
