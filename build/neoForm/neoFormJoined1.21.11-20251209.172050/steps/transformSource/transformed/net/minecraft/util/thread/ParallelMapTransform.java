package net.minecraft.util.thread;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.function.BiFunction;
import net.minecraft.util.Mth;
import net.minecraft.util.Util;
import org.jspecify.annotations.Nullable;

public class ParallelMapTransform {
    private static final int DEFAULT_TASKS_PER_THREAD = 16;

    public static <K, U, V> CompletableFuture<Map<K, V>> schedule(
        Map<K, U> inputs, BiFunction<K, U, @Nullable V> operation, int maxTasksPerBatch, Executor executor
    ) {
        int i = inputs.size();
        if (i == 0) {
            return CompletableFuture.completedFuture(Map.of());
        } else if (i == 1) {
            Entry<K, U> entry = inputs.entrySet().iterator().next();
            K k = entry.getKey();
            U u = entry.getValue();
            return CompletableFuture.supplyAsync(() -> {
                V v = operation.apply(k, u);
                return v != null ? Map.of(k, v) : Map.of();
            }, executor);
        } else {
            ParallelMapTransform.SplitterBase<K, U, V> splitterbase = (ParallelMapTransform.SplitterBase<K, U, V>)(i <= maxTasksPerBatch
                ? new ParallelMapTransform.SingleTaskSplitter<>(operation, i)
                : new ParallelMapTransform.BatchedTaskSplitter<>(operation, i, maxTasksPerBatch));
            return splitterbase.scheduleTasks(inputs, executor);
        }
    }

    public static <K, U, V> CompletableFuture<Map<K, V>> schedule(Map<K, U> inputs, BiFunction<K, U, @Nullable V> operation, Executor executor) {
        int i = Util.maxAllowedExecutorThreads() * 16;
        return schedule(inputs, operation, i, executor);
    }

    static class BatchedTaskSplitter<K, U, V> extends ParallelMapTransform.SplitterBase<K, U, V> {
        private final Map<K, V> result;
        private final int batchSize;
        private final int firstUndersizedBatchIndex;

        BatchedTaskSplitter(BiFunction<K, U, V> p_405224_, int p_405356_, int p_405090_) {
            super(p_405224_, p_405356_, p_405090_);
            this.result = new HashMap<>(p_405356_);
            this.batchSize = Mth.positiveCeilDiv(p_405356_, p_405090_);
            int i = this.batchSize * p_405090_;
            int j = i - p_405356_;
            this.firstUndersizedBatchIndex = p_405090_ - j;

            assert this.firstUndersizedBatchIndex > 0 && this.firstUndersizedBatchIndex <= p_405090_;
        }

        @Override
        protected CompletableFuture<?> scheduleBatch(ParallelMapTransform.Container<K, U, V> p_405061_, int p_405498_, int p_405344_, Executor p_404656_) {
            int i = p_405344_ - p_405498_;

            assert i == this.batchSize || i == this.batchSize - 1;

            return CompletableFuture.runAsync(createTask(this.result, p_405498_, p_405344_, p_405061_), p_404656_);
        }

        @Override
        protected int batchSize(int p_405542_) {
            return p_405542_ < this.firstUndersizedBatchIndex ? this.batchSize : this.batchSize - 1;
        }

        private static <K, U, V> Runnable createTask(Map<K, V> result, int lastScheduledIndex, int currentIndex, ParallelMapTransform.Container<K, U, V> container) {
            return () -> {
                for (int i = lastScheduledIndex; i < currentIndex; i++) {
                    container.applyOperation(i);
                }

                synchronized (result) {
                    for (int j = lastScheduledIndex; j < currentIndex; j++) {
                        container.copyOut(j, result);
                    }
                }
            };
        }

        @Override
        protected CompletableFuture<Map<K, V>> scheduleFinalOperation(CompletableFuture<?> p_405225_, ParallelMapTransform.Container<K, U, V> p_404779_) {
            Map<K, V> map = this.result;
            return p_405225_.thenApply(p_405484_ -> map);
        }
    }

