package net.minecraft.server.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.IdentifierArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.Stopwatch;
import net.minecraft.world.Stopwatches;

public class StopwatchCommand {
    private static final DynamicCommandExceptionType ERROR_ALREADY_EXISTS = new DynamicCommandExceptionType(
        p_454664_ -> Component.translatableEscape("commands.stopwatch.already_exists", p_454664_)
    );
    public static final DynamicCommandExceptionType ERROR_DOES_NOT_EXIST = new DynamicCommandExceptionType(
        p_456143_ -> Component.translatableEscape("commands.stopwatch.does_not_exist", p_456143_)
    );
    public static final SuggestionProvider<CommandSourceStack> SUGGEST_STOPWATCHES = (p_454878_, p_455100_) -> SharedSuggestionProvider.suggestResource(
        p_454878_.getSource().getServer().getStopwatches().ids(), p_455100_
    );

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(
            Commands.literal("stopwatch")
                .requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                .then(
                    Commands.literal("create")
                        .then(
                            Commands.argument("id", IdentifierArgument.id())
                                .executes(p_466313_ -> createStopwatch(p_466313_.getSource(), IdentifierArgument.getId(p_466313_, "id")))
                        )
                )
                .then(
                    Commands.literal("query")
                        .then(
                            Commands.argument("id", IdentifierArgument.id())
                                .suggests(SUGGEST_STOPWATCHES)
                                .then(
                                    Commands.argument("scale", DoubleArgumentType.doubleArg())
                                        .executes(
                                            p_466308_ -> queryStopwatch(
                                                p_466308_.getSource(),
                                                IdentifierArgument.getId(p_466308_, "id"),
                                                DoubleArgumentType.getDouble(p_466308_, "scale")
                                            )
                                        )
                                )
                                .executes(p_466311_ -> queryStopwatch(p_466311_.getSource(), IdentifierArgument.getId(p_466311_, "id"), 1.0))
                        )
                )
                .then(
                    Commands.literal("restart")
                        .then(
                            Commands.argument("id", IdentifierArgument.id())
                                .suggests(SUGGEST_STOPWATCHES)
                                .executes(p_466310_ -> restartStopwatch(p_466310_.getSource(), IdentifierArgument.getId(p_466310_, "id")))
                        )
                )
                .then(
                    Commands.literal("remove")
                        .then(
                            Commands.argument("id", IdentifierArgument.id())
                                .suggests(SUGGEST_STOPWATCHES)
                                .executes(p_466307_ -> removeStopwatch(p_466307_.getSource(), IdentifierArgument.getId(p_466307_, "id")))
                        )
                )
        );
    }

    private static int createStopwatch(CommandSourceStack source, Identifier id) throws CommandSyntaxException {
        MinecraftServer minecraftserver = source.getServer();
        Stopwatches stopwatches = minecraftserver.getStopwatches();
        Stopwatch stopwatch = new Stopwatch(Stopwatches.currentTime());
        if (!stopwatches.add(id, stopwatch)) {
            throw ERROR_ALREADY_EXISTS.create(id);
        } else {
            source.sendSuccess(() -> Component.translatable("commands.stopwatch.create.success", Component.translationArg(id)), true);
            return 1;
        }
    }

    private static int queryStopwatch(CommandSourceStack source, Identifier id, double scale) throws CommandSyntaxException {
        MinecraftServer minecraftserver = source.getServer();
        Stopwatches stopwatches = minecraftserver.getStopwatches();
        Stopwatch stopwatch = stopwatches.get(id);
        if (stopwatch == null) {
            throw ERROR_DOES_NOT_EXIST.create(id);
        } else {
            long i = Stopwatches.currentTime();
            double d0 = stopwatch.elapsedSeconds(i);
            source.sendSuccess(() -> Component.translatable("commands.stopwatch.query", Component.translationArg(id), d0), true);
            return (int)(d0 * scale);
        }
    }

    private static int restartStopwatch(CommandSourceStack source, Identifier id) throws CommandSyntaxException {
        MinecraftServer minecraftserver = source.getServer();
        Stopwatches stopwatches = minecraftserver.getStopwatches();
        if (!stopwatches.update(id, p_456125_ -> new Stopwatch(Stopwatches.currentTime()))) {
            throw ERROR_DOES_NOT_EXIST.create(id);
        } else {
            source.sendSuccess(() -> Component.translatable("commands.stopwatch.restart.success", Component.translationArg(id)), true);
            return 1;
        }
    }

    private static int removeStopwatch(CommandSourceStack source, Identifier id) throws CommandSyntaxException {
        MinecraftServer minecraftserver = source.getServer();
        Stopwatches stopwatches = minecraftserver.getStopwatches();
        if (!stopwatches.remove(id)) {
            throw ERROR_DOES_NOT_EXIST.create(id);
        } else {
            source.sendSuccess(() -> Component.translatable("commands.stopwatch.remove.success", Component.translationArg(id)), true);
            return 1;
        }
    }
}
