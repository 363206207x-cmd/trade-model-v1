package org.example.trademodel.analysisrun;

import jakarta.annotation.PreDestroy;
import org.example.trademodel.ai.AiOrchestratorProperties;
import org.example.trademodel.entity.AnalysisRunDO;
import org.example.trademodel.mapper.AnalysisRunMapper;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

@Service
public class AnalysisRunBackgroundWorker {
    private final AnalysisRunMapper mapper;
    private final AnalysisRunProperties analysisProperties;
    private final ThreadPoolExecutor executor;
    private final ScheduledThreadPoolExecutor scheduler;

    public AnalysisRunBackgroundWorker(AnalysisRunMapper mapper,
                                       AnalysisRunProperties analysisProperties,
                                       AiOrchestratorProperties aiProperties) {
        this.mapper = mapper;
        this.analysisProperties = analysisProperties;
        int workers = Math.max(1, aiProperties.getMaxConcurrentCalls());
        int queueCapacity = Math.max(1, aiProperties.getMaxQueuedCalls());
        this.executor = new ThreadPoolExecutor(workers, workers, 0L, TimeUnit.MILLISECONDS,
                new ArrayBlockingQueue<>(queueCapacity), threadFactory("analysis-background-"),
                new ThreadPoolExecutor.AbortPolicy());
        this.scheduler = new ScheduledThreadPoolExecutor(1, threadFactory("analysis-recovery-"));
        this.scheduler.setRemoveOnCancelPolicy(true);
    }

    public void submit(AnalysisRunDO run, Runnable work) {
        executor.execute(work);
    }

    public void schedule(Runnable work, long delayMs) {
        scheduler.schedule(work, Math.max(0L, delayMs), TimeUnit.MILLISECONDS);
    }

    public <T> T withLease(AnalysisRunDO run, Supplier<T> work) {
        if (run == null || run.getAnalysisId() == null || run.getLeaseOwner() == null) {
            return work.get();
        }
        long leaseSeconds = analysisProperties.getIdempotency().getLeaseSeconds();
        long heartbeatSeconds = Math.max(5L, leaseSeconds / 3L);
        ScheduledFuture<?> heartbeat = scheduler.scheduleAtFixedRate(
                () -> renew(run, leaseSeconds), heartbeatSeconds, heartbeatSeconds, TimeUnit.SECONDS);
        try {
            return work.get();
        } finally {
            heartbeat.cancel(false);
        }
    }

    private void renew(AnalysisRunDO run, long leaseSeconds) {
        LocalDateTime now = LocalDateTime.now(ZoneOffset.UTC);
        mapper.renewLease(run.getAnalysisId(), run.getLeaseOwner(),
                run.getVersionNo() == null ? 1 : run.getVersionNo(),
                now.plusSeconds(leaseSeconds), now);
    }

    private static ThreadFactory threadFactory(String prefix) {
        java.util.concurrent.atomic.AtomicInteger sequence = new java.util.concurrent.atomic.AtomicInteger();
        return task -> {
            Thread thread = new Thread(task, prefix + sequence.incrementAndGet());
            thread.setDaemon(true);
            return thread;
        };
    }

    @PreDestroy
    public void shutdown() {
        executor.shutdown();
        scheduler.shutdownNow();
    }
}
