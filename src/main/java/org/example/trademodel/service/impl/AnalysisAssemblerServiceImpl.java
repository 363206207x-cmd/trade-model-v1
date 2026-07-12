package org.example.trademodel.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.example.trademodel.analysisrun.AnalysisExecutionContext;
import org.example.trademodel.analysisrun.AnalysisRunIds;
import org.example.trademodel.analysisrun.AnalysisRunInputException;
import org.example.trademodel.analysisrun.AnalysisTimePolicy;
import org.example.trademodel.analysisrun.AnalysisRunTriggerType;
import org.example.trademodel.market.RealMarketEnvironmentService;
import org.example.trademodel.market.PersistedRealMarketEnvironmentAssessment;
import org.example.trademodel.market.PersistedRealMarketEnvironmentService;
import org.example.trademodel.common.EvidenceTypeConstants;
import org.example.trademodel.dto.ohlcv.PersistedOhlcvReadinessResult;
import org.example.trademodel.dto.ohlcv.PersistedOhlcvReadinessStatus;
import org.example.trademodel.derivatives.DerivativesBusinessAssessment;
import org.example.trademodel.derivatives.DerivativesBusinessInput;
import org.example.trademodel.derivatives.DerivativesBusinessIntegrationService;
import org.example.trademodel.derivatives.DerivativesEvidenceItem;
import org.example.trademodel.derivatives.DerivativesEvidenceType;
import org.example.trademodel.derivatives.DerivativesSnapshotReadPort;
import org.example.trademodel.dto.planboundary.MarketStructureBoundaryDTO;
import org.example.trademodel.dto.planboundary.MarketStructureBoundaryRequest;
import org.example.trademodel.dto.planboundary.RuntimeKlineContextDTO;
import org.example.trademodel.dto.planboundary.SourceTraceBoundaryProducerResult;
import org.example.trademodel.requestcontext.RequestIdSupport;
import org.example.trademodel.entity.*;
import org.example.trademodel.enums.AssetStateEnum;
import org.example.trademodel.enums.HotResetEventTypeEnum;
import org.example.trademodel.mapper.*;
import org.example.trademodel.service.*;
import org.example.trademodel.service.planboundary.MarketStructureBoundaryExtractor;
import org.example.trademodel.service.planboundary.SourceTraceBoundaryProducer;
import org.example.trademodel.vo.*;
import org.example.trademodel.entity.RuleConfigDO;
import org.example.trademodel.providercall.AssetPriority;
import org.example.trademodel.providercall.ProviderCallResult;
import org.example.trademodel.providercall.snapshot.DerivativesRiskSnapshot;
import org.example.trademodel.risk.UserPositionRiskAdapter;
import org.example.trademodel.risk.UserPositionRiskResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.Duration;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.UUID;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

@Service
public class AnalysisAssemblerServiceImpl implements AnalysisAssemblerService {
    private static final AtomicBoolean FIRST_ANALYSIS_RUN_LOGGED = new AtomicBoolean(false);
    private static final ObjectMapper EXPLAIN_JSON = new ObjectMapper();
    private static final Logger log = LoggerFactory.getLogger(AnalysisAssemblerServiceImpl.class);
    private static final DateTimeFormatter VALID_PERIOD_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final EvidenceService evidenceService;
    private final ScoreService scoreService;
    private final PlanService planService;
    private final DecisionEngineService decisionEngineService;   // 新增：使用我们修复的AI决策引擎
    private final RealMarketEnvironmentService realMarketEnvironmentService;
    private final AssetStateService assetStateService;
    private final RuleConfigService ruleConfigService;

    private final AnalysisRunMapper analysisRunMapper;
    private final EvidenceItemMapper evidenceItemMapper;
    private final ScoreItemMapper scoreItemMapper;
    private final DecisionResultMapper decisionResultMapper;
    private final ExecutionPlanMapper executionPlanMapper;
    private final AccountRiskSnapshotMapper accountRiskSnapshotMapper;
    private final MarketEnvironmentSnapshotMapper marketEnvironmentSnapshotMapper;
    private final PushSnapshotService pushSnapshotService;
    private final MonitorAlertWriteService monitorAlertWriteService;
    private final HotResetService hotResetService;
    private final MissedOpportunityService missedOpportunityService;
    private final OpportunityLogService opportunityLogService;
    private final PersistedOhlcvQueryService persistedOhlcvQueryService;
    private final RuntimeKlineContextAssemblyService runtimeKlineContextAssemblyService;
    private final MarketStructureBoundaryExtractor marketStructureBoundaryExtractor;
    private final SourceTraceBoundaryProducer sourceTraceBoundaryProducer;
    private DerivativesSnapshotReadPort derivativesSnapshotReadPort;
    private DerivativesBusinessIntegrationService derivativesBusinessIntegrationService;
    private UserPositionRiskAdapter userPositionRiskAdapter;
    private PersistedRealMarketEnvironmentService persistedRealMarketEnvironmentService;
    private boolean requireRealMarketEnvironment;

    private static final String KEY_ACTIVE_VERSION_FALLBACK = "rule.active_version_fallback";
    private static final String DEFAULT_ACTIVE_RULE_VERSION = "v1.0";
    private static final String MARKET_ENV_SOURCE_HEURISTIC = "BINANCE_24H_HEURISTIC";
    /** 现货 24h + USDⓈ-M {@code lastFundingRate} 均成功时的最小启发式（见 {@link RealMarketEnvironmentService}）。 */
    private static final String MARKET_ENV_SOURCE_SPOT_PERP_MIN = "BINANCE_SPOT_PERP_MIN_HEURISTIC";
    /** 现货 24h + USDⓈ-M {@code openInterest} 成功，Funding 未并入（见 {@code OI_MINIMAL_ACCESS_CONTRACT.md}）。 */
    private static final String MARKET_ENV_SOURCE_USDM_OI_MIN = "BINANCE_USDM_OI_MIN_HEURISTIC";
    /** 现货 24h + Funding + OI 附录均成功（见 {@code OI_MINIMAL_ACCESS_CONTRACT.md}）。 */
    private static final String MARKET_ENV_SOURCE_SPOT_PERP_OI_MIN = "BINANCE_SPOT_PERP_OI_MIN_HEURISTIC";
    private static final String MARKET_ENV_SOURCE_FALLBACK = "PLACEHOLDER_FALLBACK";
    private static final int BOUNDARY_REQUIRED_WINDOW_SIZE = 50;
    private static final int BOUNDARY_MIN_BARS = 7;
    private static final int BOUNDARY_MAX_TARGETS = 2;
    private static final long BOUNDARY_FALLBACK_MAX_READ_LAG_MS = 15L * 60_000L;
    private static final String BOUNDARY_DIRECTION_LONG = "LONG";
    private static final String BOUNDARY_DIRECTION_SHORT = "SHORT";

    /**
     * 现货启发式已成功时写入快照的 {@code source_type}（Funding / OI 附录组合见 {@code OI_MINIMAL_ACCESS_CONTRACT.md} 组合表）。
     */
    static String marketEnvSourceTypeForSuccessfulQuote(MarketEnvironmentVO quoteEnv) {
        if (quoteEnv == null) {
            return MARKET_ENV_SOURCE_HEURISTIC;
        }
        boolean funding = Boolean.TRUE.equals(quoteEnv.getPerpFundingApplied());
        boolean oi = Boolean.TRUE.equals(quoteEnv.getOiApplied());
        if (funding && oi) {
            return MARKET_ENV_SOURCE_SPOT_PERP_OI_MIN;
        }
        if (oi) {
            return MARKET_ENV_SOURCE_USDM_OI_MIN;
        }
        if (funding) {
            return MARKET_ENV_SOURCE_SPOT_PERP_MIN;
        }
        return MARKET_ENV_SOURCE_HEURISTIC;
    }

    public AnalysisAssemblerServiceImpl(EvidenceService evidenceService, ScoreService scoreService,
                                        PlanService planService,
                                        DecisionEngineService decisionEngineService,
                                        RealMarketEnvironmentService realMarketEnvironmentService,
                                        AssetStateService assetStateService,
                                        RuleConfigService ruleConfigService,
                                        AnalysisRunMapper analysisRunMapper,
                                        EvidenceItemMapper evidenceItemMapper,
                                        ScoreItemMapper scoreItemMapper,
                                        DecisionResultMapper decisionResultMapper,
                                        ExecutionPlanMapper executionPlanMapper,
                                        AccountRiskSnapshotMapper accountRiskSnapshotMapper,
                                        MarketEnvironmentSnapshotMapper marketEnvironmentSnapshotMapper,
                                        PushSnapshotService pushSnapshotService,
                                        MonitorAlertWriteService monitorAlertWriteService,
                                        HotResetService hotResetService,
                                        MissedOpportunityService missedOpportunityService,
                                        OpportunityLogService opportunityLogService) {
        this(evidenceService, scoreService, planService, decisionEngineService, realMarketEnvironmentService,
                assetStateService, ruleConfigService, analysisRunMapper, evidenceItemMapper, scoreItemMapper,
                decisionResultMapper, executionPlanMapper, accountRiskSnapshotMapper,
                marketEnvironmentSnapshotMapper, pushSnapshotService, monitorAlertWriteService, hotResetService,
                missedOpportunityService, opportunityLogService, null, null, null, null);
    }

