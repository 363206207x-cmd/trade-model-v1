package org.example.trademodel.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.example.trademodel.market.RealMarketEnvironmentService;
import org.example.trademodel.common.EvidenceTypeConstants;
import org.example.trademodel.entity.*;
import org.example.trademodel.enums.AssetStateEnum;
import org.example.trademodel.enums.HotResetEventTypeEnum;
import org.example.trademodel.mapper.*;
import org.example.trademodel.service.*;
import org.example.trademodel.vo.*;
import org.example.trademodel.entity.RuleConfigDO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
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
                                        MissedOpportunityService missedOpportunityService) {
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
    }

    @Override
    @Transactional
    public AssetAnalysisVO assemble(String symbol, String timeframe) {
        long assembleStart = System.currentTimeMillis();
        String analysisId = "ana-" + UUID.randomUUID().toString().substring(0, 8);
        System.out.println("=== 开始执行 assemble 方法 === symbol=" + symbol + ", timeframe=" + timeframe);

        try {
            MarketEnvironmentVO marketEnv = new MarketEnvironmentVO();
            marketEnv.setSummary("Real K-line data from Binance");
            String marketEnvSourceType = MARKET_ENV_SOURCE_FALLBACK;
            if (realMarketEnvironmentService != null) {
                MarketEnvironmentVO quoteEnv = realMarketEnvironmentService
                        .tryBuildFromRealQuote(symbol, timeframe)
                        .orElse(null);
                if (quoteEnv != null) {
                    marketEnv = quoteEnv;
                    enrichOpenInterestDeltaFromPreviousSnapshot(marketEnv, symbol, timeframe);
                    marketEnv.setDerivativesCrowdingState(
                            RealMarketEnvironmentService.computeDerivativesCrowdingState(marketEnv));
                    marketEnvSourceType = marketEnvSourceTypeForSuccessfulQuote(quoteEnv);
                    log.info("[market-env] assemble uses Binance market-env heuristic symbol={} tf={} sourceType={}",
                            symbol, timeframe, marketEnvSourceType);
                } else {
                    log.info("[market-env] assemble fallback placeholder symbol={} tf={}", symbol, timeframe);
                }
            }

            AssetAnalysisVO scoreInput = new AssetAnalysisVO();
            scoreInput.setAnalysisId(analysisId);
            List<EvidenceItemVO> evidences = evidenceService.buildEvidence(scoreInput, marketEnv);
            scoreInput.setEvidenceList(evidences);
            List<ScoreItemVO> scores = scoreService.buildScoreList(scoreInput, marketEnv);
            int dataQualityScore = estimateDataQualityScore(evidences, scores, marketEnvSourceType);
            Integer trendStructureScore = extractTrendStructureScore(scores);

            DecisionBundleVO decision = decisionEngineService.makeDecision(
                    symbol,
                    timeframe,
                    analysisId,
                    dataQualityScore,
                    trendStructureScore);

            ExecutionPlanVO plan = planService.generateExecutionPlan(decision, scores, marketEnv, new AssetAnalysisVO());

            AssetAnalysisVO analysis = new AssetAnalysisVO();
            analysis.setAnalysisId(analysisId);
            analysis.setSymbol(symbol);
            analysis.setTimeframe(timeframe);
            analysis.setAnalysisTime(LocalDateTime.now().toString());
            analysis.setMarketEnvironment(marketEnv);
            analysis.setEvidenceList(evidences);
            analysis.setScoreList(scores);
            analysis.setDecisionBundle(decision);
            analysis.setDataQualityScore(dataQualityScore);

            System.out.println("=== 准备执行落库 saveToDatabase === analysisId=" + analysisId);
            saveToDatabase(analysis, evidences, scores, decision, plan, marketEnvSourceType);
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

    private void saveToDatabase(AssetAnalysisVO analysis, List<EvidenceItemVO> evidences,
                                List<ScoreItemVO> scores, DecisionBundleVO decision, ExecutionPlanVO plan,
                                String marketEnvSourceType) {
        System.out.println("落库开始 - analysisId = " + analysis.getAnalysisId());
        String decisionInvalidCondition = null;
        HotResetCommand hotResetCommand = null;
        boolean hotWouldReset = false;
        try {
            // 1. AnalysisRun
            AnalysisRunDO run = new AnalysisRunDO();
            run.setAnalysisId(analysis.getAnalysisId());
            run.setSymbol(analysis.getSymbol());
            run.setTimeframe(analysis.getTimeframe());
            run.setAnalysisTime(LocalDateTime.now());
            run.setRuleVersion(resolveActiveRuleVersion());
            run.setDataQualityScore(analysis.getDataQualityScore());
            run.setTraceId("trace-" + System.currentTimeMillis());
            run.setStatus("SUCCESS");
            analysisRunMapper.insert(run);

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

                missedOpportunityService.recordFromAuthoritativeAnalysisIfEligible(
                        analysis.getAnalysisId(),
                        analysis.getSymbol(),
                        run.getTraceId(),
                        decision,
                        hotWouldReset);
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
                pdo.setRecommendedAction(plan.getRecommendedAction());
                pdo.setEntryZone(plan.getEntryZone());
                pdo.setStopLoss(plan.getStopLoss());
                pdo.setTakeProfitRules(plan.getTakeProfitRules());
                pdo.setLeverageSuggestion(plan.getLeverageSuggestion());
                pdo.setPositionSuggestion(plan.getPositionSuggestion());
                pdo.setAccountRiskJson(buildExecutionAccountRiskJson(analysis.getAnalysisId()));
                // invalid condition 第三刀：execution 对象同源镜像承接 decision 侧真值，不做二次推导。
                pdo.setInvalidCondition(decisionInvalidCondition != null && !decisionInvalidCondition.trim().isEmpty()
                        ? decisionInvalidCondition
                        : null);
                pdo.setCreateTime(LocalDateTime.now());
                executionPlanMapper.insert(pdo);
            }

            if (hotWouldReset && hotResetCommand != null) {
                hotResetService.evaluateAndExecute(hotResetCommand);
            }

            monitorAlertWriteService.emitAfterAnalysisPersist(run, analysis, decision);

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
        Map<String, RuleConfigDO> ruleMap = ruleConfigService != null
                ? ruleConfigService.getRuleConfigMap()
                : null;
        if (ruleMap == null) {
            return DEFAULT_ACTIVE_RULE_VERSION;
        }
        RuleConfigDO cfg = ruleMap.get(KEY_ACTIVE_VERSION_FALLBACK);
        if (cfg == null || cfg.getRuleValue() == null) {
            return DEFAULT_ACTIVE_RULE_VERSION;
        }
        String v = cfg.getRuleValue().trim();
        return v.isEmpty() ? DEFAULT_ACTIVE_RULE_VERSION : v;
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

        try {
            return EXPLAIN_JSON.writeValueAsString(root);
        } catch (Exception e) {
            return "{\"version\":\"1\",\"summary\":\"serialization_failed\",\"primaryDrivers\":[],\"reviewReasons\":[],"
                    + "\"conflict\":{\"level\":\"\",\"score\":0,\"planMode\":\"\"},\"confused\":{\"score\":0}}";
        }
    }
}
