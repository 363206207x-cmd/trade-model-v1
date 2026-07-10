package org.example.trademodel.derivatives;

import org.example.trademodel.entity.RuleConfigDO;
import org.example.trademodel.enums.AssetStateEnum;
import org.example.trademodel.providercall.ProviderDatasetType;
import org.example.trademodel.providercall.SnapshotFreshnessStatus;
import org.example.trademodel.providercall.UnifiedSourceStatus;
import org.example.trademodel.providercall.snapshot.DerivativesRiskSnapshot;
import org.example.trademodel.service.RuleConfigService;
import org.example.trademodel.vo.DecisionBundleVO;
import org.example.trademodel.vo.EvidenceItemVO;
import org.example.trademodel.vo.ExecutionPlanVO;
import org.example.trademodel.vo.ScoreItemVO;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

@Service
public class DerivativesBusinessIntegrationService {
    public static final String TREND_SCORE = "趋势结构分";
    public static final String CAPITAL_SCORE = "资金推动分";
    public static final String LEVERAGE_SCORE = "杠杆风险分";
    public static final String LIQUIDITY_SCORE = "流动性质量分";
    public static final String SENTIMENT_SCORE = "情绪温度分";
    public static final String EVENT_SCORE = "事件冲击分";
    public static final String MACRO_SCORE = "宏观环境分";
    public static final String CREDIBILITY_SCORE = "综合可信度分";

    private static final String OI_DATASET = ProviderDatasetType.COINGLASS_OPEN_INTEREST.name();
    private static final String FUNDING_DATASET = ProviderDatasetType.COINGLASS_FUNDING.name();
    private static final String LIQUIDATION_DATASET = ProviderDatasetType.COINGLASS_LIQUIDATION.name();
    private static final String LONG_SHORT_DATASET = ProviderDatasetType.COINGLASS_LONG_SHORT_RATIO.name();
    private static final List<String> FORMAL_TIMEFRAMES = List.of("5m", "15m", "1h", "4h");

    private final RuleConfigService ruleConfigService;

    public DerivativesBusinessIntegrationService(RuleConfigService ruleConfigService) {
        this.ruleConfigService = ruleConfigService;
    }

