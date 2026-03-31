package net.minecraft.server.commands;

import com.google.common.annotations.VisibleForTesting;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.context.ContextChain;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.Dynamic2CommandExceptionType;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import com.mojang.datafixers.util.Pair;
import java.util.Collection;
import net.minecraft.commands.CommandResultCallback;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.ExecutionCommandSource;
import net.minecraft.commands.FunctionInstantiationException;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.CompoundTagArgument;
import net.minecraft.commands.arguments.NbtPathArgument;
import net.minecraft.commands.arguments.item.FunctionArgument;
import net.minecraft.commands.execution.ChainModifiers;
import net.minecraft.commands.execution.CustomCommandExecutor;
import net.minecraft.commands.execution.ExecutionContext;
import net.minecraft.commands.execution.ExecutionControl;
import net.minecraft.commands.execution.Frame;
import net.minecraft.commands.execution.tasks.CallFunction;
import net.minecraft.commands.execution.tasks.FallthroughTask;
import net.minecraft.commands.functions.CommandFunction;
import net.minecraft.commands.functions.InstantiatedFunction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentUtils;
import net.minecraft.resources.Identifier;
import net.minecraft.server.ServerFunctionManager;
import net.minecraft.server.commands.data.DataAccessor;
import net.minecraft.server.commands.data.DataCommands;
import net.minecraft.server.permissions.LevelBasedPermissionSet;
import org.jspecify.annotations.Nullable;

public class FunctionCommand {
    private static final DynamicCommandExceptionType ERROR_ARGUMENT_NOT_COMPOUND = new DynamicCommandExceptionType(
        p_304240_ -> Component.translatableEscape("commands.function.error.argument_not_compound", p_304240_)
    );
    static final DynamicCommandExceptionType ERROR_NO_FUNCTIONS = new DynamicCommandExceptionType(
        p_305708_ -> Component.translatableEscape("commands.function.scheduled.no_functions", p_305708_)
    );
    @VisibleForTesting
    public static final Dynamic2CommandExceptionType ERROR_FUNCTION_INSTANTATION_FAILURE = new Dynamic2CommandExceptionType(
        (p_305709_, p_305710_) -> Component.translatableEscape("commands.function.instantiationFailure", p_305709_, p_305710_)
    );
    public static final SuggestionProvider<CommandSourceStack> SUGGEST_FUNCTION = (p_137719_, p_137720_) -> {
        ServerFunctionManager serverfunctionmanager = p_137719_.getSource().getServer().getFunctions();
        SharedSuggestionProvider.suggestResource(serverfunctionmanager.getTagNames(), p_137720_, "#");
        return SharedSuggestionProvider.suggestResource(serverfunctionmanager.getFunctionNames(), p_137720_);
    };
    static final FunctionCommand.Callbacks<CommandSourceStack> FULL_CONTEXT_CALLBACKS = new FunctionCommand.Callbacks<CommandSourceStack>() {
        public void signalResult(CommandSourceStack p_305828_, Identifier p_468691_, int p_306112_) {
            p_305828_.sendSuccess(() -> Component.translatable("commands.function.result", Component.translationArg(p_468691_), p_306112_), true);
        }
    };

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        LiteralArgumentBuilder<CommandSourceStack> literalargumentbuilder = Commands.literal("with");

        for (DataCommands.DataProvider datacommands$dataprovider : DataCommands.SOURCE_PROVIDERS) {
            datacommands$dataprovider.wrap(literalargumentbuilder, p_305702_ -> p_305702_.executes(new FunctionCommand.FunctionCustomExecutor() {
                @Override
                protected CompoundTag arguments(CommandContext<CommandSourceStack> p_306295_) throws CommandSyntaxException {
                    return datacommands$dataprovider.access(p_306295_).getData();
                }
            }).then(Commands.argument("path", NbtPathArgument.nbtPath()).executes(new FunctionCommand.FunctionCustomExecutor() {
                @Override
                protected CompoundTag arguments(CommandContext<CommandSourceStack> p_306208_) throws CommandSyntaxException {
                    return FunctionCommand.getArgumentTag(NbtPathArgument.getPath(p_306208_, "path"), datacommands$dataprovider.access(p_306208_));
                }
            })));
        }

