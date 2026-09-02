package org.example.trademodel.derivatives;

import org.example.trademodel.enums.AssetStateEnum;
import org.example.trademodel.entity.RuleConfigDO;
import org.example.trademodel.providercall.ProviderDatasetType;
import org.example.trademodel.providercall.SnapshotFreshnessStatus;
import org.example.trademodel.providercall.UnifiedSourceStatus;
import org.example.trademodel.providercall.snapshot.DerivativesRiskSnapshot;
import org.example.trademodel.service.RuleConfigService;
import org.example.trademodel.service.support.V41DecisionContractPolicy;
import org.example.trademodel.vo.DecisionBundleVO;
import org.example.trademodel.vo.EvidenceItemVO;
import org.example.trademodel.vo.ExecutionPlanVO;
import org.example.trademodel.vo.ScoreItemVO;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class DerivativesBusinessIntegrationServiceTest {
    private static final String OI = ProviderDatasetType.COINGLASS_OPEN_INTEREST.name();
    private static final String FUNDING = ProviderDatasetType.COINGLASS_FUNDING.name();
    private static final String LIQUIDATION = ProviderDatasetType.COINGLASS_LIQUIDATION.name();
    private static final String LONG_SHORT = ProviderDatasetType.COINGLASS_LONG_SHORT_RATIO.name();
    private final DerivativesBusinessIntegrationService service = new DerivativesBusinessIntegrationService(null);

    @Test
    void bullishPriceAndOiExpansionConfirmsBullishRuleDirection() {
        DerivativesBusinessAssessment result = service.evaluate(input("BULLISH", bd("110"), bd("100"),
                all("BULLISH"), complete(bd("0.06"), bd("0.0001"), bd("1.0"), bd("1000"), bd("1000"), bd("0.20")), false));
        assertThat(types(result)).contains(DerivativesEvidenceType.OPEN_INTEREST_PRICE_CONFIRMATION);
        assertThat(result.baseDirection()).isEqualTo("BULLISH");
        assertThat(result.scoreDeltas().get(DerivativesBusinessIntegrationService.CAPITAL_SCORE)).isPositive();
    }

    @Test
    void bearishPriceAndOiExpansionConfirmsBearishRuleDirection() {
        DerivativesBusinessAssessment result = service.evaluate(input("BEARISH", bd("90"), bd("100"),
                all("BEARISH"), complete(bd("0.06"), bd("-0.0001"), bd("1.0"), bd("1000"), bd("1000"), bd("0.20")), false));
        assertThat(result.evidence()).anyMatch(item -> item.evidenceType() == DerivativesEvidenceType.OPEN_INTEREST_PRICE_CONFIRMATION
                && "BEARISH".equals(item.direction()));
    }

    @Test
    void priceAndOiWithoutVolumeDoesNotBecomeStrongConfirmation() {
        DerivativesBusinessInput input = new DerivativesBusinessInput("BTCUSDT", "BULLISH",
                bd("110"), bd("100"), false, all("BULLISH"), true, 90, true, false,
                false, null, complete(bd("0.06"), bd("0.0001"), bd("1.0"), bd("1000"),
                bd("1000"), bd("0.20")), "trace-biz1", "analysis-biz1", "v1.0");

        DerivativesBusinessAssessment result = service.evaluate(input);

        assertThat(types(result)).doesNotContain(DerivativesEvidenceType.OPEN_INTEREST_PRICE_CONFIRMATION);
        assertThat(result.reasonCodes()).contains("PRICE_OI_ALIGNED_VOLUME_UNCONFIRMED");
    }

    @Test
    void priceUpAndOiDownIsShortCoveringNotStrongBullConfirmation() {
        DerivativesBusinessAssessment result = service.evaluate(input("BULLISH", bd("110"), bd("100"),
                all("BULLISH"), complete(bd("-0.06"), bd("0.0001"), bd("1.0"), bd("1000"), bd("1000"), bd("0.20")), false));
        assertThat(result.reasonCodes()).contains("PRICE_UP_OI_DOWN_SHORT_COVERING");
        assertThat(result.scoreDeltas().get(DerivativesBusinessIntegrationService.CAPITAL_SCORE)).isNegative();
    }

    @Test
    void priceDownAndOiDownIsDeleveragingNotAutomaticBearishEntry() {
        DerivativesBusinessAssessment result = service.evaluate(input("BEARISH", bd("90"), bd("100"),
                all("BEARISH"), complete(bd("-0.06"), bd("-0.0001"), bd("1.0"), bd("1000"), bd("1000"), bd("0.20")), false));
        assertThat(result.reasonCodes()).contains("PRICE_DOWN_OI_DOWN_DELEVERAGING");
        assertThat(result.confirmEligible()).isFalse();
    }

    @Test
    void extremePositiveFundingRaisesRiskWithoutFlippingDirection() {
        DerivativesBusinessAssessment result = service.evaluate(input("BULLISH", bd("110"), bd("100"),
                all("BULLISH"), complete(bd("0.06"), bd("0.001"), bd("1.4"), bd("1000"), bd("1000"), bd("0.20")), false));
        DecisionBundleVO decision = decision("BULLISH");
        service.applyDecisionAdjustments(decision, result);
        assertThat(decision.getMarketBiasHierarchy()).isEqualTo("BULLISH");
        assertThat(decision.getRiskLevel()).isEqualTo("HIGH");
    }

    @Test
    void extremeNegativeFundingRaisesSqueezeRiskWithoutFlippingDirection() {
        DerivativesBusinessAssessment result = service.evaluate(input("BEARISH", bd("90"), bd("100"),
                all("BEARISH"), complete(bd("0.06"), bd("-0.001"), bd("0.6"), bd("1000"), bd("1000"), bd("0.20")), false));
        DecisionBundleVO decision = decision("BEARISH");
        service.applyDecisionAdjustments(decision, result);
        assertThat(decision.getMarketBiasHierarchy()).isEqualTo("BEARISH");
        assertThat(result.reasonCodes()).contains("FUNDING_RISK_EXTREME_NEGATIVE");
    }

    @Test
    void longCrowdingDoesNotCreateAutomaticShortSignal() {
        DerivativesBusinessAssessment result = service.evaluate(input("BULLISH", bd("110"), bd("100"),
                all("BULLISH"), complete(bd("0.03"), bd("0.0001"), bd("1.4"), bd("1000"), bd("1000"), bd("0.20")), false));
        assertThat(result.baseDirection()).isEqualTo("BULLISH");
        assertThat(result.evidence()).anyMatch(item -> item.evidenceType() == DerivativesEvidenceType.LONG_CROWDING
                && "NEUTRAL".equals(item.direction()));
    }

    @Test
    void shortCrowdingDoesNotCreateAutomaticLongSignal() {
        DerivativesBusinessAssessment result = service.evaluate(input("BEARISH", bd("90"), bd("100"),
                all("BEARISH"), complete(bd("0.03"), bd("-0.0001"), bd("0.6"), bd("1000"), bd("1000"), bd("0.20")), false));
        assertThat(result.baseDirection()).isEqualTo("BEARISH");
        assertThat(result.evidence()).anyMatch(item -> item.evidenceType() == DerivativesEvidenceType.SHORT_CROWDING);
    }

    @Test
    void liquidationSpikeRaisesRiskAndCanTriggerHotResetCandidate() {
        DerivativesBusinessAssessment result = service.evaluate(input("BULLISH", bd("110"), bd("100"),
                all("BULLISH"), complete(bd("0.06"), bd("0.0001"), bd("1.0"), bd("4000000"), bd("500000"), bd("0.20")), false));
        assertThat(result.hotResetCandidate()).isTrue();
        assertThat(result.riskAdjustment()).isEqualTo("HIGH");
        assertThat(result.pushMode()).isEqualTo("WARNING_PUSH");
    }

    @Test
    void exchangeConcentrationRaisesDataAndExecutionRisk() {
        DerivativesBusinessAssessment result = service.evaluate(input("BULLISH", bd("110"), bd("100"),
                all("BULLISH"), complete(bd("0.06"), bd("0.0001"), bd("1.0"), bd("1000"), bd("1000"), bd("0.90")), false));
        assertThat(types(result)).contains(DerivativesEvidenceType.EXCHANGE_CONCENTRATION_HIGH);
        assertThat(result.scoreDeltas().get(DerivativesBusinessIntegrationService.LIQUIDITY_SCORE)).isNegative();
        assertThat(result.hotResetCandidate()).isTrue();
    }

    @Test
    void exchangeConcentrationAtInclusiveBoundaryTriggersRisk() {
        DerivativesBusinessAssessment result = service.evaluate(input("BULLISH", bd("110"), bd("100"),
                all("BULLISH"), complete(bd("0.06"), bd("0.0001"), bd("1.0"),
                        bd("1000"), bd("1000"), bd("0.70")), false));

        assertThat(types(result)).contains(DerivativesEvidenceType.EXCHANGE_CONCENTRATION_HIGH);
    }

    @Test
    void exchangeConcentrationBelowBoundaryDoesNotTriggerRisk() {
        DerivativesBusinessAssessment result = service.evaluate(input("BULLISH", bd("110"), bd("100"),
                all("BULLISH"), complete(bd("0.06"), bd("0.0001"), bd("1.0"),
                        bd("1000"), bd("1000"), bd("0.60")), false));

        assertThat(types(result)).doesNotContain(DerivativesEvidenceType.EXCHANGE_CONCENTRATION_HIGH);
        assertThat(result.hotResetCandidate()).isFalse();
    }

    @Test
    void percentageStyleConcentrationConfigFailsClosedToRatioDefault() {
        RuleConfigService configService = mock(RuleConfigService.class);
        String key = "derivatives_evidence_config.exchange_concentration_high";
        when(configService.getRuleConfigMap()).thenReturn(Map.of(key, config(key, "90")));
        DerivativesBusinessIntegrationService configured = new DerivativesBusinessIntegrationService(configService);

        DerivativesBusinessAssessment result = configured.evaluate(input("BULLISH", bd("110"), bd("100"),
                all("BULLISH"), complete(bd("0.06"), bd("0.0001"), bd("1.0"),
                        bd("1000"), bd("1000"), bd("0.80")), false));

        assertThat(types(result)).contains(DerivativesEvidenceType.EXCHANGE_CONCENTRATION_HIGH);
        assertThat(result.configFallbackReasons()).contains("RULE_CONFIG_OUT_OF_RANGE:" + key);
    }

    @Test
    void zeroNegativeAndGreaterThanOneConcentrationConfigFailClosed() {
        String key = "derivatives_evidence_config.exchange_concentration_high";

        for (String invalidValue : List.of("0", "-0.10", "1.01")) {
            RuleConfigService configService = mock(RuleConfigService.class);
            when(configService.getRuleConfigMap()).thenReturn(Map.of(key, config(key, invalidValue)));
            DerivativesBusinessIntegrationService configured =
                    new DerivativesBusinessIntegrationService(configService);

            DerivativesBusinessAssessment result = configured.evaluate(input("BULLISH", bd("110"), bd("100"),
                    all("BULLISH"), complete(bd("0.06"), bd("0.0001"), bd("1.0"),
                            bd("1000"), bd("1000"), bd("0.80")), false));

            assertThat(types(result)).contains(DerivativesEvidenceType.EXCHANGE_CONCENTRATION_HIGH);
            assertThat(result.configFallbackReasons()).contains("RULE_CONFIG_OUT_OF_RANGE:" + key);
        }
    }

    @Test
    void derivativesPartialLowersConfidence() {
        DerivativesRiskSnapshot partial = snapshot(bd("0.06"), bd("0.0001"), null, null, null, bd("0.20"),
                UnifiedSourceStatus.DEGRADED, SnapshotFreshnessStatus.FRESH, "PARTIAL",
                List.of(OI, FUNDING), List.of(LIQUIDATION, LONG_SHORT), List.of(LIQUIDATION, LONG_SHORT), Instant.now());
        DerivativesBusinessAssessment result = service.evaluate(input("BULLISH", bd("110"), bd("100"), all("BULLISH"), partial, false));
        assertThat(result.confidenceAdjustment()).isNegative();
        assertThat(result.dataQualityDiscount()).isPositive();
    }

    @Test
    void derivativesStaleBlocksConfirmPush() {
        DerivativesRiskSnapshot stale = snapshot(bd("0.06"), bd("0.0001"), bd("1"), bd("1000"), bd("1000"), bd("0.20"),
                UnifiedSourceStatus.STALE, SnapshotFreshnessStatus.STALE_READABLE, "COMPLETE",
                List.of(OI, FUNDING, LIQUIDATION, LONG_SHORT), List.of(), List.of(), Instant.now().minusSeconds(600));
        DerivativesBusinessAssessment result = service.evaluate(input("BULLISH", bd("110"), bd("100"), all("BULLISH"), stale, true));
        assertThat(result.confirmEligible()).isFalse();
        assertThat(result.pushMode()).isEqualTo("WARNING_PUSH");
        assertThat(result.pushRecheckAllowed()).isFalse();
    }

    @Test
    void staleReadableProviderEvidenceUsesFrozenStaleVocabularyAndRealAge() {
        DerivativesRiskSnapshot stale = snapshot(bd("0.06"), bd("0.0001"), bd("1"), bd("1000"),
                bd("1000"), bd("0.20"), UnifiedSourceStatus.READY,
                SnapshotFreshnessStatus.STALE_READABLE, "COMPLETE",
                List.of(OI, FUNDING, LIQUIDATION, LONG_SHORT), List.of(), List.of(),
                Instant.now().minusSeconds(600));

        DerivativesBusinessAssessment assessment = service.evaluate(
                input("BULLISH", bd("110"), bd("100"), all("BULLISH"), stale, true));
        List<EvidenceItemVO> evidence = service.toEvidenceVos(assessment);

        assertThat(evidence).allSatisfy(item -> {
            assertThat(item.getFreshness()).isEqualTo("STALE");
            assertThat(V41DecisionContractPolicy.evidenceItemContractComplete(
                    item, assessment.analysisId())).isTrue();
        });
        assertThat(evidence).anySatisfy(item -> {
            assertThat(item.getEvidenceType()).isEqualTo(DerivativesEvidenceType.DERIVATIVES_DATA_STALE.name());
            assertThat(new BigDecimal(item.getCurrentValue())).isGreaterThanOrEqualTo(bd("599"));
            assertThat(item.getChangeFromBaseline()).isNotBlank();
            assertThat(item.getSourceReference()).contains("metadata.providerDataAgeSeconds");
        });
    }

    @Test
    void partialEvidenceCarriesActualDatasetCoverageWithoutSyntheticMetricValues() {
        DerivativesRiskSnapshot partial = snapshot(bd("0.06"), bd("0.0001"), null, null, null, null,
                UnifiedSourceStatus.DEGRADED, SnapshotFreshnessStatus.FRESH, "PARTIAL",
                List.of(OI, FUNDING), List.of(LIQUIDATION, LONG_SHORT),
                List.of(LIQUIDATION, LONG_SHORT), Instant.now());

        DerivativesBusinessAssessment assessment = service.evaluate(
                input("BULLISH", bd("110"), bd("100"), all("BULLISH"), partial, false));
        List<EvidenceItemVO> evidence = service.toEvidenceVos(assessment);

        assertThat(evidence).anySatisfy(item -> {
            assertThat(item.getEvidenceType()).isEqualTo(DerivativesEvidenceType.DERIVATIVES_DATA_PARTIAL.name());
            assertThat(item.getFreshness()).isEqualTo("FRESH");
            assertThat(item.getCurrentValue()).isEqualTo("2");
            assertThat(item.getChangeFromBaseline()).isEqualTo("0");
            assertThat(item.getSourceReference()).contains("availableDatasets/minimumDatasetCount");
            assertThat(V41DecisionContractPolicy.evidenceItemContractComplete(
                    item, assessment.analysisId())).isTrue();
        });
    }

    @Test
    void missingOiBlocksConfirmPlanWhenRequired() {
        DerivativesRiskSnapshot missing = snapshot(null, bd("0.0001"), bd("1"), bd("1000"), bd("1000"), bd("0.20"),
                UnifiedSourceStatus.DEGRADED, SnapshotFreshnessStatus.FRESH, "PARTIAL",
                List.of(FUNDING, LIQUIDATION, LONG_SHORT), List.of(OI), List.of(OI), Instant.now());
        DerivativesBusinessAssessment result = service.evaluate(input("BULLISH", bd("110"), bd("100"), all("BULLISH"), missing, true));
        DecisionBundleVO decision = decision("BULLISH");
        service.applyDecisionAdjustments(decision, result);
        assertThat(result.confirmEligible()).isFalse();
        assertThat(result.planMode()).isEqualTo("OBSERVATION");
        assertThat(decision.getIsWorthOpening()).isFalse();
    }

    @Test
    void missingFundingBlocksConfirmPlanWhenRequired() {
        DerivativesRiskSnapshot missing = snapshot(bd("0.06"), null, bd("1"), bd("1000"), bd("1000"), bd("0.20"),
                UnifiedSourceStatus.DEGRADED, SnapshotFreshnessStatus.FRESH, "PARTIAL",
                List.of(OI, LIQUIDATION, LONG_SHORT), List.of(FUNDING), List.of(FUNDING), Instant.now());
        DerivativesBusinessAssessment result = service.evaluate(input("BULLISH", bd("110"), bd("100"), all("BULLISH"), missing, true));
        assertThat(result.confirmEligible()).isFalse();
        assertThat(result.reasonCodes()).contains("DERIVATIVES_REQUIRED:FUNDING");
    }

    @Test
    void missingLiquidationDoesNotBecomeZero() {
        DerivativesRiskSnapshot missing = snapshot(bd("0.06"), bd("0.0001"), bd("1"), null, null, bd("0.20"),
                UnifiedSourceStatus.DEGRADED, SnapshotFreshnessStatus.FRESH, "PARTIAL",
                List.of(OI, FUNDING, LONG_SHORT), List.of(LIQUIDATION), List.of(LIQUIDATION), Instant.now());
        DerivativesBusinessAssessment result = service.evaluate(input("BULLISH", bd("110"), bd("100"), all("BULLISH"), missing, false));
        assertThat(result.evidence()).noneMatch(item -> item.evidenceType() == DerivativesEvidenceType.LONG_LIQUIDATION_SPIKE
                || item.evidenceType() == DerivativesEvidenceType.SHORT_LIQUIDATION_SPIKE);
    }

    @Test
    void allDerivativesUnavailableUsesHonestRuleOnlyDegradedMode() {
        DerivativesBusinessAssessment result = service.evaluate(input("BULLISH", bd("110"), bd("100"), all("BULLISH"), null, false));
        assertThat(types(result)).containsExactly(DerivativesEvidenceType.DERIVATIVES_DATA_UNAVAILABLE);
        assertThat(result.confirmEligible()).isFalse();
        assertThat(result.opportunityState()).isEqualTo(AssetStateEnum.OBSERVING);
    }

    @Test
    void sixAssetsReceiveDistinctAnalysisScopedDerivativesEvidenceIds() {
        List<String> symbols = List.of("BTCUSDT", "ETHUSDT", "SOLUSDT", "BNBUSDT", "XRPUSDT", "DOGEUSDT");

        Set<String> ids = symbols.stream()
                .map(symbol -> service.evaluate(identityInput(symbol, "ana-cycle-" + symbol)))
                .flatMap(assessment -> service.toEvidenceVos(assessment).stream())
                .map(org.example.trademodel.vo.EvidenceItemVO::getEvidenceId)
                .collect(Collectors.toSet());

        assertThat(ids).hasSize(6);
        assertThat(ids).allMatch(id -> id.startsWith("deriv-") && id.length() <= 64);
    }

    @Test
    void allEightScoresParticipateInDecisionAggregation() {
        DerivativesBusinessAssessment result = service.evaluate(input("BULLISH", bd("110"), bd("100"), all("BULLISH"),
                complete(bd("0.06"), bd("0.001"), bd("1.4"), bd("4000000"), bd("500000"), bd("0.90")), false));
        List<ScoreItemVO> scores = scores();
        service.applyScoreAdjustments(scores, result);
        assertThat(result.scoreDeltas().keySet()).containsExactlyInAnyOrder(
                DerivativesBusinessIntegrationService.TREND_SCORE,
                DerivativesBusinessIntegrationService.CAPITAL_SCORE,
                DerivativesBusinessIntegrationService.LEVERAGE_SCORE,
                DerivativesBusinessIntegrationService.LIQUIDITY_SCORE,
                DerivativesBusinessIntegrationService.SENTIMENT_SCORE,
                DerivativesBusinessIntegrationService.EVENT_SCORE,
                DerivativesBusinessIntegrationService.MACRO_SCORE,
                DerivativesBusinessIntegrationService.CREDIBILITY_SCORE);
        assertThat(scores).allMatch(score -> score.getScoreValue() >= 0 && score.getScoreValue() <= 100);
    }

    @Test
    void configuredCapsQualityGateAndMonitorCadenceAreConsumed() {
        RuleConfigService configService = mock(RuleConfigService.class);
        Map<String, RuleConfigDO> config = new LinkedHashMap<>();
        config.put("derivatives_score_config.derivatives_score_cap",
                config("derivatives_score_config.derivatives_score_cap", "3"));
        config.put("derivatives_score_config.derivatives_trend_score_cap",
                config("derivatives_score_config.derivatives_trend_score_cap", "2"));
        config.put("derivatives_decision_config.derivatives_min_data_quality_score",
                config("derivatives_decision_config.derivatives_min_data_quality_score", "95"));
        config.put("derivatives_monitor_config.refresh_seconds",
                config("derivatives_monitor_config.refresh_seconds", "45"));
        when(configService.getRuleConfigMap()).thenReturn(config);
        DerivativesBusinessIntegrationService configured = new DerivativesBusinessIntegrationService(configService);

        DerivativesBusinessAssessment result = configured.evaluate(input("BULLISH", bd("110"), bd("100"),
                all("BULLISH"), complete(bd("0.06"), bd("0.001"), bd("1.4"),
                        bd("4000000"), bd("500000"), bd("0.90")), true));

        assertThat(result.confirmEligible()).isFalse();
        assertThat(result.scoreDeltas().get(DerivativesBusinessIntegrationService.TREND_SCORE))
                .isBetween(-2.0, 2.0);
        assertThat(result.scoreDeltas()).allSatisfy((score, value) -> {
            if (!DerivativesBusinessIntegrationService.TREND_SCORE.equals(score)) {
                assertThat(value).isBetween(-3.0, 3.0);
            }
        });
        assertThat(configured.monitorRefreshSeconds()).isEqualTo(45);
    }

    @Test
    void derivativeEvidenceCannotOverrideBaseDirection() {
        DerivativesBusinessAssessment result = service.evaluate(input("BULLISH", bd("90"), bd("100"), all("BEARISH"),
                complete(bd("0.08"), bd("-0.001"), bd("0.5"), bd("1000"), bd("1000"), bd("0.20")), false));
        DecisionBundleVO decision = decision("BULLISH");
        service.applyDecisionAdjustments(decision, result);
        assertThat(decision.getMarketBiasHierarchy()).isEqualTo("BULLISH");
    }

    @Test
    void fourTimeframesAreUsedForFormalConvergence() {
        DerivativesBusinessAssessment result = service.evaluate(input("BULLISH", bd("110"), bd("100"),
                Map.of("5m", "BEARISH", "15m", "BULLISH", "1h", "BULLISH", "4h", "BULLISH"),
                complete(bd("0.06"), bd("0.0001"), bd("1"), bd("1000"), bd("1000"), bd("0.20")), false));
        assertThat(result.opportunityState()).isEqualTo(AssetStateEnum.WAITING_TRIGGER);
    }

    @Test
    void onlyOneOrTwoTimeframesCannotProduceConfirmPlan() {
        DerivativesBusinessAssessment result = service.evaluate(input("BULLISH", bd("110"), bd("100"),
                Map.of("1h", "BULLISH", "4h", "BULLISH"),
                complete(bd("0.06"), bd("0.0001"), bd("1"), bd("1000"), bd("1000"), bd("0.20")), true));
        assertThat(result.confirmEligible()).isFalse();
        assertThat(result.opportunityState()).isEqualTo(AssetStateEnum.CANDIDATE);
    }

    @Test
    void candidateCanBeDiscoveredFromPool() {
        DerivativesRiskSnapshot partial = snapshot(bd("0.03"), bd("0.0001"), null, null, null, bd("0.20"),
                UnifiedSourceStatus.DEGRADED, SnapshotFreshnessStatus.FRESH, "PARTIAL",
                List.of(OI, FUNDING), List.of(LIQUIDATION, LONG_SHORT), List.of(), Instant.now());
        assertThat(service.evaluate(input("BULLISH", bd("110"), bd("100"), all("BULLISH"), partial, false)))
                .extracting(DerivativesBusinessAssessment::opportunityState).isEqualTo(AssetStateEnum.CANDIDATE);
    }

    @Test
    void candidateCanUpgradeToWaitingTrigger() {
        assertThat(service.evaluate(input("BULLISH", bd("110"), bd("100"), all("BULLISH"),
                complete(bd("0.06"), bd("0.0001"), bd("1"), bd("1000"), bd("1000"), bd("0.20")), false)))
                .extracting(DerivativesBusinessAssessment::opportunityState).isEqualTo(AssetStateEnum.WAITING_TRIGGER);
    }

    @Test
    void waitingTriggerCanUpgradeToTriggeredOnlyWithCompletePlan() {
        DerivativesRiskSnapshot snapshot = complete(bd("0.06"), bd("0.0001"), bd("1"), bd("1000"), bd("1000"), bd("0.20"));
        assertThat(service.evaluate(input("BULLISH", bd("110"), bd("100"), all("BULLISH"), snapshot, false)).opportunityState())
                .isEqualTo(AssetStateEnum.WAITING_TRIGGER);
        assertThat(service.evaluate(input("BULLISH", bd("110"), bd("100"), all("BULLISH"), snapshot, true)).opportunityState())
                .isEqualTo(AssetStateEnum.TRIGGERED);
    }

    @Test
    void highRiskAndConfusedCannotProduceConfirmPush() {
        DerivativesRiskSnapshot risky = complete(bd("0.06"), bd("0.001"), bd("1.5"), bd("1000"), bd("1000"), bd("0.20"));
        DerivativesBusinessAssessment highRisk = service.evaluate(input("BULLISH", bd("110"), bd("100"), all("BULLISH"), risky, true));
        DerivativesBusinessAssessment confused = service.evaluate(input("BULLISH", bd("110"), bd("100"), all("BULLISH"),
                complete(bd("0.06"), bd("0.0001"), bd("1"), bd("1000"), bd("1000"), bd("0.20")), true,
                AssetStateEnum.CONFUSED));
        assertThat(highRisk.pushMode()).isNotEqualTo("CONFIRM_PUSH");
        assertThat(confused.pushMode()).isNotEqualTo("CONFIRM_PUSH");
    }

    @Test
    void executionPlanUsesOhlcvForEntryStopAndTargets() {
        DerivativesBusinessAssessment result = service.evaluate(input("BULLISH", bd("110"), bd("100"), all("BULLISH"),
                complete(bd("0.06"), bd("0.0001"), bd("1"), bd("1000"), bd("1000"), bd("0.20")), true));
        ExecutionPlanVO plan = plan();
        service.applyPlanAdjustments(plan, result);
        assertThat(plan.getEntryZone()).isEqualTo("100-105");
        assertThat(plan.getStopLoss()).isEqualTo("95");
        assertThat(plan.getTakeProfitRules()).isEqualTo("110,120");
    }

    @Test
    void fundingRiskReducesLeverageAndPositionSuggestion() {
        DerivativesBusinessAssessment result = service.evaluate(input("BULLISH", bd("110"), bd("100"), all("BULLISH"),
                complete(bd("0.06"), bd("0.001"), bd("1.5"), bd("1000"), bd("1000"), bd("0.20")), true));
        ExecutionPlanVO plan = plan();
        service.applyPlanAdjustments(plan, result);
        assertThat(plan.getLeverageSuggestion()).contains("低杠杆");
        assertThat(plan.getPositionSuggestion()).contains("降低风险暴露");
        assertThat(plan.getNotExecutable()).isTrue();
    }

    @Test
    void strongDerivativesReversalRequiresManualConfirmation() {
        DerivativesBusinessAssessment result = service.evaluate(input("BULLISH", bd("110"), bd("100"), all("BULLISH"),
                complete(bd("-0.08"), bd("0.001"), bd("1.5"), bd("4000000"), bd("1000"), bd("0.20")), true));
        ExecutionPlanVO plan = plan();
        service.applyPlanAdjustments(plan, result);
        assertThat(plan.getManualReviewRequired()).isTrue();
        assertThat(plan.getNotExecutable()).isTrue();
    }

    @Test
    void positionLogicCanBecomeWeakenedFromDerivativesDivergence() {
        DerivativesBusinessInput positionInput = new DerivativesBusinessInput("BTCUSDT", "LONG",
                bd("110"), bd("100"), false, Map.of(), true, 90, true, false, true, null,
                complete(bd("-0.06"), bd("0.0001"), bd("1"), bd("1000"), bd("1000"), bd("0.20")),
                "trace-position", "analysis-position", "v1.0");
        DerivativesBusinessAssessment result = service.evaluate(positionInput);
        assertThat(result.positionOpen()).isTrue();
        assertThat(result.needsRevalidation()).isTrue();
        assertThat(result.reasonCodes()).contains("PRICE_UP_OI_DOWN_SHORT_COVERING");
    }

    @Test
    void deterministicEndToEndDerivativesBusinessIntegration() {
        DerivativesBusinessInput input = input("BULLISH", bd("110"), bd("100"), all("BULLISH"),
                complete(bd("0.06"), bd("0.0001"), bd("1"), bd("1000"), bd("1000"), bd("0.20")), true);
        DerivativesBusinessAssessment first = service.evaluate(input);
        DerivativesBusinessAssessment second = service.evaluate(input);
        DecisionBundleVO decision = decision("BULLISH");
        ExecutionPlanVO plan = plan();
        service.applyScoreAdjustments(scores(), first);
        service.applyDecisionAdjustments(decision, first);
        service.applyOpportunityState(decision, first);
        service.applyPlanAdjustments(plan, first);
        assertThat(first.evidence()).usingRecursiveComparison().isEqualTo(second.evidence());
        assertThat(first.traceId()).isEqualTo("trace-biz1");
        assertThat(first.analysisId()).isEqualTo("analysis-biz1");
        assertThat(first.ruleVersion()).isEqualTo("v1.0");
        assertThat(decision.getMarketBiasHierarchy()).isEqualTo("BULLISH");
        assertThat(decision.getAssetState()).isEqualTo(AssetStateEnum.TRIGGERED);
        assertThat(plan.getNotUserPositionCreation()).isTrue();
        assertThat(plan.getNotOrderExecution()).isTrue();
        assertThat(plan.getNotAutoTrading()).isTrue();
    }

    private static DerivativesBusinessInput input(String direction, BigDecimal current, BigDecimal comparison,
                                                  Map<String, String> timeframes, DerivativesRiskSnapshot snapshot,
                                                  boolean planComplete) {
        return input(direction, current, comparison, timeframes, snapshot, planComplete, null);
    }

    private static DerivativesBusinessInput input(String direction, BigDecimal current, BigDecimal comparison,
                                                  Map<String, String> timeframes, DerivativesRiskSnapshot snapshot,
                                                  boolean planComplete, AssetStateEnum state) {
        return new DerivativesBusinessInput("BTCUSDT", direction, current, comparison, true, timeframes, true,
                90, true, planComplete, false, state, snapshot, "trace-biz1", "analysis-biz1", "v1.0");
    }

    private static DerivativesBusinessInput identityInput(String symbol, String analysisId) {
        return new DerivativesBusinessInput(symbol, "NEUTRAL", bd("100"), bd("100"), true,
                all("NEUTRAL"), true, 90, true, false, false, null, null,
                "trace-" + symbol, analysisId, "v1.0");
    }

    private static Map<String, String> all(String direction) {
        return Map.of("5m", direction, "15m", direction, "1h", direction, "4h", direction);
    }

    private static DerivativesRiskSnapshot complete(BigDecimal oiChange, BigDecimal funding, BigDecimal ratio,
                                                    BigDecimal longLiquidation, BigDecimal shortLiquidation,
                                                    BigDecimal concentration) {
        return snapshot(oiChange, funding, ratio, longLiquidation, shortLiquidation, concentration,
                UnifiedSourceStatus.READY, SnapshotFreshnessStatus.FRESH, "COMPLETE",
                List.of(OI, FUNDING, LIQUIDATION, LONG_SHORT), List.of(), List.of(), Instant.now());
    }

    private static DerivativesRiskSnapshot snapshot(BigDecimal oiChange, BigDecimal funding, BigDecimal ratio,
                                                    BigDecimal longLiquidation, BigDecimal shortLiquidation,
                                                    BigDecimal concentration, UnifiedSourceStatus sourceStatus,
                                                    SnapshotFreshnessStatus freshness, String availability,
                                                    List<String> available, List<String> missing,
                                                    List<String> degraded, Instant providerDataTime) {
        Instant fetch = Instant.now();
        return new DerivativesRiskSnapshot("BTCUSDT", "COINGLASS_V4", providerDataTime, fetch,
                fetch.plusSeconds(60), bd("100000000"), null, oiChange, oiChange, oiChange,
                funding, null, ratio, "GLOBAL_ACCOUNT", null, longLiquidation, longLiquidation, longLiquidation,
                null, shortLiquidation, shortLiquidation, shortLiquidation, null, concentration,
                available, missing, degraded, sourceStatus, freshness, availability, List.of(), "trace-biz1",
                Map.of("openInterestChange5m", "COINGLASS_V4"), null);
    }

    private static List<DerivativesEvidenceType> types(DerivativesBusinessAssessment assessment) {
        return assessment.evidence().stream().map(DerivativesEvidenceItem::evidenceType).toList();
    }

    private static List<ScoreItemVO> scores() {
        List<ScoreItemVO> scores = new ArrayList<>();
        for (String type : List.of(DerivativesBusinessIntegrationService.TREND_SCORE,
                DerivativesBusinessIntegrationService.CAPITAL_SCORE,
                DerivativesBusinessIntegrationService.LEVERAGE_SCORE,
                DerivativesBusinessIntegrationService.LIQUIDITY_SCORE,
                DerivativesBusinessIntegrationService.SENTIMENT_SCORE,
                DerivativesBusinessIntegrationService.EVENT_SCORE,
                DerivativesBusinessIntegrationService.MACRO_SCORE,
                DerivativesBusinessIntegrationService.CREDIBILITY_SCORE)) {
            ScoreItemVO score = new ScoreItemVO();
            score.setScoreType(type);
            score.setScoreValue(50.0);
            scores.add(score);
        }
        return scores;
    }

    private static DecisionBundleVO decision(String direction) {
        DecisionBundleVO decision = new DecisionBundleVO();
        decision.setMarketBiasHierarchy(direction);
        decision.setConfidenceLevel("HIGH");
        decision.setRiskLevel("LOW");
        decision.setAiPlanMode("CONFIRM");
        decision.setIsWorthOpening(true);
        decision.setAssetState(AssetStateEnum.CANDIDATE);
        return decision;
    }

    private static ExecutionPlanVO plan() {
        ExecutionPlanVO plan = new ExecutionPlanVO();
        plan.setEntryZone("100-105");
        plan.setStopLoss("95");
        plan.setTakeProfitRules("110,120");
        plan.setSourceGateComplete(true);
        plan.setNotExecutable(true);
        plan.setNotUserPositionCreation(true);
        plan.setNotOrderExecution(true);
        plan.setNotAutoTrading(true);
        return plan;
    }

    private static BigDecimal bd(String value) {
        return new BigDecimal(value);
    }

    private static RuleConfigDO config(String key, String value) {
        RuleConfigDO config = new RuleConfigDO();
        config.setRuleKey(key);
        config.setRuleValue(value);
        config.setEnabled(true);
        return config;
    }
}
