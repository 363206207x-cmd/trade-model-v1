package org.example.trademodel.service;

import org.example.trademodel.ai.AiOrchestrationMode;
import org.example.trademodel.ai.AiOrchestratorResult;
import org.example.trademodel.ai.AiProviderCallStatus;
import org.example.trademodel.ai.AiProviderName;
import org.example.trademodel.ai.AiProviderReviewResult;
import org.example.trademodel.ai.AiProviderRole;
import org.example.trademodel.ai.AiReviewStance;
import org.example.trademodel.enums.AiConflictLevelEnum;
import org.example.trademodel.enums.AssetStateEnum;
import org.example.trademodel.entity.AnalysisRunDO;
import org.example.trademodel.entity.RuleConfigDO;
import org.example.trademodel.entity.TmPushSnapshotDO;
import org.example.trademodel.enums.RecheckStatusEnum;
import org.example.trademodel.market.client.MarketQuoteClient;
import org.example.trademodel.mapper.AccountRiskSnapshotMapper;
import org.example.trademodel.mapper.PushRecheckLogMapper;
import org.example.trademodel.mapper.PushSnapshotMapper;
import org.example.trademodel.risk.UserPositionRiskAdapter;
import org.example.trademodel.risk.UserPositionRiskResult;
import org.example.trademodel.service.impl.PushRecheckServiceImpl;
import org.example.trademodel.service.support.ExternalContextPolicy;
import org.example.trademodel.service.support.RuleConfigContractService;
import org.example.trademodel.vo.DecisionBundleVO;
import org.example.trademodel.vo.AssetAnalysisVO;
import org.example.trademodel.vo.EventImpactInputVO;
import org.example.trademodel.vo.ExecutionPlanVO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TimeZone;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DecisionEngineServiceTest {

    @Mock
    private DecisionOhlcvSnapshotSource ohlcvSnapshotSource;
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
    @Mock
    private PushSnapshotMapper pushSnapshotMapper;
    @Mock
    private AccountRiskSnapshotMapper accountRiskSnapshotMapper;
    @Mock
    private PushRecheckLogMapper pushRecheckLogMapper;
    @Mock
    private PushRecheckDispatchConfigService pushRecheckDispatchConfigService;
    @Mock
    private UserPositionRiskAdapter userPositionRiskAdapter;
    @Mock
    private MarketQuoteClient marketQuoteClient;
    @Mock
    private RuleConfigContractService ruleConfigContractService;

    private DecisionEngineService service;

    @BeforeEach
    void setUp() {
        service = new DecisionEngineService(
                ohlcvSnapshotSource,
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
        lenient().when(userPositionRiskAdapter.currentRisk())
                .thenReturn(UserPositionRiskResult.noOpenPosition(0));
        lenient().when(ruleConfigContractService.requirePushRecheckThresholds())
                .thenReturn(new RuleConfigContractService.PushRecheckThresholds(
                        new java.math.BigDecimal("0.02"), 70, 85, 60));
        when(ohlcvSnapshotSource.readClosedBars(anyString(), anyString(), anyInt(), anyString()))
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
    void newDecisionPlanValidityUsesInjectedUtcClock() {
        service.setDecisionClock(Clock.fixed(Instant.parse("2026-07-13T11:54:00Z"), ZoneOffset.UTC));

        DecisionBundleVO decision = service.makeDecision("BTCUSDT", "5m", "analysis-offset-time", 85, 65);

        assertThat(decision.getValidFrom()).isEqualTo(OffsetDateTime.parse("2026-07-13T11:54:00Z"));
        assertThat(decision.getExpiresAt()).isEqualTo(OffsetDateTime.parse("2026-07-14T11:54:00Z"));
        assertThat(decision.getPushExpiresAt()).isEqualTo(LocalDateTime.parse("2026-07-14T11:54:00"));
        assertThat(decision.getValidFrom().getOffset()).isEqualTo(ZoneOffset.UTC);
        assertThat(decision.getExpiresAt().getOffset()).isEqualTo(ZoneOffset.UTC);
    }

    @Test
    void pushSnapshotTtlIsTwentyFourHoursAcrossJvmTimezones() {
        Clock fixedClock = Clock.fixed(Instant.parse("2026-07-13T12:00:00Z"), ZoneOffset.UTC);
        service.setDecisionClock(fixedClock);
        PushSnapshotService snapshotService = new PushSnapshotService(pushSnapshotMapper, accountRiskSnapshotMapper);
        snapshotService.setClock(fixedClock);
        List<TmPushSnapshotDO> captured = new ArrayList<>();
        doAnswer(invocation -> {
            captured.add(invocation.getArgument(0));
            return 1;
        }).when(pushSnapshotMapper).insert(any(TmPushSnapshotDO.class));
        TimeZone original = TimeZone.getDefault();

        try {
            for (String zone : List.of("UTC", "Asia/Shanghai", "America/New_York")) {
                TimeZone.setDefault(TimeZone.getTimeZone(zone));
                DecisionBundleVO decision = service.makeDecision(
                        "BTCUSDT", "5m", "analysis-zone-" + zone.replace('/', '-'), 85, 65);
                assertThat(decision.getExpiresAt())
                        .isEqualTo(OffsetDateTime.parse("2026-07-14T12:00:00Z"));
                assertThat(decision.getPushExpiresAt())
                        .isEqualTo(LocalDateTime.parse("2026-07-14T12:00:00"));

                AnalysisRunDO run = new AnalysisRunDO();
                run.setRuleVersion("v-test");
                run.setTraceId("trace-" + zone);
                AssetAnalysisVO analysis = new AssetAnalysisVO();
                analysis.setAnalysisId("analysis-" + zone);
                analysis.setSymbol("BTCUSDT");
                analysis.setTimeframe("5m");
                ExecutionPlanVO plan = new ExecutionPlanVO();
                plan.setEntryZone("100-102");
                plan.setStopLoss("98");
                snapshotService.insertAuthoritativeSnapshot(run, analysis, decision, plan, 10L);
            }
        } finally {
            TimeZone.setDefault(original);
        }

        assertThat(captured).hasSize(3);
        assertThat(captured).allSatisfy(snapshot -> {
            assertThat(snapshot.getExpiresAt()).isEqualTo(LocalDateTime.parse("2026-07-14T12:00:00"));
            assertThat(snapshot.getPushCreateTime()).isEqualTo(LocalDateTime.parse("2026-07-13T12:00:00"));
            assertThat(snapshot.getCreateTime()).isEqualTo(LocalDateTime.parse("2026-07-13T12:00:00"));
        });
        verify(pushSnapshotMapper, times(3)).insert(any(TmPushSnapshotDO.class));

        TmPushSnapshotDO persistedSnapshot = captured.get(0);
        persistedSnapshot.setPushId(501L);
        when(pushSnapshotMapper.selectByPushId(501L)).thenReturn(persistedSnapshot);
        PushRecheckServiceImpl recheckService = new PushRecheckServiceImpl(
                pushSnapshotMapper,
                accountRiskSnapshotMapper,
                pushRecheckLogMapper,
                pushRecheckDispatchConfigService,
                userPositionRiskAdapter,
                org.example.trademodel.testsupport.MarketPriceSnapshotTestSupport.snapshotService(marketQuoteClient),
                ruleConfigContractService);

        recheckService.setClock(Clock.fixed(Instant.parse("2026-07-14T11:59:59Z"), ZoneOffset.UTC));
        RecheckResult before = recheckService.recheck(501L, new java.math.BigDecimal("100"));
        recheckService.setClock(Clock.fixed(Instant.parse("2026-07-14T12:00:00Z"), ZoneOffset.UTC));
        RecheckResult equal = recheckService.recheck(501L, new java.math.BigDecimal("100"));
        recheckService.setClock(Clock.fixed(Instant.parse("2026-07-14T12:00:01Z"), ZoneOffset.UTC));
        RecheckResult after = recheckService.recheck(501L, new java.math.BigDecimal("100"));

        assertThat(before.getRecheckStatus()).isNotEqualTo(RecheckStatusEnum.EXPIRED);
        assertThat(equal.getRecheckStatus()).isEqualTo(RecheckStatusEnum.EXPIRED);
        assertThat(after.getRecheckStatus()).isEqualTo(RecheckStatusEnum.EXPIRED);
    }

    @Test
    void makeDecision_dataQualityAtOrAbove60_doesNotChangeExistingWorthOpeningOutcome() {
        DecisionBundleVO baseline = service.makeDecision("BTCUSDT", "1m", "analysis-2", 85, 65);
        DecisionBundleVO withGateBoundaryScore = service.makeDecision("BTCUSDT", "1m", "analysis-3", 60, 65);

        assertThat(withGateBoundaryScore.getIsWorthOpening()).isEqualTo(baseline.getIsWorthOpening());
    }

    @Test
    void sixAssetsReceiveDistinctDecisionIdsEvenInOneTightCycle() {
        Set<String> decisionIds = List.of("BTCUSDT", "ETHUSDT", "SOLUSDT", "BNBUSDT", "XRPUSDT", "DOGEUSDT")
                .stream()
                .map(symbol -> service.makeDecision(symbol, "5m", "ana-" + symbol, 80, 65).getDecisionId())
                .collect(Collectors.toSet());

        assertThat(decisionIds).hasSize(6).allMatch(id -> id.startsWith("dec-") && id.length() <= 64);
    }

    @Test
    void makeDecision_dataQualityGateDowngradesUserConclusionConsistently() {
        DecisionBundleVO normal = service.makeDecision("BTCUSDT", "1m", "analysis-4", 60, 65);
        DecisionBundleVO gated = service.makeDecision("BTCUSDT", "1m", "analysis-5", 59, 65);

        assertThat(gated.getIsWorthOpening()).isFalse();
        assertThat(gated.getConfidenceLevel()).isEqualTo("LOW");
        assertThat(gated.getRiskLevel()).isEqualTo("HIGH");
        assertThat(gated.getMarketBiasHierarchy()).isEqualTo("WAIT");
        assertThat(gated.getAssetState()).isEqualTo(AssetStateEnum.HIGH_RISK);
        assertThat(gated.getAiPlanMode()).isNull();
        assertThat(normal.getMarketBiasHierarchy()).isEqualTo("STRONG_BULLISH");
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
    void makeDecision_fourHourAndOneHourConflictBlocksWorthOpening() {
        when(ohlcvSnapshotSource.readClosedBars(anyString(), anyString(), anyInt(), anyString()))
                .thenAnswer(invocation -> "1h".equals(invocation.getArgument(1))
                        ? bearishKlines()
                        : bullishKlines());

        DecisionBundleVO decision = service.makeDecision("BTCUSDT", "5m", "analysis-mtf-conflict", 85, 65);

        assertThat(decision.getMarketBiasHierarchy()).isEqualTo("BULLISH");
        assertThat(decision.getMultiTfConvergence()).isEqualTo("WEAK");
        assertThat(decision.getIsWorthOpening()).isFalse();
    }

    @Test
    void makeDecision_eightScoreAdjustmentUsesConfiguredCap() {
        RuleConfigDO cap = new RuleConfigDO();
        cap.setRuleValue("1");
        RuleConfigDO factor = new RuleConfigDO();
        factor.setRuleValue("100");
        when(ruleConfigService.getRuleConfigMap()).thenReturn(Map.of(
                "derivatives_decision_config.eight_score_adjustment_cap", cap,
                "derivatives_decision_config.eight_score_adjustment_factor_percent", factor));

        DecisionBundleVO decision = service.makeDecision("BTCUSDT", "5m", "analysis-score-cap",
                85, 65, null, null, 100);

        assertThat(decision.getConclusionSummary()).contains("八项评分修正 +1");
        assertThat(decision.getMarketBiasHierarchy()).isEqualTo("STRONG_BULLISH");
    }

    @Test
    void makeDecision_marketBiasHierarchyAlwaysUsesRuleLayerBaseDirection() {
        when(ohlcvSnapshotSource.readClosedBars(anyString(), anyString(), anyInt(), anyString()))
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

        assertThat(decision.getMarketBiasHierarchy()).isEqualTo("STRONG_BEARISH");
        assertThat(decision.getAiRoleResults())
                .contains("\"schemaVersion\":\"v1\"")
                .contains("\"finalMarketBias\":\"STRONG_BEARISH\"")
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
        assertThat(decision.getAiPlanMode()).isNull();
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

        assertThat(decision.getMarketBiasHierarchy()).isEqualTo("STRONG_BULLISH");
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

        assertThat(decision.getMarketBiasHierarchy()).isEqualTo("STRONG_BULLISH");
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
        review.setSuccessfulProviderCount(3);
        review.setAiSupportCount(2);
        review.setAiObjectionCount(1);
        review.setConflictContribution(18);
        review.setReasonCodes(List.of("AI_CHALLENGE"));
        when(aiDecisionOrchestratorService.review(any())).thenReturn(review);
        DecisionEngineService serviceWithAi = new DecisionEngineService(
                ohlcvSnapshotSource,
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
        assertThat(decision.getMarketBiasHierarchy()).isEqualTo("STRONG_BULLISH");
        assertThat(decision.getAiRoleResults())
                .contains("\"finalMarketBias\":\"STRONG_BULLISH\"")
                .contains("\"ruleDirectionPreserved\":true");
        assertThat(decision.getAiRoleResults()).doesNotContain("Grok advisory");
        assertThat(decision.getAiRoleResults()).doesNotContain("Gemini advisory");
    }

    @Test
    void makeDecision_aiProviderFailureFallsBackWithoutFailingDecision() {
        when(aiDecisionOrchestratorService.review(any())).thenThrow(new IllegalStateException("provider unavailable"));
        DecisionEngineService serviceWithAi = new DecisionEngineService(
                ohlcvSnapshotSource,
                aiConflictResolverService,
                confusedStateService,
                assetStateService,
                ruleConfigService,
                aiDecisionOrchestratorService
        );

        DecisionBundleVO decision = serviceWithAi.makeDecision("BTCUSDT", "1m", "analysis-ai-fallback", 85, 65);

        assertThat(decision.getMarketBiasHierarchy()).isEqualTo("STRONG_BULLISH");
        assertThat(decision.getAiRoleResults()).contains("RULE_ONLY_FALLBACK");
        assertThat(decision.getAiRoleResults()).contains("AI_ORCHESTRATOR_FAILED");
        assertThat(decision.getAiPlanMode()).isNull();
    }

    @Test
    void allAbstainDoesNotProduceAiPlanMode() {
        AiOrchestratorResult review = new AiOrchestratorResult();
        review.setOrchestrationMode(AiOrchestrationMode.AI_ASSISTED);
        review.setSuccessfulProviderCount(3);
        review.setAiSupportCount(0);
        review.setAiObjectionCount(0);
        review.setProviderResults(List.of(
                successfulRole(AiProviderName.OPENAI, AiProviderRole.GPT_RULE_REVIEW, AiReviewStance.ABSTAIN),
                successfulRole(AiProviderName.GEMINI, AiProviderRole.GEMINI_CONSISTENCY_REVIEW, AiReviewStance.ABSTAIN),
                successfulRole(AiProviderName.XAI, AiProviderRole.GROK_ADVERSARIAL_CHALLENGE, AiReviewStance.ABSTAIN)));
        when(aiDecisionOrchestratorService.review(any())).thenReturn(review);
        DecisionEngineService serviceWithAi = new DecisionEngineService(
                ohlcvSnapshotSource,
                aiConflictResolverService,
                confusedStateService,
                assetStateService,
                ruleConfigService,
                aiDecisionOrchestratorService);

        DecisionBundleVO decision = serviceWithAi.makeDecision(
                "BTCUSDT", "5m", "analysis-all-abstain", 85, 65);

        assertThat(decision.getAiPlanMode()).isNull();
        assertThat(decision.getAiRoleResults()).contains("\"callStatus\":\"SUCCESS\"")
                .contains("\"stance\":\"ABSTAIN\"")
                .contains("\"ruleDirectionPreserved\":true");
    }

    @Test
    void makeDecision_eachAssetUsesItsOwnMarketWindow() {
        when(ohlcvSnapshotSource.readClosedBars(anyString(), anyString(), anyInt(), anyString()))
                .thenAnswer(invocation -> "ETHUSDT".equals(invocation.getArgument(0))
                        ? bearishKlines() : bullishKlines());

        DecisionBundleVO btc = service.makeDecision("BTCUSDT", "5m", "analysis-btc", 85, 65);
        DecisionBundleVO eth = service.makeDecision("ETHUSDT", "5m", "analysis-eth", 85, 65);

        assertThat(btc.getMarketBiasHierarchy()).isEqualTo("STRONG_BULLISH");
        assertThat(eth.getMarketBiasHierarchy()).isEqualTo("STRONG_BEARISH");
    }

    @Test
    void makeDecision_missingMarketWindowDoesNotDefaultBullish() {
        when(ohlcvSnapshotSource.readClosedBars(anyString(), anyString(), anyInt(), anyString()))
                .thenReturn(List.of());

        DecisionBundleVO decision = service.makeDecision("BTCUSDT", "5m", "analysis-missing", 85, 65);

        assertThat(decision.getMarketBiasHierarchy()).isEqualTo("WAIT");
        assertThat(decision.getConfidenceLevel()).isEqualTo("LOW");
        assertThat(decision.getRiskLevel()).isEqualTo("HIGH");
        assertThat(decision.getIsWorthOpening()).isFalse();
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

    private static AiProviderReviewResult successfulRole(AiProviderName provider,
                                                         AiProviderRole role,
                                                         AiReviewStance stance) {
        AiProviderReviewResult result = new AiProviderReviewResult();
        result.setProvider(provider);
        result.setRole(role);
        result.setCallStatus(AiProviderCallStatus.SUCCESS);
        result.setStance(stance);
        result.setReasonCodes(List.of("INSUFFICIENT_DATA"));
        result.setSummary("Insufficient evidence");
        return result;
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
