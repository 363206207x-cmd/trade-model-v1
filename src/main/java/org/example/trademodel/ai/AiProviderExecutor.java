package org.example.trademodel.ai;

import jakarta.annotation.PreDestroy;
import org.springframework.stereotype.Component;

import java.util.Locale;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/** Dedicated bounded executor for the three review-only AI provider roles. */
@Component
public class AiProviderExecutor {
    public static final int THREAD_COUNT = 3;
    public static final int QUEUE_CAPACITY = 3;

    private final ThreadPoolExecutor executor;

    public AiProviderExecutor() {
        AtomicInteger sequence = new AtomicInteger();
        ThreadFactory threadFactory = task -> {
            Thread thread = new Thread(task, "ai-provider-worker-" + sequence.incrementAndGet());
            thread.setDaemon(false);
            return thread;
        };
        this.executor = new ThreadPoolExecutor(
                THREAD_COUNT,
                THREAD_COUNT,
                0L,
                TimeUnit.MILLISECONDS,
                new ArrayBlockingQueue<>(QUEUE_CAPACITY),
                threadFactory,
                new ThreadPoolExecutor.AbortPolicy());
    }

    public ExecutorService executorService() {
        return executor;
    }

    public <T> Callable<T> namedTask(AiProviderName provider, Callable<T> task) {
        return () -> {
            Thread thread = Thread.currentThread();
            String originalName = thread.getName();
            String providerName = provider == null ? "unknown" : provider.name().toLowerCase(Locale.ROOT);
            thread.setName("ai-provider-" + providerName);
            try {
                return task.call();
            } finally {
                thread.setName(originalName);
            }
        };
    }

    public int getMaximumPoolSize() {
        return executor.getMaximumPoolSize();
    }

    public int getQueueCapacity() {
        return QUEUE_CAPACITY;
    }

    public int getQueueSize() {
        return executor.getQueue().size();
    }

    public boolean isShutdown() {
        return executor.isShutdown();
    }

    @PreDestroy
    public void shutdown() {
        executor.shutdownNow();
        try {
            if (!executor.awaitTermination(5, TimeUnit.SECONDS)) {
                executor.shutdownNow();
            }
        } catch (InterruptedException exception) {
            executor.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
}
