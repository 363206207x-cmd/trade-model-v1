package org.example.trademodel.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.trademodel.analysisrun.AnalysisTimePolicy;
import org.example.trademodel.ai.AiRoleResultsCodec;
import org.example.trademodel.ai.AiRoleResultsPayload;
import org.example.trademodel.derivatives.DerivativesBusinessAssessment;
import org.example.trademodel.derivatives.DerivativesBusinessInput;
import org.example.trademodel.derivatives.DerivativesBusinessIntegrationService;
import org.example.trademodel.derivatives.DerivativesEvidenceItem;
import org.example.trademodel.derivatives.DerivativesEvidenceType;
import org.example.trademodel.derivatives.DerivativesSnapshotReadPort;
import org.example.trademodel.entity.MonitorAlertDO;
import org.example.trademodel.entity.AnalysisRunDO;
import org.example.trademodel.entity.AssetStateDO;
import org.example.trademodel.entity.ExecutionPlanDO;
import org.example.trademodel.entity.TmPushRecheckLogDO;
import org.example.trademodel.entity.TmPushSnapshotDO;
import org.example.trademodel.providercall.AssetPriority;
import org.example.trademodel.providercall.ProviderCallResult;
import org.example.trademodel.providercall.snapshot.MarketPriceSnapshot;
import org.example.trademodel.providercall.snapshot.MarketPriceSnapshotPolicy;
import org.example.trademodel.providercall.snapshot.MarketPriceSnapshotService;
import org.example.trademodel.providercall.snapshot.DerivativesRiskSnapshot;
import org.example.trademodel.mapper.PushRecheckLogMapper;
import org.example.trademodel.mapper.PushSnapshotMapper;
import org.example.trademodel.mapper.AnalysisRunMapper;
import org.example.trademodel.mapper.AssetStateMapper;
import org.example.trademodel.mapper.DecisionResultMapper;
import org.example.trademodel.mapper.ExecutionPlanMapper;
import org.example.trademodel.mapper.PersistedOhlcvBarMapper;
import org.example.trademodel.localreal.LocalRealAssetReadiness;
import org.example.trademodel.localreal.LocalRealAssetReadinessState;
import org.example.trademodel.localreal.LocalRealReadinessService;
import org.example.trademodel.entity.PersistedOhlcvBarDO;
import org.example.trademodel.service.DashboardHomeService;
import org.example.trademodel.service.ConfusedStatePolicy;
import org.example.trademodel.service.DecisionService;
import org.example.trademodel.service.MonitorService;
import org.example.trademodel.service.PositionMonitorLogService;
import org.example.trademodel.service.PositionSyncService;
import org.example.trademodel.service.PushRecheckStatusContract;
import org.example.trademodel.service.UserPositionService;
import org.example.trademodel.service.readiness.ProviderReadinessService;
import org.example.trademodel.positionmonitorlog.PositionMonitorLogDTO;
import org.example.trademodel.positionmonitor.PositionPlanSourceResolver;
import org.example.trademodel.service.support.ExecutionPlanReviewPolicy;
import org.example.trademodel.service.support.ExecutionPlanReviewPolicy.PersistedPlanState;
import org.example.trademodel.service.support.ExternalContextEvidenceBuilder;
import org.example.trademodel.service.support.UtcLocalTimePolicy;
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
import java.time.Clock;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.regex.Pattern;

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
    private static final int MIN_DATA_QUALITY_SCORE_FOR_PLAN = 60;
    private static final Pattern LEGACY_VALID_PERIOD_RANGE = Pattern.compile(
            "^(\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}:\\d{2})\\s*~\\s*"
                    + "(\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}:\\d{2})$");
    private static final DateTimeFormatter OFFSET_PLAN_TIME_FORMAT = DateTimeFormatter.ISO_OFFSET_DATE_TIME;

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
    private DerivativesSnapshotReadPort derivativesSnapshotReadPort;
    private DerivativesBusinessIntegrationService derivativesBusinessIntegrationService;
    private PersistedOhlcvBarMapper persistedOhlcvBarMapper;
    private AnalysisRunMapper analysisRunMapper;
    private DecisionResultMapper decisionResultMapper;
    private PositionPlanSourceResolver positionPlanSourceResolver;
    private LocalRealReadinessService localRealReadinessService;
    private AssetStateMapper assetStateMapper;
    private Clock planValidityClock = Clock.systemUTC();

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

    @Autowired(required = false)
    void setDerivativesBusinessIntegration(DerivativesSnapshotReadPort derivativesSnapshotReadPort,
                                           DerivativesBusinessIntegrationService derivativesBusinessIntegrationService) {
        this.derivativesSnapshotReadPort = derivativesSnapshotReadPort;
        this.derivativesBusinessIntegrationService = derivativesBusinessIntegrationService;
    }

    @Autowired(required = false)
    void setLocalRealDashboardSources(PersistedOhlcvBarMapper persistedOhlcvBarMapper,
                                      AnalysisRunMapper analysisRunMapper) {
        this.persistedOhlcvBarMapper = persistedOhlcvBarMapper;
        this.analysisRunMapper = analysisRunMapper;
    }

    @Autowired(required = false)
    void setLocalRealReadinessService(LocalRealReadinessService localRealReadinessService) {
        this.localRealReadinessService = localRealReadinessService;
    }

    @Autowired(required = false)
    void setAssetStateMapper(AssetStateMapper assetStateMapper) {
        this.assetStateMapper = assetStateMapper;
    }

    @Autowired(required = false)
    void setOriginalPlanSources(DecisionResultMapper decisionResultMapper,
                                ExecutionPlanMapper executionPlanMapper,
                                AnalysisRunMapper analysisRunMapper) {
        this.decisionResultMapper = decisionResultMapper;
        this.analysisRunMapper = analysisRunMapper;
        this.positionPlanSourceResolver = new PositionPlanSourceResolver(executionPlanMapper, analysisRunMapper);
    }

    void setPlanValidityClock(Clock planValidityClock) {
        this.planValidityClock = planValidityClock != null ? planValidityClock : Clock.systemUTC();
    }

    @Override
    public DashboardHomeVO getHome(String selectedSymbol, Integer limit, Long selectedPositionId) {
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

        DashboardHomeVO.AiDecisionVO aiDecision = buildAiDecision(selectedDecision);
        DashboardHomeVO home = new DashboardHomeVO();
        home.setHeader(buildHeader(systemStatus, positionSyncStatus, externalContext, providerReadiness, aiDecision));
        home.setSystemState(buildSystemState(systemStatus, decisions, selectedDecision, aiDecision));
        home.setAlerts(buildAlerts(alerts));
        home.setEvents(buildEvents(externalContext));
        home.setAssets(buildAssets(decisions, effectiveLimit));
        PositionRowsResult positionRowsResult = buildPositions(positions);
        List<DashboardHomeVO.PositionVO> positionRows = positionRowsResult.rows();
        home.setPositions(positionRows);
        home.setSelectedSymbol(normalizedSelected);
        PositionSelectionResult positionSelection = resolveSelectedPosition(
                positionRows, normalizedSelected, selectedPositionId);
        DashboardHomeVO.PositionVO activePosition = positionSelection.selectedPosition();
        home.setSelectedPositionId(activePosition != null ? activePosition.getPositionId() : null);
        home.setPositionSelectionStatus(positionSelection.status().name());
        home.setMatchingPositionCount(positionSelection.matchingPositionCount());
        PositionPlanSourceResolver.Resolution activePositionSource = activePosition == null
                ? null : positionRowsResult.trustedSources().get(activePosition.getPositionId());
        ResolvedOriginalPlan resolvedOriginalPlan = resolveOriginalPlan(activePosition, activePositionSource);
        home.setExecutionSuggestion(positionSelection.blocked()
                ? buildPositionSelectionSuggestion(positionSelection)
                : buildExecutionSuggestion(selectedDecision, activePosition, resolvedOriginalPlan));
        home.setAiDecision(aiDecision);
        home.setPushInbox(pushInboxContext.pushInbox());
        home.setDerivatives(buildDerivativesSummary(normalizedSelected, selectedDecision));
        home.setDiagnostics(buildDiagnostics(systemStatus, decisions, selectedDecision, positionSyncStatus,
                pushInboxContext, providerReadiness));
        home.setSafety(new DashboardHomeVO.SafetyVO());
        return home;
    }

    private DashboardHomeVO.DerivativesSummaryVO buildDerivativesSummary(String symbol,
                                                                         DecisionResultVO selectedDecision) {
        DashboardHomeVO.DerivativesSummaryVO summary = new DashboardHomeVO.DerivativesSummaryVO();
        if (derivativesSnapshotReadPort == null || derivativesBusinessIntegrationService == null) return summary;
        try {
            ProviderCallResult<DerivativesRiskSnapshot> result = derivativesSnapshotReadPort.readCached(
                    symbol, AssetPriority.P1_WATCHLIST, Duration.ofSeconds(60), "dashboard-derivatives-" + UUID.randomUUID());
            DerivativesRiskSnapshot snapshot = result == null ? null : result.payload();
            if (snapshot == null) {
                summary.setStatus("未配置");
                return summary;
            }
            DerivativesBusinessAssessment assessment = derivativesBusinessIntegrationService.evaluate(
                    new DerivativesBusinessInput(symbol,
                            selectedDecision == null ? null : selectedDecision.getMarketBiasHierarchy(),
                            null, null, false, Map.of(), false,
                            selectedDecision == null ? null : selectedDecision.getDataQualityScore(),
                            true, false, false, null, snapshot,
                            snapshot.traceId(), selectedDecision == null ? null : selectedDecision.getAnalysisId(),
                            "DASHBOARD_READONLY"));
            summary.setStatus(derivativesStatusLabel(snapshot));
            summary.setDataTime(snapshot.providerDataTime());
            summary.setReasonCodes(assessment.reasonCodes());
            for (DerivativesEvidenceItem evidence : assessment.evidence()) {
                DerivativesEvidenceType type = evidence.evidenceType();
                if (type == DerivativesEvidenceType.OPEN_INTEREST_EXPANSION) summary.setOpenInterestStructure("增加");
                if (type == DerivativesEvidenceType.OPEN_INTEREST_CONTRACTION) summary.setOpenInterestStructure("减少");
                if (type == DerivativesEvidenceType.OPEN_INTEREST_PRICE_DIVERGENCE) summary.setOpenInterestStructure("背离");
                if (type == DerivativesEvidenceType.FUNDING_NORMAL) summary.setFundingRisk("正常");
                if (type == DerivativesEvidenceType.FUNDING_POSITIVE_EXTREME
                        || type == DerivativesEvidenceType.FUNDING_NEGATIVE_EXTREME) summary.setFundingRisk("极端");
                if (type == DerivativesEvidenceType.LONG_CROWDING) summary.setCrowdingDirection("多头");
                if (type == DerivativesEvidenceType.SHORT_CROWDING) summary.setCrowdingDirection("空头");
                if (type == DerivativesEvidenceType.LONG_LIQUIDATION_SPIKE
                        || type == DerivativesEvidenceType.SHORT_LIQUIDATION_SPIKE
                        || type == DerivativesEvidenceType.LIQUIDATION_IMBALANCE) summary.setLiquidationRisk("异常");
            }
            if ("暂无".equals(summary.getLiquidationRisk())
                    && (snapshot.longLiquidationUsd5m() != null || snapshot.longLiquidationUsd15m() != null
                    || snapshot.shortLiquidationUsd5m() != null || snapshot.shortLiquidationUsd15m() != null)) {
                summary.setLiquidationRisk("正常");
            }
            summary.setDecisionImpact(assessment.isHighRisk() ? "风险阻断"
                    : "COMPLETE".equalsIgnoreCase(snapshot.evidenceAvailability()) ? "确认" : "降级");
        } catch (RuntimeException failure) {
            summary.setStatus("错误");
            summary.setDecisionImpact("等待同步");
        }
        return summary;
    }

    private static String derivativesStatusLabel(DerivativesRiskSnapshot snapshot) {
        if (snapshot == null || snapshot.sourceStatus() == null) return "等待同步";
        return switch (snapshot.sourceStatus()) {
            case READY -> "COMPLETE".equalsIgnoreCase(snapshot.evidenceAvailability()) ? "正常" : "部分";
            case DEGRADED, EMPTY_CONFIRMED -> "部分";
            case STALE -> "过期";
            case NOT_CONFIGURED, DISABLED -> "未配置";
            case ERROR -> "错误";
            default -> "等待同步";
        };
    }

    private DashboardHomeVO.HeaderVO buildHeader(LightSystemStatusVO systemStatus,
                                                 PositionSyncStatusVO positionSyncStatus,
                                                 ExternalContextSnapshot externalContext,
                                                 ProviderReadinessVO providerReadiness,
                                                 DashboardHomeVO.AiDecisionVO aiDecision) {
        DashboardHomeVO.HeaderVO header = new DashboardHomeVO.HeaderVO();
        header.setPageTitle("首页总览");
        header.setDataStatus(firstNonBlank(systemStatus != null ? systemStatus.getStatus() : null, "WAITING_SYNC"));
        String aiStatus = headerAiStatus(providerReadiness, aiDecision);
        header.setAiStatus(aiStatus);
        header.setAiStatusLabel(headerAiStatusLabel(aiStatus));
        header.setDataSourceText(dataSourceText(positionSyncStatus, externalContext, providerReadiness));
        if (localRealReadinessService != null) {
            header.setDataSourceText(localRealDataSourceText());
        }
        header.setUpdatedAt(LocalDateTime.now());
        return header;
    }

    private String headerAiStatus(ProviderReadinessVO providerReadiness,
                                  DashboardHomeVO.AiDecisionVO aiDecision) {
        String decisionStatus = trimToNull(aiDecision != null ? aiDecision.getRunStatus() : null);
        String providerStatus = trimToNull(providerReadiness != null
                ? providerReadiness.getAiProviderStatus() : null);
        if (decisionStatus != null && !"NOT_CALLED".equalsIgnoreCase(decisionStatus)) {
            return upper(decisionStatus);
        }
        if (providerStatus != null && switch (upper(providerStatus)) {
            case "DISABLED", "NOT_CONFIGURED", "MODEL_UNAVAILABLE", "TIMEOUT", "FAILED",
                    "BUDGET_BLOCKED", "RATE_LIMITED", "SUCCESS", "PARTIAL_SUCCESS" -> true;
            default -> false;
        }) {
            return upper(providerStatus);
        }
        return decisionStatus != null ? upper(decisionStatus) : "NOT_CALLED";
    }

    private String headerAiStatusLabel(String status) {
        return switch (upper(status)) {
            case "DISABLED" -> "已禁用";
            case "NOT_CONFIGURED" -> "未配置";
            case "MODEL_UNAVAILABLE" -> "模型不可用";
            case "TIMEOUT" -> "调用超时";
            case "FAILED", "INVALID_RESPONSE" -> "调用失败";
            case "BUDGET_BLOCKED" -> "预算阻断";
            case "RATE_LIMITED" -> "调用受限";
            case "SUCCESS" -> "正常";
            case "PARTIAL_SUCCESS" -> "部分可用";
            case "NOT_CALLED", "STARTED" -> "未调用";
            default -> "未知状态";
        };
    }

    private DashboardHomeVO.SystemStateVO buildSystemState(LightSystemStatusVO systemStatus,
                                                           List<DecisionResultVO> decisions,
                                                           DecisionResultVO selectedDecision,
                                                           DashboardHomeVO.AiDecisionVO aiDecision) {
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
        String selectedRisk = trendDecision != null ? trimToNull(trendDecision.getRiskLevel()) : null;
        state.setRiskLevel(card(
                "riskLevel",
                "风险等级",
                selectedRisk,
                riskLabel(selectedRisk),
                "选中资产决策风险",
                statusForText(selectedRisk),
                null
        ));
        Integer selectedDataQuality = trendDecision != null ? trendDecision.getDataQualityScore() : null;
        state.setDataQuality(card(
                "dataQuality",
                "数据质量分",
                selectedDataQuality,
                selectedDataQuality != null ? String.valueOf(selectedDataQuality) : null,
                "选中资产分析快照",
                selectedDataQuality != null ? "CONNECTED" : "WAITING_SYNC",
                selectedDataQuality
        ));
        DashboardHomeVO.ConsistencyVO consistency = aiDecision != null ? aiDecision.getConsistency() : null;
        boolean aiApplicable = consistency != null && Boolean.TRUE.equals(consistency.getAiApplicable());
        String conflictLevel = aiApplicable ? trimToNull(consistency.getLevel()) : null;
        Integer conflictScore = aiApplicable ? consistency.getScore() : null;
        state.setAiConflict(card(
                "aiConflict",
                "AI 冲突等级",
                conflictLevel,
                aiApplicable ? consistency.getConsistencyLevel() : "不适用",
                aiApplicable ? "AI 冲突" : "本轮未形成可裁决 AI 意见",
                aiApplicable
                        ? conflictLevel != null || conflictScore != null ? "CONNECTED" : "WAITING_SYNC"
                        : "NOT_APPLICABLE",
                conflictScore
        ));
        Integer pendingCount = systemStatus != null ? systemStatus.getPendingCount() : null;
        state.setPendingReview(card(
                "pendingReview",
                "待复核机会",
                pendingCount,
                pendingCount != null && pendingCount > 0 ? String.valueOf(pendingCount) : "暂无",
                "待复核数量",
                pendingCount != null ? "CONNECTED" : "WAITING_SYNC",
                pendingCount
        ));
        Integer confusedCount = directionalBlockCount(systemStatus, decisions);
        state.setConfused(card(
                "confused",
                "冲突阻断",
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
                "热重置",
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
        applyPersistedMarketData(asset, normalizeSymbol(decision.getSymbol()));
        if (analysisRunMapper != null && hasText(decision.getAnalysisId())) {
            Double average = analysisRunMapper.selectAverageScoreByAnalysisId(decision.getAnalysisId());
            asset.setCompositeScore(average == null ? null : (int) Math.round(average));
            asset.setEvidenceCount(analysisRunMapper.countEvidenceByAnalysisId(decision.getAnalysisId()));
        }
        asset.setLatestAnalysisTime(decision.getCreateTime());
        asset.setConfidenceLevel(trimToNull(decision.getConfidenceLevel()));
        asset.setConfidenceLabel(confidenceLabel(decision.getConfidenceLevel()));
        asset.setRiskLevel(trimToNull(decision.getRiskLevel()));
        asset.setRiskLabel(riskLabel(decision.getRiskLevel()));
        String assetState = authoritativeAssetState(normalizeSymbol(decision.getSymbol()),
                decision.getAssetStateSnapshot());
        asset.setAssetState(assetState);
        asset.setAssetStateLabel(assetStateLabel(assetState));
        asset.setWorthOpening(decision.getIsWorthOpening());
        asset.setCurrentConclusion(currentConclusion(decision, assetState));
        return asset;
    }

    private DashboardHomeVO.AssetVO assetPlaceholder(int slot, String symbol) {
        DashboardHomeVO.AssetVO asset = assetBase(slot, symbol);
        applyPersistedMarketData(asset, symbol);
        asset.setSlotType(asset.getLatestPrice() == null ? "DEFAULT_SLOT" : "MARKET_DATA");
        return asset;
    }

    private void applyPersistedMarketData(DashboardHomeVO.AssetVO asset, String symbol) {
        if (asset == null || persistedOhlcvBarMapper == null || !hasText(symbol)) {
            return;
        }
        Map<String, String> timeframeFreshness = new LinkedHashMap<>();
        PersistedOhlcvBarDO latest = null;
        for (String timeframe : List.of("5m", "15m", "1h", "4h")) {
            List<PersistedOhlcvBarDO> rows = persistedOhlcvBarMapper.selectLatestClosedWindow(symbol, timeframe, 1);
            PersistedOhlcvBarDO timeframeLatest = rows == null || rows.isEmpty() ? null : rows.get(0);
            timeframeFreshness.put(timeframe, timeframeLatest == null
                    ? "NO_DATA" : firstNonBlank(timeframeLatest.getFreshnessStatus(), "UNKNOWN"));
            if ("5m".equals(timeframe)) latest = timeframeLatest;
        }
        asset.setTimeframeFreshness(Map.copyOf(timeframeFreshness));
        if (latest == null) {
            LocalRealAssetReadiness readiness = localRealReadinessService == null
                    ? null : localRealReadinessService.asset(symbol);
            if (readiness != null && readiness.state() == LocalRealAssetReadinessState.UNAVAILABLE) {
                asset.setDataFreshness("UNAVAILABLE");
                asset.setUnavailableReason(readiness.reasonCode());
            } else {
                asset.setDataFreshness("NO_DATA");
            }
            return;
        }
        asset.setLatestPrice(latest.getClosePrice());
        boolean allFresh = timeframeFreshness.values().stream().allMatch("FRESH"::equalsIgnoreCase);
        boolean anyData = timeframeFreshness.values().stream().anyMatch(value -> !"NO_DATA".equals(value));
        asset.setDataFreshness(allFresh ? "FRESH" : anyData ? "PARTIAL" : "NO_DATA");
        asset.setSourceProvider(providerLabel(latest.getProvider()));
    }

    private DashboardHomeVO.AssetVO assetBase(int slot, String normalizedSymbol) {
        DashboardHomeVO.AssetVO asset = new DashboardHomeVO.AssetVO();
        asset.setSlot(slot);
        asset.setRawSymbol(normalizedSymbol);
        asset.setSymbol(toDisplaySymbol(normalizedSymbol));
        return asset;
    }

    private PositionRowsResult buildPositions(List<UserPositionVO> positions) {
        List<DashboardHomeVO.PositionVO> rows = new ArrayList<>();
        Map<Long, PositionPlanSourceResolver.Resolution> trustedSources = new LinkedHashMap<>();
        for (UserPositionVO position : positions == null ? List.<UserPositionVO>of() : positions) {
            if (!isActiveManualPosition(position)) {
                continue;
            }
            PositionMonitorLogDTO latestMonitorLog = latestPositionMonitorLog(position.getId());
            DashboardHomeVO.PositionVO row = new DashboardHomeVO.PositionVO();
            row.setPositionId(position.getId());
            row.setSymbol(toDisplaySymbol(position.getAssetSymbol()));
            row.setDirection(trimToNull(position.getSide()));
            row.setDirectionLabel(positionDirectionLabel(position.getSide()));
            row.setEntryPrice(position.getEntryPrice());
            BigDecimal currentPrice = latestMonitorLog != null && positive(latestMonitorLog.getCurrentPrice())
                    ? latestMonitorLog.getCurrentPrice()
                    : safeCurrentPrice(position.getAssetSymbol());
            row.setCurrentPrice(currentPrice);
            applyPositionPnl(row, position, currentPrice);
            row.setLeverage(position.getLeverage());
            row.setPositionSize(position.getQuantity());
            row.setPositionStatus(trimToNull(position.getStatus()));
            row.setPositionStatusLabel(positionStatusLabel(position.getStatus()));
            row.setUserStopLoss(position.getStopLoss());
            row.setUserTakeProfit(position.getTakeProfit());
            row.setSystemSuggestedStopLoss(null);
            row.setSystemSuggestedTakeProfit(null);
            row.setMonitorConclusion(latestMonitorLog != null
                    ? positionLogicStatusLabel(latestMonitorLog.getLogicStatus()) : null);
            row.setEntryLogicStatus(latestMonitorLog != null ? trimToNull(latestMonitorLog.getLogicStatus()) : "WAITING_MONITOR");
            row.setEntryLogicStatusLabel(positionLogicStatusLabel(row.getEntryLogicStatus()));
            row.setDirectionSupportStatus(directionSupportStatus(latestMonitorLog));
            row.setDirectionSupportStatusLabel(directionSupportStatusLabel(row.getDirectionSupportStatus()));
            row.setReversalStatus(reversalStatus(latestMonitorLog));
            row.setReversalStatusLabel(reversalStatusLabel(row.getReversalStatus()));
            row.setRiskLevel(latestMonitorLog != null ? trimToNull(latestMonitorLog.getRiskLevel()) : "WAITING_MONITOR");
            row.setRiskLevelLabel(positionRiskLevelLabel(row.getRiskLevel()));
            row.setSuggestedManualAction(latestMonitorLog != null ? trimToNull(latestMonitorLog.getSuggestedAction()) : "MANUAL_REVIEW");
            row.setSuggestedManualActionText(suggestedActionText(row.getSuggestedManualAction(), latestMonitorLog));
            row.setUpdatedAt(position.getUpdatedAt());
            row.setOpenedAt(position.getOpenedAt());
            row.setLastMonitorAt(latestMonitorLog != null ? latestMonitorLog.getCreatedAt() : null);
            row.setNextMonitorAt(null);
            row.setSourceRefId(trimToNull(position.getSourceRefId()));
            if (latestMonitorLog != null
                    && positionPlanSourceResolver != null
                    && Objects.equals(position.getId(), latestMonitorLog.getPositionId())) {
                PositionPlanSourceResolver.Resolution trustedSource = positionPlanSourceResolver
                        .resolveTrustedMonitorSource(position.getId(), position.getAssetSymbol(),
                                position.getSourceRefId(), latestMonitorLog.getAnalysisId(),
                                latestMonitorLog.getExecutionPlanId());
                if (trustedSource.verified()) {
                    row.setSourceAnalysisId(trustedSource.analysisId());
                    row.setSourceExecutionPlanId(trustedSource.executionPlanId());
                    row.setSourceTraceId(trustedSource.sourceTraceId());
                    trustedSources.put(position.getId(), trustedSource);
                }
            }
            rows.add(row);
        }
        return new PositionRowsResult(List.copyOf(rows), Map.copyOf(trustedSources));
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

    private ResolvedOriginalPlan resolveOriginalPlan(DashboardHomeVO.PositionVO position,
                                                     PositionPlanSourceResolver.Resolution source) {
        if (position == null) {
            return ResolvedOriginalPlan.unverified("NO_ACTIVE_POSITION");
        }
        if (source == null || !source.verified()) {
            clearUnverifiedOriginalPlanSource(position);
            return ResolvedOriginalPlan.unverified("NO_VERIFIABLE_MONITOR_SOURCE");
        }
        if (decisionResultMapper == null) {
            clearUnverifiedOriginalPlanSource(position);
            return ResolvedOriginalPlan.unverified("ORIGINAL_PLAN_READ_MODEL_UNAVAILABLE");
        }
        try {
            DecisionResultVO sourceDecision = decisionResultMapper.findByAnalysisIdAndPlanIdJoined(
                    source.analysisId(), source.executionPlanId());
            if (!validOriginalPlanDecision(position, sourceDecision, source.analysisId())) {
                clearUnverifiedOriginalPlanSource(position);
                return ResolvedOriginalPlan.unverified("ORIGINAL_PLAN_IDENTITY_UNVERIFIED");
            }

            position.setSourceAnalysisId(source.analysisId());
            position.setSourceExecutionPlanId(source.executionPlanId());
            position.setSourceTraceId(source.sourceTraceId());
            return new ResolvedOriginalPlan("VERIFIED", source.executionPlan(), sourceDecision,
                    source.analysisRun(), source.analysisId(), source.executionPlanId(),
                    source.sourceTraceId(), null);
        } catch (RuntimeException ignored) {
            clearUnverifiedOriginalPlanSource(position);
            return ResolvedOriginalPlan.unverified("ORIGINAL_PLAN_SOURCE_READ_FAILED");
        }
    }

    private boolean validOriginalPlanDecision(DashboardHomeVO.PositionVO position,
                                              DecisionResultVO decision,
                                              String expectedAnalysisId) {
        if (position == null || decision == null || expectedAnalysisId == null) {
            return false;
        }
        if (!expectedAnalysisId.equals(trimToNull(decision.getAnalysisId()))) {
            return false;
        }
        String positionSymbol = normalizeSymbol(position.getSymbol());
        String decisionSymbol = normalizeSymbol(decision.getSymbol());
        return positionSymbol != null && positionSymbol.equals(decisionSymbol);
    }

    private void clearUnverifiedOriginalPlanSource(DashboardHomeVO.PositionVO position) {
        if (position == null) return;
        position.setSourceAnalysisId(null);
        position.setSourceExecutionPlanId(null);
        position.setSourceTraceId(null);
    }

    private DashboardHomeVO.ExecutionSuggestionVO buildExecutionSuggestion(
            DecisionResultVO selectedDecisionForNewOpportunity,
            DashboardHomeVO.PositionVO activePosition,
            ResolvedOriginalPlan resolvedOriginalPlan) {
        DashboardHomeVO.ExecutionSuggestionVO suggestion = new DashboardHomeVO.ExecutionSuggestionVO();
        if (activePosition != null) {
            suggestion.setStatus("POSITION_MONITORING");
            suggestion.setStatusLabel("持仓监控");
            suggestion.setPositionMode(true);
            suggestion.setPositionMonitor(activePosition);
            if (resolvedOriginalPlan == null || !resolvedOriginalPlan.verified()) {
                suggestion.setOriginalPlanIdentity("UNVERIFIED");
                suggestion.setOriginalPlanCurrentValidity("UNVERIFIED");
                suggestion.setOriginalPlanLabel("暂无可关联的原执行计划");
                return suggestion;
            }

            DecisionResultVO originalDecision = resolvedOriginalPlan.decision();
            OriginalPlanPresentation presentation = originalPlanPresentation(resolvedOriginalPlan);
            suggestion.setOriginalPlanIdentity("VERIFIED");
            suggestion.setOriginalPlanCurrentValidity(presentation.status());
            suggestion.setOriginalPlanLabel(presentation.label());
            suggestion.setSourceAnalysisId(resolvedOriginalPlan.analysisId());
            suggestion.setSourceExecutionPlanId(resolvedOriginalPlan.executionPlanId());
            suggestion.setSourceTraceId(resolvedOriginalPlan.traceId());
            populateOriginalPlan(suggestion, resolvedOriginalPlan.executionPlan(),
                    originalDecision, presentation.validity());
            return suggestion;
        }
        DecisionResultVO decision = selectedDecisionForNewOpportunity;
        if (decision == null) {
            blockSuggestion(suggestion, "NO_COMPLETE_PLAN", "当前暂无完整执行计划", "暂无有效分析快照");
            return suggestion;
        }
        suggestion.setSourceAnalysisId(trimToNull(decision.getAnalysisId()));
        String entryZone = trimPlanValue(decision.getEntryZone());
        String stopLoss = trimPlanValue(decision.getStopLoss());
        String takeProfitRules = trimPlanValue(decision.getTakeProfitRules());
        boolean boundaryComplete = entryZone != null && stopLoss != null && takeProfitRules != null;

        if (!AnalysisTimePolicy.isExecutionPlanPrimaryTimeframe(decision.getTimeframe())) {
            blockSuggestion(suggestion, "UNSUPPORTED_TIMEFRAME", "当前暂无完整执行计划",
                    AnalysisTimePolicy.unsupportedExecutionPlanTimeframeMessage());
            return suggestion;
        }
        if (!hasText(decision.getAnalysisId())) {
            blockSuggestion(suggestion, "ANALYSIS_SNAPSHOT_MISSING", "当前暂无完整执行计划",
                    "分析快照不完整，暂不展示计划");
            return suggestion;
        }
        if (decision.getDataQualityScore() == null
                || decision.getDataQualityScore() < MIN_DATA_QUALITY_SCORE_FOR_PLAN) {
            blockSuggestion(suggestion, "DATA_QUALITY_BLOCKED", "当前暂无完整执行计划",
                    "数据质量不足，等待有效分析");
            return suggestion;
        }
        String assetState = authoritativeAssetState(normalizeSymbol(decision.getSymbol()),
                decision.getAssetStateSnapshot());
        if (!planAllowedAssetState(assetState)) {
            blockSuggestion(suggestion, "ASSET_STATE_BLOCKED", "当前暂无完整执行计划",
                    "当前资产状态不允许形成新计划");
            return suggestion;
        }
        SnapshotTraceStatus snapshotTraceStatus = executionSnapshotTraceStatus(decision);
        if (snapshotTraceStatus == SnapshotTraceStatus.MISMATCH) {
            blockSuggestion(suggestion, "STATE_SNAPSHOT_MISMATCH", "当前暂无完整执行计划",
                    "状态已更新，原计划需重新分析");
            return suggestion;
        }
        if (snapshotTraceStatus != SnapshotTraceStatus.MATCH) {
            blockSuggestion(suggestion, "STATE_SNAPSHOT_UNVERIFIED", "当前暂无完整执行计划",
                    "状态与计划关联信息不完整，需重新分析");
            return suggestion;
        }
        if (riskRank(decision.getRiskLevel()) >= riskRank("HIGH")) {
            blockSuggestion(suggestion, "RISK_BLOCKED", "当前暂无完整执行计划",
                    "风险门控未通过，等待人工复核");
            return suggestion;
        }
        if ("CONFUSED".equals(assetState)
                || decision.getConfusedScore() != null
                && decision.getConfusedScore() >= ConfusedStatePolicy.CONFUSED_ENTER_THRESHOLD) {
            blockSuggestion(suggestion, "CONFLICT_BLOCKED", "当前暂无完整执行计划",
                    "已进入冲突状态，等待人工复核");
            return suggestion;
        }
        if (directionalPushBlocked(decision)) {
            blockSuggestion(suggestion, "DIRECTION_BLOCKED", "当前暂无完整执行计划",
                    "方向结论已阻断，等待重新分析");
            return suggestion;
        }
        if (!Boolean.TRUE.equals(decision.getIsWorthOpening())) {
            blockSuggestion(suggestion, "NOT_WORTH_OPENING", "当前暂无完整执行计划",
                    "当前条件不足，暂不形成新计划");
            return suggestion;
        }
        if (!boundaryComplete) {
            blockSuggestion(suggestion, "BOUNDARY_INCOMPLETE", "当前暂无完整执行计划",
                    BOUNDARY_INCOMPLETE_VALID_PERIOD);
            return suggestion;
        }
        PlanValidity planValidity = resolvePlanValidity(decision);
        if (planValidity.status() == PlanValidityStatus.INVALID) {
            blockSuggestion(suggestion, "VALID_PERIOD_INVALID", "当前暂无完整执行计划",
                    "有效期格式异常，等待重新分析");
            return suggestion;
        }
        if (planValidity.status() == PlanValidityStatus.TIMEZONE_UNVERIFIED) {
            blockSuggestion(suggestion, "LEGACY_TIMEZONE_UNVERIFIED", "当前暂无完整执行计划",
                    "历史计划时区不可验证，需重新分析");
            return suggestion;
        }
        if (planValidity.status() == PlanValidityStatus.NOT_ACTIVE) {
            blockSuggestion(suggestion, "PLAN_NOT_ACTIVE", "当前暂无完整执行计划",
                    "计划尚未进入有效期，等待重新分析");
            return suggestion;
        }
        if (planValidity.status() == PlanValidityStatus.EXPIRED) {
            blockSuggestion(suggestion, "PLAN_EXPIRED", "当前暂无完整执行计划",
                    "计划已失效，等待重新分析");
            return suggestion;
        }

        suggestion.setStatus("USABLE_REVIEW_PLAN");
        suggestion.setStatusLabel("完整执行计划，仅供人工复核");
        suggestion.setDirection(trimToNull(decision.getMarketBiasHierarchy()));
        suggestion.setEntryZone(entryZone);
        suggestion.setStopLoss(stopLoss);
        suggestion.setTakeProfitRules(takeProfitRules);
        suggestion.setLeverageSuggestion(planLeverageLabel(decision.getLeverageSuggestion()));
        suggestion.setPositionSuggestion(trimToNull(decision.getPositionSuggestion()));
        suggestion.setValidPeriod(planValidityDisplay(decision, planValidity));
        suggestion.setValidFrom(planValidity.validFrom());
        suggestion.setExpiresAt(planValidity.expiresAt());
        suggestion.setInvalidCondition(trimToNull(decision.getInvalidCondition()));
        return suggestion;
    }

    private DashboardHomeVO.ExecutionSuggestionVO buildPositionSelectionSuggestion(
            PositionSelectionResult selection) {
        DashboardHomeVO.ExecutionSuggestionVO suggestion = new DashboardHomeVO.ExecutionSuggestionVO();
        PositionSelectionStatus status = selection != null
                ? selection.status() : PositionSelectionStatus.POSITION_NOT_FOUND;
        suggestion.setStatus(status.name());
        suggestion.setPositionMode(false);
        suggestion.setOriginalPlanIdentity("UNVERIFIED");
        suggestion.setOriginalPlanCurrentValidity("UNVERIFIED");
        switch (status) {
            case POSITION_SELECTION_REQUIRED -> {
                suggestion.setStatusLabel("请选择具体持仓");
                suggestion.setBlockedReason("当前标的存在多笔开放手动持仓");
                suggestion.setOriginalPlanLabel("请选择具体持仓");
            }
            case POSITION_SYMBOL_MISMATCH -> {
                suggestion.setStatusLabel("所选持仓与当前标的不匹配");
                suggestion.setBlockedReason("请从当前标的的持仓列表重新选择");
                suggestion.setOriginalPlanLabel("所选持仓与当前标的不匹配");
            }
            default -> {
                suggestion.setStatusLabel("所选持仓不存在");
                suggestion.setBlockedReason("请选择当前仍开放的手动持仓");
                suggestion.setOriginalPlanLabel("所选持仓不存在");
            }
        }
        return suggestion;
    }

    private OriginalPlanPresentation originalPlanPresentation(ResolvedOriginalPlan resolvedOriginalPlan) {
        DecisionResultVO decision = resolvedOriginalPlan != null ? resolvedOriginalPlan.decision() : null;
        ExecutionPlanDO executionPlan = resolvedOriginalPlan != null ? resolvedOriginalPlan.executionPlan() : null;
        PlanValidity validity = resolvePlanValidity(decision);
        if (executionPlan == null) {
            return new OriginalPlanPresentation("PLAN_INCOMPLETE",
                    "原计划边界不完整，仅用于历史复核", validity);
        }
        PersistedPlanState planState = ExecutionPlanReviewPolicy.persistedPlanState(executionPlan);
        switch (planState) {
            case INVALID:
                return new OriginalPlanPresentation("PLAN_INVALID",
                        "原计划已失效，仅用于历史复核", validity);
            case BLOCKED:
                return new OriginalPlanPresentation("PLAN_BLOCKED",
                        "原计划已被门控阻断，仅用于历史复核", validity);
            case REVALIDATION_REQUIRED:
                String reason = revalidationReviewCopy(executionPlan);
                String label = "原计划需要重新验证，仅用于历史复核";
                return new OriginalPlanPresentation("REVALIDATION_REQUIRED",
                        reason == null ? label : label + "：" + reason, validity);
            case MISSING, INCOMPLETE, REVIEW_ONLY:
                return new OriginalPlanPresentation("PLAN_INCOMPLETE",
                        "原计划边界不完整，仅用于历史复核", validity);
            case ACTIVE:
                break;
        }
        SnapshotTraceStatus traceStatus = executionSnapshotTraceStatus(
                decision, resolvedOriginalPlan.analysisRun());
        if (traceStatus == SnapshotTraceStatus.MISMATCH) {
            return new OriginalPlanPresentation("STATE_MISMATCH",
                    "状态已更新，原计划不再作为当前执行依据", validity);
        }
        if (traceStatus == SnapshotTraceStatus.UNVERIFIED) {
            return new OriginalPlanPresentation("STATE_UNVERIFIED",
                    "原执行计划来源已确认，但当前状态关联不可验证", validity);
        }
        return switch (validity.status()) {
            case ACTIVE -> new OriginalPlanPresentation("ACTIVE",
                    "原执行计划，仅用于持仓复核和复盘对照", validity);
            case EXPIRED -> new OriginalPlanPresentation("EXPIRED",
                    "原计划已失效，仅用于历史复核", validity);
            case TIMEZONE_UNVERIFIED -> new OriginalPlanPresentation("TIMEZONE_UNVERIFIED",
                    "历史计划时区不可验证", validity);
            case NOT_ACTIVE -> new OriginalPlanPresentation("NOT_ACTIVE",
                    "原计划尚未进入有效期，仅用于历史复核", validity);
            case INVALID -> new OriginalPlanPresentation("INVALID",
                    "原计划有效期异常，仅用于历史复核", validity);
        };
    }

    private String revalidationReviewCopy(ExecutionPlanDO executionPlan) {
        String reason = trimToNull(executionPlan != null ? executionPlan.getRevalidationReason() : null);
        String normalized = upper(reason);
        if (normalized.contains("EXTREME_PRICE_MOVE")) return "极端价格波动触发重新验证";
        if (normalized.contains("OI_COLLAPSE")) return "持仓量快速收缩触发重新验证";
        if (normalized.contains("LIQUIDITY_DRAIN")) return "流动性快速下降触发重新验证";
        if (normalized.contains("SYSTEMIC_SHOCK")) return "系统性冲击触发重新验证";
        if (reason == null
                && trimToNull(executionPlan != null ? executionPlan.getHotResetEventId() : null) != null) {
            return "热重置已触发重新验证";
        }
        return "重验证原因已记录，等待人工复核";
    }

    private void populateOriginalPlan(DashboardHomeVO.ExecutionSuggestionVO suggestion,
                                      ExecutionPlanDO executionPlan,
                                      DecisionResultVO decision,
                                      PlanValidity validity) {
        if (suggestion == null || executionPlan == null || decision == null) return;
        suggestion.setDirection(trimToNull(decision.getMarketBiasHierarchy()));
        suggestion.setEntryZone(trimPlanValue(executionPlan.getEntryZone()));
        suggestion.setStopLoss(trimPlanValue(executionPlan.getStopLoss()));
        suggestion.setTakeProfitRules(trimPlanValue(executionPlan.getTakeProfitRules()));
        suggestion.setLeverageSuggestion(planLeverageLabel(executionPlan.getLeverageSuggestion()));
        suggestion.setPositionSuggestion(trimToNull(executionPlan.getPositionSuggestion()));
        suggestion.setValidPeriod(planValidityDisplay(decision, validity));
        suggestion.setValidFrom(validity.validFrom());
        suggestion.setExpiresAt(validity.expiresAt());
        suggestion.setInvalidCondition(trimPlanValue(executionPlan.getInvalidCondition()));
    }

    private void blockSuggestion(DashboardHomeVO.ExecutionSuggestionVO suggestion,
                                 String status, String statusLabel, String reason) {
        suggestion.setStatus(status);
        suggestion.setStatusLabel(statusLabel);
        suggestion.setBlockedReason(reason);
    }

    private DashboardHomeVO.AiDecisionVO buildAiDecision(DecisionResultVO decision) {
        DashboardHomeVO.AiDecisionVO ai = new DashboardHomeVO.AiDecisionVO();
        ai.setActiveTab("GPT_FINAL");
        AiRoleResultsCodec.ParseResult parsed = aiRoleResultsCodec.parse(
                decision != null ? decision.getAiRoleResults() : null);
        AiRoleResultsPayload payload = parsed.current() ? parsed.payload() : null;
        AiRoleResultsPayload.SynthesisPayload synthesis = payload != null ? payload.synthesis() : null;
        ai.setSchemaVersion(payload != null ? payload.schemaVersion() : null);
        AiRoleStats roleStats = aiRoleStats(payload);
        boolean aiApplicable = roleStats.adjudicative() > 0;
        String runStatus = aiRunStatus(payload, roleStats.successful());
        ai.setRunStatus(runStatus);
        ai.setRunStatusLabel(aiRunStatusLabel(runStatus));
        ai.setDecisionMode(payload != null ? trimToNull(payload.orchestrationMode()) : "RULE_ONLY_FALLBACK");
        ai.setDecisionModeLabel(aiApplicable
                ? "AI 辅助复核"
                : roleStats.successful() > 0 ? "AI 复核无可裁决结论" : "仅规则判断");
        List<DashboardHomeVO.AiTabVO> tabs = new ArrayList<>();
        for (String role : AI_ROLES) {
            AiRoleResultsPayload.RolePayload rolePayload = payload != null ? payload.roles().get(role) : null;
            tabs.add(buildAiTab(role, rolePayload, synthesis));
        }
        ai.setTabs(tabs);
        DashboardHomeVO.ConsistencyVO consistency = new DashboardHomeVO.ConsistencyVO();
        consistency.setAiApplicable(aiApplicable);
        consistency.setLevel(aiApplicable && decision != null
                ? trimToNull(decision.getAiConflictLevel()) : null);
        consistency.setScore(aiApplicable && decision != null ? decision.getAiConflictScore() : null);
        consistency.setConfused(aiApplicable && synthesis != null && Boolean.TRUE.equals(synthesis.confused()));
        consistency.setDirectionalPushBlocked(directionalPushBlocked(decision));
        consistency.setConsistencyScore(null);
        consistency.setConsistencyLevel(aiApplicable
                ? aiConflictLevelLabel(decision != null ? decision.getAiConflictLevel() : null)
                : "不适用");
        consistency.setConsistencySummary(consistencySummary(roleStats));
        consistency.setDowngradeReason(aiApplicable && synthesis != null
                ? aiDowngradeReasonLabel(synthesis.downgradeReason()) : null);
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
            tab.setRunStatus("NOT_CALLED");
            tab.setRunStatusLabel("未调用");
            tab.setResultAvailable(false);
            tab.setStatusMessage(aiRoleStatusMessage("NOT_CALLED"));
            return tab;
        }
        String callStatus = firstNonBlank(trimToNull(rolePayload.callStatus()), "NOT_CALLED");
        tab.setRunStatus(callStatus);
        tab.setRunStatusLabel(aiRunStatusLabel(callStatus));
        boolean resultAvailable = "SUCCESS".equalsIgnoreCase(callStatus);
        tab.setResultAvailable(resultAvailable);
        tab.setStatusMessage(aiRoleStatusMessage(callStatus));
        if (!resultAvailable) {
            return tab;
        }
        tab.setStance(trimToNull(rolePayload.stance()));
        if ("ABSTAIN".equalsIgnoreCase(rolePayload.stance())) {
            tab.setReviewConclusion("证据不足，暂不判断");
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
        String finalConclusion = userFacingAiSummary(role.summary());
        List<String> coreSupportingEvidence = "SUPPORT".equals(role.stance())
                ? aiReasonLabels(role.reasonCodes())
                : List.of();
        List<String> coreCounterEvidence = "CHALLENGE".equals(role.stance())
                ? aiReasonLabels(role.reasonCodes())
                : List.of();
        String decisionSummary = userFacingAiSummary(role.summary());
        String downgradeReason = synthesis != null ? aiDowngradeReasonLabel(synthesis.downgradeReason()) : null;

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
        tab.setReviewVerdict(aiStanceLabel(role.stance()));
        tab.setDetectedContradictions("CHALLENGE".equals(role.stance())
                ? aiReasonLabels(role.reasonCodes())
                : List.of());
        tab.setWeakEvidence(List.of());
        tab.setLogicGaps(List.of());
        tab.setDowngradeRecommendation(null);
        tab.setRiskAdjustmentSuggestion(null);
        tab.setManualReviewRequired(Boolean.TRUE.equals(role.manualReviewRequired()) ? "是" : null);
        tab.setReviewConclusion(userFacingAiSummary(role.summary()));
    }

    private void populateChallengeRole(DashboardHomeVO.AiTabVO tab,
                                       AiRoleResultsPayload.RolePayload role) {
        tab.setChallengeThesis(userFacingAiSummary(role.summary()));
        tab.setEventRisks(List.of());
        tab.setSentimentReversalRisks(List.of());
        tab.setMicrostructureTraps(List.of());
        tab.setLiquidityRisks(List.of());
        tab.setCounterEvidence("CHALLENGE".equals(role.stance())
                ? aiReasonLabels(role.reasonCodes())
                : List.of());
        tab.setChallengeConclusion(userFacingAiSummary(role.summary()));
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
            LocalDateTime nowUtc = UtcLocalTimePolicy.now(planValidityClock);
            waiting = Math.max(0, pushSnapshotMapper.countPendingRecheckBacklog(nowUtc));
            executable = safeCountPushStatuses(EXECUTABLE_PUSH_STATUSES);
            invalidated = safeCountPushStatuses(INVALIDATED_PUSH_STATUSES);
            items.addAll(pushItems("CAPTURED", limit, nowUtc));
            if (items.size() < limit) {
                items.addAll(pushItems("RECHECK_REVIEW_WAITING", limit - items.size(), nowUtc));
            }
            if (items.size() < limit) {
                items.addAll(pushItems("RECHECK_VALID_WAITING", limit - items.size(), nowUtc));
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

    private List<DashboardHomeVO.PushItemVO> pushItems(String status, int limit, LocalDateTime nowUtc) {
        if (limit <= 0) {
            return List.of();
        }
        List<DashboardHomeVO.PushItemVO> items = new ArrayList<>();
        List<TmPushSnapshotDO> rows = pushSnapshotMapper.listPendingRecheck(status, nowUtc, limit);
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
        diagnostics.setConfused(directionalBlockCount(systemStatus, decisions) != null ? "CONNECTED" : "UNKNOWN");
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

    private Integer directionalBlockCount(LightSystemStatusVO systemStatus, List<DecisionResultVO> decisions) {
        if (systemStatus == null) {
            return null;
        }
        if (systemStatus.getConfusedCount() != null) {
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
                if (decision.getConfusedScore() >= ConfusedStatePolicy.DIRECTIONAL_PUSH_BLOCK_THRESHOLD) {
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

    private String localRealDataSourceText() {
        long ready = localRealReadinessService.readyAssetCount();
        List<String> partiallyAvailable = localRealReadinessService.assets().values().stream()
                .filter(item -> item.state() != LocalRealAssetReadinessState.READY)
                .map(item -> item.symbol().replace("USDT", ""))
                .toList();
        String suffix = partiallyAvailable.isEmpty()
                ? "" : " · 数据部分可用 " + String.join(",", partiallyAvailable);
        return "真实行情资产 " + ready + "/" + DEFAULT_SYMBOLS.size() + " · Kraken" + suffix;
    }

    private String providerLabel(String provider) {
        String normalized = upper(provider);
        if (normalized.startsWith("KRAKEN")) return "Kraken";
        if (normalized.startsWith("BINANCE")) return "Binance";
        return trimToNull(provider);
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

    private String authoritativeAssetState(String symbol, String compatibilitySnapshot) {
        if (assetStateMapper != null && hasText(symbol)) {
            try {
                AssetStateDO row = assetStateMapper.selectBySymbol(symbol);
                String state = row != null && row.getState() != null
                        ? recognizedAssetStateValue(row.getState().name()) : null;
                if (state != null) return state;
            } catch (RuntimeException ignored) {
                // Compatibility snapshot is used only when the authoritative state read is unavailable.
            }
        }
        return recognizedAssetStateFromSnapshot(compatibilitySnapshot);
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

    private String recognizedAssetStateValue(String value) {
        if (!hasText(value)) return null;
        return switch (value.trim().toUpperCase(Locale.ROOT)) {
            case "OBSERVING", "CANDIDATE", "WAITING_TRIGGER", "TRIGGERED",
                    "HIGH_RISK", "INVALIDATED", "COOLING", "CONFUSED" -> value.trim().toUpperCase(Locale.ROOT);
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
            case "CONFUSED" -> "冲突状态";
            default -> null;
        };
    }

    private String currentConclusion(DecisionResultVO decision, String assetState) {
        if (decision == null) return "等待分析";
        if (decision.getDataQualityScore() == null) return "等待数据质量同步";
        if (decision.getDataQualityScore() < MIN_DATA_QUALITY_SCORE_FOR_PLAN) {
            return "数据质量不足，暂不形成执行建议";
        }
        if ("CONFUSED".equals(assetState)) return "冲突状态，等待人工复核";
        if (riskRank(decision.getRiskLevel()) >= riskRank("HIGH")) return "风险较高，仅供人工复核";
        if (Boolean.TRUE.equals(decision.getIsWorthOpening())) return "条件满足，等待人工确认";
        return "当前条件不足，继续观察";
    }

    private PositionSelectionResult resolveSelectedPosition(List<DashboardHomeVO.PositionVO> positions,
                                                            String symbol,
                                                            Long selectedPositionId) {
        String normalized = normalizeSymbol(symbol);
        List<DashboardHomeVO.PositionVO> available = positions == null ? List.of() : positions;
        List<DashboardHomeVO.PositionVO> matching = normalized == null ? List.of() : available.stream()
                .filter(position -> normalized.equals(normalizeSymbol(position.getSymbol())))
                .toList();

        if (selectedPositionId != null) {
            if (selectedPositionId <= 0) {
                return PositionSelectionResult.blocked(
                        PositionSelectionStatus.POSITION_NOT_FOUND, matching.size());
            }
            DashboardHomeVO.PositionVO selected = available.stream()
                    .filter(position -> Objects.equals(position.getPositionId(), selectedPositionId))
                    .findFirst()
                    .orElse(null);
            if (selected == null) {
                return PositionSelectionResult.blocked(
                        PositionSelectionStatus.POSITION_NOT_FOUND, matching.size());
            }
            if (normalized == null || !normalized.equals(normalizeSymbol(selected.getSymbol()))) {
                return PositionSelectionResult.blocked(
                        PositionSelectionStatus.POSITION_SYMBOL_MISMATCH, matching.size());
            }
            return new PositionSelectionResult(
                    PositionSelectionStatus.EXACT_POSITION_SELECTED, selected, matching.size());
        }
        if (matching.isEmpty()) {
            return new PositionSelectionResult(PositionSelectionStatus.NO_POSITION, null, 0);
        }
        if (matching.size() == 1) {
            return new PositionSelectionResult(
                    PositionSelectionStatus.UNIQUE_POSITION_SELECTED, matching.get(0), 1);
        }
        return PositionSelectionResult.blocked(
                PositionSelectionStatus.POSITION_SELECTION_REQUIRED, matching.size());
    }

    private boolean planAllowedAssetState(String state) {
        return "CANDIDATE".equals(state) || "WAITING_TRIGGER".equals(state) || "TRIGGERED".equals(state);
    }

    private boolean directionalPushBlocked(DecisionResultVO decision) {
        if (decision == null) return false;
        if (decision.getConfusedScore() != null
                && decision.getConfusedScore() >= ConfusedStatePolicy.DIRECTIONAL_PUSH_BLOCK_THRESHOLD) {
            return true;
        }
        String snapshot = trimToNull(decision.getAssetStateSnapshot());
        if (snapshot == null) return false;
        try {
            JsonNode root = objectMapper.readTree(snapshot);
            return root != null && root.path("directionalPushBlocked").asBoolean(false);
        } catch (Exception ignored) {
            return false;
        }
    }

    private PlanValidity resolvePlanValidity(DecisionResultVO decision) {
        if (decision == null) return PlanValidity.invalid();
        OffsetDateTime structuredValidFrom = decision.getValidFrom();
        OffsetDateTime structuredExpiresAt = decision.getExpiresAt();
        if (structuredValidFrom != null || structuredExpiresAt != null) {
            if (structuredValidFrom == null || structuredExpiresAt == null) return PlanValidity.invalid();
            return evaluatePlanValidity(structuredValidFrom, structuredExpiresAt);
        }

        String value = trimToNull(decision.getValidPeriod());
        if (value == null) return PlanValidity.invalid();
        String normalized = upper(value);
        if (normalized.contains("EXPIRED") || normalized.contains("INVALIDATED")
                || value.contains("已失效") || value.contains("已过期")) {
            return PlanValidity.expired();
        }
        if (LEGACY_VALID_PERIOD_RANGE.matcher(value).matches()) {
            return PlanValidity.timezoneUnverified();
        }
        String[] range = value.split("\\s*~\\s*", -1);
        if (range.length != 2) return PlanValidity.invalid();
        try {
            return evaluatePlanValidity(OffsetDateTime.parse(range[0]), OffsetDateTime.parse(range[1]));
        } catch (DateTimeParseException ignored) {
            return PlanValidity.invalid();
        }
    }

    private PlanValidity evaluatePlanValidity(OffsetDateTime validFrom, OffsetDateTime expiresAt) {
        if (validFrom == null || expiresAt == null
                || !validFrom.toInstant().isBefore(expiresAt.toInstant())) {
            return PlanValidity.invalid();
        }
        java.time.Instant now = planValidityClock.instant();
        if (now.isBefore(validFrom.toInstant())) {
            return new PlanValidity(PlanValidityStatus.NOT_ACTIVE, validFrom, expiresAt);
        }
        if (!now.isBefore(expiresAt.toInstant())) {
            return new PlanValidity(PlanValidityStatus.EXPIRED, validFrom, expiresAt);
        }
        return new PlanValidity(PlanValidityStatus.ACTIVE, validFrom, expiresAt);
    }

    private String planValidityDisplay(DecisionResultVO decision, PlanValidity validity) {
        if (validity.validFrom() != null && validity.expiresAt() != null) {
            return OFFSET_PLAN_TIME_FORMAT.format(validity.validFrom())
                    + " ~ " + OFFSET_PLAN_TIME_FORMAT.format(validity.expiresAt());
        }
        String display = trimToNull(decision != null ? decision.getValidPeriod() : null);
        return display;
    }

    private SnapshotTraceStatus executionSnapshotTraceStatus(DecisionResultVO decision) {
        return executionSnapshotTraceStatus(decision, null);
    }

    private SnapshotTraceStatus executionSnapshotTraceStatus(DecisionResultVO decision, AnalysisRunDO resolvedRun) {
        if (decision == null || !hasText(decision.getAnalysisId()) || !hasText(decision.getSymbol())
                || assetStateMapper == null || (resolvedRun == null && analysisRunMapper == null)) {
            return SnapshotTraceStatus.UNVERIFIED;
        }
        try {
            AssetStateDO state = assetStateMapper.selectBySymbol(normalizeSymbol(decision.getSymbol()));
            AnalysisRunDO run = resolvedRun != null
                    ? resolvedRun
                    : analysisRunMapper.selectById(decision.getAnalysisId());
            String stateTraceId = state != null ? trimToNull(state.getTraceId()) : null;
            String decisionTraceId = run != null ? trimToNull(run.getTraceId()) : null;
            if (stateTraceId == null || decisionTraceId == null) return SnapshotTraceStatus.UNVERIFIED;
            return stateTraceId.equals(decisionTraceId) ? SnapshotTraceStatus.MATCH : SnapshotTraceStatus.MISMATCH;
        } catch (RuntimeException ignored) {
            return SnapshotTraceStatus.UNVERIFIED;
        }
    }

    private enum SnapshotTraceStatus { MATCH, MISMATCH, UNVERIFIED }
    private enum PlanValidityStatus { ACTIVE, NOT_ACTIVE, EXPIRED, INVALID, TIMEZONE_UNVERIFIED }
    private record PlanValidity(PlanValidityStatus status, OffsetDateTime validFrom, OffsetDateTime expiresAt) {
        private static PlanValidity invalid() {
            return new PlanValidity(PlanValidityStatus.INVALID, null, null);
        }

        private static PlanValidity expired() {
            return new PlanValidity(PlanValidityStatus.EXPIRED, null, null);
        }

        private static PlanValidity timezoneUnverified() {
            return new PlanValidity(PlanValidityStatus.TIMEZONE_UNVERIFIED, null, null);
        }
    }

    private String positionDirectionLabel(String direction) {
        return switch (upper(direction)) {
            case "LONG" -> "多头";
            case "SHORT" -> "空头";
            default -> "未知状态";
        };
    }

    private String positionStatusLabel(String status) {
        return switch (upper(status)) {
            case "OPEN" -> "持仓中";
            case "PARTIALLY_CLOSED" -> "部分记录平仓";
            case "CLOSED" -> "已记录平仓";
            default -> "未知状态";
        };
    }

    private String positionLogicStatusLabel(String status) {
        return switch (upper(status)) {
            case "LOGIC_VALID" -> "入场逻辑仍成立";
            case "LOGIC_WEAKENED" -> "入场逻辑减弱";
            case "PLAN_INVALIDATED" -> "原计划已失效";
            case "HIGH_RISK" -> "风险升高";
            case "WAITING_MONITOR" -> "等待首次监控";
            default -> "未知状态";
        };
    }

    private String directionSupportStatusLabel(String status) {
        return switch (upper(status)) {
            case "SUPPORTED" -> "当前方向仍获支持";
            case "WEAKENED" -> "方向支持减弱";
            case "NOT_SUPPORTED" -> "当前方向不再获支持";
            case "RISK_BLOCKED" -> "方向结论受风险阻断";
            case "WAITING_SYNC", "WAITING_MONITOR" -> "等待首次监控";
            default -> "未知状态";
        };
    }

    private String reversalStatusLabel(String status) {
        return switch (upper(status)) {
            case "NO_REVERSAL_SIGNAL" -> "暂无反转信号";
            case "MANUAL_REVIEW_REQUIRED" -> "需人工复核反转风险";
            case "RISK_REVIEW" -> "需复核高风险变化";
            case "WAITING_MONITOR" -> "等待首次监控";
            default -> "未知状态";
        };
    }

    private String positionRiskLevelLabel(String status) {
        if ("WAITING_MONITOR".equals(upper(status))) return "等待首次监控";
        String label = riskLabel(status);
        return label != null ? label : "未知状态";
    }

    private AiRoleStats aiRoleStats(AiRoleResultsPayload payload) {
        if (payload == null) return new AiRoleStats(0, 0, 0, 0);
        int successful = 0;
        int support = 0;
        int challenge = 0;
        int abstain = 0;
        for (AiRoleResultsPayload.RolePayload role : payload.roles().values()) {
            if (role == null || !"SUCCESS".equalsIgnoreCase(role.callStatus())) continue;
            successful++;
            switch (upper(role.stance())) {
                case "SUPPORT" -> support++;
                case "CHALLENGE" -> challenge++;
                case "ABSTAIN" -> abstain++;
                default -> {
                }
            }
        }
        return new AiRoleStats(successful, support, challenge, abstain);
    }

    private String consistencySummary(AiRoleStats stats) {
        if (stats.adjudicative() > 0) {
            return "基于本轮成功返回的 AI 角色形成一致性摘要";
        }
        if (stats.successful() > 0 && stats.abstain() == stats.successful()) {
            return "AI 成功返回，但所有角色均因证据不足而弃权";
        }
        if (stats.successful() > 0) {
            return "AI 成功返回，但未形成可裁决意见";
        }
        return "AI 未运行，本轮仅使用规则判断";
    }

    private String aiRunStatus(AiRoleResultsPayload payload, int successfulRoles) {
        if (payload == null) return "NOT_CALLED";
        if (successfulRoles >= AI_ROLES.size()) return "SUCCESS";
        if (successfulRoles > 0) return "PARTIAL_SUCCESS";
        List<String> statuses = payload.roles().values().stream()
                .filter(Objects::nonNull)
                .map(AiRoleResultsPayload.RolePayload::callStatus)
                .filter(Objects::nonNull)
                .map(this::upper)
                .toList();
        if (statuses.contains("BUDGET_BLOCKED")) return "BUDGET_BLOCKED";
        if (statuses.contains("RATE_LIMITED")) return "RATE_LIMITED";
        if (statuses.contains("TIMEOUT")) return "TIMEOUT";
        if (statuses.contains("INVALID_RESPONSE")) return "INVALID_RESPONSE";
        if (statuses.contains("FAILED")) return "FAILED";
        if (statuses.contains("MODEL_UNAVAILABLE")) return "MODEL_UNAVAILABLE";
        if (statuses.contains("NOT_CONFIGURED")) return "NOT_CONFIGURED";
        if (statuses.contains("DISABLED")) return "DISABLED";
        return "NOT_CALLED";
    }

    private String aiRunStatusLabel(String status) {
        return switch (upper(status)) {
            case "SUCCESS" -> "复核成功";
            case "PARTIAL_SUCCESS" -> "部分角色复核成功";
            case "SUPPORT" -> "成功支持";
            case "CHALLENGE" -> "成功反对";
            case "ABSTAIN" -> "成功弃权";
            case "DISABLED" -> "已禁用";
            case "TIMEOUT" -> "调用超时";
            case "FAILED" -> "调用失败";
            case "INVALID_RESPONSE" -> "返回内容无效";
            case "BUDGET_BLOCKED" -> "预算阻断";
            case "RATE_LIMITED" -> "调用受限";
            case "MODEL_UNAVAILABLE" -> "模型不可用";
            case "NOT_CONFIGURED" -> "未配置";
            case "NOT_CALLED", "STARTED" -> "未调用";
            default -> "未知状态";
        };
    }

    private String aiConflictLevelLabel(String level) {
        return switch (upper(level)) {
            case "LEVEL_1_CONSISTENT" -> "无显著分歧";
            case "LEVEL_2_LIGHT_DIVERGENCE", "LEVEL_2_REVIEW" -> "轻微分歧";
            case "LEVEL_3_SIGNIFICANT_DIVERGENCE", "LEVEL_3_DIVERGENCE" -> "显著分歧";
            case "LEVEL_4_EXTREME_DIVERGENCE" -> "极端分歧";
            default -> "未知状态";
        };
    }

    private String aiDowngradeReasonLabel(String reason) {
        if (!hasText(reason)) return null;
        return switch (upper(reason)) {
            case "EVENT_WINDOW_REVIEW" -> "外部事件窗口需人工复核";
            case "INSUFFICIENT_DATA" -> "证据不足，已降低结论强度";
            case "CONFLICT_TOO_HIGH" -> "冲突程度较高，需人工复核";
            case "GEMINI_CONTRADICTION_ONLY" -> "AI 发现证据冲突，需人工复核";
            default -> "AI 复核建议人工检查";
        };
    }

    private String aiStanceLabel(String stance) {
        return switch (upper(stance)) {
            case "SUPPORT" -> "支持规则结论";
            case "CHALLENGE" -> "提出反对意见";
            case "ABSTAIN" -> "证据不足，暂不判断";
            default -> null;
        };
    }

    private List<String> aiReasonLabels(List<String> reasons) {
        if (reasons == null || reasons.isEmpty()) return List.of();
        List<String> labels = new ArrayList<>();
        for (String reason : reasons) {
            String label = switch (upper(reason)) {
                case "RULE_DIRECTION_ALIGNED" -> "规则方向与 AI 复核一致";
                case "INSUFFICIENT_DATA" -> "证据不足";
                case "GEMINI_CONTRADICTION_ONLY" -> "AI 发现证据冲突";
                case "GROK_COUNTER_ONLY" -> "AI 提供反向证据";
                case "CONFLICT_TOO_HIGH" -> "冲突程度较高";
                default -> "AI 证据已记录，需人工复核";
            };
            if (!labels.contains(label)) labels.add(label);
        }
        return labels;
    }

    private String userFacingAiSummary(String summary) {
        String value = trimToNull(summary);
        if (value == null) return null;
        return value.matches(".*\\p{IsHan}.*")
                ? value
                : "AI 复核结果已返回，等待人工复核";
    }

    private String aiRoleStatusMessage(String status) {
        return switch (upper(status)) {
            case "SUCCESS" -> "根据角色结果展示";
            case "DISABLED" -> "AI 复核未启用";
            case "NOT_CALLED", "STARTED" -> "本轮未调用该角色";
            case "TIMEOUT" -> "AI 复核超时，本轮未采纳该角色";
            case "FAILED" -> "AI 复核失败，本轮未采纳该角色";
            case "INVALID_RESPONSE" -> "AI 返回内容无效，本轮未采纳";
            case "NOT_CONFIGURED" -> "AI 模型未配置";
            case "MODEL_UNAVAILABLE" -> "AI 模型不可用";
            case "BUDGET_BLOCKED" -> "AI 预算门控阻断";
            case "RATE_LIMITED" -> "AI 调用受限";
            default -> "本轮未调用该角色";
        };
    }

    private String planLeverageLabel(String leverageSuggestion) {
        String value = trimToNull(leverageSuggestion);
        if (value == null) return null;
        return switch (value.toLowerCase(Locale.ROOT)) {
            case "low_leverage" -> "低杠杆，仅供人工复核";
            case "moderate_leverage" -> "适中杠杆，仅供人工复核";
            default -> value.matches("[A-Za-z_ ]+") ? "未知状态" : value;
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
            case "WAIT" -> "观望";
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
            default -> null;
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
        return ExecutionPlanReviewPolicy.isConcreteBoundary(value) ? value.trim() : null;
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

    private record AiRoleStats(int successful, int support, int challenge, int abstain) {
        int adjudicative() {
            return support + challenge;
        }
    }

    private record PositionRowsResult(
            List<DashboardHomeVO.PositionVO> rows,
            Map<Long, PositionPlanSourceResolver.Resolution> trustedSources) {
    }

    private enum PositionSelectionStatus {
        NO_POSITION,
        UNIQUE_POSITION_SELECTED,
        EXACT_POSITION_SELECTED,
        POSITION_SELECTION_REQUIRED,
        POSITION_NOT_FOUND,
        POSITION_SYMBOL_MISMATCH
    }

    private record PositionSelectionResult(PositionSelectionStatus status,
                                           DashboardHomeVO.PositionVO selectedPosition,
                                           int matchingPositionCount) {
        private boolean blocked() {
            return status == PositionSelectionStatus.POSITION_SELECTION_REQUIRED
                    || status == PositionSelectionStatus.POSITION_NOT_FOUND
                    || status == PositionSelectionStatus.POSITION_SYMBOL_MISMATCH;
        }

        private static PositionSelectionResult blocked(PositionSelectionStatus status,
                                                       int matchingPositionCount) {
            return new PositionSelectionResult(status, null, matchingPositionCount);
        }
    }

    private record ResolvedOriginalPlan(String identityStatus,
                                        ExecutionPlanDO executionPlan,
                                        DecisionResultVO decision,
                                        AnalysisRunDO analysisRun,
                                        String analysisId,
                                        String executionPlanId,
                                        String traceId,
                                        String failureReason) {
        private boolean verified() {
            return "VERIFIED".equals(identityStatus);
        }

        private static ResolvedOriginalPlan unverified(String failureReason) {
            return new ResolvedOriginalPlan("UNVERIFIED", null, null, null,
                    null, null, null, failureReason);
        }
    }

    private record OriginalPlanPresentation(String status, String label, PlanValidity validity) {
    }

    private record PushInboxContext(DashboardHomeVO.PushInboxVO pushInbox, boolean readOk) {
    }
}
