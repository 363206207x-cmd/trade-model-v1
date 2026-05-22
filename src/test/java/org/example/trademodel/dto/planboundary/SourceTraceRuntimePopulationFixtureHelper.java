package org.example.trademodel.dto.planboundary;

import java.util.List;

final class SourceTraceRuntimePopulationFixtureHelper {

    static final String REVIEW_ONLY = SourceTraceEntrySourceReviewModeEnum.REVIEW_ONLY.name();

    private static final MarketReadOnlyCandidateGenerator GENERATOR = new InertMarketReadOnlyCandidateGenerator();

    private SourceTraceRuntimePopulationFixtureHelper() {
    }

    static RuntimePopulationFixture completeReviewOnlyFixture() {
        return fromSnapshot(completeSnapshotBuilder().build());
    }

    static RuntimePopulationFixture missingSourceIdentityFixture() {
        return fromSnapshot(completeSnapshotBuilder()
                .sourceOwner(null)
                .sourceRef(null)
                .sourceTimeframe(null)
                .sourceWindow(null)
                .build());
    }

    static RuntimePopulationFixture conflictingSourceFixture() {
        return fromSnapshot(completeSnapshotBuilder()
                .conflictFamilyStatus(MarketReadOnlyEvidenceStatusEnum.CONFLICT)
                .build());
    }

    static RuntimePopulationFixture unsafeSubstitutionFixture() {
        return fromSnapshot(completeSnapshotBuilder()
                .forbiddenInputMarkers(List.of("latest_price_only"))
                .build());
    }

    static RuntimePopulationFixture staleUnsafeSourceWindowFixture() {
        return fromSnapshot(completeSnapshotBuilder()
                .sourceWindow("stale-unsafe-source-window")
                .freshnessStatus(MarketReadOnlyEvidenceStatusEnum.STALE)
                .riskActionGuardBlockers(List.of("stale_unsafe_source_window"))
                .build());
    }

    private static RuntimePopulationFixture fromSnapshot(MarketReadOnlyEvidenceSnapshotDTO snapshot) {
        MarketReadOnlyCandidateResultDTO result = GENERATOR.review(snapshot);
        return new RuntimePopulationFixture(populateSourceTrace(snapshot, result), snapshot, result);
    }

    private static SourceTraceDTO populateSourceTrace(
            MarketReadOnlyEvidenceSnapshotDTO snapshot,
            MarketReadOnlyCandidateResultDTO result
    ) {
        SourceTraceDTO trace = new SourceTraceDTO();
        trace.setSymbol(snapshot.getSymbol());
        trace.setSymbolSource("market_read_only_snapshot.symbol");
        trace.setTimeframe(snapshot.getTimeframe());
        trace.setTimeframeSource("market_read_only_snapshot.timeframe");
        trace.setSourceOwner(snapshot.getSourceOwner());
        trace.setSourceRef(snapshot.getSourceRef());
        trace.setSourceTimeframe(snapshot.getSourceTimeframe());
        trace.setSourceWindow(snapshot.getSourceWindow());
        trace.setFreshnessStatus(snapshot.getFreshnessStatus() == null ? null : snapshot.getFreshnessStatus().name());
        trace.setQuoteFreshnessStatus(trace.getFreshnessStatus());
        trace.setFallbackStatus(resolveFallbackStatus(result.getCandidateStatus()));
        trace.setMissingFields(snapshot.getMissingFields());
        trace.setBlockingReasons(result.getBlockingReasons());
        trace.setReviewMode(result.getReviewMode().name());
        trace.setManualReviewRequired(result.isManualReviewRequired());
        trace.setNotTradeInstruction(result.isNotTradeInstruction());
        return trace;
    }

    private static SourceTraceFallbackStatusEnum resolveFallbackStatus(MarketReadOnlyCandidateStatusEnum status) {
        if (status == MarketReadOnlyCandidateStatusEnum.INCOMPLETE) {
            return SourceTraceFallbackStatusEnum.INCOMPLETE;
        }
        if (status == MarketReadOnlyCandidateStatusEnum.BLOCKED) {
            return SourceTraceFallbackStatusEnum.SAFE_FAIL_CLOSED_ONLY;
        }
        return SourceTraceFallbackStatusEnum.WATCH_ONLY;
    }

    private static MarketReadOnlyEvidenceSnapshotDTO.Builder completeSnapshotBuilder() {
        return MarketReadOnlyEvidenceSnapshotDTO.builder()
                .symbol("BTCUSDT")
                .timeframe("1h")
                .evidenceRefs(List.of(
                        "p155-source-trace-owner-ref",
                        "p155-source-trace-window-ref",
                        "p155-source-trace-freshness-ref"
                ))
                .evidenceFamilies(List.of(
                        MarketReadOnlyEvidenceFamilyEnum.MARKET_STRUCTURE,
                        MarketReadOnlyEvidenceFamilyEnum.KLINE_DERIVED_STRUCTURE,
                        MarketReadOnlyEvidenceFamilyEnum.ATR_VOLATILITY,
                        MarketReadOnlyEvidenceFamilyEnum.LIQUIDITY_TARGET,
                        MarketReadOnlyEvidenceFamilyEnum.PRIOR_HIGH_LOW,
                        MarketReadOnlyEvidenceFamilyEnum.EVENT,
                        MarketReadOnlyEvidenceFamilyEnum.WICK_PIN_BAR,
                        MarketReadOnlyEvidenceFamilyEnum.MULTI_TIMEFRAME
                ))
                .sourceOwner("p155-runtime-source-owner")
                .sourceRef("p155-runtime-source-ref")
                .sourceTimeframe("1h")
                .sourceReason("p155 review-only SourceTrace runtime population fixture")
                .sourceWindow("p155-runtime-source-window")
                .ruleId("p155-source-trace-runtime-population-fixture")
                .ruleVersion("p155.v1")
                .freshnessStatus(MarketReadOnlyEvidenceStatusEnum.FRESH)
                .conflictFamilyStatus(MarketReadOnlyEvidenceStatusEnum.CLEAR)
                .dataQualityScore(80)
                .eventEvidenceStatus(MarketReadOnlyEvidenceStatusEnum.PRESENT)
                .liquidityEvidenceStatus(MarketReadOnlyEvidenceStatusEnum.PRESENT)
                .wickPinBarEvidenceStatus(MarketReadOnlyEvidenceStatusEnum.PRESENT)
                .multiTimeframeEvidenceStatus(MarketReadOnlyEvidenceStatusEnum.PRESENT)
                .riskActionGuardContext("review_only_no_direct_action");
    }

    record RuntimePopulationFixture(
            SourceTraceDTO sourceTrace,
            MarketReadOnlyEvidenceSnapshotDTO snapshot,
            MarketReadOnlyCandidateResultDTO candidateResult
    ) {

        MarketReadOnlyCandidateStatusEnum candidateStatus() {
            return candidateResult.getCandidateStatus();
        }
    }
}
