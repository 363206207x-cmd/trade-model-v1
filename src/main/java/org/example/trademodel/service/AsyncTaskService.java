package org.example.trademodel.service;

import org.example.trademodel.entity.AsyncTaskDO;
import org.example.trademodel.enums.AsyncTaskStateEnum;
import org.example.trademodel.mapper.AsyncTaskMapper;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

@Service
public class AsyncTaskService {
    private static final Set<String> TASK_TYPES = Set.of(
            "POOL_SCAN", "ANALYSIS_PREVIEW", "REANALYSIS",
            "THREE_AI", "PLAN_REVALIDATION", "HOT_RESET");

    private final AsyncTaskMapper mapper;
    private final Clock clock = Clock.systemUTC();

    public AsyncTaskService(AsyncTaskMapper mapper) {
        this.mapper = mapper;
    }

    public AsyncTaskDO queueForUser(Long userId, String rawTaskType,
                                    String resourceType, String resourceId, String traceId) {
        if (userId == null || userId <= 0) {
            throw new IllegalArgumentException("userId is required");
        }
        return queue("USER", userId, rawTaskType, resourceType, resourceId, traceId);
    }

    public AsyncTaskDO queueForSystem(String rawTaskType,
                                      String resourceType, String resourceId, String traceId) {
        return queue("SYSTEM", 0L, rawTaskType, resourceType, resourceId, traceId);
    }

    private AsyncTaskDO queue(String ownerType, Long ownerId, String rawTaskType,
                              String resourceType, String resourceId, String traceId) {
        String taskType = normalizeTaskType(rawTaskType);
        LocalDateTime now = LocalDateTime.now(clock);
        AsyncTaskDO row = new AsyncTaskDO();
        row.setTaskId("task-" + UUID.randomUUID());
        row.setOwnerType(ownerType);
        row.setOwnerId(ownerId);
        row.setTaskType(taskType);
        row.setState(AsyncTaskStateEnum.QUEUED.name());
        row.setStage("QUEUED");
        row.setResourceType(trimToNull(resourceType));
        row.setResourceId(trimToNull(resourceId));
        row.setTraceId(trimToNull(traceId));
        row.setRetryCount(0);
        row.setMaxRetries(2);
        row.setCreatedAt(now);
        row.setUpdatedAt(now);
        mapper.insert(row);
        return row;
    }

    public List<AsyncTaskDO> listForUser(Long userId, int limit) {
        if (userId == null || userId <= 0) {
            throw new IllegalArgumentException("userId is required");
        }
        return mapper.listForUser(userId, Math.max(1, Math.min(limit, 50)));
    }

    public AsyncTaskDO markRunning(AsyncTaskDO task, String stage) {
        return transition(task, AsyncTaskStateEnum.RUNNING, stage, null, null, false);
    }

    public AsyncTaskDO complete(AsyncTaskDO task, boolean partial, String stage) {
        return transition(task, partial ? AsyncTaskStateEnum.PARTIAL : AsyncTaskStateEnum.SUCCEEDED,
                stage, null, null, true);
    }

    public AsyncTaskDO fail(AsyncTaskDO task, String errorCode, String errorMessage) {
        return transition(task, AsyncTaskStateEnum.FAILED, "FAILED",
                trimToNull(errorCode), trimToNull(errorMessage), true);
    }

    public AsyncTaskDO retryForUser(Long userId, String taskId) {
        if (userId == null || userId <= 0) {
            throw new IllegalArgumentException("userId is required");
        }
        AsyncTaskDO task = mapper.selectForUser(required(taskId, "taskId"), userId);
        if (task == null) {
            throw new IllegalArgumentException("task not found");
        }
        AsyncTaskStateEnum state = AsyncTaskStateEnum.valueOf(required(task.getState(), "state"));
        if (state != AsyncTaskStateEnum.FAILED && state != AsyncTaskStateEnum.PARTIAL) {
            throw new IllegalStateException("task is not retryable");
        }
        int retryCount = task.getRetryCount() == null ? 0 : task.getRetryCount();
        int maxRetries = task.getMaxRetries() == null ? 0 : task.getMaxRetries();
        if (retryCount >= maxRetries) {
            throw new IllegalStateException("task retry limit reached");
        }
        task.setRetryCount(retryCount + 1);
        task.setStartedAt(null);
        task.setCompletedAt(null);
        return transition(task, AsyncTaskStateEnum.QUEUED, "RETRY_QUEUED", null, null, false);
    }

    public AsyncTaskDO cancelForUser(Long userId, String taskId) {
        if (userId == null || userId <= 0) {
            throw new IllegalArgumentException("userId is required");
        }
        AsyncTaskDO task = mapper.selectForUser(required(taskId, "taskId"), userId);
        if (task == null) {
            throw new IllegalArgumentException("task not found");
        }
        AsyncTaskStateEnum state = AsyncTaskStateEnum.valueOf(required(task.getState(), "state"));
        if (state != AsyncTaskStateEnum.QUEUED && state != AsyncTaskStateEnum.RUNNING
                && state != AsyncTaskStateEnum.PARTIAL) {
            throw new IllegalStateException("task is not cancellable");
        }
        return transition(task, AsyncTaskStateEnum.CANCELLED, "CANCELLED", null, null, true);
    }

    private AsyncTaskDO transition(AsyncTaskDO task, AsyncTaskStateEnum state, String stage,
                                   String errorCode, String errorMessage, boolean terminal) {
        if (task == null || task.getTaskId() == null || task.getOwnerId() == null) {
            throw new IllegalArgumentException("persisted task is required");
        }
        LocalDateTime now = LocalDateTime.now(clock);
        task.setState(state.name());
        task.setStage(trimToNull(stage) == null ? state.name() : stage.trim());
        task.setErrorCode(errorCode);
        task.setErrorMessage(errorMessage);
        if (state == AsyncTaskStateEnum.RUNNING && task.getStartedAt() == null) task.setStartedAt(now);
        if (terminal) task.setCompletedAt(now);
        task.setUpdatedAt(now);
        if (mapper.updateState(task) != 1) {
            throw new IllegalStateException("task state transition was not persisted");
        }
        return task;
    }

    private String normalizeTaskType(String raw) {
        String value = trimToNull(raw);
        if (value == null || !TASK_TYPES.contains(value.toUpperCase(Locale.ROOT))) {
            throw new IllegalArgumentException("unsupported taskType");
        }
        return value.toUpperCase(Locale.ROOT);
    }

    private static String trimToNull(String value) {
        return value == null || value.trim().isEmpty() ? null : value.trim();
    }

    private static String required(String value, String field) {
        String normalized = trimToNull(value);
        if (normalized == null) throw new IllegalArgumentException(field + " is required");
        return normalized;
    }
}
