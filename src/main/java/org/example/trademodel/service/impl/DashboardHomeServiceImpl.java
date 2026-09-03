package org.example.trademodel.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.trademodel.analysisrun.AnalysisRunTriggerType;
import org.example.trademodel.analysisrun.AnalysisTimePolicy;
import org.example.trademodel.ai.AiRoleResultsCodec;
import org.example.trademodel.ai.AiRoleResultsPayload;
import org.example.trademodel.common.EvidenceTypeConstants;
import org.example.trademodel.derivatives.DerivativesBusinessAssessment;
import org.example.trademodel.derivatives.DerivativesBusinessInput;
import org.example.trademodel.derivatives.DerivativesBusinessIntegrationService;
import org.example.trademodel.derivatives.DerivativesEvidenceItem;
import org.example.trademodel.derivatives.DerivativesEvidenceType;
import org.example.trademodel.derivatives.DerivativesSnapshotReadPort;
import org.example.trademodel.entity.MonitorAlertDO;
import org.example.trademodel.entity.AnalysisRunDO;
import org.example.trademodel.entity.AssetStateDO;
import org.example.trademodel.entity.DecisionResult;
import org.example.trademodel.entity.EvidenceItemDO;
import org.example.trademodel.entity.ExecutionPlanDO;
import org.example.trademodel.enums.PlanModeEnum;
import org.example.trademodel.messagepush.MessageReadState;
import org.example.trademodel.messagepush.PublicOpportunityProjectionPolicy;
import org.example.trademodel.opportunitylog.OpportunityLogDTO;
import org.example.trademodel.opportunitylog.OpportunityLogPublicDTO;
import org.example.trademodel.providercall.AssetPriority;
import org.example.trademodel.providercall.ProviderCallResult;
import org.example.trademodel.providercall.SnapshotFreshnessStatus;
import org.example.trademodel.providercall.snapshot.MarketPriceSnapshot;
import org.example.trademodel.providercall.snapshot.MarketPriceSnapshotPolicy;
import org.example.trademodel.providercall.snapshot.MarketPriceSnapshotService;
import org.example.trademodel.providercall.snapshot.DerivativesRiskSnapshot;
import org.example.trademodel.mapper.AnalysisRunMapper;
import org.example.trademodel.mapper.AccountRiskSnapshotMapper;
import org.example.trademodel.mapper.AssetStateMapper;
import org.example.trademodel.mapper.DecisionResultMapper;
import org.example.trademodel.mapper.EvidenceItemMapper;
import org.example.trademodel.mapper.ExecutionPlanMapper;
import org.example.trademodel.mapper.PersistedOhlcvBarMapper;
import org.example.trademodel.localreal.LocalRealAssetReadiness;
import org.example.trademodel.localreal.LocalRealAssetReadinessState;
import org.example.trademodel.localreal.LocalRealReadinessService;
import org.example.trademodel.market.PersistedRealMarketEnvironmentAssessment;
import org.example.trademodel.market.PersistedRealMarketEnvironmentService;
import org.example.trademodel.entity.PersistedOhlcvBarDO;
import org.example.trademodel.service.DashboardHomeService;
import org.example.trademodel.service.ConfusedStatePolicy;
import org.example.trademodel.service.MarketDataScheduler;
import org.example.trademodel.service.MarketBiasPolicy;
import org.example.trademodel.service.DecisionService;
import org.example.trademodel.service.MonitorService;
import org.example.trademodel.service.OpportunityLogService;
import org.example.trademodel.service.OpportunityPriorityRankingService;
import org.example.trademodel.service.PersistedOhlcvQueryService;
import org.example.trademodel.service.PositionMonitorLogService;
import org.example.trademodel.service.PositionSyncService;
import org.example.trademodel.service.PlanRevalidationService;
import org.example.trademodel.service.UserPositionService;
import org.example.trademodel.enums.PlanRevalidationTriggerTypeEnum;
import org.example.trademodel.service.readiness.ProviderReadinessService;
import org.example.trademodel.positionmonitorlog.PositionMonitorLogDTO;
import org.example.trademodel.positionmonitorlog.PositionMonitorConclusionEnum;
import org.example.trademodel.positionmonitorlog.PositionMonitorSuggestedActionEnum;
import org.example.trademodel.positionmonitor.PositionPlanSourceResolver;
import org.example.trademodel.service.support.DataQualityCircuitBreakerPolicy;
import org.example.trademodel.service.support.ExecutionPlanReviewPolicy;
import org.example.trademodel.service.support.ExecutionPlanReviewPolicy.PersistedPlanState;
import org.example.trademodel.service.support.ExternalContextEvidenceBuilder;
import org.example.trademodel.service.support.ExternalContextSnapshot;
import org.example.trademodel.vo.DashboardHomeVO;
import org.example.trademodel.vo.DecisionResultVO;
import org.example.trademodel.vo.LightSystemStatusVO;
import org.example.trademodel.vo.HomeTopAssetProjection;
import org.example.trademodel.vo.MarketEnvironmentVO;
import org.example.trademodel.vo.PositionSyncStatusVO;
import org.example.trademodel.vo.ProviderReadinessVO;
import org.example.trademodel.vo.UserPositionVO;
import org.example.trademodel.entity.TmAccountRiskSnapshotDO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.example.trademodel.service.watchlistsource.AssetPoolService;

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
import java.util.Set;
import java.util.UUID;
import java.util.regex.Pattern;

