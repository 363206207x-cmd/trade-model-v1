package org.example.trademodel.service.impl;

import org.example.trademodel.analysisrun.AnalysisRunTriggerType;
import org.example.trademodel.decisionchain.DecisionChainBuildInput;
import org.example.trademodel.decisionchain.RuleValidationResult;
import org.example.trademodel.entity.ConflictResolverResultDO;
import org.example.trademodel.entity.ExecutionPlanCandidateDO;
import org.example.trademodel.entity.TmAccountRiskSnapshotDO;
import org.example.trademodel.enums.AssetStateEnum;
import org.example.trademodel.service.OpportunityTransitionResult;
import org.example.trademodel.service.support.ExecutionFeasibilityContract;
import org.example.trademodel.vo.DecisionBundleVO;
import org.example.trademodel.vo.ExecutionPlanVO;
import org.example.trademodel.vo.EvidenceItemVO;
import org.example.trademodel.vo.ScoreItemVO;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@Tag("core-regression")
class DecisionChainRuleValidatorImplTest {

    private final DecisionChainRuleValidatorImpl validator = new DecisionChainRuleValidatorImpl();

    @Test
    void validatedCandidateMustRemainInsideEveryRuleBoundary() {
        Fixture fixture = fixture();

        RuleValidationResult result = validator.validate(
                fixture.input(), fixture.opportunity(), fixture.candidate(), fixture.conflict());

        assertThat(result.passed()).isTrue();
        assertThat(result.reasons()).isEmpty();
    }

    @Test
    void candidateCannotBecomeFinalMutateStateOrCreatePosition() {
        Fixture fixture = fixture();
        fixture.candidate().setNotFinalPlan(false);
        fixture.candidate().setRecommendedAction("AUTO_ORDER");

        RuleValidationResult result = validator.validate(
                fixture.input(), fixture.opportunity(), fixture.candidate(), fixture.conflict());

        assertThat(result.passed()).isFalse();
        assertThat(result.reasons()).contains(
                "CANDIDATE_SAFETY_BOUNDARY_VIOLATION",
                "AUTOMATIC_TRADING_ACTION_FORBIDDEN");
    }

    @Test
    void candidateCannotCrossRuleDirectionEvenThoughGptOwnsDetailedBoundaries() {
        Fixture fixture = fixture();
        fixture.candidate().setCandidateDirection("BEARISH");
        fixture.candidate().setStopLoss("90");
        fixture.conflict().setRuleDirectionPreserved(false);

        RuleValidationResult result = validator.validate(
                fixture.input(), fixture.opportunity(), fixture.candidate(), fixture.conflict());

        assertThat(result.reasons()).contains(
                "RULE_DIRECTION_FAMILY_MISMATCH",
                "CONFLICT_RESOLVER_RULE_DIRECTION_NOT_PRESERVED");
        assertThat(result.reasons()).doesNotContain("STOP_LOSS_NOT_RULE_VALIDATED");
    }

    @Test
    void candidateBoundarySourcesMustReferenceCurrentAnalysisEvidence() {
        Fixture fixture = fixture();
        fixture.candidate().setEntrySource("invented://entry");
        fixture.candidate().setExpectedRiskRewardSource("invented://rr");

        RuleValidationResult result = validator.validate(
                fixture.input(), fixture.opportunity(), fixture.candidate(), fixture.conflict());

        assertThat(result.reasons()).contains(
                "ENTRY_SOURCE_NOT_TRACEABLE",
                "EXPECTED_RISK_REWARD_SOURCE_NOT_TRACEABLE");
    }

    @Test
    void candidateMustCarryEveryFrozenManualAdjustmentCondition() {
        Fixture fixture = fixture();
        fixture.candidate().setAddPositionCondition(null);
        fixture.candidate().setReducePositionCondition(" ");
        fixture.candidate().setAbandonCondition(null);

        RuleValidationResult result = validator.validate(
                fixture.input(), fixture.opportunity(), fixture.candidate(), fixture.conflict());

        assertThat(result.reasons()).contains(
                "ADD_POSITION_CONDITION_MISSING",
                "REDUCE_POSITION_CONDITION_MISSING",
                "ABANDON_CONDITION_MISSING");
    }

