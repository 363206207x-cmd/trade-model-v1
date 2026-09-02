package org.example.trademodel.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.trademodel.ai.AiDecisionChainRequest;
import org.example.trademodel.ai.AiDecisionChainResult;
import org.example.trademodel.ai.AiDecisionChainRole;
import org.example.trademodel.ai.AiOrchestratorProperties;
import org.example.trademodel.ai.AiProviderCallStatus;
import org.example.trademodel.ai.AiProviderName;
import org.example.trademodel.ai.AiRoleResultsCodec;
import org.example.trademodel.analysisrun.AnalysisRunTriggerType;
import org.example.trademodel.config.FundamentalAiV41Properties;
import org.example.trademodel.decisionchain.DecisionChainBuildInput;
import org.example.trademodel.decisionchain.DecisionChainBuildResult;
import org.example.trademodel.decisionchain.RuleValidationResult;
import org.example.trademodel.entity.ConflictResolverResultDO;
import org.example.trademodel.entity.ExecutionPlanCandidateDO;
import org.example.trademodel.entity.TmAccountRiskSnapshotDO;
import org.example.trademodel.enums.AssetStateEnum;
import org.example.trademodel.mapper.ConflictResolverResultMapper;
import org.example.trademodel.mapper.ExecutionPlanCandidateMapper;
import org.example.trademodel.providercall.SnapshotFreshnessStatus;
import org.example.trademodel.providercall.UnifiedSourceStatus;
import org.example.trademodel.providercall.snapshot.DerivativesRiskSnapshot;
import org.example.trademodel.service.AiConflictResolverService;
import org.example.trademodel.service.AssetStateService;
import org.example.trademodel.service.DecisionChainAiOrchestratorService;
import org.example.trademodel.service.DecisionChainRuleValidator;
import org.example.trademodel.service.OpportunityTransitionResult;
import org.example.trademodel.service.OpportunityStateIdentity;
import org.example.trademodel.service.OpportunityTriggerSource;
import org.example.trademodel.service.support.ExecutionFeasibilityContract;
import org.example.trademodel.service.support.V41DecisionContractPolicy;
import org.example.trademodel.service.watchlistsource.AssetPoolService;
import org.example.trademodel.vo.DecisionBundleVO;
import org.example.trademodel.vo.EvidenceItemVO;
import org.example.trademodel.vo.ExecutionPlanVO;
import org.example.trademodel.vo.ScoreItemVO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.lenient;
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
        lenient().when(assetPoolService.resolvePoolItemId(anyString(), any(), any(), anyString()))
                .thenReturn(11L);
    }

    @Test
    void symbolOutsideAssetPoolCannotCreateOpportunityCandidateOrAiCalls() {
        DecisionChainBuildInput input = input();
        when(assetPoolService.isOpportunitySource("SYSTEM", 0L, 1L, "BTCUSDT")).thenReturn(false);

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
                any(OpportunityStateIdentity.class), any(), anyInt(), anyInt(), any(), any(), anyString(), anyString(), any());
        verify(aiOrchestratorService, never()).invoke(any());
    }

    @Test
    void explicitOptionalAbsenceIsCompleteButUnexplainedNullsRemainBlocked() {
        ScoreItemVO explicitMissingScore = score("score-optional", "资金推动分");
        explicitMissingScore.setScoreValue(null);
        explicitMissingScore.setDescription(
                "coverage=0.0;missingInputs=[verifiedFundingRate];permission=INSUFFICIENT_DATA");
        assertThat(V41DecisionContractPolicy.scoreItemContractComplete(explicitMissingScore)).isTrue();

        explicitMissingScore.setDescription("provider unavailable");
        assertThat(V41DecisionContractPolicy.scoreItemContractComplete(explicitMissingScore)).isFalse();

        EvidenceItemVO unavailableEvidence = evidence();
        unavailableEvidence.setStrength(null);
        unavailableEvidence.setConfidence(null);
        unavailableEvidence.setCurrentValue(null);
        unavailableEvidence.setChangeFromBaseline(null);
        unavailableEvidence.setObservedAt(null);
        unavailableEvidence.setFreshness("SOURCE_UNAVAILABLE");
        unavailableEvidence.setDescription("Optional derivatives provider is unavailable");
        assertThat(V41DecisionContractPolicy.evidenceItemContractComplete(
                unavailableEvidence, "analysis-1")).isTrue();

        unavailableEvidence.setFreshness("FRESH");
        assertThat(V41DecisionContractPolicy.evidenceItemContractComplete(
                unavailableEvidence, "analysis-1")).isFalse();
        assertThat(V41DecisionContractPolicy.evidenceItemContractComplete(
                evidence(), "analysis-1")).isTrue();
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
        assertThat(calls.getAllValues().get(0).getRole()).isEqualTo(AiDecisionChainRole.GPT_FINAL);
        assertThat(calls.getAllValues()).extracting(AiDecisionChainRequest::getRole)
                .containsExactlyInAnyOrder(AiDecisionChainRole.GPT_FINAL,
                        AiDecisionChainRole.GEMINI_REVIEW,
                        AiDecisionChainRole.GROK_CHALLENGE);
        assertThat(calls.getAllValues()).allSatisfy(call -> {
            assertThat(call.getAnalysisId()).isEqualTo("analysis-1");
            assertThat(call.getTraceId()).isEqualTo("trace-1");
            assertThat(call.getCandidateId()).isEqualTo(result.candidate().getCandidateId());
            assertThat(call.isInputContractSatisfied()).isTrue();
            assertThat(call.getInputContractFailures()).isEmpty();
        });
        assertThat(calls.getAllValues().get(0).getInput()).satisfies(input -> {
            assertThat((List<?>) input.get("evidence")).hasSize(1);
            assertThat((List<?>) input.get("scores")).hasSize(8);
            assertThat(((Map<?, ?>) ((Map<?, ?>) input.get("decisionBundle")).get("multiTimeframe"))
                    .keySet().stream().map(String::valueOf).toList())
                    .containsExactlyInAnyOrder("4h", "1h", "15m", "5m");
        });
    }

    @Test
    void geminiAndGrokStartOnlyAfterGptAndConsumeSameImmutableCandidateSnapshotInParallel()
            throws Exception {
        when(assetPoolService.isOpportunitySource("SYSTEM", 0L, 1L, "BTCUSDT")).thenReturn(true);
        when(assetStateService.transition(
                any(OpportunityStateIdentity.class), any(), anyInt(), anyInt(), any(), any(),
                anyString(), anyString(), any()))
                .thenReturn(opportunity(AssetStateEnum.CANDIDATE, "ADVISORY_ALLOWED"));
        when(conflictResolver.resolveDecisionChain(any(), anyString(), anyString(), any(), any(), anyString()))
                .thenReturn(conflict(false));
        when(ruleValidator.validate(any(), any(), any(), any())).thenReturn(RuleValidationResult.pass());
        when(aiRoleResultsCodec.serializeDecisionChain(any(), any(), any(), any(), any())).thenReturn("{}");
        AtomicBoolean gptCompleted = new AtomicBoolean();
        AtomicBoolean reviewStartedBeforeGpt = new AtomicBoolean();
        CountDownLatch bothReviewsStarted = new CountDownLatch(2);
        CountDownLatch releaseReviews = new CountDownLatch(1);
        List<AiDecisionChainRequest> reviewRequests = Collections.synchronizedList(new ArrayList<>());
        when(aiOrchestratorService.invoke(any())).thenAnswer(invocation -> {
            AiDecisionChainRequest request = invocation.getArgument(0);
            if (request.getRole() == AiDecisionChainRole.GPT_FINAL) {
                gptCompleted.set(true);
                return success(request.getRole());
            }
            if (!gptCompleted.get()) reviewStartedBeforeGpt.set(true);
            reviewRequests.add(request);
            bothReviewsStarted.countDown();
            if (!releaseReviews.await(2, TimeUnit.SECONDS)) {
                throw new IllegalStateException("parallel review barrier timed out");
            }
            return success(request.getRole());
        });

        CompletableFuture<DecisionChainBuildResult> build = CompletableFuture.supplyAsync(
                () -> service.build(input()));
        assertThat(bothReviewsStarted.await(2, TimeUnit.SECONDS)).isTrue();
        assertThat(build.isDone()).isFalse();
        releaseReviews.countDown();
        DecisionChainBuildResult result = build.get(3, TimeUnit.SECONDS);

        assertThat(reviewStartedBeforeGpt.get()).isFalse();
        assertThat(reviewRequests).hasSize(2);
        assertThat(reviewRequests).extracting(AiDecisionChainRequest::getRole)
                .containsExactlyInAnyOrder(
                        AiDecisionChainRole.GEMINI_REVIEW, AiDecisionChainRole.GROK_CHALLENGE);
        assertThat(reviewRequests.get(0).getInput()).isEqualTo(reviewRequests.get(1).getInput());
        assertThat(reviewRequests.get(0).getInput().get("executionPlanCandidate"))
                .isSameAs(reviewRequests.get(1).getInput().get("executionPlanCandidate"));
        assertThat(reviewRequests).allSatisfy(request -> {
            assertThat(request.getAnalysisId()).isEqualTo("analysis-1");
            assertThat(request.getCandidateId()).isEqualTo(result.candidate().getCandidateId());
            assertThat(request.getInput().get("candidateSource")).isEqualTo("GPT_FINAL");
        });
        verify(conflictResolver).resolveDecisionChain(
                any(), anyString(), anyString(), any(), any(), anyString());
        verify(ruleValidator).validate(any(), any(), any(), any());
    }

    @Test
    void chainDeadlineStopsResolverAndFinalAfterBothReviewTasksWereStarted() throws Exception {
        AiOrchestratorProperties timeoutProperties = new AiOrchestratorProperties();
        timeoutProperties.getBackgroundExecution().setChainDeadlineMs(50);
        service = new DecisionChainServiceImpl(
                assetPoolService, assetStateService, aiOrchestratorService, conflictResolver,
                ruleValidator, candidateMapper, conflictMapper, new ObjectMapper(), aiRoleResultsCodec,
                FundamentalAiV41Properties.contractFixture(), timeoutProperties);
        when(assetPoolService.isOpportunitySource("SYSTEM", 0L, 1L, "BTCUSDT")).thenReturn(true);
        when(assetStateService.transition(
                any(OpportunityStateIdentity.class), any(), anyInt(), anyInt(), any(), any(),
                anyString(), anyString(), any()))
                .thenReturn(opportunity(AssetStateEnum.CANDIDATE, "ADVISORY_ALLOWED"));
        when(aiRoleResultsCodec.serializeDecisionChain(any(), any(), any(), any(), any())).thenReturn("{}");
        CountDownLatch reviewsStarted = new CountDownLatch(2);
        when(aiOrchestratorService.invoke(any())).thenAnswer(invocation -> {
            AiDecisionChainRequest request = invocation.getArgument(0);
            if (request.getRole() == AiDecisionChainRole.GPT_FINAL) return success(request.getRole());
            reviewsStarted.countDown();
            try {
                Thread.sleep(200L);
            } catch (InterruptedException interrupted) {
                Thread.currentThread().interrupt();
            }
            return success(request.getRole());
        });

        DecisionChainBuildResult result = service.build(input());
        assertThat(reviewsStarted.await(1, TimeUnit.SECONDS)).isTrue();

        assertThat(result.candidate()).isNotNull();
        assertThat(result.candidate().getCandidateStatus()).isEqualTo("REJECTED");
        assertThat(result.validation().passed()).isFalse();
        assertThat(result.validation().reasons()).containsExactly("CHAIN_DEADLINE_EXCEEDED");
        assertThat(result.finalPlan().getFinalPlan()).isFalse();
        verify(conflictResolver, never()).resolveDecisionChain(
                any(), anyString(), anyString(), any(), any(), anyString());
        verify(ruleValidator, never()).validate(any(), any(), any(), any());
    }

    @Test
    void oneReviewFailureStillWaitsForTheOtherTerminalResultBeforeResolver() throws Exception {
        when(assetPoolService.isOpportunitySource("SYSTEM", 0L, 1L, "BTCUSDT")).thenReturn(true);
        when(assetStateService.transition(
                any(OpportunityStateIdentity.class), any(), anyInt(), anyInt(), any(), any(),
                anyString(), anyString(), any()))
                .thenReturn(opportunity(AssetStateEnum.CANDIDATE, "ADVISORY_ALLOWED"));
        when(conflictResolver.resolveDecisionChain(any(), anyString(), anyString(), any(), any(), anyString()))
                .thenReturn(conflict(false));
        when(ruleValidator.validate(any(), any(), any(), any())).thenReturn(RuleValidationResult.pass());
        when(aiRoleResultsCodec.serializeDecisionChain(any(), any(), any(), any(), any())).thenReturn("{}");
        CountDownLatch grokStarted = new CountDownLatch(1);
        CountDownLatch releaseGrok = new CountDownLatch(1);
        when(aiOrchestratorService.invoke(any())).thenAnswer(invocation -> {
            AiDecisionChainRequest request = invocation.getArgument(0);
            if (request.getRole() == AiDecisionChainRole.GPT_FINAL) return success(request.getRole());
            if (request.getRole() == AiDecisionChainRole.GEMINI_REVIEW) {
                throw new IllegalStateException("review infrastructure failed");
            }
            grokStarted.countDown();
            if (!releaseGrok.await(2, TimeUnit.SECONDS)) {
                throw new IllegalStateException("grok review barrier timed out");
            }
            return success(request.getRole());
        });

        CompletableFuture<DecisionChainBuildResult> build = CompletableFuture.supplyAsync(
                () -> service.build(input()));
        assertThat(grokStarted.await(2, TimeUnit.SECONDS)).isTrue();
        assertThat(build.isDone()).isFalse();
        verify(conflictResolver, never()).resolveDecisionChain(
                any(), anyString(), anyString(), any(), any(), anyString());

        releaseGrok.countDown();
        DecisionChainBuildResult result = build.get(3, TimeUnit.SECONDS);

        assertThat(result.candidate().getCandidateStatus()).isEqualTo("REJECTED");
        assertThat(result.validation().passed()).isFalse();
        assertThat(result.validation().reasons()).containsExactly("COMPLETE_THREE_AI_CHAIN_REQUIRED");
        assertThat(result.finalPlan().getFinalPlan()).isFalse();
        verify(conflictResolver).resolveDecisionChain(
                any(), anyString(), anyString(), any(), any(), anyString());
        verify(ruleValidator).validate(any(), any(), any(), any());
    }

    @Test
    void coinglassDatasetsRemainExplicitWhenGenericEvidenceWindowIsFull() {
        stubHappyPath();
        DecisionChainBuildInput base = input();
        List<EvidenceItemVO> evidence = new ArrayList<>();
        for (int index = 0; index < 20; index++) {
            EvidenceItemVO item = evidence();
            item.setEvidenceId("price-evidence-" + index);
            evidence.add(item);
        }
        EvidenceItemVO derivative = evidence();
        derivative.setEvidenceId("coinglass-open-interest-1");
        derivative.setEvidenceType("OPEN_INTEREST_PRICE_CONFIRMATION");
        derivative.setSource("PROVIDER_SNAPSHOT");
        derivative.setSourceProvider("COINGLASS_V4");
        derivative.setSourceReference("sourceField=openInterestChange5m;provider=COINGLASS_V4");
        derivative.setCurrentValue("1.8");
        derivative.setChangeFromBaseline("+1.8%");
        evidence.add(derivative);
        DecisionChainBuildInput withDerivatives = new DecisionChainBuildInput(
                base.analysisId(), base.traceId(), base.symbol(), base.timeframe(), base.dataQualityScore(),
                base.decision(), base.rulePlan(), evidence, base.scores(), base.triggerType(), base.ownerType(),
                base.ownerId(), base.assetId(), base.ruleVersion(), base.preview(), base.requestId(),
                base.accountRiskSnapshot(), derivativesSnapshot(), null);

        service.build(withDerivatives);

        ArgumentCaptor<AiDecisionChainRequest> requests = ArgumentCaptor.forClass(AiDecisionChainRequest.class);
        verify(aiOrchestratorService, org.mockito.Mockito.times(3)).invoke(requests.capture());
        Map<String, Object> input = requests.getAllValues().get(0).getInput();
        assertThat((List<?>) input.get("evidence")).hasSize(20);
        assertThat(String.valueOf(input.get("evidence"))).doesNotContain("coinglass-open-interest-1");
        Map<?, ?> derivatives = (Map<?, ?>) input.get("derivativesContext");
        assertThat(derivatives.get("source")).isEqualTo("COINGLASS_V4");
        assertThat(((List<?>) derivatives.get("availableDatasets")).stream().map(String::valueOf).toList())
                .containsExactly("OPEN_INTEREST", "FUNDING", "LIQUIDATION", "LONG_SHORT_RATIO");
        assertThat(((Map<?, ?>) derivatives.get("datasetReadings")).keySet().stream().map(String::valueOf).toList())
                .contains("openInterest", "funding", "liquidation", "longShortRatio");
        assertThat(String.valueOf(derivatives.get("derivedEvidence")))
                .contains("coinglass-open-interest-1", "OPEN_INTEREST_PRICE_CONFIRMATION");
    }

    @Test
    void searchPreviewRunsAllThreeRolesWithoutOpportunityCandidateOrFinalPersistence() {
        DecisionChainBuildInput base = input();
        DecisionChainBuildInput preview = new DecisionChainBuildInput(
                base.analysisId(), base.traceId(), "AAVEUSDT", base.timeframe(), base.dataQualityScore(),
                base.decision(), base.rulePlan(), base.evidence(), base.scores(),
                AnalysisRunTriggerType.ANALYSIS_PREVIEW, "USER", 31L, null,
                "FUNDAMENTAL_AI_V4_1", true, "preview-request-1", base.accountRiskSnapshot());
        when(aiOrchestratorService.invoke(any())).thenAnswer(invocation -> success(
                ((AiDecisionChainRequest) invocation.getArgument(0)).getRole()));
        when(conflictResolver.resolveDecisionChain(any(), anyString(), anyString(), any(), any(), anyString()))
                .thenReturn(conflict(false));
        when(aiRoleResultsCodec.serializeDecisionChain(any(), any(), any(), any(), any()))
                .thenReturn("{\"roles\":{\"GPT_FINAL\":{},\"GEMINI_REVIEW\":{},\"GROK_CHALLENGE\":{}}}");

        DecisionChainBuildResult result = service.build(preview);
        service.persist(result);

        assertThat(result.preview()).isTrue();
        assertThat(result.opportunity()).isNull();
        assertThat(result.candidate()).isNotNull();
        assertThat(result.candidate().getOpportunityId()).isNull();
        assertThat(result.candidate().getCandidateStatus()).isEqualTo("PREVIEW_ONLY");
        assertThat(result.finalPlan()).isNull();
        assertThat(preview.decision().getAiRoleResults())
                .contains("GPT_FINAL", "GEMINI_REVIEW", "GROK_CHALLENGE");
        ArgumentCaptor<AiDecisionChainRequest> calls = ArgumentCaptor.forClass(AiDecisionChainRequest.class);
        verify(aiOrchestratorService, org.mockito.Mockito.times(3)).invoke(calls.capture());
        assertThat(calls.getAllValues().get(0).getRole()).isEqualTo(AiDecisionChainRole.GPT_FINAL);
        assertThat(calls.getAllValues()).extracting(AiDecisionChainRequest::getRole)
                .containsExactlyInAnyOrder(AiDecisionChainRole.GPT_FINAL,
                        AiDecisionChainRole.GEMINI_REVIEW, AiDecisionChainRole.GROK_CHALLENGE);
        verify(assetStateService, never()).transition(
                any(OpportunityStateIdentity.class), any(), anyInt(), anyInt(), any(), any(),
                anyString(), anyString(), any());
        verify(candidateMapper, never()).insert(any());
        verify(conflictMapper, never()).insert(any());
    }

    @Test
    void gptFailureDoesNotCreateCandidateOrRunDownstreamDecisionStages() {
        when(assetPoolService.isOpportunitySource("SYSTEM", 0L, 1L, "BTCUSDT")).thenReturn(true);
        when(assetStateService.transition(
                any(OpportunityStateIdentity.class), any(), anyInt(), anyInt(), any(), any(), anyString(), anyString(), any()))
                .thenReturn(opportunity(AssetStateEnum.CANDIDATE, "ADVISORY_ALLOWED"));
        when(aiOrchestratorService.invoke(any())).thenAnswer(invocation -> {
            AiDecisionChainRequest request = invocation.getArgument(0);
            return AiDecisionChainResult.failed(provider(request.getRole()), request.getRole(),
                    AiProviderCallStatus.TIMEOUT, "PROVIDER_TIMEOUT");
        });
        when(aiRoleResultsCodec.serializeDecisionChain(any(), any(), any(), any(), any())).thenReturn("{}");

        DecisionChainBuildResult result = service.build(input());

        assertThat(result.candidate()).isNull();
        assertThat(result.conflict()).isNull();
        assertThat(result.validation().passed()).isFalse();
        assertThat(result.validation().reasons()).containsExactly("GPT_CANDIDATE_REQUIRED");
        assertThat(result.finalPlan().getFinalPlan()).isFalse();
        assertThat(result.finalPlan().getChainStatus()).isEqualTo("RULE_VALIDATION_BLOCKED");
        verify(aiOrchestratorService).invoke(any());
        verify(conflictResolver, never()).resolveDecisionChain(
                any(), anyString(), anyString(), any(), any(), anyString());
        verify(ruleValidator, never()).validate(any(), any(), any(), any());
    }

    @Test
    void incompleteEvidenceScoresAndTimeframesStopAfterAuditedGptInputGate() {
        DecisionChainBuildInput complete = input();
        complete.evidence().get(0).setSourceReference(null);
        complete.evidence().get(0).setSourceTraceId(null);
        Map<String, Map<String, Object>> incompleteTimeframes = complete.decision().getMultiTimeframeDetails();
        incompleteTimeframes.remove("5m");
        complete.decision().setMultiTimeframeDetails(incompleteTimeframes);
        DecisionChainBuildInput incomplete = new DecisionChainBuildInput(
                complete.analysisId(), complete.traceId(), complete.symbol(), complete.timeframe(),
                complete.dataQualityScore(), complete.decision(), complete.rulePlan(), complete.evidence(),
                complete.scores().subList(0, 7), complete.triggerType(), complete.ownerType(),
                complete.ownerId(), complete.assetId(), complete.ruleVersion(), complete.preview(),
                complete.requestId(), complete.accountRiskSnapshot());
        when(assetPoolService.isOpportunitySource("SYSTEM", 0L, 1L, "BTCUSDT")).thenReturn(true);
        when(assetStateService.transition(
                any(OpportunityStateIdentity.class), any(), anyInt(), anyInt(), any(), any(), anyString(), anyString(), any()))
                .thenReturn(opportunity(AssetStateEnum.CANDIDATE, "ADVISORY_ALLOWED"));
        when(aiOrchestratorService.invoke(any())).thenAnswer(invocation -> {
            AiDecisionChainRequest request = invocation.getArgument(0);
            return AiDecisionChainResult.failed(provider(request.getRole()), request.getRole(),
                    AiProviderCallStatus.INVALID_RESPONSE, "AI_INPUT_CONTRACT_BLOCKED");
        });
        when(aiRoleResultsCodec.serializeDecisionChain(any(), any(), any(), any(), any())).thenReturn("{}");

        DecisionChainBuildResult result = service.build(incomplete);

        assertThat(result.candidate()).isNull();
        assertThat(result.conflict()).isNull();
        ArgumentCaptor<AiDecisionChainRequest> requests = ArgumentCaptor.forClass(AiDecisionChainRequest.class);
        verify(aiOrchestratorService).invoke(requests.capture());
        assertThat(requests.getAllValues()).allSatisfy(request -> {
            assertThat(request.getRole()).isEqualTo(AiDecisionChainRole.GPT_FINAL);
            assertThat(request.isInputContractSatisfied()).isFalse();
            assertThat(request.getInputContractFailures()).contains(
                    "EVIDENCE_CONTRACT_INCOMPLETE",
                    "EIGHT_SCORE_CONTRACT_INCOMPLETE",
                    "MULTI_TIMEFRAME_CONTRACT_INCOMPLETE");
        });
    }

    @Test
    void explicitOptionalMissingInputsDoNotBlockTheThreeAiInputContract() {
        DecisionChainBuildInput complete = input();
        complete.scores().get(1).setScoreValue(null);
        complete.scores().get(1).setDescription(
                "coverage=0.0;missingInputs=[verifiedFundingRate];permission=INSUFFICIENT_DATA");
        EvidenceItemVO unavailable = new EvidenceItemVO();
        unavailable.setEvidenceId("evidence-optional-unavailable");
        unavailable.setAnalysisId(complete.analysisId());
        unavailable.setEvidenceType("风险");
        unavailable.setSource("SYSTEM_GENERATED");
        unavailable.setSourceReference("provider=OPTIONAL;state=UNAVAILABLE");
        unavailable.setSourceTraceId("source-trace-optional");
        unavailable.setFreshness("UNAVAILABLE");
        unavailable.setDescription("OPTIONAL_PROVIDER_DATA_UNAVAILABLE");
        List<EvidenceItemVO> evidence = new ArrayList<>(complete.evidence());
        evidence.add(unavailable);
        complete.rulePlan().setSourceGateComplete(false);
        complete.rulePlan().setSourceGateStatus(ExecutionPlanVO.EXECUTION_PLAN_STATUS_INCOMPLETE);
        DecisionChainBuildInput degraded = new DecisionChainBuildInput(
                complete.analysisId(), complete.traceId(), complete.symbol(), complete.timeframe(),
                complete.dataQualityScore(), complete.decision(), complete.rulePlan(), evidence,
                complete.scores(), complete.triggerType(), complete.ownerType(), complete.ownerId(),
                complete.assetId(), complete.ruleVersion(), complete.preview(), complete.requestId(),
                complete.accountRiskSnapshot());
        stubHappyPath();

        service.build(degraded);

        ArgumentCaptor<AiDecisionChainRequest> requests = ArgumentCaptor.forClass(AiDecisionChainRequest.class);
        verify(aiOrchestratorService, org.mockito.Mockito.times(3)).invoke(requests.capture());
        assertThat(requests.getAllValues()).allSatisfy(request -> {
            assertThat(request.isInputContractSatisfied()).isTrue();
            assertThat(request.getInputContractFailures()).doesNotContain(
                    "EVIDENCE_CONTRACT_INCOMPLETE",
                    "EIGHT_SCORE_CONTRACT_INCOMPLETE",
                    "RULE_SOURCE_GATE_INCOMPLETE");
        });
    }

    @Test
    void validatedObservationFinalClearsEveryDirectionalExecutionParameter() {
        stubHappyPath();
        ConflictResolverResultDO observation = conflict(false);
        observation.setPlanModeAfter("OBSERVATION");
        when(conflictResolver.resolveDecisionChain(any(), anyString(), anyString(), any(), any(), anyString()))
                .thenReturn(observation);
        DecisionChainBuildInput input = input();
        input.rulePlan().setEntryZone("100-101");
        input.rulePlan().setStopLoss("95");
        input.rulePlan().setTakeProfitRules("110");
        input.rulePlan().setLeverageSuggestion("1x");
        input.rulePlan().setPositionSuggestion("10%");
        input.rulePlan().setLeverageLimit("1x");
        input.rulePlan().setPositionLimit("10%");

        DecisionChainBuildResult result = service.build(input);

        assertThat(result.finalPlan().getFinalPlan()).isTrue();
        assertThat(result.finalPlan().getFinalPlanMode()).isEqualTo("OBSERVATION");
        assertThat(result.finalPlan().getEntryZone()).isNull();
        assertThat(result.finalPlan().getStopLoss()).isNull();
        assertThat(result.finalPlan().getTakeProfitRules()).isNull();
        assertThat(result.finalPlan().getLeverageSuggestion()).isNull();
        assertThat(result.finalPlan().getPositionSuggestion()).isNull();
        assertThat(result.finalPlan().getLeverageLimit()).isNull();
        assertThat(result.finalPlan().getPositionLimit()).isNull();
        assertThat(result.finalPlan().getExpectedRiskReward()).isNull();
    }

    @Test
    void leverageOnlyEvidenceCannotTriggerThreeAiDespiteMeetingStrengthThreshold() {
        DecisionChainBuildInput complete = input();
        EvidenceItemVO leverage = complete.evidence().get(0);
        leverage.setEvidenceType("杠杆");
        leverage.setDescription("LEVERAGE_SUGGESTION_ONLY");
        leverage.setChangeFromBaseline("VOLATILITY_ADJUSTED_LEVERAGE");
        leverage.setSourceReference("market.leverageSuggestion");
        leverage.setStrength(100.0);
        stubInputGateFallback();

        service.build(complete);

        ArgumentCaptor<AiDecisionChainRequest> requests = ArgumentCaptor.forClass(AiDecisionChainRequest.class);
        verify(aiOrchestratorService).invoke(requests.capture());
        assertThat(requests.getAllValues()).allSatisfy(request -> {
            assertThat(request.isInputContractSatisfied()).isFalse();
            assertThat(request.getInputContractFailures())
                    .contains("SIGNIFICANT_EVIDENCE_CHANGE_MISSING");
        });
    }

    @Test
    void stalePriceEvidenceCannotTriggerThreeAiDespiteMeetingStrengthThreshold() {
        DecisionChainBuildInput complete = input();
        EvidenceItemVO price = complete.evidence().get(0);
        price.setEvidenceType("价格结构");
        price.setStrength(100.0);
        price.setFreshness("STALE");
        stubInputGateFallback();

        service.build(complete);

        ArgumentCaptor<AiDecisionChainRequest> requests = ArgumentCaptor.forClass(AiDecisionChainRequest.class);
        verify(aiOrchestratorService).invoke(requests.capture());
        assertThat(requests.getAllValues()).allSatisfy(request -> {
            assertThat(request.isInputContractSatisfied()).isFalse();
            assertThat(request.getInputContractFailures())
                    .contains("SIGNIFICANT_EVIDENCE_CHANGE_MISSING");
        });
    }

    @Test
    void confusedConflictUsesCanonicalStateTransitionAndBlocksFinalPlan() {
        when(assetPoolService.isOpportunitySource("SYSTEM", 0L, 1L, "BTCUSDT")).thenReturn(true);
        when(assetStateService.transition(
                any(OpportunityStateIdentity.class), any(), anyInt(), anyInt(), any(), any(), anyString(), anyString(), any()))
                .thenAnswer(invocation -> {
                    AssetStateEnum requested = invocation.getArgument(1);
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
                any(OpportunityStateIdentity.class), states.capture(), anyInt(), anyInt(), any(), any(),
                anyString(), anyString(), any());
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
        when(assetPoolService.isOpportunitySource("SYSTEM", 0L, 1L, "BTCUSDT")).thenReturn(true);
        when(assetStateService.transition(
                any(OpportunityStateIdentity.class), any(), anyInt(), anyInt(), any(), any(), anyString(), anyString(), any()))
                .thenReturn(opportunity(AssetStateEnum.CANDIDATE, "ADVISORY_ALLOWED"));
        when(aiOrchestratorService.invoke(any())).thenAnswer(invocation -> success(
                ((AiDecisionChainRequest) invocation.getArgument(0)).getRole()));
        when(conflictResolver.resolveDecisionChain(any(), anyString(), anyString(), any(), any(), anyString()))
                .thenReturn(conflict(false));
        when(ruleValidator.validate(any(), any(), any(), any())).thenReturn(RuleValidationResult.pass());
        when(aiRoleResultsCodec.serializeDecisionChain(any(), any(), any(), any(), any())).thenReturn("{}");
    }

    private void stubInputGateFallback() {
        when(assetPoolService.isOpportunitySource("SYSTEM", 0L, 1L, "BTCUSDT")).thenReturn(true);
        when(assetStateService.transition(
                any(OpportunityStateIdentity.class), any(), anyInt(), anyInt(), any(), any(), anyString(), anyString(), any()))
                .thenReturn(opportunity(AssetStateEnum.CANDIDATE, "ADVISORY_ALLOWED"));
        when(aiOrchestratorService.invoke(any())).thenAnswer(invocation -> {
            AiDecisionChainRequest request = invocation.getArgument(0);
            return AiDecisionChainResult.failed(provider(request.getRole()), request.getRole(),
                    AiProviderCallStatus.INVALID_RESPONSE, "AI_INPUT_CONTRACT_BLOCKED");
        });
        when(aiRoleResultsCodec.serializeDecisionChain(any(), any(), any(), any(), any())).thenReturn("{}");
    }

    private static DecisionChainBuildInput input() {
        DecisionBundleVO decision = new DecisionBundleVO();
        decision.setMarketBiasHierarchy("BULLISH");
        decision.setConfidenceLevel("HIGH");
        decision.setRiskLevel("MEDIUM");
        decision.setRuleMarketBias("BULLISH");
        decision.setValidatedMarketBias("BULLISH");
        decision.setDirectionDataState("READY");
        decision.setRuleConfidence("HIGH");
        decision.setRuleRisk("MEDIUM");
        decision.setConclusionSummary("Rule conclusion");
        decision.setIsWorthOpening(true);
        decision.setAssetState(AssetStateEnum.CANDIDATE);
        decision.setConfusedScore(20);
        decision.setConfusedLowStreak(0);
        decision.setMultiTfConvergence("ALIGNED");
        decision.setMultiTimeframeDetails(multiTimeframeDetails());
        decision.setExpiresAt(OffsetDateTime.of(2026, 8, 12, 0, 0, 0, 0, ZoneOffset.UTC));

        ExecutionPlanVO plan = new ExecutionPlanVO();
        plan.setPlanId("plan-1");
        plan.setExecutionPlanStatus(ExecutionPlanVO.EXECUTION_PLAN_STATUS_INCOMPLETE);
        plan.setSourceGateStatus(ExecutionPlanVO.EXECUTION_PLAN_STATUS_VALID);
        plan.setSourceGateComplete(true);
        plan.setFinalPlan(false);
        plan.setChainStatus("RULE_BASE_ASSESSMENT");
        LocalDateTime feasibilityObservedAt = LocalDateTime.now(ZoneOffset.UTC).minusMinutes(1);
        ExecutionFeasibilityContract.applyVerifiedAssessment(
                plan,
                feasibilityObservedAt,
                feasibilityObservedAt.plusHours(1),
                "[\"evidence-1\"]");
        return new DecisionChainBuildInput(
                "analysis-1", "trace-1", "BTCUSDT", "5m", 90,
                decision, plan, List.of(evidence()), scores(), AnalysisRunTriggerType.ASSET_POOL_SCAN,
                "SYSTEM", 0L, 1L, "FUNDAMENTAL_AI_V4_1", false, "request-1",
                verifiedAccountRisk());
    }

    private static TmAccountRiskSnapshotDO verifiedAccountRisk() {
        LocalDateTime now = LocalDateTime.now(ZoneOffset.UTC);
        TmAccountRiskSnapshotDO snapshot = new TmAccountRiskSnapshotDO();
        snapshot.setId(101L);
        snapshot.setAnalysisId("analysis-1");
        snapshot.setSymbol("BTCUSDT");
        snapshot.setOwnerType("SYSTEM");
        snapshot.setOwnerId(0L);
        snapshot.setAccountRiskStatus("RISK_ALLOWED");
        snapshot.setRiskLevelSnapshot("MEDIUM");
        snapshot.setRiskAllowed(true);
        snapshot.setSourceStatus("VERIFIED");
        snapshot.setObservedAt(now.minusMinutes(1));
        snapshot.setFreshUntil(now.plusMinutes(5));
        snapshot.setMaxAllowedExposure(new java.math.BigDecimal("0.20"));
        snapshot.setMaxAllowedLeverage(new java.math.BigDecimal("10"));
        return snapshot;
    }

    private static DerivativesRiskSnapshot derivativesSnapshot() {
        Instant providerTime = Instant.parse("2026-08-20T06:26:00Z");
        return new DerivativesRiskSnapshot(
                "BTCUSDT", "COINGLASS_V4", providerTime, providerTime.plusSeconds(2),
                providerTime.plusSeconds(62), new BigDecimal("38300000000"),
                new BigDecimal("0.2"), new BigDecimal("1.8"), new BigDecimal("3.1"),
                new BigDecimal("5.4"), new BigDecimal("0.00018"), new BigDecimal("72"),
                new BigDecimal("1.34"), "GLOBAL_ACCOUNT_RATIO",
                new BigDecimal("120000"), new BigDecimal("360000"), new BigDecimal("820000"),
                new BigDecimal("3100000"), new BigDecimal("90000"), new BigDecimal("280000"),
                new BigDecimal("740000"), new BigDecimal("2900000"), new BigDecimal("18"),
                new BigDecimal("0.42"),
                List.of("OPEN_INTEREST", "FUNDING", "LIQUIDATION", "LONG_SHORT_RATIO"),
                List.of(), List.of(), UnifiedSourceStatus.READY, SnapshotFreshnessStatus.FRESH,
                "COMPLETE", List.of(), "trace-1", Map.of(), null);
    }

    private static EvidenceItemVO evidence() {
        EvidenceItemVO item = new EvidenceItemVO();
        item.setEvidenceId("evidence-1");
        item.setAnalysisId("analysis-1");
        item.setEvidenceType("价格结构");
        item.setDirection("BULLISH");
        item.setStrength(80.0);
        item.setConfidence(90.0);
        item.setSource("MARKET_HEURISTIC");
        item.setSourceProvider("TEST_PROVIDER");
        item.setSourceReference("test://market/BTCUSDT/5m");
        item.setSourceTraceId("source-trace-1");
        item.setCurrentValue("101");
        item.setChangeFromBaseline("+1%");
        item.setObservedAt(LocalDateTime.of(2026, 8, 12, 0, 0));
        item.setFreshness("FRESH");
        return item;
    }

    private static List<ScoreItemVO> scores() {
        return List.of(
                score("score-1", "趋势结构分"),
                score("score-2", "资金推动分"),
                score("score-3", "杠杆风险分"),
                score("score-4", "流动性质量分"),
                score("score-5", "情绪温度分"),
                score("score-6", "事件冲击分"),
                score("score-7", "宏观环境分"),
                score("score-8", "证据可信度分"));
    }

    private static ScoreItemVO score(String id, String type) {
        ScoreItemVO item = new ScoreItemVO();
        item.setScoreId(id);
        item.setScoreType(type);
        item.setScoreValue(80.0);
        item.setWeight(1.0);
        item.setDirection("BULLISH");
        return item;
    }

    private static Map<String, Map<String, Object>> multiTimeframeDetails() {
        Map<String, Map<String, Object>> details = new LinkedHashMap<>();
        details.put("4h", timeframe("BULLISH", 85.0));
        details.put("1h", timeframe("BULLISH", 82.0));
        details.put("15m", timeframe("BULLISH", 78.0));
        details.put("5m", timeframe("BULLISH", 75.0));
        return details;
    }

    private static Map<String, Object> timeframe(String direction, double score) {
        Map<String, Object> value = new LinkedHashMap<>();
        value.put("state", "FOUND");
        value.put("direction", direction);
        value.put("trendScore", score);
        value.put("weight", 0.25);
        value.put("barCount", 3);
        return value;
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
        result.setPlanModeBefore("CONFIRMATION");
        result.setPlanModeAfter(confused ? "BLOCKED" : "CONFIRMATION");
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
                    {"coreJudgment":{"marketBias":"BULLISH","opportunityState":"CANDIDATE","text":"rule direction supported"},
                     "supportingEvidenceState":"NONE_FOUND","supportingEvidence":[],
                     "opposingEvidenceState":"NONE_FOUND","opposingEvidence":[],
                     "multiTimeframeExplanation":{"4h":"bullish","1h":"bullish","15m":"setup","5m":"risk filter"},
                     "biasAdjustment":{"before":"BULLISH","after":"BULLISH","reason":"unchanged"},
                     "candidateSummary":{"planMode":"CONFIRMATION","confidence":"HIGH","riskLevel":"MEDIUM",
                     "worthOpening":true,"opportunityType":"TREND_BREAKOUT","recommendedAction":"MANUAL_REVIEW",
                     "entryLogic":"confirmed structure retest","entryZone":"100-101","entrySource":"evidence-1",
                     "entryReason":"verified support boundary","triggerCondition":"15m close above structure",
                     "stopLogic":"rule thesis invalidation","stopZone":"95","stopSource":"evidence-1",
                     "stopReason":"verified structure low","targetLogic":"trend continuation structure",
                     "targetZones":"110 then 120","targetSource":"evidence-1","targetReason":"verified resistance zones",
                     "addPositionCondition":"manual confirmation","reducePositionCondition":"risk increases",
                     "abandonCondition":"source stale","leverageSuggestion":"1x","positionSuggestion":"10%",
                     "riskExplanation":"bounded manual risk","invalidCondition":"close below 95",
                     "invalidationSource":"evidence-1","invalidationReason":"verified structure invalidation boundary",
                     "expectedRiskReward":2.0,"expectedRiskRewardSource":"evidence-1",
                     "expectedRiskRewardReason":"validated distances","validity":"until source expiry",
                     "triggerTimeframe":"15m","holdingHorizon":"1d-3d",
                     "revalidationRule":"refresh evidence","summary":"Candidate only"}}
                    """;
            case GEMINI_REVIEW -> """
                    {"evidenceGapsState":"NONE_FOUND","evidenceGaps":[],
                     "logicConflictsState":"NONE_FOUND","logicConflicts":[],
                     "underestimatedRisksState":"NONE_FOUND","underestimatedRisks":[],
                     "downgradeSuggestion":{"before":"CONFIRMATION","after":"CONFIRMATION","reason":"none","recoveryCondition":"fresh analysis"},
                     "reviewResult":"APPROVE","conflictLevel":"LEVEL_1_CONSISTENT",
                     "finalDirectionImpact":"UNCHANGED","confidenceAdjustment":"UNCHANGED",
                     "riskAdjustment":"UNCHANGED","planModeAdjustment":"UNCHANGED","recoveryCondition":"fresh analysis"}
                    """;
            case GROK_CHALLENGE -> """
                    {"failurePathState":"NO_VERIFIABLE_FAILURE_PATH","failurePaths":[],
                     "opposingScenariosState":"NONE_FOUND","opposingScenarios":[],
                     "externalEventRisksState":"NONE_FOUND","externalEventRisks":[],
                     "microstructureRisksState":"NONE_FOUND","microstructureRisks":[],
                     "watchIndicatorsState":"NONE_FOUND","watchIndicators":[],
                     "challengeSummary":"no verifiable challenge","currentDirectionChallenge":"none",
                     "majorCounterEvidence":false,"conflictLevel":"LEVEL_1_CONSISTENT",
                     "riskAdjustment":"UNCHANGED","planModeImpact":"UNCHANGED"}
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
