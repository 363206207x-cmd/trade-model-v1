package org.example.trademodel.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.trademodel.analysisrun.AnalysisTimePolicy;
import org.example.trademodel.ai.AiRoleResultsCodec;
import org.example.trademodel.ai.AiRoleResultsPayload;
import org.example.trademodel.entity.MonitorAlertDO;
import org.example.trademodel.entity.TmPushRecheckLogDO;
import org.example.trademodel.entity.TmPushSnapshotDO;
import org.example.trademodel.providercall.AssetPriority;
import org.example.trademodel.providercall.ProviderCallResult;
import org.example.trademodel.providercall.snapshot.MarketPriceSnapshot;
import org.example.trademodel.providercall.snapshot.MarketPriceSnapshotPolicy;
import org.example.trademodel.providercall.snapshot.MarketPriceSnapshotService;
import org.example.trademodel.mapper.PushRecheckLogMapper;
import org.example.trademodel.mapper.PushSnapshotMapper;
import org.example.trademodel.service.DashboardHomeService;
import org.example.trademodel.service.DecisionService;
import org.example.trademodel.service.MonitorService;
import org.example.trademodel.service.PositionMonitorLogService;
import org.example.trademodel.service.PositionSyncService;
import org.example.trademodel.service.PushRecheckStatusContract;
import org.example.trademodel.service.UserPositionService;
import org.example.trademodel.service.readiness.ProviderReadinessService;
import org.example.trademodel.positionmonitorlog.PositionMonitorLogDTO;
import org.example.trademodel.service.support.ExternalContextEvidenceBuilder;
import org.example.trademodel.service.support.ExternalContextSnapshot;
import org.example.trademodel.vo.DashboardHomeVO;
import org.example.trademodel.vo.DecisionResultVO;
import org.example.trademodel.vo.LightSystemStatusVO;
import org.example.trademodel.vo.PositionSyncStatusVO;
import org.example.trademodel.vo.ProviderReadinessVO;
import org.example.trademodel.vo.UserPositionVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

@Service
public class DashboardHomeServiceImpl implements DashboardHomeService {
    private static final int DEFAULT_LIMIT = 6;
    private static final int MAX_LIMIT = 12;
    private static final List<String> DEFAULT_SYMBOLS = List.of(
            "BTCUSDT", "ETHUSDT", "SOLUSDT", "BNBUSDT", "XRPUSDT", "DOGEUSDT"
    );
    private static final List<String> AI_ROLES = List.of("GPT_FINAL", "GEMINI_REVIEW", "GROK_CHALLENGE");
    private static final List<String> EXECUTABLE_PUSH_STATUSES = List.of(
            PushRecheckStatusContract.PUSH_STATUS_REVIEW_PASSED,
            "RECHECK_VALID_EXECUTABLE"
    );
    private static final List<String> INVALIDATED_PUSH_STATUSES = List.of(
            PushRecheckStatusContract.PUSH_STATUS_DRIFTED_FROM_ENTRY_ZONE,
            PushRecheckStatusContract.PUSH_STATUS_INVALIDATED,
            PushRecheckStatusContract.PUSH_STATUS_RISK_BLOCKED,
            PushRecheckStatusContract.PUSH_STATUS_CONFUSED_BLOCKED,
            PushRecheckStatusContract.PUSH_STATUS_EXPIRED,
            "RECHECK_DRIFTED",
            "DRIFTED",
            "INVALIDATED",
            "RISK_BLOCKED",
            "CONFUSED_BLOCKED",
            "EXPIRED"
    );
    private static final String BOUNDARY_INCOMPLETE_VALID_PERIOD = "边界不足，等待结构确认";

    private final DecisionService decisionService;
    private final MonitorService monitorService;
    private final UserPositionService userPositionService;
    private final PositionMonitorLogService positionMonitorLogService;
    private final PositionSyncService positionSyncService;
    private final PushSnapshotMapper pushSnapshotMapper;
    private final PushRecheckLogMapper pushRecheckLogMapper;
    private final ExternalContextEvidenceBuilder externalContextEvidenceBuilder;
    private final ProviderReadinessService providerReadinessService;
    private final ObjectMapper objectMapper;
    private final AiRoleResultsCodec aiRoleResultsCodec;
    private final MarketPriceSnapshotService marketPriceSnapshotService;

    public DashboardHomeServiceImpl(DecisionService decisionService,
                                    MonitorService monitorService,
                                    UserPositionService userPositionService,
                                    PositionMonitorLogService positionMonitorLogService,
                                    PositionSyncService positionSyncService,
                                    PushSnapshotMapper pushSnapshotMapper,
                                    PushRecheckLogMapper pushRecheckLogMapper,
                                    ExternalContextEvidenceBuilder externalContextEvidenceBuilder,
                                    ProviderReadinessService providerReadinessService,
                                    ObjectMapper objectMapper) {
        this(decisionService, monitorService, userPositionService, positionMonitorLogService, positionSyncService,
                pushSnapshotMapper, pushRecheckLogMapper, externalContextEvidenceBuilder, providerReadinessService,
                objectMapper, null);
    }

    @Autowired
    public DashboardHomeServiceImpl(DecisionService decisionService,
                                    MonitorService monitorService,
                                    UserPositionService userPositionService,
                                    PositionMonitorLogService positionMonitorLogService,
                                    PositionSyncService positionSyncService,
                                    PushSnapshotMapper pushSnapshotMapper,
                                    PushRecheckLogMapper pushRecheckLogMapper,
                                    ExternalContextEvidenceBuilder externalContextEvidenceBuilder,
                                    ProviderReadinessService providerReadinessService,
                                    ObjectMapper objectMapper,
                                    MarketPriceSnapshotService marketPriceSnapshotService) {
        this.decisionService = decisionService;
        this.monitorService = monitorService;
        this.userPositionService = userPositionService;
        this.positionMonitorLogService = positionMonitorLogService;
        this.positionSyncService = positionSyncService;
        this.pushSnapshotMapper = pushSnapshotMapper;
        this.pushRecheckLogMapper = pushRecheckLogMapper;
        this.externalContextEvidenceBuilder = externalContextEvidenceBuilder;
        this.providerReadinessService = providerReadinessService;
        this.objectMapper = objectMapper;
        this.aiRoleResultsCodec = new AiRoleResultsCodec(objectMapper);
        this.marketPriceSnapshotService = marketPriceSnapshotService;
    }

