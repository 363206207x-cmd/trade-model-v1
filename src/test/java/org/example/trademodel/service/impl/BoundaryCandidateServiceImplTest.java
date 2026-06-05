package org.example.trademodel.service.impl;

import org.example.trademodel.dto.planboundary.BoundaryCandidateDTO;
import org.example.trademodel.dto.planboundary.BoundaryEntryDTO;
import org.example.trademodel.dto.planboundary.BoundarySourceFieldsDTO;
import org.example.trademodel.dto.planboundary.BoundaryStatusEnum;
import org.example.trademodel.dto.planboundary.BoundaryStopDTO;
import org.example.trademodel.dto.planboundary.BoundaryTakeProfitLevelDTO;
import org.example.trademodel.dto.planboundary.DerivativesRiskContextDTO;
import org.example.trademodel.dto.planboundary.RuntimeKlineContextDTO;
import org.example.trademodel.dto.planboundary.RuntimeKlineItemDTO;
import org.example.trademodel.dto.planboundary.SourceTraceDTO;
import org.example.trademodel.dto.planboundary.SourceTraceFallbackStatusEnum;
import org.example.trademodel.vo.DashboardDetailResponseVO;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class BoundaryCandidateServiceImplTest {

    private final BoundaryCandidateServiceImpl service = new BoundaryCandidateServiceImpl();

    @Test
    void evaluateBoundaryCandidateReturnsIncompleteWhenSourceTraceMissing() {
        BoundaryCandidateDTO candidate = service.evaluateBoundaryCandidate(
                "BTCUSDT",
                "1h",
                null,
                validEntry(),
                validStop(),
                List.of(validTakeProfitLevel()),
                validSourceFields(),
                BigDecimal.valueOf(90)
        );

        assertThat(candidate.getBoundaryStatus()).isEqualTo(BoundaryStatusEnum.INCOMPLETE);
        assertThat(candidate.isManualReviewRequired()).isTrue();
        assertThat(candidate.isNotTradeInstruction()).isTrue();
        assertThat(candidate.getBlockingReasons()).contains("sourceTrace missing");
    }

    @Test
    void evaluateBoundaryCandidateReturnsWatchOnlyWhenSourceTraceRequestsWatchOnlyFallback() {
        SourceTraceDTO sourceTrace = validSourceTrace();
        sourceTrace.setFallbackStatus(SourceTraceFallbackStatusEnum.WATCH_ONLY);
        sourceTrace.setMissingFields(List.of("eventSource"));

        BoundaryCandidateDTO candidate = service.evaluateBoundaryCandidate(
                "BTCUSDT",
                "1h",
                sourceTrace,
                validEntry(),
                validStop(),
                List.of(validTakeProfitLevel()),
                validSourceFields(),
                BigDecimal.valueOf(90)
        );

        assertThat(candidate.getBoundaryStatus()).isEqualTo(BoundaryStatusEnum.WATCH_ONLY);
        assertThat(candidate.isManualReviewRequired()).isTrue();
        assertThat(candidate.isNotTradeInstruction()).isTrue();
        assertThat(candidate.getBlockingReasons()).contains("sourceTrace fallbackStatus=WATCH_ONLY");
    }

    @Test
    void evaluateBoundaryCandidateReturnsIncompleteWhenBoundarySourcesAreMissing() {
        BoundaryEntryDTO entry = validEntry();
        entry.setNumericSourceValue(null);

        BoundaryCandidateDTO candidate = service.evaluateBoundaryCandidate(
                "BTCUSDT",
                "1h",
                validSourceTrace(),
                entry,
                validStop(),
                List.of(validTakeProfitLevel()),
                validSourceFields(),
                BigDecimal.valueOf(90)
        );

        assertThat(candidate.getBoundaryStatus()).isEqualTo(BoundaryStatusEnum.INCOMPLETE);
        assertThat(candidate.isManualReviewRequired()).isTrue();
        assertThat(candidate.isNotTradeInstruction()).isTrue();
        assertThat(candidate.getBlockingReasons()).contains("entry numeric source value missing");
    }

    @Test
    void evaluateBoundaryCandidateReturnsValidWhenAllSourcesAreTraceable() {
        BoundaryEntryDTO entry = validEntry();
        BoundaryStopDTO stop = validStop();
        BoundaryTakeProfitLevelDTO takeProfit = validTakeProfitLevel();
        BoundarySourceFieldsDTO sourceFields = validSourceFields();

        BoundaryCandidateDTO candidate = service.evaluateBoundaryCandidate(
                "BTCUSDT",
                "1h",
                validSourceTrace(),
                entry,
                stop,
                List.of(takeProfit),
                sourceFields,
                BigDecimal.valueOf(90)
        );

        assertThat(candidate.getBoundaryStatus()).isEqualTo(BoundaryStatusEnum.VALID);
        assertThat(candidate.getEntry()).isEqualTo(entry);
        assertThat(candidate.getStop()).isEqualTo(stop);
        assertThat(candidate.getTakeProfitLevels()).containsExactly(takeProfit);
        assertThat(candidate.getSourceFields()).isEqualTo(sourceFields);
        assertThat(candidate.getDataQualityScore()).isEqualByComparingTo("90");
        assertThat(candidate.isManualReviewRequired()).isTrue();
        assertThat(candidate.isNotTradeInstruction()).isTrue();
        assertThat(candidate.getBlockingReasons()).isEmpty();
    }

    @Test
    void evaluateBoundaryCandidateUsesAssemblerAndReturnsFallbackWhenRuntimeSourceIsMissing() {
        RuntimeKlineContextDTO runtimeKlineContext = validRuntimeKlineContext();
        runtimeKlineContext.setEntryPriceSource(null);

        BoundaryCandidateDTO candidate = service.evaluateBoundaryCandidate(
                "BTCUSDT",
                "1h",
                runtimeKlineContext,
                validDerivativesRiskContext(),
                validEntry(),
                validStop(),
                List.of(validTakeProfitLevel()),
                validSourceFields(),
                BigDecimal.valueOf(90)
        );

        assertThat(candidate.getBoundaryStatus()).isEqualTo(BoundaryStatusEnum.INCOMPLETE);
        assertThat(candidate.isManualReviewRequired()).isTrue();
        assertThat(candidate.isNotTradeInstruction()).isTrue();
        assertThat(candidate.getBlockingReasons()).contains("sourceTrace fallbackStatus=INCOMPLETE");
    }

    @Test
    void evaluateBoundaryCandidateDoesNotTreatRuntimeKlineVisibilityAsValidSources() {
        RuntimeKlineContextDTO runtimeKlineContext = new RuntimeKlineContextDTO();
        runtimeKlineContext.setSymbol("BTCUSDT");
        runtimeKlineContext.setTimeframe("1m");
        runtimeKlineContext.setLatestPrice(BigDecimal.valueOf(68100));
        runtimeKlineContext.setDataQualityScore(BigDecimal.valueOf(90));
        runtimeKlineContext.setKlineItems(List.of(klineItem("102.30")));
        runtimeKlineContext.setManualReviewRequired(true);
        runtimeKlineContext.setNotTradeInstruction(true);

        BoundaryCandidateDTO candidate = service.evaluateBoundaryCandidate(
                "BTCUSDT",
                "1m",
                runtimeKlineContext,
                validDerivativesRiskContext(),
                validEntry(),
                validStop(),
                List.of(validTakeProfitLevel()),
                validSourceFields(),
                BigDecimal.valueOf(90)
        );

        assertThat(candidate.getBoundaryStatus()).isEqualTo(BoundaryStatusEnum.INCOMPLETE);
        assertThat(candidate.isManualReviewRequired()).isTrue();
        assertThat(candidate.isNotTradeInstruction()).isTrue();
        assertThat(candidate.getBlockingReasons()).contains(
                "sourceTrace fallbackStatus=INCOMPLETE",
                "sourceTrace missingFields present",
                "entry source missing",
                "stop source missing",
                "TP source missing",
                "RR source missing"
        );
    }

    @Test
    void evaluateBoundaryCandidateUsesAssemblerAndReturnsValidWhenRuntimeSourcesAreComplete() {
        BoundaryEntryDTO entry = validEntry();
        BoundaryStopDTO stop = validStop();
        BoundaryTakeProfitLevelDTO takeProfit = validTakeProfitLevel();

        BoundaryCandidateDTO candidate = service.evaluateBoundaryCandidate(
                "BTCUSDT",
                "1h",
                validRuntimeKlineContext(),
                validDerivativesRiskContext(),
                entry,
                stop,
                List.of(takeProfit),
                validSourceFields(),
                BigDecimal.valueOf(90)
        );

        assertThat(candidate.getBoundaryStatus()).isEqualTo(BoundaryStatusEnum.VALID);
        assertThat(candidate.getEntry()).isEqualTo(entry);
        assertThat(candidate.getStop()).isEqualTo(stop);
        assertThat(candidate.getTakeProfitLevels()).containsExactly(takeProfit);
        assertThat(candidate.isManualReviewRequired()).isTrue();
        assertThat(candidate.isNotTradeInstruction()).isTrue();
    }

    @Test
    void evaluateBoundaryCandidateReturnsWatchOnlyWhenRiskActionGuardFailsClosed() {
        DashboardDetailResponseVO.RiskActionGuardDisplayVO risk = readyRiskActionGuard();
        risk.setStampedeDetected(true);

        BoundaryCandidateDTO candidate = service.evaluateBoundaryCandidate(
                "BTCUSDT",
                "1h",
                validSourceTrace(),
                validEntry(),
                validStop(),
                List.of(validTakeProfitLevel()),
                validSourceFields(),
                BigDecimal.valueOf(90),
                risk
        );

        assertThat(candidate.getBoundaryStatus()).isEqualTo(BoundaryStatusEnum.WATCH_ONLY);
        assertThat(candidate.isManualReviewRequired()).isTrue();
        assertThat(candidate.isNotTradeInstruction()).isTrue();
        assertThat(candidate.getBlockingReasons()).contains("stampede risk detected");
    }

    @Test
    void evaluateBoundaryCandidateReturnsWatchOnlyWhenRiskActionGuardLiquiditySourceIsMissing() {
        DashboardDetailResponseVO.RiskActionGuardDisplayVO risk = readyRiskActionGuard();
        risk.setLiquidityState("BACKEND_PENDING");

        BoundaryCandidateDTO candidate = service.evaluateBoundaryCandidate(
                "BTCUSDT",
                "1h",
                validSourceTrace(),
                validEntry(),
                validStop(),
                List.of(validTakeProfitLevel()),
                validSourceFields(),
                BigDecimal.valueOf(90),
                risk
        );

        assertThat(candidate.getBoundaryStatus()).isEqualTo(BoundaryStatusEnum.WATCH_ONLY);
        assertThat(candidate.isManualReviewRequired()).isTrue();
        assertThat(candidate.isNotTradeInstruction()).isTrue();
        assertThat(candidate.getBlockingReasons()).contains("liquidity source missing");
    }

    @Test
    void evaluateBoundaryCandidateReturnsWatchOnlyWhenRiskActionGuardDetectsWickOnlyRisk() {
        DashboardDetailResponseVO.RiskActionGuardDisplayVO risk = readyRiskActionGuard();
        risk.setWickOnlyRisk(true);

        BoundaryCandidateDTO candidate = service.evaluateBoundaryCandidate(
                "BTCUSDT",
                "1h",
                validSourceTrace(),
                validEntry(),
                validStop(),
                List.of(validTakeProfitLevel()),
                validSourceFields(),
                BigDecimal.valueOf(90),
                risk
        );

        assertThat(candidate.getBoundaryStatus()).isEqualTo(BoundaryStatusEnum.WATCH_ONLY);
        assertThat(candidate.isManualReviewRequired()).isTrue();
        assertThat(candidate.isNotTradeInstruction()).isTrue();
        assertThat(candidate.getBlockingReasons()).contains("wick-only risk detected");
    }

    @Test
    void evaluateBoundaryCandidateReturnsWatchOnlyWhenRiskActionGuardActionFlagWouldAllowTrading() {
        DashboardDetailResponseVO.RiskActionGuardDisplayVO risk = readyRiskActionGuard();
        risk.setNewPositionAllowed(true);

        BoundaryCandidateDTO candidate = service.evaluateBoundaryCandidate(
                "BTCUSDT",
                "1h",
                validSourceTrace(),
                validEntry(),
                validStop(),
                List.of(validTakeProfitLevel()),
                validSourceFields(),
                BigDecimal.valueOf(90),
                risk
        );

        assertThat(candidate.getBoundaryStatus()).isEqualTo(BoundaryStatusEnum.WATCH_ONLY);
        assertThat(candidate.isManualReviewRequired()).isTrue();
        assertThat(candidate.isNotTradeInstruction()).isTrue();
        assertThat(candidate.getBlockingReasons()).contains("riskActionGuard action flag allowed");
    }

    @Test
    void evaluateBoundaryCandidateReturnsValidWhenRiskActionGuardIsReviewOnlyReady() {
        BoundaryCandidateDTO candidate = service.evaluateBoundaryCandidate(
                "BTCUSDT",
                "1h",
                validSourceTrace(),
                validEntry(),
                validStop(),
                List.of(validTakeProfitLevel()),
                validSourceFields(),
                BigDecimal.valueOf(90),
                readyRiskActionGuard()
        );

        assertThat(candidate.getBoundaryStatus()).isEqualTo(BoundaryStatusEnum.VALID);
        assertThat(candidate.isManualReviewRequired()).isTrue();
        assertThat(candidate.isNotTradeInstruction()).isTrue();
        assertThat(candidate.getBlockingReasons()).isEmpty();
    }

    @Test
    void serviceShouldNotExposeTradingExecutionMethods() {
        List<String> methodNames = List.of(BoundaryCandidateServiceImpl.class.getDeclaredMethods())
                .stream()
                .map(Method::getName)
                .map(String::toLowerCase)
                .toList();

        assertThat(methodNames).noneMatch(name -> name.contains("execute"));
        assertThat(methodNames).noneMatch(name -> name.contains("order"));
        assertThat(methodNames).noneMatch(name -> name.contains("close"));
        assertThat(methodNames).noneMatch(name -> name.contains("reverse"));
    }

    @Test
    void ownerPathShouldNotReferenceFrozenPointOrRuntimeWrappers() throws Exception {
        List<Path> ownerPathSources = List.of(
                Path.of("src/main/java/org/example/trademodel/service/impl/BoundaryCandidateServiceImpl.java"),
                Path.of("src/main/java/org/example/trademodel/dto/planboundary/BoundaryCandidateDTO.java"),
                Path.of("src/main/java/org/example/trademodel/dto/planboundary/BoundaryEntryDTO.java"),
                Path.of("src/main/java/org/example/trademodel/dto/planboundary/BoundaryStopDTO.java"),
                Path.of("src/main/java/org/example/trademodel/dto/planboundary/BoundaryTakeProfitLevelDTO.java"),
                Path.of("src/main/java/org/example/trademodel/dto/planboundary/BoundarySourceFieldsDTO.java")
        );
        List<String> frozenWrapperNames = List.of(
                "SourceOwnedCandidateIntegrationRuntimeCandidate",
                "SourceOwnedCandidateIntegrationRuntimeCandidateAssembler",
                "SourceOwnedCandidateIntegrationRuntimeCandidateValidator",
                "ReviewOnlyNumericPointProposal",
                "ReviewOnlyPointProposal",
                "NumericPointSafetyValidator",
                "SourceTraceNumericSourceContextDTO"
        );

        for (Path ownerPathSource : ownerPathSources) {
            String sourceText = Files.readString(ownerPathSource);
            for (String frozenWrapperName : frozenWrapperNames) {
                assertThat(sourceText).doesNotContain(frozenWrapperName);
            }
        }
    }

    private SourceTraceDTO validSourceTrace() {
        SourceTraceDTO sourceTrace = new SourceTraceDTO();
        sourceTrace.setSymbol("BTCUSDT");
        sourceTrace.setTimeframe("1h");
        sourceTrace.setEntryPriceSource(BigDecimal.valueOf(68000));
        sourceTrace.setEntrySourceType("support");
        sourceTrace.setEntrySourceTimeframe("1h");
        sourceTrace.setEntrySourceReason("support retest");
        sourceTrace.setEntrySourceRef("entry-1");
        sourceTrace.setStopPriceSource(BigDecimal.valueOf(66800));
        sourceTrace.setStopSourceType("swing_low");
        sourceTrace.setStopSourceTimeframe("1h");
        sourceTrace.setStopSourceReason("recent swing low");
        sourceTrace.setStopSourceRef("stop-1");
        sourceTrace.setTpPriceSources(List.of(BigDecimal.valueOf(70400)));
        sourceTrace.setTpSourceType("rr_ladder");
        sourceTrace.setTpSourceTimeframe("1h");
        sourceTrace.setTpSourceReason("2R target");
        sourceTrace.setTpSourceRef("tp-1");
        sourceTrace.setRrSource(BigDecimal.valueOf(2));
        sourceTrace.setRrRuleRef("min_rr_2");
        sourceTrace.setLiquiditySource("liquidity-ok");
        sourceTrace.setMultiTimeframeSource("multi-timeframe-aligned");
        sourceTrace.setEventSource("no-event-window");
        sourceTrace.setWickSource("wick-confirmed");
        return sourceTrace;
    }

    private RuntimeKlineContextDTO validRuntimeKlineContext() {
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

    private DerivativesRiskContextDTO validDerivativesRiskContext() {
        DerivativesRiskContextDTO context = new DerivativesRiskContextDTO();
        context.setSymbol("BTCUSDT");
        context.setTimeframe("1h");
        context.setOpenInterestHistory(List.of(BigDecimal.valueOf(1000), BigDecimal.valueOf(1020)));
        context.setFundingHistory(List.of(new BigDecimal("0.0001"), new BigDecimal("0.0002")));
        context.setLiquidationCluster(List.of(BigDecimal.valueOf(66500)));
        context.setLeverageDistribution(java.util.Map.of("1-5x", BigDecimal.valueOf(0.6)));
        context.setLongShortRatio(BigDecimal.valueOf(1.1));
        context.setLiquidityStress("LOW");
        context.setEventWindowBlockers(List.of("none"));
        context.setWickConfirmationSources(List.of("wick-confirmed"));
        context.setDataQualityScore(BigDecimal.valueOf(90));
        return context;
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

    private BoundaryEntryDTO validEntry() {
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

    private BoundaryStopDTO validStop() {
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

    private BoundaryTakeProfitLevelDTO validTakeProfitLevel() {
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

    private BoundarySourceFieldsDTO validSourceFields() {
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

    private RuntimeKlineItemDTO klineItem(String closePrice) {
        RuntimeKlineItemDTO item = new RuntimeKlineItemDTO();
        item.setClosePrice(new BigDecimal(closePrice));
        return item;
    }
}