    @Test
    void finalPlanRequiresCompleteValidityWindowAndRiskParameters() {
        Fixture fixture = fixture();
        fixture.candidate().setValidUntil(fixture.candidate().getValidFrom());
        fixture.candidate().setLeverageSuggestion(null);
        fixture.candidate().setPositionSuggestion(null);

        RuleValidationResult result = validator.validate(
                fixture.input(), fixture.opportunity(), fixture.candidate(), fixture.conflict());

        assertThat(result.reasons()).contains(
                "VALIDITY_WINDOW_INVALID",
                "LEVERAGE_SUGGESTION_MISSING",
                "POSITION_SUGGESTION_MISSING");
    }

    @Test
    void finalPlanRequiresCompleteIdentityAndFrozenInputLineage() {
        Fixture fixture = fixture();
        fixture.candidate().setAssetId(null);
        fixture.candidate().setScoreRefsJson("[\"score-1\"]");
        fixture.conflict().setTraceId("trace-other");

        RuleValidationResult result = validator.validate(
                fixture.input(), fixture.opportunity(), fixture.candidate(), fixture.conflict());

        assertThat(result.reasons()).contains(
                "CANDIDATE_ASSET_ID_MISSING",
                "CANDIDATE_ASSET_ID_MISMATCH",
                "SCORE_REFS_INCOMPLETE",
                "RESOLVER_TRACE_ID_MISMATCH");
    }

    @Test
    void worthOpeningMustMatchFinalPlanModeDirectionPermission() {
        Fixture fixture = fixture();
        fixture.candidate().setWorthOpening(false);

        RuleValidationResult result = validator.validate(
                fixture.input(), fixture.opportunity(), fixture.candidate(), fixture.conflict());

        assertThat(result.reasons()).contains("WORTH_OPENING_PLAN_MODE_MISMATCH");
    }

    @Test
    void ruleFallbackCandidateCannotBecomeFinal() {
        Fixture fixture = fixture();
        fixture.candidate().setCandidateSource("RULE_FALLBACK");

        RuleValidationResult result = validator.validate(
                fixture.input(), fixture.opportunity(), fixture.candidate(), fixture.conflict());

        assertThat(result.reasons()).contains("GPT_CANDIDATE_REQUIRED");
    }

    @Test
    void unknownOrMorePermissiveResolverOutputsFailClosed() {
        Fixture fixture = fixture();
        fixture.conflict().setPlanModeAfter("UNRECOGNIZED");
        fixture.conflict().setConfidenceAfter("SUPER_HIGH");
        fixture.conflict().setRiskAfter("NONE");

        RuleValidationResult unknown = validator.validate(
                fixture.input(), fixture.opportunity(), fixture.candidate(), fixture.conflict());
        assertThat(unknown.reasons()).contains(
                "FINAL_PLAN_MODE_UNKNOWN",
                "FINAL_CONFIDENCE_UNKNOWN",
                "FINAL_RISK_UNKNOWN");

        Fixture watchOnly = fixture();
        watchOnly.input().decision().setIsWorthOpening(false);
        watchOnly.candidate().setWorthOpening(false);
        watchOnly.candidate().setRulePlanMode("OBSERVATION");
        watchOnly.candidate().setPlanMode("OBSERVATION");
        watchOnly.conflict().setPlanModeBefore("OBSERVATION");
        watchOnly.conflict().setPlanModeAfter("PREPARATION");
        RuleValidationResult permissive = validator.validate(
                watchOnly.input(), watchOnly.opportunity(), watchOnly.candidate(), watchOnly.conflict());
        assertThat(permissive.reasons()).contains("FINAL_PLAN_MODE_MORE_PERMISSIVE_THAN_RULE");
    }