    public DerivativesBusinessAssessment evaluate(DerivativesBusinessInput input) {
        if (input == null) {
            throw new IllegalArgumentException("derivatives business input is required");
        }
        Config config = Config.load(ruleConfigService);
        DerivativesRiskSnapshot snapshot = input.snapshot();
        if (snapshot == null) {
            return unavailable(input, config, "DERIVATIVES_SNAPSHOT_UNAVAILABLE");
        }

        Set<String> reasons = new LinkedHashSet<>(snapshot.reasonCodes());
        List<DerivativesEvidenceItem> evidence = new ArrayList<>();
        boolean stale = isStale(snapshot, config.maxDataAgeSeconds);
        boolean partial = !"COMPLETE".equalsIgnoreCase(snapshot.evidenceAvailability())
                || snapshot.availableDatasets().size() < config.minimumDatasetCount
                || !snapshot.missingDatasets().isEmpty()
                || !snapshot.degradedDatasets().isEmpty();
        boolean oiReady = contains(snapshot.availableDatasets(), OI_DATASET)
                && snapshot.openInterestUsd() != null
                && (snapshot.openInterestChange5m() != null || snapshot.openInterestChange15m() != null);
        boolean fundingReady = contains(snapshot.availableDatasets(), FUNDING_DATASET)
                && snapshot.weightedFundingRate() != null;

        if (stale) {
            reasons.add("DERIVATIVES_STALE");
            evidence.add(item(input, snapshot, DerivativesEvidenceType.DERIVATIVES_DATA_STALE,
                    "NEUTRAL", null, null, "GLOBAL", "metadata.freshnessStatus", "DERIVATIVES_STALE", 100, 100));
        } else if (partial) {
            reasons.add("DERIVATIVES_PARTIAL");
            evidence.add(item(input, snapshot, DerivativesEvidenceType.DERIVATIVES_DATA_PARTIAL,
                    "NEUTRAL", null, null, "GLOBAL", "availableDatasets/missingDatasets",
                    "DERIVATIVES_PARTIAL", 70, 70));
        }

        BigDecimal oiChange = first(snapshot.openInterestChange5m(), snapshot.openInterestChange15m());
        String oiWindow = snapshot.openInterestChange5m() != null ? "5m" : "15m";
        BigDecimal oiWeak = "5m".equals(oiWindow) ? config.oiChange5mWeak : config.oiChange15mWeak;
        BigDecimal oiStrong = "5m".equals(oiWindow) ? config.oiChange5mStrong : config.oiChange15mStrong;
        boolean oiExpansion = oiChange != null && oiChange.compareTo(oiWeak) >= 0;
        boolean oiContraction = oiChange != null && oiChange.compareTo(oiWeak.negate()) <= 0;
        boolean oiCollapse = oiChange != null && oiChange.compareTo(config.oiCollapseThreshold) <= 0;
        if (oiExpansion) {
            evidence.add(item(input, snapshot, DerivativesEvidenceType.OPEN_INTEREST_EXPANSION,
                    "NEUTRAL", oiChange, oiWeak, oiWindow, "openInterestChange" + oiWindow,
                    oiChange.compareTo(oiStrong) >= 0 ? "OI_EXPANSION_STRONG" : "OI_EXPANSION_WEAK",
                    oiChange.compareTo(oiStrong) >= 0 ? 90 : 65, 80));
        } else if (oiContraction) {
            evidence.add(item(input, snapshot, DerivativesEvidenceType.OPEN_INTEREST_CONTRACTION,
                    "NEUTRAL", oiChange, oiWeak.negate(), oiWindow, "openInterestChange" + oiWindow,
                    oiCollapse ? "OI_COLLAPSE" : "OI_CONTRACTION", oiCollapse ? 100 : 70, 80));
        }

        int priceSign = compare(input.currentPrice(), input.comparisonPrice());
        int oiSign = sign(oiChange);
        boolean directionConfirmed = false;
        boolean causeEffectDivergence = false;
        if (priceSign != 0 && oiSign != 0) {
            if (oiSign > 0) {
                String confirmationDirection = priceSign > 0 ? "BULLISH" : "BEARISH";
                directionConfirmed = input.volumeConfirmed()
                        && confirmationDirection.equals(normalizeDirection(input.baseDirection()));
                if (input.volumeConfirmed()) {
                    evidence.add(item(input, snapshot, DerivativesEvidenceType.OPEN_INTEREST_PRICE_CONFIRMATION,
                            confirmationDirection, oiChange, input.currentPrice(), oiWindow,
                            "openInterestChange" + oiWindow + "+ohlcv.close+ohlcv.volume",
                            priceSign > 0 ? "PRICE_UP_OI_UP_VOLUME_CONFIRMED"
                                    : "PRICE_DOWN_OI_UP_VOLUME_CONFIRMED", 85, 85));
                } else {
                    reasons.add("PRICE_OI_ALIGNED_VOLUME_UNCONFIRMED");
                }
            } else {
                causeEffectDivergence = true;
                String code = priceSign > 0 ? "PRICE_UP_OI_DOWN_SHORT_COVERING" : "PRICE_DOWN_OI_DOWN_DELEVERAGING";
                evidence.add(item(input, snapshot, DerivativesEvidenceType.OPEN_INTEREST_PRICE_DIVERGENCE,
                        "NEUTRAL", oiChange, input.currentPrice(), oiWindow,
                        "openInterestChange" + oiWindow + "+ohlcv.close", code, 85, 80));
                reasons.add(code);
            }
        }

        boolean fundingPositiveExtreme = false;
        boolean fundingNegativeExtreme = false;
        if (snapshot.weightedFundingRate() != null) {
            BigDecimal funding = snapshot.weightedFundingRate();
            fundingPositiveExtreme = funding.compareTo(config.fundingPositiveExtreme) >= 0;
            fundingNegativeExtreme = funding.compareTo(config.fundingNegativeExtreme) <= 0;
            DerivativesEvidenceType type = fundingPositiveExtreme
                    ? DerivativesEvidenceType.FUNDING_POSITIVE_EXTREME
                    : fundingNegativeExtreme ? DerivativesEvidenceType.FUNDING_NEGATIVE_EXTREME
                    : DerivativesEvidenceType.FUNDING_NORMAL;
            evidence.add(item(input, snapshot, type, "NEUTRAL", funding,
                    funding.signum() >= 0 ? config.fundingPositiveExtreme : config.fundingNegativeExtreme,
                    "CURRENT", "weightedFundingRate",
                    type.name(), fundingPositiveExtreme || fundingNegativeExtreme ? 90 : 40, 85));
        }

        boolean longCrowding = snapshot.longShortRatio() != null
                && snapshot.longShortRatio().compareTo(config.longCrowding) >= 0;
        boolean shortCrowding = snapshot.longShortRatio() != null
                && snapshot.longShortRatio().compareTo(config.shortCrowding) <= 0;
        if (longCrowding || shortCrowding) {
            DerivativesEvidenceType type = longCrowding
                    ? DerivativesEvidenceType.LONG_CROWDING : DerivativesEvidenceType.SHORT_CROWDING;
            evidence.add(item(input, snapshot, type, "NEUTRAL", snapshot.longShortRatio(),
                    longCrowding ? config.longCrowding : config.shortCrowding,
                    "CURRENT", "longShortRatio", type.name(), 75, 70));
        }

        BigDecimal longLiquidation = first(snapshot.longLiquidationUsd5m(), snapshot.longLiquidationUsd15m());
        BigDecimal shortLiquidation = first(snapshot.shortLiquidationUsd5m(), snapshot.shortLiquidationUsd15m());
        String liquidationWindow = snapshot.longLiquidationUsd5m() != null || snapshot.shortLiquidationUsd5m() != null
                ? "5m" : "15m";
        BigDecimal liquidationThreshold = "5m".equals(liquidationWindow)
                ? config.liquidationSpike5m : config.liquidationSpike15m;
        boolean longLiquidationSpike = longLiquidation != null
                && longLiquidation.compareTo(liquidationThreshold) >= 0;
        boolean shortLiquidationSpike = shortLiquidation != null
                && shortLiquidation.compareTo(liquidationThreshold) >= 0;
        if (longLiquidationSpike) {
            evidence.add(item(input, snapshot, DerivativesEvidenceType.LONG_LIQUIDATION_SPIKE,
                    "NEUTRAL", longLiquidation, liquidationThreshold, liquidationWindow,
                    "longLiquidationUsd" + liquidationWindow, "LONG_LIQUIDATION_SPIKE", 95, 80));
        }
        if (shortLiquidationSpike) {
            evidence.add(item(input, snapshot, DerivativesEvidenceType.SHORT_LIQUIDATION_SPIKE,
                    "NEUTRAL", shortLiquidation, liquidationThreshold, liquidationWindow,
                    "shortLiquidationUsd" + liquidationWindow, "SHORT_LIQUIDATION_SPIKE", 95, 80));
        }
        boolean liquidationImbalance = liquidationImbalance(longLiquidation, shortLiquidation, config.liquidationImbalance);
        if (liquidationImbalance) {
            BigDecimal ratio = imbalanceRatio(longLiquidation, shortLiquidation);
            evidence.add(item(input, snapshot, DerivativesEvidenceType.LIQUIDATION_IMBALANCE,
                    "NEUTRAL", ratio, config.liquidationImbalance, liquidationWindow,
                    "longLiquidationUsd/shortLiquidationUsd", "LIQUIDATION_IMBALANCE", 85, 75));
        }

        boolean concentrationHigh = snapshot.exchangeConcentrationScore() != null
                && snapshot.exchangeConcentrationScore().compareTo(config.exchangeConcentrationHigh) >= 0;
        if (concentrationHigh) {
            evidence.add(item(input, snapshot, DerivativesEvidenceType.EXCHANGE_CONCENTRATION_HIGH,
                    "NEUTRAL", snapshot.exchangeConcentrationScore(), config.exchangeConcentrationHigh,
                    "CURRENT", "exchangeConcentrationScore", "EXCHANGE_CONCENTRATION_HIGH", 85, 70));
        }

        boolean fundingCrowdingRisk = (fundingPositiveExtreme && longCrowding)
                || (fundingNegativeExtreme && shortCrowding);
        boolean liquidationRisk = longLiquidationSpike || shortLiquidationSpike || liquidationImbalance;
        boolean highRisk = fundingCrowdingRisk || liquidationRisk || concentrationHigh || oiCollapse;
        boolean formalConvergence = formalConvergence(input.timeframeDirections(), input.baseDirection());
        boolean mandatoryReady = oiReady && fundingReady;
        boolean sourceReady = snapshot.sourceStatus() == UnifiedSourceStatus.READY && !stale;
        int quality = input.dataQualityScore() == null ? 0 : input.dataQualityScore();
        boolean stateBlocked = input.currentState() == AssetStateEnum.CONFUSED
                || input.currentState() == AssetStateEnum.HIGH_RISK
                || input.currentState() == AssetStateEnum.COOLING
                || input.currentState() == AssetStateEnum.INVALIDATED;
        boolean confirmEligible = sourceReady && mandatoryReady && formalConvergence
                && input.currentPriceFresh() && quality >= config.minimumDataQualityScore
                && input.accountRiskAllowed() && !highRisk
                && !stateBlocked && !causeEffectDivergence;

        Map<String, Double> scoreDeltas = emptyScoreDeltas();
        if (directionConfirmed) {
            add(scoreDeltas, TREND_SCORE, 4);
            add(scoreDeltas, CAPITAL_SCORE, 10);
            add(scoreDeltas, CREDIBILITY_SCORE, 4);
            reasons.add("DERIVATIVES_DIRECTION_CONFIRMED");
        }
        if (causeEffectDivergence) {
            add(scoreDeltas, TREND_SCORE, -4);
            add(scoreDeltas, CAPITAL_SCORE, -10);
            add(scoreDeltas, CREDIBILITY_SCORE, -5);
        }
        if (fundingPositiveExtreme || fundingNegativeExtreme) {
            add(scoreDeltas, LEVERAGE_SCORE, -12);
            add(scoreDeltas, SENTIMENT_SCORE, fundingPositiveExtreme ? 5 : -5);
            reasons.add(fundingPositiveExtreme ? "FUNDING_RISK_EXTREME_POSITIVE" : "FUNDING_RISK_EXTREME_NEGATIVE");
        }
        if (longCrowding || shortCrowding) {
            add(scoreDeltas, LEVERAGE_SCORE, -8);
            add(scoreDeltas, SENTIMENT_SCORE, longCrowding ? 8 : -8);
        }
        if (liquidationRisk) {
            add(scoreDeltas, LEVERAGE_SCORE, -12);
            add(scoreDeltas, LIQUIDITY_SCORE, -15);
            add(scoreDeltas, EVENT_SCORE, -15);
            reasons.add("LIQUIDATION_RISK_BLOCKED");
        }
        if (concentrationHigh) {
            add(scoreDeltas, LIQUIDITY_SCORE, -10);
            add(scoreDeltas, CREDIBILITY_SCORE, -4);
        }
        int qualityDiscount = stale ? config.staleConfidencePenalty
                : partial ? config.partialConfidencePenalty : 0;
        if (qualityDiscount > 0) {
            add(scoreDeltas, CREDIBILITY_SCORE, -qualityDiscount);
        }
        capScoreDeltas(scoreDeltas, config);

        int confidenceAdjustment = directionConfirmed && !partial ? 1 : 0;
        if (partial) confidenceAdjustment--;
        if (stale || highRisk) confidenceAdjustment--;
        if (!mandatoryReady && config.requiredForConfirm) {
            reasons.add(!oiReady ? "DERIVATIVES_REQUIRED:OPEN_INTEREST" : "DERIVATIVES_REQUIRED:FUNDING");
        }
        String riskAdjustment = highRisk ? "HIGH" : partial || causeEffectDivergence ? "MEDIUM" : "NONE";
        String planMode = highRisk && config.highRiskPlanDowngrade ? "WARNING_ONLY"
                : (!mandatoryReady && config.requiredForConfirm) || stale ? "PREPARE_ONLY"
                : partial ? "REDUCED" : "CONFIRM";
        AssetStateEnum opportunityState = opportunityState(input, confirmEligible, highRisk, stale, partial);
        String pushMode = highRisk || stale ? "WARNING_PUSH"
                : opportunityState == AssetStateEnum.TRIGGERED ? "CONFIRM_PUSH"
                : opportunityState == AssetStateEnum.CANDIDATE || opportunityState == AssetStateEnum.WAITING_TRIGGER
                ? "PREPARE_PUSH" : "NONE";
        boolean needsRevalidation = stale || highRisk || causeEffectDivergence;

        return new DerivativesBusinessAssessment(input.symbol(), normalizeDirection(input.baseDirection()),
                snapshot.sourceStatus(), snapshot.freshnessStatus(), snapshot.evidenceAvailability(),
                snapshot.providerDataTime(), snapshot.fetchTime(), evidence, scoreDeltas, qualityDiscount,
                confidenceAdjustment, riskAdjustment, planMode, opportunityState, pushMode, confirmEligible,
                sourceReady && mandatoryReady, needsRevalidation, oiCollapse || liquidationRisk || concentrationHigh,
                causeEffectDivergence ? 15 : 0,
                fundingCrowdingRisk || liquidationRisk ? 20 : partial ? 8 : 0,
                liquidationRisk ? 20 : 0,
                causeEffectDivergence ? 25 : 0,
                snapshot.availableDatasets(), snapshot.missingDatasets(), snapshot.degradedDatasets(),
                List.copyOf(reasons), config.fallbackReasons, input.traceId(), input.analysisId(), input.ruleVersion(),
                input.positionOpen());
    }

