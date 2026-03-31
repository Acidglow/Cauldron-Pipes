package net.minecraft.server.commands;

import com.google.common.collect.Lists;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import net.minecraft.advancements.criterion.MinMaxBounds;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.IdentifierArgument;
import net.minecraft.commands.arguments.RangeArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.RandomSequence;
import net.minecraft.world.RandomSequences;
import org.jspecify.annotations.Nullable;

public class RandomCommand {
    private static final SimpleCommandExceptionType ERROR_RANGE_TOO_LARGE = new SimpleCommandExceptionType(
        Component.translatable("commands.random.error.range_too_large")
    );
    private static final SimpleCommandExceptionType ERROR_RANGE_TOO_SMALL = new SimpleCommandExceptionType(
        Component.translatable("commands.random.error.range_too_small")
    );

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(
            Commands.literal("random")
                .then(drawRandomValueTree("value", false))
                .then(drawRandomValueTree("roll", true))
                .then(
                    Commands.literal("reset")
                        .requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                        .then(
                            Commands.literal("*")
                                .executes(p_295916_ -> resetAllSequences(p_295916_.getSource()))
                                .then(
                                    Commands.argument("seed", IntegerArgumentType.integer())
                                        .executes(
                                            p_294436_ -> resetAllSequencesAndSetNewDefaults(
                                                p_294436_.getSource(), IntegerArgumentType.getInteger(p_294436_, "seed"), true, true
                                            )
                                        )
                                        .then(
                                            Commands.argument("includeWorldSeed", BoolArgumentType.bool())
                                                .executes(
                                                    p_295162_ -> resetAllSequencesAndSetNewDefaults(
                                                        p_295162_.getSource(),
                                                        IntegerArgumentType.getInteger(p_295162_, "seed"),
                                                        BoolArgumentType.getBool(p_295162_, "includeWorldSeed"),
                                                        true
                                                    )
                                                )
                                                .then(
                                                    Commands.argument("includeSequenceId", BoolArgumentType.bool())
                                                        .executes(
                                                            p_295871_ -> resetAllSequencesAndSetNewDefaults(
                                                                p_295871_.getSource(),
                                                                IntegerArgumentType.getInteger(p_295871_, "seed"),
                                                                BoolArgumentType.getBool(p_295871_, "includeWorldSeed"),
                                                                BoolArgumentType.getBool(p_295871_, "includeSequenceId")
                                                            )
                                                        )
                                                )
                                        )
                                )
                        )
                        .then(
                            Commands.argument("sequence", IdentifierArgument.id())
                                .suggests(RandomCommand::suggestRandomSequence)
                                .executes(p_466275_ -> resetSequence(p_466275_.getSource(), IdentifierArgument.getId(p_466275_, "sequence")))
                                .then(
                                    Commands.argument("seed", IntegerArgumentType.integer())
                                        .executes(
                                            p_466280_ -> resetSequence(
                                                p_466280_.getSource(),
                                                IdentifierArgument.getId(p_466280_, "sequence"),
                                                IntegerArgumentType.getInteger(p_466280_, "seed"),
                                                true,
                                                true
                                            )
                                        )
                                        .then(
                                            Commands.argument("includeWorldSeed", BoolArgumentType.bool())
                                                .executes(
                                                    p_466287_ -> resetSequence(
                                                        p_466287_.getSource(),
                                                        IdentifierArgument.getId(p_466287_, "sequence"),
                                                        IntegerArgumentType.getInteger(p_466287_, "seed"),
                                                        BoolArgumentType.getBool(p_466287_, "includeWorldSeed"),
                                                        true
                                                    )
                                                )
                                                .then(
                                                    Commands.argument("includeSequenceId", BoolArgumentType.bool())
                                                        .executes(
                                                            p_466286_ -> resetSequence(
                                                                p_466286_.getSource(),
                                                                IdentifierArgument.getId(p_466286_, "sequence"),
                                                                IntegerArgumentType.getInteger(p_466286_, "seed"),
                                                                BoolArgumentType.getBool(p_466286_, "includeWorldSeed"),
                                                                BoolArgumentType.getBool(p_466286_, "includeSequenceId")
                                                            )
                                                        )
                                                )
                                        )
                                )
                        )
                )
        );
    }

    private static LiteralArgumentBuilder<CommandSourceStack> drawRandomValueTree(String subcommand, boolean displayResult) {
        return Commands.literal(subcommand)
            .then(
                Commands.argument("range", RangeArgument.intRange())
                    .executes(p_466285_ -> randomSample(p_466285_.getSource(), RangeArgument.Ints.getRange(p_466285_, "range"), null, displayResult))
                    .then(
                        Commands.argument("sequence", IdentifierArgument.id())
                            .suggests(RandomCommand::suggestRandomSequence)
                            .requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                            .executes(
                                p_466282_ -> randomSample(
                                    p_466282_.getSource(),
                                    RangeArgument.Ints.getRange(p_466282_, "range"),
                                    IdentifierArgument.getId(p_466282_, "sequence"),
                                    displayResult
                                )
                            )
                    )
            );
    }

    private static CompletableFuture<Suggestions> suggestRandomSequence(CommandContext<CommandSourceStack> context, SuggestionsBuilder suggestionsBuilder) {
        List<String> list = Lists.newArrayList();
        context.getSource().getLevel().getRandomSequences().forAllSequences((p_466278_, p_466279_) -> list.add(p_466278_.toString()));
        return SharedSuggestionProvider.suggest(list, suggestionsBuilder);
    }

    private static int randomSample(CommandSourceStack source, MinMaxBounds.Ints range, @Nullable Identifier sequence, boolean displayResult) throws CommandSyntaxException {
        RandomSource randomsource;
        if (sequence != null) {
            randomsource = source.getLevel().getRandomSequence(sequence);
        } else {
            randomsource = source.getLevel().getRandom();
        }

        int i = range.min().orElse(Integer.MIN_VALUE);
        int j = range.max().orElse(Integer.MAX_VALUE);
        long k = (long)j - i;
        if (k == 0L) {
            throw ERROR_RANGE_TOO_SMALL.create();
        } else if (k >= 2147483647L) {
            throw ERROR_RANGE_TOO_LARGE.create();
        } else {
            int l = Mth.randomBetweenInclusive(randomsource, i, j);
            if (displayResult) {
                source.getServer()
                    .getPlayerList()
                    .broadcastSystemMessage(Component.translatable("commands.random.roll", source.getDisplayName(), l, i, j), false);
            } else {
                source.sendSuccess(() -> Component.translatable("commands.random.sample.success", l), false);
            }

            return l;
        }
    }

    private static int resetSequence(CommandSourceStack source, Identifier sequence) throws CommandSyntaxException {
        ServerLevel serverlevel = source.getLevel();
        serverlevel.getRandomSequences().reset(sequence, serverlevel.getSeed());
        source.sendSuccess(() -> Component.translatable("commands.random.reset.success", Component.translationArg(sequence)), false);
        return 1;
    }

    private static int resetSequence(CommandSourceStack source, Identifier sequence, int seed, boolean includeWorldSeed, boolean includeSequenceId) throws CommandSyntaxException {
        ServerLevel serverlevel = source.getLevel();
        serverlevel.getRandomSequences().reset(sequence, serverlevel.getSeed(), seed, includeWorldSeed, includeSequenceId);
        source.sendSuccess(() -> Component.translatable("commands.random.reset.success", Component.translationArg(sequence)), false);
        return 1;
    }

    private static int resetAllSequences(CommandSourceStack source) {
        int i = source.getLevel().getRandomSequences().clear();
        source.sendSuccess(() -> Component.translatable("commands.random.reset.all.success", i), false);
        return i;
    }

    private static int resetAllSequencesAndSetNewDefaults(CommandSourceStack source, int seed, boolean includeWorldSeed, boolean includeSequenceId) {
        RandomSequences randomsequences = source.getLevel().getRandomSequences();
        randomsequences.setSeedDefaults(seed, includeWorldSeed, includeSequenceId);
        int i = randomsequences.clear();
        source.sendSuccess(() -> Component.translatable("commands.random.reset.all.success", i), false);
        return i;
    }
}
