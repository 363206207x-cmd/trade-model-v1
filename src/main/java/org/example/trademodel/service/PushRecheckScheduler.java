package org.example.trademodel.service;

import org.example.trademodel.entity.TmPushSnapshotDO;
import org.example.trademodel.mapper.PushRecheckLogMapper;
import org.example.trademodel.mapper.PushSnapshotMapper;
import org.example.trademodel.service.support.UtcLocalTimePolicy;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.annotation.Autowired;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

@Component
public class PushRecheckScheduler {

    private static final Logger log = LoggerFactory.getLogger(PushRecheckScheduler.class);

    /**
     * 最小可控多轮 pending 集合：
     * - 首次：CAPTURED
     * - 下一轮：RECHECK_REVIEW_WAITING
     * - 历史兼容：RECHECK_VALID_WAITING
     *
     * 其它 RECHECK_* 属于终止态（不自动恢复）。
     */
    private static final String PENDING_PUSH_STATUS_CAPTURED = PushRecheckStatusContract.PUSH_STATUS_CAPTURED;
    private static final String PENDING_PUSH_STATUS_WAITING = PushRecheckStatusContract.PUSH_STATUS_REVIEW_WAITING;
    private static final String PENDING_PUSH_STATUS_LEGACY_WAITING = "RECHECK_VALID_WAITING";

    private volatile int defaultLimit;
    private volatile int maxAttempts;
    private volatile int minRetryMinutes;

    private final PushSnapshotMapper pushSnapshotMapper;
    private final PushRecheckLogMapper pushRecheckLogMapper;
    private final PushRecheckService pushRecheckService;
    private final PushRecheckDispatchConfigService dispatchConfigService;
    private final boolean schedulersEnabled;
    private final boolean pushRecheckSchedulerEnabled;
    private Clock clock = Clock.systemUTC();
    private volatile SchedulerExecution lastExecution = SchedulerExecution.notRun();

    public PushRecheckScheduler(
            PushSnapshotMapper pushSnapshotMapper,
            PushRecheckLogMapper pushRecheckLogMapper,
            PushRecheckService pushRecheckService,
            PushRecheckDispatchConfigService dispatchConfigService,
            @Value("${trademodel.recheck.dispatch.limit:50}") int defaultLimit,
            @Value("${trademodel.recheck.dispatch.maxAttempts:3}") int maxAttempts,
            @Value("${trademodel.recheck.dispatch.minRetryMinutes:5}") int minRetryMinutes,
            @Value("${trade-model.schedulers.enabled:true}") boolean schedulersEnabled,
            @Value("${trade-model.schedulers.push-recheck.enabled:true}") boolean pushRecheckSchedulerEnabled) {
        this.pushSnapshotMapper = pushSnapshotMapper;
        this.pushRecheckLogMapper = pushRecheckLogMapper;
        this.pushRecheckService = pushRecheckService;
        this.dispatchConfigService = dispatchConfigService;
        this.schedulersEnabled = schedulersEnabled;
        this.pushRecheckSchedulerEnabled = pushRecheckSchedulerEnabled;
        this.defaultLimit = defaultLimit;
        this.maxAttempts = maxAttempts;
        this.minRetryMinutes = minRetryMinutes;
        if (scheduledExecutionEnabled()) {
            applyRuntimeConfig(dispatchConfigService.loadOrInit(defaultLimit, maxAttempts, minRetryMinutes));
        }
    }

    @Autowired(required = false)
    public void setClock(Clock clock) {
        this.clock = clock != null ? clock : Clock.systemUTC();
    }

    @Scheduled(initialDelay = 15000, fixedRate = 30000)
    public void recheckPendingPushesScheduled() {
        if (!scheduledExecutionEnabled()) {
            return;
        }
        String batchId = "SCH-" + UUID.randomUUID().toString().replace("-", "");
        try {
            refreshRuntimeConfigFromStore();
            if (!PushRecheckStatusContract.isPendingPushStatusForScheduler(PENDING_PUSH_STATUS_CAPTURED)
                    || !PushRecheckStatusContract.isPendingPushStatusForScheduler(PENDING_PUSH_STATUS_WAITING)
                    || !PushRecheckStatusContract.isPendingPushStatusForScheduler(PENDING_PUSH_STATUS_LEGACY_WAITING)) {
                log.warn("[push-recheck-scheduler] pending status contract mismatch: {}/{}/{}",
                        PENDING_PUSH_STATUS_CAPTURED, PENDING_PUSH_STATUS_WAITING, PENDING_PUSH_STATUS_LEGACY_WAITING);
                return;
            }
            LocalDateTime referenceAt = UtcLocalTimePolicy.now(clock);
            LocalDateTime cutoffAt = referenceAt.minusMinutes(minRetryMinutes);
            List<TmPushSnapshotDO> pending = pushSnapshotMapper.listPendingRecheckNext(
                    PENDING_PUSH_STATUS_CAPTURED,
                    PENDING_PUSH_STATUS_WAITING,
                    PENDING_PUSH_STATUS_LEGACY_WAITING,
                    maxAttempts,
                    referenceAt,
                    cutoffAt,
                    defaultLimit);
            if (pending == null || pending.isEmpty()) {
                lastExecution = SchedulerExecution.succeeded(batchId, referenceAt, 0, 0);
                return;
            }

            log.info("[push-recheck-scheduler] pendingPushes={} (statuses={}/{}/{}, maxAttempts={}, minRetryMinutes={})",
                    pending.size(),
                    PENDING_PUSH_STATUS_CAPTURED,
                    PENDING_PUSH_STATUS_WAITING,
                    PENDING_PUSH_STATUS_LEGACY_WAITING,
                    maxAttempts,
                    minRetryMinutes);
            int failures = 0;
            for (TmPushSnapshotDO push : pending) {
                if (!handleOne(batchId, push)) failures++;
            }
            lastExecution = failures == 0
                    ? SchedulerExecution.succeeded(batchId, referenceAt, pending.size(), 0)
                    : SchedulerExecution.partial(batchId, referenceAt, pending.size(), failures);
        } catch (Exception e) {
            LocalDateTime failedAt = UtcLocalTimePolicy.now(clock);
            lastExecution = SchedulerExecution.failed(batchId, failedAt, e);
            log.error("[push-recheck-scheduler] traceId={} schedulerState=FAILED errorClass={} errorMessage={}",
                    batchId, e.getClass().getName(), e.getMessage(), e);
        }
    }

