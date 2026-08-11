package org.example.trademodel.service.impl;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.example.trademodel.entity.UserPositionDO;
import org.example.trademodel.providercall.AssetPriority;
import org.example.trademodel.providercall.ProviderCallResult;
import org.example.trademodel.providercall.snapshot.MarketPriceSnapshot;
import org.example.trademodel.providercall.snapshot.MarketPriceSnapshotPolicy;
import org.example.trademodel.providercall.snapshot.MarketPriceSnapshotService;
import org.example.trademodel.entity.AssetStateDO;
import org.example.trademodel.mapper.AnalysisRunMapper;
import org.example.trademodel.mapper.AssetStateMapper;
import org.example.trademodel.mapper.DecisionResultMapper;
import org.example.trademodel.mapper.MissedOpportunityMapper;
import org.example.trademodel.mapper.PushSnapshotMapper;
import org.example.trademodel.mapper.UserPositionMapper;
import org.example.trademodel.service.AssetStateService;
import org.example.trademodel.service.ConfusedStatePolicy;
import org.example.trademodel.service.DecisionService;
import org.example.trademodel.service.RuntimeMetricService;
import org.example.trademodel.service.support.UtcLocalTimePolicy;
import org.example.trademodel.vo.DecisionResultVO;
import org.example.trademodel.vo.LightSystemStatusVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class DecisionServiceImpl implements DecisionService {
    private static final int MAX_DASHBOARD_SUMMARY_LIMIT = 24;
    private static final Set<String> USER_POSITION_SOURCE_TYPES =
            Set.of("MANUAL", "MANUAL_POSITION", "SYSTEM_PLAN_POSITION");
    private static final String STATUS_OPEN = "OPEN";
    private static final String STATUS_PARTIALLY_CLOSED = "PARTIALLY_CLOSED";

    private final DecisionResultMapper decisionResultMapper;
    private final AnalysisRunMapper analysisRunMapper;
    private final MarketPriceSnapshotService marketPriceSnapshotService;
    private final UserPositionMapper userPositionMapper;
    private final AssetStateService assetStateService;
    private final AssetStateMapper assetStateMapper;
    private final MissedOpportunityMapper missedOpportunityMapper;
    private final RuntimeMetricService runtimeMetricService;
    private Clock clock = Clock.systemUTC();

    public DecisionServiceImpl(DecisionResultMapper decisionResultMapper, AnalysisRunMapper analysisRunMapper,
                               MarketPriceSnapshotService marketPriceSnapshotService, UserPositionMapper userPositionMapper,
                               AssetStateService assetStateService,
                               AssetStateMapper assetStateMapper,
                               PushSnapshotMapper pushSnapshotMapper,
                               MissedOpportunityMapper missedOpportunityMapper,
                               RuntimeMetricService runtimeMetricService) {
        this.decisionResultMapper = decisionResultMapper;
        this.analysisRunMapper = analysisRunMapper;
        this.marketPriceSnapshotService = marketPriceSnapshotService;
        this.userPositionMapper = userPositionMapper;
        this.assetStateService = assetStateService;
        this.assetStateMapper = assetStateMapper;
        this.missedOpportunityMapper = missedOpportunityMapper;
        this.runtimeMetricService = runtimeMetricService;
    }

    @Autowired(required = false)
    public void setClock(Clock clock) {
        this.clock = clock != null ? clock : Clock.systemUTC();
    }

    @Override
    public LightSystemStatusVO getLightSystemStatus() {
        long methodStart = System.currentTimeMillis();
        LightSystemStatusVO vo = new LightSystemStatusVO();
        vo.setStatus("OK");

        long distinctStart = System.currentTimeMillis();
        Integer distinct = analysisRunMapper.countDistinctSymbols();
        long distinctCostMs = System.currentTimeMillis() - distinctStart;
        System.out.println("[PERF] db_count_distinct_symbols=" + distinctCostMs + " ms");
        vo.setMonitoredCoins(distinct != null ? distinct : 0);

        long lastDecisionStart = System.currentTimeMillis();
        vo.setLastDecisionTime(decisionResultMapper.selectLastDecisionTime());
        long lastDecisionCostMs = System.currentTimeMillis() - lastDecisionStart;
        System.out.println("[PERF] db_last_decision_time=" + lastDecisionCostMs + " ms");

        long countTodayStart = System.currentTimeMillis();
        LocalDate utcDate = clock.instant().atZone(ZoneOffset.UTC).toLocalDate();
        LocalDateTime startUtc = utcDate.atStartOfDay();
        LocalDateTime endUtc = startUtc.plusDays(1);
        vo.setTotalDecisionsToday(decisionResultMapper.countDecisionsInRange(startUtc, endUtc));
        long countTodayCostMs = System.currentTimeMillis() - countTodayStart;
        System.out.println("[PERF] db_count_decisions_today=" + countTodayCostMs + " ms");

        long missedStart = System.currentTimeMillis();
        int missedToday = missedOpportunityMapper.countByBizDate(utcDate);
        vo.setMissedValidOpportunityCount(missedToday);
        long missedCostMs = System.currentTimeMillis() - missedStart;
        System.out.println("[PERF] db_count_missed_opportunity_biz_date=" + missedCostMs + " ms");

        vo.setConfusedCount(assetStateMapper.countDirectionalPushBlocked(
                ConfusedStatePolicy.DIRECTIONAL_PUSH_BLOCK_THRESHOLD));
        vo.setReverseSignalCount(decisionResultMapper.countOpenSymbolsWithReverseSignal());

        // Dashboard 口径：展示 tm_asset_state 全库最近一次 hot_reset_time 对应的“当前行语义”。
        // 该信息用于全局风险提醒，不代表某个 analysisId 的独立事件流水。
        AssetStateDO hot = assetStateService.findLatestHotResetSnapshot();
        if (hot != null && hot.getHotResetTime() != null) {
            vo.setHotResetSymbol(hot.getSymbol());
            vo.setHotResetTriggerType(hot.getHotResetTriggerType());
            vo.setHotResetTriggerValue(hot.getHotResetTriggerValue());
            vo.setHotResetTime(hot.getHotResetTime());
            vo.setHotResetFired(true);
        } else {
            vo.setHotResetFired(false);
        }

        long methodCostMs = System.currentTimeMillis() - methodStart;
        System.out.println("[PERF] service_get_light_system_status=" + methodCostMs + " ms");
        runtimeMetricService.recordDuration("decision.getLightSystemStatus", methodCostMs);
        return vo;
    }

    @Override
    public List<DecisionResultVO> getLatestDecisionResults(int limit) {
        return getLatestDecisionResults(null, limit);
    }

    @Override
    public List<DecisionResultVO> getLatestDecisionResultsForUser(Long userId, int limit) {
        requireUserId(userId);
        return getLatestDecisionResults(userId, limit);
    }

    private List<DecisionResultVO> getLatestDecisionResults(Long userId, int limit) {
        long methodStart = System.currentTimeMillis();
        int safeLimit = normalizeDashboardSummaryLimit(limit);
        long queryStart = System.currentTimeMillis();
        List<DecisionResultVO> results = decisionResultMapper.findLatestDecisionResultsJoined(safeLimit);
        long queryCostMs = System.currentTimeMillis() - queryStart;
        System.out.println("[PERF] db_latest_decisions_joined=" + queryCostMs + " ms");
        if (results == null) {
            long methodCostMs = System.currentTimeMillis() - methodStart;
            System.out.println("[PERF] service_get_latest_decision_results=" + methodCostMs + " ms");
            return new ArrayList<>();
        }
        Map<String, MarketPriceSnapshot> quoteCache = new HashMap<>();
        Map<String, UserPositionDO> openPositionMap = loadOpenManualUserPositionMap(userId);
        for (DecisionResultVO item : results) {
            if (item == null) {
                continue;
            }
            String symbol = item.getSymbol();
            if (symbol != null && !symbol.trim().isEmpty()) {
                MarketPriceSnapshot snapshot = quoteCache.computeIfAbsent(symbol, this::safeFetchQuote);
                if (snapshot != null) {
                    item.setLatestPrice(snapshot.lastPrice());
                    item.setPriceChangePct(snapshot.priceChangePercent24h());
                    Instant fetchedAt = snapshot.sourceFetchedAt();
                    item.setPriceUpdateTimeMs(fetchedAt == null ? null : fetchedAt.toEpochMilli());
                }
            }
            applyManualUserPosition(item, openPositionMap.get(normalizeSymbol(item.getSymbol())));
            annotateReadModelFallback(item);
        }
        long methodCostMs = System.currentTimeMillis() - methodStart;
        System.out.println("[PERF] service_get_latest_decision_results=" + methodCostMs + " ms");
        return results;
    }

    @Override
    public DecisionResultVO getLatestDecisionResultBySymbol(String symbol) {
        return getLatestDecisionResultBySymbol(null, symbol);
    }

    @Override
    public DecisionResultVO getLatestDecisionResultBySymbolForUser(Long userId, String symbol) {
        requireUserId(userId);
        return getLatestDecisionResultBySymbol(userId, symbol);
    }

    private DecisionResultVO getLatestDecisionResultBySymbol(Long userId, String symbol) {
        String normalized = normalizeSymbol(symbol);
        if (normalized == null) {
            return null;
        }
        long methodStart = System.currentTimeMillis();
        DecisionResultVO row = decisionResultMapper.findLatestDecisionResultBySymbolJoined(normalized);
        if (row == null) {
            runtimeMetricService.recordDuration("decision.getLatestDecisionResultBySymbol", System.currentTimeMillis() - methodStart);
            return null;
        }

        MarketPriceSnapshot snapshot = safeFetchQuote(row.getSymbol());
        if (snapshot != null) {
            row.setLatestPrice(snapshot.lastPrice());
            row.setPriceChangePct(snapshot.priceChangePercent24h());
            row.setPriceUpdateTimeMs(snapshot.sourceFetchedAt() == null ? null : snapshot.sourceFetchedAt().toEpochMilli());
        }

        Map<String, UserPositionDO> openPositionMap = loadOpenManualUserPositionMap(userId);
        applyManualUserPosition(row, openPositionMap.get(normalizeSymbol(row.getSymbol())));
        annotateReadModelFallback(row);
        runtimeMetricService.recordDuration("decision.getLatestDecisionResultBySymbol", System.currentTimeMillis() - methodStart);
        return row;
    }

    @Override
    public int countOpenPositions() {
        return 0;
    }

    @Override
    public int countOpenPositionsForUser(Long userId) {
        requireUserId(userId);
        try {
            List<UserPositionDO> openPositions = userPositionMapper.listOpenByUserId(userId);
            if (openPositions == null) {
                return 0;
            }
            int count = 0;
            for (UserPositionDO position : openPositions) {
                if (isDashboardManualOpenPosition(position)) {
                    count++;
                }
            }
            return count;
        } catch (Exception ignored) {
            return 0;
        }
    }

    private MarketPriceSnapshot safeFetchQuote(String symbol) {
        try {
            if (marketPriceSnapshotService == null) return null;
            ProviderCallResult<MarketPriceSnapshot> result = marketPriceSnapshotService.peek(symbol,
                    AssetPriority.P1_WATCHLIST, Duration.ofSeconds(30), "decision-read-" + UUID.randomUUID());
            return MarketPriceSnapshotPolicy.isFresh(result) ? result.payload() : null;
        } catch (Exception ignored) {
            return null;
        }
    }

    private Map<String, UserPositionDO> loadOpenManualUserPositionMap(Long userId) {
        Map<String, UserPositionDO> openPositionMap = new HashMap<>();
        if (userId == null || userId <= 0) {
            return openPositionMap;
        }
        try {
            List<UserPositionDO> openPositions = userPositionMapper.listOpenByUserId(userId);
            if (openPositions != null) {
                for (UserPositionDO position : openPositions) {
                    if (!isDashboardManualOpenPosition(position)) {
                        continue;
                    }
                    String normalized = normalizeSymbol(position.getAssetSymbol());
                    if (normalized != null) {
                        openPositionMap.putIfAbsent(normalized, position);
                    }
                }
            }
        } catch (Exception ignored) {
            // 持仓表不存在或暂不可用时，按无持仓处理，避免影响首页刷新链路。
        }
        return openPositionMap;
    }

    private static void requireUserId(Long userId) {
        if (userId == null || userId <= 0) {
            throw new IllegalArgumentException("userId is required");
        }
    }

    private void applyManualUserPosition(DecisionResultVO item, UserPositionDO position) {
        if (position == null) {
            item.setHasOpenPosition(false);
            // No manual UserPosition row means "no user-entered open position", not a business close conclusion.
            item.setPositionStatus(null);
            item.setPositionSide(null);
            item.setAvgOpenPrice(null);
            item.setPositionOpenTime(null);
            item.setPositionQuantity(null);
            item.setUnrealizedPnlPct(null);
            item.setMarkPrice(null);
            item.setBreakEvenPrice(null);
            item.setLiquidationPrice(null);
            return;
        }
        item.setHasOpenPosition(true);
        item.setPositionSide(position.getSide());
        item.setAvgOpenPrice(position.getEntryPrice());
        item.setPositionOpenTime(position.getOpenedAt());
        item.setPositionQuantity(position.getQuantity());
        item.setUnrealizedPnlPct(null);
        item.setPositionStatus(position.getStatus());
        item.setMarkPrice(null);
        item.setBreakEvenPrice(null);
        item.setLiquidationPrice(null);
    }

    private boolean isDashboardManualOpenPosition(UserPositionDO position) {
        if (position == null) {
            return false;
        }
        String sourceType = position.getSourceType();
        if (sourceType == null || !USER_POSITION_SOURCE_TYPES.contains(sourceType.trim().toUpperCase())) {
            return false;
        }
        String status = position.getStatus();
        return status != null
                && (STATUS_OPEN.equalsIgnoreCase(status.trim())
                || STATUS_PARTIALLY_CLOSED.equalsIgnoreCase(status.trim()));
    }

    private String normalizeSymbol(String symbol) {
        if (symbol == null) {
            return null;
        }
        String normalized = symbol.trim().toUpperCase();
        return normalized.isEmpty() ? null : normalized;
    }

    /**
     * Marks rows where persisted read-model columns are still empty so API consumers see
     * LEGACY_MISSING:* instead of inferring a false closed-loop from old hard-coded defaults.
     */
    private void annotateReadModelFallback(DecisionResultVO item) {
        List<String> missing = new ArrayList<>();
        if (isBlank(item.getValidPeriod())) {
            missing.add("valid_period");
        }
        if (isBlank(item.getInvalidCondition())) {
            missing.add("invalid_condition");
        }
        if (isBlank(item.getExplanationJson())) {
            missing.add("explanation_json");
        }
        if (isBlank(item.getReviewReasons())) {
            missing.add("review_reasons");
        }
        if (isBlank(item.getAiConflictLevel())) {
            missing.add("ai_conflict_level");
        }
        if (item.getAiConflictScore() == null) {
            missing.add("ai_conflict_score");
        }
        if (item.getConfusedScore() == null) {
            missing.add("confused_score");
        }
        if (isBlank(item.getAssetStateSnapshot())) {
            missing.add("asset_state_snapshot");
        }
        if (missing.isEmpty()) {
            item.setReadModelTruthStatus("FULL");
            item.setReadModelFallbackReason(null);
        } else {
            item.setReadModelTruthStatus("PARTIAL");
            item.setReadModelFallbackReason("LEGACY_MISSING:" + String.join(",", missing));
        }
    }

    private static boolean isBlank(String s) {
        return s == null || s.trim().isEmpty();
    }

    private int normalizeDashboardSummaryLimit(int limit) {
        if (limit <= 0) {
            return 1;
        }
        return Math.min(limit, MAX_DASHBOARD_SUMMARY_LIMIT);
    }
}