    record Container<K, U, V>(BiFunction<K, U, V> operation, @Nullable Object[] keys, @Nullable Object[] values) {
        public Container(BiFunction<K, U, V> p_405815_, int p_405453_) {
            this(p_405815_, new Object[p_405453_], new Object[p_405453_]);
        }

        public void put(int index, K key, U value) {
            this.keys[index] = key;
            this.values[index] = value;
        }

        private @Nullable K key(int index) {
            return (K)this.keys[index];
        }

        private @Nullable V output(int index) {
            return (V)this.values[index];
        }

        private @Nullable U input(int index) {
            return (U)this.values[index];
        }

        public void applyOperation(int index) {
            this.values[index] = this.operation.apply(this.key(index), this.input(index));
        }

        public void copyOut(int index, Map<K, V> outputMap) {
            V v = this.output(index);
            if (v != null) {
                K k = this.key(index);
                outputMap.put(k, v);
            }
        }

        public int size() {
            return this.keys.length;
        }
    }

    static class SingleTaskSplitter<K, U, V> extends ParallelMapTransform.SplitterBase<K, U, V> {
        SingleTaskSplitter(BiFunction<K, U, V> operation, int size) {
            super(operation, size, size);
        }

        @Override
        protected int batchSize(int p_405267_) {
            return 1;
        }

        @Override
        protected CompletableFuture<?> scheduleBatch(ParallelMapTransform.Container<K, U, V> p_404951_, int p_405718_, int p_404688_, Executor p_404914_) {
            assert p_405718_ + 1 == p_404688_;

            return CompletableFuture.runAsync(() -> p_404951_.applyOperation(p_405718_), p_404914_);
        }

        @Override
        protected CompletableFuture<Map<K, V>> scheduleFinalOperation(CompletableFuture<?> p_404928_, ParallelMapTransform.Container<K, U, V> p_405541_) {
            return p_404928_.thenApply(p_405680_ -> {
                Map<K, V> map = new HashMap<>(p_405541_.size());

                for (int i = 0; i < p_405541_.size(); i++) {
                    p_405541_.copyOut(i, map);
                }

                return map;
            });
        }
    }

    abstract static class SplitterBase<K, U, V> {
        private int lastScheduledIndex;
        private int currentIndex;
        private final CompletableFuture<?>[] tasks;
        private int batchIndex;
        private final ParallelMapTransform.Container<K, U, V> container;

        SplitterBase(BiFunction<K, U, V> operation, int containerSize, int numBatches) {
            this.container = new ParallelMapTransform.Container<>(operation, containerSize);
            this.tasks = new CompletableFuture[numBatches];
        }

        private int pendingBatchSize() {
            return this.currentIndex - this.lastScheduledIndex;
        }

        public CompletableFuture<Map<K, V>> scheduleTasks(Map<K, U> inputs, Executor executor) {
            inputs.forEach((p_405351_, p_405574_) -> {
                this.container.put(this.currentIndex++, (K)p_405351_, (U)p_405574_);
                if (this.pendingBatchSize() == this.batchSize(this.batchIndex)) {
                    this.tasks[this.batchIndex++] = this.scheduleBatch(this.container, this.lastScheduledIndex, this.currentIndex, executor);
                    this.lastScheduledIndex = this.currentIndex;
                }
            });

            assert this.currentIndex == this.container.size();

            assert this.lastScheduledIndex == this.currentIndex;

            assert this.batchIndex == this.tasks.length;

            return this.scheduleFinalOperation(CompletableFuture.allOf(this.tasks), this.container);
        }

        protected abstract int batchSize(int batchIndex);

        protected abstract CompletableFuture<?> scheduleBatch(
            ParallelMapTransform.Container<K, U, V> container, int lastScheduledIndex, int currentIndex, Executor executor
        );

        protected abstract CompletableFuture<Map<K, V>> scheduleFinalOperation(
            CompletableFuture<?> future, ParallelMapTransform.Container<K, U, V> container
        );
    }
}
