package org.example.trademodel.service.impl;

import org.example.trademodel.dto.planboundary.BoundaryTakeProfitLevelDTO;
import org.example.trademodel.dto.planboundary.MarketStructureBoundaryDTO;
import org.example.trademodel.dto.planboundary.MarketStructureTakeProfitTargetDTO;
import org.example.trademodel.dto.planboundary.SourceTraceBoundaryProducerResult;
import org.example.trademodel.dto.planboundary.SourceTraceDTO;
import org.example.trademodel.dto.planboundary.SourceTraceFallbackStatusEnum;
import org.example.trademodel.service.planboundary.SourceTraceBoundaryProducer;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class SourceTraceBoundaryProducerImplTest {

    private final SourceTraceBoundaryProducer producer = new SourceTraceBoundaryProducerImpl();

    @Test
    void readyLongBoundaryMapsEntryStopTakeProfitAndCanonicalRefs() {
        SourceTraceBoundaryProducerResult result = producer.produce(readyLongBoundary());

        assertThat(result.isBoundaryReady()).isTrue();
        assertThat(result.isSourceTraceReady()).isFalse();
        assertThat(result.getBlockingReasons()).isEmpty();
        assertThat(result.getEntry().getEntryPrice()).isEqualByComparingTo("101");
        assertThat(result.getEntry().getEntryZoneLow()).isEqualByComparingTo("100");
        assertThat(result.getEntry().getEntryZoneHigh()).isEqualByComparingTo("102");
        assertThat(result.getStop().getStopPrice()).isEqualByComparingTo("95");

        BoundaryTakeProfitLevelDTO takeProfit = result.getTakeProfitLevels().get(0);
        assertThat(takeProfit.getPrice()).isEqualByComparingTo("112");
        assertThat(takeProfit.getSourceRef()).isEqualTo("tp-resistance-1");
        assertThat(result.getSourceFields().getEvidenceRefs())
                .containsExactly("entry-support-1", "stop-swing-low-1", "tp-resistance-1");

        SourceTraceDTO sourceTrace = result.getSourceTrace();
        assertThat(sourceTrace.getEntryPriceSource()).isEqualByComparingTo("101");
        assertThat(sourceTrace.getEntrySourceRef()).isEqualTo("entry-support-1");
        assertThat(sourceTrace.getStopPriceSource()).isEqualByComparingTo("95");
        assertThat(sourceTrace.getStopSourceRef()).isEqualTo("stop-swing-low-1");
        assertThat(sourceTrace.getTpPriceSources()).containsExactly(new BigDecimal("112"));
        assertThat(sourceTrace.getTpSourceRef()).isEqualTo("tp-resistance-1");
        assertThat(sourceTrace.getRrSource()).isEqualByComparingTo("2.2");
        assertThat(sourceTrace.getRrRuleRef()).isEqualTo("STRUCTURE_TARGET");
        assertIncompleteBecauseNonBoundarySourcesAreMissing(sourceTrace);
    }

    @Test
    void readyShortBoundaryMapsDirectionallyValidBoundary() {
        MarketStructureBoundaryDTO boundary = readyLongBoundary();
        boundary.setDirection("SHORT");
        boundary.setEntryLower(new BigDecimal("100"));
        boundary.setEntryUpper(new BigDecimal("102"));
        boundary.setStopPrice(new BigDecimal("108"));
        boundary.setStopSourceType("STRUCTURE_RESISTANCE_STOP");
        boundary.setStopSourceRef("stop-swing-high-1");
        boundary.setStopReason("recent swing high");
        boundary.setTakeProfitTargets(List.of(target(
                "92",
                "STRUCTURE_SUPPORT",
                "2.0",
                "tp-support-1",
                "STRUCTURE_TARGET",
                "support target below entry"
        )));
        boundary.setRrRatio(new BigDecimal("2.0"));

        SourceTraceBoundaryProducerResult result = producer.produce(boundary);

        assertThat(result.isBoundaryReady()).isTrue();
        assertThat(result.getSourceTrace().getEntryPriceSource()).isEqualByComparingTo("101");
        assertThat(result.getSourceTrace().getStopPriceSource()).isEqualByComparingTo("108");
        assertThat(result.getSourceTrace().getTpPriceSources()).containsExactly(new BigDecimal("92"));
        assertThat(result.getSourceFields().getEvidenceRefs())
                .containsExactly("entry-support-1", "stop-swing-high-1", "tp-support-1");
        assertIncompleteBecauseNonBoundarySourcesAreMissing(result.getSourceTrace());
    }

    @Test
    void boundaryReadyFalseFailsClosedWithoutPreciseBoundaryFields() {
        MarketStructureBoundaryDTO boundary = readyLongBoundary();
        boundary.setBoundaryReady(false);

        SourceTraceBoundaryProducerResult result = producer.produce(boundary);

        assertThat(result.isBoundaryReady()).isFalse();
        assertThat(result.getSourceTrace().getFallbackStatus())
                .isEqualTo(SourceTraceFallbackStatusEnum.SAFE_FAIL_CLOSED_ONLY);
        assertThat(result.getBlockingReasons()).contains("boundaryReady=false");
        assertThat(result.getEntry()).isNull();
        assertThat(result.getStop()).isNull();
        assertThat(result.getTakeProfitLevels()).isEmpty();
        assertThat(result.getSourceTrace().getEntryPriceSource()).isNull();
        assertThat(result.getSourceTrace().getStopPriceSource()).isNull();
        assertThat(result.getSourceTrace().getTpPriceSources()).isEmpty();
    }

    @Test
    void existingBlockingReasonsArePreserved() {
        MarketStructureBoundaryDTO boundary = readyLongBoundary();
        boundary.setBlockingReasons(List.of("stale_unsafe_source_window"));

        SourceTraceBoundaryProducerResult result = producer.produce(boundary);

        assertThat(result.getSourceTrace().getFallbackStatus())
                .isEqualTo(SourceTraceFallbackStatusEnum.SAFE_FAIL_CLOSED_ONLY);
        assertThat(result.getBlockingReasons()).containsExactly("stale_unsafe_source_window");
        assertThat(result.getSourceTrace().getBlockingReasons()).containsExactly("stale_unsafe_source_window");
    }

    @Test
    void missingEntryRefFailsClosed() {
        MarketStructureBoundaryDTO boundary = readyLongBoundary();
        boundary.setEntrySourceRef(null);

        SourceTraceBoundaryProducerResult result = producer.produce(boundary);

        assertThat(result.isBoundaryReady()).isFalse();
        assertThat(result.getMissingFields()).contains("entrySourceRef");
        assertThat(result.getSourceTrace().getFallbackStatus()).isEqualTo(SourceTraceFallbackStatusEnum.INCOMPLETE);
        assertThat(result.getSourceTrace().getEntryPriceSource()).isNull();
    }

    @Test
    void missingStopRefFailsClosed() {
        MarketStructureBoundaryDTO boundary = readyLongBoundary();
        boundary.setStopSourceRef(null);

        SourceTraceBoundaryProducerResult result = producer.produce(boundary);

        assertThat(result.isBoundaryReady()).isFalse();
        assertThat(result.getMissingFields()).contains("stopSourceRef");
        assertThat(result.getSourceTrace().getStopPriceSource()).isNull();
    }

    @Test
    void missingTakeProfitRefFailsClosed() {
        MarketStructureBoundaryDTO boundary = readyLongBoundary();
        MarketStructureTakeProfitTargetDTO target = boundary.getTakeProfitTargets().get(0);
        target.setSourceRef(null);

        SourceTraceBoundaryProducerResult result = producer.produce(boundary);

        assertThat(result.isBoundaryReady()).isFalse();
        assertThat(result.getMissingFields()).contains("takeProfitTargets[0].sourceRef");
        assertThat(result.getSourceTrace().getTpPriceSources()).isEmpty();
    }

    @Test
    void directionalSanityFailureFailsClosed() {
        MarketStructureBoundaryDTO longStopAboveEntry = readyLongBoundary();
        longStopAboveEntry.setStopPrice(new BigDecimal("105"));

        MarketStructureBoundaryDTO shortStopBelowEntry = readyLongBoundary();
        shortStopBelowEntry.setDirection("SHORT");
        shortStopBelowEntry.setStopPrice(new BigDecimal("95"));

        MarketStructureBoundaryDTO longTargetBelowEntry = readyLongBoundary();
        longTargetBelowEntry.setTakeProfitTargets(List.of(target(
                "99",
                "STRUCTURE_SUPPORT",
                "1.0",
                "tp-below-entry",
                "STRUCTURE_TARGET",
                "invalid long target"
        )));

        MarketStructureBoundaryDTO shortTargetAboveEntry = readyLongBoundary();
        shortTargetAboveEntry.setDirection("SHORT");
        shortTargetAboveEntry.setStopPrice(new BigDecimal("108"));
        shortTargetAboveEntry.setTakeProfitTargets(List.of(target(
                "112",
                "STRUCTURE_RESISTANCE",
                "1.0",
                "tp-above-entry",
                "STRUCTURE_TARGET",
                "invalid short target"
        )));

        assertBlocked(longStopAboveEntry, "LONG stop must be below entry midpoint");
        assertBlocked(shortStopBelowEntry, "SHORT stop must be above entry midpoint");
        assertBlocked(longTargetBelowEntry, "LONG target must be above entry midpoint");
        assertBlocked(shortTargetAboveEntry, "SHORT target must be below entry midpoint");
    }

    @Test
    void rrLadderPreservesRuleRefAndSourceType() {
        MarketStructureBoundaryDTO boundary = readyLongBoundary();
        boundary.setTakeProfitTargets(List.of(target(
                "110",
                "RR_LADDER",
                "2",
                "tp-rr-ladder-1",
                "RR_LADDER",
                "2R ladder target"
        )));
        boundary.setRrRatio(new BigDecimal("2"));

        SourceTraceBoundaryProducerResult result = producer.produce(boundary);

        assertThat(result.isBoundaryReady()).isTrue();
        assertThat(result.getTakeProfitLevels().get(0).getSource()).isEqualTo("RR_LADDER");
        assertThat(result.getTakeProfitLevels().get(0).getNumericSourceType()).isEqualTo("RR_LADDER");
        assertThat(result.getSourceFields().getRrRule()).isEqualTo("RR_LADDER");
        assertThat(result.getSourceTrace().getTpSourceType()).isEqualTo("RR_LADDER");
        assertThat(result.getSourceTrace().getRrRuleRef()).isEqualTo("RR_LADDER");
    }

    @Test
    void missingNonBoundarySourcesAreNotFabricated() {
        SourceTraceDTO sourceTrace = producer.produce(readyLongBoundary()).getSourceTrace();

        assertThat(sourceTrace.getLiquiditySource()).isNull();
        assertThat(sourceTrace.getMultiTimeframeSource()).isNull();
        assertThat(sourceTrace.getEventSource()).isNull();
        assertThat(sourceTrace.getWickSource()).isNull();
        assertThat(sourceTrace.getMissingFields()).containsExactly(
                "liquiditySource",
                "multiTimeframeSource",
                "eventSource",
                "wickSource"
        );
        assertThat(sourceTrace.hasRequiredBoundarySources()).isFalse();
    }

    @Test
    void producerDoesNotReferenceExternalActionSurfaces() throws Exception {
        String source = Files.readString(Path.of(
                "src/main/java/org/example/trademodel/service/impl/SourceTraceBoundaryProducerImpl.java"
        ));

        assertThat(source)
                .doesNotContain("UserPosition")
                .doesNotContain("Telegram")
                .doesNotContain("PushRecheck")
                .doesNotContain("PushDispatch")
                .doesNotContain("@Scheduled")
                .doesNotContain("RestTemplate")
                .doesNotContain("WebClient")
                .doesNotContain("manual-open");
    }

    @Test
    void nullBoundaryFailsClosedToIncompleteReviewOnlyTrace() {
        SourceTraceBoundaryProducerResult result = producer.produce(null);

        assertThat(result.isBoundaryReady()).isFalse();
        assertThat(result.isSourceTraceReady()).isFalse();
        assertThat(result.getMissingFields()).containsExactly("boundary");
        assertThat(result.getBlockingReasons()).containsExactly("boundary missing");
        assertThat(result.getSourceTrace().getFallbackStatus())
                .isEqualTo(SourceTraceFallbackStatusEnum.SAFE_FAIL_CLOSED_ONLY);
        assertThat(result.getSourceTrace().isManualReviewRequired()).isTrue();
        assertThat(result.getSourceTrace().isNotTradeInstruction()).isTrue();
    }

    private void assertIncompleteBecauseNonBoundarySourcesAreMissing(SourceTraceDTO sourceTrace) {
        assertThat(sourceTrace.getFallbackStatus()).isEqualTo(SourceTraceFallbackStatusEnum.INCOMPLETE);
        assertThat(sourceTrace.getMissingFields()).containsExactly(
                "liquiditySource",
                "multiTimeframeSource",
                "eventSource",
                "wickSource"
        );
        assertThat(sourceTrace.getLiquiditySource()).isNull();
        assertThat(sourceTrace.getMultiTimeframeSource()).isNull();
        assertThat(sourceTrace.getEventSource()).isNull();
        assertThat(sourceTrace.getWickSource()).isNull();
        assertThat(sourceTrace.hasRequiredBoundarySources()).isFalse();
        assertThat(sourceTrace.isManualReviewRequired()).isTrue();
        assertThat(sourceTrace.isNotTradeInstruction()).isTrue();
    }

    private void assertBlocked(MarketStructureBoundaryDTO boundary, String reason) {
        SourceTraceBoundaryProducerResult result = producer.produce(boundary);

        assertThat(result.isBoundaryReady()).isFalse();
        assertThat(result.getSourceTrace().getFallbackStatus())
                .isEqualTo(SourceTraceFallbackStatusEnum.SAFE_FAIL_CLOSED_ONLY);
        assertThat(result.getBlockingReasons()).contains(reason);
    }

    private MarketStructureBoundaryDTO readyLongBoundary() {
        MarketStructureBoundaryDTO boundary = new MarketStructureBoundaryDTO();
        boundary.setSymbol("BTCUSDT");
        boundary.setDirection("LONG");
        boundary.setTimeframe("1h");
        boundary.setGeneratedAt(LocalDateTime.of(2026, 7, 2, 12, 0));
        boundary.setEntryLower(new BigDecimal("100"));
        boundary.setEntryUpper(new BigDecimal("102"));
        boundary.setEntrySourceType("STRUCTURE_SUPPORT");
        boundary.setEntrySourceRef("entry-support-1");
        boundary.setEntryReason("support retest");
        boundary.setStopPrice(new BigDecimal("95"));
        boundary.setStopSourceType("STRUCTURE_SWING_LOW");
        boundary.setStopSourceRef("stop-swing-low-1");
        boundary.setStopReason("recent swing low");
        boundary.setTakeProfitTargets(List.of(target(
                "112",
                "STRUCTURE_RESISTANCE",
                "2.2",
                "tp-resistance-1",
                "STRUCTURE_TARGET",
                "resistance target above entry"
        )));
        boundary.setRrRatio(new BigDecimal("2.2"));
        boundary.setFreshnessStatus("FRESH");
        boundary.setDataQualityStatus("OK");
        boundary.setBoundaryReady(true);
        return boundary;
    }

    private MarketStructureTakeProfitTargetDTO target(
            String price,
            String targetType,
            String rr,
            String sourceRef,
            String ruleRef,
            String reason
    ) {
        MarketStructureTakeProfitTargetDTO target = new MarketStructureTakeProfitTargetDTO();
        target.setTargetPrice(new BigDecimal(price));
        target.setTargetType(targetType);
        target.setRr(new BigDecimal(rr));
        target.setSourceRef(sourceRef);
        target.setRuleRef(ruleRef);
        target.setReason(reason);
        return target;
    }
}