    @Override
    public DashboardHomeVO getHome(String selectedSymbol, Integer limit) {
        int effectiveLimit = normalizeLimit(limit);
        LightSystemStatusVO systemStatus = safeSystemStatus();
        List<DecisionResultVO> decisions = safeDecisions(Math.max(effectiveLimit, DEFAULT_LIMIT));
        List<MonitorAlertDO> alerts = safeAlerts();
        List<UserPositionVO> positions = safePositions();
        PositionSyncStatusVO positionSyncStatus = safePositionSyncStatus();
        ProviderReadinessVO providerReadiness = safeProviderReadiness();

        String normalizedSelected = normalizeSymbol(selectedSymbol);
        if (normalizedSelected == null) {
            normalizedSelected = firstDecisionSymbol(decisions);
        }
        if (normalizedSelected == null) {
            normalizedSelected = DEFAULT_SYMBOLS.get(0);
        }

        DecisionResultVO selectedDecision = findDecision(decisions, normalizedSelected);
        if (selectedDecision == null) {
            selectedDecision = safeDecisionBySymbol(normalizedSelected);
        }

        ExternalContextSnapshot externalContext = safeExternalContext(normalizedSelected, selectedDecision);
        PushInboxContext pushInboxContext = buildPushInbox(positions, effectiveLimit);

        DashboardHomeVO home = new DashboardHomeVO();
        home.setHeader(buildHeader(systemStatus, positionSyncStatus, externalContext, providerReadiness));
        home.setSystemState(buildSystemState(systemStatus, decisions, selectedDecision));
        home.setAlerts(buildAlerts(alerts));
        home.setEvents(buildEvents(externalContext));
        home.setAssets(buildAssets(decisions, effectiveLimit));
        home.setPositions(buildPositions(positions));
        home.setSelectedSymbol(normalizedSelected);
        home.setExecutionSuggestion(buildExecutionSuggestion(selectedDecision));
        home.setAiDecision(buildAiDecision(selectedDecision));
        home.setPushInbox(pushInboxContext.pushInbox());
        home.setDiagnostics(buildDiagnostics(systemStatus, decisions, selectedDecision, positionSyncStatus,
                pushInboxContext, providerReadiness));
        home.setSafety(new DashboardHomeVO.SafetyVO());
        return home;
    }

    private DashboardHomeVO.HeaderVO buildHeader(LightSystemStatusVO systemStatus,
                                                 PositionSyncStatusVO positionSyncStatus,
                                                 ExternalContextSnapshot externalContext,
                                                 ProviderReadinessVO providerReadiness) {
        DashboardHomeVO.HeaderVO header = new DashboardHomeVO.HeaderVO();
        header.setPageTitle("首页总览");
        header.setDataStatus(firstNonBlank(systemStatus != null ? systemStatus.getStatus() : null, "WAITING_SYNC"));
        header.setAiStatus(firstNonBlank(providerReadiness != null ? providerReadiness.getAiProviderStatus() : null,
                "WAITING_SYNC"));
        header.setDataSourceText(dataSourceText(positionSyncStatus, externalContext, providerReadiness));
        header.setUpdatedAt(LocalDateTime.now());
        return header;
    }

    private DashboardHomeVO.SystemStateVO buildSystemState(LightSystemStatusVO systemStatus,
                                                           List<DecisionResultVO> decisions,
                                                           DecisionResultVO selectedDecision) {
        DashboardHomeVO.SystemStateVO state = new DashboardHomeVO.SystemStateVO();
        DecisionResultVO trendDecision = selectedDecision != null ? selectedDecision : firstDecision(decisions);
        state.setMarketTrend(card(
                "marketTrend",
                "市场趋势",
                trendDecision != null ? trendDecision.getMarketBiasHierarchy() : null,
                biasLabel(trendDecision != null ? trendDecision.getMarketBiasHierarchy() : null),
                "决策摘要",
                statusForText(trendDecision != null ? trendDecision.getMarketBiasHierarchy() : null),
                null
        ));
        String highestRisk = riskLevelFrom(decisions);
        state.setRiskLevel(card(
                "riskLevel",
                "风险等级",
                highestRisk,
                riskLabel(highestRisk),
                "决策风险",
                statusForText(highestRisk),
                null
        ));
        Integer averageDataQuality = averageDataQuality(decisions);
        state.setDataQuality(card(
                "dataQuality",
                "数据质量分",
                averageDataQuality,
                averageDataQuality != null ? String.valueOf(averageDataQuality) : null,
                "摘要均值",
                averageDataQuality != null ? "CONNECTED" : "WAITING_SYNC",
                averageDataQuality
        ));
        AiConflictSummary conflict = aiConflictSummary(decisions);
        state.setAiConflict(card(
                "aiConflict",
                "AI 冲突等级",
                conflict.level(),
                conflict.level(),
                "AI 冲突",
                conflict.level() != null || conflict.score() != null ? "CONNECTED" : "WAITING_SYNC",
                conflict.score()
        ));
        Integer pendingCount = systemStatus != null ? systemStatus.getPendingCount() : null;
        state.setPendingReview(card(
                "pendingReview",
                "待复核机会",
                pendingCount,
                pendingCount != null && pendingCount > 0 ? String.valueOf(pendingCount) : "暂无",
                "pendingCount",
                pendingCount != null ? "CONNECTED" : "WAITING_SYNC",
                pendingCount
        ));
        Integer confusedCount = confusedCount(systemStatus, decisions);
        state.setConfused(card(
                "confused",
                "Confused",
                confusedCount,
                confusedCount != null ? String.valueOf(confusedCount) : null,
                "当前积压",
                confusedCount != null ? "CONNECTED" : "WAITING_SYNC",
                confusedCount
        ));
        Boolean hotResetFired = systemStatus != null ? systemStatus.getHotResetFired() : null;
        Map<String, Object> hotMeta = new LinkedHashMap<>();
        if (systemStatus != null) {
            hotMeta.put("symbol", systemStatus.getHotResetSymbol());
            hotMeta.put("triggerType", systemStatus.getHotResetTriggerType());
            hotMeta.put("triggerValue", systemStatus.getHotResetTriggerValue());
            hotMeta.put("time", systemStatus.getHotResetTime());
        }
        DashboardHomeVO.StatusCardVO hotReset = card(
                "hotReset",
                "Hot Reset",
                Boolean.TRUE.equals(hotResetFired),
                Boolean.TRUE.equals(hotResetFired) ? "已触发" : "未触发",
                Boolean.TRUE.equals(hotResetFired) ? "最近一次" : "暂无",
                hotResetFired != null ? "CONNECTED" : "WAITING_SYNC",
                null
        );
        hotReset.setMeta(hotMeta);
        state.setHotReset(hotReset);
        return state;
    }

