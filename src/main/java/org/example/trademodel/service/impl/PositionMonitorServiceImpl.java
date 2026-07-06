package org.example.trademodel.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.trademodel.entity.ExecutionPlanDO;
import org.example.trademodel.entity.UserPositionDO;
import org.example.trademodel.mapper.DecisionResultMapper;
import org.example.trademodel.mapper.EvidenceItemMapper;
import org.example.trademodel.mapper.ExecutionPlanMapper;
import org.example.trademodel.mapper.ScoreItemMapper;
import org.example.trademodel.mapper.UserPositionMapper;
import org.example.trademodel.market.client.MarketQuoteClient;
import org.example.trademodel.market.dto.MarketQuoteSnapshot;
import org.example.trademodel.positionmonitor.PositionMonitorBatchResultDTO;
import org.example.trademodel.positionmonitor.PositionMonitorPolicy;
import org.example.trademodel.positionmonitor.PositionMonitorResultDTO;
import org.example.trademodel.positionmonitorlog.PositionMonitorLogDTO;
import org.example.trademodel.positionmonitorlog.RecordPositionMonitorLogCommand;
import org.example.trademodel.risk.UserPositionRiskAdapter;
import org.example.trademodel.risk.UserPositionRiskResult;
import org.example.trademodel.service.PositionMonitorLogService;
import org.example.trademodel.service.PositionMonitorService;
import org.example.trademodel.service.support.ExternalContextEvidenceBuilder;
import org.example.trademodel.service.support.ExternalContextPolicy;
import org.example.trademodel.service.support.ExternalContextSnapshot;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

@Service
public class PositionMonitorServiceImpl implements PositionMonitorService {
    private final UserPositionMapper userPositionMapper;
    private final MarketQuoteClient marketQuoteClient;
    private final UserPositionRiskAdapter userPositionRiskAdapter;
    private final ExecutionPlanMapper executionPlanMapper;
    private final PositionMonitorLogService positionMonitorLogService;
    private final EvidenceItemMapper evidenceItemMapper;
    private final ScoreItemMapper scoreItemMapper;
    private final DecisionResultMapper decisionResultMapper;
    private final ObjectMapper objectMapper;
    private final ExternalContextEvidenceBuilder externalContextEvidenceBuilder;

    public PositionMonitorServiceImpl(UserPositionMapper userPositionMapper,
                                      MarketQuoteClient marketQuoteClient,
                                      UserPositionRiskAdapter userPositionRiskAdapter,
                                      ExecutionPlanMapper executionPlanMapper,
                                      PositionMonitorLogService positionMonitorLogService,
                                      EvidenceItemMapper evidenceItemMapper,
                                      ScoreItemMapper scoreItemMapper,
                                      DecisionResultMapper decisionResultMapper,
                                      ObjectMapper objectMapper) {
        this(userPositionMapper, marketQuoteClient, userPositionRiskAdapter, executionPlanMapper,
                positionMonitorLogService, evidenceItemMapper, scoreItemMapper, decisionResultMapper, objectMapper, null);
    }

    @Autowired
    public PositionMonitorServiceImpl(UserPositionMapper userPositionMapper,
                                      MarketQuoteClient marketQuoteClient,
                                      UserPositionRiskAdapter userPositionRiskAdapter,
                                      ExecutionPlanMapper executionPlanMapper,
                                      PositionMonitorLogService positionMonitorLogService,
                                      EvidenceItemMapper evidenceItemMapper,
                                      ScoreItemMapper scoreItemMapper,
                                      DecisionResultMapper decisionResultMapper,
                                      ObjectMapper objectMapper,
                                      ExternalContextEvidenceBuilder externalContextEvidenceBuilder) {
        this.userPositionMapper = userPositionMapper;
        this.marketQuoteClient = marketQuoteClient;
        this.userPositionRiskAdapter = userPositionRiskAdapter;
        this.executionPlanMapper = executionPlanMapper;
        this.positionMonitorLogService = positionMonitorLogService;
        this.evidenceItemMapper = evidenceItemMapper;
        this.scoreItemMapper = scoreItemMapper;
        this.decisionResultMapper = decisionResultMapper;
        this.objectMapper = objectMapper;
        this.externalContextEvidenceBuilder = externalContextEvidenceBuilder;
    }

