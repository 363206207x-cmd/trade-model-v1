package org.example.trademodel.analysisrun;

import org.example.trademodel.ai.AiOrchestratorProperties;
import org.example.trademodel.entity.AnalysisRunDO;
import org.example.trademodel.mapper.AnalysisRunMapper;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class AnalysisRunBackgroundWorkerTest {

    @Test
    void executesAnalysisOffRequestThreadInBoundedWorker() throws Exception {
        AnalysisRunBackgroundWorker worker = worker(1, 1, mock(AnalysisRunMapper.class));
        CountDownLatch completed = new CountDownLatch(1);
        AtomicReference<String> threadName = new AtomicReference<>();
        try {
            worker.submit(new AnalysisRunDO(), () -> {
                threadName.set(Thread.currentThread().getName());
                completed.countDown();
            });

            assertThat(completed.await(2, TimeUnit.SECONDS)).isTrue();
            assertThat(threadName.get()).startsWith("analysis-background-");
        } finally {
            worker.shutdown();
        }
    }

    @Test
    void boundedQueueRejectsExcessWorkInsteadOfCreatingUnboundedThreads() throws Exception {
        AnalysisRunBackgroundWorker worker = worker(1, 1, mock(AnalysisRunMapper.class));
        CountDownLatch firstStarted = new CountDownLatch(1);
        CountDownLatch release = new CountDownLatch(1);
        try {
            worker.submit(new AnalysisRunDO(), () -> {
                firstStarted.countDown();
                await(release);
            });
            assertThat(firstStarted.await(2, TimeUnit.SECONDS)).isTrue();
            worker.submit(new AnalysisRunDO(), () -> { });

            assertThatThrownBy(() -> worker.submit(new AnalysisRunDO(), () -> { }))
                    .isInstanceOf(RejectedExecutionException.class);
        } finally {
            release.countDown();
            worker.shutdown();
        }
    }

    @Test
    void leaseHeartbeatUpdatesOnlyTheOwnedRunFence() {
        AnalysisRunMapper mapper = mock(AnalysisRunMapper.class);
        AnalysisRunBackgroundWorker worker = worker(1, 1, mapper);
        AnalysisRunDO run = new AnalysisRunDO();
        run.setAnalysisId("analysis-lease-1");
        run.setLeaseOwner("worker-owner-1");
        run.setVersionNo(7);
        try {
            ReflectionTestUtils.invokeMethod(worker, "renew", run, 30L);

            verify(mapper).renewLease(eq("analysis-lease-1"), eq("worker-owner-1"), eq(7),
                    any(LocalDateTime.class), any(LocalDateTime.class));
        } finally {
            worker.shutdown();
        }
    }

    @Test
    void shutdownLeavesInFlightProviderWorkUninterruptedForDurableRestartRecovery() throws Exception {
        AnalysisRunBackgroundWorker worker = worker(1, 1, mock(AnalysisRunMapper.class));
        CountDownLatch started = new CountDownLatch(1);
        CountDownLatch release = new CountDownLatch(1);
        AtomicBoolean interrupted = new AtomicBoolean();
        CountDownLatch completed = new CountDownLatch(1);
        worker.submit(new AnalysisRunDO(), () -> {
            started.countDown();
            try {
                release.await();
            } catch (InterruptedException failure) {
                interrupted.set(true);
                Thread.currentThread().interrupt();
            } finally {
                completed.countDown();
            }
        });
        assertThat(started.await(2, TimeUnit.SECONDS)).isTrue();

        worker.shutdown();

        assertThat(interrupted).isFalse();
        release.countDown();
        assertThat(completed.await(2, TimeUnit.SECONDS)).isTrue();
        assertThat(interrupted).isFalse();
    }

    private static AnalysisRunBackgroundWorker worker(int workers,
                                                       int queueCapacity,
                                                       AnalysisRunMapper mapper) {
        AiOrchestratorProperties ai = new AiOrchestratorProperties();
        ai.setMaxConcurrentCalls(workers);
        ai.setMaxQueuedCalls(queueCapacity);
        return new AnalysisRunBackgroundWorker(mapper, new AnalysisRunProperties(), ai);
    }

    private static void await(CountDownLatch latch) {
        try {
            latch.await();
        } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
        }
    }
}