    private boolean handleOne(String batchId, TmPushSnapshotDO push) {
        if (push == null) {
            return false;
        }
        Long pushId = push.getPushId();
        String symbol = push.getSymbol();
        if (pushId == null || symbol == null || symbol.isBlank()) {
            return false;
        }

        try {
            int attempt = resolveAttempt(pushId);
            String instructionId = batchId + "-PUSH-" + pushId;
            pushRecheckService.recheck(
                    pushId,
                    null,
                    RecheckExecutionCommand.scheduled(batchId, instructionId, attempt, maxAttempts, minRetryMinutes));
            return true;
        } catch (Exception e) {
            log.error("[push-recheck-scheduler] traceId={} schedulerState=ITEM_FAILED pushId={} "
                            + "errorClass={} errorMessage={}",
                    batchId, pushId, e.getClass().getName(), e.getMessage(), e);
            return false;
        }
    }

    public SchedulerExecution getLastExecution() {
        return lastExecution;
    }

    public Map<String, Integer> getDispatchConfig() {
        refreshRuntimeConfigFromStore();
        Map<String, Integer> m = new LinkedHashMap<>();
        m.put("limit", defaultLimit);
        m.put("maxAttempts", maxAttempts);
        m.put("minRetryMinutes", minRetryMinutes);
        return m;
    }

    public Map<String, Integer> updateDispatchConfig(Integer limit, Integer attempts, Integer retryMinutes) {
        Map<String, Integer> updated = dispatchConfigService.updateConfig(
                limit, attempts, retryMinutes, "api", "DISPATCH_CONFIG_API");
        applyRuntimeConfig(updated);
        return getDispatchConfig();
    }

    private void refreshRuntimeConfigFromStore() {
        Map<String, Integer> current = dispatchConfigService.loadOrInit(defaultLimit, maxAttempts, minRetryMinutes);
        applyRuntimeConfig(current);
    }

    private void applyRuntimeConfig(Map<String, Integer> config) {
        if (config == null) {
            return;
        }
        Integer limit = config.get("limit");
        Integer attempts = config.get("maxAttempts");
        Integer retryMinutes = config.get("minRetryMinutes");
        if (limit != null && limit > 0) {
            this.defaultLimit = limit;
        }
        if (attempts != null && attempts > 0) {
            this.maxAttempts = attempts;
        }
        if (retryMinutes != null && retryMinutes > 0) {
            this.minRetryMinutes = retryMinutes;
        }
    }

    private int resolveAttempt(Long pushId) {
        Integer cnt = pushRecheckLogMapper.countByPushId(pushId);
        return cnt == null ? 1 : cnt + 1;
    }

    private boolean scheduledExecutionEnabled() {
        return schedulersEnabled && pushRecheckSchedulerEnabled;
    }

    public record SchedulerExecution(String traceId,
                                     String state,
                                     LocalDateTime occurredAt,
                                     int selectedCount,
                                     int failureCount,
                                     String errorClass,
                                     String errorMessage) {
        private static SchedulerExecution notRun() {
            return new SchedulerExecution(null, "NOT_RUN", null, 0, 0, null, null);
        }

        private static SchedulerExecution succeeded(String traceId, LocalDateTime at,
                                                    int selectedCount, int failureCount) {
            return new SchedulerExecution(traceId, "SUCCEEDED", at,
                    selectedCount, failureCount, null, null);
        }

        private static SchedulerExecution partial(String traceId, LocalDateTime at,
                                                  int selectedCount, int failureCount) {
            return new SchedulerExecution(traceId, "PARTIAL", at,
                    selectedCount, failureCount, null, null);
        }

        private static SchedulerExecution failed(String traceId, LocalDateTime at, Exception failure) {
            return new SchedulerExecution(traceId, "FAILED", at, 0, 1,
                    failure.getClass().getName(), failure.getMessage());
        }
    }
}