    @Override
    public PositionMonitorResultDTO monitorUserPosition(Long positionId) {
        if (positionId == null || positionId <= 0) {
            throw new IllegalArgumentException("position_id is required");
        }
        UserPositionDO position = userPositionMapper.selectById(positionId);
        if (position == null) {
            throw new IllegalArgumentException("UserPosition not found: " + positionId);
        }
        return monitorActivePosition(position);
    }

    @Override
    public PositionMonitorBatchResultDTO monitorOpenUserPositions() {
        List<UserPositionDO> positions = Optional.ofNullable(userPositionMapper.listOpenPositions()).orElse(List.of());
        List<PositionMonitorResultDTO> results = new ArrayList<>();
        List<PositionMonitorBatchResultDTO.FailureItem> failures = new ArrayList<>();
        int blockedCount = 0;
        for (UserPositionDO position : positions) {
            try {
                PositionMonitorResultDTO result = monitorActivePosition(position);
                results.add(result);
                if (result.isRiskBlocked() || "HIGH_RISK".equals(result.getLogicStatus())) {
                    blockedCount++;
                }
            } catch (IllegalArgumentException | IllegalStateException ex) {
                failures.add(new PositionMonitorBatchResultDTO.FailureItem(
                        position == null ? null : position.getId(),
                        position == null ? null : position.getAssetSymbol(),
                        ex.getMessage()));
            }
        }
        PositionMonitorBatchResultDTO batch = new PositionMonitorBatchResultDTO();
        batch.setTotalCount(positions.size());
        batch.setSuccessCount(results.size());
        batch.setFailureCount(failures.size());
        batch.setBlockedCount(blockedCount);
        batch.setResults(results);
        batch.setFailures(failures);
        return batch;
    }