@Service
public class DashboardHomeServiceImpl implements DashboardHomeService {
    private static final int DEFAULT_LIMIT = 6;
    private static final int MAX_LIMIT = 12;
    private static final List<String> AI_ROLES = List.of("GPT_FINAL", "GEMINI_REVIEW", "GROK_CHALLENGE");
    private static final Map<String, String> ASSET_DISPLAY_NAMES = Map.ofEntries(
            Map.entry("BTC", "Bitcoin"),
            Map.entry("ETH", "Ethereum"),
            Map.entry("SOL", "Solana"),
            Map.entry("BNB", "BNB"),
            Map.entry("XRP", "XRP"),
            Map.entry("ADA", "Cardano"),
            Map.entry("DOGE", "Dogecoin"),
            Map.entry("LINK", "Chainlink"),
            Map.entry("AAVE", "Aave"),
            Map.entry("TAO", "Bittensor"),
            Map.entry("SUI", "Sui"),
            Map.entry("ARB", "Arbitrum")
    );
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
    private EvidenceItemMapper evidenceItemMapper;
    private DecisionResultMapper decisionResultMapper;
    private ExecutionPlanMapper executionPlanMapper;
    private PositionPlanSourceResolver positionPlanSourceResolver;
    private LocalRealReadinessService localRealReadinessService;
    private AssetStateMapper assetStateMapper;
    private AssetPoolService assetPoolService;
    private OpportunityPriorityRankingService opportunityPriorityRankingService;
    private AccountRiskSnapshotMapper accountRiskSnapshotMapper;
    private PersistedRealMarketEnvironmentService persistedRealMarketEnvironmentService;
    private PersistedOhlcvQueryService persistedOhlcvQueryService;
    private MarketDataScheduler marketDataScheduler;
    private PlanRevalidationService planRevalidationService;
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
    void setHomeProvenanceSource(EvidenceItemMapper evidenceItemMapper) {
        this.evidenceItemMapper = evidenceItemMapper;
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
    void setAssetPoolService(AssetPoolService assetPoolService) {
        this.assetPoolService = assetPoolService;
    }

    @Autowired
    void setOpportunityPriorityRankingService(
            OpportunityPriorityRankingService opportunityPriorityRankingService) {
        this.opportunityPriorityRankingService = opportunityPriorityRankingService;
    }

    @Autowired(required = false)
    void setAccountRiskSnapshotMapper(AccountRiskSnapshotMapper accountRiskSnapshotMapper) {
        this.accountRiskSnapshotMapper = accountRiskSnapshotMapper;
    }

    @Autowired(required = false)
    void setPersistedRealMarketEnvironmentService(
            PersistedRealMarketEnvironmentService persistedRealMarketEnvironmentService) {
        this.persistedRealMarketEnvironmentService = persistedRealMarketEnvironmentService;
    }

    @Autowired(required = false)
    void setPersistedOhlcvQueryService(PersistedOhlcvQueryService persistedOhlcvQueryService) {
        this.persistedOhlcvQueryService = persistedOhlcvQueryService;
    }

    @Autowired(required = false)
    void setMarketDataScheduler(MarketDataScheduler marketDataScheduler) {
        this.marketDataScheduler = marketDataScheduler;
    }

    @Autowired(required = false)
    void setPlanRevalidationService(PlanRevalidationService planRevalidationService) {
        this.planRevalidationService = planRevalidationService;
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
        boolean rankingEnabled = opportunityPriorityRankingService != null;
        RankingReadResult rankingRead = rankingEnabled
                ? safeHomeRanking(userId, effectiveLimit)
                : new RankingReadResult(List.of(), false);
        List<String> focusSymbols = rankingEnabled
                ? rankingRead.rows().stream().map(HomeTopAssetProjection::symbol).toList()
                : focusSymbols(userId, effectiveLimit);

        String normalizedRequest = normalizeSymbol(selectedSymbol);
        String normalizedSelected = normalizedRequest;
        if (normalizedSelected == null) {
            normalizedSelected = focusSymbols.stream().findFirst().orElse(null);
        }
        if (normalizedSelected == null && !rankingEnabled) {
            normalizedSelected = firstDecisionSymbol(decisions);
        }

        HomeTopAssetProjection selectedProjection = findRankingProjection(rankingRead.rows(), normalizedSelected);
        DecisionResultVO selectedDecision = rankingEnabled
                ? selectedProjection == null ? null : selectedProjection.sourceDecision()
                : findDecision(decisions, normalizedSelected);
        boolean selectedDecisionReadFailed = false;
        if (selectedDecision == null && normalizedSelected != null
                && (selectedProjection == null || hasText(selectedProjection.analysisId()))) {
            DecisionLookupResult lookup = safeDecisionLookup(userId, normalizedSelected);
            selectedDecision = selectedProjection == null
                    ? lookup.decision()
                    : validatedObservationDecision(userId, selectedProjection, lookup.decision());
            selectedDecisionReadFailed = lookup.failed();
        }
        List<DashboardHomeVO.AssetVO> assets = rankingEnabled
                ? buildRankedAssets(rankingRead.rows(), effectiveLimit, userId)
                : buildAssets(decisions, selectedDecision, normalizedSelected, focusSymbols, effectiveLimit);
        if (!rankingEnabled && normalizedRequest == null && !hasRenderableAsset(assets, normalizedSelected)) {
            String firstRenderableSymbol = firstRenderableAssetSymbol(assets);
            if (firstRenderableSymbol != null) {
                normalizedSelected = firstRenderableSymbol;
                selectedDecision = findDecision(decisions, normalizedSelected);
                if (selectedDecision == null) {
                    DecisionLookupResult lookup = safeDecisionLookup(userId, normalizedSelected);
                    selectedDecision = lookup.decision();
                    selectedDecisionReadFailed = selectedDecisionReadFailed || lookup.failed();
                }
                assets = buildAssets(decisions, selectedDecision, normalizedSelected, focusSymbols, effectiveLimit);
            }
        }

        ExternalContextSnapshot externalContext = safeExternalContext(normalizedSelected, selectedDecision);
        PushInboxContext pushInboxContext = buildPushInbox(positions, effectiveLimit);

        DashboardHomeVO.AiDecisionVO aiDecision = buildAiDecision(selectedDecision);
        PositionRowsResult positionRowsResult = buildPositions(userId, positions);
        Instant globalDataUpdatedAt = latestPersistedClosedBarAt();
        DashboardHomeVO home = new DashboardHomeVO();
        home.setHeader(buildHeader(systemStatus, positionSyncStatus, externalContext, providerReadiness,
                aiDecision, schedulerRuntimeProjection()));
        home.setSystemState(buildSystemState(systemStatus, decisions, aiDecision, providerReadiness,
                positionRowsResult, globalDataUpdatedAt));
        home.setAlerts(buildAlerts(alerts));
        home.setEvents(buildEvents(externalContext));
        home.setAssets(assets);
        home.setPositions(positionRowsResult.topRows());
        home.setPositionAggregate(buildPositionAggregate(positionRowsResult));
        home.setPositionMonitoringState(positionRowsResult.monitoringState());
        home.setSelectedSymbol(normalizedSelected);
        DashboardHomeVO.AssetVO selectedAsset = findHomeAsset(assets, normalizedSelected);
        DashboardHomeVO.AssetVO selectedContext = selectedAsset != null
                ? selectedAsset
                : selectedDecision == null ? null : assetFromDecision(0, selectedDecision);
        home.setSelectedAssetContext(selectedContext);
        home.setSelectedContextState(selectedAsset != null ? "RANKED"
                : selectedContext != null ? "EXITED_TOP6" : "NO_ACTIVE_CONTEXT");
        home.setSelectedContextExitReason(selectedAsset != null ? null
                : selectedContext != null ? "NO_LONGER_IN_CURRENT_TOP6" : "NO_TRUSTED_DECISION_CONTEXT");
        PositionSelectionResult positionSelection = resolveSelectedPosition(positionRowsResult.allRows(), selectedPositionId);
        DashboardHomeVO.PositionVO activePosition = positionSelection.selectedPosition();
        home.setSelectedPositionId(activePosition != null ? activePosition.getPositionId() : null);
        home.setPositionSelectionStatus(positionSelection.status().name());
        home.setMatchingPositionCount(positionSelection.matchingPositionCount());
        DecisionResultVO executionDecision = (selectedProjection != null
                && selectedProjection.sourceDecision() == null)
                || (selectedProjection != null
                && "HIGH_RISK".equalsIgnoreCase(selectedProjection.opportunityState()))
                ? null : selectedDecision;
        DashboardHomeVO.ExecutionSuggestionVO executionSuggestion = buildExecutionSuggestion(executionDecision);
        home.setExecutionSuggestion(executionSuggestion);
        home.setAiDecision(aiDecision);
        home.setPushInbox(pushInboxContext.pushInbox());
        DashboardHomeVO.DerivativesSummaryVO derivativesSummary = buildDerivativesSummary(
                normalizedSelected, selectedDecision);
        home.setDerivatives(derivativesSummary);
        if (selectedContext != null && derivativesSummary != null
                && upper(derivativesSummary.getSource()).startsWith("COINGLASS")) {
            selectedContext.setCoinGlassDataAt(derivativesSummary.getDataTime());
        }
        home.setDiagnostics(buildDiagnostics(systemStatus, decisions, selectedDecision, positionSyncStatus,
                pushInboxContext, providerReadiness, positionRowsResult));
        home.setSafety(new DashboardHomeVO.SafetyVO());
        DashboardHomeVO.ModuleStatesVO moduleStates = buildModuleStates(
                selectedContext, executionSuggestion, positionRowsResult, aiDecision,
                decisionRead.failed() || selectedDecisionReadFailed || rankingRead.failed(), positionRead.failed());
        home.setStates(moduleStates);
        home.getHeader().setDataStatus(moduleStates.getOverall());
        home.getHeader().setUpdatedAt(globalDataUpdatedAt);
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
        if (aiDecision == null || aiDecision.getConsistency() == null) {
            return "MISSING";
        }
        return switch (upper(aiDecision.getConsistency().getDataState())) {
            case "READY" -> "READY".equals(aiState) ? "READY" : "PARTIAL";
            case "INSUFFICIENT_DATA", "STALE" -> "PARTIAL";
            case "SOURCE_UNAVAILABLE", "" -> "MISSING";
            default -> "ERROR";
        };
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
                summary.setDecisionImpact("不可用于判断");
                return summary;
            }
            summary.setSource(providerLabel(snapshot.provider()));
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
            if ("暂无法判断".equals(summary.getOpenInterestStructure())
                    && snapshot.openInterestUsd() != null) {
                summary.setOpenInterestStructure("变化平稳");
            }
            if ("暂无法判断".equals(summary.getFundingRisk())
                    && snapshot.weightedFundingRate() != null) {
                summary.setFundingRisk("正常");
            }
            if ("暂无法判断".equals(summary.getCrowdingDirection())
                    && snapshot.longShortRatio() != null) {
                summary.setCrowdingDirection("暂无明显拥挤");
            }
            if ("暂无法判断".equals(summary.getLiquidationRisk())
                    && (snapshot.longLiquidationUsd5m() != null || snapshot.longLiquidationUsd15m() != null
                    || snapshot.shortLiquidationUsd5m() != null || snapshot.shortLiquidationUsd15m() != null)) {
                summary.setLiquidationRisk("正常");
            }
            boolean unavailable = snapshot.sourceStatus() == null
                    || snapshot.freshnessStatus() == null
                    || snapshot.freshnessStatus() != SnapshotFreshnessStatus.FRESH
                    || "UNAVAILABLE".equalsIgnoreCase(snapshot.evidenceAvailability());
            summary.setDecisionImpact(assessment.isHighRisk() ? "风险阻断"
                    : unavailable ? "不可用于判断"
                    : "COMPLETE".equalsIgnoreCase(snapshot.evidenceAvailability())
                    ? "未发现衍生品阻断" : "数据不足，需降级");
        } catch (RuntimeException failure) {
            summary.setStatus("错误");
            summary.setDecisionImpact("不可用于判断");
        }
        return summary;
    }

    private static String derivativesStatusLabel(DerivativesRiskSnapshot snapshot) {
        if (snapshot == null || snapshot.sourceStatus() == null) return "等待同步";
        if (snapshot.freshnessStatus() == SnapshotFreshnessStatus.STALE_READABLE) return "过期";
        if (snapshot.freshnessStatus() == SnapshotFreshnessStatus.UNAVAILABLE
                || snapshot.freshnessStatus() == SnapshotFreshnessStatus.REFRESHING) return "等待同步";
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
                                                 DashboardHomeVO.AiDecisionVO aiDecision,
                                                 SchedulerRuntimeProjection schedulerRuntime) {
        DashboardHomeVO.HeaderVO header = new DashboardHomeVO.HeaderVO();
        header.setPageTitle("首页总览");
        header.setDataStatus(firstNonBlank(systemStatus != null ? systemStatus.getStatus() : null, "WAITING_SYNC"));
        String aiStatus = headerAiStatus(providerReadiness, aiDecision);
        header.setAiStatus(aiStatus);
        header.setAiStatusLabel(headerAiStatusLabel(aiStatus));
        header.setDataSourceText(dataSourceText(positionSyncStatus, externalContext, providerReadiness));
        if (localRealReadinessService != null) {
            header.setDataSourceText(firstNonBlank(
                    providerReadiness != null ? providerReadiness.getDataSourceText() : null,
                    "WAITING_SYNC"));
        }
        header.setUpdatedAt(null);
        header.setSystemRuntimeState(schedulerRuntime.state());
        header.setSystemRuntimeLabel(schedulerRuntime.label());
        header.setSchedulerHeartbeatAt(schedulerRuntime.heartbeatAt());
        header.setScanStartedAt(schedulerRuntime.startedAt());
        header.setLastCompletedScanAt(schedulerRuntime.completedAt());
        header.setNextScheduledScanAt(schedulerRuntime.nextScheduledAt());
        header.setLastScanResult(schedulerRuntime.result());
        header.setLastScanFailureReason(schedulerRuntime.failureReason());
        return header;
    }

    private SchedulerRuntimeProjection schedulerRuntimeProjection() {
        MarketDataScheduler.RuntimeStatus runtime = marketDataScheduler == null
                ? null : marketDataScheduler.runtimeStatus();
        AnalysisRunDO active = activeAnalysisRun();

        Instant startedAt = runtime != null ? runtime.startedAt() : null;
        if (active != null && active.getStartedAt() != null) {
            startedAt = active.getStartedAt().toInstant(ZoneOffset.UTC);
        }
        Instant completedAt = runtime == null ? null : runtime.completedAt();

        if (runtime != null && runtime.running()
                && runtime.heartbeatAt() != null && !runtime.heartbeatFresh()) {
            return new SchedulerRuntimeProjection("ERROR", "运行异常",
                    runtime.heartbeatAt(), startedAt, completedAt, runtime.nextScheduledAt(),
                    runtime.result(), "SCHEDULED_SCAN_HEARTBEAT_TIMEOUT");
        }
        if (active != null || runtime != null && runtime.running()) {
            return new SchedulerRuntimeProjection("ANALYZING", "分析中",
                    runtime == null ? null : runtime.heartbeatAt(), startedAt, completedAt,
                    runtime == null ? null : runtime.nextScheduledAt(),
                    runtime == null ? null : runtime.result(),
                    runtime == null ? null : runtime.failureReason());
        }
        if (runtime == null) {
            return SchedulerRuntimeProjection.unknown();
        }
        if (!runtime.enabled()) {
            return new SchedulerRuntimeProjection("NOT_RUNNING", "未运行",
                    runtime.heartbeatAt(), startedAt, completedAt, runtime.nextScheduledAt(),
                    runtime.result(), runtime.failureReason());
        }
        if (runtime.heartbeatAt() == null) {
            return SchedulerRuntimeProjection.unknown();
        }
        if (!runtime.heartbeatFresh()) {
            return new SchedulerRuntimeProjection("NOT_RUNNING", "未运行",
                    runtime.heartbeatAt(), startedAt, completedAt, runtime.nextScheduledAt(),
                    runtime.result(), runtime.failureReason());
        }
        if ("FAILED".equals(runtime.result())) {
            return new SchedulerRuntimeProjection("ERROR", "运行异常",
                    runtime.heartbeatAt(), startedAt, completedAt, runtime.nextScheduledAt(),
                    runtime.result(), runtime.failureReason());
        }
        if ("SUCCESS".equals(runtime.result())) {
            return new SchedulerRuntimeProjection("RUNNING", "运行中",
                    runtime.heartbeatAt(), startedAt, completedAt, runtime.nextScheduledAt(),
                    runtime.result(), null);
        }
        return SchedulerRuntimeProjection.unknown();
    }

    private AnalysisRunDO activeAnalysisRun() {
        if (analysisRunMapper == null) {
            return null;
        }
        try {
            List<AnalysisRunDO> active = analysisRunMapper.selectRecoverableBackgroundRuns(1);
            return active == null || active.isEmpty() ? null : active.get(0);
        } catch (RuntimeException ignored) {
            return null;
        }
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
                                                           DashboardHomeVO.AiDecisionVO aiDecision,
                                                           ProviderReadinessVO providerReadiness,
                                                           PositionRowsResult positionRows,
                                                           Instant globalDataUpdatedAt) {
        DashboardHomeVO.SystemStateVO state = new DashboardHomeVO.SystemStateVO();
        state.setMarketTrend(macroEnvironmentCard());
        state.setRiskLevel(card(
                "riskLevel",
                "系统风险",
                null,
                "当前不可查看",
                "未取得系统级风险生产者",
                "SOURCE_UNAVAILABLE",
                null
        ));
        state.setDataQuality(globalDataUpdateCard(globalDataUpdatedAt));
        state.setServiceAvailability(serviceAvailabilityCard(providerReadiness));
        state.setAccountStatus(accountStatusCard(positionRows));
        DashboardHomeVO.ConsistencyVO consistency = aiDecision != null ? aiDecision.getConsistency() : null;
        boolean aiApplicable = consistency != null
                && "READY".equalsIgnoreCase(trimToNull(consistency.getDataState()));
        String conflictLevel = aiApplicable ? trimToNull(consistency.getConflictLevel()) : null;
        state.setAiConflict(card(
                "aiConflict",
                "AI 冲突等级",
                conflictLevel,
                aiApplicable ? aiConflictLevelLabel(conflictLevel) : "不适用",
                aiApplicable ? "AI 冲突" : "本轮未形成可裁决 AI 意见",
                aiApplicable
                        ? conflictLevel != null ? "CONNECTED" : "WAITING_SYNC"
                        : "NOT_APPLICABLE",
                null
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
        String hotResetStateLabel = Boolean.TRUE.equals(hotResetFired) ? "已触发" : "关闭";
        DashboardHomeVO.StatusCardVO hotReset = card(
                "hotReset",
                "热重置",
                hotResetFired,
                hotResetLabel(hotResetFired, systemStatus != null ? systemStatus.getHotResetSymbol() : null,
                        hotResetStateLabel),
                hotResetFired == null ? "未取得 Hot Reset 状态"
                        : Boolean.TRUE.equals(hotResetFired) ? "正式系统事件及作用域" : "正式系统状态",
                hotResetFired == null ? "SOURCE_UNAVAILABLE"
                        : Boolean.TRUE.equals(hotResetFired) && !hasText(systemStatus.getHotResetSymbol())
                        ? "TRIGGERED_SCOPE_UNKNOWN"
                        : Boolean.TRUE.equals(hotResetFired) ? "TRIGGERED" : "INACTIVE",
                null
        );
        hotReset.setMeta(hotMeta);
        state.setHotReset(hotReset);
        return state;
    }

    private DashboardHomeVO.StatusCardVO macroEnvironmentCard() {
        if (persistedRealMarketEnvironmentService == null) {
            return card("marketTrend", "BTC / 宏观环境", null, "— / 当前不可查看",
                    "正式环境生产者不可用", "SOURCE_UNAVAILABLE", null);
        }
        try {
            PersistedRealMarketEnvironmentAssessment assessment =
                    persistedRealMarketEnvironmentService.assess("BTCUSDT", "1h");
            MarketEnvironmentVO environment = assessment != null ? assessment.environment() : null;
            if (assessment == null || !assessment.ready() || environment == null
                    || !"FRESH".equalsIgnoreCase(trimToNull(environment.getFreshness()))) {
                return card("marketTrend", "BTC / 宏观环境", null, "— / 当前不可查看",
                        assessment != null ? assessment.reasonCode() : "ENVIRONMENT_SOURCE_UNAVAILABLE",
                        "SOURCE_UNAVAILABLE", null);
            }
            String environmentType = upper(environment.getEnvironmentType());
            String environmentLabel = switch (environmentType) {
                case "TREND_MARKET" -> "趋势环境";
                case "RANGE_MARKET" -> "震荡环境";
                default -> null;
            };
            if (environmentLabel == null) {
                return card("marketTrend", "BTC / 宏观环境", null, "— / 当前不可查看",
                        "环境分类不可识别", "SOURCE_UNAVAILABLE", null);
            }
            return card("marketTrend", "BTC / 宏观环境", environmentType, environmentLabel,
                    assessment.sourceType(), "CONNECTED", null);
        } catch (RuntimeException ignored) {
            return card("marketTrend", "BTC / 宏观环境", null, "— / 当前不可查看",
                    "正式环境读取失败", "SOURCE_UNAVAILABLE", null);
        }
    }

    private DashboardHomeVO.StatusCardVO globalDataUpdateCard(Instant globalDataUpdatedAt) {
        return card("dataQuality", "全局数据", globalDataUpdatedAt,
                globalDataUpdatedAt == null ? "—" : null,
                globalDataUpdatedAt == null ? "未取得正式全局更新时间"
                        : "PersistedOhlcvBarMapper.selectLatestClosedBarBySource",
                globalDataUpdatedAt == null ? "SOURCE_UNAVAILABLE" : "AVAILABLE", null);
    }

    private Instant latestPersistedClosedBarAt() {
        if (persistedOhlcvBarMapper == null) {
            return null;
        }
        try {
            PersistedOhlcvBarDO latest = latestClosedBarFromPrimarySource();
            return latest == null || latest.getCloseTimeMs() == null
                    ? null : Instant.ofEpochMilli(latest.getCloseTimeMs());
        } catch (RuntimeException ignored) {
            return null;
        }
    }

    private DashboardHomeVO.StatusCardVO accountStatusCard(PositionRowsResult positionRows) {
        List<DashboardHomeVO.PositionVO> positions = positionRows == null
                ? List.of() : positionRows.allRows();
        if (positions.isEmpty()) {
            return card("accountStatus", "账户·已录入", 0, "0 笔",
                    "活动持仓 0", "EMPTY", 0);
        }
        String highestRisk = positions.stream()
                .filter(row -> "VERIFIED_FRESH".equalsIgnoreCase(trimToNull(row.getMonitorTrustState())))
                .map(DashboardHomeVO.PositionVO::getRiskLevel)
                .filter(this::recognizedPositionRisk)
                .max(Comparator.comparingInt(this::positionRiskRank))
                .orElse(null);
        String riskSummary = highestRisk == null ? "待评估" : riskLabel(highestRisk) + "风险";
        String coverage = accountRiskCoverage(positionRows);
        String positionCountLabel = positions.size() + " 笔";
        return card("accountStatus", "账户·已录入", positions.size(),
                positionCountLabel + " · " + riskSummary,
                "活动持仓（OPEN + PARTIALLY_CLOSED） · " + coverage,
                highestRisk == null ? "PARTIAL" : "AVAILABLE", positions.size());
    }

    private int positionRiskRank(String riskLevel) {
        return switch (upper(riskLevel)) {
            case "LOW" -> 1;
            case "MEDIUM" -> 2;
            case "HIGH" -> 3;
            case "EXTREME" -> 4;
            default -> 0;
        };
    }

    private DashboardHomeVO.StatusCardVO serviceAvailabilityCard(ProviderReadinessVO providerReadiness) {
        List<ProviderReadinessVO.ProviderStatusVO> providers = providerReadiness == null
                ? List.of() : providerReadiness.getProviders();
        if (providers == null || providers.isEmpty()) {
            return card("serviceAvailability", "服务可用性", null, "—",
                    "未取得正式可用数与服务总数", "SOURCE_UNAVAILABLE", null);
        }
        List<ProviderReadinessVO.ProviderStatusVO> activeProviders = providers.stream()
                .filter(Objects::nonNull)
                .filter(provider -> Boolean.TRUE.equals(provider.getEnabled())
                        || Boolean.TRUE.equals(provider.getConnected()))
                .toList();
        if (activeProviders.isEmpty()) {
            return card("serviceAvailability", "服务可用性", 0, "未启用",
                    "ProviderReadiness.providers 中没有启用的运行能力", "NOT_APPLICABLE", 0);
        }
        long available = activeProviders.stream()
                .filter(provider -> Boolean.TRUE.equals(provider.getConnected()))
                .filter(provider -> "CONNECTED".equalsIgnoreCase(trimToNull(provider.getStatus())))
                .count();
        int total = activeProviders.size();
        String status = available == total ? "AVAILABLE"
                : available > 0 ? "PARTIAL" : "UNAVAILABLE";
        return card("serviceAvailability", "服务可用性", available,
                available + "/" + total + " 可用",
                "ProviderReadiness.providers（仅启用的运行能力）", status, total);
    }

    private String hotResetLabel(Boolean fired, String symbol, String stateLabel) {
        if (fired == null) return "当前不可查看";
        if (!fired) return stateLabel;
        String scope = toDisplaySymbol(symbol);
        return stateLabel + " · " + (hasText(scope) ? scope : "作用域未知");
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
                                                      List<String> focusSymbols,
                                                      int limit) {
        if (assetPoolService == null) {
            return buildLegacyConstructorAssets(decisions, selectedDecision, limit);
        }
        List<DashboardHomeVO.AssetVO> assets = new ArrayList<>();
        String normalizedSelected = normalizeSymbol(selectedSymbol);
        LinkedHashSet<String> ordered = new LinkedHashSet<>(focusSymbols == null ? List.of() : focusSymbols);
        if (normalizedSelected != null && ordered.remove(normalizedSelected)) {
            LinkedHashSet<String> selectedFirst = new LinkedHashSet<>();
            selectedFirst.add(normalizedSelected);
            selectedFirst.addAll(ordered);
            ordered = selectedFirst;
        }
        for (String symbol : ordered) {
            if (assets.size() >= limit) {
                break;
            }
            DecisionResultVO decision = selectedDecision != null
                    && symbol.equals(normalizeSymbol(selectedDecision.getSymbol()))
                    ? selectedDecision : findDecision(decisions, symbol);
            DashboardHomeVO.AssetVO asset = decision == null
                    ? assetPlaceholder(assets.size() + 1, symbol)
                    : assetFromDecision(assets.size() + 1, decision);
            assets.add(asset);
        }
        return assets;
    }

    private List<DashboardHomeVO.AssetVO> buildRankedAssets(
            List<HomeTopAssetProjection> projections,
            int limit,
            Long userId) {
        List<DashboardHomeVO.AssetVO> assets = new ArrayList<>();
        Set<Long> usedAssetIds = new LinkedHashSet<>();
        Set<String> usedSymbols = new LinkedHashSet<>();
        for (HomeTopAssetProjection projection : projections == null
                ? List.<HomeTopAssetProjection>of() : projections) {
            if (assets.size() >= limit) {
                break;
            }
            String symbol = projection == null ? null : normalizeSymbol(projection.symbol());
            if (projection == null || projection.assetId() == null || symbol == null
                    || usedAssetIds.contains(projection.assetId()) || usedSymbols.contains(symbol)) {
                continue;
            }
            DashboardHomeVO.AssetVO asset = projection.sourceDecision() == null
                    ? assetFromObservation(assets.size() + 1, projection, userId)
                    : assetFromDecision(assets.size() + 1, projection.sourceDecision());
            if (asset == null) {
                continue;
            }
            usedAssetIds.add(projection.assetId());
            usedSymbols.add(symbol);
            if (isObservationProjection(projection)) {
                asset.setSlotType("OBSERVATION");
            }
            asset.setAssetId(projection.assetId());
            asset.setName(canonicalAssetName(symbol, projection.name()));
            if (projection.sourceDecision() != null) {
                asset.setAnalysisId(projection.analysisId());
                asset.setOpportunityId(projection.opportunityId());
                asset.setOpportunityState(projection.opportunityState());
                asset.setPrimaryOpportunityId(projection.primaryOpportunityId());
                asset.setPrimaryTimeframe(projection.primaryTimeframe());
                asset.setPrimaryPlanMode(projection.primaryPlanMode());
                asset.setSecondaryOpportunityCount(projection.secondaryOpportunityCount());
                asset.setTimeframeConflictState(projection.timeframeConflictState());
                asset.setOpportunityScore(projection.opportunityScore());
                asset.setPlanMode(projection.planMode());
                asset.setAiDecisionResult(projection.aiDecisionResult());
                asset.setDataQualityScore(projection.dataQuality());
                asset.setRankingReason(projection.rankingReason());
            }
            if (projection.sourceDecision() != null) {
                applyCardFinalProjection(asset, projection.sourceDecision());
            } else {
                clearCardFinalProjection(asset);
            }
            if (Boolean.TRUE.equals(asset.getHasFinal()) && projection.sourceDecision() != null) {
                DecisionResultVO decision = projection.sourceDecision();
                asset.setMarketBias(asset.getFinalMarketBias());
                asset.setMarketBiasLabel(biasLabel(asset.getFinalMarketBias()));
                Integer finalConfidence = decision.getFinalConfidence();
                asset.setConfidenceLevel(finalConfidence == null ? null : String.valueOf(finalConfidence));
                asset.setConfidenceLabel(finalConfidence == null ? null : finalConfidence + "%");
                asset.setOneHourOpportunityLabel(oneHourOpportunityLabel(
                        decision.getOneHourOpportunityQuality()));
                asset.setFourHourTrendLabel(fourHourTrendLabel(asset.getFinalMarketBias(),
                        decision.getFourHourTrendAlignment()));
            } else {
                applyNonFinalCardSemantics(asset, projection.sourceDecision(), projection);
            }
            if (projection.opportunityScore() != null) {
                asset.setCompositeScore(projection.opportunityScore());
                setFieldSource(asset, "score", "DERIVED");
            }
            applyAnalysisProvenance(asset, projection, userId);
            assets.add(asset);
        }
        return assets;
    }

    private void applyAnalysisProvenance(DashboardHomeVO.AssetVO asset,
                                         HomeTopAssetProjection projection,
                                         Long userId) {
        if (asset == null || projection == null) return;

        boolean tierOne = projection.priorityScore() != null && projection.priorityScore() > 0
                && Boolean.TRUE.equals(asset.getHasFinal());
        asset.setHomeTier(tierOne ? "TIER_1" : "TIER_2");
        asset.setDirectionMaturity(tierOne ? "FINAL"
                : projection.analysisId() == null ? "NOT_EVALUATED" : "NON_FINAL");
        asset.setFreshnessStatus(trimToNull(projection.freshness()));

        String analysisId = trimToNull(projection.analysisId());
        if (analysisId == null || analysisRunMapper == null) return;
        AnalysisRunDO run;
        try {
            run = userId != null && userId > 0
                    ? analysisRunMapper.selectReadableByUser(analysisId, userId)
                    : analysisRunMapper.selectById(analysisId);
        } catch (RuntimeException ignored) {
            return;
        }
        if (!sameAnalysisAsset(run, asset, projection)) return;

        asset.setAnalysisId(run.getAnalysisId());
        asset.setAnalysisRunId(run.getAnalysisId());
        asset.setAnalysisVersion(run.getVersionNo());
        asset.setConfigurationVersion(trimToNull(run.getRuleVersion()));

        DecisionResultVO projectedDecision = projection.sourceDecision();
        DecisionResult persistedDecision = null;
        if (projectedDecision == null && decisionResultMapper != null) {
            try {
                persistedDecision = decisionResultMapper.selectLatestByAnalysisId(analysisId);
            } catch (RuntimeException ignored) {
                persistedDecision = null;
            }
        }
        asset.setDecisionId(projectedDecision != null
                ? trimToNull(projectedDecision.getDecisionId())
                : persistedDecision == null ? null : trimToNull(persistedDecision.getDecisionId()));
        asset.setTraceId(trimToNull(run.getTraceId()));
        LocalDateTime calculatedAt = projectedDecision != null && projectedDecision.getCreateTime() != null
                ? projectedDecision.getCreateTime()
                : persistedDecision != null && persistedDecision.getCreateTime() != null
                ? persistedDecision.getCreateTime()
                : run.getCompletedAt() != null ? run.getCompletedAt() : run.getAnalysisTime();
        asset.setDirectionCalculatedAt(calculatedAt);
        if (calculatedAt != null) {
            asset.setDecisionAgeSeconds(Math.max(0L,
                    Duration.between(calculatedAt, LocalDateTime.now(planValidityClock)).getSeconds()));
        }
        asset.setProviderMatrixVersion(projectedDecision != null
                ? trimToNull(projectedDecision.getProviderMatrixVersion())
                : persistedDecision == null ? null : trimToNull(persistedDecision.getProviderMatrixVersion()));
        Integer persistedDataQuality = projectedDecision != null
                ? projectedDecision.getDataQualityScore()
                : persistedDecision == null ? run.getDataQualityScore() : persistedDecision.getDataQualityScore();
        asset.setDataQualityScore(persistedDataQuality);

        clearAnalysisMarketProvenance(asset);
        EvidenceItemDO marketEvidence = analysisMarketEvidence(analysisId, asset.getRawSymbol());
        if (marketEvidence == null) {
            failClosedDirectionProvenance(asset, "DIRECTION_MARKET_EVIDENCE_UNAVAILABLE");
            return;
        }
        String sourceTraceId = trimToNull(marketEvidence.getSourceTraceId());
        String evidenceProvider = trimToNull(marketEvidence.getSourceProvider());
        String provider = primaryPersistedOhlcvProvider();
        String marketType = primaryPersistedOhlcvMarketType();
        if (sourceTraceId == null || provider == null || marketType == null
                || persistedOhlcvBarMapper == null || run.getAnalysisTime() == null
                || !sameProviderFamily(evidenceProvider, provider)) {
            failClosedDirectionProvenance(asset, "DIRECTION_MARKET_PROVENANCE_UNAVAILABLE");
            return;
        }

        long analysisTimeMs = run.getAnalysisTime().toInstant(ZoneOffset.UTC).toEpochMilli();
        try {
            PersistedOhlcvBarDO price = analysisBoundClosedBar(
                    asset.getRawSymbol(), "5m", provider, marketType, analysisTimeMs);
            PersistedOhlcvBarDO oneHour = analysisBoundClosedBar(
                    asset.getRawSymbol(), "1h", provider, marketType, analysisTimeMs);
            PersistedOhlcvBarDO fourHour = analysisBoundClosedBar(
                    asset.getRawSymbol(), "4h", provider, marketType, analysisTimeMs);
            if (!analysisBoundSourceOwned(price, asset.getRawSymbol(), "5m", provider, marketType, analysisTimeMs)
                    || !analysisBoundSourceOwned(oneHour, asset.getRawSymbol(), "1h", provider, marketType,
                    analysisTimeMs)
                    || !analysisBoundSourceOwned(fourHour, asset.getRawSymbol(), "4h", provider, marketType,
                    analysisTimeMs)) {
                failClosedDirectionProvenance(asset, "DIRECTION_TIMEFRAME_PROVENANCE_UNAVAILABLE");
                return;
            }
            asset.setSourceId(sourceTraceId);
            asset.setProvider(provider);
            asset.setPriceObservedAt(closedAt(price));
            asset.setOneHourClosedAt(closedAt(oneHour));
            asset.setFourHourClosedAt(closedAt(fourHour));
            asset.setPriceAtDecision(price.getClosePrice());
            asset.setPriceDriftPct(priceDriftPct(asset.getPriceAtDecision(), asset.getLatestPrice()));
            boolean oneHourRecalculated = asset.getLatestOneHourClosedAt() != null
                    && asset.getOneHourClosedAt() != null
                    && !asset.getOneHourClosedAt().isBefore(asset.getLatestOneHourClosedAt());
            boolean fourHourIncluded = asset.getLatestFourHourClosedAt() != null
                    && asset.getFourHourClosedAt() != null
                    && !asset.getFourHourClosedAt().isBefore(asset.getLatestFourHourClosedAt());
            asset.setNewOneHourCloseRecalculation(oneHourRecalculated);
            asset.setLatestFourHourCloseIncluded(fourHourIncluded);
            boolean hasCurrentDirection = hasText(asset.getMarketBias());
            boolean latestClosedTimeframesIncluded = oneHourRecalculated && fourHourIncluded;
            PriceInvalidation priceInvalidation = evaluatePlanInvalidation(
                    asset.getPlanInvalidationLevel(), asset.getFinalMarketBias(), asset.getLatestPrice());
            if (hasCurrentDirection && !latestClosedTimeframesIncluded) {
                markDirectionAwaitingReanalysis(asset);
            }
            if (priceInvalidation.hit()) {
                markDirectionAwaitingReanalysis(asset);
            }
            if (priceInvalidation.hit()) {
                requestPlanRevalidationIfNeeded(asset, "PRICE_CROSSED_PLAN_INVALIDATION_LEVEL");
            } else if (hasCurrentDirection && !latestClosedTimeframesIncluded) {
                requestPlanRevalidationIfNeeded(asset, oneHourRecalculated
                        ? "NEW_4H_CLOSE_AFTER_DIRECTION_CALCULATION"
                        : "NEW_1H_CLOSE_AFTER_DIRECTION_CALCULATION");
            }
        } catch (RuntimeException ignored) {
            failClosedDirectionProvenance(asset, "DIRECTION_MARKET_PROVENANCE_UNAVAILABLE");
        }
    }

    private void failClosedDirectionProvenance(DashboardHomeVO.AssetVO asset, String reason) {
        clearAnalysisMarketProvenance(asset);
        if (!hasText(asset.getMarketBias())) return;
        markDirectionAwaitingReanalysis(asset);
        requestPlanRevalidationIfNeeded(asset, reason);
    }

    private void clearAnalysisMarketProvenance(DashboardHomeVO.AssetVO asset) {
        asset.setProvider(null);
        asset.setSourceId(null);
        asset.setPriceObservedAt(null);
        asset.setOneHourClosedAt(null);
        asset.setFourHourClosedAt(null);
        asset.setPriceAtDecision(null);
        asset.setPriceDriftPct(null);
        asset.setNewOneHourCloseRecalculation(null);
        asset.setLatestFourHourCloseIncluded(null);
    }

    private boolean sameAnalysisAsset(AnalysisRunDO run,
                                      DashboardHomeVO.AssetVO asset,
                                      HomeTopAssetProjection projection) {
        if (run == null || !Objects.equals(trimToNull(run.getAnalysisId()), trimToNull(projection.analysisId()))) {
            return false;
        }
        if (!Objects.equals(normalizeSymbol(run.getSymbol()), normalizeSymbol(asset.getRawSymbol()))) {
            return false;
        }
        return run.getAssetId() == null || asset.getAssetId() == null
                || Objects.equals(run.getAssetId(), asset.getAssetId());
    }

    private EvidenceItemDO analysisMarketEvidence(String analysisId, String symbol) {
        if (evidenceItemMapper == null || !hasText(analysisId) || !hasText(symbol)) return null;
        try {
            return evidenceItemMapper.listByAnalysisId(analysisId).stream()
                    .filter(Objects::nonNull)
                    .filter(row -> EvidenceTypeConstants.PRICE_STRUCTURE.equals(trimToNull(row.getEvidenceType())))
                    .filter(row -> EvidenceTypeConstants.EVIDENCE_SOURCE_MARKET_HEURISTIC.equals(
                            trimToNull(row.getSource())))
                    .filter(row -> hasText(row.getSourceProvider()) && hasText(row.getSourceTraceId()))
                    .max(Comparator.comparing(EvidenceItemDO::getObservedAt,
                            Comparator.nullsFirst(Comparator.naturalOrder())))
                    .orElse(null);
        } catch (RuntimeException ignored) {
            return null;
        }
    }

    private boolean sameProviderFamily(String evidenceProvider, String persistedProvider) {
        String evidenceFamily = providerFamily(evidenceProvider);
        String persistedFamily = providerFamily(persistedProvider);
        return evidenceFamily != null && evidenceFamily.equals(persistedFamily);
    }

    private String providerFamily(String provider) {
        String normalized = upper(provider);
        return switch (normalized) {
            case "BINANCE", "BINANCE_PUBLIC" -> "BINANCE";
            case "KRAKEN", "KRAKEN_PUBLIC" -> "KRAKEN";
            default -> trimToNull(normalized);
        };
    }

    private PersistedOhlcvBarDO analysisBoundClosedBar(String symbol,
                                                        String timeframe,
                                                        String provider,
                                                        String marketType,
                                                        long analysisTimeMs) {
        return persistedOhlcvBarMapper.selectLatestClosedBarBySourceAtOrBefore(
                symbol, timeframe, provider, marketType, analysisTimeMs);
    }

    private boolean analysisBoundSourceOwned(PersistedOhlcvBarDO row,
                                             String symbol,
                                             String timeframe,
                                             String provider,
                                             String marketType,
                                             long analysisTimeMs) {
        return row != null
                && Objects.equals(normalizeSymbol(row.getSymbol()), normalizeSymbol(symbol))
                && timeframe.equalsIgnoreCase(trimToNull(row.getTimeframe()))
                && provider.equalsIgnoreCase(trimToNull(row.getProvider()))
                && marketType.equalsIgnoreCase(trimToNull(row.getProviderMarketType()))
                && Boolean.TRUE.equals(row.getClosed())
                && hasText(row.getSourceTraceId())
                && "READY".equalsIgnoreCase(trimToNull(row.getSourceStatus()))
                && row.getCloseTimeMs() != null
                && row.getCloseTimeMs() > 0
                && row.getCloseTimeMs() <= analysisTimeMs;
    }

    private LocalDateTime closedAt(PersistedOhlcvBarDO row) {
        return row == null || row.getCloseTimeMs() == null || row.getCloseTimeMs() <= 0
                ? null
                : LocalDateTime.ofInstant(Instant.ofEpochMilli(row.getCloseTimeMs()), ZoneOffset.UTC);
    }

    private DashboardHomeVO.AssetVO assetFromObservation(int slot,
                                                          HomeTopAssetProjection projection,
                                                          Long userId) {
        if (projection == null || projection.assetId() == null || !hasText(projection.symbol())) {
            return null;
        }
        DashboardHomeVO.AssetVO asset = assetBase(slot, normalizeSymbol(projection.symbol()));
        String formalAnalysisId = authoritativeObservationAnalysisId(projection, userId);
        boolean missingFormalAnalysis = formalAnalysisId == null;
        String observationState = missingFormalAnalysis
                ? "NEVER_SCANNED" : trimToNull(projection.opportunityState());
        asset.setSlotType("OBSERVATION");
        asset.setAssetId(projection.assetId());
        asset.setName(trimToNull(projection.name()));
        asset.setAnalysisId(formalAnalysisId);
        asset.setOpportunityId(null);
        asset.setPrimaryOpportunityId(null);
        asset.setOpportunityScore(null);
        asset.setPlanMode(null);
        asset.setPrimaryPlanMode(null);
        asset.setAiDecisionResult(null);
        asset.setDataQualityScore(null);
        asset.setMarketBias(null);
        asset.setMarketBiasLabel("暂不可判断");
        asset.setCompositeScore(null);
        asset.setConfidenceLevel(null);
        asset.setConfidenceLabel("—");
        asset.setRiskLevel(null);
        asset.setRiskLabel("暂不可判断");
        asset.setWorthOpening(null);
        clearCardFinalProjection(asset);
        asset.setOpportunityState(observationState);
        asset.setAssetState(observationState);
        asset.setAssetStateLabel(observationStateLabel(observationState));
        asset.setPrimaryTimeframe(trimToNull(projection.primaryTimeframe()));
        asset.setSecondaryOpportunityCount(0);
        asset.setTimeframeConflictState(trimToNull(projection.timeframeConflictState()));
        asset.setRankingReason(trimToNull(projection.rankingReason()));
        asset.setLatestAnalysisTime(formalAnalysisId == null ? null : projection.analysisTime());
        asset.setCurrentConclusion(observationStateLabel(observationState));
        asset.setOneHourOpportunityLabel(unavailableTimeframeLabel("1小时", null, projection));
        asset.setFourHourTrendLabel(unavailableTimeframeLabel("4小时", null, projection));
        applyPersistedMarketData(asset, normalizeSymbol(projection.symbol()));
        asset.setDataFreshness(missingFormalAnalysis
                ? "NEVER_SCANNED" : trimToNull(projection.freshness()));
        asset.setUpdatedAt(maxTime(asset.getUpdatedAt(), asset.getLatestAnalysisTime()));
        setFieldSource(asset, "assetState", "REAL");
        setFieldSource(asset, "updatedAt", asset.getUpdatedAt() == null ? "MISSING" : "REAL");
        asset.setModuleState("NEVER_SCANNED".equalsIgnoreCase(observationState)
                ? "MISSING" : "PARTIAL");
        return asset;
    }

    private void applyNonFinalCardSemantics(DashboardHomeVO.AssetVO asset,
                                            DecisionResultVO decision,
                                            HomeTopAssetProjection projection) {
        if (asset == null) return;
        asset.setWorthOpening(null);
        String marketBias = trustedAnalysisBias(decision);
        if (marketBias == null) {
            asset.setMarketBias(null);
            asset.setMarketBiasLabel("暂不可判断");
            asset.setConfidenceLevel(null);
            asset.setConfidenceLabel("—");
            asset.setRiskLevel(null);
            asset.setRiskLabel("暂不可判断");
            asset.setOneHourOpportunityLabel(unavailableTimeframeLabel("1小时", decision, projection));
            asset.setFourHourTrendLabel(unavailableTimeframeLabel("4小时", decision, projection));
            setFieldSource(asset, "direction", analysisFieldState(decision));
            setFieldSource(asset, "confidence", "MISSING");
            setFieldSource(asset, "riskLevel", "MISSING");
            return;
        }

        asset.setMarketBias(marketBias);
        asset.setMarketBiasLabel(biasLabel(marketBias));
        Integer confidence = decision.getFinalConfidence();
        asset.setConfidenceLevel(confidence == null ? null : String.valueOf(confidence));
        asset.setConfidenceLabel(confidence == null ? "—" : confidence + "%");
        String riskLevel = trimToNull(decision.getRiskLevel());
        asset.setRiskLevel(riskLevel);
        asset.setRiskLabel(riskLevel == null ? "暂不可判断" : riskLabel(riskLevel));
        asset.setOneHourOpportunityLabel(decision.getOneHourOpportunityQuality() == null
                ? "1小时分析未完成" : oneHourOpportunityLabel(decision.getOneHourOpportunityQuality()));
        asset.setFourHourTrendLabel(decision.getFourHourTrendAlignment() == null
                ? "4小时分析未完成" : fourHourTrendLabel(marketBias, decision.getFourHourTrendAlignment()));
        setFieldSource(asset, "direction", "DERIVED");
        setFieldSource(asset, "confidence", confidence == null ? "MISSING" : "DERIVED");
        setFieldSource(asset, "riskLevel", hasText(asset.getRiskLevel()) ? "DERIVED" : "MISSING");
    }

    private boolean isObservationProjection(HomeTopAssetProjection projection) {
        return projection != null && hasText(projection.rankingReason())
                && projection.rankingReason().startsWith("SLOT_TYPE=OBSERVATION|");
    }

    private String trustedAnalysisBias(DecisionResultVO decision) {
        if (decision == null || !"READY".equals(upper(decision.getDirectionDataState()))) {
            return null;
        }
        String validated = trimToNull(decision.getValidatedMarketBias());
        if (validated != null) return upper(validated);
        String neutral = upper(decision.getMarketBiasHierarchy());
        return "RANGE".equals(neutral) || "WAIT".equals(neutral) ? neutral : null;
    }

    private String analysisFieldState(DecisionResultVO decision) {
        return switch (upper(decision == null ? null : decision.getDirectionDataState())) {
            case "INSUFFICIENT_DATA" -> "MISSING";
            case "STALE", "SOURCE_UNAVAILABLE", "MULTI_TIMEFRAME_CONFLICT" -> "INVALID";
            default -> "MISSING";
        };
    }

    private String unavailableTimeframeLabel(String timeframe,
                                             DecisionResultVO decision,
                                             HomeTopAssetProjection projection) {
        String state = upper(decision == null ? null : decision.getDirectionDataState());
        if (!hasText(state)) state = upper(projection == null ? null : projection.freshness());
        return switch (state) {
            case "INSUFFICIENT_DATA" -> timeframe + "数据不足";
            case "STALE" -> timeframe + "数据已过期";
            case "SOURCE_UNAVAILABLE" -> timeframe + "来源不可用";
            case "MULTI_TIMEFRAME_CONFLICT", "TIMEFRAME_CONFLICT" -> timeframe + "方向冲突";
            case "NEVER_SCANNED" -> timeframe + "等待分析";
            default -> timeframe + "分析未完成";
        };
    }

    private void clearCardFinalProjection(DashboardHomeVO.AssetVO asset) {
        asset.setHasFinal(false);
        asset.setFinalMarketBias(null);
        asset.setFinalPlanMode(null);
        asset.setFinalPlanLifecycle(null);
    }

    private String observationStateLabel(String value) {
        return switch (value == null ? "" : value.trim().toUpperCase(Locale.ROOT)) {
            case "NO_QUALIFIED_OPPORTUNITY" -> "暂无合格机会";
            case "STALE" -> "数据过期";
            case "NEVER_SCANNED" -> "等待首次扫描";
            case "RANGE" -> "震荡";
            case "WAIT" -> "观望";
            case "CANDIDATE" -> "计划生成中";
            case "HIGH_RISK" -> "高风险观察";
            case "BLOCKED", "CONFUSED" -> "当前受限";
            default -> "观察中";
        };
    }

    private String oneHourOpportunityLabel(Integer value) {
        if (value == null) return "1小时待验证";
        if (value >= 70) return "1小时机会较强";
        if (value >= 40) return "1小时机会形成";
        return "1小时机会较弱";
    }

    private String fourHourTrendLabel(String bias, Integer alignment) {
        if (alignment == null) return "4小时数据不足";
        if (MarketBiasPolicy.bullishFamily(bias)) return "4小时趋势偏多";
        if (MarketBiasPolicy.bearishFamily(bias)) return "4小时趋势偏空";
        return "4小时趋势震荡";
    }

    private String authoritativeObservationAnalysisId(HomeTopAssetProjection projection, Long userId) {
        if (projection == null || analysisRunMapper == null || userId == null || userId <= 0
                || !hasText(projection.analysisId()) || projection.assetId() == null) {
            return null;
        }
        try {
            AnalysisRunDO run = analysisRunMapper.selectById(projection.analysisId());
            return formalObservationRun(run, projection, userId) ? run.getAnalysisId() : null;
        } catch (RuntimeException ignored) {
            return null;
        }
    }

    private DecisionResultVO validatedObservationDecision(Long userId,
                                                          HomeTopAssetProjection projection,
                                                          DecisionResultVO decision) {
        if (projection == null || decision == null
                || !Objects.equals(trimToNull(projection.analysisId()), trimToNull(decision.getAnalysisId()))
                || !Objects.equals(normalizeSymbol(projection.symbol()), normalizeSymbol(decision.getSymbol()))) {
            return null;
        }
        return authoritativeObservationAnalysisId(projection, userId) == null ? null : decision;
    }

    private boolean formalObservationRun(AnalysisRunDO run,
                                         HomeTopAssetProjection projection,
                                         Long userId) {
        return run != null
                && !Boolean.TRUE.equals(run.getPreview())
                && "SUCCESS".equalsIgnoreCase(trimToNull(run.getStatus()))
                && AnalysisRunTriggerType.normalize(run.getTriggerType()) == AnalysisRunTriggerType.ASSET_POOL_SCAN
                && !"ANALYSIS_PREVIEW".equalsIgnoreCase(trimToNull(run.getAnalysisMode()))
                && "USER".equalsIgnoreCase(trimToNull(run.getOwnerType()))
                && Objects.equals(userId, run.getOwnerId())
                && Objects.equals(projection.assetId(), run.getAssetId())
                && Objects.equals(normalizeSymbol(projection.symbol()), normalizeSymbol(run.getSymbol()))
                && hasText(projection.primaryTimeframe())
                && hasText(run.getTimeframe())
                && projection.primaryTimeframe().trim().equalsIgnoreCase(run.getTimeframe().trim());
    }

    private void applyCardFinalProjection(DashboardHomeVO.AssetVO asset, DecisionResultVO decision) {
        asset.setHasFinal(false);
        if (executionPlanMapper == null || analysisRunMapper == null || opportunityLogService == null) {
            return;
        }
        AssetExecutionPlanResolution resolution = resolveAssetExecutionPlan(decision);
        if (!resolution.verified() || resolution.executionPlan() == null) {
            return;
        }
        ExecutionPlanDO plan = resolution.executionPlan();
        String lifecycle = trimToNull(plan.getPlanLifecycleState());
        asset.setPlanId(trimToNull(plan.getPlanId()));
        asset.setTraceId(firstNonBlank(trimToNull(plan.getTraceId()), resolution.sourceTraceId()));
        asset.setPlanInvalidationLevel(planInvalidationLevel(plan));
        asset.setPlanState(lifecycle);
        boolean visibleLifecycle = "CURRENT".equals(lifecycle) || "NEEDS_REVALIDATION".equals(lifecycle);
        boolean validated = Boolean.TRUE.equals(plan.getFinalPlan())
                && "PASS".equals(trimToNull(plan.getRuleValidationStatus()))
                && trimToNull(plan.getCandidateId()) != null
                && trimToNull(plan.getFinalMarketBias()) != null
                && trimToNull(plan.getFinalPlanMode()) != null;
        if (!validated || !visibleLifecycle) {
            return;
        }
        asset.setHasFinal(true);
        asset.setFinalMarketBias(trimToNull(plan.getFinalMarketBias()));
        asset.setFinalPlanMode(trimToNull(plan.getFinalPlanMode()));
        asset.setFinalPlanLifecycle(lifecycle);
    }

    private void markDirectionAwaitingReanalysis(DashboardHomeVO.AssetVO asset) {
        asset.setDirectionMaturity("STALE_REANALYSIS_REQUIRED");
        asset.setMarketBias(null);
        asset.setMarketBiasLabel("待重新分析");
        asset.setConfidenceLevel(null);
        asset.setConfidenceLabel("待重新计算");
        asset.setRiskLevel(null);
        asset.setRiskLabel("待重新计算");
        asset.setHasFinal(false);
        asset.setFinalMarketBias(null);
        asset.setFinalPlanMode(null);
        setFieldSource(asset, "direction", "INVALID");
        setFieldSource(asset, "confidence", "INVALID");
        setFieldSource(asset, "riskLevel", "INVALID");
    }

    private void requestPlanRevalidationIfNeeded(DashboardHomeVO.AssetVO asset, String reason) {
        if (asset == null || !hasText(asset.getPlanId())) return;
        if (!"CURRENT".equalsIgnoreCase(asset.getPlanState())) {
            if ("NEEDS_REVALIDATION".equalsIgnoreCase(asset.getPlanState())) return;
            return;
        }
        if (planRevalidationService == null) {
            asset.setPlanState("BLOCKED");
            return;
        }
        try {
            planRevalidationService.requestSystem(
                    asset.getPlanId(), PlanRevalidationTriggerTypeEnum.DATA_REFRESH, reason);
            asset.setPlanState("NEEDS_REVALIDATION");
        } catch (RuntimeException failure) {
            asset.setPlanState("BLOCKED");
        }
    }

    private String planInvalidationLevel(ExecutionPlanDO plan) {
        if (plan == null) return null;
        String structured = trimToNull(plan.getInvalidCondition());
        if (structured != null) {
            try {
                JsonNode root = objectMapper.readTree(structured);
                if (root != null && root.isObject()) {
                    JsonNode below = root.get("invalidPriceBelow");
                    if (below != null && below.isNumber()) return "invalidPriceBelow=" + below.decimalValue();
                    JsonNode above = root.get("invalidPriceAbove");
                    if (above != null && above.isNumber()) return "invalidPriceAbove=" + above.decimalValue();
                }
            } catch (Exception ignored) {
                // Human-readable invalidation text remains review-only; only structured numeric values are evaluated.
            }
        }
        BigDecimal stop = exactDecimal(plan.getStopLoss());
        return stop == null ? null : "stopLoss=" + stop.stripTrailingZeros().toPlainString();
    }

    private PriceInvalidation evaluatePlanInvalidation(String invalidationLevel,
                                                       String direction,
                                                       BigDecimal latestPrice) {
        if (!positive(latestPrice) || !hasText(invalidationLevel)) return PriceInvalidation.notHit();
        String[] pair = invalidationLevel.split("=", 2);
        if (pair.length != 2) return PriceInvalidation.notHit();
        BigDecimal level = exactDecimal(pair[1]);
        if (!positive(level)) return PriceInvalidation.notHit();
        boolean hit = switch (pair[0]) {
            case "invalidPriceBelow" -> latestPrice.compareTo(level) < 0;
            case "invalidPriceAbove" -> latestPrice.compareTo(level) > 0;
            case "stopLoss" -> MarketBiasPolicy.bearishFamily(direction)
                    ? latestPrice.compareTo(level) > 0
                    : MarketBiasPolicy.bullishFamily(direction) && latestPrice.compareTo(level) < 0;
            default -> false;
        };
        return new PriceInvalidation(hit, level);
    }

    private BigDecimal exactDecimal(String value) {
        String normalized = trimToNull(value);
        if (normalized == null || !normalized.matches("[+-]?\\d+(?:\\.\\d+)?")) return null;
        try {
            return new BigDecimal(normalized);
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    private BigDecimal priceDriftPct(BigDecimal priceAtDecision, BigDecimal latestPrice) {
        if (!positive(priceAtDecision) || !positive(latestPrice)) return null;
        return latestPrice.subtract(priceAtDecision)
                .multiply(BigDecimal.valueOf(100))
                .divide(priceAtDecision, 4, RoundingMode.HALF_UP);
    }

    private List<DashboardHomeVO.AssetVO> buildLegacyConstructorAssets(List<DecisionResultVO> decisions,
                                                                        DecisionResultVO selectedDecision,
                                                                        int limit) {
        List<DashboardHomeVO.AssetVO> assets = new ArrayList<>();
        LinkedHashSet<String> used = new LinkedHashSet<>();
        for (DecisionResultVO decision : decisions == null ? List.<DecisionResultVO>of() : decisions) {
            if (assets.size() >= limit) break;
            String symbol = normalizeSymbol(decision.getSymbol());
            if (symbol == null || !used.add(symbol)) continue;
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
        return assets;
    }

    private List<String> focusSymbols(Long userId, int limit) {
        if (assetPoolService == null) return List.of();
        try {
            List<String> symbols = userId == null
                    ? assetPoolService.listScanSymbols()
                    : assetPoolService.listFocusSymbols(userId, limit);
            return (symbols == null ? List.<String>of() : symbols).stream()
                    .map(this::normalizeSymbol)
                    .filter(Objects::nonNull)
                    .distinct()
                    .limit(Math.max(1, limit))
                    .toList();
        } catch (RuntimeException ignored) {
            return List.of();
        }
    }

    private HomeTopAssetProjection findRankingProjection(
            List<HomeTopAssetProjection> projections,
            String symbol) {
        String normalized = normalizeSymbol(symbol);
        if (normalized == null || projections == null) {
            return null;
        }
        return projections.stream()
                .filter(Objects::nonNull)
                .filter(projection -> normalized.equals(normalizeSymbol(projection.symbol())))
                .findFirst()
                .orElse(null);
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
        asset.setWorthOpening(finalPlanWorthOpening(decision.getPlanMode()));
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
        PersistedOhlcvBarDO latestOneHour = null;
        PersistedOhlcvBarDO latestFourHour = null;
        try {
            for (String timeframe : List.of("5m", "15m", "1h", "4h")) {
                List<PersistedOhlcvBarDO> rows = latestClosedWindowFromPrimarySource(symbol, timeframe, 1);
                PersistedOhlcvBarDO timeframeLatest = rows == null || rows.isEmpty() ? null : rows.get(0);
                timeframeFreshness.put(timeframe, timeframeLatest == null
                        ? "NO_DATA" : firstNonBlank(timeframeLatest.getFreshnessStatus(), "UNKNOWN"));
                if ("5m".equals(timeframe)) latest = timeframeLatest;
                if ("1h".equals(timeframe)) latestOneHour = timeframeLatest;
                if ("4h".equals(timeframe)) latestFourHour = timeframeLatest;
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
        asset.setLatestOneHourClosedAt(closedAt(latestOneHour));
        asset.setLatestFourHourClosedAt(closedAt(latestFourHour));
        asset.setMarketDataAsOf(maxTime(
                asset.getLatestPriceAt(), asset.getLatestOneHourClosedAt(), asset.getLatestFourHourClosedAt()));
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
            asset.setLatestPriceAt(closedAt(latest));
            setFieldSource(asset, "latestPrice", "REAL");
        }
        asset.setMarketDataAsOf(maxTime(
                asset.getLatestPriceAt(), asset.getLatestOneHourClosedAt(), asset.getLatestFourHourClosedAt()));
        boolean allFresh = timeframeFreshness.values().stream().allMatch("FRESH"::equalsIgnoreCase);
        boolean anyData = timeframeFreshness.values().stream().anyMatch(value -> !"NO_DATA".equals(value));
        if (!"ERROR".equals(asset.getDataFreshness())) {
            asset.setDataFreshness(allFresh ? "FRESH" : anyData ? "PARTIAL" : "NO_DATA");
        }
        asset.setSourceProvider(providerLabel(latest.getProvider()));
        asset.setUpdatedAt(latestBusinessTime(latest));
        setFieldSource(asset, "updatedAt", asset.getUpdatedAt() == null ? "MISSING" : "REAL");
    }

    private PersistedOhlcvBarDO latestClosedBarFromPrimarySource() {
        String provider = primaryPersistedOhlcvProvider();
        String marketType = primaryPersistedOhlcvMarketType();
        return provider == null || marketType == null || persistedOhlcvBarMapper == null
                ? null
                : persistedOhlcvBarMapper.selectLatestClosedBarBySource(provider, marketType);
    }

    private List<PersistedOhlcvBarDO> latestClosedWindowFromPrimarySource(
            String symbol,
            String timeframe,
            int limit
    ) {
        String provider = primaryPersistedOhlcvProvider();
        String marketType = primaryPersistedOhlcvMarketType();
        return provider == null || marketType == null || persistedOhlcvBarMapper == null
                ? List.of()
                : persistedOhlcvBarMapper.selectLatestClosedWindowBySource(
                        symbol, timeframe, provider, marketType, limit);
    }

    private String primaryPersistedOhlcvProvider() {
        return persistedOhlcvQueryService == null
                ? null : trimToNull(persistedOhlcvQueryService.primarySourceProvider());
    }

    private String primaryPersistedOhlcvMarketType() {
        return persistedOhlcvQueryService == null
                ? null : trimToNull(persistedOhlcvQueryService.primarySourceMarketType());
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

    private String canonicalAssetName(String symbol, String suppliedName) {
        String normalized = normalizeSymbol(symbol);
        if (normalized == null) return trimToNull(suppliedName);
        String base = normalized.endsWith("USDT") && normalized.length() > 4
                ? normalized.substring(0, normalized.length() - 4) : normalized;
        String supplied = trimToNull(suppliedName);
        if (supplied != null
                && !supplied.equalsIgnoreCase(base)
                && !normalizeSymbol(supplied).equals(normalized)) {
            return supplied;
        }
        return ASSET_DISPLAY_NAMES.getOrDefault(base, supplied != null ? supplied : base);
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
        LocalDateTime asOf = LocalDateTime.ofInstant(planValidityClock.instant(), ZoneOffset.UTC);
        for (UserPositionVO position : positions == null ? List.<UserPositionVO>of() : positions) {
            if (!isActiveManualPosition(position)) {
                continue;
            }
            MonitorReadResult monitorRead = latestPositionMonitorLog(userId, position.getId());
            PositionMonitorLogDTO latestMonitorLog = monitorRead.log();
            boolean trustedMonitor = trustedMonitor(latestMonitorLog, asOf);
            DashboardHomeVO.PositionVO row = new DashboardHomeVO.PositionVO();
            row.setPositionId(position.getId());
            row.setSymbol(toDisplaySymbol(position.getAssetSymbol()));
            row.setDirection(trimToNull(position.getSide()));
            row.setDirectionLabel(positionDirectionLabel(position.getSide()));
            row.setEntryPrice(position.getEntryPrice());
            row.setLeverage(position.getLeverage());
            row.setPositionSize(position.getQuantity());
            row.setPositionStatus(trimToNull(position.getStatus()));
            row.setPositionStatusLabel(positionStatusLabel(position.getStatus()));
            row.setUserStopLoss(position.getStopLoss());
            row.setUserTakeProfit(position.getTakeProfit());
            row.setSystemSuggestedStopLoss(null);
            row.setSystemSuggestedTakeProfit(null);
            row.setUpdatedAt(position.getUpdatedAt());
            row.setOpenedAt(position.getOpenedAt());
            row.setNextMonitorAt(null);
            row.setSourceRefId(trimToNull(position.getSourceRefId()));
            row.setSourceType(normalizedPositionSource(position));
            row.setFinalPlanId(trimToNull(position.getFinalPlanId()));
            if (trustedMonitor) {
                applyTrustedMonitor(row, position, latestMonitorLog);
                row.setMonitorTrustState("VERIFIED_FRESH");
            } else {
                applyWaitingMonitor(row, latestMonitorLog, asOf);
            }
            if (trustedMonitor
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
            row.setWarningState(positionWarningState(row, monitorRead.failed()));
            row.setModuleState(positionModuleState(row, trustedMonitor, monitorRead.failed()));
            rows.add(row);
        }
        rows.sort(positionPriorityComparator());
        List<DashboardHomeVO.PositionVO> allRows = List.copyOf(rows);
        List<DashboardHomeVO.PositionVO> topRows = allRows.stream().limit(3).toList();
        return new PositionRowsResult(allRows, topRows, Map.copyOf(trustedSources), positionMonitoringState(allRows));
    }

    private boolean trustedMonitor(PositionMonitorLogDTO monitor, LocalDateTime asOf) {
        return monitor != null
                && monitor.isTrustedAndFreshAt(asOf)
                && positive(monitor.getCurrentPrice())
                && hasText(monitor.getMarkPriceSource())
                && recognizedEntryLogicStatus(monitor.getEntryLogicStatus())
                && recognizedReversalStatus(monitor.getReversalStatus())
                && recognizedRiskReason(monitor.getRiskChangeReason())
                && recognizedPositionRisk(monitor.getRiskLevel())
                && recognizedRiskTrend(monitor.getRiskTrend())
                && recognizedMonitorOutcome(monitor);
    }

    private void applyTrustedMonitor(DashboardHomeVO.PositionVO row,
                                     UserPositionVO position,
                                     PositionMonitorLogDTO monitor) {
        BigDecimal markPrice = monitor.getCurrentPrice();
        row.setMarkPrice(markPrice);
        row.setCurrentPrice(markPrice);
        row.setMarkPriceSource(trimToNull(monitor.getMarkPriceSource()));
        row.setMarkPriceObservedAt(monitor.getObservedAt());
        row.setMarkPriceFresh(true);
        applyPositionPnl(row, position, markPrice);
        row.setMonitorConclusion(trimToNull(monitor.getMonitorConclusion()));
        row.setMonitorConclusionLabel(monitorConclusionLabel(row.getMonitorConclusion()));
        row.setEntryLogicStatus(trimToNull(monitor.getEntryLogicStatus()));
        row.setEntryLogicStatusLabel(entryLogicStatusLabel(row.getEntryLogicStatus()));
        row.setDirectionSupportStatus(directionSupportStatus(row.getEntryLogicStatus()));
        row.setDirectionSupportStatusLabel(directionSupportStatusLabel(row.getDirectionSupportStatus()));
        row.setReversalStatus(trimToNull(monitor.getReversalStatus()));
        row.setReversalStatusLabel(reversalStatusLabel(row.getReversalStatus()));
        row.setRiskLevel(trimToNull(monitor.getRiskLevel()));
        row.setRiskLevelLabel(positionRiskLevelLabel(row.getRiskLevel()));
        row.setRiskTrend(trimToNull(monitor.getRiskTrend()));
        row.setRiskReason(trimToNull(monitor.getRiskChangeReason()));
        row.setRiskReasonLabel(riskReasonLabel(row.getRiskReason()));
        row.setSuggestedAction(trimToNull(monitor.getSuggestedAction()));
        row.setSuggestedManualAction(row.getSuggestedAction());
        row.setSuggestedManualActionText(suggestedActionText(row.getSuggestedAction()));
        row.setLastMonitorAt(monitor.getCreatedAt());
        row.setLastMonitorTime(monitor.getCreatedAt());
        row.setDataState(positionDataState(row));
    }

    private void applyWaitingMonitor(DashboardHomeVO.PositionVO row,
                                     PositionMonitorLogDTO monitor,
                                     LocalDateTime asOf) {
        row.setMarkPrice(null);
        row.setCurrentPrice(null);
        row.setMarkPriceSource(null);
        row.setMarkPriceObservedAt(null);
        row.setMarkPriceFresh(false);
        row.setFloatingPnl(null);
        row.setPnlPct(null);
        row.setPnlAmount(null);
        row.setPnlPercent(null);
        row.setAccountImpactPct(null);
        row.setMonitorConclusion(null);
        row.setMonitorConclusionLabel(null);
        row.setEntryLogicStatus(null);
        row.setEntryLogicStatusLabel(null);
        row.setDirectionSupportStatus(null);
        row.setDirectionSupportStatusLabel(null);
        row.setReversalStatus(null);
        row.setReversalStatusLabel(null);
        row.setRiskLevel(null);
        row.setRiskLevelLabel(null);
        row.setRiskTrend(null);
        row.setRiskReason(null);
        row.setRiskReasonLabel(null);
        row.setSuggestedAction(null);
        row.setSuggestedManualAction(null);
        row.setSuggestedManualActionText(null);
        row.setLastMonitorAt(null);
        row.setLastMonitorTime(null);
        row.setDataState("WAITING_MONITOR_DATA");
        row.setMonitorTrustState(monitorTrustState(monitor, asOf));
    }

    private String monitorTrustState(PositionMonitorLogDTO monitor, LocalDateTime asOf) {
        if (monitor == null) return "SOURCE_UNAVAILABLE";
        String sourceStatus = upper(monitor.getMonitorSourceStatus());
        if ("PENDING_VERIFICATION".equals(sourceStatus)) return "PENDING";
        if ("INVALID".equals(sourceStatus)) return "INVALID";
        if ("VERIFIED".equals(sourceStatus)
                && monitor.getFreshUntil() != null
                && asOf != null
                && !asOf.isBefore(monitor.getFreshUntil())) {
            return "STALE";
        }
        return "INVALID";
    }

    private String normalizedPositionSource(UserPositionVO position) {
        String source = upper(position == null ? null : position.getSourceType());
        if ("SYSTEM_PLAN_POSITION".equals(source)) {
            return trimToNull(position.getFinalPlanId()) == null ? "SOURCE_UNAVAILABLE" : source;
        }
        if ("MANUAL".equals(source) || "MANUAL_POSITION".equals(source)
                || "MANUAL_INDEPENDENT".equals(source)) {
            return "MANUAL_INDEPENDENT";
        }
        return "SOURCE_UNAVAILABLE";
    }

    private boolean isManualPosition(UserPositionVO position) {
        if (position == null) return false;
        String sourceType = trimToNull(position.getSourceType());
        return "MANUAL".equalsIgnoreCase(sourceType)
                || "MANUAL_POSITION".equalsIgnoreCase(sourceType)
                || "MANUAL_INDEPENDENT".equalsIgnoreCase(sourceType)
                || "SYSTEM_PLAN_POSITION".equalsIgnoreCase(sourceType);
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

    private String positionWarningState(DashboardHomeVO.PositionVO row, boolean readFailed) {
        if (readFailed) return "ERROR";
        if (row == null || "WAITING_MONITOR_DATA".equals(row.getDataState())) return "MISSING";
        String risk = upper(row.getRiskLevel());
        String conclusion = upper(row.getMonitorConclusion());
        if ("HIGH".equals(risk) || "EXTREME".equals(risk)) return "HIGH_RISK";
        if ("PLAN_INVALIDATED".equals(conclusion)
                || "WAIT_USER_CONFIRM_CLOSE".equals(conclusion)) return "PLAN_INVALIDATED";
        if ("LOGIC_WEAKENED".equals(conclusion) || "NEAR_STOP_LOSS".equals(conclusion)) return "WATCH";
        return "NONE";
    }

    private String positionModuleState(DashboardHomeVO.PositionVO row,
                                       boolean trustedMonitor,
                                       boolean readFailed) {
        if (readFailed) return "ERROR";
        if (row == null || row.getPositionId() == null || !hasText(row.getSymbol())
                || !hasText(row.getDirection()) || !positive(row.getEntryPrice())
                || !positive(row.getPositionSize()) || !hasText(row.getPositionStatus())) {
            return "ERROR";
        }
        if (!trustedMonitor || row.getLeverage() == null || row.getUserStopLoss() == null
                || row.getUserTakeProfit() == null || row.getUpdatedAt() == null) {
            return "PARTIAL";
        }
        return "READY";
    }

    private boolean recognizedEntryLogicStatus(String status) {
        return switch (upper(status)) {
            case "STILL_VALID", "WEAKENED", "INVALIDATED", "NOT_APPLICABLE" -> true;
            default -> false;
        };
    }

    private boolean recognizedMonitorConclusion(String status) {
        return switch (upper(status)) {
            case "LOGIC_VALID", "LOGIC_WEAKENED", "PLAN_INVALIDATED", "NEAR_STOP_LOSS",
                    "NEAR_TAKE_PROFIT", "HIGH_RISK_OBSERVATION", "WAIT_USER_CONFIRM_CLOSE" -> true;
            default -> false;
        };
    }

    private boolean recognizedReversalStatus(String status) {
        return switch (upper(status)) {
            case "NO_REVERSAL", "WEAK_REVERSAL", "STRONG_REVERSAL" -> true;
            default -> false;
        };
    }

    private boolean recognizedRiskReason(String status) {
        return switch (upper(status)) {
            case "NO_CLEAR_RISK_FACTOR", "OPPOSING_EVIDENCE_INCREASED", "STRUCTURE_CHANGED",
                    "EVENT_IMPACT", "DATA_QUALITY_DEGRADED" -> true;
            default -> false;
        };
    }

    private boolean recognizedSuggestedAction(String status) {
        return switch (upper(status)) {
            case "CONTINUE_HOLD", "NO_ADD_POSITION", "REDUCE_POSITION", "TIGHTEN_STOP", "MOVE_STOP",
                    "PARTIAL_TAKE_PROFIT", "WAIT_CONFIRMATION", "RECORD_CLOSE_REVIEW" -> true;
            default -> false;
        };
    }

    private boolean recognizedPositionRisk(String riskLevel) {
        return switch (upper(riskLevel)) {
            case "LOW", "MEDIUM", "HIGH", "EXTREME" -> true;
            default -> false;
        };
    }

    private boolean recognizedRiskTrend(String riskTrend) {
        return switch (upper(riskTrend)) {
            case "STABLE", "INCREASED", "SHARPLY_INCREASED" -> true;
            default -> false;
        };
    }

    private boolean recognizedMonitorActionPair(PositionMonitorLogDTO monitor) {
        try {
            PositionMonitorConclusionEnum conclusion = PositionMonitorConclusionEnum.valueOf(
                    upper(monitor.getMonitorConclusion()));
            PositionMonitorSuggestedActionEnum action = PositionMonitorSuggestedActionEnum.valueOf(
                    upper(monitor.getSuggestedAction()));
            return action.isAllowedFor(conclusion);
        } catch (IllegalArgumentException | NullPointerException ex) {
            return false;
        }
    }

    private boolean recognizedMonitorOutcome(PositionMonitorLogDTO monitor) {
        boolean conclusionPresent = hasText(monitor.getMonitorConclusion());
        boolean actionPresent = hasText(monitor.getSuggestedAction());
        if ("NOT_APPLICABLE".equals(upper(monitor.getEntryLogicStatus()))) {
            if (!conclusionPresent && !actionPresent) {
                return true;
            }
            return conclusionPresent && actionPresent
                    && recognizedMonitorConclusion(monitor.getMonitorConclusion())
                    && recognizedSuggestedAction(monitor.getSuggestedAction())
                    && recognizedMonitorActionPair(monitor);
        }
        return conclusionPresent && actionPresent
                && recognizedMonitorConclusion(monitor.getMonitorConclusion())
                && recognizedSuggestedAction(monitor.getSuggestedAction())
                && recognizedMonitorActionPair(monitor);
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
        row.setPnlPercent(pnlPct);
        if (positive(position.getQuantity())) {
            BigDecimal unitPnl = "SHORT".equalsIgnoreCase(side)
                    ? position.getEntryPrice().subtract(currentPrice)
                    : currentPrice.subtract(position.getEntryPrice());
            BigDecimal pnlAmount = unitPnl.multiply(position.getQuantity());
            row.setFloatingPnl(pnlAmount);
            row.setPnlAmount(pnlAmount);
            row.setPnlCoverage("MARK_PRICE_ENTRY_QUANTITY_ONLY");
            row.setFeeCoverage("UNKNOWN");
            row.setFundingCoverage("UNKNOWN");
            row.setPartialFillCoverage("UNKNOWN");
            row.setPositionAdditionCoverage("UNKNOWN");
        }
    }

    private String directionSupportStatus(String entryLogicStatus) {
        return switch (upper(entryLogicStatus)) {
            case "STILL_VALID" -> "SUPPORTED";
            case "WEAKENED" -> "WEAKENED";
            case "INVALIDATED" -> "NOT_SUPPORTED";
            case "NOT_APPLICABLE" -> "NOT_APPLICABLE";
            default -> null;
        };
    }

    private String suggestedActionText(String suggestedAction) {
        return switch (upper(suggestedAction)) {
            case "CONTINUE_HOLD" -> "继续持有";
            case "NO_ADD_POSITION" -> "暂不加仓";
            case "REDUCE_POSITION" -> "降低仓位";
            case "TIGHTEN_STOP" -> "收紧止损";
            case "MOVE_STOP" -> "移动止损";
            case "PARTIAL_TAKE_PROFIT" -> "分批止盈";
            case "WAIT_CONFIRMATION" -> "等待人工确认";
            case "RECORD_CLOSE_REVIEW" -> "记录平仓并进入复盘";
            default -> null;
        };
    }

    private String positionDataState(DashboardHomeVO.PositionVO row) {
        String conclusion = upper(row.getMonitorConclusion());
        if ("PLAN_INVALIDATED".equals(conclusion)) {
            return "PLAN_INVALIDATED";
        }
        if ("INCREASED".equals(upper(row.getRiskTrend()))
                || "SHARPLY_INCREASED".equals(upper(row.getRiskTrend()))) {
            return "RISK_ESCALATED";
        }
        return "OPEN_MONITORING";
    }

    private String positionMonitoringState(List<DashboardHomeVO.PositionVO> rows) {
        if (rows == null || rows.isEmpty()) return "NO_POSITION";
        if (rows.stream().anyMatch(row -> "PLAN_INVALIDATED".equals(row.getDataState()))) {
            return "PLAN_INVALIDATED";
        }
        if (rows.stream().anyMatch(row -> "RISK_ESCALATED".equals(row.getDataState()))) {
            return "RISK_ESCALATED";
        }
        if (rows.stream().anyMatch(row -> "WAITING_MONITOR_DATA".equals(row.getDataState()))) {
            return "WAITING_MONITOR_DATA";
        }
        return "OPEN_MONITORING";
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
        PersistedPlanState planState = ExecutionPlanReviewPolicy.currentProjectionPlanState(
                executionPlan,
                LocalDateTime.ofInstant(planValidityClock.instant(), ZoneOffset.UTC));
        if (planState != PersistedPlanState.ACTIVE) {
            blockPersistedAssetPlan(suggestion, executionPlan, planState);
            return suggestion;
        }
        PlanValidity planValidity = resolvePlanValidity(executionPlan);
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
        suggestion.setStatusLabel("完整执行计划");
        suggestion.setModuleState("READY");
        suggestion.setSourceAnalysisId(assetPlan.analysisId());
        suggestion.setSourceExecutionPlanId(assetPlan.executionPlanId());
        suggestion.setSourceTraceId(assetPlan.sourceTraceId());
        suggestion.setDirection(trimToNull(executionPlan.getFinalMarketBias()));
        suggestion.setFinalMarketBias(trimToNull(executionPlan.getFinalMarketBias()));
        suggestion.setFinalPlanMode(trimToNull(executionPlan.getFinalPlanMode()));
        suggestion.setWorthOpening(finalPlanWorthOpening(executionPlan.getFinalPlanMode()));
        suggestion.setOpportunityType(trimToNull(executionPlan.getOpportunityType()));
        suggestion.setRecommendedAction(trimToNull(executionPlan.getRecommendedAction()));
        suggestion.setEntryLogic(trimPlanValue(executionPlan.getEntryLogic()));
        suggestion.setEntryZone(trimPlanValue(executionPlan.getEntryZone()));
        suggestion.setTriggerCondition(trimPlanValue(executionPlan.getTriggerCondition()));
        suggestion.setStopLogic(trimPlanValue(executionPlan.getStopLogic()));
        suggestion.setStopZone(trimPlanValue(executionPlan.getStopLoss()));
        suggestion.setStopLoss(trimPlanValue(executionPlan.getStopLoss()));
        suggestion.setTargetLogic(trimPlanValue(executionPlan.getTargetLogic()));
        suggestion.setTargetZones(trimPlanValue(executionPlan.getTakeProfitRules()));
        suggestion.setTakeProfitRules(trimPlanValue(executionPlan.getTakeProfitRules()));
        suggestion.setAddCondition(trimPlanValue(executionPlan.getAddPositionCondition()));
        suggestion.setReduceCondition(trimPlanValue(executionPlan.getReducePositionCondition()));
        suggestion.setAbandonCondition(trimPlanValue(executionPlan.getAbandonCondition()));
        suggestion.setLeverageSuggestion(planLeverageLabel(executionPlan.getLeverageLimit()));
        suggestion.setPositionSuggestion(trimToNull(executionPlan.getPositionLimit()));
        suggestion.setExpectedRiskReward(executionPlan.getExpectedRiskReward());
        suggestion.setAnalysisTimeframes(trimToNull(executionPlan.getAnalysisTimeframesJson()));
        suggestion.setTriggerTimeframe(trimToNull(executionPlan.getTriggerTimeframe()));
        suggestion.setHoldingHorizon(trimToNull(executionPlan.getHoldingHorizon()));
        suggestion.setValidationStatus(trimToNull(executionPlan.getRuleValidationStatus()));
        suggestion.setValidationReasons(trimToNull(executionPlan.getValidationReasons()));
        suggestion.setDowngradeReason(trimToNull(executionPlan.getDowngradeReason()));
        suggestion.setRuleVetoReason(trimToNull(executionPlan.getRuleVetoReason()));
        suggestion.setSourceStatus(trimToNull(executionPlan.getSourceStatus()));
        suggestion.setChainStatus(trimToNull(executionPlan.getChainStatus()));
        suggestion.setCandidateId(trimToNull(executionPlan.getCandidateId()));
        suggestion.setResolverResultId(trimToNull(executionPlan.getResolverResultId()));
        suggestion.setValidationResultId(trimToNull(executionPlan.getValidationResultId()));
        suggestion.setFinalPlan(Boolean.TRUE.equals(executionPlan.getFinalPlan()));
        suggestion.setExecutionFeasibilityStatus(trimToNull(executionPlan.getExecutionFeasibilityStatus()));
        suggestion.setExecutionFeasibilityReason(trimToNull(executionPlan.getExecutionFeasibilityReason()));
        suggestion.setValidPeriod(planValidityDisplay(planValidity));
        suggestion.setValidFrom(planValidity.validFrom());
        suggestion.setExpiresAt(planValidity.expiresAt());
        suggestion.setInvalidCondition(trimPlanValue(executionPlan.getInvalidCondition()));
        suggestion.setPlanLifecycleState(trimToNull(executionPlan.getPlanLifecycleState()));
        suggestion.setPlanVersion(executionPlan.getPlanVersion());
        suggestion.setNeedsRevalidation(Boolean.TRUE.equals(executionPlan.getNeedsRevalidation()));
        suggestion.setRevalidationReason(trimToNull(executionPlan.getRevalidationReason()));
        suggestion.setRevalidationRule(trimToNull(executionPlan.getRevalidationRule()));
        suggestion.setNotTradeInstruction(Boolean.TRUE.equals(executionPlan.getNotTradeInstruction()));
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
        boolean recoveryConditionRequired = synthesis != null
                && (!"CONFIRMATION".equalsIgnoreCase(trimToNull(synthesis.finalPlanMode()))
                || !"LEVEL_1_CONSISTENT".equalsIgnoreCase(trimToNull(synthesis.conflictLevel())));
        boolean consistencyAvailable = aiApplicable && synthesis != null
                && hasText(synthesis.conflictLevel())
                && hasText(synthesis.finalMarketBias())
                && hasText(synthesis.finalPlanMode())
                && hasText(synthesis.mainReason())
                && (!recoveryConditionRequired || hasText(synthesis.recoveryCondition()));
        consistency.setDataState(consistencyAvailable
                ? "READY"
                : payload == null || roleStats.successful() == 0
                ? "SOURCE_UNAVAILABLE"
                : "INSUFFICIENT_DATA");
        consistency.setConflictLevel(consistencyAvailable
                ? trimToNull(synthesis.conflictLevel()) : null);
        consistency.setFinalMarketBias(consistencyAvailable
                ? trimToNull(synthesis.finalMarketBias()) : null);
        consistency.setFinalPlanMode(consistencyAvailable
                ? trimToNull(synthesis.finalPlanMode()) : null);
        consistency.setMainReason(consistencyAvailable
                ? trimToNull(synthesis.mainReason()) : null);
        consistency.setRecoveryCondition(consistencyAvailable
                ? trimToNull(synthesis.recoveryCondition()) : null);
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
        copyFormalAiContract(tab, rolePayload);
        String callStatus = firstNonBlank(trimToNull(rolePayload.callStatus()), "NOT_CALLED");
        tab.setRunStatus(callStatus);
        tab.setRunStatusLabel(aiRunStatusLabel(callStatus));
        boolean resultAvailable = Boolean.TRUE.equals(rolePayload.resultAvailable())
                && ("READY".equalsIgnoreCase(rolePayload.roleState())
                || "PARTIAL".equalsIgnoreCase(rolePayload.roleState()))
                && "SUCCESS".equalsIgnoreCase(callStatus);
        tab.setResultAvailable(resultAvailable);
        tab.setStatusMessage(aiRoleStatusMessage(callStatus));
        tab.setStance(trimToNull(rolePayload.stance()));
        if (resultAvailable) {
            populateLegacyJavaProjection(tab, role, rolePayload);
        }
        return tab;
    }

    // Kept only for Java callers compiled against the pre-v4.1 VO. These fields are
    // JsonIgnored and never participate in the API or frontend semantic contract.
    private void populateLegacyJavaProjection(DashboardHomeVO.AiTabVO tab,
                                              String role,
                                              AiRoleResultsPayload.RolePayload payload) {
        switch (role) {
            case "GPT_FINAL" -> {
                AiRoleResultsPayload.CoreJudgment judgment = payload.coreJudgment();
                AiRoleResultsPayload.CandidateSummary candidate = payload.candidateSummary();
                tab.setFinalMarketBias(judgment == null ? null : judgment.marketBias());
                tab.setDirection(judgment == null ? null : judgment.marketBias());
                tab.setFinalConclusion(judgment == null ? null : judgment.text());
                tab.setReviewConclusion(judgment == null ? null : judgment.text());
                tab.setFinalConfidence(candidate == null ? null : candidate.confidence());
                tab.setConfidenceLevel(candidate == null ? null : candidate.confidence());
                tab.setFinalRiskLevel(candidate == null ? null : candidate.riskLevel());
                tab.setFinalPlanMode(candidate == null ? null : candidate.planMode());
                tab.setWorthOpening(candidate == null ? null : worthOpeningLabel(candidate.worthOpening()));
                tab.setDecisionSummary(candidate == null ? null : candidate.summary());
                tab.setCoreSupportingEvidence(evidenceTexts(payload.supportingEvidence()));
                tab.setSupportEvidence(evidenceTexts(payload.supportingEvidence()));
                tab.setCoreCounterEvidence(evidenceTexts(payload.opposingEvidence()));
                tab.setAgainstEvidence(evidenceTexts(payload.opposingEvidence()));
                tab.setDowngradeReason(payload.biasAdjustment() == null
                        ? null : payload.biasAdjustment().reason());
            }
            case "GEMINI_REVIEW" -> {
                tab.setReviewVerdict(trimToNull(payload.reviewResult()));
                tab.setDetectedContradictions(findingTexts(payload.logicConflicts()));
                tab.setWeakEvidence(findingTexts(payload.evidenceGaps()));
                tab.setLogicGaps(findingTexts(payload.logicConflicts()));
                tab.setDowngradeRecommendation(payload.downgradeSuggestion() == null
                        ? null : payload.downgradeSuggestion().reason());
                tab.setReviewConclusion(payload.downgradeSuggestion() == null
                        ? null : payload.downgradeSuggestion().reason());
                tab.setRiskAdjustmentSuggestion(trimToNull(payload.riskAdjustment()));
                tab.setManualReviewRequired(Boolean.TRUE.equals(payload.manualReviewRequired()) ? "是" : null);
            }
            case "GROK_CHALLENGE" -> {
                tab.setChallengeThesis(trimToNull(payload.challengeSummary()));
                tab.setChallengeConclusion(trimToNull(payload.currentDirectionChallenge()));
                tab.setReviewConclusion(trimToNull(payload.currentDirectionChallenge()));
                tab.setEventRisks(findingTexts(payload.externalEventRisks()));
                tab.setSentimentReversalRisks(findingTexts(payload.opposingScenarios()));
                tab.setMicrostructureTraps(findingTexts(payload.microstructureRisks()));
                tab.setCounterEvidence(findingTexts(payload.opposingScenarios()));
            }
            default -> {
            }
        }
    }

    private void copyFormalAiContract(DashboardHomeVO.AiTabVO tab,
                                      AiRoleResultsPayload.RolePayload role) {
        tab.setAnalysisId(role.analysisId());
        tab.setTraceId(role.traceId());
        tab.setRoleState(role.roleState());
        tab.setDataState(role.dataState());
        tab.setGeneratedAt(role.generatedAt());
        tab.setProvider(role.provider());
        tab.setSourceRole(role.sourceRole());
        tab.setReasonCodes(role.reasonCodes());
        tab.setFallback(role.fallback());
        tab.setFallbackReason(role.fallbackReason());
        tab.setCoreJudgment(role.coreJudgment());
        tab.setSupportingEvidence(role.supportingEvidence());
        tab.setSupportingEvidenceState(role.supportingEvidenceState());
        tab.setOpposingEvidence(role.opposingEvidence());
        tab.setOpposingEvidenceState(role.opposingEvidenceState());
        tab.setMultiTimeframeExplanation(role.multiTimeframeExplanation());
        tab.setBiasAdjustment(role.biasAdjustment());
        tab.setCandidateSummary(role.candidateSummary());
        tab.setEvidenceGaps(role.evidenceGaps());
        tab.setEvidenceGapsState(role.evidenceGapsState());
        tab.setLogicConflicts(role.logicConflicts());
        tab.setLogicConflictsState(role.logicConflictsState());
        tab.setUnderestimatedRisks(role.underestimatedRisks());
        tab.setUnderestimatedRisksState(role.underestimatedRisksState());
        tab.setDowngradeSuggestion(role.downgradeSuggestion());
        tab.setReviewResult(role.reviewResult());
        tab.setFinalDirectionImpact(role.finalDirectionImpact());
        tab.setConfidenceAdjustment(role.confidenceAdjustment());
        tab.setRiskAdjustment(role.riskAdjustment());
        tab.setPlanModeAdjustment(role.planModeAdjustment());
        tab.setRecoveryCondition(role.recoveryCondition());
        tab.setFailurePaths(role.failurePaths());
        tab.setFailurePathState(role.failurePathState());
        tab.setOpposingScenarios(role.opposingScenarios());
        tab.setOpposingScenariosState(role.opposingScenariosState());
        tab.setExternalEventRisks(role.externalEventRisks());
        tab.setExternalEventRisksState(role.externalEventRisksState());
        tab.setMicrostructureRisks(role.microstructureRisks());
        tab.setMicrostructureRisksState(role.microstructureRisksState());
        tab.setWatchIndicators(role.watchIndicators());
        tab.setWatchIndicatorsState(role.watchIndicatorsState());
        tab.setChallengeSummary(role.challengeSummary());
        tab.setCurrentDirectionChallenge(role.currentDirectionChallenge());
        tab.setMajorCounterEvidence(role.majorCounterEvidence());
        tab.setPlanModeImpact(role.planModeImpact());
    }

    private static List<String> evidenceTexts(List<AiRoleResultsPayload.EvidencePayload> evidence) {
        if (evidence == null) return List.of();
        return evidence.stream().map(item -> item == null ? null : item.currentValue())
                .filter(Objects::nonNull).filter(value -> !value.isBlank()).toList();
    }

    private static List<String> findingTexts(List<AiRoleResultsPayload.FindingPayload> findings) {
        if (findings == null) return List.of();
        return findings.stream().map(item -> item == null ? null : item.text())
                .filter(Objects::nonNull).filter(value -> !value.isBlank()).toList();
    }

    private String worthOpeningLabel(Boolean worthOpening) {
        if (worthOpening == null) return null;
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
                                                           ProviderReadinessVO providerReadiness,
                                                           PositionRowsResult positionRows) {
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
        diagnostics.setAccountRiskCoverageState(accountRiskCoverage(positionRows));
        diagnostics.setProviderReadiness(providerReadiness);
        return diagnostics;
    }

    private String accountRiskCoverage(PositionRowsResult positionRows) {
        List<DashboardHomeVO.PositionVO> positions = positionRows == null
                ? List.of() : positionRows.allRows();
        if (positions.isEmpty()) return "UNKNOWN";
        long trusted = positions.stream()
                .filter(row -> "VERIFIED_FRESH".equalsIgnoreCase(trimToNull(row.getMonitorTrustState())))
                .count();
        if (trusted == positions.size()) return "COMPLETE";
        return trusted > 0 ? "PARTIAL_COVERAGE" : "UNKNOWN";
    }

    private DashboardHomeVO.PositionAggregateVO buildPositionAggregate(PositionRowsResult positionRows) {
        List<DashboardHomeVO.PositionVO> positions = positionRows == null
                ? List.of() : positionRows.allRows();
        DashboardHomeVO.PositionAggregateVO aggregate = new DashboardHomeVO.PositionAggregateVO();
        aggregate.setActiveCount(positions.size());
        aggregate.setHighestTrustedRisk(positions.stream()
                .filter(row -> "VERIFIED_FRESH".equalsIgnoreCase(trimToNull(row.getMonitorTrustState())))
                .map(DashboardHomeVO.PositionVO::getRiskLevel)
                .filter(this::recognizedPositionRisk)
                .max(Comparator.comparingInt(this::positionRiskRank))
                .orElse(null));
        aggregate.setCoverageState(accountRiskCoverage(positionRows));
        return aggregate;
    }

    private String safeAccountRiskCoverage(DecisionResultVO decision) {
        if (accountRiskSnapshotMapper == null || decision == null || !hasText(decision.getAnalysisId())) {
            return "UNKNOWN";
        }
        try {
            TmAccountRiskSnapshotDO snapshot = accountRiskSnapshotMapper
                    .selectLatestByAnalysisId(decision.getAnalysisId());
            return snapshot == null || !hasText(snapshot.getAccountRiskCoverageState())
                    ? "UNKNOWN" : upper(snapshot.getAccountRiskCoverageState());
        } catch (RuntimeException ignored) {
            return "UNKNOWN";
        }
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

    private RankingReadResult safeHomeRanking(Long userId, int limit) {
        try {
            List<HomeTopAssetProjection> ranked = opportunityPriorityRankingService.rankForHome(userId, limit);
            return new RankingReadResult(ranked == null ? List.of() : List.copyOf(ranked), false);
        } catch (RuntimeException ignored) {
            return new RankingReadResult(List.of(), true);
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
        int poolSize = assetPoolService == null
                ? localRealReadinessService.assets().size()
                : assetPoolService.listScanSymbols().size();
        return "真实行情资产 " + ready + "/" + poolSize + " · Kraken" + suffix;
    }

    private String providerLabel(String provider) {
        String normalized = upper(provider);
        if (normalized.startsWith("KRAKEN")) return "Kraken";
        if (normalized.startsWith("BINANCE")) return "Binance";
        if (normalized.startsWith("COINGLASS")) return "CoinGlass v4";
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
        if ("HIGH_RISK".equals(assetState)) return "高风险观察";
        if ("CONFUSED".equals(assetState)) return "冲突状态，等待人工复核";
        if ("EXTREME".equals(upper(decision.getRiskLevel()))) return "当前风险极高";
        if ("HIGH".equals(upper(decision.getRiskLevel()))) return "当前风险较高";
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
        return "CANDIDATE".equals(state) || "WAITING_TRIGGER".equals(state)
                || "TRIGGERED".equals(state) || "HIGH_RISK".equals(state);
    }

    private static Boolean finalPlanWorthOpening(String finalPlanMode) {
        try {
            return switch (PlanModeEnum.require(finalPlanMode)) {
                case CONFIRMATION, PREPARATION, REDUCED -> true;
                case OBSERVATION, BLOCKED -> false;
            };
        } catch (RuntimeException ignored) {
            return null;
        }
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

    private PlanValidity resolvePlanValidity(ExecutionPlanDO executionPlan) {
        if (executionPlan == null) return PlanValidity.invalid();
        LocalDateTime validFrom = executionPlan.getValidFrom();
        LocalDateTime validUntil = executionPlan.getValidUntil();
        if (validFrom == null || validUntil == null) return PlanValidity.invalid();
        return evaluatePlanValidity(validFrom.atOffset(ZoneOffset.UTC), validUntil.atOffset(ZoneOffset.UTC));
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

    private String planValidityDisplay(PlanValidity validity) {
        if (validity == null || validity.validFrom() == null || validity.expiresAt() == null) {
            return null;
        }
        return OFFSET_PLAN_TIME_FORMAT.format(validity.validFrom())
                + " ~ " + OFFSET_PLAN_TIME_FORMAT.format(validity.expiresAt());
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

    private String entryLogicStatusLabel(String status) {
        return switch (upper(status)) {
            case "STILL_VALID" -> "仍成立";
            case "WEAKENED" -> "弱化";
            case "INVALIDATED" -> "失效";
            case "NOT_APPLICABLE" -> "N/A";
            default -> null;
        };
    }

    private String monitorConclusionLabel(String status) {
        return switch (upper(status)) {
            case "LOGIC_VALID" -> "逻辑仍成立";
            case "LOGIC_WEAKENED" -> "逻辑弱化";
            case "PLAN_INVALIDATED" -> "计划失效";
            case "NEAR_STOP_LOSS" -> "接近止损";
            case "NEAR_TAKE_PROFIT" -> "接近止盈";
            case "HIGH_RISK_OBSERVATION" -> "高风险观察";
            case "WAIT_USER_CONFIRM_CLOSE" -> "等待用户确认平仓";
            default -> null;
        };
    }

    private String directionSupportStatusLabel(String status) {
        return switch (upper(status)) {
            case "SUPPORTED" -> "当前方向仍获支持";
            case "WEAKENED" -> "方向支持减弱";
            case "NOT_SUPPORTED" -> "当前方向不再获支持";
            case "NOT_APPLICABLE" -> "N/A";
            default -> null;
        };
    }

    private String reversalStatusLabel(String status) {
        return switch (upper(status)) {
            case "NO_REVERSAL" -> "无明显反转";
            case "WEAK_REVERSAL" -> "弱反转";
            case "STRONG_REVERSAL" -> "强反转";
            default -> null;
        };
    }

    private String positionRiskLevelLabel(String status) {
        return riskLabel(status);
    }

    private String riskReasonLabel(String status) {
        return switch (upper(status)) {
            case "NO_CLEAR_RISK_FACTOR" -> "暂无明显风险因素";
            case "OPPOSING_EVIDENCE_INCREASED" -> "反向证据增加";
            case "STRUCTURE_CHANGED" -> "结构变化";
            case "EVENT_IMPACT" -> "事件冲击";
            case "DATA_QUALITY_DEGRADED" -> "数据质量下降";
            default -> null;
        };
    }

    private AiRoleStats aiRoleStats(AiRoleResultsPayload payload) {
        if (payload == null) return new AiRoleStats(0, 0, 0, 0);
        int successful = 0;
        int support = 0;
        int challenge = 0;
        int abstain = 0;
        for (AiRoleResultsPayload.RolePayload role : payload.roles().values()) {
            if (role == null || !"SUCCESS".equalsIgnoreCase(role.callStatus())
                    || !("READY".equalsIgnoreCase(role.roleState())
                    || "PARTIAL".equalsIgnoreCase(role.roleState()))) continue;
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
            case "LEVEL_2_MINOR_DISAGREEMENT", "LEVEL_2_LIGHT_DIVERGENCE", "LEVEL_2_REVIEW" -> "轻微分歧";
            case "LEVEL_3_SIGNIFICANT_DISAGREEMENT", "LEVEL_3_SIGNIFICANT_DIVERGENCE", "LEVEL_3_DIVERGENCE" -> "显著分歧";
            case "LEVEL_4_EXTREME_CONFLICT", "LEVEL_4_EXTREME_DIVERGENCE" -> "极端分歧";
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
            case "low_leverage" -> "低杠杆";
            case "moderate_leverage" -> "适中杠杆";
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

    private record PriceInvalidation(boolean hit, BigDecimal level) {
        private static PriceInvalidation notHit() {
            return new PriceInvalidation(false, null);
        }
    }

    private record DecisionReadResult(List<DecisionResultVO> rows, boolean failed) {
    }

    private record RankingReadResult(List<HomeTopAssetProjection> rows, boolean failed) {
    }

    private record DecisionLookupResult(DecisionResultVO decision, boolean failed) {
    }

    private record PositionReadResult(List<UserPositionVO> rows, boolean failed) {
    }

    private record MonitorReadResult(PositionMonitorLogDTO log, boolean failed) {
    }

    private record SchedulerRuntimeProjection(String state,
                                              String label,
                                              Instant heartbeatAt,
                                              Instant startedAt,
                                              Instant completedAt,
                                              Instant nextScheduledAt,
                                              String result,
                                              String failureReason) {
        private static SchedulerRuntimeProjection unknown() {
            return new SchedulerRuntimeProjection(
                    "UNKNOWN", "状态未知", null, null, null, null, null, null);
        }
    }

    private record AssetStateResolution(String value, String sourceStatus) {
    }

    private record PositionRowsResult(
            List<DashboardHomeVO.PositionVO> allRows,
            List<DashboardHomeVO.PositionVO> topRows,
            Map<Long, PositionPlanSourceResolver.Resolution> trustedSources,
            String monitoringState) {
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