    @Test
    void confusedOrIneligibleOpportunityCannotProduceFinalPlan() {
        Fixture fixture = fixture();
        fixture.input().decision().setConfusedScore(80);
        OpportunityTransitionResult confused = new OpportunityTransitionResult(
                "opp-btc", "BTCUSDT", AssetStateEnum.CANDIDATE, AssetStateEnum.CONFUSED,
                true, false, "CONFLICT", "CONFUSED", "BLOCKED", LocalDateTime.now());

        RuleValidationResult result = validator.validate(
                fixture.input(), confused, fixture.candidate(), fixture.conflict());

        assertThat(result.reasons()).contains(
                "OPPORTUNITY_STATE_NOT_ELIGIBLE",
                "OPPORTUNITY_EXECUTION_PERMISSION_BLOCKED",
                "CONFUSED_BLOCKED");
    }

    @Test
    void finalPlanRequiresFrozenMultiTimeframeContractAndSupportedTriggerTimeframe() {
        Fixture fixture = fixture();
        fixture.candidate().setTriggerTimeframe("1m");
        fixture.candidate().setAnalysisTimeframesJson("{\"1h\":\"only one timeframe\"}");

        RuleValidationResult result = validator.validate(
                fixture.input(), fixture.opportunity(), fixture.candidate(), fixture.conflict());

        assertThat(result.reasons()).contains(
                "TRIGGER_TIMEFRAME_UNSUPPORTED",
                "MULTI_TIMEFRAME_CONTRACT_INCOMPLETE");
    }

    @Test
    void finalPlanRejectsAnalysisOutsideFrozenMultiTimeframeSet() {
        Fixture fixture = fixture();
        DecisionChainBuildInput source = fixture.input();
        DecisionChainBuildInput unsupported = new DecisionChainBuildInput(
                source.analysisId(), source.traceId(), source.symbol(), "1m",
                source.dataQualityScore(), source.decision(), source.rulePlan(),
                source.evidence(), source.scores(), source.triggerType(), source.ownerType(),
                source.ownerId(), source.assetId(), source.ruleVersion(), source.preview(),
                source.requestId(), source.accountRiskSnapshot());

        RuleValidationResult result = validator.validate(
                unsupported, fixture.opportunity(), fixture.candidate(), fixture.conflict());

        assertThat(result.reasons()).contains("ANALYSIS_TIMEFRAME_UNSUPPORTED");
    }

    @Test
    void candidateStateCannotProduceUntriggeredConfirmationPlan() {
        Fixture fixture = fixture();
        OpportunityTransitionResult candidateState = opportunity(AssetStateEnum.CANDIDATE);

        RuleValidationResult result = validator.validate(
                fixture.input(), candidateState, fixture.candidate(), fixture.conflict());

        assertThat(result.reasons()).contains("OPPORTUNITY_STATE_PLAN_MODE_COMBINATION_INVALID");
    }

    @Test
    void waitingTriggerOnlyPermitsPreparationBeforeTrigger() {
        Fixture fixture = fixture();
        fixture.candidate().setRulePlanMode("PREPARATION");
        fixture.candidate().setPlanMode("PREPARATION");
        fixture.input().decision().setRulePlanMode("PREPARATION");
        fixture.conflict().setPlanModeBefore("PREPARATION");
        fixture.conflict().setPlanModeAfter("PREPARATION");

        RuleValidationResult result = validator.validate(
                fixture.input(), opportunity(AssetStateEnum.WAITING_TRIGGER),
                fixture.candidate(), fixture.conflict());

        assertThat(result.passed()).isTrue();
    }

    @Test
    void weakBiasDoesNotMechanicallyOverrideTriggeredRuleValidation() {
        Fixture fixture = fixture();
        fixture.input().decision().setRuleMarketBias("WEAK_BULLISH");
        fixture.input().decision().setMarketBiasHierarchy("WEAK_BULLISH");
        fixture.candidate().setRuleDirection("WEAK_BULLISH");
        fixture.candidate().setCandidateDirection("WEAK_BULLISH");
        fixture.conflict().setRuleDirection("WEAK_BULLISH");
        fixture.conflict().setBiasBefore("WEAK_BULLISH");
        fixture.conflict().setBiasAfter("WEAK_BULLISH");

        RuleValidationResult result = validator.validate(
                fixture.input(), fixture.opportunity(), fixture.candidate(), fixture.conflict());

        assertThat(result.passed()).isTrue();
    }