    private PositionMonitorResultDTO monitorActivePosition(UserPositionDO position) {
        validateActivePosition(position);
        String side = normalize(position.getSide());
        if (!"LONG".equals(side) && !"SHORT".equals(side)) {
            throw new IllegalArgumentException("UserPosition side must be LONG or SHORT");
        }
        String assetSymbol = requireText(position.getAssetSymbol(), "asset_symbol");
        BigDecimal currentPrice = readCurrentPrice(assetSymbol);
        UserPositionRiskResult risk = currentRiskOrBlocked();
        Set<String> reasons = new LinkedHashSet<>();
        if (risk.getReasonCodes().contains("RISK_CONTEXT_UNAVAILABLE")) {
            reasons.add("RISK_CONTEXT_UNAVAILABLE");
        }

        PlanContext planContext = resolvePlanContext(position, reasons);
        boolean riskBlocked = risk.isRiskBlocked();
        String riskLevel = riskBlocked ? "HIGH" : PositionMonitorPolicy.normalizeRiskLevel(risk.getRiskLevel());
        ExternalContextSnapshot externalContext = resolveExternalContext(assetSymbol, planContext.analysisId);
        boolean externalSourceBlocked = ExternalContextPolicy.SOURCE_HEALTH_BLOCKED.equalsIgnoreCase(
                externalContext.getSourceHealth());
        boolean externalBlocked = externalContext.isExternalContextBlocked() || externalSourceBlocked;
        boolean externalHighRisk = ExternalContextPolicy.RISK_HIGH.equalsIgnoreCase(externalContext.getRiskLevel())
                || externalBlocked;
        if (externalSourceBlocked) {
            reasons.add(ExternalContextPolicy.REASON_MISSING_SOURCE);
        }
        if (externalBlocked && !externalSourceBlocked) {
            reasons.add(ExternalContextPolicy.REASON_WINDOW_BLOCKED);
        }
        if (externalHighRisk) {
            riskLevel = "HIGH";
            reasons.add("EXTERNAL_CONTEXT_REVIEW_REQUIRED");
        }
        boolean riskIncreased = riskIncreased(position.getId(), riskLevel);
        if (riskBlocked) {
            reasons.add("RISK_BLOCKED");
        }
        if (riskIncreased) {
            reasons.add("RISK_LEVEL_INCREASED");
        }

        BigDecimal stopLoss = position.getStopLoss();
        BigDecimal takeProfit = position.getTakeProfit();
        boolean missingStopLoss = !positive(stopLoss);
        boolean missingTakeProfit = !positive(takeProfit);
        if (missingStopLoss) {
            reasons.add("STOP_LOSS_MISSING");
        }
        if (missingTakeProfit) {
            reasons.add("TAKE_PROFIT_MISSING");
        }

        boolean stopLossBreached = PositionMonitorPolicy.stopLossBreached(side, currentPrice, stopLoss);
        boolean nearStopLoss = PositionMonitorPolicy.nearStopLoss(side, currentPrice, stopLoss);
        boolean takeProfitReached = PositionMonitorPolicy.takeProfitReached(side, currentPrice, takeProfit);
        boolean nearTakeProfit = PositionMonitorPolicy.nearTakeProfit(side, currentPrice, takeProfit);
        if (stopLossBreached) {
            reasons.add("STOP_LOSS_BREACHED");
        }
        if (takeProfitReached) {
            reasons.add("TAKE_PROFIT_REACHED");
        }
        if (nearStopLoss) {
            reasons.add("NEAR_STOP_LOSS");
        }
        if (nearTakeProfit) {
            reasons.add("NEAR_TAKE_PROFIT");
        }

        String planStatus = normalize(planContext.plan == null ? null : planContext.plan.getExecutionPlanStatus());
        boolean planInvalidated = stopLossBreached
                || "INVALID".equals(planStatus)
                || "BLOCKED".equals(planStatus)
                || planContext.sourceGateBlockedOrIncomplete;
        boolean logicWeakened = planContext.missing
                || riskIncreased
                || nearStopLoss
                || "INCOMPLETE".equals(planStatus)
                || "REVIEW_ONLY".equals(planStatus)
                || missingStopLoss
                || missingTakeProfit
                || (externalHighRisk && !externalBlocked);

        String logicStatus;
        String suggestedAction;
        if (riskBlocked || externalBlocked) {
            logicStatus = "HIGH_RISK";
            suggestedAction = "RISK_REVIEW";
        } else if (planInvalidated) {
            logicStatus = "PLAN_INVALIDATED";
            suggestedAction = "RECHECK_PLAN";
        } else if (logicWeakened) {
            logicStatus = "LOGIC_WEAKENED";
            suggestedAction = "MANUAL_REVIEW";
        } else {
            logicStatus = "LOGIC_VALID";
            suggestedAction = nearTakeProfit ? "MANUAL_REVIEW" : "HOLD";
            reasons.add("LOGIC_VALID");
        }

        RecordPositionMonitorLogCommand command = new RecordPositionMonitorLogCommand();
        command.setPositionId(position.getId());
        command.setAnalysisId(planContext.analysisId);
        command.setExecutionPlanId(planContext.executionPlanId);
        command.setCurrentPrice(currentPrice);
        command.setLogicStatus(logicStatus);
        command.setRiskLevel(riskLevel);
        command.setSuggestedAction(suggestedAction);
        command.setReason(String.join(",", reasons));
        command.setEvidenceSnapshot(snapshotCount("evidence", planContext.analysisId));
        command.setScoreSnapshot(snapshotCount("score", planContext.analysisId));
        command.setDecisionSnapshot(decisionSnapshot(planContext.analysisId));
        command.setRiskSnapshot(riskSnapshot(risk, externalContext));
        command.setTraceId("POSITION_MONITOR_" + position.getId());
        PositionMonitorLogDTO log = positionMonitorLogService.recordMonitorRun(command);

        PositionMonitorResultDTO result = new PositionMonitorResultDTO();
        result.setPositionId(position.getId());
        result.setAssetSymbol(assetSymbol);
        result.setSide(side);
        result.setPositionStatus(normalize(position.getStatus()));
        result.setAnalysisId(planContext.analysisId);
        result.setExecutionPlanId(planContext.executionPlanId);
        result.setCurrentPrice(currentPrice);
        result.setEntryPrice(position.getEntryPrice());
        result.setStopLoss(stopLoss);
        result.setTakeProfit(takeProfit);
        result.setLogicStatus(logicStatus);
        result.setEntryLogicStatus(logicStatus);
        result.setDirectionSupportStatus(directionSupportStatus(logicStatus));
        result.setReversalStatus(reversalStatus(planInvalidated, stopLossBreached, externalBlocked, riskBlocked));
        result.setRiskLevel(riskLevel);
        result.setRiskBlocked(riskBlocked);
        result.setRiskIncreased(riskIncreased);
        result.setNearStopLoss(nearStopLoss);
        result.setNearTakeProfit(nearTakeProfit);
        result.setStopLossBreached(stopLossBreached);
        result.setTakeProfitReached(takeProfitReached);
        result.setSuggestedAction(suggestedAction);
        result.setSuggestedManualAction(suggestedAction);
        result.setSuggestedManualActionText(suggestedActionText(suggestedAction));
        applyPnl(result, side, position.getEntryPrice(), currentPrice, position.getQuantity(), position.getLeverage());
        result.setReasonCodes(new ArrayList<>(reasons));
        applyExternalContext(result, externalContext);
        result.setMonitorLogId(log.getLogId());
        result.setMonitoredAt(LocalDateTime.now());
        return result;
    }

