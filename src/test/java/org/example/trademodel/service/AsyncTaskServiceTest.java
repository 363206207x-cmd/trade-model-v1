package org.example.trademodel.service;

import org.example.trademodel.entity.AsyncTaskDO;
import org.example.trademodel.entity.AnalysisRunDO;
import org.example.trademodel.mapper.AnalysisRunMapper;
import org.example.trademodel.mapper.AsyncTaskMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@Tag("core-regression")
class AsyncTaskServiceTest {
    @Mock
    private AsyncTaskMapper mapper;
    @Mock
    private AnalysisRunMapper analysisRunMapper;

    private AsyncTaskService service;

    @BeforeEach
    void setUp() {
        service = new AsyncTaskService(mapper);
    }

    @Test
    void userAndSystemTasksUseDistinctPersistentOwners() {
        when(mapper.insert(any())).thenReturn(1);
        AsyncTaskDO user = service.queueForUser(41L, "POOL_SCAN", "ASSET_POOL", "pool-41", "trace-user");
        AsyncTaskDO system = service.queueForSystem("HOT_RESET", "FINAL_PLAN", "plan-1", "trace-system");

        assertThat(user.getOwnerType()).isEqualTo("USER");
        assertThat(user.getOwnerId()).isEqualTo(41L);
        assertThat(system.getOwnerType()).isEqualTo("SYSTEM");
        assertThat(system.getOwnerId()).isZero();
        assertThat(user.getState()).isEqualTo("QUEUED");
        assertThat(system.getState()).isEqualTo("QUEUED");
    }

    @Test
    void repeatedPreviewSubmissionReusesOnePersistentTaskIdentity() {
        when(mapper.insertIfAbsent(any())).thenReturn(1, 0);
        AsyncTaskDO canonical = persisted("QUEUED", 0, 2);
        canonical.setTaskId("task-canonical-preview");
        canonical.setTaskType("ANALYSIS_PREVIEW");
        canonical.setResourceType("ASSET");
        canonical.setResourceId("BTCUSDT:5m");
        canonical.setIdempotencyKey("preview:41:BTCUSDT:5m");
        when(mapper.selectByIdempotencyKey("USER", 41L, "preview:41:BTCUSDT:5m"))
                .thenReturn(canonical);

        AsyncTaskDO first = service.queueIdempotentForUser(
                41L, "ANALYSIS_PREVIEW", "ASSET", "BTCUSDT:5m", null,
                "preview:41:BTCUSDT:5m");
        AsyncTaskDO retry = service.queueIdempotentForUser(
                41L, "ANALYSIS_PREVIEW", "ASSET", "BTCUSDT:5m", null,
                "preview:41:BTCUSDT:5m");

        assertThat(first.getTaskId()).isEqualTo("task-canonical-preview");
        assertThat(retry.getTaskId()).isEqualTo(first.getTaskId());
        assertThat(retry.getState()).isEqualTo("QUEUED");
    }

    @Test
    void queuedTaskHasOneAtomicExecutionClaimAcrossConcurrentWorkers() {
        AsyncTaskDO task = persisted("QUEUED", 0, 2);
        when(mapper.claimQueued(any(), any(), any(), any())).thenReturn(1, 0);

        assertThat(service.claimForExecution(task, "ANALYSIS")).isTrue();
        assertThat(service.claimForExecution(task, "ANALYSIS")).isFalse();

        verify(mapper, org.mockito.Mockito.times(2)).claimQueued(
                org.mockito.ArgumentMatchers.eq(task.getTaskId()),
                org.mockito.ArgumentMatchers.eq("ANALYSIS"), any(), any());
    }

    @Test
    void analysisIdentityIsPersistedOnTaskForRefreshRecovery() {
        AsyncTaskDO task = persisted("RUNNING", 0, 2);
        when(mapper.updateResultIdentity(task)).thenReturn(1);

        AsyncTaskDO bound = service.bindResultIdentity(task, "analysis-41", "trace-41");

        assertThat(bound.getResultResourceId()).isEqualTo("analysis-41");
        assertThat(bound.getTraceId()).isEqualTo("trace-41");
        verify(mapper).updateResultIdentity(task);
    }

