package org.example.trademodel.service;

import org.example.trademodel.dto.planboundary.BoundaryCandidateDTO;
import org.example.trademodel.dto.planboundary.BoundaryEntryDTO;
import org.example.trademodel.dto.planboundary.BoundarySourceFieldsDTO;
import org.example.trademodel.dto.planboundary.BoundaryStatusEnum;
import org.example.trademodel.dto.planboundary.BoundaryStopDTO;
import org.example.trademodel.dto.planboundary.BoundaryTakeProfitLevelDTO;
import org.example.trademodel.dto.planboundary.DerivativesRiskContextDTO;
import org.example.trademodel.dto.planboundary.RuntimeKlineContextDTO;
import org.example.trademodel.dto.planboundary.SourceTraceDTO;
import org.example.trademodel.dto.planboundary.SourceTraceFallbackStatusEnum;
import org.example.trademodel.enums.RecheckStatusEnum;
import org.example.trademodel.service.dashboard.DefaultExecutionPlanDisplayAdapter;
import org.example.trademodel.service.impl.BoundaryCandidateServiceImpl;
import org.example.trademodel.service.impl.DefaultSourceAssembler;
import org.example.trademodel.vo.DashboardDetailResponseVO;
import org.example.trademodel.vo.DecisionResultVO;
import org.example.trademodel.vo.ExecutionPlanVO;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class P17LocalFixtureFailClosedTest {

    private final DefaultSourceAssembler sourceAssembler = new DefaultSourceAssembler();
    private final BoundaryCandidateServiceImpl boundaryCandidateService = new BoundaryCandidateServiceImpl(sourceAssembler);
    private final DefaultExecutionPlanDisplayAdapter executionPlanDisplayAdapter = new DefaultExecutionPlanDisplayAdapter();

    @Test
    void completeLocalFixtureCanReachValidButRemainsReviewOnlyAcrossReadinessGates() {
        SourceTraceDTO sourceTrace = sourceAssembler.assembleSourceTrace(
                completeRuntimeKlineContext(),
                completeDerivativesRiskContext()
        );

        BoundaryCandidateDTO candidate = boundaryCandidateService.evaluateBoundaryCandidate(
                "BTCUSDT",
                "1h",
                completeRuntimeKlineContext(),
                completeDerivativesRiskContext(),
                completeEntry(),
                completeStop(),
                List.of(completeTakeProfitLevel()),
                completeSourceFields(),
                BigDecimal.valueOf(90)
        );
        DashboardDetailResponseVO.ExecutionPlanDisplayVO executionDisplay = executionPlanDisplayAdapter.build(
                lowRiskDecision(),
                boundaryDisplay(candidate.getBoundaryStatus()),
                null,
                sourceTrace,
                readyRiskActionGuard()
        );
        RuleBaseOutput ruleOutput = optimisticRuleEngine().execute(new DecisionContext(), sourceTrace, readyRiskActionGuard());

        assertThat(sourceTrace.hasRequiredBoundarySources()).isTrue();
        assertThat(candidate.getBoundaryStatus()).isEqualTo(BoundaryStatusEnum.VALID);
        assertThat(candidate.isManualReviewRequired()).isTrue();
        assertThat(candidate.isNotTradeInstruction()).isTrue();
        assertThat(executionDisplay.getExecutionPlanStatus()).isEqualTo("READY_REVIEW_ONLY");
        assertThat(executionDisplay.getNotExecutableReason()).isEqualTo("MANUAL_REVIEW_REQUIRED");
        assertThat(executionDisplay.getManualReviewRequired()).isTrue();
        assertThat(executionDisplay.getNotTradeInstruction()).isTrue();
        assertThat(ruleOutput.isCanExecute()).isFalse();
        assertThat(ruleOutput.getPlanMode()).isEqualTo(ExecutionPlanVO.PLAN_MODE_ADVISORY);
    }

    @Test
    void missingSourceTraceFixtureFailsClosedBeforeValidCandidateOrExecutionReadiness() {
        RuntimeKlineContextDTO runtime = completeRuntimeKlineContext();
        runtime.setEntryPriceSource(null);
        SourceTraceDTO sourceTrace = sourceAssembler.assembleSourceTrace(runtime, completeDerivativesRiskContext());

        BoundaryCandidateDTO candidate = boundaryCandidateService.evaluateBoundaryCandidate(
                "BTCUSDT",
                "1h",
                sourceTrace,
                completeEntry(),
                completeStop(),
                List.of(completeTakeProfitLevel()),
                completeSourceFields(),
                BigDecimal.valueOf(90)
        );
        DashboardDetailResponseVO.ExecutionPlanDisplayVO executionDisplay = executionPlanDisplayAdapter.build(
                lowRiskDecision(),
                validBoundaryDisplay(),
                null,
                sourceTrace,
                readyRiskActionGuard()
        );
        RuleBaseOutput ruleOutput = optimisticRuleEngine().execute(new DecisionContext(), sourceTrace, readyRiskActionGuard());

        assertThat(sourceTrace.getFallbackStatus()).isEqualTo(SourceTraceFallbackStatusEnum.INCOMPLETE);
        assertThat(sourceTrace.getMissingFields()).contains("entryPriceSource");
        assertThat(sourceTrace.hasRequiredBoundarySources()).isFalse();
        assertThat(candidate.getBoundaryStatus()).isEqualTo(BoundaryStatusEnum.INCOMPLETE);
        assertThat(candidate.isManualReviewRequired()).isTrue();
        assertThat(candidate.isNotTradeInstruction()).isTrue();
        assertThat(executionDisplay.getExecutionPlanStatus()).isEqualTo("INCOMPLETE");
        assertThat(executionDisplay.getNotExecutableReason()).isEqualTo("SOURCE_TRACE_INCOMPLETE");
        assertThat(ruleOutput.isCanExecute()).isFalse();
        assertThat(ruleOutput.getConfidenceLevel()).isEqualTo(ExecutionPlanVO.READINESS_INCOMPLETE);
    }

    @Test
    void missingDerivativesRiskContextFixtureFallsBackToWatchOnlyAndReviewOnly() {
        SourceTraceDTO sourceTrace = sourceAssembler.assembleSourceTrace(completeRuntimeKlineContext(), null);

        BoundaryCandidateDTO candidate = boundaryCandidateService.evaluateBoundaryCandidate(
                "BTCUSDT",
                "1h",
                completeRuntimeKlineContext(),
                null,
                completeEntry(),
                completeStop(),
                List.of(completeTakeProfitLevel()),
                completeSourceFields(),
                BigDecimal.valueOf(90)
        );
        DashboardDetailResponseVO.ExecutionPlanDisplayVO executionDisplay = executionPlanDisplayAdapter.build(
                lowRiskDecision(),
                validBoundaryDisplay(),
                null,
                sourceTrace,
                readyRiskActionGuard()
        );

        assertThat(sourceTrace.getFallbackStatus()).isEqualTo(SourceTraceFallbackStatusEnum.WATCH_ONLY);
        assertThat(sourceTrace.getMissingFields()).contains("derivativesRiskContext");
        assertThat(candidate.getBoundaryStatus()).isEqualTo(BoundaryStatusEnum.WATCH_ONLY);
        assertThat(candidate.isManualReviewRequired()).isTrue();
        assertThat(candidate.isNotTradeInstruction()).isTrue();
        assertThat(executionDisplay.getExecutionPlanStatus()).isEqualTo("WATCH_ONLY");
        assertThat(executionDisplay.getNotExecutableReason()).isEqualTo("SOURCE_TRACE_WATCH_ONLY");
    }

    @Test
    void stampedeRiskFixtureBlocksValidExecutionAndOpportunityPush() {
        DashboardDetailResponseVO.RiskActionGuardDisplayVO guard = readyRiskActionGuard();
        guard.setStampedeDetected(true);

        BoundaryCandidateDTO candidate = boundaryCandidateService.evaluateBoundaryCandidate(
                "BTCUSDT",
                "1h",
                completeSourceTrace(),
                completeEntry(),
                completeStop(),
                List.of(completeTakeProfitLevel()),
                completeSourceFields(),
                BigDecimal.valueOf(90),
                guard
        );
        DashboardDetailResponseVO.ExecutionPlanDisplayVO executionDisplay = executionPlanDisplayAdapter.build(
                lowRiskDecision(),
                validBoundaryDisplay(),
                null,
                completeSourceTrace(),
                guard
        );
        RuleBaseOutput ruleOutput = optimisticRuleEngine().execute(new DecisionContext(), completeSourceTrace(), guard);

        assertThat(candidate.getBoundaryStatus()).isEqualTo(BoundaryStatusEnum.WATCH_ONLY);
        assertThat(candidate.getBlockingReasons()).contains("stampede risk detected");
        assertThat(executionDisplay.getExecutionPlanStatus()).isEqualTo("WATCH_ONLY");
        assertThat(executionDisplay.getNotExecutableReason()).isEqualTo("STAMPEDE_RISK_REVIEW_ONLY");
        assertThat(ruleOutput.isCanExecute()).isFalse();
        assertThat(ruleOutput.getRiskLevel()).isEqualTo("STAMPEDE_RISK_REVIEW_ONLY");
        assertThat(guard.getOpportunityPushAllowed()).isFalse();
        assertThat(guard.getNewPositionAllowed()).isFalse();
        assertThat(guard.getReverseTradeAllowed()).isFalse();
    }

    @Test
    void liquidityAndWickRiskFixturesRemainWatchOnlyAndDoNotInferOneShotExitOrReversal() {
        DashboardDetailResponseVO.RiskActionGuardDisplayVO liquidityMissing = readyRiskActionGuard();
        liquidityMissing.setLiquidityState("BACKEND_PENDING");
        DashboardDetailResponseVO.RiskActionGuardDisplayVO wickOnly = readyRiskActionGuard();
        wickOnly.setWickOnlyRisk(true);

        BoundaryCandidateDTO liquidityCandidate = boundaryCandidateService.evaluateBoundaryCandidate(
                "BTCUSDT", "1h", completeSourceTrace(), completeEntry(), completeStop(),
                List.of(completeTakeProfitLevel()), completeSourceFields(), BigDecimal.valueOf(90), liquidityMissing
        );
        DashboardDetailResponseVO.ExecutionPlanDisplayVO liquidityDisplay = executionPlanDisplayAdapter.build(
                lowRiskDecision(), validBoundaryDisplay(), null, completeSourceTrace(), liquidityMissing
        );
        BoundaryCandidateDTO wickCandidate = boundaryCandidateService.evaluateBoundaryCandidate(
                "BTCUSDT", "1h", completeSourceTrace(), completeEntry(), completeStop(),
                List.of(completeTakeProfitLevel()), completeSourceFields(), BigDecimal.valueOf(90), wickOnly
        );
        DashboardDetailResponseVO.ExecutionPlanDisplayVO wickDisplay = executionPlanDisplayAdapter.build(
                lowRiskDecision(), validBoundaryDisplay(), null, completeSourceTrace(), wickOnly
        );

        assertThat(liquidityCandidate.getBoundaryStatus()).isEqualTo(BoundaryStatusEnum.WATCH_ONLY);
        assertThat(liquidityDisplay.getNotExecutableReason()).isEqualTo("LIQUIDITY_CONTEXT_MISSING");
        assertThat(liquidityMissing.getMarketOrderExitAllowed()).isFalse();
        assertThat(wickCandidate.getBoundaryStatus()).isEqualTo(BoundaryStatusEnum.WATCH_ONLY);
        assertThat(wickDisplay.getNotExecutableReason()).isEqualTo("WICK_ONLY_RISK_REVIEW_ONLY");
        assertThat(wickOnly.getReverseTradeAllowed()).isFalse();
        assertThat(wickOnly.getNewPositionAllowed()).isFalse();
    }

    @Test
    void pushRecheckReviewPassedNamesRemainReviewLabelsOnly() {
        assertThat(PushRecheckStatusContract.toPushStatus(RecheckStatusEnum.REVIEW_PASSED))
                .isEqualTo("RECHECK_REVIEW_PASSED");
        assertThat(PushRecheckStatusContract.toReviewTag(RecheckStatusEnum.REVIEW_PASSED))
                .isEqualTo(PushRecheckStatusContract.ReviewTag.PASS);
        assertThat(PushRecheckStatusContract.isPendingPushStatusForScheduler("RECHECK_VALID_EXECUTABLE"))
                .isFalse();
        assertThat(PushRecheckStatusContract.toReviewTagByPushStatus("RECHECK_RISK_BLOCKED"))
                .isEqualTo(PushRecheckStatusContract.ReviewTag.BLOCKED);
    }

    @Test
    void fixtureCatalogDocumentsAllP17FailClosedScenarios() throws IOException {
        InputStream stream = getClass().getResourceAsStream("/planboundary/p17-local-fixture-fail-closed-cases.csv");
        assertThat(stream).isNotNull();
        String content = new String(stream.readAllBytes(), StandardCharsets.UTF_8);

        assertThat(content).contains("P17-COMPLETE-REVIEW-ONLY");
        assertThat(content).contains("P17-MISSING-SOURCE-TRACE");
        assertThat(content).contains("P17-MISSING-DERIVATIVES-CONTEXT");
        assertThat(content).contains("P17-STAMPEDE-RISK");
        assertThat(content).contains("P17-LIQUIDITY-MISSING");
        assertThat(content).contains("P17-WICK-ONLY-RISK");
        assertThat(content).contains("P17-PUSH-RECHECK-NAMING");
    }

    private SourceTraceDTO completeSourceTrace() {
        return sourceAssembler.assembleSourceTrace(completeRuntimeKlineContext(), completeDerivativesRiskContext());
    }

    private RuntimeKlineContextDTO completeRuntimeKlineContext() {
        RuntimeKlineContextDTO context = new RuntimeKlineContextDTO();
        context.setSymbol("BTCUSDT");
        context.setTimeframe("1h");
        context.setLatestPrice(BigDecimal.valueOf(68100));
        context.setDataQualityScore(BigDecimal.valueOf(90));
        context.setEntryPriceSource(BigDecimal.valueOf(68000));
        context.setEntrySourceType("support");
        context.setEntrySourceTimeframe("1h");
        context.setEntrySourceReason("support retest");
        context.setEntrySourceRef("entry-1");
        context.setStopPriceSource(BigDecimal.valueOf(66800));
        context.setStopSourceType("swing_low");
        context.setStopSourceTimeframe("1h");
        context.setStopSourceReason("recent swing low");
        context.setStopSourceRef("stop-1");
        context.setTpPriceSources(List.of(BigDecimal.valueOf(70400)));
        context.setTpSourceType("rr_ladder");
        context.setTpSourceTimeframe("1h");
        context.setTpSourceReason("2R target");
        context.setTpSourceRef("tp-1");
        context.setRrSource(BigDecimal.valueOf(2));
        context.setRrRuleRef("min_rr_2");
        context.setLiquiditySource("liquidity-ok");
        context.setMultiTimeframeSource("multi-timeframe-aligned");
        context.setEventSource("no-event-window");
        context.setWickSource("wick-confirmed");
        return context;
    }

    private DerivativesRiskContextDTO completeDerivativesRiskContext() {
        DerivativesRiskContextDTO context = new DerivativesRiskContextDTO();
        context.setSymbol("BTCUSDT");
        context.setTimeframe("1h");
        context.setOpenInterestHistory(List.of(BigDecimal.valueOf(1000), BigDecimal.valueOf(1020)));
        context.setFundingHistory(List.of(new BigDecimal("0.0001"), new BigDecimal("0.0002")));
        context.setLiquidationCluster(List.of(BigDecimal.valueOf(66500)));
        context.setLeverageDistribution(Map.of("1-5x", BigDecimal.valueOf(0.6), "5-10x", BigDecimal.valueOf(0.3)));
        context.setLongShortRatio(BigDecimal.valueOf(1.1));
        context.setLiquidityStress("LOW");
        context.setLiquidityStressReason("local fixture normal liquidity");
        context.setEventWindowBlockers(List.of("none"));
        context.setWickConfirmationSources(List.of("wick-confirmed"));
        context.setDataQualityScore(BigDecimal.valueOf(90));
        return context;
    }

    private BoundaryEntryDTO completeEntry() {
        BoundaryEntryDTO entry = new BoundaryEntryDTO();
        entry.setEntryType("pullback");
        entry.setEntryPrice(BigDecimal.valueOf(68000));
        entry.setEntryZoneLow(BigDecimal.valueOf(67800));
        entry.setEntryZoneHigh(BigDecimal.valueOf(68200));
        entry.setNumericSourceType("support");
        entry.setNumericSourceValue(BigDecimal.valueOf(68000));
        entry.setSourceTimeframe("1h");
        entry.setReason("support retest");
        return entry;
    }

    private BoundaryStopDTO completeStop() {
        BoundaryStopDTO stop = new BoundaryStopDTO();
        stop.setStopType("structure_invalidated");
        stop.setStopPrice(BigDecimal.valueOf(66800));
        stop.setStopZoneLow(BigDecimal.valueOf(66600));
        stop.setStopZoneHigh(BigDecimal.valueOf(67000));
        stop.setNumericSourceType("swing_low");
        stop.setNumericSourceValue(BigDecimal.valueOf(66800));
        stop.setSourceTimeframe("1h");
        stop.setReason("recent swing low");
        return stop;
    }

    private BoundaryTakeProfitLevelDTO completeTakeProfitLevel() {
        BoundaryTakeProfitLevelDTO takeProfit = new BoundaryTakeProfitLevelDTO();
        takeProfit.setLevel(1);
        takeProfit.setPrice(BigDecimal.valueOf(70400));
        takeProfit.setRr(BigDecimal.valueOf(2));
        takeProfit.setSource("rr_ladder");
        takeProfit.setNumericSourceType("rr_ladder");
        takeProfit.setNumericSourceValue(BigDecimal.valueOf(70400));
        takeProfit.setSourceTimeframe("1h");
        takeProfit.setSourceRef("tp-1");
        takeProfit.setPartialRatio(BigDecimal.valueOf(0.5));
        takeProfit.setAllocationRatio(BigDecimal.valueOf(0.5));
        takeProfit.setReason("2R target");
        return takeProfit;
    }

    private BoundarySourceFieldsDTO completeSourceFields() {
        BoundarySourceFieldsDTO sourceFields = new BoundarySourceFieldsDTO();
        sourceFields.setEntrySourceField("supportLevel");
        sourceFields.setStopSourceField("swingLow");
        sourceFields.setTakeProfitSourceField("rrLadder");
        sourceFields.setRrRule("min_rr_2");
        sourceFields.setDataSource("sourceTrace");
        sourceFields.setDataQualityScore(BigDecimal.valueOf(90));
        sourceFields.setEvidenceRefs(List.of("source-trace-1"));
        return sourceFields;
    }

    private DashboardDetailResponseVO.PlanBoundaryDisplayVO validBoundaryDisplay() {
        return boundaryDisplay(BoundaryStatusEnum.VALID);
    }

    private DashboardDetailResponseVO.PlanBoundaryDisplayVO boundaryDisplay(BoundaryStatusEnum status) {
        DashboardDetailResponseVO.PlanBoundaryDisplayVO display = new DashboardDetailResponseVO.PlanBoundaryDisplayVO();
        display.setPlanBoundaryStatus(status.name());
        display.setManualReviewRequired(true);
        display.setNotTradeInstruction(true);
        return display;
    }

    private DecisionResultVO lowRiskDecision() {
        DecisionResultVO decision = new DecisionResultVO();
        decision.setRiskLevel("LOW");
        decision.setExecutionPlanSummary("P17 local fixture review-only summary");
        return decision;
    }

    private DashboardDetailResponseVO.RiskActionGuardDisplayVO readyRiskActionGuard() {
        DashboardDetailResponseVO.RiskActionGuardDisplayVO risk = new DashboardDetailResponseVO.RiskActionGuardDisplayVO();
        risk.setRiskActionGuardStatus("MANUAL_REVIEW_REQUIRED");
        risk.setRiskActionBlockingReason("MANUAL_REVIEW_REQUIRED");
        risk.setLiquidityState("NORMAL");
        risk.setStampedeDetected(false);
        risk.setWickOnlyRisk(false);
        risk.setOpportunityPushAllowed(false);
        risk.setReverseTradeAllowed(false);
        risk.setNewPositionAllowed(false);
        risk.setMarketOrderExitAllowed(false);
        risk.setManualRiskReviewRequired(true);
        risk.setNotTradeInstruction(true);
        return risk;
    }

    private RuleEngineService optimisticRuleEngine() {
        return ctx -> {
            RuleBaseOutput output = new RuleBaseOutput();
            output.setMarketBias("BULLISH");
            output.setConfidenceLevel("HIGH");
            output.setRiskLevel("MEDIUM");
            output.setPlanMode(ExecutionPlanVO.PLAN_MODE_SEMI_STRUCTURED);
            output.setCanExecute(true);
            return output;
        };
    }
}