    private void validateActivePosition(UserPositionDO position) {
        if (position == null || position.getId() == null || position.getId() <= 0) {
            throw new IllegalArgumentException("UserPosition not found");
        }
        String status = normalize(position.getStatus());
        if (!"OPEN".equals(status) && !"PARTIALLY_CLOSED".equals(status)) {
            throw new IllegalArgumentException("UserPosition status must be OPEN or PARTIALLY_CLOSED");
        }
    }

    private BigDecimal readCurrentPrice(String assetSymbol) {
        MarketQuoteSnapshot snapshot;
        try {
            snapshot = marketQuoteClient.fetch24hTicker(assetSymbol)
                    .orElseThrow(() -> new IllegalStateException("QUOTE_UNAVAILABLE"));
        } catch (RuntimeException ex) {
            throw new IllegalStateException("QUOTE_UNAVAILABLE", ex);
        }
        BigDecimal lastPrice = snapshot.getLastPrice();
        if (!positive(lastPrice)) {
            throw new IllegalStateException("INVALID_MARKET_PRICE");
        }
        return lastPrice;
    }

    private UserPositionRiskResult currentRiskOrBlocked() {
        try {
            UserPositionRiskResult result = userPositionRiskAdapter.currentRisk();
            return result == null ? UserPositionRiskResult.failClosed("RISK_CONTEXT_UNAVAILABLE") : result;
        } catch (RuntimeException ex) {
            return UserPositionRiskResult.failClosed("RISK_CONTEXT_UNAVAILABLE");
        }
    }

    private static void applyPnl(PositionMonitorResultDTO result, String side, BigDecimal entryPrice,
                                 BigDecimal currentPrice, BigDecimal quantity, BigDecimal leverage) {
        if (!positive(entryPrice) || !positive(currentPrice)) {
            return;
        }
        BigDecimal pct;
        if ("SHORT".equals(side)) {
            pct = entryPrice.subtract(currentPrice).divide(entryPrice, 8, RoundingMode.HALF_UP)
                    .multiply(new BigDecimal("100"));
        } else {
            pct = currentPrice.subtract(entryPrice).divide(entryPrice, 8, RoundingMode.HALF_UP)
                    .multiply(new BigDecimal("100"));
        }
        result.setPnlPct(pct);
        if (positive(quantity)) {
            BigDecimal unitPnl = "SHORT".equals(side)
                    ? entryPrice.subtract(currentPrice)
                    : currentPrice.subtract(entryPrice);
            result.setPnlAmount(unitPnl.multiply(quantity));
        }
        if (positive(leverage)) {
            result.setAccountImpactPct(pct.multiply(leverage));
        }
    }

    private static String directionSupportStatus(String logicStatus) {
        return switch (normalize(logicStatus)) {
            case "LOGIC_VALID" -> "SUPPORTED";
            case "LOGIC_WEAKENED" -> "WEAKENED";
            case "PLAN_INVALIDATED" -> "NOT_SUPPORTED";
            case "HIGH_RISK" -> "RISK_BLOCKED";
            default -> "WAITING_REVIEW";
        };
    }

