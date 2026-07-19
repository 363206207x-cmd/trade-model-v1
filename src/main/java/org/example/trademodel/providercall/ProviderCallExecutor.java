package org.example.trademodel.providercall;

import jakarta.annotation.PreDestroy;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.Callable;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.FutureTask;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

/** Owns every physical provider attempt; no provider adapter runs on the common pool. */
@Service
public class ProviderCallExecutor implements AutoCloseable {
    private final ThreadPoolExecutor executor;
    private final ScheduledThreadPoolExecutor controlScheduler;
    private final int maxQueuedCalls;
    private final int reservedPriorityQueueSlots;
    private final Object admissionLock = new Object();
    private final Set<PhysicalTask<?>> outstandingTasks = ConcurrentHashMap.newKeySet();

    @Autowired
    public ProviderCallExecutor(ProviderCallProperties properties) {
        this(properties.getMaxConcurrentProviderCalls(), properties.getMaxQueuedCalls(),
                properties.getReservedPrioritySlots());
    }

    public ProviderCallExecutor(int maxWorkers, int maxQueuedCalls, int reservedPrioritySlots) {
        int workers = Math.max(1, maxWorkers);
        this.maxQueuedCalls = Math.max(1, maxQueuedCalls);
        this.reservedPriorityQueueSlots = Math.max(1,
                Math.min(this.maxQueuedCalls, Math.max(1, reservedPrioritySlots)));
        this.executor = new ThreadPoolExecutor(workers, workers, 0L, TimeUnit.MILLISECONDS,
                new ArrayBlockingQueue<>(this.maxQueuedCalls), namedFactory("provider-call-"),
                new ThreadPoolExecutor.AbortPolicy());
        this.controlScheduler = new ScheduledThreadPoolExecutor(1,
                namedFactory("provider-call-control-"));
        this.controlScheduler.setRemoveOnCancelPolicy(true);
        this.controlScheduler.setExecuteExistingDelayedTasksAfterShutdownPolicy(false);
    }

    public <T> TaskHandle<T> submit(AssetPriority priority, Callable<T> physicalCall) {
        Objects.requireNonNull(priority, "priority");
        Objects.requireNonNull(physicalCall, "physicalCall");
        PhysicalTask<T> task = new PhysicalTask<>(physicalCall);
        FutureTask<Void> control = new FutureTask<>(task, null);
        task.bind(control);
        synchronized (admissionLock) {
            if (executor.isShutdown()) throw new RejectedExecutionException("PROVIDER_EXECUTOR_SHUTDOWN");
            if (queueReservedForHigherPriority(priority, executor.getQueue().size())) {
                throw new RejectedExecutionException("PROVIDER_EXECUTOR_PRIORITY_QUEUE_RESERVED");
            }
            outstandingTasks.add(task);
            task.completion.whenComplete((ignored, failure) -> outstandingTasks.remove(task));
            try {
                executor.execute(control);
            } catch (RejectedExecutionException rejected) {
                task.cancelBeforeStart();
                throw new RejectedExecutionException("PROVIDER_EXECUTOR_QUEUE_FULL", rejected);
            }
        }
        return new TaskHandle<>(task, () -> cancelInterruptibly(task));
    }

    public ScheduledFuture<?> schedule(Runnable command, Duration delay) {
        Objects.requireNonNull(command, "command");
        Objects.requireNonNull(delay, "delay");
        return controlScheduler.schedule(command, Math.max(0L, delay.toMillis()), TimeUnit.MILLISECONDS);
    }

    public ExecutorState state() {
        return new ExecutorState(executor.getMaximumPoolSize(), maxQueuedCalls,
                executor.getActiveCount(), executor.getQueue().size(), executor.isShutdown(),
                executor.isTerminated());
    }

    int actualQueuedTaskCount() {
        return executor.getQueue().size();
    }

    int outstandingTaskCount() {
        return outstandingTasks.size();
    }