    public List<EvidenceItemVO> toEvidenceVos(DerivativesBusinessAssessment assessment) {
        if (assessment == null) return List.of();
        List<EvidenceItemVO> result = new ArrayList<>();
        for (DerivativesEvidenceItem item : assessment.evidence()) {
            EvidenceItemVO vo = new EvidenceItemVO();
            vo.setEvidenceId("deriv-" + item.evidenceType().name() + "-" + result.size());
            vo.setEvidenceType(item.evidenceType().name());
            vo.setDirection(item.direction());
            vo.setStrength(item.strength() == null ? null : item.strength().doubleValue());
            vo.setConfidence(item.confidence() == null ? null : item.confidence().doubleValue());
            vo.setSource("PROVIDER_SNAPSHOT");
            vo.setSourceProvider(item.provider());
            vo.setSourceTraceId(item.traceId());
            vo.setSourceReference(sourceReference(item));
            vo.setDescription(item.evidenceType().name() + " | " + item.reasonCode());
            vo.setTimestamp(item.providerDataTime() == null ? null : item.providerDataTime().toString());
            result.add(vo);
        }
        return result;
    }

    public void applyScoreAdjustments(List<ScoreItemVO> scores, DerivativesBusinessAssessment assessment) {
        if (scores == null || assessment == null) return;
        for (ScoreItemVO score : scores) {
            if (score == null || score.getScoreType() == null || score.getScoreValue() == null) continue;
            double delta = assessment.scoreDeltas().getOrDefault(score.getScoreType(), 0.0);
            score.setScoreValue(clamp(score.getScoreValue() + delta, 0, 100));
            if (delta != 0) {
                String suffix = "DERIVATIVES_SCORE_CONTRIBUTION=" + delta
                        + ";DERIVATIVES_SCORE_CAP=20;DERIVATIVES_DATA_QUALITY_DISCOUNT="
                        + assessment.dataQualityDiscount();
                score.setDescription((score.getDescription() == null ? "" : score.getDescription() + " | ") + suffix);
            }
        }
    }