    private static String reversalStatus(boolean planInvalidated, boolean stopLossBreached,
                                         boolean externalBlocked, boolean riskBlocked) {
        if (riskBlocked || externalBlocked) {
            return "RISK_REVIEW";
        }
        if (planInvalidated || stopLossBreached) {
            return "MANUAL_REVIEW_REQUIRED";
        }
        return "NO_REVERSAL_SIGNAL";
    }

    private static String suggestedActionText(String suggestedAction) {
        return switch (normalize(suggestedAction)) {
            case "HOLD" -> "继续人工观察";
            case "MANUAL_REVIEW" -> "人工复核";
            case "TIGHTEN_STOP_REVIEW" -> "复核是否收紧止损";
            case "REDUCE_POSITION_REVIEW" -> "复核是否降低仓位";
            case "RECHECK_PLAN" -> "复核执行计划";
            case "RISK_REVIEW" -> "风险复核";
            default -> "人工复核";
        };
    }

    private PlanContext resolvePlanContext(UserPositionDO position, Set<String> reasons) {
        String sourceRefId = optionalText(position.getSourceRefId());
        if (sourceRefId == null) {
            reasons.add("PLAN_CONTEXT_MISSING");
            return PlanContext.missing("USER_POSITION_" + position.getId());
        }
        ExecutionPlanDO plan = executionPlanMapper.selectByPlanId(sourceRefId);
        if (plan == null) {
            plan = executionPlanMapper.selectLatestByAnalysisId(sourceRefId);
        }
        if (plan == null) {
            reasons.add("PLAN_CONTEXT_MISSING");
            return PlanContext.missing(sourceRefId);
        }
        String analysisId = optionalText(plan.getAnalysisId());
        if (analysisId == null) {
            analysisId = sourceRefId;
        }
        boolean sourceGateBlocked = !Boolean.TRUE.equals(plan.getSourceGateComplete())
                || "BLOCKED".equals(normalize(plan.getSourceGateStatus()))
                || "INVALID".equals(normalize(plan.getSourceGateStatus()))
                || "INCOMPLETE".equals(normalize(plan.getSourceGateStatus()));
        if (sourceGateBlocked) {
            reasons.add("SOURCE_GATE_INCOMPLETE");
        }
        String status = normalize(plan.getExecutionPlanStatus());
        if ("INVALID".equals(status) || "BLOCKED".equals(status)) {
            reasons.add("PLAN_INVALID");
        } else if ("INCOMPLETE".equals(status) || "REVIEW_ONLY".equals(status)) {
            reasons.add("PLAN_REVIEW_REQUIRED");
        }
        return new PlanContext(plan, analysisId, optionalText(plan.getPlanId()), false, sourceGateBlocked);
    }

    private boolean riskIncreased(Long positionId, String currentRiskLevel) {
        List<PositionMonitorLogDTO> logs = positionMonitorLogService.listByPositionId(positionId, 1);
        if (logs == null || logs.isEmpty()) {
            return false;
        }
        String previous = logs.get(0).getRiskLevel();
        return PositionMonitorPolicy.riskRank(currentRiskLevel) > PositionMonitorPolicy.riskRank(previous);
    }

    private String snapshotCount(String type, String analysisId) {
        if (optionalText(analysisId) == null) {
            return null;
        }
        int count = 0;
        if ("evidence".equals(type)) {
            count = Optional.ofNullable(evidenceItemMapper.selectTop3BriefByAnalysisId(analysisId)).orElse(List.of()).size();
        } else if ("score".equals(type)) {
            count = Optional.ofNullable(scoreItemMapper.selectTop3BriefByAnalysisId(analysisId)).orElse(List.of()).size();
        }
        if (count == 0) {
            return null;
        }
        return json(Map.of("analysisId", analysisId, "snapshotType", type, "itemCount", count));
    }

    private String decisionSnapshot(String analysisId) {
        if (optionalText(analysisId) == null || decisionResultMapper.selectLatestByAnalysisId(analysisId) == null) {
            return null;
        }
        Map<String, Object> safe = new LinkedHashMap<>();
        safe.put("analysisId", analysisId);
        safe.put("snapshotType", "decision");
        safe.put("present", true);
        return json(safe);
    }

