package org.example.trademodel.service.impl;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.example.trademodel.market.client.MarketQuoteClient;
import org.example.trademodel.market.dto.MarketQuoteSnapshot;
import org.example.trademodel.entity.AnalysisRunDO;
import org.example.trademodel.entity.AssetStateDO;
import org.example.trademodel.entity.ExecutionPlanDO;
import org.example.trademodel.mapper.AnalysisRunMapper;
import org.example.trademodel.mapper.AssetStateMapper;
import org.example.trademodel.mapper.DecisionResultMapper;
import org.example.trademodel.mapper.ExecutionPlanMapper;
import org.example.trademodel.mapper.MissedOpportunityMapper;
import org.example.trademodel.mapper.PushSnapshotMapper;
import org.example.trademodel.mapper.RealPositionMapper;
import org.example.trademodel.service.AssetStateService;
import org.example.trademodel.service.DecisionService;
import org.example.trademodel.service.RuntimeMetricService;
import org.example.trademodel.vo.DecisionResultVO;
import org.example.trademodel.vo.LightSystemStatusVO;
import org.example.trademodel.vo.RealPositionVO;
import org.springframework.stereotype.Service;

@Service
public class DecisionServiceImpl implements DecisionService {
    private static final int MAX_DASHBOARD_SUMMARY_LIMIT = 24;

    private final DecisionResultMapper decisionResultMapper;
    private final ExecutionPlanMapper executionPlanMapper;
    private final AnalysisRunMapper analysisRunMapper;
    private final MarketQuoteClient marketQuoteClient;
    private final RealPositionMapper realPositionMapper;
    private final AssetStateService assetStateService;
    private final AssetStateMapper assetStateMapper;
    private final PushSnapshotMapper pushSnapshotMapper;
    private final MissedOpportunityMapper missedOpportunityMapper;
    private final RuntimeMetricService runtimeMetricService;

