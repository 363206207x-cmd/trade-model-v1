package org.example.trademodel.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.example.trademodel.positionmonitor.PositionMonitorBatchResultDTO;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.beans.factory.annotation.Autowired;
import org.example.trademodel.v41.DashboardLiveEvent;
import org.example.trademodel.v41.DashboardLiveEventService;

import java.time.Instant;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

@Component
public class PositionMonitorScheduler {

    private static final Logger log = LoggerFactory.getLogger(PositionMonitorScheduler.class);

    private final PositionMonitorService positionMonitorService;
    private final boolean schedulersEnabled;
    private final boolean positionMonitorSchedulerEnabled;
    private final ConcurrentMap<Long, Long> pendingInitialMonitors = new ConcurrentHashMap<>();
    private final AtomicBoolean immediateMonitorRequested = new AtomicBoolean();
    private DashboardLiveEventService dashboardLiveEventService;
    private org.example.trademodel.mapper.UserPositionMapper contextPositions;
    private org.example.trademodel.mapper.DecisionResultMapper contextDecisions;
    private org.example.trademodel.mapper.AnalysisRunMapper contextRuns;
    private com.fasterxml.jackson.databind.ObjectMapper contextJson;
    private org.example.trademodel.derivatives.DerivativesSnapshotReadPort contextDerivatives;

    @Autowired
    void setStructuralContextDerivatives(org.example.trademodel.derivatives.DerivativesSnapshotReadPort value) {
        this.contextDerivatives = value;
    }

    @Autowired
    void setStructuralContextSources(org.example.trademodel.mapper.UserPositionMapper positions,
                                     org.example.trademodel.mapper.DecisionResultMapper decisions,
                                     org.example.trademodel.mapper.AnalysisRunMapper runs,
                                     com.fasterxml.jackson.databind.ObjectMapper json) {
        this.contextPositions = positions;
        this.contextDecisions = decisions;
        this.contextRuns = runs;
        this.contextJson = json;
    }

    public PositionMonitorScheduler(
            PositionMonitorService positionMonitorService,
            @Value("${trade-model.schedulers.enabled:false}") boolean schedulersEnabled,
            @Value("${trade-model.schedulers.position-monitor.enabled:false}") boolean positionMonitorSchedulerEnabled) {
        this.positionMonitorService = positionMonitorService;
        this.schedulersEnabled = schedulersEnabled;
        this.positionMonitorSchedulerEnabled = positionMonitorSchedulerEnabled;
    }

    @Autowired
    void setDashboardLiveEventService(DashboardLiveEventService value) {
        this.dashboardLiveEventService = value;
    }

    @Scheduled(initialDelayString = "${trade-model.schedulers.position-monitor.initial-delay-ms:15000}",
            fixedRateString = "${trade-model.schedulers.position-monitor.fixed-rate-ms:30000}")
    public void monitorOpenUserPositionsScheduled() {
        monitorOpenPositions("PERIODIC_30S");
    }

    @Scheduled(initialDelayString = "${trade-model.schedulers.position-monitor.immediate-delay-ms:1000}",
            fixedDelayString = "${trade-model.schedulers.position-monitor.immediate-rate-ms:1000}")
    public void monitorImmediateRiskRequestsScheduled() {
        if (immediateMonitorRequested.compareAndSet(true, false)) {
            monitorOpenPositions("REALTIME_SHOCK");
        }
    }

    public void requestImmediateRiskMonitor(String reasonCode) {
        if (!scheduledExecutionEnabled()) return;
        immediateMonitorRequested.set(true);
        log.info("[position-monitor-scheduler] immediate read-only monitor requested reason={}",
                sanitizedReason(reasonCode));
    }

    private synchronized void monitorOpenPositions(String trigger) {
        if (!scheduledExecutionEnabled()) {
            return;
        }
        refreshStructuralContexts(null);
        try {
            PositionMonitorBatchResultDTO batch = positionMonitorService.monitorClaimedOpenPositionsForSystem();
            if (batch == null) {
                log.warn("[position-monitor-scheduler] batch completed without a result summary");
                return;
            }
            log.info("[position-monitor-scheduler] batch completed trigger={} total={} success={} failure={} blocked={}",
                    trigger,
                    batch.getTotalCount(), batch.getSuccessCount(),
                    batch.getFailureCount(), batch.getBlockedCount());
            publishRefreshEvent(batch);
            if (batch.getFailureCount() > 0) {
                log.warn("[position-monitor-scheduler] failure summary={}", failureReasonSummary(batch));
            }
        } catch (RuntimeException ex) {
            log.warn("[position-monitor-scheduler] batch skipped: {}", ex.getMessage());
        }
    }

