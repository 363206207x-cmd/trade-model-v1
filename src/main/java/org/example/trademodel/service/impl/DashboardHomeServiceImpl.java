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
import org.example.trademodel.messagepush.MessageReadState;
import org.example.trademodel.messagepush.PublicOpportunityProjectionPolicy;
import org.example.trademodel.opportunitylog.OpportunityLogDTO;
import org.example.trademodel.opportunitylog.OpportunityLogPublicDTO;
import org.example.trademodel.providercall.AssetPriority;
import org.example.trademodel.providercall.ProviderCallResult;
import org.example.trademodel.providercall.snapshot.MarketPriceSnapshot;
import org.example.trademodel.providercall.snapshot.MarketPriceSnapshotPolicy;
import org.example.trademodel.providercall.snapshot.MarketPriceSnapshotService;
import org.example.trademodel.providercall.snapshot.DerivativesRiskSnapshot;
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
import org.example.trademodel.service.OpportunityLogService;
import org.example.trademodel.service.PositionMonitorLogService;
import org.example.trademodel.service.PositionSyncService;
import org.example.trademodel.service.UserPositionService;
import org.example.trademodel.service.readiness.ProviderReadinessService;
import org.example.trademodel.positionmonitorlog.PositionMonitorLogDTO;
import org.example.trademodel.positionmonitor.PositionPlanSourceResolver;
import org.example.trademodel.service.support.DataQualityCircuitBreakerPolicy;
import org.example.trademodel.service.support.ExecutionPlanReviewPolicy;
import org.example.trademodel.service.support.ExecutionPlanReviewPolicy.PersistedPlanState;
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
import java.time.Clock;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Comparator;
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
    private static final String BOUNDARY_INCOMPLETE_VALID_PERIOD = "边界不足，等待结构确认";
    private static final Pattern LEGACY_VALID_PERIOD_RANGE = Pattern.compile(
            "^(\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}:\\d{2})\\s*~\\s*"
                    + "(\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}:\\d{2})$");
    private static final DateTimeFormatter OFFSET_PLAN_TIME_FORMAT = DateTimeFormatter.ISO_OFFSET_DATE_TIME;

    private final DecisionService decisionService;
    private final MonitorService monitorService;
    private final UserPositionService userPositionService;
    private final PositionMonitorLogService positionMonitorLogService;
    private final PositionSyncService positionSyncService;
    private final OpportunityLogService opportunityLogService;
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
    private ExecutionPlanMapper executionPlanMapper;
    private PositionPlanSourceResolver positionPlanSourceResolver;
    private LocalRealReadinessService localRealReadinessService;
    private AssetStateMapper assetStateMapper;
    private Clock planValidityClock = Clock.systemUTC();

    public DashboardHomeServiceImpl(DecisionService decisionService,
                                    MonitorService monitorService,
                                    UserPositionService userPositionService,
                                    PositionMonitorLogService positionMonitorLogService,
                                    PositionSyncService positionSyncService,
                                    OpportunityLogService opportunityLogService,
                                    ExternalContextEvidenceBuilder externalContextEvidenceBuilder,
                                    ProviderReadinessService providerReadinessService,
                                    ObjectMapper objectMapper) {
        this(decisionService, monitorService, userPositionService, positionMonitorLogService, positionSyncService,
                opportunityLogService, externalContextEvidenceBuilder, providerReadinessService,
                objectMapper, null);
    }

    @Autowired
    public DashboardHomeServiceImpl(DecisionService decisionService,
                                    MonitorService monitorService,
                                    UserPositionService userPositionService,
                                    PositionMonitorLogService positionMonitorLogService,
                                    PositionSyncService positionSyncService,
                                    OpportunityLogService opportunityLogService,
                                    ExternalContextEvidenceBuilder externalContextEvidenceBuilder,
                                    ProviderReadinessService providerReadinessService,
                                    ObjectMapper objectMapper,
                                    MarketPriceSnapshotService marketPriceSnapshotService) {
        this.decisionService = decisionService;
        this.monitorService = monitorService;
        this.userPositionService = userPositionService;
        this.positionMonitorLogService = positionMonitorLogService;
        this.positionSyncService = positionSyncService;
        this.opportunityLogService = opportunityLogService;
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
        this.executionPlanMapper = executionPlanMapper;
        this.positionPlanSourceResolver = new PositionPlanSourceResolver(executionPlanMapper, analysisRunMapper);
    }

    void setPlanValidityClock(Clock planValidityClock) {
        this.planValidityClock = planValidityClock != null ? planValidityClock : Clock.systemUTC();
    }

    @Override
    public DashboardHomeVO getHome(String selectedSymbol, Integer limit, Long selectedPositionId) {
        return getHome(null, selectedSymbol, limit, selectedPositionId);
    }

    @Override
    public DashboardHomeVO getHomeForUser(Long userId, String selectedSymbol, Integer limit, Long selectedPositionId) {
        if (userId == null || userId <= 0) {
            throw new IllegalArgumentException("userId is required");
        }
        return getHome(userId, selectedSymbol, limit, selectedPositionId);
    }

    private DashboardHomeVO getHome(Long userId, String selectedSymbol, Integer limit, Long selectedPositionId) {
        int effectiveLimit = normalizeLimit(limit);
        LightSystemStatusVO systemStatus = safeSystemStatus();
        DecisionReadResult decisionRead = safeDecisionRead(userId, Math.max(effectiveLimit, DEFAULT_LIMIT));
        List<DecisionResultVO> decisions = decisionRead.rows();
        List<MonitorAlertDO> alerts = safeAlerts();
        PositionReadResult positionRead = safePositionRead(userId);
        List<UserPositionVO> positions = positionRead.rows();
        PositionSyncStatusVO positionSyncStatus = safePositionSyncStatus();
        ProviderReadinessVO providerReadiness = safeProviderReadiness();

        String normalizedRequest = normalizeSymbol(selectedSymbol);
        String normalizedSelected = normalizedRequest;
        if (normalizedSelected == null) {
            normalizedSelected = firstDecisionSymbol(decisions);
        }
        if (normalizedSelected == null) {
            normalizedSelected = DEFAULT_SYMBOLS.get(0);
        }

        DecisionResultVO selectedDecision = findDecision(decisions, normalizedSelected);
        boolean selectedDecisionReadFailed = false;
        if (selectedDecision == null) {
            DecisionLookupResult lookup = safeDecisionLookup(userId, normalizedSelected);
            selectedDecision = lookup.decision();
            selectedDecisionReadFailed = lookup.failed();
        }
        List<DashboardHomeVO.AssetVO> assets =
                buildAssets(decisions, selectedDecision, normalizedSelected, effectiveLimit);
        if (normalizedRequest == null && !hasRenderableAsset(assets, normalizedSelected)) {
            String firstRenderableSymbol = firstRenderableAssetSymbol(assets);
            if (firstRenderableSymbol != null) {
                normalizedSelected = firstRenderableSymbol;
                selectedDecision = findDecision(decisions, normalizedSelected);
                if (selectedDecision == null) {
                    DecisionLookupResult lookup = safeDecisionLookup(userId, normalizedSelected);
                    selectedDecision = lookup.decision();
                    selectedDecisionReadFailed = selectedDecisionReadFailed || lookup.failed();
                }
                assets = buildAssets(decisions, selectedDecision, normalizedSelected, effectiveLimit);
            }
        }

        ExternalContextSnapshot externalContext = safeExternalContext(normalizedSelected, selectedDecision);
        PushInboxContext pushInboxContext = buildPushInbox(positions, effectiveLimit);

        DashboardHomeVO.AiDecisionVO aiDecision = buildAiDecision(selectedDecision);
        DashboardHomeVO home = new DashboardHomeVO();
        home.setHeader(buildHeader(systemStatus, positionSyncStatus, externalContext, providerReadiness, aiDecision));
        home.setSystemState(buildSystemState(systemStatus, decisions, selectedDecision, aiDecision));
        home.setAlerts(buildAlerts(alerts));
        home.setEvents(buildEvents(externalContext));
        home.setAssets(assets);
        PositionRowsResult positionRowsResult = buildPositions(userId, positions);
        home.setPositions(positionRowsResult.topRows());
        home.setSelectedSymbol(normalizedSelected);
        PositionSelectionResult positionSelection = resolveSelectedPosition(positionRowsResult.allRows(), selectedPositionId);
        DashboardHomeVO.PositionVO activePosition = positionSelection.selectedPosition();
        home.setSelectedPositionId(activePosition != null ? activePosition.getPositionId() : null);
        home.setPositionSelectionStatus(positionSelection.status().name());
        home.setMatchingPositionCount(positionSelection.matchingPositionCount());
        DashboardHomeVO.ExecutionSuggestionVO executionSuggestion = buildExecutionSuggestion(selectedDecision);
        home.setExecutionSuggestion(executionSuggestion);
        home.setAiDecision(aiDecision);
        home.setPushInbox(pushInboxContext.pushInbox());
        home.setDerivatives(buildDerivativesSummary(normalizedSelected, selectedDecision));
        home.setDiagnostics(buildDiagnostics(systemStatus, decisions, selectedDecision, positionSyncStatus,
                pushInboxContext, providerReadiness));
        home.setSafety(new DashboardHomeVO.SafetyVO());
        DashboardHomeVO.AssetVO selectedAsset = findHomeAsset(assets, normalizedSelected);
        DashboardHomeVO.ModuleStatesVO moduleStates = buildModuleStates(
                selectedAsset, executionSuggestion, positionRowsResult, aiDecision,
                decisionRead.failed() || selectedDecisionReadFailed, positionRead.failed());
        home.setStates(moduleStates);
        home.getHeader().setDataStatus(moduleStates.getOverall());
        home.getHeader().setUpdatedAt(selectedAsset != null ? selectedAsset.getUpdatedAt() : null);
        return home;
    }

    private DashboardHomeVO.ModuleStatesVO buildModuleStates(
            DashboardHomeVO.AssetVO selectedAsset,
            DashboardHomeVO.ExecutionSuggestionVO executionSuggestion,
            PositionRowsResult positionRows,
            DashboardHomeVO.AiDecisionVO aiDecision,
            boolean assetReadFailed,
            boolean positionReadFailed) {
        DashboardHomeVO.ModuleStatesVO states = new DashboardHomeVO.ModuleStatesVO();
        String assetsState = assetReadFailed
                ? "ERROR"
                : selectedAsset == null ? "MISSING" : normalizedModuleState(selectedAsset.getModuleState(), "MISSING");
        String positionsState;
        if (positionReadFailed) {
            positionsState = "ERROR";
        } else if (positionRows == null || positionRows.allRows().isEmpty()) {
            positionsState = "EMPTY";
        } else if (positionRows.allRows().stream().anyMatch(row -> "ERROR".equals(row.getModuleState()))) {
            positionsState = "ERROR";
        } else if (positionRows.allRows().stream().anyMatch(row -> !"READY".equals(row.getModuleState()))) {
            positionsState = "PARTIAL";
        } else {
            positionsState = "READY";
        }
        String executionState = executionSuggestion == null
                ? "MISSING" : normalizedModuleState(executionSuggestion.getModuleState(), "MISSING");
        String aiState = aiModuleState(aiDecision);
        String consistencyState = consistencyModuleState(aiDecision, aiState);

        states.setAssets(assetsState);
        states.setExecutionPlan(executionState);
        states.setPositions(positionsState);
        states.setAi(aiState);
        states.setConsistency(consistencyState);
        if ("ERROR".equals(assetsState)) {
            states.setOverall("ERROR");
        } else if ("MISSING".equals(assetsState)) {
            states.setOverall("MISSING");
        } else if ("EMPTY".equals(assetsState)) {
            states.setOverall("EMPTY");
        } else if (List.of(executionState, positionsState, aiState, consistencyState).stream()
                .anyMatch(state -> "ERROR".equals(state) || "PARTIAL".equals(state) || "MISSING".equals(state))) {
            states.setOverall("PARTIAL");
        } else {
            states.setOverall("READY");
        }
        return states;
    }

    private DashboardHomeVO.AssetVO findHomeAsset(List<DashboardHomeVO.AssetVO> assets, String symbol) {
        String normalized = normalizeSymbol(symbol);
        if (normalized == null || assets == null) return null;
        return assets.stream()
                .filter(this::isRenderableAsset)
                .filter(asset -> normalized.equals(normalizeSymbol(asset.getRawSymbol())))
                .findFirst()
                .orElse(null);
    }

    private String aiModuleState(DashboardHomeVO.AiDecisionVO aiDecision) {
        String status = upper(aiDecision != null ? aiDecision.getRunStatus() : null);
        return switch (status) {
            case "SUCCESS" -> "READY";
            case "PARTIAL_SUCCESS", "STARTED" -> "PARTIAL";
            case "FAILED", "INVALID_RESPONSE", "TIMEOUT", "MODEL_UNAVAILABLE", "RATE_LIMITED" -> "ERROR";
            case "DISABLED", "NOT_CONFIGURED", "NOT_CALLED", "" -> "MISSING";
            default -> "ERROR";
        };
    }

    private String consistencyModuleState(DashboardHomeVO.AiDecisionVO aiDecision, String aiState) {
        if ("ERROR".equals(aiState)) return "ERROR";
        if (aiDecision == null || aiDecision.getConsistency() == null
                || !hasText(aiDecision.getConsistency().getConsistencySummary())) {
            return "MISSING";
        }
        return "READY".equals(aiState) ? "READY" : "PARTIAL";
    }

    private String normalizedModuleState(String state, String fallback) {
        return switch (upper(state)) {
            case "LOADING", "READY", "PARTIAL", "EMPTY", "ERROR", "MISSING" -> upper(state);
            default -> fallback;
        };
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
        header.setUpdatedAt(null);
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
        state.setPendingReview(card(
                "pendingReview",
                "待复核机会",
                null,
                "当前不可查看",
                "私有复核统计不可用于共享首页",
                "PRIVATE_SOURCE_UNAVAILABLE",
                null
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

    private List<DashboardHomeVO.AssetVO> buildAssets(List<DecisionResultVO> decisions,
                                                      DecisionResultVO selectedDecision,
                                                      String selectedSymbol,
                                                      int limit) {
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
        String selectedDecisionSymbol = selectedDecision == null
                ? null : normalizeSymbol(selectedDecision.getSymbol());
        if (selectedDecisionSymbol != null && !used.contains(selectedDecisionSymbol)) {
            if (assets.size() >= limit) {
                DashboardHomeVO.AssetVO removed = assets.remove(assets.size() - 1);
                used.remove(removed.getRawSymbol());
            }
            used.add(selectedDecisionSymbol);
            assets.add(assetFromDecision(assets.size() + 1, selectedDecision));
        }
        String normalizedSelected = normalizeSymbol(selectedSymbol);
        LinkedHashSet<String> fallbackSymbols = new LinkedHashSet<>();
        if (normalizedSelected != null && !used.contains(normalizedSelected)) {
            fallbackSymbols.add(normalizedSelected);
        }
        fallbackSymbols.addAll(DEFAULT_SYMBOLS);

        List<DashboardHomeVO.AssetVO> emptyFallbacks = new ArrayList<>();
        for (String symbol : fallbackSymbols) {
            boolean selectedFallback = symbol.equals(normalizedSelected);
            if (assets.size() >= limit && !selectedFallback) {
                break;
            }
            if (used.contains(symbol)) {
                continue;
            }
            DashboardHomeVO.AssetVO fallback = assetPlaceholder(0, symbol);
            if ("DEFAULT_SLOT".equals(fallback.getSlotType())) {
                emptyFallbacks.add(fallback);
                continue;
            }
            if (selectedFallback && assets.size() >= limit) {
                DashboardHomeVO.AssetVO removed = assets.remove(assets.size() - 1);
                used.remove(removed.getRawSymbol());
            }
            if (assets.size() < limit) {
                fallback.setSlot(assets.size() + 1);
                used.add(symbol);
                assets.add(fallback);
            }
        }
        for (DashboardHomeVO.AssetVO fallback : emptyFallbacks) {
            if (assets.size() >= limit) {
                break;
            }
            if (!used.add(fallback.getRawSymbol())) {
                continue;
            }
            fallback.setSlot(assets.size() + 1);
            assets.add(fallback);
        }
        return assets;
    }

    private boolean hasRenderableAsset(List<DashboardHomeVO.AssetVO> assets, String symbol) {
        String normalized = normalizeSymbol(symbol);
        return normalized != null && assets.stream()
                .anyMatch(asset -> isRenderableAsset(asset)
                        && normalized.equals(normalizeSymbol(asset.getRawSymbol())));
    }

    private String firstRenderableAssetSymbol(List<DashboardHomeVO.AssetVO> assets) {
        return assets.stream()
                .filter(this::isRenderableAsset)
                .map(DashboardHomeVO.AssetVO::getRawSymbol)
                .map(this::normalizeSymbol)
                .filter(Objects::nonNull)
                .findFirst()
                .orElse(null);
    }

    private boolean isRenderableAsset(DashboardHomeVO.AssetVO asset) {
        return asset != null
                && hasText(asset.getRawSymbol())
                && !"DEFAULT_SLOT".equalsIgnoreCase(asset.getSlotType());
    }

    private DashboardHomeVO.AssetVO assetFromDecision(int slot, DecisionResultVO decision) {
        DashboardHomeVO.AssetVO asset = assetBase(slot, normalizeSymbol(decision.getSymbol()));
        asset.setSlotType("DECISION");
        asset.setAnalysisId(authoritativeAnalysisId(decision, asset));
        asset.setMarketBias(trimToNull(decision.getMarketBiasHierarchy()));
        asset.setMarketBiasLabel(biasLabel(decision.getMarketBiasHierarchy()));
        setFieldSource(asset, "direction", hasText(asset.getMarketBias()) ? "DERIVED" : "MISSING");
        applyPersistedMarketData(asset, normalizeSymbol(decision.getSymbol()));
        if (analysisRunMapper != null && hasText(decision.getAnalysisId())) {
            try {
                Double average = analysisRunMapper.selectAverageScoreByAnalysisId(decision.getAnalysisId());
                asset.setCompositeScore(average == null ? null : (int) Math.round(average));
                asset.setEvidenceCount(analysisRunMapper.countEvidenceByAnalysisId(decision.getAnalysisId()));
                setFieldSource(asset, "score", average == null ? "MISSING" : "DERIVED");
            } catch (RuntimeException ignored) {
                asset.setCompositeScore(null);
                asset.setEvidenceCount(null);
                setFieldSource(asset, "score", "ERROR");
            }
        } else {
            setFieldSource(asset, "score", "MISSING");
        }
        asset.setLatestAnalysisTime(decision.getCreateTime());
        asset.setConfidenceLevel(trimToNull(decision.getConfidenceLevel()));
        asset.setConfidenceLabel(confidenceLabel(decision.getConfidenceLevel()));
        setFieldSource(asset, "confidence", hasText(asset.getConfidenceLevel()) ? "DERIVED" : "MISSING");
        asset.setRiskLevel(trimToNull(decision.getRiskLevel()));
        asset.setRiskLabel(riskLabel(decision.getRiskLevel()));
        setFieldSource(asset, "riskLevel", hasText(asset.getRiskLevel()) ? "DERIVED" : "MISSING");
        AssetStateResolution stateResolution = authoritativeAssetStateResolution(
                normalizeSymbol(decision.getSymbol()), decision.getAssetStateSnapshot());
        String assetState = stateResolution.value();
        asset.setAssetState(assetState);
        asset.setAssetStateLabel(assetStateLabel(assetState));
        setFieldSource(asset, "assetState", stateResolution.sourceStatus());
        asset.setMultiTimeframeState(trimToNull(decision.getMultiTfConvergence()));
        setFieldSource(asset, "multiTimeframeState",
                hasText(asset.getMultiTimeframeState()) ? "DERIVED" : "MISSING");
        if (assetState != null || decision.getConfusedScore() != null) {
            asset.setConfused("CONFUSED".equals(assetState)
                    || decision.getConfusedScore() != null
                    && decision.getConfusedScore() >= ConfusedStatePolicy.CONFUSED_ENTER_THRESHOLD);
            setFieldSource(asset, "confused", "DERIVED");
        } else {
            asset.setConfused(null);
            setFieldSource(asset, "confused", "MISSING");
        }
        applyDataQuality(asset, decision);
        asset.setUpdatedAt(maxTime(asset.getUpdatedAt(), decision.getCreateTime()));
        if (asset.getUpdatedAt() != null) {
            setFieldSource(asset, "updatedAt", "REAL");
        } else if (!"ERROR".equals(asset.getFieldSourceStatus().get("updatedAt"))) {
            setFieldSource(asset, "updatedAt", "MISSING");
        }
        asset.setWorthOpening(decision.getIsWorthOpening());
        asset.setCurrentConclusion(currentConclusion(decision, assetState));
        asset.setModuleState(assetModuleState(asset));
        return asset;
    }

    private String authoritativeAnalysisId(DecisionResultVO decision, DashboardHomeVO.AssetVO asset) {
        if (decision == null || asset == null || analysisRunMapper == null
                || !hasText(decision.getAnalysisId())) {
            return null;
        }
        String assetSymbol = normalizeSymbol(asset.getSymbol());
        if (assetSymbol == null) {
            return null;
        }
        try {
            AnalysisRunDO run = analysisRunMapper.selectById(decision.getAnalysisId());
            if (run == null
                    || !decision.getAnalysisId().equals(run.getAnalysisId())
                    || !hasText(run.getSymbol())
                    || !assetSymbol.equals(normalizeSymbol(run.getSymbol()))) {
                return null;
            }
            return decision.getAnalysisId();
        } catch (RuntimeException ignored) {
            return null;
        }
    }

    private DashboardHomeVO.AssetVO assetPlaceholder(int slot, String symbol) {
        DashboardHomeVO.AssetVO asset = assetBase(slot, symbol);
        applyPersistedMarketData(asset, symbol);
        asset.setSlotType(asset.getLatestPrice() == null ? "DEFAULT_SLOT" : "MARKET_DATA");
        applyDataQuality(asset, null);
        asset.setModuleState(asset.getLatestPrice() == null ? "MISSING" : "PARTIAL");
        return asset;
    }

    private void applyPersistedMarketData(DashboardHomeVO.AssetVO asset, String symbol) {
        if (asset == null || persistedOhlcvBarMapper == null || !hasText(symbol)) {
            if (asset != null) {
                setFieldSource(asset, "latestPrice", "MISSING");
                setFieldSource(asset, "updatedAt", "MISSING");
            }
            return;
        }
        Map<String, String> timeframeFreshness = new LinkedHashMap<>();
        PersistedOhlcvBarDO latest = null;
        try {
            for (String timeframe : List.of("5m", "15m", "1h", "4h")) {
                List<PersistedOhlcvBarDO> rows = persistedOhlcvBarMapper.selectLatestClosedWindow(symbol, timeframe, 1);
                PersistedOhlcvBarDO timeframeLatest = rows == null || rows.isEmpty() ? null : rows.get(0);
                timeframeFreshness.put(timeframe, timeframeLatest == null
                        ? "NO_DATA" : firstNonBlank(timeframeLatest.getFreshnessStatus(), "UNKNOWN"));
                if ("5m".equals(timeframe)) latest = timeframeLatest;
            }
        } catch (RuntimeException ignored) {
            asset.setDataFreshness("ERROR");
            asset.setDataQuality("ERROR");
            asset.setTimeframeFreshness(Map.copyOf(timeframeFreshness));
            setFieldSource(asset, "latestPrice", "ERROR");
            setFieldSource(asset, "dataQuality", "ERROR");
            setFieldSource(asset, "updatedAt", "ERROR");
            return;
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
            setFieldSource(asset, "latestPrice", "MISSING");
            setFieldSource(asset, "updatedAt", "MISSING");
            return;
        }
        if (!positive(latest.getClosePrice())) {
            asset.setDataFreshness("ERROR");
            asset.setDataQuality("ERROR");
            setFieldSource(asset, "latestPrice", "ERROR");
            setFieldSource(asset, "dataQuality", "ERROR");
        } else {
            asset.setLatestPrice(latest.getClosePrice());
            setFieldSource(asset, "latestPrice", "REAL");
        }
        boolean allFresh = timeframeFreshness.values().stream().allMatch("FRESH"::equalsIgnoreCase);
        boolean anyData = timeframeFreshness.values().stream().anyMatch(value -> !"NO_DATA".equals(value));
        if (!"ERROR".equals(asset.getDataFreshness())) {
            asset.setDataFreshness(allFresh ? "FRESH" : anyData ? "PARTIAL" : "NO_DATA");
        }
        asset.setSourceProvider(providerLabel(latest.getProvider()));
        asset.setUpdatedAt(latestBusinessTime(latest));
        setFieldSource(asset, "updatedAt", asset.getUpdatedAt() == null ? "MISSING" : "REAL");
    }

    private DashboardHomeVO.AssetVO assetBase(int slot, String normalizedSymbol) {
        DashboardHomeVO.AssetVO asset = new DashboardHomeVO.AssetVO();
        asset.setSlot(slot);
        asset.setRawSymbol(normalizedSymbol);
        asset.setSymbol(toDisplaySymbol(normalizedSymbol));
        setFieldSource(asset, "symbol", hasText(normalizedSymbol) ? "REAL" : "MISSING");
        for (String field : List.of("latestPrice", "direction", "score", "confidence", "riskLevel",
                "assetState", "dataQuality", "multiTimeframeState", "confused", "updatedAt")) {
            setFieldSource(asset, field, "MISSING");
        }
        return asset;
    }

    private void applyDataQuality(DashboardHomeVO.AssetVO asset, DecisionResultVO decision) {
        if (asset == null || "ERROR".equals(asset.getDataQuality())) return;
        Map<String, String> freshness = asset.getTimeframeFreshness();
        if (asset.getLatestPrice() == null) {
            asset.setDataQuality("MISSING");
        } else if (freshness != null && freshness.values().stream().anyMatch("STALE"::equalsIgnoreCase)) {
            asset.setDataQuality("STALE");
        } else {
            boolean allFresh = freshness != null && freshness.size() == 4
                    && freshness.values().stream().allMatch("FRESH"::equalsIgnoreCase);
            boolean qualityScoreGood = decision != null
                    && DataQualityCircuitBreakerPolicy.passes(decision.getDataQualityScore());
            asset.setDataQuality(allFresh && qualityScoreGood ? "GOOD" : "PARTIAL");
        }
        setFieldSource(asset, "dataQuality", "MISSING".equals(asset.getDataQuality()) ? "MISSING" : "DERIVED");
    }

    private String assetModuleState(DashboardHomeVO.AssetVO asset) {
        if (asset == null) return "MISSING";
        Map<String, String> sources = asset.getFieldSourceStatus();
        if (sources.values().stream().anyMatch("ERROR"::equals)) return "ERROR";
        List<String> core = List.of("symbol", "latestPrice", "direction", "score", "confidence", "riskLevel", "assetState");
        long availableCore = core.stream().filter(field -> !"MISSING".equals(sources.get(field))).count();
        if (availableCore <= 1) return "MISSING";
        boolean complete = core.stream().allMatch(field -> {
            String source = sources.get(field);
            return source != null && !"MISSING".equals(source) && !"FALLBACK".equals(source);
        });
        boolean secondaryComplete = List.of("dataQuality", "multiTimeframeState", "confused", "updatedAt")
                .stream().allMatch(field -> !"MISSING".equals(sources.get(field)));
        return complete && secondaryComplete && "GOOD".equals(asset.getDataQuality()) ? "READY" : "PARTIAL";
    }

    private void setFieldSource(DashboardHomeVO.AssetVO asset, String field, String sourceStatus) {
        if (asset == null || field == null) return;
        Map<String, String> updated = new LinkedHashMap<>(asset.getFieldSourceStatus());
        updated.put(field, sourceStatus);
        asset.setFieldSourceStatus(Map.copyOf(updated));
    }

    private LocalDateTime latestBusinessTime(PersistedOhlcvBarDO row) {
        if (row == null) return null;
        LocalDateTime closeTime = row.getCloseTimeMs() != null && row.getCloseTimeMs() > 0
                ? LocalDateTime.ofInstant(Instant.ofEpochMilli(row.getCloseTimeMs()), ZoneOffset.UTC) : null;
        return maxTime(row.getUpdatedAt(), row.getIngestedAt(), row.getFetchTime(), closeTime);
    }

    private LocalDateTime maxTime(LocalDateTime... values) {
        LocalDateTime latest = null;
        if (values == null) return null;
        for (LocalDateTime value : values) {
            if (value != null && (latest == null || value.isAfter(latest))) latest = value;
        }
        return latest;
    }

    private PositionRowsResult buildPositions(Long userId, List<UserPositionVO> positions) {
        List<DashboardHomeVO.PositionVO> rows = new ArrayList<>();
        Map<Long, PositionPlanSourceResolver.Resolution> trustedSources = new LinkedHashMap<>();
        for (UserPositionVO position : positions == null ? List.<UserPositionVO>of() : positions) {
            if (!isActiveManualPosition(position)) {
                continue;
            }
            MonitorReadResult monitorRead = latestPositionMonitorLog(userId, position.getId());
            PositionMonitorLogDTO latestMonitorLog = monitorRead.log();
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
            row.setEntryLogicStatus(latestMonitorLog != null ? trimToNull(latestMonitorLog.getLogicStatus()) : null);
            row.setEntryLogicStatusLabel(positionLogicStatusLabel(row.getEntryLogicStatus()));
            row.setDirectionSupportStatus(directionSupportStatus(latestMonitorLog));
            row.setDirectionSupportStatusLabel(directionSupportStatusLabel(row.getDirectionSupportStatus()));
            row.setReversalStatus(reversalStatus(latestMonitorLog));
            row.setReversalStatusLabel(reversalStatusLabel(row.getReversalStatus()));
            row.setRiskLevel(latestMonitorLog != null ? trimToNull(latestMonitorLog.getRiskLevel()) : null);
            row.setRiskLevelLabel(positionRiskLevelLabel(row.getRiskLevel()));
            row.setSuggestedManualAction(latestMonitorLog != null ? trimToNull(latestMonitorLog.getSuggestedAction()) : null);
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
            row.setWarningState(positionWarningState(latestMonitorLog, monitorRead.failed()));
            row.setModuleState(positionModuleState(row, latestMonitorLog, monitorRead.failed()));
            rows.add(row);
        }
        rows.sort(positionPriorityComparator());
        List<DashboardHomeVO.PositionVO> allRows = List.copyOf(rows);
        List<DashboardHomeVO.PositionVO> topRows = allRows.stream().limit(3).toList();
        return new PositionRowsResult(allRows, topRows, Map.copyOf(trustedSources));
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

    private MonitorReadResult latestPositionMonitorLog(Long userId, Long positionId) {
        if (userId == null || userId <= 0 || positionId == null) {
            return new MonitorReadResult(null, false);
        }
        try {
            List<PositionMonitorLogDTO> logs = positionMonitorLogService
                    .listByPositionIdForUser(userId, positionId, 1);
            if (logs == null || logs.isEmpty()) {
                return new MonitorReadResult(null, false);
            }
            return new MonitorReadResult(logs.get(0), false);
        } catch (RuntimeException ignored) {
            return new MonitorReadResult(null, true);
        }
    }

    private Comparator<DashboardHomeVO.PositionVO> positionPriorityComparator() {
        return Comparator
                .comparingInt((DashboardHomeVO.PositionVO row) -> positionWarningRank(row.getWarningState())).reversed()
                .thenComparing(row -> maxTime(row.getLastMonitorAt(), row.getUpdatedAt()),
                        Comparator.nullsLast(Comparator.reverseOrder()))
                .thenComparing(DashboardHomeVO.PositionVO::getPositionId,
                        Comparator.nullsLast(Comparator.naturalOrder()));
    }

    private int positionWarningRank(String warningState) {
        return switch (upper(warningState)) {
            case "ERROR" -> 5;
            case "HIGH_RISK" -> 4;
            case "PLAN_INVALIDATED" -> 3;
            case "WATCH" -> 2;
            case "MISSING" -> 1;
            default -> 0;
        };
    }

    private String positionWarningState(PositionMonitorLogDTO monitor, boolean readFailed) {
        if (readFailed) return "ERROR";
        if (monitor == null) return "MISSING";
        String risk = upper(monitor.getRiskLevel());
        String logic = upper(monitor.getLogicStatus());
        if ("HIGH".equals(risk) || "EXTREME".equals(risk) || "HIGH_RISK".equals(logic)) return "HIGH_RISK";
        if ("PLAN_INVALIDATED".equals(logic)) return "PLAN_INVALIDATED";
        if ("LOGIC_WEAKENED".equals(logic)) return "WATCH";
        return "NONE";
    }

    private String positionModuleState(DashboardHomeVO.PositionVO row,
                                       PositionMonitorLogDTO monitor,
                                       boolean readFailed) {
        if (readFailed) return "ERROR";
        if (row == null || row.getPositionId() == null || !hasText(row.getSymbol())
                || !hasText(row.getDirection()) || !positive(row.getEntryPrice())
                || !positive(row.getPositionSize()) || !hasText(row.getPositionStatus())) {
            return "ERROR";
        }
        if (monitor != null && (!recognizedPositionLogic(monitor.getLogicStatus())
                || !recognizedPositionRisk(monitor.getRiskLevel()))) {
            return "ERROR";
        }
        if (monitor == null || row.getLeverage() == null || row.getUserStopLoss() == null
                || row.getUserTakeProfit() == null || row.getUpdatedAt() == null) {
            return "PARTIAL";
        }
        return "READY";
    }

    private boolean recognizedPositionLogic(String logicStatus) {
        return switch (upper(logicStatus)) {
            case "LOGIC_VALID", "LOGIC_WEAKENED", "PLAN_INVALIDATED", "HIGH_RISK" -> true;
            default -> false;
        };
    }

    private boolean recognizedPositionRisk(String riskLevel) {
        return switch (upper(riskLevel)) {
            case "LOW", "MEDIUM", "HIGH", "EXTREME" -> true;
            default -> false;
        };
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

    private DashboardHomeVO.ExecutionSuggestionVO buildExecutionSuggestion(DecisionResultVO selectedDecision) {
        DashboardHomeVO.ExecutionSuggestionVO suggestion = new DashboardHomeVO.ExecutionSuggestionVO();
        suggestion.setPositionMode(false);
        suggestion.setPositionMonitor(null);
        DecisionResultVO decision = selectedDecision;
        if (decision == null) {
            blockSuggestion(suggestion, "NO_COMPLETE_PLAN", "当前暂无完整执行计划", "暂无有效分析快照");
            return suggestion;
        }
        suggestion.setSourceAnalysisId(trimToNull(decision.getAnalysisId()));

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
        if (DataQualityCircuitBreakerPolicy.isBlocked(decision.getDataQualityScore())) {
            blockSuggestion(suggestion, "DATA_QUALITY_BLOCKED", "当前暂无完整执行计划",
                    "数据质量不足，暂不交易 / 事件观望");
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
        if (!hasText(decision.getDecisionId())) {
            blockSuggestion(suggestion, "PLAN_IDENTITY_MISSING", "当前暂无完整执行计划",
                    "决策缺少精确身份，不能关联执行计划");
            return suggestion;
        }
        AssetExecutionPlanResolution assetPlan = resolveAssetExecutionPlan(decision);
        if (assetPlan.state() == ExactPlanIdentityState.MISSING) {
            blockSuggestion(suggestion, "PLAN_IDENTITY_MISSING", "当前暂无完整执行计划",
                    assetPlan.reason());
            return suggestion;
        }
        if (assetPlan.state() == ExactPlanIdentityState.ERROR || !assetPlan.verified()) {
            blockSuggestion(suggestion, "PLAN_IDENTITY_ERROR", "当前执行计划不可用",
                    assetPlan.reason());
            return suggestion;
        }
        ExecutionPlanDO executionPlan = assetPlan.executionPlan();
        boolean boundaryComplete = trimPlanValue(executionPlan.getEntryZone()) != null
                && trimPlanValue(executionPlan.getStopLoss()) != null
                && trimPlanValue(executionPlan.getTakeProfitRules()) != null;
        if (!boundaryComplete) {
            blockSuggestion(suggestion, "BOUNDARY_INCOMPLETE", "当前暂无完整执行计划",
                    BOUNDARY_INCOMPLETE_VALID_PERIOD);
            return suggestion;
        }
        PersistedPlanState planState = ExecutionPlanReviewPolicy.persistedPlanState(executionPlan);
        if (planState != PersistedPlanState.ACTIVE) {
            blockPersistedAssetPlan(suggestion, executionPlan, planState);
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
        suggestion.setModuleState("READY");
        suggestion.setSourceAnalysisId(assetPlan.analysisId());
        suggestion.setSourceExecutionPlanId(assetPlan.executionPlanId());
        suggestion.setSourceTraceId(assetPlan.sourceTraceId());
        suggestion.setDirection(trimToNull(decision.getMarketBiasHierarchy()));
        suggestion.setEntryZone(trimPlanValue(executionPlan.getEntryZone()));
        suggestion.setStopLoss(trimPlanValue(executionPlan.getStopLoss()));
        suggestion.setTakeProfitRules(trimPlanValue(executionPlan.getTakeProfitRules()));
        suggestion.setLeverageSuggestion(planLeverageLabel(executionPlan.getLeverageSuggestion()));
        suggestion.setPositionSuggestion(trimToNull(executionPlan.getPositionSuggestion()));
        suggestion.setValidPeriod(planValidityDisplay(decision, planValidity));
        suggestion.setValidFrom(planValidity.validFrom());
        suggestion.setExpiresAt(planValidity.expiresAt());
        suggestion.setInvalidCondition(firstNonBlank(
                trimPlanValue(executionPlan.getInvalidCondition()),
                trimToNull(decision.getInvalidCondition())));
        return suggestion;
    }

    private void blockPersistedAssetPlan(DashboardHomeVO.ExecutionSuggestionVO suggestion,
                                         ExecutionPlanDO executionPlan,
                                         PersistedPlanState planState) {
        switch (planState) {
            case MISSING -> blockSuggestion(suggestion, "PLAN_MISSING", "当前暂无完整执行计划",
                    "执行计划不存在或当前不可查看");
            case INVALID -> blockSuggestion(suggestion, "PLAN_INVALID", "当前执行计划不可用",
                    "执行计划已失效，等待重新分析");
            case BLOCKED -> blockSuggestion(suggestion, "PLAN_BLOCKED", "当前执行计划已阻断",
                    "执行计划未通过来源或风险门控");
            case REVALIDATION_REQUIRED -> blockSuggestion(suggestion, "REVALIDATION_REQUIRED",
                    "执行计划需要重新验证", revalidationReviewCopy(executionPlan));
            case REVIEW_ONLY -> blockSuggestion(suggestion, "PLAN_REVIEW_ONLY",
                    "当前计划仅供历史复核", "该计划不能作为当前资产的可用计划");
            case INCOMPLETE -> blockSuggestion(suggestion, "PLAN_INCOMPLETE", "当前暂无完整执行计划",
                    "执行计划状态、来源或边界信息不完整");
            case ACTIVE -> blockSuggestion(suggestion, "PLAN_STATE_ERROR", "当前执行计划状态异常",
                    "执行计划状态校验不一致");
        }
    }

    private AssetExecutionPlanResolution resolveAssetExecutionPlan(DecisionResultVO decision) {
        String analysisId = trimToNull(decision != null ? decision.getAnalysisId() : null);
        String decisionId = trimToNull(decision != null ? decision.getDecisionId() : null);
        String symbol = normalizeSymbol(decision != null ? decision.getSymbol() : null);
        if (analysisId == null || decisionId == null || symbol == null) {
            return AssetExecutionPlanResolution.missing("决策或分析精确身份缺失");
        }
        if (executionPlanMapper == null || analysisRunMapper == null || opportunityLogService == null) {
            return AssetExecutionPlanResolution.error("执行计划精确身份读取能力不可用");
        }
        try {
            List<OpportunityLogDTO> relations = opportunityLogService.queryForSystem(
                    analysisId, decisionId, null, symbol, null, null, null, null, 2);
            if (relations == null || relations.isEmpty()) {
                return AssetExecutionPlanResolution.missing("未找到决策与执行计划的精确持久化关系");
            }
            LinkedHashSet<String> planIds = new LinkedHashSet<>();
            for (OpportunityLogDTO relation : relations) {
                if (relation == null
                        || !analysisId.equals(trimToNull(relation.getAnalysisId()))
                        || !decisionId.equals(trimToNull(relation.getDecisionId()))
                        || !symbol.equals(normalizeSymbol(relation.getSymbol()))
                        || !"AUTHORITATIVE_ANALYSIS".equalsIgnoreCase(trimToNull(relation.getSourceType()))) {
                    return AssetExecutionPlanResolution.error("执行计划来源身份与当前决策不一致");
                }
                String relatedPlanId = trimToNull(relation.getExecutionPlanId());
                if (relatedPlanId == null) {
                    return AssetExecutionPlanResolution.missing("精确关系缺少 executionPlanId");
                }
                planIds.add(relatedPlanId);
            }
            if (planIds.size() != 1) {
                return AssetExecutionPlanResolution.error("同一决策关联了多个执行计划身份");
            }
            String planId = planIds.iterator().next();
            ExecutionPlanDO plan = executionPlanMapper.selectByPlanId(planId);
            if (plan == null) {
                return AssetExecutionPlanResolution.missing("精确执行计划记录不存在");
            }
            if (!planId.equals(trimToNull(plan.getPlanId()))
                    || !analysisId.equals(trimToNull(plan.getAnalysisId()))) {
                return AssetExecutionPlanResolution.error("执行计划记录与精确关系不一致");
            }
            AnalysisRunDO run = analysisRunMapper.selectById(analysisId);
            if (run == null
                    || !analysisId.equals(trimToNull(run.getAnalysisId()))
                    || !symbol.equals(normalizeSymbol(run.getSymbol()))) {
                return AssetExecutionPlanResolution.error("分析来源与当前资产身份不一致");
            }
            return new AssetExecutionPlanResolution(
                    ExactPlanIdentityState.READY, plan, analysisId, planId,
                    trimToNull(run.getTraceId()), null);
        } catch (RuntimeException ignored) {
            return AssetExecutionPlanResolution.error("执行计划精确身份读取失败");
        }
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
        suggestion.setModuleState(executionModuleState(status));
    }

    private String executionModuleState(String status) {
        return switch (upper(status)) {
            case "USABLE_REVIEW_PLAN" -> "READY";
            case "NO_COMPLETE_PLAN", "NOT_WORTH_OPENING" -> "EMPTY";
            case "ANALYSIS_SNAPSHOT_MISSING", "PLAN_IDENTITY_MISSING", "PLAN_MISSING" -> "MISSING";
            case "DATA_QUALITY_BLOCKED", "STATE_SNAPSHOT_UNVERIFIED", "BOUNDARY_INCOMPLETE",
                    "REVALIDATION_REQUIRED", "PLAN_INCOMPLETE", "PLAN_REVIEW_ONLY" -> "PARTIAL";
            default -> "ERROR";
        };
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
            List<OpportunityLogPublicDTO> publicRows = opportunityLogService.queryPublic(
                    null, null, null, null, null, null, null, null, limit);
            for (OpportunityLogPublicDTO row : publicRows == null
                    ? List.<OpportunityLogPublicDTO>of()
                    : publicRows) {
                PublicOpportunityProjectionPolicy.Evaluation evaluation =
                        PublicOpportunityProjectionPolicy.evaluate(
                                row, row == null ? null : row.opportunityId());
                if (row == null
                        || evaluation.state() == MessageReadState.ERROR
                        || evaluation.state() == MessageReadState.MISSING) {
                    continue;
                }
                DashboardHomeVO.PushItemVO item = publicOpportunityItem(row, evaluation);
                items.add(item);
                if (evaluation.state() == MessageReadState.PARTIAL) {
                    waiting++;
                } else if ("MISSED_INVALID".equals(evaluation.displayStatus())) {
                    invalidated++;
                }
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

    private DashboardHomeVO.PushItemVO publicOpportunityItem(
            OpportunityLogPublicDTO row,
            PublicOpportunityProjectionPolicy.Evaluation evaluation) {
        DashboardHomeVO.PushItemVO item = new DashboardHomeVO.PushItemVO();
        item.setMessageId(row.opportunityId());
        item.setSourceIdentity("OPPORTUNITY");
        item.setSymbol(toDisplaySymbol(row.symbol()));
        item.setPublicLifecycle(evaluation.publicLifecycle());
        item.setPublicStatus(evaluation.publicStatus());
        item.setPublicTimestamp(PublicOpportunityProjectionPolicy.publicTimestamp(row));
        item.setPublicDescription(PublicOpportunityProjectionPolicy.publicDescription(row));
        return item;
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

    private DecisionReadResult safeDecisionRead(Long userId, int limit) {
        try {
            List<DecisionResultVO> decisions = userId == null
                    ? decisionService.getLatestDecisionResults(limit)
                    : decisionService.getLatestDecisionResultsForUser(userId, limit);
            return new DecisionReadResult(decisions != null ? List.copyOf(decisions) : List.of(), false);
        } catch (RuntimeException ignored) {
            return new DecisionReadResult(List.of(), true);
        }
    }

    private DecisionLookupResult safeDecisionLookup(Long userId, String symbol) {
        try {
            if (!hasText(symbol)) {
                return new DecisionLookupResult(null, false);
            }
            DecisionResultVO decision = userId == null
                    ? decisionService.getLatestDecisionResultBySymbol(symbol)
                    : decisionService.getLatestDecisionResultBySymbolForUser(userId, symbol);
            return new DecisionLookupResult(decision, false);
        } catch (RuntimeException ignored) {
            return new DecisionLookupResult(null, true);
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

    private PositionReadResult safePositionRead(Long userId) {
        if (userId == null || userId <= 0) {
            return new PositionReadResult(List.of(), false);
        }
        try {
            List<UserPositionVO> positions = userPositionService.listOpenPositionsForUser(userId);
            return new PositionReadResult(positions != null ? List.copyOf(positions) : List.of(), false);
        } catch (RuntimeException ignored) {
            return new PositionReadResult(List.of(), true);
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
        return authoritativeAssetStateResolution(symbol, compatibilitySnapshot).value();
    }

    private AssetStateResolution authoritativeAssetStateResolution(String symbol, String compatibilitySnapshot) {
        if (assetStateMapper != null && hasText(symbol)) {
            try {
                AssetStateDO row = assetStateMapper.selectBySymbol(symbol);
                String state = row != null && row.getState() != null
                        ? recognizedAssetStateValue(row.getState().name()) : null;
                if (state != null) return new AssetStateResolution(state, "REAL");
                if (row != null && row.getState() != null) {
                    return new AssetStateResolution(null, "ERROR");
                }
            } catch (RuntimeException ignored) {
                String fallback = recognizedAssetStateFromSnapshot(compatibilitySnapshot);
                return fallback == null
                        ? new AssetStateResolution(null, "ERROR")
                        : new AssetStateResolution(fallback, "FALLBACK");
            }
        }
        String fallback = recognizedAssetStateFromSnapshot(compatibilitySnapshot);
        return fallback == null
                ? new AssetStateResolution(null, "MISSING")
                : new AssetStateResolution(fallback, "FALLBACK");
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
        if (DataQualityCircuitBreakerPolicy.isBlocked(decision.getDataQualityScore())) {
            return "数据质量不足，暂不交易 / 事件观望";
        }
        if ("CONFUSED".equals(assetState)) return "冲突状态，等待人工复核";
        if (riskRank(decision.getRiskLevel()) >= riskRank("HIGH")) return "风险较高，仅供人工复核";
        if (Boolean.TRUE.equals(decision.getIsWorthOpening())) return "条件满足，等待人工确认";
        return "当前条件不足，继续观察";
    }

    private PositionSelectionResult resolveSelectedPosition(List<DashboardHomeVO.PositionVO> positions,
                                                            Long selectedPositionId) {
        List<DashboardHomeVO.PositionVO> available = positions == null ? List.of() : positions;

        if (selectedPositionId != null) {
            if (selectedPositionId <= 0) {
                return PositionSelectionResult.blocked(
                        PositionSelectionStatus.POSITION_NOT_FOUND, 0);
            }
            DashboardHomeVO.PositionVO selected = available.stream()
                    .filter(position -> Objects.equals(position.getPositionId(), selectedPositionId))
                    .findFirst()
                    .orElse(null);
            if (selected == null) {
                return PositionSelectionResult.blocked(
                        PositionSelectionStatus.POSITION_NOT_FOUND, 0);
            }
            return new PositionSelectionResult(
                    PositionSelectionStatus.EXACT_POSITION_SELECTED, selected, 1);
        }
        if (available.isEmpty()) {
            return new PositionSelectionResult(PositionSelectionStatus.NO_POSITION, null, 0);
        }
        return PositionSelectionResult.blocked(
                PositionSelectionStatus.POSITION_SELECTION_REQUIRED, available.size());
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

    private record DecisionReadResult(List<DecisionResultVO> rows, boolean failed) {
    }

    private record DecisionLookupResult(DecisionResultVO decision, boolean failed) {
    }

    private record PositionReadResult(List<UserPositionVO> rows, boolean failed) {
    }

    private record MonitorReadResult(PositionMonitorLogDTO log, boolean failed) {
    }

    private record AssetStateResolution(String value, String sourceStatus) {
    }

    private record PositionRowsResult(
            List<DashboardHomeVO.PositionVO> allRows,
            List<DashboardHomeVO.PositionVO> topRows,
            Map<Long, PositionPlanSourceResolver.Resolution> trustedSources) {
    }

    private enum ExactPlanIdentityState {
        READY,
        MISSING,
        ERROR
    }

    private record AssetExecutionPlanResolution(ExactPlanIdentityState state,
                                                ExecutionPlanDO executionPlan,
                                                String analysisId,
                                                String executionPlanId,
                                                String sourceTraceId,
                                                String reason) {
        private boolean verified() {
            return state == ExactPlanIdentityState.READY
                    && executionPlan != null && analysisId != null && executionPlanId != null;
        }

        private static AssetExecutionPlanResolution missing(String reason) {
            return new AssetExecutionPlanResolution(
                    ExactPlanIdentityState.MISSING, null, null, null, null, reason);
        }

        private static AssetExecutionPlanResolution error(String reason) {
            return new AssetExecutionPlanResolution(
                    ExactPlanIdentityState.ERROR, null, null, null, null, reason);
        }
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