    private List<DashboardHomeVO.AlertRowVO> buildAlerts(List<MonitorAlertDO> alerts) {
        List<DashboardHomeVO.AlertRowVO> rows = new ArrayList<>();
        for (MonitorAlertDO alert : alerts == null ? List.<MonitorAlertDO>of() : alerts) {
            if (rows.size() >= 2) {
                break;
            }
            DashboardHomeVO.AlertRowVO row = new DashboardHomeVO.AlertRowVO();
            row.setLevel(trimToNull(alert.getAlertLevel()));
            row.setMessage(trimToNull(alert.getAlertMessage()));
            row.setSymbol(toDisplaySymbol(alert.getAssetSymbol()));
            row.setTime(trimToNull(alert.getCreatedAt()));
            rows.add(row);
        }
        return rows;
    }

    private List<DashboardHomeVO.EventRowVO> buildEvents(ExternalContextSnapshot externalContext) {
        if (externalContext == null || !hasText(externalContext.getLatestExternalEventLabel())) {
            return List.of();
        }
        DashboardHomeVO.EventRowVO row = new DashboardHomeVO.EventRowVO();
        row.setType("EXTERNAL_CONTEXT");
        row.setLabel(externalContext.getLatestExternalEventLabel());
        row.setImpactLevel(externalContext.getRiskLevel());
        row.setTimeWindow(externalContext.getEventWindowStart() != null || externalContext.getEventWindowEnd() != null
                ? String.valueOf(externalContext.getEventWindowStart()) + " ~ " + externalContext.getEventWindowEnd()
                : externalContext.getLatestExternalEventTime());
        return List.of(row);
    }

    private List<DashboardHomeVO.AssetVO> buildAssets(List<DecisionResultVO> decisions, int limit) {
        List<DashboardHomeVO.AssetVO> assets = new ArrayList<>();
        LinkedHashSet<String> used = new LinkedHashSet<>();
        for (DecisionResultVO decision : decisions == null ? List.<DecisionResultVO>of() : decisions) {
            if (assets.size() >= limit) {
                break;
            }
            String symbol = normalizeSymbol(decision.getSymbol());
            if (symbol == null || !used.add(symbol)) {
                continue;
            }
            assets.add(assetFromDecision(assets.size() + 1, decision));
        }
        for (String symbol : DEFAULT_SYMBOLS) {
            if (assets.size() >= limit) {
                break;
            }
            if (!used.add(symbol)) {
                continue;
            }
            assets.add(assetPlaceholder(assets.size() + 1, symbol));
        }
        return assets;
    }

    private DashboardHomeVO.AssetVO assetFromDecision(int slot, DecisionResultVO decision) {
        DashboardHomeVO.AssetVO asset = assetBase(slot, normalizeSymbol(decision.getSymbol()));
        asset.setSlotType("DECISION");
        asset.setMarketBias(trimToNull(decision.getMarketBiasHierarchy()));
        asset.setMarketBiasLabel(biasLabel(decision.getMarketBiasHierarchy()));
        asset.setCompositeScore(null);
        asset.setConfidenceLevel(trimToNull(decision.getConfidenceLevel()));
        asset.setConfidenceLabel(confidenceLabel(decision.getConfidenceLevel()));
        asset.setRiskLevel(trimToNull(decision.getRiskLevel()));
        asset.setRiskLabel(riskLabel(decision.getRiskLevel()));
        String assetState = recognizedAssetStateFromSnapshot(decision.getAssetStateSnapshot());
        asset.setAssetState(assetState);
        asset.setAssetStateLabel(assetStateLabel(assetState));
        asset.setWorthOpening(decision.getIsWorthOpening());
        return asset;
    }

    private DashboardHomeVO.AssetVO assetPlaceholder(int slot, String symbol) {
        DashboardHomeVO.AssetVO asset = assetBase(slot, symbol);
        asset.setSlotType("DEFAULT_SLOT");
        return asset;
    }

    private DashboardHomeVO.AssetVO assetBase(int slot, String normalizedSymbol) {
        DashboardHomeVO.AssetVO asset = new DashboardHomeVO.AssetVO();
        asset.setSlot(slot);
        asset.setRawSymbol(normalizedSymbol);
        asset.setSymbol(toDisplaySymbol(normalizedSymbol));
        return asset;
    }

    private List<DashboardHomeVO.PositionVO> buildPositions(List<UserPositionVO> positions) {
        List<DashboardHomeVO.PositionVO> rows = new ArrayList<>();
        for (UserPositionVO position : positions == null ? List.<UserPositionVO>of() : positions) {
            if (!isActiveManualPosition(position)) {
                continue;
            }
            PositionMonitorLogDTO latestMonitorLog = latestPositionMonitorLog(position.getId());
            DashboardHomeVO.PositionVO row = new DashboardHomeVO.PositionVO();
            row.setPositionId(position.getId());
            row.setSymbol(toDisplaySymbol(position.getAssetSymbol()));
            row.setDirection(trimToNull(position.getSide()));
            row.setEntryPrice(position.getEntryPrice());
            BigDecimal currentPrice = latestMonitorLog != null && positive(latestMonitorLog.getCurrentPrice())
                    ? latestMonitorLog.getCurrentPrice()
                    : safeCurrentPrice(position.getAssetSymbol());
            row.setCurrentPrice(currentPrice);
            applyPositionPnl(row, position, currentPrice);
            row.setLeverage(position.getLeverage());
            row.setPositionSize(position.getQuantity());
            row.setPositionStatus(trimToNull(position.getStatus()));
            row.setMonitorConclusion(latestMonitorLog != null ? trimToNull(latestMonitorLog.getLogicStatus()) : null);
            row.setEntryLogicStatus(latestMonitorLog != null ? trimToNull(latestMonitorLog.getLogicStatus()) : "WAITING_MONITOR");
            row.setDirectionSupportStatus(directionSupportStatus(latestMonitorLog));
            row.setReversalStatus(reversalStatus(latestMonitorLog));
            row.setRiskLevel(latestMonitorLog != null ? trimToNull(latestMonitorLog.getRiskLevel()) : "WAITING_SYNC");
            row.setSuggestedManualAction(latestMonitorLog != null ? trimToNull(latestMonitorLog.getSuggestedAction()) : "MANUAL_REVIEW");
            row.setSuggestedManualActionText(suggestedActionText(row.getSuggestedManualAction(), latestMonitorLog));
            row.setUpdatedAt(position.getUpdatedAt());
            rows.add(row);
        }
        return rows;
    }