    public int monitorRefreshSeconds() {
        return Config.load(ruleConfigService).monitorRefreshSeconds;
    }

    public void applyDecisionAdjustments(DecisionBundleVO decision, DerivativesBusinessAssessment assessment) {
        if (decision == null || assessment == null) return;
        String originalDirection = decision.getMarketBiasHierarchy();
        decision.setConfidenceLevel(adjustConfidence(decision.getConfidenceLevel(), assessment.confidenceAdjustment()));
        if (assessment.isHighRisk()) {
            decision.setRiskLevel("HIGH");
        } else if ("MEDIUM".equals(assessment.riskAdjustment()) && "LOW".equalsIgnoreCase(decision.getRiskLevel())) {
            decision.setRiskLevel("MEDIUM");
        }
        if (assessment.blocksConfirmPlan() && "CONFIRM".equalsIgnoreCase(decision.getAiPlanMode())) {
            decision.setAiPlanMode(assessment.planMode());
        } else if (assessment.blocksConfirmPlan() || decision.getAiPlanMode() == null) {
            decision.setAiPlanMode(assessment.planMode());
        }
        if (assessment.blocksConfirmPlan()
                || assessment.sourceStatus() == UnifiedSourceStatus.STALE
                || assessment.sourceStatus() == UnifiedSourceStatus.ERROR
                || assessment.sourceStatus() == UnifiedSourceStatus.NOT_CONFIGURED
                || assessment.sourceStatus() == UnifiedSourceStatus.DISABLED
                || assessment.sourceStatus() == UnifiedSourceStatus.WAITING_SYNC
                || "UNAVAILABLE".equalsIgnoreCase(assessment.evidenceAvailability())) {
            decision.setIsWorthOpening(false);
        }
        decision.setDerivativesStatus(assessment.sourceStatus() == null ? null : assessment.sourceStatus().name());
        decision.setDerivativesFreshness(assessment.freshnessStatus() == null ? null : assessment.freshnessStatus().name());
        decision.setDerivativesRequired(true);
        decision.setDerivativesConfirmEligible(assessment.confirmEligible());
        decision.setDerivativesPushMode(assessment.pushMode());
        decision.setDerivativesReasonCodes(assessment.reasonCodes());
        decision.setDerivativesProviderDataTime(assessment.providerDataTime());
        decision.setDerivativesTraceId(assessment.traceId());
        decision.setMarketBiasHierarchy(originalDirection);
    }