        dispatcher.register(
            Commands.literal("function")
                .requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                .then(
                    Commands.argument("name", FunctionArgument.functions()).suggests(SUGGEST_FUNCTION).executes(new FunctionCommand.FunctionCustomExecutor() {
                        @Override
                        protected @Nullable CompoundTag arguments(CommandContext<CommandSourceStack> p_306232_) {
                            return null;
                        }
                    }).then(Commands.argument("arguments", CompoundTagArgument.compoundTag()).executes(new FunctionCommand.FunctionCustomExecutor() {
                        @Override
                        protected CompoundTag arguments(CommandContext<CommandSourceStack> p_305935_) {
                            return CompoundTagArgument.getCompoundTag(p_305935_, "arguments");
                        }
                    })).then(literalargumentbuilder)
                )
        );
    }

    static CompoundTag getArgumentTag(NbtPathArgument.NbtPath nbtPath, DataAccessor dataAccessor) throws CommandSyntaxException {
        Tag tag = DataCommands.getSingleTag(nbtPath, dataAccessor);
        if (tag instanceof CompoundTag compoundtag) {
            return compoundtag;
        } else {
            throw ERROR_ARGUMENT_NOT_COMPOUND.create(tag.getType().getName());
        }
    }

    public static CommandSourceStack modifySenderForExecution(CommandSourceStack source) {
        return source.withSuppressedOutput().withMaximumPermission(LevelBasedPermissionSet.GAMEMASTER);
    }

    public static <T extends ExecutionCommandSource<T>> void queueFunctions(
        Collection<CommandFunction<T>> functions,
        @Nullable CompoundTag arguments,
        T originalSource,
        T source,
        ExecutionControl<T> executionControl,
        FunctionCommand.Callbacks<T> callbacks,
        ChainModifiers chainModifiers
    ) throws CommandSyntaxException {
        if (chainModifiers.isReturn()) {
            queueFunctionsAsReturn(functions, arguments, originalSource, source, executionControl, callbacks);
        } else {
            queueFunctionsNoReturn(functions, arguments, originalSource, source, executionControl, callbacks);
        }
    }

    private static <T extends ExecutionCommandSource<T>> void instantiateAndQueueFunctions(
        @Nullable CompoundTag arguments,
        ExecutionControl<T> executionControl,
        CommandDispatcher<T> dispatcher,
        T source,
        CommandFunction<T> function,
        Identifier functionId,
        CommandResultCallback resultCallback,
        boolean returnParentFrame
    ) throws CommandSyntaxException {
        try {
            InstantiatedFunction<T> instantiatedfunction = function.instantiate(arguments, dispatcher);
            executionControl.queueNext(new CallFunction<>(instantiatedfunction, resultCallback, returnParentFrame).bind(source));
        } catch (FunctionInstantiationException functioninstantiationexception) {
            throw ERROR_FUNCTION_INSTANTATION_FAILURE.create(functionId, functioninstantiationexception.messageComponent());
        }
    }

    private static <T extends ExecutionCommandSource<T>> CommandResultCallback decorateOutputIfNeeded(
        T source, FunctionCommand.Callbacks<T> callbacks, Identifier function, CommandResultCallback resultCallback
    ) {
        return source.isSilent() ? resultCallback : (p_466228_, p_466229_) -> {
            callbacks.signalResult(source, function, p_466229_);
            resultCallback.onResult(p_466228_, p_466229_);
        };
    }

    private static <T extends ExecutionCommandSource<T>> void queueFunctionsAsReturn(
        Collection<CommandFunction<T>> functions,
        @Nullable CompoundTag arguments,
        T originalSource,
        T source,
        ExecutionControl<T> executionControl,
        FunctionCommand.Callbacks<T> callbacks
    ) throws CommandSyntaxException {
        CommandDispatcher<T> commanddispatcher = originalSource.dispatcher();
        T t = source.clearCallbacks();
        CommandResultCallback commandresultcallback = CommandResultCallback.chain(originalSource.callback(), executionControl.currentFrame().returnValueConsumer());

        for (CommandFunction<T> commandfunction : functions) {
            Identifier identifier = commandfunction.id();
            CommandResultCallback commandresultcallback1 = decorateOutputIfNeeded(originalSource, callbacks, identifier, commandresultcallback);
            instantiateAndQueueFunctions(arguments, executionControl, commanddispatcher, t, commandfunction, identifier, commandresultcallback1, true);
        }

        executionControl.queueNext(FallthroughTask.instance());
    }

    private static <T extends ExecutionCommandSource<T>> void queueFunctionsNoReturn(
        Collection<CommandFunction<T>> functions,
        @Nullable CompoundTag arguments,
        T originalSource,
        T source,
        ExecutionControl<T> executionControl,
        FunctionCommand.Callbacks<T> callbacks
    ) throws CommandSyntaxException {
        CommandDispatcher<T> commanddispatcher = originalSource.dispatcher();
        T t = source.clearCallbacks();
        CommandResultCallback commandresultcallback = originalSource.callback();
        if (!functions.isEmpty()) {
            if (functions.size() == 1) {
                CommandFunction<T> commandfunction = functions.iterator().next();
                Identifier identifier = commandfunction.id();
                CommandResultCallback commandresultcallback1 = decorateOutputIfNeeded(originalSource, callbacks, identifier, commandresultcallback);
                instantiateAndQueueFunctions(arguments, executionControl, commanddispatcher, t, commandfunction, identifier, commandresultcallback1, false);
            } else if (commandresultcallback == CommandResultCallback.EMPTY) {
                for (CommandFunction<T> commandfunction1 : functions) {
                    Identifier identifier2 = commandfunction1.id();
                    CommandResultCallback commandresultcallback2 = decorateOutputIfNeeded(originalSource, callbacks, identifier2, commandresultcallback);
                    instantiateAndQueueFunctions(arguments, executionControl, commanddispatcher, t, commandfunction1, identifier2, commandresultcallback2, false);
                }
            } else {
                class Accumulator {
                    boolean anyResult;
                    int sum;

                    public void add(int p_309590_) {
                        this.anyResult = true;
                        this.sum += p_309590_;
                    }
                }

                Accumulator functioncommand$1accumulator = new Accumulator();
                CommandResultCallback commandresultcallback4 = (p_309467_, p_309468_) -> functioncommand$1accumulator.add(p_309468_);

                for (CommandFunction<T> commandfunction2 : functions) {
                    Identifier identifier1 = commandfunction2.id();
                    CommandResultCallback commandresultcallback3 = decorateOutputIfNeeded(originalSource, callbacks, identifier1, commandresultcallback4);
                    instantiateAndQueueFunctions(arguments, executionControl, commanddispatcher, t, commandfunction2, identifier1, commandresultcallback3, false);
                }

                executionControl.queueNext((p_309471_, p_309472_) -> {
                    if (functioncommand$1accumulator.anyResult) {
                        commandresultcallback.onSuccess(functioncommand$1accumulator.sum);
                    }
                });
            }
        }
    }

    public interface Callbacks<T> {
        void signalResult(T source, Identifier function, int commands);
    }

    abstract static class FunctionCustomExecutor
        extends CustomCommandExecutor.WithErrorHandling<CommandSourceStack>
        implements CustomCommandExecutor.CommandAdapter<CommandSourceStack> {
        protected abstract @Nullable CompoundTag arguments(CommandContext<CommandSourceStack> context) throws CommandSyntaxException;

        public void runGuarded(
            CommandSourceStack p_305800_, ContextChain<CommandSourceStack> p_305848_, ChainModifiers p_309662_, ExecutionControl<CommandSourceStack> p_306013_
        ) throws CommandSyntaxException {
            CommandContext<CommandSourceStack> commandcontext = p_305848_.getTopContext().copyFor(p_305800_);
            Pair<Identifier, Collection<CommandFunction<CommandSourceStack>>> pair = FunctionArgument.getFunctionCollection(commandcontext, "name");
            Collection<CommandFunction<CommandSourceStack>> collection = pair.getSecond();
            if (collection.isEmpty()) {
                throw FunctionCommand.ERROR_NO_FUNCTIONS.create(Component.translationArg(pair.getFirst()));
            } else {
                CompoundTag compoundtag = this.arguments(commandcontext);
                CommandSourceStack commandsourcestack = FunctionCommand.modifySenderForExecution(p_305800_);
                if (collection.size() == 1) {
                    p_305800_.sendSuccess(
                        () -> Component.translatable("commands.function.scheduled.single", Component.translationArg(collection.iterator().next().id())), true
                    );
                } else {
                    p_305800_.sendSuccess(
                        () -> Component.translatable(
                            "commands.function.scheduled.multiple",
                            ComponentUtils.formatList(collection.stream().map(CommandFunction::id).toList(), Component::translationArg)
                        ),
                        true
                    );
                }

                FunctionCommand.queueFunctions(
                    collection, compoundtag, p_305800_, commandsourcestack, p_306013_, FunctionCommand.FULL_CONTEXT_CALLBACKS, p_309662_
                );
            }
        }
    }
}