    @Autowired
    public AnalysisAssemblerServiceImpl(EvidenceService evidenceService, ScoreService scoreService,
                                        PlanService planService,
                                        DecisionEngineService decisionEngineService,
                                        RealMarketEnvironmentService realMarketEnvironmentService,
                                        AssetStateService assetStateService,
                                        RuleConfigService ruleConfigService,
                                        AnalysisRunMapper analysisRunMapper,
                                        EvidenceItemMapper evidenceItemMapper,
                                        ScoreItemMapper scoreItemMapper,
                                        DecisionResultMapper decisionResultMapper,
                                        ExecutionPlanMapper executionPlanMapper,
                                        AccountRiskSnapshotMapper accountRiskSnapshotMapper,
                                        MarketEnvironmentSnapshotMapper marketEnvironmentSnapshotMapper,
                                        PushSnapshotService pushSnapshotService,
                                        MonitorAlertWriteService monitorAlertWriteService,
                                        HotResetService hotResetService,
                                        MissedOpportunityService missedOpportunityService,
                                        OpportunityLogService opportunityLogService,
                                        PersistedOhlcvQueryService persistedOhlcvQueryService,
                                        RuntimeKlineContextAssemblyService runtimeKlineContextAssemblyService,
                                        MarketStructureBoundaryExtractor marketStructureBoundaryExtractor,
                                        SourceTraceBoundaryProducer sourceTraceBoundaryProducer) {
        this.evidenceService = evidenceService;
        this.scoreService = scoreService;
        this.planService = planService;
        this.decisionEngineService = decisionEngineService;
        this.realMarketEnvironmentService = realMarketEnvironmentService;
        this.assetStateService = assetStateService;
        this.ruleConfigService = ruleConfigService;
        this.analysisRunMapper = analysisRunMapper;
        this.evidenceItemMapper = evidenceItemMapper;
        this.scoreItemMapper = scoreItemMapper;
        this.decisionResultMapper = decisionResultMapper;
        this.executionPlanMapper = executionPlanMapper;
        this.accountRiskSnapshotMapper = accountRiskSnapshotMapper;
        this.marketEnvironmentSnapshotMapper = marketEnvironmentSnapshotMapper;
        this.pushSnapshotService = pushSnapshotService;
        this.monitorAlertWriteService = monitorAlertWriteService;
        this.hotResetService = hotResetService;
        this.missedOpportunityService = missedOpportunityService;
        this.opportunityLogService = opportunityLogService;
        this.persistedOhlcvQueryService = persistedOhlcvQueryService;
        this.runtimeKlineContextAssemblyService = runtimeKlineContextAssemblyService;
        this.marketStructureBoundaryExtractor = marketStructureBoundaryExtractor;
        this.sourceTraceBoundaryProducer = sourceTraceBoundaryProducer;
    }

    @Autowired(required = false)
    void setDerivativesBusinessIntegration(DerivativesSnapshotReadPort derivativesSnapshotReadPort,
                                           DerivativesBusinessIntegrationService derivativesBusinessIntegrationService,
                                           UserPositionRiskAdapter userPositionRiskAdapter) {
        this.derivativesSnapshotReadPort = derivativesSnapshotReadPort;
        this.derivativesBusinessIntegrationService = derivativesBusinessIntegrationService;
        this.userPositionRiskAdapter = userPositionRiskAdapter;
    }

    @Autowired(required = false)
    void setPersistedRealMarketEnvironmentService(
            PersistedRealMarketEnvironmentService persistedRealMarketEnvironmentService) {
        this.persistedRealMarketEnvironmentService = persistedRealMarketEnvironmentService;
    }

    @Value("${trade-model.analysis.require-real-market-environment:false}")
    void setRequireRealMarketEnvironment(boolean requireRealMarketEnvironment) {
        this.requireRealMarketEnvironment = requireRealMarketEnvironment;
    }


    @Override
    @Transactional
    public AssetAnalysisVO assemble(String symbol, String timeframe) {
        throw new IllegalStateException("DIRECT_ASSEMBLER_ENTRY_DISABLED");
    }

    @Override
    @Transactional
    public AssetAnalysisVO assemble(AnalysisExecutionContext context) {
        return assembleInternal(context);
    }

    private AssetAnalysisVO assembleInternal(AnalysisExecutionContext context) {
        long assembleStart = System.currentTimeMillis();
        AnalysisExecutionContext effectiveContext = normalizeExecutionContext(context);
        String analysisId = effectiveContext.getAnalysisId();
        String symbol = effectiveContext.getSymbol();
        String timeframe = effectiveContext.getTimeframe();
        System.out.println("=== 开始执行 assemble 方法 === analysisId=" + analysisId
                + ", traceId=" + effectiveContext.getTraceId()
                + ", symbol=" + symbol + ", timeframe=" + timeframe);

        try {
            MarketEnvironmentVO marketEnv = new MarketEnvironmentVO();
            marketEnv.setSummary("Real K-line data from Binance");
            String marketEnvSourceType = MARKET_ENV_SOURCE_FALLBACK;
            MarketEnvironmentVO quoteEnv = realMarketEnvironmentService == null ? null
                    : realMarketEnvironmentService.tryBuildFromRealQuote(symbol, timeframe).orElse(null);
            if (quoteEnv != null) {
                marketEnv = quoteEnv;
                enrichOpenInterestDeltaFromPreviousSnapshot(marketEnv, symbol, timeframe);
                marketEnv.setDerivativesCrowdingState(
                        RealMarketEnvironmentService.computeDerivativesCrowdingState(marketEnv));
                marketEnvSourceType = marketEnvSourceTypeForSuccessfulQuote(quoteEnv);
                log.info("[market-env] assemble uses Binance market-env heuristic symbol={} tf={} sourceType={}",
                        symbol, timeframe, marketEnvSourceType);
            } else {
                PersistedRealMarketEnvironmentAssessment persistedEnvironment =
                        persistedRealMarketEnvironmentService == null ? null
                                : persistedRealMarketEnvironmentService.assess(symbol, timeframe);
                if (persistedEnvironment != null && persistedEnvironment.ready()) {
                    marketEnv = persistedEnvironment.environment();
                    marketEnvSourceType = persistedEnvironment.sourceType();
                    log.info("[market-env] assemble uses persisted real OHLCV symbol={} tf={} provider={} sourceType={} closedBars={}",
                            symbol, timeframe, persistedEnvironment.provider(), marketEnvSourceType,
                            persistedEnvironment.closedBarCount());
                } else if (requireRealMarketEnvironment) {
                    String reason = persistedEnvironment == null
                            ? "REAL_MARKET_PROVENANCE_INCOMPLETE" : persistedEnvironment.reasonCode();
                    log.warn("[market-env] real environment unavailable symbol={} tf={} reason={}",
                            symbol, timeframe, reason);
                    throw new IllegalStateException("REAL_MARKET_ENVIRONMENT_REQUIRED");
                } else {
                    log.info("[market-env] assemble fallback placeholder symbol={} tf={}", symbol, timeframe);
                }
            }

            AssetAnalysisVO scoreInput = new AssetAnalysisVO();
            scoreInput.setAnalysisId(analysisId);
            scoreInput.setSymbol(symbol);
            scoreInput.setTimeframe(timeframe);
            scoreInput.setAnalysisTime(effectiveContext.getAnalysisTime().toString());
            List<EvidenceItemVO> evidences = evidenceService.buildEvidence(scoreInput, marketEnv);
            scoreInput.setEvidenceList(evidences);
            List<ScoreItemVO> scores = scoreService.buildScoreList(scoreInput, marketEnv);
            int baseDataQualityScore = estimateDataQualityScore(evidences, scores, marketEnvSourceType);
            DerivativesBusinessInput derivativesInput = buildDerivativesBusinessInput(
                    effectiveContext, baseDataQualityScore, false);
            DerivativesBusinessAssessment derivativesAssessment = evaluateDerivatives(derivativesInput);
            if (derivativesAssessment != null) {
                evidences.addAll(derivativesBusinessIntegrationService.toEvidenceVos(derivativesAssessment));
                derivativesBusinessIntegrationService.applyScoreAdjustments(scores, derivativesAssessment);
            }
            int dataQualityScore = derivativesAssessment == null
                    ? baseDataQualityScore
                    : Math.max(0, baseDataQualityScore - derivativesAssessment.dataQualityDiscount());
            Integer trendStructureScore = extractTrendStructureScore(scores);
            Integer eightScoreComposite = calculateEightScoreComposite(scores);

            DecisionBundleVO decision = decisionEngineService.makeDecision(
                    symbol,
                    timeframe,
                    analysisId,
                    dataQualityScore,
                    trendStructureScore,
                    scoreInput.getEventImpactInput(),
                    derivativesAssessment,
                    eightScoreComposite);
            if (derivativesAssessment != null) {
                derivativesBusinessIntegrationService.applyDecisionAdjustments(decision, derivativesAssessment);
            }

            ExecutionPlanVO plan = generateExecutionPlanFailClosed(
                    decision,
                    scores,
                    marketEnv,
                    scoreInput,
                    effectiveContext);
            if (derivativesInput != null && derivativesAssessment != null) {
                derivativesAssessment = evaluateDerivatives(withPlanBoundary(
                        derivativesInput, hasCompletePlanBoundary(plan)));
                derivativesBusinessIntegrationService.applyOpportunityState(decision, derivativesAssessment);
                derivativesBusinessIntegrationService.applyPlanAdjustments(plan, derivativesAssessment);
                decision.setAssetStateSnapshot(assetStateService.buildSnapshotAtDecision(
                        symbol, analysisId, decision.getAssetState(),
                        decision.getConfusedScore() == null ? 0 : decision.getConfusedScore(),
                        decision.isMultiTimeframeAligned()));
            }

            AssetAnalysisVO analysis = new AssetAnalysisVO();
            analysis.setAnalysisId(analysisId);
            analysis.setSymbol(symbol);
            analysis.setTimeframe(timeframe);
            analysis.setAnalysisTime(effectiveContext.getAnalysisTime().toString());
            analysis.setMarketEnvironment(marketEnv);
            analysis.setEvidenceList(evidences);
            analysis.setScoreList(scores);
            analysis.setDecisionBundle(decision);
            analysis.setDataQualityScore(dataQualityScore);
            analysis.setEventImpactInput(scoreInput.getEventImpactInput());
            analysis.setDerivativesAssessment(derivativesAssessment);

            System.out.println("=== 准备执行落库 saveToDatabase === analysisId=" + analysisId);
            saveToDatabase(effectiveContext, analysis, evidences, scores, decision, plan, marketEnvSourceType);
            System.out.println("=== 落库执行完成 === analysisId=" + analysisId);

            return analysis;
        } catch (Exception e) {
            System.err.println("=== assemble 方法异常 === " + e.getClass().getName() + ": " + e.getMessage());
            e.printStackTrace();
            throw e;
        } finally {
            long assembleCostMs = System.currentTimeMillis() - assembleStart;
            if (FIRST_ANALYSIS_RUN_LOGGED.compareAndSet(false, true)) {
                System.out.println("[PERF] first_analysis_run=" + assembleCostMs + " ms");
            }
        }
    }