    private boolean isManualPosition(UserPositionVO position) {
        return position != null && "MANUAL".equalsIgnoreCase(trimToNull(position.getSourceType()));
    }

    private boolean isActiveManualPosition(UserPositionVO position) {
        return isManualPosition(position) && isOpenPositionStatus(position.getStatus());
    }

    private boolean isOpenPositionStatus(String status) {
        String normalized = trimToNull(status);
        return "OPEN".equalsIgnoreCase(normalized) || "PARTIALLY_CLOSED".equalsIgnoreCase(normalized);
    }

    private PositionMonitorLogDTO latestPositionMonitorLog(Long positionId) {
        if (positionId == null) {
            return null;
        }
        try {
            List<PositionMonitorLogDTO> logs = positionMonitorLogService.listByPositionId(positionId, 1);
            if (logs == null || logs.isEmpty()) {
                return null;
            }
            return logs.get(0);
        } catch (RuntimeException ignored) {
            return null;
        }
    }

    private BigDecimal safeCurrentPrice(String assetSymbol) {
        String symbol = trimToNull(assetSymbol);
        if (symbol == null || marketPriceSnapshotService == null) {
            return null;
        }
        try {
            ProviderCallResult<MarketPriceSnapshot> result = marketPriceSnapshotService.peek(symbol,
                    AssetPriority.P0_POSITION, Duration.ofSeconds(15), "dashboard-home-" + UUID.randomUUID());
            return MarketPriceSnapshotPolicy.isFresh(result) ? result.payload().lastPrice() : null;
        } catch (RuntimeException ignored) {
            return null;
        }
    }

    private void applyPositionPnl(DashboardHomeVO.PositionVO row, UserPositionVO position, BigDecimal currentPrice) {
        if (row == null || position == null || !positive(position.getEntryPrice()) || !positive(currentPrice)) {
            return;
        }
        String side = trimToNull(position.getSide());
        BigDecimal pnlPct = "SHORT".equalsIgnoreCase(side)
                ? position.getEntryPrice().subtract(currentPrice).divide(position.getEntryPrice(), 8, RoundingMode.HALF_UP)
                        .multiply(new BigDecimal("100"))
                : currentPrice.subtract(position.getEntryPrice()).divide(position.getEntryPrice(), 8, RoundingMode.HALF_UP)
                        .multiply(new BigDecimal("100"));
        row.setPnlPct(pnlPct);
        if (positive(position.getQuantity())) {
            BigDecimal unitPnl = "SHORT".equalsIgnoreCase(side)
                    ? position.getEntryPrice().subtract(currentPrice)
                    : currentPrice.subtract(position.getEntryPrice());
            row.setFloatingPnl(unitPnl.multiply(position.getQuantity()));
        }
        if (positive(position.getLeverage())) {
            row.setAccountImpactPct(pnlPct.multiply(position.getLeverage()));
        }
    }

    private String directionSupportStatus(PositionMonitorLogDTO latestMonitorLog) {
        String logic = latestMonitorLog != null ? trimToNull(latestMonitorLog.getLogicStatus()) : null;
        return switch (logic == null ? "" : logic.toUpperCase(Locale.ROOT)) {
            case "LOGIC_VALID" -> "SUPPORTED";
            case "LOGIC_WEAKENED" -> "WEAKENED";
            case "PLAN_INVALIDATED" -> "NOT_SUPPORTED";
            case "HIGH_RISK" -> "RISK_BLOCKED";
            default -> "WAITING_SYNC";
        };
    }

    private String reversalStatus(PositionMonitorLogDTO latestMonitorLog) {
        String logic = latestMonitorLog != null ? trimToNull(latestMonitorLog.getLogicStatus()) : null;
        if ("PLAN_INVALIDATED".equalsIgnoreCase(logic)) {
            return "MANUAL_REVIEW_REQUIRED";
        }
        if ("HIGH_RISK".equalsIgnoreCase(logic)) {
            return "RISK_REVIEW";
        }
        return latestMonitorLog == null ? "WAITING_MONITOR" : "NO_REVERSAL_SIGNAL";
    }

    private String suggestedActionText(String suggestedAction, PositionMonitorLogDTO latestMonitorLog) {
        if (latestMonitorLog == null) {
            return "等待监控";
        }
        return switch (suggestedAction == null ? "" : suggestedAction.toUpperCase(Locale.ROOT)) {
            case "HOLD" -> "人工继续观察";
            case "MANUAL_REVIEW" -> "人工复核";
            case "TIGHTEN_STOP_REVIEW" -> "复核是否收紧止损";
            case "REDUCE_POSITION_REVIEW" -> "复核是否降低仓位";
            case "RECHECK_PLAN" -> "复核执行计划";
            case "RISK_REVIEW" -> "风险复核";
            default -> "人工复核";
        };
    }

