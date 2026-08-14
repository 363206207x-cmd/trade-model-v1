package org.example.trademodel.service;

import org.example.trademodel.entity.AsyncTaskDO;
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

        assertThatThrownBy(() -> service.retryForUser(41L, "task-limit"))
                .isInstanceOf(IllegalStateException.class).hasMessageContaining("retry limit");
        assertThatThrownBy(() -> service.cancelForUser(41L, "task-done"))
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
                : state.equals("SUCCEEDED") ? "task-done" : "task-1");
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