    private DerivativesBusinessAssessment evaluateDerivatives(DerivativesBusinessInput input) {
        if (input == null || derivativesBusinessIntegrationService == null) return null;
        return derivativesBusinessIntegrationService.evaluate(input);
    }

    private DerivativesBusinessInput buildDerivativesBusinessInput(
            AnalysisExecutionContext context, int dataQualityScore, boolean planBoundaryComplete) {
        if (context == null || derivativesSnapshotReadPort == null
                || derivativesBusinessIntegrationService == null || persistedOhlcvQueryService == null) {
            return null;
        }
        ProviderCallResult<DerivativesRiskSnapshot> snapshotResult;
        try {
            snapshotResult = derivativesSnapshotReadPort.readCached(context.getSymbol(), AssetPriority.P1_CORE,
                    Duration.ofSeconds(60), context.getTraceId());
        } catch (RuntimeException failure) {
            snapshotResult = null;
        }
        Map<String, String> directions = new java.util.LinkedHashMap<>();
        BigDecimal currentPrice = null;
        BigDecimal comparisonPrice = null;
        boolean volumeConfirmed = false;
        boolean currentPriceFresh = false;
        for (String tf : List.of("5m", "15m", "1h", "4h")) {
            PersistedOhlcvReadinessResult readiness = persistedOhlcvQueryService.evaluateReadiness(
                    context.getSymbol(), tf, 3, maxBoundaryReadLagMs(tf));
            if (readiness == null || readiness.getStatus() != PersistedOhlcvReadinessStatus.FRESH
                    || readiness.getBars() == null || readiness.getBars().isEmpty()) {
                continue;
            }
            PersistedOhlcvBarDO latest = readiness.getBars().get(0);
            if (latest.getOpenPrice() != null && latest.getClosePrice() != null) {
                directions.put(tf, latest.getClosePrice().compareTo(latest.getOpenPrice()) >= 0
                        ? "BULLISH" : "BEARISH");
            }
            if ("5m".equals(tf) && latest.getClosePrice() != null) {
                currentPrice = latest.getClosePrice();
                currentPriceFresh = currentPrice.compareTo(BigDecimal.ZERO) > 0;
                if (readiness.getBars().size() > 1) {
                    comparisonPrice = readiness.getBars().get(1).getClosePrice();
                    BigDecimal currentVolume = latest.getVolume();
                    BigDecimal previousVolume = readiness.getBars().get(1).getVolume();
                    volumeConfirmed = currentVolume != null && previousVolume != null
                            && currentVolume.compareTo(previousVolume) >= 0;
                }
            }
        }
        String baseDirection = directions.getOrDefault("4h", directions.get("1h"));
        return new DerivativesBusinessInput(context.getSymbol(), baseDirection, currentPrice, comparisonPrice,
                volumeConfirmed, directions, currentPriceFresh, dataQualityScore, accountRiskAllowed(),
                planBoundaryComplete, false,
                null, snapshotResult == null ? null : snapshotResult.payload(), context.getTraceId(),
                context.getAnalysisId(), context.getRuleVersion());
    }

    private boolean accountRiskAllowed() {
        if (userPositionRiskAdapter == null) return false;
        try {
            UserPositionRiskResult result = userPositionRiskAdapter.currentRisk();
            return result != null && !result.isRiskBlocked();
        } catch (RuntimeException failure) {
            return false;
        }
    }

    private static DerivativesBusinessInput withPlanBoundary(DerivativesBusinessInput input,
                                                             boolean planBoundaryComplete) {
        return new DerivativesBusinessInput(input.symbol(), input.baseDirection(), input.currentPrice(),
                input.comparisonPrice(), input.volumeConfirmed(), input.timeframeDirections(), input.currentPriceFresh(),
                input.dataQualityScore(), input.accountRiskAllowed(), planBoundaryComplete, input.positionOpen(),
                input.currentState(), input.snapshot(), input.traceId(), input.analysisId(), input.ruleVersion());
    }

    private static boolean hasCompletePlanBoundary(ExecutionPlanVO plan) {
        return plan != null
                && Boolean.TRUE.equals(plan.getSourceGateComplete())
                && concretePlanValue(plan.getEntryZone())
                && concretePlanValue(plan.getStopLoss())
                && concretePlanValue(plan.getTakeProfitRules());
    }

    private static boolean concretePlanValue(String value) {
        return value != null && !value.isBlank() && !"暂无".equals(value.trim());
    }

    private ExecutionPlanVO generateExecutionPlanFailClosed(
            DecisionBundleVO decision,
            List<ScoreItemVO> scores,
            MarketEnvironmentVO marketEnv,
            AssetAnalysisVO analysisContext,
            AnalysisExecutionContext executionContext
    ) {
        SourceTraceBoundaryProducerResult boundaryResult = buildBoundaryProducerResult(
                decision,
                marketEnv,
                executionContext);
        if (boundaryResult != null) {
            return planService.generateExecutionPlan(decision, scores, marketEnv, analysisContext, boundaryResult);
        }
        return planService.generateExecutionPlan(decision, scores, marketEnv, analysisContext);
    }

    private SourceTraceBoundaryProducerResult buildBoundaryProducerResult(
            DecisionBundleVO decision,
            MarketEnvironmentVO marketEnv,
            AnalysisExecutionContext executionContext
    ) {
        if (persistedOhlcvQueryService == null
                || runtimeKlineContextAssemblyService == null
                || marketStructureBoundaryExtractor == null
                || sourceTraceBoundaryProducer == null
                || executionContext == null) {
            return null;
        }
        String direction = boundaryDirection(decision);
        if (direction == null) {
            return null;
        }
        try {
            String symbol = executionContext.getSymbol();
            String timeframe = executionContext.getTimeframe();
            if (!AnalysisTimePolicy.isExecutionPlanPrimaryTimeframe(timeframe)) {
                log.info("[plan-boundary] fail closed unsupported execution-plan timeframe analysisId={} symbol={} timeframe={}",
                        executionContext.getAnalysisId(), symbol, timeframe);
                return null;
            }
            long maxReadLagMs = maxBoundaryReadLagMs(timeframe);
            PersistedOhlcvReadinessResult readiness = persistedOhlcvQueryService.evaluateReadiness(
                    symbol,
                    timeframe,
                    BOUNDARY_REQUIRED_WINDOW_SIZE,
                    maxReadLagMs);
            RuntimeKlineContextDTO runtimeKlineContext = runtimeKlineContextAssemblyService.assemble(readiness);
            if (!isBoundaryRuntimeKlineReady(runtimeKlineContext)) {
                return null;
            }

            LocalDateTime generatedAt = executionContext.getAnalysisTime() != null
                    ? executionContext.getAnalysisTime()
                    : LocalDateTime.now();
            MarketStructureBoundaryRequest request = new MarketStructureBoundaryRequest();
            request.setSymbol(symbol);
            request.setDirection(direction);
            request.setTimeframe(timeframe);
            request.setGeneratedAt(generatedAt);
            request.setGeneratedAtEpochMs(generatedAt.toInstant(ZoneOffset.UTC).toEpochMilli());
            request.setBars(runtimeKlineContext.getKlineItems());
            request.setAllowRrLadder(false);
            request.setMaxTargets(BOUNDARY_MAX_TARGETS);
            request.setMinBars(BOUNDARY_MIN_BARS);
            request.setFreshnessLimitMs(maxReadLagMs);
            request.setLeverageSuggestion(marketEnv != null ? marketEnv.getLeverageSuggestion() : null);

            MarketStructureBoundaryDTO boundary = marketStructureBoundaryExtractor.extract(request);
            return sourceTraceBoundaryProducer.produce(boundary);
        } catch (RuntimeException e) {
            log.warn("[plan-boundary] fail closed for analysisId={} symbol={} timeframe={} reason={}",
                    executionContext.getAnalysisId(),
                    executionContext.getSymbol(),
                    executionContext.getTimeframe(),
                    e.getClass().getSimpleName());
            return null;
        }
    }

    private static boolean isBoundaryRuntimeKlineReady(RuntimeKlineContextDTO runtimeKlineContext) {
        return runtimeKlineContext != null
                && runtimeKlineContext.getFallbackStatus() == null
                && runtimeKlineContext.getMissingFields().isEmpty()
                && runtimeKlineContext.getKlineItems() != null
                && !runtimeKlineContext.getKlineItems().isEmpty();
    }

    private static String boundaryDirection(DecisionBundleVO decision) {
        if (decision == null || decision.getMarketBiasHierarchy() == null) {
            return null;
        }
        String raw = decision.getMarketBiasHierarchy().trim();
        if (raw.isEmpty()) {
            return null;
        }
        String normalized = raw.toUpperCase(Locale.ROOT);
        if ("BULLISH".equals(normalized) || BOUNDARY_DIRECTION_LONG.equals(normalized) || "做多".equals(raw)) {
            return BOUNDARY_DIRECTION_LONG;
        }
        if ("BEARISH".equals(normalized) || BOUNDARY_DIRECTION_SHORT.equals(normalized) || "做空".equals(raw)) {
            return BOUNDARY_DIRECTION_SHORT;
        }
        return null;
    }

    private static long maxBoundaryReadLagMs(String timeframe) {
        Long intervalMs = parseBoundaryTimeframeMs(timeframe);
        if (intervalMs == null) {
            return BOUNDARY_FALLBACK_MAX_READ_LAG_MS;
        }
        return Math.max(BOUNDARY_FALLBACK_MAX_READ_LAG_MS, intervalMs * 2);
    }

    private static Long parseBoundaryTimeframeMs(String timeframe) {
        if (timeframe == null || timeframe.isBlank() || timeframe.length() < 2) {
            return null;
        }
        String unit = timeframe.substring(timeframe.length() - 1);
        String amountText = timeframe.substring(0, timeframe.length() - 1);
        long amount;
        try {
            amount = Long.parseLong(amountText);
        } catch (NumberFormatException e) {
            return null;
        }
        if (amount <= 0) {
            return null;
        }
        return switch (unit) {
            case "m" -> amount * 60_000L;
            case "h" -> amount * 60L * 60_000L;
            case "d" -> amount * 24L * 60L * 60_000L;
            default -> null;
        };
    }