    private DashboardHomeVO.ExecutionSuggestionVO buildExecutionSuggestion(DecisionResultVO decision) {
        DashboardHomeVO.ExecutionSuggestionVO suggestion = new DashboardHomeVO.ExecutionSuggestionVO();
        if (decision == null) {
            return suggestion;
        }
        suggestion.setDirection(trimToNull(decision.getMarketBiasHierarchy()));
        String entryZone = trimPlanValue(decision.getEntryZone());
        String stopLoss = trimPlanValue(decision.getStopLoss());
        String takeProfitRules = trimPlanValue(decision.getTakeProfitRules());
        boolean boundaryComplete = entryZone != null && stopLoss != null && takeProfitRules != null;

        if (!AnalysisTimePolicy.isExecutionPlanPrimaryTimeframe(decision.getTimeframe())) {
            suggestion.setValidPeriod(AnalysisTimePolicy.unsupportedExecutionPlanTimeframeMessage());
            return suggestion;
        }

        suggestion.setEntryZone(entryZone);
        suggestion.setStopLoss(stopLoss);
        suggestion.setTakeProfitRules(takeProfitRules);
        suggestion.setLeverageSuggestion(trimToNull(decision.getLeverageSuggestion()));
        suggestion.setPositionSuggestion(trimToNull(decision.getPositionSuggestion()));
        suggestion.setValidPeriod(boundaryComplete ? trimToNull(decision.getValidPeriod()) : BOUNDARY_INCOMPLETE_VALID_PERIOD);
        suggestion.setInvalidCondition(boundaryComplete ? trimToNull(decision.getInvalidCondition()) : null);
        return suggestion;
    }

    private DashboardHomeVO.AiDecisionVO buildAiDecision(DecisionResultVO decision) {
        DashboardHomeVO.AiDecisionVO ai = new DashboardHomeVO.AiDecisionVO();
        ai.setActiveTab("GPT_FINAL");
        AiRoleResultsCodec.ParseResult parsed = aiRoleResultsCodec.parse(
                decision != null ? decision.getAiRoleResults() : null);
        AiRoleResultsPayload payload = parsed.current() ? parsed.payload() : null;
        AiRoleResultsPayload.SynthesisPayload synthesis = payload != null ? payload.synthesis() : null;
        ai.setSchemaVersion(payload != null ? payload.schemaVersion() : null);
        List<DashboardHomeVO.AiTabVO> tabs = new ArrayList<>();
        for (String role : AI_ROLES) {
            AiRoleResultsPayload.RolePayload rolePayload = payload != null ? payload.roles().get(role) : null;
            tabs.add(buildAiTab(role, rolePayload, synthesis));
        }
        ai.setTabs(tabs);
        DashboardHomeVO.ConsistencyVO consistency = new DashboardHomeVO.ConsistencyVO();
        consistency.setLevel(decision != null ? trimToNull(decision.getAiConflictLevel()) : null);
        consistency.setScore(decision != null ? decision.getAiConflictScore() : null);
        consistency.setConfused(decision != null && decision.getConfusedScore() != null && decision.getConfusedScore() > 0);
        consistency.setConsistencyScore(null);
        consistency.setConsistencyLevel(null);
        consistency.setConsistencySummary(null);
        consistency.setDowngradeReason(synthesis != null ? trimToNull(synthesis.downgradeReason()) : null);
        ai.setConsistency(consistency);
        return ai;
    }

    private DashboardHomeVO.AiTabVO buildAiTab(String role,
                                               AiRoleResultsPayload.RolePayload rolePayload,
                                               AiRoleResultsPayload.SynthesisPayload synthesis) {
        DashboardHomeVO.AiTabVO tab = new DashboardHomeVO.AiTabVO();
        tab.setRole(role);
        tab.setRoleLabel(roleLabel(role));
        if (rolePayload == null) {
            return tab;
        }
        switch (role) {
            case "GPT_FINAL" -> populateFinalDecisionRole(tab, rolePayload, synthesis);
            case "GEMINI_REVIEW" -> populateConflictReviewRole(tab, rolePayload);
            case "GROK_CHALLENGE" -> populateChallengeRole(tab, rolePayload);
            default -> {
            }
        }
        return tab;
    }

    private void populateFinalDecisionRole(DashboardHomeVO.AiTabVO tab,
                                           AiRoleResultsPayload.RolePayload role,
                                           AiRoleResultsPayload.SynthesisPayload synthesis) {
        String finalMarketBias = synthesis != null ? trimToNull(synthesis.finalMarketBias()) : null;
        String finalConfidence = synthesis != null ? trimToNull(synthesis.finalConfidence()) : null;
        String finalRiskLevel = synthesis != null ? trimToNull(synthesis.finalRiskLevel()) : null;
        String finalPlanMode = synthesis != null ? trimToNull(synthesis.planModeAdjustment()) : null;
        String worthOpening = synthesis != null ? worthOpeningLabel(synthesis.worthOpening()) : null;
        String finalConclusion = trimToNull(role.summary());
        List<String> coreSupportingEvidence = "SUPPORT".equals(role.stance())
                ? role.reasonCodes()
                : List.of();
        List<String> coreCounterEvidence = "CHALLENGE".equals(role.stance())
                ? role.reasonCodes()
                : List.of();
        String decisionSummary = trimToNull(role.summary());
        String downgradeReason = synthesis != null ? trimToNull(synthesis.downgradeReason()) : null;

        tab.setFinalMarketBias(finalMarketBias);
        tab.setFinalConfidence(finalConfidence);
        tab.setFinalRiskLevel(finalRiskLevel);
        tab.setFinalPlanMode(finalPlanMode);
        tab.setWorthOpening(worthOpening);
        tab.setFinalConclusion(finalConclusion);
        tab.setCoreSupportingEvidence(coreSupportingEvidence);
        tab.setCoreCounterEvidence(coreCounterEvidence);
        tab.setDecisionSummary(decisionSummary);
        tab.setDowngradeReason(downgradeReason);

        tab.setDirection(finalMarketBias);
        tab.setConfidenceLevel(finalConfidence);
        tab.setSupportEvidence(coreSupportingEvidence);
        tab.setAgainstEvidence(coreCounterEvidence);
        tab.setReviewConclusion(firstNonBlank(finalConclusion, decisionSummary));
    }

    private void populateConflictReviewRole(DashboardHomeVO.AiTabVO tab,
                                            AiRoleResultsPayload.RolePayload role) {
        tab.setReviewVerdict(trimToNull(role.stance()));
        tab.setDetectedContradictions("CHALLENGE".equals(role.stance())
                ? role.reasonCodes()
                : List.of());
        tab.setWeakEvidence(List.of());
        tab.setLogicGaps(List.of());
        tab.setDowngradeRecommendation(null);
        tab.setRiskAdjustmentSuggestion(null);
        tab.setManualReviewRequired(Boolean.TRUE.equals(role.manualReviewRequired()) ? "是" : null);
        tab.setReviewConclusion(trimToNull(role.summary()));
    }