    public void applyPlanAdjustments(ExecutionPlanVO plan, DerivativesBusinessAssessment assessment) {
        if (plan == null || assessment == null) return;
        plan.setDerivativesStatus(assessment.sourceStatus() == null ? null : assessment.sourceStatus().name());
        plan.setDerivativesFreshness(assessment.freshnessStatus() == null ? null : assessment.freshnessStatus().name());
        plan.setDerivativesReasonCodes(assessment.reasonCodes());
        plan.setDerivativesProviderDataTime(assessment.providerDataTime());
        plan.setDerivativesTraceId(assessment.traceId());
        plan.setNeedsRevalidation(assessment.needsRevalidation());
        if (assessment.needsRevalidation()) {
            plan.setRevalidationReason(String.join(",", assessment.reasonCodes()));
        }
        if (assessment.blocksConfirmPlan()) {
            plan.setPlanMode(ExecutionPlanVO.PLAN_MODE_ADVISORY);
            plan.setManualReviewRequired(true);
            plan.setNotExecutable(true);
            plan.setNotAutoTrading(true);
            plan.setNotOrderExecution(true);
            plan.setNotUserPositionCreation(true);
            plan.setNotExecutableReason("DERIVATIVES_" + assessment.planMode());
            if (!assessment.confirmEligible()) {
                plan.setReadinessStatus(ExecutionPlanVO.READINESS_WATCH_ONLY);
            }
        }
        if ("HIGH".equals(assessment.riskAdjustment())) {
            plan.setLeverageSuggestion("人工复核：衍生品风险升高，保持低杠杆");
            plan.setPositionSuggestion("人工复核：降低风险暴露");
            plan.setAddPositionCondition("衍生品高风险期间暂不增加仓位");
        }
        plan.setReducePositionCondition(assessment.isHighRisk() ? "人工复核是否降低仓位" : plan.getReducePositionCondition());
        plan.setAbandonCondition(assessment.needsRevalidation() ? "衍生品结构变化，重新验证计划" : plan.getAbandonCondition());
    }

    public void applyOpportunityState(DecisionBundleVO decision, DerivativesBusinessAssessment assessment) {
        if (decision == null || assessment == null || assessment.opportunityState() == null) return;
        AssetStateEnum current = decision.getAssetState();
        if (current == AssetStateEnum.CONFUSED || current == AssetStateEnum.COOLING
                || current == AssetStateEnum.INVALIDATED) return;
        if (current == AssetStateEnum.HIGH_RISK && assessment.opportunityState() != AssetStateEnum.HIGH_RISK) return;
        decision.setAssetState(assessment.opportunityState());
    }