    private static Boolean booleanOrTrue(Boolean value) {
        return value != null ? value : Boolean.TRUE;
    }

    private AnalysisExecutionContext normalizeExecutionContext(AnalysisExecutionContext context) {
        if (context == null) {
            throw new AnalysisRunInputException("ANALYSIS_CONTEXT_REQUIRED", "analysis execution context is required");
        }
        return new AnalysisExecutionContext(
                requireText(context.getAnalysisId(), "ANALYSIS_ID_REQUIRED"),
                requireText(context.getTraceId(), "TRACE_ID_REQUIRED"),
                requireText(context.getRequestId(), "REQUEST_ID_REQUIRED"),
                requireText(context.getIdempotencyKey(), "IDEMPOTENCY_KEY_REQUIRED"),
                normalizeAnalysisSymbol(context.getSymbol()),
                AnalysisTimePolicy.requireSupportedTimeframe(context.getTimeframe()),
                requireTime(context.getAnalysisTime()),
                requireBucket(context.getCanonicalAnalysisTimeBucket(), context.getAnalysisTime(), context.getTimeframe()),
                requireText(context.getRuleVersion(), "RULE_VERSION_REQUIRED"),
                context.getTriggerType() != null ? context.getTriggerType() : AnalysisRunTriggerType.MANUAL_API,
                context.getTriggerReference(),
                context.getParentAnalysisId(),
                context.getParentTraceId(),
                context.getInputSnapshotJson(),
                context.getInputSnapshotHash(),
                requireText(context.getLeaseOwner(), "LEASE_OWNER_REQUIRED"),
                context.getClaimVersion() != null ? context.getClaimVersion() : 1,
                context.getAttemptCount() != null ? context.getAttemptCount() : 1,
                context.isRunAlreadyClaimed());
    }

    private static String normalizeAnalysisSymbol(String raw) {
        if (raw == null || raw.isBlank()) {
            throw new AnalysisRunInputException("SYMBOL_REQUIRED", "symbol is required");
        }
        return raw.trim().toUpperCase();
    }

    private static LocalDateTime requireTime(LocalDateTime analysisTime) {
        if (analysisTime == null) {
            throw new AnalysisRunInputException("ANALYSIS_TIME_REQUIRED", "analysisTime is required");
        }
        return analysisTime;
    }

    private static LocalDateTime requireBucket(LocalDateTime bucket, LocalDateTime analysisTime, String timeframe) {
        if (bucket != null) {
            return bucket;
        }
        return AnalysisTimePolicy.canonicalBucket(requireTime(analysisTime), timeframe);
    }

    private static String requireText(String raw, String reasonCode) {
        if (raw == null || raw.isBlank()) {
            throw new AnalysisRunInputException(reasonCode, reasonCode);
        }
        return raw.trim();
    }

    private void saveToDatabase(AssetAnalysisVO analysis, List<EvidenceItemVO> evidences,
                                List<ScoreItemVO> scores, DecisionBundleVO decision, ExecutionPlanVO plan,
                                String marketEnvSourceType) {
        throw new IllegalStateException("DIRECT_ASSEMBLER_ENTRY_DISABLED");
    }

    private void saveToDatabase(AnalysisExecutionContext context, AssetAnalysisVO analysis, List<EvidenceItemVO> evidences,
                                List<ScoreItemVO> scores, DecisionBundleVO decision, ExecutionPlanVO plan,
                                String marketEnvSourceType) {
        System.out.println("落库开始 - analysisId = " + analysis.getAnalysisId());
        String decisionInvalidCondition = null;
        HotResetCommand hotResetCommand = null;
        boolean hotWouldReset = false;
        DecisionResult persistedDecision = null;
        ExecutionPlanDO persistedPlan = null;
        try {
            // 1. AnalysisRun
            LocalDateTime persistStartedAt = LocalDateTime.now();
            AnalysisRunDO run = new AnalysisRunDO();
            run.setAnalysisId(analysis.getAnalysisId());
            run.setSymbol(analysis.getSymbol());
            run.setTimeframe(analysis.getTimeframe());
            run.setAnalysisTime(context.getAnalysisTime() != null ? context.getAnalysisTime() : persistStartedAt);
            run.setRuleVersion(context.getRuleVersion() != null ? context.getRuleVersion() : resolveActiveRuleVersion());
            run.setDataQualityScore(analysis.getDataQualityScore());
            run.setTraceId(context.getTraceId() != null ? context.getTraceId() : AnalysisRunIds.traceId());
            run.setStatus(context.isRunAlreadyClaimed() ? "STARTED" : "SUCCESS");
            run.setIdempotencyKey(context.getIdempotencyKey());
            run.setRequestId(context.getRequestId());
            run.setTriggerType(context.getTriggerType() != null ? context.getTriggerType().name() : AnalysisRunTriggerType.MANUAL_API.name());
            run.setTriggerReference(context.getTriggerReference());
            run.setParentAnalysisId(context.getParentAnalysisId());
            run.setParentTraceId(context.getParentTraceId());
            run.setInputSnapshotJson(context.getInputSnapshotJson());
            run.setInputSnapshotHash(context.getInputSnapshotHash());
            run.setAttemptCount(context.getAttemptCount() != null ? context.getAttemptCount() : 1);
            run.setLeaseOwner(context.getLeaseOwner());
            run.setLeaseExpiresAt(null);
            run.setStartedAt(persistStartedAt);
            run.setCompletedAt(context.isRunAlreadyClaimed() ? null : persistStartedAt);
            run.setCreatedAt(persistStartedAt);
            run.setUpdatedAt(persistStartedAt);
            run.setVersionNo(context.getClaimVersion() != null ? context.getClaimVersion() : 1);
            if (!context.isRunAlreadyClaimed()) {
                analysisRunMapper.insert(run);
            }

            // 2. Evidence
            if (evidences != null) {
                for (EvidenceItemVO e : evidences) {
                    EvidenceItemDO edo = new EvidenceItemDO();
                    edo.setEvidenceId(e.getEvidenceId() != null ? e.getEvidenceId() : "ev-" + System.currentTimeMillis());
                    edo.setAnalysisId(analysis.getAnalysisId());
                    edo.setEvidenceType(EvidenceTypeConstants.normalizeEvidenceType(e.getEvidenceType()));
                    edo.setDescription(e.getDescription());
                    edo.setDirection(EvidenceTypeConstants.normalizeEvidenceDirection(e.getDirection()));
                    edo.setStrength(e.getStrength());
                    edo.setConfidence(e.getConfidence());
                    edo.setSource(EvidenceTypeConstants.normalizeEvidenceSource(e.getSource()));
                    edo.setSourceProvider(e.getSourceProvider());
                    edo.setSourceReference(e.getSourceReference());
                    edo.setSourceTraceId(e.getSourceTraceId());
                    edo.setExternalEventId(e.getExternalEventId());
                    edo.setExternalEventType(e.getExternalEventType());
                    edo.setEventWindowStart(e.getEventWindowStart());
                    edo.setEventWindowEnd(e.getEventWindowEnd());
                    edo.setImpactScore(e.getImpactScore());
                    edo.setSeverity(e.getSeverity());
                    edo.setCreateTime(LocalDateTime.now());
                    evidenceItemMapper.insert(edo);
                }
            }

            // 3. Score
            if (scores != null) {
                for (ScoreItemVO s : scores) {
                    ScoreItemDO sdo = new ScoreItemDO();
                    sdo.setAnalysisId(analysis.getAnalysisId());
                    sdo.setScoreType(s.getScoreType());
                    sdo.setScoreValue(s.getScoreValue());
                    sdo.setWeight(s.getWeight() != null ? s.getWeight() : 1.0);
                    sdo.setDirection(s.getDirection());
                    sdo.setDescription(s.getDescription());
                    insertScoreItemWithRetry(sdo);
                }
            }

            // 4. Decision（现在会拿到正确的高置信度数据）
            if (decision != null) {
                String evidenceSummary = null;
                if (evidences != null && !evidences.isEmpty()) {
                    evidenceSummary = evidences.stream()
                            .map(EvidenceItemVO::getDescription)
                            .filter(Objects::nonNull)
                            .limit(5)
                            .collect(Collectors.joining("；"));
                    if (evidenceSummary.length() > 500) {
                        evidenceSummary = evidenceSummary.substring(0, 500) + "…";
                    }
                }
                DecisionResult ddo = new DecisionResult();
                ddo.setDecisionId(decision.getDecisionId());
                ddo.setAnalysisId(analysis.getAnalysisId());
                ddo.setSymbol(analysis.getSymbol());
                ddo.setMarketBiasHierarchy(decision.getMarketBiasHierarchy());
                ddo.setTradeType(decision.getTradeType());
                ddo.setConfidenceLevel(decision.getConfidenceLevel());
                ddo.setRiskLevel(decision.getRiskLevel());
                ddo.setActionPriority(decision.getActionPriority());
                ddo.setConclusionSummary(decision.getConclusionSummary());
                ddo.setIsWorthOpening(decision.getIsWorthOpening());
                ddo.setMultiTfConvergence(decision.getMultiTfConvergence());
                ddo.setAiRoleResults(decision.getAiRoleResults());
                ddo.setIsAdopted(null);
                LocalDateTime decisionCreateTime = LocalDateTime.now();
                LocalDateTime pushExpiresAt = decision.getPushExpiresAt();
                ddo.setValidPeriod(pushExpiresAt != null
                        ? VALID_PERIOD_TIME_FORMATTER.format(decisionCreateTime) + " ~ "
                        + VALID_PERIOD_TIME_FORMATTER.format(pushExpiresAt)
                        : null);
                decisionInvalidCondition = decision.getPushInvalidationSummary();
                ddo.setInvalidCondition(decisionInvalidCondition != null && !decisionInvalidCondition.trim().isEmpty()
                        ? decisionInvalidCondition
                        : null);
                ddo.setEvidenceSummary(evidenceSummary);
                ddo.setExplanationJson(buildExplanationJson(analysis, decision, evidences, scores));
                ddo.setReviewReasons(normalizeReviewReasons(decision.getReviewReasons()));
                ddo.setAiConflictLevel(decision.getAiConflictLevel());
                ddo.setAiConflictScore(decision.getAiConflictScore());
                ddo.setAiPlanMode(decision.getAiPlanMode());
                ddo.setConfusedScore(decision.getConfusedScore());
                ddo.setAssetStateSnapshot(decision.getAssetStateSnapshot());
                ddo.setCreateTime(decisionCreateTime);
                decisionResultMapper.insert(ddo);
                persistedDecision = ddo;

                int confused = decision.getConfusedScore() != null ? decision.getConfusedScore() : 0;
                hotResetCommand = buildStructuredHotResetCommand(analysis, decision, marketEnvSourceType, run.getTraceId());
                hotWouldReset = hotResetCommand != null && hotResetService.shouldTriggerHotReset(hotResetCommand);

                if (decision.getAssetState() != null) {
                    assetStateService.persistAuthoritativeState(
                            analysis.getSymbol(),
                            decision.getAssetState(),
                            confused,
                            decision.getConfusedLowStreak() != null ? decision.getConfusedLowStreak() : 0,
                            run.getTraceId());
                }
            }

            persistMarketEnvironmentSnapshot(analysis, marketEnvSourceType);

            Long accountRiskSnapshotId = pushSnapshotService.ensureAccountRiskSnapshot(run, analysis, decision, plan);

            pushSnapshotService.insertAuthoritativeSnapshot(run, analysis, decision, plan, accountRiskSnapshotId);

            // 5. ExecutionPlan
            if (plan != null) {
                ExecutionPlanDO pdo = new ExecutionPlanDO();
                pdo.setPlanId(plan.getPlanId());
                pdo.setAnalysisId(analysis.getAnalysisId());
                pdo.setPlanMode(plan.getPlanMode());
                pdo.setExecutionPlanStatus(plan.getExecutionPlanStatus());
                pdo.setSourceGateStatus(plan.getSourceGateStatus());
                pdo.setSourceGateComplete(plan.getSourceGateComplete());
                pdo.setSourceMissingReasons(joinReasons(plan.getMissingSourceReasons()));
                pdo.setSourceBlockerReasons(joinReasons(plan.getSourceBlockerReasons()));
                pdo.setSourceCompletenessSummary(plan.getSourceCompletenessSummary());
                pdo.setRecommendedAction(plan.getRecommendedAction());
                pdo.setEntryZone(plan.getEntryZone());
                pdo.setStopLoss(plan.getStopLoss());
                pdo.setTakeProfitRules(plan.getTakeProfitRules());
                pdo.setLeverageSuggestion(plan.getLeverageSuggestion());
                pdo.setPositionSuggestion(plan.getPositionSuggestion());
                pdo.setAccountRiskJson(buildExecutionAccountRiskJson(analysis.getAnalysisId()));
                String planInvalidCondition = plan.getInvalidCondition();
                pdo.setInvalidCondition(planInvalidCondition != null && !planInvalidCondition.trim().isEmpty()
                        ? planInvalidCondition
                        : null);
                pdo.setManualReviewRequired(booleanOrTrue(plan.getManualReviewRequired()));
                pdo.setNotTradeInstruction(booleanOrTrue(plan.getNotTradeInstruction()));
                pdo.setNotExecutable(booleanOrTrue(plan.getNotExecutable()));
                pdo.setNotAutoTrading(booleanOrTrue(plan.getNotAutoTrading()));
                pdo.setNotOrderExecution(booleanOrTrue(plan.getNotOrderExecution()));
                pdo.setNotUserPositionCreation(booleanOrTrue(plan.getNotUserPositionCreation()));
                pdo.setNeedsRevalidation(Boolean.TRUE.equals(plan.getNeedsRevalidation()));
                pdo.setRevalidationReason(plan.getRevalidationReason());
                pdo.setCreateTime(LocalDateTime.now());
                executionPlanMapper.insert(pdo);
                persistedPlan = pdo;
            }

            if (!hotWouldReset && persistedDecision != null && persistedPlan != null) {
                opportunityLogService.recordFromAuthoritativeAnalysis(
                        run,
                        persistedDecision,
                        persistedPlan,
                        accountRiskSnapshotId,
                        run.getTraceId());
            }

            if (hotWouldReset && hotResetCommand != null) {
                hotResetService.evaluateAndExecute(hotResetCommand);
            }

            monitorAlertWriteService.emitAfterAnalysisPersist(run, analysis, decision);

            if (context.isRunAlreadyClaimed()) {
                int updated = analysisRunMapper.markSuccess(
                        analysis.getAnalysisId(),
                        analysis.getDataQualityScore(),
                        LocalDateTime.now(),
                        context.getLeaseOwner(),
                        context.getClaimVersion() != null ? context.getClaimVersion() : 1);
                if (updated != 1) {
                    throw new IllegalStateException("ANALYSIS_RUN_LEASE_FENCING_CONFLICT");
                }
            }

            System.out.println("✅ 6张表落库全部完成！analysisId = " + analysis.getAnalysisId());
        } catch (Exception ex) {
            System.err.println("落库异常: " + ex.getClass().getName() + " - " + ex.getMessage());
            ex.printStackTrace();
            throw ex;
        }
    }