    @Test
    void rangeAndWaitRemainNonDirectional() {
        for (String bias : List.of("RANGE", "WAIT")) {
            Fixture fixture = fixture();
            fixture.input().decision().setRuleMarketBias(bias);
            fixture.input().decision().setMarketBiasHierarchy(bias);
            fixture.candidate().setRuleDirection(bias);
            fixture.candidate().setCandidateDirection(bias);
            fixture.conflict().setRuleDirection(bias);
            fixture.conflict().setBiasBefore(bias);
            fixture.conflict().setBiasAfter(bias);

            RuleValidationResult result = validator.validate(
                    fixture.input(), fixture.opportunity(), fixture.candidate(), fixture.conflict());

            assertThat(result.reasons()).contains("BIAS_PLAN_MODE_COMBINATION_INVALID");
        }
    }

    @Test
    void highRiskOpportunityCannotRetainConfirmationIntensity() {
        Fixture fixture = fixture();

        RuleValidationResult result = validator.validate(
                fixture.input(), opportunity(AssetStateEnum.HIGH_RISK),
                fixture.candidate(), fixture.conflict());

        assertThat(result.reasons()).contains("OPPORTUNITY_STATE_PLAN_MODE_COMBINATION_INVALID");
    }

    @Test
    void resolverBiasMustBeAuditableSameFamilyCandidateResult() {
        Fixture fixture = fixture();
        fixture.conflict().setBiasAfter("BEARISH");

        RuleValidationResult result = validator.validate(
                fixture.input(), fixture.opportunity(), fixture.candidate(), fixture.conflict());

        assertThat(result.reasons()).contains(
                "RESOLVER_BIAS_AFTER_CANDIDATE_MISMATCH",
                "FINAL_MARKET_BIAS_FAMILY_MISMATCH");
    }

    @Test
    void sameFamilyBiasDowngradeRequiresReasonAndSupportsReducedPlan() {
        Fixture fixture = fixture();
        fixture.input().decision().setRuleMarketBias("STRONG_BULLISH");
        fixture.input().decision().setMarketBiasHierarchy("STRONG_BULLISH");
        fixture.input().decision().setRulePlanMode("REDUCED");
        fixture.candidate().setRuleDirection("STRONG_BULLISH");
        fixture.candidate().setCandidateDirection("BULLISH");
        fixture.candidate().setRulePlanMode("REDUCED");
        fixture.candidate().setPlanMode("REDUCED");
        fixture.conflict().setRuleDirection("STRONG_BULLISH");
        fixture.conflict().setBiasBefore("STRONG_BULLISH");
        fixture.conflict().setBiasAfter("BULLISH");
        fixture.conflict().setPlanModeBefore("REDUCED");
        fixture.conflict().setPlanModeAfter("REDUCED");
        fixture.conflict().setAdjustmentReason("verified evidence supports one-level downgrade");

        RuleValidationResult result = validator.validate(
                fixture.input(), fixture.opportunity(), fixture.candidate(), fixture.conflict());

        assertThat(result.passed()).isTrue();
    }