    private void populateChallengeRole(DashboardHomeVO.AiTabVO tab,
                                       AiRoleResultsPayload.RolePayload role) {
        tab.setChallengeThesis(trimToNull(role.summary()));
        tab.setEventRisks(List.of());
        tab.setSentimentReversalRisks(List.of());
        tab.setMicrostructureTraps(List.of());
        tab.setLiquidityRisks(List.of());
        tab.setCounterEvidence("CHALLENGE".equals(role.stance())
                ? role.reasonCodes()
                : List.of());
        tab.setChallengeConclusion(trimToNull(role.summary()));
        tab.setReviewConclusion(tab.getChallengeConclusion());
    }

    private String worthOpeningLabel(Boolean worthOpening) {
        if (worthOpening == null) {
            return null;
        }
        return worthOpening ? "是" : "否";
    }

    private PushInboxContext buildPushInbox(List<UserPositionVO> positions, int limit) {
        DashboardHomeVO.PushInboxVO inbox = new DashboardHomeVO.PushInboxVO();
        boolean hasOpenPosition = hasOpenManualPosition(positions);
        inbox.setHasOpenPosition(hasOpenPosition);
        inbox.setMode(hasOpenPosition ? "OPPORTUNITY_AND_POSITION_RISK" : "OPPORTUNITY_ONLY");
        inbox.setTelegramStatus("WAITING_SYNC");

        boolean readOk = false;
        int waiting = 0;
        int executable = 0;
        int invalidated = 0;
        List<DashboardHomeVO.PushItemVO> items = new ArrayList<>();
        try {
            waiting = Math.max(0, pushSnapshotMapper.countPendingRecheckBacklog());
            executable = safeCountPushStatuses(EXECUTABLE_PUSH_STATUSES);
            invalidated = safeCountPushStatuses(INVALIDATED_PUSH_STATUSES);
            items.addAll(pushItems("CAPTURED", limit));
            if (items.size() < limit) {
                items.addAll(pushItems("RECHECK_REVIEW_WAITING", limit - items.size()));
            }
            if (items.size() < limit) {
                items.addAll(pushItems("RECHECK_VALID_WAITING", limit - items.size()));
            }
            readOk = true;
        } catch (RuntimeException ignored) {
            waiting = 0;
            items = List.of();
        }

        DashboardHomeVO.PushCountsVO counts = new DashboardHomeVO.PushCountsVO();
        counts.setExecutable(executable);
        counts.setWaiting(waiting);
        counts.setInvalidated(invalidated);
        counts.setPositionRisk(0);
        inbox.setCounts(counts);
        inbox.setItems(items.size() > limit ? items.subList(0, limit) : items);
        return new PushInboxContext(inbox, readOk);
    }

    private boolean hasOpenManualPosition(List<UserPositionVO> positions) {
        for (UserPositionVO position : positions == null ? List.<UserPositionVO>of() : positions) {
            if (isActiveManualPosition(position)) {
                return true;
            }
        }
        return false;
    }

    private int safeCountPushStatuses(List<String> statuses) {
        if (statuses == null || statuses.isEmpty()) {
            return 0;
        }
        try {
            return Math.max(0, pushSnapshotMapper.countByPushStatuses(statuses));
        } catch (RuntimeException ignored) {
            return 0;
        }
    }

    private List<DashboardHomeVO.PushItemVO> pushItems(String status, int limit) {
        if (limit <= 0) {
            return List.of();
        }
        List<DashboardHomeVO.PushItemVO> items = new ArrayList<>();
        List<TmPushSnapshotDO> rows = pushSnapshotMapper.listPendingRecheck(status, limit);
        for (TmPushSnapshotDO row : rows == null ? List.<TmPushSnapshotDO>of() : rows) {
            if (row == null) {
                continue;
            }
            DashboardHomeVO.PushItemVO item = new DashboardHomeVO.PushItemVO();
            item.setPushId(row.getPushId());
            item.setSymbol(toDisplaySymbol(row.getSymbol()));
            item.setStatus(row.getPushStatus());
            item.setType(row.getPushType());
            item.setExpiresAt(row.getExpiresAt());
            item.setRecheckStatus(latestRecheckStatus(row.getPushId()));
            item.setCreatedAt(row.getPushCreateTime() != null ? row.getPushCreateTime() : row.getCreateTime());
            items.add(item);
        }
        return items;
    }

    private String latestRecheckStatus(Long pushId) {
        if (pushId == null) {
            return null;
        }
        try {
            TmPushRecheckLogDO latest = pushRecheckLogMapper.selectLatestByPushId(pushId);
            return latest != null
                    ? trimToNull(PushRecheckStatusContract.canonicalizeRecheckStatusName(latest.getRecheckStatus()))
                    : null;
        } catch (RuntimeException ignored) {
            return null;
        }
    }

    private DashboardHomeVO.DiagnosticsVO buildDiagnostics(LightSystemStatusVO systemStatus,
                                                           List<DecisionResultVO> decisions,
                                                           DecisionResultVO selectedDecision,
                                                           PositionSyncStatusVO positionSyncStatus,
                                                           PushInboxContext pushInboxContext,
                                                           ProviderReadinessVO providerReadiness) {
        DashboardHomeVO.DiagnosticsVO diagnostics = new DashboardHomeVO.DiagnosticsVO();
        diagnostics.setDataIngestion(diagnosticFromFreshness(positionSyncStatus));
        diagnostics.setDataQuality(averageDataQuality(decisions) != null ? "CONNECTED" : "WAITING_SYNC");
        diagnostics.setAiCall(hasText(selectedDecision != null ? selectedDecision.getAiRoleResults() : null) ? "CONNECTED" : "WAITING_SYNC");
        diagnostics.setPushRecheck(pushInboxContext.readOk() ? "CONNECTED" : "UNKNOWN");
        diagnostics.setTelegram("WAITING_SYNC");
        diagnostics.setConfused(confusedCount(systemStatus, decisions) != null ? "CONNECTED" : "UNKNOWN");
        diagnostics.setHotReset(systemStatus != null && systemStatus.getHotResetFired() != null ? "CONNECTED" : "WAITING_SYNC");
        diagnostics.setOpportunityLog("UNKNOWN");
        diagnostics.setReview("UNKNOWN");
        diagnostics.setMarketDataProvider(firstNonBlank(
                providerReadiness != null ? providerReadiness.getMarketDataProviderStatus() : null,
                "WAITING_SYNC"));
        diagnostics.setAiProvider(firstNonBlank(
                providerReadiness != null ? providerReadiness.getAiProviderStatus() : null,
                "WAITING_SYNC"));
        diagnostics.setExternalContextProvider(firstNonBlank(
                providerReadiness != null ? providerReadiness.getExternalContextProviderStatus() : null,
                "WAITING_SYNC"));
        diagnostics.setProviderReadiness(providerReadiness);
        return diagnostics;
    }