    private HotResetCommand buildStructuredHotResetCommand(AssetAnalysisVO analysis, DecisionBundleVO decision,
                                                           String marketEnvSourceType, String traceId) {
        if (analysis == null || analysis.getMarketEnvironment() == null || decision == null) {
            return null;
        }
        MarketEnvironmentVO env = analysis.getMarketEnvironment();
        List<HotResetCommand> candidates = new ArrayList<>();
        HotResetCommand derivatives = derivativesHotResetCommand(analysis, decision, traceId);
        if (derivatives != null) {
            candidates.add(derivatives);
        }
        HotResetCommand systemic = systemicShockCommand(analysis, decision, env, traceId);
        if (systemic != null) {
            candidates.add(systemic);
        }
        HotResetCommand liquidity = liquidityDrainCommand(analysis, decision, env, traceId);
        if (liquidity != null) {
            candidates.add(liquidity);
        }
        HotResetCommand oi = oiCollapseCommand(analysis, decision, env, marketEnvSourceType, traceId);
        if (oi != null) {
            candidates.add(oi);
        }
        HotResetCommand price = priceMoveCommand(analysis, decision, env, marketEnvSourceType, traceId);
        if (price != null) {
            candidates.add(price);
        }
        if (candidates.isEmpty()) {
            return null;
        }
        for (HotResetCommand candidate : candidates) {
            if (hotResetService.shouldTriggerHotReset(candidate)) {
                return candidate;
            }
        }
        return candidates.get(0);
    }

    private HotResetCommand derivativesHotResetCommand(AssetAnalysisVO analysis, DecisionBundleVO decision,
                                                        String traceId) {
        DerivativesBusinessAssessment assessment = analysis.getDerivativesAssessment();
        if (assessment == null || !assessment.hotResetCandidate()) return null;
        DerivativesEvidenceItem oiCollapse = assessment.evidence().stream()
                .filter(item -> item.evidenceType() == DerivativesEvidenceType.OPEN_INTEREST_CONTRACTION)
                .filter(item -> "OI_COLLAPSE".equals(item.reasonCode()))
                .findFirst().orElse(null);
        if (oiCollapse != null && oiCollapse.currentValue() != null) {
            HotResetCommand command = baseHotResetCommand(analysis, decision, traceId,
                    HotResetEventTypeEnum.OI_COLLAPSE, 90);
            command.setOpenInterestChangeRatio(oiCollapse.currentValue());
            command.setSourceType("COINGLASS_V4");
            command.setSourceReference(oiCollapse.sourceField());
            command.setEventKey(hotResetEventKey(analysis, command.getEventType(),
                    oiCollapse.currentValue().toPlainString()));
            return command;
        }
        HotResetCommand command = baseHotResetCommand(analysis, decision, traceId,
                HotResetEventTypeEnum.SYSTEMIC_SHOCK, 90);
        command.setSystemicShock(true);
        command.setSourceType("COINGLASS_V4");
        command.setSourceReference("DerivativesBusinessAssessment.hotResetCandidate");
        command.setEventKey(hotResetEventKey(analysis, command.getEventType(),
                String.join(",", assessment.reasonCodes())));
        return command;
    }

    private HotResetCommand priceMoveCommand(AssetAnalysisVO analysis, DecisionBundleVO decision,
                                             MarketEnvironmentVO env, String sourceType, String traceId) {
        if (env.getPriceChangePercent24h() == null) {
            return null;
        }
        BigDecimal ratio = env.getPriceChangePercent24h().movePointLeft(2);
        HotResetCommand command = baseHotResetCommand(analysis, decision, traceId,
                HotResetEventTypeEnum.EXTREME_PRICE_MOVE, 80);
        command.setPriceMoveRatio(ratio);
        command.setSourceType(sourceType);
        command.setSourceReference("tm_market_environment_snapshot.price_change_percent_24h");
        command.setEventKey(hotResetEventKey(analysis, command.getEventType(), ratio.toPlainString()));
        return command;
    }

    private HotResetCommand oiCollapseCommand(AssetAnalysisVO analysis, DecisionBundleVO decision,
                                             MarketEnvironmentVO env, String sourceType, String traceId) {
        if (env.getLastOpenInterest() == null || env.getOpenInterestDelta() == null) {
            return null;
        }
        BigDecimal previous = env.getLastOpenInterest().subtract(env.getOpenInterestDelta());
        if (previous.compareTo(BigDecimal.ZERO) <= 0) {
            return null;
        }
        BigDecimal ratio = env.getOpenInterestDelta().divide(previous, 8, RoundingMode.HALF_UP);
        HotResetCommand command = baseHotResetCommand(analysis, decision, traceId,
                HotResetEventTypeEnum.OI_COLLAPSE, 75);
        command.setCurrentOpenInterest(env.getLastOpenInterest());
        command.setPreviousOpenInterest(previous);
        command.setOpenInterestChangeRatio(ratio);
        command.setSourceType(sourceType);
        command.setSourceReference("tm_market_environment_snapshot.open_interest_delta");
        command.setEventKey(hotResetEventKey(analysis, command.getEventType(), ratio.toPlainString()));
        return command;
    }