    @Test
    void bindingAResultThatAlreadyFinishedClosesTheCanonicalTaskRace() {
        service = new AsyncTaskService(mapper, analysisRunMapper);
        AsyncTaskDO task = persisted("RUNNING", 0, 2);
        AnalysisRunDO run = new AnalysisRunDO();
        run.setAnalysisId("analysis-finished-before-bind");
        run.setStatus("SUCCESS");
        when(mapper.updateResultIdentity(task)).thenReturn(1);
        when(analysisRunMapper.selectById("analysis-finished-before-bind")).thenReturn(run);
        when(mapper.completeByResultResourceId(
                org.mockito.ArgumentMatchers.eq("analysis-finished-before-bind"),
                org.mockito.ArgumentMatchers.eq("SUCCEEDED"),
                org.mockito.ArgumentMatchers.eq("COMPLETE"),
                org.mockito.ArgumentMatchers.isNull(),
                org.mockito.ArgumentMatchers.isNull(), any(), any())).thenReturn(1);

        service.bindResultIdentity(task, "analysis-finished-before-bind", "trace-finished");

        assertThat(task.getState()).isEqualTo("SUCCEEDED");
        assertThat(task.getStage()).isEqualTo("COMPLETE");
        assertThat(task.getCompletedAt()).isNotNull();
    }

    @Test
    void failedTaskCanRetryWithinLimitAndPersistsRealCounter() {
        AsyncTaskDO task = persisted("FAILED", 0, 2);
        when(mapper.selectForUser("task-1", 41L)).thenReturn(task);
        when(mapper.updateState(any())).thenReturn(1);

        AsyncTaskDO retried = service.retryForUser(41L, "task-1");

        assertThat(retried.getState()).isEqualTo("QUEUED");
        assertThat(retried.getStage()).isEqualTo("RETRY_QUEUED");
        assertThat(retried.getRetryCount()).isEqualTo(1);
        assertThat(retried.getErrorCode()).isNull();
        assertThat(retried.getErrorMessage()).isNull();
        verify(mapper).updateState(task);
    }

    @Test
    void retryLimitAndTerminalCancelFailClosed() {
        when(mapper.selectForUser("task-limit", 41L)).thenReturn(persisted("FAILED", 2, 2));
        when(mapper.selectForUser("task-done", 41L)).thenReturn(persisted("SUCCEEDED", 0, 2));
        when(mapper.selectForUser("task-partial", 41L)).thenReturn(persisted("PARTIAL", 0, 2));

        assertThatThrownBy(() -> service.retryForUser(41L, "task-limit"))
                .isInstanceOf(IllegalStateException.class).hasMessageContaining("retry limit");
        assertThatThrownBy(() -> service.cancelForUser(41L, "task-done"))
                .isInstanceOf(IllegalStateException.class).hasMessageContaining("not cancellable");
        assertThatThrownBy(() -> service.cancelForUser(41L, "task-partial"))
                .isInstanceOf(IllegalStateException.class).hasMessageContaining("not cancellable");
    }

    @Test
    void runningTaskCanBeCancelledWithoutInventingProgress() {
        AsyncTaskDO task = persisted("RUNNING", 0, 2);
        when(mapper.selectForUser("task-1", 41L)).thenReturn(task);
        when(mapper.updateState(any())).thenReturn(1);

        AsyncTaskDO cancelled = service.cancelForUser(41L, "task-1");

        assertThat(cancelled.getState()).isEqualTo("CANCELLED");
        assertThat(cancelled.getStage()).isEqualTo("CANCELLED");
        assertThat(cancelled.getCompletedAt()).isNotNull();
        ArgumentCaptor<AsyncTaskDO> captor = ArgumentCaptor.forClass(AsyncTaskDO.class);
        verify(mapper).updateState(captor.capture());
        assertThat(captor.getValue().getState()).isEqualTo("CANCELLED");
    }

    private static AsyncTaskDO persisted(String state, int retryCount, int maxRetries) {
        AsyncTaskDO task = new AsyncTaskDO();
        task.setTaskId(state.equals("FAILED") && retryCount == 2 ? "task-limit"
                : state.equals("SUCCEEDED") ? "task-done"
                : state.equals("PARTIAL") ? "task-partial" : "task-1");
        task.setOwnerType("USER");
        task.setOwnerId(41L);
        task.setTaskType("POOL_SCAN");
        task.setState(state);
        task.setRetryCount(retryCount);
        task.setMaxRetries(maxRetries);
        task.setErrorCode("SOURCE_UNAVAILABLE");
        task.setErrorMessage("provider unavailable");
        return task;
    }
}
