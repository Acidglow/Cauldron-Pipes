package net.minecraft.commands.execution;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.context.ContextChain;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.ExecutionCommandSource;
import org.jspecify.annotations.Nullable;

public interface CustomCommandExecutor<T> {
    void run(T source, ContextChain<T> contextChain, ChainModifiers chainModifiers, ExecutionControl<T> executionControl);

    public interface CommandAdapter<T> extends Command<T>, CustomCommandExecutor<T> {
        @Override
        default int run(CommandContext<T> context) throws CommandSyntaxException {
            throw new UnsupportedOperationException("This function should not run");
        }
    }

    public abstract static class WithErrorHandling<T extends ExecutionCommandSource<T>> implements CustomCommandExecutor<T> {
        public final void run(T p_306339_, ContextChain<T> p_306289_, ChainModifiers p_309578_, ExecutionControl<T> p_306027_) {
            try {
                this.runGuarded(p_306339_, p_306289_, p_309578_, p_306027_);
            } catch (CommandSyntaxException commandsyntaxexception) {
                this.onError(commandsyntaxexception, p_306339_, p_309578_, p_306027_.tracer());
                p_306339_.callback().onFailure();
            }
        }

        protected void onError(CommandSyntaxException error, T source, ChainModifiers chainModifiers, @Nullable TraceCallbacks traceCallbacks) {
            source.handleError(error, chainModifiers.isForked(), traceCallbacks);
        }

        protected abstract void runGuarded(T source, ContextChain<T> contextChain, ChainModifiers chainModifiers, ExecutionControl<T> executionControl) throws CommandSyntaxException;
    }
}