    private DashboardHomeVO.StatusCardVO card(String key,
                                              String label,
                                              Object value,
                                              String valueLabel,
                                              String helper,
                                              String status,
                                              Integer score) {
        DashboardHomeVO.StatusCardVO card = new DashboardHomeVO.StatusCardVO();
        card.setKey(key);
        card.setLabel(label);
        card.setValue(value);
        card.setValueLabel(valueLabel);
        card.setHelper(helper);
        card.setStatus(status);
        card.setScore(score);
        return card;
    }

    private LightSystemStatusVO safeSystemStatus() {
        try {
            return decisionService.getLightSystemStatus();
        } catch (RuntimeException ignored) {
            return null;
        }
    }

    private List<DecisionResultVO> safeDecisions(int limit) {
        try {
            List<DecisionResultVO> decisions = decisionService.getLatestDecisionResults(limit);
            return decisions != null ? decisions : List.of();
        } catch (RuntimeException ignored) {
            return List.of();
        }
    }

    private DecisionResultVO safeDecisionBySymbol(String symbol) {
        try {
            return hasText(symbol) ? decisionService.getLatestDecisionResultBySymbol(symbol) : null;
        } catch (RuntimeException ignored) {
            return null;
        }
    }

    private List<MonitorAlertDO> safeAlerts() {
        try {
            List<MonitorAlertDO> alerts = monitorService.getRecentAlerts(2);
            return alerts != null ? alerts : List.of();
        } catch (RuntimeException ignored) {
            return List.of();
        }
    }

    private List<UserPositionVO> safePositions() {
        try {
            List<UserPositionVO> positions = userPositionService.listOpenPositions();
            return positions != null ? positions : List.of();
        } catch (RuntimeException ignored) {
            return List.of();
        }
    }

    private PositionSyncStatusVO safePositionSyncStatus() {
        try {
            return positionSyncService.getPositionSyncStatus();
        } catch (RuntimeException ignored) {
            return null;
        }
    }

    private ProviderReadinessVO safeProviderReadiness() {
        try {
            ProviderReadinessVO readiness = providerReadinessService.getReadiness();
            return readiness != null ? readiness : new ProviderReadinessVO();
        } catch (RuntimeException ignored) {
            return new ProviderReadinessVO();
        }
    }

    private ExternalContextSnapshot safeExternalContext(String selectedSymbol, DecisionResultVO decision) {
        try {
            return externalContextEvidenceBuilder.buildSnapshot(
                    "dashboard-home",
                    selectedSymbol,
                    firstNonBlank(decision != null ? decision.getTimeframe() : null, "1h"),
                    LocalDateTime.now(),
                    "CRYPTO"
            );
        } catch (RuntimeException ignored) {
            return null;
        }
    }

    private int normalizeLimit(Integer limit) {
        if (limit == null || limit <= 0) {
            return DEFAULT_LIMIT;
        }
        return Math.min(limit, MAX_LIMIT);
    }

    private String firstDecisionSymbol(List<DecisionResultVO> decisions) {
        DecisionResultVO first = firstDecision(decisions);
        return first != null ? normalizeSymbol(first.getSymbol()) : null;
    }

    private DecisionResultVO firstDecision(List<DecisionResultVO> decisions) {
        if (decisions == null) {
            return null;
        }
        return decisions.stream().filter(Objects::nonNull).findFirst().orElse(null);
    }

    private DecisionResultVO findDecision(List<DecisionResultVO> decisions, String normalizedSymbol) {
        if (!hasText(normalizedSymbol) || decisions == null) {
            return null;
        }
        return decisions.stream()
                .filter(Objects::nonNull)
                .filter(d -> normalizedSymbol.equals(normalizeSymbol(d.getSymbol())))
                .findFirst()
                .orElse(null);
    }

    private String riskLevelFrom(List<DecisionResultVO> decisions) {
        String highest = null;
        int highestRank = -1;
        if (decisions != null) {
            for (DecisionResultVO decision : decisions) {
                if (decision == null) {
                    continue;
                }
                String risk = trimToNull(decision.getRiskLevel());
                int rank = riskRank(risk);
                if (rank > highestRank) {
                    highest = risk;
                    highestRank = rank;
                }
            }
        }
        return highest;
    }

    private Integer averageDataQuality(List<DecisionResultVO> decisions) {
        if (decisions == null || decisions.isEmpty()) {
            return null;
        }
        int sum = 0;
        int count = 0;
        for (DecisionResultVO decision : decisions) {
            if (decision != null && decision.getDataQualityScore() != null) {
                sum += decision.getDataQualityScore();
                count++;
            }
        }
        return count > 0 ? Math.round((float) sum / count) : null;
    }

    private AiConflictSummary aiConflictSummary(List<DecisionResultVO> decisions) {
        String level = null;
        Integer score = null;
        if (decisions != null) {
            for (DecisionResultVO decision : decisions) {
                if (decision == null) {
                    continue;
                }
                if (level == null && hasText(decision.getAiConflictLevel())) {
                    level = decision.getAiConflictLevel();
                }
                if (decision.getAiConflictScore() != null && (score == null || decision.getAiConflictScore() > score)) {
                    score = decision.getAiConflictScore();
                    if (hasText(decision.getAiConflictLevel())) {
                        level = decision.getAiConflictLevel();
                    }
                }
            }
        }
        return new AiConflictSummary(level, score);
    }

