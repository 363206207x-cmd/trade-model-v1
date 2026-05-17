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
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class P18DerivativesRiskContextFixtureExtensionTest {

    private final DefaultSourceAssembler sourceAssembler = new DefaultSourceAssembler();
    private final BoundaryCandidateServiceImpl boundaryCandidateService = new BoundaryCandidateServiceImpl(sourceAssembler);
    private final DefaultExecutionPlanDisplayAdapter executionPlanDisplayAdapter = new DefaultExecutionPlanDisplayAdapter();

    @Test
    void completeDerivativesRiskContextSupportsValidReviewOnlyCandidateButNotExecution() {
        SourceTraceDTO sourceTrace = sourceAssembler.assembleSourceTrace(
                completeRuntimeKlineContext(),
                completeDerivativesRiskContext()
        );
        BoundaryCandidateDTO candidate = evaluateWith(completeDerivativesRiskContext(), readyRiskActionGuard());
        DashboardDetailResponseVO.ExecutionPlanDisplayVO display = executionPlanDisplayAdapter.build(
                lowRiskDecision(),
                boundaryDisplay(candidate.getBoundaryStatus()),
                null,
                sourceTrace,
                readyRiskActionGuard()
        );
        RuleBaseOutput output = optimisticRuleEngine().execute(new DecisionContext(), sourceTrace, readyRiskActionGuard());

        assertThat(sourceTrace.getFallbackStatus()).isNull();
        assertThat(sourceTrace.getMissingFields()).isEmpty();
        assertThat(sourceTrace.hasRequiredBoundarySources()).isTrue();
        assertThat(candidate.getBoundaryStatus()).isEqualTo(BoundaryStatusEnum.VALID);
        assertThat(candidate.isManualReviewRequired()).isTrue();
        assertThat(candidate.isNotTradeInstruction()).isTrue();
        assertThat(display.getExecutionPlanStatus()).isEqualTo("READY_REVIEW_ONLY");
        assertThat(display.getNotExecutableReason()).isEqualTo("MANUAL_REVIEW_REQUIRED");
        assertThat(display.getManualReviewRequired()).isTrue();
        assertThat(display.getNotTradeInstruction()).isTrue();
        assertThat(output.isCanExecute()).isFalse();
        assertThat(output.getPlanMode()).isEqualTo(ExecutionPlanVO.PLAN_MODE_ADVISORY);
    }

    @Test
    void missingIndividualDerivativesRiskFieldsDowngradeToWatchOnlyReviewState() {
        List<String> missingFields = List.of(
                "openInterestHistory",
                "fundingHistory",
                "liquidationCluster",
                "leverageDistribution",
                "longShortRatio"
        );

        for (String missingField : missingFields) {
            DerivativesRiskContextDTO context = completeDerivativesRiskContext();
            context.setMissingFields(List.of(missingField));
            context.setFallbackStatus(SourceTraceFallbackStatusEnum.WATCH_ONLY);

            assertRiskContextFallback(
                    context,
                    SourceTraceFallbackStatusEnum.WATCH_ONLY,
                    BoundaryStatusEnum.WATCH_ONLY,
                    "SOURCE_TRACE_WATCH_ONLY",
                    "derivativesRiskContext." + missingField
            );
        }
    }

    @Test
    void staleAndExtremeDerivativesRiskSignalsStayWatchOnlyAndDoNotBecomeTradeActions() {
        Map<String, DerivativesRiskContextDTO> scenarios = new LinkedHashMap<>();
        scenarios.put("openInterestHistory.stale", staleOpenInterestContext());
        scenarios.put("fundingHistory.stale", staleFundingContext());
        scenarios.put("fundingRate.extreme", extremeFundingContext());
        scenarios.put("openInterestDelta.abnormal", abnormalOpenInterestContext());
        scenarios.put("liquidationCluster.abnormalConcentration", abnormalLiquidationContext());
        scenarios.put("leverageDistribution.highRiskSkew", highLeverageSkewContext());
        scenarios.put("longShortRatio.extremeCrowding", extremeLongShortCrowdingContext());
        scenarios.put("derivativesRiskSignals.conflicting", conflictingDerivativesSignalsContext());
        scenarios.put("dataQualityScore.downgrade", dataQualityDowngradeContext());

        for (Map.Entry<String, DerivativesRiskContextDTO> scenario : scenarios.entrySet()) {
            assertRiskContextFallback(
                    scenario.getValue(),
                    SourceTraceFallbackStatusEnum.WATCH_ONLY,
                    BoundaryStatusEnum.WATCH_ONLY,
                    "SOURCE_TRACE_WATCH_ONLY",
                    "derivativesRiskContext." + scenario.getKey()
            );
        }
    }

    @Test
    void liquidityStressMissingOrWorseningFailsClosedBeforeExecutionReadiness() {
        Map<String, DerivativesRiskContextDTO> scenarios = new LinkedHashMap<>();
        scenarios.put("liquidityStress", missingLiquidityStressContext());
        scenarios.put("liquidityStress.worsening", worseningLiquidityStressContext());

        for (Map.Entry<String, DerivativesRiskContextDTO> scenario : scenarios.entrySet()) {
            assertRiskContextFallback(
                    scenario.getValue(),
                    SourceTraceFallbackStatusEnum.SAFE_FAIL_CLOSED_ONLY,
                    BoundaryStatusEnum.WATCH_ONLY,
                    "SOURCE_TRACE_SAFE_FAIL_CLOSED_ONLY",
                    "derivativesRiskContext." + scenario.getKey()
            );
        }
    }

    @Test
    void stampedeLikeLiquidityStressBlocksCandidateExecutionAndOpportunityPush() {
        DerivativesRiskContextDTO context = stampedeLikeLiquidityStressContext();
        SourceTraceDTO sourceTrace = sourceAssembler.assembleSourceTrace(completeRuntimeKlineContext(), context);
        DashboardDetailResponseVO.RiskActionGuardDisplayVO guard = readyRiskActionGuard();
        guard.setStampedeDetected(true);

        BoundaryCandidateDTO candidate = boundaryCandidateService.evaluateBoundaryCandidate(
                "BTCUSDT",
                "1h",
                sourceTrace,
                completeEntry(),
                completeStop(),
                List.of(completeTakeProfitLevel()),
                completeSourceFields(),
                BigDecimal.valueOf(90),
                guard
        );
        DashboardDetailResponseVO.ExecutionPlanDisplayVO display = executionPlanDisplayAdapter.build(
                lowRiskDecision(),
                validBoundaryDisplay(),
                null,
                sourceTrace,
                guard
        );
        RuleBaseOutput output = optimisticRuleEngine().execute(new DecisionContext(), sourceTrace, guard);

        assertThat(sourceTrace.getFallbackStatus()).isEqualTo(SourceTraceFallbackStatusEnum.SAFE_FAIL_CLOSED_ONLY);
        assertThat(candidate.getBoundaryStatus()).isEqualTo(BoundaryStatusEnum.WATCH_ONLY);
        assertThat(candidate.getBlockingReasons()).contains("sourceTrace fallbackStatus=SAFE_FAIL_CLOSED_ONLY");
        assertThat(candidate.getBlockingReasons()).contains("stampede risk detected");
        assertThat(display.getExecutionPlanStatus()).isEqualTo("WATCH_ONLY");
        assertThat(display.getNotExecutableReason()).isEqualTo("SOURCE_TRACE_SAFE_FAIL_CLOSED_ONLY");
        assertThat(output.isCanExecute()).isFalse();
        assertThat(output.getPlanMode()).isEqualTo(ExecutionPlanVO.PLAN_MODE_ADVISORY);
        assertThat(guard.getOpportunityPushAllowed()).isFalse();
        assertThat(guard.getNewPositionAllowed()).isFalse();
        assertThat(guard.getReverseTradeAllowed()).isFalse();
    }

    @Test
    void fixtureCatalogDocumentsAllP18DerivativesRiskScenarios() throws IOException {
        InputStream stream = getClass().getResourceAsStream(
                "/planboundary/p18-derivatives-risk-fixture-extension-cases.csv"
        );
        assertThat(stream).isNotNull();
        String content = new String(stream.readAllBytes(), StandardCharsets.UTF_8);

        assertThat(content).contains("P18-COMPLETE-DERIVATIVES-RISK");
        assertThat(content).contains("P18-MISSING-OI-HISTORY");
        assertThat(content).contains("P18-STALE-OI-HISTORY");
        assertThat(content).contains("P18-MISSING-FUNDING-HISTORY");
        assertThat(content).contains("P18-EXTREME-FUNDING");
        assertThat(content).contains("P18-MISSING-LIQUIDATION-CLUSTER");
        assertThat(content).contains("P18-ABNORMAL-LIQUIDATION-CONCENTRATION");
        assertThat(content).contains("P18-MISSING-LEVERAGE-DISTRIBUTION");
        assertThat(content).contains("P18-HIGH-LEVERAGE-SKEW");
        assertThat(content).contains("P18-MISSING-LONG-SHORT-RATIO");
        assertThat(content).contains("P18-EXTREME-LONG-SHORT-CROWDING");
        assertThat(content).contains("P18-LIQUIDITY-STRESS-MISSING");
        assertThat(content).contains("P18-LIQUIDITY-STRESS-WORSENING");
        assertThat(content).contains("P18-STAMPEDE-LIKE-STRESS");
        assertThat(content).contains("P18-CONFLICTING-DERIVATIVES-SIGNALS");
        assertThat(content).contains("P18-DATA-QUALITY-DOWNGRADE");
    }

    private void assertRiskContextFallback(
            DerivativesRiskContextDTO context,
            SourceTraceFallbackStatusEnum expectedSourceFallback,
            BoundaryStatusEnum expectedBoundaryStatus,
            String expectedExecutionReason,
            String expectedMissingField
    ) {
        SourceTraceDTO sourceTrace = sourceAssembler.assembleSourceTrace(completeRuntimeKlineContext(), context);
        BoundaryCandidateDTO candidate = evaluateWith(context, readyRiskActionGuard());
        DashboardDetailResponseVO.ExecutionPlanDisplayVO display = executionPlanDisplayAdapter.build(
                lowRiskDecision(),
                validBoundaryDisplay(),
                null,
                sourceTrace,
                readyRiskActionGuard()
        );
        RuleBaseOutput output = optimisticRuleEngine().execute(new DecisionContext(), sourceTrace, readyRiskActionGuard());

        assertThat(sourceTrace.getFallbackStatus()).isEqualTo(expectedSourceFallback);
        assertThat(sourceTrace.getMissingFields()).contains(expectedMissingField);
        assertThat(sourceTrace.hasRequiredBoundarySources()).isFalse();
        assertThat(candidate.getBoundaryStatus()).isEqualTo(expectedBoundaryStatus);
        assertThat(candidate.isManualReviewRequired()).isTrue();
        assertThat(candidate.isNotTradeInstruction()).isTrue();
        assertThat(display.getExecutionPlanStatus()).isEqualTo("WATCH_ONLY");
        assertThat(display.getNotExecutableReason()).isEqualTo(expectedExecutionReason);
        assertThat(display.getManualReviewRequired()).isTrue();
        assertThat(display.getNotTradeInstruction()).isTrue();
        assertThat(output.isCanExecute()).isFalse();
        assertThat(output.getPlanMode()).isEqualTo(ExecutionPlanVO.PLAN_MODE_ADVISORY);
    }

    private BoundaryCandidateDTO evaluateWith(
            DerivativesRiskContextDTO derivativesRiskContext,
            DashboardDetailResponseVO.RiskActionGuardDisplayVO guard
    ) {
        SourceTraceDTO sourceTrace = sourceAssembler.assembleSourceTrace(completeRuntimeKlineContext(), derivativesRiskContext);
        return boundaryCandidateService.evaluateBoundaryCandidate(
                "BTCUSDT",
                "1h",
                sourceTrace,
                completeEntry(),
                completeStop(),
                List.of(completeTakeProfitLevel()),
                completeSourceFields(),
                BigDecimal.valueOf(90),
                guard
        );
    }

    private DerivativesRiskContextDTO staleOpenInterestContext() {
        DerivativesRiskContextDTO context = completeDerivativesRiskContext();
        context.setMissingFields(List.of("openInterestHistory.stale"));
        context.setFallbackStatus(SourceTraceFallbackStatusEnum.WATCH_ONLY);
        return context;
    }

    private DerivativesRiskContextDTO staleFundingContext() {
        DerivativesRiskContextDTO context = completeDerivativesRiskContext();
        context.setMissingFields(List.of("fundingHistory.stale"));
        context.setFallbackStatus(SourceTraceFallbackStatusEnum.WATCH_ONLY);
        return context;
    }

    private DerivativesRiskContextDTO extremeFundingContext() {
        DerivativesRiskContextDTO context = completeDerivativesRiskContext();
        context.setLastFundingRate(new BigDecimal("0.005"));
        context.setFundingHistory(List.of(new BigDecimal("0.003"), new BigDecimal("0.005")));
        context.setMissingFields(List.of("fundingRate.extreme"));
        context.setFallbackStatus(SourceTraceFallbackStatusEnum.WATCH_ONLY);
        return context;
    }

    private DerivativesRiskContextDTO abnormalOpenInterestContext() {
        DerivativesRiskContextDTO context = completeDerivativesRiskContext();
        context.setOpenInterestDelta(BigDecimal.valueOf(45));
        context.setMissingFields(List.of("openInterestDelta.abnormal"));
        context.setFallbackStatus(SourceTraceFallbackStatusEnum.WATCH_ONLY);
        return context;
    }

    private DerivativesRiskContextDTO abnormalLiquidationContext() {
        DerivativesRiskContextDTO context = completeDerivativesRiskContext();
        context.setLiquidationCluster(List.of(BigDecimal.valueOf(67900), BigDecimal.valueOf(68050), BigDecimal.valueOf(68120)));
        context.setMissingFields(List.of("liquidationCluster.abnormalConcentration"));
        context.setFallbackStatus(SourceTraceFallbackStatusEnum.WATCH_ONLY);
        return context;
    }

    private DerivativesRiskContextDTO highLeverageSkewContext() {
        DerivativesRiskContextDTO context = completeDerivativesRiskContext();
        context.setLeverageDistribution(Map.of("20x+", BigDecimal.valueOf(0.72), "50x+", BigDecimal.valueOf(0.18)));
        context.setMissingFields(List.of("leverageDistribution.highRiskSkew"));
        context.setFallbackStatus(SourceTraceFallbackStatusEnum.WATCH_ONLY);
        return context;
    }

    private DerivativesRiskContextDTO extremeLongShortCrowdingContext() {
        DerivativesRiskContextDTO context = completeDerivativesRiskContext();
        context.setLongShortRatio(BigDecimal.valueOf(3.4));
        context.setMissingFields(List.of("longShortRatio.extremeCrowding"));
        context.setFallbackStatus(SourceTraceFallbackStatusEnum.WATCH_ONLY);
        return context;
    }

    private DerivativesRiskContextDTO conflictingDerivativesSignalsContext() {
        DerivativesRiskContextDTO context = completeDerivativesRiskContext();
        context.setOpenInterestDelta(BigDecimal.valueOf(35));
        context.setLastFundingRate(new BigDecimal("-0.004"));
        context.setLongShortRatio(BigDecimal.valueOf(2.9));
        context.setMissingFields(List.of("derivativesRiskSignals.conflicting"));
        context.setFallbackStatus(SourceTraceFallbackStatusEnum.WATCH_ONLY);
        return context;
    }

    private DerivativesRiskContextDTO dataQualityDowngradeContext() {
        DerivativesRiskContextDTO context = completeDerivativesRiskContext();
        context.setDataQualityScore(BigDecimal.valueOf(55));
        context.setMissingFields(List.of("dataQualityScore.downgrade"));
        context.setFallbackStatus(SourceTraceFallbackStatusEnum.WATCH_ONLY);
        return context;
    }

    private DerivativesRiskContextDTO missingLiquidityStressContext() {
        DerivativesRiskContextDTO context = completeDerivativesRiskContext();
        context.setLiquidityStress(null);
        context.setMissingFields(List.of("liquidityStress"));
        context.setFallbackStatus(SourceTraceFallbackStatusEnum.SAFE_FAIL_CLOSED_ONLY);
        return context;
    }

    private DerivativesRiskContextDTO worseningLiquidityStressContext() {
        DerivativesRiskContextDTO context = completeDerivativesRiskContext();
        context.setLiquidityStress("WORSENING");
        context.setLiquidityStressReason("local fixture liquidity stress worsening");
        context.setMissingFields(List.of("liquidityStress.worsening"));
        context.setFallbackStatus(SourceTraceFallbackStatusEnum.SAFE_FAIL_CLOSED_ONLY);
        return context;
    }

    private DerivativesRiskContextDTO stampedeLikeLiquidityStressContext() {
        DerivativesRiskContextDTO context = completeDerivativesRiskContext();
        context.setLiquidityStress("STAMPEDE_LIKE");
        context.setLiquidityStressReason("local fixture stampede-like liquidity stress");
        context.setMissingFields(List.of("liquidityStress.stampedeLike"));
        context.setFallbackStatus(SourceTraceFallbackStatusEnum.SAFE_FAIL_CLOSED_ONLY);
        return context;
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
        context.setContextTime(LocalDateTime.of(2026, 5, 17, 10, 0));
        context.setOpenInterestHistory(List.of(BigDecimal.valueOf(1000), BigDecimal.valueOf(1020)));
        context.setOpenInterestDelta(BigDecimal.valueOf(20));
        context.setLastFundingRate(new BigDecimal("0.0002"));
        context.setFundingHistory(List.of(new BigDecimal("0.0001"), new BigDecimal("0.0002")));
        context.setLiquidationCluster(List.of(BigDecimal.valueOf(66500), BigDecimal.valueOf(72000)));
        context.setLeverageDistribution(Map.of("1-5x", BigDecimal.valueOf(0.60), "5-10x", BigDecimal.valueOf(0.25)));
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
        decision.setExecutionPlanSummary("P18 local derivatives risk fixture review-only summary");
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