    private ExternalContextSnapshot resolveExternalContext(String assetSymbol, String analysisId) {
        if (externalContextEvidenceBuilder == null) {
            return new ExternalContextSnapshot();
        }
        try {
            return externalContextEvidenceBuilder.buildSnapshot(analysisId, assetSymbol, null, LocalDateTime.now(), null);
        } catch (RuntimeException ignored) {
            ExternalContextSnapshot snapshot = new ExternalContextSnapshot();
            snapshot.setSourceHealth(ExternalContextPolicy.SOURCE_HEALTH_BLOCKED);
            snapshot.setExternalContextBlocked(true);
            snapshot.setRiskLevel(ExternalContextPolicy.RISK_HIGH);
            snapshot.addReason(ExternalContextPolicy.REASON_MISSING_SOURCE);
            return snapshot;
        }
    }

    private static void applyExternalContext(PositionMonitorResultDTO result, ExternalContextSnapshot snapshot) {
        result.setExternalContextStatus(snapshot.getStatus());
        result.setActiveExternalEventCount(snapshot.getActiveExternalEventCount());
        result.setActiveMacroEventCount(snapshot.getActiveMacroEventCount());
        result.setActiveNewsEventCount(snapshot.getActiveNewsEventCount());
        result.setExternalContextRiskLevel(snapshot.getRiskLevel());
        result.setExternalContextBlocked(snapshot.isExternalContextBlocked());
        result.setExternalEventIds(snapshot.getExternalEventIds());
        result.setExternalContextReasonCodes(snapshot.getReasonCodes());
        result.setNextExternalEventTime(snapshot.getNextExternalEventTime());
        result.setExternalContextSourceHealth(snapshot.getSourceHealth());
    }

    private String riskSnapshot(UserPositionRiskResult risk, ExternalContextSnapshot externalContext) {
        Map<String, Object> safe = new LinkedHashMap<>();
        safe.put("riskLevel", PositionMonitorPolicy.normalizeRiskLevel(risk.getRiskLevel()));
        safe.put("riskBlocked", risk.isRiskBlocked());
        safe.put("aggregateRiskScore", risk.getAggregateRiskScore());
        if (externalContext != null) {
            Map<String, Object> external = new LinkedHashMap<>();
            external.put("status", externalContext.getStatus());
            external.put("sourceHealth", externalContext.getSourceHealth());
            external.put("riskLevel", externalContext.getRiskLevel());
            external.put("blocked", externalContext.isExternalContextBlocked());
            external.put("activeCount", externalContext.getActiveExternalEventCount());
            external.put("macroCount", externalContext.getActiveMacroEventCount());
            external.put("newsCount", externalContext.getActiveNewsEventCount());
            external.put("reasonCodes", externalContext.getReasonCodes());
            external.put("eventIds", externalContext.getExternalEventIds());
            safe.put("externalContext", external);
        }
        return json(safe);
    }

    private String json(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("MONITOR_SNAPSHOT_SERIALIZATION_FAILED", e);
        }
    }

    private static boolean positive(BigDecimal value) {
        return value != null && value.compareTo(BigDecimal.ZERO) > 0;
    }

    private static String requireText(String value, String fieldName) {
        String text = optionalText(value);
        if (text == null) {
            throw new IllegalArgumentException(fieldName + " is required");
        }
        return text;
    }

    private static String optionalText(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private static String normalize(String value) {
        return value == null ? null : value.trim().toUpperCase(Locale.ROOT);
    }

    private static class PlanContext {
        private final ExecutionPlanDO plan;
        private final String analysisId;
        private final String executionPlanId;
        private final boolean missing;
        private final boolean sourceGateBlockedOrIncomplete;

        private PlanContext(ExecutionPlanDO plan,
                            String analysisId,
                            String executionPlanId,
                            boolean missing,
                            boolean sourceGateBlockedOrIncomplete) {
            this.plan = plan;
            this.analysisId = analysisId;
            this.executionPlanId = executionPlanId;
            this.missing = missing;
            this.sourceGateBlockedOrIncomplete = sourceGateBlockedOrIncomplete;
        }

        private static PlanContext missing(String analysisId) {
            return new PlanContext(null, analysisId, null, true, false);
        }
    }
}