    public DecisionServiceImpl(DecisionResultMapper decisionResultMapper,
                               ExecutionPlanMapper executionPlanMapper,
                               AnalysisRunMapper analysisRunMapper,
                               MarketQuoteClient marketQuoteClient, RealPositionMapper realPositionMapper,
                               AssetStateService assetStateService,
                               AssetStateMapper assetStateMapper,
                               PushSnapshotMapper pushSnapshotMapper,
                               MissedOpportunityMapper missedOpportunityMapper,
                               RuntimeMetricService runtimeMetricService) {
        this.decisionResultMapper = decisionResultMapper;
        this.executionPlanMapper = executionPlanMapper;
        this.analysisRunMapper = analysisRunMapper;
        this.marketQuoteClient = marketQuoteClient;
        this.realPositionMapper = realPositionMapper;
        this.assetStateService = assetStateService;
        this.assetStateMapper = assetStateMapper;
        this.pushSnapshotMapper = pushSnapshotMapper;
        this.missedOpportunityMapper = missedOpportunityMapper;
        this.runtimeMetricService = runtimeMetricService;
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
        vo.setTotalDecisionsToday(decisionResultMapper.countDecisionsToday());
        long countTodayCostMs = System.currentTimeMillis() - countTodayStart;
        System.out.println("[PERF] db_count_decisions_today=" + countTodayCostMs + " ms");

        long missedStart = System.currentTimeMillis();
        int missedToday = missedOpportunityMapper.countByBizDate(LocalDate.now());
        vo.setMissedValidOpportunityCount(missedToday);
        long missedCostMs = System.currentTimeMillis() - missedStart;
        System.out.println("[PERF] db_count_missed_opportunity_biz_date=" + missedCostMs + " ms");

        vo.setConfusedCount(assetStateMapper.countSymbolsWhereConfusedScorePositive());
        vo.setPendingCount(pushSnapshotMapper.countPendingRecheckBacklog());
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
        long methodStart = System.currentTimeMillis();
        int safeLimit = normalizeDashboardSummaryLimit(limit);
        long baseStart = System.currentTimeMillis();
        List<DecisionResultVO> results = decisionResultMapper.findLatestDecisionResultsBase(safeLimit);
        long baseMs = System.currentTimeMillis() - baseStart;
        System.out.println("[PERF] decision.summary.baseList=" + baseMs + " ms");
        runtimeMetricService.recordDuration("decision.summary.baseList", baseMs);
        if (results == null || results.isEmpty()) {
            long totalMs = System.currentTimeMillis() - methodStart;
            System.out.println("[PERF] decision.summary.total=" + totalMs + " ms");
            runtimeMetricService.recordDuration("decision.summary.total", totalMs);
            System.out.println("[PERF] service_get_latest_decision_results=" + totalMs + " ms");
            return results == null ? new ArrayList<>() : results;
        }

        LinkedHashSet<String> analysisIdSet = new LinkedHashSet<>();
        for (DecisionResultVO item : results) {
            if (item == null) {
                continue;
            }
            String aid = item.getAnalysisId();
            if (aid != null && !aid.isBlank()) {
                analysisIdSet.add(aid.trim());
            }
        }
        List<String> analysisIds = new ArrayList<>(analysisIdSet);

        Map<String, ExecutionPlanDO> planByAnalysisId = new HashMap<>();
        Map<String, AnalysisRunDO> runByAnalysisId = new HashMap<>();
        if (!analysisIds.isEmpty()) {
            long planStart = System.currentTimeMillis();
            List<ExecutionPlanDO> plans = executionPlanMapper.selectLatestByAnalysisIdsTieBreak(analysisIds);
            long planMs = System.currentTimeMillis() - planStart;
            System.out.println("[PERF] decision.summary.batchPlan=" + planMs + " ms");
            runtimeMetricService.recordDuration("decision.summary.batchPlan", planMs);

            long runStart = System.currentTimeMillis();
            List<AnalysisRunDO> runs = analysisRunMapper.selectByIds(analysisIds);
            long runMs = System.currentTimeMillis() - runStart;
            System.out.println("[PERF] decision.summary.batchAnalysisRun=" + runMs + " ms");
            runtimeMetricService.recordDuration("decision.summary.batchAnalysisRun", runMs);

            if (plans != null) {
                for (ExecutionPlanDO p : plans) {
                    if (p != null && p.getAnalysisId() != null && !p.getAnalysisId().isBlank()) {
                        planByAnalysisId.put(p.getAnalysisId().trim(), p);
                    }
                }
            }
            if (runs != null) {
                for (AnalysisRunDO ar : runs) {
                    if (ar != null && ar.getAnalysisId() != null && !ar.getAnalysisId().isBlank()) {
                        runByAnalysisId.put(ar.getAnalysisId().trim(), ar);
                    }
                }
            }
        }

        for (DecisionResultVO item : results) {
            if (item == null) {
                continue;
            }
            String aid = item.getAnalysisId();
            if (aid == null || aid.isBlank()) {
                continue;
            }
            String key = aid.trim();
            mergeExecutionPlanAndAnalysisRun(item, planByAnalysisId.get(key), runByAnalysisId.get(key));
        }

        Map<String, MarketQuoteSnapshot> quoteCache = new HashMap<>();
        Map<String, RealPositionVO> openPositionMap = loadOpenPositionMap();
        for (DecisionResultVO item : results) {
            if (item == null) {
                continue;
            }
            String symbol = item.getSymbol();
            if (symbol != null && !symbol.trim().isEmpty()) {
                MarketQuoteSnapshot snapshot = quoteCache.computeIfAbsent(symbol, this::safeFetchQuote);
                if (snapshot != null) {
                    item.setLatestPrice(snapshot.getLastPrice());
                    item.setPriceChangePct(snapshot.getPriceChangePercent24h());
                    item.setPriceUpdateTimeMs(snapshot.getFetchedAtEpochMillis());
                }
            }
            RealPositionVO openPosition = openPositionMap.get(normalizeSymbol(item.getSymbol()));
            if (openPosition != null) {
                item.setHasOpenPosition(true);
                item.setPositionSide(openPosition.getPositionSide());
                item.setAvgOpenPrice(openPosition.getAvgOpenPrice());
                item.setPositionOpenTime(openPosition.getPositionOpenTime());
                item.setPositionQuantity(openPosition.getPositionQuantity());
                item.setUnrealizedPnlPct(openPosition.getUnrealizedPnlPct());
                item.setPositionStatus(openPosition.getPositionStatus());
                item.setMarkPrice(openPosition.getMarkPrice());
                item.setBreakEvenPrice(openPosition.getBreakEvenPrice());
                item.setLiquidationPrice(openPosition.getLiquidationPrice());
            } else {
                item.setHasOpenPosition(false);
                // No open position row means "not observed by read model now", not a business close conclusion.
                item.setPositionStatus(null);
            }
            annotateReadModelFallback(item);
        }
        long totalMs = System.currentTimeMillis() - methodStart;
        System.out.println("[PERF] decision.summary.total=" + totalMs + " ms");
        runtimeMetricService.recordDuration("decision.summary.total", totalMs);
        System.out.println("[PERF] service_get_latest_decision_results=" + totalMs + " ms");
        return results;
    }

