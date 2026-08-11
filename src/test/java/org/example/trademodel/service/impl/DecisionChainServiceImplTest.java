package org.example.trademodel.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.trademodel.ai.AiDecisionChainRequest;
import org.example.trademodel.ai.AiDecisionChainResult;
import org.example.trademodel.ai.AiDecisionChainRole;
import org.example.trademodel.ai.AiProviderCallStatus;
import org.example.trademodel.ai.AiProviderName;
import org.example.trademodel.ai.AiRoleResultsCodec;
import org.example.trademodel.analysisrun.AnalysisRunTriggerType;
import org.example.trademodel.decisionchain.DecisionChainBuildInput;
import org.example.trademodel.decisionchain.DecisionChainBuildResult;
import org.example.trademodel.decisionchain.RuleValidationResult;
import org.example.trademodel.entity.ConflictResolverResultDO;
import org.example.trademodel.entity.ExecutionPlanCandidateDO;
import org.example.trademodel.enums.AssetStateEnum;
import org.example.trademodel.mapper.ConflictResolverResultMapper;
import org.example.trademodel.mapper.ExecutionPlanCandidateMapper;
import org.example.trademodel.service.AiConflictResolverService;
import org.example.trademodel.service.AssetStateService;
import org.example.trademodel.service.DecisionChainAiOrchestratorService;
import org.example.trademodel.service.DecisionChainRuleValidator;
import org.example.trademodel.service.OpportunityTransitionResult;
import org.example.trademodel.service.OpportunityTriggerSource;
import org.example.trademodel.service.watchlistsource.AssetPoolService;
import org.example.trademodel.vo.DecisionBundleVO;
import org.example.trademodel.vo.ExecutionPlanVO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@Tag("core-regression")
class DecisionChainServiceImplTest {

    @Mock
    private AssetPoolService assetPoolService;
    @Mock
    private AssetStateService assetStateService;
    @Mock
    private DecisionChainAiOrchestratorService aiOrchestratorService;
    @Mock
    private AiConflictResolverService conflictResolver;
    @Mock
    private DecisionChainRuleValidator ruleValidator;
    @Mock
    private ExecutionPlanCandidateMapper candidateMapper;
    @Mock
    private ConflictResolverResultMapper conflictMapper;
    @Mock
    private AiRoleResultsCodec aiRoleResultsCodec;

