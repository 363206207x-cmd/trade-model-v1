package org.example.trademodel.service;

import org.example.trademodel.ai.AiOrchestrationMode;
import org.example.trademodel.ai.AiOrchestratorResult;
import org.example.trademodel.enums.AiConflictLevelEnum;
import org.example.trademodel.enums.AssetStateEnum;
import org.example.trademodel.service.support.ExternalContextPolicy;
import org.example.trademodel.vo.DecisionBundleVO;
import org.example.trademodel.vo.EventImpactInputVO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DecisionEngineServiceTest {

    @Mock
    private RealMarketDataFetcherService marketDataFetcher;
    @Mock
    private AiConflictResolverService aiConflictResolverService;
    @Mock
    private ConfusedStateService confusedStateService;
    @Mock
    private AssetStateService assetStateService;
    @Mock
    private RuleConfigService ruleConfigService;
    @Mock
    private AiDecisionOrchestratorService aiDecisionOrchestratorService;

    private DecisionEngineService service;

    @BeforeEach
    void setUp() {
        service = new DecisionEngineService(
                marketDataFetcher,
                aiConflictResolverService,
                confusedStateService,
                assetStateService,
                ruleConfigService
        );
        when(ruleConfigService.getRuleConfigMap()).thenReturn(Collections.emptyMap());
        when(assetStateService.buildSnapshotAtDecision(
                anyString(), anyString(), any(AssetStateEnum.class), any(AssetStateEnum.class),
                anyInt(), anyInt(), anyBoolean(), anyBoolean()))
                .thenAnswer(invocation -> {
                    AssetStateEnum previous = invocation.getArgument(2);
                    AssetStateEnum next = invocation.getArgument(3);
                    return "{\"previousState\":\"" + previous.name() + "\",\"nextState\":\""
                            + next.name() + "\",\"state\":\"" + next.name() + "\"}";
                });
        lenient().when(aiConflictResolverService.resolve(any(DecisionContext.class)))
                .thenReturn(new AiConflictResult(
                        AiConflictLevelEnum.LEVEL_1_CONSISTENT,
                        "BULLISH",
                        "HIGH",
                        "CONFIRM",
                        20
                ));
        lenient().when(confusedStateService.calculateConfused(anyString(), any(DecisionContext.class)))
                .thenReturn(new ConfusedResult(20, false, false, "none"));
        when(marketDataFetcher.fetchKlines(anyString(), anyString(), any(Integer.class)))
                .thenAnswer(invocation -> "1m".equals(invocation.getArgument(1))
                        ? bullishKlines()
                        : bullishKlines());
    }

    @Test
    void makeDecision_forcesWorthOpeningFalseWhenDataQualityBelow60_andKeepsReasonPresent() {
        DecisionBundleVO decision = service.makeDecision("BTCUSDT", "1m", "analysis-1", 59, 65);

        assertThat(decision.getIsWorthOpening()).isFalse();
        assertThat(decision.getReviewReasons()).contains("DATA_QUALITY_INSUFFICIENT");
    }

    @Test
    void makeDecision_dataQualityAtOrAbove60_doesNotChangeExistingWorthOpeningOutcome() {
        DecisionBundleVO baseline = service.makeDecision("BTCUSDT", "1m", "analysis-2", null, 65);
        DecisionBundleVO withGateBoundaryScore = service.makeDecision("BTCUSDT", "1m", "analysis-3", 60, 65);

        assertThat(withGateBoundaryScore.getIsWorthOpening()).isEqualTo(baseline.getIsWorthOpening());
    }

    @Test
    void makeDecision_dataQualityGate_doesNotAffectConfidenceRiskOrMarketBias() {
        DecisionBundleVO normal = service.makeDecision("BTCUSDT", "1m", "analysis-4", 60, 65);
        DecisionBundleVO gated = service.makeDecision("BTCUSDT", "1m", "analysis-5", 59, 65);

        assertThat(gated.getIsWorthOpening()).isFalse();
        assertThat(gated.getConfidenceLevel()).isEqualTo(normal.getConfidenceLevel());
        assertThat(gated.getRiskLevel()).isEqualTo(normal.getRiskLevel());
        assertThat(gated.getMarketBiasHierarchy()).isEqualTo(normal.getMarketBiasHierarchy());
    }

    @Test
    void makeDecision_forcesWorthOpeningFalseWhenTrendStructureScoreBelow50_andKeepsReasonPresent() {
        DecisionBundleVO decision = service.makeDecision("BTCUSDT", "1m", "analysis-6", 60, 49);

        assertThat(decision.getIsWorthOpening()).isFalse();
        assertThat(decision.getReviewReasons()).contains("TREND_STRUCTURE_SCORE_INSUFFICIENT");
    }

    @Test
    void makeDecision_trendStructureGate_runsBeforeDataQualityGateButDqRemainsFinalFallback() {
        DecisionBundleVO trendOnlyGate = service.makeDecision("BTCUSDT", "1m", "analysis-7", 60, 49);
        DecisionBundleVO dqOnlyGate = service.makeDecision("BTCUSDT", "1m", "analysis-8", 59, 65);

        assertThat(trendOnlyGate.getIsWorthOpening()).isFalse();
        assertThat(trendOnlyGate.getReviewReasons()).contains("TREND_STRUCTURE_SCORE_INSUFFICIENT");
        assertThat(dqOnlyGate.getIsWorthOpening()).isFalse();
        assertThat(dqOnlyGate.getReviewReasons()).contains("DATA_QUALITY_INSUFFICIENT");
    }

    @Test
    void makeDecision_gateRuleUnchanged_whenDqTierMovesFrom55To85() {
        DecisionBundleVO dq55 = service.makeDecision("BTCUSDT", "1m", "analysis-9", 55, 65);
        DecisionBundleVO dq85 = service.makeDecision("BTCUSDT", "1m", "analysis-10", 85, 65);

        assertThat(dq55.getIsWorthOpening()).isFalse();
        assertThat(dq55.getReviewReasons()).contains("DATA_QUALITY_INSUFFICIENT");
        assertThat(dq85.getIsWorthOpening()).isTrue();
        assertThat(dq85.getReviewReasons()).doesNotContain("DATA_QUALITY_INSUFFICIENT");
    }

    @Test
    void makeDecision_marketBiasHierarchyAlwaysUsesRuleLayerBaseDirection() {
        when(marketDataFetcher.fetchKlines(anyString(), anyString(), any(Integer.class)))
                .thenReturn(bearishKlines());
        when(aiConflictResolverService.resolve(any(DecisionContext.class)))
                .thenReturn(new AiConflictResult(
                        AiConflictLevelEnum.LEVEL_4_EXTREME_DIVERGENCE,
                        "BEARISH",
                        "LOW",
                        "HIGH",
                        "CONFUSED",
                        90,
                        3,
                        false,
                        90
                ));
        when(confusedStateService.calculateConfused(anyString(), any(DecisionContext.class)))
                .thenReturn(new ConfusedResult(20, "OBSERVING", "OBSERVING",
                        false, false, 0, false, "none", "base"));

        DecisionBundleVO decision = service.makeDecision("BTCUSDT", "1m", "analysis-11", 85, 65);

        assertThat(decision.getMarketBiasHierarchy()).isEqualTo("BEARISH");
        assertThat(decision.getAiRoleResults())
                .contains("\"schemaVersion\":\"v1\"")
                .contains("\"finalMarketBias\":\"BEARISH\"")
                .contains("\"ruleDirectionPreserved\":true");
        assertThat(decision.getAiRoleResults()).doesNotContain("最终裁决");
    }

    @Test
    void makeDecision_aiDisagreementDoesNotDirectlyChangeAssetState() {
        when(aiConflictResolverService.resolve(any(DecisionContext.class)))
                .thenReturn(new AiConflictResult(
                        AiConflictLevelEnum.LEVEL_4_EXTREME_DIVERGENCE,
                        "BULLISH",
                        "LOW",
                        "HIGH",
                        "CONFUSED",
                        95,
                        3,
                        false,
                        95
                ));
        when(confusedStateService.calculateConfused(anyString(), any(DecisionContext.class)))
                .thenReturn(new ConfusedResult(20, "OBSERVING", "OBSERVING",
                        false, false, 0, false, "none", "base"));

        DecisionBundleVO decision = service.makeDecision("BTCUSDT", "1m", "analysis-12", 85, 65);

        assertThat(decision.getAssetState()).isEqualTo(AssetStateEnum.OBSERVING);
        assertThat(decision.getAiPlanMode()).isEqualTo("CONFUSED");
    }

    @Test
    void makeDecision_confusedScore70EntersConfused() {
        when(confusedStateService.calculateConfused(anyString(), any(DecisionContext.class)))
                .thenReturn(new ConfusedResult(70, "OBSERVING", "CONFUSED",
                        true, false, 0, false, "threshold", "enter"));

        DecisionBundleVO decision = service.makeDecision("BTCUSDT", "1m", "analysis-13", 85, 65);

        assertThat(decision.getAssetState()).isEqualTo(AssetStateEnum.CONFUSED);
        assertThat(decision.getConfusedScore()).isEqualTo(70);
        assertThat(decision.isDirectionalPushBlocked()).isFalse();
    }

    @Test
    void makeDecision_confusedScore85BlocksDirectionalPushAndWorthOpening() {
        when(confusedStateService.calculateConfused(anyString(), any(DecisionContext.class)))
                .thenReturn(new ConfusedResult(85, "OBSERVING", "CONFUSED",
                        true, false, 0, true, "threshold", "block"));

        DecisionBundleVO decision = service.makeDecision("BTCUSDT", "1m", "analysis-14", 85, 65);

        assertThat(decision.getAssetState()).isEqualTo(AssetStateEnum.CONFUSED);
        assertThat(decision.isDirectionalPushBlocked()).isTrue();
        assertThat(decision.getDirectionalPushBlockReason()).isEqualTo("CONFUSED_SCORE_BLOCK_THRESHOLD");
        assertThat(decision.getIsWorthOpening()).isFalse();
    }

    @Test
    void makeDecision_secondLowCycleExitsToCoolingNotTriggered() {
        when(confusedStateService.calculateConfused(anyString(), any(DecisionContext.class)))
                .thenReturn(new ConfusedResult(54, "CONFUSED", "COOLING",
                        false, true, 0, false, "low", "exit"));

        DecisionBundleVO decision = service.makeDecision("BTCUSDT", "1m", "analysis-15", 85, 65);

        assertThat(decision.getAssetState()).isEqualTo(AssetStateEnum.COOLING);
        assertThat(decision.getAssetState()).isNotEqualTo(AssetStateEnum.TRIGGERED);
        assertThat(decision.getAssetState()).isNotEqualTo(AssetStateEnum.WAITING_TRIGGER);
    }

    @Test
    void makeDecision_highExternalContextLowersConfidenceWithoutDirectionReversal() {
        EventImpactInputVO external = externalInput(false, "HIGH", ExternalContextPolicy.SOURCE_HEALTH_OK,
                List.of(ExternalContextPolicy.REASON_HIGH_IMPACT_REVIEW));

        DecisionBundleVO decision = service.makeDecision("BTCUSDT", "1m", "analysis-ext-high", 85, 65, external);

        assertThat(decision.getMarketBiasHierarchy()).isEqualTo("BULLISH");
        assertThat(decision.getRiskLevel()).isEqualTo("HIGH");
        assertThat(decision.getConfidenceLevel()).isEqualTo("MEDIUM");
        assertThat(decision.getIsWorthOpening()).isTrue();
        assertThat(decision.getReviewReasons()).contains(ExternalContextPolicy.REASON_HIGH_IMPACT_REVIEW);
    }

    @Test
    void makeDecision_blockingExternalContextForcesWorthOpeningFalse() {
        EventImpactInputVO external = externalInput(true, "HIGH", ExternalContextPolicy.SOURCE_HEALTH_OK,
                List.of(ExternalContextPolicy.REASON_WINDOW_BLOCKED));

        DecisionBundleVO decision = service.makeDecision("BTCUSDT", "1m", "analysis-ext-block", 85, 65, external);

        assertThat(decision.getMarketBiasHierarchy()).isEqualTo("BULLISH");
        assertThat(decision.getRiskLevel()).isEqualTo("HIGH");
        assertThat(decision.getIsWorthOpening()).isFalse();
        assertThat(decision.getAssetState()).isEqualTo(AssetStateEnum.HIGH_RISK);
        assertThat(decision.getAssetStateSnapshot()).contains("\"nextState\":\"HIGH_RISK\"");
        assertThat(decision.getAssetStateSnapshot()).doesNotContain("CANDIDATE");
        assertThat(decision.getReviewReasons()).contains(ExternalContextPolicy.REASON_WINDOW_BLOCKED);
    }

    @Test
    void makeDecision_blockedExternalSourceFailClosesAssetStateBeforeSnapshot() {
        EventImpactInputVO external = externalInput(false, "LOW", ExternalContextPolicy.SOURCE_HEALTH_BLOCKED,
                List.of(ExternalContextPolicy.REASON_MISSING_SOURCE));

        DecisionBundleVO decision = service.makeDecision("BTCUSDT", "1m", "analysis-ext-source-block", 85, 65, external);

        assertThat(decision.getRiskLevel()).isEqualTo("HIGH");
        assertThat(decision.getIsWorthOpening()).isFalse();
        assertThat(decision.getAssetState()).isEqualTo(AssetStateEnum.HIGH_RISK);
        assertThat(decision.getAssetStateSnapshot()).contains("\"nextState\":\"HIGH_RISK\"");
        assertThat(decision.getAssetStateSnapshot()).doesNotContain("CANDIDATE");
        assertThat(decision.getReviewReasons()).contains(ExternalContextPolicy.REASON_MISSING_SOURCE);
    }

    @Test
    void makeDecision_blockingExternalContextPreservesStricterConfusedState() {
        EventImpactInputVO external = externalInput(true, "HIGH", ExternalContextPolicy.SOURCE_HEALTH_OK,
                List.of(ExternalContextPolicy.REASON_WINDOW_BLOCKED));
        when(confusedStateService.calculateConfused(anyString(), any(DecisionContext.class)))
                .thenReturn(new ConfusedResult(85, "OBSERVING", "CONFUSED",
                        true, false, 0, true, "threshold", "block"));

        DecisionBundleVO decision = service.makeDecision("BTCUSDT", "1m", "analysis-ext-confused", 85, 65, external);

        assertThat(decision.getRiskLevel()).isEqualTo("HIGH");
        assertThat(decision.getIsWorthOpening()).isFalse();
        assertThat(decision.getAssetState()).isEqualTo(AssetStateEnum.CONFUSED);
        assertThat(decision.getAssetStateSnapshot()).contains("\"nextState\":\"CONFUSED\"");
    }

    @Test
    void makeDecision_aiChallengeEntersConflictContextWithoutChangingRuleDirection() {
        AiOrchestratorResult review = new AiOrchestratorResult();
        review.setOrchestrationMode(AiOrchestrationMode.AI_ASSISTED);
        review.setGptConsistentWithRule(false);
        review.setGeminiConsistentWithRule(true);
        review.setGrokConsistentWithRule(true);
        review.setConflictContribution(18);
        review.setReasonCodes(List.of("AI_CHALLENGE"));
        when(aiDecisionOrchestratorService.review(any())).thenReturn(review);
        DecisionEngineService serviceWithAi = new DecisionEngineService(
                marketDataFetcher,
                aiConflictResolverService,
                confusedStateService,
                assetStateService,
                ruleConfigService,
                aiDecisionOrchestratorService
        );

        DecisionBundleVO decision = serviceWithAi.makeDecision("BTCUSDT", "1m", "analysis-ai-challenge", 85, 65);

        ArgumentCaptor<DecisionContext> captor = ArgumentCaptor.forClass(DecisionContext.class);
        verify(aiConflictResolverService).resolve(captor.capture());
        DecisionContext context = captor.getValue();
        assertThat(context.isGptConsistentWithRule()).isFalse();
        assertThat(context.getAiProviderConflictContribution()).isEqualTo(18);
        assertThat(context.getAiOrchestrationMode()).isEqualTo("AI_ASSISTED");
        assertThat(decision.getMarketBiasHierarchy()).isEqualTo("BULLISH");
        assertThat(decision.getAiRoleResults())
                .contains("\"finalMarketBias\":\"BULLISH\"")
                .contains("\"ruleDirectionPreserved\":true");
        assertThat(decision.getAiRoleResults()).doesNotContain("Grok advisory");
        assertThat(decision.getAiRoleResults()).doesNotContain("Gemini advisory");
    }

    @Test
    void makeDecision_aiProviderFailureFallsBackWithoutFailingDecision() {
        when(aiDecisionOrchestratorService.review(any())).thenThrow(new IllegalStateException("provider unavailable"));
        DecisionEngineService serviceWithAi = new DecisionEngineService(
                marketDataFetcher,
                aiConflictResolverService,
                confusedStateService,
                assetStateService,
                ruleConfigService,
                aiDecisionOrchestratorService
        );

        DecisionBundleVO decision = serviceWithAi.makeDecision("BTCUSDT", "1m", "analysis-ai-fallback", 85, 65);

        assertThat(decision.getMarketBiasHierarchy()).isEqualTo("BULLISH");
        assertThat(decision.getAiRoleResults()).contains("RULE_ONLY_FALLBACK");
        assertThat(decision.getAiRoleResults()).contains("AI_ORCHESTRATOR_FAILED");
    }

    private static List<String[]> bullishKlines() {
        return List.of(
                new String[]{"0", "100", "110", "95", "108"},
                new String[]{"0", "108", "112", "107", "110"},
                new String[]{"0", "110", "118", "109", "117"}
        );
    }

    private static List<String[]> bearishKlines() {
        return List.of(
                new String[]{"0", "110", "112", "105", "108"},
                new String[]{"0", "108", "109", "101", "104"},
                new String[]{"0", "104", "105", "98", "100"}
        );
    }

    private static EventImpactInputVO externalInput(boolean blocked, String riskLevel, String sourceHealth, List<String> reasons) {
        EventImpactInputVO input = new EventImpactInputVO();
        input.setExternalContextStatus(blocked ? "BLOCKED" : "READY");
        input.setExternalContextBlocked(blocked);
        input.setExternalContextRiskLevel(riskLevel);
        input.setExternalContextSourceHealth(sourceHealth);
        input.setActiveExternalEventCount(1);
        input.setActiveMacroEventCount(0);
        input.setActiveNewsEventCount(1);
        input.setExternalEventIds(List.of("NEWS:news-test"));
        input.setExternalContextReasonCodes(reasons);
        return input;
    }
}