    private HotResetCommand liquidityDrainCommand(AssetAnalysisVO analysis, DecisionBundleVO decision,
                                                  MarketEnvironmentVO env, String traceId) {
        if (env.getLiquidityChangeRatio() == null
                && (env.getCurrentLiquidity() == null || env.getBaselineLiquidity() == null)) {
            return null;
        }
        HotResetCommand command = baseHotResetCommand(analysis, decision, traceId,
                HotResetEventTypeEnum.LIQUIDITY_DRAIN, 54);
        command.setCurrentLiquidity(env.getCurrentLiquidity());
        command.setBaselineLiquidity(env.getBaselineLiquidity());
        command.setLiquidityChangeRatio(env.getLiquidityChangeRatio());
        command.setSourceType("STRUCTURED_MARKET_ENVIRONMENT");
        command.setSourceReference("MarketEnvironmentVO.liquidityChangeRatio");
        String fingerprint = env.getLiquidityChangeRatio() != null
                ? env.getLiquidityChangeRatio().toPlainString()
                : String.valueOf(env.getCurrentLiquidity()) + ":" + env.getBaselineLiquidity();
        command.setEventKey(hotResetEventKey(analysis, command.getEventType(), fingerprint));
        return command;
    }

    private HotResetCommand systemicShockCommand(AssetAnalysisVO analysis, DecisionBundleVO decision,
                                                 MarketEnvironmentVO env, String traceId) {
        if (!Boolean.TRUE.equals(env.getSystemicShock()) && env.getSystemicShockSeverityScore() == null) {
            return null;
        }
        HotResetCommand command = baseHotResetCommand(analysis, decision, traceId,
                HotResetEventTypeEnum.SYSTEMIC_SHOCK,
                env.getSystemicShockSeverityScore() != null ? env.getSystemicShockSeverityScore() : 0);
        command.setSystemicShock(env.getSystemicShock());
        command.setSourceType(env.getSystemicShockSourceType());
        command.setSourceReference(env.getSystemicShockSourceReference());
        command.setEventKey(hotResetEventKey(analysis, command.getEventType(),
                String.valueOf(env.getSystemicShockSeverityScore())));
        return command;
    }

    private HotResetCommand baseHotResetCommand(AssetAnalysisVO analysis, DecisionBundleVO decision, String traceId,
                                                HotResetEventTypeEnum eventType, int severityScore) {
        HotResetCommand command = new HotResetCommand();
        command.setAnalysisId(analysis.getAnalysisId());
        command.setTraceId(traceId);
        command.setSymbol(analysis.getSymbol());
        command.setTimeframe(analysis.getTimeframe());
        command.setEventType(eventType);
        command.setOccurredAt(LocalDateTime.now());
        command.setSeverityScore(severityScore);
        DecisionContext context = new DecisionContext();
        context.setSymbol(analysis.getSymbol());
        context.setWorthOpening(decision.getIsWorthOpening());
        context.setMultiTimeframeAligned(decision.isMultiTimeframeAligned());
        context.setRiskTier(decision.getRiskLevel());
        context.setDriverConflictScore(severityScore);
        context.setExecutionInstabilityScore(severityScore);
        context.setMicrostructureTrapScore(severityScore);
        context.setCauseEffectDivergenceScore(severityScore);
        context.setAiConflictScore(severityScore);
        command.setDecisionContext(context);
        return command;
    }

    private static String hotResetEventKey(AssetAnalysisVO analysis, HotResetEventTypeEnum eventType, String fingerprint) {
        String raw = "HOT_RESET:" + safeKeyPart(analysis.getSymbol()) + ":" + safeKeyPart(analysis.getTimeframe())
                + ":" + eventType.name() + ":" + safeKeyPart(fingerprint);
        String compact = raw.replaceAll("[^A-Za-z0-9:_\\-.]", "_");
        String hash = Integer.toHexString(compact.hashCode());
        if (compact.length() > 108) {
            compact = compact.substring(0, 108);
        }
        return compact + ":" + hash;
    }

    private static String safeKeyPart(String raw) {
        return raw == null || raw.isBlank() ? "NA" : raw.trim();
    }

    /**
     * 空策略：统一为 JSON 数组文本 {@code []}，避免 null / "" / "{}" 混用。
     */
    private static ArrayNode parseReviewReasonsArray(String raw) {
        if (raw != null && !raw.isBlank()) {
            try {
                JsonNode n = EXPLAIN_JSON.readTree(raw.trim());
                if (n.isArray()) {
                    return (ArrayNode) n;
                }
            } catch (Exception ignored) {
            }
        }
        return EXPLAIN_JSON.createArrayNode();
    }

    /** 与 {@link org.example.trademodel.service.impl.EvidenceServiceImpl} 第二维 volatility 行 description 同源模板的前缀。 */
    private static final String DQ_VOLATILITY_EXPLANATORY_DESC_PREFIX = "24h 价格振幅约 ";
    /** 同上模板的固定后缀（含全角分号）。 */
    private static final String DQ_VOLATILITY_EXPLANATORY_DESC_SUFFIX = "；口径：Binance 24h ticker 启发式。";

    /** 与 {@link RealMarketEnvironmentService#BUILD_FUNDING_APPENDIX_TRIM_PREFIX} 同源。 */
    private static final String DQ_FUNDING_EXPLANATORY_DESC_PREFIX =
            RealMarketEnvironmentService.BUILD_FUNDING_APPENDIX_TRIM_PREFIX;
    /** 与 {@link RealMarketEnvironmentService#BUILD_FUNDING_APPENDIX_TRIM_SUFFIX} 同源。 */
    private static final String DQ_FUNDING_EXPLANATORY_DESC_SUFFIX =
            RealMarketEnvironmentService.BUILD_FUNDING_APPENDIX_TRIM_SUFFIX;
    /** 与 {@link RealMarketEnvironmentService#BUILD_OPEN_INTEREST_APPENDIX_TRIM_PREFIX} 同源。 */
    private static final String DQ_OI_EXPLANATORY_DESC_PREFIX =
            RealMarketEnvironmentService.BUILD_OPEN_INTEREST_APPENDIX_TRIM_PREFIX;
    /** 与 {@link RealMarketEnvironmentService#BUILD_OPEN_INTEREST_APPENDIX_TRIM_SUFFIX} 同源。 */
    private static final String DQ_OI_EXPLANATORY_DESC_SUFFIX =
            RealMarketEnvironmentService.BUILD_OPEN_INTEREST_APPENDIX_TRIM_SUFFIX;

    /**
     * Run 级写入 {@code tm_analysis_run.data_quality_score} 前的阶段性整数估计（与本方法体内算法一致）。
     * <p><b>当前输入：</b>（1）{@code evidences}、{@code scores} 列表用于分档：其中 evidence 使用 <strong>有效条数</strong>
     * {@code effectiveEv}（见下），{@code scores} 仍用列表长度；二者均不读取业务语义作矩阵扣分；
     * （2）与 {@code tm_market_environment_snapshot.source_type} 同源的 {@code marketEnvSourceType}
     * （{@code BINANCE_24H_HEURISTIC} / {@code BINANCE_SPOT_PERP_MIN_HEURISTIC} / {@code BINANCE_USDM_OI_MIN_HEURISTIC} / {@code BINANCE_SPOT_PERP_OI_MIN_HEURISTIC} / {@code PLACEHOLDER_FALLBACK}，与本类常量一致），
     * 仅用于封顶，非矩阵扣分；其中非 {@code PLACEHOLDER_FALLBACK} 均属非 fallback，不按条数区别对待。
     * 不读取 Push/Recheck、{@code systemHealth} 或其它链外质量字段。
     * <p><b>有效 evidence 条数：</b>在 {@code evidences} 上排除三类「市场环境解释性锚点」——（a）第二维振幅：
     * {@code evidenceType=风险}、{@code direction=NEUTRAL}、{@code source=MARKET_HEURISTIC}，
     * 且 {@code description} 为 {@code EvidenceServiceImpl} 中 24h 振幅+波动体制固定模板（以前缀 {@link #DQ_VOLATILITY_EXPLANATORY_DESC_PREFIX}、
     * 后缀 {@link #DQ_VOLATILITY_EXPLANATORY_DESC_SUFFIX} 识别）；（b）Funding：
     * {@code evidenceType=资金}、{@code direction=NEUTRAL}、{@code source=MARKET_HEURISTIC}，
     * 且 {@code description}（trim 后）与 {@link org.example.trademodel.market.RealMarketEnvironmentService#buildFundingAppendix}
     * 同源窄模板（以前缀 {@link #DQ_FUNDING_EXPLANATORY_DESC_PREFIX}、后缀 {@link #DQ_FUNDING_EXPLANATORY_DESC_SUFFIX} 识别）；
     * （c）杠杆（{@code EvidenceServiceImpl} 当前最小切口）：{@code evidenceType=杠杆}、{@code direction=NEUTRAL}、{@code source=MARKET_HEURISTIC}，
     * 且 {@code description}（trim 后）与 {@link org.example.trademodel.service.impl.EvidenceServiceImpl#LEVERAGE_EVIDENCE_DESCRIPTION_LOW}
     * 或 {@link org.example.trademodel.service.impl.EvidenceServiceImpl#LEVERAGE_EVIDENCE_DESCRIPTION_MODERATE} 完全一致；
     * （d）OI（当前最小切口）：{@code evidenceType=风险}、{@code direction=NEUTRAL}、{@code source=MARKET_HEURISTIC}，
     * 且 {@code description}（trim 后）与 {@link org.example.trademodel.market.RealMarketEnvironmentService#buildOpenInterestAppendix}
     * 同源窄模板（以前缀 {@link #DQ_OI_EXPLANATORY_DESC_PREFIX}、后缀 {@link #DQ_OI_EXPLANATORY_DESC_SUFFIX} 识别）。
     * 以上条目均<strong>不计入</strong>将 DQ 从 55 抬向 85 的 evidence 计数，避免「解释增强 = 输入档跳升」。
     * <p><b>条数三档：</b>{@code effectiveEv==0 && sc==0 → 35}；{@code effectiveEv &lt; 2 → 55}；否则 {@code 85}（{@code sc} 为评分条数）。
     * <p><b>sourceType 封顶：</b>若 {@code marketEnvSourceType == PLACEHOLDER_FALLBACK}，结果<strong>最高不超过 55</strong>；
     * 若为 {@code BINANCE_24H_HEURISTIC}、{@code BINANCE_SPOT_PERP_MIN_HEURISTIC}、{@code BINANCE_USDM_OI_MIN_HEURISTIC}、{@code BINANCE_SPOT_PERP_OI_MIN_HEURISTIC} 等，保持条数档结果不变。
     * <p><b>语义边界：</b>assemble 链上的<strong>稀疏档位 + 单信号封顶</strong>；仍<strong>不是</strong>{@code PROJECT_SPEC.md}
     * 中「数据源扣分矩阵」或规格级 70/85 熔断的完整实现，决策主路径亦未据此接线。勿将本整数误读为 tm_data_source_health 类矩阵得分。
     */
    static int estimateDataQualityScore(List<EvidenceItemVO> evidences, List<ScoreItemVO> scores,
                                        String marketEnvSourceType) {
        int ev = effectiveEvidenceCountForDataQuality(evidences, marketEnvSourceType);
        int sc = scores == null ? 0 : scores.size();
        int base;
        if (ev == 0 && sc == 0) {
            base = 35;
        } else if (ev < 2) {
            base = 55;
        } else {
            base = 85;
        }
        if (MARKET_ENV_SOURCE_FALLBACK.equals(marketEnvSourceType)) {
            return Math.min(base, 55);
        }
        return base;
    }