    /** Refresh memory from persisted positions/decisions only; never run a monitor batch or mutate positions. */
    public void refreshStructuralContextsForUser(Long userId) {
        if (userId != null && userId > 0) refreshStructuralContexts(userId);
    }

    private synchronized void refreshStructuralContexts(Long userId) {
        if (dashboardLiveEventService == null || contextPositions == null) return;
        try {
            var rows = userId == null ? contextPositions.listClaimedOpenForSystemMonitoring()
                    : contextPositions.listOpenByUserId(userId);
            if (rows == null) return; // a failed read is not evidence that all positions were closed
            var scopes = new java.util.LinkedHashMap<Long, java.util.LinkedHashSet<String>>();
            for (var position : rows) {
                if (position == null || position.getUserId() == null || position.getUserId() <= 0
                        || userId != null && !userId.equals(position.getUserId())
                        || !("OPEN".equals(position.getStatus()) || "PARTIALLY_CLOSED".equals(position.getStatus()))
                        || !java.util.Set.of("MANUAL", "MANUAL_INDEPENDENT", "MANUAL_POSITION", "SYSTEM_PLAN_POSITION")
                        .contains(String.valueOf(position.getSourceType()))
                        || position.getAssetSymbol() == null || position.getAssetSymbol().isBlank()) continue;
                scopes.computeIfAbsent(position.getUserId(), ignored -> new java.util.LinkedHashSet<>())
                        .add(position.getAssetSymbol().trim().toUpperCase(java.util.Locale.ROOT));
            }
            var contexts = new java.util.ArrayList<DashboardLiveEventService.ScopedStructuralContext>();
            for (var scope : scopes.entrySet()) {
                var decisions = contextDecisions.findLatestDecisionResultsForSymbolsJoined(
                        java.util.List.copyOf(scope.getValue()), "USER", scope.getKey());
                if (decisions == null) throw new IllegalStateException("CONTEXT_DECISION_READ_UNAVAILABLE");
                var selected = new java.util.HashSet<String>();
                for (var decision : decisions) {
                    if (decision == null || !scope.getValue().contains(decision.getSymbol())
                            || !"1h".equalsIgnoreCase(decision.getTimeframe())
                            || !selected.add(decision.getSymbol())) continue;
                    var context = persistedStructuralContext(scope.getKey(), decision);
                    if (context != null) contexts.add(new DashboardLiveEventService.ScopedStructuralContext(
                            scope.getKey(), context));
                }
            }
            dashboardLiveEventService.replacePositionStructuralContexts(userId, contexts);
        } catch (RuntimeException failure) {
            // Keep the last complete registry on read failure; periodic base-price monitoring still runs.
            log.warn("[position-monitor-scheduler] context refresh unavailable type={}",
                    failure.getClass().getSimpleName());
        }
    }

    private DashboardLiveEventService.StructuralContext persistedStructuralContext(
            Long userId, org.example.trademodel.vo.DecisionResultVO decision) {
        var run = contextRuns.selectReadableByUser(decision.getAnalysisId(), userId);
        if (run == null || !java.util.Objects.equals(run.getAnalysisId(), decision.getAnalysisId())
                || !java.util.Objects.equals(run.getSymbol(), decision.getSymbol())
                || !"SUCCESS".equals(run.getStatus()) || Boolean.TRUE.equals(run.getPreview())
                || !"1h".equalsIgnoreCase(run.getTimeframe()) || run.getTraceId() == null
                || run.getTraceId().isBlank() || decision.getDecisionId() == null
                || !java.util.Set.of("READY", "PARTIAL").contains(String.valueOf(decision.getDirectionDataState()))
                || !("SYSTEM".equals(run.getOwnerType()) && Long.valueOf(0).equals(run.getOwnerId())
                || "USER".equals(run.getOwnerType()) && userId.equals(run.getOwnerId()))) return null;
        String direction = java.util.stream.Stream.of(decision.getValidatedMarketBias(),
                        decision.getFinalMarketBias(), decision.getMarketBiasHierarchy())
                .filter(value -> value != null && !value.isBlank()).findFirst().orElse(null);
        if (direction == null) return null;
        try {
            var evidence = contextJson.readTree(decision.getExplanationJson() == null ? "{}" : decision.getExplanationJson());
            var atr = evidence.path("atr1h");
            if (!atr.isNumber() || atr.decimalValue().signum() <= 0 || decision.getCreateTime() == null) return null;
            java.math.BigDecimal invalidation = null;
            if (decision.getInvalidCondition() != null && decision.getInvalidCondition().trim().startsWith("{")) {
                var boundary = contextJson.readTree(decision.getInvalidCondition());
                var level = boundary.has("invalidPriceBelow") ? boundary.path("invalidPriceBelow")
                        : boundary.path("invalidPriceAbove");
                if (level.isNumber() && level.decimalValue().signum() > 0) invalidation = level.decimalValue();
            }
            if (invalidation == null && decision.getStopLoss() != null
                    && decision.getStopLoss().matches("[0-9]+(?:\\.[0-9]+)?")) {
                var stop = new java.math.BigDecimal(decision.getStopLoss());
                if (stop.signum() > 0) invalidation = stop;
            }
            var calculatedAt = decision.getCreateTime().toInstant(java.time.ZoneOffset.UTC);
            boolean derivativesFresh = false;
            if (contextDerivatives != null) {
                try {
                var cached = contextDerivatives.readCached(decision.getSymbol(),
                        org.example.trademodel.providercall.AssetPriority.P1_WATCHLIST,
                        java.time.Duration.ofMinutes(10), run.getTraceId());
                var snapshot = cached == null ? null : cached.payload();
                derivativesFresh = snapshot != null && decision.getSymbol().equals(snapshot.symbol())
                        && snapshot.providerDataTime() != null
                        && !snapshot.providerDataTime().isBefore(Instant.now().minusSeconds(600))
                        && !snapshot.providerDataTime().isAfter(Instant.now());
                } catch (RuntimeException unavailable) {
                    // Optional cache failure does not remove basic monitoring or retain a closed position's scope.
                    derivativesFresh = false;
                }
            }
            // Only persisted/cached facts; no Final Plan or AI is required for base-price monitoring.
            return new DashboardLiveEventService.StructuralContext(decision.getSymbol(), direction,
                    atr.decimalValue(), invalidation, decision.getFinalConfidence(), decision.getAnalysisId(),
                    decision.getDecisionId(), run.getTraceId(), calculatedAt, calculatedAt.plusSeconds(90 * 60), derivativesFresh);
        } catch (com.fasterxml.jackson.core.JsonProcessingException invalidEvidence) {
            return null;
        }
    }