    public boolean shutdownCleanly(Duration timeout) {
        Duration bounded = timeout == null || timeout.isNegative() ? Duration.ZERO : timeout;
        executor.shutdown();
        controlScheduler.shutdown();
        try {
            boolean providerStopped = executor.awaitTermination(bounded.toMillis(), TimeUnit.MILLISECONDS);
            boolean controlStopped = controlScheduler.awaitTermination(bounded.toMillis(), TimeUnit.MILLISECONDS);
            if (!providerStopped) executor.shutdownNow();
            if (!controlStopped) controlScheduler.shutdownNow();
            if (!providerStopped) cancelOutstandingTasks();
            return providerStopped && controlStopped;
        } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
            executor.shutdownNow();
            controlScheduler.shutdownNow();
            cancelOutstandingTasks();
            return false;
        }
    }

    private void cancelOutstandingTasks() {
        outstandingTasks.forEach(this::cancelInterruptibly);
    }

    private CancellationOutcome cancelInterruptibly(PhysicalTask<?> task) {
        CancellationOutcome queuedOutcome = null;
        synchronized (admissionLock) {
            if (task.transitionToCancelledBeforeStart()) {
                FutureTask<Void> control = task.control();
                control.cancel(false);
                queuedOutcome = executor.remove(control)
                        ? CancellationOutcome.CANCELLED_AND_REMOVED_BEFORE_START
                        : CancellationOutcome.CANCELLED_BEFORE_START_ALREADY_DEQUEUED;
            }
        }
        if (queuedOutcome != null) {
            task.completeCancelledBeforeStart("provider call cancelled before start");
            return queuedOutcome;
        }
        return task.requestRunningCancellation();
    }

    @PreDestroy
    @Override
    public void close() {
        shutdownCleanly(Duration.ofSeconds(5));
    }

    private boolean queueReservedForHigherPriority(AssetPriority priority, int queued) {
        int limit = switch (priority) {
            case P3_DISCOVERY -> maxQueuedCalls - reservedPriorityQueueSlots;
            case P1_WATCHLIST -> maxQueuedCalls - Math.max(1, reservedPriorityQueueSlots / 2);
            case P2_CANDIDATE, P0_POSITION -> maxQueuedCalls;
        };
        return queued >= Math.max(0, limit);
    }

    private static ThreadFactory namedFactory(String prefix) {
        AtomicInteger sequence = new AtomicInteger();
        return runnable -> {
            Thread thread = new Thread(runnable, prefix + sequence.incrementAndGet());
            thread.setDaemon(true);
            return thread;
        };
    }

    public record ExecutorState(
            int maxWorkers,
            int maxQueuedCalls,
            int activeWorkers,
            int queuedCalls,
            boolean shutdown,
            boolean terminated
    ) {
    }

    public static final class TaskHandle<T> {
        private final PhysicalTask<T> task;
        private final Supplier<CancellationOutcome> cancelAction;

        private TaskHandle(PhysicalTask<T> task, Supplier<CancellationOutcome> cancelAction) {
            this.task = task;
            this.cancelAction = cancelAction;
        }

        public CompletableFuture<T> completion() {
            return task.completion;
        }

        public CancellationOutcome cancelInterruptibly() {
            return cancelAction.get();
        }

        public boolean physicallyFinished() {
            TaskState current = task.state.get();
            return current == TaskState.FINISHED || current == TaskState.CANCELLED_BEFORE_START;
        }
    }

    public enum CancellationOutcome {
        CANCELLED_AND_REMOVED_BEFORE_START,
        CANCELLED_BEFORE_START_ALREADY_DEQUEUED,
        INTERRUPT_REQUESTED_FOR_RUNNING_TASK,
        ALREADY_FINISHED
    }

    private enum TaskState {
        NEW,
        RUNNING,
        FINISHED,
        CANCELLED_BEFORE_START
    }

    private static final class PhysicalTask<T> implements Runnable {
        private final Callable<T> physicalCall;
        private final CompletableFuture<T> completion = new CompletableFuture<>();
        private final AtomicReference<TaskState> state = new AtomicReference<>(TaskState.NEW);
        private volatile FutureTask<Void> control;

        private PhysicalTask(Callable<T> physicalCall) {
            this.physicalCall = physicalCall;
        }

        private void bind(FutureTask<Void> control) {
            this.control = control;
        }

        private FutureTask<Void> control() {
            return control;
        }

        @Override
        public void run() {
            if (!state.compareAndSet(TaskState.NEW, TaskState.RUNNING)) return;
            try {
                completion.complete(physicalCall.call());
            } catch (Throwable failure) {
                completion.completeExceptionally(failure);
            } finally {
                state.set(TaskState.FINISHED);
            }
        }

        private boolean transitionToCancelledBeforeStart() {
            return state.compareAndSet(TaskState.NEW, TaskState.CANCELLED_BEFORE_START);
        }

        private void completeCancelledBeforeStart(String reason) {
            completion.completeExceptionally(new CancellationException(reason));
        }

        private CancellationOutcome requestRunningCancellation() {
            if (state.get() != TaskState.RUNNING) return CancellationOutcome.ALREADY_FINISHED;
            control.cancel(true);
            return CancellationOutcome.INTERRUPT_REQUESTED_FOR_RUNNING_TASK;
        }

        private void cancelBeforeStart() {
            if (transitionToCancelledBeforeStart()) {
                completeCancelledBeforeStart("provider call rejected before start");
            }
        }
    }
}