    private DerivativesBusinessAssessment unavailable(DerivativesBusinessInput input, Config config, String reason) {
        DerivativesEvidenceItem evidence = new DerivativesEvidenceItem(
                DerivativesEvidenceType.DERIVATIVES_DATA_UNAVAILABLE, input.symbol(), "NEUTRAL",
                BigDecimal.valueOf(100), BigDecimal.valueOf(100), null, null, "GLOBAL", "COINGLASS_V4",
                null, null, UnifiedSourceStatus.WAITING_SYNC, SnapshotFreshnessStatus.UNAVAILABLE,
                "snapshot", reason, input.traceId(), input.analysisId(), input.ruleVersion());
        Map<String, Double> deltas = emptyScoreDeltas();
        add(deltas, CREDIBILITY_SCORE, -config.staleConfidencePenalty);
        return new DerivativesBusinessAssessment(input.symbol(), normalizeDirection(input.baseDirection()),
                UnifiedSourceStatus.WAITING_SYNC, SnapshotFreshnessStatus.UNAVAILABLE, "UNAVAILABLE", null, null,
                List.of(evidence), deltas, config.staleConfidencePenalty, -2, "MEDIUM", "PREPARE_ONLY",
                AssetStateEnum.OBSERVING, "NONE", false, false, true, false,
                0, 10, 0, 0, List.of(), List.of(OI_DATASET, FUNDING_DATASET, LIQUIDATION_DATASET, LONG_SHORT_DATASET),
                List.of(), List.of(reason), config.fallbackReasons, input.traceId(), input.analysisId(),
                input.ruleVersion(), input.positionOpen());
    }

    private static AssetStateEnum opportunityState(DerivativesBusinessInput input, boolean confirmEligible,
                                                    boolean highRisk, boolean stale, boolean partial) {
        if (input.currentState() == AssetStateEnum.CONFUSED
                || input.currentState() == AssetStateEnum.COOLING
                || input.currentState() == AssetStateEnum.INVALIDATED
                || input.currentState() == AssetStateEnum.HIGH_RISK) {
            return input.currentState();
        }
        if (highRisk) return AssetStateEnum.HIGH_RISK;
        if (stale) return AssetStateEnum.OBSERVING;
        if (!formalConvergence(input.timeframeDirections(), input.baseDirection())) {
            return validDirection(input.baseDirection()) ? AssetStateEnum.CANDIDATE : AssetStateEnum.OBSERVING;
        }
        if (confirmEligible && input.planBoundaryComplete()) return AssetStateEnum.TRIGGERED;
        if (confirmEligible || !partial) return AssetStateEnum.WAITING_TRIGGER;
        return AssetStateEnum.CANDIDATE;
    }

    private static boolean formalConvergence(Map<String, String> directions, String baseDirection) {
        if (directions == null || !directions.keySet().containsAll(FORMAL_TIMEFRAMES)) return false;
        String normalizedBase = normalizeDirection(baseDirection);
        if (!validDirection(normalizedBase)) return false;
        if (!normalizeDirection(directions.get("4h")).equals(normalizeDirection(directions.get("1h")))) return false;
        long aligned = FORMAL_TIMEFRAMES.stream()
                .map(directions::get)
                .map(DerivativesBusinessIntegrationService::normalizeDirection)
                .filter(normalizedBase::equals)
                .count();
        return aligned >= 3;
    }

    private static DerivativesEvidenceItem item(DerivativesBusinessInput input, DerivativesRiskSnapshot snapshot,
                                                DerivativesEvidenceType type, String direction,
                                                BigDecimal currentValue, BigDecimal comparisonValue,
                                                String timeframe, String sourceField, String reasonCode,
                                                int strength, int confidence) {
        return new DerivativesEvidenceItem(type, input.symbol(), direction, BigDecimal.valueOf(strength),
                BigDecimal.valueOf(confidence), currentValue, comparisonValue, timeframe,
                snapshot.provider(), snapshot.providerDataTime(), snapshot.fetchTime(), snapshot.sourceStatus(),
                snapshot.freshnessStatus(), sourceField, reasonCode, input.traceId(), input.analysisId(),
                input.ruleVersion());
    }

    private static String sourceReference(DerivativesEvidenceItem item) {
        return "sourceField=" + value(item.sourceField())
                + ";currentValue=" + value(item.currentValue())
                + ";comparisonValue=" + value(item.comparisonValue())
                + ";timeframe=" + value(item.timeframe())
                + ";sourceStatus=" + value(item.sourceStatus())
                + ";freshnessStatus=" + value(item.freshnessStatus())
                + ";reasonCode=" + value(item.reasonCode())
                + ";analysisId=" + value(item.analysisId())
                + ";ruleVersion=" + value(item.ruleVersion());
    }

    private static String adjustConfidence(String current, int adjustment) {
        int level = switch (current == null ? "LOW" : current.trim().toUpperCase(Locale.ROOT)) {
            case "HIGH" -> 2;
            case "MEDIUM" -> 1;
            default -> 0;
        };
        int adjusted = Math.max(0, Math.min(2, level + adjustment));
        return adjusted == 2 ? "HIGH" : adjusted == 1 ? "MEDIUM" : "LOW";
    }

    private static Map<String, Double> emptyScoreDeltas() {
        Map<String, Double> result = new LinkedHashMap<>();
        for (String score : List.of(TREND_SCORE, CAPITAL_SCORE, LEVERAGE_SCORE, LIQUIDITY_SCORE,
                SENTIMENT_SCORE, EVENT_SCORE, MACRO_SCORE, CREDIBILITY_SCORE)) {
            result.put(score, 0.0);
        }
        return result;
    }

