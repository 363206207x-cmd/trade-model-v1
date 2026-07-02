package org.example.trademodel.service;

import org.example.trademodel.entity.TmPushSnapshotDO;
import org.example.trademodel.market.client.MarketQuoteClient;
import org.example.trademodel.mapper.PushRecheckLogMapper;
import org.example.trademodel.mapper.PushSnapshotMapper;
import org.example.trademodel.market.dto.MarketQuoteSnapshot;
import org.springframework.beans.factory.annotation.Value;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
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
    private final MarketQuoteClient marketQuoteClient;
    private final PushRecheckService pushRecheckService;
    private final PushRecheckDispatchConfigService dispatchConfigService;
    private final boolean schedulersEnabled;
    private final boolean pushRecheckSchedulerEnabled;

    public PushRecheckScheduler(
            PushSnapshotMapper pushSnapshotMapper,
            PushRecheckLogMapper pushRecheckLogMapper,
            MarketQuoteClient marketQuoteClient,
            PushRecheckService pushRecheckService,
            PushRecheckDispatchConfigService dispatchConfigService,
            @Value("${trademodel.recheck.dispatch.limit:50}") int defaultLimit,
            @Value("${trademodel.recheck.dispatch.maxAttempts:3}") int maxAttempts,
            @Value("${trademodel.recheck.dispatch.minRetryMinutes:5}") int minRetryMinutes,
            @Value("${trade-model.schedulers.enabled:true}") boolean schedulersEnabled,
            @Value("${trade-model.schedulers.push-recheck.enabled:true}") boolean pushRecheckSchedulerEnabled) {
        this.pushSnapshotMapper = pushSnapshotMapper;
        this.pushRecheckLogMapper = pushRecheckLogMapper;
        this.marketQuoteClient = marketQuoteClient;
        this.pushRecheckService = pushRecheckService;
        this.dispatchConfigService = dispatchConfigService;
        this.schedulersEnabled = schedulersEnabled;
        this.pushRecheckSchedulerEnabled = pushRecheckSchedulerEnabled;
        applyRuntimeConfig(dispatchConfigService.loadOrInit(defaultLimit, maxAttempts, minRetryMinutes));
    }

    @Scheduled(initialDelay = 15000, fixedRate = 30000)
    public void recheckPendingPushesScheduled() {
        if (!scheduledExecutionEnabled()) {
            return;
        }
        try {
            refreshRuntimeConfigFromStore();
            if (!PushRecheckStatusContract.isPendingPushStatusForScheduler(PENDING_PUSH_STATUS_CAPTURED)
                    || !PushRecheckStatusContract.isPendingPushStatusForScheduler(PENDING_PUSH_STATUS_WAITING)
                    || !PushRecheckStatusContract.isPendingPushStatusForScheduler(PENDING_PUSH_STATUS_LEGACY_WAITING)) {
                log.warn("[push-recheck-scheduler] pending status contract mismatch: {}/{}/{}",
                        PENDING_PUSH_STATUS_CAPTURED, PENDING_PUSH_STATUS_WAITING, PENDING_PUSH_STATUS_LEGACY_WAITING);
                return;
            }
            List<TmPushSnapshotDO> pending = pushSnapshotMapper.listPendingRecheckNext(
                    PENDING_PUSH_STATUS_CAPTURED,
                    PENDING_PUSH_STATUS_WAITING,
                    PENDING_PUSH_STATUS_LEGACY_WAITING,
                    maxAttempts,
                    minRetryMinutes,
                    defaultLimit);
            if (pending == null || pending.isEmpty()) {
                return;
            }

            String batchId = "SCH-" + UUID.randomUUID().toString().replace("-", "");

            log.info("[push-recheck-scheduler] pendingPushes={} (statuses={}/{}/{}, maxAttempts={}, minRetryMinutes={})",
                    pending.size(),
                    PENDING_PUSH_STATUS_CAPTURED,
                    PENDING_PUSH_STATUS_WAITING,
                    PENDING_PUSH_STATUS_LEGACY_WAITING,
                    maxAttempts,
                    minRetryMinutes);
            for (TmPushSnapshotDO push : pending) {
                handleOne(batchId, push);
            }
        } catch (Exception e) {
            // 理论上不会到这里；但最小化风险：让下一轮继续执行
            log.warn("[push-recheck-scheduler] batch failed: {}", e.getMessage());
        }
    }

    private void handleOne(String batchId, TmPushSnapshotDO push) {
        if (push == null) {
            return;
        }
        Long pushId = push.getPushId();
        String symbol = push.getSymbol();
        if (pushId == null || symbol == null || symbol.isBlank()) {
            return;
        }

        try {
            Optional<MarketQuoteSnapshot> opt = marketQuoteClient.fetch24hTicker(symbol);
            if (opt.isEmpty()) {
                log.debug("[push-recheck-scheduler] skip price not found: pushId={} symbol={}", pushId, symbol);
                return;
            }

            BigDecimal price = opt.get().getLastPrice();
            if (price == null || price.compareTo(BigDecimal.ZERO) <= 0) {
                log.debug("[push-recheck-scheduler] skip invalid price: pushId={} lastPrice={}", pushId, price);
                return;
            }

            int attempt = resolveAttempt(pushId);
            String instructionId = batchId + "-PUSH-" + pushId;
            pushRecheckService.recheck(
                    pushId,
                    price,
                    RecheckExecutionCommand.scheduled(batchId, instructionId, attempt, maxAttempts, minRetryMinutes));
        } catch (Exception e) {
            // 单条失败不要拖垮整轮
            log.warn("[push-recheck-scheduler] skip pushId={} due to err={}", pushId, e.getMessage());
        }
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
}