    static Integer extractTrendStructureScore(List<ScoreItemVO> scores) {
        if (scores == null || scores.isEmpty()) {
            return null;
        }
        for (ScoreItemVO score : scores) {
            if (score == null || score.getScoreType() == null || score.getScoreValue() == null) {
                continue;
            }
            if ("趋势结构分".equals(score.getScoreType().trim())) {
                return (int) Math.round(score.getScoreValue());
            }
        }
        return null;
    }

    static Integer calculateEightScoreComposite(List<ScoreItemVO> scores) {
        if (scores == null || scores.size() != 8) {
            return null;
        }
        double sum = 0.0;
        for (ScoreItemVO score : scores) {
            if (score == null || score.getScoreValue() == null) {
                return null;
            }
            sum += score.getScoreValue();
        }
        return (int) Math.round(sum / 8.0);
    }

    /**
     * 供 run 级 DQ 分档：从原始列表长度中减去第二维振幅、Funding、杠杆、及当前最小 OI 解释性模板条目（极窄，不误伤其它类证据）。
     */
    static int effectiveEvidenceCountForDataQuality(List<EvidenceItemVO> evidences) {
        return effectiveEvidenceCountForDataQuality(evidences, null);
    }

    static int effectiveEvidenceCountForDataQuality(List<EvidenceItemVO> evidences, String marketEnvSourceType) {
        if (evidences == null || evidences.isEmpty()) {
            return 0;
        }
        boolean oiApplied = isOiAppliedForDataQualityCount(marketEnvSourceType);
        int n = 0;
        for (EvidenceItemVO e : evidences) {
            if (!isSecondDimensionVolatilityExplanatoryEvidenceForDq(e)
                    && !isFundingExplanatoryEvidenceForDq(e)
                    && !isLeverageExplanatoryEvidenceForDq(e)
                    && !isOpenInterestExplanatoryEvidenceForDq(e, oiApplied)) {
                n++;
            }
        }
        return n;
    }

    /**
     * 仅当与 {@code EvidenceServiceImpl} 第二维行完全一致（类型/方向/来源 + 描述前后缀模板）时返回 true。
     */
    static boolean isSecondDimensionVolatilityExplanatoryEvidenceForDq(EvidenceItemVO e) {
        if (e == null) {
            return false;
        }
        String type = e.getEvidenceType();
        if (type == null || !EvidenceTypeConstants.RISK.equals(type.trim())) {
            return false;
        }
        String dir = e.getDirection();
        if (dir == null || !EvidenceTypeConstants.EVIDENCE_DIRECTION_NEUTRAL.equals(dir.trim())) {
            return false;
        }
        String src = e.getSource();
        if (src == null || !EvidenceTypeConstants.EVIDENCE_SOURCE_MARKET_HEURISTIC.equals(src.trim())) {
            return false;
        }
        String desc = e.getDescription();
        if (desc == null) {
            return false;
        }
        String t = desc.trim();
        return t.startsWith(DQ_VOLATILITY_EXPLANATORY_DESC_PREFIX) && t.endsWith(DQ_VOLATILITY_EXPLANATORY_DESC_SUFFIX);
    }

    /**
     * 仅当与 {@code EvidenceServiceImpl} Funding 行一致（类型/方向/来源 + {@link org.example.trademodel.market.RealMarketEnvironmentService#buildFundingAppendix} trim 后前后缀）时返回 true。
     */
    static boolean isFundingExplanatoryEvidenceForDq(EvidenceItemVO e) {
        if (e == null) {
            return false;
        }
        String type = e.getEvidenceType();
        if (type == null || !EvidenceTypeConstants.FUNDING.equals(type.trim())) {
            return false;
        }
        String dir = e.getDirection();
        if (dir == null || !EvidenceTypeConstants.EVIDENCE_DIRECTION_NEUTRAL.equals(dir.trim())) {
            return false;
        }
        String src = e.getSource();
        if (src == null || !EvidenceTypeConstants.EVIDENCE_SOURCE_MARKET_HEURISTIC.equals(src.trim())) {
            return false;
        }
        String desc = e.getDescription();
        if (desc == null) {
            return false;
        }
        String t = desc.trim();
        return t.startsWith(DQ_FUNDING_EXPLANATORY_DESC_PREFIX) && t.endsWith(DQ_FUNDING_EXPLANATORY_DESC_SUFFIX);
    }

    /**
     * 仅当与 {@code EvidenceServiceImpl} 当前最小杠杆行一致（类型/方向/来源 + description 与
     * {@link EvidenceServiceImpl#LEVERAGE_EVIDENCE_DESCRIPTION_LOW} /
     * {@link EvidenceServiceImpl#LEVERAGE_EVIDENCE_DESCRIPTION_MODERATE} 逐字相等）时返回 true。
     */
    static boolean isLeverageExplanatoryEvidenceForDq(EvidenceItemVO e) {
        if (e == null) {
            return false;
        }
        String type = e.getEvidenceType();
        if (type == null || !EvidenceTypeConstants.LEVERAGE.equals(type.trim())) {
            return false;
        }
        String dir = e.getDirection();
        if (dir == null || !EvidenceTypeConstants.EVIDENCE_DIRECTION_NEUTRAL.equals(dir.trim())) {
            return false;
        }
        String src = e.getSource();
        if (src == null || !EvidenceTypeConstants.EVIDENCE_SOURCE_MARKET_HEURISTIC.equals(src.trim())) {
            return false;
        }
        String desc = e.getDescription();
        if (desc == null) {
            return false;
        }
        String t = desc.trim();
        return EvidenceServiceImpl.LEVERAGE_EVIDENCE_DESCRIPTION_LOW.equals(t)
                || EvidenceServiceImpl.LEVERAGE_EVIDENCE_DESCRIPTION_MODERATE.equals(t);
    }

    /**
     * 仅当与 {@code EvidenceServiceImpl} 当前最小 OI 风险行一致（类型/方向/来源 + buildOpenInterestAppendix trim 后前后缀）时返回 true。
     */
    static boolean isOpenInterestExplanatoryEvidenceForDq(EvidenceItemVO e) {
        return isOpenInterestExplanatoryEvidenceForDq(e, false);
    }

    static boolean isOpenInterestExplanatoryEvidenceForDq(EvidenceItemVO e, boolean oiAppliedForDqCount) {
        if (e == null) {
            return false;
        }
        String type = e.getEvidenceType();
        if (type == null || !EvidenceTypeConstants.RISK.equals(type.trim())) {
            return false;
        }
        String dir = e.getDirection();
        if (dir == null || !EvidenceTypeConstants.EVIDENCE_DIRECTION_NEUTRAL.equals(dir.trim())) {
            return false;
        }
        String src = e.getSource();
        if (src == null || !EvidenceTypeConstants.EVIDENCE_SOURCE_MARKET_HEURISTIC.equals(src.trim())) {
            return false;
        }
        String desc = e.getDescription();
        if (desc == null) {
            return false;
        }
        String t = desc.trim();
        boolean strictOiTemplate = t.startsWith(DQ_OI_EXPLANATORY_DESC_PREFIX) && t.endsWith(DQ_OI_EXPLANATORY_DESC_SUFFIX);
        if (!strictOiTemplate) {
            return false;
        }
        // OI carve-out 第一刀仅在“已应用”条件成立时放行计数，其余场景保持解释性锚点语义。
        return !oiAppliedForDqCount;
    }

    static boolean isOiAppliedForDataQualityCount(String marketEnvSourceType) {
        return MARKET_ENV_SOURCE_USDM_OI_MIN.equals(marketEnvSourceType)
                || MARKET_ENV_SOURCE_SPOT_PERP_OI_MIN.equals(marketEnvSourceType);
    }

    private void persistMarketEnvironmentSnapshot(AssetAnalysisVO analysis, String sourceType) {
        if (analysis == null || analysis.getAnalysisId() == null || analysis.getAnalysisId().isBlank()) {
            return;
        }
        MarketEnvironmentVO env = analysis.getMarketEnvironment();
        MarketEnvironmentSnapshotDO row = new MarketEnvironmentSnapshotDO();
        row.setAnalysisId(analysis.getAnalysisId());
        row.setSymbol(analysis.getSymbol());
        row.setTimeframe(analysis.getTimeframe());
        row.setEnvironmentType(env != null ? env.getEnvironmentType() : null);
        row.setRiskMode(env != null ? env.getRiskMode() : null);
        row.setTrendFriendliness(env != null && env.getTrendFriendliness() != null
                ? (int) Math.round(env.getTrendFriendliness())
                : null);
        row.setLeverageSuggestion(env != null ? env.getLeverageSuggestion() : null);
        row.setRangePct24h(env != null ? env.getRangePct24h() : null);
        row.setVolatilityRegime(env != null ? env.getVolatilityRegime() : null);
        row.setLastFundingRate(env != null ? env.getLastFundingRate() : null);
        row.setPerpFundingApplied(env != null ? env.getPerpFundingApplied() : null);
        row.setLastOpenInterest(env != null ? env.getLastOpenInterest() : null);
        row.setOpenInterestDelta(env != null ? env.getOpenInterestDelta() : null);
        row.setOiApplied(env != null ? env.getOiApplied() : null);
        row.setDerivativesCrowdingState(env != null ? env.getDerivativesCrowdingState() : null);
        row.setSummary(env != null ? env.getSummary() : null);
        row.setSourceType(sourceType != null && !sourceType.isBlank() ? sourceType : MARKET_ENV_SOURCE_FALLBACK);
        row.setCreateTime(LocalDateTime.now());
        marketEnvironmentSnapshotMapper.insert(row);
    }