    private static Fixture fixture() {
        OffsetDateTime expiresAt = OffsetDateTime.of(2026, 8, 12, 0, 0, 0, 0, ZoneOffset.UTC);
        DecisionBundleVO decision = new DecisionBundleVO();
        decision.setMarketBiasHierarchy("BULLISH");
        decision.setConfidenceLevel("HIGH");
        decision.setRiskLevel("MEDIUM");
        decision.setRuleMarketBias("BULLISH");
        decision.setRuleConfidence("HIGH");
        decision.setRuleRisk("MEDIUM");
        decision.setRulePlanMode("CONFIRMATION");
        decision.setRuleCanExecute(true);
        decision.setIsWorthOpening(true);
        decision.setConfusedScore(20);
        decision.setExpiresAt(expiresAt);

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
        plan.setPositionSuggestion("10%");
        plan.setInvalidCondition("close below 95");
        LocalDateTime feasibilityObservedAt = LocalDateTime.now(ZoneOffset.UTC).minusMinutes(1);
        ExecutionFeasibilityContract.applyVerifiedAssessment(
                plan,
                feasibilityObservedAt,
                feasibilityObservedAt.plusHours(1),
                "[\"evidence-1\"]");

        EvidenceItemVO evidence = new EvidenceItemVO();
        evidence.setEvidenceId("evidence-1");
        evidence.setAnalysisId("analysis-1");
        evidence.setSource("VERIFIED_MARKET_SOURCE");
        evidence.setSourceReference("source-1");
        evidence.setSourceTraceId("source-trace-1");
        evidence.setObservedAt(LocalDateTime.now(ZoneOffset.UTC).minusMinutes(1));
        evidence.setFreshness("FRESH");

        DecisionChainBuildInput input = new DecisionChainBuildInput(
                "analysis-1", "trace-1", "BTCUSDT", "5m", 90,
                decision, plan, List.of(evidence), frozenScores(), AnalysisRunTriggerType.ASSET_POOL_SCAN,
                "SYSTEM", 0L, 1L, "FUNDAMENTAL_AI_V4_1", false, "request-1",
                verifiedAccountRisk());
        OpportunityTransitionResult opportunity = new OpportunityTransitionResult(
                "opp-btc", "BTCUSDT", AssetStateEnum.WAITING_TRIGGER, AssetStateEnum.TRIGGERED,
                true, false, "PROMOTED", "ASSET_POOL_SCAN", "ADVISORY_ALLOWED", LocalDateTime.now());

        ExecutionPlanCandidateDO candidate = new ExecutionPlanCandidateDO();
        candidate.setCandidateId("candidate-1");
        candidate.setOpportunityId("opp-btc");
        candidate.setAnalysisId("analysis-1");
        candidate.setTraceId("trace-1");
        candidate.setAssetId(1L);
        candidate.setRuleVersion("FUNDAMENTAL_AI_V4_1");
        candidate.setRuleDirection("BULLISH");
        candidate.setRuleConfidence("HIGH");
        candidate.setRuleRisk("MEDIUM");
        candidate.setRulePlanMode("CONFIRMATION");
        candidate.setRuleCanExecute(true);
        candidate.setCandidateDirection("BULLISH");
        candidate.setPlanMode("CONFIRMATION");
        candidate.setConfidenceLevel("HIGH");
        candidate.setRiskLevel("MEDIUM");
        candidate.setWorthOpening(true);
        candidate.setCandidateSource("GPT_FINAL");
        candidate.setRecommendedAction("MANUAL_REVIEW");
        candidate.setOpportunityType("TREND_CONTINUATION");
        candidate.setEntryLogic("verified structure continuation");
        candidate.setEntryZone("100-101");
        candidate.setEntrySource("evidence-1");
        candidate.setEntryReason("verified source boundary");
        candidate.setTriggerCondition("manual confirmation after source refresh");
        candidate.setStopLogic("rule invalidation boundary");
        candidate.setStopLoss("95");
        candidate.setStopSource("evidence-1");
        candidate.setStopReason("structure invalidated below 95");
        candidate.setTargetLogic("rule risk-reward targets");
        candidate.setTakeProfitRules("110 then 120");
        candidate.setTargetSource("evidence-1");
        candidate.setTargetReason("verified target zones");
        candidate.setAddPositionCondition("manual add only after fresh trigger confirmation");
        candidate.setReducePositionCondition("manual reduce when risk evidence increases");
        candidate.setAbandonCondition("manual abandon when the entry structure invalidates");
        candidate.setLeverageSuggestion("1x");
        candidate.setPositionSuggestion("10%");
        candidate.setInvalidCondition("close below 95");
        candidate.setInvalidationSource("evidence-1");
        candidate.setInvalidationReason("verified structure invalidation boundary");
        candidate.setRiskExplanation("bounded manual decision risk");
        candidate.setExpectedRiskReward(new BigDecimal("2.00"));
        candidate.setExpectedRiskRewardSource("evidence-1");
        candidate.setExpectedRiskRewardReason("validated entry stop target relation");
        candidate.setAnalysisTimeframesJson("""
                {"4h":"direction","1h":"structure","15m":"trigger","5m":"risk filter"}
                """);
        candidate.setTriggerTimeframe("5m");
        candidate.setHoldingHorizon("intraday");
        candidate.setRevalidationRule("refresh verified evidence before expiry");
        candidate.setEvidenceRefsJson("[\"evidence-1\"]");
        candidate.setScoreRefsJson("[\"score-1\",\"score-2\",\"score-3\",\"score-4\","
                + "\"score-5\",\"score-6\",\"score-7\",\"score-8\"]");
        candidate.setSourceRefsJson("[\"source-1\"]");
        candidate.setValidity(expiresAt.toString());
        candidate.setValidFrom(LocalDateTime.now(ZoneOffset.UTC).minusMinutes(1));
        candidate.setValidUntil(LocalDateTime.now(ZoneOffset.UTC).plusHours(1));
        candidate.setDataQuality(90);
        candidate.setAccountRiskSnapshotId(101L);
        candidate.setNotFinalPlan(true);
        candidate.setNotStateMachineMutation(true);
        candidate.setNotUserPositionCreation(true);

        ConflictResolverResultDO conflict = new ConflictResolverResultDO();
        conflict.setResolverResultId("resolver-1");
        conflict.setCandidateId("candidate-1");
        conflict.setAnalysisId("analysis-1");
        conflict.setTraceId("trace-1");
        conflict.setRuleDirection("BULLISH");
        conflict.setRuleConfidence("HIGH");
        conflict.setRuleRisk("MEDIUM");
        conflict.setPlanModeBefore("CONFIRMATION");
        conflict.setPlanModeAfter("CONFIRMATION");
        conflict.setConfidenceBefore("HIGH");
        conflict.setConfidenceAfter("HIGH");
        conflict.setRiskBefore("MEDIUM");
        conflict.setRiskAfter("MEDIUM");
        conflict.setBiasBefore("BULLISH");
        conflict.setBiasAfter("BULLISH");
        conflict.setRuleDirectionPreserved(true);
        conflict.setConfusedDecision(false);
        return new Fixture(input, opportunity, candidate, conflict);
    }