    private static void add(Map<String, Double> values, String key, double delta) {
        values.put(key, values.getOrDefault(key, 0.0) + delta);
    }

    private static void capScoreDeltas(Map<String, Double> values, Config config) {
        for (Map.Entry<String, Double> entry : values.entrySet()) {
            double cap = TREND_SCORE.equals(entry.getKey()) ? config.trendScoreCap : config.scoreCap;
            entry.setValue(clamp(entry.getValue(), -cap, cap));
        }
    }

    private static boolean isStale(DerivativesRiskSnapshot snapshot, int maxAgeSeconds) {
        if (snapshot.sourceStatus() == UnifiedSourceStatus.STALE
                || snapshot.freshnessStatus() == SnapshotFreshnessStatus.STALE) return true;
        Instant dataTime = snapshot.providerDataTime();
        return dataTime != null && dataTime.plusSeconds(maxAgeSeconds).isBefore(Instant.now());
    }

    private static boolean liquidationImbalance(BigDecimal longValue, BigDecimal shortValue, BigDecimal threshold) {
        BigDecimal ratio = imbalanceRatio(longValue, shortValue);
        return ratio != null && ratio.compareTo(threshold) >= 0;
    }

    private static BigDecimal imbalanceRatio(BigDecimal first, BigDecimal second) {
        if (first == null || second == null || first.signum() < 0 || second.signum() < 0) return null;
        BigDecimal low = first.min(second);
        BigDecimal high = first.max(second);
        if (low.signum() == 0) return high.signum() == 0 ? BigDecimal.ONE : BigDecimal.valueOf(999);
        return high.divide(low, 8, RoundingMode.HALF_UP);
    }

    private static int compare(BigDecimal current, BigDecimal previous) {
        if (current == null || previous == null) return 0;
        return Integer.signum(current.compareTo(previous));
    }

    private static int sign(BigDecimal value) {
        return value == null ? 0 : Integer.signum(value.signum());
    }

    private static BigDecimal first(BigDecimal first, BigDecimal second) {
        return first != null ? first : second;
    }

    private static boolean contains(List<String> values, String expected) {
        return values != null && values.stream().anyMatch(expected::equals);
    }

    private static String normalizeDirection(String direction) {
        if (direction == null) return "NEUTRAL";
        String normalized = direction.trim().toUpperCase(Locale.ROOT);
        if ("LONG".equals(normalized)) return "BULLISH";
        if ("SHORT".equals(normalized)) return "BEARISH";
        return normalized;
    }

    private static boolean validDirection(String direction) {
        String normalized = normalizeDirection(direction);
        return "BULLISH".equals(normalized) || "BEARISH".equals(normalized);
    }