    private void enrichOpenInterestDeltaFromPreviousSnapshot(MarketEnvironmentVO env, String symbol, String timeframe) {
        if (env == null || marketEnvironmentSnapshotMapper == null) {
            return;
        }
        String symbolKey = normalizeKey(symbol);
        String timeframeKey = normalizeKey(timeframe);
        if (symbolKey == null || timeframeKey == null) {
            env.setOpenInterestDelta(null);
            return;
        }
        BigDecimal previousOi = null;
        MarketEnvironmentSnapshotDO previous = marketEnvironmentSnapshotMapper
                .selectLatestBySymbolAndTimeframe(symbolKey, timeframeKey);
        if (previous != null) {
            previousOi = previous.getLastOpenInterest();
        }
        env.setOpenInterestDelta(RealMarketEnvironmentService.computeOpenInterestDelta(
                env.getOiApplied(),
                env.getLastOpenInterest(),
                previousOi));
    }

    private static String normalizeKey(String raw) {
        if (raw == null) {
            return null;
        }
        String t = raw.trim();
        return t.isEmpty() ? null : t;
    }

    private static String normalizeReviewReasons(String raw) {
        if (raw == null || raw.trim().isEmpty()) {
            return "[]";
        }
        String t = raw.trim();
        if ("null".equalsIgnoreCase(t) || "{}".equals(t)) {
            return "[]";
        }
        return t;
    }

    private static String joinReasons(List<String> reasons) {
        if (reasons == null || reasons.isEmpty()) {
            return null;
        }
        String joined = reasons.stream()
                .filter(Objects::nonNull)
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.joining(","));
        return joined.isEmpty() ? null : joined;
    }

    private String buildExecutionAccountRiskJson(String analysisId) {
        if (analysisId == null || analysisId.isBlank()) {
            return null;
        }
        TmAccountRiskSnapshotDO snapshot = accountRiskSnapshotMapper.selectLatestByAnalysisId(analysisId);
        if (snapshot == null) {
            return null;
        }
        try {
            ObjectNode n = EXPLAIN_JSON.createObjectNode();
            if (snapshot.getRiskAllowed() == null) {
                n.putNull("riskAllowed");
            } else {
                n.put("riskAllowed", snapshot.getRiskAllowed());
            }
            if (snapshot.getRiskReasonCode() == null) {
                n.putNull("riskReasonCode");
            } else {
                n.put("riskReasonCode", snapshot.getRiskReasonCode());
            }
            if (snapshot.getRiskReasonText() == null) {
                n.putNull("riskReasonText");
            } else {
                n.put("riskReasonText", snapshot.getRiskReasonText());
            }
            if (snapshot.getPositionExposure() == null) {
                n.putNull("positionExposure");
            } else {
                n.put("positionExposure", snapshot.getPositionExposure());
            }
            if (snapshot.getMaxAllowedExposure() == null) {
                n.putNull("maxAllowedExposure");
            } else {
                n.put("maxAllowedExposure", snapshot.getMaxAllowedExposure());
            }
            if (snapshot.getSnapshotSource() == null) {
                n.putNull("snapshotSource");
            } else {
                n.put("snapshotSource", snapshot.getSnapshotSource());
            }
            if (snapshot.getSnapshotVersion() == null) {
                n.putNull("snapshotVersion");
            } else {
                n.put("snapshotVersion", snapshot.getSnapshotVersion());
            }
            return EXPLAIN_JSON.writeValueAsString(n);
        } catch (Exception ignored) {
            return null;
        }
    }

    private void insertScoreItemWithRetry(ScoreItemDO scoreItemDO) {
        // 仅处理 score_id 主键冲突，快速重试以恢复样本生成链路。
        final int maxAttempts = 3;
        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            scoreItemDO.setScoreId("sc-" + UUID.randomUUID().toString().replace("-", ""));
            try {
                scoreItemMapper.insert(scoreItemDO);
                return;
            } catch (DuplicateKeyException ex) {
                if (attempt == maxAttempts) {
                    throw ex;
                }
            }
        }
    }

    private String resolveActiveRuleVersion() {
        return ruleConfigService != null ? ruleConfigService.resolveActiveRuleVersion() : DEFAULT_ACTIVE_RULE_VERSION;
    }

    /**
     * 本次分析可解释快照：字段来自本 run 的 evidence/score/decision（含冲突与困惑），非空对象。
     */
    private String buildExplanationJson(AssetAnalysisVO analysis, DecisionBundleVO decision,
                                        List<EvidenceItemVO> evidences, List<ScoreItemVO> scores) {
        ObjectNode root = EXPLAIN_JSON.createObjectNode();
        root.put("version", "1");
        root.put("analysisId", analysis.getAnalysisId());
        root.put("symbol", analysis.getSymbol());
        root.put("timeframe", analysis.getTimeframe() != null ? analysis.getTimeframe() : "");
        String summary = decision.getConclusionSummary();
        if (summary != null && summary.length() > 480) {
            summary = summary.substring(0, 477) + "...";
        }
        root.put("summary", summary != null ? summary : "");

        ArrayNode drivers = EXPLAIN_JSON.createArrayNode();
        if (evidences != null) {
            for (EvidenceItemVO e : evidences) {
                if (e == null || e.getDescription() == null || e.getDescription().isBlank()) {
                    continue;
                }
                drivers.add(e.getDescription().trim());
                if (drivers.size() >= 5) {
                    break;
                }
            }
        }
        if (drivers.isEmpty() && scores != null) {
            for (ScoreItemVO s : scores) {
                if (s == null) {
                    continue;
                }
                String line = (s.getScoreType() != null ? s.getScoreType() : "score")
                        + "=" + (s.getScoreValue() != null ? s.getScoreValue() : "0");
                drivers.add(line);
                if (drivers.size() >= 4) {
                    break;
                }
            }
        }
        if (drivers.isEmpty()) {
            drivers.add("multi_tf_convergence=" + decision.getMultiTfConvergence());
            drivers.add("market_bias=" + decision.getMarketBiasHierarchy());
        }
        root.set("primaryDrivers", drivers);

        root.set("reviewReasons", parseReviewReasonsArray(decision.getReviewReasons()));

        ObjectNode conflict = EXPLAIN_JSON.createObjectNode();
        conflict.put("level", decision.getAiConflictLevel() != null ? decision.getAiConflictLevel() : "");
        conflict.put("score", decision.getAiConflictScore() != null ? decision.getAiConflictScore() : 0);
        conflict.put("planMode", decision.getAiPlanMode() != null ? decision.getAiPlanMode() : "");
        root.set("conflict", conflict);

        ObjectNode confused = EXPLAIN_JSON.createObjectNode();
        confused.put("score", decision.getConfusedScore() != null ? decision.getConfusedScore() : 0);
        root.set("confused", confused);

        ObjectNode external = EXPLAIN_JSON.createObjectNode();
        external.put("status", decision.getExternalContextStatus() != null ? decision.getExternalContextStatus() : "READY");
        external.put("sourceHealth", decision.getExternalContextSourceHealth() != null ? decision.getExternalContextSourceHealth() : "OK");
        external.put("riskLevel", decision.getExternalContextRiskLevel() != null ? decision.getExternalContextRiskLevel() : "LOW");
        external.put("blocked", Boolean.TRUE.equals(decision.getExternalContextBlocked()));
        external.put("activeCount", decision.getActiveExternalEventCount() != null ? decision.getActiveExternalEventCount() : 0);
        ArrayNode externalReasons = EXPLAIN_JSON.createArrayNode();
        if (decision.getExternalContextReasonCodes() != null) {
            decision.getExternalContextReasonCodes().forEach(externalReasons::add);
        }
        external.set("reasonCodes", externalReasons);
        root.set("externalContext", external);

        ObjectNode derivatives = EXPLAIN_JSON.createObjectNode();
        derivatives.put("status", decision.getDerivativesStatus() != null ? decision.getDerivativesStatus() : "WAITING_SYNC");
        derivatives.put("freshness", decision.getDerivativesFreshness() != null ? decision.getDerivativesFreshness() : "UNAVAILABLE");
        derivatives.put("required", Boolean.TRUE.equals(decision.getDerivativesRequired()));
        derivatives.put("confirmEligible", Boolean.TRUE.equals(decision.getDerivativesConfirmEligible()));
        derivatives.put("pushMode", decision.getDerivativesPushMode() != null ? decision.getDerivativesPushMode() : "NONE");
        derivatives.put("providerDataTime", decision.getDerivativesProviderDataTime() == null
                ? "" : decision.getDerivativesProviderDataTime().toString());
        derivatives.put("traceId", decision.getDerivativesTraceId() != null ? decision.getDerivativesTraceId() : "");
        ArrayNode derivativesReasons = EXPLAIN_JSON.createArrayNode();
        decision.getDerivativesReasonCodes().forEach(derivativesReasons::add);
        derivatives.set("reasonCodes", derivativesReasons);
        root.set("derivatives", derivatives);

        try {
            return EXPLAIN_JSON.writeValueAsString(root);
        } catch (Exception e) {
            return "{\"version\":\"1\",\"summary\":\"serialization_failed\",\"primaryDrivers\":[],\"reviewReasons\":[],"
                    + "\"conflict\":{\"level\":\"\",\"score\":0,\"planMode\":\"\"},\"confused\":{\"score\":0}}";
        }
    }
}