    private Integer confusedCount(LightSystemStatusVO systemStatus, List<DecisionResultVO> decisions) {
        if (systemStatus != null && systemStatus.getConfusedCount() != null) {
            return systemStatus.getConfusedCount();
        }
        if (decisions == null) {
            return null;
        }
        int count = 0;
        boolean sawField = false;
        for (DecisionResultVO decision : decisions) {
            if (decision != null && decision.getConfusedScore() != null) {
                sawField = true;
                if (decision.getConfusedScore() > 0) {
                    count++;
                }
            }
        }
        return sawField ? count : null;
    }

    private String dataSourceText(PositionSyncStatusVO positionSyncStatus,
                                  ExternalContextSnapshot externalContext,
                                  ProviderReadinessVO providerReadiness) {
        String provider = positionSyncStatus != null
                ? firstNonBlank(positionSyncStatus.getActiveProviderType(), positionSyncStatus.getConfiguredProviderType())
                : null;
        String sourceHealth = externalContext != null ? externalContext.getSourceHealth() : null;
        if (hasText(provider) && hasText(sourceHealth)) {
            return provider + " / " + sourceHealth;
        }
        return firstNonBlank(
                provider,
                providerReadiness != null ? providerReadiness.getDataSourceText() : null,
                sourceHealth,
                "WAITING_SYNC"
        );
    }

    private String diagnosticFromFreshness(PositionSyncStatusVO positionSyncStatus) {
        if (positionSyncStatus == null || !hasText(positionSyncStatus.getFreshnessStatus())) {
            return "WAITING_SYNC";
        }
        String value = positionSyncStatus.getFreshnessStatus().toUpperCase(Locale.ROOT);
        if (value.contains("FRESH") || value.contains("READY")) {
            return "CONNECTED";
        }
        if (value.contains("STALE") || value.contains("PARTIAL")) {
            return "PARTIAL";
        }
        return "UNKNOWN";
    }

    private String statusForText(String value) {
        return hasText(value) ? "CONNECTED" : "WAITING_SYNC";
    }

    private String recognizedAssetStateFromSnapshot(String snapshot) {
        String raw = trimToNull(snapshot);
        if (raw == null) {
            return null;
        }
        try {
            JsonNode root = objectMapper.readTree(raw);
            if (root == null || !root.isObject()) {
                return null;
            }
            String state = recognizedAssetStateValue(root.get("state"));
            if (state != null) {
                return state;
            }
            return recognizedAssetStateValue(root.get("nextState"));
        } catch (Exception ignored) {
            return null;
        }
    }

    private String recognizedAssetStateValue(JsonNode node) {
        if (node == null || !node.isTextual()) {
            return null;
        }
        return switch (node.asText()) {
            case "OBSERVING", "CANDIDATE", "WAITING_TRIGGER", "TRIGGERED",
                    "HIGH_RISK", "INVALIDATED", "COOLING", "CONFUSED" -> node.asText();
            default -> null;
        };
    }

    private String assetStateLabel(String assetState) {
        return switch (assetState != null ? assetState : "") {
            case "OBSERVING" -> "观察";
            case "CANDIDATE" -> "候选";
            case "WAITING_TRIGGER" -> "等待触发";
            case "TRIGGERED" -> "已触发";
            case "HIGH_RISK" -> "高风险观察";
            case "INVALIDATED" -> "已失效";
            case "COOLING" -> "冷却";
            case "CONFUSED" -> "Confused 阻断";
            default -> null;
        };
    }

    private int riskRank(String risk) {
        return switch (upper(risk)) {
            case "EXTREME" -> 4;
            case "HIGH" -> 3;
            case "MEDIUM" -> 2;
            case "LOW" -> 1;
            default -> -1;
        };
    }

    private String biasLabel(String bias) {
        String value = upper(bias);
        return switch (value) {
            case "STRONG_BULLISH" -> "强偏多";
            case "BULLISH" -> "偏多";
            case "WEAK_BULLISH" -> "弱偏多";
            case "RANGE" -> "震荡";
            case "WEAK_BEARISH" -> "弱偏空";
            case "BEARISH" -> "偏空";
            case "STRONG_BEARISH" -> "强偏空";
            case "WAIT" -> "等待";
            default -> null;
        };
    }

    private String confidenceLabel(String confidence) {
        String value = upper(confidence);
        return switch (value) {
            case "HIGH" -> "高";
            case "MEDIUM" -> "中";
            case "LOW" -> "低";
            default -> null;
        };
    }

    private String riskLabel(String risk) {
        String value = upper(risk);
        return switch (value) {
            case "EXTREME", "VERY_HIGH" -> "极高";
            case "HIGH" -> "高";
            case "MEDIUM" -> "中";
            case "LOW" -> "低";
            default -> hasText(risk) ? risk : null;
        };
    }

    private String roleLabel(String role) {
        return switch (role) {
            case "GPT_FINAL" -> "最终裁决官";
            case "GEMINI_REVIEW" -> "冲突复核官";
            case "GROK_CHALLENGE" -> "反方挑战官";
            default -> role;
        };
    }

    private String normalizeSymbol(String symbol) {
        if (!hasText(symbol)) {
            return null;
        }
        return symbol.trim().toUpperCase(Locale.ROOT).replace("/", "");
    }

    private String toDisplaySymbol(String symbol) {
        String normalized = normalizeSymbol(symbol);
        if (normalized == null) {
            return null;
        }
        if (normalized.endsWith("USDT") && normalized.length() > 4) {
            return normalized.substring(0, normalized.length() - 4) + "/USDT";
        }
        return normalized;
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private String trimPlanValue(String value) {
        String trimmed = trimToNull(value);
        if (trimmed == null || "暂无".equals(trimmed) || "—".equals(trimmed) || "待生成".equals(trimmed)) {
            return null;
        }
        return trimmed;
    }

    private String firstNonBlank(String... values) {
        for (String value : values) {
            String trimmed = trimToNull(value);
            if (trimmed != null) {
                return trimmed;
            }
        }
        return null;
    }

    private boolean hasText(String value) {
        return trimToNull(value) != null;
    }

    private boolean positive(BigDecimal value) {
        return value != null && value.compareTo(BigDecimal.ZERO) > 0;
    }

    private String upper(String value) {
        String trimmed = trimToNull(value);
        return trimmed == null ? "" : trimmed.toUpperCase(Locale.ROOT);
    }

    private record AiConflictSummary(String level, Integer score) {
    }

    private record PushInboxContext(DashboardHomeVO.PushInboxVO pushInbox, boolean readOk) {
    }
}