    private void publishRefreshEvent(PositionMonitorBatchResultDTO batch) {
        if (dashboardLiveEventService == null) return;
        long version = System.currentTimeMillis();
        dashboardLiveEventService.publish(new DashboardLiveEvent(
                "position-monitor-" + version, "POSITION_MONITOR_UPDATED", "SYSTEM", version,
                Instant.now(), Instant.now(), Map.of("refreshRequired", true,
                "successCount", batch.getSuccessCount(), "failureCount", batch.getFailureCount(),
                "version", "V41-POSITION-RISK-VECTOR-1")));
    }

    public void requestInitialMonitor(Long positionId, Long userId) {
        if (positionId == null || positionId <= 0) {
            throw new IllegalArgumentException("positionId is required");
        }
        if (userId == null || userId <= 0) {
            throw new IllegalArgumentException("userId is required");
        }
        pendingInitialMonitors.put(positionId, userId);
    }

    @Scheduled(initialDelayString = "${trade-model.schedulers.position-monitor.initial-request-delay-ms:1000}",
            fixedDelayString = "${trade-model.schedulers.position-monitor.initial-request-rate-ms:1000}")
    public void monitorInitialRequestsScheduled() {
        if (!scheduledExecutionEnabled() || pendingInitialMonitors.isEmpty()) {
            return;
        }
        var claimed = java.util.Map.copyOf(pendingInitialMonitors);
        for (var request : claimed.entrySet()) {
            try {
                positionMonitorService.monitorUserPositionForUser(request.getKey(), request.getValue());
                pendingInitialMonitors.remove(request.getKey(), request.getValue());
            } catch (RuntimeException ex) {
                log.warn("[position-monitor-scheduler] initial position retained for retry positionId={}",
                        request.getKey());
            }
        }
    }

    int pendingInitialMonitorCount() {
        return pendingInitialMonitors.size();
    }

    boolean scheduledExecutionEnabled() {
        return schedulersEnabled && positionMonitorSchedulerEnabled;
    }

    static String failureReasonSummary(PositionMonitorBatchResultDTO batch) {
        if (batch == null || batch.getFailures().isEmpty()) {
            return "NONE";
        }
        Map<String, Long> counts = batch.getFailures().stream()
                .collect(Collectors.groupingBy(
                        failure -> sanitizedReason(failure == null ? null : failure.getReason()),
                        TreeMap::new,
                        Collectors.counting()));
        return counts.entrySet().stream()
                .map(entry -> entry.getKey() + "=" + entry.getValue())
                .collect(Collectors.joining(","));
    }

    private static String sanitizedReason(String reason) {
        if (reason == null || reason.isBlank()) {
            return "UNKNOWN";
        }
        String token = reason.trim().toUpperCase(java.util.Locale.ROOT)
                .replaceAll("[^A-Z0-9:_-]", "_");
        return token.length() <= 120 ? token : token.substring(0, 120);
    }
}