    private static List<ScoreItemVO> frozenScores() {
        return List.of(
                score("score-1", "趋势结构分"),
                score("score-2", "资金推动分"),
                score("score-3", "杠杆风险分"),
                score("score-4", "流动性质量分"),
                score("score-5", "情绪温度分"),
                score("score-6", "事件冲击分"),
                score("score-7", "宏观环境分"),
                score("score-8", "综合可信度分"));
    }

    private static ScoreItemVO score(String id, String type) {
        ScoreItemVO score = new ScoreItemVO();
        score.setScoreId(id);
        score.setScoreType(type);
        score.setScoreValue(80D);
        return score;
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
        snapshot.setMaxAllowedExposure(new BigDecimal("0.20"));
        snapshot.setMaxAllowedLeverage(new BigDecimal("10"));
        return snapshot;
    }

    private static OpportunityTransitionResult opportunity(AssetStateEnum state) {
        return new OpportunityTransitionResult(
                "opp-btc", "BTCUSDT", state, state, false, false,
                "STATE_CONTRACT_TEST", "TEST", "ADVISORY_ALLOWED", LocalDateTime.now());
    }

    private record Fixture(DecisionChainBuildInput input,
                           OpportunityTransitionResult opportunity,
                           ExecutionPlanCandidateDO candidate,
                           ConflictResolverResultDO conflict) {
    }
}