    @Override
    public DecisionResultVO getLatestDecisionResultBySymbol(String symbol) {
        String normalized = normalizeSymbol(symbol);
        if (normalized == null) {
            return null;
        }
        long methodStart = System.currentTimeMillis();
        long baseStart = System.currentTimeMillis();
        DecisionResultVO row = decisionResultMapper.findLatestDecisionResultBaseBySymbol(normalized);
        long baseMs = System.currentTimeMillis() - baseStart;
        System.out.println("[PERF] decision_detail_base_ms=" + baseMs);
        runtimeMetricService.recordDuration("decision.detail.baseBySymbol", baseMs);
        if (row == null) {
            long totalMs = System.currentTimeMillis() - methodStart;
            System.out.println("[PERF] decision_detail_total_ms=" + totalMs);
            runtimeMetricService.recordDuration("decision.getLatestDecisionResultBySymbol", totalMs);
            return null;
        }

        String analysisId = row.getAnalysisId();
        if (analysisId != null && !analysisId.isBlank()) {
            String aid = analysisId.trim();
            long planStart = System.currentTimeMillis();
            ExecutionPlanDO plan = executionPlanMapper.selectLatestByAnalysisIdTieBreak(aid);
            long planMs = System.currentTimeMillis() - planStart;
            System.out.println("[PERF] decision_detail_plan_ms=" + planMs);
            runtimeMetricService.recordDuration("decision.detail.planTieBreak", planMs);

            long runStart = System.currentTimeMillis();
            AnalysisRunDO ar = analysisRunMapper.selectById(aid);
            long runMs = System.currentTimeMillis() - runStart;
            System.out.println("[PERF] decision_detail_analysis_run_ms=" + runMs);
            runtimeMetricService.recordDuration("decision.detail.analysisRun", runMs);

            mergeExecutionPlanAndAnalysisRun(row, plan, ar);
        }

        MarketQuoteSnapshot snapshot = safeFetchQuote(row.getSymbol());
        if (snapshot != null) {
            row.setLatestPrice(snapshot.getLastPrice());
            row.setPriceChangePct(snapshot.getPriceChangePercent24h());
            row.setPriceUpdateTimeMs(snapshot.getFetchedAtEpochMillis());
        }

        Map<String, RealPositionVO> openPositionMap = loadOpenPositionMap();
        RealPositionVO openPosition = openPositionMap.get(normalizeSymbol(row.getSymbol()));
        if (openPosition != null) {
            row.setHasOpenPosition(true);
            row.setPositionSide(openPosition.getPositionSide());
            row.setAvgOpenPrice(openPosition.getAvgOpenPrice());
            row.setPositionOpenTime(openPosition.getPositionOpenTime());
            row.setPositionQuantity(openPosition.getPositionQuantity());
            row.setUnrealizedPnlPct(openPosition.getUnrealizedPnlPct());
            row.setPositionStatus(openPosition.getPositionStatus());
            row.setMarkPrice(openPosition.getMarkPrice());
            row.setBreakEvenPrice(openPosition.getBreakEvenPrice());
            row.setLiquidationPrice(openPosition.getLiquidationPrice());
        } else {
            row.setHasOpenPosition(false);
            row.setPositionStatus(null);
        }
        annotateReadModelFallback(row);
        long totalMs = System.currentTimeMillis() - methodStart;
        System.out.println("[PERF] decision_detail_total_ms=" + totalMs);
        runtimeMetricService.recordDuration("decision.getLatestDecisionResultBySymbol", totalMs);
        return row;
    }

    private static void mergeExecutionPlanAndAnalysisRun(DecisionResultVO base, ExecutionPlanDO plan, AnalysisRunDO ar) {
        if (plan != null) {
            base.setRecommendedAction(plan.getRecommendedAction());
            base.setPlanMode(plan.getPlanMode());
            base.setEntryZone(plan.getEntryZone());
            base.setStopLoss(plan.getStopLoss());
            base.setTakeProfitRules(plan.getTakeProfitRules());
            base.setLeverageSuggestion(plan.getLeverageSuggestion());
            base.setPositionSuggestion(plan.getPositionSuggestion());
        }
        if (ar != null) {
            base.setDataQualityScore(ar.getDataQualityScore());
        }
    }

    @Override
    public int countOpenPositions() {
        try {
            return realPositionMapper.countOpenPositions();
        } catch (Exception ignored) {
            return 0;
        }
    }

    private MarketQuoteSnapshot safeFetchQuote(String symbol) {
        try {
            Optional<MarketQuoteSnapshot> snapshot = marketQuoteClient.fetch24hTicker(symbol);
            return snapshot.orElse(null);
        } catch (Exception ignored) {
            return null;
        }
    }

    private Map<String, RealPositionVO> loadOpenPositionMap() {
        Map<String, RealPositionVO> openPositionMap = new HashMap<>();
        try {
            List<RealPositionVO> openPositions = realPositionMapper.findOpenPositions();
            if (openPositions != null) {
                for (RealPositionVO position : openPositions) {
                    if (position == null) {
                        continue;
                    }
                    String normalized = normalizeSymbol(position.getSymbol());
                    if (normalized != null) {
                        openPositionMap.put(normalized, position);
                    }
                }
            }
        } catch (Exception ignored) {
            // 持仓表不存在或暂不可用时，按无持仓处理，避免影响首页刷新链路。
        }
        return openPositionMap;
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