    private static double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }

    private static String value(Object value) {
        return value == null ? "" : value.toString();
    }

    private static final class Config {
        private final BigDecimal oiChange5mWeak;
        private final BigDecimal oiChange5mStrong;
        private final BigDecimal oiChange15mWeak;
        private final BigDecimal oiChange15mStrong;
        private final BigDecimal fundingPositiveExtreme;
        private final BigDecimal fundingNegativeExtreme;
        private final BigDecimal longCrowding;
        private final BigDecimal shortCrowding;
        private final BigDecimal liquidationSpike5m;
        private final BigDecimal liquidationSpike15m;
        private final BigDecimal liquidationImbalance;
        private final BigDecimal exchangeConcentrationHigh;
        private final int maxDataAgeSeconds;
        private final boolean requiredForConfirm;
        private final int minimumDatasetCount;
        private final int partialConfidencePenalty;
        private final int staleConfidencePenalty;
        private final int scoreCap;
        private final int trendScoreCap;
        private final int minimumDataQualityScore;
        private final boolean highRiskPlanDowngrade;
        private final int monitorRefreshSeconds;
        private final BigDecimal oiCollapseThreshold;
        private final List<String> fallbackReasons;

        private Config(Map<String, RuleConfigDO> map, List<String> fallbacks) {
            oiChange5mWeak = decimal(map, "derivatives_evidence_config.oi_change_5m_weak", "0.02", fallbacks);
            oiChange5mStrong = decimal(map, "derivatives_evidence_config.oi_change_5m_strong", "0.05", fallbacks);
            oiChange15mWeak = decimal(map, "derivatives_evidence_config.oi_change_15m_weak", "0.04", fallbacks);
            oiChange15mStrong = decimal(map, "derivatives_evidence_config.oi_change_15m_strong", "0.10", fallbacks);
            fundingPositiveExtreme = decimal(map, "derivatives_evidence_config.funding_positive_extreme", "0.0005", fallbacks);
            fundingNegativeExtreme = decimal(map, "derivatives_evidence_config.funding_negative_extreme", "-0.0005", fallbacks);
            longCrowding = decimal(map, "derivatives_evidence_config.long_short_long_crowding", "1.20", fallbacks);
            shortCrowding = decimal(map, "derivatives_evidence_config.long_short_short_crowding", "0.80", fallbacks);
            liquidationSpike5m = decimal(map, "derivatives_evidence_config.liquidation_spike_5m", "1000000", fallbacks);
            liquidationSpike15m = decimal(map, "derivatives_evidence_config.liquidation_spike_15m", "3000000", fallbacks);
            liquidationImbalance = decimal(map, "derivatives_evidence_config.liquidation_imbalance_ratio", "2.0", fallbacks);
            exchangeConcentrationHigh = boundedDecimal(map,
                    "derivatives_evidence_config.exchange_concentration_high", "0.70",
                    BigDecimal.ZERO, BigDecimal.ONE, fallbacks);
            maxDataAgeSeconds = integer(map, "derivatives_decision_config.derivatives_max_data_age_seconds", 120, fallbacks);
            requiredForConfirm = bool(map, "derivatives_decision_config.derivatives_required_for_confirm", true, fallbacks);
            minimumDatasetCount = integer(map, "derivatives_decision_config.derivatives_minimum_dataset_count", 2, fallbacks);
            partialConfidencePenalty = integer(map, "derivatives_score_config.derivatives_partial_confidence_penalty", 15, fallbacks);
            staleConfidencePenalty = integer(map, "derivatives_score_config.derivatives_stale_confidence_penalty", 30, fallbacks);
            scoreCap = boundedInteger(map, "derivatives_score_config.derivatives_score_cap", 20, 1, 50, fallbacks);
            trendScoreCap = boundedInteger(map, "derivatives_score_config.derivatives_trend_score_cap", 5, 1, 20, fallbacks);
            minimumDataQualityScore = boundedInteger(map,
                    "derivatives_decision_config.derivatives_min_data_quality_score", 60, 1, 100, fallbacks);
            highRiskPlanDowngrade = bool(map, "derivatives_decision_config.derivatives_high_risk_plan_downgrade", true, fallbacks);
            monitorRefreshSeconds = boundedInteger(map, "derivatives_monitor_config.refresh_seconds",
                    60, 15, 3600, fallbacks);
            oiCollapseThreshold = decimal(map, "hot_reset_config.oi_collapse_change_ratio_threshold", "-0.30", fallbacks);
            fallbackReasons = List.copyOf(fallbacks);
        }

        private static Config load(RuleConfigService service) {
            Map<String, RuleConfigDO> map = Map.of();
            List<String> fallbacks = new ArrayList<>();
            try {
                Map<String, RuleConfigDO> loaded = service == null ? null : service.getRuleConfigMap();
                if (loaded != null) map = loaded;
            } catch (RuntimeException failure) {
                fallbacks.add("RULE_CONFIG_READ_FAILED");
            }
            return new Config(map, fallbacks);
        }

        private static BigDecimal decimal(Map<String, RuleConfigDO> map, String key, String fallback,
                                          List<String> reasons) {
            String raw = raw(map, key);
            if (raw != null) {
                try {
                    return new BigDecimal(raw);
                } catch (RuntimeException ignored) {
                    reasons.add("RULE_CONFIG_INVALID:" + key);
                }
            } else {
                reasons.add("RULE_CONFIG_FALLBACK:" + key);
            }
            return new BigDecimal(fallback);
        }

        private static int integer(Map<String, RuleConfigDO> map, String key, int fallback, List<String> reasons) {
            String raw = raw(map, key);
            if (raw != null) {
                try {
                    return Integer.parseInt(raw);
                } catch (RuntimeException ignored) {
                    reasons.add("RULE_CONFIG_INVALID:" + key);
                }
            } else {
                reasons.add("RULE_CONFIG_FALLBACK:" + key);
            }
            return fallback;
        }

        private static int boundedInteger(Map<String, RuleConfigDO> map, String key, int fallback,
                                          int minimum, int maximum, List<String> reasons) {
            int value = integer(map, key, fallback, reasons);
            if (value < minimum || value > maximum) {
                reasons.add("RULE_CONFIG_OUT_OF_RANGE:" + key);
                return fallback;
            }
            return value;
        }

        private static BigDecimal boundedDecimal(Map<String, RuleConfigDO> map, String key, String fallback,
                                                 BigDecimal minimumExclusive, BigDecimal maximumInclusive,
                                                 List<String> reasons) {
            BigDecimal value = decimal(map, key, fallback, reasons);
            if (value.compareTo(minimumExclusive) <= 0 || value.compareTo(maximumInclusive) > 0) {
                reasons.add("RULE_CONFIG_OUT_OF_RANGE:" + key);
                return new BigDecimal(fallback);
            }
            return value;
        }

        private static boolean bool(Map<String, RuleConfigDO> map, String key, boolean fallback,
                                    List<String> reasons) {
            String raw = raw(map, key);
            if (raw != null && ("true".equalsIgnoreCase(raw) || "false".equalsIgnoreCase(raw))) {
                return Boolean.parseBoolean(raw);
            }
            reasons.add((raw == null ? "RULE_CONFIG_FALLBACK:" : "RULE_CONFIG_INVALID:") + key);
            return fallback;
        }

        private static String raw(Map<String, RuleConfigDO> map, String key) {
            RuleConfigDO config = map == null ? null : map.get(key);
            if (config == null || config.getRuleValue() == null || config.getRuleValue().isBlank()) return null;
            return config.getRuleValue().trim();
        }
    }
}