    private DecisionChainServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new DecisionChainServiceImpl(
                assetPoolService,
                assetStateService,
                aiOrchestratorService,
                conflictResolver,
                ruleValidator,
                candidateMapper,
                conflictMapper,
                new ObjectMapper(),
                aiRoleResultsCodec);
    }

    @Test
    void symbolOutsideAssetPoolCannotCreateOpportunityCandidateOrAiCalls() {
        DecisionChainBuildInput input = input();
        when(assetPoolService.isOpportunitySource("BTCUSDT")).thenReturn(false);

        DecisionChainBuildResult result = service.build(input);

        assertThat(result.opportunity()).isNull();
        assertThat(result.candidate()).isNull();
        assertThat(result.conflict()).isNull();
        assertThat(result.validation().passed()).isFalse();
        assertThat(result.validation().reasons()).containsExactly("ASSET_POOL_SOURCE_REQUIRED");
        assertThat(result.finalPlan().getFinalPlan()).isFalse();
        assertThat(result.finalPlan().getRuleValidationStatus()).isEqualTo("BLOCKED");
        assertThat(input.decision().getIsWorthOpening()).isFalse();
        verify(assetStateService, never()).transition(
                anyString(), any(), anyInt(), anyInt(), any(), any(), anyString(), any());
        verify(aiOrchestratorService, never()).invoke(any());
    }

    @Test
    void threeRoleChainProducesSeparateCandidateAndRuleValidatedFinalPlan() {
        stubHappyPath();

        DecisionChainBuildResult result = service.build(input());

        assertThat(result.candidate()).isNotNull();
        assertThat(result.candidate().getNotFinalPlan()).isTrue();
        assertThat(result.candidate().getCandidateSource()).isEqualTo("GPT_FINAL");
        assertThat(result.finalPlan()).isNotSameAs(result.candidate());
        assertThat(result.finalPlan().getFinalPlan()).isTrue();
        assertThat(result.finalPlan().getRuleValidationStatus()).isEqualTo("PASS");
        assertThat(result.finalPlan().getCandidateId()).isEqualTo(result.candidate().getCandidateId());
        assertThat(result.finalPlan().getOpportunityId()).isEqualTo("opp-btc");
        assertThat(result.finalPlan().getResolverResultId()).isEqualTo("resolver-1");
        assertThat(result.finalPlan().getNotUserPositionCreation()).isTrue();

        ArgumentCaptor<AiDecisionChainRequest> calls = ArgumentCaptor.forClass(AiDecisionChainRequest.class);
        verify(aiOrchestratorService, org.mockito.Mockito.times(3)).invoke(calls.capture());
        assertThat(calls.getAllValues()).extracting(AiDecisionChainRequest::getRole)
                .containsExactly(
                        AiDecisionChainRole.GPT_FINAL,
                        AiDecisionChainRole.GEMINI_REVIEW,
                        AiDecisionChainRole.GROK_CHALLENGE);
        assertThat(calls.getAllValues()).allSatisfy(call -> {
            assertThat(call.getAnalysisId()).isEqualTo("analysis-1");
            assertThat(call.getTraceId()).isEqualTo("trace-1");
            assertThat(call.getCandidateId()).isEqualTo(result.candidate().getCandidateId());
        });
    }

    @Test
    void gptFailureFallsBackToRuleCandidateAndNeverFabricatesAiReview() {
        when(assetPoolService.isOpportunitySource("BTCUSDT")).thenReturn(true);
        when(assetStateService.transition(
                anyString(), anyString(), any(), anyInt(), anyInt(), any(), any(), anyString(), any()))
                .thenReturn(opportunity(AssetStateEnum.CANDIDATE, "ADVISORY_ALLOWED"));
        when(aiOrchestratorService.invoke(any())).thenAnswer(invocation -> {
            AiDecisionChainRequest request = invocation.getArgument(0);
            return AiDecisionChainResult.failed(provider(request.getRole()), request.getRole(),
                    AiProviderCallStatus.TIMEOUT, "PROVIDER_TIMEOUT");
        });
        when(conflictResolver.resolveDecisionChain(any(), anyString(), anyString(), any(), any(), anyString()))
                .thenReturn(conflict(false));
        when(ruleValidator.validate(any(), any(), any(), any())).thenReturn(RuleValidationResult.pass());
        when(aiRoleResultsCodec.serializeDecisionChain(any(), any(), any(), any(), any())).thenReturn("{}");

        DecisionChainBuildResult result = service.build(input());

        assertThat(result.candidate().getCandidateSource()).isEqualTo("RULE_FALLBACK");
        assertThat(result.candidate().getFallbackReason()).isEqualTo("PROVIDER_TIMEOUT");
        assertThat(result.finalPlan().getChainStatus()).isEqualTo("RULE_FALLBACK_VALIDATED");
        ArgumentCaptor<String> fallbackPayloads = ArgumentCaptor.forClass(String.class);
        verify(conflictResolver).resolveDecisionChain(
                any(), fallbackPayloads.capture(), fallbackPayloads.capture(), any(), any(), anyString());
        assertThat(fallbackPayloads.getAllValues()).allSatisfy(payload ->
                assertThat(payload).contains("\"fallback\":true"));
    }

    @Test
    void confusedConflictUsesCanonicalStateTransitionAndBlocksFinalPlan() {
        when(assetPoolService.isOpportunitySource("BTCUSDT")).thenReturn(true);
        when(assetStateService.transition(
                anyString(), anyString(), any(), anyInt(), anyInt(), any(), any(), anyString(), any()))
                .thenAnswer(invocation -> {
                    AssetStateEnum requested = invocation.getArgument(2);
                    return requested == AssetStateEnum.CONFUSED
                            ? opportunity(AssetStateEnum.CONFUSED, "BLOCKED")
                            : opportunity(AssetStateEnum.CANDIDATE, "ADVISORY_ALLOWED");
                });
        when(aiOrchestratorService.invoke(any())).thenAnswer(invocation -> success(
                ((AiDecisionChainRequest) invocation.getArgument(0)).getRole()));
        when(conflictResolver.resolveDecisionChain(any(), anyString(), anyString(), any(), any(), anyString()))
                .thenReturn(conflict(true));
        when(ruleValidator.validate(any(), any(), any(), any()))
                .thenReturn(RuleValidationResult.blocked(List.of("PLAN_MODE_BLOCKED")));
        when(aiRoleResultsCodec.serializeDecisionChain(any(), any(), any(), any(), any())).thenReturn("{}");

        DecisionChainBuildResult result = service.build(input());

        assertThat(result.opportunity().state()).isEqualTo(AssetStateEnum.CONFUSED);
        assertThat(result.finalPlan().getFinalPlan()).isFalse();
        assertThat(result.finalPlan().getRuleValidationStatus()).isEqualTo("BLOCKED");
        ArgumentCaptor<AssetStateEnum> states = ArgumentCaptor.forClass(AssetStateEnum.class);
        verify(assetStateService, org.mockito.Mockito.times(2)).transition(
                anyString(), anyString(), states.capture(), anyInt(), anyInt(), any(), any(), anyString(), any());
        assertThat(states.getAllValues()).containsExactly(AssetStateEnum.CANDIDATE, AssetStateEnum.CONFUSED);
    }

    @Test
    void persistenceWritesCandidateBeforeResolverAndRejectsPartialWrites() {
        ExecutionPlanCandidateDO candidate = new ExecutionPlanCandidateDO();
        candidate.setCandidateId("candidate-1");
        ConflictResolverResultDO conflict = conflict(false);
        DecisionChainBuildResult result = new DecisionChainBuildResult(
                opportunity(AssetStateEnum.CANDIDATE, "ADVISORY_ALLOWED"),
                candidate,
                conflict,
                RuleValidationResult.pass(),
                new ExecutionPlanVO());
        when(candidateMapper.insert(candidate)).thenReturn(1);
        when(conflictMapper.insert(conflict)).thenReturn(1);

        service.persist(result);

        org.mockito.InOrder order = org.mockito.Mockito.inOrder(candidateMapper, conflictMapper);
        order.verify(candidateMapper).insert(candidate);
        order.verify(conflictMapper).insert(conflict);
    }

    private void stubHappyPath() {
        when(assetPoolService.isOpportunitySource("BTCUSDT")).thenReturn(true);
        when(assetStateService.transition(
                anyString(), anyString(), any(), anyInt(), anyInt(), any(), any(), anyString(), any()))
                .thenReturn(opportunity(AssetStateEnum.CANDIDATE, "ADVISORY_ALLOWED"));
        when(aiOrchestratorService.invoke(any())).thenAnswer(invocation -> success(
                ((AiDecisionChainRequest) invocation.getArgument(0)).getRole()));
        when(conflictResolver.resolveDecisionChain(any(), anyString(), anyString(), any(), any(), anyString()))
                .thenReturn(conflict(false));
        when(ruleValidator.validate(any(), any(), any(), any())).thenReturn(RuleValidationResult.pass());
        when(aiRoleResultsCodec.serializeDecisionChain(any(), any(), any(), any(), any())).thenReturn("{}");
    }

    private static DecisionChainBuildInput input() {
        DecisionBundleVO decision = new DecisionBundleVO();
        decision.setMarketBiasHierarchy("BULLISH");
        decision.setConfidenceLevel("HIGH");
        decision.setRiskLevel("MEDIUM");
        decision.setConclusionSummary("Rule conclusion");
        decision.setIsWorthOpening(true);
        decision.setAssetState(AssetStateEnum.CANDIDATE);
        decision.setConfusedScore(20);
        decision.setConfusedLowStreak(0);
        decision.setExpiresAt(OffsetDateTime.of(2026, 8, 12, 0, 0, 0, 0, ZoneOffset.UTC));

        ExecutionPlanVO plan = new ExecutionPlanVO();
        plan.setPlanId("plan-1");
        plan.setExecutionPlanStatus(ExecutionPlanVO.EXECUTION_PLAN_STATUS_VALID);
        plan.setSourceGateStatus(ExecutionPlanVO.EXECUTION_PLAN_STATUS_VALID);
        plan.setSourceGateComplete(true);
        plan.setRecommendedAction("MANUAL_REVIEW");
        plan.setEntryZone("100-101");
        plan.setStopLoss("95");
        plan.setTakeProfitRules("110 then 120");
        plan.setLeverageSuggestion("1x");
        plan.setPositionSuggestion("small");
        plan.setInvalidCondition("close below 95");
        plan.setRiskActionGuardReady(true);
        return new DecisionChainBuildInput(
                "analysis-1", "trace-1", "BTCUSDT", "5m", 90,
                decision, plan, List.of(), List.of(), AnalysisRunTriggerType.ASSET_POOL_SCAN);
    }

    private static OpportunityTransitionResult opportunity(AssetStateEnum state, String permission) {
        return new OpportunityTransitionResult(
                "opp-btc", "BTCUSDT", AssetStateEnum.OBSERVING, state,
                true, false, "PROMOTED", OpportunityTriggerSource.ASSET_POOL_SCAN.name(),
                permission, LocalDateTime.now());
    }

    private static ConflictResolverResultDO conflict(boolean confused) {
        ConflictResolverResultDO result = new ConflictResolverResultDO();
        result.setResolverResultId("resolver-1");
        result.setCandidateId("candidate-1");
        result.setAnalysisId("analysis-1");
        result.setTraceId("trace-1");
        result.setRuleDirection("BULLISH");
        result.setRuleConfidence("HIGH");
        result.setRuleRisk("MEDIUM");
        result.setGeminiReviewJson("{}");
        result.setGrokChallengeJson("{}");
        result.setConflictLevel(confused
                ? "LEVEL_4_EXTREME_CONFLICT"
                : "LEVEL_1_CONSISTENT");
        result.setConflictScore(confused ? 90 : 0);
        result.setPlanModeBefore("CONFIRM");
        result.setPlanModeAfter(confused ? "BLOCKED" : "CONFIRM");
        result.setConfidenceBefore("HIGH");
        result.setConfidenceAfter(confused ? "LOW" : "HIGH");
        result.setRiskBefore("MEDIUM");
        result.setRiskAfter(confused ? "HIGH" : "MEDIUM");
        result.setConfusedDecision(confused);
        result.setDowngradeReason(confused ? "CONFLICT" : null);
        result.setRuleDirectionPreserved(true);
        result.setCreatedAt(LocalDateTime.now());
        return result;
    }

    private static AiDecisionChainResult success(AiDecisionChainRole role) {
        AiDecisionChainResult result = new AiDecisionChainResult();
        result.setProvider(provider(role));
        result.setRole(role);
        result.setCallStatus(AiProviderCallStatus.SUCCESS);
        result.setPayloadJson(switch (role) {
            case GPT_FINAL -> """
                    {"direction":"BULLISH","planMode":"CONFIRM","confidence":"HIGH","riskLevel":"MEDIUM",
                     "worthOpening":true,"recommendedAction":"MANUAL_REVIEW","entryZone":"100-101",
                     "stopLoss":"95","takeProfitRules":"110 then 120","leverageSuggestion":"1x",
                     "positionSuggestion":"small","invalidCondition":"close below 95",
                     "validity":"2026-08-12T00:00Z","summary":"Candidate only"}
                    """;
            case GEMINI_REVIEW -> """
                    {"verdict":"APPROVE","conflictLevel":"LEVEL_1_CONSISTENT","confidenceAdjustment":"UNCHANGED",
                     "riskAdjustment":"UNCHANGED","planModeAdjustment":"UNCHANGED","reasons":[],"summary":"approved"}
                    """;
            case GROK_CHALLENGE -> """
                    {"opposingView":"none","riskLevel":"MEDIUM","challengeLevel":"LEVEL_1_CONSISTENT",
                     "majorCounterEvidence":false,"planModeImpact":"UNCHANGED","reasons":[],"summary":"no challenge"}
                    """;
        });
        return result;
    }

    private static AiProviderName provider(AiDecisionChainRole role) {
        return switch (role) {
            case GPT_FINAL -> AiProviderName.OPENAI;
            case GEMINI_REVIEW -> AiProviderName.GEMINI;
            case GROK_CHALLENGE -> AiProviderName.XAI;
        };
    }
}
